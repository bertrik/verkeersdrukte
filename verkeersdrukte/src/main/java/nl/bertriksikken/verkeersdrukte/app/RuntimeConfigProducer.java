package nl.bertriksikken.verkeersdrukte.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the application configuration from Quarkus configuration sources. */
@ApplicationScoped
public class RuntimeConfigProducer {
    @Inject
    Config config;

    @Inject
    NdwConfig ndwConfig;

    @Inject
    TrafficConfig trafficConfig;

    @Produces
    VerkeersDrukteAppConfig applicationConfig() {
        VerkeersDrukteAppConfig config = new VerkeersDrukteAppConfig();
        config.setNdwConfig(ndwConfig);
        config.setTrafficConfig(trafficConfig);
        config.setHeaders(headers());
        return config;
    }

    private Map<String, String> headers() {
        String prefix = "verkeersdrukte.headers.";
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : config.getPropertyNames()) {
            if (name.startsWith(prefix)) {
                headers.put(name.substring(prefix.length()), config.getValue(name, String.class));
            }
        }
        return headers;
    }
}
