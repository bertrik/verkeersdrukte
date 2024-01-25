package nl.bertriksikken.verkeersdrukte.traffic;

import java.time.Instant;
import java.util.Locale;

/**
 * Internal simple representation of a measurement.
 */
public final class AggregateMeasurement {
    public final Instant dateTime;
    public final Double flow; // vehicles per hour
    public final Double speed; // km per hour

    public AggregateMeasurement(Instant dateTime, double flow, double speed) {
        this.dateTime = dateTime;
        this.flow = flow;
        this.speed = speed;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "{flow=%.1f,speed=%.1f}", flow, speed);
    }
}

