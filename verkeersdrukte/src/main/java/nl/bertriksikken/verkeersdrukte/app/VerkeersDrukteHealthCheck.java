package nl.bertriksikken.verkeersdrukte.app;

import com.codahale.metrics.health.HealthCheck;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficHandler;

public final class VerkeersDrukteHealthCheck extends HealthCheck {
    private final TrafficHandler handler;

    public VerkeersDrukteHealthCheck(TrafficHandler handler) {
        this.handler = handler;
    }

    @Override
    protected Result check() throws Exception {
        return handler.isHealthy() ? Result.healthy() : Result.unhealthy("not feeling well");
    }
}
