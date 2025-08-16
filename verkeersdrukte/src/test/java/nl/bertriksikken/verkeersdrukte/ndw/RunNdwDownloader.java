package nl.bertriksikken.verkeersdrukte.ndw;

import java.io.File;

public final class RunNdwDownloader {

    public static void main(String[] args) throws Exception {
        NdwConfig config = new NdwConfig();
        try (NdwDownloader downloader = new NdwDownloader(config)) {
            downloader.start();
            File file = downloader.fetchFile(INdwApi.TRAFFIC_SPEED_SHAPEFILE);
            System.out.println("File = " + file);
        }
    }
}
