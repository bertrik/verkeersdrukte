package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;

public final class XmlUtil {

    public static String getPath(JsonParser parser) {
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
