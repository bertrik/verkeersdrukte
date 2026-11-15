package nl.bertriksikken.verkeersdrukte.app;

import jakarta.enterprise.context.ApplicationScoped;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficHandler;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class VerkeersDrukteHealthCheck implements HealthCheck {
    private final TrafficHandler handler;

    public VerkeersDrukteHealthCheck(TrafficHandler handler) {
        this.handler = handler;
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("ndw").status(handler.isHealthy()).build();
    }
}
