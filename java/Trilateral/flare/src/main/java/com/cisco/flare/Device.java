package com.cisco.flare;

import android.graphics.Color;
import android.graphics.PointF;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by azamlerc on 3/23/15.
 */
public class Device extends Flare implements Flare.PositionObject {

    protected PointF position;

    public Device(JSONObject json) {
        super(json);

        try {
            try { this.position = getPoint(json.getJSONObject("position")); } catch (Exception e) { }

            // Log.d("Device", "name: "+this.name+" - color: "+getColorString());
        } catch (Exception e) {
            Log.e("Flare", "Parse error: ", e);
        }
    }

    public Device(Device d) {
        super(d);
        this.position = d.position;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + position;
    }

    public PointF getPosition() {
        return position;
    }

    public void setPosition(PointF position) {
        this.position = position;
    }

    public double distanceTo(Thing thing) {
        double dy = thing.getPosition().y - position.y;
        double dx = thing.getPosition().x - position.x;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public double angleTo(Thing thing) {
        double dy = thing.getPosition().y - position.y;
        double dx = thing.getPosition().x - position.x;
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;
        return angle;
    }

}
