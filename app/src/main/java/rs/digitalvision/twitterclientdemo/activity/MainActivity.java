package rs.digitalvision.twitterclientdemo.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import rs.digitalvision.twitterclientdemo.R;
import rs.digitalvision.twitterclientdemo.adapter.UpdateAdapter;
import rs.digitalvision.twitterclientdemo.db.DataHelper;
import rs.digitalvision.twitterclientdemo.service.TimelineService;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = "MainActivity";
    /** these has to be obfuscated before release */
    /**
     * developer account key for this app
     */
    public final static String TWITTER_KEY = "jJC29MJuuCfJKFVrvn0sxegxs";
    /**
     * developer secret for the app
     */
    public final static String TWITTER_SECRET = "PpCm9xQvvv00xCn3kvTmKz6KoHtYU01RohhYPFy8XW1TlbF6l0";
    /**
     * app url
     */
    public final static String TWITTER_URL = "tnice-android:///";

    /**
     * Twitter instance
     */
    private Twitter twitter;
    /**
     * request token for accessing user account
     */
    private RequestToken requestToken;

    private AccessToken accessToken;
    /**
     * shared preferences to store user details
     */
    private SharedPreferences prefs;

    Button signIn;
    /**
     * main view for the home timeline
     */
    private ListView homeTimeline;
    /**
     * database helper for update data
     */
    private DataHelper timelineHelper;
    /**
     * update database
     */
    private SQLiteDatabase timelineDB;
    /**
     * cursor for handling data
     */
    private Cursor timelineCursor;
    /**
     * adapter for mapping data
     */
    private UpdateAdapter timelineAdapter;

    /**
     * Broadcast receiver for when new updates are available
     */
    private BroadcastReceiver niceStatusReceiver;

    private SwipeRefreshLayout mSwipeRefreshLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get the preferences for the app
        prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        //find out if the user preferences are set
        if (prefs.getString("user_token", null) == null) {

            //no user preferences so prompt to sign in
            setContentView(R.layout.activity_main);

            signIn = (Button) findViewById(R.id.signin);
            signIn.setOnClickListener(this);
            signIn.setEnabled(false);


            //get a twitter instance for authentication
            twitter = TwitterFactory.getSingleton();
            //pass developer key and secret
            twitter.setOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);

            //try to get request token
            new RetrieveRequestToken().execute();

        } else {

            setupTimeline();
        }

    }

    public void onClick(View v) {
        //find view
        switch (v.getId()) {
            //sign in button pressed
            case R.id.signin:
                //take user to twitter authentication web page to allow app access to their twitter account
                String authURL = requestToken.getAuthenticationURL();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authURL)));
                break;
            //other listeners here

            default:
                break;
        }
    }

    /*
 * onNewIntent fires when user returns from Twitter authentication Web page
 */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //get the retrieved data
        Uri twitURI = intent.getData();
        //make sure the url is correct
        if (twitURI != null && twitURI.toString().startsWith(TWITTER_URL)) {
            //is verifcation - get the returned data
            String oaVerifier = twitURI.getQueryParameter("oauth_verifier");

            //attempt to retrieve access token
            new GetAccessToken().execute(oaVerifier);

        }
    }

    private void setupTimeline() {
        Log.v(TAG, "setting up timeline");
        setContentView(R.layout.timeline);
        try {
            //get the timeline
            //get reference to the list view
            homeTimeline = (ListView) findViewById(R.id.homeList);
            //instantiate database helper
            timelineHelper = new DataHelper(this);

            //get the database
            timelineDB = timelineHelper.getReadableDatabase();

            //query the database, most recent tweets first
            timelineCursor = timelineDB.query
                    ("home", null, null, null, null, null, "update_time DESC");

            //manage the updates using a cursor
            startManagingCursor(timelineCursor);
            //instantiate adapter
            timelineAdapter = new UpdateAdapter(this, timelineCursor);

            //this will make the app populate the new update data in the timeline view
            homeTimeline.setAdapter(timelineAdapter);
            //instantiate receiver class for finding out when new updates are available
            niceStatusReceiver = new TwitterUpdateReceiver();
            //register for updates
            registerReceiver(niceStatusReceiver, new IntentFilter("TWITTER_UPDATES"));

            //start the Service for updates now
            this.getApplicationContext().startService(new Intent(this.getApplicationContext(), TimelineService.class));

        } catch (Exception te) {
            Log.e(TAG, "Failed to fetch timeline: " + te.getMessage());
        }
    }

    class RetrieveRequestToken extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... urls) {
            //try to get request token
            try {
                //get authentication request token
                requestToken = twitter.getOAuthRequestToken(TWITTER_URL);

            } catch (TwitterException te) {
                Log.e(TAG, "TE " + te.getMessage());
            }
            return null;
        }

        protected void onPostExecute(Void a) {
            Log.d("TAG", "Retrieved token");
            signIn.setEnabled(true);
        }
    }

    class GetAccessToken extends AsyncTask<String, Void, AccessToken> {

        protected AccessToken doInBackground(String... str) {
            AccessToken accToken;
            try {
                //try to get an access token using the returned data from the verification page
                accToken = twitter.getOAuthAccessToken(requestToken, str[0]);


            } catch (TwitterException te) {
                Log.e(TAG, "Failed to get access token: " + te.getMessage());
                return null;
            }
            return accToken;
        }

        protected void onPostExecute(AccessToken accToken) {
            Log.d("TAG", "Got acces token");
            Log.d("TAG", "Saving token...");
            //add the token and secret to shared prefs for future reference
            prefs.edit()
                    .putString("user_token", accToken.getToken())
                    .putString("user_secret", accToken.getTokenSecret())
                    .apply();

            //display the timeline
            setupTimeline();
        }
    }

    /**
     * Class to implement Broadcast receipt for new updates
     */
    class TwitterUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int rowLimit = 100;
            if (DatabaseUtils.queryNumEntries(timelineDB, "home") > rowLimit) {
                String deleteQuery = "DELETE FROM home WHERE " + BaseColumns._ID + " NOT IN " +
                        "(SELECT " + BaseColumns._ID + " FROM home ORDER BY " + "update_time DESC " +
                        "limit " + rowLimit + ")";
                timelineDB.execSQL(deleteQuery);
            }

            timelineCursor = timelineDB.query("home", null, null, null, null, null, "update_time DESC");
            startManagingCursor(timelineCursor);
            timelineAdapter = new UpdateAdapter(context, timelineCursor);
            homeTimeline.setAdapter(timelineAdapter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            //stop the updater Service
            stopService(new Intent(this, TimelineService.class));
            //remove receiver register
            unregisterReceiver(niceStatusReceiver);
            //close the database
            timelineDB.close();
        } catch (Exception se) {
            Log.e(TAG, "unable to stop Service or receiver");
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.sign_out:

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
