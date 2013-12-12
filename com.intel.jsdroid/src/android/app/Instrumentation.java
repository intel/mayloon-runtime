package android.app;

import java.util.ArrayList;
import java.util.List;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.app.ActivityThread.ApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.MessageQueue;
import android.os.RemoteException;
import android.util.ActivityNotFoundException;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.widget.Toast;

public class Instrumentation {
	/**
     * If included in the status or final bundle sent to an IInstrumentationWatcher, this key 
     * identifies the class that is writing the report.  This can be used to provide more structured
     * logging or reporting capabilities in the IInstrumentationWatcher.
     */
    public static final String REPORT_KEY_IDENTIFIER = "id";
    
    private static final String TAG = "Instrumentation";
    /**
     * If included in the status or final bundle sent to an IInstrumentationWatcher, this key 
     * identifies a string which can simply be printed to the output stream.  Using these streams
     * provides a "pretty printer" version of the status & final packets.  Any bundles including 
     * this key should also include the complete set of raw key/value pairs, so that the
     * instrumentation can also be launched, and results collected, by an automated system.
     */
	public static final String REPORT_KEY_STREAMRESULT = "stream";
	private Context mAppContext;
	private Thread mRunner;
	private static List<ActivityWaiter> mWaitingActivities;
	private MessageQueue mMessageQueue = null;
	private ActivityThread mThread = null;
	private Context mInstrContext;
	private IInstrumentationWatcher mWatcher;
	
	/*package*/ final void init(ActivityThread thread,
            Context instrContext, Context appContext, ComponentName component,
            IInstrumentationWatcher watcher) {
        mThread = thread;
        //mMessageQueue = mThread.getLooper().myQueue();
        mInstrContext = instrContext;
        mAppContext = appContext;
        //mComponent = component;
        mWatcher = watcher;
    }

	
	/**
     * Called when the instrumentation is starting, before any application code
     * has been loaded.  Usually this will be implemented to simply call
     * {@link #start} to begin the instrumentation thread, which will then
     * continue execution in {@link #onStart}.
     * 
     * <p>If you do not need your own thread -- that is you are writing your
     * instrumentation to be completely asynchronous (returning to the event
     * loop so that the application can run), you can simply begin your
     * instrumentation here, for example call {@link Context#startActivity} to
     * begin the appropriate first activity of the application. 
     *  
     * @param arguments Any additional arguments that were supplied when the 
     *                  instrumentation was started.
     */
    public void onCreate(Bundle arguments) {
    }
    
    /**
     * Create and start a new thread in which to run instrumentation.  This new
     * thread will call to {@link #onStart} where you can implement the
     * instrumentation.
     */
    public void start() {
        if (mRunner != null) {
            throw new RuntimeException("Instrumentation already started");
        }
        mRunner = new InstrumentationThread("Instr: " + getClass().getName());
        mRunner.start();
    }
    
    /**
     * Method where the instrumentation thread enters execution.  This allows
     * you to run your instrumentation code in a separate thread than the
     * application, so that it can perform blocking operation such as
     * {@link #sendKeySync} or {@link #startActivitySync}.
     * 
     * <p>You will typically want to call finish() when this function is done,
     * to end your instrumentation.
     */
    public void onStart() {
    }
    
    /**
     * Terminate instrumentation of the application.  This will cause the
     * application process to exit, removing this instrumentation from the next
     * time the application is started. 
     *  
     * @param resultCode Overall success/failure of instrumentation. 
     * @param results Any results to send back to the code that started the 
     *                instrumentation.
     */
    public void finish(int resultCode, Bundle results) {
    	/*
        if (mAutomaticPerformanceSnapshots) {
            endPerformanceSnapshot();
        }
        
        if (mPerfMetrics != null) {
            results.putAll(mPerfMetrics);
        }
        */
        mThread.finishInstrumentation(resultCode, results);
    }
	
	public static final class ActivityResult {
		/**
		 * Create a new activity result.  See {@link Activity#setResult} for 
		 * more information. 
		 *  
		 * @param resultCode The result code to propagate back to the
		 * originating activity, often RESULT_CANCELED or RESULT_OK
		 * @param resultData The data to propagate back to the originating
		 * activity.
		 */
		public ActivityResult(int resultCode, Intent resultData) {
			mResultCode = resultCode;
			mResultData = resultData;
		}

		/**
		 * Retrieve the result code contained in this result.
		 */
		public int getResultCode() {
			return mResultCode;
		}

		/**
		 * Retrieve the data contained in this result.
		 */
		public Intent getResultData() {
			return mResultData;
		}

		private final int mResultCode;
		private final Intent mResultData;
	}

	public void callActivityOnSaveInstanceState(Activity activity,
			Bundle outState) {
		activity.performSaveInstanceState(outState);
	}

	public void callActivityOnPause(Activity activity) {
		activity.performPause();
	}

	public void callActivityOnResume(Activity activity) {
		activity.onResume();
	}

	public ActivityResult execStartActivity(Context who, IBinder contextThread,
			IBinder token, Activity target, Intent intent, int requestCode) {
		System.out.println("in execStartActivity");
		ApplicationThread whoThread = (ApplicationThread) contextThread;
		try{
			int result = ((ActivityManager) Context.getSystemContext().getSystemService(
					Context.ACTIVITY_SERVICE)).startActivity(whoThread, intent,intent.resolveTypeIfNeeded(who.getContentResolver()),
					null, 0, token, target != null ? target.mEmbeddedID : null,
					requestCode, false, false);
			//Add exception indication
            if (result == -1) {
                Toast.makeText(who, "No applications can perform this action.", Toast.LENGTH_LONG).show();
            }
			checkStartActivityResult(result,intent);
		}catch(Exception e){
			
		}
		return null;
	}
    /**
     * Perform instantiation of the process's {@link Application} object.  The
     * default implementation provides the normal system behavior.
     * 
     * @param cl The ClassLoader with which to instantiate the object.
     * @param className The name of the class implementing the Application
     *                  object.
     * @param context The context to initialize the application with
     * 
     * @return The newly instantiated Application object.
     */
    public Application newApplication(String className, Context context)
            throws InstantiationException, IllegalAccessException, 
            ClassNotFoundException {
        Application app = (Application)Class.forName(className).newInstance();
        app.attach(context);
        return app;
    }
    
    /**
     * Perform calling of the application's {@link Application#onCreate}
     * method.  The default implementation simply calls through to that method.
     * 
     * @param app The application being created.
     */
    public void callApplicationOnCreate(Application app) {
        app.onCreate();
    }
    
	   /*package*/ static void checkStartActivityResult(int res, Object intent) {
	        if (res >= ActivityManager.START_SUCCESS) {
	            return;
	        }
	        
	        switch (res) {
	            case ActivityManager.START_INTENT_NOT_RESOLVED:
	            case ActivityManager.START_CLASS_NOT_FOUND:
	                if (intent instanceof Intent && ((Intent)intent).getComponent() != null)
	                    throw new ActivityNotFoundException(
	                            "Unable to find explicit activity class "
	                            + ((Intent)intent).getComponent().toShortString()
	                            + "; have you declared this activity in your AndroidManifest.xml?");
	                throw new ActivityNotFoundException(
	                        "No Activity found to handle " + intent);
	            case ActivityManager.START_PERMISSION_DENIED:
	                throw new SecurityException("Not allowed to start activity "
	                        + intent);
	            case ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT:
	                throw new AndroidRuntimeException(
	                        "FORWARD_RESULT_FLAG used while also requesting a result");
	            case ActivityManager.START_NOT_ACTIVITY:
	                throw new IllegalArgumentException(
	                        "PendingIntent is not an activity");
	            default:
	                throw new AndroidRuntimeException("Unknown error code "
	                        + res + " when starting " + intent);
	        }
	    }
	/**
	 * Perform instantiation of the process's {@link Activity} object.  The
	 * default implementation provides the normal system behavior.
	 */
	public Activity newActivity(String className, Intent intent)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		//		System.out.println("loading Class: " + className);
		return (Activity) Class.forName(className).newInstance();
	}

	/**
	 * Perform calling of an activity's {@link Activity#onCreate}
	 * method.  The default implementation simply calls through to that method.
	 * 
	 * @param activity The activity being created.
	 * @param icicle The previously frozen state (or null) to pass through to
	 *               onCreate().
	 */
	public void callActivityOnCreate(Activity activity, Bundle icicle) {
	    /**
	     * @Mayloon update
	     * Removed message queue
	     */
        if (mWaitingActivities == null) {
            mWaitingActivities = new ArrayList();
        }
        final int N = mWaitingActivities.size();
        for (int i = 0; i < N; i++) {
            final ActivityWaiter aw = mWaitingActivities.get(i);
            final Intent intent = aw.intent;
            if (intent.filterEquals(activity.getIntent())) {
                aw.activity = activity;
                // mMessageQueue.addIdleHandler(new ActivityGoing(aw));
            }
        }

		activity.onCreate(icicle);
	}

	/**
	 * Perform calling of an activity's {@link Activity#onStop}
	 * method.  The default implementation simply calls through to that method.
	 * 
	 * @param activity The activity being stopped.
	 */
	public void callActivityOnStop(Activity activity) {
		activity.onStop();
	}

	/**
	 * Perform calling of an activity's {@link Activity#onRestoreInstanceState}
	 * method.  The default implementation simply calls through to that method.
	 * 
	 * @param activity The activity being restored.
	 * @param savedInstanceState The previously saved state being restored.
	 */
	public void callActivityOnRestoreInstanceState(Activity activity,
			Bundle savedInstanceState) {
		activity.performRestoreInstanceState(savedInstanceState);
	}

	/**
	 * Perform calling of an activity's {@link Activity#onPostCreate} method.
	 * The default implementation simply calls through to that method.
	 * 
	 * @param activity The activity being created.
	 * @param icicle The previously frozen state (or null) to pass through to
	 *               onPostCreate().
	 */
	public void callActivityOnPostCreate(Activity activity, Bundle icicle) {
		activity.onPostCreate(icicle);
	}

	/**
	 * Perform calling of an activity's {@link Activity#onStart}
	 * method.  The default implementation simply calls through to that method.
	 * 
	 * @param activity The activity being started.
	 */
	public void callActivityOnStart(Activity activity) {
		activity.onStart();
	}
	
    /**
     * This is called whenever the system captures an unhandled exception that
     * was thrown by the application.  The default implementation simply
     * returns false, allowing normal system handling of the exception to take
     * place.
     * 
     * @param obj The client object that generated the exception.  May be an
     *            Application, Activity, BroadcastReceiver, Service, or null.
     * @param e The exception that was thrown.
     *  
     * @return To allow normal system exception process to occur, return false.
     *         If true is returned, the system will proceed as if the exception
     *         didn't happen.
     */
    public boolean onException(Object obj, Throwable e) {
        return false;
    }
    
    /**
     * Return a Context for the target application being instrumented.  Note
     * that this is often different than the Context of the instrumentation
     * code, since the instrumentation code often lives is a different package
     * than that of the application it is running against. See
     * {@link #getContext} to retrieve a Context for the instrumentation code.
     * 
     * @return A Context in the target application.
     * 
     * @see #getContext
     */
    public Context getTargetContext() {
        return mAppContext;
    }
    
    private static final class ActivityWaiter {
        public final Intent intent;
        public Activity activity;

        public ActivityWaiter(Intent _intent) {
            intent = _intent;
        }
    }
    
    /**
     * Start a new activity and wait for it to begin running before returning.
     * In addition to being synchronous, this method as some semantic
     * differences from the standard {@link Context#startActivity} call: the
     * activity component is resolved before talking with the activity manager
     * (its class name is specified in the Intent that this method ultimately
     * starts), and it does not allow you to start activities that run in a
     * different process.  In addition, if the given Intent resolves to
     * multiple activities, instead of displaying a dialog for the user to
     * select an activity, an exception will be thrown.
     * 
     * <p>The function returns as soon as the activity goes idle following the
     * call to its {@link Activity#onCreate}.  Generally this means it has gone
     * through the full initialization including {@link Activity#onResume} and
     * drawn and displayed its initial window.
     * 
     * @param intent Description of the activity to start.
     * 
     * @see Context#startActivity
     */
    public Activity startActivitySync(Intent intent) {

        intent = new Intent(intent);

        ActivityInfo ai = intent.resolveActivityInfo(
            getTargetContext().getPackageManager(), 0);
        if (ai == null) {
            throw new RuntimeException("Unable to resolve activity for: " + intent);
        }

        intent.setComponent(new ComponentName(
                ai.applicationInfo.packageName, ai.name));
        final ActivityWaiter aw = new ActivityWaiter(intent);

        if (mWaitingActivities == null) {
            mWaitingActivities = new ArrayList();
        }
        mWaitingActivities.add(aw);

        getTargetContext().startActivity(intent);
/*
        do {
            try {
                mSync.wait();
            } catch (InterruptedException e) {
            }
        } while (mWaitingActivities.contains(aw));
*/
        return aw.activity;
    }

    /**
     * Schedule a callback for when the application's main thread goes idle
     * (has no more events to process).
     * 
     * @param recipient Called the next time the thread's message queue is
     *                  idle.
     */
    public void waitForIdle(Runnable recipient) { 
        /*mMessageQueue.addIdleHandler(new Idler(recipient));
        mThread.getHandler().post(new EmptyRunnable());*/
    }

    /**
     * Synchronously wait for the application to be idle.  Can not be called
     * from the main application thread -- use {@link #start} to execute
     * instrumentation in its own thread.
     */
    public void waitForIdleSync() {
    	/***
    	 * @Mayloon update
    	 * Remove implementation since this API will not work in mayloon
    	 */
        /*Idler idler = new Idler(null);
        mMessageQueue.addIdleHandler(idler);
        mThread.getHandler().post(new EmptyRunnable());
        idler.waitForIdle();*/
    }
    
    /**
     * Execute a call on the application's main thread, blocking until it is
     * complete.  Useful for doing things that are not thread-safe, such as
     * looking at or modifying the view hierarchy.
     * 
     * @param runner The code to run on the main thread.
     */
    public void runOnMainSync(Runnable runner) {
        SyncRunnable sr = new SyncRunnable(runner);
        mThread.getHandler().post(sr);
        sr.waitForComplete();
    }


    private static final class Idler implements MessageQueue.IdleHandler {
        private final Runnable mCallback;
        private boolean mIdle;

        public Idler(Runnable callback) {
            mCallback = callback;
            mIdle = false;
        }

        public final boolean queueIdle() {
            if (mCallback != null) {
                mCallback.run();
            }
            synchronized (this) {
                mIdle = true;
                notifyAll();
            }
            return false;
        }

        public void waitForIdle() {
            synchronized (this) {
                while (!mIdle) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    private static final class EmptyRunnable implements Runnable {
        public void run() {
        }
    }
    
    private static final class SyncRunnable implements Runnable {
        private final Runnable mTarget;
        private boolean mComplete;

        public SyncRunnable(Runnable target) {
            mTarget = target;
        }

        public void run() {
            mTarget.run();
            synchronized (this) {
                mComplete = true;
                notifyAll();
            }
        }

        public void waitForComplete() {
            synchronized (this) {
                while (!mComplete) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    /**
     * @Mayloon update
     * Add argument ID for sendKeySync
     * ID is used to identify related element in javascript
     */
    /**
     * Send a key event to the currently focused window/view and wait for it to
     * be processed.  Finished at some point after the recipient has returned
     * from its event processing, though it may <em>not</em> have completely
     * finished reacting from the event -- for example, if it needs to update
     * its display as a result, it may still be in the process of doing that.
     * 
     * @param event The event to send to the current focus.
     */
    public void sendKeySync(KeyEvent event, int ID) {
    	int action = event.getAction();
    	int code = event.getKeyCode();
    	/**
		  @j2sNative
		var evt;
		var eventType;
		var element = document.getElementById(ID);
		if(action == 0)
			eventType = "keydown";
		else
			eventType = "keyup";
			
        if (window.KeyEvent) {
            evt = document.createEvent('KeyEvents');
            evt.initKeyEvent(eventType, false, true, window, false, false, false, false, code, code);
        }
        else if(window.KeyboardEvent)
        {
            evt = document.createEvent('KeyboardEvents');
            Object.defineProperty(evt,'keyCode', {
                    get : function(){
                            return this.keyCodeVal;
                    }
            })

            evt.initKeyboardEvent(eventType, false, true, window, code, code, false, false, false, false);
            evt.keyCodeVal = Number(code);
        }
        else {
            evt = document.createEvent('UIEvent');
            evt.shiftKey = false;
            evt.metaKey = false;
            evt.altKey = false;
            evt.ctrlKey = false;

            evt.initUIEvent(eventType, false, true, window, 1);
            evt.charCode = code;
            evt.keyCode = code;
            evt.which = code;
        }
		element.dispatchEvent(evt);
		 */{}
    }
    
    /**
     * Send event for focus
     */
    public void sendEvent(String eventType, int ID, boolean canBubble) {
    	/**
    	 * @j2sNative
    	 * var element = document.getElementById(ID);
    	 * var evt = document.createEvent("HTMLEvents");
    	 * evt.initEvent(eventType,canBubble,true);
    	 * element.dispatchEvent(evt);
    	 */{}

    }
    
    /**
     * Higher-level method for sending both the down and up key events for a
     * particular character key code.  Equivalent to creating both KeyEvent
     * objects by hand and calling {@link #sendKeySync}.  The event appears
     * as if it came from keyboard 0, the built in one.
     * 
     * @param keyCode The key code of the character to send.
     */
    public void sendCharacterSync(int keyCode, int ID) {
        sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode), ID);
        sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, keyCode), ID);
    }


    /**
     * Sends an up and down key event sync to the currently focused window.
     * 
     * @param key The integer keycode for the event.
     */
    public void sendKeyDownUpSync(int key, int ID) {
        sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, key),ID);
        sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, key),ID);
    }

    /**
     * Perform instantiation of an {@link Activity} object.  This method is intended for use with
     * unit tests, such as android.test.ActivityUnitTestCase.  The activity will be useable
     * locally but will be missing some of the linkages necessary for use within the sytem.
     * 
     * @param clazz The Class of the desired Activity
     * @param context The base context for the activity to use
     * @param token The token for this activity to communicate with
     * @param application The application object (if any)
     * @param intent The intent that started this Activity
     * @param info ActivityInfo from the manifest
     * @param title The title, typically retrieved from the ActivityInfo record
     * @param parent The parent Activity (if any)
     * @param id The embedded Id (if any)
     * @param lastNonConfigurationInstance Arbitrary object that will be
     * available via {@link Activity#getLastNonConfigurationInstance()
     * Activity.getLastNonConfigurationInstance()}.
     * @return Returns the instantiated activity
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public Activity newActivity(Class<?> clazz, Context context,
            IBinder token, Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            Object lastNonConfigurationInstance) throws InstantiationException,
            IllegalAccessException {
        Activity activity = (Activity)clazz.newInstance();
        ActivityThread aThread = null;
        activity.attach(context, aThread, this, token, application, intent, info, title,
                parent, id, lastNonConfigurationInstance, new Configuration());
        return activity;
    }
    
    /**
     * Perform instantiation of the process's {@link Application} object.  The
     * default implementation provides the normal system behavior.
     * 
     * @param clazz The class used to create an Application object from.
     * @param context The context to initialize the application with
     * 
     * @return The newly instantiated Application object.
     */
    static public Application newApplication(Class<?> clazz, Context context)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        Application app = (Application)clazz.newInstance();
        app.attach(context);
        return app;
    }

    /**
     * Return the Context of this instrumentation's package.  Note that this is
     * often different than the Context of the application being
     * instrumentated, since the instrumentation code often lives is a
     * different package than that of the application it is running against.
     * See {@link #getTargetContext} to retrieve a Context for the target
     * application.
     * 
     * @return The instrumentation's package context.
     * 
     * @see #getTargetContext
     */
    public Context getContext() {
        return mInstrContext;
    }
    
    private final class InstrumentationThread extends Thread {
        public InstrumentationThread(String name) {
            super(name);
        }
        public void run() {
        	/*
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            } catch (RuntimeException e) {
                Log.w(TAG, "Exception setting priority of instrumentation thread "
                        + Process.myTid(), e);
            }
            */
            onStart();
        }
    }

    /**
     * Provide a status report about the application.
     *  
     * @param resultCode Current success/failure of instrumentation. 
     * @param results Any results to send back to the code that started the instrumentation.
     */
    public void sendStatus(int resultCode, Bundle results) {
    	/*
        if (mWatcher != null) {
            try {
                mWatcher.instrumentationStatus(mComponent, resultCode, results);
            }
            catch (RemoteException e) {
                mWatcher = null;
            }
        }
        */
    }

    /**
     * Dispatch a pointer event. Finished at some point after the recipient has
     * returned from its event processing, though it may <em>not</em> have
     * completely finished reacting from the event -- for example, if it needs
     * to update its display as a result, it may still be in the process of
     * doing that.
     * 
     * @param event A motion event describing the pointer action.  (As noted in 
     * {@link MotionEvent#obtain(long, long, int, float, float, int)}, be sure to use 
     * {@link SystemClock#uptimeMillis()} as the timebase.
     */
    public void sendPointerSync(MotionEvent event) {
    	/*
        try {
            (IWindowManager.Stub.asInterface(ServiceManager.getService("window")))
                .injectPointerEvent(event, true);
        } catch (RemoteException e) {
        }
        */
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        float rawx = event.getRawX();
        float rawy = event.getRawY();
        int count = event.getPointerCount();
       /**
        *@j2sNative
        *var evt;
        *var eventType;
        *if(action == 0)
        *{
        *   eventType = "mousedown";
        *}
        *if(action == 1)
        *{
        *   eventType = "mouseup";
        *}
        *if(action == 2)
        *{
        *   eventType = "mousemove";
        *}
        *if(window.MouseEvent)
        *{
        *   evt = document.createEvent("MouseEvent");
        *   evt.initMouseEvent(eventType,false,true,window,count,rawx,rawy,x,y,false,false,false,false,0,null);
        *}
        *window.dispatchEvent(evt);
        */{}
    }

    /**
     * @j2sNative
     * console.log("Missing method: getBinderCounts");
     */
    @MayloonStubAnnotation()
    public Bundle getBinderCounts() {
        System.out.println("Stub" + " Function : getBinderCounts");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: callActivityOnRestart");
     */
    @MayloonStubAnnotation()
    public void callActivityOnRestart(Activity activity) {
        System.out.println("Stub" + " Function : callActivityOnRestart");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: endPerformanceSnapshot");
     */
    @MayloonStubAnnotation()
    public void endPerformanceSnapshot() {
        System.out.println("Stub" + " Function : endPerformanceSnapshot");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: stopAllocCounting");
     */
    @MayloonStubAnnotation()
    public void stopAllocCounting() {
        System.out.println("Stub" + " Function : stopAllocCounting");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onDestroy");
     */
    @MayloonStubAnnotation()
    public void onDestroy() {
        System.out.println("Stub" + " Function : onDestroy");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: startAllocCounting");
     */
    @MayloonStubAnnotation()
    public void startAllocCounting() {
        System.out.println("Stub" + " Function : startAllocCounting");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isProfiling");
     */
    @MayloonStubAnnotation()
    public boolean isProfiling() {
        System.out.println("Stub" + " Function : isProfiling");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: addMonitor");
     */
    @MayloonStubAnnotation()
    public Object addMonitor(String cls, ActivityResult result,
            boolean block) {
        System.out.println("Stub" + " Function : addMonitor");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: stopProfiling");
     */
    @MayloonStubAnnotation()
    public void stopProfiling() {
        System.out.println("Stub" + " Function : stopProfiling");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: callActivityOnUserLeaving");
     */
    @MayloonStubAnnotation()
    public void callActivityOnUserLeaving(Activity activity) {
        System.out.println("Stub" + " Function : callActivityOnUserLeaving");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: sendKeyDownUpSync");
     */
    @MayloonStubAnnotation()
    public void sendKeyDownUpSync(int key) {
        System.out.println("Stub" + " Function : sendKeyDownUpSync");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: callActivityOnDestroy");
     */
    @MayloonStubAnnotation()
    public void callActivityOnDestroy(Activity activity) {
        System.out.println("Stub" + " Function : callActivityOnDestroy");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: removeMonitor");
     */
    @MayloonStubAnnotation()
    public void removeMonitor(Object monitor) {
        System.out.println("Stub" + " Function : removeMonitor");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setAutomaticPerformanceSnapshots");
     */
    @MayloonStubAnnotation()
    public void setAutomaticPerformanceSnapshots() {
        System.out.println("Stub" + " Function : setAutomaticPerformanceSnapshots");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: startProfiling");
     */
    @MayloonStubAnnotation()
    public void startProfiling() {
        System.out.println("Stub" + " Function : startProfiling");
        return;
    }

    /**
     * Force the global system in or out of touch mode.  This can be used if
     * your instrumentation relies on the UI being in one more or the other
     * when it starts.
     * 
     * @param inTouch Set to true to be in touch mode, false to be in
     * focus mode.
     */
    public void setInTouchMode(boolean inTouch) {
//        try {
//            IWindowManager.Stub.asInterface(
//                    ServiceManager.getService("window")).setInTouchMode(inTouch);
//        } catch (RemoteException e) {
//            // Shouldn't happen!
//        }
        // In MayLoon, we set this flag in WindowManager, in Android, it set it in WindowManagerService.
        WindowManagerImpl mWindowManager = (WindowManagerImpl)WindowManagerImpl.getDefault();
        mWindowManager.setInTouchMode(inTouch);
        
    }

    /**
     * @j2sNative
     * console.log("Missing method: waitForMonitor");
     */
    @MayloonStubAnnotation()
    public Activity waitForMonitor(Object monitor) {
        System.out.println("Stub" + " Function : waitForMonitor");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: startPerformanceSnapshot");
     */
    @MayloonStubAnnotation()
    public void startPerformanceSnapshot() {
        System.out.println("Stub" + " Function : startPerformanceSnapshot");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: addMonitor");
     */
    @MayloonStubAnnotation()
    public void addMonitor(Object monitor) {
        System.out.println("Stub" + " Function : addMonitor");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: sendStringSync");
     */
    @MayloonStubAnnotation()
    public void sendStringSync(String text) {
        System.out.println("Stub" + " Function : sendStringSync");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: sendCharacterSync");
     */
    @MayloonStubAnnotation()
    public void sendCharacterSync(int keyCode) {
        System.out.println("Stub" + " Function : sendCharacterSync");
        return;
    }
}
