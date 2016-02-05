package com.cisco.flare.trilateral;


import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

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

import com.cisco.flare.trilateral.common.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class NotificationListener extends WearableListenerService
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final String TAG = "W-NotificationListener";
	private GoogleApiClient googleApiClient;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Creating NotificationListener");
		googleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Wearable.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();

		registerReceiver(sendMessageReceiver, new IntentFilter(Constants.SEND_MESSAGE_INTENT));
	}

	private BroadcastReceiver sendMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String value = intent.getExtras().getString(Constants.MESSAGE_VALUE_KEY);
			String type = intent.getExtras().getString(Constants.MESSAGE_TYPE_KEY);
			sendMessageToMobile(value, type);
		}
	};

	private void sendMessageToActivity(String message, String type) {
		// Log.d(TAG, "sendMessageToActivity " + type + ": " + message);
		Intent intent = new Intent(Constants.RECEIVE_MESSAGE_INTENT);
		intent.putExtra(Constants.MESSAGE_TYPE_KEY, type);
		intent.putExtra(Constants.MESSAGE_VALUE_KEY, message);
		sendBroadcast(intent);
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {
		// Log.d(TAG, "onDataChanged");
		for (DataEvent dataEvent : dataEvents) {
			if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
//                Log.v(TAG, "  type CHANGED");
				DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
				String msg = dataMap.getString(Constants.KEY_TEXT_FROM_SERVER);
				String msgType = dataMap.getString(Constants.KEY_MESSAGE_TYPE);
//                Log.i(TAG, "message: "+msg);
				if (Constants.MESSAGE_MOBILE_TO_WEARABLE_PATH.equals(dataEvent.getDataItem().getUri().getPath())) {
					sendMessageToActivity(msg, msgType);
				}
			}
			else if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
				Log.d(TAG, "  type DELETED");
				if (Log.isLoggable(TAG, Log.DEBUG)) {
					Log.d(TAG, "DataItem deleted: " + dataEvent.getDataItem().getUri().getPath());
				}
				if (Constants.MESSAGE_MOBILE_TO_WEARABLE_PATH.equals(dataEvent.getDataItem().getUri().getPath())) {
					// Dismiss the corresponding notification
					Log.i(TAG, "dismiss text?");
//                    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
//                            .cancel(Constants.WATCH_ONLY_ID);
				}
			}
		}
	}

	private void sendMessageToMobile(String message, String type) {
		Log.d(TAG, "sendMessageToMobile(): wear->mobile " + type + ": " + message);
		if (googleApiClient.isConnected()) {
			PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.MESSAGE_WEARABLE_TO_MOBILE_PATH);
			putDataMapRequest.getDataMap().putString(Constants.KEY_MESSAGE_TYPE, type);
			putDataMapRequest.getDataMap().putString(Constants.KEY_TEXT_FROM_SERVER, message);
			putDataMapRequest.getDataMap().putLong(Constants.KEY_TIMESTAMP, new Date().getTime());
			PutDataRequest request = putDataMapRequest.asPutDataRequest();
			Wearable.DataApi.putDataItem(googleApiClient, request)
					.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
						@Override
						public void onResult(DataApi.DataItemResult dataItemResult) {
							if (!dataItemResult.getStatus().isSuccess()) {
								Log.e(TAG, "buildWatchOnlyNotification(): Failed to set the data, "
										+ "status: " + dataItemResult.getStatus().getStatusCode());
							}
							else {
								// Log.d(TAG, "message sent successfully!");
							}
						}
					});
		} else {
			Log.e(TAG, "sendMessageToMobile(): no Google API Client connection");
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		Toast.makeText(this, "Flare service starting", Toast.LENGTH_SHORT).show();
		googleApiClient.connect();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		unregisterReceiver(sendMessageReceiver);
		googleApiClient.disconnect();
		googleApiClient = null;
		Toast.makeText(this, "Flare service stopped", Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}

	@Override // ConnectionCallbacks
	public void onConnected(Bundle bundle) {
		Log.d(TAG, "onConnected");
		sendMessageToMobile("from wearable", Constants.TYPE_HELLO);

		// note: send 2 messages in a quick succession may result in the first message being lost
		// so we'll delay the second message
		// run after 100ms to avoid multiple intents at the same time
/*		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				getPrefs();
			}
		}, 100); */
	}

	@Override // ConnectionCallbacks
	public void onConnectionSuspended(int i) {
		Log.d(TAG, "onConnectionSuspended");
//        uiUpdateTextStatus(Constants.STATUS_NOT_CONNECTED);
	}

	@Override // OnConnectionFailedListener
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d(TAG, "onConnectionFailed");
		Log.e(TAG, "Failed to connect to the Google API client");
//        uiUpdateTextStatus(Constants.STATUS_NOT_CONNECTED);
	}
}
