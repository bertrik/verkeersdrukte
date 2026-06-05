package nl.bertriksikken.verkeersdrukte.ndw;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

import java.util.Map;

/**
 * Typical URL:
 * <a href="https://maps.ndw.nu/api/v1/mst/latest/geojson/pointRecord.geojson">url</a>
 */
public interface INdwMapsApi {

    String POINT_RECORD_GEOJSON = "pointRecord.geojson";

    @Streaming
    @GET("/api/v1/mst/latest/geojson/{filename}")
    Call<ResponseBody> fetchLatestGeojson(@Path(value = "filename") String filename, @HeaderMap Map<String, String> headers);

}
