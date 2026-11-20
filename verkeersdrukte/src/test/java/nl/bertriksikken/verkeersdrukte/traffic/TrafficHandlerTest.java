package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.verkeersdrukte.TestConfig;
import nl.bertriksikken.verkeersdrukte.app.VerkeersDrukteAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TrafficHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficHandlerTest.class);

    public static void main(String[] args) {
        VerkeersDrukteAppConfig config = TestConfig.mapping(VerkeersDrukteAppConfig.class);
        TrafficHandler trafficHandler = new TrafficHandler(config);
        trafficHandler.start();
        trafficHandler.subscribe("client", TrafficHandlerTest::notifyData);
    }

    private static void notifyData() {
        LOG.info("notifyData");
    }
}
