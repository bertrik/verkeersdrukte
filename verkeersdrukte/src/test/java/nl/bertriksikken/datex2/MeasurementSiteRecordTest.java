package nl.bertriksikken.datex2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MeasurementSiteRecordTest {

    private final ObjectMapper XML_MAPPER = MeasurementSiteTable.createXmlMapper();

    @Test
    public void testDeserialize() throws IOException {
        InputStream is = getClass().getResourceAsStream("/MeasurementSiteRecord.xml");
        MeasurementSiteRecord record = XML_MAPPER.readValue(is, MeasurementSiteRecord.class);
        assertNotNull(record);
        MeasurementSpecificCharacteristics characteristic = record.findCharacteristic(4);
        assertEquals("lane1", characteristic.element().specificLane());
        assertEquals("trafficFlow", characteristic.element().specificMeasurementValueType());
        assertEquals("anyVehicle", characteristic.element().specificVehicleCharacteristics().vehicleType());
    }

    @Test
    public void testSerialize() throws IOException {
        MeasurementSiteRecord record = new MeasurementSiteRecord("MSR");
        SpecificVehicleCharacteristics anyVehicle = new SpecificVehicleCharacteristics("anyVehicle");
        MeasurementSpecificCharacteristicsElement characteristics = new MeasurementSpecificCharacteristicsElement("lane", "trafficFlow", anyVehicle);
        record.addCharacteristic(characteristics);
        XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(System.out, record);
    }

}
