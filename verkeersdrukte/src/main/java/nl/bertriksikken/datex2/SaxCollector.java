package nl.bertriksikken.datex2;

import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * Collects element data as part of an XML SAX parser.
 */
public final class SaxCollector {

    private final Stack<String> stack = new Stack<>();
    private final StringBuilder xml = new StringBuilder();

    public void resetElement() {
        xml.setLength(0);
    }

    public String updatePath(String qName) {
        stack.push(qName);
        return getPath();
    }

    private String getPath() {
        return String.join("/", stack);
    }

    public void appendBegin(String qName, Attributes attributes) {
        xml.append("<").append(qName);
        for (int i = 0; i < attributes.getLength(); i++) {
            xml.append(" ").append(attributes.getQName(i)).append("=\"").append(attributes.getValue(i)).append("\"");
        }
        xml.append(">");
    }

    public void appendData(char[] ch, int start, int length) {
        xml.append(ch, start, length);
    }

    public String appendEnd(String qName) {
        String path = getPath();
        xml.append("</").append(qName).append(">");
        stack.pop();
        return path;
    }

    public String getElement() {
        return xml.toString();
    }

}
