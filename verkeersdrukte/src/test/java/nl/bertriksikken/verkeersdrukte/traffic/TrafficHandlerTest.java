package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TrafficHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficHandlerTest.class);

    public static void main(String[] args) throws InterruptedException {
        NdwConfig config = new NdwConfig();
        TrafficHandler trafficHandler = new TrafficHandler(config);
        trafficHandler.start();
        try {
            trafficHandler.subscribe("client", () -> notifyData());
        } finally {
            Thread.sleep(120_000L);
            trafficHandler.stop();
        }
    }

    private static void notifyData() {
        LOG.info("notifyData");
    }
}
