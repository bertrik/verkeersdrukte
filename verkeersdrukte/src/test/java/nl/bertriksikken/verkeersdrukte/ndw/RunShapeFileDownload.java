package nl.bertriksikken.verkeersdrukte.ndw;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class RunShapeFileDownload {

    public static void main(String[] args) throws IOException {
        File destination = new File("verkeersdrukte/src/test/resources", INdwApi.TRAFFIC_SPEED_SHAPEFILE);
        System.out.println("path = " + destination.getAbsolutePath());

        NdwConfig ndwConfig = new NdwConfig();
        try (NdwClient ndwClient = NdwClient.create(ndwConfig)) {
            ndwClient.getFile(INdwApi.TRAFFIC_SPEED_SHAPEFILE, Map.of(), destination);
        }
    }
}
