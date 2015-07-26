package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
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
import java.util.concurrent.TimeUnit;

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

import android.R.drawable;



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

    private int audioDuration;
    private int totalDisplayDuration;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private MediaPlayer mPlayer;
    private SeekBar seekbar;

    private TextView totalDurationTextView;
    private TextView playTimeTextView;

    ImageButton recordButton;
    ImageButton playButtton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_editor);

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

        audioDuration = 0;
        totalDisplayDuration = 0;

        serverName = getIntent().getStringExtra(getString(R.string.stringExtraServerName));
        collectionName = getIntent().getStringExtra(getString(R.string.stringExtraCollectionName));
        projectName = getIntent().getStringExtra(getString(R.string.stringExtraProjectName));

        //EditText projectNameEditText = (EditText) findViewById(R.id.projectNameEditText);
        // projectNameEditText.setText(projectName, TextView.BufferType.EDITABLE);

        projectFile = new File(this.getFilesDir().getPath()+"/"+serverName+"/"+collectionName+"/"+projectName+".xml");

        File extDir = this.getExternalFilesDir(null);

        if (extDir != null) {
            System.out.println("audio file path set");
            audioFileUri = extDir.getAbsolutePath()+ "/" + serverName + "/" + collectionName + "/" + projectName + "/" + "record.mp4";
        }
        audioAdded = false;

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

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                AlertDialog.Builder adb = new AlertDialog.Builder(EditorActivity.this);

                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete this image?");

                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String imageToDeletePath = uriList.get(position);

                        if (removeImageFromList(position)) {
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

    public void onRecord(View view) {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    public void onPlay(View view) {
        if (!isPlaying) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFileUri);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        playButtton.setImageResource(R.drawable.ic_media_pause);
        isPlaying = true;
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        playButtton.setImageResource(R.drawable.ic_media_play);
        isPlaying = false;
    }

    private void startRecording() {
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
        loadAudio(audioFileUri);
    }

    private void loadAudio(String uri) {
        MediaPlayer mp = MediaPlayer.create(this, Uri.parse(uri));
        long duration = mp.getDuration();
        totalDurationTextView.setText(String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))));
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
                        if (!audioNodeUri.isEmpty()) {
                            audioFileUri = audioNodeUri;
                            audioAdded = true;
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
