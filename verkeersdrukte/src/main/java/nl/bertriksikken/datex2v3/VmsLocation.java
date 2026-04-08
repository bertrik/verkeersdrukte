package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VmsLocation(
        @JacksonXmlProperty(localName = "type", isAttribute = true) String type,
        @JacksonXmlProperty(localName = "loc:pointByCoordinates") PointByCoordinates pointByCoordinates) {

    public PointCoordinates findPointCoordinates() {
        return Optional.ofNullable(pointByCoordinates).map(PointByCoordinates::pointCoordinates).orElse(null);
    }

    public Integer findBearing() {
        return Optional.ofNullable(pointByCoordinates).map(PointByCoordinates::bearing).orElse(null);
    }

    public record PointByCoordinates(
            @JacksonXmlProperty(localName = "loc:bearing") int bearing,
            @JacksonXmlProperty(localName = "loc:pointCoordinates") PointCoordinates pointCoordinates) {
    }

}
