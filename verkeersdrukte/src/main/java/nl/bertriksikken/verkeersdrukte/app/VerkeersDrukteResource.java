package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.caching.CacheControl;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.verkeersdrukte.traffic.AggregateMeasurement;
import nl.bertriksikken.verkeersdrukte.traffic.ITrafficHandler;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Path(VerkeersDrukteResource.TRAFFIC_PATH)
@Produces(MediaType.APPLICATION_JSON)
public final class VerkeersDrukteResource {
    private static final Logger LOG = LoggerFactory.getLogger(VerkeersDrukteResource.class);

    static final String TRAFFIC_PATH = "/traffic";
    static final String STATIC_PATH = "/static";
    static final String DYNAMIC_PATH = "/dynamic";

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ITrafficHandler handler;
    private final AtomicInteger atomicInteger = new AtomicInteger();

    private final TrafficConfig config;

    VerkeersDrukteResource(ITrafficHandler handler, TrafficConfig config) {
        this.handler = handler;
        this.config = config;
        mapper.findAndRegisterModules();
    }

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

    @GET
    @Path(STATIC_PATH + "/{location}")
    public Optional<FeatureCollection.Feature> getStatic(@PathParam("location") String location) {
        FeatureCollection.Feature feature = handler.getStaticData(location);
        if (feature != null) {
            feature = addUrlProperties(feature);
        }
        return Optional.ofNullable(feature);
    }

    @GET
    @Path(DYNAMIC_PATH + "/{location}")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Optional<MeasurementResult> getDynamic(@PathParam("location") String location) {
        // return snapshot of most recent measurement for location
        AggregateMeasurement aggregateMeasurement = handler.getDynamicData(location);
        if (aggregateMeasurement == null) {
            return Optional.empty();
        }
        return Optional.of(new MeasurementResult(aggregateMeasurement));
    }

    @GET
    @Path(DYNAMIC_PATH + "/{location}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void getTrafficEvents(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam("location") String location) {
        // verify that location exists
        if (handler.getStaticData(location) == null) {
            throw new NotFoundException();
        }

        //  get initial data
        String clientId = "client-" + atomicInteger.incrementAndGet();
        BlockingQueue<AggregateMeasurement> queue = new ArrayBlockingQueue<>(10);
        eventCallback(clientId, queue, location);

        // subscribe to updates
        handler.subscribe(clientId, () -> eventCallback(clientId, queue, location));
        try (sseEventSink) {
            while (!sseEventSink.isClosed()) {
                AggregateMeasurement measurement = queue.poll(5, TimeUnit.SECONDS);
                if (measurement != null) {
                    String id = String.valueOf(measurement.dateTime.getEpochSecond() / 60);
                    String json = mapper.writeValueAsString(new MeasurementResult(measurement));
                    OutboundSseEvent event = sse.newEventBuilder().id(id).name("measurement").data(json).build();
                    sseEventSink.send(event);
                }
            }
        } catch (InterruptedException | JsonProcessingException e) {
            LOG.warn("Error sending SSE: {}", e.getMessage());
        } finally {
            handler.unsubscribe(clientId);
        }
    }

    private void eventCallback(String clientId, Queue<AggregateMeasurement> queue, String location) {
        AggregateMeasurement aggregateMeasurement = handler.getDynamicData(location);
        if (aggregateMeasurement != null) {
            if (!queue.offer(aggregateMeasurement)) {
                LOG.warn("Queue for client '{}' location '{}' got stuck", clientId, location);
            }
        }
    }

    /**
     * JSON serializable representation of {@link AggregateMeasurement}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    final class MeasurementResult {
        @JsonProperty("datetime")
        private final String dateTime;

        @JsonProperty("flow")
        private final Long flow;

        @JsonProperty("speed")
        private final BigDecimal speed;

        MeasurementResult(AggregateMeasurement measurement) {
            OffsetDateTime dateTime = OffsetDateTime.ofInstant(measurement.dateTime, config.getTimeZone());
            this.dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
            this.flow = Double.isFinite(measurement.flow) ? Math.round(measurement.flow) : null;
            this.speed = Double.isFinite(measurement.speed) ? BigDecimal.valueOf(measurement.speed).setScale(1, RoundingMode.HALF_UP) : null;
        }
    }

}
