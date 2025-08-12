package nl.bertriksikken.verkeersdrukte.traffic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;

public final class MeasurementCache {

    private final Cache<String, SiteMeasurement> cache;

    MeasurementCache(Duration expiryDuration) {
        cache = CacheBuilder.newBuilder().expireAfterWrite(expiryDuration).build();
    }

    public void put(String location, SiteMeasurement measurement) {
        cache.put(location, measurement);
    }

    public SiteMeasurement get(String location) {
        return cache.getIfPresent(location);
    }

}
