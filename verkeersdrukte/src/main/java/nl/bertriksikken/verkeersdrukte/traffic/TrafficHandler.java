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
        executor.schedule(this::downloadTrafficSpeed, interval.toMillis(), TimeUnit.MILLISECONDS);

        notifyClients();
    }

    private void decode(ByteArrayInputStream inputStream) throws IOException {
        LOG.info("Parsing");
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            JsonNode node = mapper.readValue(gzis, JsonNode.class);
            JsonNode d2LogicalModel = node.at("/Body/d2LogicalModel");
            D2LogicalModel model = mapper.treeToValue(d2LogicalModel, D2LogicalModel.class);
            D2LogicalModel.PayloadPublication payloadPublication = model.payloadPublication;
            LOG.info("Payload publication: type {}, time {}", payloadPublication.type, payloadPublication.publicationTime);

            D2LogicalModel.MeasuredDataPublication measuredDataPublication = (D2LogicalModel.MeasuredDataPublication) payloadPublication;
            int numMeasurements = 0;
            int numSites = 0;
            for (SiteMeasurements measurements : measuredDataPublication.siteMeasurementsList) {
                for (MeasuredValue value : measurements.measuredValueList) {
                    numMeasurements++;
                }
                numSites++;
            }
            LOG.info("Parsed {} measurements from {} sites", numMeasurements, numSites);
        }
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
