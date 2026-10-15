package com.vedant.safepath.Data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.vedant.safepath.Model.UserProfile;

@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    LiveData<UserProfile> getProfile(String uid);

    @Query("DELETE FROM user_profile")
    void deleteAll();
}
