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

import android.app.util.desolation.IOHelper;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

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
    private String mStoragePath;
    private String mDownloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.boot_animation_settings);
        mBootAnimDisable = (SwitchPreference) findPreference(USE_BOOTANIMATION_KEY);
        mBootAnimSelect = (ListPreference) findPreference(SET_BOOTANIMATION_KEY);
        Log.i(TAG, "BootAnimations are set to "+(mBootAnimDisable.isChecked() ? true:false));
        File bootanimations = new File(Environment.getExternalStorageDirectory(), "deso/bootanimations/");
        mStoragePath = bootanimations.getAbsolutePath();
        if (Helpers.checkSu() == false){
		CMDProcessor.canSU();
        }

	File bootaniBackup = new File("/system/media/bootanimation.backup");
	if (bootaniBackup.exists() != true){
      		CMDProcessor.runSuCommand("sysrw && cp /system/media/bootanimation.zip /system/media/bootanimation.backup && sysro").getStdout();
  	}
    	File vendorProp = new File("/vendor/build.prop");
    	if (vendorProp.exists() != true){
      		CMDProcessor.runSuCommand("sysrw && touch /vendor/build.prop && chmod 0644 /vendor/build.prop && sysro").getStdout();
	}
        if (bootanimations.mkdirs()) {
		Log.i(TAG, "Path Created: "+mStoragePath);
        } else {
		Log.i(TAG, "Path already exists: "+mStoragePath);
        }
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
        updateBootAnimSelect();
    }

    private void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
    }

    private void updateUseBootAnimation() {
	updateSwitchPreference( mBootAnimDisable, SystemProperties.getBoolean("persist.sys.deso.bootanim", true));
    }

    private void updateBootAnimSelect(){
	List<CharSequence> storagelist = new ArrayList<CharSequence>();
	List<CharSequence> storagevalues = new ArrayList<CharSequence>();
	/*--- Available zips from Storage
	 * --Static path set to External storage deso/bootanimations */
	// ---EDIT BELOW THIS LINE FOR STATIC ENTRIES
	CharSequence[] staticentries = {
	//If more are added please modify here -- See vendor
		"Stock"
	};
	CharSequence[] staticvalues = {
	// & remember to add their matching path -- See vendor
		"/system/media/bootanimation.zip"
	};// 3 ---EDIT ABOVE THIS LINE FOR STATIC ENTRIES
	for (CharSequence a: staticentries){
		storagelist.add(a);
	}
	for (CharSequence c: staticvalues){
		storagevalues.add(c);
	}
	if (IOHelper.runStorageCheck(mDownloadsPath) == 1){
		CharSequence[] dloaddir = IOHelper.zipFileFilter(mDownloadsPath);
		for (CharSequence b: dloaddir){
			storagelist.add(b);
		}
		for (CharSequence d: dloaddir){
			storagevalues.add(d);
		}
	}
	if (IOHelper.runStorageCheck(mStoragePath) == 1){
		CharSequence[] storageentries = IOHelper.zipFileFilter(mStoragePath);
		for (CharSequence b: storageentries){
			storagelist.add(b);
		}
		for (CharSequence d: storageentries){
			storagevalues.add(d);
		}
	}
	CharSequence[] entries = storagelist.toArray(staticentries);
	CharSequence[] values = storagevalues.toArray(staticvalues);
	mBootAnimSelect.setEntries(entries);
	mBootAnimSelect.setEntryValues(values);
	mBootAnimSelect.setValue(SystemProperties.get("persist.sys.deso.bootanimfile", "/system/media/bootanimation.zip"));
	mBootAnimSelect.setOnPreferenceChangeListener(this);
  }

  private void writeUseBootAnimation() {
	SystemProperties.set( "persist.sys.deso.bootanim",  mBootAnimDisable.isChecked() ?  "1" : "0" );
	if (mBootAnimDisable.isChecked() == true) {
		CMDProcessor.runSuCommand("sysrw && sed -i '/debug.sf.nobootanimation=/d' /vendor/build.prop && sysro").getStdout();
		Log.i(TAG, "Enabled Boot Animations");
	} else {
		CMDProcessor.runSuCommand("sysrw && echo 'debug.sf.nobootanimation=1' >> /vendor/build.prop && sysro").getStdout();
		Log.i(TAG, "Disabled Boot Animations");
	}
  }

  private void writeBootAnimSelect(Object newValue) {
	int index = mBootAnimSelect.findIndexOfValue((String) newValue);
	Log.i(TAG, "Index value "+index+" set to "+(mBootAnimSelect.getEntries()[index]));
	SystemProperties.set("persist.sys.deso.bootanimfile", String.valueOf((String) newValue));
	mBootAnimSelect.setSummary(mBootAnimSelect.getEntries()[index]);
	if (index != 0){
		CMDProcessor.runSuCommand("sysrw && cp "+String.valueOf((String) newValue)+" /system/media/bootanimation.zip && sysro").getStdout();
	} else { //Stock Chosen
		CMDProcessor.runSuCommand("sysrw && cp /system/media/bootanimation.backup /system/media/bootanimation.zip && sysro").getStdout();
	}
  }

  private void removePreference(Preference preference) {
	getPreferenceScreen().removePreference(preference);
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
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
