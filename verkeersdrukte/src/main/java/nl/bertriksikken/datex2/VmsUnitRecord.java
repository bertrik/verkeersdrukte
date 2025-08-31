package nl.bertriksikken.datex2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A record from the VmsTablePublication, containing static location data of a VMS.
 */
public final class VmsUnitRecord {
    @JacksonXmlProperty(localName = "id", isAttribute = true)
    String id;

    @JacksonXmlProperty(localName = "version", isAttribute = true)
    String version;

    @JacksonXmlProperty(localName = "numberOfVms")
    int numberOfVms;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "vmsRecord")
    List<VmsRecordWithIndex> vmsRecords = new ArrayList<>();

    // no-arg jackson constructor
    VmsUnitRecord() {
        this("", "");
    }

    public VmsUnitRecord(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public VmsRecord find(int index) {
        return vmsRecords.stream()
                .filter(record -> record.index == index)
                .map(VmsRecordWithIndex::vmsRecord)
                .findFirst().orElse(null);
    }

    void addVmsRecord(VmsRecord record) {
        numberOfVms = vmsRecords.size() + 1;
        vmsRecords.add(new VmsRecordWithIndex(numberOfVms, record));
    }

    record VmsRecordWithIndex(@JacksonXmlProperty(localName = "vmsIndex", isAttribute = true) int index,
                              @JacksonXmlProperty(localName = "vmsRecord") VmsRecord vmsRecord) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VmsRecord(@JacksonXmlProperty(localName = "vmsLocation") VmsLocation vmsLocation) {
        public VmsRecord {
            if (vmsLocation == null) {
                vmsLocation = new VmsLocation(Double.NaN, Double.NaN);
            }
        }

        public VmsRecord(double latitude, double longitude) {
            this(new VmsLocation(latitude, longitude));
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record VmsLocation(@JacksonXmlProperty(localName = "locationForDisplay")
                                  LocationForDisplay locationForDisplay) {
            public VmsLocation(double latitude, double longitude) {
                this(new LocationForDisplay(latitude, longitude));
            }

            public record LocationForDisplay(@JacksonXmlProperty(localName = "latitude") double latitude,
                                             @JacksonXmlProperty(localName = "longitude") double longitude) {
                public boolean isValid() {
                    return Double.isFinite(latitude) && Double.isFinite(longitude);
                }
            }
        }
    }
}
