package com.cisco.flare.trilateral;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cisco.flare.Thing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Round view replicating what Flare looks like on a smart watch
 */
public class GraphicsView extends CommonView {

    private final String TAG = "GraphicsView";

    private RectF mRectF;
    private RectF mNearRectF;
    private float mFinStrokeWidth;
    private float mNearStrokeWidth;
    private Paint mPaint;

    private final boolean showTicks = false;
    private final int mDefaultColor = 0xffffcd00;
    private final int mNorthColor = 0xffff0000;
    private final int mTickColor = 0xffffffff;

    // private final double LARGEST_FIN_LENGTH = 80, SMALLEST_FIN_LENGTH = 10;
    // private final double NEAR_DISTANCE = 2, FAR_DISTANCE = 10;
    private final double LARGEST_FIN_LENGTH = 40, SMALLEST_FIN_LENGTH = 5;
    private final double NEAR_DISTANCE = 1, FAR_DISTANCE = 12;
    private final double NEAR_OPACITY = 1, FAR_OPACITY = 0.25;

    private final static int FACTOR_TYPE_OPACITY = 0;
    private final static int FACTOR_TYPE_FIN_SIZE = 1;

    public GraphicsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "new GraphicsView");
    }

    public void initialiseSurfaceHolder(SurfaceHolder surfaceHolder) {
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    }

    public void onSurfaceCreated() {
        mRectF = new RectF();
        mNearRectF = new RectF();

        mFinStrokeWidth = ScreenDensity.getInstance().screenDimension(getResources().getDimension(R.dimen.default_stroke_width));
        mNearStrokeWidth = mFinStrokeWidth * 1.6f;

        updateFinStrokeWidth(mRectF, mFinStrokeWidth);
        updateFinStrokeWidth(mNearRectF, mNearStrokeWidth);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(20);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mDefaultColor);
    }

    // nothing to update
    public void onFlareVariablesChanged() {}

    // draw the Flare view. Note that the surface holder is already locked
    public void doDraw(Canvas canvas) {
        // draw the background (clear the screen)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);

        float arcStartAngle = 10, arcSweepAngle=20;
        float strokeWidth = mFinStrokeWidth;// + finWidthNearestDefault - nearFinStrokeWidth;
        mPaint.setStrokeWidth(strokeWidth);

        if (mSelectedZone != null && mDevice != null) {

            setGlobalAngle((float)mDevice.getAngle());

            // draw things
            for (Thing thing : mSelectedZone.getThings()) {
                mPaint.setColor(determineThingColor(thing));

                // set the opacity to the maximum before adjusting it based on distance
                mPaint.setAlpha(255);

                // calculate the length of the fin depending on the distance between the Thing and the device
                double distanceFromDevice = mDevice.distanceTo(thing);
                arcSweepAngle = (float)finSize(distanceFromDevice);

                // calculate the opacity of the fin depending on the distance from the device
                mPaint.setColor(adjustAlpha(mPaint.getColor(), (float) finOpacity(distanceFromDevice)));

                // draw the selected thing thicker
                boolean near = mDevice.getNearbyThing() == thing;
                mPaint.setStrokeWidth(near ? mNearStrokeWidth : mFinStrokeWidth);

                // calculate the angle (orientation) of the fin from the position of the device
                arcStartAngle = -(float)mDevice.angleTo(thing);
                arcStartAngle -= arcSweepAngle/2;

                // draw the fin for this Thing
                canvas.drawArc(near ? mNearRectF : mRectF, arcStartAngle - deviceAngle(), arcSweepAngle, false, mPaint);
            }
        }

        // draw tick marks
        mPaint.setStrokeWidth(mFinStrokeWidth);

        if (showTicks) {
            for (int i = 0; i < 16; i++) {
                mPaint.setColor(i == 12 ? mNorthColor : mTickColor); // north is red, others are white
                mPaint.setAlpha(i % 2 == 0 ? 128 : 64); // alternate 50% and 25% opaque
                canvas.drawArc(mRectF, (float)i * 22.5f - deviceAngle(), 1, false, mPaint); // every 22.5°
            }
        }
    }

    private Thing touchedThing;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mFlareManager == null || mDevice == null || mSelectedZone == null) return false;

        if (MotionEvent.ACTION_DOWN == event.getAction()) {
            PointF touch = new PointF(event.getX(), event.getY());
            double touchAngle = touchAngle(touch);
            touchedThing = thingNearAngle(touchAngle);
        } else if (MotionEvent.ACTION_UP == event.getAction()) {
            if (touchedThing != null) {
                Log.d(TAG, "Selected: " + touchedThing.getName());
                final MainActivity activity = (MainActivity)getContext();
                activity.near(touchedThing, mDevice, mDevice.distanceTo(touchedThing));
            }
        }

        return true;
    }

    private double touchAngle(PointF point) {
        double touchAngle = Math.atan2(point.x - mRectF.centerX(), point.y - mRectF.centerY());
        double touchDegrees = touchAngle * (180.0 / 3.14159) - 90;
        if (touchDegrees < 0) touchDegrees += 360;
        Log.d(TAG, "Touch: " + touchDegrees + "°");
        return touchDegrees;
    }

    private Thing thingNearAngle(double angle) {
        for (Thing thing : mSelectedZone.getThings()) {
            double distanceFromDevice = mDevice.distanceTo(thing);
            float sweep = (float)finSize(distanceFromDevice);

            float start = -(-(float)mDevice.angleTo(thing) - deviceAngle() + (sweep / 2.0f));
            while (start < 0) start+= 360f;
            while (start > 360f) start-= 360f;
            float end = start + sweep;

            Log.d(TAG, thing.getName() + " " + thing.getColor() + ": " + start + " - " + end);

            if (start < angle && angle < end) {
                return thing;
            }
        }
        return null;
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Calculates the fin size depending on the distance between the user and the object
     * @param distance  distance between user and object
     * @return fin size
     */
    private double finSize(final double distance) {
        return finFactor(FACTOR_TYPE_FIN_SIZE, distance);
    }

    /**
     * Calculates the fin opacity depending on the distance between the user and the object
     * @param distance  distance between user and object
     * @return fin opacity
     */
    private double finOpacity(final double distance) {
        return finFactor(FACTOR_TYPE_OPACITY, distance);
    }

    private double finFactor(int type, final double distance) {
        double nearValue, farValue;
        switch (type) {
            case FACTOR_TYPE_FIN_SIZE:
                nearValue = LARGEST_FIN_LENGTH;
                farValue = SMALLEST_FIN_LENGTH;
                break;
            case FACTOR_TYPE_OPACITY:
                nearValue = NEAR_OPACITY;
                farValue = FAR_OPACITY;
                break;
            default:
                return 0;
        }

        if (distance <= NEAR_DISTANCE) {
            return nearValue;
        }
        else if (distance >= FAR_DISTANCE) {
            return farValue;
        }

        double distRange = NEAR_DISTANCE - FAR_DISTANCE;
        double distFromFar = distance - FAR_DISTANCE;
        double sizeRange = nearValue - farValue;
        return farValue + (distFromFar/distRange)*sizeRange;
    }

    private void updateFinStrokeWidth(RectF rect, float width) {
        float halfStrokeWidth = width / 2;
        float rectWidth = getWidth() - halfStrokeWidth,
                rectHeight = getHeight() - halfStrokeWidth;
        rect.set(halfStrokeWidth, halfStrokeWidth, rectWidth, rectHeight);
    }


    public void updateUIElements(RelativeLayout timeLayout, RelativeLayout rimHoverInfo, TextView objectName, TextView objectDesc) {
        // if we're near a Thing, display its name
        if (mThingsNearDevice.size() > 0) {
            String name = "", description = "";
            // if we're near more than one object, simply display the number of near objects
            if (mThingsNearDevice.size() > 1) {
                name = "Multiple Things";
                description = mThingsNearDevice.size()+" objects near you";
                for (Thing t: mThingsNearDevice) {
                }
            }
            else {
                Thing thing = mThingsNearDevice.get(0);
                name = thing.getName();
                description = thing.getDescription();
            }
            objectName.setText(name.toUpperCase());
            objectDesc.setText(description);

            timeLayout.setVisibility(View.INVISIBLE);
            rimHoverInfo.setVisibility(View.VISIBLE);
        }
        else {
            timeLayout.setVisibility(View.VISIBLE);
            rimHoverInfo.setVisibility(View.INVISIBLE);
        }
    }
}
