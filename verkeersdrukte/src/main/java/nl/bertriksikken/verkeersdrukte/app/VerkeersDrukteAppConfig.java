package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class VerkeersDrukteAppConfig extends Configuration {

    @JsonProperty("ndw")
    private NdwConfig ndwConfig = new NdwConfig();
    @JsonProperty("traffic")
    private TrafficConfig trafficConfig = new TrafficConfig();
    @JsonProperty("headers")
    private Map<String, String> headers = Map.of();

    public NdwConfig getNdwConfig() {
        return ndwConfig;
    }

    public TrafficConfig getTrafficConfig() {
        return trafficConfig;
    }

    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }
}
