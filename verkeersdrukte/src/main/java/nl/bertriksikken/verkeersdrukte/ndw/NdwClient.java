package nl.bertriksikken.verkeersdrukte.ndw;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.eclipse.jetty.http.HttpHeader;
import org.glassfish.jersey.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

public final class NdwClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NdwClient.class);
    private static final String USER_AGENT = "github.com/bertrik/verkeersdrukte";

    private final OkHttpClient httpClient;
    private final INdwApi trafficApi;
    private final INdwMapsApi mapsApi;

    NdwClient(OkHttpClient httpClient, INdwApi trafficApi, INdwMapsApi mapsApi) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.trafficApi = Objects.requireNonNull(trafficApi);
        this.mapsApi = Objects.requireNonNull(mapsApi);
    }

    public static NdwClient create(NdwConfig config) {
        LOG.info("Creating new REST client for URL '{}' with timeout {}", config.getTrafficUrl(), config.getTimeout());
        OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(NdwClient::addUserAgent).connectTimeout(config.getTimeout())
                .readTimeout(config.getTimeout()).build();
        Retrofit.Builder retrofit = new Retrofit.Builder().baseUrl(config.getTrafficUrl())
                .addConverterFactory(ScalarsConverterFactory.create()).client(client);
        INdwApi trafficApi = retrofit.baseUrl(config.getTrafficUrl()).build().create(INdwApi.class);
        INdwMapsApi mapsApi = retrofit.baseUrl(config.getMapsUrl()).build().create(INdwMapsApi.class);
        return new NdwClient(client, trafficApi, mapsApi);
    }

    private static okhttp3.Response addUserAgent(Interceptor.Chain chain) throws IOException {
        Request userAgentRequest = chain.request().newBuilder().header(HttpHeader.USER_AGENT.asString(), USER_AGENT).build();
        return chain.proceed(userAgentRequest);
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    public FileResponse getVmsPayload() throws IOException {
        Map<String, String> headers = Map.of(HttpHeaders.ACCEPT_ENCODING, "gzip");
        return getFile(INdwApi.VMS_PAYLOAD, headers);
    }

    public FileResponse getTrafficSpeed() throws IOException {
        Map<String, String> headers = Map.of(HttpHeaders.ACCEPT_ENCODING, "gzip");
        return getFile(INdwApi.TRAFFIC_SPEED_XML_GZ, headers);
    }

    public FileResponse getMeasurementSiteTable(String etag) throws IOException {
        Map<String, String> headers = Map.of(HttpHeaders.IF_NONE_MATCH, etag);
        return getFile(INdwApi.MEASUREMENT_SITE_TABLE, headers);
    }

    FileResponse getFile(String name, Map<String, String> headers) throws IOException {
        Response<ResponseBody> response = trafficApi.downloadFile(name, headers).execute();
        if (response.isSuccessful()) {
            try (ResponseBody body = response.body()) {
                return FileResponse.withBody(response.code(), response.headers().toMultimap(), body.bytes());
            }
        } else {
            if (response.code() > 400) {
                LOG.warn("getFile('{}') failed, code {}: '{}'", name, response.code(), response.message());
            }
            return FileResponse.create(response.code(), response.headers().toMultimap());
        }
    }

    /**
     * Fetches the remote file, streams it to disk, returns response with http response code and headers
     */
    FileResponse getFile(String name, Map<String, String> headers, File file) throws IOException {
        Response<ResponseBody> response = trafficApi.downloadFileStreaming(name, headers).execute();
        if (response.isSuccessful()) {
            try (ResponseBody body = response.body();
                 InputStream in = body.byteStream();
                 OutputStream out = new FileOutputStream(file)) {
                in.transferTo(out);
            }
        } else {
            if (response.code() > 400) {
                LOG.warn("getFile('{}') failed, code {}: '{}'", name, response.code(), response.message());
            }
        }
        return FileResponse.create(response.code(), response.headers().toMultimap());
    }

    /**
     * Fetches the remote file, streams it to disk, returns response with http response code and headers
     */
    public FileResponse getMapFile(String name, Map<String, String> headers, File file) throws IOException {
        Response<ResponseBody> response = mapsApi.fetchLatestGeojson(name, headers).execute();
        if (response.isSuccessful()) {
            try (ResponseBody body = response.body();
                 InputStream in = body.byteStream();
                 OutputStream out = new FileOutputStream(file)) {
                in.transferTo(out);
            }
        } else {
            if (response.code() > 400) {
                LOG.warn("getMapFile('{}') failed, code {}: '{}'", name, response.code(), response.message());
            }
        }
        return FileResponse.create(response.code(), response.headers().toMultimap());
    }

}