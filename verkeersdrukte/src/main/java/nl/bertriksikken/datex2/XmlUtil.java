package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;

import java.util.ArrayDeque;
import java.util.Deque;

public final class XmlUtil {

    public static String getPath(JsonParser parser) {
        Deque<String> parts = new ArrayDeque<>();
        for (JsonStreamContext ctx = parser.getParsingContext(); ctx != null; ctx = ctx.getParent()) {
            String name = ctx.getCurrentName();
            if (name != null) {
                parts.push(name);
            }
        }
        return "/" + String.join("/", parts);
    }

}
