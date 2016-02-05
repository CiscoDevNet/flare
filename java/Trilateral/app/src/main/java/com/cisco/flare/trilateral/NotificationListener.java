package com.cisco.flare.trilateral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.cisco.flare.trilateral.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Date;

public class NotificationListener extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "M-NotificationListener";
    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating NotificationListener");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        registerReceiver(sendMessageReceiver, new IntentFilter(Constants.SEND_MESSAGE_INTENT));
    }

    private BroadcastReceiver sendMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String value = intent.getExtras().getString(Constants.MESSAGE_VALUE_KEY);
            String type = intent.getExtras().getString(Constants.MESSAGE_TYPE_KEY);
            sendMessageToWearable(value, type);
        }
    };

    private void sendMessageToWearable(String message, String type) {
        // Log.d(TAG, "sendMessageToWearable: " + type + ": " + message);
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.MESSAGE_MOBILE_TO_WEARABLE_PATH);
            putDataMapRequest.getDataMap().putString(Constants.KEY_TEXT_FROM_SERVER, message);
            putDataMapRequest.getDataMap().putLong(Constants.KEY_TIMESTAMP, new Date().getTime());
            putDataMapRequest.getDataMap().putString(Constants.KEY_MESSAGE_TYPE, type);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "buildWatchOnlyNotification(): Failed to set the data, "
                                        + "status: " + dataItemResult.getStatus().getStatusCode());
                            } else {
                                // Log.d(TAG, "message sent successfully!");
                            }
                        }
                    });
        } else {
            Log.e(TAG, "sendMessageToWearable(): no Google API Client connection");
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String message = dataMap.getString(Constants.KEY_TEXT_FROM_SERVER);
                String type = dataMap.getString(Constants.KEY_MESSAGE_TYPE);
                String path = dataEvent.getDataItem().getUri().getPath();

                if (Constants.MESSAGE_WEARABLE_TO_MOBILE_PATH.equals(path)) {
                    sendMessageToActivity(message, type);
                }
            } else if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "Message deleted");
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DataItem deleted: " + dataEvent.getDataItem().getUri().getPath());
                }
                if (Constants.MESSAGE_WEARABLE_TO_MOBILE_PATH.equals(dataEvent.getDataItem().getUri().getPath())) {

                }
            }
        }
    }

    private void sendMessageToActivity(String message, String type) {
        Intent intent = new Intent(Constants.RECEIVE_MESSAGE_INTENT);
        Log.d(TAG, "sendMessageToActivity " + type + ": " + message);
        intent.putExtra(Constants.MESSAGE_TYPE_KEY, type);
        intent.putExtra(Constants.MESSAGE_VALUE_KEY, message);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        Toast.makeText(this, "Flare service starting", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(sendMessageReceiver);
        mGoogleApiClient.disconnect();
        Toast.makeText(this, "Flare service stopped", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override // ConnectionCallbacks
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        sendMessageToWearable("from mobile", "hello");

    }

    @Override // ConnectionCallbacks
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override // OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed");
        Log.e(TAG, "Failed to connect to the Google API client");
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // The Android Wear app is not installed
            Log.e(TAG, "The Android Wear app is not installed");
        }
    }
}
