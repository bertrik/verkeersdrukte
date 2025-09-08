package nl.bertriksikken.verkeersdrukte.app;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.bertriksikken.verkeersdrukte.traffic.ITrafficHandler;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficConfig;

import java.io.InputStream;
import java.util.Objects;

/**
 * Base class for derived traffic/drip resources.
 */
public abstract class BaseResource {

    protected final ITrafficHandler handler;
    protected final TrafficConfig config;

    BaseResource(ITrafficHandler handler, TrafficConfig config) {
        this.handler = Objects.requireNonNull(handler);
        this.config = Objects.requireNonNull(config);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public abstract Response getIndex();

    @GET
    @Path("/favicon.png")
    @Produces("image/png")
    public Response getFavicon() {
        InputStream in = getClass().getResourceAsStream("/assets/verkeersdrukte.png");
        return Response.ok(in, "image/png").build();
    }

}
