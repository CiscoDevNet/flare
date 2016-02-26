package com.cisco.flare;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.location.Location;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

public class FlareManager {
    private Socket flareSocket;
    private boolean debugRest = false;
    private boolean debugSocket = true;

    private static final String TAG = "FlareManager";

    public String host = "localhost";
    public int port = 1234;
    String server = "http://" + host + ":" + port;

    private Activity activity;
    private Delegate delegate;
    private RequestQueue queue;
    private HashMap<String, Flare> flareIndex = new HashMap<>();

    public FlareManager(String host, int port) {
        this.host = host;
        this.port = port;
        this.server = "http://" + host + ":" + port;
    }

    public interface Handler {
        public void gotResponse(JSONObject object);
    }

    public interface ListHandler {
        public void gotResponse(ArrayList<JSONObject> object);
    }

    public interface StringHandler {
        public void gotResponse(String value);
    }

    // TODO: can we combine these?
    public interface EnvironmentsHandler {
        public void gotResponse(ArrayList<Environment> environments);
    }

    public interface EnvironmentHandler {
        public void gotResponse(Environment environment);
    }

    public interface ZoneHandler {
        public void gotResponse(Zone zone);
    }

    public interface ThingHandler {
        public void gotResponse(Thing thing);
    }

    public interface DeviceHandler {
        public void gotResponse(Device device);
    }

    public interface Delegate {
        void didReceiveData(Flare flare, JSONObject data, Flare sender);
        void didReceivePosition(Flare flare, PointF oldPosition, PointF newPosition, Flare sender);
        void handleAction(Flare flare, String action, Flare sender);
        void enter(Zone zone, Device device);
        void exit(Zone zone, Device device);
        void near(Thing thing, Device device, double distance);
        void far(Thing thing, Device device);
    }

    public void setActivity(Activity value) {
        this.activity = value;
        this.queue = Volley.newRequestQueue(activity);
    }

    public void setDelegate(Delegate value) {
        this.delegate = value;
    }

    // TODO: calling this should be automatic
    public void addToIndex(Flare flare) {
        flareIndex.put(flare.getId(), flare);
    }

    public Flare flareWithName(ArrayList<Flare> array, String name) {
        for (Flare flare : array) {
            if (name.equals(flare.getName())) {
                return flare;
            }
        }
        return null;
    }

    private int requests = 0;

    public void loadEnvironments(Location location, EnvironmentsHandler handler) {
        ArrayList<Environment> environments = new ArrayList<>();
        requests = 0;

        requests++;
        listEnvironments(location, (environmentsList) -> {
            if (environmentsList == null) {
                Log.d(TAG, "Could not load environments.");
                handler.gotResponse(null);
                return;
            }

            if (environmentsList.isEmpty() && location != null) {
                Log.d(TAG, "No environments found for location: " + location);
            }

            for (JSONObject environmentJson : environmentsList) {
                if (environmentsList != null) {
                    Environment environment = new Environment(environmentJson);
                    environments.add(environment);
                    addToIndex(environment);

                    requests++;
                    listZones(environment.getId(), (zonesList) -> {
                        if (zonesList != null) {
                            for (JSONObject zoneJson : zonesList) {
                                Zone zone = new Zone(zoneJson);
                                environment.getZones().add(zone);
                                addToIndex(zone);

                                requests++;
                                listThings(zone.getId(), environment.getId(), (thingsList) -> {
                                    if (thingsList != null) {
                                        for (JSONObject thingJson : thingsList) {
                                            Thing thing = new Thing(thingJson);
                                            zone.getThings().add(thing);
                                            addToIndex(thing);
                                        }
                                    }

                                    requests--;
                                    if (requests == 0) handler.gotResponse(environments);
                                });
                            }
                        }

                        requests--;
                        if (requests == 0) handler.gotResponse(environments);
                    });

                    requests++;
                    listDevices(environment.getId(), (devicesList) -> {
                        if (devicesList != null) {
                            for (JSONObject deviceJson : devicesList) {
                                Device device = new Device(deviceJson);
                                environment.getDevices().add(device);
                                addToIndex(device);
                            }
                        }

                        requests--;
                        if (requests == 0) handler.gotResponse(environments);
                    });
                }
            }

            requests--;
            if (requests == 0) handler.gotResponse(environments);
        });
    }

    // tries to find an existing device object in the current environment
    // if one is not found, creates a new device object
    public void getCurrentDevice(String environmentId, JSONObject template, String deviceType, SharedPreferences prefs, FlareManager.DeviceHandler handler) {
        savedDevice(environmentId, deviceType, prefs, (savedDevice) -> {
            if (savedDevice != null) {
                handler.gotResponse(savedDevice);
            } else {
                newDeviceObject(environmentId, template, deviceType, prefs, (newDevice) -> {
                    if (newDevice != null) {
                        handler.gotResponse(newDevice);
                    }
                });
            }
        });
    }

    public String deviceIdKey(String environmentId, String deviceType) {
        return "deviceId-" + deviceType + "-" + environmentId;
    }

    // looks for an existing device object in the current environment, and if found calls the handler with it
    public void savedDevice(String environmentId, String deviceType, SharedPreferences prefs, FlareManager.DeviceHandler handler) {
        String deviceId = prefs.getString(deviceIdKey(environmentId, deviceType), null);
        if (deviceId != null) {
            getDevice(deviceId, environmentId, (json) -> {
                try {
                    String validId = json.getString("_id");
                    String deviceEnvironment = json.getString("environment");

                    if (deviceEnvironment.equals(environmentId)) {
                        Device device = new Device(json);
                        addToIndex(device);

                        Log.d(TAG, "Found existing device: " + device.getName());
                        handler.gotResponse(device);
                    } else {
                        Log.d(TAG, "Device in wrong environment");
                        handler.gotResponse(null);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Device not found in current environment");
                    handler.gotResponse(null);
                }
            });
        } else {
            Log.d(TAG, "No saved device");
            handler.gotResponse(null);
        }
    }

    public void getCurrentZone(String environmentId, PointF position, FlareManager.ZoneHandler handler) {
        listZones(environmentId, position, (jsonArray) -> {
            if (jsonArray != null && !jsonArray.isEmpty()) {
                try {
                    JSONObject json = jsonArray.get(0);
                    String zoneId = json.getString("_id");
                    Zone zone = (Zone) flareIndex.get(zoneId);
                    handler.gotResponse(zone);
                } catch (Exception e) {
                    handler.gotResponse(null);
                }
            } else {
                handler.gotResponse(null);
            }
        });
    }

    public void getNearbyThing(String environmentId, String deviceId, FlareManager.ThingHandler handler) {
        getDevice(deviceId, environmentId, (json) -> {
            try {
                String nearestId = json.getString("nearest");
                Thing thing = (Thing) flareIndex.get(nearestId);
                handler.gotResponse(thing);
            } catch (Exception e) {
                handler.gotResponse(null);
            }
        });
    }

    // creates a new device object using the template
    public void newDeviceObject(String environmentId, JSONObject template, String deviceType, SharedPreferences prefs, FlareManager.DeviceHandler handler) {
        newDevice(environmentId, template, (json) -> {
            Device device = new Device(json);
            addToIndex(device);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(deviceIdKey(environmentId, deviceType), device.getId());
            editor.commit();

            Log.d(TAG, "Created new device: " + device.getName());
            handler.gotResponse(device);
        });
    }
    public Flare flareForMessage(JSONObject message) {
        try {
            String id = message.getString("thing");
            if (id != null) return flareIndex.get(id);
        } catch (Exception e) {}

        try {
            String id = message.getString("device");
            if (id != null) return flareIndex.get(id);
        } catch (Exception e) {}

        try {
            String id = message.getString("zone");
            if (id != null) return flareIndex.get(id);
        } catch (Exception e) {}

        try {
            String id = message.getString("environment");
            if (id != null) return flareIndex.get(id);
        } catch (Exception e) {}

        return null;
    }

    public Flare senderForMessage(JSONObject message) {
        try {
            String sender = message.getString("sender");
            if (sender != null) return flareIndex.get("sender");
        } catch (Exception e) {}

        return null;
    }

    public void connect() {
        try {
            flareSocket = IO.socket(server);
            addListeners();
            flareSocket.connect();
            if (debugSocket) Log.d(TAG, "Connected to Flare server: " + server);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Couldn't connect to Flare server: " + server);
        }
    }

    public void disconnect() {
        if (flareSocket != null) {
            flareSocket.disconnect();
            flareSocket = null;
            if (debugSocket) Log.d(TAG, "Disconected from Flare server.");
        }
    }

    // MARK: REST interface

    public void sendRequest(int method, String uri, final Handler handler) {
        String url = server + uri;
        JsonObjectRequest request = new JsonObjectRequest(method, url, (response) -> {
            if (debugRest) Log.d(TAG, uri + ": " + response.toString());
            handler.gotResponse(response);
        }, (error) -> {
            Log.e(TAG, "Could not connect to: " + url);
            handler.gotResponse(null);
        });
        queue.add(request);
    }

    public void sendRequest(int method, String uri, JSONObject message, final Handler handler) {
        String url = server + uri;
        JsonObjectRequest request = new JsonObjectRequest(method, url, message, (response) -> {
            if (debugRest) Log.d(TAG, uri + ": " + response.toString());
            handler.gotResponse(response);
        }, (error) -> {
            Log.e(TAG, "Could not connect to: " + uri);
            handler.gotResponse(null);
        });
        queue.add(request);
    }

    public void sendListRequest(int method, String uri, final ListHandler handler) {
        String url = server + uri;
        JsonArrayRequest request = new JsonArrayRequest(method, url, (response) -> {
            if (debugRest) Log.d(TAG, uri + ": " + response.toString());
            handler.gotResponse(toList(response));
        }, (error) -> {
            Log.e(TAG, "Could not connect to: " + uri);
            handler.gotResponse(null);
        });
        queue.add(request);
    }

    public void sendStringRequest(int method, String uri, final StringHandler handler) {
        String url = uri.contains("://") ? uri : server + uri;
        StringRequest request = new StringRequest(method, url, (response) -> {
            if (debugRest) Log.d(TAG, uri + ": " + response);
            handler.gotResponse(response);
        }, (error) -> {
            Log.e(TAG, "Could not connect to: " + uri);
            handler.gotResponse(null);
        });
        queue.add(request);
    }

    // convert JSONArray to ArrayList<JSONObject> so that we can enumerate them!
    public ArrayList<JSONObject> toList(JSONArray jsonArray) {
        ArrayList<JSONObject> list = new ArrayList<>();
        int length = jsonArray.length();

        for (int i = 0; i < length; i++) {
            try {
                JSONObject json = jsonArray.getJSONObject(i);
                list.add(json);
            } catch (Exception e) {}
        }

        return list;
    }

    public void listEnvironments(ListHandler handler) {
        listEnvironments(null, handler);
    }

    public void listEnvironments(Location location, ListHandler handler) {
        String uri = "/environments";
        if (location != null) uri += "?latitude=" + location.getLatitude() + "&longitude=" + location.getLongitude();
        sendListRequest(Request.Method.GET, uri, handler);
    }

    public void newEnvironment(JSONObject message, Handler handler) {
        String uri = "/environments";
        sendRequest(Request.Method.POST, uri, message, handler);
    }

    public void getEnvironment(String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId;
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void updateEnvironment(String environmentId, JSONObject message, Handler handler) {
        String uri = "/environments/" + environmentId;
        sendRequest(Request.Method.PUT, uri, message, handler);
    }

    public void deleteEnvironment(String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId;
        sendRequest(Request.Method.DELETE, uri, handler);
    }

    public void listZones(String environmentId, ListHandler handler) {
        listZones(environmentId, null, handler);
    }

    public void listZones(String environmentId, PointF point, ListHandler handler) {
        String uri = "/environments/" + environmentId + "/zones";
        if (point != null) uri += "?x=" + point.x + "&y=" + point.y;
        sendListRequest(Request.Method.GET, uri, handler);
    }

    public void newZone(String environmentId, JSONObject message, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones";
        sendRequest(Request.Method.POST, uri, message, handler);
    }

    public void getZone(String zoneId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId;
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void updateZone(String zoneId, String environmentId, JSONObject message, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId;
        sendRequest(Request.Method.PUT, uri, message, handler);
    }

    public void deleteZone(String zoneId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId;
        sendRequest(Request.Method.DELETE, uri, handler);
    }

    public void listThings(String zoneId, String environmentId, ListHandler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId + "/things";
        sendListRequest(Request.Method.GET, uri, handler);
    }

    public void newThing(String zoneId, String environmentId, JSONObject message, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId + "/things";
        sendRequest(Request.Method.POST, uri, message, handler);
    }

    public void getThing(String thingId, String zoneId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId + "/things/" + thingId;
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void getThingData(String thingId, String zoneId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId + "/things/" + thingId + "/data";
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void getThingPosition(String thingId, String zoneId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId + "/things/" + thingId + "/position";
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void updateThing(String thingId, String zoneId, String environmentId, JSONObject message, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId + "/things/" + thingId;
        sendRequest(Request.Method.PUT, uri, message, handler);
    }

    public void deleteThing(String thingId, String zoneId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/zones/" + zoneId + "/things/" + thingId;
        sendRequest(Request.Method.DELETE, uri, handler);
    }

    public void listDevices(String environmentId, ListHandler handler) {
        String uri = "/environments/" + environmentId + "/devices";
        sendListRequest(Request.Method.GET, uri, handler);
    }

    public void newDevice(String environmentId, JSONObject message, Handler handler) {
        String uri = "/environments/" + environmentId + "/devices";
        sendRequest(Request.Method.POST, uri, message, handler);
    }

    public void getDevice(String deviceId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/devices/" + deviceId;
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void getDeviceData(String deviceId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/devices/" + deviceId + "/data";
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void getDevicePosition(String deviceId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/devices/" + deviceId + "/position";
        sendRequest(Request.Method.GET, uri, handler);
    }

    public void updateDevice(String deviceId, String environmentId, JSONObject message, Handler handler) {
        String uri = "/environments/" + environmentId + "/devices/" + deviceId;
        sendRequest(Request.Method.PUT, uri, message, handler);
    }

    public void deleteDevice(String deviceId, String environmentId, Handler handler) {
        String uri = "/environments/" + environmentId + "/devices/" + deviceId;
        sendRequest(Request.Method.DELETE, uri, handler);
    }

    public void getMacAddress(StringHandler handler) {
        String url = "http://" + this.host + "/mac/mac.php";
        sendStringRequest(Request.Method.GET, url, handler);
    }

    // MARK: ScoketIO sent

    public void subscribe(Flare flare) {
        subscribe(flare, false);
    }

    public void subscribe(Flare flare, boolean all) {
        JSONObject message = flare.idInfo();
        try { if (all) message.put("all", true); } catch (Exception e) {}
        if (debugSocket) Log.d(TAG, "subscribe: " + message.toString());
        flareSocket.emit("subscribe", message);
    }

    public void unsubscribe(Flare flare) {
        JSONObject message = flare.idInfo();
        if (debugSocket) Log.d(TAG, "unsubscribe: " + message.toString());
        flareSocket.emit("unsubscribe", message);
    }

    public void getData(Flare flare) {
        JSONObject message = flare.idInfo();
        if (debugSocket) Log.d(TAG, "getData: " + message.toString());
        flareSocket.emit("getData", message);
    }

    public void getData(Flare flare, String key) {
        JSONObject message = flare.idInfo();
        try { message.put("key", key); } catch (Exception e) {}
        if (debugSocket) Log.d(TAG, "getData: " + message.toString());
        flareSocket.emit("getData", message);
    }

    public void setData(Flare flare, String key, Object value, Flare sender) {
        JSONObject message = flare.idInfo();
        try { message.put("key", key); } catch (Exception e) {}
        try { message.put("value", value); } catch (Exception e) {}
        if (sender != null) try { message.put("sender", sender.getId()); } catch (Exception e) {}
        if (debugSocket) Log.d(TAG, "setData: " + message.toString());
        flareSocket.emit("setData", message);
    }

    public void setData(Flare flare, String key, double value, Flare sender) {
        JSONObject message = flare.idInfo();
        try { message.put("key", key); } catch (Exception e) {}
        try { message.put("value", value); } catch (Exception e) {}
        if (sender != null) try { message.put("sender", sender.getId()); } catch (Exception e) {}
        if (debugSocket) Log.d(TAG, "setData: " + message.toString());
        flareSocket.emit("setData", message);
    }

    public void getPosition(Flare flare) {
        JSONObject message = flare.idInfo();
        if (debugSocket) Log.d(TAG, "getPosition: " + message.toString());
        flareSocket.emit("getPosition", message);
    }

    public void setPosition(Flare flare, PointF point, Flare sender) {
        JSONObject message = flare.idInfo();
        try { message.put("position", Flare.pointToJSON(point)); } catch (Exception e) {}
        if (sender != null) try { message.put("sender", sender.getId()); } catch (Exception e) {}
        if (debugSocket) Log.d(TAG, "setPosition: " + message.toString());
        flareSocket.emit("setPosition", message);
    }

    public void performAction(Flare flare, String action, Flare sender) {
        JSONObject message = flare.idInfo();
        try { message.put("action", action); } catch (Exception e) {}
        if (sender != null) try { message.put("sender", sender.getId()); } catch (Exception e) {}
        if (debugSocket) Log.d(TAG, "performAction: " + message.toString());
        flareSocket.emit("performAction", message);
    }

    // MARK: SocketIO received

    public void addListeners() {
        flareSocket.on("data", (args) -> {
            final JSONObject message = (JSONObject) args[0];
            activity.runOnUiThread(() -> {
                if (debugSocket) Log.d(TAG, "data: " + message.toString());
                try {
                    Flare flare = flareForMessage(message);
                    JSONObject data = message.getJSONObject("data");
                    Flare sender = senderForMessage(message);

                    try {
                        String key = (String)data.names().get(0);
                        flare.getData().put(key, data.get(key));
                    } catch (Exception e) {}

                    this.delegate.didReceiveData(flare, data, sender);
                } catch (Exception e) {}
            });
        });

        flareSocket.on("position", (args) -> {
            final JSONObject message = (JSONObject) args[0];
            activity.runOnUiThread(() -> {
                if (debugSocket) Log.d(TAG, "position: " + message.toString());
                try {
                    Flare flare = flareForMessage(message);
                    PointF newPosition = Flare.getPoint(message.getJSONObject("position"));
                    Flare sender = senderForMessage(message);

                    PointF oldPosition = ((Flare.PositionObject)flare).getPosition();
                    ((Flare.PositionObject)flare).setPosition(newPosition);

                    this.delegate.didReceivePosition(flare, oldPosition, newPosition, sender);
                } catch (Exception e) {}
            });
        });

        flareSocket.on("handleAction", (args) -> {
            final JSONObject message = (JSONObject) args[0];
            activity.runOnUiThread(() -> {
                if (debugSocket) Log.d(TAG, "handleAction: " + message.toString());
                try {
                    Flare flare = flareForMessage(message);
                    String action = message.getString("action");
                    Flare sender = senderForMessage(message);
                    this.delegate.handleAction(flare, action, sender);
                } catch (Exception e) {}
            });
        });

        flareSocket.on("enter", (args) -> {
            final JSONObject message = (JSONObject) args[0];
            activity.runOnUiThread(() -> {
                if (debugSocket) Log.d(TAG, "enter: " + message.toString());
                try {
                    Zone zone = (Zone)flareIndex.get(message.getString("zone"));
                    Device device = (Device)flareIndex.get(message.getString("device"));
                    this.delegate.enter(zone, device);
                } catch (Exception e) {} // TODO: print an error if the thing/device aren't cached in memory!
            });
        });

        flareSocket.on("exit", (args) -> {
            final JSONObject message = (JSONObject) args[0];
            activity.runOnUiThread(() -> {
                if (debugSocket) Log.d(TAG, "exit: " + message.toString());
                try {
                    Zone zone = (Zone)flareIndex.get(message.getString("zone"));
                    Device device = (Device)flareIndex.get(message.getString("device"));
                    this.delegate.exit(zone, device);
                } catch (Exception e) {} // TODO: print an error if the thing/device aren't cached in memory!
            });
        });

        flareSocket.on("near", (args) -> {
            final JSONObject message = (JSONObject) args[0];
            activity.runOnUiThread(() -> {
                if (debugSocket) Log.d(TAG, "near: " + message.toString());
                try {
                    Thing thing = (Thing)flareIndex.get(message.getString("thing"));
                    Device device = (Device)flareIndex.get(message.getString("device"));
                    double distance = message.getDouble("distance");
                    this.delegate.near(thing, device, distance);
                } catch (Exception e) {}
            });
        });

        flareSocket.on("far", (args) -> {
            final JSONObject message = (JSONObject) args[0];
            activity.runOnUiThread(() -> {
                if (debugSocket) Log.d(TAG, "far: " + message.toString());
                try {
                    Thing thing = (Thing)flareIndex.get(message.getString("thing"));
                    Device device = (Device)flareIndex.get(message.getString("device"));
                    this.delegate.far(thing, device);
                } catch (Exception e) {} // TODO: print an error if the thing/device aren't cached in memory!
            });
        });

    }



}
