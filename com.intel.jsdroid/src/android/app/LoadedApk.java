package android.app;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Handler;

public final class LoadedApk {
	
    private final HashMap<Context, HashMap<BroadcastReceiver, IIntentReceiver>> mReceivers
    	= new HashMap<Context, HashMap<BroadcastReceiver, IIntentReceiver>>();
    private final HashMap<Context, HashMap<BroadcastReceiver, IIntentReceiver>> mUnregisteredReceivers
    	= new HashMap<Context, HashMap<BroadcastReceiver, IIntentReceiver>>();
    
    private ActivityThread mActivityThread;
    private ClassLoader mClassLoader;
    private final ClassLoader mBaseClassLoader = null;
    private ApplicationInfo mApplicationInfo;
    private String mPackageName;
    Resources mResources;
    private Application mApplication;
    private final String mAppDir = null;
    private final String[] mSharedLibraries = null;
    private final boolean mIncludeCode = false;

    public LoadedApk(){
    	
    }
    
    public LoadedApk(ActivityThread activityThread, ApplicationInfo aInfo) {
        mActivityThread = activityThread;
        mApplicationInfo = aInfo;
        mPackageName = aInfo.packageName;
    }
    
    public String getPackageName() {
        return mPackageName;
    }
    
    public Application getApplication() {
        return mApplication;
    }
    
    public Application makeApplication(boolean forceDefaultAppClass,
            Instrumentation instrumentation) {
        if (mApplication != null) {
            return mApplication;
        }

        Application app = null;

        String appClass = mApplicationInfo.className;
        if (forceDefaultAppClass || (appClass == null)) {
            appClass = "android.app.Application";
        }

        try {
        	Context context = Context.getSystemContext().createPackageContext(
        			mApplicationInfo.packageName, mActivityThread);
            app = mActivityThread.mInstrumentation.newApplication(appClass, context);
            context.setOuterContext(app);
        } catch (Exception e) {
            if (!mActivityThread.mInstrumentation.onException(app, e)) {
                throw new RuntimeException(
                    "Unable to instantiate application " + appClass
                    + ": " + e.toString(), e);
            }
        }
        mActivityThread.mAllApplications.add(app);
        mApplication = app;

        if (instrumentation != null) {
            try {
                instrumentation.callApplicationOnCreate(app);
            } catch (Exception e) {
                if (!instrumentation.onException(app, e)) {
                    throw new RuntimeException(
                        "Unable to create application " + app.getClass().getName()
                        + ": " + e.toString(), e);
                }
            }
        }
        
        return app;
    }
    
    public IIntentReceiver getReceiverDispatcher(BroadcastReceiver r,
            Context context, Handler handler, boolean registered) {
        synchronized (mReceivers) {
        	IIntentReceiver rd = null;
            HashMap<BroadcastReceiver, IIntentReceiver> map = null;
            if (registered) {
                map = mReceivers.get(context);
                if (map != null) {
                    rd = map.get(r);
                }
            }
            if (rd == null) {
                rd = new IIntentReceiver(r, context, handler,registered);
                if (registered) {
                    if (map == null) {
                        map = new HashMap<BroadcastReceiver, IIntentReceiver>();
                        mReceivers.put(context, map);
                    }
                    map.put(r, rd);
                }
            } 
            return rd;
        }
    }
    
}
