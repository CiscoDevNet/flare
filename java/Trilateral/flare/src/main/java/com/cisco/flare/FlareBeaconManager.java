package com.cisco.flare;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RangedBeacon;

import java.util.Collection;

/**
 * Created by ofrebour on 19/05/15.
 */
public class FlareBeaconManager {
    private static final String TAG="FlareBeaconManager";

    public final static String KEY_PREF_BEACON_DEVICE = "pref_beacon_device";
    public final static String KEY_PREF_BEACON_DEVICE_NONE = "NONE";
    public final static String KEY_PREF_BEACON_DEVICE_MOBILE = "MOBILE";
    public final static String KEY_PREF_BEACON_DEVICE_WATCH = "WATCH";

    private static FlareBeaconManager thisInstance = new FlareBeaconManager();

    public interface Callback {
        public void onPositionUpdate(PointF pos);
    }

    private BeaconManager beaconManager;
    private Region beaconRegion;
    private boolean isEmulator = Build.HARDWARE.contains("goldfish");
    private String thisBeaconDevice = KEY_PREF_BEACON_DEVICE_WATCH;

    private Environment environment = null;

    // preferences - used to determine if we should start detecting beacons
    private String pref_beaconDevice;
    private float pref_beaconSmoothing;

    private Callback callback;

    private BeaconConsumer beaconConsumer;

    public FlareBeaconManager() {
        pref_beaconDevice = KEY_PREF_BEACON_DEVICE_MOBILE;
        pref_beaconSmoothing = 2.0f;
    }

    public static void setDeviceTypeAndConsumer(final String device, BeaconConsumer consumer) {
        thisInstance.thisBeaconDevice = device;
        thisInstance.beaconConsumer = consumer;
    }

    public static void setEnvironment(Environment value) {
        Log.d(TAG, "setEnvironment: " + value.getName());
        thisInstance.environment = value;
    }

    public static void setCallback(Callback value) {
        thisInstance.callback = value;
    }

    public static void setRangeNotifier() {
        if (thisInstance.beaconManager != null) {
            thisInstance.beaconManager.setRangeNotifier((Collection<Beacon> beacons, Region region) -> {
                // Log.d(TAG, "Found " + beacons.size() + " beacons in " + region);
                if (beacons.size() >= 3 && thisInstance.environment != null) {
                    thisInstance.environment.resetDistances(); // clears the distance for beacons that are no longer visible
                    String uuid = thisInstance.environment.getUuid();

                    for (Beacon beacon: beacons) {
                        if (uuid != null && uuid.equalsIgnoreCase(beacon.getId1().toString())) {
                            int minor = beacon.getId3().toInt();
                            double distance = beacon.getDistance();

                            Thing thing = thisInstance.environment.getThingWithMinor(minor);

                            if (thing != null) {
                                thing.setDistance(distance);
                                // Log.d(TAG, "Beacon " + thing.getName() + ": " + distance);
                            }
                        } else {
                            Log.d(thisInstance.TAG, "Unknown beacon: " + beacon.getId1());
                        }
                    }

                    final PointF location = thisInstance.environment.userLocation();
                    if (location != null && thisInstance.callback != null) {
                        thisInstance.callback.onPositionUpdate(location);
                    }
                }
            });

            thisInstance.startRangingBeacons();
        }
    }

    /**
     * Bind the context when the activity gets created (Activity.onCreate())
     * @param context activity this class is used from
     */
    public static void bind(Context context) {
        Log.d(TAG, "bind " + context);
        // thisInstance.beaconManager.setDebug(true);
        thisInstance.beaconRegion = new Region("myRangingUniqueId", null, null, null);
        thisInstance.beaconManager = BeaconManager.getInstanceForApplication(context);

        // Log.d(TAG, "Binding " + thisInstance.beaconManager + " to " + thisInstance.beaconConsumer);

        thisInstance.beaconManager.bind(thisInstance.beaconConsumer);

        // Log.d(TAG, "isBound? " + (thisInstance.beaconManager.isBound(thisInstance.beaconConsumer) ? "YES" : "NO"));

        // making sure that it's in the right mode as this could have been called after resume()
        thisInstance.beaconManager.setBackgroundMode(false);
    }

    /**
     * Unbind the context when the activity gets destroyed (Activity.onDestroy())
     */
    public static void unbind() {
        Log.d(TAG, "unbind");
        if (thisInstance.beaconManager != null) {
            thisInstance.beaconManager.unbind(thisInstance.beaconConsumer);
            thisInstance.beaconManager = null;
            thisInstance.beaconRegion = null;
        }
    }

    public static void pause() {
        Log.d(TAG, "FlareBeaconManager - pause");
        Log.d(TAG, "beaconManager="+thisInstance.beaconManager + " ; beaconConsumer="+thisInstance.beaconConsumer);
        if (thisInstance.beaconManager != null) Log.d(TAG, "isBound? "+(thisInstance.beaconManager.isBound(thisInstance.beaconConsumer)?"YES":"NO"));
        if (thisInstance.beaconManager != null && thisInstance.beaconManager.isBound(thisInstance.beaconConsumer))
            thisInstance.beaconManager.setBackgroundMode(true);
    }

    public static void resume() {
        Log.d(TAG, "FlareBeaconManager - resume");
        Log.d(TAG, "beaconManager="+thisInstance.beaconManager + " ; beaconConsumer="+thisInstance.beaconConsumer);
        if (thisInstance.beaconManager != null) Log.d(TAG, "isBound? "+(thisInstance.beaconManager.isBound(thisInstance.beaconConsumer)?"YES":"NO"));
        if (thisInstance.beaconManager != null && thisInstance.beaconManager.isBound(thisInstance.beaconConsumer))
            thisInstance.beaconManager.setBackgroundMode(false);
    }

    public static void updateBeaconPrefs(String beaconDevice, float beaconSmoothing) {
        thisInstance.pref_beaconDevice = beaconDevice;
        thisInstance.pref_beaconSmoothing = beaconSmoothing;
    }

    private void startRangingBeacons() {
        // if ranging beacons on the mobile, and not running in the emulator
        Log.d(TAG, "startRangingBeacons - thisBeaconDevice="+thisBeaconDevice+" ; pref_beaconDevice="+pref_beaconDevice+" ; isEmulator="+isEmulator);
        if (thisBeaconDevice.equals(pref_beaconDevice) && !isEmulator) {
            try {
                if (beaconManager != null && beaconRegion != null) {
                    if (!beaconManager.isBound(beaconConsumer)) {
                        Log.w(TAG, "Cannot start ranging beacons as beaconManager " + beaconManager + " not bound to " + beaconConsumer);
                        return;
                    }
                    Log.d(TAG, "Start ranging beacons.");
                    RangedBeacon.setSampleExpirationMilliseconds((long)(pref_beaconSmoothing * 1000));
                    beaconManager.startRangingBeaconsInRegion(beaconRegion);
                }
                else {
                    Log.w(TAG, "Cannot start ranging beacons as beaconManager is null");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error start ranging beacons.", e);
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Not ranging: " + thisBeaconDevice + " != " + pref_beaconDevice);
        }
    }

    private void stopRangingBeacons() {
        try {
            Log.d(TAG, "Stop ranging beacons.");
            if (beaconManager != null && beaconRegion != null) {
                if (beaconManager.isBound(beaconConsumer)) {
                    beaconManager.stopRangingBeaconsInRegion(beaconRegion);
                }
                else {
                    Log.w(TAG, "Cannot stop ranging beacons as beaconManager " + beaconManager + " not bound to " + beaconConsumer);
                }
            }
            else {
                Log.w(TAG, "Cannot stop ranging beacons as beaconManager is null");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error start ranging beacons.", e);
            e.printStackTrace();
        }
    }

    public static void restartRangingBeacons() {
        thisInstance.stopRangingBeacons();
        thisInstance.startRangingBeacons();
    }
}
