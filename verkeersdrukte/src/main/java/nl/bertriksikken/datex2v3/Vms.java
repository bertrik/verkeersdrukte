package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Vms(@JacksonXmlProperty(localName = "vms:description") MultilingualString description,
                  @JacksonXmlProperty(localName = "vms:physicalSupport") String physicalSupport,
                  @JacksonXmlProperty(localName = "vms:vmsLocation") VmsLocation vmsLocation,
                  @JacksonXmlProperty(localName = "vms:vmsType") String vmsType) {
}