package com.vedant.safepath.Data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vedant.safepath.Model.UserProfile;

import java.util.function.Consumer;

public class UserProfileRepository {

    private final UserProfileDao userProfileDao;
    private final DatabaseReference databaseReference;
    private static final String TAG = "UserProfileRepository";

    public UserProfileRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.userProfileDao = db.userProfileDao();
        this.databaseReference = FirebaseDatabase.getInstance().getReference("users");
    }

    public LiveData<UserProfile> getUserProfile(String uid) {
        return userProfileDao.getProfile(uid);
    }

    public void syncProfileFromServer(String uid, Runnable onComplete) {
        databaseReference.child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                UserProfile profile = task.getResult().getValue(UserProfile.class);
                if (profile != null) {
                    profile.setUid(uid);
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        userProfileDao.upsert(profile);
                        if (onComplete != null) onComplete.run();
                    });
                }
            } else {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public void upsertProfile(UserProfile profile, Consumer<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userProfileDao.upsert(profile);
            databaseReference.child(profile.getUid()).setValue(profile)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.accept(true);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.accept(false);
                    });
        });
    }

    public void clearLocalProfileData() {
        AppDatabase.databaseWriteExecutor.execute(userProfileDao::deleteAll);
    }

    public void deleteUserAccount(FirebaseUser user, Consumer<Boolean> callback) {
        String uid = user.getUid();

        databaseReference.child(uid).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User data deleted from Realtime DB.");

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    userProfileDao.deleteAll();
                    Log.d(TAG, "User data deleted from Room.");

                    user.delete().addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            Log.d(TAG, "User account deleted from Firebase Auth.");
                            if (callback != null) callback.accept(true);
                        } else {
                            Log.e(TAG, "Failed to delete user from Firebase Auth.", authTask.getException());
                            if (callback != null) callback.accept(false);
                        }
                    });
                });
            } else {
                Log.e(TAG, "Failed to delete user data from Realtime DB.", task.getException());
                if (callback != null) callback.accept(false);
            }
        });
    }
}
