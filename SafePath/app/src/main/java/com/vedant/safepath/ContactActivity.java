package com.vedant.safepath;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ContactActivity extends AppCompatActivity {

    MaterialToolbar toolbar;
    TextInputLayout numberLayout, messageLayout, emailLayout, subjectLayout, emailContentLayout;
    TextInputEditText numberInput, messageInput, emailInput, subjectInput, emailContentInput;
    AppCompatButton sendSMS, sendEmail;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        findID();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!hasSmsPermission()) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        }

        setUpLisnters();
    }

    public void findID(){
        toolbar = findViewById(R.id.toolbar);
        numberLayout = findViewById(R.id.number_layout);
        messageLayout = findViewById(R.id.message_layout);
        sendSMS = findViewById(R.id.button_sendSMS);
        numberInput = findViewById(R.id.number_input);
        messageInput = findViewById(R.id.message_input);

        emailLayout = findViewById(R.id.emailrecipient_layout);
        subjectLayout = findViewById(R.id.emailsubject_layout);
        emailContentLayout = findViewById(R.id.emailcontent_layout);
        sendEmail = findViewById(R.id.button_sendEmail);
        emailInput = findViewById(R.id.emailrecipient_input);
        subjectInput = findViewById(R.id.emailsubject_input);
        emailContentInput = findViewById(R.id.emailcontent_input);
    }

    public void setUpLisnters(){
        sendSMS.setOnClickListener(v -> sendSMS());
        sendEmail.setOnClickListener(v -> sendEmail());
    }

    public boolean validateSMSForm(){
        boolean isValid = true;
        String number = numberInput.getText().toString().trim();
        String feedback = messageInput.getText().toString().trim();

        if(number.isEmpty() || number.length() != 10){
            numberLayout.setError("Invalid number");
            isValid = false;
        }else{
            numberLayout.setError(null);
        }

        if(feedback.isEmpty()){
            messageLayout.setError("Message is required");
            isValid = false;
        }else {
            messageLayout.setError(null);
        }

        return isValid;
    }
    public boolean validateEmailForm(){
        boolean isValid = true;
        String recipient = emailInput.getText().toString().trim();
        String subject = subjectInput.getText().toString().trim();
        String content = emailContentInput.getText().toString().trim();

        if(recipient.isEmpty()){
            emailLayout.setError("Recipient is required");
            isValid = false;
        }else{
            emailLayout.setError(null);
        }

        if(subject.isEmpty()){
            subjectLayout.setError("Subject is required");
            isValid = false;
        }else{
            subjectLayout.setError(null);
        }

        if(content.isEmpty()){
            emailContentLayout.setError("Content is required");
            isValid = false;
        }else{
            emailContentLayout.setError(null);
        }

        return isValid;
    }

    public void sendSMS(){
        if(!validateSMSForm()) return;

        String number = numberInput.getText().toString().trim();
        String feedback = messageInput.getText().toString().trim();

        try{
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, feedback, null, null);
            Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show();
            numberInput.setText("");
            messageInput.setText("");
        }catch(Exception e){
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            Log.d("SMS_error",e.getMessage());
        }

    }

    public void sendEmail() {
        if (!validateEmailForm()) return;

        String recipient = emailInput.getText().toString().trim();
        String subject = subjectInput.getText().toString().trim();
        String content = emailContentInput.getText().toString().trim();

        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));

            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, content);

            startActivity(Intent.createChooser(intent, "Send Email"));
            emailInput.setText("");
            subjectInput.setText("");
            emailContentInput.setText("");
        } catch (Exception e) {
            Toast.makeText(this, "No email client installed", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted.", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean hasSmsPermission() {

        return ActivityCompat.checkSelfPermission(ContactActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
