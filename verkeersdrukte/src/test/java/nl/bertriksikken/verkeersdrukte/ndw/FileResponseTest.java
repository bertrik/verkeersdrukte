package nl.bertriksikken.verkeersdrukte.ndw;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class FileResponseTest {

    @Test
    public void test() {
        byte[] contents = new byte[]{1, 2, 3};
        Map<String, List<String>> headers = Map.of("Last-Modified", List.of("Tue, 3 Jun 2008 11:05:30 GMT"));
        FileResponse response = FileResponse.create(200, headers, contents);

        Assert.assertArrayEquals(contents, response.getContents());
        Instant lastModified = response.getLastModified();
        Assert.assertNotNull(lastModified);
    }

}
