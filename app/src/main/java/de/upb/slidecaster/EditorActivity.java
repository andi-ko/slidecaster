package de.upb.slidecaster;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;


public class EditorActivity extends Activity {

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_editor);

        intent = getIntent();
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
        if (intent.getBooleanExtra("CREATE_PROJECT",false)) {
            EditText projectNameEditText = (EditText) findViewById(R.id.projectNameEditText);
            String projectName = projectNameEditText.getText().toString();
            Intent intent = new Intent(this, AddServerActivity.class);
            intent.putExtra("PROJECT_NAME", projectName);
            // Set The Result in Intent
            setResult(2, intent);
        }
        safeProject();
        // finish The activity
        super.finish();
    }

    private void safeProject() {
        //  TODO: safe to xml
    }
}
