package com.cisco.flare.trilateral;

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
import java.util.HashMap;

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
                if (mDataChanged) {
                    try {
                        c = mSurfaceHolder.lockCanvas(null);
                        synchronized (mSurfaceHolder) {
                            if (mFlareVariablesChanged) {
                                onFlareVariablesChanged();
                                mFlareVariablesChanged = false;
                            }
                            if (c != null) {
                                mDataChanged = false;
                                doDraw(c);
                            }
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

                try {
                    Thread.sleep(500);
                } catch (Exception e) {}
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
    protected boolean mDataChanged;
    protected int mThingDefaultColor;

    protected ArrayList<Thing> mThingsNearDevice;

    public static HashMap<String, String> mHtmlColorNames;

    static {
        initHtmlColorNames();
    }

    public CommonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "new GraphicsView");

        mContext = context;
        mFlareVariablesChanged = false;
        mDataChanged = false;

        mThingsNearDevice = new ArrayList<>();

        // register our interest in hearing about changes to our surface
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        createThread();
    }

    private static void initHtmlColorNames() {
        mHtmlColorNames = new HashMap<>();
        mHtmlColorNames.put("aliceblue", "#F0F8FF");
        mHtmlColorNames.put("antiquewhite", "#FAEBD7");
        mHtmlColorNames.put("aqua", "#00FFFF");
        mHtmlColorNames.put("aquamarine", "#7FFFD4");
        mHtmlColorNames.put("azure", "#F0FFFF");
        mHtmlColorNames.put("beige", "#F5F5DC");
        mHtmlColorNames.put("bisque", "#FFE4C4");
        mHtmlColorNames.put("black", "#000000");
        mHtmlColorNames.put("blanchedalmond", "#FFEBCD");
        mHtmlColorNames.put("blue", "#0000FF");
        mHtmlColorNames.put("blueviolet", "#8A2BE2");
        mHtmlColorNames.put("brown", "#A52A2A");
        mHtmlColorNames.put("burlywood", "#DEB887");
        mHtmlColorNames.put("cadetblue", "#5F9EA0");
        mHtmlColorNames.put("chartreuse", "#7FFF00");
        mHtmlColorNames.put("chocolate", "#D2691E");
        mHtmlColorNames.put("coral", "#FF7F50");
        mHtmlColorNames.put("cornflowerblue", "#6495ED");
        mHtmlColorNames.put("cornsilk", "#FFF8DC");
        mHtmlColorNames.put("crimson", "#DC143C");
        mHtmlColorNames.put("cyan", "#00FFFF");
        mHtmlColorNames.put("darkblue", "#00008B");
        mHtmlColorNames.put("darkcyan", "#008B8B");
        mHtmlColorNames.put("darkgoldenrod", "#B8860B");
        mHtmlColorNames.put("darkgray", "#A9A9A9");
        mHtmlColorNames.put("darkgrey", "#A9A9A9");
        mHtmlColorNames.put("darkgreen", "#006400");
        mHtmlColorNames.put("darkkhaki", "#BDB76B");
        mHtmlColorNames.put("darkmagenta", "#8B008B");
        mHtmlColorNames.put("darkolivegreen", "#556B2F");
        mHtmlColorNames.put("darkorange", "#FF8C00");
        mHtmlColorNames.put("darkorchid", "#9932CC");
        mHtmlColorNames.put("darkred", "#8B0000");
        mHtmlColorNames.put("darksalmon", "#E9967A");
        mHtmlColorNames.put("darkseagreen", "#8FBC8F");
        mHtmlColorNames.put("darkslateblue", "#483D8B");
        mHtmlColorNames.put("darkslategray", "#2F4F4F");
        mHtmlColorNames.put("darkslategrey", "#2F4F4F");
        mHtmlColorNames.put("darkturquoise", "#00CED1");
        mHtmlColorNames.put("darkviolet", "#9400D3");
        mHtmlColorNames.put("deeppink", "#FF1493");
        mHtmlColorNames.put("deepskyblue", "#00BFFF");
        mHtmlColorNames.put("dimgray", "#696969");
        mHtmlColorNames.put("dimgrey", "#696969");
        mHtmlColorNames.put("dodgerblue", "#1E90FF");
        mHtmlColorNames.put("firebrick", "#B22222");
        mHtmlColorNames.put("floralwhite", "#FFFAF0");
        mHtmlColorNames.put("forestgreen", "#228B22");
        mHtmlColorNames.put("fuchsia", "#FF00FF");
        mHtmlColorNames.put("gainsboro", "#DCDCDC");
        mHtmlColorNames.put("ghostwhite", "#F8F8FF");
        mHtmlColorNames.put("gold", "#FFD700");
        mHtmlColorNames.put("goldenrod", "#DAA520");
        mHtmlColorNames.put("gray", "#808080");
        mHtmlColorNames.put("grey", "#808080");
        mHtmlColorNames.put("green", "#008000");
        mHtmlColorNames.put("greenyellow", "#ADFF2F");
        mHtmlColorNames.put("honeydew", "#F0FFF0");
        mHtmlColorNames.put("hotpink", "#FF69B4");
        mHtmlColorNames.put("indianred ", "#CD5C5C");
        mHtmlColorNames.put("indigo ", "#4B0082");
        mHtmlColorNames.put("ivory", "#FFFFF0");
        mHtmlColorNames.put("khaki", "#F0E68C");
        mHtmlColorNames.put("lavender", "#E6E6FA");
        mHtmlColorNames.put("lavenderblush", "#FFF0F5");
        mHtmlColorNames.put("lawngreen", "#7CFC00");
        mHtmlColorNames.put("lemonchiffon", "#FFFACD");
        mHtmlColorNames.put("lightblue", "#ADD8E6");
        mHtmlColorNames.put("lightcoral", "#F08080");
        mHtmlColorNames.put("lightcyan", "#E0FFFF");
        mHtmlColorNames.put("lightgoldenrodyellow", "#FAFAD2");
        mHtmlColorNames.put("lightgray", "#D3D3D3");
        mHtmlColorNames.put("lightgrey", "#D3D3D3");
        mHtmlColorNames.put("lightgreen", "#90EE90");
        mHtmlColorNames.put("lightpink", "#FFB6C1");
        mHtmlColorNames.put("lightsalmon", "#FFA07A");
        mHtmlColorNames.put("lightseagreen", "#20B2AA");
        mHtmlColorNames.put("lightskyblue", "#87CEFA");
        mHtmlColorNames.put("lightslategray", "#778899");
        mHtmlColorNames.put("lightslategrey", "#778899");
        mHtmlColorNames.put("lightsteelblue", "#B0C4DE");
        mHtmlColorNames.put("lightyellow", "#FFFFE0");
        mHtmlColorNames.put("lime", "#00FF00");
        mHtmlColorNames.put("limegreen", "#32CD32");
        mHtmlColorNames.put("linen", "#FAF0E6");
        mHtmlColorNames.put("magenta", "#FF00FF");
        mHtmlColorNames.put("maroon", "#800000");
        mHtmlColorNames.put("mediumaquamarine", "#66CDAA");
        mHtmlColorNames.put("mediumblue", "#0000CD");
        mHtmlColorNames.put("mediumorchid", "#BA55D3");
        mHtmlColorNames.put("mediumpurple", "#9370DB");
        mHtmlColorNames.put("mediumseagreen", "#3CB371");
        mHtmlColorNames.put("mediumslateblue", "#7B68EE");
        mHtmlColorNames.put("mediumspringgreen", "#00FA9A");
        mHtmlColorNames.put("mediumturquoise", "#48D1CC");
        mHtmlColorNames.put("mediumvioletred", "#C71585");
        mHtmlColorNames.put("midnightblue", "#191970");
        mHtmlColorNames.put("mintcream", "#F5FFFA");
        mHtmlColorNames.put("mistyrose", "#FFE4E1");
        mHtmlColorNames.put("moccasin", "#FFE4B5");
        mHtmlColorNames.put("navajowhite", "#FFDEAD");
        mHtmlColorNames.put("navy", "#000080");
        mHtmlColorNames.put("oldlace", "#FDF5E6");
        mHtmlColorNames.put("olive", "#808000");
        mHtmlColorNames.put("olivedrab", "#6B8E23");
        mHtmlColorNames.put("orange", "#FFA500");
        mHtmlColorNames.put("orangered", "#FF4500");
        mHtmlColorNames.put("orchid", "#DA70D6");
        mHtmlColorNames.put("palegoldenrod", "#EEE8AA");
        mHtmlColorNames.put("palegreen", "#98FB98");
        mHtmlColorNames.put("paleturquoise", "#AFEEEE");
        mHtmlColorNames.put("palevioletred", "#DB7093");
        mHtmlColorNames.put("papayawhip", "#FFEFD5");
        mHtmlColorNames.put("peachpuff", "#FFDAB9");
        mHtmlColorNames.put("peru", "#CD853F");
        mHtmlColorNames.put("pink", "#FFC0CB");
        mHtmlColorNames.put("plum", "#DDA0DD");
        mHtmlColorNames.put("powderblue", "#B0E0E6");
        mHtmlColorNames.put("purple", "#800080");
        mHtmlColorNames.put("rebeccapurple", "#663399");
        mHtmlColorNames.put("red", "#FF0000");
        mHtmlColorNames.put("rosybrown", "#BC8F8F");
        mHtmlColorNames.put("royalblue", "#4169E1");
        mHtmlColorNames.put("saddlebrown", "#8B4513");
        mHtmlColorNames.put("salmon", "#FA8072");
        mHtmlColorNames.put("sandybrown", "#F4A460");
        mHtmlColorNames.put("seagreen", "#2E8B57");
        mHtmlColorNames.put("seashell", "#FFF5EE");
        mHtmlColorNames.put("sienna", "#A0522D");
        mHtmlColorNames.put("silver", "#C0C0C0");
        mHtmlColorNames.put("skyblue", "#87CEEB");
        mHtmlColorNames.put("slateblue", "#6A5ACD");
        mHtmlColorNames.put("slategray", "#708090");
        mHtmlColorNames.put("slategrey", "#708090");
        mHtmlColorNames.put("snow", "#FFFAFA");
        mHtmlColorNames.put("springgreen", "#00FF7F");
        mHtmlColorNames.put("steelblue", "#4682B4");
        mHtmlColorNames.put("tan", "#D2B48C");
        mHtmlColorNames.put("teal", "#008080");
        mHtmlColorNames.put("thistle", "#D8BFD8");
        mHtmlColorNames.put("tomato", "#FF6347");
        mHtmlColorNames.put("turquoise", "#40E0D0");
        mHtmlColorNames.put("violet", "#EE82EE");
        mHtmlColorNames.put("wheat", "#F5DEB3");
        mHtmlColorNames.put("white", "#FFFFFF");
        mHtmlColorNames.put("whitesmoke", "#F5F5F5");
        mHtmlColorNames.put("yellow", "#FFFF00");
        mHtmlColorNames.put("yellowgreen", "#9ACD32");
        mHtmlColorNames.put("oak", "#DEB887");
        mHtmlColorNames.put("birch", "#F5DEB3");
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
            mDataChanged = true;
        }
    }

    public void dataChanged() {
        synchronized (mSurfaceHolder) {
            mDataChanged = true;
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
                    dataChanged();
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
                dataChanged();
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
                chosenColor = CommonView.getHtmlColor(thingColor);
            } catch (JSONException e) {
                // the color of this Thing wasn't specified so we'll go with the default one
            } catch (Exception e) {
                // this exception is being thrown when the color is invalid
                // Log.e(TAG, "Invalid color "+thingColor);
            }
        }
        return chosenColor;
    }

    public static int getHtmlColor(String name) {
        try {
            String colorName = mHtmlColorNames.get(name);
            return Color.parseColor(colorName);
        } catch (Exception e) {
            return Color.parseColor("white");
        }
    }

    protected float deviceAngle() {
        if (mEnvironment == null) return 0;
        return mDeviceAngle + (float)mEnvironment.getAngle();
    }
}
