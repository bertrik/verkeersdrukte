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

import java.io.IOException;
import java.util.Objects;

public final class NdwClient {

    private static final Logger LOG = LoggerFactory.getLogger(NdwClient.class);
    private static final String USER_AGENT = "github.com/bertrik/verkeersdrukte";

    private final INdwApi restApi;

    NdwClient(INdwApi restApi) {
        this.restApi = Objects.requireNonNull(restApi);
    }

    public static NdwClient create(NdwConfig config) {
        LOG.info("Creating new REST client for URL '{}' with timeout {}", config.getUrl(), config.getTimeout());
        OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(NdwClient::addUserAgent).connectTimeout(config.getTimeout())
                .readTimeout(config.getTimeout()).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(config.getUrl())
                .addConverterFactory(ScalarsConverterFactory.create()).client(client).build();
        INdwApi restApi = retrofit.create(INdwApi.class);
        return new NdwClient(restApi);
    }

    private static okhttp3.Response addUserAgent(Interceptor.Chain chain) throws IOException {
        Request userAgentRequest = chain.request().newBuilder().header(HttpHeader.USER_AGENT.asString(), USER_AGENT).build();
        return chain.proceed(userAgentRequest);
    }

    public FileResponse getTrafficSpeed() throws IOException {
        Response<ResponseBody> response = restApi.downloadFile(INdwApi.TRAFFIC_SPEED_XML_GZ).execute();
        if (response.isSuccessful()) {
            return FileResponse.create(response.body().bytes(), response.headers().get("Last-Modified"));
        } else {
            LOG.warn("getTrafficSpeed failed, code {}, message {}", response.code(), response.message());
            return FileResponse.empty();
        }
    }
}