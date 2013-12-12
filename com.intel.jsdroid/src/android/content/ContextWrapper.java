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

import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;

/**
 * Proxying implementation of Context that simply delegates all of its calls to
 * another Context. Can be subclassed to modify behavior without changing the
 * original Context.
 */
public class ContextWrapper extends Context {
	Context mBase;
	
	 public ContextWrapper(Context base) {
	        mBase = base;
	    }
	/**
	 * Set the base context for this ContextWrapper. All calls will then be
	 * delegated to the base context. Throws IllegalStateException if a base
	 * context has already been set.
	 * 
	 * @param base
	 *            The new base context for this wrapper.
	 */
	protected void attachBaseContext(Context base) {
		if (mBase != null) {
			throw new IllegalStateException("Base context already set");
		}
		mBase = base;
	}

	/**
	 * @return the base context as set by the constructor or setBaseContext
	 */
	public Context getBaseContext() {
		return mBase;
	}

    public Context getApplicationContext() {
        return mBase.getApplicationContext();
    }

    @Override
    public AssetManager getAssets() {
        return mBase.getAssets();
    }

	@Override
	public Resources getResources() {
		return mBase.getResources();
	}

	@Override
	public PackageManager getPackageManager() {
		return mBase.getPackageManager();
	}

	@Override
	public void setTheme(int resid) {
		mBase.setTheme(resid);
	}

	@Override
	public Resources.Theme getTheme() {
		return mBase.getTheme();
	}

	@Override
	public ClassLoader getClassLoader() {
		return mBase.getClassLoader();
	}

	@Override
	public String getPackageName() {
		return mBase.getPackageName();
	}

	@Override
	public boolean isRestricted() {
		return mBase.isRestricted();
	}
	
	@Override
    public void startActivity(Intent intent) {
        mBase.startActivity(intent);
    }
	
	@Override
	public ComponentName startService(Intent service)
	{
		return mBase.startService(service);
	}
	
	@Override
	public boolean bindService(Intent service, ServiceConnection conn,
            int flags) {
        return mBase.bindService(service, conn, flags);
    }
	
	@Override
    public void unbindService(ServiceConnection conn) {
        mBase.unbindService(conn);
    }
	
	 @Override
	    public Intent registerReceiver(
	        BroadcastReceiver receiver, IntentFilter filter) {
	        return mBase.registerReceiver(receiver, filter);
	    }

	    @Override
	    public Intent registerReceiver(
	        BroadcastReceiver receiver, IntentFilter filter,
	        String broadcastPermission, Handler scheduler) {
	        return mBase.registerReceiver(receiver, filter, broadcastPermission,
	                scheduler);
	    }
}
