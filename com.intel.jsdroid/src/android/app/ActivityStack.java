/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.app.ActivityManager.START_CLASS_NOT_FOUND;
import static android.app.ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT;
import static android.app.ActivityManager.START_INTENT_NOT_RESOLVED;
import static android.app.ActivityManager.START_SUCCESS;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * State and management of a single stack of activities.
 */
public class ActivityStack {
	static final String TAG = "ActivityStack>>>";

	// How long we wait until giving up on the last activity telling us it
	// is idle.
	static final int IDLE_TIMEOUT = 10 * 1000;

	// How long we wait until giving up on the last activity to pause.  This
	// is short because it directly impacts the responsiveness of starting the
	// next activity.
	static final int PAUSE_TIMEOUT = 500;

	// How long we can hold the launch wake lock before giving up.
	//	static final int LAUNCH_TIMEOUT = Integer.getInteger(
	//			"com.android.server.am.ActivityStack.launch_timeout", 10 * 1000);

	// How long we wait until giving up on an activity telling us it has
	// finished destroying itself.
	static final int DESTROY_TIMEOUT = 10 * 1000;

	// How long until we reset a task when the user returns to it.  Currently
	// 30 minutes.
	static final long ACTIVITY_INACTIVE_RESET_TIME = 1000 * 60 * 30;

	// How long between activity launches that we consider safe to not warn
	// the user about an unexpected activity being launched on top.
	static final long START_WARN_TIME = 5 * 1000;

	// Set to false to disable the preview that is shown while a new activity
	// is being started.
	static final boolean SHOW_APP_STARTING_PREVIEW = true;

	public static final class ActivityState {
		public static final int INITIALIZING = 0;
		public static final int RESUMED = 1;
		public static final int PAUSING = 2;
		public static final int PAUSED = 3;
		public static final int STOPPING = 4;
		public static final int STOPPED = 5;
		public static final int FINISHING = 6;
		public static final int DESTROYING = 7;
		public static final int DESTROYED = 8;
	}

	final ActivityManager mService;
	final boolean mMainStack;

	final Context mContext;

	/**
	 * The back history of all previous (and possibly still
	 * running) activities.  It contains HistoryRecord objects.
	 */
	@SuppressWarnings("rawtypes")
	final ArrayList mHistory = new ArrayList();

	/**
	 * List of running activities, sorted by recent usage.
	 * The first entry in the list is the least recently used.
	 * It contains HistoryRecord objects.
	 */
	@SuppressWarnings("rawtypes")
	final ArrayList mLRUActivities = new ArrayList();

	/**
	 * List of activities that are waiting for a new activity
	 * to become visible before completing whatever operation they are
	 * supposed to do.
	 */
	final ArrayList<ActivityRecord> mWaitingVisibleActivities = new ArrayList<ActivityRecord>();

	/**
	 * List of activities that are ready to be stopped, but waiting
	 * for the next activity to settle down before doing so.  It contains
	 * HistoryRecord objects.
	 */
	final ArrayList<ActivityRecord> mStoppingActivities = new ArrayList<ActivityRecord>();

	/**
	 * Animations that for the current transition have requested not to
	 * be considered for the transition animation.
	 */
	final ArrayList<ActivityRecord> mNoAnimActivities = new ArrayList<ActivityRecord>();

	/**
	 * List of activities that are ready to be finished, but waiting
	 * for the previous activity to settle down before doing so.  It contains
	 * HistoryRecord objects.
	 */
	final ArrayList<ActivityRecord> mFinishingActivities = new ArrayList<ActivityRecord>();

	/**
	 * When we are in the process of pausing an activity, before starting the
	 * next one, this variable holds the activity that is currently being paused.
	 */
	ActivityRecord mPausingActivity = null;

	/**
	 * This is the last activity that we put into the paused state.  This is
	 * used to determine if we need to do an activity transition while sleeping,
	 * when we normally hold the top activity paused.
	 */
	ActivityRecord mLastPausedActivity = null;

	/**
	 * Current activity that is resumed, or null if there is none.
	 */
	ActivityRecord mResumedActivity = null;

	/**
	 * This is the last activity that has been started.  It is only used to
	 * identify when multiple activities are started at once so that the user
	 * can be warned they may not be in the activity they think they are.
	 */
	ActivityRecord mLastStartedActivity = null;

	/**
	 * Set when we know we are going to be calling updateConfiguration()
	 * soon, so want to skip intermediate config checks.
	 */
	boolean mConfigWillChange;

	/**
	 * Set to indicate whether to issue an onUserLeaving callback when a
	 * newly launched activity is being brought in front of us.
	 */
	boolean mUserLeaving = false;

	long mInitialStartTime = 0;

	static final int PAUSE_TIMEOUT_MSG = 9;
	static final int IDLE_TIMEOUT_MSG = 10;
	static final int IDLE_NOW_MSG = 11;
	static final int LAUNCH_TIMEOUT_MSG = 16;
	static final int DESTROY_TIMEOUT_MSG = 17;
	static final int RESUME_TOP_ACTIVITY_MSG = 19;

	final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PAUSE_TIMEOUT_MSG: {
				IBinder token = (IBinder) msg.obj;
				// We don't at this point know if the activity is fullscreen,
				// so we need to be conservative and assume it isn't.
				Log.i(TAG, "Activity pause timeout for " + token);
				activityPaused(token, null, true);
			}
				break;
			case IDLE_TIMEOUT_MSG: {
				//				if (mService.mDidDexOpt) {
				//					mService.mDidDexOpt = false;
				//					Message nmsg = mHandler.obtainMessage(IDLE_TIMEOUT_MSG);
				//					nmsg.obj = msg.obj;
				//					mHandler.sendMessageDelayed(nmsg, IDLE_TIMEOUT);
				//					return;
				//				}
				// We don't at this point know if the activity is fullscreen,
				// so we need to be conservative and assume it isn't.
				//				IBinder token = (IBinder) msg.obj;
				//				System.out.println(TAG, "Activity idle timeout for " + token);
				//				activityIdleInternal(token, true, null);
			}
				break;
			case DESTROY_TIMEOUT_MSG: {
				//				IBinder token = (IBinder) msg.obj;
				// We don't at this point know if the activity is fullscreen,
				// so we need to be conservative and assume it isn't.
				//				System.out.println(TAG, "Activity destroy timeout for " + token);
				//				activityDestroyed(token);
			}
				break;
			case IDLE_NOW_MSG: {
				IBinder token = (IBinder) msg.obj;
				activityIdleInternal(token, false, null);
			}
				break;
			case LAUNCH_TIMEOUT_MSG: {
				//				if (mService.mDidDexOpt) {
				//					mService.mDidDexOpt = false;
				//					Message nmsg = mHandler.obtainMessage(LAUNCH_TIMEOUT_MSG);
				//					mHandler.sendMessageDelayed(nmsg, LAUNCH_TIMEOUT);
				//					return;
				//				}
				//				synchronized (mService) {
				//					if (mLaunchingActivity.isHeld()) {
				//						System.out.println(TAG,
				//								"Launch timeout has expired, giving up wake lock!");
				//						mLaunchingActivity.release();
				//					}
				//				}
			}
				break;
			case RESUME_TOP_ACTIVITY_MSG: {
				resumeTopActivityLocked(null);
			}
				break;
			}
		}
	};

	ActivityStack(ActivityManager service, Context context, boolean mainStack) {
		mService = service;
		mContext = context;
		mMainStack = mainStack;
	}

	final ActivityRecord topRunningActivityLocked(ActivityRecord notTop) {
		int i = mHistory.size() - 1;
		while (i >= 0) {
			ActivityRecord r = (ActivityRecord) mHistory.get(i);
			if (!r.finishing && r != notTop) {
				return r;
			}
			i--;
		}
		return null;
	}

	final ActivityRecord topRunningNonDelayedActivityLocked(
			ActivityRecord notTop) {
		int i = mHistory.size() - 1;
		while (i >= 0) {
			ActivityRecord r = (ActivityRecord) mHistory.get(i);
			if (!r.finishing && !r.delayedResume && r != notTop) {
				return r;
			}
			i--;
		}
		return null;
	}

	/**
	 * This is a simplified version of topRunningActivityLocked that provides a number of
	 * optional skip-over modes.  It is intended for use with the ActivityController hook only.
	 * 
	 * @param token If non-null, any history records matching this token will be skipped.
	 * @param taskId If non-zero, we'll attempt to skip over records with the same task ID.
	 * 
	 * @return Returns the HistoryRecord of the next activity on the stack.
	 */
	final ActivityRecord topRunningActivityLocked(IBinder token, int taskId) {
		int i = mHistory.size() - 1;
		while (i >= 0) {
			ActivityRecord r = (ActivityRecord) mHistory.get(i);
			// Note: the taskId check depends on real taskId fields being non-zero
			if (!r.finishing && (token != r) && (taskId != r.task.taskId)) {
				return r;
			}
			i--;
		}
		return null;
	}

	final int indexOfTokenLocked(IBinder token) {
		int count = mHistory.size();

		// convert the token to an entry in the history.
		int index = -1;
		for (int i = count - 1; i >= 0; i--) {
			Object o = mHistory.get(i);
			if (o == token) {
				index = i;
				break;
			}
		}

		return index;
	}

	//	private final boolean updateLRUListLocked(ActivityRecord r) {
	//		final boolean hadit = mLRUActivities.remove(r);
	//		mLRUActivities.add(r);
	//		return hadit;
	//	}

	/**
	 * Returns the top activity in any existing task matching the given
	 * Intent.  Returns null if no such task is found.
	 */
	private ActivityRecord findTaskLocked(Intent intent, ActivityInfo info) {
		ComponentName cls = intent.getComponent();
		if (info.targetActivity != null) {
			cls = new ComponentName(info.packageName, info.targetActivity);
		}

		TaskRecord cp = null;

		final int N = mHistory.size();
		for (int i = (N - 1); i >= 0; i--) {
			ActivityRecord r = (ActivityRecord) mHistory.get(i);
			if (!r.finishing && r.task != cp
					&& r.launchMode != ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
				cp = r.task;
				//System.out.println(TAG, "Comparing existing cls=" + r.task.intent.getComponent().flattenToShortString()
				//        + "/aff=" + r.task.affinity + " to new cls="
				//        + intent.getComponent().flattenToShortString() + "/aff=" + taskAffinity);
				if (r.task.affinity != null) {
					if (r.task.affinity.equals(info.taskAffinity)) {
						//System.out.println(TAG, "Found matching affinity!");
						return r;
					}
				} else if (r.task.intent != null
						&& r.task.intent.getComponent().equals(cls)) {
					//System.out.println(TAG, "Found matching class!");
					//dump();
					//System.out.println(TAG, "For Intent " + intent + " bringing to top: " + r.intent);
					return r;
				} else if (r.task.affinityIntent != null
						&& r.task.affinityIntent.getComponent().equals(cls)) {
					//System.out.println(TAG, "Found matching class!");
					//dump();
					//System.out.println(TAG, "For Intent " + intent + " bringing to top: " + r.intent);
					return r;
				}
			}
		}

		return null;
	}

	/**
	 * Returns the first activity (starting from the top of the stack) that
	 * is the same as the given activity.  Returns null if no such activity
	 * is found.
	 */
	private ActivityRecord findActivityLocked(Intent intent, ActivityInfo info) {
		ComponentName cls = intent.getComponent();
		if (info.targetActivity != null) {
			cls = new ComponentName(info.packageName, info.targetActivity);
		}

		final int N = mHistory.size();
		for (int i = (N - 1); i >= 0; i--) {
			ActivityRecord r = (ActivityRecord) mHistory.get(i);
			if (!r.finishing) {
				if (r.intent.getComponent().equals(cls)) {
					return r;
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app,
			boolean andResume, boolean checkConfig) {
		// TODO 利用windowmanager进行屏幕visible变幻的
		//		r.startFreezingScreenLocked(app, 0);
		//		mService.mWindowManager.setAppVisibility(r, true);

		r.app = app;
//		System.out.println(TAG + "Launching: " + r.info.name);

		int idx = app.activities.indexOf(r);
		if (idx < 0) {
			app.activities.add(r);
		}

		//		try {
		if (app.thread == null) {
//			System.out.println(TAG + "app.thread == null");
			return false;
		}
		List<ResultInfo> results = null;
		List<Intent> newIntents = null;
		if (andResume) {
			results = r.results;
			newIntents = r.newIntents;
		}
        if (andResume) {
            // As part of the process of launching, ActivityThread also performs
            // a resume.
            r.state = ActivityState.RESUMED;
            r.icicle = null;
            r.haveState = false;
            r.stopped = false;
            mResumedActivity = r;
            completeResumeLocked(r);
        } else {
            // This activity is not starting in the resumed state... which
            // should look like we asked it to pause+stop (but remain visible),
            // and it has done so and reported back the current icicle and
            // other state.
            r.state = ActivityState.STOPPED;
            r.stopped = true;
        }
		app.thread.scheduleLaunchActivity(new Intent(r.intent), r, 0, r.info,
				r.icicle, results, newIntents, !andResume, false);
		return true;
	}

	private final void startSpecificActivityLocked(ActivityRecord r,
			boolean andResume, boolean checkConfig) {
		// Is this activity's application already running?
		ProcessRecord app = mService.getProcessRecordLocked(r.processName);

		if (app != null && app.thread != null) {
			realStartActivityLocked(r, app, andResume, checkConfig);
			return;
		}

		mService.startProcessLocked(r.processName, r.info.applicationInfo,
				true, 0, "activity", r.intent.getComponent(), false);
	}

	private final void startPausingLocked(boolean userLeaving,
			boolean uiSleeping) {
		Log.i(TAG, "in startPausingLocked");
		if (mPausingActivity != null) {
//			System.out.println(TAG
//					+ "Trying to pause when pause is already pending for "
//					+ mPausingActivity.info.name);
		}
		ActivityRecord prev = mResumedActivity;
		if (prev == null) {
//			System.out.println(TAG + "Trying to pause when nothing is resumed");
			resumeTopActivityLocked(null);
			return;
		}
//		System.out.println(TAG + "Start pausing: " + prev.info.name);
		mResumedActivity = null;
		mPausingActivity = prev;
		mLastPausedActivity = prev;
		prev.state = ActivityState.PAUSING;

		if (prev.app != null && prev.app.thread != null) {
			prev.app.thread.schedulePauseActivity(prev, prev.finishing,
					userLeaving, prev.configChangeFlags);
		} else {
			mPausingActivity = null;
			mLastPausedActivity = null;
		}

		if (mPausingActivity != null) {
			// Have the window manager pause its key dispatching until the new
			// activity has started.  If we're pausing the activity just because
			// the screen is being turned off and the UI is sleeping, don't interrupt
			// key dispatch; the same activity will pick it up again on wakeup.
			if (uiSleeping == false) {
				prev.pauseKeyDispatchingLocked();
			}
			// Schedule a pause timeout in case the app doesn't respond.
			// We don't give it much time because this directly impacts the
			// responsiveness seen by the user.
			Message msg = mHandler.obtainMessage(PAUSE_TIMEOUT_MSG);
			msg.obj = prev;
			mHandler.sendMessageDelayed(msg, PAUSE_TIMEOUT);
		} else {
			// This activity failed to schedule the
			// pause, so just treat it as being paused now.
			//			if (DEBUG_PAUSE)
			//				System.out.println(TAG, "Activity not running, resuming next.");
			resumeTopActivityLocked(null);
		}
	}

	final void activityPaused(IBinder token, Bundle icicle, boolean timeout) {
		Log.i(TAG, "Activity paused: token=" + ((ActivityRecord) token).info.name);

		ActivityRecord r = null;

		int index = indexOfTokenLocked(token);
		if (index >= 0) {
			r = (ActivityRecord) mHistory.get(index);
			if (!timeout) {
				r.icicle = icicle;
				r.haveState = true;
			}
			mHandler.removeMessages(PAUSE_TIMEOUT_MSG, r);
			if (mPausingActivity == r) {
				r.state = ActivityState.PAUSED;
				completePauseLocked();
			} else {
				Log.i(TAG, "impossible here! mPausingActivity != r");
				//					EventLog.writeEvent(
				//							EventLogTags.AM_FAILED_TO_PAUSE,
				//							System.identityHashCode(r),
				//							r.shortComponentName,
				//							mPausingActivity != null ? mPausingActivity.shortComponentName
				//									: "(none)");
			}
		}
	}
    private final ActivityRecord finishCurrentActivityLocked(ActivityRecord r,
            int mode) {
        final int index = indexOfTokenLocked(r);
        if (index < 0) {
            return null;
        }

        return finishCurrentActivityLocked(r, index, mode);
    }
	private final void completePauseLocked() {
		ActivityRecord prev = mPausingActivity;
		Log.i(TAG, "Complete pause: " + prev.info.name);

		if (prev != null) {
			if (prev.finishing) {
				Log.i(TAG, "prev.finishing is true...");
				//				if (DEBUG_PAUSE)
				//					System.out.println(TAG, "Executing finish of activity: " + prev);
				prev = finishCurrentActivityLocked(prev, FINISH_AFTER_VISIBLE);
			} else if (prev.app != null) {
				//				if (DEBUG_PAUSE)
				//					System.out.println(TAG, "Enqueueing pending stop: " + prev);
				if (prev.waitingVisible) {
					prev.waitingVisible = false;
					mWaitingVisibleActivities.remove(prev);
					//					if (DEBUG_SWITCH || DEBUG_PAUSE)
					//						System.out.println(TAG, "Complete pause, no longer waiting: "
					//								+ prev);
				}
				if (prev.configDestroy) {
					Log.i(TAG, "impossible here! prev.configDestroy is true");
					// The previous is being paused because the configuration
					// is changing, which means it is actually stopping...
					// To juggle the fact that we are also starting a new
					// instance right now, we need to first completely stop
					// the current instance before starting the new one.
					//					if (DEBUG_PAUSE)
					//						System.out.println(TAG, "Destroying after pause: " + prev);
					//					destroyActivityLocked(prev, true);
				} else {
					mStoppingActivities.add(prev);
					if (mStoppingActivities.size() > 3) {
						// If we already have a few activities waiting to stop,
						// then give up on things going idle and start clearing
						// them out.
						Log.i(TAG, "To many pending stops, forcing idle");
						Message msg = Message.obtain();
						msg.what = IDLE_NOW_MSG;
						mHandler.sendMessage(msg);
					}
				}
			} else {
				//				if (DEBUG_PAUSE)
				//					System.out.println(TAG, "App died during pause, not stopping: " + prev);
				Log.i(TAG, "can come here?");
				prev = null;
			}
			mPausingActivity = null;
		}

		resumeTopActivityLocked(prev);

		if (prev != null) {
			prev.resumeKeyDispatchingLocked();
		}
	}

	/**
	 * Once we know that we have asked an application to put an activity in
	 * the resumed state (either by launching it or explicitly telling it),
	 * this function updates the rest of our state to match that fact.
	 */
	private final void completeResumeLocked(ActivityRecord next) {
		next.idle = false;
		next.results = null;
		next.newIntents = null;

		if (mMainStack) {
			mService.setFocusedActivityLocked(next);
		}
		next.resumeKeyDispatchingLocked();
		ensureActivitiesVisibleLocked(null, 0);
		//		mService.mWindowManager.executeAppTransition();
		mNoAnimActivities.clear();
	}

	/**
	 * Make sure that all activities that need to be visible (that is, they
	 * currently can be seen by the user) actually are.
	 */
	final void ensureActivitiesVisibleLocked(ActivityRecord top,
			ActivityRecord starting, String onlyThisProcess, int configChanges) {
//		if (DEBUG_VISBILITY)
			Log.i(TAG, "ensureActivitiesVisible behind " + top
					+ " configChanges=0x" + Integer.toHexString(configChanges));

		// If the top activity is not fullscreen, then we need to
		// make sure any activities under it are now visible.
		final int count = mHistory.size();
		int i = count - 1;
		while (mHistory.get(i) != top) {
			i--;
		}
		ActivityRecord r;
		boolean behindFullscreen = false;
		for (; i >= 0; i--) {
			r = (ActivityRecord) mHistory.get(i);
//			if (DEBUG_VISBILITY)
//				System.out.println(TAG, "Make visible? " + r + " finishing=" + r.finishing
//						+ " state=" + r.state);
			if (r.finishing) {
				continue;
			}

			final boolean doThisProcess = onlyThisProcess == null
					|| onlyThisProcess.equals(r.processName);

			// First: if this is not the current activity being started, make
			// sure it matches the current configuration.
			if (r != starting && doThisProcess) {
//				ensureActivityConfigurationLocked(r, 0);
			}

			if (r.app == null || r.app.thread == null) {
				if (onlyThisProcess == null
						|| onlyThisProcess.equals(r.processName)) {
					// This activity needs to be visible, but isn't even
					// running...  get it started, but don't resume it
					// at this point.
//					if (DEBUG_VISBILITY)
//						System.out.println(TAG, "Start and freeze screen for " + r);
					if (r != starting) {
//						r.startFreezingScreenLocked(r.app, configChanges);
					}
					if (!r.visible) {
//						if (DEBUG_VISBILITY)
//							System.out.println(TAG, "Starting and making visible: " + r);
//						mService.mWindowManager.setAppVisibility(r, true);
					}
					if (r != starting) {
						startSpecificActivityLocked(r, false, false);
					}
				}

			} else if (r.visible) {
				// If this activity is already visible, then there is nothing
				// else to do here.
//				if (DEBUG_VISBILITY)
//					System.out.println(TAG, "Skipping: already visible at " + r);
//				r.stopFreezingScreenLocked(false);

			} else if (onlyThisProcess == null) {
				// This activity is not currently visible, but is running.
				// Tell it to become visible.
				r.visible = true;
				if (r.state != ActivityState.RESUMED && r != starting) {
					// If this activity is paused, tell it
					// to now show its window.
//					if (DEBUG_VISBILITY)
//						System.out.println(TAG,
//								"Making visible and scheduling visibility: "
//										+ r);
					try {
//						mService.mWindowManager.setAppVisibility(r, true);
						r.app.thread.scheduleWindowVisibility(r, true);
//						r.stopFreezingScreenLocked(false);
					} catch (Exception e) {
						// Just skip on any failure; we'll make it
						// visible when it next restarts.
						Log.i(TAG, "Exception thrown making visibile: " + r.intent.getComponent(), e);
					}
				}
			}

			// Aggregate current change flags.
			configChanges |= r.configChangeFlags;

			if (r.fullscreen) {
				// At this point, nothing else needs to be shown
//				if (DEBUG_VISBILITY)
//					System.out.println(TAG, "Stopping: fullscreen at " + r);
				behindFullscreen = true;
				i--;
				break;
			}
		}

		// Now for any activities that aren't visible to the user, make
		// sure they no longer are keeping the screen frozen.
		while (i >= 0) {
			r = (ActivityRecord) mHistory.get(i);
//			if (DEBUG_VISBILITY)
//				System.out.println(TAG, "Make invisible? " + r + " finishing="
//						+ r.finishing + " state=" + r.state
//						+ " behindFullscreen=" + behindFullscreen);
			if (!r.finishing) {
				if (behindFullscreen) {
					if (r.visible) {
//						if (DEBUG_VISBILITY)
							Log.i(TAG, "Making invisible: " + r);
						r.visible = false;
						try {
//							mService.mWindowManager.setAppVisibility(r, false);
							if ((r.state == ActivityState.STOPPING || r.state == ActivityState.STOPPED)
									&& r.app != null && r.app.thread != null) {
//								if (DEBUG_VISBILITY)
//									System.out.println(TAG, "Scheduling invisibility: " + r);
								r.app.thread.scheduleWindowVisibility(r, false);
							}
						} catch (Exception e) {
							// Just skip on any failure; we'll make it
							// visible when it next restarts.
//							System.out.println(TAG, "Exception thrown making hidden: "
//									+ r.intent.getComponent(), e);
						}
					} else {
//						if (DEBUG_VISBILITY)
							Log.i(TAG, "Already invisible: " + r);
					}
				} else if (r.fullscreen) {
//					if (DEBUG_VISBILITY)
						Log.i(TAG, "Now behindFullscreen: " + r);
					behindFullscreen = true;
				}
			}
			i--;
		}
	}

	/**
	 * Version of ensureActivitiesVisible that can easily be called anywhere.
	 */
	final void ensureActivitiesVisibleLocked(ActivityRecord starting,
			int configChanges) {
		ActivityRecord r = topRunningActivityLocked(null);
		if (r != null) {
			ensureActivitiesVisibleLocked(r, starting, null, configChanges);
		}
	}

	/**
	 * Ensure that the top activity in the stack is resumed.
	 *
	 * @param prev The previously resumed activity, for when in the process
	 * of pausing; can be null to call from elsewhere.
	 *
	 * @return Returns true if something is being resumed, or false if
	 * nothing happened.
	 */
	final boolean resumeTopActivityLocked(ActivityRecord prev) {
		Log.i(TAG, "in resumeTopActivityLocked");
		// Find the first activity that is not finishing.
		ActivityRecord next = topRunningActivityLocked(null);

		// Remember how we'll process this pause/resume situation, and ensure
		// that the state is reset however we wind up proceeding.
		final boolean userLeaving = mUserLeaving;
		mUserLeaving = false;

		if (next == null) {
			// There are no more activities!  Let's just start up the
			// Launcher...
			if (mMainStack) {
				return mService.startHomeActivityLocked();
			}
		} else {
//			System.out.println(TAG + "top running activity is "
//					+ next.info.name + ", will/already in process: "
//					+ next.processName);
		}

		next.delayedResume = false;

		// If the top activity is the resumed one, nothing to do.
		if (mResumedActivity == next && next.state == ActivityState.RESUMED) {
			// Make sure we have executed any pending transitions, since there
			// should be nothing left to do at this point.
			//			mService.mWindowManager.executeAppTransition();
			mNoAnimActivities.clear();
			//			System.out.println("come here finally");
			return false;
		}

		// The activity may be waiting for stop, but that is no longer
		// appropriate for it.
		mStoppingActivities.remove(next);
		mWaitingVisibleActivities.remove(next);

		//		System.out.println(TAG + "Resuming " + next.info.name);

		// If we are currently pausing an activity, then don't do anything
		// until that is done.
		//		System.out.println("mPausingActivity is null? "
		//				+ (mPausingActivity == null) + ", mResumedActivity is null? "
		//				+ (mResumedActivity == null));
		if (mPausingActivity != null) {
			return false;
		}

		// We need to start pausing the current activity so the top one
		// can be resumed...
		if (mResumedActivity != null) {
			startPausingLocked(userLeaving, false);
			return true;
		}

		if (prev != null && prev != next) {
			if (!prev.waitingVisible && next != null && !next.nowVisible) {
				prev.waitingVisible = true;
				mWaitingVisibleActivities.add(prev);
				//				if (DEBUG_SWITCH)
				//					System.out.println(TAG, "Resuming top, waiting visible to hide: "
				//							+ prev);
			} else {
				// The next activity is already visible, so hide the previous
				// activity's windows right now so we can show the new one ASAP.
				// We only do this if the previous is finishing, which should mean
				// it is on top of the one being resumed so hiding it quickly
				// is good.  Otherwise, we want to do the normal route of allowing
				// the resumed activity to be shown so we can decide if the
				// previous should actually be hidden depending on whether the
				// new one is found to be full-screen or not.
				if (prev.finishing) {
					//					mService.mWindowManager.setAppVisibility(prev, false);
					//					if (DEBUG_SWITCH)
					//						System.out.println(TAG, "Not waiting for visible to hide: " + prev
					//								+ ", waitingVisible="
					//								+ (prev != null ? prev.waitingVisible : null)
					//								+ ", nowVisible=" + next.nowVisible);
				} else {
					//					if (DEBUG_SWITCH)
					//						System.out.println(TAG,
					//								"Previous already visible but still waiting to hide: "
					//										+ prev
					//										+ ", waitingVisible="
					//										+ (prev != null ? prev.waitingVisible
					//												: null) + ", nowVisible="
					//										+ next.nowVisible);
				}
			}
		}

		// We are starting up the next activity, so tell the window manager
		// that the previous one will be hidden soon.  This way it can know
		// to ignore it when computing the desired screen orientation.
		if (prev != null) {
			if (prev.finishing) {
				//				if (DEBUG_TRANSITION)
				//					System.out.println(TAG, "Prepare close transition: prev=" + prev);
				if (mNoAnimActivities.contains(prev)) {
					//					mService.mWindowManager
					//							.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
				} else {
					//					mService.mWindowManager
					//							.prepareAppTransition(prev.task == next.task ? WindowManagerPolicy.TRANSIT_ACTIVITY_CLOSE
					//									: WindowManagerPolicy.TRANSIT_TASK_CLOSE);
				}
				//				mService.mWindowManager.setAppWillBeHidden(prev);
				//				mService.mWindowManager.setAppVisibility(prev, false);
			} else {
				//				if (DEBUG_TRANSITION)
				//					System.out.println(TAG, "Prepare open transition: prev=" + prev);
				if (mNoAnimActivities.contains(next)) {
					//					mService.mWindowManager
					//							.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
				} else {
					//					mService.mWindowManager
					//							.prepareAppTransition(prev.task == next.task ? WindowManagerPolicy.TRANSIT_ACTIVITY_OPEN
					//									: WindowManagerPolicy.TRANSIT_TASK_OPEN);
				}
			}
			//			if (false) {
			//				mService.mWindowManager.setAppWillBeHidden(prev);
			//				mService.mWindowManager.setAppVisibility(prev, false);
			//			}
		} else if (mHistory.size() > 1) {
			//			if (DEBUG_TRANSITION)
			//				System.out.println(TAG, "Prepare open transition: no previous");
			if (mNoAnimActivities.contains(next)) {
				//				mService.mWindowManager
				//						.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
			} else {
				//				mService.mWindowManager
				//						.prepareAppTransition(WindowManagerPolicy.TRANSIT_ACTIVITY_OPEN);
			}
		}
		if (next.app != null && next.app.thread != null) {
//			System.out.println(TAG + "Resume running: " + next.info.name);
			/**/
			// This activity is now becoming visible.
			//			mService.mWindowManager.setAppVisibility(next, true);

			next.state = ActivityState.RESUMED;
			mResumedActivity = next;
			// Have the window manager re-evaluate the orientation of
			// the screen based on the new activity order.
			//			boolean updated = false;
			//			if (!updated) {
			//				// The configuration update wasn't able to keep the existing
			//				// instance of the activity, and instead started a new one.
			//				// We should be all done, but let's just make sure our activity
			//				// is still at the top and schedule another run if something
			//				// weird happened.
			//				ActivityRecord nextNext = topRunningActivityLocked(null);
			//				//				if (DEBUG_SWITCH)
			//				//					System.out.println(TAG, "Activity config changed during resume: "
			//				//							+ next + ", new next: " + nextNext);
			//				if (nextNext != next) {
			//					// Do over!
			//					System.out.println(TAG + "come here nextNext != next");
			//					mHandler.sendEmptyMessage(RESUME_TOP_ACTIVITY_MSG);
			//				}
			//				if (mMainStack) {
			//					mService.setFocusedActivityLocked(next);
			//				}
			//				//				ensureActivitiesVisibleLocked(null, 0);
			//				//				mService.mWindowManager.executeAppTransition();
			//				mNoAnimActivities.clear();
			//				return true;
			//			}

			// Deliver all pending results.
			ArrayList a = next.results;
			if (a != null) {
				final int N = a.size();
				if (!next.finishing && N > 0) {
					//						if (DEBUG_RESULTS)
					//							System.out.println(TAG, "Delivering results to " + next + ": "
					//									+ a);
					next.app.thread.scheduleSendResult(next, a);
				}
			}

			if (next.newIntents != null) {
//				System.out.println(TAG
//						+ "impossible here! next.newIntents != null");
				//				next.app.thread.scheduleNewIntent(next.newIntents, next);
			}

			// TODO 这里是false么？isForward
			next.app.thread.scheduleResumeActivity(next, false);

			// From this point on, if something goes wrong there is no way
			// to recover the activity.
			next.visible = true;
			completeResumeLocked(next);

			// Didn't need to use the icicle, and it is now out of date.
			next.icicle = null;
			next.haveState = false;
			next.stopped = false;
			/**/
		} else {
			// Whoops, need to restart this activity!
			if (!next.hasBeenLaunched) {
				next.hasBeenLaunched = true;
			} else {
				//				if (SHOW_APP_STARTING_PREVIEW) {
				//					mService.mWindowManager.setAppStartingWindow(next,
				//							next.packageName, next.theme,
				//							next.nonLocalizedLabel, next.labelRes, next.icon,
				//							null, true);
				//				}
				//				if (DEBUG_SWITCH)
				//					System.out.println(TAG, "Restarting: " + next);
			}
			startSpecificActivityLocked(next, true, true);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private final void startActivityLocked(ActivityRecord r, boolean newTask,
			boolean doResume) {
		Log.i(TAG, "in private startActivityLocked");
		final int NH = mHistory.size();

		int addPos = -1;

		if (!newTask) {
			//			System.out.println(TAG + "newTask is false");
			// If starting in an existing task, find where that is...
			boolean startIt = true;
			for (int i = NH - 1; i >= 0; i--) {
				ActivityRecord p = (ActivityRecord) mHistory.get(i);
				if (p.finishing) {
					continue;
				}
				if (p.task == r.task) {
					// Here it is!  Now, if this is not yet visible to the
					// user, then just add it without starting; it will
					// get started when the user navigates back to it.
					addPos = i + 1;
					if (!startIt) {
						//						System.out.println("startIt is false");
						mHistory.add(addPos, r);
						// correctly set zIndex, 3 layers
						r.info.zIndex = 3 * (mHistory.size() - 1);
						r.inHistory = true;
						r.task.numActivities++;
						//						mService.mWindowManager.addAppToken(addPos, r,
						//								r.task.taskId, r.info.screenOrientation,
						//								r.fullscreen);
						return;
					}
					break;
				}
				if (p.fullscreen) {
					startIt = false;
				}
			}
		}

		// Place a new activity at top of stack, so it is next to interact
		// with the user.
		if (addPos < 0) {
			addPos = NH;
		}

		// If we are not placing the new activity frontmost, we do not want
		// to deliver the onUserLeaving callback to the actual frontmost
		// activity
		if (addPos < NH) {
			mUserLeaving = false;
//			System.out.println(TAG
//					+ "startActivity() behind front, mUserLeaving=false");
		}

		// Slot the activity into the history stack and proceed
		mHistory.add(addPos, r);
		// correctly set zIndex, 3 layers
		r.info.zIndex = 3 * (mHistory.size() - 1);
		r.inHistory = true;
		r.frontOfTask = newTask;
		r.task.numActivities++;
//		System.out.println(TAG + "activityRecord: " + r.info.name
//				+ " is added to mHistory");
		if (NH > 0) {
			Log.i(TAG, "NH > 0, NH: " + NH);
			/**
			// We want to show the starting preview window if we are
			// switching to a new task, or the next activity's process is
			// not currently running.
			boolean showStartingIcon = newTask;
			ProcessRecord proc = r.app;
			if (proc == null) {
				proc = mService.mProcessNames.get(r.processName,
						r.info.applicationInfo.uid);
			}
			if (proc == null || proc.thread == null) {
				showStartingIcon = true;
			}
			if (DEBUG_TRANSITION)
				System.out.println(TAG, "Prepare open transition: starting "
						+ r);
			if ((r.intent.getFlags() & Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0) {
				mService.mWindowManager
						.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
				mNoAnimActivities.add(r);
			} else if ((r.intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) != 0) {
				mService.mWindowManager
						.prepareAppTransition(WindowManagerPolicy.TRANSIT_TASK_OPEN);
				mNoAnimActivities.remove(r);
			} else {
				mService.mWindowManager
						.prepareAppTransition(newTask ? WindowManagerPolicy.TRANSIT_TASK_OPEN
								: WindowManagerPolicy.TRANSIT_ACTIVITY_OPEN);
				mNoAnimActivities.remove(r);
			}
			mService.mWindowManager.addAppToken(addPos, r, r.task.taskId,
					r.info.screenOrientation, r.fullscreen);
			boolean doShow = true;
			if (newTask) {
				// Even though this activity is starting fresh, we still need
				// to reset it to make sure we apply affinities to move any
				// existing activities from other tasks in to it.
				// If the caller has requested that the target task be
				// reset, then do so.
				if ((r.intent.getFlags() & Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) != 0) {
					resetTaskIfNeededLocked(r, r);
					doShow = topRunningNonDelayedActivityLocked(null) == r;
				}
			}
			if (SHOW_APP_STARTING_PREVIEW && doShow) {
				// Figure out if we are transitioning from another activity that is
				// "has the same starting icon" as the next one.  This allows the
				// window manager to keep the previous window it had previously
				// created, if it still had one.
				ActivityRecord prev = mResumedActivity;
				if (prev != null) {
					// We don't want to reuse the previous starting preview if:
					// (1) The current activity is in a different task.
					if (prev.task != r.task)
						prev = null;
					// (2) The current activity is already displayed.
					else if (prev.nowVisible)
						prev = null;
				}
				mService.mWindowManager.setAppStartingWindow(r, r.packageName,
						r.theme, r.nonLocalizedLabel, r.labelRes, r.icon, prev,
						showStartingIcon);
			}
			/**/
		} else {
			// If this is the first activity, don't do any fancy animations,
			// because there is nothing for it to animate on top of.
			// TODO 以后应该要用，把一个activityRecord对象保存到windowmanger里托管
			//			mService.mWindowManager.addAppToken(addPos, r, r.task.taskId,
			//					r.info.screenOrientation, r.fullscreen);
		}

		if (doResume) {
			resumeTopActivityLocked(null);
		}
	}

	//
	//	/**
	//	 * Perform a reset of the given task, if needed as part of launching it.
	//	 * Returns the new HistoryRecord at the top of the task.
	//	 */
	//	private final ActivityRecord resetTaskIfNeededLocked(
	//			ActivityRecord taskTop, ActivityRecord newActivity) {
	//		boolean forceReset = (newActivity.info.flags & ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0;
	//		if (taskTop.task.getInactiveDuration() > ACTIVITY_INACTIVE_RESET_TIME) {
	//			if ((newActivity.info.flags & ActivityInfo.FLAG_ALWAYS_RETAIN_TASK_STATE) == 0) {
	//				forceReset = true;
	//			}
	//		}
	//
	//		final TaskRecord task = taskTop.task;
	//
	//		// We are going to move through the history list so that we can look
	//		// at each activity 'target' with 'below' either the interesting
	//		// activity immediately below it in the stack or null.
	//		ActivityRecord target = null;
	//		int targetI = 0;
	//		int taskTopI = -1;
	//		int replyChainEnd = -1;
	//		int lastReparentPos = -1;
	//		for (int i = mHistory.size() - 1; i >= -1; i--) {
	//			ActivityRecord below = i >= 0 ? (ActivityRecord) mHistory.get(i)
	//					: null;
	//
	//			if (below != null && below.finishing) {
	//				continue;
	//			}
	//			if (target == null) {
	//				target = below;
	//				targetI = i;
	//				// If we were in the middle of a reply chain before this
	//				// task, it doesn't appear like the root of the chain wants
	//				// anything interesting, so drop it.
	//				replyChainEnd = -1;
	//				continue;
	//			}
	//
	//			final int flags = target.info.flags;
	//
	//			final boolean finishOnTaskLaunch = (flags & ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH) != 0;
	//			final boolean allowTaskReparenting = (flags & ActivityInfo.FLAG_ALLOW_TASK_REPARENTING) != 0;
	//
	//			if (target.task == task) {
	//				// We are inside of the task being reset...  we'll either
	//				// finish this activity, push it out for another task,
	//				// or leave it as-is.  We only do this
	//				// for activities that are not the root of the task (since
	//				// if we finish the root, we may no longer have the task!).
	//				if (taskTopI < 0) {
	//					taskTopI = targetI;
	//				}
	//				if (below != null && below.task == task) {
	//					final boolean clearWhenTaskReset = (target.intent
	//							.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) != 0;
	//					if (!finishOnTaskLaunch && !clearWhenTaskReset
	//							&& target.resultTo != null) {
	//						// If this activity is sending a reply to a previous
	//						// activity, we can't do anything with it now until
	//						// we reach the start of the reply chain.
	//						// XXX note that we are assuming the result is always
	//						// to the previous activity, which is almost always
	//						// the case but we really shouldn't count on.
	//						if (replyChainEnd < 0) {
	//							replyChainEnd = targetI;
	//						}
	//					} else if (!finishOnTaskLaunch && !clearWhenTaskReset
	//							&& allowTaskReparenting
	//							&& target.taskAffinity != null
	//							&& !target.taskAffinity.equals(task.affinity)) {
	//						// If this activity has an affinity for another
	//						// task, then we need to move it out of here.  We will
	//						// move it as far out of the way as possible, to the
	//						// bottom of the activity stack.  This also keeps it
	//						// correctly ordered with any activities we previously
	//						// moved.
	//						ActivityRecord p = (ActivityRecord) mHistory.get(0);
	//						if (target.taskAffinity != null
	//								&& target.taskAffinity.equals(p.task.affinity)) {
	//							// If the activity currently at the bottom has the
	//							// same task affinity as the one we are moving,
	//							// then merge it into the same task.
	//							target.task = p.task;
	//							if (DEBUG_TASKS)
	//								System.out.println(TAG, "Start pushing activity " + target
	//										+ " out to bottom task " + p.task);
	//						} else {
	//							mService.mCurTask++;
	//							if (mService.mCurTask <= 0) {
	//								mService.mCurTask = 1;
	//							}
	//							target.task = new TaskRecord(
	//									mService.mCurTask,
	//									target.info,
	//									null,
	//									(target.info.flags & ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
	//							target.task.affinityIntent = target.intent;
	//							if (DEBUG_TASKS)
	//								System.out.println(TAG, "Start pushing activity " + target
	//										+ " out to new task " + target.task);
	//						}
	//						mService.mWindowManager.setAppGroupId(target,
	//								task.taskId);
	//						if (replyChainEnd < 0) {
	//							replyChainEnd = targetI;
	//						}
	//						int dstPos = 0;
	//						for (int srcPos = targetI; srcPos <= replyChainEnd; srcPos++) {
	//							p = (ActivityRecord) mHistory.get(srcPos);
	//							if (p.finishing) {
	//								continue;
	//							}
	//							if (DEBUG_TASKS)
	//								System.out.println(TAG, "Pushing next activity " + p
	//										+ " out to target's task "
	//										+ target.task);
	//							task.numActivities--;
	//							p.task = target.task;
	//							target.task.numActivities++;
	//							mHistory.remove(srcPos);
	//							mHistory.add(dstPos, p);
	//							mService.mWindowManager.moveAppToken(dstPos, p);
	//							mService.mWindowManager.setAppGroupId(p,
	//									p.task.taskId);
	//							dstPos++;
	//							if (VALIDATE_TOKENS) {
	//								mService.mWindowManager
	//										.validateAppTokens(mHistory);
	//							}
	//							i++;
	//						}
	//						if (taskTop == p) {
	//							taskTop = below;
	//						}
	//						if (taskTopI == replyChainEnd) {
	//							taskTopI = -1;
	//						}
	//						replyChainEnd = -1;
	//						if (mMainStack) {
	//							mService.addRecentTaskLocked(target.task);
	//						}
	//					} else if (forceReset || finishOnTaskLaunch
	//							|| clearWhenTaskReset) {
	//						// If the activity should just be removed -- either
	//						// because it asks for it, or the task should be
	//						// cleared -- then finish it and anything that is
	//						// part of its reply chain.
	//						if (clearWhenTaskReset) {
	//							// In this case, we want to finish this activity
	//							// and everything above it, so be sneaky and pretend
	//							// like these are all in the reply chain.
	//							replyChainEnd = targetI + 1;
	//							while (replyChainEnd < mHistory.size()
	//									&& ((ActivityRecord) mHistory
	//											.get(replyChainEnd)).task == task) {
	//								replyChainEnd++;
	//							}
	//							replyChainEnd--;
	//						} else if (replyChainEnd < 0) {
	//							replyChainEnd = targetI;
	//						}
	//						ActivityRecord p = null;
	//						for (int srcPos = targetI; srcPos <= replyChainEnd; srcPos++) {
	//							p = (ActivityRecord) mHistory.get(srcPos);
	//							if (p.finishing) {
	//								continue;
	//							}
	//							if (finishActivityLocked(p, srcPos,
	//									Activity.RESULT_CANCELED, null, "reset")) {
	//								replyChainEnd--;
	//								srcPos--;
	//							}
	//						}
	//						if (taskTop == p) {
	//							taskTop = below;
	//						}
	//						if (taskTopI == replyChainEnd) {
	//							taskTopI = -1;
	//						}
	//						replyChainEnd = -1;
	//					} else {
	//						// If we were in the middle of a chain, well the
	//						// activity that started it all doesn't want anything
	//						// special, so leave it all as-is.
	//						replyChainEnd = -1;
	//					}
	//				} else {
	//					// Reached the bottom of the task -- any reply chain
	//					// should be left as-is.
	//					replyChainEnd = -1;
	//				}
	//
	//			} else if (target.resultTo != null) {
	//				// If this activity is sending a reply to a previous
	//				// activity, we can't do anything with it now until
	//				// we reach the start of the reply chain.
	//				// XXX note that we are assuming the result is always
	//				// to the previous activity, which is almost always
	//				// the case but we really shouldn't count on.
	//				if (replyChainEnd < 0) {
	//					replyChainEnd = targetI;
	//				}
	//
	//			} else if (taskTopI >= 0 && allowTaskReparenting
	//					&& task.affinity != null
	//					&& task.affinity.equals(target.taskAffinity)) {
	//				// We are inside of another task...  if this activity has
	//				// an affinity for our task, then either remove it if we are
	//				// clearing or move it over to our task.  Note that
	//				// we currently punt on the case where we are resetting a
	//				// task that is not at the top but who has activities above
	//				// with an affinity to it...  this is really not a normal
	//				// case, and we will need to later pull that task to the front
	//				// and usually at that point we will do the reset and pick
	//				// up those remaining activities.  (This only happens if
	//				// someone starts an activity in a new task from an activity
	//				// in a task that is not currently on top.)
	//				if (forceReset || finishOnTaskLaunch) {
	//					if (replyChainEnd < 0) {
	//						replyChainEnd = targetI;
	//					}
	//					ActivityRecord p = null;
	//					for (int srcPos = targetI; srcPos <= replyChainEnd; srcPos++) {
	//						p = (ActivityRecord) mHistory.get(srcPos);
	//						if (p.finishing) {
	//							continue;
	//						}
	//						if (finishActivityLocked(p, srcPos,
	//								Activity.RESULT_CANCELED, null, "reset")) {
	//							taskTopI--;
	//							lastReparentPos--;
	//							replyChainEnd--;
	//							srcPos--;
	//						}
	//					}
	//					replyChainEnd = -1;
	//				} else {
	//					if (replyChainEnd < 0) {
	//						replyChainEnd = targetI;
	//					}
	//					for (int srcPos = replyChainEnd; srcPos >= targetI; srcPos--) {
	//						ActivityRecord p = (ActivityRecord) mHistory
	//								.get(srcPos);
	//						if (p.finishing) {
	//							continue;
	//						}
	//						if (lastReparentPos < 0) {
	//							lastReparentPos = taskTopI;
	//							taskTop = p;
	//						} else {
	//							lastReparentPos--;
	//						}
	//						mHistory.remove(srcPos);
	//						p.task.numActivities--;
	//						p.task = task;
	//						mHistory.add(lastReparentPos, p);
	//						if (DEBUG_TASKS)
	//							System.out.println(TAG, "Pulling activity " + p
	//									+ " in to resetting task " + task);
	//						task.numActivities++;
	//						mService.mWindowManager
	//								.moveAppToken(lastReparentPos, p);
	//						mService.mWindowManager.setAppGroupId(p, p.task.taskId);
	//						if (VALIDATE_TOKENS) {
	//							mService.mWindowManager.validateAppTokens(mHistory);
	//						}
	//					}
	//					replyChainEnd = -1;
	//
	//					// Now we've moved it in to place...  but what if this is
	//					// a singleTop activity and we have put it on top of another
	//					// instance of the same activity?  Then we drop the instance
	//					// below so it remains singleTop.
	//					if (target.info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
	//						for (int j = lastReparentPos - 1; j >= 0; j--) {
	//							ActivityRecord p = (ActivityRecord) mHistory.get(j);
	//							if (p.finishing) {
	//								continue;
	//							}
	//							if (p.intent.getComponent().equals(
	//									target.intent.getComponent())) {
	//								if (finishActivityLocked(p, j,
	//										Activity.RESULT_CANCELED, null,
	//										"replace")) {
	//									taskTopI--;
	//									lastReparentPos--;
	//								}
	//							}
	//						}
	//					}
	//				}
	//			}
	//
	//			target = below;
	//			targetI = i;
	//		}
	//
	//		return taskTop;
	//	}
	//
	//	/**
	//	 * Perform clear operation as requested by
	//	 * {@link Intent#FLAG_ACTIVITY_CLEAR_TOP}: search from the top of the
	//	 * stack to the given task, then look for
	//	 * an instance of that activity in the stack and, if found, finish all
	//	 * activities on top of it and return the instance.
	//	 *
	//	 * @param newR Description of the new activity being started.
	//	 * @return Returns the old activity that should be continue to be used,
	//	 * or null if none was found.
	//	 */
	//	private final ActivityRecord performClearTaskLocked(int taskId,
	//			ActivityRecord newR, int launchFlags, boolean doClear) {
	//		int i = mHistory.size();
	//
	//		// First find the requested task.
	//		while (i > 0) {
	//			i--;
	//			ActivityRecord r = (ActivityRecord) mHistory.get(i);
	//			if (r.task.taskId == taskId) {
	//				i++;
	//				break;
	//			}
	//		}
	//
	//		// Now clear it.
	//		while (i > 0) {
	//			i--;
	//			ActivityRecord r = (ActivityRecord) mHistory.get(i);
	//			if (r.finishing) {
	//				continue;
	//			}
	//			if (r.task.taskId != taskId) {
	//				return null;
	//			}
	//			if (r.realActivity.equals(newR.realActivity)) {
	//				// Here it is!  Now finish everything in front...
	//				ActivityRecord ret = r;
	//				if (doClear) {
	//					while (i < (mHistory.size() - 1)) {
	//						i++;
	//						r = (ActivityRecord) mHistory.get(i);
	//						if (r.finishing) {
	//							continue;
	//						}
	//						if (finishActivityLocked(r, i,
	//								Activity.RESULT_CANCELED, null, "clear")) {
	//							i--;
	//						}
	//					}
	//				}
	//
	//				// Finally, if this is a normal launch mode (that is, not
	//				// expecting onNewIntent()), then we will finish the current
	//				// instance of the activity so a new fresh one can be started.
	//				if (ret.launchMode == ActivityInfo.LAUNCH_MULTIPLE
	//						&& (launchFlags & Intent.FLAG_ACTIVITY_SINGLE_TOP) == 0) {
	//					if (!ret.finishing) {
	//						int index = indexOfTokenLocked(ret);
	//						if (index >= 0) {
	//							finishActivityLocked(ret, index,
	//									Activity.RESULT_CANCELED, null, "clear");
	//						}
	//						return null;
	//					}
	//				}
	//
	//				return ret;
	//			}
	//		}
	//
	//		return null;
	//	}
	//
	//	/**
	//	 * Find the activity in the history stack within the given task.  Returns
	//	 * the index within the history at which it's found, or < 0 if not found.
	//	 */
	//	private final int findActivityInHistoryLocked(ActivityRecord r, int task) {
	//		int i = mHistory.size();
	//		while (i > 0) {
	//			i--;
	//			ActivityRecord candidate = (ActivityRecord) mHistory.get(i);
	//			if (candidate.task.taskId != task) {
	//				break;
	//			}
	//			if (candidate.realActivity.equals(r.realActivity)) {
	//				return i;
	//			}
	//		}
	//
	//		return -1;
	//	}
	//
	//	/**
	//	 * Reorder the history stack so that the activity at the given index is
	//	 * brought to the front.
	//	 */
	//	private final ActivityRecord moveActivityToFrontLocked(int where) {
	//		ActivityRecord newTop = (ActivityRecord) mHistory.remove(where);
	//		int top = mHistory.size();
	//		ActivityRecord oldTop = (ActivityRecord) mHistory.get(top - 1);
	//		mHistory.add(top, newTop);
	//		oldTop.frontOfTask = false;
	//		newTop.frontOfTask = true;
	//		return newTop;
	//	}
	//
	final int startActivityLocked(IApplicationThread caller, Intent intent,
			String resolvedType, Uri[] grantedUriPermissions, int grantedMode,
			ActivityInfo aInfo, IBinder resultTo, String resultWho,
			int requestCode, int callingPid, int callingUid,
			boolean onlyIfNeeded, boolean componentSpecified) {
		//		System.out.println(TAG + "in package startActivityLocked");
		int err = START_SUCCESS;

		ProcessRecord callerApp = null;
		if (caller != null) {
			callerApp = mService.getRecordForAppLocked(caller);
			if (callerApp != null) {
//				System.out.println(TAG + "get a record for app, callerApp: "
//						+ callerApp.processName);
				//				callingPid = callerApp.pid;
				//				callingUid = callerApp.info.uid;
			} else {
//				System.out.println(TAG + "can't get a record for app");
				//				System.out.println(TAG + "Unable to find app for caller "
				//						+ caller + " (pid=" + callingPid + ") when starting: "
				//						+ intent.toString());
				err = ActivityManager.START_PERMISSION_DENIED;
			}
		}

		ActivityRecord sourceRecord = null;
		ActivityRecord resultRecord = null;
		if (resultTo != null) {
			int index = indexOfTokenLocked(resultTo);
			Log.i(TAG, TAG + "Sending result to "
					+ ((ActivityRecord) resultTo).info.name + " (index "
					+ index + ")");
			if (index >= 0) {
				sourceRecord = (ActivityRecord) mHistory.get(index);
				if (requestCode >= 0 && !sourceRecord.finishing) {
					resultRecord = sourceRecord;
				}
			}
		}

		int launchFlags = intent.getFlags();

		// TODO 将来要用的，跟setResult有关
		if ((launchFlags & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0
				&& sourceRecord != null) {
			Log.i(TAG, "impossible here! Intent.FLAG_ACTIVITY_FORWARD_RESULT can't be set");
			// Transfer the result target from the source activity to the new
			// one being started, including any failures.
			if (requestCode >= 0) {
				return START_FORWARD_AND_REQUEST_CONFLICT;
			}
			resultRecord = sourceRecord.resultTo;
			resultWho = sourceRecord.resultWho;
			requestCode = sourceRecord.requestCode;
			sourceRecord.resultTo = null;
			if (resultRecord != null) {
				resultRecord.removeResultsLocked(sourceRecord, resultWho,
						requestCode);
			}
		}

		if (err == START_SUCCESS && intent.getComponent() == null) {
			// We couldn't find a class that can handle the given Intent.
			// That's the end of that!
			err = START_INTENT_NOT_RESOLVED;
		}

		if (err == START_SUCCESS && aInfo == null) {
			// We couldn't find the specific class specified in the Intent.
			// Also the end of the line.
			err = START_CLASS_NOT_FOUND;
		}

		if (err != START_SUCCESS) {
//			System.out.println(TAG + "something wrong happens! err: " + err);
			//			if (resultRecord != null) {
			//				sendActivityResultLocked(-1, resultRecord, resultWho,
			//						requestCode, Activity.RESULT_CANCELED, null);
			//			}
			return err;
		}

		ActivityRecord r = new ActivityRecord(mService, this, callerApp,
				callingUid, intent, resolvedType, aInfo, null, resultRecord,
				resultWho, requestCode, componentSpecified);

		return startActivityUncheckedLocked(r, sourceRecord,
				grantedUriPermissions, grantedMode, onlyIfNeeded, true);
	}

	final int startActivityUncheckedLocked(ActivityRecord r,
			ActivityRecord sourceRecord, Uri[] grantedUriPermissions,
			int grantedMode, boolean onlyIfNeeded, boolean doResume) {
		//		System.out.println(TAG + "in startActivityUncheckedLocked");
		final Intent intent = r.intent;
		int launchFlags = intent.getFlags();

		// We'll invoke onUserLeaving before onPause only if the launching
		// activity did not explicitly state that this is an automated launch.
		mUserLeaving = (launchFlags & Intent.FLAG_ACTIVITY_NO_USER_ACTION) == 0;
		//		System.out.println(TAG + "startActivity() => mUserLeaving="
		//				+ mUserLeaving);

		// If the caller has asked not to resume at this point, we make note
		// of this in the record so that we can skip it when trying to find
		// the top running activity.
		if (!doResume) {
			r.delayedResume = true;
		}

		ActivityRecord notTop = (launchFlags & Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP) != 0 ? r
				: null;

		// If the onlyIfNeeded flag is set, then we can do this if the activity
		// being launched is the same as the one making the call...  or, as
		// a special case, if we do not know the caller then we count the
		// current top activity as the caller.
		if (onlyIfNeeded) {
//			System.out.println(TAG + "onlyIfNeeded: " + onlyIfNeeded);
			ActivityRecord checkedCaller = sourceRecord;
			if (checkedCaller == null) {
				checkedCaller = topRunningNonDelayedActivityLocked(notTop);
			}
			if (!checkedCaller.realActivity.equals(r.realActivity)) {
				// Caller is not the same as launcher, so always needed.
				onlyIfNeeded = false;
			}
		}

		if (sourceRecord == null) {
			// This activity is not being started from another...  in this
			// case we -always- start a new task.
			//			System.out.println(TAG + "should come here");
			if ((launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
//				System.out
//						.println(TAG
//								+ "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: "
//								+ intent);
				launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
			}
		} else if (sourceRecord.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
			// The original activity who is starting us is running as a single
			// instance...  this new activity it is starting must go on its
			// own task.
			launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
		} else if (r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE
				|| r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
			// The activity being started is a single instance...  it always
			// gets launched into its own task.
			launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
		}

		if (r.resultTo != null
				&& (launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			// For whatever reason this activity is being launched into a new
			// task...  yet the caller has requested a result back.  Well, that
			// is pretty messed up, so instead immediately send back a cancel
			// and let the new task continue launched as normal without a
			// dependency on its originator.
//			System.out
//					.println(TAG
//							+ "impossible here! this activity is being launched into a new task... yet the caller has requested a result back");
			//			System.out
			//					.println(TAG
			//							+ "Activity is launching as a new task, so cancelling activity result.");
			//			sendActivityResultLocked(-1, r.resultTo, r.resultWho,
			//					r.requestCode, Activity.RESULT_CANCELED, null);
			r.resultTo = null;
		}

		boolean addingToTask = false;
		if (((launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0 && (launchFlags & Intent.FLAG_ACTIVITY_MULTIPLE_TASK) == 0)
				|| r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK
				|| r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
			// If bring to front is requested, and no result is requested, and
			// we can find a task that was started with this same
			// component, then instead of launching bring that one to the front.
			if (r.resultTo == null) {
				// See if there is a task to bring to the front.  If this is
				// a SINGLE_INSTANCE activity, there can be one and only one
				// instance of it in the history, and it is always in its own
				// unique task, so we do a special search.
				ActivityRecord taskTop = r.launchMode != ActivityInfo.LAUNCH_SINGLE_INSTANCE ? findTaskLocked(
						intent, r.info) : findActivityLocked(intent, r.info);
				if (taskTop != null) {
					//actually, it is possible!
//					System.out
//							.println(TAG + "impossible here! taskTop != null");
					
					if (taskTop.task.intent == null) {
						// This task was started because of movement of
						// the activity based on affinity...  now that we
						// are actually launching it, we can assign the
						// base intent.
						taskTop.task.setIntent(intent, r.info);
					}
					// If the target task is not in the front, then we need
					// to bring it to the front...  except...  well, with
					// SINGLE_TASK_LAUNCH it's not entirely clear.  We'd like
					// to have the same behavior as if a new instance was
					// being started, which means not bringing it to the front
					// if the caller is not itself in the front.
					ActivityRecord curTop = topRunningNonDelayedActivityLocked(notTop);
					if (curTop.task != taskTop.task) {
						r.intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
						boolean callerAtFront = sourceRecord == null
								|| curTop.task == sourceRecord.task;
						if (callerAtFront) {
							// We really do want to push this one into the
							// user's face, right now.
							moveTaskToFrontLocked(taskTop.task, r);
						}
					}
					// If the caller has requested that the target task be
					// reset, then do so.
					if ((launchFlags & Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) != 0) {
						taskTop = resetTaskIfNeededLocked(taskTop, r);
					}
					if (onlyIfNeeded) {
						// We don't need to start a new activity, and
						// the client said not to do anything if that
						// is the case, so this is it!  And for paranoia, make
						// sure we have correctly resumed the top activity.
						if (doResume) {
							resumeTopActivityLocked(null);
						}
						return ActivityManager.START_RETURN_INTENT_TO_CALLER;
					}
					if ((launchFlags & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0
							|| r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK
							|| r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
						// In this situation we want to remove all activities
						// from the task up to the one being started.  In most
						// cases this means we are resetting the task to its
						// initial state.
						ActivityRecord top = performClearTaskLocked(
								taskTop.task.taskId, r, launchFlags, true);
						if (top != null) {
							if (top.frontOfTask) {
								// Activity aliases may mean we use different
								// intents for the top activity, so make sure
								// the task now has the identity of the new
								// intent.
								top.task.setIntent(r.intent, r.info);
							}
							//							logStartActivity(EventLogTags.AM_NEW_INTENT, r,
							//									top.task);
							//top.deliverNewIntentLocked(callingUid, r.intent);
						} else {
							// A special case: we need to
							// start the activity because it is not currently
							// running, and the caller has asked to clear the
							// current task to have this activity at the top.
							addingToTask = true;
							// Now pretend like this activity is being started
							// by the top of its task, so it is put in the
							// right place.
							sourceRecord = taskTop;
						}
					} else if (r.realActivity.equals(taskTop.task.realActivity)) {
						// In this case the top activity on the task is the
						// same as the one being launched, so we take that
						// as a request to bring the task to the foreground.
						// If the top activity in the task is the root
						// activity, deliver this new intent to it if it
						// desires.
						if ((launchFlags & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
								&& taskTop.realActivity.equals(r.realActivity)) {
							//							logStartActivity(EventLogTags.AM_NEW_INTENT, r,
							//									taskTop.task);
							if (taskTop.frontOfTask) {
								taskTop.task.setIntent(r.intent, r.info);
							}
							//taskTop.deliverNewIntentLocked(callingUid, r.intent);
							
						} else if (!r.intent.filterEquals(taskTop.task.intent)) {
							// In this case we are launching the root activity
							// of the task, but with a different intent.  We
							// should start a new instance on top.
							addingToTask = true;
							sourceRecord = taskTop;
						}
					} else if ((launchFlags & Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) == 0) {
						// In this case an activity is being launched in to an
						// existing task, without resetting that task.  This
						// is typically the situation of launching an activity
						// from a notification or shortcut.  We want to place
						// the new activity on top of the current task.
						addingToTask = true;
						sourceRecord = taskTop;
					} else if (!taskTop.task.rootWasReset) {
						// In this case we are launching in to an existing task
						// that has not yet been started from its front door.
						// The current task has been brought to the front.
						// Ideally, we'd probably like to place this new task
						// at the bottom of its stack, but that's a little hard
						// to do with the current organization of the code so
						// for now we'll just drop it.
						taskTop.task.setIntent(r.intent, r.info);
					}
					if (!addingToTask) {
						// We didn't do anything...  but it was needed (a.k.a., client
						// don't use that intent!)  And for paranoia, make
						// sure we have correctly resumed the top activity.
						if (doResume) {
							resumeTopActivityLocked(null);
						}
						return ActivityManager.START_TASK_TO_FRONT;
					}
					
				}
			}
		}

		if (r.packageName != null) {
			// If the activity being launched is the same as the one currently
			// at the top, then we need to check if it should only be launched
			// once.
			ActivityRecord top = topRunningNonDelayedActivityLocked(notTop);
			if (top != null && r.resultTo == null) {
				if (top.realActivity.equals(r.realActivity)) {
//					System.out.println(TAG + "impossible here!");
					/**
					if (top.app != null && top.app.thread != null) {
						if ((launchFlags & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
								|| r.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP
								|| r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
							// For paranoia, make sure we have correctly
							// resumed the top activity.
							if (doResume) {
								resumeTopActivityLocked(null);
							}
							if (onlyIfNeeded) {
								// We don't need to start a new activity, and
								// the client said not to do anything if that
								// is the case, so this is it!
								return START_RETURN_INTENT_TO_CALLER;
							}
							top.deliverNewIntentLocked(callingUid, r.intent);
							return START_DELIVERED_TO_TOP;
						}
					}
					/**/
				}
			}
		} else {
			return START_CLASS_NOT_FOUND;
		}

		boolean newTask = false;
		// Should this be considered a new task?
		if (r.resultTo == null && addingToTask == false
				&& (launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			// todo: should do better management of integers.
			mService.mCurTask++;
			if (mService.mCurTask <= 0) {
				mService.mCurTask = 1;
			}
			//			System.out.println(TAG + "ready to create new task");
			r.task = new TaskRecord(
					mService.mCurTask,
					r.info,
					intent,
					(r.info.flags & ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
			//			System.out.println(TAG + "Starting new activity " + r.info.name
			//					+ " in new task " + r.task.taskId);
			newTask = true;
			if (mMainStack) {
				//MayLoon TODO: we do not konw whether this task has been added successfully or not, maybe should use
				// r.task.taskid to qeury it
				mService.addRecentTaskLocked(r.task);
			}
		} else if (sourceRecord != null) {
			// MayLoon TODO: will be enabled future
			
			if (!addingToTask
					&& (launchFlags & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) {
				// In this case, we are adding the activity to an existing
				// task, but the caller has asked to clear that task if the
				// activity is already running.
				ActivityRecord top = performClearTaskLocked(
						sourceRecord.task.taskId, r, launchFlags, true);
				if (top != null) {
					//					logStartActivity(EventLogTags.AM_NEW_INTENT, r, top.task);
					//top.deliverNewIntentLocked(callingUid, r.intent);
					// For paranoia, make sure we have correctly
					// resumed the top activity.
					if (doResume) {
						resumeTopActivityLocked(null);
					}
					return ActivityManager.START_DELIVERED_TO_TOP;
				}
			} else if (!addingToTask
					&& (launchFlags & Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) != 0) {
				// In this case, we are launching an activity in our own task
				// that may already be running somewhere in the history, and
				// we want to shuffle it to the front of the stack if so.
				int where = findActivityInHistoryLocked(r,
						sourceRecord.task.taskId);
				if (where >= 0) {
					ActivityRecord top = moveActivityToFrontLocked(where);
					//					logStartActivity(EventLogTags.AM_NEW_INTENT, r, top.task);
					//top.deliverNewIntentLocked(callingUid, r.intent);
					if (doResume) {
						resumeTopActivityLocked(null);
					}
					return ActivityManager.START_DELIVERED_TO_TOP;
				}
			}
			
			// An existing activity is starting this new activity, so we want
			// to keep the new one in the same task as the one that is starting
			// it.
			r.task = sourceRecord.task;
//			System.out.println(TAG + "Starting new activity " + r.info.name
//					+ " in existing task " + r.task.taskId);
		} else {
			// This not being started from an existing activity, and not part
			// of a new task...  just put it in the top task, though these days
			// this case should never happen.
//			System.out.println(TAG + "impossible here");
			final int N = mHistory.size();
			ActivityRecord prev = N > 0 ? (ActivityRecord) mHistory.get(N - 1)
					: null;
			r.task = prev != null ? prev.task
					: new TaskRecord(
							mService.mCurTask,
							r.info,
							intent,
							(r.info.flags & ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
//			System.out.println(TAG + "Starting new activity " + r
//					+ " in new guessed " + r.task);
		}
		startActivityLocked(r, newTask, doResume);
		return START_SUCCESS;
	}
	
    /**
     * Reorder the history stack so that the activity at the given index is
     * brought to the front.
     */
    private final ActivityRecord moveActivityToFrontLocked(int where) {
        ActivityRecord newTop = (ActivityRecord)mHistory.remove(where);
        int top = mHistory.size();
        ActivityRecord oldTop = (ActivityRecord)mHistory.get(top-1);
        mHistory.add(top, newTop);
        oldTop.frontOfTask = false;
        newTop.frontOfTask = true;
        return newTop;
    }
    /**
     * Find the activity in the history stack within the given task.  Returns
     * the index within the history at which it's found, or < 0 if not found.
     */
    private final int findActivityInHistoryLocked(ActivityRecord r, int task) {
        int i = mHistory.size();
        while (i > 0) {
            i--;
            ActivityRecord candidate = (ActivityRecord)mHistory.get(i);
            if (candidate.task.taskId != task) {
                break;
            }
            if (candidate.realActivity.equals(r.realActivity)) {
                return i;
            }
        }

        return -1;
    }
	
	 /**
     * Perform clear operation as requested by
     * {@link Intent#FLAG_ACTIVITY_CLEAR_TOP}: search from the top of the
     * stack to the given task, then look for
     * an instance of that activity in the stack and, if found, finish all
     * activities on top of it and return the instance.
     *
     * @param newR Description of the new activity being started.
     * @return Returns the old activity that should be continue to be used,
     * or null if none was found.
     */
	private final ActivityRecord performClearTaskLocked(int taskId,
            ActivityRecord newR, int launchFlags, boolean doClear) {

        int i = mHistory.size();
        
        // First find the requested task.
        while (i > 0) {
            i--;
            ActivityRecord r = (ActivityRecord)mHistory.get(i);
            if (r.task.taskId == taskId) {
                i++;
                break;
            }
        }
        
        // Now clear it.
        while (i > 0) {
            i--;
            ActivityRecord r = (ActivityRecord)mHistory.get(i);
            if (r.finishing) {
                continue;
            }
            if (r.task.taskId != taskId) {
                return null;
            }
            if (r.realActivity.equals(newR.realActivity)) {
                // Here it is!  Now finish everything in front...
                ActivityRecord ret = r;
                if (doClear) {
                    while (i < (mHistory.size()-1)) {
                        i++;
                        r = (ActivityRecord)mHistory.get(i);
                        if (r.finishing) {
                            continue;
                        }
                        if (finishActivityLocked(r, i, Activity.RESULT_CANCELED,
                                null, "clear")) {
                            i--;
                        }
                    }
                }
                
                // Finally, if this is a normal launch mode (that is, not
                // expecting onNewIntent()), then we will finish the current
                // instance of the activity so a new fresh one can be started.
                if (ret.launchMode == ActivityInfo.LAUNCH_MULTIPLE
                        && (launchFlags&Intent.FLAG_ACTIVITY_SINGLE_TOP) == 0) {
                    if (!ret.finishing) {
                        int index = indexOfTokenLocked(ret);
                        if (index >= 0) {
                            finishActivityLocked(ret, index, Activity.RESULT_CANCELED,
                                    null, "clear");
                        }
                        return null;
                    }
                }
                
                return ret;
            }
        }

        return null;
    
	}
    /**
     * Perform a reset of the given task, if needed as part of launching it.
     * Returns the new HistoryRecord at the top of the task.
     */
    private final ActivityRecord resetTaskIfNeededLocked(ActivityRecord taskTop,
            ActivityRecord newActivity){

        boolean forceReset = (newActivity.info.flags
                &ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0;
        if (taskTop.task.getInactiveDuration() > ACTIVITY_INACTIVE_RESET_TIME) {
            if ((newActivity.info.flags
                    &ActivityInfo.FLAG_ALWAYS_RETAIN_TASK_STATE) == 0) {
                forceReset = true;
            }
        }
        
        final TaskRecord task = taskTop.task;
        
        // We are going to move through the history list so that we can look
        // at each activity 'target' with 'below' either the interesting
        // activity immediately below it in the stack or null.
        ActivityRecord target = null;
        int targetI = 0;
        int taskTopI = -1;
        int replyChainEnd = -1;
        int lastReparentPos = -1;
        for (int i=mHistory.size()-1; i>=-1; i--) {
            ActivityRecord below = i >= 0 ? (ActivityRecord)mHistory.get(i) : null;
            
            if (below != null && below.finishing) {
                continue;
            }
            if (target == null) {
                target = below;
                targetI = i;
                // If we were in the middle of a reply chain before this
                // task, it doesn't appear like the root of the chain wants
                // anything interesting, so drop it.
                replyChainEnd = -1;
                continue;
            }
        
            final int flags = target.info.flags;
            
            final boolean finishOnTaskLaunch =
                (flags&ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH) != 0;
            final boolean allowTaskReparenting =
                (flags&ActivityInfo.FLAG_ALLOW_TASK_REPARENTING) != 0;
            
            if (target.task == task) {
                // We are inside of the task being reset...  we'll either
                // finish this activity, push it out for another task,
                // or leave it as-is.  We only do this
                // for activities that are not the root of the task (since
                // if we finish the root, we may no longer have the task!).
                if (taskTopI < 0) {
                    taskTopI = targetI;
                }
                if (below != null && below.task == task) {
                    final boolean clearWhenTaskReset =
                            (target.intent.getFlags()
                                    &Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) != 0;
                    if (!finishOnTaskLaunch && !clearWhenTaskReset && target.resultTo != null) {
                        // If this activity is sending a reply to a previous
                        // activity, we can't do anything with it now until
                        // we reach the start of the reply chain.
                        // XXX note that we are assuming the result is always
                        // to the previous activity, which is almost always
                        // the case but we really shouldn't count on.
                        if (replyChainEnd < 0) {
                            replyChainEnd = targetI;
                        }
                    } else if (!finishOnTaskLaunch && !clearWhenTaskReset && allowTaskReparenting
                            && target.taskAffinity != null
                            && !target.taskAffinity.equals(task.affinity)) {
                        // If this activity has an affinity for another
                        // task, then we need to move it out of here.  We will
                        // move it as far out of the way as possible, to the
                        // bottom of the activity stack.  This also keeps it
                        // correctly ordered with any activities we previously
                        // moved.
                        ActivityRecord p = (ActivityRecord)mHistory.get(0);
                        if (target.taskAffinity != null
                                && target.taskAffinity.equals(p.task.affinity)) {
                            // If the activity currently at the bottom has the
                            // same task affinity as the one we are moving,
                            // then merge it into the same task.
                            target.task = p.task;
                        } else {
                            mService.mCurTask++;
                            if (mService.mCurTask <= 0) {
                                mService.mCurTask = 1;
                            }
                            target.task = new TaskRecord(mService.mCurTask, target.info, null,
                                    (target.info.flags&ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
                            target.task.affinityIntent = target.intent;
                        }
                        if (replyChainEnd < 0) {
                            replyChainEnd = targetI;
                        }
                        int dstPos = 0;
                        for (int srcPos=targetI; srcPos<=replyChainEnd; srcPos++) {
                            p = (ActivityRecord)mHistory.get(srcPos);
                            if (p.finishing) {
                                continue;
                            }
                            task.numActivities--;
                            p.task = target.task;
                            target.task.numActivities++;
                            mHistory.remove(srcPos);
                            mHistory.add(dstPos, p);
                            dstPos++;
                            i++;
                        }
                        if (taskTop == p) {
                            taskTop = below;
                        }
                        if (taskTopI == replyChainEnd) {
                            taskTopI = -1;
                        }
                        replyChainEnd = -1;
                        if (mMainStack) {
                            mService.addRecentTaskLocked(target.task);
                        }
                    } else if (forceReset || finishOnTaskLaunch
                            || clearWhenTaskReset) {
                        // If the activity should just be removed -- either
                        // because it asks for it, or the task should be
                        // cleared -- then finish it and anything that is
                        // part of its reply chain.
                        if (clearWhenTaskReset) {
                            // In this case, we want to finish this activity
                            // and everything above it, so be sneaky and pretend
                            // like these are all in the reply chain.
                            replyChainEnd = targetI+1;
                            while (replyChainEnd < mHistory.size() &&
                                    ((ActivityRecord)mHistory.get(
                                                replyChainEnd)).task == task) {
                                replyChainEnd++;
                            }
                            replyChainEnd--;
                        } else if (replyChainEnd < 0) {
                            replyChainEnd = targetI;
                        }
                        ActivityRecord p = null;
                        for (int srcPos=targetI; srcPos<=replyChainEnd; srcPos++) {
                            p = (ActivityRecord)mHistory.get(srcPos);
                            if (p.finishing) {
                                continue;
                            }
                            if (finishActivityLocked(p, srcPos,
                                    Activity.RESULT_CANCELED, null, "reset")) {
                                replyChainEnd--;
                                srcPos--;
                            }
                        }
                        if (taskTop == p) {
                            taskTop = below;
                        }
                        if (taskTopI == replyChainEnd) {
                            taskTopI = -1;
                        }
                        replyChainEnd = -1;
                    } else {
                        // If we were in the middle of a chain, well the
                        // activity that started it all doesn't want anything
                        // special, so leave it all as-is.
                        replyChainEnd = -1;
                    }
                } else {
                    // Reached the bottom of the task -- any reply chain
                    // should be left as-is.
                    replyChainEnd = -1;
                }
                
            } else if (target.resultTo != null) {
                // If this activity is sending a reply to a previous
                // activity, we can't do anything with it now until
                // we reach the start of the reply chain.
                // XXX note that we are assuming the result is always
                // to the previous activity, which is almost always
                // the case but we really shouldn't count on.
                if (replyChainEnd < 0) {
                    replyChainEnd = targetI;
                }

            } else if (taskTopI >= 0 && allowTaskReparenting
                    && task.affinity != null
                    && task.affinity.equals(target.taskAffinity)) {
                // We are inside of another task...  if this activity has
                // an affinity for our task, then either remove it if we are
                // clearing or move it over to our task.  Note that
                // we currently punt on the case where we are resetting a
                // task that is not at the top but who has activities above
                // with an affinity to it...  this is really not a normal
                // case, and we will need to later pull that task to the front
                // and usually at that point we will do the reset and pick
                // up those remaining activities.  (This only happens if
                // someone starts an activity in a new task from an activity
                // in a task that is not currently on top.)
                if (forceReset || finishOnTaskLaunch) {
                    if (replyChainEnd < 0) {
                        replyChainEnd = targetI;
                    }
                    ActivityRecord p = null;
                    for (int srcPos=targetI; srcPos<=replyChainEnd; srcPos++) {
                        p = (ActivityRecord)mHistory.get(srcPos);
                        if (p.finishing) {
                            continue;
                        }
                        if (finishActivityLocked(p, srcPos,
                                Activity.RESULT_CANCELED, null, "reset")) {
                            taskTopI--;
                            lastReparentPos--;
                            replyChainEnd--;
                            srcPos--;
                        }
                    }
                    replyChainEnd = -1;
                } else {
                    if (replyChainEnd < 0) {
                        replyChainEnd = targetI;
                    }
                    for (int srcPos=replyChainEnd; srcPos>=targetI; srcPos--) {
                        ActivityRecord p = (ActivityRecord)mHistory.get(srcPos);
                        if (p.finishing) {
                            continue;
                        }
                        if (lastReparentPos < 0) {
                            lastReparentPos = taskTopI;
                            taskTop = p;
                        } else {
                            lastReparentPos--;
                        }
                        mHistory.remove(srcPos);
                        p.task.numActivities--;
                        p.task = task;
                        mHistory.add(lastReparentPos, p);
                        task.numActivities++;
                    }
                    replyChainEnd = -1;
                    
                    // Now we've moved it in to place...  but what if this is
                    // a singleTop activity and we have put it on top of another
                    // instance of the same activity?  Then we drop the instance
                    // below so it remains singleTop.
                    if (target.info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
                        for (int j=lastReparentPos-1; j>=0; j--) {
                            ActivityRecord p = (ActivityRecord)mHistory.get(j);
                            if (p.finishing) {
                                continue;
                            }
                            if (p.intent.getComponent().equals(target.intent.getComponent())) {
                                if (finishActivityLocked(p, j,
                                        Activity.RESULT_CANCELED, null, "replace")) {
                                    taskTopI--;
                                    lastReparentPos--;
                                }
                            }
                        }
                    }
                }
            }
            
            target = below;
            targetI = i;
        }
        
        return taskTop;
    
    	
    }
	 final void moveTaskToFrontLocked(TaskRecord tr, ActivityRecord reason) {
	        final int task = tr.taskId;
	        int top = mHistory.size()-1;
	        if (top < 0 || ((ActivityRecord)mHistory.get(top)).task.taskId == task) {
	            // nothing to do!
	            return;
	        }
	        ArrayList moved = new ArrayList();

	        // Applying the affinities may have removed entries from the history,
	        // so get the size again.
	        top = mHistory.size()-1;
	        int pos = top;

	        // Shift all activities with this task up to the top
	        // of the stack, keeping them in the same internal order.
	        while (pos >= 0) {
	            ActivityRecord r = (ActivityRecord)mHistory.get(pos);
	            boolean first = true;
	            if (r.task.taskId == task) {
	                mHistory.remove(pos);
	                mHistory.add(top, r);
	                moved.add(0, r);
	                top--;
	                if (first && mMainStack) {
	                    mService.addRecentTaskLocked(r.task);
	                    first = false;
	                }
	            }
	            pos--;
	        }
	        if (reason != null &&
	                (reason.intent.getFlags()&Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0) {
	            ActivityRecord r = topRunningActivityLocked(null);
	            if (r != null) {
	                mNoAnimActivities.add(r);
	            }
	        } else {
	        }
	        finishTaskMoveLocked(task);	    
	 }
	    private final void finishTaskMoveLocked(int task) {
	        resumeTopActivityLocked(null);
	    }

	final int startActivityMayWait(IApplicationThread caller, Intent intent,
			String resolvedType, Uri[] grantedUriPermissions, int grantedMode,
			IBinder resultTo, String resultWho, int requestCode,
			boolean onlyIfNeeded, boolean debug) {
		// Refuse possible leaked file descriptors
		if (intent != null && intent.hasFileDescriptors()) {
			throw new IllegalArgumentException(
					"File descriptors passed in Intent");
		}
		Log.i(TAG, " startActivityMayWait");
		boolean componentSpecified = intent.getComponent() != null;

		// Don't modify the client's object!
		intent = new Intent(intent);

		// Collect information about the target of the Intent.
		ActivityInfo aInfo;
		ResolveInfo rInfo = Context
				.getSystemContext()
				.getPackageManager()
				.resolveIntent(
						intent,
						resolvedType,
						PackageManager.MATCH_DEFAULT_ONLY
								| ActivityManager.STOCK_PM_FLAGS);
		aInfo = rInfo != null ? rInfo.activityInfo : null;
		Log.i(TAG, " startActivityMayWait2");
        if (aInfo != null) {
            // Store the found target back into the intent, because now that
            // we have it we never want to do this again. For example, if the
            // user navigates back to this point in the history, we should
            // always restart the exact same activity.
            intent.setComponent(new ComponentName(
                    aInfo.applicationInfo.packageName, aInfo.name));
            Log.i(TAG, "aInfo.applicationInfo.packageName: "
                    + aInfo.applicationInfo.packageName + ", aInfo.name: "
                    + aInfo.name);
            Log.i(TAG, "intent.getComponent: "
                    + intent.getComponent().flattenToString());
        }

		int res = startActivityLocked(caller, intent, resolvedType,
				grantedUriPermissions, grantedMode, aInfo, resultTo, resultWho,
				requestCode, 0, 0, onlyIfNeeded, componentSpecified);

		return res;
	}

	//
	//	void reportActivityLaunchedLocked(boolean timeout, ActivityRecord r,
	//			long thisTime, long totalTime) {
	//		for (int i = mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
	//			WaitResult w = mWaitingActivityLaunched.get(i);
	//			w.timeout = timeout;
	//			if (r != null) {
	//				w.who = new ComponentName(r.info.packageName, r.info.name);
	//			}
	//			w.thisTime = thisTime;
	//			w.totalTime = totalTime;
	//		}
	//		mService.notifyAll();
	//	}
	//
	//	void reportActivityVisibleLocked(ActivityRecord r) {
	//		for (int i = mWaitingActivityVisible.size() - 1; i >= 0; i--) {
	//			WaitResult w = mWaitingActivityVisible.get(i);
	//			w.timeout = false;
	//			if (r != null) {
	//				w.who = new ComponentName(r.info.packageName, r.info.name);
	//			}
	//			w.totalTime = SystemClock.uptimeMillis() - w.thisTime;
	//			w.thisTime = w.totalTime;
	//		}
	//		mService.notifyAll();
	//	}
	//
	//	void sendActivityResultLocked(int callingUid, ActivityRecord r,
	//			String resultWho, int requestCode, int resultCode, Intent data) {
	//
	//		if (callingUid > 0) {
	//			mService.grantUriPermissionFromIntentLocked(callingUid,
	//					r.packageName, data, r.getUriPermissionsLocked());
	//		}
	//
	//		if (DEBUG_RESULTS)
	//			System.out.println(TAG, "Send activity result to " + r + " : who=" + resultWho
	//					+ " req=" + requestCode + " res=" + resultCode + " data="
	//					+ data);
	//		if (mResumedActivity == r && r.app != null && r.app.thread != null) {
	//			try {
	//				ArrayList<ResultInfo> list = new ArrayList<ResultInfo>();
	//				list.add(new ResultInfo(resultWho, requestCode, resultCode,
	//						data));
	//				r.app.thread.scheduleSendResult(r, list);
	//				return;
	//			} catch (Exception e) {
	//				System.out.println(TAG, "Exception thrown sending result to " + r, e);
	//			}
	//		}
	//
	//		r.addResultLocked(null, resultWho, requestCode, resultCode, data);
	//	}

	private final void stopActivityLocked(ActivityRecord r) {
		Log.i(TAG, "Stopping: " + r);
		if ((r.intent.getFlags() & Intent.FLAG_ACTIVITY_NO_HISTORY) != 0
				|| (r.info.flags & ActivityInfo.FLAG_NO_HISTORY) != 0) {
			if (!r.finishing) {
				requestFinishActivityLocked(r, Activity.RESULT_CANCELED, null,
						"no-history");
			}
		} else if (r.app != null && r.app.thread != null) {
			if (mMainStack) {
				if (mService.mFocusedActivity == r) {
					mService.setFocusedActivityLocked(topRunningActivityLocked(null));
				}
			}
			r.resumeKeyDispatchingLocked();
			try {
				r.stopped = false;
				r.state = ActivityState.STOPPING;
//				if (DEBUG_VISBILITY)
					Log.i(TAG, "Stopping visible=" + r.visible + " for " + r);
//				if (!r.visible) {
//					mService.mWindowManager.setAppVisibility(r, false);
//				}
				r.app.thread.scheduleStopActivity(r, r.visible,
						r.configChangeFlags);
			} catch (Exception e) {
				// Maybe just ignore exceptions here...  if the process
				// has crashed, our death notification will clean things
				// up.
//				System.out.println(TAG, "Exception thrown during pause", e);
				// Just in case, assume it to be stopped.
				r.stopped = true;
				r.state = ActivityState.STOPPED;
				if (r.configDestroy) {
					destroyActivityLocked(r, true);
				}
			}
		}
	}

	final ArrayList<ActivityRecord> processStoppingActivitiesLocked(
			boolean remove) {
		int N = mStoppingActivities.size();
		if (N <= 0)
			return null;

		ArrayList<ActivityRecord> stops = null;

        if (mResumedActivity!=null) {
            if (mResumedActivity.fullscreen) {
                mResumedActivity.nowVisible = true;
            }
        }
		final boolean nowVisible = mResumedActivity != null
				&& mResumedActivity.nowVisible
				&& !mResumedActivity.waitingVisible;
		for (int i = 0; i < N; i++) {
			ActivityRecord s = mStoppingActivities.get(i);
//			if (localLOGV)
//				System.out.println(TAG, "Stopping " + s + ": nowVisible=" + nowVisible
//						+ " waitingVisible=" + s.waitingVisible + " finishing="
//						+ s.finishing);
			if (s.waitingVisible && nowVisible) {
				mWaitingVisibleActivities.remove(s);
				s.waitingVisible = false;
				if (s.finishing) {
					// If this activity is finishing, it is sitting on top of
					// everyone else but we now know it is no longer needed...
					// so get rid of it.  Otherwise, we need to go through the
					// normal flow and hide it once we determine that it is
					// hidden by the activities in front of it.
//					if (localLOGV)
//						System.out.println(TAG, "Before stopping, can hide: " + s);
//					mService.mWindowManager.setAppVisibility(s, false);
				}
			}
			if (!s.waitingVisible && remove) {
//				if (localLOGV)
//					System.out.println(TAG, "Ready to stop: " + s);
				if (stops == null) {
					stops = new ArrayList<ActivityRecord>();
				}
				stops.add(s);
				mStoppingActivities.remove(i);
				N--;
				i--;
			}
		}

		return stops;
	}

	final void activityIdleInternal(IBinder token, boolean fromTimeout,
			Configuration config) {
		Log.i(TAG, "Activity idle: " + token);

		ArrayList<ActivityRecord> stops = null;
		ArrayList<ActivityRecord> finishes = null;
		ArrayList<ActivityRecord> thumbnails = null;
		int NS = 0;
		int NF = 0;
		int NT = 0;
		IApplicationThread sendThumbnail = null;
		boolean booting = false;
		boolean enableScreen = false;

		if (token != null) {
			mHandler.removeMessages(IDLE_TIMEOUT_MSG, token);
		}

		// Get the activity record.
		int index = indexOfTokenLocked(token);
		if (index >= 0) {
			ActivityRecord r = (ActivityRecord) mHistory.get(index);

//			if (fromTimeout) {
//				reportActivityLaunchedLocked(fromTimeout, r, -1, -1);
//			}

			// This is a hack to semi-deal with a race condition
			// in the client where it can be constructed with a
			// newer configuration from when we asked it to launch.
			// We'll update with whatever configuration it now says
			// it used to launch.
			if (config != null) {
				r.configuration = config;
			}

			// No longer need to keep the device awake.
			//&& mLaunchingActivity.isHeld()
			if (mResumedActivity == r) {
				mHandler.removeMessages(LAUNCH_TIMEOUT_MSG);
//				mLaunchingActivity.release();
			}

			// We are now idle.  If someone is waiting for a thumbnail from
			// us, we can now deliver.
			r.idle = true;
//			mService.scheduleAppGcsLocked();
			if (r.thumbnailNeeded && r.app != null && r.app.thread != null) {
				sendThumbnail = r.app.thread;
				r.thumbnailNeeded = false;
			}

			// If this activity is fullscreen, set up to hide those under it.

//			if (DEBUG_VISBILITY)
				Log.i(TAG, "Idle activity for " + r);
			ensureActivitiesVisibleLocked(null, 0);

			//System.out.println(TAG, "IDLE: mBooted=" + mBooted + ", fromTimeout=" + fromTimeout);
//			if (mMainStack) {
//				if (!mService.mBooted && !fromTimeout) {
//					mService.mBooted = true;
//					enableScreen = true;
//				}
//			}

//		} else if (fromTimeout) {
//			reportActivityLaunchedLocked(fromTimeout, null, -1, -1);
		}

		// Atomically retrieve all of the other things to do.
		stops = processStoppingActivitiesLocked(true);
		NS = stops != null ? stops.size() : 0;
		if ((NF = mFinishingActivities.size()) > 0) {
			finishes = new ArrayList<ActivityRecord>(mFinishingActivities);
			mFinishingActivities.clear();
		}
//		if ((NT = mService.mCancelledThumbnails.size()) > 0) {
//			thumbnails = new ArrayList<ActivityRecord>(
//					mService.mCancelledThumbnails);
//			mService.mCancelledThumbnails.clear();
//		}

//		if (mMainStack) {
//			booting = mService.mBooting;
//			mService.mBooting = false;
//		}

		int i;

		// Send thumbnail if requested.
//		if (sendThumbnail != null) {
//			try {
//				sendThumbnail.requestThumbnail(token);
//			} catch (Exception e) {
//				System.out.println(TAG, "Exception thrown when requesting thumbnail", e);
//				mService.sendPendingThumbnail(null, token, null, null, true);
//			}
//		}

		// Stop any activities that are scheduled to do so but have been
		// waiting for the next one to start.
		for (i = 0; i < NS; i++) {
			ActivityRecord r = (ActivityRecord) stops.get(i);
			if (r.finishing) {
				finishCurrentActivityLocked(r, FINISH_IMMEDIATELY);
			} else {
				stopActivityLocked(r);
			}
		}

		// Finish any activities that are scheduled to do so but have been
		// waiting for the next one to start.
		for (i = 0; i < NF; i++) {
			ActivityRecord r = (ActivityRecord) finishes.get(i);
			synchronized (mService) {
				destroyActivityLocked(r, true);
			}
		}

		// Report back to any thumbnail receivers.
//		for (i = 0; i < NT; i++) {
//			ActivityRecord r = (ActivityRecord) thumbnails.get(i);
//			mService.sendPendingThumbnail(r, null, null, null, true);
//		}

//		if (booting) {
//			mService.finishBooting();
//		}

//		mService.trimApplications();
		//dump();
		//mWindowManager.dump();

//		if (enableScreen) {
//			mService.enableScreenAfterBoot();
//		}
	}

	/**
	 * @return Returns true if the activity is being finished, false if for
	 * some reason it is being left as-is.
	 */
	final boolean requestFinishActivityLocked(IBinder token, int resultCode,
			Intent resultData, String reason) {
		//		if (DEBUG_RESULTS)
		//			System.out.println(TAG, "Finishing activity: token=" + token
		//					+ ", result=" + resultCode + ", data=" + resultData);

		int index = indexOfTokenLocked(token);
		if (index < 0) {
			return false;
		}
		ActivityRecord r = (ActivityRecord) mHistory.get(index);

		// Is this the last activity left?
		boolean lastActivity = true;
		for (int i = mHistory.size() - 1; i >= 0; i--) {
			ActivityRecord p = (ActivityRecord) mHistory.get(i);
			if (!p.finishing && p != r) {
				lastActivity = false;
				break;
			}
		}

		// If this is the last activity, but it is the home activity, then
		// just don't finish it.
		if (lastActivity) {
			if (r.intent.hasCategory(Intent.CATEGORY_HOME)) {
				return false;
			}
		}

		finishActivityLocked(r, index, resultCode, resultData, reason);
		return true;
	}

	/**
	 * @return Returns true if this activity has been removed from the history
	 * list, or false if it is still in the list and will be removed later.
	 */
	final boolean finishActivityLocked(ActivityRecord r, int index,
			int resultCode, Intent resultData, String reason) {
//		System.out.println(TAG + "in finishActivityLocked");
		if (r.finishing) {
//			System.out.println(TAG + "Duplicate finish request for "
//					+ r.info.name);
			return false;
		}

		r.finishing = true;
		r.task.numActivities--;
		if (index < (mHistory.size() - 1)) {
			// not the top activity
			ActivityRecord next = (ActivityRecord) mHistory.get(index + 1);
			if (next.task == r.task) {
				if (r.frontOfTask) {
					// The next activity is now the front of the task.
					next.frontOfTask = true;
				}
				if ((r.intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) != 0) {
					// If the caller asked that this activity (and all above it)
					// be cleared when the task is reset, don't lose that information,
					// but propagate it up to the next activity.
					next.intent
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				}
			}
		}

		r.pauseKeyDispatchingLocked();
		if (mMainStack) {
			if (mService.mFocusedActivity == r) {
				mService.setFocusedActivityLocked(topRunningActivityLocked(null));
			}
		}

		// send the result
		ActivityRecord resultTo = r.resultTo;
		if (resultTo != null) {
			//				System.out.println(TAG+ "Adding result to " + resultTo
			//						+ " who=" + r.resultWho + " req=" + r.requestCode
			//						+ " res=" + resultCode + " data=" + resultData);
			resultTo.addResultLocked(r, r.resultWho, r.requestCode, resultCode,
					resultData);
			r.resultTo = null;
		}

		// Make sure this HistoryRecord is not holding on to other resources,
		// because clients have remote IPC references to this object so we
		// can't assume that will go away and want to avoid circular IPC refs.
		r.results = null;
		r.newIntents = null;
		r.icicle = null;

		if (mResumedActivity == r) {
			//			boolean endTask = index <= 0
			//					|| ((ActivityRecord) mHistory.get(index - 1)).task != r.task;
			//			if (DEBUG_TRANSITION)
			//				System.out.println(TAG, "Prepare close transition: finishing "
			//						+ r);
			//			mService.mWindowManager
			//					.prepareAppTransition(endTask ? WindowManagerPolicy.TRANSIT_TASK_CLOSE
			//							: WindowManagerPolicy.TRANSIT_ACTIVITY_CLOSE);
			//
			//			// Tell window manager to prepare for this one to be removed.
			// TODO 这里以后要的，必须？有个机制通知window不要visible
			//			mService.mWindowManager.setAppVisibility(r, false);

			if (mPausingActivity == null) {
				Log.i(TAG, "Finish needs to pause: "
						+ r.info.name);
				startPausingLocked(false, false);
				// after startPausingLocked, in JSDroid, the top activity has been resumed and been visible
				Log.i(TAG, "Finish not pausing: " + r.info.name);
				return finishCurrentActivityLocked(r, index, FINISH_IMMEDIATELY) == null;
			}
		} else if (r.state != ActivityState.PAUSING) {
			// If the activity is PAUSING, we will complete the finish once
			// it is done pausing; else we can just directly finish it here.
			Log.i(TAG, "impossible here");
			Log.i(TAG, "Finish not pausing: " + r.info.name);
            if (r.state == ActivityState.PAUSED) {
                return finishCurrentActivityLocked(r, index, FINISH_IMMEDIATELY) == null;
            }
			return finishCurrentActivityLocked(r, index, FINISH_AFTER_PAUSE) == null;
		} else {
			Log.i(TAG, "Finish waiting for pause of: "
					+ r.info.name);
		}

		Log.i(TAG, "finishActivityLocked Over");
		return false;
	}

	private static final int FINISH_IMMEDIATELY = 0;
	private static final int FINISH_AFTER_PAUSE = 1;
	private static final int FINISH_AFTER_VISIBLE = 2;

	//	private final ActivityRecord finishCurrentActivityLocked(ActivityRecord r,
	//			int mode) {
	//		final int index = indexOfTokenLocked(r);
	//		if (index < 0) {
	//			return null;
	//		}
	//
	//		return finishCurrentActivityLocked(r, index, mode);
	//	}

	private final ActivityRecord finishCurrentActivityLocked(ActivityRecord r,
			int index, int mode) {
		Log.i(TAG, "in finishCurrentActivityLocked");
		// First things first: if this activity is currently visible,
		// and the resumed activity is not yet visible, then hold off on
		// finishing until the resumed one becomes visible.
		if (mode == FINISH_AFTER_VISIBLE && r.nowVisible) {
			if (!mStoppingActivities.contains(r)) {
				mStoppingActivities.add(r);
				if (mStoppingActivities.size() > 3) {
					// If we already have a few activities waiting to stop,
					// then give up on things going idle and start clearing
					// them out.
					Message msg = Message.obtain();
					msg.what = IDLE_NOW_MSG;
					mHandler.sendMessage(msg);
				}
			}
			r.state = ActivityState.STOPPING;
			return r;
		}

		// make sure the record is cleaned out of other places.
		mStoppingActivities.remove(r);
		mWaitingVisibleActivities.remove(r);
		if (mResumedActivity == r) {
			mResumedActivity = null;
		}
		final int prevState = r.state;
		r.state = ActivityState.FINISHING;

		if (mode == FINISH_IMMEDIATELY || prevState == ActivityState.STOPPED
				|| prevState == ActivityState.INITIALIZING) {
			// If this activity is already stopped, we can just finish
			// it right now.
			return destroyActivityLocked(r, true) ? null : r;
		} else {
			// Need to go through the full pause cycle to get this
			// activity into the stopped state and then finish it.
			Log.i(TAG, "state:"+prevState);
			Log.i(TAG, "Enqueueing pending finish: "
								+ r.info.name);
			mFinishingActivities.add(r);
			resumeTopActivityLocked(null);
		}
		return r;
	}

	/**
	 * Perform the common clean-up of an activity record.  This is called both
	 * as part of destroyActivityLocked() (when destroying the client-side
	 * representation) and cleaning things up as a result of its hosting
	 * processing going away, in which case there is no remaining client-side
	 * state to destroy so only the cleanup here is needed.
	 */
	final void cleanUpActivityLocked(ActivityRecord r, boolean cleanServices) {
		if (mResumedActivity == r) {
			mResumedActivity = null;
		}
		if (mService.mFocusedActivity == r) {
			mService.mFocusedActivity = null;
		}

		r.configDestroy = false;
		r.frozenBeforeDestroy = false;

		// Make sure this record is no longer in the pending finishes list.
		// This could happen, for example, if we are trimming activities
		// down to the max limit while they are still waiting to finish.
		mFinishingActivities.remove(r);
		mWaitingVisibleActivities.remove(r);
	}

	private final void removeActivityFromHistoryLocked(ActivityRecord r) {
		if (r.state != ActivityState.DESTROYED) {
			mHistory.remove(r);
			r.inHistory = false;
			r.state = ActivityState.DESTROYED;
			//			mService.mWindowManager.removeAppToken(r);
		}
	}

	//
	//	/**
	//	 * Perform clean-up of service connections in an activity record.
	//	 */
	//	final void cleanUpActivityServicesLocked(ActivityRecord r) {
	//		// Throw away any services that have been bound by this activity.
	//		if (r.connections != null) {
	//			Iterator<ConnectionRecord> it = r.connections.iterator();
	//			while (it.hasNext()) {
	//				ConnectionRecord c = it.next();
	//				mService.removeConnectionLocked(c, null, r);
	//			}
	//			r.connections = null;
	//		}
	//	}

	/**
	 * Destroy the current CLIENT SIDE instance of an activity.  This may be
	 * called both when actually finishing an activity, or when performing
	 * a configuration switch where we destroy the current client-side object
	 * but then create a new client-side object for this same HistoryRecord.
	 */
	final boolean destroyActivityLocked(ActivityRecord r, boolean removeFromApp) {
		boolean removedFromHistory = false;
		cleanUpActivityLocked(r, false);

		final boolean hadApp = r.app != null;

		if (hadApp) {
			if (removeFromApp) {
				int idx = r.app.activities.indexOf(r);
				if (idx >= 0) {
					r.app.activities.remove(idx);
				}
			}
			boolean skipDestroy = false;

//			System.out.println(TAG + "Destroying: " + r.info.name);
			r.app.thread.scheduleDestroyActivity(r, r.finishing,
					r.configChangeFlags);

			r.app = null;
			r.nowVisible = false;

			if (r.finishing && !skipDestroy) {
				r.state = ActivityState.DESTROYING;
			} else {
				r.state = ActivityState.DESTROYED;
			}
		} else {
			// remove this record from the history.
			if (r.finishing) {
				removeActivityFromHistoryLocked(r);
				removedFromHistory = true;
			} else {
				r.state = ActivityState.DESTROYED;
			}
		}

		r.configChangeFlags = 0;

		if (!mLRUActivities.remove(r) && hadApp) {
			Log.i(TAG, "Activity " + r.info.name
					+ " being finished, but not in LRU list");
		}

		return removedFromHistory;
	}

	final void activityDestroyed(IBinder token) {
		int index = indexOfTokenLocked(token);
		if (index >= 0) {
			ActivityRecord r = (ActivityRecord) mHistory.get(index);
			if (r.state == ActivityState.DESTROYING) {
				removeActivityFromHistoryLocked(r);
			}
		}
	}
	//
	//	private static void removeHistoryRecordsForAppLocked(ArrayList list,
	//			ProcessRecord app) {
	//		int i = list.size();
	//		if (localLOGV)
	//			System.out.println(TAG, "Removing app " + app + " from list " + list + " with "
	//					+ i + " entries");
	//		while (i > 0) {
	//			i--;
	//			ActivityRecord r = (ActivityRecord) list.get(i);
	//			if (localLOGV)
	//				System.out.println(TAG, "Record #" + i + " " + r + ": app=" + r.app);
	//			if (r.app == app) {
	//				if (localLOGV)
	//					System.out.println(TAG, "Removing this entry!");
	//				list.remove(i);
	//			}
	//		}
	//	}
	//
	//	void removeHistoryRecordsForAppLocked(ProcessRecord app) {
	//		removeHistoryRecordsForAppLocked(mLRUActivities, app);
	//		removeHistoryRecordsForAppLocked(mStoppingActivities, app);
	//		removeHistoryRecordsForAppLocked(mWaitingVisibleActivities, app);
	//		removeHistoryRecordsForAppLocked(mFinishingActivities, app);
	//	}
	//
	//	final void moveTaskToFrontLocked(TaskRecord tr, ActivityRecord reason) {
	//		if (DEBUG_SWITCH)
	//			System.out.println(TAG, "moveTaskToFront: " + tr);
	//
	//		final int task = tr.taskId;
	//		int top = mHistory.size() - 1;
	//
	//		if (top < 0 || ((ActivityRecord) mHistory.get(top)).task.taskId == task) {
	//			// nothing to do!
	//			return;
	//		}
	//
	//		ArrayList moved = new ArrayList();
	//
	//		// Applying the affinities may have removed entries from the history,
	//		// so get the size again.
	//		top = mHistory.size() - 1;
	//		int pos = top;
	//
	//		// Shift all activities with this task up to the top
	//		// of the stack, keeping them in the same internal order.
	//		while (pos >= 0) {
	//			ActivityRecord r = (ActivityRecord) mHistory.get(pos);
	//			if (localLOGV)
	//				System.out.println(TAG, "At " + pos + " ckp " + r.task + ": " + r);
	//			boolean first = true;
	//			if (r.task.taskId == task) {
	//				if (localLOGV)
	//					System.out.println(TAG, "Removing and adding at " + top);
	//				mHistory.remove(pos);
	//				mHistory.add(top, r);
	//				moved.add(0, r);
	//				top--;
	//				if (first && mMainStack) {
	//					mService.addRecentTaskLocked(r.task);
	//					first = false;
	//				}
	//			}
	//			pos--;
	//		}
	//
	//		if (DEBUG_TRANSITION)
	//			System.out.println(TAG, "Prepare to front transition: task=" + tr);
	//		if (reason != null
	//				&& (reason.intent.getFlags() & Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0) {
	//			mService.mWindowManager
	//					.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
	//			ActivityRecord r = topRunningActivityLocked(null);
	//			if (r != null) {
	//				mNoAnimActivities.add(r);
	//			}
	//		} else {
	//			mService.mWindowManager
	//					.prepareAppTransition(WindowManagerPolicy.TRANSIT_TASK_TO_FRONT);
	//		}
	//
	//		mService.mWindowManager.moveAppTokensToTop(moved);
	//		if (VALIDATE_TOKENS) {
	//			mService.mWindowManager.validateAppTokens(mHistory);
	//		}
	//
	//		finishTaskMoveLocked(task);
	//		EventLog.writeEvent(EventLogTags.AM_TASK_TO_FRONT, task);
	//	}
	//
	//	private final void finishTaskMoveLocked(int task) {
	//		resumeTopActivityLocked(null);
	//	}
	//
	//	/**
	//	 * Worker method for rearranging history stack.  Implements the function of moving all 
	//	 * activities for a specific task (gathering them if disjoint) into a single group at the 
	//	 * bottom of the stack.
	//	 * 
	//	 * If a watcher is installed, the action is preflighted and the watcher has an opportunity
	//	 * to premeptively cancel the move.
	//	 * 
	//	 * @param task The taskId to collect and move to the bottom.
	//	 * @return Returns true if the move completed, false if not.
	//	 */
	//	final boolean moveTaskToBackLocked(int task, ActivityRecord reason) {
	//		System.out.println(TAG, "moveTaskToBack: " + task);
	//
	//		// If we have a watcher, preflight the move before committing to it.  First check
	//		// for *other* available tasks, but if none are available, then try again allowing the
	//		// current task to be selected.
	//		if (mMainStack && mService.mController != null) {
	//			ActivityRecord next = topRunningActivityLocked(null, task);
	//			if (next == null) {
	//				next = topRunningActivityLocked(null, 0);
	//			}
	//			if (next != null) {
	//				// ask watcher if this is allowed
	//				boolean moveOK = true;
	//				try {
	//					moveOK = mService.mController
	//							.activityResuming(next.packageName);
	//				} catch (RemoteException e) {
	//					mService.mController = null;
	//				}
	//				if (!moveOK) {
	//					return false;
	//				}
	//			}
	//		}
	//
	//		ArrayList moved = new ArrayList();
	//
	//		if (DEBUG_TRANSITION)
	//			System.out.println(TAG, "Prepare to back transition: task=" + task);
	//
	//		final int N = mHistory.size();
	//		int bottom = 0;
	//		int pos = 0;
	//
	//		// Shift all activities with this task down to the bottom
	//		// of the stack, keeping them in the same internal order.
	//		while (pos < N) {
	//			ActivityRecord r = (ActivityRecord) mHistory.get(pos);
	//			if (localLOGV)
	//				System.out.println(TAG, "At " + pos + " ckp " + r.task + ": " + r);
	//			if (r.task.taskId == task) {
	//				if (localLOGV)
	//					System.out.println(TAG, "Removing and adding at " + (N - 1));
	//				mHistory.remove(pos);
	//				mHistory.add(bottom, r);
	//				moved.add(r);
	//				bottom++;
	//			}
	//			pos++;
	//		}
	//
	//		if (reason != null
	//				&& (reason.intent.getFlags() & Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0) {
	//			mService.mWindowManager
	//					.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
	//			ActivityRecord r = topRunningActivityLocked(null);
	//			if (r != null) {
	//				mNoAnimActivities.add(r);
	//			}
	//		} else {
	//			mService.mWindowManager
	//					.prepareAppTransition(WindowManagerPolicy.TRANSIT_TASK_TO_BACK);
	//		}
	//		mService.mWindowManager.moveAppTokensToBottom(moved);
	//		if (VALIDATE_TOKENS) {
	//			mService.mWindowManager.validateAppTokens(mHistory);
	//		}
	//
	//		finishTaskMoveLocked(task);
	//		return true;
	//	}
	//
	//	private final void logStartActivity(int tag, ActivityRecord r,
	//			TaskRecord task) {
	//		EventLog.writeEvent(tag, System.identityHashCode(r), task.taskId,
	//				r.shortComponentName, r.intent.getAction(), r.intent.getType(),
	//				r.intent.getDataString(), r.intent.getFlags());
	//	}
	//
	//	/**
	//	 * Make sure the given activity matches the current configuration.  Returns
	//	 * false if the activity had to be destroyed.  Returns true if the
	//	 * configuration is the same, or the activity will remain running as-is
	//	 * for whatever reason.  Ensures the HistoryRecord is updated with the
	//	 * correct configuration and all other bookkeeping is handled.
	//	 */
	//	final boolean ensureActivityConfigurationLocked(ActivityRecord r,
	//			int globalChanges) {
	//		if (mConfigWillChange) {
	//			if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//				System.out.println(TAG, "Skipping config check (will change): " + r);
	//			return true;
	//		}
	//
	//		if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//			System.out.println(TAG, "Ensuring correct configuration: " + r);
	//
	//		// Short circuit: if the two configurations are the exact same
	//		// object (the common case), then there is nothing to do.
	//		Configuration newConfig = mService.mConfiguration;
	//		if (r.configuration == newConfig) {
	//			if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//				System.out.println(TAG, "Configuration unchanged in " + r);
	//			return true;
	//		}
	//
	//		// We don't worry about activities that are finishing.
	//		if (r.finishing) {
	//			if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//				System.out.println(TAG, "Configuration doesn't matter in finishing " + r);
	//			r.stopFreezingScreenLocked(false);
	//			return true;
	//		}
	//
	//		// Okay we now are going to make this activity have the new config.
	//		// But then we need to figure out how it needs to deal with that.
	//		Configuration oldConfig = r.configuration;
	//		r.configuration = newConfig;
	//
	//		// If the activity isn't currently running, just leave the new
	//		// configuration and it will pick that up next time it starts.
	//		if (r.app == null || r.app.thread == null) {
	//			if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//				System.out.println(TAG, "Configuration doesn't matter not running " + r);
	//			r.stopFreezingScreenLocked(false);
	//			return true;
	//		}
	//
	//		// Figure out what has changed between the two configurations.
	//		int changes = oldConfig.diff(newConfig);
	//		if (DEBUG_SWITCH || DEBUG_CONFIGURATION) {
	//			System.out.println(TAG,
	//					"Checking to restart " + r.info.name + ": changed=0x"
	//							+ Integer.toHexString(changes) + ", handles=0x"
	//							+ Integer.toHexString(r.info.configChanges)
	//							+ ", newConfig=" + newConfig);
	//		}
	//		if ((changes & (~r.info.configChanges)) != 0) {
	//			// Aha, the activity isn't handling the change, so DIE DIE DIE.
	//			r.configChangeFlags |= changes;
	//			r.startFreezingScreenLocked(r.app, globalChanges);
	//			if (r.app == null || r.app.thread == null) {
	//				if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//					System.out.println(TAG, "Switch is destroying non-running " + r);
	//				destroyActivityLocked(r, true);
	//			} else if (r.state == ActivityState.PAUSING) {
	//				// A little annoying: we are waiting for this activity to
	//				// finish pausing.  Let's not do anything now, but just
	//				// flag that it needs to be restarted when done pausing.
	//				if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//					System.out.println(TAG, "Switch is skipping already pausing " + r);
	//				r.configDestroy = true;
	//				return true;
	//			} else if (r.state == ActivityState.RESUMED) {
	//				// Try to optimize this case: the configuration is changing
	//				// and we need to restart the top, resumed activity.
	//				// Instead of doing the normal handshaking, just say
	//				// "restart!".
	//				if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//					System.out.println(TAG, "Switch is restarting resumed " + r);
	//				relaunchActivityLocked(r, r.configChangeFlags, true);
	//				r.configChangeFlags = 0;
	//			} else {
	//				if (DEBUG_SWITCH || DEBUG_CONFIGURATION)
	//					System.out.println(TAG, "Switch is restarting non-resumed " + r);
	//				relaunchActivityLocked(r, r.configChangeFlags, false);
	//				r.configChangeFlags = 0;
	//			}
	//
	//			// All done...  tell the caller we weren't able to keep this
	//			// activity around.
	//			return false;
	//		}
	//
	//		// Default case: the activity can handle this new configuration, so
	//		// hand it over.  Note that we don't need to give it the new
	//		// configuration, since we always send configuration changes to all
	//		// process when they happen so it can just use whatever configuration
	//		// it last got.
	//		if (r.app != null && r.app.thread != null) {
	//			try {
	//				if (DEBUG_CONFIGURATION)
	//					System.out.println(TAG, "Sending new config to " + r);
	//				r.app.thread.scheduleActivityConfigurationChanged(r);
	//			} catch (RemoteException e) {
	//				// If process died, whatever.
	//			}
	//		}
	//		r.stopFreezingScreenLocked(false);
	//
	//		return true;
	//	}
	//
	//	private final boolean relaunchActivityLocked(ActivityRecord r, int changes,
	//			boolean andResume) {
	//		List<ResultInfo> results = null;
	//		List<Intent> newIntents = null;
	//		if (andResume) {
	//			results = r.results;
	//			newIntents = r.newIntents;
	//		}
	//		if (DEBUG_SWITCH)
	//			System.out.println(TAG, "Relaunching: " + r + " with results=" + results
	//					+ " newIntents=" + newIntents + " andResume=" + andResume);
	//		EventLog.writeEvent(
	//				andResume ? EventLogTags.AM_RELAUNCH_RESUME_ACTIVITY
	//						: EventLogTags.AM_RELAUNCH_ACTIVITY, System
	//						.identityHashCode(r), r.task.taskId,
	//				r.shortComponentName);
	//
	//		r.startFreezingScreenLocked(r.app, 0);
	//
	//		try {
	//			if (DEBUG_SWITCH)
	//				System.out.println(TAG, "Switch is restarting resumed " + r);
	//			r.app.thread.scheduleRelaunchActivity(r, results, newIntents,
	//					changes, !andResume, mService.mConfiguration);
	//			// Note: don't need to call pauseIfSleepingLocked() here, because
	//			// the caller will only pass in 'andResume' if this activity is
	//			// currently resumed, which implies we aren't sleeping.
	//		} catch (RemoteException e) {
	//			return false;
	//		}
	//
	//		if (andResume) {
	//			r.results = null;
	//			r.newIntents = null;
	//			if (mMainStack) {
	//				mService.reportResumedActivityLocked(r);
	//			}
	//		}
	//
	//		return true;
	//	}
}
