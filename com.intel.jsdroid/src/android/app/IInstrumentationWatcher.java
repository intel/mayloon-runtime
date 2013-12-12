/* //device/java/android/android/app/IInstrumentationWatcher.aidl
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package android.app;

import android.content.ComponentName;
import android.os.Bundle;

/** @hide */
public class IInstrumentationWatcher{
	private boolean mFinished = false;
    private boolean mRawMode = false;

    /**
     * Set or reset "raw mode".  In "raw mode", all bundles are dumped.  In "pretty mode",
     * if a bundle includes Instrumentation.REPORT_KEY_STREAMRESULT, just print that.
     * @param rawMode true for raw mode, false for pretty mode.
     */
    public void setRawOutput(boolean rawMode) {
        mRawMode = rawMode;
    }

    public void instrumentationStatus(ComponentName name, int resultCode, Bundle results) {
        synchronized (this) {
            // pretty printer mode?
            String pretty = null;
            if (!mRawMode && results != null) {
                pretty = results.getString(Instrumentation.REPORT_KEY_STREAMRESULT);
            }
            if (pretty != null) {
                System.out.print(pretty);
            } else {
                if (results != null) {
                    for (String key : results.keySet()) {
                        System.out.println(
                                "INSTRUMENTATION_STATUS: " + key + "=" + results.get(key));
                    }
                }
                System.out.println("INSTRUMENTATION_STATUS_CODE: " + resultCode);
            }
            notifyAll();
        }
    }

    public void instrumentationFinished(ComponentName name, int resultCode,
            Bundle results) {
        synchronized (this) {
            // pretty printer mode?
            String pretty = null;
            if (!mRawMode && results != null) {
                pretty = results.getString(Instrumentation.REPORT_KEY_STREAMRESULT);
            }
            if (pretty != null) {
                System.out.println(pretty);
            } else {
                if (results != null) {
                    for (String key : results.keySet()) {
                        System.out.println(
                                "INSTRUMENTATION_RESULT: " + key + "=" + results.get(key));
                    }
                }
                System.out.println("INSTRUMENTATION_CODE: " + resultCode);
            }
            mFinished = true;
            notifyAll();
        }
    }

}

