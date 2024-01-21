package nl.bertriksikken.verkeersdrukte.ndw;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public final class NdwConfig {

    @JsonProperty("host")
    String host = "https://opendata.ndw.nu";

    @JsonProperty("timeout")
    int timeout = 30;

    public String getUrl() {
        return host;
    }

    public Duration getTimeout() {
        return Duration.ofSeconds(timeout);
    }
}
