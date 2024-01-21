package nl.bertriksikken.datex2;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
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
    String type = null;

    @JacksonXmlProperty(localName = "measuredValue")
    MeasuredValueWrapper measuredValue;

    MeasuredValue() {
        // jackson constructor
    }

    public MeasuredValue(int index, BasicData basicData) {
        this.index = index;
        this.measuredValue = new MeasuredValueWrapper(basicData);
    }

    @Override
    public String toString() {
        return measuredValue.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MeasuredValueWrapper {
        @JacksonXmlProperty(localName = "basicData")
        BasicData basicData;

        MeasuredValueWrapper() {
            // jackson constructor
            this(null);
        }

        MeasuredValueWrapper(BasicData basicData) {
            this.basicData = basicData;
        }

        @Override
        public String toString() {
            return basicData.toString();
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({@Type(value = TrafficFlow.class, name = "TrafficFlow"), @Type(value = TrafficSpeed.class, name = "TrafficSpeed")})
    public static abstract class BasicData {
        String type;

        @JsonCreator
        public BasicData(String type) {
            this.type = type;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(Include.NON_NULL)
    public static abstract class BasicDataValue {
        @JacksonXmlProperty(localName = "dataError")
        Boolean dataError;
        @JacksonXmlProperty(localName = "numberOfInputValuesUsed", isAttribute = true)
        int numberOfInputValuesUsed;

        BasicDataValue(int numberOfInputValuesUsed) {
            this.numberOfInputValuesUsed = numberOfInputValuesUsed;
        }
    }

    public static final class TrafficFlow extends BasicData {
        @JacksonXmlProperty(localName = "vehicleFlow")
        VehicleFlow vehicleFlow;

        TrafficFlow() {
            // jackson constructor
            this(0, 0);
        }

        TrafficFlow(int rate, int numberOfInputValuesUsed) {
            super("TrafficFlow");
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
            int vehicleFlowRate;

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

    public static final class TrafficSpeed extends BasicData {
        @JacksonXmlProperty(localName = "averageVehicleSpeed")
        AverageVehicleSpeed averageVehicleSpeed;

        TrafficSpeed() {
            // jackson constructor
            this(-1, 0);
        }

        TrafficSpeed(int speed, int numberOfInputValuesUsed) {
            super("TrafficSpeed");
            this.averageVehicleSpeed = new AverageVehicleSpeed(speed, numberOfInputValuesUsed);
        }

        @Override
        public String toString() {
            return averageVehicleSpeed.toString();
        }

        public static final class AverageVehicleSpeed extends BasicDataValue {
            @JacksonXmlProperty(localName = "speed")
            Double speed;

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
