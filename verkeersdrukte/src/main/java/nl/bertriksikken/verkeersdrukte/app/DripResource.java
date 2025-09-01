package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.bertriksikken.datex2.MultilingualString;
import nl.bertriksikken.datex2.MultilingualString.MultilingualStringValue;
import nl.bertriksikken.datex2.VmsTablePublication;
import nl.bertriksikken.datex2.VmsUnit.ImageData;
import nl.bertriksikken.datex2.VmsUnit.VmsImage;
import nl.bertriksikken.datex2.VmsUnit.VmsMessage;
import nl.bertriksikken.datex2.VmsUnit.VmsMessageExtension;
import nl.bertriksikken.datex2.VmsUnitRecord;
import nl.bertriksikken.datex2.VmsUnitRecord.VmsRecord;
import nl.bertriksikken.datex2.VmsUnitRecord.VmsRecord.VmsLocation;
import nl.bertriksikken.datex2.VmsUnitRecord.VmsRecord.VmsLocation.LocationForDisplay;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.geojson.FeatureCollection.Feature;
import nl.bertriksikken.geojson.FeatureCollection.PointGeometry;
import nl.bertriksikken.verkeersdrukte.traffic.ITrafficHandler;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;

import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Path(DripResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public final class DripResource {

    public static final String PATH = "/drips";
    static final String STATIC_PATH = "/static";
    static final String DYNAMIC_PATH = "/dynamic";

    private final ITrafficHandler handler;
    private final TrafficConfig config;

    DripResource(ITrafficHandler handler, TrafficConfig config) {
        this.handler = handler;
        this.config = config;
    }

    private FeatureCollection.Feature addUrlProperties(FeatureCollection.Feature f) {
        FeatureCollection.Feature feature = new FeatureCollection.Feature(f);
        String id = (String) f.getProperties().getOrDefault("id", "");
        if (!id.isEmpty()) {
            String staticDataUrl = PATH + STATIC_PATH + "/" + id;
            feature.addProperty("staticDataUrl", staticDataUrl);
            String dynamicDataUrl = PATH + DYNAMIC_PATH + "/" + id;
            feature.addProperty("dynamicDataUrl", dynamicDataUrl);
        }
        return feature;
    }

    @Operation(summary = "Get GeoJSON containing all DRIPs", tags = {"static"})
    @GET
    @Path(STATIC_PATH)
    public FeatureCollection getStatic() {
        VmsTablePublication vmsLocationTable = handler.getVmsLocationTable();
        FeatureCollection featureCollection = new FeatureCollection();
        vmsLocationTable.getRecords().stream()
                .map(this::mapVmsUnitRecord).filter(Objects::nonNull)
                .map(this::addUrlProperties)
                .forEach(featureCollection::add);
        return featureCollection;
    }

    private Feature mapVmsUnitRecord(VmsUnitRecord vmsUnitRecord) {
        VmsRecord vmsRecord = Optional.ofNullable(vmsUnitRecord).map(r -> r.find(1)).orElse(null);
        Feature feature = Optional.ofNullable(vmsRecord)
                .map(VmsRecord::vmsLocation)
                .map(VmsLocation::locationForDisplay)
                .filter(LocationForDisplay::isValid)
                .map(l -> new PointGeometry(l.latitude(), l.longitude()))
                .map(Feature::new)
                .orElse(null);
        if (feature != null) {
            feature.addProperty("id", vmsUnitRecord.getId());
            feature.addProperty("physicalMounting", vmsRecord.physicalMounting());
            feature.addProperty("type", vmsRecord.type());
            String description = Optional.ofNullable(vmsRecord.vmsDescription())
                    .map(MultilingualString::values)
                    .map(l -> l.isEmpty() ? null : l.get(0))
                    .map(MultilingualStringValue::value)
                    .orElse("");
            feature.addProperty("description", description);
        }
        return feature;
    }

    @Operation(summary = "Get static data for a specific DRIP", tags = {"static"})
    @GET
    @Path(STATIC_PATH + "/{id}")
    public Optional<Feature> getStatic(@PathParam("id") String id) {
        VmsTablePublication vmsLocationTable = handler.getVmsLocationTable();
        VmsUnitRecord vmsUnitRecord = vmsLocationTable.find(id);
        return Optional.ofNullable(mapVmsUnitRecord(vmsUnitRecord));
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response getIndex() {
        InputStream in = getClass().getResourceAsStream("/assets/drips.html");
        return Response.ok(in, MediaType.TEXT_HTML).build();
    }

    @Operation(summary = "Get dynamic data for a specific DRIP", tags = {"dynamic"})
    @GET
    @Path(DYNAMIC_PATH + "/{id}")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Optional<DynamicDataJson> getDynamic(@PathParam("id") String id) {
        return Optional.ofNullable(handler.getVmsPublication())
                .map(pub -> pub.find(id))   // VmsUnit
                .map(u -> u.find(1))  // VmsUnit.Vms
                .map(v -> v.find(1))  // VmsMessage
                .map(this::mapVmsMessage);  // build DynamicDataJson
    }

    private DynamicDataJson mapVmsMessage(VmsMessage vmsMessage) {
        Instant timeLastSet = Instant.parse(vmsMessage.timeLastSet());
        byte[] imageData = Optional.of(vmsMessage)
                .map(VmsMessage::extension)
                .map(VmsMessageExtension::vmsImage)
                .map(VmsImage::imageData)
                .map(ImageData::asBytes).orElse(new byte[0]);
        return new DynamicDataJson(timeLastSet, imageData);
    }

    public final class DynamicDataJson {
        @SuppressWarnings("unused")
        @JsonProperty("lastUpdate")
        final String lastUpdate;

        @SuppressWarnings("unused")
        @JsonProperty("pngData")
        final byte[] imageData;

        DynamicDataJson(Instant instant, byte[] data) {
            OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant.truncatedTo(ChronoUnit.SECONDS), config.getTimeZone());
            lastUpdate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
            imageData = data.clone();
        }
    }

}
