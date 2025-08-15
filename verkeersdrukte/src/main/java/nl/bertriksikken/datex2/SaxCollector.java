package nl.bertriksikken.datex2;

import org.xml.sax.Attributes;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Collects element data as part of an XML SAX parser.
 */
public final class SaxCollector {

    private final Deque<String> stack = new ArrayDeque<>();
    private final StringBuilder element = new StringBuilder();

    public void resetElement() {
        element.setLength(0);
    }

    public String updatePath(String qName) {
        stack.addLast(qName);
        return getPath();
    }

    private String getPath() {
        return String.join("/", stack);
    }

    public void appendBegin(String qName, Attributes attributes) {
        element.append("<").append(qName);
        for (int i = 0; i < attributes.getLength(); i++) {
            element.append(" ").append(attributes.getQName(i)).append("=\"").append(attributes.getValue(i)).append("\"");
        }
        element.append(">");
    }

    public void appendData(char[] ch, int start, int length) {
        String text = new String(ch, start, length);
        element.append(escapeXml(text));
    }

    public String appendEnd(String qName) {
        String path = getPath();
        element.append("</").append(qName).append(">");
        stack.removeLast();
        return path;
    }

    public String getElement() {
        return element.toString();
    }

    private String escapeXml(String input) {
        return input
                .replace("&", "&amp;")   // must be first
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}
