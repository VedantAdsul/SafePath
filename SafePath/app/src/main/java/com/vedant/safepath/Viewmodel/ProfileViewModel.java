package com.vedant.safepath.Viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.vedant.safepath.Data.UserProfileRepository;
import com.vedant.safepath.Model.UserProfile;

import java.util.function.Consumer;

public class ProfileViewModel extends ViewModel {
    private final UserProfileRepository repository;
    private final LiveData<UserProfile> userProfile;
    private final String uid;

    public ProfileViewModel(UserProfileRepository repository, String uid) {
        this.repository = repository;
        this.uid = uid;
        this.userProfile = repository.getUserProfile(uid);
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public void syncProfile() {
        repository.syncProfileFromServer(uid, null);
    }

    public void updateProfile(UserProfile profile, Consumer<Boolean> callback) {
        repository.upsertProfile(profile, callback);
    }
}