package com.example.surjitsingh.swipethisdemo;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends ArrayAdapter<String> {

    private Context context;
    private int resource;
    private List<String> persons;

    MyAdapter(@NonNull Context context, int resource, ArrayList<String> persons) {
        super(context, resource, persons);
        this.resource = resource;
        this.context = context;
        this.persons = persons;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        RecordHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(resource, parent, false);

            holder = new RecordHolder();
            holder.txtTitle = row.findViewById(R.id.personName);
            holder.infoButton = row.findViewById(R.id.delete_button);
            row.setTag(holder);
        } else {
            holder = (RecordHolder) row.getTag();
        }

        holder.txtTitle.setText(persons.get(position));
        holder.infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Delete CLICKED", Toast.LENGTH_SHORT).show();
            }
        });
        return row;

    }

    static class RecordHolder {
        TextView txtTitle;
        ImageButton infoButton;
    }
}
