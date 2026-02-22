package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Runnables;
import io.dropwizard.lifecycle.Managed;
import nl.bertriksikken.datex2.MeasuredDataPublication;
import nl.bertriksikken.datex2.MeasuredValue;
import nl.bertriksikken.datex2.MeasurementSiteRecord;
import nl.bertriksikken.datex2.MeasurementSiteRecord.MeasurementSpecificCharacteristicsElement;
import nl.bertriksikken.datex2.MeasurementSiteTable;
import nl.bertriksikken.datex2.SiteMeasurements;
import nl.bertriksikken.datex2.VmsPublication;
import nl.bertriksikken.datex2.VmsTablePublication;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.geojson.FeatureCollection.Feature;
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
import java.util.Objects;
import java.util.TreeMap;
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
    private final XmlMapper xmlMapper = new XmlMapper();
    private final NdwClient ndwClient;
    private final MeasurementCache measurementCache;
    private final NdwDownloader ndwDownloader;
    private final ShapeFileDownloader shapeFileDownloader;
    private MeasurementSiteTable mst = new MeasurementSiteTable();

    private FeatureCollection shapeFile = new FeatureCollection();
    private VmsTablePublication vmsLocationTable = new VmsTablePublication();
    private VmsPublication vmsPublication;

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
        LOG.info("Schedule shapefile/MST download ...");
        schedule(this::downloadShapeFileMst, Duration.ZERO);

        // schedule regular fetches, starting immediately
        LOG.info("Schedule traffic speed download ...");
        schedule(this::downloadTrafficSpeed, Duration.ZERO);

        // schedule VMS download
        schedule(this::downloadVmsPublication, Duration.ZERO);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void schedule(Runnable action, Duration delay) {
        Runnable runnable = () -> {
            try {
                action.run();
            } catch (Throwable e) {
                LOG.warn("Caught throwable in scheduled task, exiting ...", e);
                System.exit(1);
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

    private void downloadVmsPublication() {
        LOG.info("Download VMS publication");
        Instant next;
        try {
            FileResponse response = ndwClient.getVmsPublication();
            Duration age = Duration.between(response.getLastModified(), Instant.now());
            next = response.getLastModified().plusSeconds(65);
            LOG.info("Got data, {} bytes, age {}", response.getContents().length, age);
            decodeVmsPublication(new ByteArrayInputStream(response.getContents()));
        } catch (IOException e) {
            LOG.warn("Download VMS failed", e);
            next = Instant.now().plusSeconds(60);
        }

        // schedule next
        Duration interval = Duration.between(Instant.now(), next);
        while (interval.isNegative()) {
            interval = interval.plusSeconds(60);
        }
        LOG.info("Scheduling next VMS download in {}", interval);
        schedule(this::downloadVmsPublication, interval);

        notifyClients();

    }

    private void downloadShapeFileMst() {
        // get VMS location table
        LOG.info("Fetching VMS location table...");
        try {
            File file = ndwDownloader.fetchFile(INdwApi.VMS_LOCATION_TABLE);
            if (file != null) {
                try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(file))) {
                    LOG.info("Parsing VMS location table...");
                    Stopwatch sw = Stopwatch.createStarted();
                    VmsTablePublication vmsTablePublication = new VmsTablePublication();
                    vmsTablePublication.parse(gzis);
                    LOG.info("Parsed VMS location table, {} entries, took {}", vmsTablePublication.getRecords().size(), sw.elapsed());
                    vmsLocationTable = vmsTablePublication;
                }
            } else {
                LOG.warn("VMS location table downloaded");
            }
        } catch (IOException e) {
            LOG.warn("VMS location table download failed: {}", e.getMessage());
        }

        // get shape file
        LOG.info("Fetching shapefile...");
        try {
            if (shapeFileDownloader.download()) {
                shapeFile = shapeFileDownloader.getGeoJson();
                LOG.info("Parsed shapefile, {} features", shapeFile.getFeatures().size());
            }
        } catch (IOException e) {
            LOG.warn("Shapefile download failed with exception: {}", e.getMessage());
        }

        // get MST
        LOG.info("Fetching MST...");
        try {
            File file = ndwDownloader.fetchFile(INdwApi.MEASUREMENT_SITE_TABLE);
            if (file != null) {
                try (FileInputStream fis = new FileInputStream(file);
                     GZIPInputStream gzis = new GZIPInputStream(fis)) {
                    LOG.info("Parsing MST...");
                    Stopwatch sw = Stopwatch.createStarted();
                    mst = new MeasurementSiteTable();
                    mst.parse(gzis, shapeFileDownloader.getSiteIds());
                    LOG.info("Parsed MST, {} entries, took {}", mst.getMeasurementSiteIds().size(), sw.elapsed());
                }
            } else {
                LOG.warn("MST not downloaded");
            }
        } catch (IOException e) {
            LOG.warn("MST download failed: {}", e.getMessage());
        }

        // reschedule
        schedule(this::downloadShapeFileMst, Duration.ofDays(1));
    }

    private void decode(InputStream inputStream) throws IOException {
        MeasuredDataPublication publication = new MeasuredDataPublication(xmlMapper);
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            LOG.info("Parsing MDP...");
            Stopwatch sw = Stopwatch.createStarted();
            publication.parse(gzis);
            List<SiteMeasurements> siteMeasurementsList = publication.getSiteMeasurementsList();
            for (SiteMeasurements measurements : siteMeasurementsList) {
                SiteMeasurement siteMeasurement = processSiteMeasurements(measurements);
                measurementCache.put(measurements.reference.id, siteMeasurement);
            }
            LOG.info("Parsed MDP, {} entries, took {}", siteMeasurementsList.size(), sw.elapsed());
        }
    }

    private void decodeVmsPublication(InputStream inputStream) throws IOException {
        VmsPublication publication = new VmsPublication(xmlMapper);
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            LOG.info("Parsing VmsPublication...");
            Stopwatch sw = Stopwatch.createStarted();
            publication.parse(gzis);
            LOG.info("Parsed VmsPublication, {} entries, took {}", publication.getRecords().size(), sw.elapsed());
            vmsPublication = publication;
        }
    }

    /**
     * Processes NDW measurements, relating them to the MST,  finding the "anyVehicle" measurements per lane.
     */
    private SiteMeasurement processSiteMeasurements(SiteMeasurements measurements) {
        Instant dateTime = measurements.getMeasurementTime();
        SiteMeasurement measurement = new SiteMeasurement(dateTime);
        // group by type
        Map<String, MeasuredValue.TrafficFlow> flows = new TreeMap<>(); // sorted on keys (lane id)
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
            String vehicleType = Objects.toString(chars.specificVehicleCharacteristics().vehicleType(), "");
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
    public Feature getStaticData(String location) {
        for (Feature feature : shapeFile.getFeatures()) {
            String dlgLoc = feature.getProperties().get("dgl_loc").toString();
            if (location.equals(dlgLoc)) {
                return feature;
            }
        }
        // not found
        return null;
    }

    @Override
    public VmsTablePublication getVmsLocationTable() {
        return vmsLocationTable;
    }

    @Override
    public VmsPublication getVmsPublication() {
        return vmsPublication;
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
