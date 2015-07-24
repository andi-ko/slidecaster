package de.upb.ddi.slidecaster.net;

import android.os.AsyncTask;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

public abstract class DatabaseTask extends AsyncTask<String, Void, String[]> {
    /** The system calls this to perform work in a worker thread and
     * delivers it the parameters given to AsyncTask.execute() */

    static MongoClient mongoClient;

    public static MongoDatabase getMongoDB() {
        return mongoDB;
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }

    public static String getTextUri() {
        return textUri;
    }

    public static boolean isConnected() {
        return isConnected;
    }

    static MongoDatabase mongoDB;
    static String textUri;

    static boolean isConnected = false;

    @Override
    protected String[] doInBackground(String... params) {

        if (params.length > 0) {
            textUri = params[0];
        }

        if (mongoClient != null) {
            try {
                mongoClient.listDatabaseNames();
            } catch (Exception e) {
                isConnected = false;
            }
        }
        if (!isConnected) {
            connect(textUri);
        }
        try {
            return process();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected abstract String[] process();

    protected void connect (String textUri) {

        //<dbuser>:<dbpassword>@ds059702.mongolab.com:59702/google-datastore

        System.out.println(textUri);

        MongoClientURI uri = new MongoClientURI(textUri);

        System.out.println(textUri);

        try {
            mongoClient = new MongoClient(uri);

            System.out.println("Client created: "+mongoClient.getConnectPoint());

            mongoDB = mongoClient.getDatabase(uri.getDatabase());

            isConnected = true;

        }  catch (Exception e) {
            e.printStackTrace();
        }
    }
}
