package com.example.eventlottery.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.example.eventlottery.models.Image;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * AdminImagesFragment provides an interface for administrators to manage uploaded images.
 * It allows viewing a list of all images, searching/filtering them, and deleting images from the database.
 */
public class AdminImagesFragment extends Fragment {

    private TextView imagesCount;
    private EditText searchImages;
    private LinearLayout imagesList;
    private ProgressBar loadingSpinner;
    private TextView emptyMessage;

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private final List<ImageDataWithEvent> allImages = new ArrayList<>();
    private final List<ImageDataWithEvent> filteredImages = new ArrayList<>();
    private final Map<String, String> eventNameCache = new HashMap<>();
    /**
     * Initializes the fragment's UI components and triggers the data loading process.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_images, container, false);

        db = FirebaseFirestore.getInstance();

        imagesCount = view.findViewById(R.id.imagesCount);
        searchImages = view.findViewById(R.id.searchImages);
        imagesList = view.findViewById(R.id.imagesList);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        emptyMessage = view.findViewById(R.id.emptyMessage);

        setupSearch();
        loadImages();

        return view;
    }
    /**
     * Sets up a TextWatcher on the search bar to filter the image list as the user types.
     */
    private void setupSearch() {
        searchImages.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                filterImages(s.toString());
            }
            @Override public void afterTextChanged(Editable editable) {}
        });
    }
    /**
     * Connects to Firestore to listen for real-time updates to the "images" collection.
     * Fetches image metadata like event name, organizer, and date.
     */
    private void loadImages() {
        showLoading(true);

        listener = db.collection("images")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showLoading(false);
                        showError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        allImages.clear();
                        List<Image> images = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : value) {
                            Image image = Image.fromMap(doc.getId(), doc.getData());
                            images.add(image);
                        }

                        // Fetch event names for all images
                        fetchEventNamesForImages(images);
                    }
                });
    }

    /**
     * Fetches event names for all images and creates ImageDataWithEvent objects
     */
    private void fetchEventNamesForImages(List<Image> images) {
        if (images.isEmpty()) {
            imagesCount.setText("0");
            filterImages(searchImages.getText().toString());
            showLoading(false);
            return;
        }

        // Counter to track when all event names are fetched
        final int[] fetchedCount = {0};
        final int totalImages = images.size();

        for (Image image : images) {
            String eventId = image.getEventId();

            // Check if event name is already cached
            if (eventNameCache.containsKey(eventId)) {
                allImages.add(new ImageDataWithEvent(image, eventNameCache.get(eventId)));
                fetchedCount[0]++;

                if (fetchedCount[0] == totalImages) {
                    onAllEventNamesFetched();
                }
            } else {
                // Fetch event name from Firestore
                db.collection("events").document(eventId)
                        .get()
                        .addOnSuccessListener(eventDoc -> {
                            String eventName = "Unknown Event";
                            if (eventDoc.exists()) {
                                eventName = eventDoc.getString("eventName");
                                if (eventName == null) eventName = "Unknown Event";
                                eventNameCache.put(eventId, eventName);
                            }

                            allImages.add(new ImageDataWithEvent(image, eventName));
                            fetchedCount[0]++;

                            if (fetchedCount[0] == totalImages) {
                                onAllEventNamesFetched();
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Add with unknown event name on failure
                            allImages.add(new ImageDataWithEvent(image, "Unknown Event"));
                            fetchedCount[0]++;

                            if (fetchedCount[0] == totalImages) {
                                onAllEventNamesFetched();
                            }
                        });
            }
        }
    }

    /**
     * Called when all event names have been fetched
     */
    private void onAllEventNamesFetched() {
        imagesCount.setText(String.valueOf(allImages.size()));
        filterImages(searchImages.getText().toString());
        showLoading(false);
    }
    /**
     * Filters the list of images based on the search query.
     * Matches against the event name or organizer name.
     *
     * @param query The search string entered by the user.
     */
    private void filterImages(String query) {
        filteredImages.clear();

        if (query.isEmpty()) {
            filteredImages.addAll(allImages);
        } else {
            String q = query.toLowerCase();
            for (ImageDataWithEvent img : allImages) {
                if (img.eventName.toLowerCase().contains(q) ||
                        img.image.getOrganizerName().toLowerCase().contains(q)) {
                    filteredImages.add(img);
                }
            }
        }

        showImages();
    }
    /**
     * Renders the list of filtered images into the LinearLayout container.
     * Displays an empty message if no images are found.
     */
    private void showImages() {
        imagesList.removeAllViews();

        if (filteredImages.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        emptyMessage.setVisibility(View.GONE);

        for (ImageDataWithEvent img : filteredImages) addImageCard(img);
    }
    /**
     * Inflates and populates a single image card view with data, then adds it to the list.
     * Sets up the delete button listener.
     *
     * @param imgData The ImageDataWithEvent object containing details to display.
     */
    @SuppressLint("SetTextI18n")
    private void addImageCard(ImageDataWithEvent imgData) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_image_card, imagesList, false);

        ImageView imagePreview = card.findViewById(R.id.imagePreview);
        TextView name = card.findViewById(R.id.eventName);
        TextView organizer = card.findViewById(R.id.organizerName);
        TextView info = card.findViewById(R.id.imageInfo);
        Button deleteBtn = card.findViewById(R.id.deleteImageBtn);

        // Decode and display the image
        try {
            byte[] imageBytes = Base64.decode(imgData.image.getImageData(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            imagePreview.setImageBitmap(bitmap);
        } catch (Exception e) {
            // If image fails to load, keep the default background
            imagePreview.setImageResource(android.R.color.transparent);
        }

        name.setText(imgData.eventName);
        organizer.setText("By " + imgData.image.getOrganizerName());

        // Format the upload date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date(imgData.image.getUploadedAt()));
        info.setText("Uploaded: " + formattedDate);

        deleteBtn.setOnClickListener(v -> deleteImage(imgData.image.getImageId(), imgData.eventName));

        imagesList.addView(card);
    }
    /**
     * Shows a confirmation dialog to delete an image. If confirmed, deletes the image from Firestore.
     *
     * @param imageId The unique document ID of the image to delete.
     * @param eventName The name of the event (used in the confirmation message).
     */
    private void deleteImage(String imageId, String eventName) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image from '" + eventName + "'?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Toast.makeText(getContext(), "Deleting image...", Toast.LENGTH_SHORT).show();

                    db.collection("images").document(imageId)
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete image: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    /**
     * Toggles the visibility of the loading spinner and the image list.
     *
     * @param b True to show the loading spinner, false to show the list.
     */
    private void showLoading(boolean b) {
        loadingSpinner.setVisibility(b ? View.VISIBLE : View.GONE);
        imagesList.setVisibility(b ? View.GONE : View.VISIBLE);
    }
    /**
     * Displays a toast message with an error description.
     *
     * @param msg The error message to display.
     */
    private void showError(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
    /**
     * Cleans up resources when the fragment view is destroyed, removing the Firestore listener.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
    /**
     * A simple data model class to hold image information combined with event name for display.
     */
    private static class ImageDataWithEvent {
        Image image;
        String eventName;

        ImageDataWithEvent(Image image, String eventName) {
            this.image = image;
            this.eventName = eventName;
        }
    }
}
