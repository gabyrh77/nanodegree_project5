package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

import com.example.xyzreader.remote.RemoteEndpointUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_NO_NETWORK, STATUS_ERROR, STATUS_OK})
    public @interface ServiceStatus {}
    public static final int STATUS_NO_NETWORK = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_OK = 3;
    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_STATE
            = "com.example.xyzreader.intent.extra.STATE";

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            sendStatusBroadcast(STATUS_NO_NETWORK);
            return;
        }

        List<ContentValues> mValueList = new ArrayList<>();

        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                Log.e(TAG, "Error retrieving new content.");
                sendStatusBroadcast(STATUS_ERROR);
                return;
            }

            for (int i = 0; i < array.length(); i++) {
                ContentValues values = new ContentValues();
                JSONObject object = array.getJSONObject(i);
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id"));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author"));
                values.put(ItemsContract.Items.TITLE, object.getString("title"));
                values.put(ItemsContract.Items.BODY, object.getString("body"));
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb"));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo"));
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio"));
                time.parse3339(object.getString("published_date"));
                values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));
                mValueList.add(values);
            }

            ContentValues[] bulkToInsert = new ContentValues[mValueList.size()];
            mValueList.toArray(bulkToInsert);
            getContentResolver().bulkInsert(ItemsContract.Items.buildDirUri(), bulkToInsert);

        } catch (JSONException | SQLiteException e) {
            Log.e(TAG, "Error updating content.", e);
            sendStatusBroadcast(STATUS_ERROR);
            return;
        }

        sendStatusBroadcast(STATUS_OK);
    }

    private void sendStatusBroadcast(@ServiceStatus int status){
        Log.d(TAG, "sending broadcast status: " + String.valueOf(status));
        Intent messageIntent = new Intent(BROADCAST_ACTION_STATE_CHANGE);
        messageIntent.putExtra(EXTRA_STATE, status);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
    }
}
