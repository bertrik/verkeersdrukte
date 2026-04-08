package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Static VMS controller data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VmsController {

    @JacksonXmlProperty(localName = "id", isAttribute = true)
    public String id;

    @JacksonXmlProperty(localName = "version", isAttribute = true)
    public String version;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "vms:vms")
    List<VmsWithIndex> vmsList = new ArrayList<>();

    public PointCoordinates findLocationData() {
        Vms vms = findFirstVms();
        return Optional.ofNullable(vms)
                .map(Vms::vmsLocation)
                .map(VmsLocation::findPointCoordinates)
                .orElse(null);
    }

    public boolean hasLocationData() {
        return findLocationData() != null;
    }

    public Vms findFirstVms() {
        return vmsList.stream().findFirst().map(VmsWithIndex::vms).orElse(null);
    }

    public String getId() {
        return id;
    }

    public record VmsWithIndex(@JacksonXmlProperty(localName = "vmsIndex", isAttribute = true) int vmsIndex,
                               @JacksonXmlProperty(localName = "vms:vms") Vms vms) {
    }
}
