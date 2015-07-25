package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.util.LruCache;
import android.widget.Toast;

import java.sql.Time;
import java.util.ArrayList;

import de.upb.ddi.slidecaster.util.CustomList;


public class EditorActivity extends Activity {

    private String serverName;
    private String collectionName;
    private String projectName;
    private CustomList adapter;

    private ArrayList<String> uriList;
    private ArrayList<Time> displayDurationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_editor);

        serverName = getIntent().getStringExtra(getString(R.string.stringExtraServerName));
        collectionName = getIntent().getStringExtra(getString(R.string.stringExtraCollectionName));
        projectName = getIntent().getStringExtra(getString(R.string.stringExtraProjectName));

        //EditText projectNameEditText = (EditText) findViewById(R.id.projectNameEditText);
        // projectNameEditText.setText(projectName, TextView.BufferType.EDITABLE);

        adapter = new CustomList(EditorActivity.this, uriList, displayDurationList);
        ListView list = (ListView)findViewById(R.id.imageListView);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> Toast.makeText(EditorActivity.this, "You Clicked at item: " + position, Toast.LENGTH_SHORT).show());
    }

    private class LruBitmapCache extends LruCache<String, Bitmap> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruBitmapCache(int maxSize) {
            super(maxSize);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        safeProject();
        // finish The activity
        super.finish();
    }

    private void safeProject() {
        //  TODO: safe to xml
    }
}
