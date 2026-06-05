package nl.bertriksikken.verkeersdrukte.ndw;

import java.io.File;

public final class RunNdwDownloader {

    public static void main(String[] args) {
        NdwConfig config = new NdwConfig();
        try (NdwDownloader downloader = new NdwDownloader(config)) {
            downloader.start();
            File file = downloader.getTrafficFile(INdwApi.MEASUREMENT_SITE_TABLE);
            System.out.println("File = " + file);
        }
    }
}
