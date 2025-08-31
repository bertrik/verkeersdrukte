package nl.bertriksikken.datex2;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

public final class VmsPublicationTest {

    @Test
    public void testParse() throws IOException {
        VmsPublication publication = new VmsPublication(new XmlMapper());
        InputStream is = getClass().getResourceAsStream("/DRIPS.xml.gz");
        try (GZIPInputStream gzis = new GZIPInputStream(is)) {
            publication.parse(gzis);
        }
        assertFalse(publication.getRecords().isEmpty());
    }
}
