package nl.bertriksikken.verkeersdrukte.traffic;

import nl.bertriksikken.datex2.VmsPublication;
import nl.bertriksikken.datex2.VmsTablePublication;
import nl.bertriksikken.geojson.FeatureCollection;

public interface ITrafficHandler {

    boolean isHealthy();

    SiteMeasurement getDynamicData(String location);

    FeatureCollection getStaticData();

    FeatureCollection.Feature getStaticData(String location);

    VmsTablePublication getVmsLocationTable();

    VmsPublication getVmsPublication();

    void subscribe(String clientId, INotifyData callback);

    void unsubscribe(String clientId);

    interface INotifyData {
        void notifyUpdate();
    }
}
