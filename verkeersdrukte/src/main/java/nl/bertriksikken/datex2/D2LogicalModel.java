package nl.bertriksikken.datex2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "d2LogicalModel")
@JsonIgnoreProperties(ignoreUnknown = true)
public final class D2LogicalModel {

    @JacksonXmlProperty(localName = "modelBaseVersion", isAttribute = true)
    String modelBaseVersion = "2";

    @JacksonXmlProperty(localName = "payloadPublication")
    public PayloadPublication payloadPublication;

    // jackson constructor
    D2LogicalModel() {
        this(null);
    }

    public D2LogicalModel(PayloadPublication payloadPublication) {
        this.payloadPublication = payloadPublication;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
    @JsonSubTypes({@Type(value = MeasuredDataPublication.class, name = "MeasuredDataPublication")})
    public static abstract class PayloadPublication {
        @JacksonXmlProperty(localName = "type", isAttribute = true)
        public final String type;

        @JacksonXmlProperty(localName = "publicationTime")
        public String publicationTime;

        PayloadPublication(String type) {
            this.type = type;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MeasuredDataPublication extends PayloadPublication {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "siteMeasurements")
        public List<SiteMeasurements> siteMeasurementsList = new ArrayList<>();

        public MeasuredDataPublication() {
            // jackson no-arg constructor
            super("MeasuredDataPublication");
        }

        public MeasuredDataPublication(Instant publicationTime) {
            super("MeasuredDataPublication");
            this.publicationTime = publicationTime.truncatedTo(ChronoUnit.SECONDS).toString();
        }

        public void addSiteMeasurements(SiteMeasurements measurements) {
            siteMeasurementsList.add(measurements);
        }

    }
}
