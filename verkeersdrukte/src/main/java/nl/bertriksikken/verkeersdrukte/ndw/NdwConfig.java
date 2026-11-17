package nl.bertriksikken.verkeersdrukte.ndw;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.time.Duration;

/**
 * NDW settings nested in the application configuration mapping.
 */
public interface NdwConfig {

    @WithName("host")
    @WithDefault("https://opendata.ndw.nu")
    String host();

    @WithName("timeout")
    @WithDefault("30")
    int timeout();

    @WithName("cache-location")
    @WithDefault(".cache")
    String cacheLocation();

    default String getUrl() {
        return host();
    }

    default Duration getTimeout() {
        return Duration.ofSeconds(timeout());
    }

    default String getCacheLocation() {
        return cacheLocation();
    }
}
