package com.cisco.flare.trilateral;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by azamlerc on 9/4/15.
 */
public class SettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
