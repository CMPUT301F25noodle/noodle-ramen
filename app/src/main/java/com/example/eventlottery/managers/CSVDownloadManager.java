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

/**
 * CSVDownloadManager handles the export of data lists to CSV files.
 * It ensures files are saved to the device's public Downloads folder, adapting to different Android API storage requirements.
 */
public class CSVDownloadManager {
    /**
     * Exports a list of user names to a CSV file in the system's Downloads directory.
     * Automatically chooses the correct storage method based on the Android version (MediaStore for newer, File I/O for older).
     *
     * @param context   The application context.
     * @param fileName  The base name for the file (timestamp will be appended).
     * @param userNames The list of strings to write into the CSV.
     */
    public static void exportToCSV(Context context, String fileName, List<String> userNames) {
        String finalFileName = fileName + "_" + getTimestamp() + ".csv";


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloadsMediaStore(context, finalFileName, userNames);
        } else {

            saveToDownloadsLegacy(context, finalFileName, userNames);
        }
    }
    /**
     * Saves the CSV file using the MediaStore API, required for Android Q (API 29) and above.
     *
     * @param context   The application context.
     * @param fileName  The full file name including extension.
     * @param userNames The list of data to write.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static void saveToDownloadsMediaStore(Context context, String fileName, List<String> userNames) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

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

    /**
     * Saves the CSV file using standard File I/O, used for legacy Android versions (before API 29).
     *
     * @param context   The application context.
     * @param fileName  The full file name including extension.
     * @param userNames The list of data to write.
     */
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