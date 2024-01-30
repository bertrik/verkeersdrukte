package nl.bertriksikken.shapefile;

import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfMetadata;
import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class ShapeFile {

    private static final Logger LOG = LoggerFactory.getLogger(ShapeFile.class);
    private static final int FILE_CODE = 9994;
    private static final int FILE_VERSION = 1000;

    private final List<ShapeRecord> records = new ArrayList<>();

    private double xmin;
    private double ymin;
    private double xmax;
    private double ymax;

    public static ShapeFile read(InputStream shpStream, InputStream dbfStream) throws IOException {
        ShapeFile shapeFile = new ShapeFile();
        try {
            List<ShapeProperties> properties = shapeFile.readDbf(dbfStream);
            shapeFile.readShp(shpStream, properties);
            return shapeFile;
        } catch (BufferUnderflowException e) {
            throw new IOException(e);
        }
    }

    List<ShapeProperties> readDbf(InputStream stream) throws IOException {
        List<ShapeProperties> propertiesList = new ArrayList<>();
        try (DbfReader reader = new DbfReader(stream)) {
            DbfMetadata meta = reader.getMetadata();
            DbfRecord record;
            while ((record = reader.read()) != null) {
                ShapeProperties properties = new ShapeProperties();
                for (DbfField field : meta.getFields()) {
                    String name = field.getName();
                    String value = record.getString(name, StandardCharsets.UTF_8);
                    if (value != null) {
                        properties.put(name, value);
                    }
                }
                propertiesList.add(properties);
            }
        }
        return propertiesList;
    }

    void readShp(InputStream stream, List<ShapeProperties> propertiesList) throws IOException, BufferUnderflowException {
        byte[] data = stream.readAllBytes();
        ByteBuffer bb = ByteBuffer.wrap(data);

        // file header
        bb.order(ByteOrder.BIG_ENDIAN);
        long fileCode = bb.getInt();
        if (fileCode != FILE_CODE) {
            throw new IOException("Unexpected file code: " + fileCode);
        }
        bb.position(bb.position() + 20);
        int fileLength = 2 * bb.getInt();
        if (fileLength > data.length) {
            throw new IOException("Unexpected file length: " + fileLength);
        }

        // shape header
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int fileVersion = bb.getInt();
        if (fileVersion != FILE_VERSION) {
            throw new IOException("Unexpected file version: " + fileVersion);
        }
        int fileShapeType = bb.getInt();
        if (EShapeType.fromValue(fileShapeType) == null) {
            throw new IOException("Unhandled shape type: " + fileShapeType);
        }
        xmin = bb.getDouble();
        ymin = bb.getDouble();
        xmax = bb.getDouble();
        ymax = bb.getDouble();
        if ((xmin > xmax) || (ymin > ymax)) {
            throw new IOException("Invalid bounding box");
        }
        bb.position(bb.position() + 32);

        // shape records
        bb.order(ByteOrder.BIG_ENDIAN);
        int index = 0;
        while (bb.position() < fileLength) {
            int recordNr = bb.getInt();
            int recordLength = 2 * bb.getInt();
            byte[] recordData = new byte[recordLength];
            bb.get(recordData);
            ShapeRecord record = readRecord(recordData);
            if (record != null) {
                // add properties from the DBF file
                ShapeProperties properties = propertiesList.get(index);
                if (properties != null) {
                    properties.forEach(record::addProperty);
                }
                records.add(record);
            }
            index++;
        }
    }

    private ShapeRecord readRecord(byte[] data) {
        ByteBuffer shapeBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        EShapeType shapeType = EShapeType.fromValue(shapeBuffer.getInt());
        switch (shapeType) {
            case Null:
                break;
            case Point:
                return ShapeRecord.Point.parse(shapeBuffer);
            default:
                LOG.info("Unhandled shape type {}", shapeType);
                break;
        }
        return null;
    }

    public List<ShapeRecord> getRecords() {
        return List.copyOf(records);
    }

    private static final class ShapeProperties extends LinkedHashMap<String, Object> {}

}
