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

package android.os;

import com.intel.mpt.annotation.MayloonStubAnnotation;


public class PowerManager{
	
	private static final String TAG = "PowerManager";
    
    /**
     * These internal values define the underlying power elements that we might
     * want to control individually.  Eventually we'd like to expose them.
     */
    private static final int WAKE_BIT_CPU_STRONG = 1;
    private static final int WAKE_BIT_CPU_WEAK = 2;
    private static final int WAKE_BIT_SCREEN_DIM = 4;
    private static final int WAKE_BIT_SCREEN_BRIGHT = 8;
    private static final int WAKE_BIT_KEYBOARD_BRIGHT = 16;
    private static final int WAKE_BIT_PROXIMITY_SCREEN_OFF = 32;
    
    private static final int LOCK_MASK = WAKE_BIT_CPU_STRONG
                                        | WAKE_BIT_CPU_WEAK
                                        | WAKE_BIT_SCREEN_DIM
                                        | WAKE_BIT_SCREEN_BRIGHT
                                        | WAKE_BIT_KEYBOARD_BRIGHT
                                        | WAKE_BIT_PROXIMITY_SCREEN_OFF;

    /**
     * Wake lock that ensures that the CPU is running.  The screen might
     * not be on.
     */
    public static final int PARTIAL_WAKE_LOCK = WAKE_BIT_CPU_STRONG;

    /**
     * Wake lock that ensures that the screen and keyboard are on at
     * full brightness.
     */
    public static final int FULL_WAKE_LOCK = WAKE_BIT_CPU_WEAK | WAKE_BIT_SCREEN_BRIGHT 
                                            | WAKE_BIT_KEYBOARD_BRIGHT;

    /**
     * Wake lock that ensures that the screen is on at full brightness;
     * the keyboard backlight will be allowed to go off.
     */
    public static final int SCREEN_BRIGHT_WAKE_LOCK = WAKE_BIT_CPU_WEAK | WAKE_BIT_SCREEN_BRIGHT;

    /**
     * Wake lock that ensures that the screen is on (but may be dimmed);
     * the keyboard backlight will be allowed to go off.
     */
    public static final int SCREEN_DIM_WAKE_LOCK = WAKE_BIT_CPU_WEAK | WAKE_BIT_SCREEN_DIM;

    /**
     * Wake lock that turns the screen off when the proximity sensor activates.
     * Since not all devices have proximity sensors, use
     * {@link #getSupportedWakeLockFlags() getSupportedWakeLockFlags()} to determine if
     * this wake lock mode is supported.
     *
     * {@hide}
     */
    public static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = WAKE_BIT_PROXIMITY_SCREEN_OFF;

    /**
     * Flag for {@link WakeLock#release release(int)} to defer releasing a
     * {@link #WAKE_BIT_PROXIMITY_SCREEN_OFF} wakelock until the proximity sensor returns
     * a negative value.
     *
     * {@hide}
     */
    public static final int WAIT_FOR_PROXIMITY_NEGATIVE = 1;

    /**
     * Normally wake locks don't actually wake the device, they just cause
     * it to remain on once it's already on.  Think of the video player
     * app as the normal behavior.  Notifications that pop up and want
     * the device to be on are the exception; use this flag to be like them.
     * <p> 
     * Does not work with PARTIAL_WAKE_LOCKs.
     */
    public static final int ACQUIRE_CAUSES_WAKEUP = 0x10000000;

    /**
     * When this wake lock is released, poke the user activity timer
     * so the screen stays on for a little longer.
     * <p>
     * Will not turn the screen on if it is not already on.  See {@link #ACQUIRE_CAUSES_WAKEUP}
     * if you want that.
     * <p>
     * Does not work with PARTIAL_WAKE_LOCKs.
     */
    public static final int ON_AFTER_RELEASE = 0x20000000;
    
    public WakeLock newWakeLock(int flags, String tag)
    {
        if (tag == null) {
            throw new NullPointerException("tag is null in PowerManager.newWakeLock");
        }
        return new WakeLock(flags, tag);
    }
    
	public class WakeLock
    {
        static final int RELEASE_WAKE_LOCK = 1;

        Runnable mReleaser = new Runnable() {
            public void run() {
                release();
            }
        };
	
        int mFlags;
        String mTag;
        IBinder mToken;
        int mCount = 0;
        boolean mRefCounted = true;
        boolean mHeld = false;

        WakeLock(int flags, String tag)
        {}

        /**
         * Sets whether this WakeLock is ref counted.
         *
         * <p>Wake locks are reference counted by default.
         *
         * @param value true for ref counted, false for not ref counted.
         */
        public void setReferenceCounted(boolean value)
        {
            mRefCounted = value;
        }

        /**
         * Makes sure the device is on at the level you asked when you created
         * the wake lock.
         */
        public void acquire()
        {
        }
        
        /**
         * Makes sure the device is on at the level you asked when you created
         * the wake lock. The lock will be released after the given timeout.
         * 
         * @param timeout Release the lock after the give timeout in milliseconds.
         */
        public void acquire(long timeout) {
            acquire();
        }
        

        /**
         * Release your claim to the CPU or screen being on.
         *
         * <p>
         * It may turn off shortly after you release it, or it may not if there
         * are other wake locks held.
         */
        public void release()
        {
            release(0);
        }

        /**
         * Release your claim to the CPU or screen being on.
         * @param flags Combination of flag values to modify the release behavior.
         *              Currently only {@link #WAIT_FOR_PROXIMITY_NEGATIVE} is supported.
         *
         * <p>
         * It may turn off shortly after you release it, or it may not if there
         * are other wake locks held.
         *
         * {@hide}
         */
        public void release(int flags)
        {
        }

        public boolean isHeld()
        {
            synchronized (mToken) {
                return mHeld;
            }
        }


        public String toString() {
            synchronized (mToken) {
                return "WakeLock{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " held=" + mHeld + ", refCount=" + mCount + "}";
            }
        }

        @Override
        protected void finalize() throws Throwable
        {}
    }

    /**
     * @j2sNative
     * console.log("Missing method: goToSleep");
     */
    @MayloonStubAnnotation()
    public void goToSleep(long time) {
        System.out.println("Stub" + " Function : goToSleep");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: reboot");
     */
    @MayloonStubAnnotation()
    public void reboot(String reason) {
        System.out.println("Stub" + " Function : reboot");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isScreenOn");
     */
    @MayloonStubAnnotation()
    public boolean isScreenOn() {
        System.out.println("Stub" + " Function : isScreenOn");
        return true;
    }
}
