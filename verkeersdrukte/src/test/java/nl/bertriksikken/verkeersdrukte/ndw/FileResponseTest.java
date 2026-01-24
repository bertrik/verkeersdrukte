package nl.bertriksikken.verkeersdrukte.ndw;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class FileResponseTest {

    @Test
    public void test() {
        byte[] contents = new byte[]{1, 2, 3};
        Map<String, List<String>> headers = Map.of("Last-Modified", List.of("Tue, 3 Jun 2008 11:05:30 GMT"));
        FileResponse response = FileResponse.withBody(200, headers, contents);

        Assertions.assertArrayEquals(contents, response.getContents());
        Instant lastModified = response.getLastModified();
        Assertions.assertNotNull(lastModified);
    }

}
