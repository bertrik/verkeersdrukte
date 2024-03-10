package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class MeasuredDataPublication {
    private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    private final List<SiteMeasurements> siteMeasurementsList = new ArrayList<>();
    private Instant publicationTime = Instant.now();

    public static MeasuredDataPublication parse(InputStream inputStream) throws IOException {
        MeasuredDataPublication measuredDataPublication = new MeasuredDataPublication();
        try {
            SAXParser saxParser = saxParserFactory.newSAXParser();
            DefaultHandler handler = new StreamingHandler(measuredDataPublication);
            saxParser.parse(inputStream, handler);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
        return measuredDataPublication;
    }

    public Instant getPublicationTime() {
        return publicationTime;
    }

    public List<SiteMeasurements> getSiteMeasurementsList() {
        return List.copyOf(siteMeasurementsList);
    }

    private void addSiteMeasurements(SiteMeasurements siteMeasurements) {
        siteMeasurementsList.add(siteMeasurements);
    }

    private static final class StreamingHandler extends DefaultHandler {
        private static final String SITE_MEASUREMENTS = "d2LogicalModel/payloadPublication/siteMeasurements";
        private static final String PUBLICATION_TIME = "d2LogicalModel/payloadPublication/publicationTime";
        private static final XmlMapper mapper = new XmlMapper();
        private final MeasuredDataPublication publication;
        private final SaxCollector collector = new SaxCollector();

        private StreamingHandler(MeasuredDataPublication publication) {
            this.publication = publication;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String path = collector.updatePath(qName);
            if (path.endsWith(SITE_MEASUREMENTS) || path.endsWith(PUBLICATION_TIME)) {
                collector.resetElement();
            }
            collector.appendBegin(qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            String path = collector.appendEnd(qName);
            try {
                if (path.endsWith(PUBLICATION_TIME)) {
                    String publicationTime = mapper.readValue(collector.getElement(), String.class);
                    publication.publicationTime = Instant.parse(publicationTime);
                } else if (path.endsWith(SITE_MEASUREMENTS)) {
                    SiteMeasurements siteMeasurements = mapper.readValue(collector.getElement(), SiteMeasurements.class);
                    publication.addSiteMeasurements(siteMeasurements);
                }
            } catch (JsonProcessingException e) {
                throw new SAXException(e);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            collector.appendData(ch, start, length);
        }
    }

}
