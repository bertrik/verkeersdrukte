package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VmsMessage(@JacksonXmlProperty(localName = "vms:timeLastSet") String timeLastSet,
                         @JacksonXmlProperty(localName = "vms:image") VmsImage vmsImage) {

    public Instant getTimeLastSet() {
        return Instant.parse(timeLastSet);
    }

}
