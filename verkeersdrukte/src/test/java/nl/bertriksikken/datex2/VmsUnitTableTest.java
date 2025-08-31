package nl.bertriksikken.datex2;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class VmsUnitTableTest {

    private static final Logger LOG = LoggerFactory.getLogger(VmsUnitTableTest.class);

    @Test
    public void testDeserialize() throws IOException {
        VmsTablePublication table = new VmsTablePublication();
        InputStream is = getClass().getResourceAsStream("/LocatietabelDRIPS.xml.gz");
        try (GZIPInputStream gzis = new GZIPInputStream(is)) {
            table.parse(gzis);
        }
        List<VmsUnitRecord> records = table.getRecords();
        LOG.info("Got {} records", records.size());
        assertFalse(records.isEmpty());
        VmsUnitRecord first = records.getFirst();
        assertNotNull(first);
    }

}
