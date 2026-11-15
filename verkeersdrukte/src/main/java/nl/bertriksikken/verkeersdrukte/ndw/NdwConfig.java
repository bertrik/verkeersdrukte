package nl.bertriksikken.verkeersdrukte.ndw;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

@ConfigMapping(prefix = "verkeersdrukte.ndw")
@StaticInitSafe
public interface NdwConfig {

    @WithDefault("https://opendata.ndw.nu")
    String host();

    @WithDefault("30")
    int timeout();

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
