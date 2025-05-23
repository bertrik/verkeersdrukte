package nl.bertriksikken.verkeersdrukte.ndw;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;

import java.util.Map;

public interface INdwApi {

    String TRAFFIC_SPEED_XML_GZ = "trafficspeed.xml.gz";
    String TRAFFIC_SPEED_SHAPEFILE = "NDW_AVG_Meetlocaties_Shapefile.zip";

    /**
     * Downloads a file from <a href="https://opendata.ndw.nu/">NDW open data portaal</a>
     */
    @GET("/{filename}")
    Call<ResponseBody> downloadFile(@Path("filename") String filename, @HeaderMap Map<String, String> headers);

}
