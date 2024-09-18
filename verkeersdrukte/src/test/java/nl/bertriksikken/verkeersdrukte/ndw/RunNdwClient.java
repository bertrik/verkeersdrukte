package nl.bertriksikken.verkeersdrukte.ndw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Downloads the traffic speed file and saves it to disk.
 */
public final class RunNdwClient {

    private static final Logger LOG = LoggerFactory.getLogger(RunNdwClient.class);

    /**
     * Fetches the traffic speed file and stores it to a file
     */
    public static void main(String[] args) throws IOException {
        NdwConfig config = new NdwConfig();
        NdwClient client = NdwClient.create(config);
        FileResponse response = client.getShapeFile("");
        byte[] contents = response.getContents();
        String etag = response.getEtag();
        LOG.info("Got file, {} bytes, etag: {}", contents.length, etag);
        File file = new File(INdwApi.TRAFFIC_SPEED_SHAPEFILE);
        Files.write(file.toPath(), contents);

        // get again, expect HTTP code xxx
        FileResponse nextResponse = client.getShapeFile(etag);
        LOG.info("Next response: {}", nextResponse);
    }
}
