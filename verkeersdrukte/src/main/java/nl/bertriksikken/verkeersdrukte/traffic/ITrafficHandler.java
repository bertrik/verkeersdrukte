package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.datex2v3.VmsPayload;
import nl.bertriksikken.geojson.FeatureCollection;

public interface ITrafficHandler {

    boolean isHealthy();

    SiteMeasurement getDynamicData(String location);

    FeatureCollection getStaticData();

    FeatureCollection.Feature getStaticData(String location);

    VmsPayload getVmsPayload();

    void subscribe(String clientId, INotifyData callback);

    void unsubscribe(String clientId);

    interface INotifyData {
        void notifyUpdate();
    }
}
