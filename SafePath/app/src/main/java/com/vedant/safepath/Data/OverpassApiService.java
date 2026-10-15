package com.vedant.safepath.Data;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OverpassApiService {
    @GET("api/interpreter")
    Call<JsonObject> getSafetyZones(@Query("data") String query);
}