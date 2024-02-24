package nl.bertriksikken.verkeersdrukte.ndw;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class FileResponse {

    private final byte[] contents;
    private final Instant lastModified;

    FileResponse(byte[] contents, Instant lastModified) {
        this.contents = contents;
        this.lastModified = lastModified;
    }

    public static FileResponse create(byte[] contents, String lastModified) {
        Instant date = DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModified, Instant::from);
        return new FileResponse(contents, date);
    }

    public static FileResponse empty() {
        return new FileResponse(new byte[0], Instant.now());
    }

    public byte[] getContents() {
        return contents;
    }

    public Instant getLastModified() {
        return lastModified;
    }

}