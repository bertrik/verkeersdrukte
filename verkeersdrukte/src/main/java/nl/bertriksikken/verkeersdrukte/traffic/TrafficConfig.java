package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.time.Duration;
import java.time.ZoneId;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public final class TrafficConfig {

    @JsonProperty("timeZone")
    private String timeZone = "Europe/Amsterdam";
    @JsonProperty("baseUrl")
    private String baseUrl = "http://stofradar.nl:9002";
    @JsonProperty("expiryDurationMinutes")
    private int expiryDurationMinutes = 1440;
    @JsonProperty("shapeFileFolder")
    private String shapeFileFolder = ".shapefile";

    public ZoneId getTimeZone() {
        return ZoneId.of(timeZone);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getExpiryDuration() {
        return Duration.ofMinutes(expiryDurationMinutes);
    }

    public File getShapeFileFolder() {
        return new File(shapeFileFolder);
    }
}
