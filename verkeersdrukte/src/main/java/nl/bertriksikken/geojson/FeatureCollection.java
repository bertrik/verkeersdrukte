package nl.bertriksikken.geojson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureCollection extends GeoJsonObject {

    @JsonProperty("features")
    private final List<Feature> features;

    @JsonCreator
    public FeatureCollection(
            @JsonProperty("features") List<Feature> features) {

        super("FeatureCollection");
        this.features = new ArrayList<>(features);
    }

    public FeatureCollection() {
        this(new ArrayList<>());
    }

    public void add(Feature feature) {
        features.add(feature);
    }

    public List<Feature> getFeatures() {
        return List.copyOf(features);
    }

    enum EGeometry {
        POINT("Point");

        private final String id;

        EGeometry(String id) {
            this.id = id;
        }
    }

    public static final class Feature extends GeoJsonObject {

        private final String id;

        @JsonProperty("geometry")
        private final GeoJsonGeometry geometry;

        @JsonProperty("properties")
        private final Map<String, Object> properties;

        @JsonCreator
        public Feature(
                @JsonProperty("id") String id,
                @JsonProperty("geometry") GeoJsonGeometry geometry,
                @JsonProperty("properties") Map<String, Object> properties) {

            super("Feature");
            this.id = id;
            this.geometry = geometry;
            this.properties = (properties != null)
                    ? new LinkedHashMap<>(properties)
                    : new LinkedHashMap<>();
        }

        // convenience constructor
        public Feature(GeoJsonGeometry geometry) {
            this("", geometry, null);
        }

        // copy-constructor
        public Feature(Feature feature) {
            this(feature.id, feature.geometry, feature.properties);
        }

        public GeoJsonGeometry getGeometry() {
            return geometry;
        }

        public Map<String, Object> getProperties() {
            return new LinkedHashMap<>(properties);
        }

        public void addProperty(String name, Object value) {
            properties.put(name, value);
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(
                    value = PointGeometry.class,
                    name = "Point")
    })
    public static abstract class GeoJsonGeometry extends GeoJsonObject {

        private GeoJsonGeometry(EGeometry geometry) {
            super(geometry.id);
        }
    }

    @JsonAutoDetect(getterVisibility = Visibility.NONE)
    public static final class PointGeometry extends GeoJsonGeometry {

        @JsonProperty("coordinates")
        private final double[] coordinates;

        @JsonCreator
        public PointGeometry(
                @JsonProperty("coordinates") double[] coordinates) {

            super(EGeometry.POINT);
            this.coordinates = coordinates;
        }

        public PointGeometry(double latitude, double longitude) {
            this(new double[]{longitude, latitude});
        }

        public double getLatitude() {
            return coordinates[1];
        }

        public double getLongitude() {
            return coordinates[0];
        }
    }
}