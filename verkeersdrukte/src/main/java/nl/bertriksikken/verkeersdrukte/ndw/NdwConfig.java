package nl.bertriksikken.verkeersdrukte.ndw;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public final class NdwConfig {

    @JsonProperty("host")
    String trafficUrl = "https://opendata.ndw.nu";

    @JsonProperty("mapsUrl")
    String mapsUrl = "https://maps.ndw.nu";

    @JsonProperty("timeout")
    int timeout = 30;

    @JsonProperty("cacheLocation")
    String cacheLocation = ".cache";

    public String getTrafficUrl() {
        return trafficUrl;
    }

    public String getMapsUrl() {
        return mapsUrl;
    }

    public Duration getTimeout() {
        return Duration.ofSeconds(timeout);
    }

    public String getCacheLocation() {
        return cacheLocation;
    }

    void setCacheLocation(String cacheLocation) {
        this.cacheLocation = cacheLocation;
    }

}
