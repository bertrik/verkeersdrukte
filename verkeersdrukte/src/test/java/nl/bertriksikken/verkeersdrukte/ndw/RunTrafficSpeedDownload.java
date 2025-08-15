package nl.bertriksikken.verkeersdrukte.ndw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class RunTrafficSpeedDownload {

    public static void main(String[] args) throws IOException {
        File destination = new File("verkeersdrukte/src/test/resources", INdwApi.TRAFFIC_SPEED_XML_GZ);
        System.out.println("path = " + destination.getAbsolutePath());

        NdwConfig ndwConfig = new NdwConfig();
        try (NdwClient ndwClient = NdwClient.create(ndwConfig)) {
            FileResponse response = ndwClient.getTrafficSpeed();
            try (FileOutputStream outputStream = new FileOutputStream(destination)) {
                outputStream.write(response.getContents());
            }
        }
    }
}
