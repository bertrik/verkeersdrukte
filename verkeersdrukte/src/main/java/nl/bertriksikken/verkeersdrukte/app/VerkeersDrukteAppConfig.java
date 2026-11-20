package nl.bertriksikken.verkeersdrukte.app;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;

import java.util.Map;

@ConfigMapping(prefix = "verkeersdrukte")
@StaticInitSafe
public interface VerkeersDrukteAppConfig {

    @WithName("ndw")
    NdwConfig ndw();

    @WithName("traffic")
    TrafficConfig traffic();

    Map<String, String> headers();
}
