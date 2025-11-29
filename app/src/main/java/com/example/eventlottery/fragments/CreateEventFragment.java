package com.example.eventlottery.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import java.util.Arrays;

import com.bumptech.glide.Glide;
import com.example.eventlottery.R;
import com.example.eventlottery.QrGenerator;
import com.example.eventlottery.managers.ImageManager;
import com.example.eventlottery.models.Image;
import com.example.eventlottery.utils.ImageCompressionHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
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
    private Spinner spinnerCategory;
    private RadioGroup rgGeolocation;
    private Button btnAddImage, btnDone, btnCancel;
    private ImageView btnDeleteImage1, btnDeleteImage2, btnDeleteImage3;
    private ImageView ivEventImage1, ivEventImage2, ivEventImage3;
    private LinearLayout llImageSlot1, llImageSlot2, llImageSlot3;
    private ImageButton btnLocation;

    // firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ImageManager imageManager;

    // image management
    private ActivityResultLauncher<String[]> imagePickerLauncher;
    private List<Image> uploadedImages = new ArrayList<>();
    private List<Uri> selectedImageUris = new ArrayList<>(); // Store URIs before upload

    private String eventId = null;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<Intent> locationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1) { // Activity.RESULT_OK
                    Intent data = result.getData();
                    if (data != null) {
                        com.google.android.libraries.places.api.model.Place place =
                                com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(data);

                        if (etLocation != null) {
                            etLocation.setText(place.getAddress());
                        }
                    }
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                    // --- THIS IS THE MISSING PART ---
                    Intent data = result.getData();
                    com.google.android.gms.common.api.Status status =
                            com.google.android.libraries.places.widget.Autocomplete.getStatusFromIntent(data);

                    String message = "Places Error: " + status.getStatusMessage();
                    Log.e("Places", message);
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                } else if (result.getResultCode() == 0) { // RESULT_CANCELED
                    // User closed the search without selecting anything
                }
            });

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
        imageManager = ImageManager.getInstance();
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(requireContext(), "AIzaSyAQE0X0FVaZeOnI6v2FHNGbz6y4Tz6H1ek");
        }

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        // Initialize image picker launcher with OpenDocument (supports persistable permissions)
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        // Take persistable URI permission for long-term access
                        try {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (SecurityException e) {
                            Log.e("CreateEventFragment", "Failed to take persistable permission", e);
                        }
                        handleImageSelected(uri);
                    }
                });

        initializeViews(view);
        setupListeners();

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId != null && !eventId.isEmpty()) {
                isEditMode = true;
                btnDone.setText("Save Changes");
                loadEventData(eventId);
                loadEventImages(eventId);
            }
        }

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
        spinnerCategory = view.findViewById(R.id.spinner_category);
        rgGeolocation = view.findViewById(R.id.rg_geolocation);

        // Setup category spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.event_categories,
                android.R.layout.simple_spinner_item
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        btnAddImage = view.findViewById(R.id.btn_add_image);
        btnDone = view.findViewById(R.id.btn_done);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnDeleteImage1 = view.findViewById(R.id.btn_delete_image1);
        btnDeleteImage2 = view.findViewById(R.id.btn_delete_image2);
        btnDeleteImage3 = view.findViewById(R.id.btn_delete_image3);
        btnLocation = view.findViewById(R.id.btn_location);

        // Initialize image views and containers
        ivEventImage1 = view.findViewById(R.id.iv_event_image_1);
        ivEventImage2 = view.findViewById(R.id.iv_event_image_2);
        ivEventImage3 = view.findViewById(R.id.iv_event_image_3);
        llImageSlot1 = view.findViewById(R.id.ll_image_slot_1);
        llImageSlot2 = view.findViewById(R.id.ll_image_slot_2);
        llImageSlot3 = view.findViewById(R.id.ll_image_slot_3);
    }

    /**
     * sets up button actions
     */
    private void setupListeners() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnLocation.setOnClickListener(v -> {
            List<com.google.android.libraries.places.api.model.Place.Field> fields = Arrays.asList(
                    com.google.android.libraries.places.api.model.Place.Field.ID,
                    com.google.android.libraries.places.api.model.Place.Field.NAME,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS
            );

            // Launch the search overlay
            Intent intent = new com.google.android.libraries.places.widget.Autocomplete.IntentBuilder(
                    com.google.android.libraries.places.widget.model.AutocompleteActivityMode.OVERLAY, fields)
                    .build(requireContext());
            locationPickerLauncher.launch(intent);
        });



        btnAddImage.setOnClickListener(v -> {
            int totalImages = isEditMode ? uploadedImages.size() : selectedImageUris.size();
            if (totalImages >= 3) {
                Toast.makeText(getContext(), "Maximum of 3 images allowed", Toast.LENGTH_SHORT).show();
            } else {
                // OpenDocument requires an array of MIME types
                imagePickerLauncher.launch(new String[]{"image/*"});
            }
        });

        btnDeleteImage1.setOnClickListener(v -> deleteImageAtIndex(0));
        btnDeleteImage2.setOnClickListener(v -> deleteImageAtIndex(1));
        btnDeleteImage3.setOnClickListener(v -> deleteImageAtIndex(2));

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
     * loads event data from Firestore for editing
     */
    private void loadEventData(String eventId) {
        Toast.makeText(getContext(), "Loading event data...", Toast.LENGTH_SHORT).show();

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate form fields with existing data
                        etPreCreatedEvent.setText(documentSnapshot.getString("eventName"));
                        etLocation.setText(documentSnapshot.getString("location"));
                        etOrganizerName.setText(documentSnapshot.getString("organizerName"));
                        etEventDescription.setText(documentSnapshot.getString("description"));
                        etEligibilityCriteria.setText(documentSnapshot.getString("eligibility"));
                        etStartDate.setText(documentSnapshot.getString("startDate"));
                        etEndDate.setText(documentSnapshot.getString("endDate"));

                        String price = documentSnapshot.getString("price");
                        if (price != null && !price.equals("0")) {
                            etPrice.setText(price);
                        }

                        String waitlistLimit = documentSnapshot.getString("waitlistLimit");
                        if (waitlistLimit != null && !waitlistLimit.equals("0")) {
                            etWaitlistLimit.setText(waitlistLimit);
                        }

                        String entrantMax = documentSnapshot.getString("entrantMaxCapacity");
                        if (entrantMax != null && !entrantMax.equals("0")) {
                            etPoolSize.setText(entrantMax);
                        }

                        Boolean geolocationRequired = documentSnapshot.getBoolean("geolocationRequired");
                        if (geolocationRequired != null && geolocationRequired) {
                            rgGeolocation.check(R.id.rb_geo_yes);
                        } else {
                            rgGeolocation.check(R.id.rb_geo_no);
                        }

                        Toast.makeText(getContext(), "Event loaded successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        navigateToOrganizerDashboard();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    navigateToOrganizerDashboard();
                });
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

        String category = spinnerCategory.getSelectedItem().toString();

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
        eventData.put("category", category);
        eventData.put("createdAt", System.currentTimeMillis());

        Toast.makeText(getContext(), "saving to firestore", Toast.LENGTH_SHORT).show();

        if (!isEditMode) {
            eventData.put("createdAt", System.currentTimeMillis());

            db.collection("events")
                    .add(eventData)
                    .addOnSuccessListener(documentReference -> {
                        String newEventId = documentReference.getId();

                        // Check if there are images to upload
                        if (!selectedImageUris.isEmpty()) {
                            Toast.makeText(getContext(), "Event created! Uploading images...", Toast.LENGTH_SHORT).show();
                            uploadSelectedImages(newEventId, organizerName);
                        } else {
                            Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_LONG).show();
                            clearForm();
                            navigateToOrganizerDashboard();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    });
        } else {


            db.collection("events")
                    .document(eventId)
                    .update(eventData)  // Updates existing event
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "event updated successfully!", Toast.LENGTH_LONG).show();
                        clearForm();
                        navigateToOrganizerDashboard();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "failed to update event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    });
        }
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
        uploadedImages.clear();
        selectedImageUris.clear();
        displayImages();
    }

    /**
     * Handles image selection from gallery
     */
    private void handleImageSelected(Uri imageUri) {
        if (isEditMode && eventId != null && !eventId.isEmpty()) {
            // Edit mode: Upload immediately
            String organizerName = etOrganizerName.getText().toString().trim();
            if (organizerName.isEmpty()) {
                organizerName = "Organizer";
            }

            Toast.makeText(getContext(), "Compressing and uploading image...", Toast.LENGTH_SHORT).show();

            imageManager.uploadImage(requireContext(), eventId, imageUri, organizerName, new ImageManager.ImageUploadCallback() {
                @Override
                public void onSuccess(Image image) {
                    uploadedImages.add(image);
                    displayImages();
                    Toast.makeText(getContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Create mode: Store URI for later upload
            selectedImageUris.add(imageUri);
            displaySelectedImages();
            Toast.makeText(getContext(), "Image added (will upload when event is created)", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load existing images for an event when editing
     */
    private void loadEventImages(String eventId) {
        imageManager.getImagesForEvent(eventId, new ImageManager.ImageListCallback() {
            @Override
            public void onSuccess(List<Image> images) {
                uploadedImages.clear();
                uploadedImages.addAll(images);
                displayImages();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Failed to load images: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Display uploaded images in the UI
     */
    private void displayImages() {
        // Hide all slots first
        llImageSlot1.setVisibility(View.GONE);
        llImageSlot2.setVisibility(View.GONE);
        llImageSlot3.setVisibility(View.GONE);

        // Display images based on how many we have
        for (int i = 0; i < uploadedImages.size() && i < 3; i++) {
            Image image = uploadedImages.get(i);
            ImageView imageView;
            LinearLayout slot;

            switch (i) {
                case 0:
                    imageView = ivEventImage1;
                    slot = llImageSlot1;
                    break;
                case 1:
                    imageView = ivEventImage2;
                    slot = llImageSlot2;
                    break;
                case 2:
                    imageView = ivEventImage3;
                    slot = llImageSlot3;
                    break;
                default:
                    continue;
            }

            // Decode and load image using Glide
            if (getContext() != null && image.getImageData() != null) {
                android.graphics.Bitmap bitmap = ImageCompressionHelper.decodeFromBase64(image.getImageData());
                if (bitmap != null) {
                    Glide.with(getContext())
                            .load(bitmap)
                            .centerCrop()
                            .into(imageView);
                }
            }
            slot.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Delete image at specified index
     */
    private void deleteImageAtIndex(int index) {
        if (isEditMode) {
            // Edit mode: Delete from Firebase
            if (index >= uploadedImages.size()) {
                return;
            }

            Image imageToDelete = uploadedImages.get(index);
            Toast.makeText(getContext(), "Deleting image...", Toast.LENGTH_SHORT).show();

            imageManager.deleteImage(imageToDelete, new ImageManager.ImageDeleteCallback() {
                @Override
                public void onSuccess() {
                    uploadedImages.remove(index);
                    displayImages();
                    Toast.makeText(getContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Delete failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Create mode: Remove from selected URIs
            if (index >= selectedImageUris.size()) {
                return;
            }
            selectedImageUris.remove(index);
            displaySelectedImages();
            Toast.makeText(getContext(), "Image removed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Display selected images (before upload) in create mode
     */
    private void displaySelectedImages() {
        // Hide all slots first
        llImageSlot1.setVisibility(View.GONE);
        llImageSlot2.setVisibility(View.GONE);
        llImageSlot3.setVisibility(View.GONE);

        // Display selected URIs
        for (int i = 0; i < selectedImageUris.size() && i < 3; i++) {
            Uri imageUri = selectedImageUris.get(i);
            ImageView imageView;
            LinearLayout slot;

            switch (i) {
                case 0:
                    imageView = ivEventImage1;
                    slot = llImageSlot1;
                    break;
                case 1:
                    imageView = ivEventImage2;
                    slot = llImageSlot2;
                    break;
                case 2:
                    imageView = ivEventImage3;
                    slot = llImageSlot3;
                    break;
                default:
                    continue;
            }

            // Load image using Glide from URI
            if (getContext() != null) {
                Glide.with(getContext())
                        .load(imageUri)
                        .centerCrop()
                        .into(imageView);
            }
            slot.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Upload all selected images after event creation
     */
    private void uploadSelectedImages(String newEventId, String organizerName) {
        final int totalImages = selectedImageUris.size();
        final int[] uploadedCount = {0};
        final int[] failedCount = {0};

        for (Uri imageUri : selectedImageUris) {
            imageManager.uploadImage(requireContext(), newEventId, imageUri, organizerName, new ImageManager.ImageUploadCallback() {
                @Override
                public void onSuccess(Image image) {
                    uploadedCount[0]++;
                    checkUploadComplete(uploadedCount[0], failedCount[0], totalImages);
                }

                @Override
                public void onFailure(String error) {
                    failedCount[0]++;
                    checkUploadComplete(uploadedCount[0], failedCount[0], totalImages);
                }
            });
        }
    }

    /**
     * Check if all images have been uploaded and show result
     */
    private void checkUploadComplete(int uploaded, int failed, int total) {
        if (uploaded + failed == total) {
            if (failed == 0) {
                Toast.makeText(getContext(), "Event and images created successfully!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Event created. " + failed + " image(s) failed to upload.", Toast.LENGTH_LONG).show();
            }
            clearForm();
            navigateToOrganizerDashboard();
        }
    }
}