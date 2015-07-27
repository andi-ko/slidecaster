package de.upb.ddi.slidecaster.util;

import android.util.Xml;

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

public class XMLHelpers {

    public static void initProjectFile(File projectFile) {

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
            System.out.println("new xml created");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readProjectData(File projectFile, ArrayList<String> uriList, ArrayList<Integer> displayDurationList) {

        System.out.println("reading project file");

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
                            return audioNodeUri;
                        }
                    }
                }
            }

        } catch (ParserConfigurationException pce) {
            System.err.println(pce.getMessage());
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
