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
import java.util.List;
import java.util.Map;

public class VmsTablePublication {

    private static final XmlFactory xmlFactory;

    static {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xmlFactory = new XmlFactory(xmlInputFactory);
    }

    private final XmlMapper xmlMapper;
    private final Map<String, VmsUnitRecord> records = new LinkedHashMap<>();

    public VmsTablePublication() {
        xmlMapper = new XmlMapper(xmlFactory);
    }

    public List<VmsUnitRecord> getRecords() {
        return List.copyOf(records.values());
    }

    public VmsUnitRecord find(String id) {
        return records.get(id);
    }

    static XmlMapper createXmlMapper() {
        return new XmlMapper(xmlFactory);
    }

    public void parse(InputStream stream) throws IOException {
        ObjectReader recordReader = xmlMapper.readerFor(VmsUnitRecord.class);
        try (JsonParser parser = xmlFactory.createParser(stream)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token.isStructStart()) {
                    String xpath = getPath(parser);
                    if (xpath.endsWith("/vmsUnitTable/vmsUnitRecord")) {
                        VmsUnitRecord record = recordReader.readValue(parser);
                        records.put(record.id, record);
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

}
