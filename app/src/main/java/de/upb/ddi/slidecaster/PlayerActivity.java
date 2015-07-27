package de.upb.ddi.slidecaster;

import de.upb.ddi.slidecaster.util.SystemUiHider;
import de.upb.ddi.slidecaster.util.XMLHelpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PlayerActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION | SystemUiHider.FLAG_FULLSCREEN;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private ArrayList<String> uriList;
    private ArrayList<Integer> displayDurationList;

    private String audioFileUri;

    private boolean isPlaying = false;

    private MediaPlayer mPlayer;
    private boolean audioAdded;

    private Handler myHandler;

    private ImageView imageView;

    private int currentImagePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);

        final View controlsView = findViewById(R.id.playerActivityImageView);
        final View contentView = findViewById(R.id.playerActivityImageView);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        imageView = (ImageView) findViewById(R.id.playerActivityImageView);

        uriList = new ArrayList<>();
        displayDurationList = new ArrayList<>();

        File projectFile = new File(getIntent().getStringExtra(getString(R.string.stringExtraProjectFilePath)));

        audioFileUri = XMLHelpers.readProjectData(projectFile, uriList, displayDurationList);
        loadAudio(audioFileUri);

        currentImagePosition = 0;

        myHandler = new Handler();
        startPlaying();
    }

    private Runnable RunSlideshow = new Runnable() {

        int time;
        int nextImageAt = 0;

        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                time = mPlayer.getCurrentPosition();

                if (time > nextImageAt && uriList.size() > currentImagePosition) {
                    nextImageAt += (displayDurationList.get(currentImagePosition) * 1000);
                    imageView.setImageURI(Uri.parse(uriList.get(currentImagePosition)));

                    currentImagePosition++;
                }
                myHandler.postDelayed(this, 100);
            } else {
                stopPlaying();
                finish();
            }
        }
    };

    public void onPlay(View view) {
        if (audioAdded) {
            if (!isPlaying) {
                startPlaying();
            } else {
                stopPlaying();
            }
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFileUri);
            mPlayer.prepare();
            myHandler.postDelayed(RunSlideshow, 0);
            mPlayer.start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        isPlaying = true;
    }

    private void stopPlaying() {
        if (mPlayer != null && isPlaying) {
            mPlayer.release();
            mPlayer = null;
            isPlaying = false;
        }
    }

    private void loadAudio(String uri) {
        if (uri != null) {
            System.out.println("loading audio");
            audioAdded = true;
            MediaPlayer mp = MediaPlayer.create(this, Uri.parse(uri));
            long duration = mp.getDuration();
            System.out.println("duration: " + duration + " ms");
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

    @Override
    public void finish() {
        stopPlaying();
        super.finish();
    }
}