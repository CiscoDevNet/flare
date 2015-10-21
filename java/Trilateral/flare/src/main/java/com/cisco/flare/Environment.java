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
public class Environment extends Flare {

	private Geofence geofence;
	private RectF perimeter;
	private double angle;
	private String uuid;
	private ArrayList<Zone> zones;
	private ArrayList<Device> devices;

	public Environment(JSONObject json) {
		super(json);
		this.zones = new ArrayList<Zone>();
		this.devices = new ArrayList<Device>();

		try { this.uuid = this.data.getString("uuid"); } catch (Exception e) { }
		try { this.geofence = new Geofence(json.getJSONObject("geofence")); } catch (Exception e) {}
		try { this.perimeter = getRect(json.getJSONObject("perimeter")); } catch (Exception e) {}
		try { this.angle = json.getDouble("angle"); } catch (Exception e) {}
	}

	@Override
	public String toString() {
		return super.toString() + " - " + perimeter;
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

	public void setUuid(String uuid) {
		this.uuid = uuid;
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

	public Thing getThingWithMinor(int minor) {
		for (Zone zone: zones) {
			for (Thing thing : zone.getThings()) {
				if (thing.getMinor() == minor) {
					return thing;
				}
			}
		}
		return null;
	}

	public PointF userLocation() {
		float total = 0.0f;
		float x = 0.0f;
		float y = 0.0f;

		for (Zone zone: zones) {
			for (Thing thing : zone.getThings()) {
				double weight = thing.getInverseDistance();
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
