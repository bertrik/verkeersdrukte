package nl.bertriksikken.datex2;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import nl.bertriksikken.datex2.VmsUnit.ImageData;
import nl.bertriksikken.datex2.VmsUnit.Vms;
import nl.bertriksikken.datex2.VmsUnit.VmsImage;
import nl.bertriksikken.datex2.VmsUnit.VmsMessage;
import nl.bertriksikken.datex2.VmsUnit.VmsMessageExtension;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class VmsUnitTest {

    @Test
    public void testSerialize() throws IOException {
        VmsUnit vmsUnit = new VmsUnit();
        Vms vms = new Vms(true);

        ImageData imageData = ImageData.png(new byte[] {1, 2, 3});
        VmsImage image = new VmsImage(imageData);
        VmsMessageExtension extension = new VmsMessageExtension(image);

        VmsMessage vmsMessage = new VmsMessage("today", extension);
        vms.addVmsMessage(vmsMessage);
        vmsUnit.addVms(vms);

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.writerWithDefaultPrettyPrinter().writeValue(System.out, vmsUnit);
    }

    @Test
    public void testParse() throws IOException {
        InputStream is = getClass().getResourceAsStream("/VmsUnit.xml");
        XmlMapper xmlMapper = new XmlMapper();
        VmsUnit vmsUnit = xmlMapper.readValue(is, VmsUnit.class);
        assertNotNull(vmsUnit);
    }
}
