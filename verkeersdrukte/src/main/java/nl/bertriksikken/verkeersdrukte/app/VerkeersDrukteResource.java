package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.caching.CacheControl;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import nl.bertriksikken.verkeersdrukte.traffic.AggregateMeasurement;
import nl.bertriksikken.verkeersdrukte.traffic.ITrafficHandler;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/traffic")
@Produces(MediaType.APPLICATION_JSON)
public final class VerkeersDrukteResource {
    private static final Logger LOG = LoggerFactory.getLogger(VerkeersDrukteResource.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ITrafficHandler handler;
    private final AtomicInteger atomicInteger = new AtomicInteger();

    private final ZoneId zoneId;

    VerkeersDrukteResource(ITrafficHandler handler, TrafficConfig config) {
        this.handler = handler;
        mapper.findAndRegisterModules();
        zoneId = config.getTimeZone();
    }

    @GET
    @Path("/static")
    public String getStatic() {
        // return the entire shape file, FeatureCollection
        LOG.info("getStatic()");
        return "static";
    }

    @GET
    @Path("/static/{location}")
    public String getStatic(@PathParam("location") String location) {
        // return part of the shape file, a single Feature
        LOG.info("getStatic() for location {}", location);
        return location;
    }

    @GET
    @Path("/dynamic/{location}")
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
    @Path("/dynamic/{location}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void getTrafficEvents(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam("location") String location) {
        // attempt to get initial data
        AggregateMeasurement measurement = handler.getDynamicData(location);
        if (measurement == null) {
            sseEventSink.close();
            return;
        }

        String clientId = "client-" + atomicInteger.incrementAndGet();
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        handler.subscribe(clientId, () -> eventCallback(clientId, queue, location));
        eventCallback(clientId, queue, location);
        try {
            while (!sseEventSink.isClosed()) {
                String value = queue.take();
                OutboundSseEvent event = sse.newEvent("measurement", value);
                sseEventSink.send(event);
            }
        } catch (InterruptedException e) {
            LOG.info("subscription interrupted");
        } finally {
            handler.unsubscribe(clientId);
        }
    }

    private void eventCallback(String clientId, Queue<String> queue, String location) {
        Optional<MeasurementResult> dynamic = getDynamic(location);
        try {
            if (dynamic.isPresent()) {
                String json = mapper.writeValueAsString(dynamic.get());
                queue.offer(json);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Caught exception", e);
            handler.unsubscribe(clientId);
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
        private final BigDecimal flow;

        @JsonProperty("speed")
        private final BigDecimal speed;

        MeasurementResult(AggregateMeasurement measurement) {
            OffsetDateTime dateTime = OffsetDateTime.ofInstant(measurement.dateTime, zoneId);
            this.dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
            this.flow = Double.isFinite(measurement.flow) ? BigDecimal.valueOf(measurement.flow).setScale(1, RoundingMode.HALF_UP) : null;
            this.speed = Double.isFinite(measurement.speed) ? BigDecimal.valueOf(measurement.speed).setScale(1, RoundingMode.HALF_UP) : null;
        }
    }

}
