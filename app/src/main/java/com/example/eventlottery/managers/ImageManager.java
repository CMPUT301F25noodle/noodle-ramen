package com.example.eventlottery.managers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.eventlottery.models.Image;
import com.example.eventlottery.utils.ImageCompressionHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager for handling all image operations:
 * - Compress and encode images
 * - Store image data in Firestore
 * - Retrieve images for events
 * - Delete images from Firestore
 */
public class ImageManager {
    private static final String TAG = "ImageManager";
    private static final int MAX_IMAGES_PER_EVENT = 3;
    private static final String IMAGES_COLLECTION = "images";

    private static ImageManager instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private ImageManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static ImageManager getInstance() {
        if (instance == null) {
            instance = new ImageManager();
        }
        return instance;
    }

    /**
     * Upload an image for an event (compresses and stores in Firestore)
     * @param context Application context for image compression
     * @param eventId The event ID this image belongs to
     * @param imageUri The URI of the image to upload
     * @param organizerName The name of the organizer uploading the image
     * @param callback Callback with the uploaded Image object or error
     */
    public void uploadImage(Context context, String eventId, Uri imageUri, String organizerName, ImageUploadCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Check if event already has 3 images
        getImagesForEvent(eventId, new ImageListCallback() {
            @Override
            public void onSuccess(List<Image> images) {
                if (images.size() >= MAX_IMAGES_PER_EVENT) {
                    callback.onFailure("Maximum of " + MAX_IMAGES_PER_EVENT + " images per event reached");
                    return;
                }

                // Proceed with compression and upload
                performUpload(context, eventId, imageUri, userId, organizerName, callback);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    private void performUpload(Context context, String eventId, Uri imageUri, String userId, String organizerName, ImageUploadCallback callback) {
        // Generate unique image ID
        String imageId = db.collection(IMAGES_COLLECTION).document().getId();
        long timestamp = System.currentTimeMillis();
        String fileName = "image_" + timestamp + ".jpg";

        // Compress and encode the image in background
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting image compression for: " + imageUri);

                // Compress and encode to Base64
                String imageData = ImageCompressionHelper.compressAndEncode(context, imageUri);

                if (imageData == null) {
                    // Post failure to main thread
                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                    mainHandler.post(() -> callback.onFailure("Failed to compress image"));
                    return;
                }

                Log.d(TAG, "Image compressed successfully, storing in Firestore...");

                // Create Image object
                Image image = new Image(
                        imageId,
                        eventId,
                        userId,
                        organizerName,
                        imageData,
                        timestamp,
                        fileName
                );

                // Store in Firestore on main thread
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    db.collection(IMAGES_COLLECTION)
                            .document(imageId)
                            .set(image.toMap())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Image uploaded successfully: " + imageId);
                                callback.onSuccess(image);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to store image in Firestore", e);
                                callback.onFailure("Failed to store image: " + e.getMessage());
                            });
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during image compression", e);
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> callback.onFailure("Error compressing image: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Get all images for a specific event
     * @param eventId The event ID
     * @param callback Callback with list of Image objects
     */
    public void getImagesForEvent(String eventId, ImageListCallback callback) {
        db.collection(IMAGES_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Image> images = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Image image = Image.fromMap(doc.getId(), doc.getData());
                        images.add(image);
                    }
                    Log.d(TAG, "Retrieved " + images.size() + " images for event " + eventId);
                    callback.onSuccess(images);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get images for event", e);
                    callback.onFailure("Failed to retrieve images: " + e.getMessage());
                });
    }

    /**
     * Delete an image from Firestore
     * @param image The Image object to delete
     * @param callback Callback indicating success or failure
     */
    public void deleteImage(Image image, ImageDeleteCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Check if user is the organizer who uploaded this image
        if (!image.getOrganizerId().equals(userId)) {
            callback.onFailure("You can only delete your own images");
            return;
        }

        // Delete from Firestore
        db.collection(IMAGES_COLLECTION)
                .document(image.getImageId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image deleted successfully: " + image.getImageId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete image", e);
                    callback.onFailure("Failed to delete image: " + e.getMessage());
                });
    }

    /**
     * Delete all images for an event (used when an event is deleted)
     * @param eventId The event ID
     * @param callback Callback indicating success or failure
     */
    public void deleteAllImagesForEvent(String eventId, ImageDeleteCallback callback) {
        getImagesForEvent(eventId, new ImageListCallback() {
            @Override
            public void onSuccess(List<Image> images) {
                if (images.isEmpty()) {
                    callback.onSuccess();
                    return;
                }

                // Delete all images
                List<Task<Void>> deleteTasks = new ArrayList<>();
                for (Image image : images) {
                    Task<Void> deleteTask = db.collection(IMAGES_COLLECTION)
                            .document(image.getImageId())
                            .delete();
                    deleteTasks.add(deleteTask);
                }

                // Wait for all deletions to complete
                Tasks.whenAllComplete(deleteTasks)
                        .addOnSuccessListener(tasks -> {
                            Log.d(TAG, "All images deleted for event: " + eventId);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete some images", e);
                            callback.onFailure("Failed to delete some images: " + e.getMessage());
                        });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Get images uploaded by a specific organizer (useful for admin functionality)
     * @param organizerId The organizer's user ID
     * @param callback Callback with list of Image objects
     */
    public void getImagesByOrganizer(String organizerId, ImageListCallback callback) {
        db.collection(IMAGES_COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Image> images = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Image image = Image.fromMap(doc.getId(), doc.getData());
                        images.add(image);
                    }
                    Log.d(TAG, "Retrieved " + images.size() + " images for organizer " + organizerId);
                    callback.onSuccess(images);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get images for organizer", e);
                    callback.onFailure("Failed to retrieve images: " + e.getMessage());
                });
    }

    // Callback interfaces
    public interface ImageUploadCallback {
        void onSuccess(Image image);
        void onFailure(String error);
    }

    public interface ImageListCallback {
        void onSuccess(List<Image> images);
        void onFailure(String error);
    }

    public interface ImageDeleteCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
