package com.example.eventlottery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * Scans event QR codes and navigates to EventDetailActivity.
 */
public class QrScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private DecoratedBarcodeView barcodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        barcodeView = findViewById(R.id.barcode_scanner);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            barcodeView.resume();
        }

        barcodeView.decodeContinuous(callback); // if there are scan result, callback
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null) return;

            String scannedData = result.getText();
            if (scannedData.startsWith("eventlottery://event/")) {
                String eventId = scannedData.substring("eventlottery://event/".length());
                Toast.makeText(QrScannerActivity.this, "Scanned: " + eventId, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(QrScannerActivity.this, EventDetailActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
                barcodeView.pause();
                finish();
            }
        }
    };
    //callback method
    // decrease camera resource
    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            // if user granted permission => scanner resumes
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                barcodeView.resume();
            // denied permission
            else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
