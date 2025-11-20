package com.example.eventlottery.managers;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CSVDownloadManager {

    public static void exportToCSV(Context context, String fileName, List<String> userNames) {
        try {
            File csvFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    fileName + "_" + getTimestamp() + ".csv");

            FileWriter writer = new FileWriter(csvFile);

            // Write CSV header
            writer.append("Name\n");

            // Write data rows
            for (String name : userNames) {
                writer.append(name.replace(",", " ")); // Remove commas to prevent CSV issues
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            // Open share dialog to let user save/share the file
            Uri fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", csvFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Save CSV File"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error creating CSV: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }
}



