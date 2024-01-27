package nl.bertriksikken.geojson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureCollection extends GeoJsonObject {

    @JsonProperty("features")
    private final List<Feature> features = new ArrayList<>();

    public FeatureCollection() {
        super("FeatureCollection");
    }

    public void add(Feature feature) {
        features.add(feature);
    }

    public List<Feature> getFeatures() {
        return List.copyOf(features);
    }

    public static final class Feature extends GeoJsonObject {
        @JsonProperty("geometry")
        private final GeoJsonGeometry geometry;

        @JsonProperty("properties")
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public Feature(GeoJsonGeometry geometry) {
            super("Feature");
            this.geometry = geometry;
        }

        public void addProperty(String name, Object value) {
            properties.put(name, value);
        }

        public Map<String, Object> getProperties() {
            return Map.copyOf(properties);
        }
    }

    public static abstract class GeoJsonGeometry extends GeoJsonObject {
        private GeoJsonGeometry(EGeometry geometry) {
            super(geometry.id);
        }
    }

    @JsonAutoDetect(getterVisibility = Visibility.NONE)
    public static final class PointGeometry extends GeoJsonGeometry {
        @JsonProperty("coordinates")
        private final double[] coordinates;

        // jackson no-arg constructor
        @SuppressWarnings("unused")
        private PointGeometry() {
            this(Double.NaN, Double.NaN);
        }

        public PointGeometry(double latitude, double longitude) {
            super(EGeometry.POINT);
            coordinates = new double[]{longitude, latitude};
        }

        public double getLatitude() {
            return coordinates[1];
        }

        public double getLongitude() {
            return coordinates[0];
        }
    }

    enum EGeometry {
        POINT("Point");

        private final String id;

        EGeometry(String id) {
            this.id = id;
        }
    }
}