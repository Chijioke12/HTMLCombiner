package com.htmlcombiner;

import android.content.ContentResolver;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class LocalServer extends NanoHTTPD {

    private final List<FileItem>  fileItems;
    private final ContentResolver resolver;
    private final Map<String, FileItem> nameMap = new HashMap<>();
    private boolean running = false;

    public LocalServer(int port, List<FileItem> fileItems, ContentResolver resolver)
            throws IOException {
        super(port);
        this.fileItems = fileItems;
        this.resolver  = resolver;
        rebuildNameMap();
    }

    @Override
    public void start() throws IOException {
        super.start();
        running = true;
    }

    @Override
    public void stop() {
        super.stop();
        running = false;
    }

    public boolean isAlive() {
        return running;
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
        if (uri.startsWith("/")) uri = uri.substring(1);

        if (uri.isEmpty()) {
            return newFixedLengthResponse(Response.Status.OK,
                    "text/html; charset=utf-8", buildIndexPage());
        }

        FileItem fi = nameMap.get(uri.toLowerCase());
        if (fi == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "text/plain", "404 — Not found: " + uri);
        }

        try {
            InputStream is = resolver.openInputStream(fi.uri);
            if (is == null) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        "text/plain", "Cannot open: " + fi.name);
            }
            return newChunkedResponse(Response.Status.OK, guessMime(fi.name), is);
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
          .append("<title>HTML Combiner</title>")
          .append("<style>body{font-family:sans-serif;background:#0d1117;color:#c9d1d9;padding:20px}")
          .append("a{color:#58a6ff}.badge{display:inline-block;padding:2px 8px;border-radius:4px;")
          .append("font-size:.75rem;font-weight:bold;margin-right:8px;color:#fff}")
          .append(".HTML{background:#E91E63}.CSS{background:#2196F3}.JS{background:#FF9800}")
          .append("li{margin:8px 0;list-style:none}</style></head><body>")
          .append("<h2>&#128421; Local Server — localhost:8080</h2><ul>");
        for (FileItem f : fileItems) {
            if (!f.enabled) continue;
            sb.append("<li><span class='badge ").append(f.type).append("'>")
              .append(f.type).append("</span>")
              .append("<a href='/").append(f.name).append("'>").append(f.name).append("</a></li>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    private String guessMime(String name) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(name);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mime != null) return mime;
        if (name.endsWith(".js"))   return "application/javascript";
        if (name.endsWith(".css"))  return "text/css";
        if (name.endsWith(".html")) return "text/html";
        return "text/plain";
    }
}
