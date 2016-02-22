package com.cisco.flare.trilateral;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.cisco.flare.Device;
import com.cisco.flare.Thing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ofrebour on 08/10/15.
 */
public class MapView extends CommonView {

    private final String TAG = "MapView";

    private Paint mPerimeterPaint;
    private Drawable mDrawableThing;
    private Rect mDrawableThingDimensions;
    private Drawable mDrawableDevice;
    private Rect mDrawableDeviceDimensions;
    private float mPerimeterToCanvasMultiplier = 1;
    private PointF mPerimeterOriginOnCanvas;
    private int mMapBgColor;
    private Paint mLinkPaint; // link between an object near the device

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "new MapView");
        mPerimeterOriginOnCanvas = new PointF(0,0);
    }

    public void initialiseSurfaceHolder(SurfaceHolder surfaceHolder) {
        surfaceHolder.setFormat(PixelFormat.OPAQUE);
    }

    public void onSurfaceCreated() {

        mPerimeterPaint = new Paint();
        mPerimeterPaint.setStrokeWidth(2);
        mPerimeterPaint.setStyle(Paint.Style.STROKE);
        mPerimeterPaint.setColor(Color.BLACK);

        mLinkPaint = new Paint();
        mLinkPaint.setStrokeWidth(10);
        mLinkPaint.setStyle(Paint.Style.STROKE);
        mLinkPaint.setColor(Color.BLUE);

        mDrawableThing = mContext.getResources().getDrawable(R.drawable.thing);
        mDrawableThingDimensions = new Rect(0, 0, mDrawableThing.getMinimumWidth(), mDrawableThing.getMinimumHeight());

        mDrawableDevice = mContext.getResources().getDrawable(R.drawable.device);
        mDrawableDeviceDimensions = new Rect(0, 0, mDrawableDevice.getMinimumWidth(), mDrawableDevice.getMinimumHeight());

        Resources res = mContext.getResources();
        mMapBgColor = res.getColor(R.color.map_bg_color);
    }

    // update mapping between the environment perimeter and the canvas size
    public void onFlareVariablesChanged() {
        // exit if mSelectedZone hasn't been initialised yet
        if (mSelectedZone == null)
            return;

        RectF perimeter = mSelectedZone.getPerimeter();
        // leave a margin on the canvas to be able to draw Things (half a Thing on each side) on the perimeter if needed
        float halfThingWidth = (float)mDrawableDeviceDimensions.width()*0.7f, // changed from 0.5f
              halfThingHeight = (float)mDrawableDeviceDimensions.height()*0.7f;
        float canvasWidth = (float)mCanvasWidth - 2*halfThingWidth,
              canvasHeight = (float)mCanvasHeight - 2*halfThingHeight;

        mPerimeterOriginOnCanvas.set(halfThingWidth, halfThingHeight);
        mPerimeterToCanvasMultiplier = canvasWidth / perimeter.width();
        float yMax = mPerimeterToCanvasMultiplier * perimeter.height();
        if (yMax <= canvasHeight) {
            // the map fits vertically
            mPerimeterOriginOnCanvas.set(halfThingWidth, halfThingHeight+(canvasHeight-yMax)/2);
        }
        else {
            // the map doesn't fit vertically, we need to recalculate the multiplier
            mPerimeterToCanvasMultiplier = canvasHeight / perimeter.height();
            float xMax = mPerimeterToCanvasMultiplier * perimeter.width();
            mPerimeterOriginOnCanvas.set(halfThingWidth + (canvasWidth - xMax) / 2, halfThingHeight);
        }
    }

    // draw map view. Note that the surface holder is already locked
    public void doDraw(Canvas canvas) {
        // draw the background (clear the screen)
        canvas.drawColor(mMapBgColor, PorterDuff.Mode.SRC);

        // Log.e(TAG, "doDraw");

        // draw perimeter
        if (mSelectedZone != null) {
            RectF perimeter = mSelectedZone.getPerimeter();
            float xMin = mPerimeterOriginOnCanvas.x,
                    yMin = mPerimeterOriginOnCanvas.y,
                    xMax = xMin + mPerimeterToCanvasMultiplier * perimeter.width(),
                    yMax = yMin + mPerimeterToCanvasMultiplier * perimeter.height();
            canvas.drawRect(xMin, yMin, xMax, yMax, mPerimeterPaint);

            // draw a link between the device and things it is near to
            for (Thing t : mThingsNearDevice) {
                drawLinkBetweenDeviceAndThing(canvas, t);
            }

            // draw things
            for (Thing thing : mSelectedZone.getThings()) {
                PointF position = thing.getPosition();
                mDrawableThing.setColorFilter(determineThingColor(thing), PorterDuff.Mode.SRC);
                drawAtObjectPosition(canvas, mDrawableThing, (int) position.x, (int) position.y);
            }
        }

        // draw device position and orientation
        if (mDevice != null) {
            canvas.save();
            PointF devicePosition = mDevice.getPosition();
            PointF devicePositionOnCanvas = positionInCanvas(devicePosition);
            canvas.rotate(deviceAngle(), (int) devicePositionOnCanvas.x, (int) devicePositionOnCanvas.y);
            drawAtCanvasPosition(canvas, mDrawableDevice, (int) devicePositionOnCanvas.x, (int) devicePositionOnCanvas.y);
            canvas.restore();
        }
    }

    private void drawLinkBetweenDeviceAndThing(Canvas canvas, Thing thing) {
        if (mDevice != null) {
            PointF devicePosition = positionInCanvas(mDevice.getPosition());
            PointF objectPosition = positionInCanvas(thing.getPosition());
            canvas.drawLine(devicePosition.x, devicePosition.y, objectPosition.x, objectPosition.y, mLinkPaint);
        }
    }

    private void drawAtCanvasPosition(Canvas canvas, Drawable object, int x, int y) {
        int halfWidth = object.getMinimumWidth()/2,
            halfHeight = object.getMinimumHeight()/2;
        object.setBounds(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight);
        object.draw(canvas);
    }

    private PointF positionInCanvas(PointF pos) {
        float xInCanvas = mPerimeterOriginOnCanvas.x+mPerimeterToCanvasMultiplier*pos.x;
        float yInCanvas = mCanvasHeight - (mPerimeterOriginOnCanvas.y+mPerimeterToCanvasMultiplier*pos.y);
        return new PointF(xInCanvas, yInCanvas);
    }

    private PointF positionFromCanvas(PointF pos) {
        float x = (pos.x-mPerimeterOriginOnCanvas.x)/mPerimeterToCanvasMultiplier;
        float y = ((mCanvasHeight - pos.y)-mPerimeterOriginOnCanvas.y)/mPerimeterToCanvasMultiplier;
        return new PointF(x, y);
    }

    private void drawAtObjectPosition(Canvas canvas, Drawable object, int x, int y) {
        int halfWidth = object.getMinimumWidth()/2,
            halfHeight = object.getMinimumHeight()/2;
        int xInCanvas = (int)(mPerimeterOriginOnCanvas.x+mPerimeterToCanvasMultiplier*x);
        int yInCanvas = mCanvasHeight - (int)(mPerimeterOriginOnCanvas.y+mPerimeterToCanvasMultiplier*y);
        object.setBounds(xInCanvas - halfWidth, yInCanvas - halfHeight, xInCanvas + halfWidth, yInCanvas + halfHeight);
        object.draw(canvas);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // ignore touches if we mFlareManager or mDevice aren't initialised
        if (mFlareManager == null || mDevice == null || mSelectedZone == null) return false;

        if (MotionEvent.ACTION_UP == event.getAction()) {
            PointF touchedCoords = new PointF(event.getX(), event.getY());
            RectF perimeter = mSelectedZone.getPerimeter();
            float xMin = mPerimeterOriginOnCanvas.x,
                    yMin = mPerimeterOriginOnCanvas.y,
                    xMax = xMin + mPerimeterToCanvasMultiplier * perimeter.width(),
                    yMax = yMin + mPerimeterToCanvasMultiplier * perimeter.height();
            if (touchedCoords.x >= xMin &&
                    touchedCoords.y >= yMin &&
                    touchedCoords.x <= xMax &&
                    touchedCoords.y <= yMax) {
                // transform from canvas to perimeter coordinates
                PointF position = positionFromCanvas(touchedCoords);

                Thing nearThing = thingNearPoint(position, 1.0f);
                if (nearThing != null) {
                    Log.d(TAG, "Selected: " + nearThing.getName());
                    final MainActivity activity = (MainActivity)getContext();
                    activity.near(nearThing, mDevice, mDevice.distanceTo(nearThing));
                }
            }
        }

        return true;
    }

    public Thing thingNearPoint(PointF point, float distance) {
        if (mSelectedZone == null) return null;

        for (Thing thing : mSelectedZone.getThings()) {
            if (thing.distanceTo(point) < distance) {
                return thing;
            }
        }

        return null;
    }
}
