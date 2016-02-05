package com.cisco.flare.trilateral;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.cisco.flare.Thing;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by ofrebour on 25/03/15.
 */
public class Scene {
    private static final String TAG = "Scene";
    private Vector<DrawableThing> objects;
    private PointF userPosition;
    private SceneChangedCallback sceneChangedCallback;
    private DrawableThing touchedThing = null;
    private DrawableThing selectedThing = null;

    public Scene() {
        objects = new Vector<>();
        userPosition = new PointF(0, 0);
        selectedThing = null;
        touchedThing = null;
        sceneChangedCallback = null;
    }

	public void setUserPosition(PointF value) {
        userPosition = value;
    }

    // the calling function (in FullScreenView) already locks the canvas
    // to make sure that we're not modifying the scene while trying to draw it
    public void replaceObjects(ArrayList<Thing> things) {
        objects.clear();

        // reset all the flags to make sure we don't access an index outside the boundaries of the array
        selectedThing = null;

        // make a DrawableThing subclass from each thing
        for (int i = 0 ; i < things.size() ; i++) {
            objects.add(new DrawableThing(things.get(i)));
        }
    }

     public void draw(RectF rectF, Canvas canvas, final float globalAngle, final TouchInformation touchInfo,
                     final float finStrokeWidth, final float nearFinStrokeWidth) {
         touchedThing = null;
         for (int i = 0 ; i < objects.size() ; i++) {
            DrawableThing thing = objects.elementAt(i);

            if (thing.isBeingTouched() && thing != touchedThing && thing != selectedThing) {
                touchedThing = thing;
                if (sceneChangedCallback != null) {
                    sceneChangedCallback.selectionChanged(touchedThing.getThing());
                }
            }

            thing.update(rectF, userPosition, globalAngle, touchInfo);
        }

        for (int i = 0 ; i < objects.size() ; i++) {
            DrawableThing thing = objects.elementAt(i);
            float strokeWidth = thing == selectedThing ? nearFinStrokeWidth : finStrokeWidth;
            thing.drawOn(rectF, canvas, strokeWidth);
        }
    }

    public DrawableThing getTouchedThing() {
        return touchedThing;
    }

    public void selectTouchedThing() {
        if (touchedThing != null) {
            selectedThing = touchedThing;
            touchedThing = null;
        }
    }

    public void selectThing(Thing thing) {
        if (thing != null) {
            for (int i = 0; i < objects.size(); i++) {
                DrawableThing drawableThing = objects.get(i);
                if (drawableThing.getId().equals(thing.getId())) {
                    Log.d(TAG, "Selecting thing: " + thing.getName());
                    selectedThing = drawableThing;
                    break;
                }
            }
        }
    }

    public interface SceneChangedCallback {
        /**
         * This is called when the scene has changed.
         *
         * @param thing The currently selected Thing.
         */
         void selectionChanged(Thing thing);
    }

    public void addSceneChangedCallback(Scene.SceneChangedCallback callback) {
        sceneChangedCallback = callback;
    }

    public void updateColor(Thing thing) {
        Thing drawableThing = findObjectWithId(thing.getId());
        if (drawableThing != null) {
            String color = thing.getColor();
            drawableThing.setColor(color);
        }
    }
    public Thing findObjectWithId(String id) {
        Thing thing = null;
        for (int i = 0 ; i < objects.size() ; i++) {
            Thing thingAtIndex = objects.elementAt(i);
            if (thingAtIndex.getId().equals(id)) {
                thing = thingAtIndex;
                break;
            }
        }
        return thing;
    }
}
