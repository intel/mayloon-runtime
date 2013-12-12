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

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

/**
 * A description of an Intent and target action to perform with it.  Instances
 * of this class are created with {@link #getActivity},
 * {@link #getBroadcast}, {@link #getService}; the returned object can be
 * handed to other applications so that they can perform the action you
 * described on your behalf at a later time.
 *
 * <p>By giving a PendingIntent to another application,
 * you are granting it the right to perform the operation you have specified
 * as if the other application was yourself (with the same permissions and
 * identity).  As such, you should be careful about how you build the PendingIntent:
 * often, for example, the base Intent you supply will have the component
 * name explicitly set to one of your own components, to ensure it is ultimately
 * sent there and nowhere else.
 *
 * <p>A PendingIntent itself is simply a reference to a token maintained by
 * the system describing the original data used to retrieve it.  This means
 * that, even if its owning application's process is killed, the
 * PendingIntent itself will remain usable from other processes that
 * have been given it.  If the creating application later re-retrieves the
 * same kind of PendingIntent (same operation, same Intent action, data,
 * categories, and components, and same flags), it will receive a PendingIntent
 * representing the same token if that is still valid, and can thus call
 * {@link #cancel} to remove it.
 */
public final class PendingIntent implements Parcelable {
	
	public Context mContext;
	public Intent mIntent;
	public boolean BROADCAST=false;

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static PendingIntent getBroadcast(Context context, int requestCode,
            Intent intent, int flags) {
        PendingIntent pIntent=new PendingIntent();
        pIntent.mContext=context;
        pIntent.mIntent=intent;
        pIntent.BROADCAST=true;
        
        return pIntent;
    }

	public void send(){
		if(BROADCAST==true)
			mContext.sendBroadcast(mIntent);
    }
	
	/**
     * Callback interface for discovering when a send operation has
     * completed.  Primarily for use with a PendingIntent that is
     * performing a broadcast, this provides the same information as
     * calling {@link Context#sendOrderedBroadcast(Intent, String,
     * android.content.BroadcastReceiver, Handler, int, String, Bundle)
     * Context.sendBroadcast()} with a final BroadcastReceiver.
     */
    public interface OnFinished {
        /**
         * Called when a send operation as completed.
         *
         * @param pendingIntent The PendingIntent this operation was sent through.
         * @param intent The original Intent that was sent.
         * @param resultCode The final result code determined by the send.
         * @param resultData The final data collected by a broadcast.
         * @param resultExtras The final extras collected by a broadcast.
         */
        void onSendFinished(PendingIntent pendingIntent, Intent intent,
                int resultCode, String resultData, Bundle resultExtras);
    }

    /**
     * @j2sNative
     * console.log("Missing method: getTarget");
     */
    @MayloonStubAnnotation()
    public Object getTarget() {
        System.out.println("Stub" + " Function : getTarget");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: send");
     */
    @MayloonStubAnnotation()
    public void send(int code) {
        System.out.println("Stub" + " Function : send");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getTargetPackage");
     */
    @MayloonStubAnnotation()
    public String getTargetPackage() {
        System.out.println("Stub" + " Function : getTargetPackage");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: cancel");
     */
    @MayloonStubAnnotation()
    public void cancel() {
        System.out.println("Stub" + " Function : cancel");
        return;
    }
}
