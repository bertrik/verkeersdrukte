package nl.bertriksikken.datex2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.io.InputStream;

public final class MeasuredDataPublication {

    private final XmlMapper xmlMapper;

    public MeasuredDataPublication(XmlMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
    }

    public void parse(InputStream stream, ISiteMeasurementsHandler handler) throws IOException {
        ObjectReader measurementsReader = xmlMapper.readerFor(SiteMeasurements.class);
        try (JsonParser parser = xmlMapper.createParser(stream)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token.isStructStart()) {
                    String xpath = XmlUtil.getPath(parser);
                    if (xpath.endsWith("/payloadPublication/siteMeasurements")) {
                        SiteMeasurements siteMeasurements = measurementsReader.readValue(parser);
                        handler.handle(siteMeasurements);
                    }
                }
            }
        }
    }

    public interface ISiteMeasurementsHandler {
        void handle(SiteMeasurements record);
    }

}
