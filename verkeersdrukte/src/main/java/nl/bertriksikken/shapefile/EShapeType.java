package nl.bertriksikken.shapefile;

import java.util.Arrays;

public enum EShapeType {
    Null(0),
    Point(1),
    PolyLine(3);

    private final int value;

    EShapeType(int value) {
        this.value = value;
    }

    public static EShapeType fromValue(int value) {
        return Arrays.stream(values()).filter(v -> v.value == value).findFirst().orElse(null);
    }
 }
