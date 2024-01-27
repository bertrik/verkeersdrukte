package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.geojson.FeatureCollection;

public interface ITrafficHandler {

    boolean isHealthy();

    AggregateMeasurement getDynamicData(String location);

    FeatureCollection getStaticData();

    FeatureCollection.Feature getStaticData(String location);

    void subscribe(String clientId, INotifyData callback);

    void unsubscribe(String clientId);

    interface INotifyData {
        void notifyUpdate();
    }
}
