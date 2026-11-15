package nl.bertriksikken.verkeersdrukte.traffic;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.quarkus.runtime.annotations.StaticInitSafe;

import java.io.File;
import java.time.Duration;
import java.time.ZoneId;

@ConfigMapping(prefix = "verkeersdrukte.traffic")
@StaticInitSafe
public interface TrafficConfig {

    @WithDefault("Europe/Amsterdam")
    String timeZone();

    @WithDefault("http://stofradar.nl:9002")
    String baseUrl();

    @WithDefault("60")
    int expiryDurationMinutes();

    @WithDefault(".shapefile")
    String shapeFileFolder();

    default ZoneId getTimeZone() {
        return ZoneId.of(timeZone());
    }

    default String getBaseUrl() {
        return baseUrl();
    }

    default Duration getExpiryDuration() {
        return Duration.ofMinutes(expiryDurationMinutes());
    }

    default File getShapeFileFolder() {
        return new File(shapeFileFolder());
    }
}
