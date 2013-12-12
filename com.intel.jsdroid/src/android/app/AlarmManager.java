/*
 * Copyright (C) 2007 The Android Open Source Project
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



/**
 * This class provides access to the system alarm services.  These allow you
 * to schedule your application to be run at some point in the future.  When
 * an alarm goes off, the {@link Intent} that had been registered for it
 * is broadcast by the system, automatically starting the target application
 * if it is not already running.  Registered alarms are retained while the
 * device is asleep (and can optionally wake the device up if they go off
 * during that time), but will be cleared if it is turned off and rebooted.
 * 
 * <p>The Alarm Manager holds a CPU wake lock as long as the alarm receiver's
 * onReceive() method is executing. This guarantees that the phone will not sleep
 * until you have finished handling the broadcast. Once onReceive() returns, the
 * Alarm Manager releases this wake lock. This means that the phone will in some
 * cases sleep as soon as your onReceive() method completes.  If your alarm receiver
 * called {@link android.content.Context#startService Context.startService()}, it
 * is possible that the phone will sleep before the requested service is launched.
 * To prevent this, your BroadcastReceiver and Service will need to implement a
 * separate wake lock policy to ensure that the phone continues running until the
 * service becomes available.
 *
 * <p><b>Note: The Alarm Manager is intended for cases where you want to have
 * your application code run at a specific time, even if your application is
 * not currently running.  For normal timing operations (ticks, timeouts,
 * etc) it is easier and much more efficient to use
 * {@link android.os.Handler}.</b>
 *
 * <p>You do not
 * instantiate this class directly; instead, retrieve it through
 * {@link android.content.Context#getSystemService
 * Context.getSystemService(Context.ALARM_SERVICE)}.
 */
public class AlarmManager
{
	/**
     * Alarm time in {@link System#currentTimeMillis System.currentTimeMillis()}
     * (wall clock time in UTC), which will wake up the device when
     * it goes off.
     */
    public static final int RTC_WAKEUP = 0;
    
    public int alarm_timer;
    
    public void set(int type, long triggerAtTime, PendingIntent operation) {
    	System.out.println("triggerAtTime:"+triggerAtTime);
    	long delayMillis=triggerAtTime-System.currentTimeMillis();
    	System.out.println("delayMillis:"+delayMillis);
    	/**
	  	@j2sNative
  		alarm_timer=window.setTimeout((function(operation) {
			return function() {
				window.log("timeout: alarm");
				operation.send();
			};
		})(operation), delayMillis);
		 */
    	{		
    	}
    }
    
    public void cancel(PendingIntent operation) {
    	/**
	  	@j2sNative
  		clearTimeout(alarm_timer);
		 */
    	{ 		
    	}
    }

    /**
     * @j2sNative
     * console.log("Missing method: setTime");
     */
    @MayloonStubAnnotation()
    public void setTime(long millis) {
        System.out.println("Stub" + " Function : setTime");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setTimeZone");
     */
    @MayloonStubAnnotation()
    public void setTimeZone(String timeZone) {
        System.out.println("Stub" + " Function : setTimeZone");
        return;
    }
}
