package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MeasurementSiteTable {
    private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private final ObjectReader msrReader;

    private final Map<String, MeasurementSiteRecord> records = new LinkedHashMap<>();
    private final Set<String> siteIds;

    static XmlMapper createXmlMapper() {
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        return new XmlMapper(new XmlFactory(xmlInputFactory));
    }

    public MeasurementSiteTable(Set<String> siteIds) {
        this.siteIds = Set.copyOf(siteIds);
        msrReader = createXmlMapper().readerFor(MeasurementSiteRecord.class);
    }

    public void parse(InputStream inputStream) throws IOException {
        try {
            SAXParser saxParser = saxParserFactory.newSAXParser();
            var handler = new SaxRecordHandler("measurementSiteTable/measurementSiteRecord", this::processRecord);
            saxParser.parse(inputStream, handler);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    private void processRecord(String s) {
        try {
            MeasurementSiteRecord record = msrReader.readValue(s);
            if (siteIds.isEmpty() || siteIds.contains(record.id())) {
                records.put(record.id(), record);
            }
        } catch (JsonProcessingException e) {
            // ignore
        }
    }

    public Set<String> getMeasurementSiteIds() {
        return Set.copyOf(records.keySet());
    }

    public MeasurementSiteRecord findMeasurementSiteRecord(String id) {
        return records.get(id);
    }

}
