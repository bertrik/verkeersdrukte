package nl.bertriksikken.shapefile;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class ShapeRecord {

    private final EShapeType type;
    private final Map<String, Object> properties = new HashMap<>();

    ShapeRecord(EShapeType type) {
        this.type = type;
    }

    public void addProperty(String name, Object value) {
        properties.put(name, value);
    }

    public Map<String, Object> getProperties() {
        return Map.copyOf(properties);
    }

    public EShapeType getType() {
        return type;
    }

    public static class Point extends ShapeRecord {

        public final double x;
        public final double y;

        Point(double x, double y) {
            super(EShapeType.Point);
            this.x = x;
            this.y = y;
        }

        public static Point parse(ByteBuffer bb) {
            double x = bb.getDouble();
            double y = bb.getDouble();
            return new Point(x, y);
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "x=%.6f,y=%.6f", x, y);
        }
    }
}
