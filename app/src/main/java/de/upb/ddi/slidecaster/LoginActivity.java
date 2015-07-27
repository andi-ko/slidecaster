package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("=========================== LoginActivity: onCreate(): begin ===========================");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            serverName=extras.getString(getString(R.string.stringExtraServerName));
            serverAddress=extras.getString(getString(R.string.stringExtraServerAddress));
        }
        if (serverName.equals(getString(R.string.defaultServerName))) {
            EditText loginNameEditText = (EditText) findViewById(R.id.loginNameEditText);
            System.out.println(loginNameEditText.getText());
            loginNameEditText.setText(getString(R.string.defaultServerGuestUser), TextView.BufferType.EDITABLE);
            EditText loginPasswordEditText = (EditText) findViewById(R.id.loginPasswordEditText);
            System.out.println(loginPasswordEditText.getText());
            loginPasswordEditText.setText(getString(R.string.defaultServerGuestPassword), TextView.BufferType.EDITABLE);
        }
    }

    private class CollectionFetchTask extends DatabaseTask {

        @Override
        protected String[] doInBackground(String... params) {
            if (connect(params[0])) {
                return getCollections();
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
                AlertDialog.Builder adb=new AlertDialog.Builder(LoginActivity.this);

                adb.setTitle("Error");
                adb.setMessage("Connection to database cannot be established");

                adb.show();
            }
            else {
                Intent intent = new Intent(LoginActivity.this, CollectionsActivity.class);

                ArrayList<String> publicCollections = new ArrayList<>(Arrays.asList(result));

                intent.putStringArrayListExtra(getString(R.string.stringExtraCollectionNameList), publicCollections);
                intent.putExtra(getString(R.string.stringExtraServerName), serverName);
                startActivity(intent);
            }
        }

        protected String[] getCollections() {
            try {
                MongoIterable<String> allCollections = getMongoDB().listCollectionNames();

                ArrayList<String> publicCollections = new ArrayList<>();

                for (String collectionName : allCollections) {
                    System.out.println("Collection name: " + collectionName);
                    if (!collectionName.equals("system.indexes") && !collectionName.contains("/")) {
                        publicCollections.add(collectionName);
                    }
                }
                String[] result = new String[publicCollections.size()];

                return publicCollections.toArray(result);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return null;
        }
    }

    public void dbLogin(View view) {
        dialog = new ProgressDialog(LoginActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Connecting, please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

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
