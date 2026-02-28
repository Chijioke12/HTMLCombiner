package com.htmlcombiner;

import android.content.ContentResolver;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nanohttpd.protocols.http.NanoHTTPD;

/**
 * Embedded local HTTP server (port 8080) that serves all added files
 * by name, so that references between HTML / CSS / JS work naturally.
 *
 * e.g.  http://localhost:8080/index.html
 *       http://localhost:8080/styles.css
 *       http://localhost:8080/app.js
 */
public class LocalServer extends NanoHTTPD {

    private final List<FileItem>    fileItems;
    private final ContentResolver   resolver;
    private final Map<String, FileItem> nameMap = new HashMap<>();

    public LocalServer(int port, List<FileItem> fileItems, ContentResolver resolver)
            throws IOException {
        super(port);
        this.fileItems = fileItems;
        this.resolver  = resolver;
        rebuildNameMap();
    }

    private void rebuildNameMap() {
        nameMap.clear();
        for (FileItem fi : fileItems) {
            if (fi.enabled) nameMap.put(fi.name.toLowerCase(), fi);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        // Strip leading slash
        if (uri.startsWith("/")) uri = uri.substring(1);

        // Root → directory listing
        if (uri.isEmpty() || uri.equals("index")) {
            return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8",
                    buildIndexPage());
        }

        // Find file by name (case-insensitive)
        FileItem fi = nameMap.get(uri.toLowerCase());
        if (fi == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "text/plain", "404 — File not found: " + uri);
        }

        try {
            InputStream is = resolver.openInputStream(fi.uri);
            if (is == null) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        "text/plain", "Cannot open file: " + fi.name);
            }
            String mime = guessMime(fi.name);
            return newChunkedResponse(Response.Status.OK, mime, is);
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "text/plain", "Error: " + e.getMessage());
        }
    }

    private String buildIndexPage() {
        rebuildNameMap();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head>")
          .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
          .append("<title>HTML Combiner – Local Server</title>")
          .append("<style>")
          .append("body{font-family:system-ui,sans-serif;background:#0d1117;color:#c9d1d9;padding:20px;margin:0}")
          .append("h1{color:#58a6ff;font-size:1.4rem}a{color:#58a6ff;text-decoration:none}")
          .append("a:hover{text-decoration:underline}")
          .append(".badge{display:inline-block;padding:2px 8px;border-radius:4px;font-size:.75rem;")
          .append("font-weight:bold;margin-right:8px;color:#fff}")
          .append(".HTML{background:#E91E63}.CSS{background:#2196F3}.JS{background:#FF9800}.OTHER{background:#9E9E9E}")
          .append("li{margin:8px 0;list-style:none}.ul{padding:0}")
          .append("</style></head><body>")
          .append("<h1>🖥 Local Server — Served Files</h1>")
          .append("<p style='color:#8b949e'>Click a file to open it. Drag-reorder in the app to control load order.</p>")
          .append("<ul class='ul'>");

        for (FileItem fi : fileItems) {
            if (!fi.enabled) continue;
            sb.append("<li>")
              .append("<span class='badge ").append(fi.type).append("'>").append(fi.type).append("</span>")
              .append("<a href='/").append(fi.name).append("'>").append(fi.name).append("</a>")
              .append("</li>");
        }

        sb.append("</ul>")
          .append("<hr style='border-color:#30363d;margin-top:24px'>")
          .append("<p style='color:#6e7681;font-size:.85rem'>HTML Combiner v1.0 • localhost:8080</p>")
          .append("</body></html>");
        return sb.toString();
    }

    private String guessMime(String name) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(name);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mime != null) return mime;
        if (name.endsWith(".js"))   return "application/javascript";
        if (name.endsWith(".css"))  return "text/css";
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html";
        return "text/plain";
    }
}
