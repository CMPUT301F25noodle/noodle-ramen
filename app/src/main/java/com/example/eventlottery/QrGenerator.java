package com.example.eventlottery;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// generates a QR code and shows it on the screen
public class QrGenerator extends AppCompatActivity {

    private ImageView imgQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        EditText etTitle = findViewById(R.id.etEventTitle);
        Button btnGen = findViewById(R.id.btnGenerate);
        imgQr = findViewById(R.id.imgQr);
        // When user clicks button -> generate a new QR
        btnGen.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            // create unique eventID
            String eventId = UUID.randomUUID().toString();
            // String encoded inside QR code
            String payload = "eventlottery://event/" + eventId;
            // Generate bitmap QR
            Bitmap bitmap = createQrBitmap(payload, 800);
            // Show QR on ImageView
            imgQr.setImageBitmap(bitmap);

            Toast.makeText(this, "QR created! eventId = "+ eventId, Toast.LENGTH_SHORT).show();
        });
    }
    // Helper function that creates QR bitmap using ZXing
    private Bitmap createQrBitmap(String content, int sizePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            // black/white pixel data
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);

            for (int y = 0; y < sizePx; y++) {
                for (int x = 0; x < sizePx; x++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bmp;
        // if fail to create QR, return null
        } catch (WriterException e) {
            Log.e("QR", "QR generation failed", e);
            return null;
        }
    }
}
