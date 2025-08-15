package nl.bertriksikken.datex2;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public final class SaxRecordHandler extends DefaultHandler {
    private final SaxCollector collector = new SaxCollector();
    private final RecordCallback callback;
    private final String pathSuffix;

    public SaxRecordHandler(String pathSuffix, RecordCallback callback) {
        this.pathSuffix = pathSuffix;
        this.callback = callback;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        String path = collector.updatePath(qName);
        if (path.endsWith(pathSuffix)) {
            collector.resetElement();
        }
        collector.appendBegin(qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String path = collector.appendEnd(qName);
        if (path.endsWith(pathSuffix)) {
            callback.notifyRecord(collector.getElement());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        collector.appendData(ch, start, length);
    }

    @FunctionalInterface
    public interface RecordCallback {
        void notifyRecord(String record);
    }
}
