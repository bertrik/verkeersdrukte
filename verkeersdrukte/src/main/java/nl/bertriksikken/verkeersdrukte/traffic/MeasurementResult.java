package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class MeasurementResult {

    @JsonProperty("location")
    public final String location;

    @JsonProperty("measurement")
    public final AggregateMeasurement measurement;

    public MeasurementResult(String location, AggregateMeasurement measurement) {
        this.location = location;
        this.measurement = measurement;
    }

}
