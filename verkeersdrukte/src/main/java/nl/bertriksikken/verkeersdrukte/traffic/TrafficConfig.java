package nl.bertriksikken.verkeersdrukte.traffic;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.io.File;
import java.time.Duration;
import java.time.ZoneId;

/** Traffic settings nested in the application configuration mapping. */
public interface TrafficConfig {

    @WithName("time-zone")
    @WithDefault("Europe/Amsterdam")
    String timeZone();

    @WithName("base-url")
    @WithDefault("http://stofradar.nl:9002")
    String baseUrl();

    @WithName("expiry-duration-minutes")
    @WithDefault("60")
    int expiryDurationMinutes();

    @WithName("shape-file-folder")
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
