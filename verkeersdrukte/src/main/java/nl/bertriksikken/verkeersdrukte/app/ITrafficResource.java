package nl.bertriksikken.verkeersdrukte.app;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import nl.bertriksikken.geojson.FeatureCollection;

import java.util.Optional;
public interface ITrafficResource {
    Response getIndex();
    FeatureCollection getStatic();
    Optional<FeatureCollection.Feature> getStatic(@PathParam("location") String location);
    Optional<TrafficResource.DynamicDataJson> getDynamic(@PathParam("location") String location);
    void getTrafficEvents(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam("location") String location);
}
