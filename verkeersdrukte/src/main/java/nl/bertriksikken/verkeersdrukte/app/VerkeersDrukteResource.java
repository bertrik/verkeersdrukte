package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.caching.CacheControl;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.verkeersdrukte.traffic.ITrafficHandler;
import nl.bertriksikken.verkeersdrukte.traffic.SiteMeasurement;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Path(VerkeersDrukteResource.TRAFFIC_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public final class VerkeersDrukteResource implements IVerkeersDrukteResource {
    static final String TRAFFIC_PATH = "/traffic";
    static final String STATIC_PATH = "/static";
    static final String DYNAMIC_PATH = "/dynamic";
    private static final Logger LOG = LoggerFactory.getLogger(VerkeersDrukteResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ITrafficHandler handler;
    private final AtomicInteger atomicInteger = new AtomicInteger();

    private final TrafficConfig config;

    VerkeersDrukteResource(ITrafficHandler handler, TrafficConfig config) {
        this.handler = handler;
        this.config = config;
        mapper.findAndRegisterModules();
    }

    @Override
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response getIndex() {
        InputStream in = getClass().getResourceAsStream("/assets/index.html");
        return Response.ok(in, MediaType.TEXT_HTML).build();
    }

    @Override
    @GET
    @Path(STATIC_PATH)
    public FeatureCollection getStatic() {
        FeatureCollection featureCollection = new FeatureCollection();
        for (FeatureCollection.Feature feature : handler.getStaticData().getFeatures()) {
            FeatureCollection.Feature updated = addUrlProperties(feature);
            featureCollection.add(updated);
        }
        return featureCollection;
    }

    private FeatureCollection.Feature addUrlProperties(FeatureCollection.Feature f) {
        FeatureCollection.Feature feature = new FeatureCollection.Feature(f);
        String location = (String) f.getProperties().getOrDefault("dgl_loc", "");
        if (!location.isEmpty()) {
            String staticDataUrl = config.getBaseUrl() + TRAFFIC_PATH + STATIC_PATH + "/" + location;
            feature.addProperty("staticDataUrl", staticDataUrl);
            String dynamicDataUrl = config.getBaseUrl() + TRAFFIC_PATH + DYNAMIC_PATH + "/" + location;
            feature.addProperty("dynamicDataUrl", dynamicDataUrl);
            // streetview, see https://stackoverflow.com/questions/387942/google-street-view-url
            FeatureCollection.PointGeometry geometry = (FeatureCollection.PointGeometry) feature.getGeometry();
            int angle = Integer.parseInt((String) f.getProperties().getOrDefault("meetricht", 0));
            String streetviewUrl = String.format(Locale.ROOT, "https://maps.google.com/maps?layer=c&cbll=%.6f,%.6f&cbp=12,%d,0,0,0",
                    geometry.getLatitude(), geometry.getLongitude(), angle);
            feature.addProperty("streetviewUrl", streetviewUrl);
        }
        return feature;
    }

    @Override
    @GET
    @Path(STATIC_PATH + "/{location}")
    public Optional<FeatureCollection.Feature> getStatic(@PathParam("location") String location) {
        FeatureCollection.Feature feature = handler.getStaticData(location);
        if (feature != null) {
            feature = addUrlProperties(feature);
        }
        return Optional.ofNullable(feature);
    }

    @Override
    @GET
    @Path(DYNAMIC_PATH + "/{location}")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Optional<DynamicDataJson> getDynamic(@PathParam("location") String location) {
        // return snapshot of most recent measurement for location
        SiteMeasurement siteMeasurement = handler.getDynamicData(location);
        if (siteMeasurement == null) {
            return Optional.empty();
        }
        return Optional.of(new DynamicDataJson(siteMeasurement));
    }

    @Override
    @GET
    @Path(DYNAMIC_PATH + "/{location}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @CacheControl(noCache = true)
    public void getTrafficEvents(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam("location") String location) {
        // verify that location exists
        if (handler.getStaticData(location) == null) {
            throw new NotFoundException();
        }

        //  get initial data
        String clientId = "client-" + atomicInteger.incrementAndGet();
        BlockingQueue<SiteMeasurement> queue = new ArrayBlockingQueue<>(3);
        eventCallback(clientId, queue, location);

        // subscribe to updates
        LOG.info("Subscribing client '{}' for '{}'", clientId, location);
        handler.subscribe(clientId, () -> eventCallback(clientId, queue, location));
        try (sseEventSink) {
            while (!sseEventSink.isClosed()) {
                SiteMeasurement measurement = queue.poll(1, TimeUnit.SECONDS);
                if (measurement != null) {
                    String id = String.valueOf(measurement.getDateTime().getEpochSecond() / 60);
                    String json = mapper.writeValueAsString(new DynamicDataJson(measurement));
                    OutboundSseEvent event = sse.newEventBuilder().id(id).data(json).build();
                    sseEventSink.send(event);
                }
            }
        } catch (InterruptedException | JsonProcessingException e) {
            LOG.warn("Error sending SSE: {}", e.getMessage());
        } finally {
            LOG.info("Unsubscribing client '{}' for '{}'", clientId, location);
            handler.unsubscribe(clientId);
        }
    }

    private void eventCallback(String clientId, Queue<SiteMeasurement> queue, String location) {
        SiteMeasurement aggregateMeasurement = handler.getDynamicData(location);
        if (aggregateMeasurement != null) {
            if (!queue.offer(aggregateMeasurement)) {
                LOG.warn("Queue for client '{}' location '{}' got stuck", clientId, location);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static final class FlowSpeedJson {
        @JsonProperty("flow")
        private final Long flow;

        @JsonProperty("speed")
        private final BigDecimal speed;

        public FlowSpeedJson(SiteMeasurement.LaneMeasurement measurement) {
            this.flow = Double.isFinite(measurement.flow()) ? Math.round(measurement.flow()) : null;
            this.speed = Double.isFinite(measurement.speed()) ? BigDecimal.valueOf(measurement.speed()).setScale(1, RoundingMode.HALF_UP) : null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    final class DynamicDataJson {
        @JsonProperty("datetime")
        public String dateTime;

        @JsonProperty("flow")
        public Long flow;

        @JsonProperty("speed")
        public BigDecimal speed;

        @JsonProperty("lanes")
        List<FlowSpeedJson> lanes = new ArrayList<>();

        DynamicDataJson(SiteMeasurement measurement) {
            OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(measurement.getDateTime(), config.getTimeZone());
            dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);

            FlowSpeedJson aggregate = new FlowSpeedJson(measurement.aggregate());
            flow = aggregate.flow;
            speed = aggregate.speed;

            measurement.getLanes().stream().map(FlowSpeedJson::new).forEach(lanes::add);
        }
    }

}
