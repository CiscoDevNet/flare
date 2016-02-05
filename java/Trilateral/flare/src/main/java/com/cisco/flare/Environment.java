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
// TODO: make Environment, Flare, Thing and Zone implement Parcelable
// in order to be able to pass custom objects between Android activities
// more info: http://sohailaziz05.blogspot.com/2012/04/passing-custom-objects-between-android.html
public class Environment extends Flare implements Flare.PerimeterObject {

	private Geofence geofence;
	private RectF perimeter;
	private double angle;
	private String uuid;
	private String shortUuid1;
	private String shortUuid2;
	private ArrayList<Zone> zones;
	private ArrayList<Device> devices;

	public Environment(JSONObject json) {
		super(json);
		this.zones = new ArrayList<Zone>();
		this.devices = new ArrayList<Device>();

		try { this.setUuid(this.data.getString("uuid")); } catch (Exception e) { }
		try { this.setGeofence(new Geofence(json.getJSONObject("geofence"))); } catch (Exception e) {}
		try { this.setPerimeter(getRect(json.getJSONObject("perimeter"))); } catch (Exception e) {}
		try { this.setAngle(json.getDouble("angle")); } catch (Exception e) {}

		try {
			JSONArray zoneArray = json.getJSONArray("zones");
			for (int i = 0; i < zoneArray.length(); i++) {
				JSONObject zoneJson = zoneArray.getJSONObject(i);
				this.zones.add(new Zone(zoneJson));
			}
		} catch (Exception e) {}
	}

	@Override
	public String toString() {
		return super.toString() + " - " + perimeter;
	}

	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		try { json.put("geofence", this.geofence.toJSON()); } catch (Exception e) {}
		try { json.put("perimeter", Flare.rectToJSON(this.perimeter)); } catch (Exception e) {}
		try { json.put("angle", this.angle); } catch (Exception e) {}
		try { json.put("zones", Flare.arrayToJSON(this.zones)); } catch (Exception e) {}
		// does not include devices
		return json;
	}

	public Geofence getGeofence() {
		return geofence;
	}

	public void setGeofence(Geofence geofence) {
		this.geofence = geofence;
	}

	public RectF getPerimeter() {
		return perimeter;
	}

	public void setPerimeter(RectF perimeter) {
		this.perimeter = perimeter;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public String getUuid() {
		return uuid;
	}

	public String getShortUuid1() {
		return shortUuid1;
	}

	public String getShortUuid2() {
		return shortUuid2;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;

		// shortUuid1 is the first five and last five bytes of the UUID
		// shortUuid2 is the first four and last six bytes of the UUID
		// http://developer.radiusnetworks.com/2015/07/14/introducing-eddystone.html

		if (uuid != null) {
			String id = uuid.replace("-", ""); // remove hyphens
			if (id.length() >= 20) {
				this.shortUuid1 = id.substring(0, 10) + id.substring(id.length() - 10);
				this.shortUuid2 = id.substring(0, 8) + id.substring(id.length() - 12);
			}
		}
	}

	public ArrayList<Zone> getZones() {
		return zones;
	}

	public ArrayList<Device> getDevices() {
		return devices;
	}

	public void resetDistances() {
		for (Zone zone: zones) {
			for (Thing thing: zone.getThings()) {
				thing.setDistance(-1);
			}
		}
	}

	public Zone getZoneWithId(String id) {
		for (Zone zone: zones) {
			if (zone.getId().equals(id)) {
				return zone;
			}
		}
		return null;
	}

	public Thing getThingWithId(String id) {
		for (Zone zone: zones) {
			for (Thing thing: zone.getThings()) {
				if (thing.getId().equals(id)) {
					return thing;
				}
			}
		}
		return null;
	}

	public Thing getThingForBeacon(int major, int minor) {
		for (Zone zone: zones) {
			if (zone.getMajor() == major) {
				for (Thing thing : zone.getThings()) {
					if (thing.getMinor() == minor) {
						return thing;
					}
				}
			}
		}
		return null;
	}

	public PointF userLocation(boolean useSquare) {
		float total = 0.0f;
		float x = 0.0f;
		float y = 0.0f;

		for (Zone zone: zones) {
			for (Thing thing : zone.getThings()) {
				double weight = useSquare ? thing.getInverseSquareDistance() : thing.getInverseDistance();
				if (weight > 0) {
					x += thing.getPosition().x * weight;
					y += thing.getPosition().y * weight;
					total += weight;
				}
			}
		}

		if (total == 0.0) return null;

		return new PointF(x / total, y / total);
	}
}
