package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.shapefile.EShapeType;
import nl.bertriksikken.shapefile.ShapeFile;
import nl.bertriksikken.shapefile.ShapeRecord;
import nl.bertriksikken.verkeersdrukte.ndw.INdwApi;
import nl.bertriksikken.verkeersdrukte.ndw.NdwDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ShapeFileDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(ShapeFileDownloader.class);

    private final NdwDownloader downloader;
    private final File folder;

    private ShapeFile shapeFile;

    ShapeFileDownloader(File folder, NdwDownloader downloader) {
        this.downloader = Objects.requireNonNull(downloader);
        this.folder = Objects.requireNonNull(folder);
    }

    /**
     * Reads the shape file from local storage.
     *
     * @return if file was read successfully.
     */
    public boolean loadCache() {
        LOG.info("Loading shapefile from ({})...", folder.getAbsolutePath());
        try (FileInputStream shpStream = new FileInputStream(new File(folder, "Telpunten_WGS84.shp"))) {
            try (FileInputStream dbfStream = new FileInputStream(new File(folder, "Telpunten_WGS84.dbf"))) {
                shapeFile = ShapeFile.read(shpStream, dbfStream);
                return true;
            }
        } catch (IOException e) {
            LOG.warn("Loading shapefile failed");
        }
        return false;
    }

    /**
     * Downloads the shape file from remote source and stores it locally.
     */
    public boolean download() throws IOException {
        File file = downloader.fetchFile(INdwApi.TRAFFIC_SPEED_SHAPEFILE);

        // unzip
        folder.mkdirs();
        deleteFiles(folder);
        unzip(file, folder);

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

    private void unzip(File zipFile, File folder) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
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

    public FeatureCollection getGeoJson() {
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
