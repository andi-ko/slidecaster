package de.upb.ddi.slidecaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends Activity {

    private ArrayList<String> serverNames;
    private ArrayList<String> serverAdresses;

    private String defaulServerName;
    private String defaulServerAddress;

    private File serverListFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("=========================== MainActivity: onCreate(): begin ===========================");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        String serverListFileName = getString(R.string.serverListFileName);
        defaulServerName = getString(R.string.defaultServerName);
        defaulServerAddress = getString(R.string.defaultServerAddress);

        serverListFile = new File(getBaseContext().getFilesDir(), serverListFileName);

        // serverListFile.delete();

        try {
            if (serverListFile.createNewFile()) {
                firstRun(serverListFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        readServerList(serverListFile);

        final ListView serverListView = (ListView)findViewById(R.id.serverListView);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, serverNames);

        serverListView.setAdapter(adapter);

        serverListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra(getString(R.string.stringExtraServerAddress), serverAdresses.get(position));
                intent.putExtra(getString(R.string.stringExtraServerName), serverNames.get(position));
                startActivity(intent);
            }
        });

        serverListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                AlertDialog.Builder adb=new AlertDialog.Builder(MainActivity.this);

                if (serverNames.get(position).equals(defaulServerName)) {
                    adb.setTitle("Denied");
                    adb.setMessage("Default server cannot be deleted");
                }
                else {
                    adb.setTitle("Delete?");
                    adb.setMessage("Are you sure you want to delete " + serverNames.get(position));
                    final int positionToRemove = position;

                    adb.setNegativeButton("Cancel", null);
                    adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            removeServerFromList(positionToRemove);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
                adb.show();
                return true;
            }
        });
    }

    public void firstRun(File serverListFile) {

        System.out.println("=========================== MainActivity: firstRun() ===========================");

        try {
            FileOutputStream fos = new FileOutputStream(serverListFile);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag(null, "root");
            serializer.startTag(null, "server");
            serializer.startTag(null, "name");
            serializer.text(defaulServerName);
            serializer.endTag(null, "name");
            serializer.startTag(null, "address");
            serializer.text(defaulServerAddress);
            serializer.endTag(null, "address");
            serializer.endTag(null, "server");
            serializer.endTag(null, "root");
            serializer.endDocument();

            serializer.flush();

            fos.close();

            System.out.println("=========================== MainActivity: firstRun(): serverListFile closed ===========================");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addServer(View view) {
        Intent intent = new Intent(this, AddServerActivity.class);
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // check if the request code is same as what is passed, here it is 2
        if(requestCode==2 && data != null)
        {
            // fetch the message String
            String serverName=data.getStringExtra(getString(R.string.stringExtraServerName));
            String serverAddress=data.getStringExtra(getString(R.string.stringExtraServerAddress));

            if (!addServerToList(serverName, serverAddress)) {
                AlertDialog.Builder adb=new AlertDialog.Builder(MainActivity.this);

                adb.setTitle("Error");
                adb.setMessage("Server name already exists");
                adb.show();
            }

            final ListView serverListView = (ListView)findViewById(R.id.serverListView);

            ((ArrayAdapter)serverListView.getAdapter()).notifyDataSetChanged();

        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void readServerList (File serverListFile) {

        System.out.println("=========================== MainActivity: readServerList() ===========================");

        serverNames = new ArrayList<>();
        serverAdresses = new ArrayList<>();
        String serverName;
        String serverAddress;

        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            Document dom = db.parse(serverListFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            dom.getDocumentElement().normalize();

            Element doc = dom.getDocumentElement();

            System.out.println("=========================== MainActivity: readServerList(): get servers ===========================");

            NodeList serverList = doc.getElementsByTagName("server");

            for (int i = 0; i < serverList.getLength(); i++) {

                System.out.println("=========================== MainActivity: readServerList(): read server ===========================");

                Node serverNode = serverList.item(i);

                if (serverNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) serverNode;

                    serverName = ((Element) serverNode).getElementsByTagName("name").item(0).getTextContent();

                    System.out.println(serverName);

                    if (serverName != null) {
                        if (!serverName.isEmpty())
                            serverNames.add(serverName);
                    }

                    serverAddress = ((Element) serverNode).getElementsByTagName("address").item(0).getTextContent();

                    System.out.println(serverAddress);

                    if (serverAddress != null) {
                        if (!serverAddress.isEmpty())
                            serverAdresses.add(serverAddress);
                    }
                }
            }

            System.out.println("=========================== MainActivity: readServerList(): success ===========================");
        } catch (ParserConfigurationException pce) {
            System.err.println(pce.getMessage());
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public void removeServerFromList(int position) {

        System.out.println("=========================== MainActivity: removeServerFromList(): begin ===========================");

        String serverName = serverNames.get(position);
        String serverAddress = serverAdresses.get(position);

        System.out.println(serverName);
        System.out.println(serverAddress);

        serverNames.remove(position);
        serverAdresses.remove(position);

        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            Document dom = db.parse(serverListFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            dom.getDocumentElement().normalize();

            Element doc = dom.getDocumentElement();

            NodeList serverList = doc.getElementsByTagName("server");

            System.out.println("=========================== MainActivity: removeServerFromList(): process server list ===========================");

            for (int i = 0; i < serverList.getLength(); i++) {

                Node serverNode = serverList.item(i);

                if (serverNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) serverNode;

                    System.out.println("=========================== MainActivity: removeServerFromList(): check server item ===========================");

                    System.out.println(((Element) serverNode).getElementsByTagName("name").item(0).getTextContent());
                    System.out.println(((Element) serverNode).getElementsByTagName("address").item(0).getTextContent());

                    if (serverName.equals(((Element) serverNode).getElementsByTagName("name").item(0).getTextContent()) &&
                            serverAddress.equals(((Element) serverNode).getElementsByTagName("address").item(0).getTextContent())) {
                        System.out.println("remove: " + ((Element) serverNode).getElementsByTagName("name").item(0).getTextContent());
                        serverNode.getParentNode().removeChild(serverNode);
                    }
                }
            }

            DOMSource source = new DOMSource(dom);

            System.out.println("=========================== MainActivity: removeServerFromList(): write file ===========================");

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(serverListFile);
            transformer.transform(source, result);

            System.out.println("=========================== MainActivity: removeServerFromList(): success ===========================");
        } catch (ParserConfigurationException | SAXException | TransformerException | IOException pce) {
            System.err.println(pce.getMessage());
        }
    }

    public boolean addServerToList(String serverName, String serverAddress) {

        System.out.println("=========================== MainActivity: addServerToList(): begin ===========================");

        if (serverName != null && !serverName.isEmpty() && serverAddress!=null && !serverAddress.isEmpty()) {

            System.out.println("=========================== MainActivity: addServerToList(): add to list ===========================");

            System.out.println(serverName);

            System.out.println(serverAddress);

            if (serverNames.indexOf(serverName) != -1) {
                return false;
            }

            serverNames.add(serverName);
            serverAdresses.add(serverAddress);

            // Make an  instance of the DocumentBuilderFactory
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            try {
                // use the factory to take an instance of the document builder
                DocumentBuilder db = dbf.newDocumentBuilder();
                // parse using the builder to get the DOM mapping of the
                // XML file
                Document dom = db.parse(serverListFile);

                //optional, but recommended
                //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
                dom.getDocumentElement().normalize();

                Element doc = dom.getDocumentElement();

                System.out.println("=========================== MainActivity: addServerToList(): add server to root ===========================");
                // server elements
                Element newServer = dom.createElement("server");

                Element name = dom.createElement("name");
                name.appendChild(dom.createTextNode(serverName));
                newServer.appendChild(name);

                Element address = dom.createElement("address");
                address.appendChild(dom.createTextNode(serverAddress));
                newServer.appendChild(address);

                doc.appendChild(newServer);

                System.out.println("=========================== MainActivity: addServerToList(): append server data ===========================");

                DOMSource source = new DOMSource(dom);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                StreamResult result = new StreamResult(serverListFile);
                transformer.transform(source, result);

                System.out.println("=========================== MainActivity: addServerToList(): stream new file ===========================");

            } catch (SAXException | ParserConfigurationException se) {
                System.out.println(se.getMessage());
            } catch (IOException | TransformerException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
        return true;
    }
}
