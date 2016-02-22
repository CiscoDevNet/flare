package com.cisco.flare.trilateral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cisco.flare.CompassManager;
import com.cisco.flare.Device;
import com.cisco.flare.Environment;
import com.cisco.flare.Flare;
import com.cisco.flare.FlareBeaconManager;
import com.cisco.flare.FlareManager;
import com.cisco.flare.Thing;
import com.cisco.flare.Zone;
import com.cisco.flare.trilateral.common.Constants;
import com.cisco.flare.trilateral.common.HTMLColors;

import org.altbeacon.beacon.BeaconConsumer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FlareManager.Delegate, CompassManager.Delegate, BeaconConsumer {

	public static MainActivity mainActivity;
	private static final String TAG = "MainActivity";

	private SectionsPagerAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;
	public ArrayList<FlareFragment> flareFragments;

	private SharedPreferences prefs;
	private LocationManager locationManager;
	private Location currentLocation;
	private CompassManager compassManager;

	public FlareManager flareManager;
	public Environment environment;
	public Device mobileDevice;
	public Device watchDevice;

	private boolean useEddystone = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mainActivity = this;
		flareFragments = new ArrayList<FlareFragment>();

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		mViewPager = (ViewPager) findViewById(R.id.container);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
		tabLayout.setupWithViewPager(mViewPager);

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new MyLocationListener();
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 100, locationListener);
		} catch (SecurityException exception) {
			Log.d(TAG, "Location permission not available.");
		}

		registerReceiver(messageReceiver, new IntentFilter(Constants.RECEIVE_MESSAGE_INTENT));
		startService(new Intent(this, NotificationListener.class));

		setupBeaconManager();

		compassManager = new CompassManager((SensorManager)getSystemService(SENSOR_SERVICE));
		compassManager.setDelegate(this);

		setEnvironment(null);
		setMobileDevice(null);
		setWatchDevice(null);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener((SharedPreferences prefs, String key) -> {
			if (key.equals("pref_flare_host") || key.equals("pref_flare_port")) {
				load();
			}
		});

		load();
	}

	public void load() {
		String host = prefs.getString("pref_flare_host", "");
		int port = 1234;
		try {
			port = Integer.parseInt(prefs.getString("pref_flare_port", "1234"));
		} catch (Exception e) {
			Log.e(TAG, "Port could not be converted to int.", e);
		}

		flareManager = new FlareManager(host, port);
		flareManager.setActivity(this);
		flareManager.setDelegate(this);
		flareManager.connect();

		boolean useGPS = prefs.getBoolean("use_gps", true);
		currentLocation = useGPS ? getLastKnownLocation() : null;

		flareManager.loadEnvironments(currentLocation, (environments) -> {
			if (environments != null) {
				if (!environments.isEmpty()) {
					setEnvironment(environments.get(0));
				} else {
					flareManager.loadEnvironments(null, (allEnvironments) -> {
						if (allEnvironments != null && !allEnvironments.isEmpty()) {
							Log.d(TAG, "No nearby environment found, using first environment.");
							setEnvironment(allEnvironments.get(0));
						} else {
							Log.d(TAG, "No environments found.");
							showMessage("No environments found");
						}
					});
				}
			} else {
				Log.d(TAG, "Could not connect to server.");
				showMessage("Could not connect to server.");
			}
		});
	}

	public void showMessage(String message) {
		for (FlareFragment fragment : flareFragments) {
			fragment.showMessage(message);
		}
	}

	// called by setEnvironment()
	public void loadDevices() {
		flareManager.getCurrentDevice(environment.getId(), mobileDeviceTemplate(), "mobile", prefs, (newMobileDevice) -> {
			if (newMobileDevice != null) {
				setMobileDevice(newMobileDevice);
				flareManager.subscribe(mobileDevice);

				flareManager.getCurrentZone(environment.getId(), mobileDevice.getPosition(), (newZone) -> {
					mobileDevice.setCurrentZone(newZone);
					objectsChanged();
				});

				flareManager.getNearbyThing(environment.getId(), mobileDevice.getId(), (newNearbyThing) -> {
					mobileDevice.setNearbyThing(newNearbyThing);
					objectsChanged();
				});

				getMacAddress(mobileDevice);
			} else {
				Log.d(TAG, "Could not load mobile device.");
			}
		});

		flareManager.getCurrentDevice(environment.getId(), watchDeviceTemplate(), "watch", prefs, (newWatchDevice) -> {
			if (newWatchDevice != null) {
				setWatchDevice(newWatchDevice);
				flareManager.subscribe(watchDevice);

				if (watchDevice.getPosition() == null) newWatchDevice.setPosition(new PointF(0,0));

				flareManager.getCurrentZone(environment.getId(), watchDevice.getPosition(), (newZone) -> {
					watchDevice.setCurrentZone(newZone);

					flareManager.getNearbyThing(environment.getId(), watchDevice.getId(), (newNearbyThing) -> {
						watchDevice.setNearbyThing(newNearbyThing);

						sendEnvironmentToWearable();
					});
				});
			} else {
				Log.d(TAG, "Could not load watch device.");
			}
		});
	}

	public void getMacAddress(Device device) {
		if (device != null) {
			try {
				String mac = device.getData().getString("mac");
				if (mac == null || mac.equals("02:00:00:00:00:00")) {
					loadMacAddress(device);
				}
			} catch (Exception e) {
				loadMacAddress(device);
			}
		}
	}

	public void loadMacAddress(Device device) {
		flareManager.getMacAddress((String mac) -> {
			Log.d(TAG, "mac: " + mac);
			try { device.getData().put("mac", mac); } catch (Exception e) {}
			flareManager.setData(device, "mac", mac, device);
		});
	}

	public void setupBeaconManager() {
		FlareBeaconManager.setConsumer(this);
		FlareBeaconManager.setCallback((PointF position) -> {
			runOnUiThread(() -> {
				gotPosition(position, mobileDevice);
			});
		});
		FlareBeaconManager.bind(this);
		FlareBeaconManager.useEddystone(useEddystone);
		FlareBeaconManager.useSquare(true);
		FlareBeaconManager.restartRangingBeacons();
	}

	public void gotPosition(PointF position, Device device) {
		// Log.d(TAG, "" + device.getN + " position: " + position);
		if (device != null) {
			flareManager.setPosition(device, position, null);
			device.setPosition(position);
			for (FlareFragment fragment : flareFragments) {
				fragment.positionChanged();
			}
		}
	}

	public void deviceAngleChanged(float angle) {
		gotAngle(angle, mobileDevice);
	}

	public void gotAngle(float angle, Device device) {
		// Log.d(TAG, "Angle: " + angle);
		if (flareManager != null && device != null) {
			try {
				device.getData().put("angle", angle);
			} catch (Exception exception) {
				Log.d(TAG, "Couldn't save angle.");
			}
			flareManager.setData(device, "angle", angle, device);
			for (FlareFragment fragment : flareFragments) {
				fragment.angleChanged();
			}
		}
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment value) {
		this.environment = value;

		if (value != null) {
			loadDevices();

			flareManager.subscribe(environment, true);
			FlareBeaconManager.setEnvironment(environment);

			printEnvironment(value);
		}

		objectsChanged();
	}

	public Device getDevice() {
		// TODO should actually return the mobile or watch depending upon setting
		return getMobileDevice();
	}

	public Device getMobileDevice() {
		return mobileDevice;
	}

	public void setMobileDevice(Device value) {
		this.mobileDevice = value;
		objectsChanged();
	}

	public Device getWatchDevice() {
		return watchDevice;
	}

	public void setWatchDevice(Device value) {
		this.watchDevice = value;
		// objectsChanged();
	}

	public void printEnvironment(Environment environment) {
		Log.d(TAG, environment.toString());

		for (Zone zone : environment.getZones()) {
			Log.d(TAG, "  " + zone.toString());

			for (Thing thing : zone.getThings()) {
				Log.d(TAG, "    " + thing.toString());

				flareManager.subscribe(thing);
			}
		}

		for (Device device: environment.getDevices()) {
			Log.d(TAG, device.toString());
		}
	}

	// returns a JSON object with default values for a new device
	public JSONObject mobileDeviceTemplate() {
		String userName = fullName();
		if (userName != null) userName = userName.replaceAll(" .*", "");
		String deviceName = userName != null ? userName + "'s " + Build.MODEL : Build.MODEL;
		String brand = Build.BRAND;
		if (brand.equals("google")) brand = "Google";
		String description = brand + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE;
		JSONObject data = new JSONObject();

		// This doesn't work anymore as of Android 6
		// String macAddress = macAddress();
		// if (macAddress != null) try { data.put("mac", macAddress); } catch (Exception e) {}

		JSONObject template = new JSONObject();
		try { template.put("name", deviceName ); } catch (Exception e) {}
		try { template.put("description", description); } catch (Exception e) {}
		try { template.put("data", data); } catch (Exception e) {}
		try { template.put("position", new PointF(-1, -1)); } catch (Exception e) {}

		return template;
	}

	public JSONObject watchDeviceTemplate() {
		JSONObject template = mobileDeviceTemplate();

		String userName = fullName();
		if (userName != null) userName = userName.replaceAll(" .*", "");
		String deviceName = userName != null ? userName + "'s Watch" : "Android Watch";
		String description = "Moto 360, Android 5.0"; // could try to get from actual watch
		try { template.put("name", deviceName ); } catch (Exception e) {}
		try { template.put("description", description); } catch (Exception e) {}

		return template;
	}

	// returns the user's full name
	public String fullName() {
		String fullName = null;

		try {
			Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
			int count = c.getCount();
			String[] columnNames = c.getColumnNames();
			boolean b = c.moveToFirst();
			int pos = c.getPosition();
			if (count == 1 && pos == 0) {
				for (int j = 0; j < columnNames.length; j++) {
					String columnName = columnNames[j];
					String columnValue = c.getString(c.getColumnIndex(columnName));
					if (columnName.equals("display_name")) fullName = columnValue;
				}
			}
			c.close();
		} catch (SecurityException e) {
			Log.d(TAG, "App does not have contacts permissions.");
		}
		return fullName;
	}

	// this doesn't work anymore as of Android 6
	public String macAddress() {
		String macAddr = null;
		try {
			WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInf = wifiMan.getConnectionInfo();
			macAddr = wifiInf.getMacAddress().toLowerCase();
		} catch (Exception e) {
			Log.d(TAG, "Couldn't get MAC address.", e);
		}
		return macAddr;
	}

	public void changePosition(PointF position) {
		try {
			mobileDevice.setPosition(position);
			flareManager.setPosition(mobileDevice, position, mobileDevice);
			for (FlareFragment fragment : flareFragments) {
				fragment.positionChanged();
			}
		} catch (Exception e) {}
	}

	public void changeColor(String color) {
		try {
			Thing nearbyThing = mobileDevice.getNearbyThing();
			nearbyThing.getData().put("color", color);
			flareManager.setData(nearbyThing, "color", color, mobileDevice);

			// hack to forward the message to the watch, because data notifications aren't echoed to the sender
			if (nearbyThing == watchDevice.getNearbyThing()) {
				JSONObject data = new JSONObject();
				try { data.put("color", color); } catch (Exception e) {}
				sendMessageToWearable(data, Constants.TYPE_DATA);
			}
		} catch (Exception e) {}
	}

	public void changeBrightness(double brightness) {
		try {
			Thing nearbyThing = mobileDevice.getNearbyThing();
			nearbyThing.getData().put("brightness", brightness);
			flareManager.setData(nearbyThing, "brightness", brightness, mobileDevice);
		} catch (Exception e) {}
	}

	public void changeAngle(double angle) {
		try {
			mobileDevice.getData().put("angle", angle);
			flareManager.setData(mobileDevice, "angle", angle, mobileDevice);
			for (FlareFragment fragment : flareFragments) {
				fragment.angleChanged();
			}
		} catch (Exception e) {
		}
	}

	public void performAction(String action) {
		try {
			Thing nearbyThing = mobileDevice.getNearbyThing();
			flareManager.performAction(nearbyThing, action, mobileDevice);
		} catch (Exception e) {
			Log.e(TAG, "performAction", e);
		}
	}

	public void didReceiveData(Flare flare, JSONObject data, Flare sender) {
		Log.d(TAG, flare.getName() + " data: " + data.toString());
		try {
			String key = (String) data.names().get(0);
			if (key != null) {
				if ((flare == mobileDevice && key.equals("angle"))) {
					for (FlareFragment fragment : flareFragments) {
						fragment.angleChanged();
					}
				}

				if ((flare == mobileDevice.getNearbyThing() && (key.equals("color") || key.equals("brightness")))) {
					for (FlareFragment fragment : flareFragments) {
						fragment.dataChanged();
					}
				}

				// send all color updates to watch
				if (key.equals("color")) {
					try {
						JSONObject message = new JSONObject();
						message.put("thing", flare.getId());
						message.put("data", data);
						sendMessageToWearable(message, Constants.TYPE_DATA);
					} catch (Exception e) {}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void didReceivePosition(Flare flare, PointF oldPosition, PointF newPosition, Flare sender) {
		Log.d(TAG, flare.getName() + " position: " + newPosition.toString());
		for (FlareFragment fragment : flareFragments) {
			fragment.positionChanged();
		}
	}

	public void handleAction(Flare flare, String action, Flare sender) {
		Log.d(TAG, flare.getName() + " action: " + action);
	}

	boolean didEnter = false;

	public void enter(Zone zone, Device device) {
		Log.d(TAG, device.getName() + " entered " + zone.getName());
		device.setCurrentZone(zone);
		objectsChanged();
		didEnter = true;
		new java.util.Timer().schedule(
			new java.util.TimerTask() {
				@Override
				public void run() {
					didEnter = false;
				}
			}, 500
		);
	}

	public void exit(Zone zone, Device device) {
		if (!didEnter) {
			Log.d(TAG, device.getName() + " exited " + zone.getName());
			device.setCurrentZone(null);
			objectsChanged();
		} else {
			Log.d(TAG, "Ignoring exit message just after enter message");
		}
	}

	public void near(Thing thing, Device device, double distance) {
		Log.d(TAG, device.getName() + " is near to " + thing.getName());

		if ((device == mobileDevice || device == watchDevice) && thing != device.getNearbyThing()) {
			device.setNearbyThing(thing);
			objectsChanged();
			// not necessary because we're subscribing to all things
			// flareManager.subscribe(thing);
			flareManager.getData(thing);
			flareManager.getPosition(thing);

			if (device == watchDevice) {
				JSONObject message = new JSONObject();
				try { message.put("thing", thing.getId()); } catch (Exception e) {}
				sendMessageToWearable(message, Constants.TYPE_NEAR_THING);
			}
		}

		for (FlareFragment fragment : flareFragments) {
			fragment.near(thing, device, distance);
		}
	}

	public void far(Thing thing, Device device) {
		Log.d(TAG, device.getName() + " is far from " + thing.getName());

		/*
		// we're turning off far messages
		// with manual selection, we don't want the selection to randomly go away

		if ((device == mobileDevice || device == watchDevice) && thing == device.getNearbyThing()) {
			// not necessary because we're subscribing to all things
			// flareManager.unsubscribe(thing);
			device.setNearbyThing(null);
			objectsChanged();

			if (device == watchDevice) {
				JSONObject message = new JSONObject();
				try { message.put("thing", thing.getId()); } catch (Exception e) {}
				sendMessageToWearable(message, Constants.TYPE_FAR_THING);
			}
		}

		for (FlareFragment fragment : flareFragments) {
			fragment.far(thing, device);
		} */
	}

	private Location getLastKnownLocation() {
		List<String> providers = locationManager.getProviders(true);
		Location bestLocation = null;
		for (String provider : providers) {
			Location location = null;
			try {
				location = locationManager.getLastKnownLocation(provider);
			} catch (SecurityException exception) {
				Log.d(TAG, "App does not have location permissions.");
			}
			if (location == null) {
				continue;
			}
			if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
				bestLocation = location;
			}
		}
		return bestLocation;
	}

	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			Log.d(TAG, "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
		}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	}

	@Override
	public void onBeaconServiceConnect() {
		FlareBeaconManager.setRangeNotifier();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(messageReceiver);
		stopService(new Intent(this, NotificationListener.class));
		FlareBeaconManager.unbind();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (prefs.getBoolean("use_beacons", true)) FlareBeaconManager.pause();
		if (prefs.getBoolean("use_compass", true)) compassManager.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (prefs.getBoolean("use_beacons", true)) FlareBeaconManager.resume();
		if (prefs.getBoolean("use_compass", true)) compassManager.resume();
	}

	// FRAGMENTS

	public void addFragment(FlareFragment fragment) {
		flareFragments.add(fragment);
	}

	public void removeFragment(FlareFragment fragment) {
		flareFragments.remove(fragment);
	}

	public void objectsChanged() {
		for (FlareFragment fragment : flareFragments) {
			fragment.objectsChanged();
		}
	}

	public void dataChanged() {
		for (FlareFragment fragment : flareFragments) {
			fragment.dataChanged();
		}
	}

	public static class FlareFragment extends Fragment {
		public void onResume() {
			super.onResume();
			mainActivity.addFragment(this);
		}

		public void onPause() {
			super.onPause();
			mainActivity.removeFragment(this);
		}

		public void showMessage(String message) {}
		public void objectsChanged() {}
		public void positionChanged() {}
		public void angleChanged() {}
		public void dataChanged() {}
		public void near(Thing thing, Device device, double distance) {}
		public void far(Thing thing, Device device) {}
	}

	public static class PhoneFragment extends FlareFragment {

		public TextView environmentTextView;
		public TextView zoneTextView;
		public TextView deviceTextView;
		public TextView positionTextView;
		public TextView angleTextView;
		public TextView nearbyThingTextView;

		public PhoneFragment() {

		}

		public static PhoneFragment newInstance() {
			return new PhoneFragment();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_phone, container, false);

			environmentTextView = (TextView) view.findViewById(R.id.environmentTextView);
			zoneTextView = (TextView) view.findViewById(R.id.zoneTextView);
			deviceTextView = (TextView) view.findViewById(R.id.deviceTextView);
			positionTextView = (TextView) view.findViewById(R.id.positionTextView);
			angleTextView = (TextView) view.findViewById(R.id.angleTextView);
			nearbyThingTextView = (TextView) view.findViewById(R.id.nearbyThingTextView);

			objectsChanged();

			return view;
		}

		@Override
		public void showMessage(String message) {
			environmentTextView.setText(message);
		}

		@Override
		public void objectsChanged() {
			super.objectsChanged();

			Environment environment = mainActivity.environment;
			Device device = mainActivity.getDevice();
			Zone currentZone = device != null ? device.getCurrentZone() : null;
			Thing nearbyThing = device != null ? device.getNearbyThing() : null;

			Log.d(TAG, "Device: " + device);
			Log.d(TAG, "Current zone: " + currentZone);

			environmentTextView.setText(environment != null ? environment.getName() : "none");
			zoneTextView.setText(currentZone != null ? currentZone.getName() : "none");
			deviceTextView.setText(device != null ? device.getName() : "none");
			nearbyThingTextView.setText(nearbyThing != null ? nearbyThing.getName() : "none");
		}

		@Override
		public void positionChanged() {
			Device device = mainActivity.getDevice();

			if (device != null) {
				positionTextView.setText(String.format("%.1f, %.1f", device.getPosition().x, device.getPosition().y));
			} else {
				positionTextView.setText("0.0, 0.0");
			}
		}

		@Override
		public void angleChanged() {
			Device device = mainActivity.getDevice();

			try {
				angleTextView.setText(String.format("%.0f°", device.getData().getDouble("angle")));
			} catch (Exception e) {
				angleTextView.setText("0°");
			}
		}

		@Override
		public void near(Thing thing, Device device, double distance) {
			objectsChanged();
		}

		@Override
		public void far(Thing thing, Device device) {
			objectsChanged();
		}
	}

	public static class CompassFragment extends FlareFragment {
		private ClippedRoundLayout mRoundLayout;
		private GraphicsView mGraphicsView;
		private RelativeLayout mRimHoverInfo;
		private TextView mHoverObjectName;
		private TextView mHoverDescription;

		private RelativeLayout mTimeLayout;public CompassFragment() {

		}

		public static CompassFragment newInstance() {
			return new CompassFragment();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_compass, container, false);
			mRoundLayout = (ClippedRoundLayout) view.findViewById(R.id.round_layout);
			mGraphicsView = (GraphicsView) view.findViewById(R.id.graphical_view);
			mRimHoverInfo = (RelativeLayout) view.findViewById(R.id.rim_hover_info);
			mHoverObjectName = (TextView) view.findViewById(R.id.hover_objectName);
			mHoverDescription = (TextView) view.findViewById(R.id.hover_description);
			mTimeLayout = (RelativeLayout) view.findViewById(R.id.time_layout);
			objectsChanged();
			return view;
		}

		@Override
		public void objectsChanged() {
			super.objectsChanged();
			Device device = mainActivity.getDevice();
			Zone currentZone = device != null ? device.getCurrentZone() : null;
 			mGraphicsView.updateFlareView(mainActivity.flareManager, mainActivity.environment, currentZone, device);
		}

		@Override
		public void angleChanged() {
			mGraphicsView.dataChanged();
		}

		@Override
		public void positionChanged() {
			mGraphicsView.dataChanged();
		}

		@Override
		public void dataChanged() {
			mGraphicsView.dataChanged();
		}

		@Override
		public void near(Thing thing, Device device, double distance) {
			mGraphicsView.near(thing, device, distance);
		}

		@Override
		public void far(Thing thing, Device device) {
			mGraphicsView.far(thing, device);
		}
	}

	public static class MapFragment extends FlareFragment {
		private MapView mMapView;
		private FrameLayout mMapLayout;

		public MapFragment() {

		}

		public static MapFragment newInstance() {
			return new MapFragment();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_map, container, false);
			mMapView = (MapView) view.findViewById(R.id.map_view);
			mMapLayout = (FrameLayout) view.findViewById(R.id.map_view_layout);
			objectsChanged();
			return view;
		}

		@Override
		public void objectsChanged() {
			super.objectsChanged();
			Device device = mainActivity.getDevice();
			Zone currentZone = device != null ? device.getCurrentZone() : null;
			mMapView.updateFlareView(mainActivity.flareManager, mainActivity.environment, currentZone, device);
		}

		@Override
		public void angleChanged() {
			mMapView.dataChanged();
		}

		@Override
		public void positionChanged() {
			mMapView.dataChanged();
		}

		@Override
		public void dataChanged() {
			mMapView.dataChanged();
		}

		@Override
		public void near(Thing thing, Device device, double distance) {
			mMapView.near(thing, device, distance);
		}

		@Override
		public void far(Thing thing, Device device) {
			mMapView.far(thing, device);
		}
	}

	public static class CatalogFragment extends FlareFragment {

		public ListView catalogListView;
		public ThingArrayAdapter adapter;
		public ArrayList<Thing> things;

		public CatalogFragment() {

		}

		public static CatalogFragment newInstance() {
			return new CatalogFragment();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_list, container, false);

			catalogListView = (ListView) view.findViewById(R.id.catalogListView);
			catalogListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

			Device device = mainActivity.getDevice();
			Zone currentZone = device != null ? device.getCurrentZone() : null;
			Thing nearbyThing = device != null ? device.getNearbyThing() : null;
			things = currentZone != null ? currentZone.getThings() : null;
			sortThingsByDistanceFrom(device);

			adapter = new ThingArrayAdapter(getContext(), things);
			catalogListView.setAdapter(adapter);
			catalogListView.setOnItemClickListener((parent, view2, position, id) -> {
				Thing selectedThing = (Thing) catalogListView.getItemAtPosition(position);
				if (selectedThing != null) {
					// simulate a near message to select the device
					mainActivity.near(selectedThing, device, device.distanceTo(selectedThing));
				}
				Log.d(TAG, "Selected: " + selectedThing.getName());
			});

			selectThing(nearbyThing);
			objectsChanged();

			return view;
		}

		public void sortThingsByDistanceFrom(Device device) {
			Collections.sort(things, (Thing t1, Thing t2) -> (int)(device.distanceTo(t1) - device.distanceTo(t2)));
		}

		public class ThingArrayAdapter extends ArrayAdapter<Thing> {
			private final Context context;
			private ArrayList<Thing> values;
			private HashMap<String, Drawable> imageMap = new HashMap<>();

			public ThingArrayAdapter(Context context, ArrayList<Thing> values) {
				super(context, -1, values);
				this.context = context;
				this.values = values;
			}

			@Override
			public View getView(int position, View rowView, ViewGroup parent) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				if (rowView == null) {
					rowView = inflater.inflate(R.layout.fragment_list_thing, parent, false);
				}

				boolean selected = catalogListView.isItemChecked(position);
				rowView.setBackgroundColor(selected ? Color.LTGRAY : Color.WHITE);

				ImageView thingIcon = (ImageView) rowView.findViewById(R.id.thing_icon);
				TextView nameView = (TextView) rowView.findViewById(R.id.thing_name);
				TextView descriptionView = (TextView) rowView.findViewById(R.id.thing_description);
				TextView priceView = (TextView) rowView.findViewById(R.id.thing_price);
				TextView distanceView = (TextView) rowView.findViewById(R.id.thing_distance);

				Thing thing = values.get(position);
				Device device = mainActivity.getDevice();
				int price = 99;
				try { price = thing.getData().getInt("price"); } catch (Exception e) {}
				double distance = device.distanceTo(thing);

				nameView.setText(thing.getName());
				descriptionView.setText(thing.getDescription());
				priceView.setText("$" + price);
				distanceView.setText(String.format("%.2fm", distance));

				try {
					String imageName = thing.getName().toLowerCase() + "_" + thing.getColor() + "_small";
					Drawable image = imageMap.get(imageName);
					if (image == null) {
						Log.d(TAG, "Getting image: " + imageName);
						image = mainActivity.getImageNamed(imageName);
						if (image != null) imageMap.put(imageName, image);
					}
					thingIcon.setImageDrawable(image);
				} catch (Exception e) {
					thingIcon.setImageResource(android.R.color.transparent);
				}

				return rowView;
			}
		}


		@Override
		public void showMessage(String message) {

		}

		@Override
		public void objectsChanged() {
			super.objectsChanged();

			Device device = mainActivity.getDevice();
			Zone currentZone = device != null ? device.getCurrentZone() : null;
			Thing nearbyThing = device != null ? device.getNearbyThing() : null;
			things = currentZone != null ? currentZone.getThings() : null;
			sortThingsByDistanceFrom(device);
			selectThing(nearbyThing);

			adapter.notifyDataSetChanged();
		}

		private void selectThing(Thing thing) {
			if (catalogListView != null) {
				if (thing != null) {
					int position = things.indexOf(thing);
					if (position != -1) {
						catalogListView.setItemChecked(position, true);
					}
				}
			}
		}

		@Override
		public void positionChanged() {
			Device device = mainActivity.getDevice();
			sortThingsByDistanceFrom(device);
			adapter.notifyDataSetChanged();
		}

		@Override
		public void angleChanged() {

		}

		@Override
		public void near(Thing thing, Device device, double distance) {
			// change selection
		}

		@Override
		public void far(Thing thing, Device device) {

		}
	}

	public static class ThingFragment extends FlareFragment {
		public ThingFragment() {
			Log.d(TAG, "ThingFragment");
		}

		public ImageView imageView;
		public TextView nearbyThingTextView;
		public TextView nearbyThingDescription;
		// public TextView colorTextView;
		// public TextView brightnessTextView;

		public Button previousButton;
		public Button nextButton;
		public Button darkerButton;
		public Button lighterButton;
		public SeekBar brightnessBar;

		public ArrayList<Button> colorButtons = new ArrayList();
		public JSONArray defaultColorOptions;
		public JSONArray colorOptions;
		public String selectedColor = null;

		public static ThingFragment newInstance() {
			return new ThingFragment();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_thing, container, false);

			imageView = (ImageView) view.findViewById(R.id.imageView);

			while (!colorButtons.isEmpty()) { colorButtons.remove(0); }
			colorButtons.add((Button) view.findViewById(R.id.color_button_1));
			colorButtons.add((Button) view.findViewById(R.id.color_button_2));
			colorButtons.add((Button) view.findViewById(R.id.color_button_3));
			colorButtons.add((Button) view.findViewById(R.id.color_button_4));
			colorButtons.add((Button) view.findViewById(R.id.color_button_5));
			colorButtons.add((Button) view.findViewById(R.id.color_button_6));

			try {
				defaultColorOptions = new JSONArray("[\"red\", \"orange\", \"yellow\", \"green\", \"blue\", \"purple\"]");
			} catch (Exception e) {
				Log.d(TAG, "Couldn't parse default color options.");
			}
			colorOptions = defaultColorOptions;

			nearbyThingTextView = (TextView) view.findViewById(R.id.nearbyThingTextView2);
			nearbyThingDescription = (TextView) view.findViewById(R.id.nearbyThingDescription);
			// colorTextView = (TextView) view.findViewById(R.id.colorTextView);
			// brightnessTextView = (TextView) view.findViewById(R.id.brightnessTextView);

			previousButton = (Button) view.findViewById(R.id.previousButton);
			nextButton = (Button) view.findViewById(R.id.nextButton);
			darkerButton = (Button) view.findViewById(R.id.darkerButton);
			lighterButton = (Button) view.findViewById(R.id.lighterButton);

			previousButton.setOnClickListener(arg0 -> {
				mainActivity.performAction("previousColor");
			});
			nextButton.setOnClickListener(arg0 -> { mainActivity.performAction("nextColor"); });
			darkerButton.setOnClickListener(arg0 -> { mainActivity.performAction("darker"); });
			lighterButton.setOnClickListener(arg0 -> {
				mainActivity.performAction("lighter");
			});

			brightnessBar = (SeekBar) view.findViewById(R.id.brightnessBar);
			brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				int progress = 0;

				@Override
				public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
					progress = progressValue;
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					mainActivity.changeBrightness((double)progress / 100.0);
					dataChanged();
				}
			});

			objectsChanged();

			return view;
		}

		public void updateColorButtons() {
			for (int i = 0; i < colorButtons.size(); i++) {
				Button colorButton = colorButtons.get(i);
				if (i < colorOptions.length()) {
					String name = "red";
					try {
						name = colorOptions.getString(i);
					} catch (Exception e) {
						Log.d(TAG, "Couldn't get color from options.");
					}
					final String colorName = name;

					colorButton.setVisibility(View.VISIBLE);
					Drawable background = colorButton.getBackground();
					int color = HTMLColors.getHtmlColor(colorName);

					if (background instanceof ShapeDrawable) {
						((ShapeDrawable)background).getPaint().setColor(color);
					} else if (background instanceof GradientDrawable) {
						((GradientDrawable)background).setColor(color);
					}

					colorButton.setOnClickListener(arg0 -> {
						mainActivity.changeColor(colorName);
						dataChanged();
					});
				} else {
					colorButton.setVisibility(View.INVISIBLE);
				}
			}
		}

		public void updateSelectedColor() {
			for (int i = 0; i < colorButtons.size(); i++) {
				Button colorButton = colorButtons.get(i);
				if (i < colorOptions.length()) {
					String colorName;
					boolean selected = false;

					try {
						colorName = colorOptions.getString(i);
						selected = colorName.equals(selectedColor);
					} catch (Exception e) {
						Log.d(TAG, "Couldn't get color from options.");
					}

					Drawable background = colorButton.getBackground();
					if (background instanceof GradientDrawable) {
						((GradientDrawable)background).setStroke(10, selected ? 0xFF3D47C0 : 0xFFFFFFFF);
					}
				}
			}
		}

		@Override
		public void objectsChanged() {
			super.objectsChanged();

			Device device = mainActivity.getDevice();
			Thing nearbyThing = device != null ? device.getNearbyThing() : null;

			nearbyThingTextView.setText(nearbyThing != null ? nearbyThing.getName() : "none");
			nearbyThingDescription.setText(nearbyThing != null ? nearbyThing.getDescription() : "");

			try {
				selectedColor = nearbyThing.getColor();
			} catch (Exception e) {
				selectedColor = null;
			}

			try {
				JSONArray options = nearbyThing.getData().getJSONArray("options");
				colorOptions = (options.length() > 0) ? options : defaultColorOptions;
			} catch (Exception e) {
				colorOptions = defaultColorOptions;
			}
			updateColorButtons();

			if (imageView != null) imageView.setImageResource(android.R.color.transparent);

			dataChanged();
		}

		@Override
		public void dataChanged() {
			Device device = mainActivity.getDevice();
			Thing nearbyThing = device != null ? device.getNearbyThing() : null;

			try {
				selectedColor = nearbyThing.getColor();
				// colorTextView.setText(selectedColor);

				try {
					String name = nearbyThing.getName().toLowerCase();
					String imageName = name + "_" + selectedColor;
					if (imageView != null) imageView.setImageDrawable(mainActivity.getImageNamed(imageName));
				} catch (Exception e) {
					if (imageView != null) imageView.setImageResource(android.R.color.transparent);
				}
			} catch (Exception e) {
				selectedColor = null;
				// colorTextView.setText("—");
			}

			updateSelectedColor();

			try {
				double brightness = nearbyThing.getData().getDouble("brightness");
				// brightnessTextView.setText(String.format("%.1f", brightness));
				brightnessBar.setProgress((int)(brightness * 100));
			} catch (Exception e) {
				// brightnessTextView.setText("—");
			}
		}

		@Override
		public void near(Thing thing, Device device, double distance) {
			objectsChanged();
		}

		@Override
		public void far(Thing thing, Device device) {
			objectsChanged();
		}
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 5;
		}

		@Override
		public Fragment getItem(int position) {
			Fragment item = null;
			switch (position) {
				case 0: item = PhoneFragment.newInstance(); break;
				case 1: item = CompassFragment.newInstance(); break;
				case 2: item = MapFragment.newInstance(); break;
				case 3: item = CatalogFragment.newInstance(); break;
				case 4: item = ThingFragment.newInstance(); break;
			}
			return item;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0: return "PHONE";
				case 1: return "RIM";
				case 2: return "MAP";
				case 3: return "LIST";
				case 4: return "THING";
			}
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
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
			Intent startActivity = new Intent(this, com.cisco.flare.trilateral.SettingsActivity.class);
			startActivity(startActivity);
			return true;
		} else if (id == R.id.action_reload) {
			load();
			return true;
		} else if (id == R.id.action_update_watch) {
			sendEnvironmentToWearable();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public Drawable getImageNamed(String name) {
		Resources resources = getResources();
		final int resourceId = resources.getIdentifier(name, "drawable", getPackageName());
		return resources.getDrawable(resourceId, getTheme());
	}

	// WEARABLE

	private void sendMessageToWearable(JSONObject json, String type) {
		sendMessageToWearable(json.toString(), type);
	}

	private void sendMessageToWearable(String message, String type) {
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

			if (message != null && message.length() > 200) {
				Log.d(TAG, "Received " + type + ": " + message.length() + " bytes");
			} else {
				Log.d(TAG, "Received " + type + ": " + message);
			}

			if (type.equals(Constants.TYPE_HELLO)) {
				Log.d(TAG, "Got hello.");
				sendEnvironmentToWearable();
			} else if (type.equals(Constants.TYPE_POSITION_ANGLE)) {
				receivePositionAngle(message);
			} else if (type.equals(Constants.TYPE_ACTION)) {
				receiveAction(message);
			} else {
				Log.w(TAG, "Message type unknown: " + type);
			}
		}
	};

	private void sendEnvironmentToWearable() {
		JSONObject message = new JSONObject();
		try { if (environment != null) message.put("environment", environment.toJSON()); } catch (Exception e) {}
		if (watchDevice != null) {
			try { message.put("device", watchDevice.toJSON()); } catch (Exception e) {}
		}
		sendMessageToWearable(message, Constants.TYPE_ENVIRONMENT);
	}

	private void sendPositionToWearable(PointF position) {
		sendMessageToWearable(Flare.pointToJSON(position), Constants.TYPE_POSITION);
	}

	private void receivePositionAngle(String jsonString) {
		try {
			JSONObject messageJson = new JSONObject(jsonString);
			JSONObject positionJson = messageJson.getJSONObject("position");
			PointF position = Flare.getPoint(positionJson);
			float angle = (float)messageJson.getDouble("angle");

			gotPosition(position, watchDevice);
			gotAngle(angle, watchDevice);
		} catch (Exception e) {
			Log.e(TAG, "Couldn't parse position: " + jsonString, e);
			e.printStackTrace();
		}
	};

	private void receiveAction(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);
			String thingId = json.getString("thing");
			String action = json.getString("action");

			if (watchDevice != null && environment != null && thingId != null) {
				Thing thing = environment.getThingWithId(thingId);
				if (thing != null) {
					flareManager.performAction(thing, action, watchDevice);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Couldn't parse action: " + jsonString, e);
			e.printStackTrace();
		}
	};

}
