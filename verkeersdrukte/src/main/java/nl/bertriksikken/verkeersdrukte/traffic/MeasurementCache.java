package nl.bertriksikken.verkeersdrukte.traffic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;

public final class MeasurementCache {

    private final Cache<String, AggregateMeasurement> cache;

    MeasurementCache(Duration expiryDuration) {
        cache = CacheBuilder.newBuilder().expireAfterWrite(expiryDuration).build();
    }

    public void put(String location, AggregateMeasurement measurement) {
        cache.put(location, measurement);
    }

    public AggregateMeasurement get(String location) {
        return cache.getIfPresent(location);
    }

}
