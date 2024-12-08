package nl.bertriksikken.shapefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import nl.bertriksikken.geojson.FeatureCollection;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class ShapeFileReaderTest {

    @Test
    public void testWriteGeoJson() throws IOException {
        InputStream shpStream = getClass().getClassLoader().getResourceAsStream("shapefile/Telpunten_WGS84.shp");
        InputStream dbfStream = getClass().getClassLoader().getResourceAsStream("shapefile/Telpunten_WGS84.dbf");
        ShapeFile shapeFile = ShapeFile.read(shpStream, dbfStream);

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
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File("shape.geojson"), collection);
    }

}
