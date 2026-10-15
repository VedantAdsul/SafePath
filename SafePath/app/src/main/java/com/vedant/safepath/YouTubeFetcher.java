package com.vedant.safepath;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.vedant.safepath.Model.VideoItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class YouTubeFetcher {

    public interface YouTubeCallback {
        void onVideosFetched(List<VideoItem> videoItems);
        void onError(String error);
    }

    public static void fetchYouTubeVideos(Context context, String query, String apiKey, YouTubeCallback callback) {
        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=10&q="
                + Uri.encode(query) + "&key=" + apiKey;
        Log.d("YouTubeFetcher", "Requesting URL: " + url);
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    List<VideoItem> videoItems = new ArrayList<>();
                    try {
                        JSONArray items = response.getJSONArray("items");
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            JSONObject idObj = item.getJSONObject("id");
                            JSONObject snippet = item.getJSONObject("snippet");

                            String videoId = idObj.getString("videoId");
                            String title = snippet.getString("title");
                            String thumbnail = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url");

                            videoItems.add(new VideoItem(videoId, title, thumbnail));
                        }
                        callback.onVideosFetched(videoItems);
                    } catch (JSONException e) {
                        callback.onError("Parsing error: " + e.getMessage());
                    }
                },
                error -> callback.onError("Volley error: " + error.toString())
        );

        queue.add(request);
    }
}

