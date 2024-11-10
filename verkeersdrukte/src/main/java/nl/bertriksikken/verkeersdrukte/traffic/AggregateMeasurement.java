package nl.bertriksikken.verkeersdrukte.traffic;

import java.time.Instant;

/**
 * Internal simple representation of a measurement.
 */
public record AggregateMeasurement(Instant dateTime, double flow, double speed) { }

