package nl.bertriksikken.verkeersdrukte.traffic;

import com.google.common.util.concurrent.Runnables;
import io.dropwizard.lifecycle.Managed;
import nl.bertriksikken.datex2.MeasuredDataPublication;
import nl.bertriksikken.datex2.MeasuredValue;
import nl.bertriksikken.datex2.SiteMeasurements;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.verkeersdrukte.app.VerkeersDrukteAppConfig;
import nl.bertriksikken.verkeersdrukte.ndw.FileResponse;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final ShapeFileDownloader shapeFileDownloader;
    private FeatureCollection shapeFile;

    public TrafficHandler(VerkeersDrukteAppConfig config) {
        ndwClient = NdwClient.create(config.getNdwConfig());
        measurementCache = new MeasurementCache(config.getTrafficConfig().getExpiryDuration());
        shapeFileDownloader = new ShapeFileDownloader(config.getTrafficConfig().getShapeFileFolder(), ndwClient);
    }

    @Override
    public void start() {
        // schedule shape file download
        LOG.info("Schedule shape file download ...");
        schedule(this::downloadShapeFile, Duration.ZERO);

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
            LOG.info("Downloading shapefile ...");
            if (shapeFileDownloader.download()) {
                shapeFile = shapeFileDownloader.getFeatureCollection();
            }
        } catch (IOException e) {
            LOG.warn("Shapefile download failed: {}", e.getMessage());
        }
        // reschedule
        schedule(this::downloadShapeFile, Duration.ofDays(1));
    }

    private void decode(ByteArrayInputStream inputStream) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            MeasuredDataPublication publication = MeasuredDataPublication.parse(gzis);
            LOG.info("Got data for time {}", publication.getPublicationTime());
            for (SiteMeasurements measurements : publication.getSiteMeasurementsList()) {
                AggregateMeasurement aggregateMeasurement = aggregateValues(measurements);
                measurementCache.put(measurements.reference.id, aggregateMeasurement);
            }
        }
    }

    private AggregateMeasurement aggregateValues(SiteMeasurements measurements) {
        Instant dateTime = measurements.getMeasurementTime();
        // group by type
        List<MeasuredValue.TrafficFlow> flows = new ArrayList<>();
        List<MeasuredValue.TrafficSpeed> speeds = new ArrayList<>();
        for (MeasuredValue value : measurements.measuredValueList) {
            switch (value.measuredValue.basicData.type) {
                case MeasuredValue.TrafficFlow.TYPE:
                    flows.add((MeasuredValue.TrafficFlow) value.measuredValue.basicData);
                    break;
                case MeasuredValue.TrafficSpeed.TYPE:
                    speeds.add((MeasuredValue.TrafficSpeed) value.measuredValue.basicData);
                    break;
                default:
                    break;
            }
        }
        if (flows.isEmpty() || (flows.size() != speeds.size())) {
            // cannot determine speed
            return new AggregateMeasurement(dateTime, Double.NaN, Double.NaN);
        }

        // aggregate flow as simple sum, speed as flow-weighted sum
        double sumFlowSpeed = 0.0;
        double sumFlow = 0.0;
        for (int i = 0; i < flows.size(); i++) {
            MeasuredValue.TrafficFlow flow = flows.get(i);
            MeasuredValue.TrafficSpeed speed = speeds.get(i);
            double flowValue = flow.vehicleFlow.dataError ? Double.NaN : flow.vehicleFlow.vehicleFlowRate;
            double speedValue = speed.averageVehicleSpeed.dataError ? Double.NaN : speed.averageVehicleSpeed.speed;
            sumFlowSpeed += flowValue * speedValue;
            sumFlow += flowValue;
        }
        double aggregateSpeed = sumFlowSpeed / sumFlow;
        return new AggregateMeasurement(dateTime, sumFlow, aggregateSpeed);
    }

    @Override
    public AggregateMeasurement getDynamicData(String location) {
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
