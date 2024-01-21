package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
final class VerkeersDrukteAppConfig extends Configuration {

    @JsonProperty("ndw")
    public final NdwConfig ndwConfig = new NdwConfig();

    public NdwConfig getNdwConfig() {
        return ndwConfig;
    }
}
