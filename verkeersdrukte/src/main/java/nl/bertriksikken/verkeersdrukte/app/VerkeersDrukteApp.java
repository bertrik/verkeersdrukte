package nl.bertriksikken.verkeersdrukte.app;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import nl.bertriksikken.verkeersdrukte.traffic.TrafficHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public final class VerkeersDrukteApp extends Application<VerkeersDrukteAppConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(VerkeersDrukteApp.class);
    private static final String CONFIG_FILE = "configuration.yaml";
    private Map<String, String> headers = Map.of();

    private VerkeersDrukteApp() {
    }

    @Override
    public void initialize(Bootstrap<VerkeersDrukteAppConfig> bootstrap) {
        bootstrap.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        bootstrap.addBundle(new TrafficSwaggerBundle(TrafficResource.class.getPackage().getName()));
    }

    @Override
    public void run(VerkeersDrukteAppConfig configuration, Environment environment) {
        headers = configuration.getHeaders();

        TrafficHandler ndwHandler = new TrafficHandler(configuration);
        TrafficResource trafficResource = new TrafficResource(ndwHandler, configuration.getTrafficConfig());
        environment.healthChecks().register("ndw", new VerkeersDrukteHealthCheck(ndwHandler));
        environment.jersey().register(trafficResource);
        environment.lifecycle().manage(ndwHandler);

        DripResource dripResource = new DripResource(ndwHandler, configuration.getTrafficConfig());
        environment.jersey().register(dripResource);

        // Add headers to each response
        environment.jersey().register((ContainerResponseFilter) this::addHeaders);
    }

    private void addHeaders(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        headers.forEach((header, value) -> responseContext.getHeaders().add(header, value));
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

    private static final class TrafficSwaggerBundle extends SwaggerBundle<Configuration> {
        private final SwaggerBundleConfiguration configuration = new SwaggerBundleConfiguration();

        TrafficSwaggerBundle(String resourcePackage) {
            this.configuration.setResourcePackage(resourcePackage);
        }

        @Override
        protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(Configuration configuration) {
            return this.configuration;
        }
    }

}
