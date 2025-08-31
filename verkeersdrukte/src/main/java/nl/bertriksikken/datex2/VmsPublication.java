package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The VMS publication, containing a list of VmsUnit records, each with VMS display data.
 */
public final class VmsPublication {

    private final XmlMapper xmlMapper;
    private final Map<String, VmsUnit> records = new LinkedHashMap<>();

    public VmsPublication(XmlMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
    }

    public VmsUnit find(String id) {
        return records.get(id);
    }

    public List<VmsUnit> getRecords() {
        return List.copyOf(records.values());
    }

    public void parse(InputStream stream) throws IOException {
        ObjectReader recordReader = xmlMapper.readerFor(VmsUnit.class);
        try (JsonParser parser = xmlMapper.createParser(stream)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token.isStructStart()) {
                    String xpath = getPath(parser);
                    if (xpath.endsWith("/payloadPublication/vmsUnit")) {
                        VmsUnit record = recordReader.readValue(parser);
                        records.put(record.getId(), record);
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
