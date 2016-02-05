package com.cisco.flare.trilateral;

import android.graphics.PointF;

/**
 * Created by ofrebour on 01/04/15.
 */
public class TouchInformation {
    public boolean screenTouched;
    public PointF coordinates;
//    public DrawableThing thing;

    public TouchInformation() {
        screenTouched = false;
        coordinates = new PointF();
//        thing = null;
    }

    public String getCoordinates() {
        return coordinates.x + ", " + coordinates.y;
    }
}
