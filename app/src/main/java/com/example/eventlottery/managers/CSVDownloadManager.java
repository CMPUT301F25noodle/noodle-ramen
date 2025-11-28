package com.example.eventlottery.managers;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.annotation.RequiresApi;


public class CSVDownloadManager {

    public static void exportToCSV(Context context, String fileName, List<String> userNames) {
        String finalFileName = fileName + "_" + getTimestamp() + ".csv";

        // Method 1: For Modern Android (API 29+) - The standard for new apps
        // This saves directly to the user's "Downloads" folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloadsMediaStore(context, finalFileName, userNames);
        } else {
            // Method 2: Fallback for older Android versions (Before Android 10)
            saveToDownloadsLegacy(context, finalFileName, userNames);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static void saveToDownloadsMediaStore(Context context, String fileName, List<String> userNames) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            // This inserts a new file record into the system
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Name\n"); // Header
                    for (String name : userNames) {
                        sb.append(name.replace(",", " ")).append("\n");
                    }

                    outputStream.write(sb.toString().getBytes());
                    outputStream.close();

                    Toast.makeText(context, "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Legacy support (Before Android 10) - unlikely to be used on your emulator but good practice
    private static void saveToDownloadsLegacy(Context context, String fileName, List<String> userNames) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File file = new File(downloadsDir, fileName);

            FileWriter writer = new FileWriter(file);
            writer.append("Name\n");
            for (String name : userNames) {
                writer.append(name.replace(",", " ")).append("\n");
            }
            writer.flush();
            writer.close();

            Toast.makeText(context, "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }
}