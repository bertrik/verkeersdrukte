package nl.bertriksikken.verkeersdrukte.traffic;

public interface ITrafficHandler {


    boolean isHealthy();

    AggregateMeasurement getDynamicData(String location);

     void subscribe(String clientId, INotifyData callback);

     void unsubscribe(String clientId);

    public interface INotifyData {
        void notifyUpdate();
    }
}
