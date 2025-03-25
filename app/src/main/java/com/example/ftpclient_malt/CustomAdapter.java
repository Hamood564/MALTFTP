package com.example.ftpclient_malt;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.enterprisedt.net.ftp.FTPException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomAdapter extends ArrayAdapter<FileItem> {
    private List<FileItem> items;
    private Context context;
    private Set<Integer> selectedPositions = new HashSet<>();

    public CustomAdapter(Context context, List<FileItem> items) {
        super(context, R.layout.list_item, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.list_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.text);
        ImageView imageView = convertView.findViewById(R.id.icon);

        FileItem item = items.get(position);

        if (item.isDirectory()) {
            imageView.setImageResource(R.drawable.ic_baseline_folder_24);
        } else {
            imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
        }
        textView.setText(item.getName());

        // Highlight selected items
        if (selectedPositions.contains(position)) {
            convertView.setBackgroundColor(Color.LTGRAY); // Highlight color
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT); // Default color
        }

        return convertView;
    }
    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyDataSetChanged();
    }

    public List<FileItem> getSelectedItems() {
        List<FileItem> selectedItems = new ArrayList<>();
        for (Integer position : selectedPositions) {
            selectedItems.add(items.get(position));
        }
        return selectedItems;
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }
}

