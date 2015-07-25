package de.upb.ddi.slidecaster.util;


import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URI;
import java.sql.Time;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.upb.ddi.slidecaster.R;

public class CustomList extends ArrayAdapter<String>{

    private final Activity context;
    private final ArrayList<String> uriList;
    private final ArrayList<Time> displayDurationList;

    public CustomList(Activity context, ArrayList<String> uriList, ArrayList<Time> displayDurationList) {
        super(context, R.layout.list_single, uriList);
        this.context = context;
        this.uriList = uriList;
        this.displayDurationList = displayDurationList;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.list_single, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.img);

        txtTitle.setText("show: " + displayDurationList.get(position).toString());
        imageView.setImageURI(Uri.parse(uriList.get(position)));
        return rowView;
    }
}