package com.vedant.safepath;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkManager {

    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isNetworkAvailable = new MutableLiveData<>();
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            isNetworkAvailable.postValue(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            isNetworkAvailable.postValue(false);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            final boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            final boolean isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            isNetworkAvailable.postValue(hasInternet && isValidated);
        }
    };

    public NetworkManager(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public LiveData<Boolean> getNetworkAvailability() {
        return isNetworkAvailable;
    }

    public void registerCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void unregisterCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    public void checkNetworkAndDispatch() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            isNetworkAvailable.postValue(false);
            return;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        boolean hasInternet = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean isValidated = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        isNetworkAvailable.postValue(hasInternet && isValidated);
    }
}
