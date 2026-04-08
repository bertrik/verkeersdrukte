package nl.bertriksikken.datex2v3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class VmsPayloadTest {

    @Test
    void testParse() throws IOException {
        VmsPayload publication = new VmsPayload();
        try (InputStream is = getClass().getResourceAsStream("/dynamische_route_informatie_paneel.xml.gz")) {
            try (GZIPInputStream gzis = new GZIPInputStream(is)) {
                publication.parse(gzis);
            }
        }
        assertFalse(publication.getStatuses().isEmpty());
    }
}
