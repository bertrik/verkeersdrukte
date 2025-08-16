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

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public final class NdwClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NdwClient.class);
    private static final String USER_AGENT = "github.com/bertrik/verkeersdrukte";

    private final OkHttpClient httpClient;
    private final INdwApi restApi;

    NdwClient(OkHttpClient httpClient, INdwApi restApi) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.restApi = Objects.requireNonNull(restApi);
    }

    public static NdwClient create(NdwConfig config) {
        LOG.info("Creating new REST client for URL '{}' with timeout {}", config.getUrl(), config.getTimeout());
        OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(NdwClient::addUserAgent).connectTimeout(config.getTimeout())
                .readTimeout(config.getTimeout()).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(config.getUrl())
                .addConverterFactory(ScalarsConverterFactory.create()).client(client).build();
        INdwApi restApi = retrofit.create(INdwApi.class);
        return new NdwClient(client, restApi);
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    private static okhttp3.Response addUserAgent(Interceptor.Chain chain) throws IOException {
        Request userAgentRequest = chain.request().newBuilder().header(HttpHeader.USER_AGENT.asString(), USER_AGENT).build();
        return chain.proceed(userAgentRequest);
    }

    public FileResponse getTrafficSpeed() throws IOException {
        Map<String, String> headers = Map.of(HttpHeaders.ACCEPT_ENCODING, "gzip");
        return getFile(INdwApi.TRAFFIC_SPEED_XML_GZ, headers);
    }

    public FileResponse getShapeFile(String etag) throws IOException {
        Map<String, String> headers = Map.of(HttpHeaders.IF_NONE_MATCH, etag);
        return getFile(INdwApi.TRAFFIC_SPEED_SHAPEFILE, headers);
    }

    public FileResponse getMeasurementSiteTable(String etag) throws IOException {
        Map<String, String> headers = Map.of(HttpHeaders.IF_NONE_MATCH, etag);
        return getFile(INdwApi.MEASUREMENT_SITE_TABLE, headers);
    }

    FileResponse getFile(String name, Map<String, String> headers) throws IOException {
        Response<ResponseBody> response = restApi.downloadFile(name, headers).execute();
        if (response.isSuccessful()) {
            return FileResponse.create(response.code(), response.headers().toMultimap(), response.body().bytes());
        } else {
            LOG.warn("getFile('{}') failed, code {}: '{}'", name, response.code(), response.message());
            return FileResponse.create(response.code(), response.headers().toMultimap(), new byte[0]);
        }
    }
}