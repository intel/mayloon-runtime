package android.app;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.View;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerImpl;

public class ActivityThread {
	static final String TAG = "ActivityThread";
	private static boolean DEBUG_PROVIDER = true;
	static final boolean DEBUG_MESSAGES = false;
	static boolean DEBUG = true;
	final ApplicationThread mAppThread = new ApplicationThread();
	final H mH = new H();
	final HashMap<IBinder, ActivityClientRecord> mActivities = new HashMap<IBinder, ActivityClientRecord>();
	Instrumentation mInstrumentation;
	int mNumVisibleActivities = 0;
	ActivityClientRecord mNewActivities = null;
	String mProcessName = null;
	static ContextImpl mSystemContext = null;
	public static PackageManager sPackageManager;
	AppBindData mBoundApplication;
	Application mInitialApplication;
	static  ActivityThread sThreadLocal = new ActivityThread();
    final HashMap<ContentProvider, ProviderRefCount> mProviderRefCountMap
    = new HashMap<ContentProvider, ProviderRefCount>();
    final HashMap<String, ContentProvider> mProviderMap
    = new HashMap<String, ContentProvider>();
    private static final Pattern PATTERN_SEMICOLON = Pattern.compile(";");
	public static final boolean DEBUG_BROADCAST = false;
    final ArrayList<Application> mAllApplications = new ArrayList<Application>();

    String mInstrumentationAppDir = null;
    String mInstrumentationAppPackage = null;
    String mInstrumentedAppDir = null;
    
    Display mDisplay = null;
    DisplayMetrics mDisplayMetrics = null;
    
//	boolean mGcIdlerScheduled = false;
	final HashMap<IBinder, Service> mServices
    = new HashMap<IBinder, Service>();

	final HashMap<String, LoadedApk> mPackages
    = new HashMap<String, LoadedApk>();
	
    final HashMap<String, LoadedApk> mResourcePackages
    = new HashMap<String, LoadedApk>();

    final Looper mLooper = Looper.myLooper();

	public ApplicationThread getApplicationThread() {
		return mAppThread;
	}

    public Looper getLooper() {
        return mLooper;
    }

    public static final ActivityThread currentActivityThread() {
        return sThreadLocal;
    }
    public static final Application currentApplication() {
        ActivityThread am = currentActivityThread();
        return am != null ? am.mInitialApplication : null;
    }
    public static PackageManager getPackageManager() {
        return sPackageManager;
    }
    public  String currentPackageName() {
       return mProcessName;
    }
	public void main(String processName) {
		mProcessName = processName;
		Looper.prepareMainLooper();
		this.attach(false);
	}
    public final ContentProvider acquireProvider(Context c, String name) {
    	if(DEBUG_PROVIDER) System.out.println("ActivityThread:acquireProvider(Context c, String name)");
        ContentProvider provider = getProvider(c, name);
        if(provider == null)
            return null;
        ContentProvider jBinder = provider.asBinder();

        ProviderRefCount prc = mProviderRefCountMap.get(jBinder);
        if(prc == null) {
            mProviderRefCountMap.put(jBinder, new ProviderRefCount(1));
        } else {
            prc.count++;
        } //end else

        return provider;
    }
    private final ContentProvider getProvider(Context context, String name) {
        ContentProvider existing = getExistingProvider(context, name);
        if (existing != null) {
            return existing;
        }

//        ContentService.ContentProviderHolder holder = null;
//        System.out.println("ContentResolver:getProvider:ContentService.getDefault().getContentProvider...");
//        holder = ContentService.getDefault().getContentProvider(
//                getApplicationThread(), name);
//
        ActivityManager.ContentProviderHolder holder = null;
        if(DEBUG_PROVIDER)System.out.println("ActivityThread:getContentProvider from ActivityManager");
        holder = ((ActivityManager) Context.getSystemContext().getSystemService(
				Context.ACTIVITY_SERVICE)).getContentProvider(getApplicationThread(), name);
        if (holder == null) {
            System.out.println(TAG+"Failed to find provider info for " + name);
            return null;
        }

        ContentProvider prov = installProvider(context, holder.provider,
                holder.info, true);
        if(prov==null){
        	if(DEBUG_PROVIDER) System.out.println(TAG+" provider installation falled...");
        	return null;
        }
        //Slog.i(TAG, "noReleaseNeeded=" + holder.noReleaseNeeded);
        if (holder.noReleaseNeeded || holder.provider == null) {
            // We are not going to release the provider if it is an external
            // provider that doesn't care about being released, or if it is
            // a local provider running in this process.
            //Slog.i(TAG, "*** NO RELEASE NEEDED");
            mProviderRefCountMap.put(prov.asBinder(), new ProviderRefCount(10000));

        }
        return prov;
    }
    public final boolean releaseProvider(ContentProvider provider) {
        if(provider == null) {
            return false;
        }
        ContentProvider jBinder = provider.asBinder();

        ProviderRefCount prc = mProviderRefCountMap.get(jBinder);
        if(prc == null) {
            if(DEBUG) Log.v(TAG, "releaseProvider::Weird shouldn't be here");
            return false;
        } else {
            prc.count--;
            if(prc.count == 0) {
                // Schedule the actual remove asynchronously, since we
                // don't know the context this will be called in.
                // TODO: it would be nice to post a delayed message, so
                // if we come back and need the same provider quickly
                // we will still have it available.
                //Message msg = mH.obtainMessage(H.REMOVE_PROVIDER, provider);
                //mH.sendMessage(msg);
            } //end if
        } //end else
        return true;
    }
    private final ContentProvider installProvider(Context context,
            ContentProvider provider, ProviderInfo info, boolean noisy) {
        ContentProvider localProvider = null;
        if(DEBUG_PROVIDER)System.out.println(TAG+" installProvider...");
        if (provider == null) {
            if (noisy) {
                Log.d(TAG, "Loading provider " + info.authority + ": "
                        + info.name);
            }
            Context c = null;
            ApplicationInfo ai = info.applicationInfo;
            if (context.getPackageName().equals(ai.packageName)) {
            	//this app
                c = context;
                System.out.println("this app");
            } else if (mInitialApplication != null &&
                    mInitialApplication.getPackageName().equals(ai.packageName)) {
                //init app
            	c = mInitialApplication;
            	System.out.println("init app");
            } else {
            	//I only care about this
            	System.out.println("app:"+ai.packageName);
                try {
                    c = context.createPackageContext(ai.packageName,this);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            if (c == null) {
                Log.w(TAG, "Unable to get context for package " +
                      ai.packageName +
                      " while loading content provider " +
                      info.name);
                return null;
            }
            try {
                //final java.lang.ClassLoader cl = c.getClassLoader();
//                localProvider = (ContentProvider)cl.
//                    loadClass(info.name).newInstance();
                System.out.println("Provider:"+info.name);
                localProvider = (ContentProvider)(Class.forName(info.name).newInstance());

                if(DEBUG_PROVIDER)System.out.println(TAG+" Provider is attached...");
                provider = localProvider/*localProvider.getIContentProvider()*/;
                if (provider == null) {
                    Log.e(TAG, "Failed to instantiate class " +
                          info.name + " from sourceDir " +
                          info.applicationInfo.sourceDir);
                    return null;
                }
                if(DEBUG_PROVIDER) Log.v(
                    TAG, "Instantiating local provider " + info.name);
                // XXX Need to create the correct context for this provider.
                localProvider.attachInfo(c, info);
                if(DEBUG_PROVIDER)System.out.println(TAG+" Provider is attached...");
            } catch (java.lang.Exception e) {
                Log.d(TAG, "Exception occured...");
                return null;
            }
        } else if (DEBUG) {
            Log.v(TAG, "Installing external provider " + info.authority + ": "
                    + info.name);
        }
        if(DEBUG_PROVIDER)System.out.println(TAG+" Provider ...");
        // Cache the pointer for the remote provider.
       // String names[] = PATTERN_SEMICOLON.split(info.authority);
        String names[] = info.authority.split(";");
        if(DEBUG_PROVIDER)System.out.println(TAG+" Provider test...");
        for(int i=0;i<names.length;i++){
        	mProviderMap.put(names[i], provider);
        }
        if(DEBUG_PROVIDER)System.out.println(TAG+" Provider is installed...");
        return provider;
    }
    public final ContentProvider acquireExistingProvider(Context c, String name) {
        ContentProvider provider = getExistingProvider(c, name);
        if(provider == null)
            return null;
        ContentProvider jBinder = provider.asBinder();

        ProviderRefCount prc = mProviderRefCountMap.get(jBinder);
        if(prc == null) {
            mProviderRefCountMap.put(jBinder, new ProviderRefCount(1));
        } else {
            prc.count++;
        } //end else
        return provider;
    }
    private final ContentProvider getExistingProvider(Context context, String name) {
        System.out.println("Getting existing provider...");
        final ContentProvider pr = mProviderMap.get(name);
        if (pr != null) {
            return pr;
        }
        return null;
    }
    public final void removeExistingProvider(Context context, String name) {
        mProviderMap.remove(name);
    }
    private final class ProviderRefCount {
        public int count;
        ProviderRefCount(int pCount) {
            count = pCount;
        }
    }

    private void attach(boolean system) {
        sThreadLocal = this;
        if (system == false) {
            ActivityManager mgr = (ActivityManager) Context.getSystemContext()
                    .getSystemService(Context.ACTIVITY_SERVICE);
            mgr.attachApplication(mAppThread, mProcessName);
        }
    }

	private final void queueOrSendMessage(int what, Object obj, int arg1,
			int arg2) {
		Message msg = Message.obtain();
		msg.what = what;
		msg.obj = obj;
		msg.arg1 = arg1;
		msg.arg2 = arg2;
		mH.sendMessage(msg);
	}

	final public Handler getHandler() {
        return mH;
    }

	private final void queueOrSendMessage(int what, Object obj) {
		queueOrSendMessage(what, obj, 0, 0);
	}

	private final void queueOrSendMessage(int what, Object obj, int arg1) {
		queueOrSendMessage(what, obj, arg1, 0);
	}

	final class ApplicationThread extends Binder implements IApplicationThread {
		static final String descriptor = "android.app.ApplicationThread";

		final int SCHEDULE_PAUSE_ACTIVITY_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION;
		final int SCHEDULE_STOP_ACTIVITY_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 2;
		final int SCHEDULE_WINDOW_VISIBILITY_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 3;
		final int SCHEDULE_RESUME_ACTIVITY_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 4;
		final int SCHEDULE_SEND_RESULT_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 5;
		final int SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 6;
		final int SCHEDULE_FINISH_ACTIVITY_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 8;

		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
			switch (code) {
			case SCHEDULE_PAUSE_ACTIVITY_TRANSACTION: {
				data.enforceInterface(ApplicationThread.descriptor);
				IBinder b = data.readStrongBinder();
				boolean finished = false;// data.readInt() != 0;
				boolean userLeaving = false;// data.readInt() != 0;
				int configChanges = 0;// data.readInt();
				schedulePauseActivity(b, finished, userLeaving, configChanges);
				return true;
			}

			case SCHEDULE_RESUME_ACTIVITY_TRANSACTION: {
				data.enforceInterface(ApplicationThread.descriptor);
				IBinder b = data.readStrongBinder();
				boolean isForward = false;// data.readInt() != 0;
				scheduleResumeActivity(b, isForward);
				return true;
			}
			}

			return false;
		}

		public final void schedulePauseActivity(IBinder token,
				boolean finished, boolean userLeaving, int configChanges) {
			queueOrSendMessage(finished ? H.PAUSE_ACTIVITY_FINISHING
					: H.PAUSE_ACTIVITY, token, (userLeaving ? 1 : 0),
					configChanges);
		}

		public final void scheduleStopActivity(IBinder token,
				boolean showWindow, int configChanges) {
			queueOrSendMessage(showWindow ? H.STOP_ACTIVITY_SHOW
					: H.STOP_ACTIVITY_HIDE, token, 0, configChanges);
		}

		public final void scheduleWindowVisibility(IBinder token,
				boolean showWindow) {
			queueOrSendMessage(showWindow ? H.SHOW_WINDOW : H.HIDE_WINDOW,
					token);
		}


		public final void scheduleResumeActivity(IBinder token,
				boolean isForward) {
			queueOrSendMessage(H.RESUME_ACTIVITY, token, isForward ? 1 : 0);
		}

		public final void scheduleSendResult(IBinder token,
				List<ResultInfo> results) {
			ResultData res = new ResultData();
			res.token = token;
			res.results = results;
			queueOrSendMessage(H.SEND_RESULT, res);
		}

		// we use token to identify this activity without having to send the
		// activity itself back to the activity manager. (matters more with ipc)
		public final void scheduleLaunchActivity(Intent intent, IBinder token,
				int ident, ActivityInfo info, Bundle state,
				List<ResultInfo> pendingResults,
				List<Intent> pendingNewIntents, boolean notResumed,
				boolean isForward) {
			ActivityClientRecord r = new ActivityClientRecord();

			r.token = token;
			r.ident = ident;
			r.intent = intent;
			r.activityInfo = info;
			r.state = state;

			r.pendingResults = pendingResults;
			r.pendingIntents = pendingNewIntents;

			r.startsNotResumed = notResumed;
			r.isForward = isForward;

			queueOrSendMessage(H.LAUNCH_ACTIVITY, r);
		}

		public final void scheduleDestroyActivity(IBinder token,
				boolean finishing, int configChanges) {
			queueOrSendMessage(H.DESTROY_ACTIVITY, token, finishing ? 1 : 0,
					configChanges);
		}

		public final void scheduleCreateService(IBinder token, ServiceInfo info)
		{
			CreateServiceData s = new CreateServiceData();
            s.token = token;
            s.info = info;
			queueOrSendMessage(H.CREATE_SERVICE, s);
		}

		public final void scheduleBindService(IBinder token, Intent intent,
                boolean rebind) {
            BindServiceData s = new BindServiceData();
            s.token = token;
            s.intent = intent;
            s.rebind = rebind;

            queueOrSendMessage(H.BIND_SERVICE, s);
        }

		public void scheduleServiceArgs(ServiceRecord token, int startId, int flags,
				Intent args) {
			 ServiceArgsData s = new ServiceArgsData();
	            s.token = token;
	            s.startId = startId;
	            s.flags = flags;
	            s.args = args;

	            queueOrSendMessage(H.SERVICE_ARGS, s);

		}

		public final void bindApplication(String processName,
				ApplicationInfo appInfo, ComponentName instrumentationName,
				String profileFile, Bundle instrumentationArgs, int debugMode,
				boolean isRestrictedBackupMode) {
			//			System.out.println("in ApplicationThread.bindAppliaction");
			AppBindData data = new AppBindData();
			data.processName = processName;
			data.appInfo = appInfo;
			data.instrumentationName = instrumentationName;
			data.profileFile = profileFile;
			data.instrumentationArgs = instrumentationArgs;
			data.debugMode = debugMode;
			data.restrictedBackupMode = isRestrictedBackupMode;
			queueOrSendMessage(H.BIND_APPLICATION, data);
		}

		@Override
		public IBinder asBinder() {
			return this;
		}

		public Handler getHandler(){
			return mH;
		}


	}

	private static final class AppBindData {
		LoadedApk info;
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        ComponentName instrumentationName;
        String profileFile;
        Bundle instrumentationArgs;
        IInstrumentationWatcher instrumentationWatcher;
        int debugMode;
        boolean restrictedBackupMode;
        Configuration config;
        boolean handlingProfiling;
        public String toString() {
            return "AppBindData{appInfo=" + appInfo + "}";
        }
	}

    private final class H extends Handler {
        public static final int LAUNCH_ACTIVITY = 100;
        public static final int PAUSE_ACTIVITY = 101;
        public static final int PAUSE_ACTIVITY_FINISHING = 102;
        public static final int STOP_ACTIVITY_SHOW = 103;
        public static final int STOP_ACTIVITY_HIDE = 104;
        public static final int SHOW_WINDOW = 105;
        public static final int HIDE_WINDOW = 106;
        public static final int RESUME_ACTIVITY = 107;
        public static final int SEND_RESULT = 108;
        public static final int DESTROY_ACTIVITY = 109;
        public static final int BIND_APPLICATION = 110;
        public static final int EXIT_APPLICATION = 111;
        public static final int NEW_INTENT = 112;
        public static final int RECEIVER = 113;
        public static final int CREATE_SERVICE = 114;
        public static final int SERVICE_ARGS = 115;
        public static final int STOP_SERVICE = 116;
        public static final int REQUEST_THUMBNAIL = 117;
        public static final int CONFIGURATION_CHANGED = 118;
        public static final int CLEAN_UP_CONTEXT = 119;
        public static final int GC_WHEN_IDLE = 120;
        public static final int BIND_SERVICE = 121;
        public static final int UNBIND_SERVICE = 122;
        public static final int DUMP_SERVICE = 123;
        public static final int LOW_MEMORY = 124;
        public static final int ACTIVITY_CONFIGURATION_CHANGED = 125;
        public static final int RELAUNCH_ACTIVITY = 126;
        public static final int PROFILER_CONTROL = 127;
        public static final int CREATE_BACKUP_AGENT = 128;
        public static final int DESTROY_BACKUP_AGENT = 129;
        public static final int SUICIDE = 130;
        public static final int REMOVE_PROVIDER = 131;
        public static final int ENABLE_JIT = 132;
        public static final int DISPATCH_PACKAGE_BROADCAST = 133;
        public static final int SCHEDULE_CRASH = 134;

        String codeToString(int code) {
            if (DEBUG_MESSAGES) {
                switch (code) {
                    case PAUSE_ACTIVITY:
                        return "PAUSE_ACTIVITY";
                    case PAUSE_ACTIVITY_FINISHING:
                        return "PAUSE_ACTIVITY_FINISHING";
                    case RESUME_ACTIVITY:
                        return "RESUME_ACTIVITY";
                }
            }
            return "(unknown)";
        }

        public void handleMessage(Message msg) {
            if (DEBUG_MESSAGES)
                Slog.v(TAG, ">>> handling: " + msg.what);
            switch (msg.what) {
                case LAUNCH_ACTIVITY: {
                    ActivityClientRecord r = (ActivityClientRecord) msg.obj;

                    r.packageInfo = getPackageInfoNoCheck(
                            r.activityInfo.applicationInfo);
                    handleLaunchActivity(r, null);
                }
                    break;
                case RELAUNCH_ACTIVITY: {
                    ActivityClientRecord r = (ActivityClientRecord) msg.obj;
                    // handleRelaunchActivity(r, msg.arg1);
                    Log.e(TAG, "RelaunchActivity is not handled now!");
                }
                    break;
                case PAUSE_ACTIVITY:
                    handlePauseActivity((IBinder) msg.obj, false, msg.arg1 != 0, msg.arg2);
                    // maybeSnapshot();
                    break;
                case PAUSE_ACTIVITY_FINISHING:
                    handlePauseActivity((IBinder) msg.obj, true, msg.arg1 != 0, msg.arg2);
                    break;
                case STOP_ACTIVITY_SHOW:
                    handleStopActivity((IBinder)msg.obj, true, msg.arg2);
                    break;
                case STOP_ACTIVITY_HIDE:
                    handleStopActivity((IBinder)msg.obj, false, msg.arg2);
                    break;
                case SHOW_WINDOW:
                    handleWindowVisibility((IBinder)msg.obj, true);
                    break;
                case HIDE_WINDOW:
                    handleWindowVisibility((IBinder)msg.obj, false);
                    break;
                case RESUME_ACTIVITY:
                    handleResumeActivity((IBinder) msg.obj, true,
                            msg.arg1 != 0);
                    break;
                case SEND_RESULT:
                    handleSendResult((ResultData) msg.obj);
                    break;
                case DESTROY_ACTIVITY:
                    handleDestroyActivity((IBinder) msg.obj, msg.arg1 != 0,
                            msg.arg2, false);
                    break;
                case BIND_APPLICATION:
                    AppBindData data = (AppBindData) msg.obj;
                    handleBindApplication(data);
                    break;
                case EXIT_APPLICATION:
                    if (mInitialApplication != null) {
                        mInitialApplication.onTerminate();
                    }
                    Looper.myLooper().quit();
                    break;
                case NEW_INTENT:
                    // handleNewIntent((NewIntentData)msg.obj);
                    Log.e(TAG, "NewIntent is not handled now!");
                    break;
                case RECEIVER:
                    // handleReceiver((ReceiverData)msg.obj);
                    Log.e(TAG, "Receiver is not handled now!");
                    // maybeSnapshot();
                    break;
                case CREATE_SERVICE:
                    handleCreateService((CreateServiceData) msg.obj);
                    break;
                case BIND_SERVICE:
                    handleBindService((BindServiceData) msg.obj);
                    break;
                case UNBIND_SERVICE:
                    // handleUnbindService((BindServiceData)msg.obj);
                    Log.e(TAG, "UnbindService is not handled now!");
                    break;
                case SERVICE_ARGS:
                    handleServiceArgs((ServiceArgsData) msg.obj);
                    break;
                case STOP_SERVICE:
                    // handleStopService((IBinder)msg.obj);
                    Log.e(TAG, "StopService is not handled now!");
                    // maybeSnapshot();
                    break;
                case REQUEST_THUMBNAIL:
                    // handleRequestThumbnail((IBinder)msg.obj);
                    Log.e(TAG, "RequestThumbnail is not handled now!");
                    break;
                case CONFIGURATION_CHANGED:
                    // handleConfigurationChanged((Configuration)msg.obj);
                    Log.e(TAG, "ConfigurationChanged is not handled now!");
                    break;
                case CLEAN_UP_CONTEXT:
                    // ContextCleanupInfo cci = (ContextCleanupInfo)msg.obj;
                    // cci.context.performFinalCleanup(cci.who, cci.what);
                    Log.e(TAG, "CLEAN_UP_CONTEXT is not handled now!");
                    break;
                case GC_WHEN_IDLE:
                    // scheduleGcIdler();
                    Log.e(TAG, "GC_WHEN_IDLE is not handled now!");
                    break;
                case DUMP_SERVICE:
                    // handleDumpService((DumpServiceInfo)msg.obj);
                    Log.e(TAG, "DumpService is not handled now!");
                    break;
                case LOW_MEMORY:
                    // handleLowMemory();
                    Log.e(TAG, "LowMemory is not handled now!");
                    break;
                case ACTIVITY_CONFIGURATION_CHANGED:
                    // handleActivityConfigurationChanged((IBinder)msg.obj);
                    Log.e(TAG, "ActivityConfigurationChanged is not handled now!");
                    break;
                case PROFILER_CONTROL:
                    // handleProfilerControl(msg.arg1 != 0,
                    // (ProfilerControlData)msg.obj);
                    Log.e(TAG, "ProfilerControl is not handled now!");
                    break;
                case CREATE_BACKUP_AGENT:
                    // handleCreateBackupAgent((CreateBackupAgentData)msg.obj);
                    Log.e(TAG, "CreateBackupAgent is not handled now!");
                    break;
                case DESTROY_BACKUP_AGENT:
                    // handleDestroyBackupAgent((CreateBackupAgentData)msg.obj);
                    Log.e(TAG, "DestroyBackupAgent is not handled now!");
                    break;
                case SUICIDE:
                    // Process.killProcess(Process.myPid());
                    Log.e(TAG, "SUICIDE is not handled now!");
                    break;
                case REMOVE_PROVIDER:
                    // completeRemoveProvider((IContentProvider)msg.obj);
                    Log.e(TAG, "RemoveProvider is not handled now!");
                    break;
                case ENABLE_JIT:
                    // ensureJitEnabled();
                    Log.e(TAG, "ENABLE_JIT is not handled now!");
                    break;
                case DISPATCH_PACKAGE_BROADCAST:
                    // handleDispatchPackageBroadcast(msg.arg1,
                    // (String[])msg.obj);
                    Log.e(TAG, "DispatchPackageBroadcast is not handled now!");
                    break;
                case SCHEDULE_CRASH:
                    throw new RemoteServiceException((String) msg.obj);
            }
            if (DEBUG_MESSAGES)
                Slog.v(TAG, "<<< done: " + msg.what);
        }
    }

	public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai) {
        return getPackageInfo(ai, null, false, true);
    }

	private final void handleBindApplication(AppBindData data) {
		mBoundApplication = data;
        //mConfiguration = new Configuration(data.config);

        // send up app name; do this *before* waiting for debugger
        //Process.setArgV0(data.processName);
        //android.ddm.DdmHandleAppName.setAppName(data.processName);

        /*
         * Before spawning a new process, reset the time zone to be the system time zone.
         * This needs to be done because the system time zone could have changed after the
         * the spawning of this process. Without doing this this process would have the incorrect
         * system time zone.
         */
        //TimeZone.setDefault(null);

        /*
         * Initialize the default locale in this process for the reasons we set the time zone.
         */
        //Locale.setDefault(data.config.locale);

        /*
         * Update the system configuration since its preloaded and might not
         * reflect configuration changes. The configuration object passed
         * in AppBindData can be safely assumed to be up to date
         */
        //Resources.getSystem().updateConfiguration(mConfiguration, null);

        data.info = getPackageInfoNoCheck(data.appInfo);

        /**
         * For system applications on userdebug/eng builds, log stack
         * traces of disk and network access to dropbox for analysis.
         */
        if ((data.appInfo.flags &
             (ApplicationInfo.FLAG_SYSTEM |
              ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
           // StrictMode.conditionallyEnableDebugLogging();
        }

        /**
         * Switch this process to density compatibility mode if needed.
         */
        if ((data.appInfo.flags&ApplicationInfo.FLAG_SUPPORTS_SCREEN_DENSITIES)
                == 0) {
            Bitmap.setDefaultDensity(DisplayMetrics.DENSITY_DEFAULT);
        }

        /*
        if (data.debugMode != IApplicationThread.DEBUG_OFF) {
            // XXX should have option to change the port.
            Debug.changeDebugPort(8100);
            if (data.debugMode == IApplicationThread.DEBUG_WAIT) {
                Slog.w(TAG, "Application " + data.info.getPackageName()
                      + " is waiting for the debugger on port 8100...");

                IActivityManager mgr = ActivityManagerNative.getDefault();
                try {
                    mgr.showWaitingForDebugger(mAppThread, true);
                } catch (RemoteException ex) {
                }

                Debug.waitForDebugger();

                try {
                    mgr.showWaitingForDebugger(mAppThread, false);
                } catch (RemoteException ex) {
                }

            } else {
                Slog.w(TAG, "Application " + data.info.getPackageName()
                      + " can be debugged on port 8100...");
            }
        }
		*/

        if (data.instrumentationName != null) {
            ContextImpl appContext = new ContextImpl();
            appContext.init(data.info, null, this);
            InstrumentationInfo ii = null;
            
            /**
             * @Mayloon update
             * Remove catch exception
             */
            ii = appContext.getPackageManager().
                    getInstrumentationInfo(data.instrumentationName, 0);

            if (ii == null) {
                throw new RuntimeException(
                    "Unable to find instrumentation info for: "
                    + data.instrumentationName);
            }

            mInstrumentationAppDir = ii.sourceDir;
            mInstrumentationAppPackage = ii.packageName;
            //mInstrumentedAppDir = data.info.getAppDir();

            ApplicationInfo instrApp = new ApplicationInfo();
            instrApp.packageName = ii.packageName;
            instrApp.sourceDir = ii.sourceDir;
            instrApp.publicSourceDir = ii.publicSourceDir;
            instrApp.dataDir = ii.dataDir;
            instrApp.nativeLibraryDir = ii.nativeLibraryDir;
            LoadedApk pi = getPackageInfo(instrApp,
                    appContext.getClassLoader(), false, true);
            ContextImpl instrContext = new ContextImpl();
            instrContext.init(pi, null, this);

            try {
                /**
                 * @Mayloon update
                 * Using Class.forName instead of classLoader to load class
                 */
                /*
                 * java.lang.ClassLoader cl = instrContext.getClassLoader();
                 * mInstrumentation = (Instrumentation)
                 * cl.loadClass(data.instrumentationName
                 * .getClassName()).newInstance();
                 */
                mInstrumentation = (Instrumentation) Class.forName(
                        data.instrumentationName.getClassName()).newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                    "Unable to instantiate instrumentation "
                    + data.instrumentationName + ": " + e.toString(), e);
            }
            //mInstrumentation.init(this, instrContext, appContext,
                    //new ComponentName(ii.packageName, ii.name), data.instrumentationWatcher);
            mInstrumentation.init(this, instrContext, appContext,
                    new ComponentName(ii.packageName, ii.name), data.instrumentationWatcher);

            if (data.profileFile != null && !ii.handleProfiling) {
                data.handlingProfiling = true;
                File file = new File(data.profileFile);
                file.getParentFile().mkdirs();
                //Debug.startMethodTracing(file.toString(), 8 * 1024 * 1024);
            }

            try {
                mInstrumentation.onCreate(data.instrumentationArgs);
            }
            catch (Exception e) {
                throw new RuntimeException(
                    "Exception thrown in onCreate() of "
                    + data.instrumentationName + ": " + e.toString(), e);
            }

        } else {
            mInstrumentation = new Instrumentation();
        }

     // If the app is being launched for full backup or restore, bring it up in
        // a restricted environment with the base application class.
        Application app = data.info.makeApplication(data.restrictedBackupMode, null);
        mInitialApplication = app;

        List<ProviderInfo> providers = data.providers;
        if (providers != null) {
            //installContentProviders(app, providers);
            // For process that contain content providers, we want to
            // ensure that the JIT is enabled "at some point".
            mH.sendEmptyMessageDelayed(H.ENABLE_JIT, 10*1000);
        }

        try {
            mInstrumentation.callApplicationOnCreate(app);
        } catch (Exception e) {
            if (!mInstrumentation.onException(app, e)) {
                throw new RuntimeException(
                    "Unable to create application " + app.getClass().getName()
                    + ": " + e.toString(), e);
            }
        }
    }

    private final void handleLaunchActivity(ActivityClientRecord r,
            Intent customIntent) {
        long start = System.currentTimeMillis();
        Log.w(TAG,"Begin to launch Activity " + r.activityInfo.name + " at time: " + start);
        Activity a = performLaunchActivity(r, customIntent);

        if (a != null) {
            // r.createdConfig = new Configuration(mConfiguration);
            Bundle oldState = r.state;
            handleResumeActivity(r.token, false, r.isForward);

            if (!r.activity.mFinished && r.startsNotResumed) {
                // The activity manager actually wants this one to start out
                // paused, because it needs to be visible but isn't in the
                // foreground. We accomplish this by going through the
                // normal startup (because activities expect to go through
                // onResume() the first time they run, before their window
                // is displayed), and then pausing it. However, in this case
                // we do -not- need to do the full pause cycle (of freezing
                // and such) because the activity manager assumes it can just
                // retain the current state it has.
                System.out.println("if catched");
                r.activity.mCalled = false;
                mInstrumentation.callActivityOnPause(r.activity);
                // We need to keep around the original state, in case
                // we need to be created again.
                r.state = oldState;
                if (!r.activity.mCalled) {
                    System.out.println("Activity "
                            + r.intent.getComponent().toShortString()
                            + " did not call through to super.onPause()");
                }
                r.paused = true;
            }
        } else {
            // If there was an error, for any reason, tell the activity
            // manager to stop us.
            ((ActivityManager) Context.getSystemContext().getSystemService(
                    Context.ACTIVITY_SERVICE)).finishActivity(r.token,
                    Activity.RESULT_CANCELED, null);
        }
        long end = System.currentTimeMillis();
        Log.w(TAG,"Finish to launch Activity " + r.activityInfo.name + " at time: " + end);
        Log.w(TAG,"Launch Activity " + r.activityInfo.name + " used time:" + (end-start) + "ms");
    }

    private final void deliverNewIntents(ActivityClientRecord r,
            List<Intent> intents) {
        final int N = intents.size();
        for (int i=0; i<N; i++) {
            Intent intent = intents.get(i);
            intent.setExtrasClassLoader(r.activity.getClassLoader());
//            mInstrumentation.callActivityOnNewIntent(r.activity, intent);
        }
    }

    public final void performNewIntents(IBinder token,
            List<Intent> intents) {
        ActivityClientRecord r = mActivities.get(token);
        if (r != null) {
            final boolean resumed = !r.paused;
            if (resumed) {
                mInstrumentation.callActivityOnPause(r.activity);
            }
            deliverNewIntents(r, intents);
            if (resumed) {
                mInstrumentation.callActivityOnResume(r.activity);
            }
        }
    }
    
    DisplayMetrics getDisplayMetricsLocked(boolean forceUpdate) {
        if (mDisplayMetrics != null && !forceUpdate) {
            return mDisplayMetrics;
        }
        if (mDisplay == null) {
            WindowManager wm = WindowManagerImpl.getDefault();
            mDisplay = wm.getDefaultDisplay();
        }
        DisplayMetrics metrics = mDisplayMetrics = new DisplayMetrics();
        mDisplay.getMetrics(metrics);
        //Slog.i("foo", "New metrics: w=" + metrics.widthPixels + " h="
        //        + metrics.heightPixels + " den=" + metrics.density
        //        + " xdpi=" + metrics.xdpi + " ydpi=" + metrics.ydpi);
        return metrics;
    }
    
    public final LoadedApk getPackageInfo(ApplicationInfo ai) {
        return getPackageInfo(ai, null, false, false);
    }
    
    public final LoadedApk getPackageInfo(String packageName, int flags) {
    	LoadedApk packageInfo = null;
        if ((flags&Context.CONTEXT_INCLUDE_CODE) != 0) {
        	packageInfo = mPackages.get(packageName);
        } else {
        	packageInfo = mResourcePackages.get(packageName);
        }
        
        //Slog.i(TAG, "getPackageInfo " + packageName + ": " + packageInfo);
        //if (packageInfo != null) Slog.i(TAG, "isUptoDate " + packageInfo.mResDir
        //        + ": " + packageInfo.mResources.getAssets().isUpToDate());
        if (packageInfo != null && (packageInfo.mResources == null
                || packageInfo.mResources.getAssets().isUpToDate())) {
        	/*
            if (packageInfo.isSecurityViolation()
                    && (flags&Context.CONTEXT_IGNORE_SECURITY) == 0) {
                throw new SecurityException(
                        "Requesting code from " + packageName
                        + " to be run in process "
                        + mBoundApplication.processName
                        + "/" + mBoundApplication.appInfo.uid);
            }
            */
            return packageInfo;
        }

        ApplicationInfo ai = null;
        /*
        try {
            ai = getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_SHARED_LIBRARY_FILES);
        } catch (RemoteException e) {
        }
		*/
        if (ai != null) {
            return getPackageInfo(ai, flags);
        }

        return null;
    }
    
    public final LoadedApk getPackageInfo(ApplicationInfo ai, int flags) {
        boolean includeCode = (flags&Context.CONTEXT_INCLUDE_CODE) != 0;
        boolean securityViolation = false;
        /*
        boolean securityViolation = includeCode && ai.uid != 0
                && ai.uid != Process.SYSTEM_UID && (mBoundApplication != null
                        ? ai.uid != mBoundApplication.appInfo.uid : true);
        */
        if ((flags&(Context.CONTEXT_INCLUDE_CODE
                |Context.CONTEXT_IGNORE_SECURITY))
                == Context.CONTEXT_INCLUDE_CODE) {
            if (securityViolation) {
                String msg = "Requesting code from " + ai.packageName
                        + " (with uid " + ai.uid + ")";
                if (mBoundApplication != null) {
                    msg = msg + " to be run in process "
                        + mBoundApplication.processName + " (with uid "
                        + mBoundApplication.appInfo.uid + ")";
                }
                throw new SecurityException(msg);
            }
        }
        return getPackageInfo(ai, null, securityViolation, includeCode);
    }


    private final LoadedApk getPackageInfo(ApplicationInfo aInfo,
            ClassLoader baseLoader, boolean securityViolation, boolean includeCode) {
        LoadedApk packageInfo = mResourcePackages.get(aInfo.packageName);
        if (packageInfo == null) {
            packageInfo = new LoadedApk(this, aInfo);
            mResourcePackages.put(aInfo.packageName, packageInfo);
        }
        return packageInfo;
    }
    
    public ContextImpl getSystemContext() {
        if (mSystemContext == null) {
            ContextImpl context =
                ContextImpl.createSystemContext(this);
            //LoadedApk info = new LoadedApk(this, "android", context, null);
            LoadedApk info = new LoadedApk(this, null);
            context.init(info, null, this);
            /*
            context.getResources().updateConfiguration(
                    getConfiguration(), getDisplayMetricsLocked(false));
            */
            mSystemContext = context;
            //Slog.i(TAG, "Created system resources " + context.getResources()
            //        + ": " + context.getResources().getConfiguration());
        }
        return mSystemContext;
    }


	private final Activity performLaunchActivity(ActivityClientRecord r,
			Intent customIntent) {
        ActivityInfo aInfo = r.activityInfo;
        if (r.packageInfo == null) {
            r.packageInfo = getPackageInfo(aInfo.applicationInfo);
        }

		ComponentName component = r.intent.getComponent();
		if (component == null) {
			System.out.println("component is null");
			//			component = r.intent.resolveActivity(mInitialApplication
			//					.getPackageManager());
			//			r.intent.setComponent(component);
		}

		if (r.activityInfo.targetActivity != null) {
			component = new ComponentName(r.activityInfo.packageName,
					r.activityInfo.targetActivity);
		}

		Activity activity = null;
		try {
			activity = mInstrumentation.newActivity(component.getClassName(),
					r.intent);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		Application app = r.packageInfo.makeApplication(false, mInstrumentation);

		if (activity != null) {
			activity.mZIndex = r.activityInfo.zIndex;

			Context appContext = null;
			try {
				//				System.out.println("r.activityInfo.packageName: "
				//						+ r.activityInfo.packageName);
				appContext = Context.getSystemContext().createPackageContext(
						r.activityInfo.packageName,this);
				appContext.setLoadedApk(r.packageInfo);
				appContext.setOuterContext(activity);
				Log.i(TAG, "appContext is created:"+appContext.icount);
				if(appContext.mActivityThread==null){
					System.out.println("appContext's mActivityThread is null");
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}

            // MayLoon: We are not aware of Display/Configuration change, here
            // we update the display metrics
            DisplayMetrics dm = getDisplayMetricsLocked(true);
            appContext.getResources().updateConfiguration(null, dm);

			CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());
			activity.attach(appContext, mInstrumentation, title, this, r.token, r.intent, app);
			Context.setOuterContext(activity);

			//			System.out.println("attach ok");
			System.out.println("activity context in AT is:"+activity.icount);
			if(activity.mActivityThread==null){
				System.out.println("activity thread in AT is null");
			}
			if (customIntent != null) {
				activity.mIntent = customIntent;
			}
			r.lastNonConfigurationInstance = null;
			r.lastNonConfigurationChildInstances = null;
			activity.mStartedActivity = false;
			int theme = r.activityInfo.getThemeResource();
			if (theme != 0) {
				activity.setTheme(theme);
			}

			Context.getSystemContext().getActivityManager().mCurActivity = activity;
			activity.mCalled = false;
			// In OnCreate, we will call setContent where the main view layout occurs.
			long start = System.currentTimeMillis();
			Log.w(TAG,"Begin to call Activity OnCreate " + activity.getComponentName().getClassName() + " at time: " + start);
			mInstrumentation.callActivityOnCreate(activity, r.state);
			if (!activity.mCalled) {
				System.out.println("Activity "
						+ r.intent.getComponent().toShortString()
						+ " did not call through to super.onCreate()");
			}
			long end = System.currentTimeMillis();
			Log.w(TAG,"Finish to call Activity OnCreate " + activity.getComponentName().getClassName() + " at time: " + end);
			Log.w(TAG,"Activity OnCreate for " + activity.getComponentName().getClassName() + " used time:" + (end-start) + "ms");
			/**/
			r.activity = activity;
			r.stopped = true;
			if (!r.activity.mFinished) {
				activity.performStart();
				r.stopped = false;
			}
			if (!r.activity.mFinished) {
				if (r.state != null) {
					mInstrumentation.callActivityOnRestoreInstanceState(
							activity, r.state);
				}
			}
			if (!r.activity.mFinished) {
				activity.mCalled = false;
				mInstrumentation.callActivityOnPostCreate(activity, r.state);
				if (!activity.mCalled) {
					System.out.println("Activity "
							+ r.intent.getComponent().toShortString()
							+ " did not call through to super.onPostCreate()");
				}
			}
			/**/
		}
		r.paused = true;

		mActivities.put(r.token, r);

		//		System.out.println("perform launch over");
		return activity;
	}

	private final void handlePauseActivity(IBinder token, boolean finished,
			boolean userLeaving, int configChanges) {
		//		System.out.println("in handlePauseActivity, finished: " + finished);
		ActivityClientRecord r = mActivities.get(token);
		if (r != null) {
			//			r.activity.mConfigChangeFlags |= configChanges;
			Bundle state = performPauseActivity(token, finished, true);

			// Tell the activity manager we have paused.
			Context.getSystemContext().getActivityManager()
					.activityPaused(token, state);
		}
	}

	final Bundle performPauseActivity(IBinder token, boolean finished,
			boolean saveState) {
		ActivityClientRecord r = mActivities.get(token);
		return r != null ? performPauseActivity(r, finished, saveState) : null;
	}

	final Bundle performPauseActivity(ActivityClientRecord r, boolean finished,
			boolean saveState) {
		if (r.paused) {
			if (r.activity.mFinished) {
				// If we are finishing, we won't call onResume() in certain
				// cases.
				// So here we likewise don't want to call onPause() if the
				// activity
				// isn't resumed.
				return null;
			}
			RuntimeException e = new RuntimeException(
					"Performing pause of activity that is not resumed: "
							+ r.intent.getComponent().toShortString());
			System.out.println(TAG + e.getMessage());
		}
		Bundle state = null;
		if (finished) {
			r.activity.mFinished = true;
		}
		// Next have the activity save its current state and managed
		// dialogs...
		if (!r.activity.mFinished && saveState) {
			state = new Bundle();
			mInstrumentation.callActivityOnSaveInstanceState(r.activity, state);
			r.state = state;
		}
		// Now we are idle.
		r.activity.mCalled = false;
		mInstrumentation.callActivityOnPause(r.activity);
		r.paused = true;
		return state;
	}

    private final void handleStopActivity(IBinder token, boolean show, int configChanges) {
        Log.i(TAG, "handleStopActivity");
        ActivityClientRecord r = mActivities.get(token);
        r.activity.mConfigChangeFlags |= configChanges;

//        StopInfo info = new StopInfo();
        performStopActivityInner(r, show);

//        if (localLOGV) Slog.v(
//            TAG, "Finishing stop of " + r + ": show=" + show
//            + " win=" + r.window);

        updateVisibility(r, show);

        // Tell activity manager we have been stopped.
        Context.getSystemContext().getActivityManager().activityStopped(r.token);
    }

    final void performStopActivity(IBinder token) {
        ActivityClientRecord r = mActivities.get(token);
        performStopActivityInner(r, false);
    }

    private final void performStopActivityInner(ActivityClientRecord r,
            boolean keepShown) {
        if (r != null) {
            if (!keepShown && r.stopped) {
                if (r.activity.mFinished) {
                    // If we are finishing, we won't call onResume() in certain
                    // cases.  So here we likewise don't want to call onStop()
                    // if the activity isn't resumed.
                    return;
                }
                RuntimeException e = new RuntimeException(
                        "Performing stop of activity that is not resumed: "
                        + r.intent.getComponent().toShortString());
                Slog.e(TAG, e.getMessage(), e);
            }

            if (!keepShown) {
                try {
                    // Now we are idle.
                    r.activity.performStop();
                } catch (Exception e) {
                    if (!mInstrumentation.onException(r.activity, e)) {
                        throw new RuntimeException(
                                "Unable to stop activity "
                                + r.intent.getComponent().toShortString()
                                + ": " + e.toString(), e);
                    }
                }
                r.stopped = true;
            }

            r.paused = true;
        }
    }

	final void handleResumeActivity(IBinder token, boolean clearHide,
			boolean isForward) {
		//		System.out.println("in handleResumeActivity");
		ActivityClientRecord r = performResumeActivity(token, clearHide);

		if (r != null) {
		    // We are going to make the Activity visible to the screen.
		    long start = System.currentTimeMillis();
		    Log.w(TAG,"Begin to resume Activity, make it visible on the screen: " + r.activity.getComponentName().getClassName() + " at time: " + start);
			Context.getSystemContext().getActivityManager().mCurActivity = r.activity;
			final Activity a = r.activity;

			final int forwardBit = isForward ? WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION
					: 0;

			// If the window hasn't yet been added to the window manager,
			// and this guy didn't finish itself or start another activity,
			// then go ahead and add the window.
			boolean willBeVisible = !a.mStartedActivity;
			if (!willBeVisible) {
				//				willBeVisible = ActivityManager.getDefault()
				//						.willActivityBeVisible(a.getActivityToken());
			}
			if (r.window == null && !a.mFinished && willBeVisible) {
				r.window = r.activity.getWindow();
				View decor = r.window.getDecorView();
				decor.zIndex = r.activity.mZIndex;
				decor.setVisibility(View.INVISIBLE);
				ViewManager wm = a.getWindowManager();
				WindowManager.LayoutParams l = r.window.getAttributes();
				a.mDecor = decor;
				l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
				l.softInputMode |= forwardBit;
				if (a.mVisibleFromClient) {
					a.mWindowAdded = true;
					wm.addView(decor, l);
				}

				// If the window has already been added, but during resume
				// we started another activity, then don't yet make the
				// window visible.
			} else if (!willBeVisible) {
				r.hideForNow = true;
			}

			// The window is now visible if it has been added, we are not
			// simply finishing, and we are not starting another activity.
			if (!r.activity.mFinished && willBeVisible
					&& r.activity.mDecor != null && !r.hideForNow) {
				WindowManager.LayoutParams l = r.window.getAttributes();
				if ((l.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION) != forwardBit) {
					l.softInputMode = (l.softInputMode & (~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION))
							| forwardBit;
					if (r.activity.mVisibleFromClient) {
						ViewManager wm = a.getWindowManager();
						View decor = r.window.getDecorView();
						wm.updateViewLayout(decor, l);
					}
				}
				r.activity.mVisibleFromServer = true;
				mNumVisibleActivities++;
				if (r.activity.mVisibleFromClient) {
					r.activity.makeVisible();
				}
			}

			r.nextIdle = mNewActivities;
			mNewActivities = r;
			queueIdle();
			long end = System.currentTimeMillis();
			Log.w(TAG,"Finish to resume Activity, now it is visible on the screen " + r.activity.getComponentName().getClassName() + " at time: " + end);
			Log.w(TAG,"Activity, now it is visible on the screen " + r.activity.getComponentName().getClassName() + " used time:" + (end-start) + "ms");
		} else {
			// If an exception was thrown when trying to resume, then
			// just end this activity.
			((ActivityManager) Context.getSystemContext().getSystemService(
					Context.ACTIVITY_SERVICE)).finishActivity(token,
					Activity.RESULT_CANCELED, null);
		}
		//		System.out.println("handleResumeActivity Over");
	}

    public final boolean queueIdle() {
        ActivityClientRecord a = mNewActivities;
        if (a != null) {
            mNewActivities = null;
            ActivityManager am = ((ActivityManager) Context.getSystemContext().getSystemService(Context.ACTIVITY_SERVICE));
            ActivityClientRecord prev;
            do {
                if (a.activity != null && !a.activity.mFinished) {
                    am.activityIdle(a.token, a.createdConfig);
                    a.createdConfig = null;
                }
                prev = a;
                a = a.nextIdle;
                prev.nextIdle = null;
            } while (a != null);
        }
        //ensureJitEnabled();
        return false;
    }

	 private void handleCreateService(CreateServiceData data) {
	        // If we are getting ready to gc after going to the background, well
	        // we are back active so skip it.
//	        unscheduleGcIdler();

//	        LoadedApk packageInfo = getPackageInfoNoCheck(
//	                data.info.applicationInfo, data.compatInfo);
	        Service service = null;
	        try {
//	            java.lang.ClassLoader cl = packageInfo.getClassLoader();
	            service = (Service) Class.forName(data.info.name).newInstance();
	        } catch (Exception e) {
	        	System.out.println(e.getMessage());
	        }

	        try {
//	            if (localLOGV) Slog.v(TAG, "Creating service " + data.info.name);

	            Context context = Context.getSystemContext().createPackageContext(data.info.packageName, this);
	            service.attach(context, this, data.info.name, data.token,
	            		 Context.getSystemContext().getActivityManager());
	            Context.setOuterContext(service);
	            System.out.println("service context in AT is:"+service.icount);
	            service.onCreate();
	            mServices.put(data.token, service);
//	            try {
//	                ActivityManagerNative.getDefault().serviceDoneExecuting(
//	                        data.token, 0, 0, 0);
//	            } catch (RemoteException e) {
//	                // nothing to do.
//	            }
	        } catch (Exception e) {
	        	System.out.println(e.getMessage());

	        }
	    }
	 private void handleServiceArgs(ServiceArgsData data) {
	        Service s = mServices.get(data.token);
	        if (s != null) {

	                if (data.args != null) {
	                    data.args.setExtrasClassLoader(s.getClassLoader());
	                }
	                int res;
//	                if (!data.taskRemoved) {
	                    res = s.onStartCommand(data.args, data.flags, data.startId);
//	                } else {
//	                    s.onTaskRemoved(data.args);
//	                    res = Service.START_TASK_REMOVED_COMPLETE;
//	                }

//	                QueuedWork.waitToFinish();

//	                try {
//	                    ActivityManagerNative.getDefault().serviceDoneExecuting(
//	                            data.token, 1, data.startId, res);
//	                } catch (RemoteException e) {
//	                    // nothing to do.
//	                }
//	                ensureJitEnabled();
//
	        }
	    }

	 private void handleBindService(BindServiceData data) {
	        Service s = mServices.get(data.token);
	        if (s != null) {
	                data.intent.setExtrasClassLoader(s.getClassLoader());

	                    if (!data.rebind) {
	                        IBinder binder = s.onBind(data.intent);
	                        Context.getSystemContext().getActivityManager().publishService(
	                                data.token, data.intent, binder);
	                    } else {
	                        s.onRebind(data.intent);
//	                        Context.getSystemContext().getActivityManager().serviceDoneExecuting(
//	                                data.token, 0, 0, 0);
	                    }
//	                    ensureJitEnabled();

	        }
	    }

//	private void unscheduleGcIdler() {
//		if (mGcIdlerScheduled) {
//            mGcIdlerScheduled = false;
//            Looper.myQueue().removeIdleHandler(mGcIdler);
//        }
//        mH.removeMessages(H.GC_WHEN_IDLE);
//
//	}

	    public final ActivityInfo resolveActivityInfo(Intent intent) {
	        ActivityInfo aInfo = intent.resolveActivityInfo(
	                mInitialApplication.getPackageManager(), PackageManager.GET_SHARED_LIBRARY_FILES);
	        if (aInfo == null) {
	            // Throw an exception.
	            Instrumentation.checkStartActivityResult(
	                    ActivityManager.START_CLASS_NOT_FOUND, intent);
	        }
	        return aInfo;
	    }

	    public final Activity startActivityNow(Activity parent, String id,
	        Intent intent, ActivityInfo activityInfo, IBinder token, Bundle state,
	        Object lastNonConfigurationInstance) {
	        ActivityClientRecord r = new ActivityClientRecord();
	            r.token = token;
	            r.ident = 0;
	            r.intent = intent;
	            r.state = state;
	            r.parent = parent;
	            r.embeddedID = id;
	            r.activityInfo = activityInfo;
	            r.lastNonConfigurationInstance = lastNonConfigurationInstance;
	        return performLaunchActivity(r, null);
	    }

	public final ActivityClientRecord performResumeActivity(IBinder token,
			boolean clearHide) {
		//		System.out.println("in performResumeActivity");
		ActivityClientRecord r = mActivities.get(token);
		if (r != null && !r.activity.mFinished) {
			if (clearHide) {
				r.hideForNow = false;
				r.activity.mStartedActivity = false;
			}
			
            r.activity.performResume();
            r.paused = false;
            r.stopped = false;
            r.state = null;
		}
		//		System.out.println("performResumeActivity Over");
		return r;
	}


	private static final class ActivityClientRecord {
		public List<ResultInfo> pendingResults;
		IBinder token;
		int ident;
		Intent intent;
		Bundle state;
		Activity activity;
		Window window;
		Activity parent;
		String embeddedID;
		Object lastNonConfigurationInstance;
		HashMap<String, Object> lastNonConfigurationChildInstances;
		boolean paused;
		boolean stopped;
		boolean hideForNow;
		Configuration newConfig;
		Configuration createdConfig;
		ActivityClientRecord nextIdle;

		ActivityInfo activityInfo;
		LoadedApk packageInfo;

		List<Intent> pendingIntents;

		boolean startsNotResumed;
		boolean isForward;

		ActivityClientRecord() {
			parent = null;
			embeddedID = null;
			paused = false;
			stopped = false;
			hideForNow = false;
			nextIdle = null;
		}

		public String toString() {
			ComponentName componentName = intent.getComponent();
			return "ActivityRecord{"
					//+ Integer.toHexString(System.identityHashCode(this))
					+ " token="
					+ token
					+ " "
					+ (componentName == null ? "no component name"
							: componentName.toShortString()) + "}";
		}
	}

	private static final class ResultData {
		IBinder token;
		List<ResultInfo> results;

		public String toString() {
			System.out.println("ResultData.toString()");
			return "ResultData{token=" + token + " results" + results + "}";
		}
	}

	static final class CreateServiceData {
        IBinder token;
        ServiceInfo info;
        //CompatibilityInfo compatInfo;
        Intent intent;
        public String toString() {
            return "CreateServiceData{token=" + token + " className="
            + info.name + " packageName=" + info.packageName
            + " intent=" + intent + "}";
        }
    }

	static final class BindServiceData {
        IBinder token;
        Intent intent;
        boolean rebind;
        public String toString() {
            return "BindServiceData{token=" + token + " intent=" + intent + "}";
        }
    }

	static final class ServiceArgsData {
        IBinder token;
        int startId;
        int flags;
        Intent args;
        public String toString() {
            return "ServiceArgsData{token=" + token + " startId=" + startId
            + " args=" + args + "}";
        }
    }

	public final void sendActivityResult(IBinder token, String id,
			int requestCode, int resultCode, Intent data) {
		//		if (DEBUG_RESULTS)
		System.out.println(TAG + "sendActivityResult: id=" + id + " req="
				+ requestCode + " res=" + resultCode + " data=");
		ArrayList<ResultInfo> list = new ArrayList<ResultInfo>();
		list.add(new ResultInfo(id, requestCode, resultCode, data));
		mAppThread.scheduleSendResult(token, list);
	}

	private final void handleDestroyActivity(IBinder token, boolean finishing,
			int configChanges, boolean getNonConfigInstance) {
		ActivityClientRecord r = performDestroyActivity(token, finishing,
				configChanges, getNonConfigInstance);
		if (r != null) {
			WindowManager wm = r.activity.getWindowManager();
			View v = r.activity.mDecor;
			if (v != null) {
				if (r.activity.mVisibleFromServer) {
					mNumVisibleActivities--;
				}
				//				IBinder wtoken = v.getWindowToken();
				if (r.activity.mWindowAdded) {
					wm.removeViewImmediate(v);
				}
				//				if (wtoken != null) {
				//					WindowManagerImpl.getDefault().closeAll(wtoken,
				//							r.activity.getClass().getName(), "Activity");
				//				}
				r.activity.mDecor = null;
			}
			//			WindowManagerImpl.getDefault().closeAll(token,
			//					r.activity.getClass().getName(), "Activity");

			// Mocked out contexts won't be participating in the normal
			// process lifecycle, but if we're running with a proper
			// ApplicationContext we need to have it tear down things
			// cleanly.
			//			Context c = r.activity.getBaseContext();
			//			if (c instanceof ContextImpl) {
			//				((ContextImpl) c).scheduleFinalCleanup(r.activity.getClass()
			//						.getName(), "Activity");
			//			}
		}
		if (finishing) {
			Context.getSystemContext().getActivityManager()
					.activityDestroyed(token);
		}
	}

    public final ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing) {
        return performDestroyActivity(token, finishing, 0, false);
    }

	private final ActivityClientRecord performDestroyActivity(IBinder token,
			boolean finishing, int configChanges, boolean getNonConfigInstance) {
		ActivityClientRecord r = mActivities.get(token);
		if (r != null) {
		    Log.i(TAG, "Performing finish of " + r.activityInfo.name);
			if (finishing) {
				r.activity.mFinished = true;
			}
			if (!r.paused) {
				r.activity.mCalled = false;
				mInstrumentation.callActivityOnPause(r.activity);
				if (!r.activity.mCalled) {
					System.out
							.println("Activity did not call through to super.onPause()");
				}
				r.paused = true;
			}
			if (!r.stopped) {
				r.activity.performStop();
				r.stopped = true;
			}
			r.activity.mCalled = false;
			r.activity.onDestroy();
			if (!r.activity.mCalled) {
				System.out
						.println("Activity did not call through to super.onDestroy()");
			}
			// destroy the DecorView
			if (r.window != null) {
				r.window.closeAllPanels();
				r.window.getDecorView().destroy();
			}
		}
		mActivities.remove(token);

		return r;
	}

	private final void handleSendResult(ResultData res) {
		ActivityClientRecord r = mActivities.get(res.token);
		System.out.println(TAG + "Handling send result to "
				+ r.activityInfo.name);
		if (r != null) {
			final boolean resumed = !r.paused;
			if (!r.activity.mFinished && r.activity.mDecor != null
					&& r.hideForNow && resumed) {
				// We had hidden the activity because it started another
				// one...  we have gotten a result back and we are not
				// paused, so make sure our window is visible.
				updateVisibility(r, true);
			}
			if (resumed) {
				// Now we are idle.
				r.activity.mCalled = false;
				mInstrumentation.callActivityOnPause(r.activity);
				if (!r.activity.mCalled) {
					System.out
							.println("Activity did not call through to super.onPause()");
				}
			}
			deliverResults(r, res.results);
			if (resumed) {
				mInstrumentation.callActivityOnResume(r.activity);
			}
		}
	}

	private final void updateVisibility(ActivityClientRecord r, boolean show) {
		View v = r.activity.mDecor;
		if (v != null) {
			if (show) {
				if (!r.activity.mVisibleFromServer) {
					r.activity.mVisibleFromServer = true;
					mNumVisibleActivities++;
					if (r.activity.mVisibleFromClient) {
						r.activity.makeVisible();
					}
				}
			} else {
				if (r.activity.mVisibleFromServer) {
					r.activity.mVisibleFromServer = false;
					mNumVisibleActivities--;
					v.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

    final void performRestartActivity(IBinder token) {
        ActivityClientRecord r = mActivities.get(token);
        if (r.stopped) {
//            r.activity.performRestart();
            r.stopped = false;
        }
    }

    private final void handleWindowVisibility(IBinder token, boolean show) {
        ActivityClientRecord r = mActivities.get(token);
        if (!show && !r.stopped) {
            performStopActivityInner(r, show);
        } else if (show && r.stopped) {
            // If we are getting ready to gc after going to the background, well
            // we are back active so skip it.
//            unscheduleGcIdler();

//            r.activity.performRestart();
            r.stopped = false;
        }
        if (r.activity.mDecor != null) {
//            if (Config.LOGV) Slog.v(
//                TAG, "Handle window " + r + " visibility: " + show);
            updateVisibility(r, show);
        }
    }

	private final void deliverResults(ActivityClientRecord r,
			List<ResultInfo> results) {
		final int N = results.size();
		for (int i = 0; i < N; i++) {
			ResultInfo ri = results.get(i);
			if (ri.mData != null) {
				ri.mData.setExtrasClassLoader(r.activity.getClassLoader());
			
			System.out.println(TAG + "Delivering result to activity "
					+ r.activityInfo.name + " : " + ri.mData.getAction());
			}
			r.activity.dispatchActivityResult(ri.mResultWho, ri.mRequestCode,
					ri.mResultCode, ri.mData);
		}
	}

    public Application getApplication() {
        return mInitialApplication;
    }
    
    /*package*/ final void finishInstrumentation(int resultCode, Bundle results) {
        Context.getSystemContext().getActivityManager().finishInstrumentation(mAppThread, resultCode, results);
    }
    
    public Instrumentation getInstrumentation() {
        return mInstrumentation;
    }
    
}
