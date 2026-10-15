package com.vedant.safepath;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.vedant.safepath.Model.SharedPreferencesHelper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SosService extends Service {

    public static final String ACTION_START = "com.vedant.safepath.ACTION_START";
    public static final String ACTION_STOP = "com.vedant.safepath.ACTION_STOP";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_EMERGENCY_CONTACT = "extra_emergency_contact";

    private static final String CHANNEL_ID = "SosServiceChannel";
    private static final String TAG = "SosService";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer sirenPlayer;
    private CameraManager cameraManager;
    private String cameraId;
    private Runnable blinkRunnable;
    private SharedPreferencesHelper prefsHelper;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefsHelper = new SharedPreferencesHelper(this);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        initializeCamera();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            startForegroundService();

            String lat = intent.getStringExtra(EXTRA_LATITUDE);
            String lng = intent.getStringExtra(EXTRA_LONGITUDE);
            String emergencyContact = intent.getStringExtra(EXTRA_EMERGENCY_CONTACT);

            if (prefsHelper.getSosSmsEnabled()) {
                sendSosSms(emergencyContact, lat, lng);
            }
            if (prefsHelper.getSosCallEnabled()) {
                makeEmergencyCall(emergencyContact);
            }
            if (prefsHelper.getSosRecordEnabled()) {
                startRecording();
            }
            if (prefsHelper.getSosSirenEnabled()) {
                startSiren();
            }
            if (prefsHelper.getSosFlashEnabled()) {
                startBlinkingFlashlight();
            }
        } else if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopSelfIntent = new Intent(this, SosService.class);
        stopSelfIntent.setAction(ACTION_STOP);
        PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafePath SOS Activated")
                .setContentText("Tap to return to the app. Background actions are running.")
                .setSmallIcon(R.drawable.ic_sos)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_close, "Stop SOS", pStopSelf)
                .build();

        startForeground(1, notification);
    }

    private void sendSosSms(String contact, String lat, String lng) {
        if (TextUtils.isEmpty(contact)) return;
        String message = "EMERGENCY! This is an automated SOS from the SafePath app. My current location is: https://maps.google.com/?q=" + lat + "," + lng;
        try {
            SmsManager.getDefault().sendTextMessage(contact, null, message, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SOS SMS.", e);
        }
    }

    private void makeEmergencyCall(String contact) {
        if (TextUtils.isEmpty(contact)) return;
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + contact));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to make emergency call due to permissions.", e);
        }
    }

    private void startRecording() {
        if (isRecording) return;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "SOS_Recording_" + timeStamp;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SafePath Recordings");
        }

        ContentResolver contentResolver = getContentResolver();
        Uri audioUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

        if (audioUri == null) {
            Log.e(TAG, "Failed to create new MediaStore entry.");
            return;
        }

        try (ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(audioUri, "w")) {
            if (pfd == null) {
                Log.e(TAG, "ParcelFileDescriptor is null");
                return;
            }

            FileDescriptor fileDescriptor = pfd.getFileDescriptor();
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(fileDescriptor);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "SOS: Audio recording started successfully. URI: " + audioUri);

        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Failed to prepare or start MediaRecorder for audio", e);
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
            isRecording = false;
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (RuntimeException stopException) {
                Log.e(TAG, "Failed to stop MediaRecorder", stopException);
            } finally {
                mediaRecorder = null;
                isRecording = false;
                Toast.makeText(this, "SOS: Recording stopped.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "SOS: Recording stopped.");
            }
        }
    }


    private void initializeCamera() {
        try {
            for (String id : cameraManager.getCameraIdList()) {
                if (cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to access camera for flashlight.", e);
        }
    }

    private void startSiren() {
        if (sirenPlayer != null) sirenPlayer.release();
        sirenPlayer = MediaPlayer.create(this, R.raw.siren);
        if (sirenPlayer != null) {
            sirenPlayer.setLooping(true);
            sirenPlayer.start();
        }
    }

    private void startBlinkingFlashlight() {
        if (cameraId == null) return;
        blinkRunnable = new Runnable() {
            private boolean isOn = false;
            @Override
            public void run() {
                try {
                    cameraManager.setTorchMode(cameraId, isOn);
                    isOn = !isOn;
                    handler.postDelayed(this, 300);
                } catch (CameraAccessException e) {
                    Log.e(TAG, "Failed to blink flashlight.", e);
                }
            }
        };
        handler.post(blinkRunnable);
    }

    private void stopPanicActions() {
        stopRecording();

        if (blinkRunnable != null) handler.removeCallbacks(blinkRunnable);
        try {
            if (cameraId != null) cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to turn off flashlight.", e);
        }

        if (sirenPlayer != null) {
            sirenPlayer.stop();
            sirenPlayer.release();
            sirenPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPanicActions();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "SOS Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
