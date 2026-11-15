package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class VerkeersDrukteAppConfig {

    @JsonProperty("ndw")
    private NdwConfig ndwConfig;
    @JsonProperty("traffic")
    private TrafficConfig trafficConfig;
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

    public void setNdwConfig(NdwConfig value) { ndwConfig = value; }
    public void setTrafficConfig(TrafficConfig value) { trafficConfig = value; }
    public void setHeaders(Map<String, String> value) { headers = Map.copyOf(value); }
}
