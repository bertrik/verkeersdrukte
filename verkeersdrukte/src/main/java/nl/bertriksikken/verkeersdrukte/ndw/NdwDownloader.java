package nl.bertriksikken.verkeersdrukte.ndw;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.glassfish.jersey.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NdwDownloader implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NdwDownloader.class);

    private final File cacheLocation;
    private final NdwClient client;
    private final YAMLMapper yamlMapper = new YAMLMapper();
    private CacheIndex cacheIndex = new CacheIndex();

    public NdwDownloader(NdwConfig config) {
        this.cacheLocation = new File(config.getCacheLocation());
        this.client = NdwClient.create(config);
    }

    public void start() {
        cacheLocation.mkdirs();
        if (!loadCache()) {
            cacheIndex = new CacheIndex();
            saveCache();
        }
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * Fetch a file, either from cache, or freshly downloaded.
     */
    public File fetchFile(String name) {
        File file = new File(cacheLocation, name);

        // download it with e-tag
        CacheEntry cacheEntry = cacheIndex.get(name);
        String etag = file.exists() ? cacheEntry.etag() : "";

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.IF_NONE_MATCH, etag);
        headers.put(HttpHeaders.ACCEPT_ENCODING, "gzip");
        try {
            FileResponse response = client.getFile(name, headers, file);
            switch (response.getCode()) {
                case 200 -> {
                    // update our cache
                    cacheEntry = cacheEntry.update(response.getEtag(), response.getLastModified());
                    cacheIndex.put(name, cacheEntry);
                    saveCache();
                }
                case 304 -> {
                    // we already have the file in our cache, do nothing
                    LOG.info("File '{}' already cached (etag {})", name, etag);
                }
                default -> LOG.warn("Failed to download '{}': {}", name, response);
            }
        } catch (IOException e) {
            LOG.warn("Error processing: {}", e.getMessage());
            return null;
        }
        return file;
    }

    private boolean loadCache() {
        File cacheFile = new File(cacheLocation, "index.yaml");
        try (FileInputStream fis = new FileInputStream(cacheFile)) {
            cacheIndex = yamlMapper.readValue(fis, CacheIndex.class);
            return true;
        } catch (IOException e) {
            LOG.warn("Failed to load cache at {}", cacheFile.getAbsolutePath());
        }
        return false;
    }

    private void saveCache() {
        File cacheFile = new File(cacheLocation, "index.yaml");
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            yamlMapper.writeValue(fos, cacheIndex);
        } catch (IOException e) {
            LOG.warn("Failed to save cache at {}", cacheFile.getAbsolutePath());
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
    static class CacheIndex {
        @JsonProperty("entries")
        private final Map<String, CacheEntry> entries = new LinkedHashMap<>();

        CacheEntry get(String name) {
            return entries.getOrDefault(name, new CacheEntry("", ""));
        }

        void put(String name, CacheEntry entry) {
            entries.put(name, entry);
        }
    }

    record CacheEntry(String etag, String lastModified) {
        public CacheEntry update(String etag, Instant lastModified) {
            ZonedDateTime utcTime = ZonedDateTime.ofInstant(lastModified, ZoneId.of("UTC"));
            String dateTime = DateTimeFormatter.RFC_1123_DATE_TIME.format(utcTime);
            return new CacheEntry(etag, dateTime);
        }
    }

}
