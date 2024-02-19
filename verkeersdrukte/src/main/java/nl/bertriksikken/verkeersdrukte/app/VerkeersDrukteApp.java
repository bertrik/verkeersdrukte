package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class VerkeersDrukteApp extends Application<VerkeersDrukteAppConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(VerkeersDrukteApp.class);
    private static final String CONFIG_FILE = "configuration.yaml";

    private VerkeersDrukteApp() {
    }

    @Override
    public void initialize(Bootstrap<VerkeersDrukteAppConfig> bootstrap) {
        bootstrap.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        bootstrap.addBundle(new AssetsBundle("/assets/verkeersdrukte.png", "/favicon.ico"));
    }

    @Override
    public void run(VerkeersDrukteAppConfig configuration, Environment environment) {
        TrafficHandler ndwHandler = new TrafficHandler(configuration.getNdwConfig());
        VerkeersDrukteResource resource = new VerkeersDrukteResource(ndwHandler, configuration.getTrafficConfig());
        environment.healthChecks().register("ndw", new VerkeersDrukteHealthCheck(ndwHandler));
        environment.jersey().register(resource);
        environment.lifecycle().manage(ndwHandler);

        // Add CORS header to each response
        environment.jersey().register((ContainerResponseFilter)this::addCorsHeader);
    }

    private void addCorsHeader(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
    }

    public static void main(String[] args) throws Exception {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            LOG.info("Config file not found, creating default");
            YAMLMapper mapper = new YAMLMapper();
            mapper.findAndRegisterModules();
            VerkeersDrukteAppConfig config = new VerkeersDrukteAppConfig();
            mapper.writeValue(configFile, config);
        }

        VerkeersDrukteApp app = new VerkeersDrukteApp();
        app.run("server", CONFIG_FILE);
    }

}
