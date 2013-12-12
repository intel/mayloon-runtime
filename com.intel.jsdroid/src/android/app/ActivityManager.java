package android.app;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.app.ActivityStack.ActivityState;
import android.content.BroadcastFilter;
import android.content.BroadcastReceiver;
import android.content.BroadcastRecord;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderRecord;
import android.content.ContentService;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverList;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.IntentResolver;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;

public class ActivityManager {
	public static final String LAUNCHER = "com.intel.jsdroid.sudoku";
	private static boolean DEBUG_PROVIDER = true;

	/**
	 * Returned by startActivity() if the start request was canceled because app
	 * switches are temporarily canceled to ensure the user's last request (such
	 * as pressing home) is performed.
	 */
	public static final int START_SWITCHES_CANCELED = 4;
	/**
	 * Returned by startActivity() if an activity wasn't really started, but the
	 * given Intent was given to the existing top activity.
	 */
	public static final int START_DELIVERED_TO_TOP = 3;
	/**
	 * Returned by startActivity() if an activity wasn't really started, but a
	 * task was simply brought to the foreground.
	 */
	public static final int START_TASK_TO_FRONT = 2;
	/**
	 * Returned by startActivity() if the caller asked that the Intent not be
	 * executed if it is the recipient, and that is indeed the case.
	 */
	public static final int START_RETURN_INTENT_TO_CALLER = 1;
	/**
	 * Activity was started successfully as normal.
	 */
	public static final int START_SUCCESS = 0;
	public static final int START_INTENT_NOT_RESOLVED = -1;
	public static final int START_CLASS_NOT_FOUND = -2;
	public static final int START_FORWARD_AND_REQUEST_CONFLICT = -3;
	public static final int START_PERMISSION_DENIED = -4;
	public static final int START_NOT_ACTIVITY = -5;
	public static final int START_CANCELED = -6;

	// This is a process running a core server, such as telephony.  Definitely
    // don't want to kill it, but doing so is not completely fatal.
    static final int CORE_SERVER_ADJ = -12;


	public static final String TAG = "ActivityManager>>>";
	private final Context mContext;
	public Activity mCurActivity = null;


	// How long we wait for a service to finish executing.
	static final int SERVICE_TIMEOUT = 20 * 1000;

	static final boolean DEBUG_SERVICE = true;

	/**
     * List of persistent applications that are in the process
     * of being started.
     */
    final ArrayList<ProcessRecord> mPersistentStartingProcesses
            = new ArrayList<ProcessRecord>();


	/**
	 * Task identifier that activities are currently being started in.
	 * Incremented each time a new task is created. todo: Replace this with a
	 * TokenSpace class that generates non-repeating integers that won't wrap.
	 */
	int mCurTask = 0;

	public ActivityManager(Context context) {
		mContext = context;
	}

	public void main() {
		Looper.prepare();
		mMainStack = new ActivityStack(this, mContext, true);

		startRunning(null, null, null, null);
	}

	public ActivityStack mMainStack;
	boolean mStartRunning;
	String mTopAction;
	String mTopData;
	boolean mSystemReady;
	ComponentName mTopComponent;

	public int startActivity(IApplicationThread caller, Intent intent,
			String resolvedType, Uri[] grantedUriPermissions, int grantedMode,
			IBinder resultTo, String resultWho, int requestCode,
			boolean onlyIfNeeded, boolean debug) {
		// System.out.println("in startActivity");
		return mMainStack.startActivityMayWait(caller, intent, resolvedType,
				grantedUriPermissions, grantedMode, resultTo, resultWho,
				requestCode, onlyIfNeeded, debug);
	}



	public ComponentName startService(IApplicationThread caller, Intent service,
            String resolvedType)
	{
		if (service != null && service.hasFileDescriptors() == true) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }

        synchronized(this) {
           // final int callingPid = Binder.getCallingPid();
           // final int callingUid = Binder.getCallingUid();
           // final long origId = Binder.clearCallingIdentity();
            ComponentName res = startServiceLocked(caller, service,
                    resolvedType);
           // Binder.restoreCallingIdentity(origId);
            return res;
        }
	}

	private static final HashMap<ServiceRecord, ArrayList<ConnectionRecord>> mServiceConnection = new HashMap<ServiceRecord, ArrayList<ConnectionRecord>>();
	private class ConnectionInfo {
        IBinder binder;
//        IBinder.DeathRecipient deathMonitor;
    }
	 private final  HashMap<ComponentName, ConnectionInfo> mActiveConnections
     = new HashMap<ComponentName, ConnectionInfo>();

	public int bindService(IApplicationThread caller,
            Intent service, String resolvedType,
            ServiceConnection connection, int flags) {
        // Refuse possible leaked file descriptors
        if (service != null && service.hasFileDescriptors() == true) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }

        final ProcessRecord callerApp = getRecordForAppLocked(caller);

        Log.i(TAG, "bindService:");
            int clientLabel = service.getIntExtra(Intent.EXTRA_CLIENT_LABEL, 0);
//            PendingIntent clientIntent = null;
//
//
//                try {
//                    clientIntent = (PendingIntent)service.getParcelableExtra(
//                            Intent.EXTRA_CLIENT_INTENT);
//                } catch (RuntimeException e) {
//                }
//                if (clientIntent != null) {
//                    clientLabel = service.getIntExtra(Intent.EXTRA_CLIENT_LABEL, 0);
//                    if (clientLabel != 0) {
//                        // There are no useful extras in the intent, trash them.
//                        // System code calling with this stuff just needs to know
//                        // this will happen.
//                        service = service.cloneFilter();
//                    }
//                }

            ServiceLookupResult res =
                retrieveServiceLocked(service, resolvedType);
            if (res == null) {
                return 0;
            }
            if (res.record == null) {
                return -1;
            }
            ServiceRecord s = res.record;

//            final long origId = Binder.clearCallingIdentity();

//            if (unscheduleServiceRestartLocked(s)) {
//                if (DEBUG_SERVICE) Slog.v(TAG, "BIND SERVICE WHILE RESTART PENDING: "
//                        + s);
//            }

            AppBindRecord b = s.retrieveAppBindingLocked(service, callerApp);
            ConnectionRecord c = new ConnectionRecord(b,
                    connection, flags, clientLabel);

//            IBinder binder = connection.asBinder();
            ArrayList<ConnectionRecord> clist = mServiceConnection.get(s);
            if (clist == null) {
                clist = new ArrayList<ConnectionRecord>();
                mServiceConnection.put(s, clist);
            }
            clist.add(c);
//            b.connections.add(c);
//            if (activity != null) {
//                if (activity.connections == null) {
//                    activity.connections = new HashSet<ConnectionRecord>();
//                }
//                activity.connections.add(c);
//            }
//            b.client.connections.add(c);
//            if ((c.flags&Context.BIND_ABOVE_CLIENT) != 0) {
//                b.client.hasAboveClient = true;
//            }
//            clist = mServiceConnections.get(binder);
//            if (clist == null) {
//                clist = new ArrayList<ConnectionRecord>();
//                mServiceConnections.put(binder, clist);
//            }
//            clist.add(c);


            if ((flags&Context.BIND_AUTO_CREATE) != 0) {
                s.lastActivity = SystemClock.uptimeMillis();
                if (!bringUpServiceLocked(s, service.getFlags(), false)) {
                    return 0;
                }
            }

//            if (s.app != null) {
//                // This could have made the service more important.
//                updateOomAdjLocked(s.app);
//            }
//
//            if (DEBUG_SERVICE) Slog.v(TAG, "Bind " + s + " with " + b
//                    + ": received=" + b.intent.received
//                    + " apps=" + b.intent.apps.size()
//                    + " doRebind=" + b.intent.doRebind);
//
//            if (s.app != null && b.intent.received) {
//                // Service is already running, so we can immediately
//                // publish the connection.
//                try {
//                    c.conn.connected(s.name, b.intent.binder);
//                } catch (Exception e) {
//                    Slog.w(TAG, "Failure sending service " + s.shortName
//                            + " to connection " + c.conn.asBinder()
//                            + " (in " + c.binding.client.processName + ")", e);
//                }
//
//                // If this is the first app connected back to this binding,
//                // and the service had previously asked to be told when
//                // rebound, then do so.
//                if (b.intent.apps.size() == 1 && b.intent.doRebind) {
//                    requestServiceBindingLocked(s, b.intent, true);
//                }
//            } else if (!b.intent.requested) {
//                requestServiceBindingLocked(s, b.intent, false);
//            }
//
//            Binder.restoreCallingIdentity(origId);


        return 1;
    }

	private ComponentName startServiceLocked(IApplicationThread caller,
			Intent service, String resolvedType) {
		  synchronized(this) {
//	            if (DEBUG_SERVICE) Slog.v(TAG, "startService: " + service
//	                    + " type=" + resolvedType + " args=" + service.getExtras());

//	            if (caller != null) {
//	                final ProcessRecord callerApp = getRecordForAppLocked(caller);
//	                if (callerApp == null) {
//	                    throw new SecurityException(
//	                            "Unable to find app for caller " + caller
//	                            + ") when starting service " + service);
//	                }
//	            }

	            ServiceLookupResult res =
	                retrieveServiceLocked(service, resolvedType);
	            if (res == null) {
	                return null;
	            }
	            if (res.record == null) {
	                return new ComponentName("!", res.permission != null
	                        ? res.permission : "private to package");
	            }
	            ServiceRecord r = res.record;
//	            int targetPermissionUid = checkGrantUriPermissionFromIntentLocked(
//	                    callingUid, r.packageName, service);
//	            if (unscheduleServiceRestartLocked(r)) {
//	                if (DEBUG_SERVICE) Slog.v(TAG, "START SERVICE WHILE RESTART PENDING: " + r);
//	            }
	            r.startRequested = true;
	            r.callStart = false;
	            r.pendingStarts.add(new ServiceRecord.StartItem(r, 1, service, -1));
	            r.lastActivity = SystemClock.uptimeMillis();
//	            synchronized (r.stats.getBatteryStats()) {
//	                r.stats.startRunningLocked();
//	            }
	            if (!bringUpServiceLocked(r, service.getFlags(), false)) {
	                return new ComponentName("!", "Service process is bad");
	            }
	            return r.name;
	        }
	}

	private final boolean bringUpServiceLocked(ServiceRecord r,
            int intentFlags, boolean whileRestarting) {
        //Slog.i(TAG, "Bring up service:");
        //r.dump("  ");

		Log.i(TAG, "bringUpServiceLocked:");
        if (r.app != null && r.app.thread != null) {
            //sendServiceArgsLocked(r, false);
            return true;
        }

//        if (!whileRestarting && r.restartDelay > 0) {
//            // If waiting for a restart, then do nothing.
//            return true;
//        }

//        if (DEBUG_SERVICE) Slog.v(TAG, "Bringing up " + r + " " + r.intent);

        // We are now bringing the service up, so no longer in the
        // restarting state.
//        mRestartingServices.remove(r);

        // Service is now being launched, its package can't be stopped.
//        try {
//            AppGlobals.getPackageManager().setPackageStoppedState(
//                    r.packageName, false);
//        } catch (RemoteException e) {
//        } catch (IllegalArgumentException e) {
//            Slog.w(TAG, "Failed trying to unstop package "
//                    + r.packageName + ": " + e);
//        }

        final String appName = r.processName;


//        ProcessRecord app = mProcessNames.get(appName);
        ProcessRecord app = newProcessRecordLocked(null, r.appInfo, appName);
//        app.thread = mContext.getActivityThread().getApplicationThread();
//        ProcessRecord app = getProcessRecordLocked(appName, r.appInfo.uid);;
        if (app != null ) {

            try {
                app.addPackage(r.appInfo.packageName);
                realStartServiceLocked(r, app);
                return true;
            } catch (Exception e) {

            }

            // If a dead object exception was thrown -- fall through to
            // restart the application.
        }

        // Not running -- get it started, and enqueue this service record
//        // to be executed when the app comes up.
//        if (startProcessLocked(appName, r.appInfo, true, intentFlags,
//                "service", r.name, false) == null) {
//            Slog.w(TAG, "Unable to launch app "
//                    + r.appInfo.packageName + "/"
//                    + r.appInfo.uid + " for service "
//                    + r.intent.getIntent() + ": process is bad");
//            bringDownServiceLocked(r, true);
//            return false;
//        }

        if (!mPendingServices.contains(r)) {
            mPendingServices.add(r);
        }

        return true;
    }



	   private final class ServiceLookupResult {
	        final ServiceRecord record;
	        final String permission;

	        ServiceLookupResult(ServiceRecord _record, String _permission) {
	            record = _record;
	            permission = _permission;
	        }
	    };

	    private final void realStartServiceLocked(ServiceRecord r,
	            ProcessRecord app)  {
	    	Log.i(TAG, "realStartServiceLocked:");
	        r.app = app;
	        r.restartTime = r.lastActivity = SystemClock.uptimeMillis();

	        app.services.add(r);
//	        bumpServiceExecutingLocked(r, "create");
//	        updateLruProcessLocked(app, true, true);

//	        boolean created = false;
//	        try {
//	            mStringBuilder.setLength(0);
//	            r.intent.getIntent().toShortString(mStringBuilder, true, false, true);
//	            EventLog.writeEvent(EventLogTags.AM_CREATE_SERVICE,
//	                    System.identityHashCode(r), r.shortName,
//	                    mStringBuilder.toString(), r.app.pid);
//	            synchronized (r.stats.getBatteryStats()) {
//	                r.stats.startLaunchedLocked();
//	            }
//	            ensurePackageDexOpt(r.serviceInfo.packageName);
	            mContext.getActivityThread().getApplicationThread().scheduleCreateService(r, r.serviceInfo);
//	                    compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo));
//	            r.postNotification();
//	            created = true;
//	        } finally {
//	            if (!created) {
//	                app.services.remove(r);
//	                scheduleServiceRestartLocked(r, false);
//	            }
//	        }
//
	        requestServiceBindingsLocked(r);
//
//	        // If the service is in the started state, and there are no
//	        // pending arguments, then fake up one so its onStartCommand() will
//	        // be called.
	        if (r.startRequested && r.callStart && r.pendingStarts.size() == 0) {
	            r.pendingStarts.add(new ServiceRecord.StartItem(r,  1, null, -1));
	        }
//
	        sendServiceArgsLocked(r, true);
	    }


	    private void sendServiceArgsLocked(ServiceRecord r, boolean b) {
	    	final int N = r.pendingStarts.size();
	        if (N == 0) {
	            return;
	        }

	        while (r.pendingStarts.size() > 0) {

	                ServiceRecord.StartItem si = r.pendingStarts.remove(0);
	                if (si.intent == null && N > 1) {
	                    // If somehow we got a dummy null intent in the middle,
	                    // then skip it.  DO NOT skip a null intent when it is
	                    // the only one in the list -- this is to support the
	                    // onStartCommand(null) case.
	                    continue;
	                }
	                si.deliveredTime = SystemClock.uptimeMillis();
	                r.deliveredStarts.add(si);
	                si.deliveryCount++;
//	                if (si.targetPermissionUid >= 0 && si.intent != null) {
//	                    grantUriPermissionUncheckedFromIntentLocked(si.targetPermissionUid,
//	                            r.packageName, si.intent, si.getUriPermissionsLocked());
//	                }
//	                bumpServiceExecutingLocked(r, "start");
//	                if (!oomAdjusted) {
//	                    oomAdjusted = true;
//	                    updateOomAdjLocked(r.app);
//	                }
	                int flags = 0;
	                if (si.deliveryCount > 0) {
	                    flags |= Service.START_FLAG_RETRY;
	                }
	                if (si.doneExecutingCount > 0) {
	                    flags |= Service.START_FLAG_REDELIVERY;
	                }
	                mContext.getActivityThread().getApplicationThread().scheduleServiceArgs(r, si.id, flags, si.intent);
	        }

		}

		private void requestServiceBindingsLocked(ServiceRecord r) {

			Log.i(TAG, "requestServiceBindingsLocked:");
			// TODO Auto-generated method stub
	    	Iterator<IntentBindRecord> bindings = r.bindings.values().iterator();
	        while (bindings.hasNext()) {
	            IntentBindRecord i = bindings.next();
	            if (!requestServiceBindingLocked(r, i, false)) {
	                break;
	            }
	        }
		}

		private boolean requestServiceBindingLocked(ServiceRecord r,
				IntentBindRecord i, boolean rebind) {
			Log.i(TAG, "requestServiceBindingsLocked2:");
	        if (r.app == null ) {
	            // If service is not currently running, can't yet bind.
	            return false;
	        }
	        if ((!i.requested || rebind) && i.apps.size() > 0) {
//	                bumpServiceExecutingLocked(r, "bind");
	                mContext.getActivityThread().getApplicationThread().scheduleBindService(r, i.intent.getIntent(), rebind);
	                if (!rebind) {
	                    i.requested = true;
	                }
	                i.hasBound = true;
	                i.doRebind = false;
	            }
	        return true;
		}

		private ServiceLookupResult retrieveServiceLocked(Intent service,
	            String resolvedType) {

	        ServiceRecord r = null;
	        if (service.getComponent() != null) {
	            r = mServices.get(service.getComponent());
	        }
	        Intent.FilterComparison filter = new Intent.FilterComparison(service);
	        r = mServicesByIntent.get(filter);

	        if (r == null) {
	        	ResolveInfo rInfo = Context.getSystemContext().getPackageManager().resolveService(
	        			service,resolvedType,STOCK_PM_FLAGS);

	            ServiceInfo sInfo = rInfo != null ? rInfo.serviceInfo : null;

	            ComponentName name = service.getComponent();
	    	        r = mServices.get(name);
	                if (r == null) {
	                    filter = new Intent.FilterComparison(service.cloneFilter());
	                    ServiceRestarter res = new ServiceRestarter();
//	                    BatteryStatsImpl.Uid.Pkg.Serv ss = null;
//	                    BatteryStatsImpl stats = mBatteryStatsService.getActiveStatistics();
//	                    synchronized (stats) {
//	                        ss = stats.getServiceStatsLocked(
//	                                sInfo.applicationInfo.uid, sInfo.packageName,
//	                                sInfo.name);
//	                    }
	                    r = new ServiceRecord(this,  name, filter, sInfo, res);
	                    res.setService(r);
	                    mServices.put(name, r);
	                    mServicesByIntent.put(filter, r);
	                    // Make sure this component isn't in the pending list.
	                    int N = mPendingServices.size();
	                    for (int i=0; i<N; i++) {
	                        ServiceRecord pr = mPendingServices.get(i);
	                        if (pr.name.equals(name)) {
	                            mPendingServices.remove(i);
	                            i--;
	                            N--;
	                        }
	                    }
	                }
	        }
	        if (r != null) {

//	            if (checkComponentPermission(r.permission,
//	                    callingPid, callingUid, r.appInfo.uid, r.exported)
//	                    != PackageManager.PERMISSION_GRANTED) {
//	                if (!r.exported) {
//	                    Slog.w(TAG, "Permission Denial: Accessing service " + r.name
//	                            + " from pid=" + callingPid
//	                            + ", uid=" + callingUid
//	                            + " that is not exported from uid " + r.appInfo.uid);
//	                    return new ServiceLookupResult(null, "not exported from uid "
//	                            + r.appInfo.uid);
//	                }
//	                Slog.w(TAG, "Permission Denial: Accessing service " + r.name
//	                        + " from pid=" + callingPid
//	                        + ", uid=" + callingUid
//	                        + " requires " + r.permission);
//	                return new ServiceLookupResult(null, r.permission);
//	            }
	            return new ServiceLookupResult(r, null);
	        }
	        return null;
	    }

		public void publishService(IBinder token, Intent intent, IBinder service) {
	        // Refuse possible leaked file descriptors
	        if (intent != null && intent.hasFileDescriptors() == true) {
	            throw new IllegalArgumentException("File descriptors passed in Intent");
	        }

	        Log.i(TAG, "publishService:");
	            if (!(token instanceof ServiceRecord)) {
	                throw new IllegalArgumentException("Invalid service token");
	            }
	            ServiceRecord r = (ServiceRecord)token;

//	            final long origId = Binder.clearCallingIdentity();

//	            if (DEBUG_SERVICE) Slog.v(TAG, "PUBLISHING " + r
//	                    + " " + intent + ": " + service);
	            if (r != null) {
	                Intent.FilterComparison filter
	                        = new Intent.FilterComparison(intent);
	                IntentBindRecord b = r.bindings.get(filter);
	                if(mServiceConnection.get(r) == null)
	                	Log.i(TAG, "service connection  = null:");
	                    if (mServiceConnection.get(r) != null) {
	                        Iterator<ArrayList<ConnectionRecord>> it
	                                = mServiceConnection.values().iterator();
	                        while (it.hasNext()) {
	                            ArrayList<ConnectionRecord> clist = it.next();
	                            for (int i=0; i<clist.size(); i++) {
	                                ConnectionRecord c = clist.get(i);
	                                Log.i(TAG, "c.conn:");
	                                mContext.getActivityThread().getApplicationThread().getHandler().post(new RunConnection(r.name,service,0,c.conn));
	                            }
	                        }
	                    }


//	                serviceDoneExecutingLocked(r, mStoppingServices.contains(r));
//
//	                Binder.restoreCallingIdentity(origId);
	            }

	    }

		final class RunConnection implements Runnable{

			 RunConnection(ComponentName name, IBinder service, int command,ServiceConnection connection) {
				 Log.i(TAG, "new RunConnection:");
				 mName = name;
	                mService = service;
	                mCommand = command;
	                mConnection = connection;
	            }

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.i(TAG, "RunConnection.run:");
				 if (mCommand == 0) {
					 ConnectionInfo old;
			         ConnectionInfo info;
			         old = mActiveConnections.get(mName);
			         if (old != null && old.binder == mService) {
			             // Huh, already have this one.  Oh well!
			             return;
			         }

			         if (mService != null) {
			                    // A new service is being connected... set it all up.

			            info = new ConnectionInfo();
			            info.binder = mService;
//			            info.deathMonitor = new IBinder.DeathRecipient();

//			            mService.linkToDeath(info.deathMonitor, 0);
			            mActiveConnections.put(mName, info);
			        } else {
			                    // The named service is being disconnected... clean up.
			            mActiveConnections.remove(mName);
			                }

//			                if (old != null) {
//			                    old.binder.unlinkToDeath(old.deathMonitor, 0);
//			                }
//

			            // If there was an old service, it is not disconnected.
			            if (old != null) {
			                mConnection.onServiceDisconnected(mName);
			            }
			            // If there is a new service, it is now connected.
			            if (mService != null) {
			            	Log.i(TAG, "RunConnection:");
			                mConnection.onServiceConnected(mName, mService);
			            }
	                } else if (mCommand == 1) {
//	                    doDeath(mName, mService);
	                	mConnection.onServiceDisconnected(mName);
	                }
			}

			final ComponentName mName;
            final IBinder mService;
            final int mCommand;
            final ServiceConnection mConnection;
		}


	    private class ServiceRestarter implements Runnable {
	        private ServiceRecord mService;

	        void setService(ServiceRecord service) {
	            mService = service;
	        }

	        public void run() {
//	            synchronized(ActivityManager.this) {
//	                performServiceRestartLocked(mService);
//	            }
	        }
	    }


	public final void startRunning(String pkg, String cls, String action,
			String data) {
		if (mStartRunning) {
			return;
		}
		mStartRunning = true;
		mTopComponent = pkg != null && cls != null ? new ComponentName(pkg, cls)
				: null;
		mTopAction = action != null ? action : Intent.ACTION_MAIN;
		mTopData = data;
		// if (!mSystemReady) {
		// return;
		// }

		systemReady();
	}

	// The flags that are set for all calls we make to the package manager.
	static final int STOCK_PM_FLAGS = PackageManager.GET_SHARED_LIBRARY_FILES;

	boolean startHomeActivityLocked() {
		// System.out.println("in startHomeActivityLocked");

		Intent intent = new Intent(mTopAction,
				mTopData != null ? Uri.parse(mTopData) : null);

		intent.addCategory(Intent.CATEGORY_HOME);
		ActivityInfo aInfo = intent.resolveActivityInfo(mContext.getPackageManager(),
			STOCK_PM_FLAGS);

        if (aInfo == null) {
            /*
             * No Home activity is found. Find a main activity to Launch.
             */
            PackageManager manager = mContext.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
            aInfo = apps.get(0).activityInfo;
        }

		if (aInfo == null) {
			Log.i(TAG, "Cannot find intent " + intent);
		} else {
			intent.setComponent(new ComponentName(
					aInfo.applicationInfo.packageName, aInfo.name));
			// Don't do this if the home app is currently being
			// instrumented.
			ProcessRecord app = getProcessRecordLocked(aInfo.processName);
			if (app == null || app.instrumentationClass == null) {
				intent.setFlags(intent.getFlags()
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				mMainStack.startActivityLocked(null, intent, null, null, 0,
						aInfo, null, null, 0, 0, 0, false, false);
			}
		}

        /**
         * @j2sNative
         * try {
         *  // Removing startup screen
         *  if (document.getElementById('mayloonstartup')) {
         *      document.getElementById('mayloonstartup').style.display = 'none';
         *  }
         * } catch ( e ) {
         *  // Just ignore any exception occurred
         * }
         */ { }

		// System.out.println("startHomeActivityLocked Over");
		return true;
	}

	final HashMap<String, ProcessRecord> mProcessNames = new HashMap<String, ProcessRecord>();

	final ProcessRecord getProcessRecordLocked(String processName) {
		// System.out.println("processName: " + processName);
		ProcessRecord proc = mProcessNames.get(processName);
		return proc;
	}

	public void systemReady() {
		// System.out.println("in systemReady");
		// initialize Content service
		ContentService.main(mContext);
		mMainStack.resumeTopActivityLocked(null);
	}

	// Maximum number of recent tasks that we can remember.
	static final int MAX_RECENT_TASKS = 20;
	private static final boolean DEBUG_BROADCAST = true;
	private static final boolean DEBUG_BROADCAST_LIGHT = false;
	private static final int BROADCAST_SUCCESS = 0;
	final ArrayList<TaskRecord> mRecentTasks = new ArrayList<TaskRecord>();
	public ActivityRecord mFocusedActivity;

	final void addRecentTaskLocked(TaskRecord task) {
		// Remove any existing entries that are the same kind of task.
		int N = mRecentTasks.size();
		for (int i = 0; i < N; i++) {
			TaskRecord tr = mRecentTasks.get(i);
			if ((task.affinity != null && task.affinity.equals(tr.affinity))
					|| (task.intent != null && task.intent
							.filterEquals(tr.intent))) {
				mRecentTasks.remove(i);
				i--;
				N--;
				if (task.intent == null) {
					// If the new recent task we are adding is not fully
					// specified, then replace it with the existing recent task.
					task = tr;
				}
			}
		}
		if (N >= MAX_RECENT_TASKS) {
			mRecentTasks.remove(N - 1);
		}
		mRecentTasks.add(0, task);
	}

	public final IntentResolver<BroadcastFilter, BroadcastFilter> mReceiverResolver = new IntentResolver<BroadcastFilter, BroadcastFilter>() {
		@Override
		protected boolean allowFilterResult(BroadcastFilter filter,
				List<BroadcastFilter> dest) {
			IIntentReceiver target = filter.receiverList.receiver;
			for (int i = dest.size() - 1; i >= 0; i--) {
				if (dest.get(i).receiverList.receiver == target) {
					return false;
				}
			}
			return true;
		}
	};

	final HashMap mRegisteredReceivers = new HashMap();

	public final Intent registerReceiver(IApplicationThread caller,
			IIntentReceiver receiver, IntentFilter filter, String permission) {
		synchronized (this) {
			ProcessRecord callerApp = null;
			if (caller != null) {
				callerApp = getRecordForAppLocked(caller);
			}

			List allSticky = null;

			// Look for any matching sticky broadcasts...
			Iterator actions = filter.actionsIterator();
			if(DEBUG_BROADCAST)
				Log.i(TAG, "this is register in activitymanager");
			if (actions != null) {
				while (actions.hasNext()) {
					String action = (String) actions.next();
					// allSticky = getStickiesLocked(action, filter, allSticky);
					if(DEBUG_BROADCAST)
						Log.i(TAG, "action:"+action);
				}
			} else {
				// allSticky = getStickiesLocked(null, filter, allSticky);
			}

			// The first sticky in the list is returned directly back to
			// the client.
			Intent sticky = allSticky != null ? (Intent) allSticky.get(0)
					: null;

			if (receiver == null) {
				return sticky;
			}

			ReceiverList rl = (ReceiverList) mRegisteredReceivers.get(receiver);
			if (rl == null) {
				rl = new ReceiverList(this, callerApp, 0, 0, receiver);
				mRegisteredReceivers.put(receiver, rl);
			}
			BroadcastFilter bf = new BroadcastFilter(filter, rl, permission);
			rl.add(bf);
			mReceiverResolver.addFilter(bf);

			if (!bf.debugCheck()) {
				Slog.w(TAG, "==> For Dynamic broadast");
			}

			// Enqueue broadcasts for all existing stickies that match
			// this filter.
			if (allSticky != null) {
				/*
				 * ArrayList receivers = new ArrayList(); receivers.add(bf);
				 *
				 * int N = allSticky.size(); for (int i=0; i<N; i++) { Intent
				 * intent = (Intent)allSticky.get(i); BroadcastRecord r = new
				 * BroadcastRecord(intent, null, null, -1, -1, null, receivers,
				 * null, 0, null, null, false, true, true); if
				 * (mParallelBroadcasts.size() == 0) {
				 * scheduleBroadcastsLocked(); } mParallelBroadcasts.add(r); }
				 */
			}

			// return sticky;
		return null;
		}
	}

	public void unregisterReceiver(IIntentReceiver receiver) {
		ReceiverList rl = (ReceiverList) mRegisteredReceivers.get(receiver);
		if(rl!=null){
			removeReceiverLocked(rl);
		}
    }

	void removeReceiverLocked(ReceiverList rl) {
        mRegisteredReceivers.remove(rl.receiver);
        int N = rl.size();
        for (int i=0; i<N; i++) {
            mReceiverResolver.removeFilter(rl.get(i));
        }
    }


	private final void deliverToRegisteredReceiverLocked(BroadcastRecord r,
            BroadcastFilter filter, boolean ordered) {
		Context context=filter.receiverList.receiver.mOuterContext;
		Intent intent=r.intent;
		filter.receiverList.receiver.receiver.onReceive(context, intent);
	}

	private final void processNextBroadcast(Intent intent) {
		// First, deliver any non-serialized broadcasts right away.
		if (mParallelBroadcasts.size() > 0) {
			BroadcastRecord r=mParallelBroadcasts.remove(0);
			r.dispatchTime = SystemClock.uptimeMillis();
			final int N = r.receivers.size();
			for (int i = 0; i < N; i++) {
				Object target = r.receivers.get(i);
				if (DEBUG_BROADCAST)
					Log.i(TAG, "receivers size:" + N);
				deliverToRegisteredReceiverLocked(r, (BroadcastFilter) target,
						false);
			}
		}
		if (mOrderedBroadcasts.size() > 0) {
			BroadcastRecord r=mOrderedBroadcasts.remove(0);
			r.dispatchTime = SystemClock.uptimeMillis();
			final int N = r.receivers.size();
			for (int i = 0; i < N; i++) {
				ResolveInfo target = (ResolveInfo)r.receivers.get(i);
				if (DEBUG_BROADCAST)
					Log.i(TAG, "package receivers size:" + N);

				BroadcastReceiver receiver = null;
		        try {
		            receiver = (BroadcastReceiver)Class.forName(target.activityInfo.name).newInstance();
		        } catch (Exception e) {
		        	Log.i(TAG, "Create broadrecerver error");
		        }
		        receiver.onReceive(Context.mOuterContext, intent);
			}
		}
	}

	private final void scheduleBroadcastsLocked(Intent intent) {
		processNextBroadcast(intent);
	}


	final ArrayList<BroadcastRecord> mParallelBroadcasts
    = new ArrayList<BroadcastRecord>();

	final ArrayList<BroadcastRecord> mOrderedBroadcasts
    = new ArrayList<BroadcastRecord>();

	public final int broadcastIntent(IApplicationThread caller, Intent intent,
			String resolvedType, IIntentReceiver resultTo, int resultCode,
			String resultData, Bundle map, String requiredPermission,
			boolean ordered, boolean sticky) {

		final ProcessRecord callerApp = getRecordForAppLocked(caller);

		if (DEBUG_BROADCAST_LIGHT)
			Slog.v(TAG, (sticky ? "Broadcast sticky: " : "Broadcast: ")
					+ intent + " ordered=" + ordered);
		if ((resultTo != null) && !ordered) {
			Slog.w(TAG, "Broadcast " + intent
					+ " not ordered but result callback requested!");
		}

		// Add to the sticky list if requested.

		// Figure out who all will receive this broadcast.
		List receivers = null;
		List<BroadcastFilter> registeredReceivers = null;

		registeredReceivers = mReceiverResolver.queryIntent(intent,
				resolvedType, false);

		final boolean replacePending = (intent.getFlags() & Intent.FLAG_RECEIVER_REPLACE_PENDING) != 0;


		Log.i(TAG, "ComponentNamename:"+intent.getComponent());
		if (intent.getComponent() != null) {
			// Broadcast is going to one specific receiver class...
			ActivityInfo ai = Context.getSystemContext().getPackageManager()
					.getReceiverInfo(intent.getComponent(), STOCK_PM_FLAGS);
			if (ai != null) {
				receivers = new ArrayList();
				ResolveInfo ri = new ResolveInfo();
				ri.activityInfo = ai;
				receivers.add(ri);
			}
		} else {
			// Need to resolve the intent to interested receivers...
			Log.i(TAG, "if:"+String.valueOf(intent.getFlags() & Intent.FLAG_RECEIVER_REGISTERED_ONLY));
			if ((intent.getFlags() & Intent.FLAG_RECEIVER_REGISTERED_ONLY) == 0) {
				receivers = Context
						.getSystemContext()
						.getPackageManager()
						.queryIntentReceivers(intent, resolvedType,
								STOCK_PM_FLAGS);
			}
		}

		Log.i(TAG, "package receiver size is:"+String.valueOf(receivers.size()));
		int NR = registeredReceivers != null ? registeredReceivers.size() : 0;
		if (!ordered && NR > 0) {
			// If we are not serializing this broadcast, then send the
			// registered receivers separately so they don't wait for the
			// components to be launched.
			BroadcastRecord r = new BroadcastRecord(intent, callerApp,
					null, 0, 0, requiredPermission,
					registeredReceivers, resultCode, resultData, map, ordered,
					sticky, false);

			mParallelBroadcasts.add(r);
			registeredReceivers = null;
			NR = 0;
		}

		BroadcastRecord s = new BroadcastRecord(intent, callerApp,
				null, 0, 0, requiredPermission,
				receivers, resultCode, resultData, map, ordered,
				sticky, false);
		mOrderedBroadcasts.add(s);
		scheduleBroadcastsLocked(intent);

		return BROADCAST_SUCCESS;
	}

	final ProcessRecord startProcessLocked(String processName,
			ApplicationInfo info, boolean knownToBeDead, int intentFlags,
			String hostingType, ComponentName hostingName,
			boolean allowWhileBooting) {
		// System.out.println(TAG + "in package startProcessLocked");
		// System.out.println(TAG + "processName: " + processName + ", info: "
		// + info.name);
		/**/
		ProcessRecord app = getProcessRecordLocked(processName);
		// We don't have to do anything more if:
		// (1) There is an existing application record; and
		// (2) The caller doesn't think it is dead, OR there is no thread
		// object attached to it so we know it couldn't have crashed; and
		// (3) There is a pid assigned to it, so it is either starting or
		// already running.
		// if (DEBUG_PROCESSES)
		// Slog.v(TAG, "startProcess: name=" + processName + " app=" + app
		// + " knownToBeDead=" + knownToBeDead + " thread="
		// + (app != null ? app.thread : null) + " pid="
		// + (app != null ? app.pid : -1));
		if (app != null && app.pid > 0) {
			Log.i(TAG, "impossible here");
			if (!knownToBeDead || app.thread == null) {
				// We already have the app running, or are waiting for it to
				// come up (we have a pid but not yet its thread), so keep it.
				// if (DEBUG_PROCESSES)
				// Slog.v(TAG, "App already running: " + app);
				return app;
			} else {
				// An application record is attached to a previous process,
				// clean it up now.
				// if (DEBUG_PROCESSES)
				// Slog.v(TAG, "App died: " + app);
				// handleAppDiedLocked(app, true);
			}
		}

		String hostingNameStr = hostingName != null ? hostingName
				.flattenToShortString() : null;
		// System.out.println(TAG + "hostingNameStr: " + hostingNameStr);

		if ((intentFlags & Intent.FLAG_FROM_BACKGROUND) != 0) {
			Log.i(TAG, "impossible here");
			// If we are in the background, then check to see if this process
			// is bad. If so, we will just silently fail.
			// if (mBadProcesses.get(info.processName, info.uid) != null) {
			// if (DEBUG_PROCESSES)
			// Slog.v(TAG, "Bad process: " + info.uid + "/"
			// + info.processName);
			// return null;
			// }
		} else {
			// When the user is explicitly starting a process, then clear its
			// crash count so that we won't make it bad until they see at
			// least one crash dialog again, and make the process good again
			// if it had been bad.
			// if (DEBUG_PROCESSES)
			// Slog.v(TAG, "Clearing bad process: " + info.uid + "/"
			// + info.processName);
			// mProcessCrashTimes.remove(info.processName, info.uid);
			// if (mBadProcesses.get(info.processName, info.uid) != null) {
			// EventLog.writeEvent(EventLogTags.AM_PROC_GOOD, info.uid,
			// info.processName);
			// mBadProcesses.remove(info.processName, info.uid);
			// if (app != null) {
			// app.bad = false;
			// }
			// }
		}

		if (app == null) {
			app = newProcessRecordLocked(null, info, processName);
			// app.processName = processName = app.info.packageName;
			// System.out.println("processName: " + processName + ", app.PN: "
			// + app.processName);
			mProcessNames.put(processName, app);
//			System.out.print("processName: " + processName);
		} else {
			// If this is a new package in the process, add the package to the
			// list
			app.addPackage(info.packageName);
		}

		/**
		 * // If the system is not ready yet, then hold off on starting this //
		 * process until it is. if (!mProcessesReady &&
		 * !isAllowedWhileBooting(info) && !allowWhileBooting) { if
		 * (!mProcessesOnHold.contains(app)) { mProcessesOnHold.add(app); } if
		 * (DEBUG_PROCESSES) Slog.v(TAG, "System not ready, putting on hold: " +
		 * app); return app; }
		 *
		 * /
		 **/
		startProcessLocked(app, hostingType, hostingNameStr);
		return (app.pid != 0) ? app : null;
	}

	final ProcessRecord newProcessRecordLocked(IApplicationThread thread,
			ApplicationInfo info, String customProcess) {
		String proc = customProcess != null ? customProcess : info.processName;
		// System.out.println(TAG + "proc: " + proc);
		return new ProcessRecord(thread, info, proc);
	}

	private final void startProcessLocked(ProcessRecord app,
			String hostingType, String hostingNameStr) {
		// System.out.println(TAG + "in private startProcessLocked");
		// int pid = Process.start("android.app.ActivityThread",
		// mSimpleProcessManagement ? app.processName : null, uid, uid,
		// gids, debugFlags, new String[] { targetPackage });
		try {
			ActivityThread acT = (ActivityThread) Class.forName(
					"android.app.ActivityThread").newInstance();
			// if (app.processName == null)
			// app.processName = app.info.packageName;
			acT.main(app.processName);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public final void attachApplication(IApplicationThread thread,
			String processName) {
		// System.out.println(TAG + "in attachApplication, starting "
		// + processName);

		// Find the application record that is being attached... either via
		// the pid if we are running in multiple processes, or just pull the
		// next app record if we are emulating process with anonymous threads.
		ProcessRecord app = null;
		app = mProcessNames.get(processName);
		if (app == null) {
			Log.e(TAG, "process get fail");
			return;
		}

		// System.out.println(TAG + "get process: " + app.processName);
		app.thread = thread;
		app.curAdj = app.setAdj = -100;
		app.forcingToForeground = null;
		app.foregroundServices = false;
		app.debugging = false;

		boolean normalMode = true;

		thread.bindApplication(processName,
				app.instrumentationInfo != null ? app.instrumentationInfo
						: app.info, app.instrumentationClass,
				app.instrumentationProfileFile, app.instrumentationArguments,
				IApplicationThread.DEBUG_OFF, false || !normalMode);
		// System.out.println("after bindApplication");

		// See if the top visible activity is waiting to run in this process...
		ActivityRecord hr = mMainStack.topRunningActivityLocked(null);
		if (hr != null && normalMode) {
			// System.out.println("processName: " + processName
			// + ", hr.processName: " + hr.processName);
			if (hr.app == null && processName.equals(hr.processName)) {
				try {
					mMainStack.realStartActivityLocked(hr, app, true, true);
				} catch (Exception e) {
					Log.i(TAG, e.getMessage());
					Log.i(TAG, "Exception in new application when starting activity "
									+ hr.intent.getComponent()
											.flattenToShortString());
				}
			} else {
				Log.i(TAG, "impossible here lalala");
				// mMainStack.ensureActivitiesVisibleLocked(hr, null,
				// processName,
				// 0);
			}
		}
	}

	public final boolean finishActivity(IBinder token, int resultCode,
			Intent resultData) {
		// System.out.println(TAG + "in finishActivity");
		boolean res = mMainStack.requestFinishActivityLocked(token, resultCode,
				resultData, "app-request");
		// System.out.println(TAG + "finishActivity Over! res: " + res);
		//mCurActivity.setVisible(true);
		return res;
	}

	final ProcessRecord getRecordForAppLocked(IApplicationThread thread) {
		if (thread == null) {
			return null;
		}

		Iterator<Entry<String, ProcessRecord>> entrySetIterator = mProcessNames
				.entrySet().iterator();
		while (entrySetIterator.hasNext()) {
			Entry<String, ProcessRecord> entry = entrySetIterator.next();
			// System.out.println("Entry, Key: " + entry.getKey());
			if (entry.getValue().thread == thread) {
				return entry.getValue();
			}
		}
		return null;
	}

	final void setFocusedActivityLocked(ActivityRecord r) {
		// TODO
		if (mFocusedActivity != r) {
			mFocusedActivity = r;
			// mWindowManager.setFocusedApp(r, true);
		}
	}

	public final void activityPaused(IBinder token, Bundle icicle) {
		mMainStack.activityPaused(token, icicle, false);
	}

	public final void activityDestroyed(IBinder token) {
		mMainStack.activityDestroyed(token);
	}

    public final void activityIdle(IBinder token, Configuration config) {
        final long origId = Binder.clearCallingIdentity();
        mMainStack.activityIdleInternal(token, false, config);
        Binder.restoreCallingIdentity(origId);
    }

    public final void activityStopped(IBinder token) {
        ActivityRecord r = null;
        int index = mMainStack.indexOfTokenLocked(token);
        if (index >= 0) {
            r = (ActivityRecord)mMainStack.mHistory.get(index);
            r.stopped = true;
            r.state = ActivityState.STOPPED;
            if (!r.finishing) {
                if (r.configDestroy) {
                    r.stack.destroyActivityLocked(r, true);
                    r.stack.resumeTopActivityLocked(null);
                }
            }
        }
//        trimApplications();
    }

	/**
	 * All currently running services.
	 */
	final HashMap<ComponentName, ServiceRecord> mServices = new HashMap<ComponentName, ServiceRecord>();

	/**
	 * All currently running services indexed by the Intent used to start them.
	 */
	final HashMap<Intent.FilterComparison, ServiceRecord> mServicesByIntent = new HashMap<Intent.FilterComparison, ServiceRecord>();

	/**
	 * List of services that we have been asked to start, but haven't yet been
	 * able to. It is used to hold start requests while waiting for their
	 * corresponding application thread to get going.
	 */
	final ArrayList<ServiceRecord> mPendingServices = new ArrayList<ServiceRecord>();

	/**
	 * List of services that are scheduled to restart following a crash.
	 */
	final ArrayList<ServiceRecord> mRestartingServices = new ArrayList<ServiceRecord>();

	/**
	 * List of services that are in the process of being stopped.
	 */
	final ArrayList<ServiceRecord> mStoppingServices = new ArrayList<ServiceRecord>();

	private final ServiceRecord findServiceLocked(ComponentName name,
			IBinder token) {
		ServiceRecord r = mServices.get(name);
		return r == token ? r : null;
	}

	public boolean stopServiceToken(ComponentName className, IBinder token,
			int startId) {
		synchronized (this) {
			if (DEBUG_SERVICE)
				Log.i(TAG, "stopServiceToken: " + className + " "
						+ token + " startId=" + startId);
			ServiceRecord r = findServiceLocked(className, token);
			if (r != null) {
				if (startId >= 0) {
					// Asked to only stop if done with all work. Note that
					// to avoid leaks, we will take this as dropping all
					// start items up to and including this one.
					ServiceRecord.StartItem si = r.findDeliveredStart(startId,
							false);
					if (si != null) {
						while (r.deliveredStarts.size() > 0) {
							ServiceRecord.StartItem cur = r.deliveredStarts
									.remove(0);
							// cur.removeUriPermissionsLocked();
							if (cur == si) {
								break;
							}
						}
					}

					if (r.lastStartId != startId) {
						return false;
					}

					if (r.deliveredStarts.size() > 0) {
						Log.i(TAG, "stopServiceToken startId "
								+ startId + " is last, but have "
								+ r.deliveredStarts.size() + " remaining args");
					}
				}

				{
					r.startRequested = false;
					r.callStart = false;
				}
				// final long origId = Binder.clearCallingIdentity();
				bringDownServiceLocked(r, false);
				// Binder.restoreCallingIdentity(origId);
				return true;
			}
		}
		return false;
	}

	public void serviceDoneExecutingLocked(ServiceRecord r, boolean inStopping) {
		if (DEBUG_SERVICE)
			Log.i(TAG, "<<< DONE EXECUTING " + r + ": nesting="
					+ r.executeNesting + ", inStopping=" + inStopping
					+ ", app=" + r.app);
		else if (DEBUG_SERVICE)
			System.out.println(TAG + "<<< DONE EXECUTING " + r.shortName);
		r.executeNesting--;
		if (r.executeNesting <= 0 && r.app != null) {
			if (DEBUG_SERVICE)
				Log.i(TAG, "Nesting at 0 of " + r.shortName);
			r.app.executingServices.remove(r);
			if (r.app.executingServices.size() == 0) {
				if (DEBUG_SERVICE)
					Log.i(TAG, "No more executingServices of "
							+ r.shortName);
				/**
				 * FIXME: implement message handler
				 */
				// mHandler.removeMessages(SERVICE_TIMEOUT_MSG, r.app);
			}
			if (inStopping) {
				if (DEBUG_SERVICE)
					Log.i(TAG, "doneExecuting remove stopping "
							+ r);
				mStoppingServices.remove(r);
			}
		}
	}

	private final boolean unscheduleServiceRestartLocked(ServiceRecord r) {
		if (r.restartDelay == 0) {
			return false;
		}
		r.resetRestartCounter();
		mRestartingServices.remove(r);
		/**
		 * FIXME: implement message handler
		 */
		// mHandler.removeCallbacks(r.restarter);
		return true;
	}

	private final void bringDownServiceLocked(ServiceRecord r, boolean force) {
		// Slog.i(TAG, "Bring down service:");
		// r.dump("  ");

		// Does it still need to run?
		if (!force && r.startRequested) {
			return;
		}
		if (r.connections.size() > 0) {
			if (!force) {
				// XXX should probably keep a count of the number of auto-create
				// connections directly in the service.
				Iterator<ArrayList<ConnectionRecord>> it = r.connections
						.values().iterator();
				while (it.hasNext()) {
					ArrayList<ConnectionRecord> cr = it.next();
					for (int i = 0; i < cr.size(); i++) {
						if ((cr.get(i).flags & Context.BIND_AUTO_CREATE) != 0) {
							return;
						}
					}
				}
			}

			// Report to all of the connections that the service is no longer
			// available.
			Iterator<ArrayList<ConnectionRecord>> it = r.connections.values()
					.iterator();
			while (it.hasNext()) {
				ArrayList<ConnectionRecord> c = it.next();
				for (int i = 0; i < c.size(); i++) {
					try {
						/**
						 * FIXME: What to do with service connection?
						 */
						// c.get(i).conn.connected(r.name, null);
					} catch (Exception e) {
						Log.i(TAG, "Failure disconnecting service " + r.name
								+ " to connection " + " (in "
								+ c.get(i).binding.client.processName + ")");
					}
				}
			}
		}

		// Tell the service that it has been unbound.
		if (r.bindings.size() > 0 && r.app != null && r.app.thread != null) {
			Iterator<IntentBindRecord> it = r.bindings.values().iterator();
			while (it.hasNext()) {
				IntentBindRecord ibr = it.next();
				if (DEBUG_SERVICE)
					Log.i(TAG, "Bringing down binding " + ibr
							+ ": hasBound=" + ibr.hasBound);
				if (r.app != null && r.app.thread != null && ibr.hasBound) {
					try {
						bumpServiceExecutingLocked(r, "bring down unbind");
						ibr.hasBound = false;
						/**
						 * FIXME: How do we unbind service?
						 */
						// r.app.thread.scheduleUnbindService(r,
						// ibr.intent.getIntent());
					} catch (Exception e) {
						Log.i(TAG, "Exception when unbinding service "
								+ r.shortName);
						serviceDoneExecutingLocked(r, true);
					}
				}
			}
		}

		if (DEBUG_SERVICE)
			Log.i(TAG, "Bringing down " + r + " " + r.intent);
		/*
		 * EventLog.writeEvent(EventLogTags.AM_DESTROY_SERVICE,
		 * System.identityHashCode(r), r.shortName, (r.app != null) ? r.app.pid
		 * : -1);
		 */
		mServices.remove(r.name);
		mServicesByIntent.remove(r.intent);
		r.totalRestartCount = 0;
		unscheduleServiceRestartLocked(r);

		// Also make sure it is not on the pending list.
		int N = mPendingServices.size();
		for (int i = 0; i < N; i++) {
			if (mPendingServices.get(i) == r) {
				mPendingServices.remove(i);
				if (DEBUG_SERVICE)
					Log.i(TAG, "Removed pending: " + r);
				i--;
				N--;
			}
		}

		r.cancelNotification();
		r.isForeground = false;
		r.foregroundId = 0;

		// Clear start entries.
		r.clearDeliveredStartsLocked();
		r.pendingStarts.clear();

		if (r.app != null) {
			r.app.services.remove(r);
			if (r.app.thread != null) {
				try {
					bumpServiceExecutingLocked(r, "stop");
					mStoppingServices.add(r);
					/**
					 * FIXME: How do we implement stopService?
					 */
					// r.app.thread.scheduleStopService(r);
				} catch (Exception e) {
					Log.i(TAG, "Exception when stopping service "
							+ r.shortName);
					serviceDoneExecutingLocked(r, true);
				}
				// updateServiceForegroundLocked(r.app, false);
			} else {
				if (DEBUG_SERVICE)
					Log.i(TAG, "Removed service that has no process: " + r);
			}
		} else {
			if (DEBUG_SERVICE)
				Log.i(TAG, "Removed service that is not running: " + r);
		}
	}

	static final int SHOW_ERROR_MSG = 1;
	static final int SHOW_NOT_RESPONDING_MSG = 2;
	static final int SHOW_FACTORY_ERROR_MSG = 3;
	static final int UPDATE_CONFIGURATION_MSG = 4;
	static final int GC_BACKGROUND_PROCESSES_MSG = 5;
	static final int WAIT_FOR_DEBUGGER_MSG = 6;
	static final int BROADCAST_INTENT_MSG = 7;
	static final int BROADCAST_TIMEOUT_MSG = 8;
	static final int SERVICE_TIMEOUT_MSG = 12;
	static final int UPDATE_TIME_ZONE = 13;
	static final int SHOW_UID_ERROR_MSG = 14;
	static final int IM_FEELING_LUCKY_MSG = 15;
	static final int PROC_START_TIMEOUT_MSG = 20;
	static final int DO_PENDING_ACTIVITY_LAUNCHES_MSG = 21;
	static final int KILL_APPLICATION_MSG = 22;
	static final int FINALIZE_PENDING_INTENT_MSG = 23;
	static final int POST_HEAVY_NOTIFICATION_MSG = 24;
	static final int CANCEL_HEAVY_NOTIFICATION_MSG = 25;
	static final int SHOW_STRICT_MODE_VIOLATION_MSG = 26;
	static final int CHECK_EXCESSIVE_WAKE_LOCKS_MSG = 27;

	/**
	 * final Handler mHandler = new Handler() { //public Handler() { // if
	 * (localLOGV) Slog.v(TAG, "Handler started!"); //}
	 *
	 * public void handleMessage(Message msg) { switch (msg.what) { case
	 * SHOW_ERROR_MSG: {
	 *
	 * } break; case SHOW_NOT_RESPONDING_MSG: { synchronized
	 * (ActivityManager.this) {
	 *
	 * }
	 *
	 * } break; case SHOW_STRICT_MODE_VIOLATION_MSG: {
	 *
	 * } break; case SHOW_FACTORY_ERROR_MSG: {
	 *
	 * } break; case UPDATE_CONFIGURATION_MSG: {
	 *
	 * } break; case GC_BACKGROUND_PROCESSES_MSG: {
	 *
	 * } break; case WAIT_FOR_DEBUGGER_MSG: {
	 *
	 * } break; case BROADCAST_INTENT_MSG: {
	 *
	 * } break; case BROADCAST_TIMEOUT_MSG: {
	 *
	 * } break; case SERVICE_TIMEOUT_MSG: {
	 *
	 * } break; case UPDATE_TIME_ZONE: {
	 *
	 * } break; case SHOW_UID_ERROR_MSG: {
	 *
	 * } break; case IM_FEELING_LUCKY_MSG: {
	 *
	 * } break; case PROC_START_TIMEOUT_MSG: {
	 *
	 * } break; case DO_PENDING_ACTIVITY_LAUNCHES_MSG: {
	 *
	 * } break; case KILL_APPLICATION_MSG: {
	 *
	 * } break; case FINALIZE_PENDING_INTENT_MSG: {
	 *
	 * } break; case POST_HEAVY_NOTIFICATION_MSG: {
	 *
	 * } break; case CANCEL_HEAVY_NOTIFICATION_MSG: {
	 *
	 * } break; case CHECK_EXCESSIVE_WAKE_LOCKS_MSG: {
	 *
	 * } break; } } };
	 */
	private final void bumpServiceExecutingLocked(ServiceRecord r, String why) {
		if (DEBUG_SERVICE)
			Log.i(TAG, ">>> EXECUTING " + why + " of " + r
					+ " in app " + r.app);
		else if (DEBUG_SERVICE)
			Log.i(TAG, ">>> EXECUTING " + why + " of "
					+ r.shortName);
		long now = SystemClock.uptimeMillis();
		if (r.executeNesting == 0 && r.app != null) {
			if (r.app.executingServices.size() == 0) {
				/**
				 * FIXME: implement message handler
				 */
				// Message msg = mHandler.obtainMessage(SERVICE_TIMEOUT_MSG);
				// msg.obj = r.app;
				// mHandler.sendMessageAtTime(msg, now+SERVICE_TIMEOUT);
			}
			r.app.executingServices.add(r);
		}
		r.executeNesting++;
		r.executingStart = now;
	}

	public void setServiceForeground(ComponentName className, IBinder token,
			int id, boolean removeNotification) {
		// final long origId = Binder.clearCallingIdentity();
		try {
			synchronized (this) {
				ServiceRecord r = findServiceLocked(className, token);
				if (r != null) {
					if (id != 0) {

						if (r.foregroundId != id) {
							r.cancelNotification();
							r.foregroundId = id;
						}

						r.isForeground = true;
						r.postNotification();

					} else {
						if (r.isForeground) {
							r.isForeground = false;

						}
						if (removeNotification) {
							r.cancelNotification();
							r.foregroundId = 0;

						}
					}
				}
			}
		} finally {
			// Binder.restoreCallingIdentity(origId);
		}
	}

	// Content Provider

	/**
	 * All of the currently running global content providers. Keys are a string
	 * containing the provider name and values are a ContentProviderRecord
	 * object containing the data about it. Note that a single provider may be
	 * published under multiple names, so there may be multiple entries here for
	 * a single one in mProvidersByClass.
	 */
	final HashMap<String, ContentProviderRecord> mProvidersByName = new HashMap<String, ContentProviderRecord>();
	/**
	 * All of the currently running global content providers. Keys are a string
	 * containing the provider's implementation class and values are a
	 * ContentProviderRecord object containing the data about it.
	 */
	final HashMap<String, ContentProviderRecord> mProvidersByClass = new HashMap<String, ContentProviderRecord>();

	/** Information you can retrieve about a particular application. */
	public static class ContentProviderHolder implements Parcelable {
		public final ProviderInfo info;
		public ContentProvider provider;
		public boolean noReleaseNeeded;

		public ContentProviderHolder(ProviderInfo _info) {
			info = _info;
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			info.writeToParcel(dest, 0);
			if (provider != null) {
				dest.writeStrongBinder(/*provider.asBinder()*/null);
			} else {
				dest.writeStrongBinder(null);
			}
			dest.writeInt(noReleaseNeeded ? 1 : 0);
		}

		public static final Creator<ContentProviderHolder> CREATOR = new Creator<ContentProviderHolder>() {
			public ContentProviderHolder createFromParcel(Parcel source) {
				return new ContentProviderHolder(source);
			}

			public ContentProviderHolder[] newArray(int size) {
				return new ContentProviderHolder[size];
			}
		};

		private ContentProviderHolder(Parcel source) {
			info = null;
			// info = ProviderInfo.CREATOR.createFromParcel(source);
			// provider = ContentProviderNative.asInterface(
			// source.readStrongBinder());
			// noReleaseNeeded = source.readInt() != 0;
		}
	};

	public ContentProviderHolder getContentProvider(IApplicationThread caller,
			String name) {
		if (caller == null) {
			String msg = "null IApplicationThread when getting content provider "
					+ name;
			Log.w(TAG, msg);
		}
		return getContentProviderImpl(caller, name);
	}
    private ContentProviderHolder getContentProviderExternal(String name) {
        return getContentProviderImpl(null, name);
    }
	   public String getProviderMimeType(Uri uri) {
	        final String name = uri.getAuthority();
	        ContentProviderHolder holder = null;

	        try {
	            holder = getContentProviderExternal(name);
	            if(DEBUG_PROVIDER)Log.i(TAG, "getProviderMimeType:"+holder);
	            if (holder != null) {
	                return holder.provider.getType(uri);
	            }
	        } catch (Exception e) {
	            Log.w(TAG, "Content provider dead retrieving " + uri, e);
	            return null;
	        } finally {
	            if (holder != null) {
	                removeContentProviderExternal(name);
	            }
	        }

	        return null;
	    }
	    private void removeContentProviderExternal(String name) {
	            ContentProviderRecord cpr = mProvidersByName.get(name);
	            if(cpr == null) {
	                //remove from mProvidersByClass
	                if(DEBUG_PROVIDER) Slog.v(TAG, name+" content provider not found in providers list");
	                return;
	            }

	            //update content provider record entry info
	            ContentProviderRecord localCpr = mProvidersByClass.get(cpr.info.name);
	            localCpr.externals--;
	            if (localCpr.externals < 0) {
	                Slog.e(TAG, "Externals < 0 for content provider " + localCpr);
	            }

	    }
	private final ContentProviderHolder getContentProviderImpl(
			IApplicationThread caller, String name) {
		ContentProviderRecord cpr;
		ProviderInfo cpi = null;
		if (DEBUG_PROVIDER)
			Log.i(TAG, "ActivityManager:getContentProviderImpl");
		ProcessRecord r = null;
		if (caller != null) {
			// TODO AND FIXME: I don't know when to add the process record
			r = getRecordForAppLocked(caller);
			if (r == null) {
				throw new SecurityException("Unable to find app for caller "
						+ caller + ") when getting content provider " + name);
			}
		}
		if (DEBUG_PROVIDER)
			Log.i(TAG, "ActivityManager:getContentProviderImpl");
		// First check if this content provider has been published...
		cpr = mProvidersByName.get(name);
		if (cpr != null) {
			cpi = cpr.info;
			if (r != null && cpr.canRunHere(r)) {
				// This provider has been published or is in the process
				// of being published... but it is also allowed to run
				// in the caller's process, so don't make a connection
				// and just let the caller instantiate its own instance.
				if (cpr.provider != null) {
					// don't give caller the provider object, it needs
					// to make its own.
					cpr = new ContentProviderRecord(cpr);
				}
				return cpr;
			}
			// // In this case the provider instance already exists, so we can
			// // return it right away.
			// if (r != null) {
			// if (DEBUG) Log.v(TAG,
			// "Adding provider requested by "
			// + r.processName + " from process "
			// + cpr.info.processName);
			// Integer cnt = r.conProviders.get(cpr);
			// if (cnt == null) {
			// r.conProviders.put(cpr, new Integer(1));
			// } else {
			// r.conProviders.put(cpr, new Integer(cnt.intValue()+1));
			// }
			// cpr.clients.add(r);
			// if (cpr.app != null && r.setAdj <= PERCEPTIBLE_APP_ADJ) {
			// // If this is a perceptible app accessing the provider,
			// // make sure to count it as being accessed and thus
			// // back up on the LRU list. This is good because
			// // content providers are often expensive to start.
			// updateLruProcessLocked(cpr.app, false, true);
			// }
			// } else {
			// cpr.externals++;
			// }
			//
			// if (cpr.app != null) {
			// //updateOomAdjLocked(cpr.app);
			// }
			//
			// //Binder.restoreCallingIdentity(origId);

		} else {
			if (DEBUG_PROVIDER)
				Log.i(TAG, "ActivityManager:getContentProviderImpl:AppGlobals.getPackageManager().resolveContentProvider");
			cpi = Context
					.getSystemContext()
					.getPackageManager()
					.resolveContentProvider(
							name,
							STOCK_PM_FLAGS
									| PackageManager.GET_URI_PERMISSION_PATTERNS);
			if (cpi == null) {
				Log.d(TAG, "cpi(ProviderInfo is null.)");
				return null;
			}
			if (DEBUG_PROVIDER)
				Log.i(TAG, "ActivityManager:ProviderInfo is got!");
			cpr = mProvidersByClass.get(cpi.name);
			final boolean firstClass = cpr == null;
			if (firstClass) {
				try {
					ApplicationInfo ai = Context
							.getSystemContext()
							.getPackageManager()
							.getApplicationInfo(
									cpi.applicationInfo.packageName,
									STOCK_PM_FLAGS);
					if (ai == null) {
						Log.w(TAG, "No package info for content provider "
								+ cpi.name);
						return null;
					}
					cpr = new ContentProviderRecord(cpi, ai);
	                // Make sure the provider is published (the same provider class
	                // may be published under multiple names).
	                mProvidersByClass.put(cpi.name, cpr);
	                mProvidersByName.put(name, cpr);
				} catch (Exception ex) {

				}
			}

			if (/*r != null &&*/ cpr.canRunHere(r)) {
				// If this is a multiprocess provider, then just return its
				// info and allow the caller to instantiate it. Only do
				// this if the provider is the same user as the caller's
				// process, or can run as root (so can be in any process).
				return cpr;
			}
			// FIXME: i ignore the case that the content provider should run
			// on the caller's single process

			// This is single process, and our app is now connecting to it.
			// See if we are already in the process of launching this
			// provider.
			// final int N = mLaunchingProviders.size();
			// int i;
			// for (i=0; i<N; i++) {
			// if (mLaunchingProviders.get(i) == cpr) {
			// break;
			// }
			// }

			// If the provider is not already being launched, then get it
			// started.
			// if (i >= N) {
			// final long origId = Binder.clearCallingIdentity();
			// ProcessRecord proc = startProcessLocked(cpi.processName,
			// cpr.appInfo, false, 0, "content provider",
			// new ComponentName(cpi.applicationInfo.packageName,
			// cpi.name), false);
			// if (proc == null) {
			// Slog.w(TAG, "Unable to launch app "
			// + cpi.applicationInfo.packageName + "/"
			// + cpi.applicationInfo.uid + " for provider "
			// + name + ": process is bad");
			// return null;
			// }
			// cpr.launchingApp = proc;
			// mLaunchingProviders.add(cpr);
			// Binder.restoreCallingIdentity(origId);
			// }

			// Make sure the provider is published (the same provider class
			// may be published under multiple names).
			// if (firstClass) {
			// mProvidersByClass.put(cpi.name, cpr);
			// }
			// mProvidersByName.put(name, cpr);
			//
			// if (r != null) {
			// if (DEBUG) Log.v(TAG,
			// "Adding provider requested by "
			// + r.processName + " from process "
			// + cpr.info.processName);
			// Integer cnt = r.conProviders.get(cpr);
			// if (cnt == null) {
			// r.conProviders.put(cpr, new Integer(1));
			// } else {
			// r.conProviders.put(cpr, new Integer(cnt.intValue()+1));
			// }
			// cpr.clients.add(r);
			// } else {
			// cpr.externals++;
			// }
		}

		// Wait for the provider to be published...

		while (cpr.provider == null) {
			if (cpr.launchingApp == null) {
				Log.w(TAG, "Unable to launch app "
						+ cpi.applicationInfo.packageName + "/"
						+ cpi.applicationInfo.uid + " for provider " + name
						+ ": launching app became null");
				return null;
			}
			try {
				cpr.wait();
			} catch (InterruptedException ex) {
			}
		}

		return cpr;
	}

	public void unbindService(ServiceConnection conn) {
		// TODO Auto-generated method stub
	}

    /**
     * Get the device configuration attributes.
     */
    public ConfigurationInfo getDeviceConfigurationInfo() {
//        try {
//            return ActivityManagerNative.getDefault().getDeviceConfigurationInfo();
//        } catch (RemoteException e) {
//        }
        ConfigurationInfo info = new ConfigurationInfo();
        info.reqGlEsVersion = 0x00020000; // We support OpenGLES 2.0
        return info;
    }

    /**
     * Information you can retrieve about any processes that are in an error condition.
     */
    public static class ProcessErrorStateInfo implements Parcelable {
        /**
         * Condition codes
         */
        public static final int NO_ERROR = 0;
        public static final int CRASHED = 1;
        public static final int NOT_RESPONDING = 2;

        /**
         * The condition that the process is in.
         */
        public int condition;

        /**
         * The process name in which the crash or error occurred.
         */
        public String processName;

        /**
         * The pid of this process; 0 if none
         */
        public int pid;

        /**
         * The kernel user-ID that has been assigned to this process;
         * currently this is not a unique ID (multiple applications can have
         * the same uid).
         */
        public int uid;

        /**
         * The activity name associated with the error, if known.  May be null.
         */
        public String tag;

        /**
         * A short message describing the error condition.
         */
        public String shortMsg;

        /**
         * A long message describing the error condition.
         */
        public String longMsg;

        /**
         * The stack trace where the error originated.  May be null.
         */
        public String stackTrace;

        /**
         * to be deprecated: This value will always be null.
         */
        public byte[] crashData = null;

        public ProcessErrorStateInfo() {
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(condition);
            dest.writeString(processName);
            dest.writeInt(pid);
            dest.writeInt(uid);
            dest.writeString(tag);
            dest.writeString(shortMsg);
            dest.writeString(longMsg);
            dest.writeString(stackTrace);
        }

        public void readFromParcel(Parcel source) {
            condition = source.readInt();
            processName = source.readString();
            pid = source.readInt();
            uid = source.readInt();
            tag = source.readString();
            shortMsg = source.readString();
            longMsg = source.readString();
            stackTrace = source.readString();
        }

        public static final Creator<ProcessErrorStateInfo> CREATOR =
                new Creator<ProcessErrorStateInfo>() {
            public ProcessErrorStateInfo createFromParcel(Parcel source) {
                return new ProcessErrorStateInfo(source);
            }
            public ProcessErrorStateInfo[] newArray(int size) {
                return new ProcessErrorStateInfo[size];
            }
        };

        private ProcessErrorStateInfo(Parcel source) {
            readFromParcel(source);
        }
    }

    /**
     * Returns a list of any processes that are currently in an error condition.  The result
     * will be null if all processes are running properly at this time.
     *
     * @return Returns a list of ProcessErrorStateInfo records, or null if there are no
     * current error conditions (it will not return an empty list).  This list ordering is not
     * specified.
     */
    public List<ProcessErrorStateInfo> getProcessesInErrorState() {
    	/*
        try {
            return ActivityManagerNative.getDefault().getProcessesInErrorState();
        } catch (RemoteException e) {
            return null;
        }
        */
    	return null;
    }

    void finishInstrumentationLocked(ProcessRecord app, int resultCode, Bundle results) {
        /*
    	if (app.instrumentationWatcher != null) {
            try {
                // NOTE:  IInstrumentationWatcher *must* be oneway here
                app.instrumentationWatcher.instrumentationFinished(
                    app.instrumentationClass,
                    resultCode,
                    results);
            } catch (RemoteException e) {
            }
        }
        */
        //app.instrumentationWatcher = null;
        /**
         * @Mayloon update
         * Implement finishInstrumentationLocked here
         * In android source code
         * finishInstrumentationLocked is implemented in ActivityManagerService
         */
        String pretty = null;
        if (results != null) {
            pretty = results.getString(Instrumentation.REPORT_KEY_STREAMRESULT);
        }
        if (pretty != null) {
            Log.i(TAG, pretty);
        } else {
            if (results != null) {
                for (String key : results.keySet()) {
                    Log.i(TAG, "Instrumentation_RESULT: " + key + "=" + results.getByte(key));
                }
            }
            Log.i(TAG, "Instrumentation_code: " + resultCode);
        }

        app.instrumentationClass = null;
        app.instrumentationInfo = null;
        app.instrumentationProfileFile = null;
        app.instrumentationArguments = null;

        //forceStopPackageLocked(app.processName, -1, false, false, true);
    }


    public void finishInstrumentation(IApplicationThread target,
            int resultCode, Bundle results) {
        // Refuse possible leaked file descriptors
        if (results != null && results.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }

        synchronized(this) {
            ProcessRecord app = getRecordForAppLocked(target);
            if (app == null) {
                Slog.w(TAG, "finishInstrumentation: no app for " + target);
                return;
            }
            //final long origId = Binder.clearCallingIdentity();
            finishInstrumentationLocked(app, resultCode, results);
            //Binder.restoreCallingIdentity(origId);
        }
    }

    final ProcessRecord getProcessRecordLocked(
            String processName, int uid) {
	/**
	/*@Mayloon update
	*/
    	/*
        if (uid == Process.SYSTEM_UID) {
            // The system gets to run in any process.  If there are multiple
            // processes with the same uid, just pick the first (this
            // should never happen).
            SparseArray<ProcessRecord> procs = mProcessNames.getMap().get(
                    processName);
            return procs != null ? procs.valueAt(0) : null;
        }
        */
        ProcessRecord proc = mProcessNames.get(processName);
        return proc;
    }


    final ProcessRecord addAppLocked(ApplicationInfo info) {
        ProcessRecord app = getProcessRecordLocked(info.processName, info.uid);

        if (app == null) {
            app = newProcessRecordLocked(null, info, null);
            mProcessNames.put(info.processName, app);
	    /**
            /*@Mayloon update
            */
            //updateLruProcessLocked(app, true, true);
        }

        if ((info.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_PERSISTENT))
                == (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_PERSISTENT)) {
            app.persistent = true;
            app.maxAdj = CORE_SERVER_ADJ;
        }
        /*
        if (app.thread == null && mPersistentStartingProcesses.indexOf(app) < 0) {
            mPersistentStartingProcesses.add(app);
            startProcessLocked(app, "added application", app.processName);
            Log.i(TAG,"start process locked");
        }
		*/
        return app;
    }



 // =========================================================
    // INSTRUMENTATION
    // =========================================================

    public boolean startInstrumentation(ComponentName className,
            String profileFile, int flags, Bundle arguments,
            IInstrumentationWatcher watcher) {
    	Log.i(TAG,"start instrumentation");
        // Refuse possible leaked file descriptors
        if (arguments != null && arguments.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Bundle");
        }

        InstrumentationInfo ii = null;
        ApplicationInfo ai = null;
        try {
            ii = Context.getSystemContext().getPackageManager().getInstrumentationInfo(
                className, STOCK_PM_FLAGS);
            ai = Context.getSystemContext().getPackageManager().getApplicationInfo(
                ii.targetPackage, STOCK_PM_FLAGS);
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (ii == null) {
            /**
            /*@Mayloon update
            */
            //reportStartInstrumentationFailure(watcher, className,
                    //"Unable to find instrumentation info for: " + className);
        	Log.e(TAG,"Unable to find instrumentation info for: " + className);
            return false;
        }
        if (ai == null) {
            /**
            /*@Mayloon update
            */
            //reportStartInstrumentationFailure(watcher, className,
                    //"Unable to find instrumentation target package: " + ii.targetPackage);
        	Log.e(TAG,"Unable to find instrumentation target package: " + ii.targetPackage);
        	return false;
        }

        /**
        /*@Mayloon update
        */
        /*
        int match = mContext.getPackageManager().checkSignatures(
                ii.targetPackage, ii.packageName);
        if (match < 0 && match != PackageManager.SIGNATURE_FIRST_NOT_SIGNED) {
            String msg = "Permission Denial: starting instrumentation "
                    + className + " from pid="
                    + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingPid()
                    + " not allowed because package " + ii.packageName
                    + " does not have a signature matching the target "
                    + ii.targetPackage;
            reportStartInstrumentationFailure(watcher, className, msg);
            throw new SecurityException(msg);
        }
        */

        /**
        /*@Mayloon update
        */
        //final long origId = Binder.clearCallingIdentity();
        //forceStopPackageLocked(ii.targetPackage, -1, true, false, true);
        ProcessRecord app = addAppLocked(ai);
        app.instrumentationClass = className;
        app.instrumentationInfo = ai;
        app.instrumentationProfileFile = profileFile;
        app.instrumentationArguments = arguments;
        /**
        /*@Mayloon update
        */
        //app.instrumentationWatcher = watcher;
        app.instrumentationResultClass = className;
        /**
        /*@Mayloon update
        */
        //Binder.restoreCallingIdentity(origId);

        /**
         * @Mayloon update
         * Comment this for bug 808
         */
        //if (app.thread == null && mPersistentStartingProcesses.indexOf(app) < 0) {
            mPersistentStartingProcesses.add(app);
            startProcessLocked(app, "added application", app.processName);
            Log.i(TAG, "start process locked");
        //}

        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getProcessMemoryInfo");
     */
    @MayloonStubAnnotation()
    public Object[] getProcessMemoryInfo(int[] pids) {
        System.out.println("Stub" + " Function : getProcessMemoryInfo");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: killBackgroundProcesses");
     */
    @MayloonStubAnnotation()
    public void killBackgroundProcesses(String packageName) {
        System.out.println("Stub" + " Function : killBackgroundProcesses");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getMemoryInfo");
     */
    @MayloonStubAnnotation()
    public void getMemoryInfo(Object outInfo) {
        System.out.println("Stub" + " Function : getMemoryInfo");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getRunningServices");
     */
    @MayloonStubAnnotation()
    public List getRunningServices(int maxNum) {
        System.out.println("Stub" + " Function : getRunningServices");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getRunningAppProcesses");
     */
    @MayloonStubAnnotation()
    public List getRunningAppProcesses() {
        System.out.println("Stub" + " Function : getRunningAppProcesses");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getMemoryClass");
     */
    @MayloonStubAnnotation()
    public int getMemoryClass() {
        System.out.println("Stub" + " Function : getMemoryClass");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: restartPackage");
     */
    @MayloonStubAnnotation()
    public void restartPackage(String packageName) {
        System.out.println("Stub" + " Function : restartPackage");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isUserAMonkey");
     */
    @MayloonStubAnnotation()
    public static boolean isUserAMonkey() {
        System.out.println("Stub" + " Function : isUserAMonkey");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getRunningTasks");
     */
    @MayloonStubAnnotation()
    public List getRunningTasks(int maxNum) {
        System.out.println("Stub" + " Function : getRunningTasks");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: MemoryInfo");
     */
    @MayloonStubAnnotation()
    public void MemoryInfo() {
        System.out.println("Stub" + " Function : MemoryInfo");
        return;
    }
}
