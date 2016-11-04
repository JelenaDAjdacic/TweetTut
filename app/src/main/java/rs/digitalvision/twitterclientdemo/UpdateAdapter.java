package rs.digitalvision.twitterclientdemo;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import java.net.URL;

/**
 * Created by 1 on 11/4/2016.
 */

public class UpdateAdapter extends SimpleCursorAdapter {
    /**
     * twitter developer key
     */
    public final static String TWIT_KEY = "jJC29MJuuCfJKFVrvn0sxegxs";//alter
    /**
     * twitter developer secret
     */
    public final static String TWIT_SECRET = "PpCm9xQvvv00xCn3kvTmKz6KoHtYU01RohhYPFy8XW1TlbF6l0";//alter

    /**
     * strings representing database column names to map to views
     */
    static final String[] from = {"update_text", "user_screen",
            "update_time", "user_img"};
    /**
     * view item IDs for mapping database record values to
     */
    static final int[] to = {R.id.updateText, R.id.userScreen,
            R.id.updateTime, R.id.userImg};

    private String LOG_TAG = "UpdateAdapter";

    private Context mContext;

    /**
     * constructor sets up adapter, passing 'from' data and 'to' views
     *
     * @param context
     * @param c
     */
    public UpdateAdapter(Context context, Cursor c) {
        super(context, R.layout.update, c, from, to);
        mContext = context;
    }

    /*
 * Bind the data to the visible views
 */
    @Override
    public void bindView(View row, Context context, Cursor cursor) {
        super.bindView(row, context, cursor);

        try {
            //get profile image
            URL profileURL =
                    new URL(cursor.getString(cursor.getColumnIndex("user_img")));

            //set the image in the view for the current tweet
            NetworkImageView profPic = (NetworkImageView) row.findViewById(R.id.userImg);
            profPic.setImageUrl(cursor.getString(cursor.getColumnIndex("user_img")), VolleySingleton.getsInstance(mContext).getImageLoader());
        } catch (Exception te) {
            Log.e(LOG_TAG, te.getMessage());
        }

        //get the update time
        long createdAt = cursor.getLong(cursor.getColumnIndex("update_time"));
        //get the update time view
        TextView textCreatedAt = (TextView) row.findViewById(R.id.updateTime);
        //adjust the way the time is displayed to make it human-readable
        textCreatedAt.setText(DateUtils.getRelativeTimeSpanString(createdAt) + " ");
    }


}