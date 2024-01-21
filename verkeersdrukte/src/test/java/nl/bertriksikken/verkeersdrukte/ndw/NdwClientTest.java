package nl.bertriksikken.verkeersdrukte.ndw;

import nl.bertriksikken.verkeersdrukte.ndw.FileResponse;
import nl.bertriksikken.verkeersdrukte.ndw.INdwApi;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Downloads the traffic speed file and saves it to disk.
 */
public final class NdwClientTest {

    /**
     * Fetches the traffic speed file and stores it to a file
     */
    public static void main(String[] args) throws IOException {
        NdwConfig config = new NdwConfig();
        NdwClient client = NdwClient.create(config);
        FileResponse response = client.getTrafficSpeed();
        File file = new File(INdwApi.TRAFFIC_SPEED_XML_GZ);
        Files.write(file.toPath(), response.getContents());
    }
}
