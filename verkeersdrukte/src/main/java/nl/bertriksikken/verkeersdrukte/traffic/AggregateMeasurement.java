package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * JSON serializable representation of a measurement for one location.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AggregateMeasurement {
    @JsonProperty("datetime")
    final String dateTime;
    @JsonProperty("flow")
    final Double flow; // vehicles per hour
    @JsonProperty("speed")
    final Double speed; // km per hour

    public AggregateMeasurement(String dateTime, double flow, double speed) {
        this.dateTime = dateTime;
        this.flow = Double.isFinite(flow) ? flow : null;
        this.speed = Double.isFinite(speed) ? speed : null;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "{flow=%.1f,speed=%.1f}", flow, speed);
    }
}

