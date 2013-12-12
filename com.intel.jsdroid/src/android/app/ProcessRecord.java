/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.app;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


import android.content.ComponentName;
import android.content.ContentProviderRecord;
import android.content.ReceiverList;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Full information about a particular process that
 * is currently running.
 */
public class ProcessRecord {
	final ApplicationInfo info; // all about the first app in the process
	public final String processName; // name of the process
	// List of packages running in the process
	final HashSet<String> pkgList = new HashSet<String>();
	public IApplicationThread thread; // the actual proc...  may be null only if
								// 'persistent' is true (in which case we
								// are in the process of launching the app)
	int pid; // The process of this application; 0 if none
	boolean starting; // True if the process is being started
	public long lastActivityTime; // For managing the LRU list
	public long lruWeight; // Weight for ordering in LRU list
	int maxAdj; // Maximum OOM adjustment for this process
	int hiddenAdj; // If hidden, this is the adjustment to use
	int curRawAdj; // Current OOM unlimited adjustment for this process
	int setRawAdj; // Last set OOM unlimited adjustment for this process
	int curAdj; // Current OOM adjustment for this process
	public int setAdj; // Last set OOM adjustment for this process
	int curSchedGroup; // Currently desired scheduling class
	int setSchedGroup; // Last set to background scheduling class
	boolean keeping; // Actively running code so don't kill due to that?
	boolean setIsForeground; // Running foreground UI when last set?
	boolean foregroundServices; // Running any services that are foreground?
	boolean bad; // True if disabled in the bad process list
	boolean killedBackground; // True when proc has been killed due to too many bg
	IBinder forcingToForeground;// Token that is forcing this process to be foreground
	int adjSeq; // Sequence id for identifying oom_adj assignment cycles
	public int lruSeq; // Sequence id for identifying LRU update cycles
	ComponentName instrumentationClass;// class installed to instrument app
	ApplicationInfo instrumentationInfo; // the application being instrumented
	String instrumentationProfileFile; // where to save profiling
	Bundle instrumentationArguments;// as given to us
	ComponentName instrumentationResultClass;// copy of instrumentationClass
	long lastWakeTime; // How long proc held wake lock at last check
	long lastCpuTime; // How long proc has run CPU at last check
	long curCpuTime; // How long proc has run CPU most recently
	long lastRequestedGc; // When we last asked the app to do a gc
	long lastLowMemory; // When we last told the app that memory is low
	boolean reportLowMemory; // Set to true when waiting to report low mem
	boolean empty; // Is this an empty background process?
	boolean hidden; // Is this a hidden process?
	int lastPss; // Last pss size reported by app.
	String adjType; // Debugging: primary thing impacting oom_adj.
	int adjTypeCode; // Debugging: adj code to report to app.
	Object adjSource; // Debugging: option dependent object.
	Object adjTarget; // Debugging: target component impacting oom_adj.

	// contains HistoryRecord objects
	public final ArrayList<ActivityRecord> activities = new ArrayList<ActivityRecord>();

    // all ServiceRecord running in this process
    final HashSet<ServiceRecord> services = new HashSet<ServiceRecord>();
    // services that are currently executing code (need to remain foreground).
    final HashSet<ServiceRecord> executingServices
             = new HashSet<ServiceRecord>();
    // all IIntentReceivers that are registered from this process.
    public final HashSet<ReceiverList> receivers = new HashSet<ReceiverList>();
    // class (String) -> ContentProviderRecord
    public final HashMap<String, ContentProviderRecord> pubProviders
            = new HashMap<String, ContentProviderRecord>(); 
    // All ContentProviderRecord process is using
    public final HashMap<ContentProviderRecord, Integer> conProviders
            = new HashMap<ContentProviderRecord, Integer>(); 

	boolean persistent; // always keep this application running?
	boolean crashing; // are we in the process of crashing?
	Dialog crashDialog; // dialog being displayed due to crash.
	boolean notResponding; // does the app have a not responding dialog?
	Dialog anrDialog; // dialog being displayed due to app not resp.
	boolean removed; // has app package been removed from device?
	boolean debugging; // was app launched for debugging?
	boolean waitedForDebugger; // has process show wait for debugger dialog?
	Dialog waitDialog; // current wait for debugger dialog

	String shortStringName; // caching of toShortString() result.
	String stringName; // caching of toString() result.

	// Who will be notified of the error. This is usually an activity in the
	// app that installed the package.
	ComponentName errorReportReceiver;

	ProcessRecord(IApplicationThread _thread, ApplicationInfo _info,
			String _processName) {
		info = _info;
		processName = _processName;
		pkgList.add(_info.packageName);
		thread = _thread;
		curRawAdj = setRawAdj = -100;
		curAdj = setAdj = -100;
		persistent = false;
		removed = false;
	}

	public void setPid(int _pid) {
		pid = _pid;
		shortStringName = null;
		stringName = null;
	}

	/**
	 * This method returns true if any of the activities within the process record are interesting
	 * to the user. See HistoryRecord.isInterestingToUserLocked()
	 */
	public boolean isInterestingToUserLocked() {
		final int size = activities.size();
		for (int i = 0; i < size; i++) {
			ActivityRecord r = activities.get(i);
			if (r.isInterestingToUserLocked()) {
				return true;
			}
		}
		return false;
	}

	//	public void stopFreezingAllLocked() {
	//		int i = activities.size();
	//		while (i > 0) {
	//			i--;
	//			activities.get(i).stopFreezingScreenLocked(true);
	//		}
	//	}

	public String toShortString() {
		if (shortStringName != null) {
			return shortStringName;
		}
		StringBuilder sb = new StringBuilder(128);
		toShortString(sb);
		return shortStringName = sb.toString();
	}

	void toShortString(StringBuilder sb) {
		//sb.append(Integer.toHexString(System.identityHashCode(this)));
		sb.append(' ');
		sb.append(pid);
		sb.append(':');
		sb.append(processName);
		sb.append('/');
		sb.append(info.uid);
	}

	public String toString() {
		if (stringName != null) {
			return stringName;
		}
		StringBuilder sb = new StringBuilder(128);
		sb.append("ProcessRecord{");
		toShortString(sb);
		sb.append('}');
		return stringName = sb.toString();
	}

	/*
	 *  Return true if package has been added false if not
	 */
	public boolean addPackage(String pkg) {
		if (!pkgList.contains(pkg)) {
			pkgList.add(pkg);
			return true;
		}
		return false;
	}

	/*
	 *  Delete all packages from list except the package indicated in info
	 */
	public void resetPackageList() {
		pkgList.clear();
		pkgList.add(info.packageName);
	}

	public String[] getPackageList() {
		int size = pkgList.size();
		if (size == 0) {
			return null;
		}
		String list[] = new String[size];
		pkgList.toArray(list);
		return list;
	}
}
