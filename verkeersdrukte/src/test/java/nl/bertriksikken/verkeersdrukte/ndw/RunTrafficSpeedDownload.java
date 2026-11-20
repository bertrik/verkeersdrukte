package nl.bertriksikken.verkeersdrukte.ndw;

import nl.bertriksikken.verkeersdrukte.TestConfig;
import nl.bertriksikken.verkeersdrukte.app.VerkeersDrukteAppConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class RunTrafficSpeedDownload {

    public static void main(String[] args) throws IOException {
        File destination = new File("verkeersdrukte/src/test/resources", INdwApi.TRAFFIC_SPEED_XML_GZ);
        System.out.println("path = " + destination.getAbsolutePath());

        NdwConfig ndwConfig = TestConfig.mapping(VerkeersDrukteAppConfig.class).ndw();
        try (NdwClient ndwClient = NdwClient.create(ndwConfig)) {
            ndwClient.getFile(INdwApi.TRAFFIC_SPEED_XML_GZ, Map.of(), destination);
        }
    }
}
