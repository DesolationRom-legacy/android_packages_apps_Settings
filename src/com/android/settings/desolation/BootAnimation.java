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

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.util.desolation.IOHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
import android.widget.Toast;

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
    private static final String mServer = "http://snuzzo.android-edge.com/Roms/deso/deso-bootanis";
    long ref;
    File bootanimations;
    ConnectivityManager cManager;
    DownloadManager dmgr;
    DownloadManager.Request request;
    NetworkInfo netinfo;
    /* The following are the items which can be downloaded or are currently our prebuilt animations
    Small explanation - mServer contains a dir which holds multiple zip files we base it off
    the baked in stock animation's sizes (480/800/1080/1440). This then gets passed in the form of a property
    to tell our download services which zip to request we then name it as the dir name ending in zip */
    private String[] staticentries = { "Stock", "8-Bit Arcade by @Scar45"	};
    private String[] staticvalues = { "/system/media/bootanimation.zip", "8bitarcade" };
    private int mPrebuiltListLength = staticentries.length;
    /* any questions do not hesitate to comment or email me - Snuzzo */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.boot_animation_settings);
        mBootAnimDisable = (SwitchPreference) findPreference(USE_BOOTANIMATION_KEY);
        mBootAnimSelect = (ListPreference) findPreference(SET_BOOTANIMATION_KEY);
        Log.i(TAG, "BootAnimations are set to "+(mBootAnimDisable.isChecked() ? true:false));
      	cManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
      	dmgr = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
      	netinfo = cManager.getActiveNetworkInfo();
        bootanimations = new File(Environment.getExternalStorageDirectory(), "deso/bootanimations/");
        mStoragePath = bootanimations.getAbsolutePath();
      	IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
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
      	getActivity().getApplicationContext().registerReceiver(receiver, filter);
        Log.i(TAG, "Found "+mPrebuiltListLength+" prebuilt entries: "+Arrays.toString(staticentries));
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
      	List<String> storagelist = new ArrayList<String>();
      	List<String> storagevalues = new ArrayList<String>();
      	for (String a: staticentries){
      		storagelist.add(a);
      	}
      	for (String c: staticvalues){
      		storagevalues.add(c);
      	}
      	if (IOHelper.runStorageCheck(mStoragePath) == 1){
      		String[] storageentries = IOHelper.zipFileFilter(mStoragePath);
      		for (String b: storageentries){
      			storagelist.add(b.replace(mStoragePath, "").replace(".zip", "").replace("/", ""));
      		}
      		for (String d: storageentries){
      			storagevalues.add(d);
      		}
      	}
      	CharSequence[] entries = storagelist.toArray(staticentries);
      	CharSequence[] values = storagevalues.toArray(staticvalues);

      	mBootAnimSelect.setEntries(entries);
      	mBootAnimSelect.setEntryValues(values);
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
      	mBootAnimSelect.setSummary(mBootAnimSelect.getEntries()[index]);
      	if (index == 0){ /*- file copy of our backup to revert baked to our baked in animation -*/
      		CMDProcessor.runSuCommand("sysrw && cp /system/media/bootanimation.backup /system/media/bootanimation.zip && sysro").getStdout();
      	} else if (index > (mPrebuiltListLength - 1)){ /*- This a user selected custom animation -*/
      		CMDProcessor.runSuCommand("sysrw && cp "+String.valueOf((String) newValue)+" /system/media/bootanimation.zip && sysro").getStdout();
      	} else if (index <= (mPrebuiltListLength - 1)) { /*- This a prebuilt downloaded zip file -*/
      		File destination = new File(mStoragePath+"/"+String.valueOf((String) newValue)+".bootani");
      		if (destination.exists()){
      			CMDProcessor.runSuCommand("sysrw && cp "+String.valueOf(destination)+" /system/media/bootanimation.zip && sysro").getStdout();
      		} else {
      			downloadBootani(String.valueOf((String) newValue), mBootAnimSelect.getEntries()[index]);
      		}
      	}
  }

  private void removePreference(Preference preference) {
    	 getPreferenceScreen().removePreference(preference);
  }

  private void downloadBootani(String bootaniname, CharSequence title) {
      	Uri uri = Uri.parse(mServer+"/"+bootaniname+"/"+SystemProperties.get("ro.product.bootanimationsize", null)+".zip");
      	File destination = new File(mStoragePath+"/"+bootaniname+".bootani");
      	request = new Request(uri)
      		.setTitle("Bootanimation: "+title)
      		.setVisibleInDownloadsUi(true)
      		.setDestinationUri(Uri.fromFile(destination));
      	ref = dmgr.enqueue(request);
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
  BroadcastReceiver receiver = new BroadcastReceiver() {
	  @Override
    	  public void onReceive(Context context, Intent intent) {
        		long refCompleted = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        	  	if (refCompleted == ref) {
            			Query myDownloadQuery = new Query();
            			myDownloadQuery.setFilterById(ref);
            			Cursor myDownload = dmgr.query(myDownloadQuery);
            			if (myDownload.moveToFirst()) {
            				int fileNameId = myDownload.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            				String fileUri = myDownload.getString(fileNameId);
            				String path = Uri.parse(fileUri).getPath();
            				CMDProcessor.runSuCommand("sysrw && cp "+path+" /system/media/bootanimation.zip && sysro").getStdout();
            			}
            			myDownload.close();
        			}
    		}
  };
}
