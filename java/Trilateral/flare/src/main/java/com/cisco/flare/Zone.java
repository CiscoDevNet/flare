package com.cisco.flare;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by azamlerc on 3/23/15.
 */
public class Zone extends Flare implements Flare.PerimeterObject {

	private RectF perimeter;
	private int major;
    private JSONArray actions;
	private ArrayList<Thing> things;

	public Zone(JSONObject json) {
		super(json);
		this.things = new ArrayList<Thing>();

		try {
			this.perimeter = getRect(json.getJSONObject("perimeter"));
			try { this.major = this.data.getInt("major"); } catch (Exception e) { }

			try { actions = json.getJSONArray("actions"); } catch (Exception e) {}
            if (actions == null) {
                actions = new JSONArray();
            }
		} catch (Exception e) {
			Log.e("Flare", "Parse error: ", e);
		}
	}

	@Override
	public String toString() {
		return super.toString() + " - " + perimeter;
	}

	public RectF getPerimeter() {
		return perimeter;
	}

	public void setPerimeter(RectF perimeter) {
		this.perimeter = perimeter;
	}

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public JSONArray getActions() {
		return actions;
	}

	public ArrayList<Thing> getThings() {
		return things;
	}


}
