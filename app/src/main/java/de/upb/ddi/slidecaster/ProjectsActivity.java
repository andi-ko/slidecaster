package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.DigitsKeyListener;
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
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
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
import de.upb.ddi.slidecaster.util.XMLHelpers;


public class ProjectsActivity extends Activity {

    private ArrayList<String> projectNames;
    ArrayAdapter<String> adapter;
    private ProgressDialog dialog;

    private String serverName;
    private String collectionName;

    private File projectListFile;

    static final int REQUEST_PROJECT_RELEASE = 2;
    static final int REQUEST_REPLY = 3;

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

                String projectName = projectNames.get(position);

                if (!projectNames.get(position).endsWith("(remote)")) {
                    intent = new Intent(ProjectsActivity.this, EditorActivity.class);
                    intent.putExtra(getString(R.string.stringExtraServerName), serverName);
                    intent.putExtra(getString(R.string.stringExtraCollectionName), collectionName);
                    intent.putExtra(getString(R.string.stringExtraProjectName), projectName);
                    startActivityForResult(intent, REQUEST_REPLY);
                }
                else {
                    projectName = projectName.substring(0, projectName.lastIndexOf("(remote)"));

                    // check for data

                    File projectFile = new File(getFilesDir().getPath()+"/"+serverName+"/"+collectionName+"/"+projectName+".xml");

                    if (projectFile.getParentFile().mkdirs()) {
                        System.out.println("new project file created");
                    }

                    try {
                        if (projectFile.createNewFile()) {

                            dialog = new ProgressDialog(ProjectsActivity.this);
                            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            dialog.setMessage("Fetching data, please wait...");
                            dialog.setIndeterminate(true);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();

                            // fetch project

                            FetchRemetoProjectTask task = new FetchRemetoProjectTask();
                            task.execute(projectName, projectFile.getAbsolutePath());
                        }
                        else {
                            launchPlayer(projectName);
                        }
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        AlertDialog.Builder adb=new AlertDialog.Builder(ProjectsActivity.this);

                        adb.setTitle("Error");
                        adb.setMessage("Missing FS write access.");

                        adb.show();
                    }
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

    private class FetchRemetoProjectTask extends DatabaseTask {

        private String projectName;
        private String nameSpace;

        private File projectFile;

        private File extDir;

        @Override
        protected String[] doInBackground(String... params) {
            if (assertConnection()) {
                projectName = params[0];
                projectFile = new File(params[1]);
                nameSpace = collectionName+"/"+projectName;

                extDir = getExternalFilesDir(null);

                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state) && extDir != null) {

                    System.out.println(extDir.getAbsolutePath());

                    extDir = new File(extDir.getAbsolutePath()+"/" + serverName + "/" + collectionName + "/" + projectName);

                    if (extDir.mkdirs()) {
                        System.out.println("new external directory created");
                    }
                }


                return getRemoteProject();
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
                AlertDialog.Builder adb=new AlertDialog.Builder(ProjectsActivity.this);

                adb.setTitle("Error");
                adb.setMessage("Download failed.");

                adb.show();

                if (projectFile.delete()){
                    System.out.println("project file deleted");
                }
            }
            else {
                launchPlayer(projectName);
            }
        }
        @SuppressWarnings("deprecation")
        protected String[] getRemoteProject() {
            try {
                System.out.println("download project attempt");

                String audioFileName;

                ArrayList<String> imageFileNames = new ArrayList<>();

                MongoCollection<org.bson.Document> collection = getMongoDB().getCollection(collectionName);

                try {
                    FileOutputStream fos = new FileOutputStream(projectFile);

                    XmlSerializer serializer = Xml.newSerializer();
                    serializer.setOutput(fos, "UTF-8");
                    serializer.startDocument("UTF-8", true);
                    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                    serializer.startTag("", "root");
                    serializer.startTag("", "ID");
                    serializer.text(Integer.toString((int) (Math.random() * 10000)));
                    serializer.endTag("", "ID");

                    // read database

                    org.bson.BsonDocument projectDocument = new BsonDocument();

                    // read project meta

                    MongoCursor<Document> cursor  = collection.find(
                            new Document("name", projectName)).projection(Projections.excludeId()).iterator();

                    while (cursor.hasNext()) {
                        projectDocument = org.bson.BsonDocument.parse(cursor.next().toJson());
                        System.out.println("document found:");
                        System.out.println(projectDocument.toString());
                    }
                    cursor.close();

                    // Construct a BsonReader
                    BsonReader reader = new BsonDocumentReader(projectDocument);

                    reader.readStartDocument();

                    // read project name
                    System.out.println("project name: " + projectName);
                    reader.readName();
                    String remoteProject = reader.readString();
                    System.out.println("remote name: " + remoteProject);
                    if (!projectName.equals(remoteProject)) {
                        throw new IOException("remote project error!");
                    }

                    // read audio file name
                    reader.readName();
                    audioFileName = reader.readString();
                    serializer.startTag("", "audio");
                    serializer.startTag("", "uri");
                    serializer.text(extDir.getAbsolutePath() + "/" + audioFileName);
                    serializer.endTag("", "uri");
                    serializer.endTag("", "audio");

                    // read images
                    reader.readName();
                    reader.readStartArray();
                    while (reader.readBsonType() == BsonType.DOCUMENT){
                        System.out.println("reading image data");
                        serializer.startTag("", "image");
                        reader.readStartDocument();
                        reader.readName();
                        serializer.startTag("", "uri");
                        String imageFileName = reader.readString();
                        imageFileNames.add(imageFileName);
                        serializer.text(extDir.getAbsolutePath() + "/" + imageFileName);
                        serializer.endTag("", "uri");
                        reader.readName();
                        serializer.startTag("", "displayDuration");
                        serializer.text(Integer.toString(reader.readInt32()));
                        serializer.endTag("", "displayDuration");
                        reader.readEndDocument();
                        serializer.endTag("", "image");
                        System.out.println("image read");
                    }
                    reader.readEndArray();
                    reader.readEndDocument();

                    // finish XML

                    serializer.endTag("", "root");
                    serializer.endDocument();

                    serializer.flush();
                    fos.close();
                    System.out.println("new xml created");

                    // connect to GridFS
                    GridFS fileStore = new GridFS(getMongoClient().getDB(serverName), nameSpace);

                    // Download audio
                    String dbFileName = audioFileName;
                    GridFSDBFile remoteFile = fileStore.findOne(dbFileName);
                    System.out.println("download audio");
                    remoteFile.writeTo(extDir.getAbsolutePath() + "/" + dbFileName);

                    // Download images
                    for (int i = 0; i < imageFileNames.size(); i++) {
                        dbFileName = imageFileNames.get(i);
                        remoteFile = fileStore.findOne(dbFileName);
                        System.out.println("download image");
                        remoteFile.writeTo(extDir.getAbsolutePath() + "/" + dbFileName);
                    }

                    System.out.println("download complete");

                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }

                return new String[1];

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return null;
        }
    }

    public void launchPlayer(String projectName) {
        Intent intent = new Intent(ProjectsActivity.this, PlayerActivity.class);

        intent.putExtra(getString(R.string.stringExtraProjectName), projectName);
        intent.putExtra(getString(R.string.stringExtraServerName), serverName);
        intent.putExtra(getString(R.string.stringExtraCollectionName), collectionName);

        String projectFilePath = this.getFilesDir().getPath() + "/"+serverName+"/"+collectionName+"/"+projectName+".xml";

        System.out.println(projectFilePath);

        intent.putExtra(getString(R.string.stringExtraProjectFilePath), projectFilePath);

        startActivity(intent);
    }

    public void createProject(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        builder.setMessage("Enter the title for your new project");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFilters(new InputFilter[]{
                new InputFilter() {
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (!Character.isLetterOrDigit(source.charAt(i))) {
                                return "";
                            }
                        }
                        return null;
                    }
                }
        });
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
        if(resultCode == REQUEST_PROJECT_RELEASE && data != null)
        {
            // fetch the message String
            String projectName=data.getStringExtra(getString(R.string.stringExtraProjectName));

            System.out.println(projectName);

            if (projectNames.indexOf(projectName) != -1 && removeProjectFromList(projectNames.indexOf(projectName))) {
                if (addProjectToList(projectName+"(remote)")) {
                    adapter.notifyDataSetChanged();
                    System.out.println("appending successful");
                    AlertDialog.Builder adb = new AlertDialog.Builder(ProjectsActivity.this);
                    adb.setTitle("Success");
                    adb.setMessage("Your project is now listed as a remote project");
                    adb.show();
                }
            }
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
            if (projectNames.indexOf(projectName+"(remote)") != -1) {
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
