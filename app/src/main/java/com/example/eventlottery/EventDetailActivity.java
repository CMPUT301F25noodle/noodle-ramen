package com.example.eventlottery;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.eventlottery.managers.ImageManager;
import com.example.eventlottery.models.Image;
import com.example.eventlottery.utils.ImageCompressionHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.firebase.firestore.FieldValue;
import com.example.eventlottery.managers.WaitlistManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows event details and its QR code.
 * Fetches data from firebase and handles waitlist joining with optional geolocation.
 */
public class EventDetailActivity extends AppCompatActivity {

    private TextView eventTitle, eventDescription, eventCriteria;
    private TextView statusBadge, priceText, locationText, dateText;
    private TextView waitlistInfo, spotsText;
    private ImageView qrCodeImage, backButton;
    private ImageView eventMainImage;
    private TextView imagePlaceholder;
    private Button joinWaitlistButton;
    private android.widget.ProgressBar joinButtonProgress;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageManager imageManager;

    private boolean isGeolocationRequired = false; // Track if this specific event needs location
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private WaitlistManager waitlistManager;
    private String currentEventId = null;

    private boolean hasJoined = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_event_detail);

        initializeViews();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        imageManager = ImageManager.getInstance();

        // Setup back button
        backButton.setOnClickListener(v -> finish());

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        waitlistManager = WaitlistManager.getInstance();
        currentEventId = eventId;

        loadEvent(eventId);
        loadEventImages(eventId);
    }

    private void initializeViews() {
        eventTitle = findViewById(R.id.event_title);
        eventDescription = findViewById(R.id.event_description);
        eventCriteria = findViewById(R.id.event_criteria);
        statusBadge = findViewById(R.id.status_badge);
        priceText = findViewById(R.id.price_text);
        locationText = findViewById(R.id.location_text);
        dateText = findViewById(R.id.date_text);
        waitlistInfo = findViewById(R.id.waitlist_info);
        spotsText = findViewById(R.id.spots_text);
        qrCodeImage = findViewById(R.id.qr_code_image);
        joinWaitlistButton = findViewById(R.id.join_waitlist_button);
        backButton = findViewById(R.id.back_button);
        eventMainImage = findViewById(R.id.eventMainImage);
        imagePlaceholder = findViewById(R.id.imagePlaceholder);
        joinButtonProgress = findViewById(R.id.join_button_progress);
    }

    private void loadEvent(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String eventName = doc.getString("eventName");
                        String description = doc.getString("description");
                        String eligibility = doc.getString("eligibility");
                        String location = doc.getString("location");
                        String startDate = doc.getString("startDate");
                        String endDate = doc.getString("endDate");
                        String priceStr = doc.getString("price");
                        String waitlistLimitStr = doc.getString("waitlistLimit");
                        String entrantMaxStr = doc.getString("entrantMaxCapacity");

                        Boolean geoReq = doc.getBoolean("geolocationRequired");
                        isGeolocationRequired = geoReq != null && geoReq;

                        eventTitle.setText(eventName != null ? eventName : "Untitled Event");
                        eventDescription.setText(description != null ? description : "No description available");
                        eventCriteria.setText(eligibility != null ? eligibility : "No specific criteria");
                        locationText.setText(location != null ? location : "TBD");

                        if (startDate != null && endDate != null) {
                            dateText.setText(startDate + " - " + endDate);
                        } else {
                            dateText.setText("Date TBD");
                        }

                        priceText.setText((priceStr != null && !priceStr.isEmpty()) ? "$" + priceStr : "Free");
                        statusBadge.setText("Open");

                        int waitlistLimit = waitlistLimitStr != null ? Integer.parseInt(waitlistLimitStr) : 0;
                        waitlistInfo.setText("Waitlist limit: " + waitlistLimit);

                        int entrantMax = entrantMaxStr != null ? Integer.parseInt(entrantMaxStr) : 0;
                        spotsText.setText(entrantMax + " spots");


                        if (auth.getCurrentUser() != null) {
                            List<String> waitlistUsers = (List<String>) doc.get("waitlistUsers");
                            String currentUserId = auth.getCurrentUser().getUid();

                            // Check if list exists and contains user ID
                            if (waitlistUsers != null && waitlistUsers.contains(currentUserId)) {
                                hasJoined = true;
                            } else {
                                hasJoined = false;
                            }
                            // Update button immediately
                            updateButtonState();
                        }

                        // Set Join Button Listener
                        joinWaitlistButton.setOnClickListener(v -> handleJoinClick(eventId));

                        // Generate QR code
                        String qrPayload = "eventlottery://event/" + eventId;
                        Bitmap qr = createQrBitmap(qrPayload, 400);
                        if (qr != null) qrCodeImage.setImageBitmap(qr);


                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Update button text and state based on hasJoined flag
     */
    private void updateButtonState() {
        updateButtonState(false);
    }

    /**
     * Update button state with optional loading indicator
     */
    private void updateButtonState(boolean isLoading) {
        if (isLoading) {
            joinWaitlistButton.setText("");
            joinWaitlistButton.setEnabled(false);
            joinButtonProgress.setVisibility(View.VISIBLE);
        } else {
            joinButtonProgress.setVisibility(View.GONE);
            if (hasJoined) {
                joinWaitlistButton.setText("Leave Waitlist");
                joinWaitlistButton.setEnabled(true);
            } else {
                joinWaitlistButton.setText("Join Waitlist");
                joinWaitlistButton.setEnabled(true);
            }
        }
    }

    /**
     * Decides whether to join or leave the waitlist based on current state
     */
    private void handleJoinClick(String eventId) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to join", Toast.LENGTH_SHORT).show();
            return;
        }

        currentEventId = eventId; // Store for permission callback

        // Check if user has already joined - if so, leave the waitlist
        if (hasJoined) {
            performLeaveWaitlist(eventId);
            return;
        }

        // User hasn't joined yet, proceed with join logic
        if (isGeolocationRequired) {
            // Check if we already have permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request Permission
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted
                joinWithLocation(eventId);
            }
        } else {
            // Geolocation NOT required, join without it
            updateButtonState(true);
            performJoinWaitlist(eventId, null, null);
        }
    }

    /**
     * Handle the result of the permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to join
                if (currentEventId != null) {
                    joinWithLocation(currentEventId);
                }
            } else {
                Toast.makeText(this, "Location permission is required to join this event.", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void joinWithLocation(String eventId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Show loading state
        updateButtonState(true);

        // Try to get last location first (faster)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // We have a recent location, use it
                        processLocationAndJoin(eventId, location);
                    } else {
                        // No cached location, request current location
                        requestCurrentLocation(eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to get last location, try current location
                    Log.w(TAG, "Failed to get last location, requesting current location", e);
                    requestCurrentLocation(eventId);
                });
    }

    /**
     * Request current location actively (not from cache)
     */
    private void requestCurrentLocation(String eventId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        com.google.android.gms.tasks.CancellationTokenSource cancellationTokenSource =
            new com.google.android.gms.tasks.CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        processLocationAndJoin(eventId, location);
                    } else {
                        // Clear loading state
                        updateButtonState(false);
                        Toast.makeText(this,
                                "Unable to get location. Please ensure GPS is enabled and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Clear loading state
                    updateButtonState(false);
                    Log.e(TAG, "Error getting current location", e);
                    Toast.makeText(this,
                            "Error getting location: " + e.getMessage() + ". Please ensure GPS is enabled.",
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Process the location and join the waitlist
     */
    private void processLocationAndJoin(String eventId, android.location.Location location) {
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Get user name for location tracking
        db.collection("users").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String userName = userDoc.exists() ? userDoc.getString("name") : "Unknown";
                    performJoinWaitlist(eventId, geoPoint, userName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user data", e);
                    performJoinWaitlist(eventId, geoPoint, "Unknown");
                });
    }

    /**
     * Actually perform the join operation using WaitlistManager
     */
    private void performJoinWaitlist(String eventId, GeoPoint location, String userName) {
        waitlistManager.joinWaitlist(eventId, location, userName, new WaitlistManager.WaitlistCallback() {
            @Override
            public void onSuccess() {
                hasJoined = true;
                updateButtonState(false);
            }

            @Override
            public void onFailure(String error) {
                updateButtonState(false);
                Log.e(TAG, "Error joining waitlist" + error);
                Toast.makeText(EventDetailActivity.this, "Failed to join: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Leave the waitlist
     */
    private void performLeaveWaitlist(String eventId) {
        // Show loading state
        updateButtonState(true);

        waitlistManager.leaveWaitlist(eventId, new WaitlistManager.WaitlistCallback() {
            @Override
            public void onSuccess() {
                hasJoined = false;
                updateButtonState(false);
            }

            @Override
            public void onFailure(String error) {
                updateButtonState(false);
                Log.e(TAG, "Error leaving waitlist: " + error);
                Toast.makeText(EventDetailActivity.this, "Failed to leave waitlist: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }



    private Bitmap createQrBitmap(String content, int sizePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < sizePx; y++) {
                for (int x = 0; x < sizePx; x++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load and display event images
     */
    private void loadEventImages(String eventId) {
        imageManager.getImagesForEvent(eventId, new ImageManager.ImageListCallback() {
            @Override
            public void onSuccess(List<Image> images) {
                if (!images.isEmpty()) {
                    // Get the first image's data
                    String imageData = images.get(0).getImageData();

                    if (imageData != null && !imageData.isEmpty()) {
                        // Decode Base64 to Bitmap
                        Bitmap bitmap = ImageCompressionHelper.decodeFromBase64(imageData);

                        if (bitmap != null) {
                            // Show image, hide placeholder
                            eventMainImage.setVisibility(View.VISIBLE);
                            imagePlaceholder.setVisibility(View.GONE);

                            // Load with Glide
                            Glide.with(EventDetailActivity.this)
                                    .load(bitmap)
                                    .centerCrop()
                                    .into(eventMainImage);
                        }
                    }
                } else {
                    // No images, keep placeholder visible
                    eventMainImage.setVisibility(View.GONE);
                    imagePlaceholder.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load event images: " + error);
                // Keep placeholder visible on error
                eventMainImage.setVisibility(View.GONE);
                imagePlaceholder.setVisibility(View.VISIBLE);
            }
        });
    }
}