package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dynamic VMS controller status data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VmsControllerStatus {

    @JacksonXmlProperty(localName = "vms:vmsControllerReference")
    VmsControllerReference vmsControllerReference;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "vms:vmsStatus")
    List<VmsStatusWithIndex> vmsStatuses = new ArrayList<>();

    public String getId() {
        return Optional.ofNullable(vmsControllerReference).map(VmsControllerReference::id).orElse("");
    }

    public Optional<VmsStatus> findFirstVmsStatus() {
        return vmsStatuses.stream()
                .findFirst()
                .map(VmsStatusWithIndex::vmsStatus);
    }

    public byte[] getImageData() {
        return findFirstVmsStatus()
                .map(VmsStatus::findFirstVmsMessage)
                .map(VmsMessage::vmsImage)
                .map(VmsImage::asBytes)
                .orElse(null);
    }

    public boolean hasImageData() {
        return findFirstVmsStatus().map(VmsStatus::hasImageData).orElse(false);
    }

    public boolean isWorking() {
        return findFirstVmsStatus().map(VmsStatus::isWorking).orElse(false);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VmsControllerReference(@JacksonXmlProperty(localName = "id", isAttribute = true) String id,
                                         @JacksonXmlProperty(localName = "version", isAttribute = true) String version) {
    }

    public record VmsStatusWithIndex(@JacksonXmlProperty(localName = "vmsIndex", isAttribute = true) int vmsIndex,
                                     @JacksonXmlProperty(localName = "vms:vmsStatus") VmsStatus vmsStatus) {
    }
}
