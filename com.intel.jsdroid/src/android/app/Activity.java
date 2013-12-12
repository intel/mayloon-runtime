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

import java.util.ArrayList;
import java.util.HashMap;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;

/**
 * An activity is a single, focused thing that the user can do.  Almost all
 * activities interact with the user, so the Activity class takes care of
 * creating a window for you in which you can place your UI with
 * {@link #setContentView}.  While activities are often presented to the user
 * as full-screen windows, they can also be used in other ways: as floating
 * windows (via a theme with {@link android.R.attr#windowIsFloating} set)
 * or embedded inside of another activity (using {@link ActivityGroup}).
 */
public class Activity extends ContextThemeWrapper implements Window.Callback, KeyEvent.Callback, OnCreateContextMenuListener {
	private static final String TAG = "Activity";

	/** Standard activity result: operation canceled. */
	public static final int RESULT_CANCELED = 0;
	/** Standard activity result: operation succeeded. */
	public static final int RESULT_OK = -1;
	/** Start of user-defined activity results. */
	public static final int RESULT_FIRST_USER = 1;
    private static final String WINDOW_HIERARCHY_TAG = "android:viewHierarchyState";

	private static long sInstanceCount = 0;

	private Window mWindow;
	/*package*/View mDecor;

	Activity mParent;
	boolean mCalled;
	boolean mFinished;
	boolean mStartedActivity;
	/*package*/ int mConfigChangeFlags;
	/*package*/Intent mIntent;

	/*package*/boolean mWindowAdded = false;
	/*package*/boolean mVisibleFromServer = false;
	/*package*/boolean mVisibleFromClient = true;

	private ComponentName mComponent;
	private Instrumentation mInstrumentation;
	private boolean mResumed;
	private boolean mStopped;
	private ActivityThread mMainThread;
	private Application mApplication;

	private CharSequence mTitle;
	private int mTitleColor = 0;
	private boolean mTitleReady = false;
    private int mDefaultKeyMode = DEFAULT_KEYS_DISABLE;
    private SpannableStringBuilder mDefaultKeySsb = null;
    private ActivityThread mUiThread;
    private final Handler mHandler = new Handler();

    private static class ManagedDialog {
        Dialog mDialog;
        Bundle mArgs;
    }
    private SparseArray<ManagedDialog> mManagedDialogs;

    private static final class ManagedCursor {
        ManagedCursor(Cursor cursor) {
            mCursor = cursor;
            mReleased = false;
            mUpdated = false;
        }

        private final Cursor mCursor;
        private boolean mReleased;
        private boolean mUpdated;
    }
    private final ArrayList<ManagedCursor> mManagedCursors =
        new ArrayList<ManagedCursor>();
	private IBinder mToken;
	/*package*/String mEmbeddedID;

	private WindowManager mWindowManager;

	public int mZIndex = 0;

	public static long getInstanceCount() {
		return sInstanceCount;
	}

	protected Activity() {
	}

    /** Is this activity embedded inside of another activity? */
    public final boolean isChild() {
        return mParent != null;
    }

	public final Activity getParent() {
		return mParent;
	}
	/**
	 * FIXME: It may always return null
	 * @return
	 */
	public Intent getIntent(){
		if(mIntent==null){
			mIntent = new Intent();
		}
		return mIntent;
	}
    /**
     * Change the intent returned by {@link #getIntent}.  This holds a
     * reference to the given intent; it does not copy it.  Often used in
     * conjunction with {@link #onNewIntent}.
     *
     * @param newIntent The new Intent object to return from getIntent
     *
     * @see #getIntent
     * @see #onNewIntent
     */
    public void setIntent(Intent newIntent) {
        mIntent = newIntent;
    }

    protected void onCreate(Bundle state) {
        Intent intent = getIntent();
        if (intent.getBooleanExtra("isContentOfTabHost", false)) {
            getWindow().removeBackKeyInLocalFeature();
            mWindow.setBeContentOfTab(true);
        } else {
            mWindow.setBeContentOfTab(false);
        }

        mVisibleFromClient = !mWindow.getWindowStyle().getBoolean(
                com.android.internal.R.styleable.Window_windowNoDisplay, false);
        mCalled = true;
    }
    /**
    * Initialize the contents of the Activity's standard options menu.  You
    * should place your menu items in to <var>menu</var>.
    * 
    * <p>This is only called once, the first time the options menu is
    * displayed.  To update the menu every time it is displayed, see
    * {@link #onPrepareOptionsMenu}.
    * 
    * <p>The default implementation populates the menu with standard system
    * menu items.  These are placed in the {@link MenuBuilder#CATEGORY_SYSTEM} group so that 
    * they will be correctly ordered with application-defined menu items. 
    * Deriving classes should always call through to the base implementation. 
    * 
    * <p>You can safely hold on to <var>menu</var> (and any items created
    * from it), making modifications to it as desired, until the next
    * time onCreateOptionsMenu() is called.
    * 
    * <p>When you add items to the menu, you can implement the Activity's
    * {@link #onOptionsItemSelected} method to handle them there.
    * 
    * @param menu The options menu in which you place your items.
    * 
    * @return You must return true for the menu to be displayed;
    *         if you return false it will not be shown.
    * 
    * @see #onPrepareOptionsMenu
    * @see #onOptionsItemSelected
    */
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mParent != null) {
            return mParent.onCreateOptionsMenu(menu);
        }
        return true;
    }


    /**
     * Wrapper around
     * {@link ContentResolver#query(android.net.Uri , String[], String, String[], String)}
     * that gives the resulting {@link Cursor} to call
     * {@link #startManagingCursor} so that the activity will manage its
     * lifecycle for you.
     *
     * @param uri The URI of the content provider to query.
     * @param projection List of columns to return.
     * @param selection SQL WHERE clause.
     * @param sortOrder SQL ORDER BY clause.
     *
     * @return The Cursor that was returned by query().
     *
     * @see ContentResolver#query(android.net.Uri , String[], String, String[], String)
     * @see #startManagingCursor
     * @hide
     */
    public final Cursor managedQuery(Uri uri,
                                     String[] projection,
                                     String selection,
                                     String sortOrder)
    {
        Cursor c = getContentResolver().query(uri, projection, selection, null, sortOrder);
        if (c != null) {
            startManagingCursor(c);
        }
        return c;
    }

    /**
     * Wrapper around
     * {@link ContentResolver#query(android.net.Uri , String[], String, String[], String)}
     * that gives the resulting {@link Cursor} to call
     * {@link #startManagingCursor} so that the activity will manage its
     * lifecycle for you.
     *
     * @param uri The URI of the content provider to query.
     * @param projection List of columns to return.
     * @param selection SQL WHERE clause.
     * @param selectionArgs The arguments to selection, if any ?s are pesent
     * @param sortOrder SQL ORDER BY clause.
     *
     * @return The Cursor that was returned by query().
     *
     * @see ContentResolver#query(android.net.Uri , String[], String, String[], String)
     * @see #startManagingCursor
     */
    public final Cursor managedQuery(Uri uri,
                                     String[] projection,
                                     String selection,
                                     String[] selectionArgs,
                                     String sortOrder)
    {
        Cursor c = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        if (c != null) {
            startManagingCursor(c);
        }
        return c;
    }
    /**
     * This method allows the activity to take care of managing the given
     * {@link Cursor}'s lifecycle for you based on the activity's lifecycle.
     * That is, when the activity is stopped it will automatically call
     * {@link Cursor#deactivate} on the given Cursor, and when it is later restarted
     * it will call {@link Cursor#requery} for you.  When the activity is
     * destroyed, all managed Cursors will be closed automatically.
     *
     * @param c The Cursor to be managed.
     *
     * @see #managedQuery(android.net.Uri , String[], String, String[], String)
     * @see #stopManagingCursor
     */
    public void startManagingCursor(Cursor c) {
         mManagedCursors.add(new ManagedCursor(c));
    }

    protected void onSaveInstanceState(Bundle state) {
        state.putBundle(WINDOW_HIERARCHY_TAG, mWindow.saveHierarchyState());
    }

	/**
	 * Finds a view that was identified by the id attribute from the XML that
	 * was processed in {@link #onCreate}.
	 *
	 * @return The view if found or null otherwise.
	 */
	public View findViewById(int id) {
		return mWindow.findViewById(id);
	}

	/**
	 * Set the activity content from a layout resource.  The resource will be
	 * inflated, adding all top-level views to the activity.
	 *
	 * @param layoutResID Resource ID to be inflated.
	 */
	public void setContentView(int layoutResID) {
		mWindow.setContentView(layoutResID);
		mDecor = mWindow.getDecorView();
		//		this.openOptionsMenu();
	}

	public void setContentView(View view) {
		mWindow.setContentView(view);
		mDecor = mWindow.getDecorView();
		//		this.openOptionsMenu();
	}

	protected void onPause() {
		print("onPause");
		mCalled = true;
	}

	private void print(String functionName) {
		Class<? extends Activity> klass = getClass();
		Log.d(TAG, "Activity's CanonicalName: " + klass.getName()
				+ ", " + functionName);
	}

	protected void onResume() {
		print("onResume");
		mCalled = true;
	}
    
    /**
     * Use with {@link #setDefaultKeyMode} to turn off default handling of
     * keys.
     * 
     * @see #setDefaultKeyMode
     */
    static public final int DEFAULT_KEYS_DISABLE = 0;
    /**
     * Use with {@link #setDefaultKeyMode} to launch the dialer during default
     * key handling.
     * 
     * @see #setDefaultKeyMode
     */
    static public final int DEFAULT_KEYS_DIALER = 1;
    /**
     * Use with {@link #setDefaultKeyMode} to execute a menu shortcut in
     * default key handling.
     * 
     * <p>That is, the user does not need to hold down the menu key to execute menu shortcuts.
     * 
     * @see #setDefaultKeyMode
     */
    static public final int DEFAULT_KEYS_SHORTCUT = 2;
    /**
     * Use with {@link #setDefaultKeyMode} to specify that unhandled keystrokes
     * will start an application-defined search.  (If the application or activity does not
     * actually define a search, the the keys will be ignored.)
     * 
     * <p>See {@link android.app.SearchManager android.app.SearchManager} for more details.
     * 
     * @see #setDefaultKeyMode
     */
    static public final int DEFAULT_KEYS_SEARCH_LOCAL = 3;

    /**
     * Use with {@link #setDefaultKeyMode} to specify that unhandled keystrokes
     * will start a global search (typically web search, but some platforms may define alternate
     * methods for global search)
     * 
     * <p>See {@link android.app.SearchManager android.app.SearchManager} for more details.
     * 
     * @see #setDefaultKeyMode
     */
    static public final int DEFAULT_KEYS_SEARCH_GLOBAL = 4;
    
    /**
     * Select the default key handling for this activity.  This controls what
     * will happen to key events that are not otherwise handled.  The default
     * mode ({@link #DEFAULT_KEYS_DISABLE}) will simply drop them on the
     * floor. Other modes allow you to launch the dialer
     * ({@link #DEFAULT_KEYS_DIALER}), execute a shortcut in your options
     * menu without requiring the menu key be held down
     * ({@link #DEFAULT_KEYS_SHORTCUT}), or launch a search ({@link #DEFAULT_KEYS_SEARCH_LOCAL} 
     * and {@link #DEFAULT_KEYS_SEARCH_GLOBAL}).
     * 
     * <p>Note that the mode selected here does not impact the default
     * handling of system keys, such as the "back" and "menu" keys, and your
     * activity and its views always get a first chance to receive and handle
     * all application keys.
     * 
     * @param mode The desired default key mode constant.
     * 
     * @see #DEFAULT_KEYS_DISABLE
     * @see #DEFAULT_KEYS_DIALER
     * @see #DEFAULT_KEYS_SHORTCUT
     * @see #DEFAULT_KEYS_SEARCH_LOCAL
     * @see #DEFAULT_KEYS_SEARCH_GLOBAL
     * @see #onKeyDown
     */
    public final void setDefaultKeyMode(int mode) {
        mDefaultKeyMode = mode;
        
        // Some modes use a SpannableStringBuilder to track & dispatch input events
        // This list must remain in sync with the switch in onKeyDown()
        switch (mode) {
        case DEFAULT_KEYS_DISABLE:
        case DEFAULT_KEYS_SHORTCUT:
            mDefaultKeySsb = null;      // not used in these modes
            break;
        case DEFAULT_KEYS_DIALER:
        case DEFAULT_KEYS_SEARCH_LOCAL:
        case DEFAULT_KEYS_SEARCH_GLOBAL:
            mDefaultKeySsb = new SpannableStringBuilder();
            Selection.setSelection(mDefaultKeySsb,0);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }

        if (mDefaultKeyMode == DEFAULT_KEYS_DISABLE) {
            return false;
        } else if(mDefaultKeyMode == DEFAULT_KEYS_SHORTCUT) {
            return false;
        }else {
            // Common code for DEFAULT_KEYS_DIALER & DEFAULT_KEYS_SEARCH_*
            boolean clearSpannable = false;
            boolean handled = false;
            if (keyEvent.getRepeatCount() != 0) {
                clearSpannable = true;
                handled = false;
            }
//          else {
//                handled = TextKeyListener.getInstance().onKeyDown(
//                        null, mDefaultKeySsb, keyCode, keyEvent);
//                if (handled && mDefaultKeySsb.length() > 0) {
//                    // something useable has been typed - dispatch it now.
//                    final String str = mDefaultKeySsb.toString();
//                    clearSpannable = true;
//                    switch (mDefaultKeyMode) {
//                    case DEFAULT_KEYS_DIALER:
//                        Intent intent = new Intent(Intent.ACTION_DIAL,  Uri.parse("tel:" + str));
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);    
//                        break;
//                    case DEFAULT_KEYS_SEARCH_LOCAL:
//                        startSearch(str, false, null, false);
//                        break;
//                    case DEFAULT_KEYS_SEARCH_GLOBAL:
//                        startSearch(str, false, null, true);
//                        break;
//                    }
//                }
//            }
            if (clearSpannable) {
                mDefaultKeySsb.clear();
                mDefaultKeySsb.clearSpans();
                Selection.setSelection(mDefaultKeySsb,0);
            }
            return handled;
        }
    }

    /**
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     * 
     * <p>The default implementation updates the system menu items based on the
     * activity's state.  Deriving classes should always call through to the
     * base class implementation.
     * 
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     * 
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     * 
     * @see #onCreateOptionsMenu
     */
    public boolean onPrepareOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        if (mParent != null) {
            return mParent.onPrepareOptionsMenu(menu);
        }
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     * 
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     * 
     * @param item The menu item that was selected.
     * 
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     * 
     * @see #onCreateOptionsMenu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mParent != null) {
            return mParent.onOptionsItemSelected(item);
        }

        return false;
    }

    final void setParent(Activity parent) {
        mParent = parent;
    }

	final void attach(Context context, Instrumentation instr, CharSequence title,
			ActivityThread aThread, IBinder token, Intent intent, Application app) {

		attachBaseContext(context);
		//TODO: for a test
		mActivityThread = aThread;

		mMainThread = aThread;
        mUiThread = ActivityThread.currentActivityThread();
		mWindow = new Window(this);
		mWindow.setCallback(this);
		mInstrumentation = instr;
		mToken = token; // an ActivityRecord object
		mIntent = intent;
		mComponent = intent.getComponent();

		mTitle = title;
		mIntent = intent;
		mComponent = intent.getComponent();
		mApplication = app;

		mWindow.setWindowManager(null, mToken, null);
		if (mParent != null) {
			mWindow.setContainer(mParent.getWindow());
		}
		mWindowManager = mWindow.getWindowManager();
	}

	final IBinder getToken(){
		return mToken;
	}

    /** Return the application that owns this activity. */
    public final Application getApplication() {
        return mApplication;
    }

	final void performStart() {
		mCalled = false;
		mInstrumentation.callActivityOnStart(this);
		if (!mCalled) {
			Log.d(TAG,"Activity " + mComponent.toShortString()
					+ " did not call through to super.onStart()");
		}
	}

	final void performRestart() {
        synchronized (mManagedCursors) {
            final int N = mManagedCursors.size();
            for (int i=0; i<N; i++) {
                ManagedCursor mc = mManagedCursors.get(i);
                if (mc.mReleased || mc.mUpdated) {
                    mc.mCursor.requery();
                    mc.mReleased = false;
                    mc.mUpdated = false;
                }
            }
        }

        if (mStopped) {
            mStopped = false;
            mCalled = false;
            mInstrumentation.callActivityOnRestart(this);
//            if (!mCalled) {
//                throw new SuperNotCalledException(
//                    "Activity " + mComponent.toShortString() +
//                    " did not call through to super.onRestart()");
//            }
            performStart();
        }
    }

	void makeVisible() {
		if (!mWindowAdded) {
			ViewManager wm = (ViewManager) getWindowManager();
			//			wm.addView(mDecor, getWindow().getAttributes());
			mDecor.zIndex = this.mZIndex;
			wm.addView(mDecor, new WindowManager.LayoutParams(
					WindowManager.LayoutParams.TYPE_APPLICATION, 0,
					PixelFormat.OPAQUE));
			mWindowAdded = true;
		}
		mDecor.setVisibility(View.VISIBLE);
	}

    public void setVisible(boolean visible) {
        if (mVisibleFromClient != visible) {
            mVisibleFromClient = visible;
            if (mVisibleFromServer) {
                if (visible)
                    makeVisible();
                else
                    mDecor.setVisibility(View.INVISIBLE);
            }
        }
    }

	public Window getWindow() {
		return mWindow;
	}

    /**
     * This hook is called whenever the options menu is being closed (either by the user canceling
     * the menu with the back/menu button, or when an item is selected).
     *  
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     */
    public void onOptionsMenuClosed(Menu menu) {
        if (mParent != null) {
            mParent.onOptionsMenuClosed(menu);
        }
    }
    /**
     * Programmatically opens the options menu. If the options menu is already
     * open, this method does nothing.
     */
	public void openOptionsMenu() {
		mWindow.togglePanel(Window.FEATURE_OPTIONS_PANEL, null);
	}
    
    /**
     * Progammatically closes the options menu. If the options menu is already
     * closed, this method does nothing.
     */
    public void closeOptionsMenu() {
        mWindow.closePanel(Window.FEATURE_OPTIONS_PANEL);
    }
    
    
    /**
     * Called when a context menu for the {@code view} is about to be shown.
     * Unlike {@link #onCreateOptionsMenu(MenuBuilder)}, this will be called every
     * time the context menu is about to be shown and should be populated for
     * the view (or item inside the view for {@link AdapterView} subclasses,
     * this can be found in the {@code menuInfo})).
     * <p>
     * Use {@link #onContextItemSelected(android.view.MenuItemImpl)} to know when an
     * item has been selected.
     * <p>
     * It is not safe to hold onto the context menu after this method returns.
     * {@inheritDoc}
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    }

    /**
     * Registers a context menu to be shown for the given view (multiple views
     * can show the context menu). This method will set the
     * {@link OnCreateContextMenuListener} on the view to this activity, so
     * {@link #onCreateContextMenu(ContextMenu, View, ContextMenuInfo)} will be
     * called when it is time to show the context menu.
     * 
     * @see #unregisterForContextMenu(View)
     * @param view The view that should show a context menu.
     */
    public void registerForContextMenu(View view) {
        view.setOnCreateContextMenuListener(this);
    }
    
    /**
     * Prevents a context menu to be shown for the given view. This method will remove the
     * {@link OnCreateContextMenuListener} on the view.
     * 
     * @see #registerForContextMenu(View)
     * @param view The view that should stop showing a context menu.
     */
    public void unregisterForContextMenu(View view) {
        view.setOnCreateContextMenuListener(null);
    }
    
    /**
     * Programmatically opens the context menu for a particular {@code view}.
     * The {@code view} should have been added via
     * {@link #registerForContextMenu(View)}.
     * 
     * @param view The view to show the context menu for.
     */
    public void openContextMenu(View view) {
        view.showContextMenu();
    }
    
    /**
     * Programmatically closes the most recently opened context menu, if showing.
     */
    public void closeContextMenu() {
        mWindow.closePanel(Window.FEATURE_CONTEXT_MENU);
    }
    
	/**
	 * Convenience for calling {@link android.view.Window#getLayoutInflater}.
	 */
	public LayoutInflater getLayoutInflater() {
		return getWindow().getLayoutInflater();
	}

	 public final boolean requestWindowFeature(int featureId) {
	        return getWindow().requestFeature(featureId);
	    }

    /**
     * Called whenever a key, touch, or trackball event is dispatched to the
     * activity.  Implement this method if you wish to know that the user has
     * interacted with the device in some way while your activity is running.
     * This callback and {@link #onUserLeaveHint} are intended to help
     * activities manage status bar notifications intelligently; specifically,
     * for helping activities determine the proper time to cancel a notfication.
     *
     * <p>All calls to your activity's {@link #onUserLeaveHint} callback will
     * be accompanied by calls to {@link #onUserInteraction}.  This
     * ensures that your activity will be told of relevant user activity such
     * as pulling down the notification pane and touching an item there.
     *
     * <p>Note that this callback will be invoked for the touch down action
     * that begins a touch gesture, but may not be invoked for the touch-moved
     * and touch-up actions that follow.
     *
     * @see #onUserLeaveHint()
     */
    public void onUserInteraction() {
    }

    
    /**
     * Called to process key events.  You can override this to intercept all
     * key events before they are dispatched to the window.  Be sure to call
     * this implementation for key events that should be handled normally.
     *
     * @param event The key event.
     *
     * @return boolean Return true if this event was consumed.
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        onUserInteraction();
        if (getWindow().superDispatchKeyEvent(event)) {
            return true;
        }
        View decor = mDecor;
        if (decor == null) {
        	decor = getWindow().getDecorView();
        }
        return event.dispatch(this, decor != null ? decor.getKeyDispatcherState() : null, this);
    }

    /**
     * Called to process touch screen events.  You can override this to
     * intercept all touch screen events before they are dispatched to the
     * window.  Be sure to call this implementation for touch screen events
     * that should be handled normally.
     *
     * @param ev The touch screen event.
     *
     * @return boolean Return true if this event was consumed.
     */
    public boolean dispatchTouchEvent(MotionEvent ev) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "Activity dispatchTouchEvent at time:" + start);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            onUserInteraction();
        }
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key.  The default implementation simply finishes the current activity,
     * but you can override this to do whatever you want.
     */
    public void onBackPressed() {
        finish();
    }

    /**
     * Called when a touch screen event was not handled by any of the views
     * under it.  This is most useful to process touch events that happen
     * outside of your window bounds, where there is no view to receive it.
     *
     * @param event The touch screen event being processed.
     *
     * @return Return true if you have consumed the event, false if you haven't.
     * The default implementation always returns false.
     */
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
    
    public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
        // Update window manager if: we have a view, that view is
        // attached to its parent (which will be a RootView), and
        // this activity is not embedded.
        if (mParent == null) {
            View decor = mDecor;
            if (decor != null && decor.getParent() != null) {
                getWindowManager().updateViewLayout(decor, params);
            }
        }
    }
    
	@Override
	public void onContentChanged() {
		// TODO Auto-generated method stub
	}

	@Override
	public View onCreatePanelView(int featureId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			return onCreateOptionsMenu(menu);
		}
		return false;
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null) {
			boolean goforit = onPrepareOptionsMenu(menu);
			return goforit && menu.hasVisibleItems();
		}
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		return true;
	}

    /**
     * Default implementation of
     * {@link android.view.Window.Callback#onMenuItemSelected}
     * for activities.  This calls through to the new
     * {@link #onOptionsItemSelected} method for the
     * {@link android.view.Window#FEATURE_OPTIONS_PANEL}
     * panel, so that subclasses of
     * Activity don't need to deal with feature codes.
     */
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (featureId) {
            case Window.FEATURE_OPTIONS_PANEL:
                // Put event logging here so it gets called even if subclass
                // doesn't call through to superclass's implmeentation of each
                // of these methods below
                //EventLog.writeEvent(50000, 0, item.getTitleCondensed());
                return onOptionsItemSelected(item);
                
            case Window.FEATURE_CONTEXT_MENU:
                //EventLog.writeEvent(50000, 1, item.getTitleCondensed());
                return onContextItemSelected(item);
                
            default:
                return false;
        }
    }
    
    /**
     * Default implementation of
     * {@link android.view.Window.Callback#onPanelClosed(int, MenuBuilder)} for
     * activities. This calls through to {@link #onOptionsMenuClosed(MenuBuilder)}
     * method for the {@link android.view.Window#FEATURE_OPTIONS_PANEL} panel,
     * so that subclasses of Activity don't need to deal with feature codes.
     * For context menus ({@link Window#FEATURE_CONTEXT_MENU}), the
     * {@link #onContextMenuClosed(MenuBuilder)} will be called.
     */
    public void onPanelClosed(int featureId, Menu menu) {
        switch (featureId) {
            case Window.FEATURE_OPTIONS_PANEL:
                onOptionsMenuClosed(menu);
                break;
                
            case Window.FEATURE_CONTEXT_MENU:
                onContextMenuClosed(menu);
                break;
        }
    }
    
    /**
     * This hook is called whenever the context menu is being closed (either by
     * the user canceling the menu with the back/menu button, or when an item is
     * selected).
     * 
     * @param menu The context menu that is being closed.
     */
    public void onContextMenuClosed(Menu menu) {
        if (mParent != null) {
            mParent.onContextMenuClosed(menu);
        }
    }
    
    
    /**
     * This hook is called whenever an item in a context menu is selected. The
     * default implementation simply returns false to have the normal processing
     * happen (calling the item's Runnable or sending a message to its Handler
     * as appropriate). You can use this method for any items for which you
     * would like to do processing without those other facilities.
     * <p>
     * Use {@link MenuItemImpl#getMenuInfo()} to get extra information set by the
     * View that added this menu item.
     * <p>
     * Derived classes should call through to the base class for it to perform
     * the default menu handling.
     * 
     * @param item The context menu item that was selected.
     * @return boolean Return false to allow normal context menu processing to
     *         proceed, true to consume it here.
     */
    public boolean onContextItemSelected(MenuItem item) {
        if (mParent != null) {
            return mParent.onContextItemSelected(item);
        }
        return false;
    }

	public WindowManager getWindowManager() {
		return mWindowManager;
	}

	// New Functions
	final void performSaveInstanceState(Bundle outState) {
		onSaveInstanceState(outState);
	}

	final void performPause() {
		mCalled = false;
		onPause();
		if (!mCalled) {
			Log.d(TAG,"Activity " + mComponent.toShortString()
					+ " did not call through to super.onPause()");
		}
	}

	protected void onPostResume() {
		//		final Window win = getWindow();
		//		if (win != null)
		//			win.makeActive();
		mCalled = true;
	}

	public void performResume() {
		// First call onResume() -before- setting mResumed, so we don't
		// send out any status bar / menu notifications the client makes.
		performRestart();
		mCalled = false;
		mInstrumentation.callActivityOnResume(this);
		if (!mCalled) {
			Log.d(TAG,"Activity " + mComponent.toShortString()
					+ " did not call through to super.onResume()");
		}

		// Now really resume, and install the current status bar and menu.
		mResumed = true;
		mCalled = false;
		onPostResume();
		if (!mCalled) {
			Log.d(TAG,"Activity " + mComponent.toShortString()
					+ " did not call through to super.onPostResume()");
		}
	}

	/**
	 * Returns complete component name of this activity.
	 *
	 * @return Returns the complete component name for this activity
	 */
	public ComponentName getComponentName() {
		return mComponent;
	}

	@Override
	public void startActivity(Intent intent) {
		startActivityForResult(intent, -1);
	}

	public void startActivityForResult(Intent intent, int requestCode) {
		if (mParent == null) {
			Log.d(TAG,"in startActivityForResult");
			Instrumentation.ActivityResult ar = mInstrumentation
					.execStartActivity(this,
							mMainThread.getApplicationThread(), mToken, this,
							intent, requestCode);
			if (ar != null) {
				Log.d(TAG,
						"impossible here! ar will be set to null by default");
				mMainThread.sendActivityResult(mToken, mEmbeddedID,
						requestCode, ar.getResultCode(), ar.getResultData());
			}
			if (requestCode >= 0) {
				// If this start is requesting a result, we can avoid making
				// the activity visible until the result is received. Setting
				// this code during onCreate(Bundle savedInstanceState) or
				// onResume() will keep the
				// activity hidden during this time, to avoid flickering.
				// This can only be done when a result is requested because
				// that guarantees we will get information back when the
				// activity is finished, no matter what happens to it.
				mStartedActivity = true;
			}
		}
	}

	int mResultCode = RESULT_CANCELED;
	Intent mResultData = null;

	public final void setResult(int resultCode) {
		mResultCode = resultCode;
		mResultData = null;
	}

	/**
	 * Call this to set the result that your activity will return to its
	 * caller.
	 *
	 * @param resultCode The result code to propagate back to the originating
	 *                   activity, often RESULT_CANCELED or RESULT_OK
	 * @param data The data to propagate back to the originating activity.
	 *
	 * @see #RESULT_CANCELED
	 * @see #RESULT_OK
	 * @see #RESULT_FIRST_USER
	 * @see #setResult(int)
	 */
	public final void setResult(int resultCode, Intent data) {
		mResultCode = resultCode;
		mResultData = data;
	}

	/**
	 * Call this when your activity is done and should be closed.  The
	 * ActivityResult is propagated back to whoever launched you via
	 * onActivityResult().
	 */
	public void finish() {
		int resultCode;
		Intent resultData;
		resultCode = mResultCode;
		resultData = mResultData;
        if (((ActivityRecord) mToken).info != null) {
            Log.d(TAG, "Finishing self: token="
                    + ((ActivityRecord) mToken).info.name);
        }
		if (Context.getSystemContext().getActivityManager()
				.finishActivity(mToken, resultCode, resultData)) {
			mFinished = true;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	}

	/**
	 * The hook for {@link ActivityThread} to restore the state of this activity.
	 *
	 * Calls {@link #onSaveInstanceState(android.os.Bundle)} and
	 * {@link #restoreManagedDialogs(android.os.Bundle)}.
	 *
	 * @param savedInstanceState contains the saved state
	 */
	final void performRestoreInstanceState(Bundle savedInstanceState) {
		onRestoreInstanceState(savedInstanceState);
	}

    /**
     * Called when activity start-up is complete (after {@link #onStart}
     * and {@link #onRestoreInstanceState} have been called).  Applications will
     * generally not implement this method; it is intended for system
     * classes to do final initialization after application code has run.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     * @see #onCreate
     */
	protected void onPostCreate(Bundle savedInstanceState) {
        if (!isChild()) {
            mTitleReady = true;
            onTitleChanged(getTitle(), getTitleColor());
        }
		mCalled = true;
	}

	protected void onStart() {
		print("onStart");
		mCalled = true;
	}

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (mWindow != null) {
            Bundle windowState = savedInstanceState.getBundle(WINDOW_HIERARCHY_TAG);
            if (windowState != null) {
                mWindow.restoreHierarchyState(windowState);
            }
        }
    }

    protected void onDestroy() {
        mCalled = true;

        // dismiss any dialogs we are managing.
        if (mManagedDialogs != null) {
            final int numDialogs = mManagedDialogs.size();
            for (int i = 0; i < numDialogs; i++) {
                final ManagedDialog md = mManagedDialogs.valueAt(i);
                if (md.mDialog.isShowing()) {
                    md.mDialog.dismiss();
                }
            }
            mManagedDialogs = null;
        }

        // close any cursors we are managing.
        synchronized (mManagedCursors) {
            int numCursors = mManagedCursors.size();
            for (int i = 0; i < numCursors; i++) {
                ManagedCursor c = mManagedCursors.get(i);
                if (c != null) {
                    c.mCursor.close();
                }
            }
            mManagedCursors.clear();
        }

    }

	protected void onStop() {
		print("onStop");
		mCalled = true;
	}

	final void performStop() {
		Log.i(TAG, "in performStop");
		if (!mStopped) {
			if (mWindow != null) {
				mWindow.closeAllPanels();
			}

			mCalled = false;
			mInstrumentation.callActivityOnStop(this);
			if (!mCalled) {
				System.out
						.println("Activity did not call through to super.onStop()");
			}

            final int N = mManagedCursors.size();
            for (int i=0; i<N; i++) {
                ManagedCursor mc = mManagedCursors.get(i);
                if (!mc.mReleased) {
                    mc.mCursor.deactivate();
                    mc.mReleased = true;
                }
            }

			mStopped = true;
		}
//		mResumed = false;
	}

	void dispatchActivityResult(String who, int requestCode, int resultCode,
			Intent data) {
		if (who == null) {
			onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
                && !event.isCanceled()) {
            onBackPressed();
            return true;
        }
        return false;
    }

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

    private Dialog createDialog(Integer dialogId, Bundle state, Bundle args) {
        final Dialog dialog = onCreateDialog(dialogId, args);
        if (dialog == null) {
            return null;
        }
        dialog.dispatchOnCreate(state);
        return dialog;
    }

    /**
     * Callback for creating dialogs that are managed (saved and restored) for you
     * by the activity.  The default implementation calls through to
     * {@link #onCreateDialog(int)} for compatibility.
     *
     * <p>If you use {@link #showDialog(int)}, the activity will call through to
     * this method the first time, and hang onto it thereafter.  Any dialog
     * that is created by this method will automatically be saved and restored
     * for you, including whether it is showing.
     *
     * <p>If you would like the activity to manage saving and restoring dialogs
     * for you, you should override this method and handle any ids that are
     * passed to {@link #showDialog}.
     *
     * <p>If you would like an opportunity to prepare your dialog before it is shown,
     * override {@link #onPrepareDialog(int, Dialog, Bundle)}.
     *
     * @param id The id of the dialog.
     * @param args The dialog arguments provided to {@link #showDialog(int, Bundle)}.
     * @return The dialog.  If you return null, the dialog will not be created.
     *
     * @see #onPrepareDialog(int, Dialog, Bundle)
     * @see #showDialog(int, Bundle)
     * @see #dismissDialog(int)
     * @see #removeDialog(int)
     */
    protected Dialog onCreateDialog(int id, Bundle args) {
        return onCreateDialog(id);
    }

    /**
     * @deprecated Old no-arguments version of {@link #onCreateDialog(int, Bundle)}.
     */
    @Deprecated
    protected Dialog onCreateDialog(int id) {
        return null;
    }

    /**
     * @deprecated Old no-arguments version of
     * {@link #onPrepareDialog(int, Dialog, Bundle)}.
     */
    @Deprecated
    protected void onPrepareDialog(int id, Dialog dialog) {
        dialog.setOwnerActivity(this);
    }

    /**
     * Provides an opportunity to prepare a managed dialog before it is being
     * shown.  The default implementation calls through to
     * {@link #onPrepareDialog(int, Dialog)} for compatibility.
     *
     * <p>
     * Override this if you need to update a managed dialog based on the state
     * of the application each time it is shown. For example, a time picker
     * dialog might want to be updated with the current time. You should call
     * through to the superclass's implementation. The default implementation
     * will set this Activity as the owner activity on the Dialog.
     *
     * @param id The id of the managed dialog.
     * @param dialog The dialog.
     * @param args The dialog arguments provided to {@link #showDialog(int, Bundle)}.
     * @see #onCreateDialog(int, Bundle)
     * @see #showDialog(int)
     * @see #dismissDialog(int)
     * @see #removeDialog(int)
     */
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        onPrepareDialog(id, dialog);
    }

    /**
     * Simple version of {@link #showDialog(int, Bundle)} that does not
     * take any arguments.  Simply calls {@link #showDialog(int, Bundle)}
     * with null arguments.
     */
    public final void showDialog(int id) {
         showDialog(id, null);
    }

    /**
     * Show a dialog managed by this activity.  A call to {@link #onCreateDialog(int, Bundle)}
     * will be made with the same id the first time this is called for a given
     * id.  From thereafter, the dialog will be automatically saved and restored.
     *
     * <p>Each time a dialog is shown, {@link #onPrepareDialog(int, Dialog, Bundle)} will
     * be made to provide an opportunity to do any timely preparation.
     *
     * @param id The id of the managed dialog.
     * @param args Arguments to pass through to the dialog.  These will be saved
     * and restored for you.  Note that if the dialog is already created,
     * {@link #onCreateDialog(int, Bundle)} will not be called with the new
     * arguments but {@link #onPrepareDialog(int, Dialog, Bundle)} will be.
     * If you need to rebuild the dialog, call {@link #removeDialog(int)} first.
     * @return Returns true if the Dialog was created; false is returned if
     * it is not created because {@link #onCreateDialog(int, Bundle)} returns false.
     *
     * @see Dialog
     * @see #onCreateDialog(int, Bundle)
     * @see #onPrepareDialog(int, Dialog, Bundle)
     * @see #dismissDialog(int)
     * @see #removeDialog(int)
     */
    public final boolean showDialog(int id, Bundle args) {
        if (mManagedDialogs == null) {
            mManagedDialogs = new SparseArray<ManagedDialog>();
        }
        ManagedDialog md = mManagedDialogs.get(id);
        if (md == null) {
            md = new ManagedDialog();
            md.mDialog = createDialog(id, null, args);
            if (md.mDialog == null) {
                return false;
            }
            mManagedDialogs.put(id, md);
        }

        md.mArgs = args;
        onPrepareDialog(id, md.mDialog, args);
        md.mDialog.show();
        return true;
    }

    /**
     * Change the title associated with this activity.  If this is a
     * top-level activity, the title for its window will change.  If it
     * is an embedded activity, the parent can do whatever it wants
     * with it.
     */
    public void setTitle(CharSequence title) {
        mTitle = title;
        onTitleChanged(title, mTitleColor);

        if (mParent != null) {
            mParent.onChildTitleChanged(this, title);
        }
    }

    /**
     * Change the title associated with this activity.  If this is a
     * top-level activity, the title for its window will change.  If it
     * is an embedded activity, the parent can do whatever it wants
     * with it.
     */
    public void setTitle(int titleId) {
        setTitle(getText(titleId));
    }

    public void setTitleColor(int textColor) {
        mTitleColor = textColor;
        onTitleChanged(mTitle, textColor);
    }

    public final CharSequence getTitle() {
        return mTitle;
    }

    public final int getTitleColor() {
        return mTitleColor;
    }

    protected void onTitleChanged(CharSequence title, int color) {
        if (mTitleReady) {
            final Window win = getWindow();
            if (win != null) {
                win.setTitle(title);
                if (color != 0) {
                    win.setTitleColor(color);
                }
            }
        }
    }

    protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
    }

    /**
     * Sets the visibility of the progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public final void setProgressBarVisibility(boolean visible) {
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, visible ? Window.PROGRESS_VISIBILITY_ON :
            Window.PROGRESS_VISIBILITY_OFF);
    }

    /**
     * Sets the visibility of the indeterminate progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public final void setProgressBarIndeterminateVisibility(boolean visible) {
        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                visible ? Window.PROGRESS_VISIBILITY_ON : Window.PROGRESS_VISIBILITY_OFF);
    }

    /**
     * Sets whether the horizontal progress bar in the title should be indeterminate (the circular
     * is always indeterminate).
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param indeterminate Whether the horizontal progress bar should be indeterminate.
     */
    public final void setProgressBarIndeterminate(boolean indeterminate) {
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                indeterminate ? Window.PROGRESS_INDETERMINATE_ON : Window.PROGRESS_INDETERMINATE_OFF);
    }

    /**
     * Sets the progress for the progress bars in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param progress The progress for the progress bar. Valid ranges are from
     *            0 to 10000 (both inclusive). If 10000 is given, the progress
     *            bar will be completely filled and will fade out.
     */
    public final void setProgress(int progress) {
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress + Window.PROGRESS_START);
    }

    /**
     * Sets the secondary progress for the progress bar in the title. This
     * progress is drawn between the primary progress (set via
     * {@link #setProgress(int)} and the background. It can be ideal for media
     * scenarios such as showing the buffering progress while the default
     * progress shows the play progress.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param secondaryProgress The secondary progress for the progress bar. Valid ranges are from
     *            0 to 10000 (both inclusive).
     */
    public final void setSecondaryProgress(int secondaryProgress) {
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                secondaryProgress + Window.PROGRESS_SECONDARY_START);
    }

    public boolean isFinishing() {
        return mFinished;
    }

    /**
     * This is called for activities that set launchMode to "singleTop" in
     * their package, or if a client used the {@link Intent#FLAG_ACTIVITY_SINGLE_TOP}
     * flag when calling {@link #startActivity}.  In either case, when the
     * activity is re-launched while at the top of the activity stack instead
     * of a new instance of the activity being started, onNewIntent() will be
     * called on the existing instance with the Intent that was used to
     * re-launch it.
     *
     * <p>An activity will always be paused before receiving a new intent, so
     * you can count on {@link #onResume} being called after this method.
     *
     * <p>Note that {@link #getIntent} still returns the original Intent.  You
     * can use {@link #setIntent} to update it to this new Intent.
     *
     * @param intent The new intent that was started for the activity.
     *
     * @see #getIntent
     * @see #setIntent
     * @see #onResume
     */
    protected void onNewIntent(Intent intent) {
    }

    /**
     * Called after {@link #onStop} when the current activity is being
     * re-displayed to the user (the user has navigated back to it).  It will
     * be followed by {@link #onStart} and then {@link #onResume}.
     *
     * <p>For activities that are using raw {@link Cursor} objects (instead of
     * creating them through
     * {@link #managedQuery(android.net.Uri , String[], String, String[], String)},
     * this is usually the place
     * where the cursor should be requeried (because you had deactivated it in
     * {@link #onStop}.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onStop
     * @see #onStart
     * @see #onResume
     */
    protected void onRestart() {
        mCalled = true;
    }

    final void attach(Context context, ActivityThread aThread, Instrumentation instr, IBinder token,
            Application application, Intent intent, ActivityInfo info, CharSequence title,
            Activity parent, String id, Object lastNonConfigurationInstance,
            Configuration config) {
        attach(context, aThread, instr, token, 0, application, intent, info, title, parent, id,
            lastNonConfigurationInstance, null, config);
    }

    final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            Object lastNonConfigurationInstance,
            HashMap<String,Object> lastNonConfigurationChildInstances,
            Configuration config) {
        attachBaseContext(context);
/*
        mWindow = PolicyManager.makeNewWindow(this);
        mWindow.setCallback(this);
        if (info.softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
            mWindow.setSoftInputMode(info.softInputMode);
        }
        mUiThread = Thread.currentThread();

        mMainThread = aThread;
        mInstrumentation = instr;
        mToken = token;
        mIdent = ident;
        mApplication = application;
        mIntent = intent;
        mComponent = intent.getComponent();
        mActivityInfo = info;
        mTitle = title;
        mParent = parent;
        mEmbeddedID = id;
        mLastNonConfigurationInstance = lastNonConfigurationInstance;
        mLastNonConfigurationChildInstances = lastNonConfigurationChildInstances;

        mWindow.setWindowManager(null, mToken, mComponent.flattenToString());
        if (mParent != null) {
            mWindow.setContainer(mParent.getWindow());
        }
        mWindowManager = mWindow.getWindowManager();
        mCurrentConfig = config;
        */
    }
    
    /**
     * Returns a {@link MenuInflater} with this context.
     */
    public MenuInflater getMenuInflater() {
        return new MenuInflater(this);
    }

    public SharedPreferences getPreferences(int mode) {
        return getSharedPreferences(getLocalClassName(), mode);
    }

    /**
     * @j2sNative
     * console.log("Missing method: setRequestedOrientation");
     */
    @MayloonStubAnnotation()
    public void setRequestedOrientation(int requestedOrientation) {
        System.out.println("Stub" + " Function : setRequestedOrientation");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getTaskId");
     */
    @MayloonStubAnnotation()
    public int getTaskId() {
        System.out.println("Stub" + " Function : getTaskId");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onRetainNonConfigurationInstance");
     */
    @MayloonStubAnnotation()
    public Object onRetainNonConfigurationInstance() {
        System.out.println("Stub" + " Function : onRetainNonConfigurationInstance");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: takeKeyEvents");
     */
    @MayloonStubAnnotation()
    public void takeKeyEvents(boolean get) {
        System.out.println("Stub" + " Function : takeKeyEvents");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getCallingPackage");
     */
    @MayloonStubAnnotation()
    public String getCallingPackage() {
        System.out.println("Stub" + " Function : getCallingPackage");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setPersistent");
     */
    @MayloonStubAnnotation()
    public void setPersistent(boolean isPersistent) {
        System.out.println("Stub" + " Function : setPersistent");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: dismissDialog");
     */
    @MayloonStubAnnotation()
    public final void dismissDialog(int id) {
        System.out.println("Stub" + " Function : dismissDialog");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setVolumeControlStream");
     */
    @MayloonStubAnnotation()
    public final void setVolumeControlStream(int streamType) {
        System.out.println("Stub" + " Function : setVolumeControlStream");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: hasWindowFocus");
     */
    @MayloonStubAnnotation()
    public boolean hasWindowFocus() {
        System.out.println("Stub" + " Function : hasWindowFocus");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getLastNonConfigurationInstance");
     */
    @MayloonStubAnnotation()
    public Object getLastNonConfigurationInstance() {
        System.out.println("Stub" + " Function : getLastNonConfigurationInstance");
        return null;
    }

    public String getLocalClassName() {
        final String pkg = getPackageName();
        final String cls = mComponent.getClassName();
        int packageLen = pkg.length();
        if (!cls.startsWith(pkg) || cls.length() <= packageLen
                || cls.charAt(packageLen) != '.') {
            return cls;
        }
        return cls.substring(packageLen+1);
    }

    /**
     * @j2sNative
     * console.log("Missing method: isTaskRoot");
     */
    @MayloonStubAnnotation()
    public boolean isTaskRoot() {
        System.out.println("Stub" + " Function : isTaskRoot");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onUserLeaveHint");
     */
    @MayloonStubAnnotation()
    protected void onUserLeaveHint() {
        System.out.println("Stub" + " Function : onUserLeaveHint");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onAttachedToWindow");
     */
    @MayloonStubAnnotation()
    public void onAttachedToWindow() {
        System.out.println("Stub" + " Function : onAttachedToWindow");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onWindowFocusChanged");
     */
    @MayloonStubAnnotation()
    public void onWindowFocusChanged(boolean hasFocus) {
        System.out.println("Stub" + " Function : onWindowFocusChanged");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onLowMemory");
     */
    @MayloonStubAnnotation()
    public void onLowMemory() {
        System.out.println("Stub" + " Function : onLowMemory");
        return;
    }

    public final void runOnUiThread(Runnable action) {
        if (ActivityThread.currentActivityThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: finishActivity");
     */
    @MayloonStubAnnotation()
    public void finishActivity(int requestCode) {
        System.out.println("Stub" + " Function : finishActivity");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onDetachedFromWindow");
     */
    @MayloonStubAnnotation()
    public void onDetachedFromWindow() {
        System.out.println("Stub" + " Function : onDetachedFromWindow");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getChangingConfigurations");
     */
    @MayloonStubAnnotation()
    public int getChangingConfigurations() {
        System.out.println("Stub" + " Function : getChangingConfigurations");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getVolumeControlStream");
     */
    @MayloonStubAnnotation()
    public final int getVolumeControlStream() {
        System.out.println("Stub" + " Function : getVolumeControlStream");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: removeDialog");
     */
    @MayloonStubAnnotation()
    public final void removeDialog(int id) {
        System.out.println("Stub" + " Function : removeDialog");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onCreateDescription");
     */
    @MayloonStubAnnotation()
    public CharSequence onCreateDescription() {
        System.out.println("Stub" + " Function : onCreateDescription");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: moveTaskToBack");
     */
    @MayloonStubAnnotation()
    public boolean moveTaskToBack(boolean nonRoot) {
        System.out.println("Stub" + " Function : moveTaskToBack");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onSearchRequested");
     */
    @MayloonStubAnnotation()
    public boolean onSearchRequested() {
        System.out.println("Stub" + " Function : onSearchRequested");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: finishFromChild");
     */
    @MayloonStubAnnotation()
    public void finishFromChild(Activity child) {
        System.out.println("Stub" + " Function : finishFromChild");
        return;
    }

    /**
     * @j2sNative 
     * console.log("Missing method: getRequestedOrientation");
     */
    @MayloonStubAnnotation()
    public int getRequestedOrientation() {
        System.out.println("Stub" + " Function : getRequestedOrientation");
        return 0;
    }

    /**
     * Called by the system when the device configuration changes while your
     * activity is running.  Note that this will <em>only</em> be called if
     * you have selected configurations you would like to handle with the
     * {@link android.R.attr#configChanges} attribute in your manifest.  If
     * any configuration change occurs that is not selected to be reported
     * by that attribute, then instead of reporting it the system will stop
     * and restart the activity (to have it launched with the new
     * configuration).
     * 
     * <p>At the time that this function has been called, your Resources
     * object will have been updated to return resource values matching the
     * new configuration.
     * 
     * @param newConfig The new device configuration.
     */
    public void onConfigurationChanged(Configuration newConfig) {
        mCalled = true;

        if (mWindow != null) {
            // Pass the configuration changed event to the window
            mWindow.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (getBaseContext() == null) {
            throw new IllegalStateException(
                    "System services not available to Activities before onCreate()");
        }

        if (WINDOW_SERVICE.equals(name)) {
            return mWindowManager;
        }
        return super.getSystemService(name);
    }

}
