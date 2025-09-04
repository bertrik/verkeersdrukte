package nl.bertriksikken.datex2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class MeasurementSiteTableTest {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementSiteTableTest.class);

    @Test
    public void testDeserialize() throws IOException {
        InputStream is = getClass().getResourceAsStream("/MeasurementSiteTable.xml");
        MeasurementSiteTable mst = new MeasurementSiteTable();
        mst.parse(is, Set.of());
        Assertions.assertFalse(mst.getMeasurementSiteIds().isEmpty());
        LOG.info("Got {} elements", mst.getMeasurementSiteIds().size());

        MeasurementSiteRecord record = mst.findMeasurementSiteRecord("PZH01_MST_0629_00");
        assertNotNull(record);
    }

}
