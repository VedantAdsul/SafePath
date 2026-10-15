package com.vedant.safepath.Fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.vedant.safepath.HomeActivity;
import com.vedant.safepath.Model.SafetyZone;
import com.vedant.safepath.R;
import com.vedant.safepath.Model.SharedPreferencesHelper; // Corrected import path
import com.vedant.safepath.Viewmodel.MapViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private static final LatLng INDIA_DEFAULT_LOCATION = new LatLng(20.5937, 78.9629);
    private static final float INDIA_DEFAULT_ZOOM = 4f;
    private static final float LOCATION_ZOOM = 15f;

    private GoogleMap mMap;
    private Marker currentMarker;
    private Marker searchedMarker;
    private Polyline routePolyline;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private TextToSpeech textToSpeech;
    private ActivityResultLauncher<Intent> voiceSearchLauncher;
    private RequestQueue requestQueue;

    private LatLng freshCurrentLocation;
    private boolean isFirstMapAnimation = true;
    private SearchView searchView;
    private SharedPreferences.OnSharedPreferenceChangeListener settingsListener;
    private SharedPreferencesHelper prefsHelper;
    private MapViewModel mapViewModel;
    private List<Circle> safetyZoneCircles = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsHelper = new SharedPreferencesHelper(requireContext());
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        requestQueue = Volley.newRequestQueue(requireContext());
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        initializeLocationCallback();
        initializeVoiceSearchLauncher();
        initializeTextToSpeech();
        setupSettingsListener();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map_container, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        setupUI(root);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setStatusBarColor();
    }

    private void setStatusBarColor() {
        if (getActivity() == null) return;
        Window window = getActivity().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = window.getDecorView();
            int flags = decor.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    private void setupUI(View root) {
        searchView = root.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchView.clearFocus();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) clearSearch();
                return true;
            }
        });
        searchView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (mMap != null) {
                int padding = searchView.getHeight() + 40;
                mMap.setPadding(0, padding, 0, 0);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (freshCurrentLocation == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(INDIA_DEFAULT_LOCATION, INDIA_DEFAULT_ZOOM));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(freshCurrentLocation, LOCATION_ZOOM));
        }
        if (isLocationPermissionGranted()) {
            onLocationPermissionGranted();
        }
        applyMapTypeFromSettings();
        observeSafetyZones();
    }

    private void setupSettingsListener() {
        settingsListener = (sharedPreferences, key) -> {
            if (key == null || getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (key.equals(SharedPreferencesHelper.PREF_MAP_TYPE)) {
                    applyMapTypeFromSettings();
                } else if (key.equals(SharedPreferencesHelper.PREF_LOCATION_FREQ)) {
                    restartLocationUpdates();
                } else if (key.equals(SharedPreferencesHelper.PREF_SAFE_ZONES)) {
                    // When the toggle is changed, update the visibility of circles
                    updateSafetyZoneVisibility();
                }
            });
        };
        requireContext().getSharedPreferences("SafePathSettings", Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(settingsListener);
    }

    private void applyMapTypeFromSettings() {
        if (mMap == null) return;
        mMap.setMapType(prefsHelper.getMapType());
    }

    public void onLocationPermissionGranted() {
        if (mMap == null || getContext() == null || !isLocationPermissionGranted()) return;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        startLocationUpdates();
    }

    private void initializeLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    updateLocationMarker(locationResult.getLastLocation());
                }
            }
        };
    }

    private void initializeVoiceSearchLauncher() {
        voiceSearchLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                ArrayList<String> spokenResult = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (spokenResult != null && !spokenResult.isEmpty()) {
                    searchView.setQuery(spokenResult.get(0), true);
                }
            }
        });
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("hi", "IN"));
            } else {
                Log.e(TAG, "TTS Initialization failed.");
            }
        });
    }

    public void performSearch(String query) {
        if (query == null || query.trim().isEmpty() || getContext() == null || mMap == null) {
            clearSearch();
            return;
        }
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(query, 1);
            clearSearch();
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng destination = new LatLng(address.getLatitude(), address.getLongitude());
                String title = address.getFeatureName() != null ? address.getFeatureName() : query;
                searchedMarker = mMap.addMarker(new MarkerOptions()
                        .position(destination)
                        .title(title)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, LOCATION_ZOOM));
                if (freshCurrentLocation != null) {
                    drawRoute(freshCurrentLocation, destination);
                }
            } else {
                Toast.makeText(getContext(), "No location found for: " + query, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder search error", e);
            Toast.makeText(getContext(), "Search service not available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearSearch() {
        if (routePolyline != null) routePolyline.remove();
        if (searchedMarker != null) searchedMarker.remove();
        routePolyline = null;
        searchedMarker = null;
    }

    private void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(getContext(), "Please turn on your device location", Toast.LENGTH_LONG).show();
            return;
        }

        long interval = prefsHelper.getLocationUpdateInterval();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(interval / 2)
                .build();
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Started location updates with interval: " + interval + "ms");
    }

    private void restartLocationUpdates() {
        Log.d(TAG, "Restarting location updates due to settings change.");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        startLocationUpdates();
    }

    private void updateLocationMarker(Location location) {
        if (getContext() == null || mMap == null) return;
        freshCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        saveLocationToPrefs(freshCurrentLocation);
        if (currentMarker == null) {

        }
        if (isFirstMapAnimation) {
            mapViewModel.fetchSafetyZones(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(freshCurrentLocation, LOCATION_ZOOM));
            isFirstMapAnimation = false;
        }
    }

    private void saveLocationToPrefs(LatLng location) {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("SafePathPrefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("lat", String.valueOf(location.latitude))
                    .putString("lng", String.valueOf(location.longitude))
                    .apply();
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        String apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjdlZThhNTc5MjBmOTQ1Yzg5YWQyMjc4MTZjN2M5Yjc4IiwiaCI6Im11cm11cjY0In0=";
        String url = "https://api.openrouteservice.org/v2/directions/driving-car/geojson";
        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray coordinates = new JSONArray();
            coordinates.put(new JSONArray(new double[]{origin.longitude, origin.latitude}));
            coordinates.put(new JSONArray(new double[]{destination.longitude, destination.latitude}));
            jsonBody.put("coordinates", coordinates);
        } catch (JSONException e) {
            Log.e(TAG, "JSON body error for route.", e);
            return;
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    try {
                        JSONArray coordinates = response.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                        List<LatLng> path = new ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            path.add(new LatLng(coord.getDouble(1), coord.getDouble(0)));
                        }
                        if (routePolyline != null) routePolyline.remove();
                        TypedValue typedValue = new TypedValue();
                        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
                        routePolyline = mMap.addPolyline(new PolylineOptions().addAll(path).color(typedValue.data).width(12f));
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error for route.", e);
                    }
                },
                error -> {
                    Log.e(TAG, "OpenRouteService error", error);
                    Toast.makeText(getContext(), "Failed to fetch route.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", apiKey);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        requestQueue.add(request);
    }

    private boolean isLocationPermissionGranted() {
        return getContext() != null && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLocationPermissionGranted()) {
            startLocationUpdates();
        }
        setStatusBarColor();
    }

    @Override
    public void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (getContext() != null && settingsListener != null) {
            getContext().getSharedPreferences("SafePathSettings", Context.MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(settingsListener);
        }
        super.onDestroy();
    }
    private void observeSafetyZones() {
        mapViewModel.getAllSafetyZones().observe(getViewLifecycleOwner(), zones -> {
            for (Circle circle : safetyZoneCircles) {
                circle.remove();
            }
            safetyZoneCircles.clear();

            if (mMap == null) return;

            for (SafetyZone zone : zones) {
                CircleOptions circleOptions = new CircleOptions()
                        .center(new LatLng(zone.lat, zone.lon))
                        .radius(150);

                if ("police".equals(zone.type) || "hospital".equals(zone.type)) {
                    circleOptions.strokeColor(Color.TRANSPARENT).fillColor(0x4032CD32);
                } else if ("dark_footway".equals(zone.type)) {
                    circleOptions.strokeColor(Color.TRANSPARENT).fillColor(0x40FF4500);
                } else {
                    continue;
                }
                safetyZoneCircles.add(mMap.addCircle(circleOptions));
            }
            updateSafetyZoneVisibility();
        });
    }

    private void updateSafetyZoneVisibility() {
        if (mMap == null) return;
        boolean showZones = prefsHelper.getShowSafeZones();
        for (Circle circle : safetyZoneCircles) {
            circle.setVisible(showZones);
        }
    }
}
