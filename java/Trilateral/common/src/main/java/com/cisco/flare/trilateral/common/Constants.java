package com.cisco.flare.trilateral.common;

/**
 * Created by azamlerc on 1/30/16.
 */
public final class Constants {
	private Constants() {}

	public static final String SEND_MESSAGE_INTENT = "com.cisco.ctao.ioe.ux.flare.SEND_MESSAGE";
	public static final String RECEIVE_MESSAGE_INTENT = "com.cisco.ctao.ioe.ux.flare.RECEIVE_MESSAGE";
	public static final String MESSAGE_TYPE_KEY = "com.cisco.ctao.ioe.ux.flare.MESSAGE_TYPE";
	public static final String MESSAGE_VALUE_KEY = "com.cisco.ctao.ioe.ux.flare.MESSAGE_VALUE";

	public static final String KEY_TEXT_FROM_SERVER = "com.cisco.ctao.ioe.ux.flare.IntentKey_textFromServer";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_MESSAGE_TYPE = "messageType";

	public static final String TYPE_HELLO = "type_hello";
	public static final String TYPE_ENVIRONMENT = "type_environment";
	public static final String TYPE_POSITION = "type_position";
	public static final String TYPE_POSITION_ANGLE = "type_position_angle"; // {position: {x, y}, angle}
	public static final String TYPE_DATA = "type_data";
	public static final String TYPE_NEAR_THING = "type_near_thing";
	public static final String TYPE_FAR_THING = "type_far_thing";
	public static final String TYPE_ENTER_ZONE = "type_enter_zone";
	public static final String TYPE_EXIT_ZONE = "type_exit_zone";
	public static final String TYPE_GET_PREFS = "type_get_prefs";
	public static final String TYPE_SELECTION_CHANGED = "type_selection_changed";
	public static final String TYPE_ACTION = "type_action";

	public static final String MESSAGE_MOBILE_TO_WEARABLE_PATH = "/mobile2wearable";
	public static final String MESSAGE_WEARABLE_TO_MOBILE_PATH = "/wearable2mobile";

	public static final int FLARE_FIN_ARC_STROKE_WIDTH = 20;
	public static final int FLARE_FIN_TOUCHABLE_DISTANCE = 40;

	//    public static final String THING_SELECTED_INTENT = "com.cisco.ctao.ioe.ux.flare.THING_SELECTED_UPDATED";
	public static final String THING_SELECTED_ID_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_thingSelectedId";
	public static final String THING_SELECTED_NAME_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_thingSelectedName";
	public static final String THING_SELECTED_DESC_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_thingSelectedDesc";
	public static final String THING_SELECTED_COLOR_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_thingSelectedColor";
	public static final String THING_SELECTED_INDEX = "com.cisco.ctao.ioe.ux.flare.IntentKey_thingSelectedIndex";

	//    public static final String QUICK_STATUS_VIEW_INTENT = "com.cisco.ctao.ioe.ux.flare.QUICK_STATUS_VIEW";
	public static final String QUICK_STATUS_VIEW_NAME_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_quickStatusName";
	public static final String QUICK_STATUS_VIEW_DESC_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_quickStatusDesc";
	public static final String QUICK_STATUS_VIEW_ACTIONS_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_quickStatusActions";
	public static final String QUICK_STATUS_VIEW_ID_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_quickStatusId";
	public static final String QUICK_STATUS_VIEW_ACTION_COLOR_KEY = "com.cisco.ctao.ioe.ux.flare.IntentKey_quickStatusActionColor";

//    public static final int BEACON_SAMPLE_SCAN_PERIOD_MS = 1100;

	// a drag&drop area is in the middle of the screen. The following constant defines its radius.
	public static final double DRAG_DROP_RADIUS = 50.0;

	public final static int CAROUSEL_FIRST_PAGE = 0;
	public final static float CAROUSEL_BIG_SCALE = 1.0f;
	public final static float CAROUSEL_SMALL_SCALE = 0.7f;
	public final static float CAROUSEL_DIFF_SCALE = CAROUSEL_BIG_SCALE - CAROUSEL_SMALL_SCALE;

	public final static String KEY_PREF_BEACON_DEVICE = "pref_beacon_device";
	public final static String KEY_PREF_BEACON_DEVICE_NONE = "NONE";
	public final static String KEY_PREF_BEACON_DEVICE_MOBILE = "MOBILE";
	public final static String KEY_PREF_BEACON_DEVICE_WATCH = "WATCH";

	public final static String KEY_PREF_BEACON_SMOOTHING = "pref_beacon_smoothing";

	public final static String KEY_PREF_WATCH_ANGLE = "pref_watch_global_angle";

	public final static String KEY_PREF_FIN_FAR_SIZE = "pref_fin_far_size";
	public final static String KEY_PREF_FIN_NEAR_SIZE = "pref_fin_near_size";
	public final static String KEY_PREF_FIN_FAR_OPACITY = "pref_fin_far_opacity";
	public final static String KEY_PREF_FIN_NEAR_OPACITY = "pref_fin_near_opacity";
	public final static String KEY_PREF_FIN_FAR_DISTANCE = "pref_fin_far_distance";
	public final static String KEY_PREF_FIN_NEAR_DISTANCE = "pref_fin_near_distance";
	public final static String KEY_PREF_FIN_GRAV_FACTOR = "pref_fin_grav_factor";

	public final static String KEY_PREF_DEBUG_SHOW_PIE = "pref_debug_show_pie";
	public final static String KEY_PREF_DEBUG_SHOW_BEACONS = "pref_debug_show_beacons";

	public static final double TWO_PI=Math.PI*2.0;
	public static final double HALF_PI=Math.PI/2.0;
	public static final double PI_AND_A_HALF=Math.PI+HALF_PI;

	public static final String DEVICE_TYPE_UNKNOWN  = "device_unknown";
	public static final String DEVICE_TYPE_WEARABLE = "device_wearable";
	public static final String DEVICE_TYPE_MOBILE   = "device_mobile";

	public static final String REFLEKTOR_ACTION_START_QUICK_STATUS  = "start_quickstatus";
	public static final String REFLEKTOR_ACTION_STOP_QUICK_STATUS   = "stop_quickstatus";
	public static final String REFLEKTOR_ACTION_START_QUICK_ACTIONS = "start_quickactions";
	public static final String REFLEKTOR_ACTION_STOP_QUICK_ACTIONS  = "stop_quickactions";
}