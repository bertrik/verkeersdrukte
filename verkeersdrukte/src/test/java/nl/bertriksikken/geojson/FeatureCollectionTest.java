package nl.bertriksikken.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

public final class FeatureCollectionTest {

    @Test
    @Disabled
    public void testDeserialize() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/FeatureCollection.json")) {
            FeatureCollection fc = mapper.readValue(is, FeatureCollection.class);
            assertFalse(fc.getFeatures().isEmpty());
        }
    }

}
