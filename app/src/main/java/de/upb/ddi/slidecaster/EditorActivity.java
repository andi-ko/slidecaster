package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.upb.ddi.slidecaster.util.CustomList;
import de.upb.ddi.slidecaster.util.SystemUiHider;


public class EditorActivity extends Activity {

    private String serverName;
    private String collectionName;
    private String projectName;
    private CustomList adapter;

    private ArrayList<String> uriList;
    private ArrayList<Integer> displayDurationList;

    private String newImageFileName;

    private File projectFile;

    private String audioFileUri;

    private int audioDuration;
    private int totalDisplayDuration;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_editor);

        audioDuration = 0;
        totalDisplayDuration = 0;

        serverName = getIntent().getStringExtra(getString(R.string.stringExtraServerName));
        collectionName = getIntent().getStringExtra(getString(R.string.stringExtraCollectionName));
        projectName = getIntent().getStringExtra(getString(R.string.stringExtraProjectName));

        //EditText projectNameEditText = (EditText) findViewById(R.id.projectNameEditText);
        // projectNameEditText.setText(projectName, TextView.BufferType.EDITABLE);

        projectFile = new File(this.getFilesDir().getPath()+"/"+serverName+"/"+collectionName+"/"+projectName+".xml");

        // System.out.println(projectFile.delete());

        if (projectFile.getParentFile().mkdirs()) {
            System.out.println("path created");
        }

        try {
            if (projectFile.createNewFile()) {
                System.out.println("new project file created");
                firstRun(projectFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        readProjectData(projectFile);

        adapter = new CustomList(EditorActivity.this, uriList, displayDurationList);
        ListView list = (ListView)findViewById(R.id.imageListView);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(EditorActivity.this, "You Clicked at item: " + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void cameraButttonPressed(View view) {

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {

            File extDir = this.getExternalFilesDir(null);

            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) && extDir != null) {

                try {
                    File newImageFile = new File(extDir.getAbsolutePath()+ "/" + serverName + "/" + collectionName + "/" + projectName + "/" + uriList.size() + ".jpg");

                    System.out.println(newImageFile.delete());

                    if (newImageFile.getParentFile().mkdirs()) {
                        System.out.println("first image!");
                    }

                    if (newImageFile.createNewFile()) {

                        newImageFileName = newImageFile.getAbsolutePath();

                        System.out.println("newImageFileName: "+ newImageFileName);

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(newImageFile)); // set the image file name

                        // start the image capture Intent
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            } else {
                AlertDialog.Builder adb = new AlertDialog.Builder(EditorActivity.this);

                adb.setTitle("Error");
                adb.setMessage("Missing write access to external storage.");
                adb.show();
            }
        } else {
            AlertDialog.Builder adb = new AlertDialog.Builder(EditorActivity.this);

            adb.setTitle("Error");
            adb.setMessage("Camera not available.");
            adb.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            System.out.println("picture taken!");

            int displayDuration;

            if (audioDuration > totalDisplayDuration) {
                displayDuration = audioDuration - totalDisplayDuration;
            }
            else {
                displayDuration = 1;
            }
            if (addImageToList(newImageFileName, displayDuration)) {
                System.out.println("add image: "+newImageFileName);
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected void updateDisplayDuration(int duration, int position) {
        displayDurationList.add(position, duration);
        displayDurationList.remove(position+1);
        adapter.notifyDataSetChanged();
    }

    public void firstRun(File projectFile) {

        try {
            FileOutputStream fos = new FileOutputStream(projectFile);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument("UTF-8", true);
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

    public void readProjectData(File projectFile) {

        uriList = new ArrayList<>();
        displayDurationList = new ArrayList<>();

        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            Document dom = db.parse(projectFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            dom.getDocumentElement().normalize();

            Element doc = dom.getDocumentElement();

            NodeList imageList = doc.getElementsByTagName("image");

            for (int i = 0; i < imageList.getLength(); i++) {

                Node imageNode = imageList.item(i);

                if (imageNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) imageNode;

                    String imageNodeUri = eElement.getElementsByTagName("uri").item(0).getTextContent();

                    String imageNodeDisplayDuration = eElement.getElementsByTagName("displayDuration").item(0).getTextContent();

                    System.out.println(imageNodeUri);
                    System.out.println(imageNodeDisplayDuration);

                    if (imageNodeUri != null) {
                        if (!imageNodeUri.isEmpty())
                            uriList.add(imageNodeUri);
                    }
                    if (imageNodeDisplayDuration != null) {
                        if (!imageNodeDisplayDuration.isEmpty()) {
                            displayDurationList.add(Integer.parseInt(imageNodeDisplayDuration));
                            totalDisplayDuration += Integer.parseInt(imageNodeDisplayDuration);
                        }
                    }
                }
            }

            NodeList audioList = doc.getElementsByTagName("audio");

            for (int i = 0; i < audioList.getLength(); i++) {

                Node audioNode = audioList.item(i);

                if (audioNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) audioNode;

                    String audioNodeUri = eElement.getElementsByTagName("uri").item(0).getTextContent();

                    System.out.println(audioNodeUri);

                    if (audioNodeUri != null) {
                        if (!audioNodeUri.isEmpty())
                            audioFileUri = audioNodeUri;
                    }
                }
            }

        } catch (ParserConfigurationException pce) {
            System.err.println(pce.getMessage());
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean removeImageFromList(int position) {

        String imageName = uriList.get(position);
        int displayDuration = displayDurationList.get(position);

        System.out.println("search: "+imageName);

        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            Document dom = db.parse(projectFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            dom.getDocumentElement().normalize();

            Element doc = dom.getDocumentElement();

            NodeList imageList = doc.getElementsByTagName("image");

            for (int i = 0; i < imageList.getLength(); i++) {

                Node imageNode = imageList.item(i);

                if (imageNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) imageNode;

                    System.out.println(eElement.getElementsByTagName("urri").item(0).getTextContent());

                    if (imageName.equals(eElement.getElementsByTagName("uri").item(0).getTextContent())) {
                        if (displayDuration == Integer.parseInt(eElement.getElementsByTagName("displayDuration").item(0).getTextContent())) {
                            System.out.println("remove: " + eElement.getElementsByTagName("uri").item(0).getTextContent());
                            imageNode.getParentNode().removeChild(imageNode);
                        }
                    }
                }
            }

            DOMSource source = new DOMSource(dom);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(projectFile);
            transformer.transform(source, result);

            uriList.remove(position);
            displayDurationList.remove(position);
            totalDisplayDuration -= displayDurationList.get(position);

            return true;

        } catch (ParserConfigurationException | SAXException | TransformerException | IOException pce) {
            System.err.println(pce.getMessage());
        }
        return false;
    }

    public boolean addImageToList(String imageUri, int displayDuration) {

        if (imageUri != null && !imageUri.isEmpty()) {

            System.out.println(imageUri);

            if (uriList.indexOf(imageUri) != -1) {
                System.out.println("element already exists");
                return false;
            }

            // Make an  instance of the DocumentBuilderFactory
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            try {
                // use the factory to take an instance of the document builder
                DocumentBuilder db = dbf.newDocumentBuilder();
                // parse using the builder to get the DOM mapping of the
                // XML file
                Document dom = db.parse(projectFile);

                //optional, but recommended
                //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
                dom.getDocumentElement().normalize();

                Element doc = dom.getDocumentElement();

                // server elements
                Element newImage = dom.createElement("image");

                Element uri = dom.createElement("uri");
                uri.appendChild(dom.createTextNode(imageUri));
                newImage.appendChild(uri);

                Element displayDurationElement = dom.createElement("displayDuration");
                displayDurationElement.appendChild(dom.createTextNode(Integer.toString(displayDuration)));
                newImage.appendChild(displayDurationElement);

                doc.appendChild(newImage);

                DOMSource source = new DOMSource(dom);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                StreamResult result = new StreamResult(projectFile);
                transformer.transform(source, result);

                uriList.add(imageUri);
                displayDurationList.add(displayDuration);
                totalDisplayDuration += displayDuration;

                return true;

            } catch (SAXException | ParserConfigurationException se) {
                System.out.println(se.getMessage());
            } catch (IOException | TransformerException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        System.out.println("missing element");
        return false;
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

}
