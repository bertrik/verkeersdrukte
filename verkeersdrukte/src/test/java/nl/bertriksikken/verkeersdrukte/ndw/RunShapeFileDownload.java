package nl.bertriksikken.verkeersdrukte.ndw;

import java.io.IOException;

public final class RunShapeFileDownload {

    public static void main(String[] args) throws IOException {
        NdwConfig ndwConfig = new NdwConfig();
        ndwConfig.setCacheLocation("verkeersdrukte/src/test/resources");
        try (NdwDownloader downloader = new NdwDownloader(ndwConfig)) {
            NdwShapeFile ndwShapeFile = new NdwShapeFile();
            ndwShapeFile.download(downloader);
        }
    }
}
