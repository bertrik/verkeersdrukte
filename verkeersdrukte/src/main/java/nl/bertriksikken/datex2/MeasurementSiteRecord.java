package nl.bertriksikken.datex2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeasurementSiteRecord {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "measurementSpecificCharacteristics")
    final List<MeasurementSpecificCharacteristics> characteristics = new ArrayList<>();
    @JacksonXmlProperty(localName = "id", isAttribute = true)
    final private String id;

    // jackson constructor
    MeasurementSiteRecord() {
        this("");
    }

    public MeasurementSiteRecord(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public List<MeasurementSpecificCharacteristics> getCharacteristics() {
        return List.copyOf(characteristics);
    }

    public void addCharacteristic(MeasurementSpecificCharacteristicsElement newCharacteristic) {
        int index = characteristics.size() + 1;
        characteristics.add(new MeasurementSpecificCharacteristics(index, newCharacteristic));
    }

    public MeasurementSpecificCharacteristics findCharacteristic(int index) {
        return characteristics.stream().filter(e -> e.index() == index).findFirst().orElse(null);
    }
}

record MeasurementSpecificCharacteristics(
        @JacksonXmlProperty(localName = "index", isAttribute = true) int index,
        @JacksonXmlProperty(localName = "measurementSpecificCharacteristics") MeasurementSpecificCharacteristicsElement element) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record MeasurementSpecificCharacteristicsElement(
        @JacksonXmlProperty(localName = "specificLane") String specificLane,
        @JacksonXmlProperty(localName = "specificMeasurementValueType") String specificMeasurementValueType,
        @JacksonXmlProperty(localName = "specificVehicleCharacteristics") SpecificVehicleCharacteristics specificVehicleCharacteristics) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record SpecificVehicleCharacteristics(@JacksonXmlProperty(localName = "vehicleType") String vehicleType) { }
