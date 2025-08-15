package nl.bertriksikken.verkeersdrukte.ndw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Downloads the MST from NDW (10 MB!)
 */
public final class RunMstDownload {

    public static void main(String[] args) throws IOException {
        File destination = new File("verkeersdrukte/src/test/resources", INdwApi.MEASUREMENT_SITE_TABLE);
        System.out.println("path = " + destination.getAbsolutePath());

        NdwConfig ndwConfig = new NdwConfig();
        try (NdwClient ndwClient = NdwClient.create(ndwConfig)) {
            FileResponse response = ndwClient.getMeasurementSiteTable("");
            try (FileOutputStream outputStream = new FileOutputStream(destination)) {
                outputStream.write(response.getContents());
            }
        }
    }

}
