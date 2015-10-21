package com.cisco.ctao.ioe.ux.trilateral;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cisco.flare.Device;
import com.cisco.flare.Environment;
import com.cisco.flare.FlareManager;
import com.cisco.flare.Thing;
import com.cisco.flare.Zone;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by ofrebour on 09/10/15.
 */
public abstract class CommonView extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = "CommonView";

    /**
     * Placeholder to initialise variables or anything specific to the SurfaceHolder
     *  e.g. setting its format (e.g. surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);)
     * @param holder    SurfaceHolder to initialise
     */
    public abstract void initialiseSurfaceHolder(SurfaceHolder holder);

    /**
     * Draw on the canvas
     * @param c Canvas to draw on
     */
    public abstract void doDraw(Canvas c);

    /**
     * Function called when the Flare manager or the environment or the zone has changed
     */
    public abstract void onFlareVariablesChanged();

    /**
     * Function called when the surface has been created
     */
    public abstract void onSurfaceCreated();

    protected class CommonViewThread extends Thread {

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        public CommonViewThread(SurfaceHolder surfaceHolder) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;

            setZOrderOnTop(true);
            initialiseSurfaceHolder(mSurfaceHolder);
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mFlareVariablesChanged) {
                            onFlareVariablesChanged();
                            mFlareVariablesChanged = false;
                        }
                        if (c != null) doDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
            Log.d(TAG, "finishing running thread");
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
            }
        }
    }

    /**
     * Current height of the surface/canvas.
     */
    protected int mCanvasHeight = 1;

    /**
     * Current width of the surface/canvas.
     */
    protected int mCanvasWidth = 1;

    /** Handle to the application context, used to e.g. fetch Drawables. */
    protected Context mContext;

    /** Handle to the surface manager object we interact with */
    private SurfaceHolder mSurfaceHolder;

    /** The thread that actually draws the animation */
    private CommonViewThread mThread;

    private float mDeviceAngle = 0.0f;
    protected FlareManager mFlareManager;
    protected Environment mEnvironment;
    protected Zone mSelectedZone;
    protected Device mDevice;
    protected boolean mFlareVariablesChanged;
    protected int mThingDefaultColor;

    protected ArrayList<Thing> mThingsNearDevice;

    public CommonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "new GraphicsView");

        mContext = context;
        mFlareVariablesChanged = false;

        mThingsNearDevice = new ArrayList<>();

        // register our interest in hearing about changes to our surface
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        createThread();
    }

    private void createThread() {
        mThread = new CommonViewThread(mSurfaceHolder);
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d(TAG, "surface changed");
        mThread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surface created");

        mThingDefaultColor = mContext.getResources().getColor(R.color.map_thing_default_color);

        onSurfaceCreated();

        // recreate thread if necessary
        if (mThread == null) {
            createThread();
        }

        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        mThread.setRunning(true);
        try {
            mThread.start();
        } catch (IllegalThreadStateException e) {
            Log.w(TAG, "Thread is already started");
        }
        Log.d(TAG, "thread started");
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "destroying surface...");
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        mThread.setRunning(false);
        while (retry) {
            try {
                mThread.join(1000);
                retry = false;
            } catch (InterruptedException e) {
            }
            finally {
                mThread.interrupt();
                mThread = null;
            }
        }
        Log.v(TAG, "surface destroyed");
    }

    public synchronized void setGlobalAngle(float a) {
        mDeviceAngle = a;
    }

    public void updateFlareView(FlareManager flareManager, Environment env, Zone zone, Device device) {
        synchronized (mSurfaceHolder) {
            mFlareManager = flareManager;
            mEnvironment = env;
            mSelectedZone = zone;
            mDevice = device;
            mFlareVariablesChanged = true;
        }
    }

    /**
     * Notification from FlareManager when a Thing and a Device are near each other
     * @param thing
     * @param device
     * @param distance
     */
    public void near(Thing thing, Device device, double distance) {
        Log.d(TAG, device.getName() + " is near to " + thing.getName());
        if (device == mDevice) {
            for (Thing t : mSelectedZone.getThings()) {
                if (t == thing) {
                    Log.i(TAG, "found thing the device is near to");
                    synchronized (mSurfaceHolder) {
                        // find if this is already in the array (to avoid duplications)
                        if (!mThingsNearDevice.contains(thing)) {
                            mThingsNearDevice.add(thing);
                        }
                    }
                    break;
                }
            }
//            Log.v(TAG, "finished searching for thing");
        }
        else {
            Log.v(TAG, "near: device is different " + device.getId() + " " + mDevice.getId());
        }
    }

    /**
     * Notification from FlareManager when a Thing and a Device are far from each other
     * @param thing
     * @param device
     */
    public void far(Thing thing, Device device) {
        Log.d(TAG, device.getName() + " is far from " + thing.getName());
        int i = 0;
        for (Thing t : mThingsNearDevice) {
            if (t == thing) {
                synchronized (mSurfaceHolder) {
                    mThingsNearDevice.remove(i);
                }
                break;
            }
            i++;
        }
    }

    /**
     * Checks the Thing's data and checks if it has a color defined.
     * If the color is unavailable or cannot be turned into rgb values, we return the default color.
     * @param thing
     * @return int (chosen color)
     */
    protected int determineThingColor(Thing thing) {
        int chosenColor = mThingDefaultColor;
        JSONObject data = thing.getData();
        if (data != null) {
            String thingColor = "unknown";
            try {
                thingColor = data.getString("color");
                chosenColor = Color.parseColor(thingColor);
            } catch (JSONException e) {
                // the color of this Thing wasn't specified so we'll go with the default one
            } catch (Exception e) {
                // this exception is being thrown when the color is invalid
//              Log.e(TAG, "Invalid color "+thingColor);
            }
        }
        return chosenColor;
    }

    protected float deviceAngle() {
        if (mEnvironment == null) return 0;
        return mDeviceAngle + (float)mEnvironment.getAngle();
    }
}
