package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record PointCoordinates(@JacksonXmlProperty(localName = "loc:latitude") double latitude,
                               @JacksonXmlProperty(localName = "loc:longitude") double longitude) {
}
