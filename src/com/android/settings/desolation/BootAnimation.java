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
    private FilenameFilter mZipFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".zip");
		}
    };
    private FileFilter mDirFilter = new FileFilter() {
		public boolean accept(File file) {
			return file.isDirectory();
		}
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	if (runStorageCheck("/sdcard/desobootanimations") == 1){
		mStoragePath = "/sdcard/desobootanimations";
	} else {
		mStoragePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
	}
        addPreferencesFromResource(R.xml.boot_animation_settings);
        mBootAnimDisable = (SwitchPreference) findPreference(USE_BOOTANIMATION_KEY);
        mBootAnimSelect = (ListPreference) findPreference(SET_BOOTANIMATION_KEY);
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
	 * --Static path set to /sdcard/desobootanimations */
	// ---EDIT BELOW THIS LINE FOR STATIC ENTRIES
	CharSequence[] staticentries = {
	//If more are added please modify here -- See vendor
	"Stock", // 1
	"8-bit Arcade by Scar45" // 2
	};
	CharSequence[] staticvalues = {
	// & remember to add their matching path -- See vendor
	"/vendor/bootanimations/stockbootani.zip", // 1
	"/vendor/bootanimations/8bitarcade.zip" // 2
	};// 3 ---EDIT ABOVE THIS LINE FOR STATIC ENTRIES
	switch(runStorageCheck(mStoragePath)){
		case 0:
			mBootAnimSelect.setEntries(staticentries);
			mBootAnimSelect.setEntryValues(staticvalues);
		case 1:
			for (CharSequence a: staticentries){
				storagelist.add(a);
			}
			for (CharSequence c: staticvalues){
				storagevalues.add(c);
			}
			CharSequence[] storageentries = zipFileFilter(mStoragePath);
			for (CharSequence b: storageentries){
				storagelist.add(b);
			}
			for (CharSequence d: storageentries){
				storagevalues.add(d);
			}
			CharSequence[] entries = storagelist.toArray(staticentries);
			CharSequence[] values = storagevalues.toArray(staticvalues);
			mBootAnimSelect.setEntries(entries);
			mBootAnimSelect.setEntryValues(values);
	}
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

    public CharSequence[] zipFileFilter(String path){
	File f = new File(path);
	File[] g;
	g = f.listFiles(mDirFilter);
	List<CharSequence> ret = new ArrayList<CharSequence>();
	for (File a: g){
		File[] h = a.listFiles(mZipFilter);
		for (File o: h){
			if (o.isFile()){
				ret.add(o.getAbsolutePath());
				Log.i(TAG, " Found file: "+o.getAbsolutePath());
			}
		}
	}
	for (File b: g){
		String dir = b.getAbsolutePath();
		Log.i(TAG, " Found Path: "+dir);
		zipFileFilter(dir);	
		}
		File[] n = f.listFiles(mZipFilter);
			for (File c: n){
				if (c.isFile()){
					ret.add(c.getAbsolutePath());
					Log.i(TAG, " Found file: "+c.getAbsolutePath());
				}
			}
		CharSequence[] ret2 = new CharSequence[ret.size()];
		ret2 = ret.toArray(ret2);
		return ret2;
	}

    public int runStorageCheck(String path) {
	File f = new File(path);
	File[] g = f.listFiles();
	if (g == null){
		return 0;
	} else {
		return 1;
	}			
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
