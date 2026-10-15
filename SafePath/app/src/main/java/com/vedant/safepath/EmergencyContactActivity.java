package com.vedant.safepath;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EmergencyContactActivity extends AppCompatActivity {

    private List<EmergencyContact> contactList = new ArrayList<>();
    private ContactAdapter adapter;
    private EmergencyContact primaryContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setPadding(0,60,0,0);

        RecyclerView recyclerView = findViewById(R.id.contactRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadContacts();

        adapter = new ContactAdapter(this, contactList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton addFab = findViewById(R.id.fab_add_contact);
        addFab.setOnClickListener(v -> showAddContactDialog(null, -1));
    }

    private void showAddContactDialog(EmergencyContact existingContact, int position) {
        if (existingContact != null && existingContact.isPrimary) {
            Toast.makeText(this, "Primary contact can only be changed from your profile.", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        EditText nameField = dialogView.findViewById(R.id.edit_name);
        EditText numberField = dialogView.findViewById(R.id.edit_number);

        String title = existingContact == null ? "Add New Contact" : "Edit Contact";
        String positiveButtonText = existingContact == null ? "Add" : "Save";

        if (existingContact != null) {
            nameField.setText(existingContact.name);
            numberField.setText(existingContact.phone);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    String name = nameField.getText().toString().trim();
                    String number = numberField.getText().toString().trim();
                    if (!name.isEmpty() && !number.isEmpty()) {
                        if (existingContact == null) {
                            contactList.add(new EmergencyContact(name, number, false));
                        } else {
                            existingContact.name = name;
                            existingContact.phone = number;
                        }
                        adapter.notifyDataSetChanged();
                        saveContactsToPrefs();
                    } else {
                        Toast.makeText(this, "Name and number cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmationDialog(int position) {
        EmergencyContact contact = contactList.get(position);
        if (contact.isPrimary) {
            Toast.makeText(this, "Primary contact cannot be deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete this contact?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    contactList.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveContactsToPrefs();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
        private final Context context;
        private final List<EmergencyContact> contacts;

        ContactAdapter(Context context, List<EmergencyContact> contacts) {
            this.context = context;
            this.contacts = contacts;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, phoneText, primaryLabel;
            ImageButton callButton, deleteButton;

            ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.contact_name);
                phoneText = itemView.findViewById(R.id.contact_number);
                callButton = itemView.findViewById(R.id.button_call);
                deleteButton = itemView.findViewById(R.id.button_delete);
                primaryLabel = itemView.findViewById(R.id.primary_contact_label);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EmergencyContact contact = contacts.get(position);
            holder.nameText.setText(contact.name);
            holder.phoneText.setText(contact.phone);

            if (contact.isPrimary) {
                holder.primaryLabel.setVisibility(View.VISIBLE);
                holder.deleteButton.setVisibility(View.GONE);
            } else {
                holder.primaryLabel.setVisibility(View.GONE);
                holder.deleteButton.setVisibility(View.VISIBLE);
            }

            holder.callButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.phone));
                context.startActivity(intent);
            });

            holder.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(holder.getAdapterPosition()));
            holder.itemView.setOnClickListener(v -> showAddContactDialog(contact, holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }
    }

    private void saveContactsToPrefs() {
        SharedPreferences prefs = getSharedPreferences("SafePathPrefs", MODE_PRIVATE);
        List<EmergencyContact> userAddedContacts = new ArrayList<>();
        for (EmergencyContact contact : contactList) {
            if (!contact.isPrimary) {
                userAddedContacts.add(contact);
            }
        }
        String json = new Gson().toJson(userAddedContacts);
        prefs.edit().putString("emergency_contacts_list", json).apply();
    }

    private void loadContacts() {
        SharedPreferences prefs = getSharedPreferences("SafePathPrefs", MODE_PRIVATE);
        contactList.clear();

        String primaryContactPhone = prefs.getString(ProfileActivity.KEY_EMERGENCY_CONTACT, null);
        if (primaryContactPhone != null && !primaryContactPhone.isEmpty()) {
            primaryContact = new EmergencyContact("Primary Emergency Contact", primaryContactPhone, true);
            contactList.add(primaryContact);
        }

        String json = prefs.getString("emergency_contacts_list", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
            List<EmergencyContact> userAddedContacts = new Gson().fromJson(json, type);
            if (userAddedContacts != null) {
                contactList.addAll(userAddedContacts);
            }
        }

        if (contactList.isEmpty()) {
            contactList.add(new EmergencyContact("Police", "100", false));
            contactList.add(new EmergencyContact("Ambulance", "108", false));
            contactList.add(new EmergencyContact("Fire Department", "101", false));
            saveContactsToPrefs(); // Save the defaults
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class EmergencyContact {
        public String name;
        public String phone;
        public boolean isPrimary;

        public EmergencyContact(String name, String phone, boolean isPrimary) {
            this.name = name;
            this.phone = phone;
            this.isPrimary = isPrimary;
        }
    }
}
