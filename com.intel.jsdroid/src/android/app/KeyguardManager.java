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
 * Class that can be used to lock and unlock the keyboard. Get an instance of this 
 * class by calling {@link android.content.Context#getSystemService(java.lang.String)}
 * with argument {@link android.content.Context#KEYGUARD_SERVICE}. The
 * Actual class to control the keyboard locking is
 * {@link android.app.KeyguardManager.KeyguardLock}.
 */
public class KeyguardManager {
	
	public boolean inKeyguardRestrictedInputMode() {
		return false;
    }
	
	public KeyguardLock newKeyguardLock(String tag) {
        return new KeyguardLock(tag);
    }
	
	public class KeyguardLock {
        private String mTag;

        KeyguardLock(String tag) {
            mTag = tag;
        }

        /**
         * Disable the keyguard from showing.  If the keyguard is currently
         * showing, hide it.  The keyguard will be prevented from showing again
         * until {@link #reenableKeyguard()} is called.
         *
         * A good place to call this is from {@link android.app.Activity#onResume()}
         *
         * Note: This call has no effect while any {@link android.app.admin.DevicePolicyManager} 
         * is enabled that requires a password.
         *
         * @see #reenableKeyguard()
         */
        public void disableKeyguard() {
        }

        /**
         * Reenable the keyguard.  The keyguard will reappear if the previous
         * call to {@link #disableKeyguard()} caused it it to be hidden.
         *
         * A good place to call this is from {@link android.app.Activity#onPause()}
         *
         * Note: This call has no effect while any {@link android.app.admin.DevicePolicyManager}
         * is enabled that requires a password.
         *
         * @see #disableKeyguard()
         */
        public void reenableKeyguard() {
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: exitKeyguardSecurely");
     */
    @MayloonStubAnnotation()
    public void exitKeyguardSecurely(Object callback) {
        System.out.println("Stub" + " Function : exitKeyguardSecurely");
        return;
    }
}
