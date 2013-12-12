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

import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

/**
 * System private API for communicating with the application.  This is given to
 * the activity manager by an application  when it starts up, for the activity
 * manager to tell the application about things it needs to do.
 *
 * {@hide}
 */
public interface IApplicationThread extends IInterface {
	static final int DEBUG_OFF = 0;
	static final int DEBUG_ON = 1;
	static final int DEBUG_WAIT = 2;

	void schedulePauseActivity(IBinder token, boolean finished,
			boolean userLeaving, int configChanges);

	void scheduleStopActivity(IBinder token, boolean showWindow,
			int configChanges);

	void scheduleWindowVisibility(IBinder token, boolean showWindow);

	void scheduleResumeActivity(IBinder token, boolean isForward);

	void scheduleSendResult(IBinder token, List<ResultInfo> results);

	void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
			ActivityInfo info, Bundle state, List<ResultInfo> pendingResults,
			List<Intent> pendingNewIntents, boolean notResumed,
			boolean isForward);

	void scheduleDestroyActivity(IBinder token, boolean finished,
			int configChanges);

	void bindApplication(String packageName, ApplicationInfo info,
			ComponentName testName, String profileName, Bundle testArguments,
			int debugMode, boolean restrictedBackupMode);
}
