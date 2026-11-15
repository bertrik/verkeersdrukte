package nl.bertriksikken.verkeersdrukte.ndw;

import nl.bertriksikken.verkeersdrukte.TestConfig;

import java.io.File;

public final class RunNdwDownloader {

    public static void main(String[] args) {
        NdwConfig config = TestConfig.mapping(NdwConfig.class);
        try (NdwDownloader downloader = new NdwDownloader(config)) {
            downloader.start();
            File file = downloader.fetchFile(INdwApi.TRAFFIC_SPEED_SHAPEFILE);
            System.out.println("File = " + file);
        }
    }
}
