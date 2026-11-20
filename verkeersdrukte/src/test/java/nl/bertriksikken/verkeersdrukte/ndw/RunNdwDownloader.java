package nl.bertriksikken.verkeersdrukte.ndw;

import nl.bertriksikken.verkeersdrukte.TestConfig;
import nl.bertriksikken.verkeersdrukte.app.VerkeersDrukteAppConfig;

import java.io.File;

public final class RunNdwDownloader {

    public static void main(String[] args) {
        NdwConfig config = TestConfig.mapping(VerkeersDrukteAppConfig.class).ndw();
        try (NdwDownloader downloader = new NdwDownloader(config)) {
            downloader.start();
            File file = downloader.fetchFile(INdwApi.TRAFFIC_SPEED_SHAPEFILE);
            System.out.println("File = " + file);
        }
    }
}
