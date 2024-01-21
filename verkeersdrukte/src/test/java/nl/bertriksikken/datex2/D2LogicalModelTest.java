package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.zip.GZIPInputStream;

public final class D2LogicalModelTest {

    private static final Logger LOG = LoggerFactory.getLogger(D2LogicalModelTest.class);

    /**
     * Unpacks the gzipped traffic speed file and parses it.
     */
    @Test
    public void testDecode() throws IOException {
        ObjectMapper mapper = new XmlMapper();
        mapper.findAndRegisterModules();
        InputStream is = getClass().getResourceAsStream("/trafficspeed.xml.gz");
        try (GZIPInputStream gzis = new GZIPInputStream(is)) {
            LOG.info("Parsing");
            JsonNode node = mapper.readValue(gzis, JsonNode.class);
            JsonNode d2LogicalModel = node.at("/Body/d2LogicalModel");
            D2LogicalModel model = mapper.treeToValue(d2LogicalModel, D2LogicalModel.class);

            D2LogicalModel.MeasuredDataPublication measuredDataPublication = (D2LogicalModel.MeasuredDataPublication) model.payloadPublication;
            int numMeasurements = 0;
            int numSites = 0;
            for (SiteMeasurements measurements : measuredDataPublication.siteMeasurementsList) {
                for (MeasuredValue value : measurements.measuredValueList) {
                    numMeasurements++;
                }
                numSites++;
            }
            LOG.info("Parsed {} measurements from {} sites", numMeasurements, numSites);
        }
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        D2LogicalModel.MeasuredDataPublication measuredDataPublication = new D2LogicalModel.MeasuredDataPublication(Instant.now());
        D2LogicalModel model = new D2LogicalModel(measuredDataPublication);

        SiteMeasurements.Reference reference = new SiteMeasurements.Reference("id", "version");
        SiteMeasurements siteMeasurements = new SiteMeasurements(reference, Instant.now());
        siteMeasurements.addMeasuredValue(new MeasuredValue.TrafficFlow(5, 1));
        siteMeasurements.addMeasuredValue(new MeasuredValue.TrafficSpeed(80, 1));
        measuredDataPublication.addSiteMeasurements(siteMeasurements);

        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
        System.out.println(xml);
    }
}
