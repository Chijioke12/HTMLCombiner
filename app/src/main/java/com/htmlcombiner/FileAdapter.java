package com.htmlcombiner;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {

    public interface OnRemove    { void onRemove(int pos); }
    public interface OnTypeClick { void onTypeClick(int pos); }

    private final List<FileItem> items;
    private final OnRemove       onRemove;
    private final OnTypeClick    onTypeClick;

    public FileAdapter(List<FileItem> items, OnRemove onRemove, OnTypeClick onTypeClick) {
        this.items       = items;
        this.onRemove    = onRemove;
        this.onTypeClick = onTypeClick;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        FileItem fi = items.get(position);
        h.tvName.setText(fi.name);
        h.tvType.setText(fi.type);
        h.swEnabled.setChecked(fi.enabled);

        // Color badge by type
        int color;
        switch (fi.type) {
            case FileItem.TYPE_HTML:  color = Color.parseColor("#E91E63"); break;
            case FileItem.TYPE_CSS:   color = Color.parseColor("#2196F3"); break;
            case FileItem.TYPE_JS:    color = Color.parseColor("#FF9800"); break;
            default:                  color = Color.parseColor("#9E9E9E"); break;
        }
        h.tvType.setBackgroundColor(color);

        h.swEnabled.setOnCheckedChangeListener((btn, checked) -> {
            fi.enabled = checked;
            h.tvName.setAlpha(checked ? 1f : 0.4f);
        });

        h.tvType.setOnClickListener(v -> onTypeClick.onTypeClick(h.getAdapterPosition()));
        h.btnRemove.setOnClickListener(v -> onRemove.onRemove(h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView    tvName, tvType;
        Switch      swEnabled;
        ImageButton btnRemove;

        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvFileName);
            tvType    = v.findViewById(R.id.tvFileType);
            swEnabled = v.findViewById(R.id.swEnabled);
            btnRemove = v.findViewById(R.id.btnRemoveFile);
        }
    }
}
