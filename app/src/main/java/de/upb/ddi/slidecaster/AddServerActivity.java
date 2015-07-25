package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;


public class AddServerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_server);
    }

    public void addServer(View view) {
        EditText serverNameEditText = (EditText) findViewById(R.id.serverNameEditText);
        String serverName = serverNameEditText.getText().toString();
        EditText serverAddressEditText = (EditText) findViewById(R.id.serverAddressEditText);
        String serverAddress = serverAddressEditText.getText().toString();

        if (serverName.isEmpty()){
            AlertDialog.Builder adb=new AlertDialog.Builder(AddServerActivity.this);

            adb.setTitle("Name missing");
            adb.setMessage("Please enter a server name");

            adb.show();
        }
        else if (serverAddress.isEmpty()){
            AlertDialog.Builder adb=new AlertDialog.Builder(AddServerActivity.this);

            adb.setTitle("Address missing");
            adb.setMessage("Please enter a server address");

            adb.show();
        }
        else {
            Intent intent = new Intent(this, AddServerActivity.class);
            intent.putExtra(getString(R.string.stringExtraServerAddress), serverAddress);
            intent.putExtra(getString(R.string.stringExtraServerName), serverName);

            // Set The Result in Intent
            setResult(2, intent);
            // finish The activity
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_server, menu);
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
}
