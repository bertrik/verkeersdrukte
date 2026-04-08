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
import nl.bertriksikken.datex2v3.Vms;
import nl.bertriksikken.datex2v3.VmsController;
import nl.bertriksikken.datex2v3.VmsControllerStatus;
import nl.bertriksikken.datex2v3.VmsLocation;
import nl.bertriksikken.datex2v3.VmsMessage;
import nl.bertriksikken.datex2v3.VmsPayload;
import nl.bertriksikken.datex2v3.VmsStatus;
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
public final class DripResource extends BaseResource {

    public static final String PATH = "/drips";
    static final String STATIC_PATH = "/static";
    static final String DYNAMIC_PATH = "/dynamic";

    DripResource(ITrafficHandler handler, TrafficConfig config) {
        super(handler, config);
    }

    @Override
    public Response getIndex() {
        InputStream in = getClass().getResourceAsStream("/assets/drips.html");
        return Response.ok(in, MediaType.TEXT_HTML).build();
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
        VmsPayload vmsPayload = handler.getVmsPayload();
        FeatureCollection featureCollection = new FeatureCollection();
        vmsPayload.getStatuses().stream().filter(VmsControllerStatus::isWorking).filter(VmsControllerStatus::hasImageData).map(VmsControllerStatus::getId).map(vmsPayload::findController).filter(VmsController::hasLocationData).map(this::mapVmsController).filter(Objects::nonNull).forEach(featureCollection::add);
        return featureCollection;
    }

    private Feature mapVmsController(VmsController controller) {
        Vms vms = Optional.ofNullable(controller).map(VmsController::findFirstVms).orElse(null);
        VmsLocation vmsLocation = Optional.ofNullable(vms).map(Vms::vmsLocation).orElse(null);
        Feature feature = Optional.ofNullable(vmsLocation).map(VmsLocation::findPointCoordinates).map(c -> new PointGeometry(c.latitude(), c.longitude())).map(Feature::new).orElse(null);
        if (feature != null) {
            feature.addProperty("id", controller.getId());
            feature.addProperty("physicalSupport", vms.physicalSupport());
            feature.addProperty("type", vms.vmsType());
            String description = Optional.ofNullable(vms.description()).map(Object::toString).orElse("");
            feature.addProperty("description", description);
            feature.addProperty("bearing", vmsLocation.findBearing());
            feature = addUrlProperties(feature);
        }
        return feature;
    }

    @Operation(summary = "Get static data for a specific DRIP", tags = {"static"})
    @GET
    @Path(STATIC_PATH + "/{id}")
    public Optional<Feature> getStatic(@PathParam("id") String id) {
        VmsPayload vmsPayload = handler.getVmsPayload();
        VmsController controller = vmsPayload.findController(id);
        return Optional.ofNullable(mapVmsController(controller));
    }

    @Operation(summary = "Get dynamic data for a specific DRIP", tags = {"dynamic"})
    @GET
    @Path(DYNAMIC_PATH + "/{id}")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Optional<DynamicDataJson> getDynamic(@PathParam("id") String id) {
        return findVmsMessage(id).map(VmsMessage::getTimeLastSet).map(DynamicDataJson::new);
    }

    @Operation(summary = "Get image data for a specific DRIP", tags = {"dynamic"})
    @GET
    @Path(DYNAMIC_PATH + "/{id}/image")
    @Produces("image/png")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Optional<byte[]> getDynamicImage(@PathParam("id") String id) {
        VmsControllerStatus status = handler.getVmsPayload().findStatus(id);
        return Optional.ofNullable(status).map(VmsControllerStatus::getImageData);
    }

    private Optional<VmsMessage> findVmsMessage(String id) {
        VmsControllerStatus status = handler.getVmsPayload().findStatus(id);
        return Optional.ofNullable(status).flatMap(VmsControllerStatus::findFirstVmsStatus).map(VmsStatus::findFirstVmsMessage);
    }

    final class DynamicDataJson {
        @SuppressWarnings("unused")
        @JsonProperty("lastUpdate")
        final String lastUpdate;

        DynamicDataJson(Instant instant) {
            OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant.truncatedTo(ChronoUnit.SECONDS), config.getTimeZone());
            lastUpdate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
        }
    }

}
