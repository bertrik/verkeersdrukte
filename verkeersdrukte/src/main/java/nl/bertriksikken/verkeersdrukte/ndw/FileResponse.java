package nl.bertriksikken.verkeersdrukte.ndw;

import com.google.common.collect.Iterables;
import jakarta.ws.rs.core.HttpHeaders;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FileResponse {

    private final int code;
    private final byte[] contents;
    private final Map<String, List<String>> headers;

    FileResponse(int code, Map<String, List<String>> headers, byte[] contents) {
        this.code = code;
        this.headers = headers; // do not copy, original map has special case-insensitive properties
        this.contents = contents.clone();
    }

    public static FileResponse create(int code, Map<String, List<String>> headers, byte[] contents) {
        return new FileResponse(code, headers, contents);
    }

    public int getCode() {
        return code;
    }

    public byte[] getContents() {
        return contents;
    }

    public Instant getLastModified() {
        String lastModified = Iterables.getFirst(headers.get(HttpHeaders.LAST_MODIFIED), "");
        return DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModified, Instant::from);
    }

    public String getEtag() {
        List<String> values = headers.getOrDefault(HttpHeaders.ETAG, List.of());
        return Iterables.getFirst(values, "");
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "{code=%s,contents=%d bytes,headers=%s}", code, contents.length, headers);
    }
}