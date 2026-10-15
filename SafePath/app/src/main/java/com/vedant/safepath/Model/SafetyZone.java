package com.vedant.safepath.Model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "safety_zones")
public class SafetyZone {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String type;
    public double lat;
    public double lon;
}