package com.example.eventlottery;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.UUID;

/**
 * Organizer creates a new event.
 * When saved, a unique QR code is generated and stored in Firestore.
 */
public class QrGenerator extends AppCompatActivity {

    private EditText eventTitleInput, eventDescriptionInput;
    private ImageView qrPreview;
    private Button btnSave;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();

        eventTitleInput = findViewById(R.id.eventTitleInput);
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput);
        qrPreview = findViewById(R.id.qrPreview);
        btnSave = findViewById(R.id.btnSaveEvent);

        btnSave.setOnClickListener(v -> createEvent());
    }

    private void createEvent() {
        String title = eventTitleInput.getText().toString().trim();
        String desc = eventDescriptionInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate unique eventId
        String eventId = UUID.randomUUID().toString();
        String payload = "eventlottery://event/" + eventId;

        // Generate QR bitmap
        Bitmap qrBitmap = createQrBitmap(payload, 600);
        qrPreview.setImageBitmap(qrBitmap);

        // Save event data
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", title);
        event.put("description", desc);
        event.put("qrPayload", payload);

        db.collection("events").document(eventId)
                .set(event)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

