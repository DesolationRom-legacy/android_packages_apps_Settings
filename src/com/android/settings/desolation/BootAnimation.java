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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;

import com.android.settings.R;
import com.android.settings.util.Helpers;
import com.android.settings.util.CMDProcessor;
import com.android.settings.SettingsPreferenceFragment;

public class BootAnimation extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "DesoCore BootAnimations";

    public static final String USE_BOOTANIMATION_KEY = "enable_bootanimation";
    public static final String SET_BOOTANIMATION_KEY = "select_bootanimation";
    private SwitchPreference mBootAnimDisable;
    private ListPreference mBootAnimSelect;
    
	private FilenameFilter mZipFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".zip");
		}
	};
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.boot_animation_settings);
        mBootAnimDisable = (SwitchPreference) findPreference(USE_BOOTANIMATION_KEY);
        mBootAnimSelect = (ListPreference) findPreference(SET_BOOTANIMATION_KEY);
        updateState();
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
        if(mBootAnimDisable.isChecked()){
			updateBootAnimSelect();
		} else {
			removePreference(mBootAnimSelect);
		}
    }
    
    private void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
    }

    private void updateUseBootAnimation() {
			updateSwitchPreference( mBootAnimDisable, SystemProperties.getBoolean("persist.sys.deso.bootanim", true));
        if(mBootAnimDisable.isChecked()){
			updateBootAnimSelect();
		} else {
			removePreference(mBootAnimSelect);
		}
    }
 
    private void updateBootAnimSelect(){
		String[] entries = {
		//If more are added please modify here -- See vendor
		"Stock", // 1
		"8-bit Arcade by Scar45", // 2
		zipFileFilter("/sdcard/desobootanimations")// 3 ---EDIT ABOVE THIS LINE--- Available file(s) from Storage
		};
		String[] values = {
		// & remember to add their matching path -- See vendor
		"/vendor/bootanimations/stockbootani.zip", // 1
		"/vendor/bootanimations/8bitarcade.zip", // 2
		zipFileFilter("/sdcard/desobootanimations")// 3 ---EDIT ABOVE THIS LINE--- Available file(s) from Storage
		};
		System.out.println(Arrays.toString(entries));
		mBootAnimSelect.setEntries(entries);
		mBootAnimSelect.setEntryValues(values);
		SystemProperties.get("persist.sys.deso.bootanimfile", "/system/media/bootanimation.zip");
        mBootAnimSelect.setValue(SystemProperties.get("persist.sys.deso.bootanimfile", "/vendor/bootanimations/stockbootani.zip"));
        mBootAnimSelect.setOnPreferenceChangeListener(this);
	}   
    
    private void writeUseBootAnimation() {
		SystemProperties.set( "persist.sys.deso.bootanim",  mBootAnimDisable.isChecked() ?  "1" : "0" );
		if (Helpers.checkSu() == false){
			CMDProcessor.canSU();
		}
		CMDProcessor.runSuCommand("sh /system/bin/bootani toggle").getStdout();
    }
    
    private void writeBootAnimSelect(Object newValue) {
		int index = mBootAnimSelect.findIndexOfValue((String) newValue);
		SystemProperties.set("persist.sys.deso.bootanimfile", String.valueOf((String) newValue));
		mBootAnimSelect.setSummary(mBootAnimSelect.getEntries()[index]);
		if (Helpers.checkSu() == false){
			CMDProcessor.canSU();
		}
		CMDProcessor.runSuCommand("sh /system/bin/bootani writenew").getStdout();
	}

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
    }

    private String zipFileFilter(String path){
		File f = new File(path);
		File[] files = f.listFiles(mZipFilter);
		String ret = Arrays.toString(files).replace("[", "").replace("]", "").trim();
		return ret;
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

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mBootAnimSelect) {
			writeBootAnimSelect(newValue);
			return true;    
		}
		return false;
	}
}
