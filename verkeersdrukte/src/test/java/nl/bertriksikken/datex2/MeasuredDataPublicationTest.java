package nl.bertriksikken.datex2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class MeasuredDataPublicationTest {

    private static final Logger LOG = LoggerFactory.getLogger(MeasuredDataPublicationTest.class);

    @Test
    public void test() throws IOException {
        InputStream is = getClass().getResourceAsStream("/trafficspeed.xml.gz");
        MeasuredDataPublication measuredDataPublication;
        try (GZIPInputStream gzis = new GZIPInputStream(is)) {
            LOG.info("Start parsing...");
            measuredDataPublication = MeasuredDataPublication.parse(gzis);
        }
        Assert.assertFalse(measuredDataPublication.getSiteMeasurementsList().isEmpty());
        LOG.info("Got {} elements", measuredDataPublication.getSiteMeasurementsList().size());
    }

}
