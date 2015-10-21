package com.cisco.ctao.ioe.ux.trilateral;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import com.cisco.flare.Device;
import com.cisco.flare.Environment;
import com.cisco.flare.Flare;
import com.cisco.flare.FlareBeaconManager;
import com.cisco.flare.FlareManager;
import com.cisco.flare.Thing;
import com.cisco.flare.Zone;

import org.altbeacon.beacon.BeaconConsumer;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MobileMainActivity extends Activity implements FlareManager.Delegate, SensorEventListener, BeaconConsumer {
    private static final String TAG = "MobileMainActivity";

    private static final String FLARE_HOST = "pref_flare_host";
    private static final String FLARE_PORT = "pref_flare_port";

    private SharedPreferences mPrefs;

    private LocationManager mLocationManager;
    private Location mCurrentLocation;

    private FlareManager mFlareManager;
    private Environment mEnvironment;
    private Zone mSelectedZone;
    private Device mDevice;

    private EditText mServerText, mPortText;
    private Button mFlareServerConnectButton;
    private Spinner mEnvironmentDropdownList;
    private Button mEnvironmentSelectButton;
    private Spinner mZoneDropdownList;
    private Button mZoneSelectButton;
    private TableRow mEnvironmentRow;
    private TableRow mZoneRow;
    private TableRow mErrorStatusRow;
    private ClippedRoundLayout mRoundLayout;
    private GraphicsView mGraphicsView;
    private SeekBar mAngleSeekBar; // used for the emulator to change the device angle
    private float mAngleSeekBarToDegrees;
    private MapView mMapView;
    private FrameLayout mMapLayout;

    // Rim Hover Info layout and elements it contains
    private RelativeLayout mRimHoverInfo;
    private TextView mHoverObjectName;
    private TextView mHoverDescription;
    private RelativeLayout mTimeLayout;

    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;
    private float mOrientation[] = new float[3];
    private float[] mInR3x3 = new float[9];
    private boolean mScreenReady = false;
    float mLastOrientationAngleSent = -1.0f;

    // handler for the clock
    private Handler mTimeHandler = new Handler();
    private TextView mTimeTextView;
    private TextView mAmPmTextView;

    public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ScreenDensity.getInstance().setScale(getApplicationContext());

        // set the callback to update the time
        mTimeTextView = (TextView) findViewById(R.id.text_time);
        mAmPmTextView = (TextView) findViewById(R.id.text_amPm);

        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 100, locationListener);

        mServerText = (EditText) findViewById(R.id.server);
        mPortText = (EditText) findViewById(R.id.port);
        mEnvironmentRow = (TableRow) findViewById(R.id.environment_row);
        mZoneRow = (TableRow) findViewById(R.id.zone_row);
        mErrorStatusRow = (TableRow) findViewById(R.id.error_status_row);
        mFlareServerConnectButton = (Button) findViewById(R.id.flare_connect_button);
        mFlareServerConnectButton.setOnClickListener((View v) -> {
            // saving preferences first
            SharedPreferences.Editor editor = mPrefs.edit();
            String host = mServerText.getText().toString(),
                   port = mPortText.getText().toString();
            editor.putString(FLARE_HOST, host);
            editor.putString(FLARE_PORT, port);
            editor.commit();

            // hide the environment and zone rows, and the graphics view
            mEnvironmentRow.setVisibility(View.INVISIBLE);
            mZoneRow.setVisibility(View.INVISIBLE);
            mErrorStatusRow.setVisibility(View.INVISIBLE);
            setGraphicsVisibility(View.INVISIBLE);

            // then connect to the Flare server
            try {
                connectToFlareServer(host, Integer.parseInt(port));
            } catch (Exception e) {
                Log.e(TAG, "Port could not be converted to int.", e);
            }
        });
        mEnvironmentDropdownList = (Spinner) findViewById(R.id.environment_list);
        // when the environment selection changes, hide the zone drop down list
        mEnvironmentDropdownList.setOnItemSelectedListener(new CustomOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                environmentSelectionChanged();
            }
        });

        mEnvironmentSelectButton = (Button) findViewById(R.id.env_select_button);
        mZoneDropdownList = (Spinner) findViewById(R.id.zone_list);
        mZoneSelectButton = (Button) findViewById(R.id.zone_select_button);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mRoundLayout = (ClippedRoundLayout) findViewById(R.id.round_layout);
        mGraphicsView = (GraphicsView) findViewById(R.id.graphical_view);
        mMapView = (MapView) findViewById(R.id.map_view);
        mMapLayout = (FrameLayout) findViewById(R.id.map_view_layout);

        mRimHoverInfo = (RelativeLayout) findViewById(R.id.rim_hover_info);
        mHoverObjectName = (TextView) findViewById(R.id.hover_objectName);
        mHoverDescription = (TextView) findViewById(R.id.hover_description);

        mTimeLayout = (RelativeLayout) findViewById(R.id.time_layout);

        loadPreferences();

        startSensorDataAcquisition();
    }

    private void setGraphicsVisibility(int visibility) {
        mRoundLayout.setVisibility(visibility);
        mGraphicsView.setVisibility(visibility);
        mMapLayout.setVisibility(visibility);
        mMapView.setVisibility(visibility);
        if (mAngleSeekBar != null) {
            mAngleSeekBar.setVisibility(visibility);
        }
    }

    // when the environment selection changes, hide the zone drop down list and the graphics view
    private void environmentSelectionChanged() {
        mZoneRow.setVisibility(View.INVISIBLE);
        mErrorStatusRow.setVisibility(View.INVISIBLE);
        setGraphicsVisibility(View.INVISIBLE);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadPreferences() {
        String host = mPrefs.getString(FLARE_HOST, "");
        int port = 1234;
        try {
            String portString = mPrefs.getString(FLARE_PORT, "1234");
            port = Integer.parseInt(portString);
        } catch (Exception e) {
            Log.e(TAG, "Port could not be converted to int.", e);
        }

        mServerText.setText(host);
        mPortText.setText("" + port);
    }

    private void connectToFlareServer(String host, int port) {
        if (mFlareManager != null) {
            mFlareManager.disconnect();
            mFlareManager = null;
        }
        mFlareManager = new FlareManager(host, port);
        mFlareManager.setActivity(this);
        mFlareManager.setDelegate(this);
        mFlareManager.connect();

        mCurrentLocation = getLastKnownLocation();
        mFlareManager.loadEnvironments(mCurrentLocation, (environments) -> {
            if (environments == null) {
                Log.e(TAG, "Cannot connect to Flare server to retrieve environments");
                mErrorStatusRow.setVisibility(View.VISIBLE);
            } else if (!environments.isEmpty()) {
                List<String> list = new ArrayList<String>();
                for (int i = 0; i < environments.size(); i++) {
                    list.add(environments.get(i).getName());
                    Log.d(TAG, "env #" + i + " = " + environments.get(i).getName());
                }
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item, list);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mEnvironmentDropdownList.setAdapter(dataAdapter);

                // show the environment row
                mEnvironmentRow.setVisibility(View.VISIBLE);
                // hide the status row now that the environment was received properly
                mErrorStatusRow.setVisibility(View.INVISIBLE);

                mEnvironmentSelectButton.setOnClickListener((View v) -> {

                    // hide the environment row
                    mZoneRow.setVisibility(View.INVISIBLE);

                    mEnvironment = environments.get(mEnvironmentDropdownList.getSelectedItemPosition());
                    Log.i(TAG, "environment selected: " + mEnvironment.getName());

                    getCurrentDevice(mEnvironment.getId(), deviceTemplate(), (value) -> {
                        mDevice = value;

                        // subscribe to receive location updates for this device
                        mFlareManager.subscribe(mDevice);

                        notifyViews();
                    });

                    ArrayList<Zone> zones = mEnvironment.getZones();
                    if (!zones.isEmpty()) {
                        // show the environment row
                        mZoneRow.setVisibility(View.VISIBLE);

                        List<String> zoneList = new ArrayList<String>();
                        for (int i = 0; i < zones.size(); i++) {
                            zoneList.add(zones.get(i).getName());
                        }
                        ArrayAdapter<String> zoneDataAdapter = new ArrayAdapter<String>(this,
                                android.R.layout.simple_spinner_item, zoneList);
                        zoneDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        mZoneDropdownList.setAdapter(zoneDataAdapter);
                        mZoneSelectButton.setOnClickListener((View v2) -> {
                            // when a zone gets selected, show the graphics view
                            setGraphicsVisibility(View.VISIBLE);

                            mSelectedZone = zones.get(mZoneDropdownList.getSelectedItemPosition());
                            notifyViews();
                        });
                    } else {
                        // TODO: display "no zone"
                    }

                    FlareBeaconManager.setDeviceTypeAndConsumer(FlareBeaconManager.KEY_PREF_BEACON_DEVICE_MOBILE, this);
                    FlareBeaconManager.setCallback((PointF position) -> {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Position: " + position);
//                        positionTextView.setText(position.x + ", " + position.y);
                            if (this.mDevice != null) {
                                mFlareManager.setPosition(this.mDevice, position, this.mDevice);
                            } else {
                                Log.d(TAG, "Device is null.");
                            }
                        });
                    });
                    FlareBeaconManager.bind(this);
                    FlareBeaconManager.setEnvironment(mEnvironment);
                    FlareBeaconManager.restartRangingBeacons();

                    mScreenReady = true;

                    printEnvironments(environments);
                });

            }
        });
    }

    private void notifyViews() {
        notifyView(mGraphicsView);
        notifyView(mMapView);
    }

    private void notifyView(CommonView view) {
        view.updateFlareView(mFlareManager, mEnvironment, mSelectedZone, mDevice);
    }

    private void forwardNearNotificationToViews(Thing thing, Device device, double distance) {
        forwardNearNotification(mGraphicsView, thing, device, distance);
        forwardNearNotification(mMapView, thing, device, distance);

        mGraphicsView.updateUIElements(mTimeLayout, mRimHoverInfo, mHoverObjectName, mHoverDescription);
    }

    private void forwardNearNotification(CommonView view, Thing thing, Device device, double distance) {
        view.near(thing, device, distance);
    }

    private void forwardFarNotificationToViews(Thing thing, Device device) {
        forwardFarNotification(mGraphicsView, thing, device);
        forwardFarNotification(mMapView, thing, device);

        mGraphicsView.updateUIElements(mTimeLayout, mRimHoverInfo, mHoverObjectName, mHoverDescription);
    }

    private void forwardFarNotification(CommonView view, Thing thing, Device device) {
        view.far(thing, device);
    }

    public void printEnvironments(ArrayList<Environment> environments) {
        for (Environment mEnvironment : environments) {
            Log.d(TAG, mEnvironment.toString());

            for (Zone zone : mEnvironment.getZones()) {
                Log.d(TAG, "  " + zone.toString());

                for (Thing thing : zone.getThings()) {
                    Log.d(TAG, "    " + thing.toString());

                    mFlareManager.subscribe(thing);
                }
            }

            for (Device device: mEnvironment.getDevices()) {
                Log.d(TAG, device.toString());
            }
        }
    }

    public interface DeviceHandler {
        public void gotResponse(Device device);
    }

    // tries to find an existing device object in the current environment
    // if one is not found, creates a new device object
    public void getCurrentDevice(String environmentId, JSONObject template, DeviceHandler handler) {
        savedDevice(environmentId, (savedDevice) -> {
            if (savedDevice != null) {
                handler.gotResponse(savedDevice);
            } else {
                newDeviceObject(environmentId, template, (newDevice) -> {
                    if (newDevice != null) {
                        handler.gotResponse(newDevice);
                    }
                });
            }
        });
    }

    // looks for an existing device object in the current environment, and if found calls the handler with it
    public void savedDevice(String environmentId, DeviceHandler handler) {
        String deviceId = mPrefs.getString("deviceId", null);
        if (deviceId != null) {
            mFlareManager.getDevice(deviceId, environmentId, (json) -> {
                try {
                    String validId = json.getString("_id");
                    String deviceEnvironment = json.getString("environment");

                    if (deviceEnvironment.equals(environmentId)) {
                        Device device = new Device(json);
                        mFlareManager.addToIndex(device);

                        Log.d(TAG, "Found existing device: " + device.getName());
                        handler.gotResponse(device);
                    } else {
                        Log.d(TAG, "Device in wrong environment");
                        handler.gotResponse(null);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Device not found in current environment");
                    handler.gotResponse(null);
                }
            });
        } else {
            Log.d(TAG, "No saved device");
            handler.gotResponse(null);
        }
    }

    // creates a new device object using the template
    public void newDeviceObject(String environmentId, JSONObject template, DeviceHandler handler) {
        mFlareManager.newDevice(environmentId, template, (json) -> {
            Device device = new Device(json);
            mFlareManager.addToIndex(device);

            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("deviceId", device.getId());
            editor.commit();

            Log.d(TAG, "Created new device: " + device.getName());
            handler.gotResponse(device);
        });
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
        WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String macAddr = wifiInf.getMacAddress();
        // the emulator doesn't have a mac address, so return an empty string if that's the case
        if (macAddr == null) return "";
        return macAddr.toLowerCase();
    }

    public void didReceiveData(Flare flare, JSONObject data, Flare sender) {
        Log.d(TAG, flare.getName() + " data: " + data.toString());

        // update the data for this object
        String thingId = flare.getId();
        mSelectedZone.getThing(thingId).setData(data);
    }

    public void didReceivePosition(Flare flare, PointF position, Flare sender) {
        Log.d(TAG, flare.getName() + " position: " + position.toString());

        if (flare.getClass().toString().equals(Thing.class.toString())) {
            // update the position for this object
            String thingId = flare.getId();
            mSelectedZone.getThing(thingId).setPosition(position);
        }
        else if (flare.getClass().toString().equals(Device.class.toString())) {
            mDevice.setPosition(position);
        }
        else {
            Log.e(TAG, "Received position update for unknown object type '"+flare.getClass().toString()+"'");
        }
    }

    public void handleAction(Flare flare, String action, Flare sender) {
        Log.d(TAG, flare.getName() + " action: " + action);
    }

    public void enter(Zone zone, Device device) {
        Log.d(TAG, device.getName() + " entered " + zone.getName());
    }

    public void exit(Zone zone, Device device) {
        Log.d(TAG, device.getName() + " exited " + zone.getName());
    }

    public void near(Thing thing, Device device, double distance) {
        Log.d(TAG, device.getName() + " is near to " + thing.getName());
        forwardNearNotificationToViews(thing, device, distance);
    }

    public void far(Thing thing, Device device) {
        Log.d(TAG, device.getName() + " is far from " + thing.getName());
        forwardFarNotificationToViews(thing, device);
    }

    private Location getLastKnownLocation() {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location location = mLocationManager.getLastKnownLocation(provider);
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

    /**
     * Handle time display
     */
    private Runnable updateCurrentTime = new Runnable () {
        public void run() {
            Calendar c = Calendar.getInstance();

            // refresh time
            int minutes = c.get(Calendar.MINUTE);
            int hours = c.get(Calendar.HOUR);
            // 0pm is midday so it should read 12pm
            if (hours == 0 && c.get(Calendar.AM_PM) == Calendar.PM) { hours = 12; }
            String time = hours+":"+(minutes < 10 ? "0" : "")+minutes;
            String amPm = c.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";
            onUpdateTime(time, amPm);

            // refresh every minute
            int nextRefresh = (60-c.get(Calendar.SECOND))*1000;

            // queue the task to run again in X seconds...
            mTimeHandler.postDelayed(updateCurrentTime, nextRefresh);
        }
    };

    private void onUpdateTime(String timeText, String amPmText) {
        if (mTimeTextView != null && mAmPmTextView != null) {
            mTimeTextView.setText(timeText);
            mAmPmTextView.setText(amPmText);
        }
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
    protected void onStop() {
        super.onStop();

        // stop listening to notifications
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FlareBeaconManager.pause();

        // cancel runnables to save battery
        mTimeHandler.removeCallbacks(updateCurrentTime);

        // stop listening to sensors
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mRotationVectorSensor);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FlareBeaconManager.resume();

        // start updating the current time being displayed in 1.5 seconds (there's no rush, that leaves time for the debugger to start)
       mTimeHandler.postDelayed(updateCurrentTime, 1500);

        // start listening to sensors
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL/*SENSOR_DELAY_GAME*/);
        }
    }


    /**********************************************************************************************/
    /* Sensors */

    private void startSensorDataAcquisition() {
        Log.d(TAG, "start sensors");

        // the rotation vector sensor is null if we're on the emulator
        // or if use_orientation_sensor is set to false in options.xml
        boolean useOrientationSensor = getResources().getBoolean(R.bool.use_orientation_sensor);
        if (useOrientationSensor) {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            // if we failed to get the rotation sensor working, reset useOrientationSensor to false
            if (mRotationVectorSensor == null) {
                useOrientationSensor = false;
                Log.d(TAG, "mRotationVectorSensor is null, so this must be the emulator");
            }
        }
        // set up the angle seek bar to emulate the rotation
        if (!useOrientationSensor) {
            mAngleSeekBar = (SeekBar) findViewById(R.id.angle_seek_bar);
            mAngleSeekBarToDegrees = (float)mAngleSeekBar.getMax() / 360.0f;
            if (mAngleSeekBar != null) {
                mAngleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float azimuth = (float)-progress/mAngleSeekBarToDegrees;
                        deviceAngleChanged(azimuth);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
        }

        // the listeners for the sensor manager are being registered in onResume
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        // ignoring sensor data for now
        if (!mScreenReady) {
            return;
        }

        // If the sensor data is unreliable return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
            return;
        }
        // convert the rotation vector (event.values) to a 4x4 or 3x3 matrix (inR)
        SensorManager.getRotationMatrixFromVector(mInR3x3, event.values);
        SensorManager.getOrientation(mInR3x3, mOrientation);
        float azimuth = (float)Math.toDegrees(mOrientation[0]);
        while (azimuth < 0) {
            azimuth += 360;
        }

        deviceAngleChanged(azimuth);
    }

    private void deviceAngleChanged(float azimuth) {
        // update angle if the difference since last time is at least 1 degree
        if (Math.abs(mLastOrientationAngleSent - azimuth) >= 1.0f) {
            // tell the graphics view that the device angle has changed
            mGraphicsView.setGlobalAngle(azimuth);
            mMapView.setGlobalAngle(azimuth);
            // tell the server that the device angle has changed
            if (mFlareManager != null && mDevice != null) {
                mFlareManager.setData(mDevice, "angle", azimuth, mDevice);
            }

            mLastOrientationAngleSent = azimuth;
        }
    }
}
