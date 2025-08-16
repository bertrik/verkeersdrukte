package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import nl.bertriksikken.verkeersdrukte.ndw.NdwDownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class RunShapeFileDownloader {

    public static void main(String[] args) throws IOException {
        NdwConfig ndwConfig = new NdwConfig();
        try (NdwDownloader ndwDownloader = new NdwDownloader(ndwConfig)) {
            ndwDownloader.start();
            File folder = new File(".shapefile");
            folder.mkdir();
            ShapeFileDownloader downloader = new ShapeFileDownloader(folder, ndwDownloader);
            if (downloader.download()) {
                FeatureCollection featureCollection = downloader.getGeoJson();
                try (FileOutputStream fos = new FileOutputStream("shapefile.json")) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writerWithDefaultPrettyPrinter().writeValue(fos, featureCollection);
                }
            }
        }
    }

}
