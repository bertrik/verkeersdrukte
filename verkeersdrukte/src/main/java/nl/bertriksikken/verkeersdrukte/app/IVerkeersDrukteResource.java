package nl.bertriksikken.verkeersdrukte.app;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import nl.bertriksikken.geojson.FeatureCollection;

import java.net.URISyntaxException;
import java.util.Optional;

@OpenAPIDefinition(
        info = @Info(
                title = "Verkeersdrukte",
                description = "Provides near real-time speed/intensity data for motorways in the Netherlands",
                contact = @Contact(name = "Bertrik Sikken", email = "bertrik@gmail.com")),
        servers = {@Server(url = "https://stofradar.nl"), @Server(url = "http://stofradar.nl:9002")},
        tags = {@Tag(name = "static"), @Tag(name = "dynamic")})
public interface IVerkeersDrukteResource {
    @Operation(hidden = true)
    void redirectSwagger() throws URISyntaxException;

    @Operation(summary = "Get GeoJSON containing all locations")
    @Tag(name = "static")
    FeatureCollection getStatic();

    @Operation(summary = "Get static data for a specific location")
    @Tag(name = "static")
    Optional<FeatureCollection.Feature> getStatic(@PathParam("location") String location);

    @Operation(summary = "Get dynamic traffic data for a specific location")
    @Tag(name = "dynamic")
    Optional<VerkeersDrukteResource.MeasurementResult> getDynamic(@PathParam("location") String location);

    @Operation(summary = "Get event stream with dynamic traffic data for a specific location")
    @Tag(name = "dynamic")
    void getTrafficEvents(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam("location") String location);
}
