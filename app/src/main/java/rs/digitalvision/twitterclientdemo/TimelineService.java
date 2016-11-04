package rs.digitalvision.twitterclientdemo;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class TimelineService extends Service {
    /**
     * twitter authentication key
     */
    public final static String TWITTER_KEY = "jJC29MJuuCfJKFVrvn0sxegxs";
    /**
     * twitter secret
     */
    public final static String TWITTER_SECRET = "PpCm9xQvvv00xCn3kvTmKz6KoHtYU01RohhYPFy8XW1TlbF6l0";
    /**
     * twitter object
     */
    private Twitter timelineTwitter;

    /**
     * database helper object
     */
    private DataHelper niceHelper;
    /**
     * timeline database
     */
    private SQLiteDatabase niceDB;

    /**
     * shared preferences for user details
     */
    private SharedPreferences nicePrefs;
    /**
     * handler for updater
     */
    private Handler niceHandler;
    /**
     * delay between fetching new tweets
     */
    private static int mins = 1;//alter to suit
    private static final long FETCH_DELAY = mins * (60 * 1000);
    //debugging tag
    private String LOG_TAG = "TimelineService";

    /**
     * updater thread object
     */
    private TimelineUpdater niceUpdater;

    Configuration twitConf;

    @Override
    public void onCreate() {
        super.onCreate();
        //setup the class
        //get prefs
        nicePrefs = getSharedPreferences("myPrefs", 0);
        //get database helper
        niceHelper = new DataHelper(this);
        //get the database
        niceDB = niceHelper.getWritableDatabase();

        //get user preferences
        String userToken = nicePrefs.getString("user_token", null);
        String userSecret = nicePrefs.getString("user_secret", null);

        //create new configuration
        twitConf = new ConfigurationBuilder()
                .setOAuthConsumerKey(TWITTER_KEY)
                .setOAuthConsumerSecret(TWITTER_SECRET)
                .setOAuthAccessToken(userToken)
                .setOAuthAccessTokenSecret(userSecret)
                .build();
        timelineTwitter = new TwitterFactory(twitConf).getInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleStart(intent, startId);
        return START_NOT_STICKY;

    }

    void handleStart(Intent intent, int startId) {
        // do work
        //get handler
        niceHandler = new Handler();
        //create an instance of the updater class
        niceUpdater = new TimelineUpdater();
        //add to run queue
        niceHandler.post(niceUpdater);
        //return sticky

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stop the updating
        niceHandler.removeCallbacks(niceUpdater);
        niceDB.close();
    }

    /**
     * TimelineUpdater class implements the runnable interface
     */
    class TimelineUpdater implements Runnable {
        //fetch updates
        //run method
        public void run() {
            //check for updates - assume none
            new GetHomeTimeline().execute();
            //delay fetching new updates
            niceHandler.postDelayed(this, FETCH_DELAY);
        }
    }

    class GetHomeTimeline extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... str) {
            boolean statusChanges = false;
            try {
                Log.e(LOG_TAG, "inside doInBackground...");
                //fetch timeline
                //retrieve the new home timeline tweets as a list
                //instantiate new twitter

                List<twitter4j.Status> homeTimeline = timelineTwitter.getHomeTimeline();
                Log.e(LOG_TAG, "got home timeline");
                //iterate through new status updates
                for (twitter4j.Status statusUpdate : homeTimeline) {
                    //call the getValues method of the data helper class, passing the new updates
                    Log.e(LOG_TAG, "looping...");
                    ContentValues timelineValues = DataHelper.getValues(statusUpdate);
                    //if the database already contains the updates they will not be inserted
                    niceDB.insertOrThrow("home", null, timelineValues);
                    //confirm we have new updates
                    statusChanges = true;
                }
            } catch (Exception te) {
                Log.e(LOG_TAG, "Exception: " + te);
            }

            //if we have new updates, send a Broadcast
            if (statusChanges) {
                //this should be received in the main timeline class
                sendBroadcast(new Intent("TWITTER_UPDATES"));
            }
            return null;
        }

        protected void onPostExecute(Void accToken) {

        }
    }


}
