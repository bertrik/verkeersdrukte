package nl.bertriksikken.verkeersdrukte.ndw;

import nl.bertriksikken.verkeersdrukte.TestConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Downloads the MST from NDW (10 MB!)
 */
public final class RunMstDownload {

    public static void main(String[] args) throws IOException {
        File destination = new File("verkeersdrukte/src/test/resources", INdwApi.MEASUREMENT_SITE_TABLE);
        System.out.println("path = " + destination.getAbsolutePath());

        NdwConfig ndwConfig = TestConfig.mapping(NdwConfig.class);
        try (NdwClient ndwClient = NdwClient.create(ndwConfig)) {
            ndwClient.getFile(INdwApi.MEASUREMENT_SITE_TABLE, Map.of(), destination);
        }
    }

}
