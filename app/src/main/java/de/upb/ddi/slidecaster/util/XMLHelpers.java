package de.upb.ddi.slidecaster.util;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
}
