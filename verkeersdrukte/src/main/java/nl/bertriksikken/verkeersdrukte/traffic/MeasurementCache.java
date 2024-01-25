package nl.bertriksikken.verkeersdrukte.traffic;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MeasurementCache {

    private final Instant publishedDateTime;
    private Map<String, AggregateMeasurement> measurementMap = new ConcurrentHashMap<>();

    MeasurementCache(Instant publishedDateTime) {
        this.publishedDateTime = publishedDateTime;
    }

    public void put(String location, AggregateMeasurement measurement) {
        measurementMap.put(location, measurement);
    }

    public AggregateMeasurement get(String location) {
        return measurementMap.get(location);
    }

}
