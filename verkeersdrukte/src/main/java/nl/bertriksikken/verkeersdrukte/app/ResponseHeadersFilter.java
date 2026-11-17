package nl.bertriksikken.verkeersdrukte.app;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Adds configured headers and cache policy to every REST response.
 */
@Provider
public class ResponseHeadersFilter implements ContainerResponseFilter {
    private final VerkeersDrukteAppConfig config;

    @Inject
    public ResponseHeadersFilter(VerkeersDrukteAppConfig config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        config.headers().forEach(response.getHeaders()::putSingle);
        String path = request.getUriInfo().getPath();
        if (path.contains("/dynamic/") && path.endsWith("/events")) {
            response.getHeaders().putSingle("Cache-Control", "no-cache");
        } else if (path.contains("/dynamic/")) {
            response.getHeaders().putSingle("Cache-Control", "max-age=60");
        }
    }
}
