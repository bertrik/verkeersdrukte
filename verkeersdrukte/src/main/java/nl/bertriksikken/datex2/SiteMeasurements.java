package nl.bertriksikken.datex2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SiteMeasurements {

    @JacksonXmlProperty(localName = "measurementSiteReference")
    Reference reference;

    @JacksonXmlProperty(localName = "measurementTimeDefault")
    String measurementTimeDefault = "";

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "measuredValue")
    public List<MeasuredValue> measuredValueList = new ArrayList<>();

    // jackson constructor
    SiteMeasurements() {
        this(null, "");
    }

    SiteMeasurements(Reference reference, String measurementTimeDefault) {
        this.reference = reference;
        this.measurementTimeDefault = measurementTimeDefault;
    }

    SiteMeasurements(Reference reference, Instant time) {
        this(reference, time.truncatedTo(ChronoUnit.SECONDS).toString());
    }

    public void addMeasuredValue(MeasuredValue.BasicData basicData) {
        int index = measuredValueList.size() + 1;
        measuredValueList.add(new MeasuredValue(index, basicData));
    }

    public static final class Reference {
        @JacksonXmlProperty(localName = "id", isAttribute = true)
        String id = "";

        @JacksonXmlProperty(localName = "version", isAttribute = true)
        String version = "";

        @JacksonXmlProperty(localName = "targetClass", isAttribute = true)
        String targetClass = "";

        // jackson constructor
        Reference() {
            this("", "");
        }

        Reference(String id, String version) {
            this.id = id;
            this.version = version;
        }
    }
}

