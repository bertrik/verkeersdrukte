package nl.bertriksikken.verkeersdrukte.ndw;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface INdwApi {

    String TRAFFIC_SPEED_XML_GZ = "trafficspeed.xml.gz";

    /**
     * Downloads a file from <a href="https://opendata.ndw.nu/">NDW open data portaal</a>
     */
    @GET("/{filename}")
    @Headers({"Accept-Encoding: gzip"})
    Call<ResponseBody> downloadFile(@Path("filename") String filename);

}
