package com.example.eventlottery.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Helper class for compressing and encoding images for Firestore storage.
 * Resizes images to reasonable dimensions and compresses them to stay under Firestore's 1MB limit.
 */
public class ImageCompressionHelper {
    private static final String TAG = "ImageCompression";

    // Target dimensions for compressed images
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;

    // JPEG quality (0-100, higher = better quality but larger size)
    private static final int JPEG_QUALITY = 75;

    // Maximum size in bytes (~900KB to leave room for other document data)
    private static final int MAX_SIZE_BYTES = 900 * 1024;

    /**
     * Compress an image from URI and convert to Base64 string for Firestore storage
     *
     * @param context Application context for accessing content resolver
     * @param imageUri URI of the image to compress
     * @return Base64 encoded string of the compressed image, or null on error
     */
    public static String compressAndEncode(Context context, Uri imageUri) {
        try {
            // Step 1: Load the image from URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + imageUri);
                return null;
            }

            // Step 2: Decode the image into a Bitmap
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: " + imageUri);
                return null;
            }

            Log.d(TAG, "Original image size: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

            // Step 3: Resize the bitmap to target dimensions
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, MAX_WIDTH, MAX_HEIGHT);

            // Recycle original bitmap to free memory
            if (resizedBitmap != originalBitmap) {
                originalBitmap.recycle();
            }

            Log.d(TAG, "Resized image size: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());

            // Step 4: Compress to JPEG format
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // Recycle resized bitmap
            resizedBitmap.recycle();

            Log.d(TAG, "Compressed image size: " + imageBytes.length + " bytes (" +
                  (imageBytes.length / 1024) + " KB)");

            // Step 5: Check if size is acceptable
            if (imageBytes.length > MAX_SIZE_BYTES) {
                Log.w(TAG, "Compressed image exceeds max size. Attempting further compression...");
                // Try with lower quality
                return compressWithQuality(resizedBitmap, 60);
            }

            // Step 6: Convert to Base64 string
            String base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            Log.d(TAG, "Base64 encoded string length: " + base64String.length() + " characters");

            return base64String;

        } catch (Exception e) {
            Log.e(TAG, "Error compressing and encoding image", e);
            return null;
        }
    }

    /**
     * Resize a bitmap to fit within maximum dimensions while maintaining aspect ratio
     *
     * @param original Original bitmap to resize
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Resized bitmap
     */
    private static Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Calculate aspect ratio
        float aspectRatio = (float) width / height;

        // Calculate new dimensions
        int newWidth, newHeight;
        if (width > height) {
            // Landscape orientation
            newWidth = Math.min(width, maxWidth);
            newHeight = Math.round(newWidth / aspectRatio);
        } else {
            // Portrait orientation
            newHeight = Math.min(height, maxHeight);
            newWidth = Math.round(newHeight * aspectRatio);
        }

        // Only resize if image is larger than target dimensions
        if (newWidth < width || newHeight < height) {
            return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        }

        return original;
    }

    /**
     * Compress bitmap with specific quality
     *
     * @param bitmap Bitmap to compress
     * @param quality JPEG quality (0-100)
     * @return Base64 encoded string
     */
    private static String compressWithQuality(Bitmap bitmap, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        byte[] imageBytes = outputStream.toByteArray();

        Log.d(TAG, "Recompressed with quality " + quality + ": " +
              imageBytes.length + " bytes (" + (imageBytes.length / 1024) + " KB)");

        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Decode a Base64 string back to a Bitmap
     *
     * @param base64String Base64 encoded image string
     * @return Decoded Bitmap, or null on error
     */
    public static Bitmap decodeFromBase64(String base64String) {
        try {
            byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding Base64 to bitmap", e);
            return null;
        }
    }
}
