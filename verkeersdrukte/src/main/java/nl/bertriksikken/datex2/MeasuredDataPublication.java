package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class MeasuredDataPublication {
    private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    private final List<SiteMeasurements> siteMeasurementsList = new ArrayList<>();
    private final XmlMapper mapper = new XmlMapper();

    public void parse(InputStream inputStream) throws IOException {
        try {
            SAXParser saxParser = saxParserFactory.newSAXParser();
            DefaultHandler handler = new SaxRecordHandler("d2LogicalModel/payloadPublication/siteMeasurements", this::processRecord);
            saxParser.parse(inputStream, handler);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    private void processRecord(String xml) {
        try {
            SiteMeasurements siteMeasurements = mapper.readValue(xml, SiteMeasurements.class);
            siteMeasurementsList.add(siteMeasurements);
        } catch (JsonProcessingException e) {
            // ignore
        }
    }

    public List<SiteMeasurements> getSiteMeasurementsList() {
        return List.copyOf(siteMeasurementsList);
    }

}
