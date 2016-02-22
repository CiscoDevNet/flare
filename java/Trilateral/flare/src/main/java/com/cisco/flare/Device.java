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

    protected Zone currentZone;
    protected Thing nearbyThing;

    public Device(JSONObject json) {
        super(json);

        try {
            try { this.position = getPoint(json.getJSONObject("position")); } catch (Exception e) { this.position = new PointF(0,0); }
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

    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try { json.put("position", Flare.pointToJSON(this.position)); } catch (Exception e) {}
        try { if (currentZone != null) json.put("zoneId", currentZone.getId()); } catch (Exception e) {}
        try { if (nearbyThing != null) json.put("nearbyThingId", nearbyThing.getId()); } catch (Exception e) {}
        return json;
    }

    public double getAngle() {
        try {
            return getData().getDouble("angle");
        } catch (Exception e) {
            return 0;
        }
    }

    public PointF getPosition() {
        return position;
    }

    public void setPosition(PointF position) {
        this.position = position;
    }

    public double distanceTo(Thing thing) {
        if (position == null || thing.getPosition() == null) return 0;
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

    public Zone getCurrentZone() {
        return currentZone;
    }

    public void setCurrentZone(Zone value) {
        this.currentZone = value;
    }

    public Thing getNearbyThing() {
        return nearbyThing;
    }

    public void setNearbyThing(Thing value) {
        this.nearbyThing = value;
    }
}
