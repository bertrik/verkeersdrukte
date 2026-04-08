package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import nl.bertriksikken.datex2.XmlUtil;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of file dynamische_route_informatie_paneel.xml
 * <p>
 * Contains two payloads:
 * - VmsController payload, with static data
 * - VmsControllerStatus payload, with dynamic status data
 */
public final class VmsPayload {

    private static final XmlFactory xmlFactory;

    static {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xmlFactory = new XmlFactory(xmlInputFactory);
    }

    private final XmlMapper xmlMapper;
    private final Map<String, VmsController> controllers = new LinkedHashMap<>();
    private final Map<String, VmsControllerStatus> statuses = new LinkedHashMap<>();

    public VmsPayload() {
        xmlMapper = new XmlMapper(xmlFactory);
    }

    public void parse(InputStream stream) throws IOException {
        ObjectReader controllerReader = xmlMapper.readerFor(VmsController.class);
        ObjectReader statusReader = xmlMapper.readerFor(VmsControllerStatus.class);
        try (JsonParser parser = xmlFactory.createParser(stream)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token.isStructStart()) {
                    String xpath = XmlUtil.getPath(parser);
                    if (xpath.endsWith("vms:vmsController")) {
                        VmsController controller = controllerReader.readValue(parser);
                        controllers.put(controller.getId(), controller);
                    }
                    if (xpath.endsWith("vms:vmsControllerStatus")) {
                        VmsControllerStatus status = statusReader.readValue(parser);
                        statuses.put(status.getId(), status);
                    }
                }
            }
        }
    }

    public List<VmsControllerStatus> getStatuses() {
        return List.copyOf(statuses.values());
    }

    public VmsController findController(String id) {
        return controllers.get(id);
    }

    public VmsControllerStatus findStatus(String id) {
        return statuses.get(id);
    }

}
