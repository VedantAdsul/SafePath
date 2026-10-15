package com.vedant.safepath.Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.vedant.safepath.EmergencyContactActivity;
import com.vedant.safepath.ProfileActivity;
import com.vedant.safepath.R;
import com.vedant.safepath.SosService;

import java.util.ArrayList;
import java.util.List;

public class EmergencyFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST_CODE = 101;

    private TextView txtLocation;
    private MaterialButton btnSos, btnShareLocation, btnFlashlight, btnFirstAid, btnEmergencyContacts, btnCallPolice, btnIamSafe;

    private boolean isFlashOn = false;
    private CameraManager cameraManager;
    private String cameraId;
    private String lat, lng;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_emergency, container, false);

        initializeViews(view);
        initializeCamera();

        SharedPreferences locationPrefs = requireContext().getSharedPreferences("SafePathPrefs", MODE_PRIVATE);
        lat = locationPrefs.getString("lat", "0.0");
        lng = locationPrefs.getString("lng", "0.0");

        updateLocationText();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        txtLocation = view.findViewById(R.id.txt_location);
        btnSos = view.findViewById(R.id.btn_sos);
        btnShareLocation = view.findViewById(R.id.btn_share_location);
        btnFlashlight = view.findViewById(R.id.btn_flashlight);
        btnFirstAid = view.findViewById(R.id.btn_first_aid);
        btnEmergencyContacts = view.findViewById(R.id.btn_emergency_contacts);
        btnCallPolice = view.findViewById(R.id.btn_call_police);
        btnIamSafe = view.findViewById(R.id.btn_iam_safe);
    }

    private void initializeCamera() {
        cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                if (cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updateLocationText() {
        if ("0.0".equals(lat) || "0.0".equals(lng)) {
            txtLocation.setText("📍 Waiting for location...");
        } else {
            txtLocation.setText("📍 " + lat + ", " + lng);
        }
    }

    private void setupClickListeners() {
        btnShareLocation.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here's my location: https://maps.google.com/?q=" + lat + "," + lng);
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, "Share Location via"));
        });

        btnFlashlight.setOnClickListener(v -> toggleFlashlight());

        btnEmergencyContacts.setOnClickListener(v -> startActivity(new Intent(getContext(), EmergencyContactActivity.class)));

        btnFirstAid.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.redcross.org/content/dam/redcross/atg/PDF_s/Health___Safety_Services/First_Aid_Poster_EN.pdf"))));

        btnSos.setOnClickListener(v -> {
            if (!hasRequiredPermissions()) {
                requestRequiredPermissions();
            } else {
                triggerSosActions();
            }
        });

        btnCallPolice.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CODE);
            } else {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + "100"));
                startActivity(callIntent);
            }
        });

        btnIamSafe.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_CODE);
            } else {
                sendSafeMessage();
            }
        });
    }

    private void toggleFlashlight() {
        if (cameraId == null) {
            Toast.makeText(getContext(), "Flashlight not supported.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);
            btnFlashlight.setIconResource(isFlashOn ? R.drawable.ic_flashlight_off : R.drawable.ic_flashlight);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void triggerSosActions() {
        String primaryContact = getPrimaryContact();
        if (TextUtils.isEmpty(primaryContact)) {
            Toast.makeText(getContext(), "No primary emergency contact set. Please add one in your profile.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), "SOS Activated! Triggering selected actions...", Toast.LENGTH_LONG).show();

        Intent serviceIntent = new Intent(getContext(), SosService.class);
        serviceIntent.setAction(SosService.ACTION_START);
        serviceIntent.putExtra(SosService.EXTRA_LATITUDE, lat);
        serviceIntent.putExtra(SosService.EXTRA_LONGITUDE, lng);
        serviceIntent.putExtra(SosService.EXTRA_EMERGENCY_CONTACT, primaryContact);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
    }

    private void sendSafeMessage() {
        String primaryContactPhone = getPrimaryContact();
        if (TextUtils.isEmpty(primaryContactPhone)) {
            Toast.makeText(getContext(), "No primary emergency contact set.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), "Sending 'I am safe' message...", Toast.LENGTH_SHORT).show();
        String message = "UPDATE: I am safe now. This is a follow-up message from the SafePath app.";

        try {
            SmsManager.getDefault().sendTextMessage(primaryContactPhone, null, message, null, null);
            Toast.makeText(getContext(), "'I am safe' message sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send SMS.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private String getPrimaryContact() {
        SharedPreferences prefs = requireContext().getSharedPreferences("SafePathPrefs", MODE_PRIVATE);
        return prefs.getString(ProfileActivity.KEY_EMERGENCY_CONTACT, null);
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissionsToRequest.isEmpty()) {
            requestPermissions(permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(getContext(), "All permissions granted. You can now use SOS.", Toast.LENGTH_SHORT).show();
                triggerSosActions();
            } else {
                Toast.makeText(getContext(), "Some permissions were denied. SOS features may be limited.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
