package com.cisco.flare.trilateral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cisco.flare.CompassManager;
import com.cisco.flare.Device;
import com.cisco.flare.Environment;
import com.cisco.flare.Flare;
import com.cisco.flare.FlareBeaconManager;
import com.cisco.flare.Thing;
import com.cisco.flare.Zone;
import com.cisco.flare.trilateral.common.Constants;
import com.cisco.flare.trilateral.common.ScreenDensity;

import org.altbeacon.beacon.BeaconConsumer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

public class WearActivity extends WearableActivity implements CompassManager.Delegate, BeaconConsumer {

	private CompassView compassView = null;

	public Environment environment;
	public Device device;
	public Zone currentZone;
	public Thing nearbyThing;
	public String color;

	private CompassManager compassManager;

	private LinearLayout environmentLayout;
	private TextView environmentTextView;
	private TextView zoneTextView;
	private TextView positionTextView;
	private TextView angleTextView;

	private LinearLayout thingLayout;
	private Button thingCloseButton;
	private TextView nearbyThingTextView;
	private TextView colorTextView;
	private TextView descriptionTextView;
	private Button previousButton;
	private Button nextButton;

	public PointF currentPosition = new PointF(0,0);
	public double currentAngle = 0.0;

	private static String TAG = "WearActivity";

	private GestureDetectorCompat gestureDetector;
	private DismissOverlayView dismissOverlayView;

	private boolean isEmulator = Build.HARDWARE.contains("goldfish");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wear);
		setAmbientEnabled();

		compassView = (CompassView) findViewById(R.id.fullscreen_view);

		environmentLayout = (LinearLayout) findViewById(R.id.environmentLayout);
		environmentTextView = (TextView) findViewById(R.id.environment);
		zoneTextView = (TextView) findViewById(R.id.zone);
		positionTextView = (TextView) findViewById(R.id.position);
		angleTextView = (TextView) findViewById(R.id.angle);

		thingLayout = (LinearLayout) findViewById(R.id.thingLayout);
		thingCloseButton = (Button) findViewById(R.id.thingCloseButton);
		thingLayout.setVisibility(View.INVISIBLE);
		nearbyThingTextView = (TextView) findViewById(R.id.nearbyThing);
		descriptionTextView = (TextView) findViewById(R.id.thingDescription);
		colorTextView = (TextView) findViewById(R.id.thingColor);
		previousButton = (Button) findViewById(R.id.previousButton);
		nextButton = (Button) findViewById(R.id.nextButton);

		thingCloseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setNearbyThing(null);
			}
		});
		previousButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				previousColor();
			}
		});
		nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				nextColor();
			}
		});

		compassManager = new CompassManager((SensorManager)getSystemService(SENSOR_SERVICE));
		compassManager.setDelegate(this);

		setupBeaconManager();

		ScreenDensity.getInstance().setScale(getApplicationContext());

		dismissOverlayView = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
		gestureDetector = new GestureDetectorCompat(this, new LongPressListener());

		if (isEmulator) {
			receiveEnvironment("{\"environment\":{\"name\":\"Cisco Live Berlin\",\"angle\":29,\"_id\":\"5645fa4c0f1b508cb3e53979\",\"data\":{\"uuid\":\"2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6\",\"cool\":\"yah!\"},\"zones\":[{\"description\":\"9th floor\",\"things\":[{\"description\":\"sofa with footstool\",\"_id\":\"5645fa4c0f1b508cb3e5397d\",\"data\":{\"price\":510,\"size\":{\"width\":955,\"height\":500},\"sku\":\"298.937.59\",\"designer\":\"IKEA of Sweden\",\"options\":[\"white\",\"red\",\"orange\",\"green\",\"blue\"],\"color\":\"red\",\"location\":{\"section\":17,\"aisle\":2},\"dimensions\":{\"width\":217,\"depth\":76,\"height\":66},\"quantity\":{\"red\":12,\"white\":4,\"orange\":16,\"blue\":9,\"green\":5},\"url\":\"http://www.ikea.com/us/en/catalog/products/S29893759/\",\"minor\":2},\"position\":{\"y\":10,\"x\":0},\"name\":\"Arholma\"},{\"description\":\"sofa\",\"_id\":\"5645fa4c0f1b508cb3e5397c\",\"data\":{\"price\":350,\"size\":{\"width\":955,\"height\":500},\"on\":true,\"designer\":\"Cisc&oslash; of Chandlers Fj&ouml;rd\",\"brightness\":1,\"color\":\"yellow\",\"location\":{\"section\":20,\"aisle\":2},\"dimensions\":{\"width\":205,\"depth\":93,\"height\":80},\"sku\":\"098.405.35\",\"options\":[\"gray\",\"lightgray\",\"black\",\"yellow\",\"red\"],\"quantity\":{\"red\":12,\"black\":7,\"lightgray\":10,\"gray\":5,\"yellow\":3},\"url\":\"http://www.ikea.com/us/en/catalog/products/S09840535/\",\"minor\":1},\"position\":{\"y\":0,\"x\":0},\"name\":\"Coombe\"},{\"description\":\"loveseat with chaise\",\"_id\":\"5645fa4c0f1b508cb3e5397e\",\"data\":{\"size\":{\"width\":1050,\"height\":500},\"price\":699,\"on\":true,\"designer\":\"Carl &Ouml;jerstam\",\"brightness\":1,\"color\":\"green\",\"location\":{\"section\":18,\"aisle\":2},\"dimensions\":{\"width\":157,\"depth\":123,\"height\":77},\"sku\":\"602.991.96\",\"options\":[\"red\",\"green\",\"lightblue\",\"purple\"],\"quantity\":{\"lightblue\":6,\"red\":5,\"purple\":22,\"green\":18},\"url\":\"http://www.ikea.com/us/en/catalog/products/60299196/\",\"minor\":3},\"position\":{\"y\":10,\"x\":10},\"name\":\"Dagarn\"},{\"description\":\"sofa bed\",\"_id\":\"5645fa4c0f1b508cb3e5397f\",\"data\":{\"price\":449,\"size\":{\"width\":875,\"height\":500},\"on\":true,\"designer\":\"IKEA of Sweden\",\"brightness\":0.8,\"color\":\"purple\",\"location\":{\"section\":19,\"aisle\":2},\"dimensions\":{\"width\":162,\"depth\":68,\"height\":60},\"sku\":\"902.932.30\",\"options\":[\"red\",\"salmon\",\"orange\",\"teal\",\"purple\"],\"quantity\":{\"salmon\":10,\"teal\":3,\"red\":5,\"orange\":7,\"purple\":12},\"url\":\"http://www.ikea.com/us/en/catalog/products/90293230/\",\"minor\":4},\"position\":{\"y\":0,\"x\":10},\"name\":\"Erska\"},{\"description\":\"shelf unit\",\"_id\":\"5645fa4c0f1b508cb3e53980\",\"data\":{\"price\":139,\"size\":{\"width\":500,\"height\":500},\"sku\":\"102.651.08\",\"designer\":\"Tord Bj&ouml;rklund\",\"options\":[\"white\",\"oak\",\"black\",\"birch\"],\"color\":\"birch\",\"location\":{\"section\":8,\"aisle\":14},\"dimensions\":{\"width\":147,\"depth\":40,\"height\":147},\"quantity\":{\"oak\":7,\"black\":3,\"white\":10,\"birch\":0},\"url\":\"http://www.ikea.com/us/en/catalog/products/10265108/\",\"minor\":5},\"position\":{\"y\":5,\"x\":0},\"name\":\"Kallax\"},{\"description\":\"sofa\",\"_id\":\"5645fa4c0f1b508cb3e53981\",\"data\":{\"price\":350,\"size\":{\"width\":955,\"height\":500},\"sku\":\"098.405.35\",\"designer\":\"Tord Bj&ouml;rklund\",\"options\":[\"white\",\"magenta\",\"orange\",\"yellow\",\"turquoise\"],\"color\":\"magenta\",\"location\":{\"section\":20,\"aisle\":2},\"dimensions\":{\"width\":205,\"depth\":93,\"height\":80},\"quantity\":{\"magenta\":0,\"white\":17,\"turquoise\":12,\"orange\":5,\"yellow\":8},\"url\":\"http://www.ikea.com/us/en/catalog/products/S09840535/\",\"minor\":6},\"position\":{\"y\":5,\"x\":10},\"name\":\"Karlstad\"},{\"description\":\"sofa\",\"_id\":\"5645fa4c0f1b508cb3e53982\",\"data\":{\"price\":299,\"size\":{\"width\":875,\"height\":500},\"sku\":\"802.789.23\",\"designer\":\"IKEA of Sweden\",\"options\":[\"black\",\"gray\",\"red\",\"green\",\"teal\"],\"color\":\"red\",\"location\":{\"section\":21,\"aisle\":2},\"dimensions\":{\"width\":165,\"depth\":89,\"height\":94},\"quantity\":{\"teal\":4,\"red\":7,\"black\":11,\"gray\":0,\"green\":15},\"url\":\"http://www.ikea.com/us/en/catalog/products/80278923/\",\"minor\":7},\"position\":{\"y\":0,\"x\":5},\"name\":\"Knislinge\"},{\"description\":\"loveseat\",\"_id\":\"56a68870f2cc40ab0e2bc378\",\"data\":{\"price\":99,\"size\":{\"width\":750,\"height\":500},\"sku\":\"102.651.08\",\"designer\":\"Nike Karlsson\",\"options\":[\"pink\",\"yellow\",\"green\",\"blue\"],\"color\":\"pink\",\"location\":{\"section\":9,\"aisle\":3},\"dimensions\":{\"width\":119,\"depth\":76,\"height\":69},\"quantity\":{\"green\":3,\"pink\":10,\"blue\":12,\"yellow\":7},\"url\":\"http://www.ikea.com/us/en/catalog/products/10265108/\",\"minor\":8},\"position\":{\"y\":10,\"x\":5},\"name\":\"Knopparp\"}],\"_id\":\"5645fa4c0f1b508cb3e5397a\",\"data\":{\"major\":1979},\"perimeter\":{\"origin\":{\"y\":-0.5,\"x\":-0.5},\"size\":{\"width\":11,\"height\":11}},\"name\":\"DevNet Zone\"}],\"perimeter\":{\"origin\":{\"y\":-1,\"x\":-1},\"size\":{\"width\":12,\"height\":12}},\"description\":\"Berlin, Germany\",\"geofence\":{\"radius\":2000,\"longitude\":-77.044207,\"latitude\":38.93444}},\"device\":{\"description\":\"Moto 360, Android 5.0\",\"_id\":\"56afcbcd3f1a7741645b3c8f\",\"data\":{\"angle\":20,\"mac\":\"02:00:00:00:00:00\"},\"position\":{\"y\":7.21,\"x\":4.39},\"name\":\"Andrew's Watch\",\"zoneId\":\"5645fa4c0f1b508cb3e5397a\",\"nearbyThingId\":\"5645fa4c0f1b508cb3e5397e\"}}");
		}
	}

	@Override
	public void onEnterAmbient(Bundle ambientDetails) {
		super.onEnterAmbient(ambientDetails);
		updateDisplay();
	}

	@Override
	public void onUpdateAmbient() {
		super.onUpdateAmbient();
		updateDisplay();
	}

	@Override
	public void onExitAmbient() {
		updateDisplay();
		super.onExitAmbient();
	}

	private void updateDisplay() {
		if (isAmbient()) {
			// mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
			// mTextView.setTextColor(getResources().getColor(android.R.color.white));
		} else {
			// mContainerView.setBackground(null);
			// mTextView.setTextColor(getResources().getColor(android.R.color.black));
		}
	}

	@Override
	protected void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();

		startService(new Intent(this, NotificationListener.class));
		registerReceiver(messageReceiver, new IntentFilter(Constants.RECEIVE_MESSAGE_INTENT));
	}

	@Override
	protected void onRestart() {
		Log.i(TAG, "onRestart");
		super.onRestart();  // Always call the superclass method first
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();

		FlareBeaconManager.resume();
		compassManager.resume();
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();

		FlareBeaconManager.pause();
		compassManager.pause();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();

		unregisterReceiver(messageReceiver);
		stopService(new Intent(this, NotificationListener.class));
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();

		FlareBeaconManager.unbind();

		gestureDetector = null;
		dismissOverlayView = null;
		((ThreadPoolExecutor)AsyncTask.THREAD_POOL_EXECUTOR).purge();
	}

	@Override
	public void onBeaconServiceConnect() {
		FlareBeaconManager.setRangeNotifier();
	}

	public void setupBeaconManager() {
		Log.d(TAG, "Setting up beacons.");
		FlareBeaconManager.setConsumer(this);
		FlareBeaconManager.setCallback(new FlareBeaconManager.Callback() {
			@Override
			public void onPositionUpdate(final PointF position) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						gotPosition(position);
					}
				});
			}
		});
		FlareBeaconManager.bind(this);
		FlareBeaconManager.useEddystone(true);
		FlareBeaconManager.useSquare(false);
		FlareBeaconManager.restartRangingBeacons();
	}

	public void gotPosition(PointF position) {
		currentPosition = position;
		updateInfo();
		compassView.setUserPosition(position);
		sendPositionAngleToMobile(currentPosition, currentAngle);
	}

	public void deviceAngleChanged(float angle) {
		currentAngle = angle;
		updateInfo();
		compassView.setGlobalAngle(angle);
		// sendPositionAngleToMobile(currentPosition, currentAngle); // TODO: don't need to send this twice every second
	}

	private void updateInfo() {
		environmentTextView.setText(environment != null ? environment.getName() : "--");
		zoneTextView.setText(currentZone != null ? currentZone.getName() : "none");
		positionTextView.setText(String.format("%.2f, %.2f", currentPosition.x, currentPosition.y));
		angleTextView.setText("" + currentAngle + "°");
		nearbyThingTextView.setText(nearbyThing != null ? nearbyThing.getName() : "none");
		descriptionTextView.setText(nearbyThing != null ? nearbyThing.getDescription() : "none");
		colorTextView.setText(color != null ? color : "none");
	}

	private void sendMessageToMobile(JSONObject json, String type) {
		sendMessageToMobile(json.toString(), type);
	}

	private void sendMessageToMobile(String message, String type) {
		Log.d(TAG, "Sending " + type + " to wearable: " + message);
		Intent intent = new Intent(Constants.SEND_MESSAGE_INTENT);
		intent.putExtra(Constants.MESSAGE_VALUE_KEY, message);
		intent.putExtra(Constants.MESSAGE_TYPE_KEY, type);
		sendBroadcast(intent);
	}

	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String type = intent.getExtras().getString(Constants.MESSAGE_TYPE_KEY);
			String message = intent.getExtras().getString(Constants.MESSAGE_VALUE_KEY);
			Log.d(TAG, "Received " + type + ": " + message);

			if (type.equals(Constants.TYPE_HELLO)) {
				Log.d(TAG, "Got hello.");
				sendMessageToMobile("from wearable", "hello back");
			} else if (type.equals(Constants.TYPE_POSITION)) {
				receivePosition(message);
			} else if (type.equals(Constants.TYPE_ENVIRONMENT)) {
				receiveEnvironment(message);
			} else if (type.equals(Constants.TYPE_ENTER_ZONE)) {
				enter(message);
			} else if (type.equals(Constants.TYPE_EXIT_ZONE)) {
				exit(message);
			} else if (type.equals(Constants.TYPE_NEAR_THING)) {
				near(message);
			} else if (type.equals(Constants.TYPE_FAR_THING)) {
				far(message);
			} else if (type.equals(Constants.TYPE_DATA)) {
				receiveData(message);
			} else {
				Log.w(TAG, "Message type unknown: " + type);
			}
		}
	};

	private void receiveEnvironment(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);
			JSONObject deviceJson = null;

			try {
				environment = new Environment(json.getJSONObject("environment"));
				Log.d(TAG, "Got environment: " + environment.getName());

				FlareBeaconManager.setEnvironment(environment);

				if (compassView != null) {
					compassView.setEnvAngle(environment.getAngle());
				}
			} catch (Exception e) {}

			try {
				deviceJson = json.getJSONObject("device");
				device = new Device(deviceJson);
				compassView.setUserPosition(device.getPosition());
				Log.d(TAG, "Got device: " + device.getName());
			} catch (Exception e) {}

			// will call compassView.replaceObjects() with things in the zone
			try { setCurrentZoneId(deviceJson != null ? deviceJson.getString("zoneId") : null); } catch (Exception e) {}
			try { setNearbyThingId(deviceJson != null ? deviceJson.getString("nearbyThingId") : null); } catch (Exception e) {}

			updateInfo();
		} catch (Exception e) {
			Log.e(TAG, "Couldn't parse environment: " + jsonString, e);
		}
	}

	private void setCurrentZoneId(String zoneId) {
		if (zoneId != null) {
			currentZone = environment.getZoneWithId(zoneId);
			compassView.replaceObjects(currentZone.getThings());
			if (currentZone != null) {
				Log.d(TAG, "Got zone: " + currentZone.getName());
			} else {
				Log.d(TAG, "Couldn't find zone with ID: " + zoneId);
			}
		}
	}

	private void setNearbyThingId(String nearbyThingId) {
		if (nearbyThingId != null) {
			Thing thing = environment.getThingWithId(nearbyThingId);
			if (thing != null) {
				Log.d(TAG, "Got nearby thing: " + thing.getName());

				setNearbyThing(thing);
			} else {
				Log.d(TAG, "Couldn't find thing with ID: " + nearbyThingId);
			}
		}
	}

	public void setNearbyThing(Thing value) {
		this.nearbyThing = value;

		environmentLayout.setVisibility(nearbyThing == null ? View.VISIBLE : View.INVISIBLE);
		thingLayout.setVisibility(nearbyThing != null ? View.VISIBLE : View.INVISIBLE);
		compassView.selectThing(nearbyThing);

		try {
			color = nearbyThing.getData().getString("color");
		} catch (Exception e) {
			color = null;
		}

		updateInfo();
	}

	private void sendPositionAngleToMobile(PointF position, double angle) {
		String message = String.format("{\"position\":{\"x\":%.2f,\"y\":%.2f},\"angle\":%.2f}", position.x, position.y, angle);
		sendMessageToMobile(message, Constants.TYPE_POSITION_ANGLE);
	}

	private void receivePosition(String jsonString) {
		try {
			JSONObject positionJson = new JSONObject(jsonString);
			currentPosition = Flare.getPoint(positionJson);

			updateInfo();

			if (compassView != null) {
				compassView.setUserPosition(currentPosition);
			}
		} catch (Exception e) {
			Log.e(TAG, "Couldn't parse position: " + jsonString, e);
		}
	}

	private void enter(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);
			String nearbyThingId = json.getString("thing");
			setNearbyThingId(nearbyThingId);
			updateInfo();
		} catch (Exception e) {
			Log.e(TAG, "Couldn't parse position: " + jsonString, e);
		}
	}

	private void exit(String jsonString) {
		currentZone = null;
		updateInfo();
	}

	private void near(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);
			String nearbyThingId = json.getString("thing");
			setNearbyThingId(nearbyThingId);
			updateInfo();
		} catch (Exception e) {
			Log.e(TAG, "Couldn't parse position: " + jsonString, e);
		}
	}

	private void far(String jsonString) {
		// setNearbyThing(null);
		// updateInfo();
	}

	private void receiveData(String jsonString) {
		try {
			JSONObject message = new JSONObject(jsonString);
			String thingId = message.getString("thing");
			Thing thing = environment.getThingWithId(thingId);
			JSONObject data = message.getJSONObject("data");
			String key = (String) data.names().get(0);

			if (thing != null && key.equals("color")) {
				try {
					color = data.getString(key);
					thing.setColor(color);
					compassView.updateColor(thing);
					updateInfo();
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Couldn't parse data: " + jsonString, e);
		}
	}

	private void sendAction(String action) {
		if (nearbyThing != null) {
			JSONObject message = new JSONObject();
			try {
				message.put("thing", nearbyThing.getId());
				message.put("action", action);
				sendMessageToMobile(message, Constants.TYPE_ACTION);
			} catch (Exception e) {

			}
		}
	}

	// TODO: update display immediately?
	private void previousColor() {
		Log.d(TAG, "previousColor");
		sendAction("previousColor");
	}

	private void nextColor() {
		Log.d(TAG, "nextColor");
		sendAction("nextColor");
	}

	// CommonDrawingActivity code

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
	}

	private class LongPressListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public void onLongPress(MotionEvent event) {
			dismissOverlayView.show();
		}
	}
}

