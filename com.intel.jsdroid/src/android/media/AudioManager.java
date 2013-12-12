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

package android.media;

//import android.annotation.SdkConstant;
//import android.annotation.SdkConstant.SdkConstantType;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
//import android.os.ServiceManager;
//import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Iterator;
import java.util.HashMap;

import com.intel.mpt.annotation.MayloonStubAnnotation;

/**
 * AudioManager provides access to volume and ringer mode control.
 * <p>
 * Use <code>Context.getSystemService(Context.AUDIO_SERVICE)</code> to get
 * an instance of this class.
 */
public class AudioManager {

    private final Context mContext;
    //private final Handler mHandler;

    private static String TAG = "AudioManager";
    private static boolean DEBUG = false;
    private static boolean localLOGV = DEBUG || android.util.Config.LOGV;

    /**
     * Broadcast intent, a hint for applications that audio is about to become
     * 'noisy' due to a change in audio outputs. For example, this intent may
     * be sent when a wired headset is unplugged, or when an A2DP audio
     * sink is disconnected, and the audio system is about to automatically
     * switch audio route to the speaker. Applications that are controlling
     * audio streams may consider pausing, reducing volume or some other action
     * on receipt of this intent so as not to surprise the user with audio
     * from the speaker.
     */
    //@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY";

    /**
     * Sticky broadcast intent action indicating that the ringer mode has
     * changed. Includes the new ringer mode.
     *
     * @see #EXTRA_RINGER_MODE
     */
    //@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String RINGER_MODE_CHANGED_ACTION = "android.media.RINGER_MODE_CHANGED";

    /**
     * The new ringer mode.
     *
     * @see #RINGER_MODE_CHANGED_ACTION
     * @see #RINGER_MODE_NORMAL
     * @see #RINGER_MODE_SILENT
     * @see #RINGER_MODE_VIBRATE
     */
    public static final String EXTRA_RINGER_MODE = "android.media.EXTRA_RINGER_MODE";

    /**
     * Broadcast intent action indicating that the vibrate setting has
     * changed. Includes the vibrate type and its new setting.
     *
     * @see #EXTRA_VIBRATE_TYPE
     * @see #EXTRA_VIBRATE_SETTING
     */
    //@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String VIBRATE_SETTING_CHANGED_ACTION = "android.media.VIBRATE_SETTING_CHANGED";

    /**
     * @hide Broadcast intent when the volume for a particular stream type changes.
     * Includes the stream, the new volume and previous volumes
     *
     * @see #EXTRA_VOLUME_STREAM_TYPE
     * @see #EXTRA_VOLUME_STREAM_VALUE
     * @see #EXTRA_PREV_VOLUME_STREAM_VALUE
     */
    //@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    /**
     * The new vibrate setting for a particular type.
     *
     * @see #VIBRATE_SETTING_CHANGED_ACTION
     * @see #EXTRA_VIBRATE_TYPE
     * @see #VIBRATE_SETTING_ON
     * @see #VIBRATE_SETTING_OFF
     * @see #VIBRATE_SETTING_ONLY_SILENT
     */
    public static final String EXTRA_VIBRATE_SETTING = "android.media.EXTRA_VIBRATE_SETTING";

    /**
     * The vibrate type whose setting has changed.
     *
     * @see #VIBRATE_SETTING_CHANGED_ACTION
     * @see #VIBRATE_TYPE_NOTIFICATION
     * @see #VIBRATE_TYPE_RINGER
     */
    public static final String EXTRA_VIBRATE_TYPE = "android.media.EXTRA_VIBRATE_TYPE";

    /**
     * @hide The stream type for the volume changed intent.
     */
    public static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

    /**
     * @hide The volume associated with the stream for the volume changed intent.
     */
    public static final String EXTRA_VOLUME_STREAM_VALUE =
        "android.media.EXTRA_VOLUME_STREAM_VALUE";

    /**
     * @hide The previous volume associated with the stream for the volume changed intent.
     */
    public static final String EXTRA_PREV_VOLUME_STREAM_VALUE =
        "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";

    /** The audio stream for phone calls */
    public static final int STREAM_VOICE_CALL = AudioSystem.STREAM_VOICE_CALL;
    /** The audio stream for system sounds */
    public static final int STREAM_SYSTEM = AudioSystem.STREAM_SYSTEM;
    /** The audio stream for the phone ring */
    public static final int STREAM_RING = AudioSystem.STREAM_RING;
    /** The audio stream for music playback */
    public static final int STREAM_MUSIC = AudioSystem.STREAM_MUSIC;
    /** The audio stream for alarms */
    public static final int STREAM_ALARM = AudioSystem.STREAM_ALARM;
    /** The audio stream for notifications */
    public static final int STREAM_NOTIFICATION = AudioSystem.STREAM_NOTIFICATION;
    /** @hide The audio stream for phone calls when connected to bluetooth */
    public static final int STREAM_BLUETOOTH_SCO = AudioSystem.STREAM_BLUETOOTH_SCO;
    /** @hide The audio stream for enforced system sounds in certain countries (e.g camera in Japan) */
    public static final int STREAM_SYSTEM_ENFORCED = AudioSystem.STREAM_SYSTEM_ENFORCED;
    /** The audio stream for DTMF Tones */
    public static final int STREAM_DTMF = AudioSystem.STREAM_DTMF;
    /** @hide The audio stream for text to speech (TTS) */
    public static final int STREAM_TTS = AudioSystem.STREAM_TTS;
    /** Number of audio streams */
    /**
     * @deprecated Use AudioSystem.getNumStreamTypes() instead
     */
    @Deprecated public static final int NUM_STREAMS = AudioSystem.NUM_STREAMS;


    /**  @hide Default volume index values for audio streams */
    public static final int[] DEFAULT_STREAM_VOLUME = new int[] {
        4,  // STREAM_VOICE_CALL
        7,  // STREAM_SYSTEM
        5,  // STREAM_RING
        11, // STREAM_MUSIC
        6,  // STREAM_ALARM
        5,  // STREAM_NOTIFICATION
        7,  // STREAM_BLUETOOTH_SCO
        7,  // STREAM_SYSTEM_ENFORCED
        11, // STREAM_DTMF
        11  // STREAM_TTS
    };

    /**
     * Increase the ringer volume.
     *
     * @see #adjustVolume(int, int)
     * @see #adjustStreamVolume(int, int, int)
     */
    public static final int ADJUST_RAISE = 1;

    /**
     * Decrease the ringer volume.
     *
     * @see #adjustVolume(int, int)
     * @see #adjustStreamVolume(int, int, int)
     */
    public static final int ADJUST_LOWER = -1;

    /**
     * Maintain the previous ringer volume. This may be useful when needing to
     * show the volume toast without actually modifying the volume.
     *
     * @see #adjustVolume(int, int)
     * @see #adjustStreamVolume(int, int, int)
     */
    public static final int ADJUST_SAME = 0;

    // Flags should be powers of 2!

    /**
     * Show a toast containing the current volume.
     *
     * @see #adjustStreamVolume(int, int, int)
     * @see #adjustVolume(int, int)
     * @see #setStreamVolume(int, int, int)
     * @see #setRingerMode(int)
     */
    public static final int FLAG_SHOW_UI = 1 << 0;

    /**
     * Whether to include ringer modes as possible options when changing volume.
     * For example, if true and volume level is 0 and the volume is adjusted
     * with {@link #ADJUST_LOWER}, then the ringer mode may switch the silent or
     * vibrate mode.
     * <p>
     * By default this is on for the ring stream. If this flag is included,
     * this behavior will be present regardless of the stream type being
     * affected by the ringer mode.
     *
     * @see #adjustVolume(int, int)
     * @see #adjustStreamVolume(int, int, int)
     */
    public static final int FLAG_ALLOW_RINGER_MODES = 1 << 1;

    /**
     * Whether to play a sound when changing the volume.
     * <p>
     * If this is given to {@link #adjustVolume(int, int)} or
     * {@link #adjustSuggestedStreamVolume(int, int, int)}, it may be ignored
     * in some cases (for example, the decided stream type is not
     * {@link AudioManager#STREAM_RING}, or the volume is being adjusted
     * downward).
     *
     * @see #adjustStreamVolume(int, int, int)
     * @see #adjustVolume(int, int)
     * @see #setStreamVolume(int, int, int)
     */
    public static final int FLAG_PLAY_SOUND = 1 << 2;

    /**
     * Removes any sounds/vibrate that may be in the queue, or are playing (related to
     * changing volume).
     */
    public static final int FLAG_REMOVE_SOUND_AND_VIBRATE = 1 << 3;

    /**
     * Whether to vibrate if going into the vibrate ringer mode.
     */
    public static final int FLAG_VIBRATE = 1 << 4;

    /**
     * Ringer mode that will be silent and will not vibrate. (This overrides the
     * vibrate setting.)
     *
     * @see #setRingerMode(int)
     * @see #getRingerMode()
     */
    public static final int RINGER_MODE_SILENT = 0;

    /**
     * Ringer mode that will be silent and will vibrate. (This will cause the
     * phone ringer to always vibrate, but the notification vibrate to only
     * vibrate if set.)
     *
     * @see #setRingerMode(int)
     * @see #getRingerMode()
     */
    public static final int RINGER_MODE_VIBRATE = 1;

    /**
     * Ringer mode that may be audible and may vibrate. It will be audible if
     * the volume before changing out of this mode was audible. It will vibrate
     * if the vibrate setting is on.
     *
     * @see #setRingerMode(int)
     * @see #getRingerMode()
     */
    public static final int RINGER_MODE_NORMAL = 2;

    /**
     * Vibrate type that corresponds to the ringer.
     *
     * @see #setVibrateSetting(int, int)
     * @see #getVibrateSetting(int)
     * @see #shouldVibrate(int)
     */
    public static final int VIBRATE_TYPE_RINGER = 0;

    /**
     * Vibrate type that corresponds to notifications.
     *
     * @see #setVibrateSetting(int, int)
     * @see #getVibrateSetting(int)
     * @see #shouldVibrate(int)
     */
    public static final int VIBRATE_TYPE_NOTIFICATION = 1;

    /**
     * Vibrate setting that suggests to never vibrate.
     *
     * @see #setVibrateSetting(int, int)
     * @see #getVibrateSetting(int)
     */
    public static final int VIBRATE_SETTING_OFF = 0;

    /**
     * Vibrate setting that suggests to vibrate when possible.
     *
     * @see #setVibrateSetting(int, int)
     * @see #getVibrateSetting(int)
     */
    public static final int VIBRATE_SETTING_ON = 1;

    /**
     * Vibrate setting that suggests to only vibrate when in the vibrate ringer
     * mode.
     *
     * @see #setVibrateSetting(int, int)
     * @see #getVibrateSetting(int)
     */
    public static final int VIBRATE_SETTING_ONLY_SILENT = 2;

    /**
     * Suggests using the default stream type. This may not be used in all
     * places a stream type is needed.
     */
    public static final int USE_DEFAULT_STREAM_TYPE = Integer.MIN_VALUE;

    //private static IAudioService sService;

    /**
     * @hide
     */
    public AudioManager(Context context) {
        mContext = context;
        //mHandler = new Handler(context.getMainLooper());
    }

    //TODO: the following functions are temp removed, add them back when necessary

    /**
     * @j2sNative
     * console.log("Missing method: unregisterMediaButtonEventReceiver");
     */
    @MayloonStubAnnotation()
    public void unregisterMediaButtonEventReceiver(ComponentName eventReceiver) {
        System.out.println("Stub"
                + " Function : unregisterMediaButtonEventReceiver");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getParameters");
     */
    @MayloonStubAnnotation()
    public String getParameters(String keys) {
        System.out.println("Stub" + " Function : getParameters");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setWiredHeadsetOn");
     */
    @MayloonStubAnnotation()
    public void setWiredHeadsetOn(boolean on) {
        System.out.println("Stub" + " Function : setWiredHeadsetOn");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: loadSoundEffects");
     */
    @MayloonStubAnnotation()
    public void loadSoundEffects() {
        System.out.println("Stub" + " Function : loadSoundEffects");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setBluetoothScoOn");
     */
    @MayloonStubAnnotation()
    public void setBluetoothScoOn(boolean on) {
        System.out.println("Stub" + " Function : setBluetoothScoOn");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setBluetoothA2dpOn");
     */
    @MayloonStubAnnotation()
    public void setBluetoothA2dpOn(boolean on) {
        System.out.println("Stub" + " Function : setBluetoothA2dpOn");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getRouting");
     */
    @MayloonStubAnnotation()
    public int getRouting(int mode) {
        System.out.println("Stub" + " Function : getRouting");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isMicrophoneMute");
     */
    @MayloonStubAnnotation()
    public boolean isMicrophoneMute() {
        System.out.println("Stub" + " Function : isMicrophoneMute");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setMicrophoneMute");
     */
    @MayloonStubAnnotation()
    public void setMicrophoneMute(boolean on) {
        System.out.println("Stub" + " Function : setMicrophoneMute");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getRingerMode");
     */
    @MayloonStubAnnotation()
    public int getRingerMode() {
        System.out.println("Stub" + " Function : getRingerMode");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getStreamVolume");
     */
    @MayloonStubAnnotation()
    public int getStreamVolume(int streamType) {
        System.out.println("Stub" + " Function : getStreamVolume");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isSpeakerphoneOn");
     */
    @MayloonStubAnnotation()
    public boolean isSpeakerphoneOn() {
        System.out.println("Stub" + " Function : isSpeakerphoneOn");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setMode");
     */
    @MayloonStubAnnotation()
    public void setMode(int mode) {
        System.out.println("Stub" + " Function : setMode");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setRingerMode");
     */
    @MayloonStubAnnotation()
    public void setRingerMode(int ringerMode) {
        System.out.println("Stub" + " Function : setRingerMode");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: shouldVibrate");
     */
    @MayloonStubAnnotation()
    public boolean shouldVibrate(int vibrateType) {
        System.out.println("Stub" + " Function : shouldVibrate");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: abandonAudioFocus");
     */
    @MayloonStubAnnotation()
    public int abandonAudioFocus(Object audioFocusChangeListener) {
        System.out.println("Stub" + " Function : abandonAudioFocus");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isWiredHeadsetOn");
     */
    @MayloonStubAnnotation()
    public boolean isWiredHeadsetOn() {
        System.out.println("Stub" + " Function : isWiredHeadsetOn");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isBluetoothScoAvailableOffCall");
     */
    @MayloonStubAnnotation()
    public boolean isBluetoothScoAvailableOffCall() {
        System.out.println("Stub" + " Function : isBluetoothScoAvailableOffCall");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setParameter");
     */
    @MayloonStubAnnotation()
    public void setParameter(String key, String value) {
        System.out.println("Stub" + " Function : setParameter");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: startBluetoothSco");
     */
    @MayloonStubAnnotation()
    public void startBluetoothSco() {
        System.out.println("Stub" + " Function : startBluetoothSco");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: stopBluetoothSco");
     */
    @MayloonStubAnnotation()
    public void stopBluetoothSco() {
        System.out.println("Stub" + " Function : stopBluetoothSco");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: playSoundEffect");
     */
    @MayloonStubAnnotation()
    public void playSoundEffect(int effectType) {
        System.out.println("Stub" + " Function : playSoundEffect");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isBluetoothA2dpOn");
     */
    @MayloonStubAnnotation()
    public boolean isBluetoothA2dpOn() {
        System.out.println("Stub" + " Function : isBluetoothA2dpOn");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setParameters");
     */
    @MayloonStubAnnotation()
    public void setParameters(String keyValuePairs) {
        System.out.println("Stub" + " Function : setParameters");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: unloadSoundEffects");
     */
    @MayloonStubAnnotation()
    public void unloadSoundEffects() {
        System.out.println("Stub" + " Function : unloadSoundEffects");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getStreamMaxVolume");
     */
    @MayloonStubAnnotation()
    public int getStreamMaxVolume(int streamType) {
        System.out.println("Stub" + " Function : getStreamMaxVolume");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getMode");
     */
    @MayloonStubAnnotation()
    public int getMode() {
        System.out.println("Stub" + " Function : getMode");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getVibrateSetting");
     */
    @MayloonStubAnnotation()
    public int getVibrateSetting(int vibrateType) {
        System.out.println("Stub" + " Function : getVibrateSetting");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setSpeakerphoneOn");
     */
    @MayloonStubAnnotation()
    public void setSpeakerphoneOn(boolean on) {
        System.out.println("Stub" + " Function : setSpeakerphoneOn");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isBluetoothScoOn");
     */
    @MayloonStubAnnotation()
    public boolean isBluetoothScoOn() {
        System.out.println("Stub" + " Function : isBluetoothScoOn");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isMusicActive");
     */
    @MayloonStubAnnotation()
    public boolean isMusicActive() {
        System.out.println("Stub" + " Function : isMusicActive");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: playSoundEffect");
     */
    @MayloonStubAnnotation()
    public void playSoundEffect(int effectType, float volume) {
        System.out.println("Stub" + " Function : playSoundEffect");
        return;
    }
}
