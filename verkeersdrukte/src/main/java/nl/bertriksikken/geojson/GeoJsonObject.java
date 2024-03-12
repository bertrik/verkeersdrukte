package nl.bertriksikken.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The base type for all GeoJSON objects.
 */
public abstract class GeoJsonObject {

    @JsonProperty("type")
    public final String type;

    GeoJsonObject(String type) {
        this.type = type;
    }

}