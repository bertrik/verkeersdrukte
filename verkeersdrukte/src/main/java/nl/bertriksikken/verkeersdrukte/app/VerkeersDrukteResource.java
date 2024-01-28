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
    public FeatureCollection getStatic() {
        return handler.getStaticData();
    }

    @GET
    @Path("/static/{location}")
    public Optional<FeatureCollection.Feature> getStatic(@PathParam("location") String location) {
        return Optional.ofNullable(handler.getStaticData(location));
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
        try {
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
            sseEventSink.close();
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
            OffsetDateTime dateTime = OffsetDateTime.ofInstant(measurement.dateTime, zoneId);
            this.dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
            this.flow = Double.isFinite(measurement.flow) ? Math.round(measurement.flow) : null;
            this.speed = Double.isFinite(measurement.speed) ? BigDecimal.valueOf(measurement.speed).setScale(1, RoundingMode.HALF_UP) : null;
        }
    }

}
