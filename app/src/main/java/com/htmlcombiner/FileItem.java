package com.htmlcombiner;

import android.net.Uri;

public class FileItem {
    public static final String TYPE_HTML  = "HTML";
    public static final String TYPE_CSS   = "CSS";
    public static final String TYPE_JS    = "JS";
    public static final String TYPE_OTHER = "OTHER";

    public String name;
    public Uri    uri;
    public String type;
    public boolean enabled;

    public FileItem(String name, Uri uri, String type) {
        this.name    = name;
        this.uri     = uri;
        this.type    = type;
        this.enabled = true;
    }
}
