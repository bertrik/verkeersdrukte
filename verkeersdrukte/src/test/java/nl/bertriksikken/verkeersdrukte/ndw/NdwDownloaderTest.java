package nl.bertriksikken.verkeersdrukte.ndw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class NdwDownloaderTest {

    @Test
    public void testSerialize() throws JsonProcessingException {
        NdwDownloader.CacheIndex index = new NdwDownloader.CacheIndex();
        NdwDownloader.CacheEntry entry = new NdwDownloader.CacheEntry("etag", "");
        index.put("name", entry);
        index.put("name", entry.update("nieuw", Instant.now()));
        YAMLMapper mapper = new YAMLMapper();
        String yaml = mapper.writeValueAsString(index);
        System.out.println(yaml);
    }

    @Test
    public void testDeserialize() throws IOException {
        InputStream is = getClass().getResourceAsStream("/index.yaml");
        YAMLMapper mapper = new YAMLMapper();
        NdwDownloader.CacheIndex index = mapper.readValue(is, NdwDownloader.CacheIndex.class);
        assertNotNull(index);
    }
}
