
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

package android.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.LoadedApk;
import android.app.NotificationManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser.NewPermissionInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewRoot;
import android.view.Window;
import android.view.WindowManagerImpl;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/**
 * Interface to global information about an application environment. This is an
 * abstract class whose implementation is provided by the Android system. It
 * allows access to application-specific resources and classes, as well as
 * up-calls for application-level operations such as launching activities,
 * broadcasting and receiving intents, etc.
 */
public class Context {
    private final static String TAG = "Context";
    private final static boolean DEBUG = false;
	public static  Context mOuterContext;
	private int mThemeResource = 0;
	private Resources.Theme mTheme = null;
	private IBinder mActivityToken = null;
	private Resources mResources;
    private AssetManager mAssets;
	private PackageInfo mPackageInfo;
	public static int count = 0;
	public int icount = 0;
	public ActivityThread mActivityThread;
	public LoadedApk mReceiverInfo=new LoadedApk();
	public LoadedApk mLoadedApk;
	private static boolean DEBUG_PROVIDER = true;

    private File mPreferencesDir;
    private File mFilesDir;
    private File mCacheDir;
    private File mExternalFilesDir;
    private File mExternalCacheDir;
    /**
     * Flag for use with {@link #createPackageContext}: include the application
     * code with the context.  This means loading code into the caller's
     * process, so that {@link #getClassLoader()} can be used to instantiate
     * the application's classes.  Setting this flags imposes security
     * restrictions on what application context you can access; if the
     * requested application can not be safely loaded into your process,
     * java.lang.SecurityException will be thrown.  If this flag is not set,
     * there will be no restrictions on the packages that can be loaded,
     * but {@link #getClassLoader} will always return the default system
     * class loader.
     */
    public static final int CONTEXT_INCLUDE_CODE = 0x00000001;

    /**
     * Flag for use with {@link #createPackageContext}: ignore any security
     * restrictions on the Context being requested, allowing it to always
     * be loaded.  For use with {@link #CONTEXT_INCLUDE_CODE} to allow code
     * to be loaded into a process even when it isn't safe to do so.  Use
     * with extreme care!
     */
    public static final int CONTEXT_IGNORE_SECURITY = 0x00000002;

    /**
     * Flag for use with {@link #createPackageContext}: a restricted context may
     * disable specific features. For instance, a View associated with a restricted
     * context would ignore particular XML attributes.
     */
    public static final int CONTEXT_RESTRICTED = 0x00000004;

//    static{
//    	try {
//    		//Looper.prepareMainLooper();
//			mMainThread = (ActivityThread) Class.forName(
//			"android.app.ActivityThread").newInstance();
//			mMainThread.main("com.intel.jsdroid.provider.test");
//		} catch(Exception e){
//			System.out.println("Could not load mMainThread");
//		}
//    }
	private ContentResolver mContentResolver;



	/**
	 * Flag for {@link #bindService}: automatically create the service as long
	 * as the binding exists.  Note that while this will create the service,
	 * its {@link android.app.Service#onStartCommand}
	 * method will still only be called due to an
	 * explicit call to {@link #startService}.  Even without that, though,
	 * this still provides you with access to the service object while the
	 * service is created.
	 *
	 * <p>Specifying this flag also tells the system to treat the service
	 * as being as important as your own process -- that is, when deciding
	 * which process should be killed to free memory, the service will only
	 * be considered a candidate as long as the processes of any such bindings
	 * is also a candidate to be killed.  This is to avoid situations where
	 * the service is being continually created and killed due to low memory.
	 */
	public static final int BIND_AUTO_CREATE = 0x0001;

	/**
	 * Flag for {@link #bindService}: include debugging help for mismatched
	 * calls to unbind.  When this flag is set, the callstack of the following
	 * {@link #unbindService} call is retained, to be printed if a later
	 * incorrect unbind call is made.  Note that doing this requires retaining
	 * information about the binding that was made for the lifetime of the app,
	 * resulting in a leak -- this should only be used for debugging.
	 */
	public static final int BIND_DEBUG_UNBIND = 0x0002;

	/**
	 * Flag for {@link #bindService}: don't allow this binding to raise
	 * the target service's process to the foreground scheduling priority.
	 * It will still be raised to the at least the same memory priority
	 * as the client (so that its process will not be killable in any
	 * situation where the client is not killable), but for CPU scheduling
	 * purposes it may be left in the background.  This only has an impact
	 * in the situation where the binding client is a foreground process
	 * and the target service is in a background process.
	 */
	public static final int BIND_NOT_FOREGROUND = 0x0004;

    public final Context getOuterContext() {
        return mOuterContext;
    }

	public Context() {
		this.mOuterContext = this;
		count++;
		icount = count;
		Log.d(TAG, "Context is:"+icount);
	}

	private static Context createSystemContext() {
		Context c = new Context();
		c.init(null, Resources.getSystem(),null);
		return c;
	}

	public void setLoadedApk(LoadedApk loadedApk) {
		this.mLoadedApk = loadedApk;
	}

    public Context getApplicationContext() {
        return (mLoadedApk != null) ?
        		mLoadedApk.getApplication() : mActivityThread.getApplication();
    }

	// TODO temporarily put it here
	private static Context mSystemContext = null;

	public static Context getSystemContext() {
		if (mSystemContext == null) {
			mSystemContext = Context.createSystemContext();
			if(DEBUG_PROVIDER) Log.i(TAG, "System context is:"+mSystemContext.icount);
		}
		return mSystemContext;
	}

//	public Context createPackageContext(String packageName)
//			throws NameNotFoundException {
//		PackageInfo pi = ((PackageManager) Context.getSystemContext()
//				.getSystemService(PACKAGE_SERVICE)).getPackageInfo(packageName,
//				0);
//		if (pi != null) {
//			Context c = new Context();
//			c.init(pi, ((PackageManager) Context.getSystemContext()
//					.getSystemService(PACKAGE_SERVICE))
//					.getPackageResources(packageName));
//			if (c.mResources != null)
//				return c;
//		}
//
//		throw new NameNotFoundException("Application package " + packageName
//				+ " not found");
//	}

    /**
     * global button events from screen_simple_tizen.xml
     * global button is created in Window.generateLayout.
     * This events support backKey and menuKey.
     * @param v Button
     */
    public void onKeyClick(View v) {
        if (v.getId() == Window.ID_BACK_KEY) {
            ViewRoot.simulateBack();
        }
        if (v.getId() == Window.ID_MENU_KEY) {
            ViewRoot.simulateMenu();
        }
    }

	public static void setOuterContext(Context context) {
        mOuterContext = context;
    }

	public Context createPackageContext(String packageName,ActivityThread thread)
		throws NameNotFoundException {

		PackageInfo pi = ((PackageManager) Context.getSystemContext()
				.getSystemService(PACKAGE_SERVICE)).getPackageInfo(packageName,
				0);
		if (pi != null) {
			Context c = new Context();
			c.init(pi, ((PackageManager) Context.getSystemContext()
					.getSystemService(PACKAGE_SERVICE))
					.getPackageResources(packageName),thread);

			if (c.mResources != null)
				return c;
		}

		throw new NameNotFoundException("Application package " + packageName
			+ " not found");
	}
	
	    public Context createPackageContext(String packageName, int flags)
	        throws PackageManager.NameNotFoundException {
	        return createPackageContext(packageName, mActivityThread);
	    }
	

//	final void init(PackageInfo pi, Resources res) {
//		mPackageInfo = pi;
//		mResources = res;
//		System.out.println("!!!!!!!!ActivityThread is not created!!!");
//	}
	public final void init(PackageInfo pi,Resources res,ActivityThread thread){
		mPackageInfo = pi;
		mResources = res;
		mActivityThread = thread;
		mContentResolver = new ApplicationContentResolver(this, thread);
		if(DEBUG_PROVIDER){
			if(mContentResolver==null){
				Log.e(TAG, "!ApplicationContentResolver!NULL");
			}else{
				Log.d(TAG, "!ApplicationContentResolver!NOT NULL");
			}
		}

	}
    public void init(PackageInfo packageInfo,
            IBinder activityToken, ActivityThread mainThread,
            Resources container) {
        mPackageInfo = packageInfo;
        mResources = container;
        mActivityThread = mainThread;
        mContentResolver = new ApplicationContentResolver(this, mainThread);
	}
//	final void init(Resources resources, ActivityThread mainThread) {
//        mPackageInfo = null;
//		mResources = resources;
//        //mMainThread = mainThread;
//        mContentResolver = new ApplicationContentResolver(this, mainThread);
//    }

	/**
	 * Use with {@link #getSystemService} to retrieve a
	 * {@link android.view.LayoutInflater} for inflating layout resources in
	 * this context.
	 *
	 * @see #getSystemService
	 * @see android.view.LayoutInflater
	 */
	public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
	public static final String ACTIVITY_SERVICE = "activity";
	public static final String PACKAGE_SERVICE = "package";
    public static final String ALARM_SERVICE = "alarm";
    public static final String KEYGUARD_SERVICE = "keyguard";
    public static final String NOTIFICATION_SERVICE = "notification";
    public static final String POWER_SERVICE = "power";
    public static final String WINDOW_SERVICE = "window";
    public static final String LOCATION_SERVICE="location";

	private LayoutInflater mLayoutInflater = null;
	private PackageManager mPackageManager = null;
	private static ActivityManager mActivityManager = null;
	private static AlarmManager mAlarmManager=null;
	private static KeyguardManager mKeyguardManager=null;
	private static NotificationManager mNotificationManager=null;
	private static PowerManager mPowerManager=null;
	private static WindowManagerImpl mWindowManager=null;
	private static LocationManager mLocationManager=null;
    private static final HashMap<String, SharedPreferencesJ2SImpl> sSharedPrefs =
        new HashMap<String, SharedPreferencesJ2SImpl>();

	/**
	 * Indicates whether this Context is restricted.
	 *
	 * @return True if this Context is restricted, false otherwise.
	 *
	 * @see #CONTEXT_RESTRICTED
	 */
	public boolean isRestricted() {
		return false;
	}

	/**
	 * Set the base theme for this context. Note that this should be called
	 * before any views are instantiated in the Context (for example before
	 * calling {@link android.app.Activity#setContentView} or
	 * {@link android.view.LayoutInflater#inflate}).
	 *
	 * @param resid
	 *            The style resource describing the theme.
	 */
	public void setTheme(int resid) {
		mThemeResource = resid;
	}

	public Intent registerReceiver(BroadcastReceiver receiver,IntentFilter filter){
    	return registerReceiver(receiver, filter,null,null);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
            String broadcastPermission, Handler scheduler) {
        return registerReceiverInternal(receiver, filter, broadcastPermission,
            scheduler, this);
    }

    private Intent registerReceiverInternal(BroadcastReceiver receiver,
            IntentFilter filter, String broadcastPermission,
            Handler scheduler, Context context) {
        if (receiver != null) {
            if (context != null) {
                if (scheduler == null) {
                    scheduler = mActivityThread.getHandler();
                }
            }
        }
        IIntentReceiver rd=mReceiverInfo.getReceiverDispatcher(receiver, context, scheduler, true);

        return mActivityManager.registerReceiver(mActivityThread.getApplicationThread(),rd, filter,broadcastPermission);
    }

    public  void unregisterReceiver(BroadcastReceiver receiver){
    	IIntentReceiver rd=mReceiverInfo.getReceiverDispatcher(receiver, this, mActivityThread.getHandler(), true);

    	mActivityManager.unregisterReceiver(rd);
    }

    public final void sendBroadcast(Intent intent){
    	mActivityManager.broadcastIntent(
    			mActivityThread.getApplicationThread(), intent, null, null,
                Activity.RESULT_OK, null, null, null, false, false);
    }

    public final void sendBroadcast(Intent intent,
            String receiverPermission){
    	mActivityManager.broadcastIntent(
    			mActivityThread.getApplicationThread(), intent, null, null,
                Activity.RESULT_OK, null, null, receiverPermission, false, false);
    }

    public final void sendOrderedBroadcast(Intent intent,
            String receiverPermission) {
        if (mActivityThread == null) {
            mActivityThread = ActivityThread.currentActivityThread();
        }
        mActivityManager.broadcastIntent(
                mActivityThread.getApplicationThread(), intent, null, null,
                Activity.RESULT_OK, null, null, receiverPermission, true, false);
    }

    public final void sendOrderedBroadcast(Intent intent,
            String receiverPermission, BroadcastReceiver resultReceiver,
            Handler scheduler, int initialCode, String initialData,
            Bundle initialExtras){
        if (mActivityThread == null) {
            mActivityThread = ActivityThread.currentActivityThread();
        }
    	mActivityManager.broadcastIntent(
    			mActivityThread.getApplicationThread(), intent, null, null,
                Activity.RESULT_OK, null, null, receiverPermission, true, false);
    }
    public void sendStickyBroadcast(Intent intent){
    	mActivityManager.broadcastIntent(
    			mActivityThread.getApplicationThread(), intent, null, null,
                Activity.RESULT_OK, null, null, null, false, true);
    }
    public final void sendStickyOrderedBroadcast(Intent intent,
            BroadcastReceiver resultReceiver,
            Handler scheduler, int initialCode, String initialData,
            Bundle initialExtras){
    	mActivityManager.broadcastIntent(
    			mActivityThread.getApplicationThread(), intent, null, null,
                Activity.RESULT_OK, null, null, null, true, true);
    }
    public final void removeStickyBroadcast(Intent intent){
    	//we don't need this now
    }
	/**
	 * Return the Theme object associated with this Context.
	 */
	public Resources.Theme getTheme() {
		if (mTheme == null) {
			if (mThemeResource == 0) {
				mThemeResource = com.android.internal.R.style.Theme;
			}
			mTheme = mResources.newTheme();
//			System.err
//					.println("calling mTheme.applyStyle! This will be called multiple times ?!");
			mTheme.applyStyle(mThemeResource, true);
		}
		return mTheme;
	}

	/**
	* Retrieve styled attribute information in this Context's theme.  See
	* {@link Resources.Theme#obtainStyledAttributes(int[])}
	* for more information.
	*
	* @see Resources.Theme#obtainStyledAttributes(int[])
	*/
	public final TypedArray obtainStyledAttributes(int[] attrs) {
		return getTheme().obtainStyledAttributes(attrs);
	}

	/**
	 * Retrieve styled attribute information in this Context's theme.  See
	 * {@link Resources.Theme#obtainStyledAttributes(int, int[])}
	 * for more information.
	 *
	 * @see Resources.Theme#obtainStyledAttributes(int, int[])
	 */
	public final TypedArray obtainStyledAttributes(int resid, int[] attrs) {
		return getTheme().obtainStyledAttributes(resid, attrs);
	}

	/**
	 * Retrieve styled attribute information in this Context's theme.  See
	 * {@link Resources.Theme#obtainStyledAttributes(AttributeSet, int[], int, int)}
	 * for more information.
	 *
	 * @see Resources.Theme#obtainStyledAttributes(AttributeSet, int[], int, int)
	 */
	public final TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs) {
		return getTheme().obtainStyledAttributes(set, attrs, 0, 0);
	}

	/**
	 * Retrieve styled attribute information in this Context's theme.  See
	 * {@link Resources.Theme#obtainStyledAttributes(AttributeSet, int[], int, int)}
	 * for more information.
	 *
	 * @see Resources.Theme#obtainStyledAttributes(AttributeSet, int[], int, int)
	 */
	public final TypedArray obtainStyledAttributes(AttributeSet set,
			int[] attrs, int defStyleAttr, int defStyleRes) {
		return getTheme().obtainStyledAttributes(set, attrs, defStyleAttr,
				defStyleRes);
	}

	public Resources getResources() {
		return mResources;
	}

	public PackageManager getPackageManager() {
		if (mPackageManager == null) {
			mPackageManager = (PackageManager) getSystemContext().getSystemService(PACKAGE_SERVICE);
		}

		return mPackageManager;
	}

	public ActivityManager getActivityManager() {
		if (mActivityManager != null) {
			return mActivityManager;
		}

		return (mActivityManager = new ActivityManager(this));
	}

	public ActivityThread getActivityThread(){
		if(mActivityThread != null){
			return mActivityThread;
		}
		return (mActivityThread = new ActivityThread());
	}

	/**
	 * Return a localized, styled CharSequence from the application's package's
	 * default string table.
	 *
	 * @param resId Resource id for the CharSequence text
	 */
	public final CharSequence getText(int resId) {
		return getResources().getText(resId);
	}

	/**
	 * Return a localized string from the application's package's
	 * default string table.
	 *
	 * @param resId Resource id for the string
	 */
	public final String getString(int resId) {
		return getResources().getString(resId);
	}

    /**
     * Return a localized formatted string from the application's package's
     * default string table, substituting the format arguments as defined in
     * {@link java.util.Formatter} and {@link java.lang.String#format}.
     *
     * @param resId Resource id for the format string
     * @param formatArgs The format arguments that will be used for substitution.
     */

    public final String getString(int resId, Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

	// EMMA TODO, we need to implement this function later
	public ClassLoader getClassLoader() {
	     return /*ClassLoader.getSystemClassLoader();*/null;
		//return null;// ClassLoader.getSystemClassLoader();
	}

	public Object getSystemService(String name) {
		if (LAYOUT_INFLATER_SERVICE.equals(name)) {
			LayoutInflater inflater = mLayoutInflater;
			if (inflater != null) {
				return inflater;
			}
			mLayoutInflater = inflater = new LayoutInflater(this);
			return inflater;
		} else if (ACTIVITY_SERVICE.equals(name)) {
			ActivityManager am = mActivityManager;
			if (am != null) {
				return am;
			}
			mActivityManager = am = new ActivityManager(this);
			return am;
		} else if (PACKAGE_SERVICE.equals(name)) {
			PackageManager pm = mPackageManager;
			if (pm != null)
				return pm;
			mPackageManager = pm = new PackageManager();
			return pm;
		} else if(ALARM_SERVICE.equals(name)){
			AlarmManager am= mAlarmManager;
			if (am != null) {
				return am;
			}
			mAlarmManager = am = new AlarmManager();
			return am;
		}else if(KEYGUARD_SERVICE.equals(name)){
			KeyguardManager km=mKeyguardManager;
			if(km!=null){
				return km;
			}
			mKeyguardManager=km=new KeyguardManager();
			return km;
		}else if(NOTIFICATION_SERVICE.equals(name)){
			NotificationManager nm=mNotificationManager;
			if(nm!=null){
				return nm;
			}
			mNotificationManager=nm=new NotificationManager();
			return nm;
		}else if(POWER_SERVICE.equals(name)){
			PowerManager pm=mPowerManager;
			if(pm!=null){
				return pm;
			}
			mPowerManager=pm=new PowerManager();
			return pm;
		}else if(WINDOW_SERVICE.equals(name)){
			WindowManagerImpl wm = mWindowManager;
			if(wm!=null){
				return wm;
			}
			mWindowManager=wm=new WindowManagerImpl();
			return wm;
		}else if (LOCATION_SERVICE.equals(name)) {
			LocationManager lm=mLocationManager;
			if(lm!=null){
				return lm;
			}
			mLocationManager=lm=new LocationManager();
			return lm;
		}
		return null;
	}

	public String getPackageName() {
		if (mPackageInfo != null) {
			return mPackageInfo.packageName;
		}
		throw new RuntimeException("Not supported in system context");
	}

    public void startActivity(Intent intent) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
            throw new AndroidRuntimeException(
                    "Calling startActivity() from outside of an Activity "
                            + " context requires the FLAG_ACTIVITY_NEW_TASK flag."
                            + " Is this really what you want?");
        }
        mActivityThread.getInstrumentation().execStartActivity(
                getOuterContext(), mActivityThread.getApplicationThread(), null, null, intent, -1);
    }

	public ComponentName startService(Intent service){

		service.setAllowFds(false);
        ComponentName cn = this.getActivityManager().startService(
            mActivityThread.getApplicationThread(), service,
            "");
        if (cn != null && cn.getPackageName().equals("!")) {
            throw new SecurityException(
                    "Not allowed to start service " + service
                    + " without permission " + cn.getClassName());
        }
        return cn;
	}

    /**
     * Request that a given application service be stopped.  If the service is
     * not running, nothing happens.  Otherwise it is stopped.  Note that calls
     * to startService() are not counted -- this stops the service no matter
     * how many times it was started.
     *
     * <p>Note that if a stopped service still has {@link ServiceConnection}
     * objects bound to it with the {@link #BIND_AUTO_CREATE} set, it will
     * not be destroyed until all of these bindings are removed.  See
     * the {@link android.app.Service} documentation for more details on a
     * service's lifecycle.
     *
     * <p>This function will throw {@link SecurityException} if you do not
     * have permission to stop the given service.
     *
     * @param service Description of the service to be stopped.  The Intent may
     *      specify either an explicit component name to start, or a logical
     *      description (action, category, etc) to match an
     *      {@link IntentFilter} published by a service.
     *
     * @return If there is a service matching the given Intent that is already
     * running, then it is stopped and true is returned; else false is returned.
     *
     * @throws SecurityException
     *
     * @see #startService
     */
    public boolean stopService(Intent service){
    	return true;
    }

	 public boolean bindService(Intent service, ServiceConnection conn,
	            int flags) {
//	        IServiceConnection sd;
//	        if (mPackageInfo != null) {
//	            sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(),
//	                    mMainThread.getHandler(), flags);
//	        } else {
//	            throw new RuntimeException("Not supported in system context");
//	        }
//	        IBinder token = getActivityToken();
//	        if (token == null && (flags&BIND_AUTO_CREATE) == 0 && mPackageInfo != null
//	            && mPackageInfo.getApplicationInfo().targetSdkVersion
//	            < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//	            flags |= BIND_WAIVE_PRIORITY;
//	         }
	        service.setAllowFds(false);
	        int res = this.getActivityManager().bindService(
	           mActivityThread.getApplicationThread(),
	           service, "",
	           conn, flags);
	        if (res < 0) {
	            throw new SecurityException(
	              "Not allowed to bind to service " + service);
	        }
	        return res != 0;
	    }


	    public void unbindService(ServiceConnection conn) {
	        if (mPackageInfo != null) {

	            	this.getActivityManager().unbindService(conn);
	        } else {
	            throw new RuntimeException("Not supported in system context");
	        }
	    }

	 final void setActivityToken(IBinder token) {
	        mActivityToken = token;
	    }

	  final IBinder getActivityToken() {
	        return mActivityToken;
	    }




	/**
	 * File creation mode: the default mode, where the created file can only
	 * be accessed by the calling application (or all applications sharing the
	 * same user ID).
	 * @see #MODE_WORLD_READABLE
	 * @see #MODE_WORLD_WRITEABLE
	 */
	public static final int MODE_PRIVATE = 0x0000;
	/**
	 * File creation mode: allow all other applications to have read access
	 * to the created file.
	 * @see #MODE_PRIVATE
	 * @see #MODE_WORLD_WRITEABLE
	 */
	public static final int MODE_WORLD_READABLE = 0x0001;
	/**
	 * File creation mode: allow all other applications to have write access
	 * to the created file.
	 * @see #MODE_PRIVATE
	 * @see #MODE_WORLD_READABLE
	 */
	public static final int MODE_WORLD_WRITEABLE = 0x0002;
	/**
	 * File creation mode: for use with {@link #openFileOutput}, if the file
	 * already exists then write data to the end of the existing file
	 * instead of erasing it.
	 * @see #openFileOutput
	 */
	public static final int MODE_APPEND = 0x8000;

	public FileInputStream openFileInput(String name) throws FileNotFoundException{
        File f = makeFilename(getFilesDir(), name);
        return new FileInputStream(f);
	}
    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException(
                "File " + name + " contains a path separator");
    }
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException{
        final boolean append = (mode&MODE_APPEND) != 0;
        File f = makeFilename(getFilesDir(), name);
        try {
            FileOutputStream fos = new FileOutputStream(f, append);
            return fos;
        } catch (FileNotFoundException e) {
        }
        File parent = f.getParentFile();
        parent.mkdir();
        FileOutputStream fos = new FileOutputStream(f, append);
        return fos;
	}

	public ContentResolver getContentResolver() {
		if(mContentResolver==null){
			mContentResolver = new ApplicationContentResolver(this,mActivityThread);
		}
		return mContentResolver;
	}
    public  class ApplicationContentResolver extends ContentResolver {
    	public ApplicationContentResolver(Context context){
    		super(context);
    		if(DEBUG_PROVIDER) Log.d(TAG, "ApplicationContentResolver created 0..."+context.icount);
    	}
        public ApplicationContentResolver(Context context, ActivityThread mainThread) {
            super(context);
            mMainThread = mainThread;
            if(DEBUG_PROVIDER){
                if(context==null){
                	System.out.println("context is null");
                }
                if(mMainThread==null){
                	Log.d(TAG, "mMainThread is null");
                }
            }
            if(DEBUG_PROVIDER) Log.d(TAG, "ApplicationContentResolver create 1..."+context.icount);
        }

        protected ContentProvider acquireProvider(Context context, String name) {
            if(DEBUG_PROVIDER) System.out.println("Context: Calling acquireProvider(Context context, String name)");
            return mMainThread.acquireProvider(context, name);
        }

        protected void removeProvider(Context context, String name) {
            mMainThread.removeExistingProvider(context, name);
        }

        protected ContentProvider acquireExistingProvider(Context context, String name) {
            return mMainThread.acquireExistingProvider(context, name);
        }

        public boolean releaseProvider(ContentProvider provider) {
            return mMainThread.releaseProvider(provider);
        }
        public int data = 0;
        public /*static*/ ActivityThread mMainThread;
    }

	public SQLiteDatabase openOrCreateDatabase(String name,
            int mode, CursorFactory factory)
	{
		//tqi3: Pname means package name to prevent different applications have the same name database, the web SQL database's name is set as <Package_Name>+"_"+<database_name> 
		String Pname = getPackageName(); 
		String path = "/data/data/"+Pname + "/data/" + name;
		SQLiteDatabase sqlitedatabase = SQLiteDatabase.openOrCreateDatabase(path, factory);

		return sqlitedatabase;
	}



	// There is no way to enumerate or delete the databases available for an origin from this API in web database.
	public boolean deleteDatabase(String name)
	{
		return false;
	}
    private File getDataDirFile() {
//        if (mPackageInfo != null) {
//            return mPackageInfo.getDataDirFile();
//        }
        if(mActivityThread!=null){
        	return new File(mActivityThread.currentPackageName());
        }
        throw new RuntimeException("Not supported in system context");
    }
    public File getFilesDir() throws RuntimeException {
        if (mFilesDir == null) {
            mFilesDir = new File(getDataDirFile(), "files");
        }
        if (!mFilesDir.exists()) {
            if(!mFilesDir.mkdirs()) {
                return null;
            }
        }
        return mFilesDir;
    
    }
    public File getCacheDir() {
		if (mCacheDir == null) {
			mCacheDir = new File(getDataDirFile(), "cache");
		}
		if (!mCacheDir.exists()) {
			if (!mCacheDir.mkdirs()) {
				Log.w(TAG, "Unable to create cache directory");
				return null;
			}
			FileUtils.setPermissions(mCacheDir.getPath(), FileUtils.S_IRWXU
					| FileUtils.S_IRWXG | FileUtils.S_IXOTH, -1, -1);
		}

		return mCacheDir;
    }
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferencesJ2SImpl sp;
        //File prefsFile;
        boolean needInitialLoad = false;
        synchronized (sSharedPrefs) {
            //use package as prefix
            name = getPackageName() + name;
            sp = sSharedPrefs.get(name);
            if (sp != null && !sp.hasFileChangedUnexpectedly()) {
                return sp;
            }
            //prefsFile = new File(name);
            if (sp == null) {
                sp = new SharedPreferencesJ2SImpl(name, mode, null);
                sSharedPrefs.put(name, sp);
                needInitialLoad = true;
            }
        }

        synchronized (sp) {
            if (needInitialLoad && sp.isLoaded()) {
                // lost the race to load; another thread handled it
                return sp;
            }
//            File backup = makeBackupFile(prefsFile);
//            if (backup.exists()) {
//                prefsFile.delete();
//                backup.renameTo(prefsFile);
//            }

            // Debugging
            //if (prefsFile.exists() && !prefsFile.canRead()) {
            //    Log.w(TAG, "Attempt to read preferences file " + prefsFile + " without permission");
            //}

//            Map map = null;
//            FileStatus stat = new FileStatus();
//            if (FileUtils.getFileStatus(prefsFile.getPath(), stat) && prefsFile.canRead()) {
//                try {
//                    FileInputStream str = new FileInputStream(prefsFile);
//                    map = XmlUtils.readMapXml(str);
//                    str.close();
//                } catch (org.xmlpull.v1.XmlPullParserException e) {
//                    Log.w(TAG, "getSharedPreferences", e);
//                } catch (FileNotFoundException e) {
//                    Log.w(TAG, "getSharedPreferences", e);
//                } catch (IOException e) {
//                    Log.w(TAG, "getSharedPreferences", e);
//                }
//            }
//            sp.replace(map);
        }
        return sp;
    }

    /**
     * j2s class implemented SharedPreferences
     * */
    private static final class SharedPreferencesJ2SImpl implements SharedPreferences {

        private String appName;
        private int mMode;
        //temp data
        private Map<String, Object> mMap;
        private boolean mLoaded = false;

        private static final Object mContent = new Object();
        private WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners;

        /**
         * MayLoon workaround: avoid method's args name become to a,b,c of inner class and js can't use
         * the args.
         */
        private Object returnVal = "";

        @SuppressWarnings("unchecked")
        public SharedPreferencesJ2SImpl(String name, int mode, Map initialContents) {
            // split by '@'
            appName = name + "@";
            mMode = mode;
            mLoaded = initialContents != null;
            mMap = initialContents != null ? initialContents : new HashMap<String, Object>();
            mListeners = new WeakHashMap<OnSharedPreferenceChangeListener, Object>();
            loadData();
        }

        //init mMap from localStorage by appName
        private void loadData() {
            /**
             * @j2sNative
             * var size = localStorage.length;
             * for (var i = 0; i < size; i++) {
             *     var key = localStorage.key(i);
             *     if (key.indexOf(this.appName) == 0) {
             *         var val = localStorage.getItem(key);
             *         key = key.substr(this.appName.length);
             *         this.mMap.put(key, val);
             *     }
             * }
             */{}
             mLoaded = true;
        }

        public String getString(String key, String defValue) {
            synchronized (this) {
                String v = (String) mMap.get(key);
                return v != null ? v : defValue;
            }
        }

        public int getInt(String key, int defValue) {
            synchronized (this) {
                Integer v = (Integer)mMap.get(key);
                if (v != null) {
                    this.returnVal = v;
                    /**
                     * @j2sNative
                     * return Number(this.returnVal);
                     */{}
                }
                return v != null ? v : defValue;
            }
        }

        public long getLong(String key, long defValue) {
            synchronized (this) {
                Long v = (Long) mMap.get(key);
                if (v != null) {
                    this.returnVal = v;
                    /**
                     * @j2sNative
                     * return Number(this.returnVal);
                     */{}
                }
                return v != null ? v : defValue;
            }
        }

        public float getFloat(String key, float defValue) {
            synchronized (this) {
                Float v = (Float) mMap.get(key);
                if (v != null) {
                    this.returnVal = v;
                    /**
                     * @j2sNative
                     * return Number(this.returnVal);
                     */{}
                }
                return v != null ? v : defValue;
            }
        }

        public boolean getBoolean(String key, boolean defValue) {
            synchronized (this) {
                Boolean v = (Boolean) mMap.get(key);
                if (v != null) {
                    String strV = v.toString();
                    if ("true".equals(strV)) {
                        return true;
                    }
                    if ("false".equals(strV)) {
                        return false;
                    }
                }
                return defValue;
            }
        }

        public boolean contains(String key) {
            synchronized (this) {
                return mMap.containsKey(key);
            }
        }

        public Editor edit() {
            return new EditorImpl();
        }

        public final class EditorImpl implements Editor {
            private final Map<String, Object> mModified = new HashMap();
            private boolean mClear = false;

            /**
             * MayLoon workaround: avoid method's args name become to a,b,c of inner class and js can't use
             * the args.
             */
            private String argKey = "";
            private String argVal = "";

            public Editor putString(String key, String value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }

            public Editor putInt(String key, int value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }

            public Editor putLong(String key, long value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }

            public Editor putFloat(String key, float value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }

            public Editor putBoolean(String key, boolean value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }

            public Editor remove(String key) {
                synchronized (this) {
                    mModified.put(key, this);
                    return this;
                }
            }

            public Editor clear() {
                synchronized (this) {
                    mClear = true;
                    return this;
                }
            }

            public void apply() {
                final MemoryCommitResult mcr = commitToMemory();
                // Okay to notify the listeners before it's hit disk
                // because the listeners should always get the same
                // SharedPreferences instance back, which has the
                // changes reflected in memory.
                notifyListeners(mcr);
            }

            public boolean commit() {
                MemoryCommitResult mcr = commitToMemory();
                notifyListeners(mcr);
                return mcr.writeToDiskResult;
            }

            // Returns true if any changes were made
            private MemoryCommitResult commitToMemory() {
                MemoryCommitResult mcr = new MemoryCommitResult();

                boolean hasListeners = mListeners.size() > 0;
                if (hasListeners) {
                    mcr.keysModified = new ArrayList<String>();
                    mcr.listeners =
                        new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
                }

                synchronized (this) {
                    if (mClear) {
                        if (!mMap.isEmpty()) {
                            // clear the data from localStorage and mMap
                            Iterator<String> it = mMap.keySet().iterator();
                            while (it.hasNext()) {
                                this.argKey = appName + it.next();
                                /**
                                 * @j2sNative
                                 * localStorage.removeItem(this.argKey);
                                 */{}
                            }
                            mcr.changesMade = true;
                            mMap.clear();
                        }
                    }
                    mClear = false;
                }
                for (Entry<String, Object> e : mModified.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();
                    this.argKey = appName + k;
                    this.argVal = v.toString();
                    // remove
                    if (v == this) {
                        if (!mMap.containsKey(k)) {
                            continue;
                        }
                        /**
                         * @j2sNative
                         * localStorage.removeItem(this.argKey);
                         */{}
                        mMap.remove(k);
                    } else {
                        // update or add
                        if (mMap.containsKey(k)) {
                            Object existingValue = mMap.get(k);
                            if (existingValue != null && existingValue.toString().equals(v.toString())) {
                                continue;
                            }
                        }
                        /**
                         * @j2sNative
                         * localStorage.setItem(this.argKey, this.argVal);
                         */{}
                        mMap.put(k, v);
                    }
                    mcr.changesMade = true;
                    if (hasListeners) {
                        mcr.keysModified.add(k);
                    }
                }
                mModified.clear();
                return mcr;
            }

            private void notifyListeners(final MemoryCommitResult mcr) {
                if (mcr.listeners == null || mcr.keysModified == null ||
                        mcr.keysModified.size() == 0) {
                    return;
                }
                for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                    final String key = mcr.keysModified.get(i);
                    for (OnSharedPreferenceChangeListener listener : mcr.listeners) {
                        if (listener != null) {
                            listener.onSharedPreferenceChanged(SharedPreferencesJ2SImpl.this, key);
                        }
                    }
                }
            }
        }

        // Has this SharedPreferences ever had values assigned to it?
        boolean isLoaded() {
            synchronized (this) {
                return mLoaded;
            }
        }

        // Has the file changed out from under us? i.e. writes that
        // we didn't instigate.
        public boolean hasFileChangedUnexpectedly() {
            return false;
        }

        /* package */void replace(Map newContents) {
            synchronized (this) {
                mLoaded = true;
                if (newContents != null) {
                    mMap = newContents;
                }
            }
        }

        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            synchronized (this) {
                mListeners.put(listener, mContent);
            }
        }

        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            synchronized (this) {
                mListeners.remove(listener);
            }
        }

        public Map<String, ?> getAll() {
            synchronized (this) {
                return new HashMap<String, Object>(mMap);
            }
        }

        // Return value from EditorImpl#commitToMemory()
        private static class MemoryCommitResult {
            public List<String> keysModified;
            public Set<OnSharedPreferenceChangeListener> listeners;
            public volatile boolean writeToDiskResult = true;
            public boolean changesMade;
        }
    }

    /**
     * implemented SharePreferencesJ2SImpl use js's localStorage for mayloon
     */
    private static final class SharedPreferencesImpl implements SharedPreferences {

        // Lock ordering rules:
        //  - acquire SharedPreferencesImpl.this before EditorImpl.this
        //  - acquire mWritingToDiskLock before EditorImpl.this

        private final File mFile;
//        private final File mBackupFile;
        private final int mMode;

        private Map<String, Object> mMap;     // guarded by 'this'
        private int mDiskWritesInFlight = 0;  // guarded by 'this'
        private boolean mLoaded = false;      // guarded by 'this'
        private long mStatTimestamp;          // guarded by 'this'
        private long mStatSize;               // guarded by 'this'

        private final Object mWritingToDiskLock = new Object();
        private static final Object mContent = new Object();
        private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners;

        SharedPreferencesImpl(
            File file, int mode, Map initialContents) {
            mFile = file;
//            mBackupFile = makeBackupFile(file);
            mMode = mode;
            mLoaded = initialContents != null;
            mMap = initialContents != null ? initialContents : new HashMap<String, Object>();
//            FileStatus stat = new FileStatus();

            mListeners = new WeakHashMap<OnSharedPreferenceChangeListener, Object>();
        }

        // Has this SharedPreferences ever had values assigned to it?
        boolean isLoaded() {
            synchronized (this) {
                return mLoaded;
            }
        }

        // Has the file changed out from under us?  i.e. writes that
        // we didn't instigate.
        public boolean hasFileChangedUnexpectedly() {
            synchronized (this) {
                if (mDiskWritesInFlight > 0) {
                    // If we know we caused it, it's not unexpected.
                    if (DEBUG) Log.d(TAG, "disk write in flight, not unexpected.");
                    return false;
                }
            }
            return false;
        }

        /* package */ void replace(Map newContents) {
            synchronized (this) {
                mLoaded = true;
                if (newContents != null) {
                    mMap = newContents;
                }
            }
        }

        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            synchronized(this) {
                mListeners.put(listener, mContent);
            }
        }

        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            synchronized(this) {
                mListeners.remove(listener);
            }
        }

        public Map<String, ?> getAll() {
            synchronized(this) {
                //noinspection unchecked
                return new HashMap<String, Object>(mMap);
            }
        }

        public String getString(String key, String defValue) {
            synchronized (this) {
                String v = (String)mMap.get(key);
                return v != null ? v : defValue;
            }
        }

        public int getInt(String key, int defValue) {
            synchronized (this) {
                Integer v = (Integer)mMap.get(key);
                return v != null ? v : defValue;
            }
        }
        public long getLong(String key, long defValue) {
            synchronized (this) {
                Long v = (Long)mMap.get(key);
                return v != null ? v : defValue;
            }
        }
        public float getFloat(String key, float defValue) {
            synchronized (this) {
                Float v = (Float)mMap.get(key);
                return v != null ? v : defValue;
            }
        }
        public boolean getBoolean(String key, boolean defValue) {
            synchronized (this) {
                Boolean v = (Boolean)mMap.get(key);
                return v != null ? v : defValue;
            }
        }

        public boolean contains(String key) {
            synchronized (this) {
                return mMap.containsKey(key);
            }
        }

        public Editor edit() {
            return new EditorImpl();
        }

        // Return value from EditorImpl#commitToMemory()
        private static class MemoryCommitResult {
            public boolean changesMade;  // any keys different?
            public List<String> keysModified;  // may be null
            public Set<OnSharedPreferenceChangeListener> listeners;  // may be null
            public Map<?, ?> mapToWriteToDisk;
            public volatile boolean writeToDiskResult = false;

            public void setDiskWriteResult(boolean result) {
                writeToDiskResult = result;
            }
        }

        public final class EditorImpl implements Editor {
            private final Map<String, Object> mModified = new HashMap();
            private boolean mClear = false;

            public Editor putString(String key, String value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }
            public Editor putInt(String key, int value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }
            public Editor putLong(String key, long value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }
            public Editor putFloat(String key, float value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }
            public Editor putBoolean(String key, boolean value) {
                synchronized (this) {
                    mModified.put(key, value);
                    return this;
                }
            }

            public Editor remove(String key) {
                synchronized (this) {
                    mModified.put(key, this);
                    return this;
                }
            }

            public Editor clear() {
                synchronized (this) {
                    mClear = true;
                    return this;
                }
            }

            public void apply() {
                final MemoryCommitResult mcr = commitToMemory();


//                QueuedWork.add(awaitCommit);

                // Okay to notify the listeners before it's hit disk
                // because the listeners should always get the same
                // SharedPreferences instance back, which has the
                // changes reflected in memory.
                notifyListeners(mcr);
            }

            // Returns true if any changes were made
            private MemoryCommitResult commitToMemory() {
                MemoryCommitResult mcr = new MemoryCommitResult();
                synchronized (SharedPreferencesImpl.this) {
                    // We optimistically don't make a deep copy until
                    // a memory commit comes in when we're already
                    // writing to disk.
                    if (mDiskWritesInFlight > 0) {
                        // We can't modify our mMap as a currently
                        // in-flight write owns it.  Clone it before
                        // modifying it.
                        // noinspection unchecked
                        mMap = new HashMap<String, Object>(mMap);
                    }
                    mcr.mapToWriteToDisk = mMap;
                    mDiskWritesInFlight++;

                    boolean hasListeners = mListeners.size() > 0;
                    if (hasListeners) {
                        mcr.keysModified = new ArrayList<String>();
                        mcr.listeners =
                            new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
                    }

                    synchronized (this) {
                        if (mClear) {
                            if (!mMap.isEmpty()) {
                                mcr.changesMade = true;
                                mMap.clear();
                            }
                            mClear = false;
                        }

                        for (Entry<String, Object> e : mModified.entrySet()) {
                            String k = e.getKey();
                            Object v = e.getValue();
                            if (v == this) {  // magic value for a removal mutation
                                if (!mMap.containsKey(k)) {
                                    continue;
                                }
                                mMap.remove(k);
                            } else {
                                boolean isSame = false;
                                if (mMap.containsKey(k)) {
                                    Object existingValue = mMap.get(k);
                                    if (existingValue != null && existingValue.equals(v)) {
                                        continue;
                                    }
                                }
                                mMap.put(k, v);
                            }

                            mcr.changesMade = true;
                            if (hasListeners) {
                                mcr.keysModified.add(k);
                            }
                        }

                        mModified.clear();
                    }
                }
                return mcr;
            }

            public boolean commit() {
                MemoryCommitResult mcr = commitToMemory();
                SharedPreferencesImpl.this.enqueueDiskWrite(
                    mcr, null /* sync write on this thread okay */);

                notifyListeners(mcr);
                return mcr.writeToDiskResult;
            }

            private void notifyListeners(final MemoryCommitResult mcr) {
                if (mcr.listeners == null || mcr.keysModified == null ||
                    mcr.keysModified.size() == 0) {
                    return;
                }
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                        final String key = mcr.keysModified.get(i);
                        for (OnSharedPreferenceChangeListener listener : mcr.listeners) {
                            if (listener != null) {
                                listener.onSharedPreferenceChanged(SharedPreferencesImpl.this, key);
                            }
                        }
                    }
                } else {
                    // Run this function on the main thread.
//                    Context.this.mActivityThread.getHandler().post(new Runnable() {
//                            public void run() {
//                                notifyListeners(mcr);
//                            }
//                        });
                }
            }
        }

        /**
         * Enqueue an already-committed-to-memory result to be written
         * to disk.
         *
         * They will be written to disk one-at-a-time in the order
         * that they're enqueued.
         *
         * @param postWriteRunnable if non-null, we're being called
         *   from apply() and this is the runnable to run after
         *   the write proceeds.  if null (from a regular commit()),
         *   then we're allowed to do this disk write on the main
         *   thread (which in addition to reducing allocations and
         *   creating a background thread, this has the advantage that
         *   we catch them in userdebug StrictMode reports to convert
         *   them where possible to apply() ...)
         */
        private void enqueueDiskWrite(final MemoryCommitResult mcr,
                                      final Runnable postWriteRunnable) {
            final Runnable writeToDiskRunnable = new Runnable() {
                    public void run() {
                        synchronized (mWritingToDiskLock) {
                            writeToFile(mcr);
                        }
                        synchronized (SharedPreferencesImpl.this) {
                            mDiskWritesInFlight--;
                        }
                        if (postWriteRunnable != null) {
                            postWriteRunnable.run();
                        }
                    }
                };

            final boolean isFromSyncCommit = (postWriteRunnable == null);

            // Typical #commit() path with fewer allocations, doing a write on
            // the current thread.
            if (isFromSyncCommit) {
                boolean wasEmpty = false;
                synchronized (SharedPreferencesImpl.this) {
                    wasEmpty = mDiskWritesInFlight == 1;
                }
                if (wasEmpty) {
                    writeToDiskRunnable.run();
                    return;
                }
            }

//            QueuedWork.singleThreadExecutor().execute(writeToDiskRunnable);
        }

        private static FileOutputStream createFileOutputStream(File file) {
            FileOutputStream str = null;
            try {
                str = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                File parent = file.getParentFile();
                if (!parent.mkdir()) {
                    Log.e(TAG, "Couldn't create directory for SharedPreferences file " + file);
                    return null;
                }
//                FileUtils.setPermissions(
//                    parent.getPath(),
//                    FileUtils.S_IRWXU|FileUtils.S_IRWXG|FileUtils.S_IXOTH,
//                    -1, -1);
                try {
                    str = new FileOutputStream(file);
                } catch (FileNotFoundException e2) {
                    Log.e(TAG, "Couldn't create SharedPreferences file " + file, e2);
                }
            }
            return str;
        }

        // Note: must hold mWritingToDiskLock
        private void writeToFile(MemoryCommitResult mcr) {
            // Rename the current file so it may be used as a backup during the next read
            if (mFile.exists()) {
                if (!mcr.changesMade) {
                    // If the file already exists, but no changes were
                    // made to the underlying map, it's wasteful to
                    // re-write the file.  Return as if we wrote it
                    // out.
                    mcr.setDiskWriteResult(true);
                    return;
                }
//                if (!mBackupFile.exists()) {
//                    if (!mFile.renameTo(mBackupFile)) {
//                        Log.e(TAG, "Couldn't rename file " + mFile
//                                + " to backup file " + mBackupFile);
//                        mcr.setDiskWriteResult(false);
//                        return;
//                    }
//                } else {
//                    mFile.delete();
//                }
            }

            FileOutputStream str = createFileOutputStream(mFile);
			if (str == null) {
			    mcr.setDiskWriteResult(false);
			    return;
			}
//                XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
//                FileUtils.sync(str);
//                str.close();
//                setFilePermissionsFromMode(mFile.getPath(), mMode, 0);
//                FileStatus stat = new FileStatus();
//                if (FileUtils.getFileStatus(mFile.getPath(), stat)) {
//                    synchronized (this) {
//                        mStatTimestamp = stat.mtime;
//                        mStatSize = stat.size;
//                    }
//                }
			// Writing was successful, delete the backup file if there is one.
//                mBackupFile.delete();
			mcr.setDiskWriteResult(true);
			return;
            // Clean up an unsuccessfully written file
//            if (mFile.exists()) {
//                if (!mFile.delete()) {
//                    Log.e(TAG, "Couldn't clean up partially-written file " + mFile);
//                }
//            }
//            mcr.setDiskWriteResult(false);
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: clearWallpaper");
     */
    @MayloonStubAnnotation()
    public void clearWallpaper() {
        System.out.println("Stub" + " Function : clearWallpaper");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: databaseList");
     */
    @MayloonStubAnnotation()
    public String[] databaseList() {
        System.out.println("Stub" + " Function : databaseList");
        return null;
    }

//    /**
//     * @j2sNative
//     * console.log("Missing method: getClassLoader");
//     */
//    @MayloonStubAnnotation()
//    public ClassLoader getClassLoader() {
//        System.out.println("Stub" + " Function : getClassLoader");
//        return null;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: getExternalFilesDir");
     */
    @MayloonStubAnnotation()
    public File getExternalFilesDir(String type) {
        System.out.println("Stub" + " Function : getExternalFilesDir");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getPackageCodePath");
     */
    @MayloonStubAnnotation()
    public String getPackageCodePath() {
        System.out.println("Stub" + " Function : getPackageCodePath");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: fileList");
     */
    @MayloonStubAnnotation()
    public String[] fileList() {
        System.out.println("Stub" + " Function : fileList");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setWallpaper");
     */
    @MayloonStubAnnotation()
    public void setWallpaper(InputStream data) {
        System.out.println("Stub" + " Function : setWallpaper");
        return;
    }

    public File getFileStreamPath(String name) {
        return makeFilename(getFilesDir(), name);
    }

    /**
     * @j2sNative
     * console.log("Missing method: getPackageResourcePath");
     */
    @MayloonStubAnnotation()
    public String getPackageResourcePath() {
        System.out.println("Stub" + " Function : getPackageResourcePath");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getWallpaperDesiredMinimumHeight");
     */
    @MayloonStubAnnotation()
    public int getWallpaperDesiredMinimumHeight() {
        System.out.println("Stub" + " Function : getWallpaperDesiredMinimumHeight");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getDatabasePath");
     */
    @MayloonStubAnnotation()
    public File getDatabasePath(String name) {
        System.out.println("Stub" + " Function : getDatabasePath");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: checkCallingPermission");
     */
    @MayloonStubAnnotation()
    public int checkCallingPermission(String permission) {
        System.out.println("Stub" + " Function : checkCallingPermission");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getWallpaperDesiredMinimumWidth");
     */
    @MayloonStubAnnotation()
    public int getWallpaperDesiredMinimumWidth() {
        System.out.println("Stub" + " Function : getWallpaperDesiredMinimumWidth");
        return 0;
    }

    public int checkCallingOrSelfPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return checkPermission(permission, Binder.getCallingPid(),
                Binder.getCallingUid());
    }

    public int checkPermission(String permission, int pid, int uid) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getExternalCacheDir");
     */
    @MayloonStubAnnotation()
    public File getExternalCacheDir() {
        System.out.println("Stub" + " Function : getExternalCacheDir");
        return null;
    }

    public boolean deleteFile(String name) {
        try {
            File f = makeFilename(getFilesDir(), name);
            if(f == null){
                return true;
            }
            return f.delete();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public AssetManager getAssets() {
        if(mAssets != null){
            return mAssets;
        }
        return (mAssets = new AssetManager());
    }

    public Looper getMainLooper() {
        if(mActivityThread == null) {
            mActivityThread = ActivityThread.currentActivityThread();
        }

        return mActivityThread.getLooper();
    }
}
