package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import java.util.ArrayList;
import java.util.Arrays;

import de.upb.ddi.slidecaster.net.DatabaseTask;


public class LoginActivity extends Activity {

    String serverName;
    String serverAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("=========================== LoginActivity: onCreate(): begin ===========================");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            serverName=extras.getString("SERVER_NAME");
            serverAddress=extras.getString("SERVER_ADDRESS");
        }
    }

    private class CollectionFetchTask extends DatabaseTask {

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(String[] result) {

            if (result == null) {
                AlertDialog.Builder adb=new AlertDialog.Builder(LoginActivity.this);

                adb.setTitle("Error");
                adb.setMessage("Connection to database cannot be established");

                adb.show();
            }
            else {
                Intent intent = new Intent(LoginActivity.this, CollectionsActivity.class);

                ArrayList<String> publicCollections = new ArrayList<>(Arrays.asList(result));

                intent.putStringArrayListExtra("COLLECTION_NAMES", publicCollections);
                startActivity(intent);
            }
        }

        @Override
        protected String[] process() {
            MongoIterable<String> allCollections = getMongoDB().listCollectionNames();

            ArrayList<String> publicCollections = new ArrayList<>();

            for (String collection : allCollections) {
                System.out.println("Collection name: " + collection);
                if (!collection.equals("system.indexes")) {
                    publicCollections.add(collection);
                }
            }
            String[] result = new String[publicCollections.size()];

            return publicCollections.toArray(result);
        }
    }

    public void dbLogin(View view) {
        EditText loginNameEditText = (EditText) findViewById(R.id.loginNameEditText);
        String username = loginNameEditText.getText().toString();
        EditText loginPasswordEditText = (EditText) findViewById(R.id.loginPasswordEditText);
        String password = loginPasswordEditText.getText().toString();

        CollectionFetchTask task = new CollectionFetchTask();
        task.execute("mongodb://"+username+":"+password+"@"+serverAddress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            //noinspection SimplifiableIfStatement
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
