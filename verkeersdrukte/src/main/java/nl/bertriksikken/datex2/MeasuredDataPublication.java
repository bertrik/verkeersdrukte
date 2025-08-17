package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(MeasuredDataPublication.class);
    private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    private final List<SiteMeasurements> siteMeasurementsList = new ArrayList<>();
    private final ObjectReader measurementsReader;

    public MeasuredDataPublication(XmlMapper xmlMapper) {
        measurementsReader = xmlMapper.readerFor(SiteMeasurements.class);
    }

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
            SiteMeasurements siteMeasurements = measurementsReader.readValue(xml);
            siteMeasurementsList.add(siteMeasurements);
        } catch (JsonProcessingException e) {
            LOG.warn("JSON parsing exception: {}", e.getMessage());
        }
    }

    public List<SiteMeasurements> getSiteMeasurementsList() {
        return List.copyOf(siteMeasurementsList);
    }

}
