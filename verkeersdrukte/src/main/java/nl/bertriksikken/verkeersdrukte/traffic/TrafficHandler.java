package nl.bertriksikken.verkeersdrukte.traffic;

import com.google.common.util.concurrent.Runnables;
import io.dropwizard.lifecycle.Managed;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import nl.bertriksikken.verkeersdrukte.ndw.FileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public final class TrafficHandler implements ITrafficHandler, Managed {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficHandler.class);

    private final Map<String, Subscription> subscriptions = new HashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final NdwClient ndwClient;

    public TrafficHandler(NdwConfig config) {
        this.ndwClient = NdwClient.create(config);

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
        } catch (IOException e) {
            LOG.warn("Download failed", e);
            next = Instant.now().plusSeconds(60);
        }

        Duration interval = Duration.between(Instant.now(), next);
        while (interval.isNegative()) {
            interval = interval.plusSeconds(60);
        }
        LOG.info("Scheduling next download in {}, at {}", interval, next);
        executor.schedule(this::downloadTrafficSpeed, interval.toMillis(), TimeUnit.MILLISECONDS);

        notifyClients();
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
