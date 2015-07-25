package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;


public class ProjectsActivity extends Activity {

    private ArrayList<String> projectNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("=========================== ProjectsActivity: onCreate(): begin ===========================");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        final ListView projectsListView = (ListView)findViewById(R.id.projectsListView);

        projectNames = getIntent().getStringArrayListExtra("PROJECT_NAMES");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, projectNames);

        projectsListView.setAdapter(adapter);

        projectsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(ProjectsActivity.this, EditorActivity.class);
                intent.putExtra("NEW_PROJECT", false);
                startActivity(intent);
            }
        });
    }

    public void createProject(View view) {
        Intent intent = new Intent(this, AddServerActivity.class);
        intent.putExtra("NEW_PROJECT", true);
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // check if the request code is same as what is passed, here it is 2
        if(requestCode==2 && data != null)
        {
            // fetch the message String
            String newProjectName=data.getStringExtra("PROJECT_NAME");
            // Set the message string in textView
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_projects, menu);
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
