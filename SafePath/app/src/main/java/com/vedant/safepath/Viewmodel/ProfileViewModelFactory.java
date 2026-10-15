package com.vedant.safepath.Viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.vedant.safepath.Data.UserProfileRepository;

public class ProfileViewModelFactory implements ViewModelProvider.Factory {
    private final UserProfileRepository repository;
    private final String uid;

    public ProfileViewModelFactory(UserProfileRepository repository, String uid) {
        this.repository = repository;
        this.uid = uid;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(repository, uid);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}