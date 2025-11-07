package com.example.eventlottery.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.example.eventlottery.QrGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * CreateEventFragment - handles event creation form
 * created by: ibrahim
 */

/**
 * provides the form that organizers use to create events
 * saves event details from form into firebase
 */
public class CreateEventFragment extends Fragment {

    // ui elements
    private EditText etPreCreatedEvent, etLocation, etOrganizerName, etEventDescription,
            etEligibilityCriteria, etStartDate, etEndDate, etPrice, etWaitlistLimit, etPoolSize;
    private RadioGroup rgGeolocation;
    private Button btnAddImage, btnDone, btnCancel;
    private ImageView btnDeleteImage1, btnDeleteImage2, btnDeleteImage3;
    private ImageButton btnLocation;

    // firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    /**
     * instanties the fragment in the user interface view
     * instializes the firebasses instances
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        initializeViews(view);
        setupListeners();

        return view;
    }

    /**
     * connects xml elements with java
     */
    private void initializeViews(View view) {
        etPreCreatedEvent = view.findViewById(R.id.et_pre_created_event);
        etLocation = view.findViewById(R.id.et_location);
        etOrganizerName = view.findViewById(R.id.et_organizer_name);
        etEventDescription = view.findViewById(R.id.et_event_description);
        etEligibilityCriteria = view.findViewById(R.id.et_eligibility_criteria);
        etStartDate = view.findViewById(R.id.et_start_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        etPrice = view.findViewById(R.id.et_price);
        etWaitlistLimit = view.findViewById(R.id.et_waitlist_limit);
        etPoolSize = view.findViewById(R.id.et_pool_size);
        rgGeolocation = view.findViewById(R.id.rg_geolocation);

        btnAddImage = view.findViewById(R.id.btn_add_image);
        btnDone = view.findViewById(R.id.btn_done);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnDeleteImage1 = view.findViewById(R.id.btn_delete_image1);
        btnDeleteImage2 = view.findViewById(R.id.btn_delete_image2);
        btnDeleteImage3 = view.findViewById(R.id.btn_delete_image3);
        btnLocation = view.findViewById(R.id.btn_location);
    }

    /**
     * sets up button actions
     */
    private void setupListeners() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QrGenerator.class);
            startActivity(intent);
        });

        btnAddImage.setOnClickListener(v ->
                Toast.makeText(getContext(), "image upload feature coming soon", Toast.LENGTH_SHORT).show()
        );

        btnDeleteImage1.setOnClickListener(v ->
                Toast.makeText(getContext(), "deleted image 1", Toast.LENGTH_SHORT).show());
        btnDeleteImage2.setOnClickListener(v ->
                Toast.makeText(getContext(), "deleted image 2", Toast.LENGTH_SHORT).show());
        btnDeleteImage3.setOnClickListener(v ->
                Toast.makeText(getContext(), "deleted image 3", Toast.LENGTH_SHORT).show());

        btnDone.setOnClickListener(v -> handleEventCreation());
        btnCancel.setOnClickListener(v -> {
            clearForm();
            Toast.makeText(getContext(), "form cleared", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * shows date picker dialog for date fields
     */
    private void showDatePicker(EditText field) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(getContext(),
                (view, year, month, day) -> field.setText(day + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * handles event creation and saves to firestore
     */
    private void handleEventCreation() {
        Toast.makeText(getContext(), "done button pressed", Toast.LENGTH_SHORT).show();

        String eventName = etPreCreatedEvent.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String organizerName = etOrganizerName.getText().toString().trim();
        String description = etEventDescription.getText().toString().trim();
        String eligibility = etEligibilityCriteria.getText().toString().trim();
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String waitlistLimit = etWaitlistLimit.getText().toString().trim();
        String entrantMax = etPoolSize.getText().toString().trim();
        boolean geolocationRequired = rgGeolocation.getCheckedRadioButtonId() == R.id.rb_geo_yes;

        Toast.makeText(getContext(), "validating inputs", Toast.LENGTH_SHORT).show();

        if (!validateInputs(eventName, organizerName)) {
            Toast.makeText(getContext(), "validation failed", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "creating event data", Toast.LENGTH_SHORT).show();

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventName", eventName);
        eventData.put("organizer", currentUserId);
        eventData.put("organizerName", organizerName);
        eventData.put("location", location);
        eventData.put("description", description);
        eventData.put("eligibility", eligibility);
        eventData.put("startDate", startDate);
        eventData.put("endDate", endDate);
        eventData.put("price", price.isEmpty() ? "0" : price);
        eventData.put("waitlistLimit", waitlistLimit.isEmpty() ? "0" : waitlistLimit);
        eventData.put("entrantMaxCapacity", entrantMax.isEmpty() ? "0" : entrantMax);
        eventData.put("geolocationRequired", geolocationRequired);
        eventData.put("createdAt", System.currentTimeMillis());

        Toast.makeText(getContext(), "saving to firestore", Toast.LENGTH_SHORT).show();

        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "event created successfully!", Toast.LENGTH_LONG).show();
                    clearForm();
                    navigateToOrganizerDashboard();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }

    /**
     * validates required input fields
     */
    private boolean validateInputs(String eventName, String organizer) {
        boolean isValid = true;

        if (eventName.isEmpty()) {
            etPreCreatedEvent.setError("please enter event name");
            etPreCreatedEvent.requestFocus();
            isValid = false;
        }
        if (organizer.isEmpty()) {
            etOrganizerName.setError("please enter organizer name");
            etOrganizerName.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    /**
     * navigates back to organizer dashboard after successful creation
     */
    private void navigateToOrganizerDashboard() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new OrganizerDashboardFragment())
                    .commit();
        }
    }

    /**
     * clears all form fields
     */
    private void clearForm() {
        etPreCreatedEvent.setText("");
        etLocation.setText("");
        etOrganizerName.setText("");
        etEventDescription.setText("");
        etEligibilityCriteria.setText("");
        etStartDate.setText("");
        etEndDate.setText("");
        etPrice.setText("");
        etWaitlistLimit.setText("");
        etPoolSize.setText("");
        rgGeolocation.check(R.id.rb_geo_yes);
    }
}