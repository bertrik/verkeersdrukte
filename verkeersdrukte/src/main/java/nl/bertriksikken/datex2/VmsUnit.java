package nl.bertriksikken.datex2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * A record from the VmsPublication, containing dynamic display data of a VMS.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VmsUnit {

    @JacksonXmlProperty(localName = "vmsUnitReference")
    VmsUnitReference vmsUnitReference;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "vms")
    List<VmsWithIndex> vmsList = new ArrayList<>();

    // no-arg jackson constructor
    VmsUnit() {
        this("", "");
    }

    public VmsUnit(String id, String version) {
        vmsUnitReference = new VmsUnitReference(id, version);
    }

    public String getId() {
        return vmsUnitReference.id();
    }

    public Vms find(int index) {
        return vmsList.stream().filter(vms -> vms.vmsIndex == index).map(VmsWithIndex::vms).findFirst().orElse(null);
    }

    public void addVms(Vms vms) {
        int index = vmsList.size() + 1;
        vmsList.add(new VmsWithIndex(index, vms));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VmsUnitReference(@JacksonXmlProperty(localName = "id", isAttribute = true) String id,
                                   @JacksonXmlProperty(localName = "version", isAttribute = true) String version) {
    }

    public record VmsWithIndex(@JacksonXmlProperty(localName = "vmsIndex") int vmsIndex,
                               @JacksonXmlProperty(localName = "vms") Vms vms) {
    }

    public record Vms(@JacksonXmlProperty(localName = "vmsWorking") boolean vmsWorking,
                      @JacksonXmlElementWrapper(useWrapping = false)
                      @JacksonXmlProperty(localName = "vmsMessage") List<VmsMessageWithIndex> vmsMessages) {
        public Vms(boolean vmsWorking) {
            this(vmsWorking, new ArrayList<>());
        }

        public VmsMessage find(int index) {
            return vmsMessages.stream().filter(vms -> vms.messageIndex == index).map(VmsMessageWithIndex::vmsMessage).findFirst().orElse(null);
        }

        public void addVmsMessage(VmsMessage vmsMessage) {
            int index = vmsMessages.size() + 1;
            vmsMessages.add(new VmsMessageWithIndex(index, vmsMessage));
        }
    }

    public record VmsMessageWithIndex(
            @JacksonXmlProperty(localName = "messageIndex", isAttribute = true) int messageIndex,
            @JacksonXmlProperty(localName = "vmsMessage") VmsMessage vmsMessage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VmsMessage(@JacksonXmlProperty(localName = "timeLastSet") String timeLastSet,
                             @JacksonXmlProperty(localName = "vmsMessageExtension") VmsMessageExtensionWrapper wrapper) {
        public VmsMessage(String timeLastSet, VmsMessageExtension vmsMessageExtension) {
            this(timeLastSet, new VmsMessageExtensionWrapper(vmsMessageExtension));
        }
        public VmsMessageExtension extension() {
            return wrapper != null ? wrapper.extension() : null;
        }
    }

    public record VmsMessageExtensionWrapper(
            @JacksonXmlProperty(localName = "vmsMessageExtension") VmsMessageExtension extension) {
    }

    public record VmsMessageExtension(@JacksonXmlProperty(localName = "vmsImage") VmsImage vmsImage) {
    }

    public record VmsImage(@JacksonXmlProperty(localName = "imageData") ImageData imageData) {
    }

    public record ImageData(@JacksonXmlProperty(localName = "binary") String binary,
                            @JacksonXmlProperty(localName = "encoding") String encoding,
                            @JacksonXmlProperty(localName = "mimeType") String mimeType) {
        public static ImageData png(byte[] binary) {
            return new ImageData(Base64.getEncoder().encodeToString(binary), "base64", "image/png");
        }

        public byte[] asBytes() {
            return Base64.getDecoder().decode(binary);
        }
    }
}
