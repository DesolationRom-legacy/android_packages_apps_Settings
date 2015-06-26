/*
 * Copyright (C) 2015 DesolationRom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.desolation;

import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class BootAnimation extends SettingsPreferenceFragment {
    private static final String TAG = "BootAnimation";

    public static final String USE_BOOTANIMATION_KEY = "enable_bootanimation";

    private SwitchPreference mBootAnimDisable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.boot_animation_settings);

        mBootAnimDisable = (SwitchPreference) findPreference(USE_BOOTANIMATION_KEY);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    private void updateState() {
        updateUseBootAnimation();
    }
    
    void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
    }

    private void updateUseBootAnimation() {
        updateSwitchPreference(
                mBootAnimDisable, SystemProperties.getBoolean("persist.sys.deso.bootanim", true));
    }
    
    private void writeUseBootAnimation() {
		 SystemProperties.set( "persist.sys.deso.bootanim",  mBootAnimDisable.isChecked() ?  "1" : "0" );
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mBootAnimDisable) {
            writeUseBootAnimation();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
