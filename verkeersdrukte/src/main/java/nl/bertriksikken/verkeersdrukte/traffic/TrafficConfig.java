package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneId;
import java.util.TimeZone;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public final class TrafficConfig {

    @JsonProperty("timeZone")
    private String timeZone = "Europe/Amsterdam";

    public ZoneId getTimeZone() {
        return ZoneId.of(timeZone);
    }
}