package com.example.gapfix;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class PdfHelper {
    private static final String TAG = "PdfHelper";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void openPdf(Context context, String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isEmpty()) return;
        Toast.makeText(context, "Opening PDF...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                URL url = new URL(pdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    File cacheDir = new File(context.getCacheDir(), "pdf_cache");
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    String fileName = "temp_" + System.currentTimeMillis() + ".pdf";
                    File cacheFile = new File(cacheDir, fileName);
                    try (InputStream input = connection.getInputStream();
                         FileOutputStream output = new FileOutputStream(cacheFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }
                    Uri contentUri = FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".fileprovider",
                            cacheFile
                    );
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(contentUri, "application/pdf");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                            Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    Toast.makeText(context, "Failed to load PDF", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
