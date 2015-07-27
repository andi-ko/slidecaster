package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.bson.BsonDocumentWriter;
import org.bson.BsonWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.upb.ddi.slidecaster.net.DatabaseTask;
import de.upb.ddi.slidecaster.util.CustomList;

import com.mongodb.client.MongoCollection;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;


public class EditorActivity extends Activity {

    private static final String LOG_TAG = "AudioRecordTest";

    private MediaRecorder mRecorder;

    private String serverName;
    private String collectionName;
    private String projectName;
    private CustomList adapter;

    private ArrayList<String> uriList;
    private ArrayList<Integer> displayDurationList;

    private String newImageFileName;

    private File projectFile;

    private String audioFileUri;
    private boolean audioAdded;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PROJECT_PAUSE = 1;
    static final int REQUEST_PROJECT_RELEASE = 2;

    private MediaPlayer mPlayer;
    private SeekBar seekbar;

    private TextView totalDurationTextView;
    private TextView playTimeTextView;

    private ProgressDialog progressDialog;

    private ImageButton recordButton;
    private ImageButton playButtton;

    private File extDir;

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("=========================== EditorActivity: onCreate(): begin ===========================");
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_editor);

        intent = new Intent(this, ProjectsActivity.class);

        // Set The Result in Intent
        setResult(REQUEST_PROJECT_PAUSE, intent);

        recordButton = (ImageButton)findViewById(R.id.recordAudioButton);
        recordButton.setImageResource(R.drawable.ic_btn_speak_now);

        playButtton = (ImageButton)findViewById(R.id.playAudioButton);
        playButtton.setImageResource(R.drawable.ic_media_play);

        seekbar = (SeekBar)findViewById(R.id.seekBar);
        seekbar.setClickable(false);

        totalDurationTextView  = (TextView) findViewById(R.id.totalDurationTextView);
        playTimeTextView = (TextView) findViewById(R.id.playingTimeTextView);

        mPlayer = null;
        mRecorder = null;

        serverName = getIntent().getStringExtra(getString(R.string.stringExtraServerName));
        collectionName = getIntent().getStringExtra(getString(R.string.stringExtraCollectionName));
        projectName = getIntent().getStringExtra(getString(R.string.stringExtraProjectName));

        //EditText projectNameEditText = (EditText) findViewById(R.id.projectNameEditText);
        // projectNameEditText.setText(projectName, TextView.BufferType.EDITABLE);

        projectFile = new File(this.getFilesDir().getPath()+"/"+serverName+"/"+collectionName+"/"+projectName+".xml");

        // System.out.println(projectFile.delete());

        extDir = this.getExternalFilesDir(null);

        if (extDir != null) {

            extDir = new File(extDir.getAbsolutePath()+"/" + serverName + "/" + collectionName + "/" + projectName);

            if (extDir.mkdirs()) {
                System.out.println("new external directory created");
            }

            audioFileUri = extDir.getAbsolutePath()+ "/" + "record.mp4";
        }
        audioAdded = false;

        if (projectFile.getParentFile().mkdirs()) {
            System.out.println("project file path created");
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
                AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
                builder.setTitle("Set duration");
                builder.setMessage("Enter the timespan for which this image is shown in seconds");

                // Set up the input
                final EditText input = new EditText(EditorActivity.this);

                // Specify the type of input expected
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);

                final int finalPosition = position;

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String duration = input.getText().toString();
                        if (duration.length() > 0) {
                            updateImageDisplayDuration(finalPosition, Integer.parseInt(duration));
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
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                AlertDialog.Builder adb = new AlertDialog.Builder(EditorActivity.this);

                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete this image?");

                final int pos = position;

                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String imageToDeletePath = uriList.get(pos);

                        if (removeImageFromList(pos)) {
                            if ((new File(imageToDeletePath)).delete()) {
                                System.out.println("image deleted");
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
                adb.show();
                return true;
            }
        });
    }

    public void cameraButttonPressed(View view) {

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {

            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) && extDir != null) {

                try {
                    File newImageFile = new File(extDir.getAbsolutePath()+ "/" + uriList.size() + ".jpg");

                    newImageFileName = newImageFile.getAbsolutePath();

                    System.out.println("newImageFileName: "+ newImageFileName);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(newImageFile)); // set the image file name

                    // start the image capture Intent
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);

                } catch (Exception e) {
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

            int displayDuration = 1;

            if (addImageToList(newImageFileName, displayDuration)) {
                System.out.println("add image: "+newImageFileName);
                adapter.notifyDataSetChanged();
            }
        }
    }

    public void onRecord(View view) {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    public void onPlay(View view) {
        if(audioAdded) {
            if (!isPlaying) {
                startPlaying();
            } else {
                stopPlaying();
            }
        }
    }

    private Handler myHandler = new Handler();

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFileUri);
            mPlayer.prepare();
            seekbar.setMax(mPlayer.getDuration());
            mPlayer.start();
            myHandler.postDelayed(UpdateSongTime,100);
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        playButtton.setImageResource(R.drawable.ic_media_stop);
        isPlaying = true;
    }

    private Runnable UpdateSongTime = new Runnable() {

        int time;

        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                time = mPlayer.getCurrentPosition();
                playTimeTextView.setText(
                        String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(time)) + ":" +
                            String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(time) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)))+" ");
                seekbar.setProgress(time);
                myHandler.postDelayed(this, 100);
            }
            else {
                stopPlaying();
                playTimeTextView.setText("");
                seekbar.setProgress(0);
            }
        }
    };

    private void stopPlaying() {
        if (mPlayer != null && isPlaying) {
            mPlayer.release();
            mPlayer = null;
            playButtton.setImageResource(R.drawable.ic_media_play);
            isPlaying = false;
        }
    }

    private void startRecording() {
        if((new File(audioFileUri)).delete()){
            System.out.println("new recording!");
        }
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(audioFileUri);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            System.out.println("preparing record");
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        recordButton.setImageResource(R.drawable.btn_circle_selected);
        isRecording = true;
        Toast.makeText(getApplicationContext(), "Recording",Toast.LENGTH_SHORT).show();
        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        recordButton.setImageResource(R.drawable.ic_btn_speak_now);
        Toast.makeText(getApplicationContext(), "Recording finished",Toast.LENGTH_SHORT).show();
        isRecording = false;
        addAudio(audioFileUri);
    }

    private void addAudio(String uri) {
        if (!audioAdded) {
            addAudioFileURI(uri);
        }
        loadAudio(uri);
    }

    private void loadAudio(String uri) {
        System.out.println("loading audio");
        audioAdded = true;
        MediaPlayer mp = MediaPlayer.create(this, Uri.parse(uri));
        long duration = mp.getDuration();
        System.out.println("duration: " + duration + " ms");
        totalDurationTextView.setText(
                String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(duration)) + ":" +
                        String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(duration) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))) + " ");
    }


    public void firstRun(File projectFile) {

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
            serializer.endTag("", "root");
            serializer.endDocument();

            serializer.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("new xml created");
    }

    public void readProjectData(File projectFile) {

        System.out.println("reading project file");

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
                        if (!imageNodeUri.isEmpty()) {
                            System.out.println("adding image: "+imageNodeUri);
                            uriList.add(imageNodeUri);
                        }
                    }
                    if (imageNodeDisplayDuration != null) {
                        if (!imageNodeDisplayDuration.isEmpty()) {
                            System.out.println("adding duration: " + imageNodeDisplayDuration);
                            displayDurationList.add(Integer.parseInt(imageNodeDisplayDuration));
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
                        if (!audioNodeUri.isEmpty()) {
                            audioFileUri = audioNodeUri;
                            loadAudio(audioFileUri);
                        }
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

                    System.out.println(eElement.getElementsByTagName("uri").item(0).getTextContent());

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

            return true;

        } catch (ParserConfigurationException | SAXException | TransformerException | IOException pce) {
            System.err.println(pce.getMessage());
        }
        return false;
    }

    public boolean updateImageDisplayDuration(int position, int duration) {

        String imageName = uriList.get(position);

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

                    System.out.println(eElement.getElementsByTagName("uri").item(0).getTextContent());

                    if (imageName.equals(eElement.getElementsByTagName("uri").item(0).getTextContent())) {

                        Element displayDurationNode = (Element)eElement.getElementsByTagName("displayDuration").item(0);

                        if (displayDurationNode != null) {
                            displayDurationNode.setTextContent(Integer.toString(duration));
                            System.out.println("new duration: " + duration);
                        }
                    }
                }
            }

            DOMSource source = new DOMSource(dom);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(projectFile);
            transformer.transform(source, result);

            displayDurationList.remove(position);

            displayDurationList.add(position, duration);

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
                org.w3c.dom.Document dom = db.parse(projectFile);

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

                System.out.println("append new image");

                doc.appendChild(newImage);

                DOMSource source = new DOMSource(dom);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                StreamResult result = new StreamResult(projectFile);
                transformer.transform(source, result);

                uriList.add(imageUri);
                displayDurationList.add(displayDuration);

                System.out.println("success");

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

    public boolean addAudioFileURI(String audioUri) {

        if (audioUri != null && !audioUri.isEmpty()) {

            System.out.println("adding audio uri: "+audioUri);

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

                Element audioElement = dom.createElement("audio");

                Element uri = dom.createElement("uri");
                uri.appendChild(dom.createTextNode(audioUri));
                audioElement.appendChild(uri);

                doc.appendChild(audioElement);

                DOMSource source = new DOMSource(dom);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                StreamResult result = new StreamResult(projectFile);
                transformer.transform(source, result);

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

    public void publishProject(View view) {
        AlertDialog.Builder adb = new AlertDialog.Builder(EditorActivity.this);
        adb.setTitle("Publish?");
        adb.setMessage("Are you sure you want to publish? A project once release cannot be unreleased.");

        adb.setNegativeButton("Cancel", null);
        adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (uriList.size() > 0 && audioAdded) {
                    ProgressDialog tempDialog = new ProgressDialog(EditorActivity.this);
                    progressDialog = tempDialog;
                    tempDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    tempDialog.setMessage("Uploading, please wait...");
                    tempDialog.setIndeterminate(true);
                    tempDialog.setCanceledOnTouchOutside(false);
                    tempDialog.show();

                    System.out.println("start uploading task ...");

                    ProjectReleaseTask task = new ProjectReleaseTask();
                    task.execute();
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(EditorActivity.this);

                    alert.setTitle("Error");
                    alert.setMessage("Audio recording and at least 1 Image are mandatory.");

                    alert.show();
                }
            }
        });
        adb.show();
    }

    private class ProjectReleaseTask extends DatabaseTask {

        @Override
        protected String[] doInBackground(String... params) {
            if (assertConnection()) {
                if (upload()){
                    String[] retValues = new String[1];
                    retValues[0] = projectName;
                    return retValues;
                }
            }
            return null;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(String[] result) {

            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            if (result == null) {
                AlertDialog.Builder adb=new AlertDialog.Builder(EditorActivity.this);

                adb.setTitle("Error");
                adb.setMessage("Upload failed.");

                adb.show();
            }
            else {

                Intent intent = new Intent(EditorActivity.this, ProjectsActivity.class);

                intent.putExtra(getString(R.string.stringExtraProjectName), projectName);

                // Set The Result in Intent
                setResult(REQUEST_PROJECT_RELEASE, intent);

                try {
                    File newFile = new File(projectFile.getParent()+projectName+"(remote).xml");
                    System.out.println("new project name: "+newFile.getAbsolutePath());
                    if (newFile.exists()){
                        System.out.println("file already exists");
                    }
                    // Rename file (or directory)
                    if (!projectFile.renameTo(newFile)) {
                        // File was not successfully renamed
                        System.out.println("rename failed");
                    }
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                try {
                    File newDir = new File(extDir.getParent()+projectName+"(remote)");
                    System.out.println("new ext dir: "+newDir.getAbsolutePath());
                    if (!newDir.exists()){
                        System.out.println("new directory lready exists");
                    }
                    // Rename file (or directory)
                    if (!projectFile.renameTo(newDir)) {
                        // File was not successfully renamed
                        System.out.println("rename failed");
                    }
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                finish();
            }
        }
        protected boolean upload() {

            try {
                MongoCollection<org.bson.Document> collection = getMongoDB().getCollection(collectionName);

                org.bson.BsonDocument projectDocument = new org.bson.BsonDocument();

                // Construct a BsonWriter
                BsonWriter writer = new BsonDocumentWriter(projectDocument);

                System.out.println("open writer");

                writer.writeStartDocument();
                writer.writeName("name");
                writer.writeString(projectName);
                writer.writeName("audioFile");
                writer.writeString("record.mp4");
                writer.writeName("images");
                writer.writeStartArray();
                for (int i = 0; i < uriList.size(); i++) {
                    writer.writeStartDocument();
                    writer.writeName("imageFile");
                    writer.writeString(uriList.get(i).substring(uriList.get(i).lastIndexOf("/")+1));
                    writer.writeName("displayDuration");
                    writer.writeInt32(displayDurationList.get(i));
                    writer.writeEndDocument();
                }
                writer.writeEndArray();
                writer.writeEndDocument();

                System.out.println("close writer");

                collection.insertOne(org.bson.Document.parse(projectDocument.toJson()));

                System.out.println("insert project meta");

                // Now let's store the binary file data using filestore GridFS
                GridFS fileStore = new GridFS(getMongoClient().getDB(serverName), collectionName + "/" + projectName);
                File install = new File(audioFileUri);
                GridFSInputFile inFile = fileStore.createFile(install);
                inFile.setFilename("record.mp4");
                inFile.save();
                System.out.println("audio data uploaded");
                for (int i = 0; i < uriList.size(); i++) {
                    //save the a file
                    install = new File(uriList.get(i));
                    inFile = fileStore.createFile(install);
                    inFile.setFilename(uriList.get(i).substring(uriList.get(i).lastIndexOf("/") + 1));
                    inFile.save();
                    System.out.println("image uploaded");
                }

                System.out.println("uploaded complete");

                return true;

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return false;
        }
    }

}
