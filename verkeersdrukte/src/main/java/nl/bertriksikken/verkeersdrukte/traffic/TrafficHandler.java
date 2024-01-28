package nl.bertriksikken.verkeersdrukte.traffic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.util.concurrent.Runnables;
import io.dropwizard.lifecycle.Managed;
import nl.bertriksikken.datex2.D2LogicalModel;
import nl.bertriksikken.datex2.MeasuredValue;
import nl.bertriksikken.datex2.SiteMeasurements;
import nl.bertriksikken.geojson.FeatureCollection;
import nl.bertriksikken.shapefile.EShapeType;
import nl.bertriksikken.shapefile.ShapeFile;
import nl.bertriksikken.shapefile.ShapeRecord;
import nl.bertriksikken.verkeersdrukte.ndw.FileResponse;
import nl.bertriksikken.verkeersdrukte.ndw.NdwClient;
import nl.bertriksikken.verkeersdrukte.ndw.NdwConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

public final class TrafficHandler implements ITrafficHandler, Managed {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficHandler.class);

    private final Map<String, INotifyData> subscriptions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper xmlMapper = new XmlMapper();
    private final NdwClient ndwClient;
    private MeasurementCache measurementCache = new MeasurementCache(Instant.now());
    private FeatureCollection shapeFile;

    public TrafficHandler(NdwConfig config) {
        ndwClient = NdwClient.create(config);
        xmlMapper.findAndRegisterModules();
    }

    @Override
    public void start() throws IOException {
        // read the shape file
        LOG.info("Reading shape file ...");
        InputStream shpStream = getClass().getClassLoader().getResourceAsStream("shapefile/Telpunten_WGS84.shp");
        InputStream dbfStream = getClass().getClassLoader().getResourceAsStream("shapefile/Telpunten_WGS84.dbf");
        shapeFile = readShapeFile(shpStream, dbfStream);

        // schedule regular fetches, starting immediately
        LOG.info("Schedule download ...");
        executor.schedule(this::downloadTrafficSpeed, 0, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warn("Executor did not terminate");
            }
        } catch (InterruptedException e) {
            LOG.warn("Error stopping executor", e);
        }
    }

    private void downloadTrafficSpeed() {
        LOG.info("Download traffic/speed");
        Instant next;
        try {
            FileResponse response = ndwClient.getTrafficSpeed();
            Duration age = Duration.between(response.getLastModified(), Instant.now());
            next = response.getLastModified().plusSeconds(65);
            LOG.info("Got data, {} bytes, age {}", response.getContents().length, age);
            measurementCache = decode(new ByteArrayInputStream(response.getContents()));
        } catch (IOException e) {
            LOG.warn("Download failed", e);
            next = Instant.now().plusSeconds(60);
        }

        // schedule next
        Duration interval = Duration.between(Instant.now(), next);
        while (interval.isNegative()) {
            interval = interval.plusSeconds(60);
        }
        LOG.info("Scheduling next download in {}", interval);
        executor.schedule(this::downloadTrafficSpeed, interval.toMillis(), TimeUnit.MILLISECONDS);

        notifyClients();
    }

    private MeasurementCache decode(ByteArrayInputStream inputStream) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            JsonNode node = xmlMapper.readValue(gzis, JsonNode.class);
            JsonNode d2LogicalModel = node.at("/Body/d2LogicalModel");
            D2LogicalModel model = xmlMapper.treeToValue(d2LogicalModel, D2LogicalModel.class);
            D2LogicalModel.PayloadPublication payloadPublication = model.payloadPublication;
            LOG.info("Payload publication: type {}, time {}", payloadPublication.type, payloadPublication.publicationTime);
            D2LogicalModel.MeasuredDataPublication measuredDataPublication = (D2LogicalModel.MeasuredDataPublication) payloadPublication;
            Instant publicationTime = Instant.parse(measuredDataPublication.publicationTime);
            MeasurementCache snapshot = new MeasurementCache(publicationTime);
            for (SiteMeasurements measurements : measuredDataPublication.siteMeasurementsList) {
                AggregateMeasurement aggregateMeasurement = aggregateValues(measurements);
                snapshot.put(measurements.reference.id, aggregateMeasurement);
            }
            return snapshot;
        }
    }


    private AggregateMeasurement aggregateValues(SiteMeasurements measurements) {
        Instant dateTime = Instant.parse(measurements.measurementTimeDefault);
        // group by type
        List<MeasuredValue.TrafficFlow> flows = new ArrayList<>();
        List<MeasuredValue.TrafficSpeed> speeds = new ArrayList<>();
        for (MeasuredValue value : measurements.measuredValueList) {
            switch (value.measuredValue.basicData.type) {
                case MeasuredValue.TrafficFlow.TYPE:
                    flows.add((MeasuredValue.TrafficFlow) value.measuredValue.basicData);
                    break;
                case MeasuredValue.TrafficSpeed.TYPE:
                    speeds.add((MeasuredValue.TrafficSpeed) value.measuredValue.basicData);
                    break;
                default:
                    break;
            }
        }
        if (flows.isEmpty() || (flows.size() != speeds.size())) {
            // cannot determine speed
            return new AggregateMeasurement(dateTime, Double.NaN, Double.NaN);
        }

        // aggregate flow as simple sum, speed as flow-weighted sum
        double sumFlowSpeed = 0.0;
        double sumFlow = 0.0;
        for (int i = 0; i < flows.size(); i++) {
            MeasuredValue.TrafficFlow flow = flows.get(i);
            MeasuredValue.TrafficSpeed speed = speeds.get(i);
            double flowValue = flow.vehicleFlow.dataError ? Double.NaN : flow.vehicleFlow.vehicleFlowRate;
            double speedValue = speed.averageVehicleSpeed.dataError ? Double.NaN : speed.averageVehicleSpeed.speed;
            sumFlowSpeed += flowValue * speedValue;
            sumFlow += flowValue;
        }
        double aggregateSpeed = sumFlowSpeed / sumFlow;
        return new AggregateMeasurement(dateTime, sumFlow, aggregateSpeed);
    }

    @Override
    public AggregateMeasurement getDynamicData(String location) {
        return measurementCache.get(location);
    }

    @Override
    public FeatureCollection getStaticData() {
        return shapeFile;
    }

    @Override
    public FeatureCollection.Feature getStaticData(String location) {
        for (FeatureCollection.Feature feature : shapeFile.getFeatures()) {
            String dlgLoc = feature.getProperties().get("dgl_loc").toString();
            if (location.equals(dlgLoc)) {
                return feature;
            }
        }
        // not found
        return null;
    }

    @Override
    public void subscribe(String clientId, INotifyData callback) {
        LOG.info("Subscribe: {}", clientId);
        subscriptions.put(clientId, callback);
    }

    @Override
    public void unsubscribe(String clientId) {
        LOG.info("Unsubscribe: {}", clientId);
        subscriptions.remove(clientId);
    }

    @Override
    public boolean isHealthy() {
        // try to submit something to the executor, and see if it responds
        try {
            executor.submit(Runnables::doNothing).get(3, TimeUnit.SECONDS);
        } catch (RejectedExecutionException | InterruptedException | TimeoutException | ExecutionException e) {
            return false;
        }
        return true;
    }

    private void notifyClients() {
        List.copyOf(subscriptions.values()).forEach(INotifyData::notifyUpdate);
    }

    private FeatureCollection readShapeFile(InputStream shpStream, InputStream dbfStream) throws IOException {
        ShapeFile shapeFile = ShapeFile.read(shpStream, dbfStream);
        FeatureCollection collection = new FeatureCollection();
        for (ShapeRecord record : shapeFile.getRecords()) {
            if (record.getType() == EShapeType.Point) {
                ShapeRecord.Point point = (ShapeRecord.Point) record;
                FeatureCollection.GeoJsonGeometry geometry = new FeatureCollection.PointGeometry(point.y, point.x);
                FeatureCollection.Feature feature = new FeatureCollection.Feature(geometry);
                record.getProperties().forEach(feature::addProperty);
                collection.add(feature);
            }
        }
        return collection;
    }
}
