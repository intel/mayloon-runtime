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

package android.os;

//import android.net.LocalSocketAddress;
//import android.net.LocalSocket;
import android.util.Log;
//import dalvik.system.Zygote;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/*package*/class ZygoteStartFailedEx extends Exception {
    /**
     * Something prevented the zygote process startup from happening normally
     */

    ZygoteStartFailedEx() {
    };

    ZygoteStartFailedEx(String s) {
        super(s);
    }

    ZygoteStartFailedEx(Throwable cause) {
        super(cause);
    }
}

/**
 * Tools for managing OS processes.
 */
public class Process {
    private static final String LOG_TAG = "Process";

    private static final String ZYGOTE_SOCKET = "zygote";

    /**
     * Name of a process for running the platform's media services. {@hide
     * }
     */
    public static final String ANDROID_SHARED_MEDIA = "com.android.process.media";

    /**
     * Name of the process that Google content providers can share. {@hide
     * }
     */
    public static final String GOOGLE_SHARED_APP_CONTENT = "com.google.process.content";

    /**
     * Defines the UID/GID under which system code runs.
     */
    public static final int SYSTEM_UID = 1000;

    /**
     * Defines the UID/GID under which the telephony code runs.
     */
    public static final int PHONE_UID = 1001;

    /**
     * Defines the UID/GID for the user shell.
     * 
     * @hide
     */
    public static final int SHELL_UID = 2000;

    /**
     * Defines the UID/GID for the log group.
     * 
     * @hide
     */
    public static final int LOG_UID = 1007;

    /**
     * Defines the UID/GID for the WIFI supplicant process.
     * 
     * @hide
     */
    public static final int WIFI_UID = 1010;

    /**
     * Defines the UID/GID for the NFC service process.
     * 
     * @hide
     */
    public static final int NFC_UID = 1025;

    /**
     * Defines the start of a range of UIDs (and GIDs), going from this number
     * to {@link #LAST_APPLICATION_UID} that are reserved for assigning to
     * applications.
     */
    public static final int FIRST_APPLICATION_UID = 10000;
    /**
     * Last of application-specific UIDs starting at
     * {@link #FIRST_APPLICATION_UID}.
     */
    public static final int LAST_APPLICATION_UID = 99999;

    /**
     * Defines a secondary group id for access to the bluetooth hardware.
     */
    public static final int BLUETOOTH_GID = 2000;

    /**
     * Standard priority of application threads. Use with
     * {@link #setThreadPriority(int)} and {@link #setThreadPriority(int, int)},
     * <b>not</b> with the normal {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_DEFAULT = 0;

    /*
     * ***************************************
     * ** Keep in sync with utils/threads.h **
     * ***************************************
     */

    /**
     * Lowest available thread priority. Only for those who really, really don't
     * want to run if anything else is happening. Use with
     * {@link #setThreadPriority(int)} and {@link #setThreadPriority(int, int)},
     * <b>not</b> with the normal {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_LOWEST = 19;

    /**
     * Standard priority background threads. This gives your thread a slightly
     * lower than normal priority, so that it will have less chance of impacting
     * the responsiveness of the user interface. Use with
     * {@link #setThreadPriority(int)} and {@link #setThreadPriority(int, int)},
     * <b>not</b> with the normal {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_BACKGROUND = 10;

    /**
     * Standard priority of threads that are currently running a user interface
     * that the user is interacting with. Applications can not normally change
     * to this priority; the system will automatically adjust your application
     * threads as the user moves through the UI. Use with
     * {@link #setThreadPriority(int)} and {@link #setThreadPriority(int, int)},
     * <b>not</b> with the normal {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_FOREGROUND = -2;

    /**
     * Standard priority of system display threads, involved in updating the
     * user interface. Applications can not normally change to this priority.
     * Use with {@link #setThreadPriority(int)} and
     * {@link #setThreadPriority(int, int)}, <b>not</b> with the normal
     * {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_DISPLAY = -4;

    /**
     * Standard priority of the most important display threads, for compositing
     * the screen and retrieving input events. Applications can not normally
     * change to this priority. Use with {@link #setThreadPriority(int)} and
     * {@link #setThreadPriority(int, int)}, <b>not</b> with the normal
     * {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_URGENT_DISPLAY = -8;

    /**
     * Standard priority of audio threads. Applications can not normally change
     * to this priority. Use with {@link #setThreadPriority(int)} and
     * {@link #setThreadPriority(int, int)}, <b>not</b> with the normal
     * {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_AUDIO = -16;

    /**
     * Standard priority of the most important audio threads. Applications can
     * not normally change to this priority. Use with
     * {@link #setThreadPriority(int)} and {@link #setThreadPriority(int, int)},
     * <b>not</b> with the normal {@link java.lang.Thread} class.
     */
    public static final int THREAD_PRIORITY_URGENT_AUDIO = -19;

    /**
     * Minimum increment to make a priority more favorable.
     */
    public static final int THREAD_PRIORITY_MORE_FAVORABLE = -1;

    /**
     * Minimum increment to make a priority less favorable.
     */
    public static final int THREAD_PRIORITY_LESS_FAVORABLE = +1;

    /**
     * Default thread group - gets a 'normal' share of the CPU
     * 
     * @hide
     */
    public static final int THREAD_GROUP_DEFAULT = 0;

    /**
     * Background non-interactive thread group - All threads in this group are
     * scheduled with a reduced share of the CPU.
     * 
     * @hide
     */
    public static final int THREAD_GROUP_BG_NONINTERACTIVE = 1;

    /**
     * Foreground 'boost' thread group - All threads in this group are scheduled
     * with an increased share of the CPU
     * 
     * @hide
     **/
    public static final int THREAD_GROUP_FG_BOOST = 2;

    public static final int SIGNAL_QUIT = 3;
    public static final int SIGNAL_KILL = 9;
    public static final int SIGNAL_USR1 = 10;

    // State for communicating with zygote process

    // static LocalSocket sZygoteSocket;
    static DataInputStream sZygoteInputStream;
    static BufferedWriter sZygoteWriter;

    /** true if previous zygote open failed */
    static boolean sPreviousZygoteOpenFailed;

    /** @hide */
    public static final int PROC_TERM_MASK = 0xff;
    /** @hide */
    public static final int PROC_ZERO_TERM = 0;
    /** @hide */
    public static final int PROC_SPACE_TERM = (int) ' ';
    /** @hide */
    public static final int PROC_TAB_TERM = (int) '\t';
    /** @hide */
    public static final int PROC_COMBINE = 0x100;
    /** @hide */
    public static final int PROC_PARENS = 0x200;
    /** @hide */
    public static final int PROC_OUT_STRING = 0x1000;
    /** @hide */
    public static final int PROC_OUT_LONG = 0x2000;
    /** @hide */
    public static final int PROC_OUT_FLOAT = 0x4000;

}
