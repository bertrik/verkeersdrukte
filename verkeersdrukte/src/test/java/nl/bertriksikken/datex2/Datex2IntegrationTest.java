package nl.bertriksikken.datex2;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import nl.bertriksikken.verkeersdrukte.ndw.NdwShapeFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * Performs a kind of integration test, between the shapefile, MST and measurement file.
 * <p>
 * Requires an actual MST in the src/resource directory
 */
public final class Datex2IntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(Datex2IntegrationTest.class);

    public static void main(String[] args) throws IOException {
        Datex2IntegrationTest test = new Datex2IntegrationTest();
        test.test();
    }

    private void test() throws IOException {
        // load shapefile
        LOG.info("Loading shapefile ...");
        NdwShapeFile shapeFile = new NdwShapeFile();
        InputStream geoStream = getClass().getResourceAsStream("/pointRecord.geojson.gz");
        try (GZIPInputStream gzis = new GZIPInputStream(geoStream)) {
            shapeFile.parse(gzis);
        }

        // initialise MST
        LOG.info("Loading MST ...");
        InputStream mstStream = getClass().getResourceAsStream("/measurement_current.xml.gz");
        MeasurementSiteTable mst = new MeasurementSiteTable();
        try (GZIPInputStream gzis = new GZIPInputStream(mstStream)) {
            mst.parse(gzis, shapeFile.getSiteIds());
        }
        LOG.info("MST has {} records", mst.getMeasurementSiteIds().size());

        // load measurements
        LOG.info("Loading measurements ...");
        InputStream measurementStream = getClass().getResourceAsStream("/trafficspeed.xml.gz");
        MeasuredDataPublication mdp = new MeasuredDataPublication(new XmlMapper());
        try (GZIPInputStream gzis = new GZIPInputStream(measurementStream)) {
            mdp.parse(gzis);
        }
        int mstNotFound = 0;
        int indexNotFound = 0;
        int anyNotFound = 0;
        for (SiteMeasurements siteMeasurements : mdp.getSiteMeasurementsList()) {
            String referenceId = siteMeasurements.reference.id;
            MeasurementSiteRecord msr = mst.findMeasurementSiteRecord(referenceId);
            if (msr == null) {
                LOG.warn("Could not find site '{}' in MST!", referenceId);
                mstNotFound++;
            } else {
                boolean hasAnyVehicle = false;
                for (MeasuredValue measuredValue : siteMeasurements.measuredValueList) {
                    int index = measuredValue.index;
                    MeasurementSiteRecord.MeasurementSpecificCharacteristicsElement chars = msr.findCharacteristic(index);
                    if (chars == null) {
                        LOG.warn("Could not find index '{}' for site '{}' in MST", index, referenceId);
                        indexNotFound++;
                    } else {
                        if (Objects.equals(chars.specificVehicleCharacteristics().vehicleType(), "anyVehicle")) {
                            hasAnyVehicle = true;
                        }
                    }
                }
                if (!hasAnyVehicle) {
                    LOG.info("The AnyVehicle index was not found for site '{}'", referenceId);
                    anyNotFound++;
                }
            }
        }
        LOG.info("Mismatches: site not found: {}, index not found: {}, any not found: {}",
                mstNotFound, indexNotFound, anyNotFound);
    }
}
