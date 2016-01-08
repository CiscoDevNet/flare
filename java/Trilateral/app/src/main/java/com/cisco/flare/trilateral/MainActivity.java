package com.cisco.flare.trilateral;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
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

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cisco.flare.CompassManager;
import com.cisco.flare.Device;
import com.cisco.flare.Environment;
import com.cisco.flare.Flare;
import com.cisco.flare.FlareBeaconManager;
import com.cisco.flare.FlareManager;
import com.cisco.flare.Thing;
import com.cisco.flare.Zone;

import org.altbeacon.beacon.BeaconConsumer;
import org.json.JSONObject;

import java.util.ArrayList;
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
	public Device device;
	public Zone currentZone;
	public Thing nearbyThing;

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

		compassManager = new CompassManager((SensorManager)getSystemService(SENSOR_SERVICE));
		compassManager.setDelegate(this);

		setEnvironment(null);
		setCurrentZone(null);
		setDevice(null);
		setNearbyThing(null);

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
			Integer.parseInt(prefs.getString("pref_flare_port", "1234"));
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
	public void loadDevice() {
		flareManager.getCurrentDevice(environment.getId(), deviceTemplate(), prefs, (newDevice) -> {
			if (newDevice != null) {
				setDevice(newDevice);
				flareManager.subscribe(device);

				flareManager.getCurrentZone(environment.getId(), device.getPosition(), (newZone) -> {
					setCurrentZone(newZone);
				});

				flareManager.getNearbyThing(environment.getId(), device.getId(), (newNearbyThing) -> {
					setNearbyThing(newNearbyThing);
				});
			} else {
				Log.d(TAG, "Could not load device.");
			}
		});
	}

	// called by setEnvironment()
	public void setupBeacons() {
		FlareBeaconManager.setDeviceTypeAndConsumer(FlareBeaconManager.KEY_PREF_BEACON_DEVICE_MOBILE, this);
		FlareBeaconManager.setCallback((PointF position) -> {
			runOnUiThread(() -> {
				Log.d(TAG, "Position: " + position);
				if (this.device != null) {
					flareManager.setPosition(this.device, position, null);
					device.setPosition(position);
					for (FlareFragment fragment : flareFragments) {
						fragment.positionChanged();
					}
				}
			});
		});
		FlareBeaconManager.bind(this);
		FlareBeaconManager.setEnvironment(environment);
		FlareBeaconManager.restartRangingBeacons();
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment value) {
		this.environment = value;

		if (value != null) {
			loadDevice();
			setupBeacons();
			printEnvironment(value);
		}

		for (FlareFragment fragment : flareFragments) {
			fragment.objectsChanged();
		}
	}

	public Zone getCurrentZone() {
		return currentZone;
	}

	public void setCurrentZone(Zone value) {
		this.currentZone = value;
		for (FlareFragment fragment : flareFragments) {
			fragment.objectsChanged();
		}
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device value) {
		this.device = value;
		for (FlareFragment fragment : flareFragments) {
			fragment.objectsChanged();
		}
	}

	public Thing getNearbyThing() {
		return nearbyThing;
	}

	public void setNearbyThing(Thing value) {
		this.nearbyThing = value;
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
	public JSONObject deviceTemplate() {
		String userName = fullName();
		if (userName != null) userName = userName.replaceAll(" .*", "");
		String deviceName = userName != null ? userName + "'s " + Build.MODEL : Build.MODEL;
		String brand = Build.BRAND;
		if (brand.equals("google")) brand = "Google";
		String description = brand + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE;
		JSONObject data = new JSONObject();
		String macAddress = macAddress();
		if (macAddress != null) try { data.put("mac", macAddress); } catch (Exception e) {}

		JSONObject template = new JSONObject();
		try { template.put("name", deviceName ); } catch (Exception e) {}
		try { template.put("description", description); } catch (Exception e) {}
		try { template.put("data", data); } catch (Exception e) {}
		try { template.put("position", Flare.zeroPoint()); } catch (Exception e) {}

		return template;
	}

	// returns the user's full name
	public String fullName() {
		String fullName = null;
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
		return fullName;
	}

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
			device.setPosition(position);
			flareManager.setPosition(device, position, device);
			for (FlareFragment fragment : flareFragments) {
				fragment.positionChanged();
			}
		} catch (Exception e) {}
	}

	public void changeColor(String color) {
		try {
			nearbyThing.getData().put("color", color);
			flareManager.setData(nearbyThing, "color", color, device);
		} catch (Exception e) {}
	}

	public void changeBrightness(double brightness) {
		try {
			nearbyThing.getData().put("brightness", brightness);
			flareManager.setData(nearbyThing, "brightness", brightness, device);
		} catch (Exception e) {}
	}

	public void changeAngle(double angle) {
		try {
			device.getData().put("angle", angle);
			flareManager.setData(device, "angle", angle, device);
			for (FlareFragment fragment : flareFragments) {
				fragment.angleChanged();
			}
		} catch (Exception e) {
		}
	}

	public void performAction(String action) {
		flareManager.performAction(nearbyThing, action, device);
	}

	public void didReceiveData(Flare flare, JSONObject data, Flare sender) {
		Log.d(TAG, flare.getName() + " data: " + data.toString());
		try {
			String key = (String) data.names().get(0);
			if ((flare == device && "angle".equals(key))) {
				for (FlareFragment fragment : flareFragments) {
					fragment.angleChanged();
				}
			} else if ((flare == nearbyThing && ("color".equals(key) || "brightness".equals(key)))) {
				for (FlareFragment fragment : flareFragments) {
					fragment.dataChanged();
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
		setCurrentZone(zone);
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
			setCurrentZone(null);
		} else {
			Log.d(TAG, "Ignoring exit message just after enter message");
		}
	}

	public void near(Thing thing, Device device, double distance) {
		Log.d(TAG, device.getName() + " is near to " + thing.getName());
		if (device == this.device && thing != nearbyThing) {
			setNearbyThing(thing);
			flareManager.subscribe(thing);
			flareManager.getData(thing);
			flareManager.getPosition(thing);
		}

		for (FlareFragment fragment : flareFragments) {
			fragment.near(thing, device, distance);
		}
	}

	public void far(Thing thing, Device device) {
		Log.d(TAG, device.getName() + " is far from " + thing.getName());
		if (device == this.device && thing == nearbyThing) {
			flareManager.unsubscribe(thing);
			setNearbyThing(null);
		}

		for (FlareFragment fragment : flareFragments) {
			fragment.far(thing, device);
		}
	}

	private Location getLastKnownLocation() {
		List<String> providers = locationManager.getProviders(true);
		Location bestLocation = null;
		for (String provider : providers) {
			Location location = null;
			try {
				location = locationManager.getLastKnownLocation(provider);
			} catch (SecurityException exception) {}
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

	public void deviceAngleChanged(float azimuth) {

		// tell the graphics view that the device angle has changed
		// mGraphicsView.setGlobalAngle(azimuth);
		// mMapView.setGlobalAngle(azimuth);
		// tell the server that the device angle has changed
		// Log.d(TAG, "Angle: " + azimuth);

		if (flareManager != null && device != null) {
			try { device.getData().put("angle", azimuth); } catch (Exception exception) { }
			flareManager.setData(device, "angle", azimuth, device);
			for (FlareFragment fragment : flareFragments) {
				fragment.angleChanged();
			}
		}
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
			PhoneFragment fragment = new PhoneFragment();

			return fragment;
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
			Zone currentZone = mainActivity.currentZone;
			Device device = mainActivity.device;
			Thing nearbyThing = mainActivity.nearbyThing;

			environmentTextView.setText(environment != null ? environment.getName() : "none");
			zoneTextView.setText(currentZone != null ? currentZone.getName() : "none");
			deviceTextView.setText(device != null ? device.getName() : "none");
			nearbyThingTextView.setText(nearbyThing != null ? nearbyThing.getName() : "none");
		}

		@Override
		public void positionChanged() {
			Device device = mainActivity.device;

			if (device != null) {
				positionTextView.setText(String.format("%.1f, %.1f", device.getPosition().x, device.getPosition().y));
			} else {
				positionTextView.setText("0.0, 0.0");
			}
		}

		@Override
		public void angleChanged() {
			Device device = mainActivity.device;

			try {
				angleTextView.setText(String.format("%.0f°", device.getData().getDouble("angle")));
			} catch (Exception e) {
				angleTextView.setText("0°");
			}
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
			CompassFragment fragment = new CompassFragment();

			return fragment;
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
			mGraphicsView.updateFlareView(mainActivity.flareManager, mainActivity.environment, mainActivity.currentZone, mainActivity.device);
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
			MapFragment fragment = new MapFragment();
			return fragment;
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
			mMapView.updateFlareView(mainActivity.flareManager, mainActivity.environment, mainActivity.currentZone, mainActivity.device);
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

	public static class ThingFragment extends FlareFragment {
		public ThingFragment() {
			Log.d(TAG, "ThingFragment");
		}

		public TextView nearbyThingTextView;
		public TextView colorTextView;
		public TextView brightnessTextView;

		public Button rainbowButton;
		public Button invertButton;
		public Button darkerButton;
		public Button lighterButton;

		public static ThingFragment newInstance() {
			ThingFragment fragment = new ThingFragment();
			return fragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_thing, container, false);

			nearbyThingTextView = (TextView) view.findViewById(R.id.nearbyThingTextView2);
			colorTextView = (TextView) view.findViewById(R.id.colorTextView);
			brightnessTextView = (TextView) view.findViewById(R.id.brightnessTextView);

			rainbowButton = (Button) view.findViewById(R.id.rainbowButton);
			invertButton = (Button) view.findViewById(R.id.invertButton);
			darkerButton = (Button) view.findViewById(R.id.darkerButton);
			lighterButton = (Button) view.findViewById(R.id.lighterButton);

			rainbowButton.setOnClickListener(arg0 -> {
				mainActivity.performAction("rainbow");
			});
			invertButton.setOnClickListener(arg0 -> { mainActivity.performAction("invert"); });
			darkerButton.setOnClickListener(arg0 -> { mainActivity.performAction("darker"); });
			lighterButton.setOnClickListener(arg0 -> { mainActivity.performAction("lighter"); });

			return view;
		}

		@Override
		public void objectsChanged() {
			super.objectsChanged();

			Thing nearbyThing = mainActivity.nearbyThing;

			nearbyThingTextView.setText(nearbyThing != null ? nearbyThing.getName() : "none");
		}

		@Override
		public void dataChanged() {
			Thing nearbyThing = mainActivity.nearbyThing;

			try {
				colorTextView.setText(nearbyThing.getData().getString("color"));
			} catch (Exception e) {
				colorTextView.setText("—");
			}

			try {
				brightnessTextView.setText(String.format("%.1f", nearbyThing.getData().getDouble("brightness")));
			} catch (Exception e) {
				brightnessTextView.setText("—");
			}
		}
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public Fragment getItem(int position) {
			Fragment item = null;
			switch (position) {
				case 0: item = PhoneFragment.newInstance(); break;
				case 1: item = CompassFragment.newInstance(); break;
				case 2: item = MapFragment.newInstance(); break;
				case 3: item = ThingFragment.newInstance(); break;
			}
			Log.d(TAG, "Get item " + position);
			return item;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0: return "PHONE";
				case 1: return "COMPASS";
				case 2: return "MAP";
				case 3: return "THING";
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
		}

		return super.onOptionsItemSelected(item);
	}

}
