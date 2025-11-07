package com.example.eventlottery;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Shows event details and its QR code.
 * fetches data from firebase
 * retrieves the event details that were entered by the organizer
 */
public class EventDetailActivity extends AppCompatActivity {

    private TextView eventTitle, eventDescription, eventCriteria;
    private TextView statusBadge, priceText, locationText, dateText;
    private TextView waitlistInfo, spotsText;
    private ImageView qrCodeImage, backButton;
    private Button joinWaitlistButton, shareButton;

    private FirebaseFirestore db;

    /**
     * called when activity is made, intializes the UI, firebase instance
     * REtieves the event ID, and triggers loading of the event
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_event_detail);

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

        db = FirebaseFirestore.getInstance();

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

    /**
     * fetche event data from the events collection in firebase
     * @param eventId
     */
    private void loadEvent(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Read all fields from Firestore
                        String eventName = doc.getString("eventName");
                        String description = doc.getString("description");
                        String eligibility = doc.getString("eligibility");
                        String location = doc.getString("location");
                        String startDate = doc.getString("startDate");
                        String endDate = doc.getString("endDate");
                        String priceStr = doc.getString("price");
                        String waitlistLimitStr = doc.getString("waitlistLimit");
                        String entrantMaxStr = doc.getString("entrantMaxCapacity");

                        // Set event title
                        eventTitle.setText(eventName != null ? eventName : "Untitled Event");

                        // Set event description
                        eventDescription.setText(description != null ? description : "No description available");

                        // Set event criteria (eligibility)
                        eventCriteria.setText(eligibility != null ? eligibility : "No specific criteria");

                        // Set location
                        locationText.setText(location != null ? location : "TBD");

                        // Set date range
                        if (startDate != null && endDate != null) {
                            dateText.setText(startDate + " - " + endDate);
                        } else {
                            dateText.setText("Date TBD");
                        }

                        // Set price
                        if (priceStr != null && !priceStr.isEmpty()) {
                            priceText.setText("$" + priceStr);
                        } else {
                            priceText.setText("Free");
                        }

                        // Set status badge (simple for now)
                        statusBadge.setText("Open");

                        // Set waitlist info
                        int waitlistLimit = waitlistLimitStr != null ? Integer.parseInt(waitlistLimitStr) : 0;
                        waitlistInfo.setText("Waitlist limit: " + waitlistLimit);

                        // Set spots available
                        int entrantMax = entrantMaxStr != null ? Integer.parseInt(entrantMaxStr) : 0;
                        spotsText.setText(entrantMax + " spots");

                        // Join waitlist button (simple toast for MVP)
                        joinWaitlistButton.setOnClickListener(v -> {
                            Toast.makeText(this, "Join waitlist functionality coming soon", Toast.LENGTH_SHORT).show();
                        });

                        // Generate and display QR code
                        String qrPayload = "eventlottery://event/" + eventId;
                        Bitmap qr = createQrBitmap(qrPayload, 400);
                        qrCodeImage.setImageBitmap(qr);

                        // Share button (simple toast for MVP)
                        shareButton.setOnClickListener(v -> {
                            Toast.makeText(this, "Share functionality coming soon", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * generates a QR code uses the string content for it
     * @param content the string data that is encoded in the QR code
     * @param sizePx
     * @return
     */
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
