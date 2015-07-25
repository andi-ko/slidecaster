package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Projections;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;

import de.upb.ddi.slidecaster.net.DatabaseTask;


public class CollectionsActivity extends Activity {

    private ArrayList<String> collectionNames;

    private String serverName;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("=========================== CollectionsActivity: onCreate(): begin ===========================");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collections);

        final ListView collectionsListView = (ListView)findViewById(R.id.collectionsListView);

        serverName = getIntent().getStringExtra(getString(R.string.stringExtraServerName));
        collectionNames = getIntent().getStringArrayListExtra(getString(R.string.stringExtraCollectionNameList));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, collectionNames);

        collectionsListView.setAdapter(adapter);

        collectionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View v, int position, long id){
                dialog = new ProgressDialog(CollectionsActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("Connecting, please wait...");
                dialog.setIndeterminate(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();

                System.out.println("start fetching task ...");

                ProjectsFetchTask task = new ProjectsFetchTask();
                task.execute(collectionNames.get(position));
            }
        });
    }

    private class ProjectsFetchTask extends DatabaseTask {

        private String collectionName;

        @Override
        protected String[] doInBackground(String... params) {
            if (assertConnection()) {
                collectionName = params[0];
                return getProjects(collectionName);
            }
            return null;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(String[] result) {

            if (dialog != null) {
                dialog.dismiss();
            }

            if (result == null) {
                AlertDialog.Builder adb=new AlertDialog.Builder(CollectionsActivity.this);

                adb.setTitle("Error");
                adb.setMessage("Projects could not be fetched.");

                adb.show();
            }
            else {
                Intent intent = new Intent(CollectionsActivity.this, ProjectsActivity.class);

                ArrayList<String> projects = new ArrayList<>(Arrays.asList(result));

                intent.putStringArrayListExtra(getString(R.string.stringExtraProjectNameList), projects);
                intent.putExtra(getString(R.string.stringExtraServerName), serverName);
                intent.putExtra(getString(R.string.stringExtraCollectionName), collectionName);

                startActivity(intent);
            }
        }
        protected String[] getProjects(String collectionName) {
            try {
                System.out.println("fetching projects");

                ArrayList<String> projects = new ArrayList<>();

                MongoCollection<Document> collection = getMongoDB().getCollection(collectionName);
                MongoCursor<Document> cursor = collection.find().projection((Projections.fields(Projections.include("name"), Projections.excludeId()))).iterator();

                System.out.println("projects fetched");

                while (cursor.hasNext()) {
                    projects.add(cursor.next().toString());
                    System.out.println(projects.get(projects.size()-1));
                }
                cursor.close();

                String[] result = new String[projects.size()];

                return projects.toArray(result);

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_collections, menu);
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
    public void onBackPressed()
    {
        super.onBackPressed();
        startActivity(new Intent(CollectionsActivity.this, MainActivity.class));
        finish();
    }
}
