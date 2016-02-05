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

			try {
				JSONArray thingArray = json.getJSONArray("things");
				for (int i = 0; i < thingArray.length(); i++) {
					JSONObject thingJson = thingArray.getJSONObject(i);
					this.things.add(new Thing(thingJson));
				}
			} catch (Exception e) {}

		} catch (Exception e) {
			Log.d("Zone", "Error parsing zone.");
		}
	}

	@Override
	public String toString() {
		return super.toString() + " - " + perimeter;
	}

	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		try { json.put("perimeter", Flare.rectToJSON(this.perimeter)); } catch (Exception e) {}
		try { json.put("things", Flare.arrayToJSON(this.things)); } catch (Exception e) {}
		return json;
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
