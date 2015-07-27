package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;

import org.bson.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.upb.ddi.slidecaster.net.DatabaseTask;


public class ProjectsActivity extends Activity {

    private ArrayList<String> projectNames;
    ArrayAdapter<String> adapter;
    private ProgressDialog dialog;

    private String serverName;
    private String collectionName;

    private File projectListFile;

    static final int REQUEST_PROJECT_RELEASE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("=========================== ProjectsActivity: onCreate(): begin ===========================");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        final ListView projectsListView = (ListView)findViewById(R.id.projectsListView);

        ArrayList<String> remoteProjectNames = getIntent().getStringArrayListExtra(getString(R.string.stringExtraProjectNameList));
        serverName = getIntent().getStringExtra(getString(R.string.stringExtraServerName));
        collectionName = getIntent().getStringExtra(getString(R.string.stringExtraCollectionName));

        projectListFile = new File(this.getFilesDir().getPath()+"/"+serverName+"/"+collectionName+".xml");

        if (projectListFile.getParentFile().mkdirs()) {
            System.out.println("path created");
        }

        try {
            if (projectListFile.createNewFile()) {
                System.out.println("new project list file created");
                firstRun(projectListFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        readProjectList(projectListFile);

        for ( int i = 0; i < remoteProjectNames.size(); i++) {
            addProjectToList(remoteProjectNames.get(i)+"(remote)");
        }

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, projectNames);

        projectsListView.setAdapter(adapter);

        projectsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                Intent intent;

                if (!projectNames.get(position).endsWith("(remote)")) {
                    intent = new Intent(ProjectsActivity.this, EditorActivity.class);
                    intent.putExtra(getString(R.string.stringExtraServerName), serverName);
                    intent.putExtra(getString(R.string.stringExtraCollectionName), collectionName);
                    intent.putExtra(getString(R.string.stringExtraProjectName), projectNames.get(position));
                    startActivity(intent);
                }
                else {
                    // TODO: launch player
                }
            }
        });

        projectsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                AlertDialog.Builder adb = new AlertDialog.Builder(ProjectsActivity.this);

                if (projectNames.get(position).endsWith("(remote)")) {
                    adb.setTitle("Denied");
                    adb.setMessage("You cannot edit remote projects.");
                } else {
                    adb.setTitle("Delete?");
                    adb.setMessage("Are you sure you want to delete " + projectNames.get(position));
                    final int positionToRemove = position;

                    adb.setNegativeButton("Cancel", null);
                    adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (removeProjectFromList(positionToRemove)) {
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
                adb.show();
                return true;
            }
        });
    }

    public void createProject(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        builder.setMessage("Enter the title for your new project");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String projectName = input.getText().toString();
                if (projectName.length() > 0) {
                    addProjectToList(projectName);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // check if the request code is same as what is passed, here it is 2
        if(requestCode==REQUEST_PROJECT_RELEASE && data != null)
        {
            // fetch the message String
            String projectName=data.getStringExtra(getString(R.string.stringExtraProjectName));

            if (removeProjectFromList(projectNames.indexOf(projectName))) {
                if (addProjectToList(projectName+"(remote)")) {
                    adapter.notifyDataSetChanged();
                    AlertDialog.Builder adb = new AlertDialog.Builder(ProjectsActivity.this);
                    adb.setTitle("Success");
                    adb.setMessage("Your project is now listed as a remote project");
                    adb.show();
                }
            }
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

    public void firstRun(File projectListFile) {

        try {
            FileOutputStream fos = new FileOutputStream(projectListFile);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "root");
            serializer.endTag(null, "root");
            serializer.endDocument();

            serializer.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("new xml created");
    }

    public void readProjectList (File projectListFile) {

        projectNames = new ArrayList<>();
        String projectName;

        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            org.w3c.dom.Document dom = db.parse(projectListFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            dom.getDocumentElement().normalize();

            Element doc = dom.getDocumentElement();

            NodeList projectList = doc.getElementsByTagName("project");

            for (int i = 0; i < projectList.getLength(); i++) {

                Node serverNode = projectList.item(i);

                if (serverNode.getNodeType() == Node.ELEMENT_NODE) {

                    projectName = ((Element) serverNode).getElementsByTagName("name").item(0).getTextContent();

                    System.out.println(projectName);

                    if (projectName != null) {
                        if (!projectName.isEmpty())
                            projectNames.add(projectName);
                    }
                }
            }

        } catch (ParserConfigurationException pce) {
            System.err.println(pce.getMessage());
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean removeProjectFromList(int position) {

        String projectName = projectNames.get(position);

        System.out.println("remove: "+projectName);

        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            org.w3c.dom.Document dom = db.parse(projectListFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            dom.getDocumentElement().normalize();

            Element doc = dom.getDocumentElement();

            NodeList projectList = doc.getElementsByTagName("project");

            for (int i = 0; i < projectList.getLength(); i++) {

                Node projectNode = projectList.item(i);

                if (projectNode.getNodeType() == Node.ELEMENT_NODE) {

                    System.out.println(((Element) projectNode).getElementsByTagName("name").item(0).getTextContent());

                    if (projectName.equals(((Element) projectNode).getElementsByTagName("name").item(0).getTextContent())) {
                        System.out.println("remove: " + ((Element) projectNode).getElementsByTagName("name").item(0).getTextContent());
                        projectNode.getParentNode().removeChild(projectNode);
                    }
                }
            }

            DOMSource source = new DOMSource(dom);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(projectListFile);
            transformer.transform(source, result);

            projectNames.remove(position);

            return true;

        } catch (ParserConfigurationException | SAXException | TransformerException | IOException pce) {
            System.err.println(pce.getMessage());
        }

        return false;
    }

    public boolean addProjectToList(String projectName) {

        if (projectName != null && !projectName.isEmpty()) {

            System.out.println(projectName);

            if (projectNames.indexOf(projectName) != -1) {
                return false;
            }

            // Make an  instance of the DocumentBuilderFactory
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            try {
                // use the factory to take an instance of the document builder
                DocumentBuilder db = dbf.newDocumentBuilder();
                // parse using the builder to get the DOM mapping of the
                // XML file
                org.w3c.dom.Document dom = db.parse(projectListFile);

                //optional, but recommended
                //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
                dom.getDocumentElement().normalize();

                Element doc = dom.getDocumentElement();

                // server elements
                Element newProject = dom.createElement("project");

                Element name = dom.createElement("name");
                name.appendChild(dom.createTextNode(projectName));
                newProject.appendChild(name);

                doc.appendChild(newProject);

                DOMSource source = new DOMSource(dom);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                StreamResult result = new StreamResult(projectListFile);
                transformer.transform(source, result);

                projectNames.add(projectName);
                return true;

            } catch (SAXException | ParserConfigurationException se) {
                System.out.println(se.getMessage());
            } catch (IOException | TransformerException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        return false;
    }
}
