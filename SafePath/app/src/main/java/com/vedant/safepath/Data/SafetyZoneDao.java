package com.vedant.safepath.Data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.vedant.safepath.Model.SafetyZone;
import java.util.List;

@Dao
public interface SafetyZoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SafetyZone> zones);

    @Query("SELECT * FROM safety_zones")
    LiveData<List<SafetyZone>> getAllZones();

    @Query("DELETE FROM safety_zones")
    void clearAll();
}