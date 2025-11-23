package com.example.eventlottery;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

import java.util.HashMap;
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
    private Button joinWaitlistButton, shareButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isGeolocationRequired = false; // Track if this specific event needs location
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_event_detail);

        initializeViews();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup back button
        backButton.setOnClickListener(v -> finish());

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEvent(eventId);
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
        shareButton = findViewById(R.id.share_button);
        backButton = findViewById(R.id.back_button);
    }

    private void loadEvent(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // 1. Get Event Data
                        String eventName = doc.getString("eventName");
                        String description = doc.getString("description");
                        String eligibility = doc.getString("eligibility");
                        String location = doc.getString("location");
                        String startDate = doc.getString("startDate");
                        String endDate = doc.getString("endDate");
                        String priceStr = doc.getString("price");
                        String waitlistLimitStr = doc.getString("waitlistLimit");
                        String entrantMaxStr = doc.getString("entrantMaxCapacity");

                        // 2. Check Geolocation Requirement
                        Boolean geoReq = doc.getBoolean("geolocationRequired");
                        isGeolocationRequired = geoReq != null && geoReq;

                        // UI Updates
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

                        // 3. Set Join Button Logic
                        // We check permissions/requirements inside handleJoinClick
                        joinWaitlistButton.setOnClickListener(v -> handleJoinClick(eventId));

                        // Generate QR code
                        String qrPayload = "eventlottery://event/" + eventId;
                        Bitmap qr = createQrBitmap(qrPayload, 400);
                        if (qr != null) qrCodeImage.setImageBitmap(qr);

                        shareButton.setOnClickListener(v -> {
                            Toast.makeText(this, "Share functionality coming soon", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Decides whether to ask for location or join directly
     */
    private void handleJoinClick(String eventId) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to join", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGeolocationRequired) {
            // Check if we already have permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request Permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted
                joinWithLocation(eventId);
            }
        } else {
            // Geolocation NOT required, join without it
            joinWaitlistAndStoreLocation(eventId, auth.getCurrentUser().getUid(), null);
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
                String eventId = getIntent().getStringExtra("eventId");
                if (eventId != null) {
                    joinWithLocation(eventId);
                }
            } else {
                Toast.makeText(this, "Location is required to join this event.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void joinWithLocation(String eventId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        joinWaitlistAndStoreLocation(eventId, auth.getCurrentUser().getUid(), geoPoint);
                    } else {
                        Toast.makeText(this, "Unable to determine location. Ensure GPS is on.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting location", e);
                    Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void joinWaitlistAndStoreLocation(String eventId, String userId, GeoPoint location) {
        // First get user's name
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String userName = userDoc.exists() ? userDoc.getString("name") : "Unknown";

                    // Add to waitlist array
                    db.collection("events").document(eventId)
                            .update("waitlistUsers", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener(aVoid -> {

                                // Only save location if we actually have one (it might be null if not required)
                                if (location != null) {
                                    Map<String, Object> locationData = new HashMap<>();
                                    locationData.put("location", location);
                                    locationData.put("userName", userName);
                                    locationData.put("timestamp", System.currentTimeMillis());

                                    db.collection("events").document(eventId)
                                            .collection("entrantLocations").document(userId)
                                            .set(locationData)
                                            .addOnSuccessListener(v -> Log.d(TAG, "Location saved"));
                                }

                                Toast.makeText(this, "Successfully joined waitlist!", Toast.LENGTH_SHORT).show();
                                joinWaitlistButton.setEnabled(false);
                                joinWaitlistButton.setText("Joined Waitlist");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error joining waitlist", e);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user data", e));
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
}