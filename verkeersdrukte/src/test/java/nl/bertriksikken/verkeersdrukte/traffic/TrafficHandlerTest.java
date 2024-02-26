package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.verkeersdrukte.app.VerkeersDrukteAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class TrafficHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficHandlerTest.class);

    public static void main(String[] args) throws IOException {
        VerkeersDrukteAppConfig config = new VerkeersDrukteAppConfig();
        TrafficHandler trafficHandler = new TrafficHandler(config);
        trafficHandler.start();
        trafficHandler.subscribe("client", TrafficHandlerTest::notifyData);
    }

    private static void notifyData() {
        LOG.info("notifyData");
    }
}
