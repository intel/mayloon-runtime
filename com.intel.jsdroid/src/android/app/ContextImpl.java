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

import com.android.internal.util.XmlUtils;
import com.google.android.collect.Maps;

import org.xmlpull.v1.XmlPullParserException;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
//import android.content.IContentProvider;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IIntentReceiver;
//import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
//import android.content.pm.FeatureInfo;
//import android.content.pm.IPackageDataObserver;
//import android.content.pm.IPackageDeleteObserver;
//import android.content.pm.IPackageInstallObserver;
//import android.content.pm.IPackageMoveObserver;
//import android.content.pm.IPackageManager;
//import android.content.pm.IPackageStatsObserver;
//import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageParser.Package;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
//import android.location.ILocationManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
//import android.os.Process;
import android.os.RemoteException;
//import android.os.ServiceManager;
import android.os.FileUtils.FileStatus;
import android.provider.Settings;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.WindowManagerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * Common implementation of Context API, which provides the base
 * context object for Activity and other application components.
 */
class ContextImpl extends Context {
    private final static String TAG = "ApplicationContext";
    private final static boolean DEBUG = false;
    private final static boolean DEBUG_ICONS = false;

    private static final Object sSync = new Object();
    private static AlarmManager sAlarmManager;
    private static PowerManager sPowerManager;
    private static LocationManager sLocationManager;

    private AudioManager mAudioManager;
    /*package*/ LoadedApk mPackageInfo;
    private Resources mResources;
    /*package*/ ActivityThread mMainThread;
    private Context mOuterContext;
    private IBinder mActivityToken = null;
    private ApplicationContentResolver mContentResolver;
    private int mThemeResource = 0;
    private Resources.Theme mTheme = null;
    private PackageManager mPackageManager;
    private NotificationManager mNotificationManager = null;
    private ActivityManager mActivityManager = null;
    private Context mReceiverRestrictedContext = null;
    private SensorManager mSensorManager = null;
    private LayoutInflater mLayoutInflater = null;
    private boolean mRestricted;

    private final Object mSync = new Object();

    private File mDatabasesDir;
    private File mPreferencesDir;
    private File mFilesDir;
    private File mCacheDir;
    private File mExternalFilesDir;
    private File mExternalCacheDir;

    private static long sInstanceCount = 0;

    private static final String[] EMPTY_FILE_LIST = {};

    // For debug only
    /*
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        --sInstanceCount;
    }
    */

    public static long getInstanceCount() {
        return sInstanceCount;
    }
    
    @Override
    public Resources getResources() {
        return mResources;
    }

    @Override
    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    @Override
    public Context getApplicationContext() {
        return (mPackageInfo != null) ?
                mPackageInfo.getApplication() : mMainThread.getApplication();
    }
    
    @Override
    public void setTheme(int resid) {
        mThemeResource = resid;
    }
    
    @Override
    public Resources.Theme getTheme() {
        if (mTheme == null) {
            if (mThemeResource == 0) {
                mThemeResource = com.android.internal.R.style.Theme;
            }
            mTheme = mResources.newTheme();
            mTheme.applyStyle(mThemeResource, true);
        }
        return mTheme;
    }

    private static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }

    @Override
    public FileInputStream openFileInput(String name)
        throws FileNotFoundException {
        File f = makeFilename(getFilesDir(), name);
        return new FileInputStream(f);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode)
        throws FileNotFoundException {
        final boolean append = (mode&MODE_APPEND) != 0;
        File f = makeFilename(getFilesDir(), name);
        try {
            FileOutputStream fos = new FileOutputStream(f, append);
            setFilePermissionsFromMode(f.getPath(), mode, 0);
            return fos;
        } catch (FileNotFoundException e) {
        }

        File parent = f.getParentFile();
        parent.mkdir();
        FileUtils.setPermissions(
            parent.getPath(),
            FileUtils.S_IRWXU|FileUtils.S_IRWXG|FileUtils.S_IXOTH,
            -1, -1);
        FileOutputStream fos = new FileOutputStream(f, append);
        setFilePermissionsFromMode(f.getPath(), mode, 0);
        return fos;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return registerReceiver(receiver, filter, null, null);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {}

    private SensorManager getSensorManager() {
        synchronized (mSync) {
            if (mSensorManager == null) {
                mSensorManager = new SensorManager(mMainThread.getHandler().getLooper());
            }
        }
        return mSensorManager;
    }

    private AudioManager getAudioManager()
    {
        if (mAudioManager == null) {
            mAudioManager = new AudioManager(this);
        }
        return mAudioManager;
    }

    private void enforce(
            String permission, int resultOfCheck,
            boolean selfToo, int uid, String message) {
        if (resultOfCheck != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    (message != null ? (message + ": ") : "") +
                    (selfToo
                     ? "Neither user " + uid + " nor current process has "
                     : "User " + uid + " does not have ") +
                    permission +
                    ".");
        }
    }

    private String uriModeFlagToString(int uriModeFlags) {
        switch (uriModeFlags) {
            case Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION:
                return "read and write";
            case Intent.FLAG_GRANT_READ_URI_PERMISSION:
                return "read";
            case Intent.FLAG_GRANT_WRITE_URI_PERMISSION:
                return "write";
        }
        throw new IllegalArgumentException(
                "Unknown permission mode flags: " + uriModeFlags);
    }

    private void enforceForUri(
            int modeFlags, int resultOfCheck, boolean selfToo,
            int uid, Uri uri, String message) {
        if (resultOfCheck != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    (message != null ? (message + ": ") : "") +
                    (selfToo
                     ? "Neither user " + uid + " nor current process has "
                     : "User " + uid + " does not have ") +
                    uriModeFlagToString(modeFlags) +
                    " permission on " +
                    uri +
                    ".");
        }
    }

    @Override
    public Context createPackageContext(String packageName, int flags)
        throws PackageManager.NameNotFoundException {
        if (packageName.equals("system") || packageName.equals("android")) {
            return new ContextImpl(mMainThread.getSystemContext());
        }

        LoadedApk pi =
            mMainThread.getPackageInfo(packageName, flags);
        if (pi != null) {
            ContextImpl c = new ContextImpl();
            c.mRestricted = (flags & CONTEXT_RESTRICTED) == CONTEXT_RESTRICTED;
            c.init(pi, null, mMainThread, mResources);
            if (c.mResources != null) {
                return c;
            }
        }

        // Should be a better exception.
        throw new PackageManager.NameNotFoundException(
            "Application package " + packageName + " not found");
    }

    @Override
    public boolean isRestricted() {
        return mRestricted;
    }

    static ContextImpl createSystemContext(ActivityThread mainThread) {
        ContextImpl context = new ContextImpl();
        context.init(Resources.getSystem(), mainThread);
        return context;
    }

    ContextImpl() {
        // For debug only
        //++sInstanceCount;
        mOuterContext = this;
    }

    /**
     * Create a new ApplicationContext from an existing one.  The new one
     * works and operates the same as the one it is copying.
     *
     * @param context Existing application context.
     */
    public ContextImpl(ContextImpl context) {
        ++sInstanceCount;
        mPackageInfo = context.mPackageInfo;
        mResources = context.mResources;
        mMainThread = context.mMainThread;
        mContentResolver = context.mContentResolver;
        mOuterContext = this;
    }

    final void init(LoadedApk packageInfo,
            IBinder activityToken, ActivityThread mainThread) {
        init(packageInfo, activityToken, mainThread, null);
    }

    final void init(LoadedApk packageInfo,
                IBinder activityToken, ActivityThread mainThread,
                Resources container) {
        mPackageInfo = packageInfo;
        /**
         * @Mayloon update
         * resource are not initialized in package info
         * so we load related package resource here
         */
        mResources = ((PackageManager) Context.getSystemContext().getSystemService(PACKAGE_SERVICE)).getPackageResources(mPackageInfo.getPackageName());
        /*
        mResources = mPackageInfo.getResources(mainThread);

        if (mResources != null && container != null
                && container.getCompatibilityInfo().applicationScale !=
                        mResources.getCompatibilityInfo().applicationScale) {
            if (DEBUG) {
                Log.d(TAG, "loaded context has different scaling. Using container's" +
                        " compatiblity info:" + container.getDisplayMetrics());
            }
            mResources = mainThread.getTopLevelResources(
                    mPackageInfo.getResDir(), container.getCompatibilityInfo().copy());
        }
        */
        mMainThread = mainThread;
        mContentResolver = new ApplicationContentResolver(this, mainThread);

        setActivityToken(activityToken);
    }

    final void init(Resources resources, ActivityThread mainThread) {
        mPackageInfo = null;
        mResources = resources;
        mMainThread = mainThread;
        mContentResolver = new ApplicationContentResolver(this, mainThread);
    }

    final Context getReceiverRestrictedContext() {
        if (mReceiverRestrictedContext != null) {
            return mReceiverRestrictedContext;
        }
        return mReceiverRestrictedContext = new ReceiverRestrictedContext(getOuterContext());
    }

    final void setActivityToken(IBinder token) {
        mActivityToken = token;
    }
    

    private static void setFilePermissionsFromMode(String name, int mode,
            int extraPermissions) {
        int perms = FileUtils.S_IRUSR|FileUtils.S_IWUSR
            |FileUtils.S_IRGRP|FileUtils.S_IWGRP
            |extraPermissions;
        if ((mode&MODE_WORLD_READABLE) != 0) {
            perms |= FileUtils.S_IROTH;
        }
        if ((mode&MODE_WORLD_WRITEABLE) != 0) {
            perms |= FileUtils.S_IWOTH;
        }
        if (DEBUG) {
            Log.i(TAG, "File " + name + ": mode=0x" + Integer.toHexString(mode)
                  + ", perms=0x" + Integer.toHexString(perms));
        }
        FileUtils.setPermissions(name, perms, -1, -1);
    }

    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException(
                "File " + name + " contains a path separator");
    }
  
    @Override
    public String getPackageName() {
        if (mPackageInfo != null) {
            return mPackageInfo.getPackageName();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public void startActivity(Intent intent) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
            throw new AndroidRuntimeException(
                    "Calling startActivity() from outside of an Activity "
                            + " context requires the FLAG_ACTIVITY_NEW_TASK flag."
                            + " Is this really what you want?");
        }
        mMainThread.getInstrumentation().execStartActivity(
                getOuterContext(), mMainThread.getApplicationThread(), null, null, intent, -1);
    }



    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------

    /*package*/
    static final class ApplicationPackageManager extends PackageManager {}

    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    
    //private static final class SharedPreferencesImpl implements SharedPreferences {}
}
