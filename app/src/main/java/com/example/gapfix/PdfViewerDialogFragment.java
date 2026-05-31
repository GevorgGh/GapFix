package com.example.gapfix;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.pdf.viewer.fragment.PdfViewerFragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class PdfViewerDialogFragment extends DialogFragment {
    private static final String ARG_PDF_URI = "arg_pdf_uri";
    private static final String ARG_PDF_TITLE = "arg_pdf_title";
    private Uri pdfUri;
    private String pdfTitle;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static PdfViewerDialogFragment newInstance(Uri pdfUri, String title) {
        PdfViewerDialogFragment fragment = new PdfViewerDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PDF_URI, pdfUri);
        args.putString(ARG_PDF_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_pdf_viewer, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            pdfUri = getArguments().getParcelable(ARG_PDF_URI);
            pdfTitle = getArguments().getString(ARG_PDF_TITLE);
        }
        ImageButton btnClosePdf = view.findViewById(R.id.btnClosePdf);
        TextView tvPdfTitle = view.findViewById(R.id.tvPdfTitle);
        ImageButton btnOpenExternal = view.findViewById(R.id.btnOpenExternal);
        if (pdfTitle != null) {
            tvPdfTitle.setText(pdfTitle);
        }
        btnClosePdf.setOnClickListener(v -> dismiss());
        btnOpenExternal.setOnClickListener(v -> {
            if (pdfUri != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "No application found to view PDFs", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (pdfUri != null) {
            if (pdfUri.getScheme() != null && (pdfUri.getScheme().equals("http") || pdfUri.getScheme().equals("https"))) {
                downloadAndShowPdf(pdfUri.toString());
            } else {
                showPdf(pdfUri);
            }
        }
    }
    private void downloadAndShowPdf(String urlString) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                File file = new File(requireContext().getCacheDir(), "temp_preview.pdf");
                FileOutputStream fos = new FileOutputStream(file);
                InputStream is = connection.getInputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();
                Uri localUri = FileProvider.getUriForFile(requireContext(), 
                        requireContext().getPackageName() + ".fileprovider", file);
                new Handler(Looper.getMainLooper()).post(() -> showPdf(localUri));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error downloading PDF", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void showPdf(Uri uri) {
        if (!isAdded()) return;
        PdfViewerFragment pdfViewerFragment = new PdfViewerFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view, (Fragment) pdfViewerFragment)
                .commitNow();
        pdfViewerFragment.setDocumentUri(uri);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
