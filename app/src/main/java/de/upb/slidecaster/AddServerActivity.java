package de.upb.slidecaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class AddServerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_server);
    }

    public void connectToServer(View view) {
        EditText serverAddressEditText = (EditText) findViewById(R.id.serverAddressEditText);
        String serverAddress = serverAddressEditText.getText().toString();
        EditText serverNameEditText = (EditText) findViewById(R.id.serverAddressEditText);
        String serverName = serverNameEditText.getText().toString();
        Intent intent = new Intent(this, AddServerActivity.class);
        intent.putExtra("SERVER_ADDRESS", serverAddress);
        intent.putExtra("SERVER_NAME", serverName);
        // Set The Result in Intent
        setResult(2, intent);
        // finish The activity
        finish();
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
