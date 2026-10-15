package com.vedant.safepath.Viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.vedant.safepath.Data.SafetyZoneRepository;
import com.vedant.safepath.Model.SafetyZone;
import java.util.List;

public class MapViewModel extends AndroidViewModel {
    private final SafetyZoneRepository repository;
    private final LiveData<List<SafetyZone>> allSafetyZones;

    public MapViewModel(@NonNull Application application) {
        super(application);
        repository = new SafetyZoneRepository(application);
        allSafetyZones = repository.getAllSafetyZones();
    }

    public LiveData<List<SafetyZone>> getAllSafetyZones() {
        return allSafetyZones;
    }

    public void fetchSafetyZones(double lat, double lon) {
        repository.fetchAndStoreSafetyZones(lat, lon);
    }
}