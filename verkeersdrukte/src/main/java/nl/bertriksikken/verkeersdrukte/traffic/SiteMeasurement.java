package nl.bertriksikken.verkeersdrukte.traffic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal representation of flow/speed measurements on a site.
 */
public final class SiteMeasurement {

    private final Instant dateTime;
    private final List<LaneMeasurement> lanes = new ArrayList<>();

    public SiteMeasurement(Instant dateTime) {
        this.dateTime = dateTime;
    }

    public Instant getDateTime() {
        return dateTime;
    }

    public void addLaneMeasurement(String id, double flow, double speed) {
        lanes.add(new LaneMeasurement(id, flow, speed));
    }

    public List<LaneMeasurement> getLanes() {
        return List.copyOf(lanes);
    }

    public LaneMeasurement aggregate() {
        double sumFlowSpeed = 0.0;
        double sumFlow = 0.0;
        for (LaneMeasurement lane : lanes) {
            if (lane.flow > 0) {
                sumFlowSpeed += lane.flow * lane.speed;
                sumFlow += lane.flow;
            }
        }
        double averageSpeed = sumFlowSpeed / sumFlow;
        return new LaneMeasurement("total", sumFlow, averageSpeed);
    }

    public record LaneMeasurement(String id, double flow, double speed) {
    }

}
