package nl.bertriksikken.verkeersdrukte.app;

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
import nl.bertriksikken.verkeersdrukte.traffic.MeasurementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Path("/traffic")
@Produces(MediaType.APPLICATION_JSON)
public final class VerkeersDrukteResource {
    private static final Logger LOG = LoggerFactory.getLogger(VerkeersDrukteResource.class);

    private final ITrafficHandler handler;

    VerkeersDrukteResource(ITrafficHandler handler) {
        this.handler = handler;
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
    public Optional<MeasurementResult> getDynamic(@PathParam("location") String location) {
        // return snapshot of most recent measurement for location
        AggregateMeasurement aggregateMeasurement = handler.getDynamicData(location);
        if (aggregateMeasurement == null) {
            return Optional.empty();
        }
        return Optional.of(new MeasurementResult(location, aggregateMeasurement));
    }

    @GET
    @Path("/dynamic/{location}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void getTraffic(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam("location") String location) {
        try {
            int count = 0;
            while (true) {
                String element = String.valueOf(count);
                OutboundSseEvent event = sse.newEvent(element);
                sseEventSink.send(event);
                Thread.sleep(2000);
                count++;
            }
        } catch (InterruptedException e) {
            LOG.info("subscription interrupted");
        }
    }

}
