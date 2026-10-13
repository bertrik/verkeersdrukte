package nl.bertriksikken.verkeersdrukte.ndw;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.eclipse.jetty.http.HttpHeader;
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

    private static okhttp3.Response addUserAgent(Interceptor.Chain chain) throws IOException {
        Request userAgentRequest = chain.request().newBuilder().header(HttpHeader.USER_AGENT.asString(), USER_AGENT).build();
        return chain.proceed(userAgentRequest);
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    /**
     * Fetches the remote file, streams it to disk, returns response with http response code and headers
     */
    FileResponse getFile(String name, Map<String, String> headers, File file) throws IOException {
        Response<ResponseBody> response = restApi.downloadFileStreaming(name, headers).execute();
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
        return FileResponse.withFile(response.code(), response.headers().toMultimap(), file);
    }

}