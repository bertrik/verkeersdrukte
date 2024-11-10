package nl.bertriksikken.datex2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Locale;

/**
 * Data structure for a measured value, contains only parts required for
 * parsing.
 */
@JacksonXmlRootElement(localName = "measuredValue")
public final class MeasuredValue {

    @JacksonXmlProperty(localName = "index", isAttribute = true)
    int index;

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    String type = ""; // sometimes "_SiteMeasurementsIndexMeasuredValue"

    @JacksonXmlProperty(localName = "measuredValue")
    public MeasuredValueWrapper measuredValue;

    MeasuredValue() {
        // jackson constructor
    }

    public MeasuredValue(int index, BasicData basicData) {
        this.index = index;
        this.measuredValue = new MeasuredValueWrapper(basicData);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%d]=%s", index, measuredValue);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeasuredValueWrapper(@JacksonXmlProperty(localName = "basicData") BasicData basicData) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(value = TrafficFlow.class, name = TrafficFlow.TYPE), @JsonSubTypes.Type(value = TrafficSpeed.class, name = TrafficSpeed.TYPE)})
    public static abstract class BasicData {
        @JacksonXmlProperty(localName = "type", isAttribute = true)
        public final String type;

        @JsonCreator
        BasicData(String type) {
            this.type = type;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(Include.NON_NULL)
    public static abstract class BasicDataValue {
        @JacksonXmlProperty(localName = "dataError")
        public boolean dataError = false;
        @JacksonXmlProperty(localName = "numberOfInputValuesUsed", isAttribute = true)
        int numberOfInputValuesUsed;

        BasicDataValue(int numberOfInputValuesUsed) {
            this.numberOfInputValuesUsed = numberOfInputValuesUsed;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TrafficFlow extends BasicData {
        public static final String TYPE = "TrafficFlow";
        @JacksonXmlProperty(localName = "vehicleFlow")
        public VehicleFlow vehicleFlow;

        TrafficFlow() {
            // jackson constructor
            this(0, 0);
        }

        TrafficFlow(int rate, int numberOfInputValuesUsed) {
            super(TYPE);
            this.vehicleFlow = new VehicleFlow(rate, numberOfInputValuesUsed);
        }

        @Override
        public String toString() {
            return vehicleFlow.toString();
        }

        /**
         * Data structure containing only basic fields.
         */
        public static final class VehicleFlow extends BasicDataValue {
            @JacksonXmlProperty(localName = "vehicleFlowRate")
            public int vehicleFlowRate;

            VehicleFlow() {
                // jackson constructor
                this(0, 0);
            }

            VehicleFlow(int rate, int numberOfInputValuesUsed) {
                super(numberOfInputValuesUsed);
                this.vehicleFlowRate = rate;
            }

            @Override
            public String toString() {
                return String.format(Locale.ROOT, "VehicleFlowRate=%d", vehicleFlowRate);
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TrafficSpeed extends BasicData {
        public static final String TYPE = "TrafficSpeed";
        @JacksonXmlProperty(localName = "averageVehicleSpeed")
        public AverageVehicleSpeed averageVehicleSpeed;

        TrafficSpeed() {
            // jackson constructor
            this(-1, 0);
        }

        TrafficSpeed(int speed, int numberOfInputValuesUsed) {
            super(TYPE);
            this.averageVehicleSpeed = new AverageVehicleSpeed(speed, numberOfInputValuesUsed);
        }

        @Override
        public String toString() {
            return averageVehicleSpeed.toString();
        }

        public static final class AverageVehicleSpeed extends BasicDataValue {
            @JacksonXmlProperty(localName = "speed")
            public final Double speed;

            AverageVehicleSpeed() {
                // jackson constructor
                this(-1, 0);
            }

            AverageVehicleSpeed(double speed, int numberOfInputValuesUsed) {
                super(numberOfInputValuesUsed);
                this.speed = speed;
            }

            @Override
            public String toString() {
                return String.format(Locale.ROOT, "AverageVehicleSpeed=%.1f", speed);
            }
        }
    }

}
