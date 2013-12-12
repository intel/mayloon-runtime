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

package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioManager;

import java.io.FileDescriptor;
//import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.lang.ref.WeakReference;


public class MediaPlayer
{
    /**
       Constant to retrieve only the new metadata since the last
       call.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean METADATA_UPDATE_ONLY = true;

    /**
       Constant to retrieve all the metadata.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean METADATA_ALL = false;

    /**
       Constant to enable the metadata filter during retrieval.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean APPLY_METADATA_FILTER = true;

    /**
       Constant to disable the metadata filter during retrieval.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean BYPASS_METADATA_FILTER = false;

    private final static String TAG = "MediaPlayer";
    // Name of the remote interface for the media player. Must be kept
    // in sync with the 2nd parameter of the IMPLEMENT_META_INTERFACE
    // macro invocation in IMediaPlayer.cpp
    private final static String IMEDIA_PLAYER = "android.media.IMediaPlayer";

    private int mNativeContext; // accessed by native methods
    private int mListenerContext; // accessed by native methods
    private Surface mSurface; // accessed by native methods
    private SurfaceHolder  mSurfaceHolder;
    private EventHandler mEventHandler;
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;

    private String mDirectPath; // for URL/File play back
    private String mPlayer; // the Player object (NOT a string object)
    private Canvas mJavaCanvas; // Target to draw the video frames
    private boolean isMediaPlaying = false;
    private boolean mLooping = false; // auto rewind and replay???
    private double mPlayerVolume = 1.0;

    /**
     * Default constructor. Consider using one of the create() methods for
     * synchronously instantiating a MediaPlayer from a Uri or resource.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances may
     * result in an exception.</p>
     */
    public MediaPlayer() {

        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        /* Native setup requires a weak reference to our object.
         * It's easier to create it here than in C++.
         */
        //native_setup(new WeakReference<MediaPlayer>(this));
    }

    /*
     * Update the MediaPlayer ISurface. Call after updating mSurface.
     */
    private native void _setVideoSurface();

    /**
     * Create a request parcel which can be routed to the native media
     * player using {@link #invoke(Parcel, Parcel)}. The Parcel
     * returned has the proper InterfaceToken set. The caller should
     * not overwrite that token, i.e it can only append data to the
     * Parcel.
     *
     * @return A parcel suitable to hold a request for the native
     * player.
     * {@hide}
     */
    public Parcel newRequest() {
        Parcel parcel = Parcel.obtain();
        parcel.writeInterfaceToken(IMEDIA_PLAYER);
        return parcel;
    }

    /**
     * Invoke a generic method on the native player using opaque
     * parcels for the request and reply. Both payloads' format is a
     * convention between the java caller and the native player.
     * Must be called after setDataSource to make sure a native player
     * exists.
     *
     * @param request Parcel with the data for the extension. The
     * caller must use {@link #newRequest()} to get one.
     *
     * @param reply Output parcel with the data returned by the
     * native player.
     *
     * @return The status code see utils/Errors.h
     * {@hide}
     */
    public int invoke(Parcel request, Parcel reply) {
        int retcode = native_invoke(request, reply);
        reply.setDataPosition(0);
        return retcode;
    }

    /**
     * Sets the SurfaceHolder to use for displaying the video portion of the media.
     * This call is optional. Not calling it when playing back a video will
     * result in only the audio track being played.
     *
     * @param sh the SurfaceHolder to use for video display
     */
    public void setDisplay(SurfaceHolder sh) {
        mSurfaceHolder = sh;
        //if (sh != null) {
        //    mSurface = sh.getSurface();
        //} else {
        //    mSurface = null;
        //}
        //_setVideoSurface();
        //updateSurfaceScreenOn();
    }

    /**
     * Convenience method to create a MediaPlayer for a given Uri.
     * On success, {@link #prepare()} will already have been called and must not be called again.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances will
     * result in an exception.</p>
     *
     * @param context the Context to use
     * @param uri the Uri from which to get the datasource
     * @return a MediaPlayer object, or null if creation failed
     */
    public static MediaPlayer create(Context context, Uri uri) {
        return create (context, uri, null);
    }

    /**
     * Convenience method to create a MediaPlayer for a given Uri.
     * On success, {@link #prepare()} will already have been called and must not be called again.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances will
     * result in an exception.</p>
     *
     * @param context the Context to use
     * @param uri the Uri from which to get the datasource
     * @param holder the SurfaceHolder to use for displaying the video
     * @return a MediaPlayer object, or null if creation failed
     */
    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder) {

        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(context, uri);
            if (holder != null) {
                mp.setDisplay(holder);
            }
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (SecurityException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        }

        return null;
    }

    /**
     * Convenience method to create a MediaPlayer for a given resource id.
     * On success, {@link #prepare()} will already have been called and must not be called again.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances will
     * result in an exception.</p>
     *
     * @param context the Context to use
     * @param resid the raw resource id (<var>R.raw.&lt;something></var>) for
     *              the resource to use as the datasource
     * @return a MediaPlayer object, or null if creation failed
     */
    public static MediaPlayer create(Context context, int resid) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) return null;

            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(afd.getRealPath());
            //mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            //afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "create failed:", ex);
           // fall through
        } catch (SecurityException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        }
        return null;
    }

    /**
     * Sets the data source as a content Uri.
     *
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(Context context, Uri uri)
        throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, null);
    }

    /**
     * Sets the data source as a content Uri.
     *
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @param headers the headers to be sent together with the request for the data
     * @throws IllegalStateException if it is called in an invalid state
     * @hide pending API council
     */
    public void setDataSource(Context context, Uri uri, Map<String, String> headers)
        throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

        String scheme = uri.getScheme();
        if(scheme == null || scheme.equals("file")) {
            setDataSource(uri.getPath());
            return;
        }

        return;
    }

    /**
     * Sets the data source (file-path or http/rtsp URL) to use.
     *
     * @param path the path of the file, or the http/rtsp URL of the stream you want to play
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(String path) throws IOException, IllegalArgumentException, IllegalStateException {
        mDirectPath = path;
    }

    /**
     * Sets the data source (file-path or http/rtsp URL) to use.
     *
     * @param path the path of the file, or the http/rtsp URL of the stream you want to play
     * @param headers the headers associated with the http request for the stream you want to play
     * @throws IllegalStateException if it is called in an invalid state
     * @hide pending API council
     */
    public native void setDataSource(String path,  Map<String, String> headers)
            throws IOException, IllegalArgumentException, IllegalStateException;

    /**
     * Sets the data source (FileDescriptor) to use. It is the caller's responsibility
     * to close the file descriptor. It is safe to do so as soon as this call returns.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        //// intentionally less than LONG_MAX
        //setDataSource(fd, 0, 0x7ffffffffffffffL);
    }

    /**
     * Sets the data source (FileDescriptor) to use.  The FileDescriptor must be
     * seekable (N.B. a LocalSocket is not seekable). It is the caller's responsibility
     * to close the file descriptor. It is safe to do so as soon as this call returns.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @param offset the offset into the file where the data to be played starts, in bytes
     * @param length the length in bytes of the data to be played
     * @throws IllegalStateException if it is called in an invalid state
     */
    public native void setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException;

    public void _prepare() {

        /**
         * @j2sNative
         * if ( this.mPlayer == null ) {
         *     this.mPlayer = document.createElement('video');
         * }
         * this.mPlayer.src = this.mDirectPath;
         * this.mPlayer.loop = this.mLooping;
         * this.mPlayer.volume = this.mPlayerVolume;
         * 
         */ {}
    }
    
    private void _setupCanvas() {
        if ( mSurfaceHolder != null ) {
            mJavaCanvas = mSurfaceHolder.lockCanvas();

            /**
             * @j2sNative
             * var that = this;
             * var canvas = document.getElementById(this.mJavaCanvas.getHTML5CanvasID());
             * //alert(mJavaCanvas.getHTML5CanvasID());
             * //var canvas = document.getElementById('1_fgCanvas');
             * var intId = setInterval(function() {
             *     if ( that.mPlayer == null ) {
             *         clearInterval(intId);
             *     } else {
             *         var ctx = canvas.getContext("2d");
             *         try {
             *             //ctx.drawImage(that.mPlayer, 0, 0,
             *             //              that.mPlayer.videoWidth, that.mPlayer.videoHeight);
             *             ctx.drawImage(that.mPlayer, 0, 0);
             *         } catch ( e ) {
             *             if ( e.name == "INDEX_SIZE_ERR" ) {
             *                 // we're playing an audio stream
             *                 clearInterval(intId); // No longer try to draw video on surface
             *             }
             *         }
             *     }
             * }, 33);
             *
             */ { }
        }
    }
    /**
     * Prepares the player for playback, synchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For files, it is OK to call prepare(),
     * which blocks until MediaPlayer is ready for playback.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void prepare() throws IOException, IllegalStateException {
        _prepare();
    }

    /**
     * Prepares the player for playback, asynchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For streams, you should call prepareAsync(),
     * which returns immediately, rather than blocking until enough data has been
     * buffered.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public native void prepareAsync() throws IllegalStateException;

    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had
     * been stopped, or never started before, playback will start at the
     * beginning.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public  void start() throws IllegalStateException {
        //stayAwake(true);
        _setupCanvas();
        _start();
    }

	private void _start() throws IllegalStateException {
		/**
		 * @j2sNative
		 * 
         * if ( this.mPlayer == null ) {
         *     this._prepare();
         * }
		 * 
		 * this.mPlayer.play();
		 * 
		 * // Set the flag
		 * this.isMediaPlaying = true;
		 * 
		 * var that = this;
		 * this.mPlayer.addEventListener('ended', function (e) {
		 *     that.isMediaPlaying = false;
		 * }, false);
		 * 
		 * this.mPlayer.addEventListener('error', function (e) {
		 *     that.isMediaPlaying = false;
		 *     // TODO: that.mOnErrorListener
		 * }, false);
		 * 
		 */ { }
	}

    /**
     * Stops playback after playback has been stopped or paused.
     *
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     */
    public void stop() throws IllegalStateException {
        //stayAwake(false);
        _stop();
    }

    private void _stop() throws IllegalStateException {
    	// Release canvas
    	mSurfaceHolder.unlockCanvasAndPost(mJavaCanvas);
    	mJavaCanvas = null;
    	
        /**
         * @j2sNative
         * this.mPlayer.pause();
         */ { }
    }

    /**
     * Pauses playback. Call start() to resume.
     *
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     */
    public void pause() throws IllegalStateException {
        //stayAwake(false);
        _pause();
    }

    private void _pause() throws IllegalStateException {
        /**
         * @j2sNative
         * this.mPlayer.pause();
         */ { }
         isMediaPlaying = false;
    }

    /**
     * Set the low-level power management behavior for this MediaPlayer.  This
     * can be used when the MediaPlayer is not playing through a SurfaceHolder
     * set with {@link #setDisplay(SurfaceHolder)} and thus can use the
     * high-level {@link #setScreenOnWhilePlaying(boolean)} feature.
     *
     * <p>This function has the MediaPlayer access the low-level power manager
     * service to control the device's power usage while playing is occurring.
     * The parameter is a combination of {@link android.os.PowerManager} wake flags.
     * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
     * permission.
     * By default, no attempt is made to keep the device awake during playback.
     *
     * @param context the Context to use
     * @param mode    the power/wake mode to set
     * @see android.os.PowerManager
     */
    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode|PowerManager.ON_AFTER_RELEASE, MediaPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }

    /**
     * Control whether we should use the attached SurfaceHolder to keep the
     * screen on while video playback is occurring.  This is the preferred
     * method over {@link #setWakeMode} where possible, since it doesn't
     * require that the application have permission for low-level wake lock
     * access.
     *
     * @param screenOn Supply true to keep the screen on, false to allow it
     * to turn off.
     */
    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (mScreenOnWhilePlaying != screenOn) {
            mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    /**
     * Returns the width of the video.
     *
     * @return the width of the video, or 0 if there is no video,
     * no display surface was set, or the width has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the width is available.
     */
    public int getVideoWidth() {
		/**
		 * @j2sNative
		 * 
	     * if ( this.mPlayer != null ) {
	     *     return this.mPlayer.videoWidth;
	     * }
		 * */ {}
    	return 0;
    }

    /**
     * Returns the height of the video.
     *
     * @return the height of the video, or 0 if there is no video,
     * no display surface was set, or the height has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the height is available.
     */
    public int getVideoHeight() {
		/**
		 * @j2sNative
		 * 
	     * if ( this.mPlayer != null ) {
	     *     return this.mPlayer.videoHeight;
	     * }
		 * */ {}
    	return 0;
    }

    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     */
    public boolean isPlaying() {
    	return this.isMediaPlaying;
    }

    /**
     * Seeks to specified time position.
     *
     * @param msec the offset in milliseconds from the start to seek to
     * @throws IllegalStateException if the internal player engine has not been
     * initialized
     */
    public void seekTo(int msec) throws IllegalStateException {
        /**
         * @j2sNative
         * if ( this.mPlayer != null ) {
         *     this.mPlayer.currentTime = msec / 1000.0;
         * }	
         */ {}
    }

    /**
     * Gets the current playback position.
     *
     * @return the current position in milliseconds
     */
    public int getCurrentPosition() {
        /**
         * @j2sNative
         * if ( this.mPlayer != null ) {
         *     return this.mPlayer.currentTime * 1000;
         * }	
         */ {}
    	return 0;
    }

    /**
     * Gets the duration of the file.
     *
     * @return the duration in milliseconds
     */
    public int getDuration() {
        /**
         * @j2sNative
         * if ( this.mPlayer != null ) {
         *     return this.mPlayer.duration * 1000;
         * }	
         */ {}
    	return 0;
    }

    /**
     * Gets the media metadata.
     *
     * @param update_only controls whether the full set of available
     * metadata is returned or just the set that changed since the
     * last call. See {@see #METADATA_UPDATE_ONLY} and {@see
     * #METADATA_ALL}.
     *
     * @param apply_filter if true only metadata that matches the
     * filter is returned. See {@see #APPLY_METADATA_FILTER} and {@see
     * #BYPASS_METADATA_FILTER}.
     *
     * @return The metadata, possibly empty. null if an error occured.
     // FIXME: unhide.
     * {@hide}
     */
    /*public Metadata getMetadata(final boolean update_only,
                                final boolean apply_filter) {
        Parcel reply = Parcel.obtain();
        Metadata data = new Metadata();

        if (!native_getMetadata(update_only, apply_filter, reply)) {
            reply.recycle();
            return null;
        }

        // Metadata takes over the parcel, don't recycle it unless
        // there is an error.
        if (!data.parse(reply)) {
            reply.recycle();
            return null;
        }
        return data;
    }*/

    /**
     * Set a filter for the metadata update notification and update
     * retrieval. The caller provides 2 set of metadata keys, allowed
     * and blocked. The blocked set always takes precedence over the
     * allowed one.
     * Metadata.MATCH_ALL and Metadata.MATCH_NONE are 2 sets available as
     * shorthands to allow/block all or no metadata.
     *
     * By default, there is no filter set.
     *
     * @param allow Is the set of metadata the client is interested
     *              in receiving new notifications for.
     * @param block Is the set of metadata the client is not interested
     *              in receiving new notifications for.
     * @return The call status code.
     *
     // FIXME: unhide.
     * {@hide}
     */
    public int setMetadataFilter(Set<Integer> allow, Set<Integer> block) {
        // Do our serialization manually instead of calling
        // Parcel.writeArray since the sets are made of the same type
        // we avoid paying the price of calling writeValue (used by
        // writeArray) which burns an extra int per element to encode
        // the type.
        Parcel request =  newRequest();

        // The parcel starts already with an interface token. There
        // are 2 filters. Each one starts with a 4bytes number to
        // store the len followed by a number of int (4 bytes as well)
        // representing the metadata type.
        //int capacity = request.dataSize() + 4 * (1 + allow.size() + 1 + block.size());
        //
        //if (request.dataCapacity() < capacity) {
        //    request.setDataCapacity(capacity);
        //}

        request.writeInt(allow.size());
        for(Integer t: allow) {
            request.writeInt(t);
        }
        request.writeInt(block.size());
        for(Integer t: block) {
            request.writeInt(t);
        }
        return native_setMetadataFilter(request);
    }

    /**
     * Releases resources associated with this MediaPlayer object.
     * It is considered good practice to call this method when you're
     * done using the MediaPlayer.
     */
    public void release() {
        //stayAwake(false);
        //updateSurfaceScreenOn();
        mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnVideoSizeChangedListener = null;
        _release();
    }

    private void _release() {
    	/**
    	 * @j2sNative
    	 * this.mPlayer.pause();
    	 * this.mPlayer.src = "";
    	 * this.mPlayer = null;
    	 */ {}
    }

    /**
     * Resets the MediaPlayer to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling prepare().
     */
    public void reset() {
        stayAwake(false);
        _reset();
        // make sure none of the listeners get called anymore
        mEventHandler.removeCallbacksAndMessages(null);
    }

    private native void _reset();

    /**
     * Suspends the MediaPlayer. The only methods that may be called while
     * suspended are {@link #reset()}, {@link #release()} and {@link #resume()}.
     * MediaPlayer will release its hardware resources as far as
     * possible and reasonable. A successfully suspended MediaPlayer will
     * cease sending events.
     * If suspension is successful, this method returns true, otherwise
     * false is returned and the player's state is not affected.
     * @hide
     */
    public boolean suspend() {
        if (native_suspend_resume(true) < 0) {
            return false;
        }

        stayAwake(false);

        // make sure none of the listeners get called anymore
        mEventHandler.removeCallbacksAndMessages(null);

        return true;
    }

    /**
     * Resumes the MediaPlayer. Only to be called after a previous (successful)
     * call to {@link #suspend()}.
     * MediaPlayer will return to a state close to what it was in before
     * suspension.
     * @hide
     */
    public boolean resume() {
        if (native_suspend_resume(false) < 0) {
            return false;
        }

        if (isPlaying()) {
            stayAwake(true);
        }

        return true;
    }

    /**
     * @hide
     */
    private native int native_suspend_resume(boolean isSuspend);

    /**
     * Sets the audio stream type for this MediaPlayer. See {@link AudioManager}
     * for a list of stream types. Must call this method before prepare() or
     * prepareAsync() in order for the target stream type to become effective
     * thereafter.
     *
     * @param streamtype the audio stream type
     * @see android.media.AudioManager
     */
    public void setAudioStreamType(int streamtype) {
    	
    }

    /**
     * Sets the player to be looping or non-looping.
     *
     * @param looping whether to loop or not
     */
    public void setLooping(boolean looping) {
    	mLooping = looping;
    	/**
    	 * @j2sNative
    	 * if ( this.mPlayer != null ) {
    	 *     this.mPlayer.loop = looping;
    	 * }
    	 * 
    	 */ {}
    }

    /**
     * Checks whether the MediaPlayer is looping or non-looping.
     *
     * @return true if the MediaPlayer is currently looping, false otherwise
     */
    public boolean isLooping() {
    	return mLooping;
    }

    /**
     * Sets the volume on this player.
     * This API is recommended for balancing the output of audio streams
     * within an application. Unless you are writing an application to
     * control user settings, this API should be used in preference to
     * {@link AudioManager#setStreamVolume(int, int, int)} which sets the volume of ALL streams of
     * a particular type. Note that the passed volume values are raw scalars.
     * UI controls should be scaled logarithmically.
     *
     * @param leftVolume left volume scalar
     * @param rightVolume right volume scalar
     */
    public void setVolume(float leftVolume, float rightVolume) {

    	if ( rightVolume < leftVolume ) {
    	   leftVolume = rightVolume; // Use the min one
    	}
    	this.mPlayerVolume = leftVolume;
   	    
    	/**
    	 * @j2sNative
    	 * if ( this.mPlayer != null ) {
    	 *     this.mPlayer.volume = this.mPlayerVolume; // also in range [0.0f ~ 1.0f]
    	 * }
    	 */ {}
    }

    /**
     * Currently not implemented, returns null.
     * @deprecated
     * @hide
     */
    public native Bitmap getFrameAt(int msec) throws IllegalStateException;

    /**
     * Sets the audio session ID.
     *
     * @param sessionId the audio session ID.
     * The audio session ID is a system wide unique identifier for the audio stream played by
     * this MediaPlayer instance.
     * The primary use of the audio session ID  is to associate audio effects to a particular
     * instance of MediaPlayer: if an audio session ID is provided when creating an audio effect,
     * this effect will be applied only to the audio content of media players within the same
     * audio session and not to the output mix.
     * When created, a MediaPlayer instance automatically generates its own audio session ID.
     * However, it is possible to force this player to be part of an already existing audio session
     * by calling this method.
     * This method must be called before one of the overloaded <code> setDataSource </code> methods.
     * @throws IllegalStateException if it is called in an invalid state
     */
    public native void setAudioSessionId(int sessionId)  throws IllegalArgumentException, IllegalStateException;

    /**
     * Returns the audio session ID.
     *
     * @return the audio session ID. {@see #setAudioSessionId(int)}
     * Note that the audio session ID is 0 only if a problem occured when the MediaPlayer was contructed.
     */
    public native int getAudioSessionId();

    /**
     * Attaches an auxiliary effect to the player. A typical auxiliary effect is a reverberation
     * effect which can be applied on any sound source that directs a certain amount of its
     * energy to this effect. This amount is defined by setAuxEffectSendLevel().
     * {@see #setAuxEffectSendLevel(float)}.
     * <p>After creating an auxiliary effect (e.g.
     * {@link android.media.audiofx.EnvironmentalReverb}), retrieve its ID with
     * {@link android.media.audiofx.AudioEffect#getId()} and use it when calling this method
     * to attach the player to the effect.
     * <p>To detach the effect from the player, call this method with a null effect id.
     * <p>This method must be called after one of the overloaded <code> setDataSource </code>
     * methods.
     * @param effectId system wide unique id of the effect to attach
     */
    public native void attachAuxEffect(int effectId);

    /**
     * Sets the send level of the player to the attached auxiliary effect
     * {@see #attachAuxEffect(int)}. The level value range is 0 to 1.0.
     * <p>By default the send level is 0, so even if an effect is attached to the player
     * this method must be called for the effect to be applied.
     * <p>Note that the passed level value is a raw scalar. UI controls should be scaled
     * logarithmically: the gain applied by audio framework ranges from -72dB to 0dB,
     * so an appropriate conversion from linear UI input x to level is:
     * x == 0 -> level = 0
     * 0 < x <= R -> level = 10^(72*(x-R)/20/R)
     * @param level send level scalar
     */
    public native void setAuxEffectSendLevel(float level);

    /**
     * @param request Parcel destinated to the media player. The
     *                Interface token must be set to the IMediaPlayer
     *                one to be routed correctly through the system.
     * @param reply[out] Parcel that will contain the reply.
     * @return The status code.
     */
    private native final int native_invoke(Parcel request, Parcel reply);


    /**
     * @param update_only If true fetch only the set of metadata that have
     *                    changed since the last invocation of getMetadata.
     *                    The set is built using the unfiltered
     *                    notifications the native player sent to the
     *                    MediaPlayerService during that period of
     *                    time. If false, all the metadatas are considered.
     * @param apply_filter  If true, once the metadata set has been built based on
     *                     the value update_only, the current filter is applied.
     * @param reply[out] On return contains the serialized
     *                   metadata. Valid only if the call was successful.
     * @return The status code.
     */
    private native final boolean native_getMetadata(boolean update_only,
                                                    boolean apply_filter,
                                                    Parcel reply);

    /**
     * @param request Parcel with the 2 serialized lists of allowed
     *                metadata types followed by the one to be
     *                dropped. Each list starts with an integer
     *                indicating the number of metadata type elements.
     * @return The status code.
     */
    private native final int native_setMetadataFilter(Parcel request);
 
    private native final void native_setup(Object mediaplayer_this);
    private native final void native_finalize();

    @Override
    protected void finalize() { native_finalize(); }

    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    private static final int MEDIA_NOP = 0; // interface test message
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_ERROR = 100;
    private static final int MEDIA_INFO = 200;

    private class EventHandler extends Handler
    {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mp, Looper looper) {
            super(looper);
            mMediaPlayer = mp;
        }

        @Override
        public void handleMessage(Message msg) {
            if (mMediaPlayer.mNativeContext == 0) {
                Log.w(TAG, "mediaplayer went away with unhandled events");
                return;
            }
            switch(msg.what) {
            case MEDIA_PREPARED:
                if (mOnPreparedListener != null)
                    mOnPreparedListener.onPrepared(mMediaPlayer);
                return;

            case MEDIA_PLAYBACK_COMPLETE:
                if (mOnCompletionListener != null)
                    mOnCompletionListener.onCompletion(mMediaPlayer);
                stayAwake(false);
                return;

            case MEDIA_BUFFERING_UPDATE:
                if (mOnBufferingUpdateListener != null)
                    mOnBufferingUpdateListener.onBufferingUpdate(mMediaPlayer, msg.arg1);
                return;

            case MEDIA_SEEK_COMPLETE:
              if (mOnSeekCompleteListener != null)
                  mOnSeekCompleteListener.onSeekComplete(mMediaPlayer);
              return;

            case MEDIA_SET_VIDEO_SIZE:
              if (mOnVideoSizeChangedListener != null)
                  mOnVideoSizeChangedListener.onVideoSizeChanged(mMediaPlayer, msg.arg1, msg.arg2);
              return;

            case MEDIA_ERROR:
                // For PV specific error values (msg.arg2) look in
                // opencore/pvmi/pvmf/include/pvmf_return_codes.h
                Log.e(TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                boolean error_was_handled = false;
                if (mOnErrorListener != null) {
                    error_was_handled = mOnErrorListener.onError(mMediaPlayer, msg.arg1, msg.arg2);
                }
                if (mOnCompletionListener != null && ! error_was_handled) {
                    mOnCompletionListener.onCompletion(mMediaPlayer);
                }
                stayAwake(false);
                return;

            case MEDIA_INFO:
                // For PV specific code values (msg.arg2) look in
                // opencore/pvmi/pvmf/include/pvmf_return_codes.h
                Log.i(TAG, "Info (" + msg.arg1 + "," + msg.arg2 + ")");
                if (mOnInfoListener != null) {
                    mOnInfoListener.onInfo(mMediaPlayer, msg.arg1, msg.arg2);
                }
                // No real default action so far.
                return;

            case MEDIA_NOP: // interface test message - ignore
                break;

            default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }
    }

    /**
     * Called from native code when an interesting event happens.  This method
     * just uses the EventHandler system to post the event back to the main app thread.
     * We use a weak reference to the original MediaPlayer object so that the native
     * code is safe from the object disappearing from underneath it.  (This is
     * the cookie passed to native_setup().)
     */
    private static void postEventFromNative(Object mediaplayer_ref,
                                            int what, int arg1, int arg2, Object obj)
    {
        MediaPlayer mp = (MediaPlayer)((WeakReference)mediaplayer_ref).get();
        if (mp == null) {
            return;
        }

        if (mp.mEventHandler != null) {
            Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mp.mEventHandler.sendMessage(m);
        }
    }

    /**
     * Interface definition for a callback to be invoked when the media
     * source is ready for playback.
     */
    public interface OnPreparedListener
    {
        /**
         * Called when the media file is ready for playback.
         *
         * @param mp the MediaPlayer that is ready for playback
         */
        void onPrepared(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when the media source is ready
     * for playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnPreparedListener(OnPreparedListener listener)
    {
        mOnPreparedListener = listener;
    }

    private OnPreparedListener mOnPreparedListener;

    /**
     * Interface definition for a callback to be invoked when playback of
     * a media source has completed.
     */
    public interface OnCompletionListener
    {
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param mp the MediaPlayer that reached the end of the file
         */
        void onCompletion(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when the end of a media source
     * has been reached during playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener listener)
    {
        mOnCompletionListener = listener;
    }

    private OnCompletionListener mOnCompletionListener;

    /**
     * Interface definition of a callback to be invoked indicating buffering
     * status of a media resource being streamed over the network.
     */
    public interface OnBufferingUpdateListener
    {
        /**
         * Called to update status in buffering a media stream.
         *
         * @param mp      the MediaPlayer the update pertains to
         * @param percent the percentage (0-100) of the buffer
         *                that has been filled thus far
         */
        void onBufferingUpdate(MediaPlayer mp, int percent);
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     *
     * @param listener the callback that will be run.
     */
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener)
    {
        mOnBufferingUpdateListener = listener;
    }

    private OnBufferingUpdateListener mOnBufferingUpdateListener;

    /**
     * Interface definition of a callback to be invoked indicating
     * the completion of a seek operation.
     */
    public interface OnSeekCompleteListener
    {
        /**
         * Called to indicate the completion of a seek operation.
         *
         * @param mp the MediaPlayer that issued the seek operation
         */
        public void onSeekComplete(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param listener the callback that will be run
     */
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener)
    {
        mOnSeekCompleteListener = listener;
    }

    private OnSeekCompleteListener mOnSeekCompleteListener;

    /**
     * Interface definition of a callback to be invoked when the
     * video size is first known or updated
     */
    public interface OnVideoSizeChangedListener
    {
        /**
         * Called to indicate the video size
         *
         * @param mp        the MediaPlayer associated with this callback
         * @param width     the width of the video
         * @param height    the height of the video
         */
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height);
    }

    /**
     * Register a callback to be invoked when the video size is
     * known or updated.
     *
     * @param listener the callback that will be run
     */
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener)
    {
        mOnVideoSizeChangedListener = listener;
    }

    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;

    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    /** Unspecified media player error.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_UNKNOWN = 1;

    /** Media server died. In this case, the application must release the
     * MediaPlayer object and instantiate a new one.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_SERVER_DIED = 100;

    /** The video is streamed and its container is not valid for progressive
     * playback i.e the video's index (e.g moov atom) is not at the start of the
     * file.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;

    /**
     * Interface definition of a callback to be invoked when there
     * has been an error during an asynchronous operation (other errors
     * will throw exceptions at method call time).
     */
    public interface OnErrorListener
    {
        /**
         * Called to indicate an error.
         *
         * @param mp      the MediaPlayer the error pertains to
         * @param what    the type of error that has occurred:
         * <ul>
         * <li>{@link #MEDIA_ERROR_UNKNOWN}
         * <li>{@link #MEDIA_ERROR_SERVER_DIED}
         * </ul>
         * @param extra an extra code, specific to the error. Typically
         * implementation dependant.
         * @return True if the method handled the error, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the OnCompletionListener to be called.
         */
        boolean onError(MediaPlayer mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an error has happened
     * during an asynchronous operation.
     *
     * @param listener the callback that will be run
     */
    public void setOnErrorListener(OnErrorListener listener)
    {
        mOnErrorListener = listener;
    }

    private OnErrorListener mOnErrorListener;


    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    /** Unspecified media player info.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_UNKNOWN = 1;

    /** The video is too complex for the decoder: it can't decode frames fast
     *  enough. Possibly only the audio plays fine at this stage.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;

    /** MediaPlayer is temporarily pausing playback internally in order to
     * buffer more data.
     */
    public static final int MEDIA_INFO_BUFFERING_START = 701;

    /** MediaPlayer is resuming playback after filling buffers.
     */
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    /** Bad interleaving means that a media has been improperly interleaved or
     * not interleaved at all, e.g has all the video samples first then all the
     * audio ones. Video is playing but a lot of disk seeks may be happening.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;

    /** The media cannot be seeked (e.g live stream)
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;

    /** A new set of metadata is available.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;

    /**
     * Interface definition of a callback to be invoked to communicate some
     * info and/or warning about the media or its playback.
     */
    public interface OnInfoListener
    {
        /**
         * Called to indicate an info or a warning.
         *
         * @param mp      the MediaPlayer the info pertains to.
         * @param what    the type of info or warning.
         * <ul>
         * <li>{@link #MEDIA_INFO_UNKNOWN}
         * <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
         * <li>{@link #MEDIA_INFO_BAD_INTERLEAVING}
         * <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
         * <li>{@link #MEDIA_INFO_METADATA_UPDATE}
         * </ul>
         * @param extra an extra code, specific to the info. Typically
         * implementation dependant.
         * @return True if the method handled the info, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the info to be discarded.
         */
        boolean onInfo(MediaPlayer mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an info/warning is available.
     *
     * @param listener the callback that will be run
     */
    public void setOnInfoListener(OnInfoListener listener)
    {
        mOnInfoListener = listener;
    }

    private OnInfoListener mOnInfoListener;

}
