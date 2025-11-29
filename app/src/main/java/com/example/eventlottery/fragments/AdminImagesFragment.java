package com.example.eventlottery.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
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

    private final List<ImageData> allImages = new ArrayList<>();
    private final List<ImageData> filteredImages = new ArrayList<>();
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
     * Fetches image metadata like event name, uploader, and date.
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

                        for (QueryDocumentSnapshot doc : value) {
                            String id = doc.getId();
                            String event = doc.getString("eventName");
                            String uploader = doc.getString("uploader");
                            String date = doc.getString("date");

                            allImages.add(new ImageData(id,
                                    event != null ? event : "Unknown Event",
                                    uploader != null ? uploader : "Unknown",
                                    date != null ? date : "Unknown Date"));
                        }

                        imagesCount.setText(String.valueOf(allImages.size()));
                        filterImages(searchImages.getText().toString());

                        showLoading(false);
                    }
                });
    }
    /**
     * Filters the list of images based on the search query.
     * Matches against the event name or uploader name.
     *
     * @param query The search string entered by the user.
     */
    private void filterImages(String query) {
        filteredImages.clear();

        if (query.isEmpty()) {
            filteredImages.addAll(allImages);
        } else {
            String q = query.toLowerCase();
            for (ImageData img : allImages) {
                if (img.eventName.toLowerCase().contains(q) ||
                        img.uploader.toLowerCase().contains(q)) {
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

        for (ImageData img : filteredImages) addImageCard(img);
    }
    /**
     * Inflates and populates a single image card view with data, then adds it to the list.
     * Sets up the delete button listener.
     *
     * @param img The ImageData object containing details to display.
     */
    @SuppressLint("SetTextI18n")
    private void addImageCard(ImageData img) {

        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_image_card, imagesList, false);

        TextView name = card.findViewById(R.id.eventName);
        TextView uploader = card.findViewById(R.id.uploaderName);
        TextView info = card.findViewById(R.id.imageInfo);
        Button viewBtn = card.findViewById(R.id.viewImageBtn);
        Button deleteBtn = card.findViewById(R.id.deleteImageBtn);

        name.setText(img.eventName);
        uploader.setText("Uploaded by " + img.uploader);
        info.setText("Date: " + img.date);

        viewBtn.setOnClickListener(v -> Toast.makeText(getContext(), "Open Image Viewer (not built)", Toast.LENGTH_SHORT).show());

        deleteBtn.setOnClickListener(v -> deleteImage(img.id));

        imagesList.addView(card);
    }
    /**
     * Deletes the specified image document from the Firestore "images" collection.
     *
     * @param id The unique document ID of the image to delete.
     */
    private void deleteImage(String id) {
        db.collection("images").document(id)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Image deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
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
     * A simple data model class to hold image information for display.
     */
    private static class ImageData {
        String id, eventName, uploader, date;

        ImageData(String i, String e, String u, String d) {
            id = i;
            eventName = e;
            uploader = u;
            date = d;
        }
    }
}
