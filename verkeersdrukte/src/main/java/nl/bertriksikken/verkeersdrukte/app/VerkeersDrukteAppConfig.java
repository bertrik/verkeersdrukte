package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
final class VerkeersDrukteAppConfig extends Configuration {

    @JsonProperty("ndw")
    private final NdwConfig ndwConfig = new NdwConfig();
    @JsonProperty("traffic")
    private TrafficConfig trafficConfig;

    public NdwConfig getNdwConfig() {
        return ndwConfig;
    }

    public TrafficConfig getTrafficConfig() {
        return trafficConfig;
    }

}
