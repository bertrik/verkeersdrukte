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

import java.util.Optional;

@OpenAPIDefinition(
        info = @Info(
                title = "Verkeersdrukte",
                description = "Provides near real-time speed/intensity traffic data for motorways in the Netherlands,"
                        + " based on open data from Nationaal Dataportaal Wegverkeer ( https://opendata.ndw.nu/ )",
                contact = @Contact(name = "Bertrik Sikken", email = "bertrik@gmail.com")),
        servers = {@Server(url = "https://stofradar.nl"), @Server(url = "http://stofradar.nl:9002")},
        tags = {
                @Tag(name = "static", description = "location data"),
                @Tag(name = "dynamic", description = "traffic data, intensity (vehicles/hour) and speed (km/hour)")
        })
public interface IVerkeersDrukteResource {
    @Operation(summary = "Get GeoJSON containing all locations", tags = {"static"})
    FeatureCollection getStatic();

    @Operation(summary = "Get static data for a specific location", tags = {"static"})
    Optional<FeatureCollection.Feature> getStatic(@PathParam("location") String location);

    @Operation(summary = "Get dynamic traffic data for a specific location", tags = {"dynamic"})
    Optional<VerkeersDrukteResource.DynamicDataJson> getDynamic(@PathParam("location") String location);

    @Operation(summary = "Get event stream with dynamic traffic data for a specific location", tags = {"dynamic"})
    void getTrafficEvents(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam("location") String location);
}
