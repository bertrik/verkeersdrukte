package nl.bertriksikken.datex2;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import nl.bertriksikken.datex2.VmsUnitRecord.VmsRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VmsUnitRecordTest {

    @Test
    public void testParseRecord() throws IOException {
        InputStream is = getClass().getResourceAsStream("/VmsUnitRecord.xml");
        XmlMapper mapper = VmsTablePublication.createXmlMapper();
        VmsUnitRecord record = mapper.readValue(is, VmsUnitRecord.class);
        assertNotNull(record);
    }

    @Test
    public void testSerialize() throws IOException {
        XmlMapper mapper = VmsTablePublication.createXmlMapper();
        VmsUnitRecord unitRecord = new VmsUnitRecord("id", "version");
        unitRecord.addVmsRecord(new VmsRecord(1.23, 4.56));
        mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, unitRecord);

    }
}
