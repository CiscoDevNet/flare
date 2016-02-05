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

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cisco.flare.trilateral.common.Constants;
import com.cisco.flare.trilateral.DrawableThing;
import com.cisco.flare.trilateral.Scene;
import com.cisco.flare.Thing;
import com.cisco.flare.trilateral.common.HTMLColors;
import com.cisco.flare.trilateral.common.ScreenDensity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class CompassView extends SurfaceView {

	private static String TAG = "CompassView";

	private Paint piePaint;
	private FullScreenAnimationThread animationThread;
	public Scene myScene;
	private float globalAngle;
	private float environmentAngle;
	private RectF rectF;
	protected TouchInformation touchInfo;
	private Paint touchCirclePaint;
	private float finStrokeWidth = Constants.FLARE_FIN_ARC_STROKE_WIDTH;
	private float nearFinStrokeWidth = Constants.FLARE_FIN_ARC_STROKE_WIDTH * 2.0f;
	protected boolean handleTouchEvents = false;

	public CompassView(Context context) {
		super(context);
		init();
	}

	public CompassView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CompassView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void setHandleTouchEvents(boolean value) {
		handleTouchEvents = value;
	}

	public synchronized void setGlobalAngle(float a) {
//        System.out.println("!angle: "+a);
//        Canvas canvas = surfaceHolder.lockCanvas();
//        synchronized (surfaceHolder) {
		globalAngle = a;
//        }
//        surfaceHolder.unlockCanvasAndPost(canvas);
	}

	private void init() {
		globalAngle = 0.0f;

		myScene = new Scene();
		touchInfo = new TouchInformation();

		handleTouchEvents = true;
		addSceneCallback();

		setZOrderOnTop(true);
		final CompassView thisView = this;
		SurfaceHolder surfaceHolder = getHolder();
		surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
		surfaceHolder.addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d("FullScreenView", "surfaceCreated");

				rectF = new RectF();
				updateFinStrokeWidth(false, finStrokeWidth);

				touchCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				touchCirclePaint.setStrokeWidth(finStrokeWidth);
				touchCirclePaint.setStyle(Paint.Style.STROKE);
				touchCirclePaint.setColor(0x88f000ff);

				piePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				piePaint.setStrokeWidth(1);
				piePaint.setStyle(Paint.Style.STROKE);
				piePaint.setColor(0x50ffffff);

				animationThread = new FullScreenAnimationThread(thisView);
				animationThread.setRunning(true);
				animationThread.start();
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder,
									   int format, int width, int height) {
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				boolean retry = true;
				animationThread.setRunning(false);
				while (retry) {
					try {
						animationThread.join();
						retry = false;
						animationThread = null;
					} catch (InterruptedException e) {
					}
				}
			}
		});
	}

	public synchronized void updateFinStrokeWidth(boolean nearFin, float w) {
//        Canvas canvas = surfaceHolder.lockCanvas();
//
//        if (canvas != null) {
//            synchronized (surfaceHolder) {
		if (nearFin) {
			nearFinStrokeWidth = ScreenDensity.dpsToPixels(w);
		}
		else {
			finStrokeWidth = ScreenDensity.dpsToPixels(w);
			float halfStrokeWidth = finStrokeWidth / 2;
			float rectWidth = getWidth() - halfStrokeWidth,
					rectHeight = getHeight() - halfStrokeWidth;
			rectF.set(halfStrokeWidth, halfStrokeWidth, rectWidth, rectHeight);
		}
//            }
//            surfaceHolder.unlockCanvasAndPost(canvas);
//        }
	}

	public synchronized void setEnvAngle(double angle) {
		environmentAngle = (float)angle;
	}

	public synchronized void setUserPosition(PointF userPosition) {
		myScene.setUserPosition(userPosition);
	}

	public synchronized void replaceObjects(ArrayList<Thing> things) {
		myScene.replaceObjects(things);
	}

	protected synchronized void drawScene(Canvas canvas) {
		// draw the background (clear the screen)
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);

		// draw the objects
		myScene.draw(rectF, canvas, environmentAngle + globalAngle, touchInfo, finStrokeWidth, nearFinStrokeWidth);
	}

	public void selectThing(Thing thing) {
		myScene.selectThing(thing);
	}

	private void addSceneCallback() {

		// when the selection changes, notify the wearable and the mobile
		// In each case, CommonDrawingActivity will get the notification and update the view accordingly
		myScene.addSceneChangedCallback(new Scene.SceneChangedCallback() {
			@Override
			public void selectionChanged(final Thing thing) {
				setNearbyThing(thing);
			}
		});
	}

	private void setNearbyThing(final Thing thing) {
		if (thing != null) {
			Log.d(TAG, "Selected thing: " + thing.getName());
			final WearActivity activity = (WearActivity)getContext();

			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					activity.setNearbyThing(thing);
				}
			});
		}
	}

	public void updateColor(Thing thing) {
		myScene.updateColor(thing);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!handleTouchEvents) {
			return false;
		}

		touchInfo.coordinates.x = event.getX();
		touchInfo.coordinates.y = event.getY();
		DrawableThing touchedThing = myScene.getTouchedThing();
		String thingName = " "+(touchedThing == null ? "null" : touchedThing.getName());

		if (MotionEvent.ACTION_DOWN == event.getAction()) {
			Log.d("onTouchEvent", "DOWN " + touchInfo.getCoordinates() + thingName);
			touchInfo.screenTouched = true;
		}
		else if (MotionEvent.ACTION_UP == event.getAction()) {
			Log.d("onTouchEvent", "  UP " + touchInfo.getCoordinates() + thingName);

			// find the first object being touched (there could be more than one if they overlap)
			// and display information about this object
			myScene.selectTouchedThing();

			touchInfo.screenTouched = false;

			// check if the user dropped something in the drag&drop area of the screen
			if (touchedThing != null) {
				PointF center = new PointF(this.getWidth() / 2, this.getHeight() / 2);
				double distanceFromMiddleOfScreen = DrawableThing.distanceBetweenTwoPoints(touchInfo.coordinates, center);
				Log.d("distance", "to "+center.x+","+center.y+" = "+distanceFromMiddleOfScreen);
				if (distanceFromMiddleOfScreen <= Constants.DRAG_DROP_RADIUS) {
					Log.d(TAG, "Swiped thing: " + touchedThing);

				}
			}
		}
		else if (MotionEvent.ACTION_MOVE == event.getAction()) {
			Log.d("onTouchEvent", "MOVE " + touchInfo.getCoordinates() + thingName);
			// nothing to do here, screenBeingTouched is already true
		}
		else
			Log.d("onTouchEvent", "action "+event.getAction()+" not handled");

		return touchedThing != null;
	}
}
