package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VmsStatus(@JacksonXmlProperty(localName = "vms:statusUpdateTime") String updateTime,
                        @JacksonXmlProperty(localName = "vms:workingStatus") String workingStatus,
                        @JacksonXmlElementWrapper(useWrapping = false)
                        @JacksonXmlProperty(localName = "vms:vmsMessage") List<VmsMessageWithIndex> vmsMessages) {

    public boolean hasImageData() {
        return Optional.ofNullable(findFirstVmsMessage())
                .map(VmsMessage::vmsImage)
                .map(VmsImage::hasImageData)
                .orElse(false);
    }

    public boolean isWorking() {
        return "working".equals(workingStatus);
    }

    public Instant getUpdateTime() {
        return Instant.parse(updateTime);
    }

    public VmsMessage findFirstVmsMessage() {
        return vmsMessages
                .stream()
                .findFirst()
                .map(VmsMessageWithIndex::vmsMessage)
                .orElse(null);
    }

    public record VmsMessageWithIndex(
            @JacksonXmlProperty(localName = "messageIndex", isAttribute = true) int messageIndex,
            @JacksonXmlProperty(localName = "vms:vmsMessage") VmsMessage vmsMessage) {
    }
}
