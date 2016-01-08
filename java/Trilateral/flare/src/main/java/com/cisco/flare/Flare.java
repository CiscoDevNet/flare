package com.cisco.flare;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by azamlerc on 3/23/15.
 */

public abstract class Flare {
	protected String id;
	protected String name;
	protected String description;
	protected JSONObject data;

	public interface PositionObject {
		public PointF getPosition();
		public void setPosition(PointF position);
	}

	public interface PerimeterObject {
		public RectF getPerimeter();
		public void setPerimeter(RectF perimeter);
	}

	public Flare() {

	}

	public Flare(JSONObject json) {
		try {
			this.id = json.getString("_id");
			this.name = json.getString("name");
		} catch (Exception e) {
			Log.e("Flare", "Parse error: ", e);
		}

		try { this.description = json.getString("description"); } catch (Exception e) { }
		try { this.data = json.getJSONObject("data"); } catch (Exception e) { }
	}

    public Flare(Flare f) {
        this.id = f.id;
        this.name = f.name;
        this.description = f.description;
        this.data = f.data;
    }

	public String toString() {
		return this.getClass().getSimpleName() + " " + this.id + " - " + this.name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public JSONObject idInfo() {
		JSONObject info = new JSONObject();
		String className = this.getClass().getSimpleName().toLowerCase();
		try {
			info.put(className, id);
		} catch (Exception e) {}
		return info;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public JSONObject getData() {
		return data;
	}

	public void setData(JSONObject data) {
		this.data = data;
	}

	public class Geofence {
		private double latitude;
		private double longitude;
		private double radius;

		public Geofence(JSONObject json) {
			try {
				this.latitude = json.getDouble("latitude");
				this.longitude = json.getDouble("longitude");
				this.radius = json.getDouble("radius");
			} catch (Exception e) {

			}
		}

		public String toString() {
			return this.latitude + "° - " + this.longitude + "° - " + this.radius + "m";
		}

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public double getRadius() {
			return radius;
		}

		public void setRadius(double radius) {
			this.radius = radius;
		}

		// TODO
		public boolean isInside(double userLatitude, double userLongitude) {
			return false;
		}
	}

	// Note that Android rect uses left, top, right, bottom
	public static RectF getRect(JSONObject json) {
		try {
			JSONObject origin = json.getJSONObject("origin");
			JSONObject size = json.getJSONObject("size");
			float x = (float)origin.getDouble("x");
			float y = (float)origin.getDouble("y");
			float width = (float)size.getDouble("width");
			float height = (float)size.getDouble("height");
            return new RectF(x, y, x + width, y + height);
		} catch (Exception e) {
			Log.e("Flare", "Parse error: ", e);
			return null;
		}
	}

	public static PointF getPoint(JSONObject json) {
		try {
			float x = (float)json.getDouble("x");
			float y = (float)json.getDouble("y");
			return new PointF(x, y);
		} catch (Exception e) {
			Log.e("Flare", "Parse error: ", e);
			return null;
		}
	}

	public static JSONObject pointToJSON(PointF point) {
		JSONObject json = new JSONObject();
		try { json.put("x", round(point.x)); } catch (Exception e) {}
		try { json.put("y", round(point.y)); } catch (Exception e) {}
		return json;
	}

	public static JSONObject zeroPoint() {
		JSONObject json = new JSONObject();
		try { json.put("x", 0.0); } catch (Exception e) {}
		try { json.put("y", 0.0); } catch (Exception e) {}
		return json;
	}

	public static double round(double value) {
		return (double)Math.round(value * 100) / 100;
	}
}
