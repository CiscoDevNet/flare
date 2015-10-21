package com.cisco.flare;

import android.graphics.Color;
import android.graphics.PointF;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by azamlerc on 3/23/15.
 */
public class Thing extends Flare {

    private final int defaultColor = 0xffffcd00;
    private static final String THING_TYPE_DEFAULT = "default";
    private static final String THING_TYPE_BEACON = "beacon";
    private static final String THING_TYPE_LIGHT = "light";

	private String type;
	protected PointF position;
	private double angle;
	private int minor = -1;


	private double distance = 1000.0;
	private double inverseDistance = 0.0;

    private Zone zone;

	public Thing(JSONObject json) {
		super(json);

		try {
			try {
                this.type = json.getString("type");
            }
            catch (Exception e) {
                this.type = THING_TYPE_DEFAULT;
            }
			try { this.position = getPoint(json.getJSONObject("position")); } catch (Exception e) { }
            try { this.angle = json.getInt("angle"); } catch (Exception e) { }
			try { this.minor = this.data.getInt("minor"); } catch (Exception e) { }

            // Log.d("Thing", "name: "+this.name+" - color: "+getColorString());
		} catch (Exception e) {
			Log.e("Flare", "Parse error: ", e);
		}
	}

    public Thing(Thing t) {
        super(t);
        this.type = t.type;
        this.position = t.position;
        this.minor = t.minor;
        this.angle = t.angle;
        this.zone = t.zone;
    }

	@Override
	public String toString() {
		return super.toString() + " - " + position;
	}

	public String getType() {
		return type;
	}

    public boolean isBeacon() {
        return type.equals(THING_TYPE_BEACON);
    }

	public void setType(String type) {
		this.type = type;
	}

	public PointF getPosition() {
		return position;
	}

	public void setPosition(PointF position) {
		this.position = position;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
		if (distance == -1 || distance == 0.0) this.inverseDistance = -1.0;
		else this.inverseDistance = 1.0 / distance;
	}

	public double getInverseDistance() {
		return inverseDistance;
	}

	public void setInverseDistance(double inverseDistance) {
		this.inverseDistance = inverseDistance;
	}
}
