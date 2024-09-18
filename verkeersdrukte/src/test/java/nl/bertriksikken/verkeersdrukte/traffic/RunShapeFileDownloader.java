package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class RunShapeFileDownloader {

    public static void main(String[] args) throws IOException {
        NdwConfig ndwConfig = new NdwConfig();
        NdwClient ndwClient = NdwClient.create(ndwConfig);
        File folder = new File("shapefile");
        folder.mkdir();
        ShapeFileDownloader downloader = new ShapeFileDownloader(folder, ndwClient);
        if (downloader.download()) {
            FeatureCollection featureCollection = downloader.getFeatureCollection();
            try (FileOutputStream fos = new FileOutputStream("shapefile.json")) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(fos, featureCollection);
            }
        }
    }

}
