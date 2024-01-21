package nl.bertriksikken.verkeersdrukte.traffic;

public interface ITrafficHandler {

    public void subscribe(String clientId, INotifyData callback);

    public void unsubscribe(String clientId);

    boolean isHealthy();

    public interface INotifyData {
        void notifyUpdate();
    }
}
