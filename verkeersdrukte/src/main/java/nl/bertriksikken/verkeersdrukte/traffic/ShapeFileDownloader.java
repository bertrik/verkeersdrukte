package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.shapefile.EShapeType;
import nl.bertriksikken.shapefile.ShapeFile;
import nl.bertriksikken.shapefile.ShapeRecord;
import nl.bertriksikken.verkeersdrukte.ndw.FileResponse;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ShapeFileDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(ShapeFileDownloader.class);

    private final File folder;
    private final NdwClient client;

    private String etag = "";
    private ShapeFile shapeFile;

    ShapeFileDownloader(File folder, NdwClient client) {
        this.folder = folder;
        this.client = client;
    }

    public boolean download() throws IOException {
        FileResponse response = client.getShapeFile(etag);
        if (response.getCode() != 200) {
            LOG.info("Shapefile not downloaded, code {}", response.getCode());
            return false;
        }

        // remember etag for next time
        etag = response.getEtag();

        // unzip
        folder.mkdirs();
        deleteFiles(folder);
        unzip(response.getContents(), folder);

        // read shape file
        try (FileInputStream shpStream = new FileInputStream(new File(folder, "Telpunten_WGS84.shp"))) {
            try (FileInputStream dbfStream = new FileInputStream(new File(folder, "Telpunten_WGS84.dbf"))) {
                shapeFile = ShapeFile.read(shpStream, dbfStream);
            }
        }
        return true;
    }

    private void deleteFiles(File folder) {
        for (File file : folder.listFiles(this::isTelpunt)) {
            LOG.info("Deleting '{}'", file.getName());
            file.delete();
        }
    }

    private void unzip(byte[] contents, File folder) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(contents);
        try (ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory() && isTelpunt(folder, name)) {
                    File file = new File(folder, name);
                    LOG.info("Unzipping {}", file.getName());
                    unzipFile(zis, file);
                }
            }
        }
    }

    private void unzipFile(ZipInputStream zis, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] data = zis.readAllBytes();
            fos.write(data);
        }
    }

    public FeatureCollection getFeatureCollection() throws IOException {
        FeatureCollection collection = new FeatureCollection();
        for (ShapeRecord record : shapeFile.getRecords()) {
            if (record.getType() == EShapeType.Point) {
                ShapeRecord.Point point = (ShapeRecord.Point) record;
                FeatureCollection.GeoJsonGeometry geometry = new FeatureCollection.PointGeometry(point.y, point.x);
                FeatureCollection.Feature feature = new FeatureCollection.Feature(geometry);
                record.getProperties().forEach(feature::addProperty);
                collection.add(feature);
            }
        }
        return collection;
    }

    private boolean isTelpunt(File dir, String name) {
        return name.startsWith("Telpunten_WGS84");
    }
}
