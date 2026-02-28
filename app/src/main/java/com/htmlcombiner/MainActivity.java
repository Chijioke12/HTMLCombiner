package com.htmlcombiner;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILES_REQUEST = 101;
    private static final int SAVE_HTML_REQUEST  = 102;

    private List<FileItem> fileItems = new ArrayList<>();
    private FileAdapter   adapter;
    private LocalServer   server;

    private TextView  tvStatus;
    private WebView   webView;
    private Button    btnStartServer;
    private String    combinedHtml = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView rv = findViewById(R.id.recyclerView);
        tvStatus       = findViewById(R.id.tvStatus);
        webView        = findViewById(R.id.webView);
        btnStartServer = findViewById(R.id.btnStartServer);

        adapter = new FileAdapter(fileItems, this::onRemoveFile, this::onMoveFileType);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Swipe-to-delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView r, RecyclerView.ViewHolder vh,
                                  RecyclerView.ViewHolder t) {
                int from = vh.getAdapterPosition();
                int to   = t.getAdapterPosition();
                fileItems.add(to, fileItems.remove(from));
                adapter.notifyItemMoved(from, to);
                return true;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                fileItems.remove(pos);
                adapter.notifyItemRemoved(pos);
                toast("File removed");
            }
        }).attachToRecyclerView(rv);

        // Setup WebView
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // Buttons
        findViewById(R.id.btnAddFiles).setOnClickListener(v -> pickFiles());
        btnStartServer.setOnClickListener(v -> toggleServer());
        findViewById(R.id.btnCombine).setOnClickListener(v -> combineFiles());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveFile());
        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnPreviewServer).setOnClickListener(v -> previewServer());
    }

    // ──────────────── File Picking ────────────────

    private void pickFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/html", "text/css", "text/javascript",
                "application/javascript", "application/x-javascript"
        });
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_FILES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == PICK_FILES_REQUEST) {
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData clip = data.getClipData();
                for (int i = 0; i < clip.getItemCount(); i++)
                    uris.add(clip.getItemAt(i).getUri());
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            for (Uri uri : uris) {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                addFile(uri);
            }
            adapter.notifyDataSetChanged();
            updateStatus("📂 " + fileItems.size() + " file(s) loaded. Ready to combine.");
        }

        if (requestCode == SAVE_HTML_REQUEST) {
            try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                if (os != null) {
                    os.write(combinedHtml.getBytes("UTF-8"));
                    toast("✅ File saved successfully!");
                }
            } catch (IOException e) {
                toast("Error saving: " + e.getMessage());
            }
        }
    }

    private void addFile(Uri uri) {
        String name = getFileName(uri);
        String type = inferType(name);
        // Avoid duplicates
        for (FileItem fi : fileItems) {
            if (fi.uri.equals(uri)) { toast("Already added: " + name); return; }
        }
        fileItems.add(new FileItem(name, uri, type));
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = c.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result != null && result.contains("/"))
                result = result.substring(result.lastIndexOf('/') + 1);
        }
        return result != null ? result : "unknown";
    }

    private String inferType(String name) {
        if (name == null) return FileItem.TYPE_OTHER;
        String lower = name.toLowerCase();
        if (lower.endsWith(".js"))                          return FileItem.TYPE_JS;
        if (lower.endsWith(".css"))                         return FileItem.TYPE_CSS;
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return FileItem.TYPE_HTML;
        return FileItem.TYPE_OTHER;
    }

    // ──────────────── Local Server ────────────────

    private void toggleServer() {
        if (server != null && server.isRunning()) {
            server.stop();
            server = null;
            btnStartServer.setText("🖥 Start Local Server");
            updateStatus("⚫ Server stopped.");
        } else {
            startServer();
        }
    }

    private void startServer() {
        try {
            if (server != null) { server.stop(); }
            server = new LocalServer(8080, fileItems, getContentResolver());
            server.start();
            btnStartServer.setText("🛑 Stop Server");
            updateStatus("🟢 Server running → http://localhost:8080\n" +
                    "Files served: " + fileItems.size() + " | Tap 'Preview' to open.");
            toast("Server started on port 8080!");
        } catch (IOException e) {
            updateStatus("❌ Server failed: " + e.getMessage());
        }
    }

    private void previewServer() {
        if (server == null || !server.isRunning()) {
            toast("Start the server first!");
            return;
        }
        // Find first HTML file, else root
        String url = "http://localhost:8080/";
        for (FileItem fi : fileItems) {
            if (FileItem.TYPE_HTML.equals(fi.type)) {
                url = "http://localhost:8080/" + fi.name;
                break;
            }
        }
        webView.loadUrl(url);
        updateStatus("🌐 Previewing: " + url);
    }

    // ──────────────── Combine Files ────────────────

    private void combineFiles() {
        if (fileItems.isEmpty()) {
            toast("Add files first!");
            return;
        }

        // Show options dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_combine_options, null);
        builder.setView(dialogView);
        builder.setTitle("⚙️ Combine Options");

        RadioGroup rgMode   = dialogView.findViewById(R.id.rgMode);
        Switch     swMinify = dialogView.findViewById(R.id.swMinify);
        EditText   etTitle  = dialogView.findViewById(R.id.etTitle);

        builder.setPositiveButton("Combine", (d, w) -> {
            boolean inlineMode = rgMode.getCheckedRadioButtonId() == R.id.rbInline;
            boolean minify     = swMinify.isChecked();
            String  title      = etTitle.getText().toString().trim();
            if (title.isEmpty()) title = "Combined App";

            FileCombiner combiner = new FileCombiner(getContentResolver());
            combinedHtml = combiner.combine(fileItems, title, inlineMode, minify);
            webView.loadData(combinedHtml, "text/html; charset=utf-8", "base64");
            updateStatus("✅ Combined " + fileItems.size() + " files → " +
                    (combinedHtml.length() / 1024) + " KB  |  " + (minify ? "Minified" : "Pretty"));
            toast("Combined successfully!");
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ──────────────── Save / Share ────────────────

    private void saveFile() {
        if (combinedHtml.isEmpty()) {
            toast("Combine files first!");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_TITLE, "combined.html");
        startActivityForResult(intent, SAVE_HTML_REQUEST);
    }

    // ──────────────── Helpers ────────────────

    private void onRemoveFile(int pos) {
        fileItems.remove(pos);
        adapter.notifyItemRemoved(pos);
        updateStatus("🗑 File removed. " + fileItems.size() + " file(s) remaining.");
    }

    private void onMoveFileType(int pos) {
        FileItem fi = fileItems.get(pos);
        String[] types = {FileItem.TYPE_HTML, FileItem.TYPE_CSS, FileItem.TYPE_JS, FileItem.TYPE_OTHER};
        new AlertDialog.Builder(this)
                .setTitle("Set file type for " + fi.name)
                .setItems(types, (d, which) -> {
                    fi.type = types[which];
                    adapter.notifyItemChanged(pos);
                })
                .show();
    }

    private void clearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear all files?")
                .setMessage("This will remove all added files and stop the server.")
                .setPositiveButton("Clear", (d, w) -> {
                    if (server != null) { server.stop(); server = null; }
                    btnStartServer.setText("🖥 Start Local Server");
                    fileItems.clear();
                    adapter.notifyDataSetChanged();
                    combinedHtml = "";
                    webView.loadData("", "text/html", "UTF-8");
                    updateStatus("🧹 Cleared. Add files to begin.");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStatus(String msg) {
        tvStatus.setText(msg);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) { server.stop(); }
    }
}
