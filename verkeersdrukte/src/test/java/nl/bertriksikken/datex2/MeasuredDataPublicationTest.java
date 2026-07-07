package nl.bertriksikken.datex2;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class MeasuredDataPublicationTest {

    private static final Logger LOG = LoggerFactory.getLogger(MeasuredDataPublicationTest.class);

    private long numEntries;

    @Test
    public void test() throws IOException {
        InputStream is = getClass().getResourceAsStream("/trafficspeed.xml.gz");
        MeasuredDataPublication measuredDataPublication = new MeasuredDataPublication(new XmlMapper());
        try (GZIPInputStream gzis = new GZIPInputStream(is)) {
            LOG.info("Start parsing...");
            numEntries = 0;
            measuredDataPublication.parse(gzis, record -> numEntries++);
        }
        Assertions.assertTrue(numEntries > 0);
        LOG.info("Got {} elements", numEntries);
    }

}
