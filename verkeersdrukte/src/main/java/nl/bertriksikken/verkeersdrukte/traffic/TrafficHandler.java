package nl.bertriksikken.verkeersdrukte.traffic;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Runnables;
import io.dropwizard.lifecycle.Managed;
import nl.bertriksikken.datex2.MeasuredDataPublication;
import nl.bertriksikken.datex2.MeasuredValue;
import nl.bertriksikken.datex2.MeasurementSiteRecord;
import nl.bertriksikken.datex2.MeasurementSiteRecord.MeasurementSpecificCharacteristicsElement;
import nl.bertriksikken.datex2.MeasurementSiteTable;
import nl.bertriksikken.datex2.SiteMeasurements;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.verkeersdrukte.app.VerkeersDrukteAppConfig;
import nl.bertriksikken.verkeersdrukte.ndw.FileResponse;
import nl.bertriksikken.verkeersdrukte.ndw.INdwApi;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import nl.bertriksikken.verkeersdrukte.ndw.NdwDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

public final class TrafficHandler implements ITrafficHandler, Managed {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficHandler.class);

    private final Map<String, INotifyData> subscriptions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final NdwClient ndwClient;
    private final MeasurementCache measurementCache;
    private final NdwDownloader ndwDownloader;
    private final ShapeFileDownloader shapeFileDownloader;
    private MeasurementSiteTable mst = new MeasurementSiteTable(Set.of());

    private FeatureCollection shapeFile = new FeatureCollection();

    public TrafficHandler(VerkeersDrukteAppConfig config) {
        ndwClient = NdwClient.create(config.getNdwConfig());
        measurementCache = new MeasurementCache(config.getTrafficConfig().getExpiryDuration());
        ndwDownloader = new NdwDownloader(config.getNdwConfig());
        shapeFileDownloader = new ShapeFileDownloader(config.getTrafficConfig().getShapeFileFolder(), ndwDownloader);
    }

    @Override
    public void start() {
        ndwDownloader.start();

        // schedule shape file download
        LOG.info("Schedule shapefile download ...");
        schedule(this::downloadShapeFile, Duration.ZERO);

        // schedule MST download
        LOG.info("Schedule MST download...");
        schedule(this::downloadMst, Duration.ZERO);

        // schedule regular fetches, starting immediately
        LOG.info("Schedule traffic speed download ...");
        schedule(this::downloadTrafficSpeed, Duration.ZERO);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void schedule(Runnable action, Duration delay) {
        Runnable runnable = () -> {
            try {
                action.run();
            } catch (Throwable e) {
                LOG.warn("Caught throwable in scheduled task", e);
            }
        };
        executor.schedule(runnable, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        ndwDownloader.close();
        ndwClient.close();
        executor.shutdownNow();
    }

    private void downloadTrafficSpeed() {
        LOG.info("Download traffic/speed");
        Instant next;
        try {
            FileResponse response = ndwClient.getTrafficSpeed();
            Duration age = Duration.between(response.getLastModified(), Instant.now());
            next = response.getLastModified().plusSeconds(65);
            LOG.info("Got data, {} bytes, age {}", response.getContents().length, age);
            decode(new ByteArrayInputStream(response.getContents()));
        } catch (IOException e) {
            LOG.warn("Download failed", e);
            next = Instant.now().plusSeconds(60);
        }

        // schedule next
        Duration interval = Duration.between(Instant.now(), next);
        while (interval.isNegative()) {
            interval = interval.plusSeconds(60);
        }
        LOG.info("Scheduling next download in {}", interval);
        schedule(this::downloadTrafficSpeed, interval);

        notifyClients();
    }

    private void downloadShapeFile() {
        try {
            if (shapeFileDownloader.download()) {
                shapeFile = shapeFileDownloader.getGeoJson();
                LOG.info("Parsed shapefile, {} features", shapeFile.getFeatures().size());
            }
        } catch (IOException e) {
            LOG.warn("Shapefile download failed with exception: {}", e.getMessage());
        }
        // reschedule
        schedule(this::downloadShapeFile, Duration.ofDays(1));
    }

    private void downloadMst() {
        LOG.info("Downloading MST...");
        try {
            File file = ndwDownloader.fetchFile(INdwApi.MEASUREMENT_SITE_TABLE);
            if (file != null) {
                try (FileInputStream fis = new FileInputStream(file);
                     GZIPInputStream gzis = new GZIPInputStream(fis)) {
                    LOG.info("Parsing MST...");
                    Instant startTime = Instant.now();
                    mst = new MeasurementSiteTable(shapeFileDownloader.getSiteIds());
                    mst.parse(gzis);
                    LOG.info("Parsed MST, {} entries, took {}",
                            mst.getMeasurementSiteIds().size(), Duration.between(startTime, Instant.now()));
                }
            } else {
                LOG.warn("MST not downloaded");
            }
        } catch (IOException e) {
            LOG.warn("MST download failed: {}", e.getMessage());
        }
    }

    private void decode(InputStream inputStream) throws IOException {
        MeasuredDataPublication publication = new MeasuredDataPublication();
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            publication.parse(gzis);
            for (SiteMeasurements measurements : publication.getSiteMeasurementsList()) {
                SiteMeasurement siteMeasurement = processSiteMeasurements(measurements);
                measurementCache.put(measurements.reference.id, siteMeasurement);
            }
        }
    }

    /**
     * Processes NDW measurements, relating them to the MST,  finding the "anyVehicle" measurements per lane.
     */
    private SiteMeasurement processSiteMeasurements(SiteMeasurements measurements) {
        Instant dateTime = measurements.getMeasurementTime();
        SiteMeasurement measurement = new SiteMeasurement(dateTime);
        // group by type
        Map<String, MeasuredValue.TrafficFlow> flows = new HashMap<>();
        Map<String, MeasuredValue.TrafficSpeed> speeds = new HashMap<>();
        String siteId = measurements.reference.id;
        MeasurementSiteRecord msr = mst.findMeasurementSiteRecord(siteId);
        if (msr == null) {
            return measurement;
        }
        for (MeasuredValue value : measurements.measuredValueList) {
            // check vehicle type from MST against "anyVehicle"
            MeasurementSpecificCharacteristicsElement chars =
                    msr.findCharacteristic(value.index);
            if (chars == null) {
                LOG.warn("MeasurementSpecificCharacteristics not found for site '{}', index '{}", siteId, value.index);
                continue;
            }
            String vehicleType = Strings.nullToEmpty(chars.specificVehicleCharacteristics().vehicleType());
            if (!vehicleType.equals("anyVehicle")) {
                continue;
            }
            String lane = chars.specificLane();
            MeasuredValue.BasicData basicData = value.measuredValue.basicData();
            switch (basicData.type) {
                case MeasuredValue.TrafficFlow.TYPE -> flows.put(lane, (MeasuredValue.TrafficFlow) basicData);
                case MeasuredValue.TrafficSpeed.TYPE -> speeds.put(lane, (MeasuredValue.TrafficSpeed) basicData);
                default -> {
                }
            }
        }
        if (flows.isEmpty() || (flows.size() != speeds.size())) {
            // cannot determine speed
            return measurement;
        }

        for (String lane : flows.keySet()) {
            MeasuredValue.TrafficFlow flow = flows.get(lane);
            MeasuredValue.TrafficSpeed speed = speeds.get(lane);
            double flowValue = flow.vehicleFlow.dataError ? Double.NaN : flow.vehicleFlow.vehicleFlowRate;
            double speedValue;
            if (flowValue > 0) {
                speedValue = speed.averageVehicleSpeed.dataError ? Double.NaN : speed.averageVehicleSpeed.speed;
            } else {
                speedValue = Double.NaN;
            }
            measurement.addLaneMeasurement(lane, flowValue, speedValue);
        }
        return measurement;
    }

    @Override
    public SiteMeasurement getDynamicData(String location) {
        return measurementCache.get(location);
    }

    @Override
    public FeatureCollection getStaticData() {
        return shapeFile;
    }

    @Override
    public FeatureCollection.Feature getStaticData(String location) {
        for (FeatureCollection.Feature feature : shapeFile.getFeatures()) {
            String dlgLoc = feature.getProperties().get("dgl_loc").toString();
            if (location.equals(dlgLoc)) {
                return feature;
            }
        }
        // not found
        return null;
    }

    @Override
    public void subscribe(String clientId, INotifyData callback) {
        subscriptions.put(clientId, callback);
    }

    @Override
    public void unsubscribe(String clientId) {
        subscriptions.remove(clientId);
    }

    @Override
    public boolean isHealthy() {
        // try to submit something to the executor, and see if it responds
        try {
            executor.submit(Runnables::doNothing).get(3, TimeUnit.SECONDS);
        } catch (RejectedExecutionException | InterruptedException | TimeoutException | ExecutionException e) {
            return false;
        }
        return true;
    }

    private void notifyClients() {
        List.copyOf(subscriptions.values()).forEach(INotifyData::notifyUpdate);
    }

}
