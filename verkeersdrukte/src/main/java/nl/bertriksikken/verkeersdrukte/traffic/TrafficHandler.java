package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.util.concurrent.Runnables;
import io.dropwizard.lifecycle.Managed;
import nl.bertriksikken.datex2.D2LogicalModel;
import nl.bertriksikken.datex2.MeasuredValue;
import nl.bertriksikken.datex2.SiteMeasurements;
import nl.bertriksikken.verkeersdrukte.ndw.FileResponse;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

public final class TrafficHandler implements ITrafficHandler, Managed {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficHandler.class);

    private final Map<String, Subscription> subscriptions = new HashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final NdwClient ndwClient;
    private final ObjectMapper mapper;
    private MeasurementCache measurementCache = new MeasurementCache("");

    public TrafficHandler(NdwConfig config) {
        this.ndwClient = NdwClient.create(config);
        this.mapper = new XmlMapper();
    }

    @Override
    public void start() {
        // schedule regular fetches, starting immediately
        executor.schedule(this::downloadTrafficSpeed, 0, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Error stopping executor", e);
        }
    }

    private void downloadTrafficSpeed() {
        LOG.info("Download traffic/speed");
        Instant next;
        try {
            FileResponse response = ndwClient.getTrafficSpeed();
            Duration age = Duration.between(response.getLastModified(), Instant.now());
            next = response.getLastModified().plusSeconds(65);
            LOG.info("Got data, {} bytes, age {}", response.getContents().length, age);
            measurementCache = decode(new ByteArrayInputStream(response.getContents()));
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
        executor.schedule(this::downloadTrafficSpeed, interval.toMillis(), TimeUnit.MILLISECONDS);

        notifyClients();
    }

    private MeasurementCache decode(ByteArrayInputStream inputStream) throws IOException {
        LOG.info("Parsing");
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            JsonNode node = mapper.readValue(gzis, JsonNode.class);
            JsonNode d2LogicalModel = node.at("/Body/d2LogicalModel");
            D2LogicalModel model = mapper.treeToValue(d2LogicalModel, D2LogicalModel.class);
            D2LogicalModel.PayloadPublication payloadPublication = model.payloadPublication;
            LOG.info("Payload publication: type {}, time {}", payloadPublication.type, payloadPublication.publicationTime);

            D2LogicalModel.MeasuredDataPublication measuredDataPublication = (D2LogicalModel.MeasuredDataPublication) payloadPublication;
            MeasurementCache snapshot = new MeasurementCache(measuredDataPublication.publicationTime);
            for (SiteMeasurements measurements : measuredDataPublication.siteMeasurementsList) {
                AggregateMeasurement aggregateMeasurement = aggregateValues(measurements);
                snapshot.put(measurements.reference.id, aggregateMeasurement);
            }
            return snapshot;
        }
    }


    private AggregateMeasurement aggregateValues(SiteMeasurements measurements) {
        String dateTime = measurements.measurementTimeDefault;
        // group by type
        List<MeasuredValue.TrafficFlow> flows = new ArrayList<>();
        List<MeasuredValue.TrafficSpeed> speeds = new ArrayList<>();
        for (MeasuredValue value : measurements.measuredValueList) {
            switch (value.measuredValue.basicData.type) {
                case "TrafficFlow":
                    flows.add((MeasuredValue.TrafficFlow) value.measuredValue.basicData);
                    break;
                case "TrafficSpeed":
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
            sumFlowSpeed += flow.vehicleFlow.vehicleFlowRate * speed.averageVehicleSpeed.speed;
            sumFlow += flow.vehicleFlow.vehicleFlowRate;
        }
        double aggregateSpeed = sumFlowSpeed / sumFlow;
        return new AggregateMeasurement(dateTime, sumFlow, aggregateSpeed);
    }

    @Override
    public AggregateMeasurement getDynamicData(String location) {
        return measurementCache.get(location);
    }

    @Override
    public void subscribe(String clientId, INotifyData callback) {
        Subscription subscription = new Subscription(clientId, callback);
        subscriptions.put(clientId, subscription);
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
        List<Subscription> copy = List.copyOf(subscriptions.values());
        copy.forEach(subscription -> subscription.callback.notifyUpdate());
    }

    private static final class Subscription {
        private final String clientId;
        private final INotifyData callback;

        Subscription(String clientId, INotifyData callback) {
            this.clientId = clientId;
            this.callback = callback;
        }
    }
}
