package nl.bertriksikken.verkeersdrukte.ndw;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bertriksikken.geojson.FeatureCollection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class NdwShapeFile {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FeatureCollection shapeFile;

    public NdwShapeFile() {
        shapeFile = new FeatureCollection();
    }

    /**
     * Downloads the shape file from remote source and stores it locally.
     */
    public void download(NdwDownloader downloader) throws IOException {
        File file = downloader.getMapFile(INdwMapsApi.POINT_RECORD_GEOJSON);
        shapeFile = MAPPER.readValue(file, FeatureCollection.class);
    }

    public FeatureCollection getFeatureCollection() {
        return shapeFile;
    }

    public Set<String> getSiteIds() {
        return shapeFile.getFeatures().stream()
                .map(FeatureCollection.Feature::getProperties)
                .map(p -> p.get("externalId"))
                .filter(Objects::nonNull)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toSet());
    }

    public void parse(InputStream inputStream) throws IOException {
        shapeFile = MAPPER.readValue(inputStream, FeatureCollection.class);
    }

    public FeatureCollection.Feature findFeature(String location) {
        return shapeFile.getFeatures().stream()
                .filter(f -> location.equals(f.getProperties().get("externalId")))
                .findFirst().orElse(null);
    }
}
