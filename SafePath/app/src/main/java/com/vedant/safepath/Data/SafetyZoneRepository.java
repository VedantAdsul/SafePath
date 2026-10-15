package com.vedant.safepath.Data;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vedant.safepath.Model.SafetyZone;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SafetyZoneRepository {
    private static final String TAG = "SafetyZoneRepository";
    private final SafetyZoneDao safetyZoneDao;
    private final OverpassApiService apiService;
    private final ExecutorService executorService;

    public SafetyZoneRepository(Application application) {
        AppDatabase db = Room.databaseBuilder(application, AppDatabase.class, "safepath-db").build();
        safetyZoneDao = db.safetyZoneDao();
        apiService = RetrofitClient.getClient().create(OverpassApiService.class);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<SafetyZone>> getAllSafetyZones() {
        return safetyZoneDao.getAllZones();
    }

    public void fetchAndStoreSafetyZones(double lat, double lon) {
        String bbox = (lat - 0.05) + "," + (lon - 0.05) + "," + (lat + 0.05) + "," + (lon + 0.05);
        String query = "[out:json];(" +
                "node[amenity=police](" + bbox + ");" +
                "node[amenity=hospital](" + bbox + ");" +
                "way[highway=street_lamp](" + bbox + ");" +
                "way[highway=footway][lit=no](" + bbox + ");" +
                ");out center;";

        apiService.getSafetyZones(query).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    parseAndSaveResponse(response.body());
                } else {
                    Log.e(TAG, "API call failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "API call error", t);
            }
        });
    }

    private void parseAndSaveResponse(JsonObject body) {
        executorService.execute(() -> {
            List<SafetyZone> zones = new ArrayList<>();
            JsonArray elements = body.getAsJsonArray("elements");
            for (JsonElement element : elements) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("lat") && obj.has("lon")) {
                    SafetyZone zone = new SafetyZone();
                    zone.lat = obj.get("lat").getAsDouble();
                    zone.lon = obj.get("lon").getAsDouble();
                    JsonObject tags = obj.getAsJsonObject("tags");
                    if (tags.has("amenity")) {
                        zone.type = tags.get("amenity").getAsString();
                    } else if (tags.has("highway")) {
                        zone.type = tags.get("highway").getAsString();
                    }
                    zones.add(zone);
                }
            }
            safetyZoneDao.clearAll();
            safetyZoneDao.insertAll(zones);
        });
    }
}