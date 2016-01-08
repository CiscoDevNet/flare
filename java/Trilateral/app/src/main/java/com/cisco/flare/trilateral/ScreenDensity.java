package com.cisco.flare.trilateral;

import android.content.Context;

/**
 * ScreenDensity provides a function to transform dps to px depending on the screen density of the device used
 */
public class ScreenDensity {
    private static ScreenDensity ourInstance = new ScreenDensity();
    private static float scale = 1.0f;

    public static ScreenDensity getInstance() {
        return ourInstance;
    }

    // setScale is called only once at the beginning of CommonDrawingActivity (which has access to the context of the application)
    public void setScale(Context context) {
        scale = context.getResources().getDisplayMetrics().density;
        // for some reason the scale on the wearable is shown as 1.5 and it should be 1.0
        // it is correctly 3.0 on the mobile
        if (scale < 2) { scale = 1; }
    }

    private ScreenDensity() {}

    public static float dpsToPixels(float dps) {
        float px = dps * scale + 0.5f;
        return px;
    }
    public static int dpsToPixels(int dps) {
        return (int)dpsToPixels((float)dps);
    }

    public static float screenDimension(float dim) {
        return dim / scale;
    }
}
