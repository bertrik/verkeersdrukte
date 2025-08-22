package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MeasurementSiteTable {

    private static final XmlFactory xmlFactory;

    static {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xmlFactory = new XmlFactory(xmlInputFactory);
    }

    private final Map<String, MeasurementSiteRecord> records = new LinkedHashMap<>();
    private final Set<String> siteIds;
    private final ObjectReader msrReader;

    public MeasurementSiteTable(Set<String> siteIds) {
        this.siteIds = Set.copyOf(siteIds);
        msrReader = createXmlMapper().readerFor(MeasurementSiteRecord.class);
    }

    static XmlMapper createXmlMapper() {
        return new XmlMapper(xmlFactory);
    }

    public void parse(InputStream stream) throws IOException {
        try (JsonParser parser = xmlFactory.createParser(stream)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token.isStructStart()) {
                    String xpath = getPath(parser);
                    if (xpath.endsWith("/measurementSiteTable/measurementSiteRecord")) {
                        MeasurementSiteRecord record = msrReader.readValue(parser);
                        if (siteIds.isEmpty() || siteIds.contains(record.id())) {
                            records.put(record.id(), record);
                        }
                    }
                }
            }
        }
    }

    private String getPath(JsonParser parser) {
        StringBuilder path = new StringBuilder();
        for (JsonStreamContext ctx = parser.getParsingContext(); ctx != null; ctx = ctx.getParent()) {
            String name = ctx.getCurrentName();
            if (name != null) {
                path.insert(0, "/" + name);
            }
        }
        return path.toString();
    }

    public Set<String> getMeasurementSiteIds() {
        return Set.copyOf(records.keySet());
    }

    public MeasurementSiteRecord findMeasurementSiteRecord(String id) {
        return records.get(id);
    }

}
