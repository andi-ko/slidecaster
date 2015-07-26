package de.upb.ddi.slidecaster.util;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private final ArrayList<Integer> displayDurationList;

    public CustomList(Activity context, ArrayList<String> uriList, ArrayList<Integer> displayDurationList) {
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

        int seconds = displayDurationList.get(position);

        txtTitle.setText("show for: " + seconds + "s");
        setPic(imageView, uriList.get(position));
        return rowView;
    }

    private void setPic(ImageView mImageView, String mCurrentPhotoPath) {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        // bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }
}