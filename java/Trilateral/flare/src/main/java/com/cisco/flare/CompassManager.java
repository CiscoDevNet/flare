package com.cisco.flare;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by azamlerc on 12/29/15.
 */
public class CompassManager implements SensorEventListener {

	public interface Delegate {
		// this function will be called if the device angle has
		// changed by more than 5°, at most every 1 second
		public void deviceAngleChanged(float azimuth);
	}

	private Delegate delegate;

	private float increment = 5.0f; // will round to the nearest 5°
	private float period = 1.0f; // messages will be sent at most every 1 second

	private SensorManager mSensorManager;
	private Sensor mRotationVectorSensor;
	private float mOrientation[] = new float[3];
	private float[] mInR3x3 = new float[9];
	private boolean mScreenReady = true;
	float mLastAngleValue = -1.0f;
	long mLastAngleTime = -1;

	private static final String TAG = "CompassManager";

	public CompassManager(SensorManager manager) {
		// the rotation vector sensor is null if we're on the emulator
		// or if use_orientation_sensor is set to false in options.xml
		boolean useOrientationSensor = true; // get from prefs // getResources().getBoolean(R.bool.use_orientation_sensor);
		if (useOrientationSensor) {
			mSensorManager = manager;
			mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
			// if we failed to get the rotation sensor working, reset useOrientationSensor to false
			if (mRotationVectorSensor == null) {
				useOrientationSensor = false;
				Log.d(TAG, "mRotationVectorSensor is null, so this must be the emulator");
			} else {
				mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}

		// the listeners for the sensor manager are being registered in onResume
	}

	public void setDelegate(Delegate value) {
		this.delegate = value;
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
		while (azimuth < 0) { azimuth += 360; } // make sure it's positive
		azimuth = Math.round(azimuth / 5.0f) * 5.0f; // round to nearest 5°

		// if the rounded reading is different from last time and it's been at least period seconds
		if (delegate != null && azimuth != mLastAngleValue && System.nanoTime() - mLastAngleTime >= period * 1000000000) {
			delegate.deviceAngleChanged(Math.round(azimuth / increment) * increment);

			mLastAngleValue = azimuth;
			mLastAngleTime = System.nanoTime();
		}
	}

	public void pause() {
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this, mRotationVectorSensor);
		}
	}

	public void resume() {
		if (mSensorManager != null) {
			mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
}
