package nl.bertriksikken.verkeersdrukte;

import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.ConfigProvider;

public final class TestConfig {

    private TestConfig() {
    }

    public static <T> T mapping(Class<T> type) {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getConfigMapping(type);
    }
}
