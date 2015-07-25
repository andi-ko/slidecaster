package de.upb.ddi.slidecaster.net;

import android.os.AsyncTask;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

public abstract class DatabaseTask extends AsyncTask<String, Void, String[]> {

    static MongoClient mongoClient;
    static MongoDatabase mongoDB;

    public static void setTextUri(String textUri) {
        DatabaseTask.textUri = textUri;
    }

    static String textUri;

    public static MongoDatabase getMongoDB() {
        return mongoDB;
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }

    public static String getTextUri() {
        return textUri;
    }

    /** The system calls this to perform work in a worker thread and
     * delivers it the parameters given to AsyncTask.execute() */

    protected boolean isConnected() {

        if (mongoDB != null) {
            try {
                mongoDB.listCollectionNames();
                return true;
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return false;
    }

    protected boolean reconnect () {
        return textUri != null && connect(textUri);
    }

    protected boolean assertConnection () {
        System.out.println("assert connection");
        return isConnected() || reconnect();
    }

    protected boolean connect (String textUri) {

        mongoDB = null;
        mongoClient = null;
        setTextUri(textUri);

        //<dbuser>:<dbpassword>@ds059702.mongolab.com:59702/google-datastore

        MongoClientURI uri = new MongoClientURI(textUri);

        try {
            System.out.println("Connecting to: "+textUri);

            mongoClient = new MongoClient(uri);

            System.out.println("Connected to: "+mongoClient.getConnectPoint());

            mongoDB = mongoClient.getDatabase(uri.getDatabase());

            return true;

        }  catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
