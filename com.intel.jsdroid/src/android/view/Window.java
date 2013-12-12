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

package android.view;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;

import com.android.internal.R;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuDialogHelper;
import com.android.internal.view.menu.MenuItemImpl;
import com.android.internal.view.menu.MenuView;
import com.android.internal.view.menu.SubMenuBuilder;
import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.view.ContextMenu;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Config;
import android.util.DebugUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Abstract base class for a top-level window look and behavior policy. An
 * instance of this class should be used as the top-level view added to the
 * window manager. It provides standard UI policies such as a background, title
 * area, default key processing, etc.
 *
 * <p>
 * The only existing implementation of this abstract class is
 * android.policy.PhoneWindow, which you should instantiate when needing a
 * Window. Eventually that class will be refactored and a factory method added
 * for creating Window instances without knowing about a particular
 * implementation.
 */
public class Window implements MenuBuilder.Callback {
    private final static String TAG = "PhoneWindow";
	/** Flag for the "options panel" feature.  This is enabled by default. */
	public static final int FEATURE_OPTIONS_PANEL = 0;
	/** Flag for the "no title" feature, turning off the title at the top
	 *  of the screen. */
	public static final int FEATURE_NO_TITLE = 1;
	/** Flag for the progress indicator feature */
	public static final int FEATURE_PROGRESS = 2;
	/** Flag for having an icon on the left side of the title bar */
	public static final int FEATURE_LEFT_ICON = 3;
	/** Flag for having an icon on the right side of the title bar */
	public static final int FEATURE_RIGHT_ICON = 4;
	/** Flag for indeterminate progress */
	public static final int FEATURE_INDETERMINATE_PROGRESS = 5;
	/** Flag for the context menu.  This is enabled by default. */
	public static final int FEATURE_CONTEXT_MENU = 6;
	/** Flag for custom title. You cannot combine this feature with other title features. */
	public static final int FEATURE_CUSTOM_TITLE = 7;

    /** Flag for asking for an OpenGL enabled window.
    All 2D graphics will be handled by OpenGL ES.
    @hide
    */
    public static final int FEATURE_OPENGL = 8;
    /** Flag for the global button. This is simulateBack key. */
    public static final int FEATURE_BACKKEY = 9;
    /** Flag for setting the progress bar's visibility to VISIBLE */
    public static final int PROGRESS_VISIBILITY_ON = -1;
    /** Flag for setting the progress bar's visibility to GONE */
    public static final int PROGRESS_VISIBILITY_OFF = -2;
    /** Flag for setting the progress bar's indeterminate mode on */
    public static final int PROGRESS_INDETERMINATE_ON = -3;
    /** Flag for setting the progress bar's indeterminate mode off */
    public static final int PROGRESS_INDETERMINATE_OFF = -4;
    /** Starting value for the (primary) progress */
    public static final int PROGRESS_START = 0;
    /** Ending value for the (primary) progress */
    public static final int PROGRESS_END = 10000;
    /** Lowest possible value for the secondary progress */
    public static final int PROGRESS_SECONDARY_START = 20000;
    /** Highest possible value for the secondary progress */
    public static final int PROGRESS_SECONDARY_END = 30000;

    /** The default features enabled */
    @SuppressWarnings({"PointlessBitwiseExpression"})
    protected static final int DEFAULT_FEATURES = (1 << FEATURE_OPTIONS_PANEL) |
            (1 << FEATURE_CONTEXT_MENU) | (1 << FEATURE_BACKKEY);


	public static final int ID_ANDROID_CONTENT = com.android.internal.R.id.content;
	public static final int ID_BACK_KEY = com.android.internal.R.id.backKey;
	public static final int ID_MENU_KEY = com.android.internal.R.id.menuKey;

	public static final int RELAYOUT_FIRST_TIME = 0x2;
	// This is the view in which the window contents are placed. It is either
	// mDecor itself, or a child of mDecor where the contents go.
	private ViewGroup mContentParent;
	SurfaceHolder.Callback2 mTakeSurfaceCallback;
	private LayoutInflater mLayoutInflater;
	private final Context mContext;

    private CharSequence mTitle = null;

    private int mTitleColor = 0;
    
    private ContextMenu mContextMenu;

    private int mBackgroundResource = 0;

    private Drawable mBackgroundDrawable;

    private int mFrameResource = 0;
    
    private int mTextColor = 0;

	//	private ViewRoot mRoot;

    private TypedArray mWindowStyle;
    private Callback mCallback;
    private boolean mIsActive = false;

	// The current window attributes.
	private final LayoutParams mWindowAttributes = new LayoutParams();

    // This is the top-level view of the window, containing the window decor.
    private DecorView mDecor;
    
    private boolean mIsFloating;

    private PanelFeatureState[] mPanels;

    private DrawableFeatureState[] mDrawables;

    private ImageView mLeftIconView;
    private TextView mTitleView;
    private ImageView mRightIconView;
    private ProgressBar mCircularProgressBar;
    private ProgressBar mHorizontalProgressBar;
	
	private int mFeatures = DEFAULT_FEATURES;
    private int mLocalFeatures = DEFAULT_FEATURES;
    private MenuDialogHelper mContextMenuHelper;
    private static boolean beContentOfTab = false;

    /**
     * API from a Window back to its caller.  This allows the client to
     * intercept key dispatching, panels and menus, etc.
     */
    public interface Callback {
        /**
         * Called to process key events.  At the very least your
         * implementation must call
         * {@link android.view.Window#superDispatchKeyEvent} to do the
         * standard key processing.
         *
         * @param event The key event.
         *
         * @return boolean Return true if this event was consumed.
         */
        public boolean dispatchKeyEvent(KeyEvent event);

        /**
         * Called to process touch screen events.  At the very least your
         * implementation must call
         * {@link android.view.Window#superDispatchTouchEvent} to do the
         * standard touch screen processing.
         *
         * @param event The touch screen event.
         *
         * @return boolean Return true if this event was consumed.
         */
        public boolean dispatchTouchEvent(MotionEvent event);
        
        /**
         * Called to process trackball events.  At the very least your
         * implementation must call
         * {@link android.view.Window#superDispatchTrackballEvent} to do the
         * standard trackball processing.
         *
         * @param event The trackball event.
         *
         * @return boolean Return true if this event was consumed.
         */
        //public boolean dispatchTrackballEvent(MotionEvent event);

        /**
         * Called to process population of {@link AccessibilityEvent}s.
         *
         * @param event The event.
         *
         * @return boolean Return true if event population was completed.
         */
        //public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event);

        /**
         * Instantiate the view to display in the panel for 'featureId'.
         * You can return null, in which case the default content (typically
         * a menu) will be created for you.
         *
         * @param featureId Which panel is being created.
         *
         * @return view The top-level view to place in the panel.
         *
         * @see #onPreparePanel
         */
        public View onCreatePanelView(int featureId);

        /**
         * Initialize the contents of the menu for panel 'featureId'.  This is
         * called if onCreatePanelView() returns null, giving you a standard
         * menu in which you can place your items.  It is only called once for
         * the panel, the first time it is shown.
         *
         * <p>You can safely hold on to <var>menu</var> (and any items created
         * from it), making modifications to it as desired, until the next
         * time onCreatePanelMenu() is called for this feature.
         *
         * @param featureId The panel being created.
         * @param menu The menu inside the panel.
         *
         * @return boolean You must return true for the panel to be displayed;
         *         if you return false it will not be shown.
         */
        public boolean onCreatePanelMenu(int featureId, Menu menu);

        /**
         * Prepare a panel to be displayed.  This is called right before the
         * panel window is shown, every time it is shown.
         *
         * @param featureId The panel that is being displayed.
         * @param view The View that was returned by onCreatePanelView().
         * @param menu If onCreatePanelView() returned null, this is the Menu
         *             being displayed in the panel.
         *
         * @return boolean You must return true for the panel to be displayed;
         *         if you return false it will not be shown.
         *
         * @see #onCreatePanelView
         */
        public boolean onPreparePanel(int featureId, View view, Menu menu);

        /**
         * Called when a panel's menu is opened by the user. This may also be
         * called when the menu is changing from one type to another (for
         * example, from the icon menu to the expanded menu).
         * 
         * @param featureId The panel that the menu is in.
         * @param menu The menu that is opened.
         * @return Return true to allow the menu to open, or false to prevent
         *         the menu from opening.
         */
        public boolean onMenuOpened(int featureId, Menu menu);
        
        /**
         * Called when a panel's menu item has been selected by the user.
         *
         * @param featureId The panel that the menu is in.
         * @param item The menu item that was selected.
         *
         * @return boolean Return true to finish processing of selection, or
         *         false to perform the normal menu handling (calling its
         *         Runnable or sending a Message to its target Handler).
         */
        public boolean onMenuItemSelected(int featureId, MenuItem item);

        /**
         * This is called whenever the current window attributes change.
         *
         */
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs);

        /**
         * This hook is called whenever the content view of the screen changes
         * (due to a call to
         * {@link Window#setContentView(View, android.view.ViewGroup.LayoutParams)
         * Window.setContentView} or
         * {@link Window#addContentView(View, android.view.ViewGroup.LayoutParams)
         * Window.addContentView}).
         */
        public void onContentChanged();

        /**
         * This hook is called whenever the window focus changes.  See
         * {@link View#onWindowFocusChanged(boolean)
         * View.onWindowFocusChanged(boolean)} for more information.
         *
         * @param hasFocus Whether the window now has focus.
         */
        //public void onWindowFocusChanged(boolean hasFocus);

        /**
         * Called when the window has been attached to the window manager.
         * See {@link View#onAttachedToWindow() View.onAttachedToWindow()}
         * for more information.
         */
        //public void onAttachedToWindow();
        
        /**
         * Called when the window has been attached to the window manager.
         * See {@link View#onDetachedFromWindow() View.onDetachedFromWindow()}
         * for more information.
         */
        //public void onDetachedFromWindow();
        
        /**
         * Called when a panel is being closed.  If another logical subsequent
         * panel is being opened (and this panel is being closed to make room for the subsequent
         * panel), this method will NOT be called.
         * 
         * @param featureId The panel that is being displayed.
         * @param menu If onCreatePanelView() returned null, this is the Menu
         *            being displayed in the panel.
         */
        public void onPanelClosed(int featureId, Menu menu);
        
        /**
         * Called when the user signals the desire to start a search.
         * 
         * @return true if search launched, false if activity refuses (blocks)
         * 
         * @see android.app.Activity#onSearchRequested() 
         */
        //public boolean onSearchRequested();
    }

    /**
     * Return the window flags that have been explicitly set by the client, so
     * will not be modified by {@link #getDecorView}.
     */
    protected final int getForcedWindowFlags() {
        return mForcedWindowFlags;
    }

	public View getDecorView() {
        if (mDecor == null) {
            installDecor();
        }
        return mDecor;
	}

    public final boolean isActive()
    {
        return mIsActive;
    }

	public View findViewById(int id) {
		return getDecorView().findViewById(id);
	}

	/**
	 * Set the Callback interface for this window, used to intercept key events
	 * and other dynamic operations in the window.
	 *
	 * @param callback
	 *            The desired Callback interface.
	 */
	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	/**
	 * Return the current Callback interface for this window.
	 */
	public final Callback getCallback() {
		return mCallback;
	}

	/**
	 * Convenience for
	 * {@link #setContentView(View, android.view.ViewGroup.LayoutParams)} to set
	 * the screen content from a layout resource. The resource will be inflated,
	 * adding all top-level views to the screen.
	 *
	 * @param layoutResID
	 *            Resource ID to be inflated.
	 * @see #setContentView(View, android.view.ViewGroup.LayoutParams)
	 */
	public void setContentView(int layoutResID) {
		if (mContentParent == null) {
			installDecor();
		} else {
			mContentParent.removeAllViews();
		}
		mLayoutInflater.inflate(layoutResID, mContentParent);
		final Callback cb = getCallback();
		if (cb != null) {
			cb.onContentChanged();
		}
	}

	public void setContentView(View view) {
		 setContentView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
	}

	public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mContentParent == null) {
            installDecor();
        } else {
            mContentParent.removeAllViews();
        }
        mContentParent.addView(view, params);
        final Callback cb = getCallback();
        if (cb != null ) {
            cb.onContentChanged();
        }
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        if (mContentParent == null) {
            installDecor();
        }
        mContentParent.addView(view, params);
        final Callback cb = getCallback();
        if (cb != null) {
            cb.onContentChanged();
        }
    }

    public View getCurrentFocus() {
        return mDecor != null ? mDecor.findFocus() : null;
    }

    public void takeSurface(SurfaceHolder.Callback2 callback) {
        mTakeSurfaceCallback = callback;
    }
    
	/**
	 * Return a LayoutInflater instance that can be used to inflate XML view layout
	 * resources for use in this Window.
	 *
	 * @return LayoutInflater The shared LayoutInflater.
	 */
	public LayoutInflater getLayoutInflater() {
		return mLayoutInflater;
	}

    public void setTitle(CharSequence title) {
        String tmp = title.toString();
        /**
         * @j2sNative
         * document.title = tmp;
         */
        {}
        mTitle = title;
        if (getTitleView() != null && !getBeContentOfTab()) {
            mTitleView.setText(title);
        }
    }

    public void setTitleColor(int textColor) {
        // Ignore this
        mTitleColor = textColor;
    }

	public final Context getContext() {
		return mContext;
	}

	/**
	 * Return the {@link com.intel.jsdroid.sample.R.styleable#Window} attributes from this
	 * window's theme.
	 */
	public final TypedArray getWindowStyle() {
		synchronized (this) {
			if (mWindowStyle == null) {
				mWindowStyle = mContext
						.obtainStyledAttributes(com.android.internal.R.styleable.Window);
			}
			return mWindowStyle;
		}
	}

	protected DecorView generateDecor() {
//		System.out.println("******** Creating view: DecorView");
		return new DecorView(getContext());
	}
	public DecorView generateDecor(Context ctx){
		return new DecorView(ctx);
	}
	private void installDecor() {
		if (mDecor == null) {
			mDecor = generateDecor();
			mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			mDecor.setIsRootNamespace(true);
		}
		if (mContentParent == null) {
			mContentParent = generateLayout(mDecor);
		}
	}

	protected ViewGroup generateLayout(DecorView decor) {
        // Apply data from current theme.

        TypedArray a = getWindowStyle();

        if (false) {
            System.out.println("From style:");
            String s = "Attrs:";
            for (int i = 0; i < com.android.internal.R.styleable.Window.length; i++) {
                s = s + " " + Integer.toHexString(com.android.internal.R.styleable.Window[i]) + "="
                        + a.getString(i);
            }
            System.out.println(s);
        }
        
        mIsFloating = a.getBoolean(com.android.internal.R.styleable.Window_windowIsFloating, false);
        int flagsToUpdate = (FLAG_LAYOUT_IN_SCREEN|FLAG_LAYOUT_INSET_DECOR)
                & (~getForcedWindowFlags());
        if (mIsFloating) {
            setLayout(WRAP_CONTENT, WRAP_CONTENT);
            setFlags(0, flagsToUpdate);
        } else {
            setFlags(FLAG_LAYOUT_IN_SCREEN|FLAG_LAYOUT_INSET_DECOR, flagsToUpdate);
        }

        if (a.getBoolean(com.android.internal.R.styleable.Window_windowNoTitle, false)) {
            requestFeature(FEATURE_NO_TITLE);
        }

        if (a.getBoolean(com.android.internal.R.styleable.Window_windowFullscreen, false)) {
            setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN&(~getForcedWindowFlags()));
        }

        if (a.getBoolean(com.android.internal.R.styleable.Window_windowShowWallpaper, false)) {
            setFlags(FLAG_SHOW_WALLPAPER, FLAG_SHOW_WALLPAPER&(~getForcedWindowFlags()));
        }

        WindowManager.LayoutParams params = getAttributes();

//        if (!hasSoftInputMode()) {
//            params.softInputMode = a.getInt(
//                    com.android.internal.R.styleable.Window_windowSoftInputMode,
//                    params.softInputMode);
//        }

        if (a.getBoolean(com.android.internal.R.styleable.Window_backgroundDimEnabled,
                mIsFloating)) {
            /* All dialogs should have the window dimmed */
            if ((getForcedWindowFlags()&WindowManager.LayoutParams.FLAG_DIM_BEHIND) == 0) {
                params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            }
            params.dimAmount = a.getFloat(
                    android.R.styleable.Window_backgroundDimAmount, 0.5f);
        }

        if (params.windowAnimations == 0) {
            params.windowAnimations = a.getResourceId(
                    com.android.internal.R.styleable.Window_windowAnimationStyle, 0);
        }
        
        // The rest are only done if this window is not embedded; otherwise,
        // the values are inherited from our container.
        if (getContainer() == null) {
            if (mBackgroundDrawable == null) {
                if (mBackgroundResource == 0) {
                    mBackgroundResource = a.getResourceId(
                            com.android.internal.R.styleable.Window_windowBackground, 0);
                }
                if (mFrameResource == 0) {
                    mFrameResource =
                            a.getResourceId(com.android.internal.R.styleable.Window_windowFrame,
                                    0);
                }
                if (false) {
                    System.out.println("Background: "
                            + Integer.toHexString(mBackgroundResource) + " Frame: "
                            /*+ Integer.toHexString(mFrameResource)*/);
                }
            }
            mTextColor = a.getColor(com.android.internal.R.styleable.Window_textColor, 0xFF000000);
        }

        // Inflate the window decor.

        // In MayLoon, all Window should not have title.
        requestFeature(Window.FEATURE_NO_TITLE);
        
        int features = getLocalFeatures();
        int layoutResource = com.android.internal.R.layout.screen_simple;

        // System.out.println("Features: 0x" + Integer.toHexString(features));
        if ((features & ((1 << FEATURE_LEFT_ICON) | (1 << FEATURE_RIGHT_ICON))) != 0) {
            if (mIsFloating) {
                layoutResource = com.android.internal.R.layout.dialog_title_icons;
            } else {
                layoutResource = com.android.internal.R.layout.screen_title_icons;
            }
            // System.out.println("Title Icons!");
        } else if ((features & ((1 << FEATURE_PROGRESS) | (1 << FEATURE_INDETERMINATE_PROGRESS))) != 0) {
            // Special case for a window with only a progress bar (and title).
            // XXX Need to have a no-title version of embedded windows.
            layoutResource = com.android.internal.R.layout.screen_progress;
            // System.out.println("Progress!");
        } else if ((features & (1 << FEATURE_CUSTOM_TITLE)) != 0) {
            // Special case for a window with a custom title.
            // If the window is floating, we need a dialog layout
            if (mIsFloating) {
                layoutResource = com.android.internal.R.layout.dialog_custom_title;
            } else {
                layoutResource = com.android.internal.R.layout.screen_custom_title;
            }
        } else if ((features & (1 << FEATURE_NO_TITLE)) == 0) {
            // If no other features and not embedded, only need a title.
            // If the window is floating, we need a dialog layout
            if (mIsFloating) {
                layoutResource = com.android.internal.R.layout.dialog_title;
            } else {
                layoutResource = com.android.internal.R.layout.screen_title;
            }
            // System.out.println("Title!");
        } else if ((features & (1 << FEATURE_BACKKEY)) != 0) {
            if (mIsFloating) {
                layoutResource = com.android.internal.R.layout.screen_simple;
            } else {
                layoutResource = com.android.internal.R.layout.screen_simple_tizen;
            }
            // System.out.println("Tizen!");
        } else {
            // Embedded, so no decoration is needed.
            layoutResource = com.android.internal.R.layout.screen_simple;
            // System.out.println("Simple!");
        }
        
        //mDecor.startChanging();
        
        View in = ((LayoutInflater) Context.getSystemContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                layoutResource, null);
        decor.addView(in,
                new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        ViewGroup contentParent = (ViewGroup) findViewById(ID_ANDROID_CONTENT);
        if (contentParent == null) {
            throw new RuntimeException(
                    "Window couldn't find content container view");
        }
        
//        if ((features & (1 << FEATURE_INDETERMINATE_PROGRESS)) != 0) {
//            ProgressBar progress = getCircularProgressBar(false);
//            if (progress != null) {
//                progress.setIndeterminate(true);
//            }
//        }

        // Remaining setup -- of background and title -- that only applies
        // to top-level windows.
        if (getContainer() == null) {
            Drawable drawable = mBackgroundDrawable;
            if (mBackgroundResource != 0) {
                drawable = getContext().getResources().getDrawable(mBackgroundResource);
            }
            mDecor.setWindowBackground(drawable);
            drawable = null;
            if (mFrameResource != 0) {
                drawable = getContext().getResources().getDrawable(mFrameResource);
            }
            mDecor.setWindowFrame(drawable);

            // System.out.println("Text=" + Integer.toHexString(mTextColor) +
            // " Sel=" + Integer.toHexString(mTextSelectedColor) +
            // " Title=" + Integer.toHexString(mTitleColor));

            if (mTitleColor == 0) {
                mTitleColor = mTextColor;
            }

            if (mTitle != null) {
                setTitle(mTitle);
            }
            setTitleColor(mTitleColor);
        }

        //mDecor.finishChanging();
        return contentParent;
    }

    private Drawable loadImageURI(Uri uri) {
        try {
            return Drawable.createFromStream(
                    getContext().getContentResolver().openInputStream(uri), null);
        } catch (Exception e) {
            Log.w(TAG, "Unable to open content: " + uri);
        }
        return null;
    }
    
    private DrawableFeatureState getDrawableState(int featureId, boolean required) {
        if ((getFeatures() & (1 << featureId)) == 0) {
            if (!required) {
                return null;
            }
            throw new RuntimeException("The feature has not been requested");
        }

        DrawableFeatureState[] ar;
        if ((ar = mDrawables) == null || ar.length <= featureId) {
            DrawableFeatureState[] nar = new DrawableFeatureState[featureId + 1];
            if (ar != null) {
                System.arraycopy(ar, 0, nar, 0, ar.length);
            }
            mDrawables = ar = nar;
        }

        DrawableFeatureState st = ar[featureId];
        if (st == null) {
            ar[featureId] = st = new DrawableFeatureState(featureId);
        }
        return st;
    }
    
    static private final String FOCUSED_ID_TAG = "android:focusedViewId";
    static private final String VIEWS_TAG = "android:views";
    static private final String PANELS_TAG = "android:Panels";
    public Bundle saveHierarchyState() {
        Bundle outState = new Bundle();
        if (mContentParent == null) {
            return outState;
        }

        SparseArray<Parcelable> states = new SparseArray<Parcelable>();
        mContentParent.saveHierarchyState(states);
        outState.putSparseParcelableArray(VIEWS_TAG, states);

        // save the focused view id
        View focusedView = mContentParent.findFocus();
        if (focusedView != null) {
            if (focusedView.getId() != View.NO_ID) {
                outState.putInt(FOCUSED_ID_TAG, focusedView.getId());
            } else {
                if (Config.LOGD) {
                    Log.d(TAG, "couldn't save which view has focus because the focused view "
                            + focusedView + " has no id.");
                }
            }
        }

        // save the panels
        SparseArray<Parcelable> panelStates = new SparseArray<Parcelable>();
        savePanelState(panelStates);
        if (panelStates.size() > 0) {
            outState.putSparseParcelableArray(PANELS_TAG, panelStates);
        }

        return outState;
    }

    public void restoreHierarchyState(Bundle savedInstanceState) {
        if (mContentParent == null) {
            return;
        }

        SparseArray<Parcelable> savedStates
                = savedInstanceState.getSparseParcelableArray(VIEWS_TAG);
        if (savedStates != null) {
            mContentParent.restoreHierarchyState(savedStates);
        }

        // restore the focused view
        int focusedViewId = savedInstanceState.getInt(FOCUSED_ID_TAG, View.NO_ID);
        if (focusedViewId != View.NO_ID) {
            View needsFocus = mContentParent.findViewById(focusedViewId);
            if (needsFocus != null) {
                needsFocus.requestFocus();
            } else {
                Log.w(TAG,
                        "Previously focused view reported id " + focusedViewId
                                + " during save, but can't be found during restore.");
            }
        }

        // restore the panels
        SparseArray<Parcelable> panelStates = savedInstanceState.getSparseParcelableArray(PANELS_TAG);
        if (panelStates != null) {
            restorePanelState(panelStates);
        }
    }

    /**
     * Invoked when the panels should freeze their state.
     *
     * @param icicles Save state into this. This is usually indexed by the
     *            featureId. This will be given to {@link #restorePanelState} in the
     *            future.
     */
    private void savePanelState(SparseArray<Parcelable> icicles) {
        PanelFeatureState[] panels = mPanels;
        if (panels == null) {
            return;
        }

        for (int curFeatureId = panels.length - 1; curFeatureId >= 0; curFeatureId--) {
            if (panels[curFeatureId] != null) {
                icicles.put(curFeatureId, panels[curFeatureId].onSaveInstanceState());
            }
        }
    }

    /**
     * Invoked when the panels should thaw their state from a previously frozen state.
     *
     * @param icicles The state saved by {@link #savePanelState} that needs to be thawed.
     */
    private void restorePanelState(SparseArray<Parcelable> icicles) {
        PanelFeatureState st;
        for (int curFeatureId = icicles.size() - 1; curFeatureId >= 0; curFeatureId--) {
            st = getPanelState(curFeatureId, false /* required */);
            if (st == null) {
                // The panel must not have been required, and is currently not around, skip it
                continue;
            }

            st.onRestoreInstanceState(icicles.get(curFeatureId));
        }

        /*
         * Implementation note: call openPanelsAfterRestore later to actually open the
         * restored panels.
         */
    }

	public final class DecorView extends FrameLayout {
		/**
		 * The feature ID of the panel, or -1 if this is the application's
		 * DecorView
		 */
		@SuppressWarnings("unused")
		private int mFeatureId = -1;

		public int mDefaultOpacity = PixelFormat.OPAQUE;
		
        private final Rect mBackgroundPadding = new Rect();
        
        private final Rect mFramePadding = new Rect();
        
        private boolean mChanging;

		public DecorView(Context context) {
			super(context);

			/**
			 @j2sNative
			 var thisView = document.getElementById(this.getUIElementID());
			 if (null == thisView) {
			 	thisView = document.createElement("span");
				thisView.style.position = "absolute";
				thisView.id = this.getUIElementID();
				thisView.style.display = "block";
			 }
			 document.body.appendChild(thisView);
			 */
			{
			}
		}

		public DecorView(Context context, int featureId) {
			super(context);
			mFeatureId = featureId;
		}

		public void assignParent(ViewParent parent, boolean modifyDom) {
			if (parent instanceof View) {
				super.assignParent(parent, modifyDom);
                if(DebugUtils.DEBUG_VIEW_IN_BROWSER) {
                    /**
                     @j2sNative
                     var thisView = document.getElementById(this.getUIElementID ());
                     if (this.mFeatureId == 0) {
                        // OptionsMenu
                        thisView.style.opacity = 0.8;
                        thisView.style.filter = "alpha(opacity=80)";
                     }
                     */
                    {
                    }
                }
			} else {
				if (mParent == null) {
					mParent = parent;
				} else if (parent == null) {
					mParent = null;
					return;
				} else {
					throw new RuntimeException("view " + this
							+ " being attached, but"
							+ " it already has been attached to a ViewRoot");
				}
			}
		}

		public void destroy() {
			// remove child
			/**
			 @j2sNative
			 var thisView = document.getElementById(this.mUIElementID);
			 if (thisView != null && thisView.parentNode != null) {
			     thisView.parentNode.removeChild(thisView);
			 }
			*/
			{
			}
			// call ViewRoot's reset
			if (this.mParent instanceof ViewRoot) {
				((ViewRoot) this.mParent).reset();
			}
		}

	    /**
	     * Used by custom windows, such as Dialog, to pass the key press event
	     * further down the view hierarchy. Application developers should
	     * not need to implement or call this.
	     *
	     */
	    public boolean superDispatchKeyEvent(KeyEvent event) {
	        return super.dispatchKeyEvent(event);
	    }

	    /**
	     * Used by custom windows, such as Dialog, to pass the touch screen event
	     * further down the view hierarchy. Application developers should
	     * not need to implement or call this.
	     *
	     */
	    public boolean superDispatchTouchEvent(MotionEvent event) {
            long start = System.currentTimeMillis();
            Log.d(TAG, "DecorView superDispatchTouchEvent at time:" + start);
	        return super.dispatchTouchEvent(event);
	    }

	    @Override
	    public boolean dispatchKeyEvent(KeyEvent event) {
	        final int keyCode = event.getKeyCode();
	        final boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
//
//	        /*
//	         * If the user hits another key within the play sound delay, then
//	         * cancel the sound
//
//	        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP
//	                && mVolumeKeyUpTime + VolumePanel.PLAY_SOUND_DELAY
//	                        > SystemClock.uptimeMillis()) {
//
//	             * The user has hit another key during the delay (e.g., 300ms)
//	             * since the last volume key up, so cancel any sounds.
//
//	            AudioManager audioManager = (AudioManager) getContext().getSystemService(
//	                    Context.AUDIO_SERVICE);
//	            if (audioManager != null) {
//	                audioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME,
//	                        mVolumeControlStreamType, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
//	            }
//	        }
//
            if (isDown && (event.getRepeatCount() == 0)) {
                // First handle chording of panel key: if a panel key is held
                // but not released, try to execute a shortcut in it.
                // if ((mPanelChordingKey > 0) && (mPanelChordingKey !=
                // keyCode)) {
                // // Perform the shortcut (mPreparedPanel can be null since
                // // global shortcuts (such as search) don't rely on a
                // // prepared panel or menu).
                // boolean handled = performPanelShortcut(mPreparedPanel,
                // keyCode, event,
                // Menu.FLAG_PERFORM_NO_CLOSE);
                //
                // if (!handled) {
                // /*
                // * If not handled, then pass it to the view hierarchy
                // * and anyone else that may be interested.
                // */
                // handled = dispatchKeyShortcutEvent(event);
                //
                // if (handled && mPreparedPanel != null) {
                // mPreparedPanel.isHandled = true;
                // }
                // }
                //
                // if (handled) {
                // return true;
                // }
                // }

                // If a panel is open, perform a shortcut on it without the
                // chorded panel key
                if ((mPreparedPanel != null) && mPreparedPanel.isOpen) {
                    if (performPanelShortcut(mPreparedPanel, keyCode, event, 0)) {
                        return true;
                    }
                }
            }

	        final Callback cb = getCallback();
	        final boolean handled = (cb != null && mFeatureId < 0
	        		                 ? cb.dispatchKeyEvent(event)
	                                 : super.dispatchKeyEvent(event));
	        if (handled) {
	            return true;
	        }
	        return isDown
	        	   ? Window.this.onKeyDown(mFeatureId, event.getKeyCode(), event)
	               : Window.this.onKeyUp(mFeatureId, event.getKeyCode(), event);
	    }

	    @Override
	    public boolean dispatchTouchEvent(MotionEvent ev) {
	        final Callback cb = getCallback();
	        return (cb != null && mFeatureId < 0
	        		? cb.dispatchTouchEvent(ev)
	        		: super.dispatchTouchEvent(ev));
	    }
	    
        public void startChanging() {
            mChanging = true;
        }

        public void finishChanging() {
            mChanging = false;
            drawableChanged();
        }
        
        public void setWindowBackground(Drawable drawable) {
            if (getBackground() != drawable) {
                setBackgroundDrawable(drawable);
                if (drawable != null) {
                    drawable.getPadding(mBackgroundPadding);
                } else {
                    mBackgroundPadding.setEmpty();
                }
                drawableChanged();
            }
        }
        
        public void setWindowFrame(Drawable drawable) {
            if (getForeground() != drawable) {
                setForeground(drawable);
                if (drawable != null) {
                    drawable.getPadding(mFramePadding);
                } else {
                    mFramePadding.setEmpty();
                }
                drawableChanged();
            }
        }
        
        private void drawableChanged() {
            if (mChanging) {
                return;
            }

            setPadding(mFramePadding.left + mBackgroundPadding.left, mFramePadding.top
                    + mBackgroundPadding.top, mFramePadding.right + mBackgroundPadding.right,
                    mFramePadding.bottom + mBackgroundPadding.bottom);
            requestLayout();
            invalidate();

//            int opacity = PixelFormat.OPAQUE;
//
//            // Note: if there is no background, we will assume opaque. The
//            // common case seems to be that an application sets there to be
//            // no background so it can draw everything itself. For that,
//            // we would like to assume OPAQUE and let the app force it to
//            // the slower TRANSLUCENT mode if that is really what it wants.
//            Drawable bg = getBackground();
//            Drawable fg = getForeground();
//            if (bg != null) {
//                if (fg == null) {
//                    opacity = bg.getOpacity();
//                } else if (mFramePadding.left <= 0 && mFramePadding.top <= 0
//                        && mFramePadding.right <= 0 && mFramePadding.bottom <= 0) {
//                    // If the frame padding is zero, then we can be opaque
//                    // if either the frame -or- the background is opaque.
//                    int fop = fg.getOpacity();
//                    int bop = bg.getOpacity();
//                    if (Config.LOGV)
//                        Log.v(TAG, "Background opacity: " + bop + ", Frame opacity: " + fop);
//                    if (fop == PixelFormat.OPAQUE || bop == PixelFormat.OPAQUE) {
//                        opacity = PixelFormat.OPAQUE;
//                    } else if (fop == PixelFormat.UNKNOWN) {
//                        opacity = bop;
//                    } else if (bop == PixelFormat.UNKNOWN) {
//                        opacity = fop;
//                    } else {
//                        opacity = Drawable.resolveOpacity(fop, bop);
//                    }
//                } else {
//                    // For now we have to assume translucent if there is a
//                    // frame with padding... there is no way to tell if the
//                    // frame and background together will draw all pixels.
//                    if (Config.LOGV)
//                        Log.v(TAG, "Padding: " + mFramePadding);
//                    opacity = PixelFormat.TRANSLUCENT;
//                }
//            }
//
//            if (Config.LOGV)
//                Log.v(TAG, "Background: " + bg + ", Frame: " + fg);
//            if (Config.LOGV)
//                Log.v(TAG, "Selected default opacity: " + opacity);
//
//            mDefaultOpacity = opacity;
//            if (mFeatureId < 0) {
//                setDefaultWindowFormat(opacity);
//            }
        }

	};

    protected boolean onKeyDown(int featureId, int keyCode, KeyEvent event) {
        final KeyEvent.DispatcherState dispatcher =
                mDecor != null ? mDecor.getKeyDispatcherState() : null;
        //Log.i(TAG, "Key down: repeat=" + event.getRepeatCount()
        //        + " flags=0x" + Integer.toHexString(event.getFlags()));
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
//                AudioManager audioManager = (AudioManager) getContext().getSystemService(
//                        Context.AUDIO_SERVICE);
//                if (audioManager != null) {
//                    /*
//                     * Adjust the volume in on key down since it is more
//                     * responsive to the user.
//                     */
//                    audioManager.adjustSuggestedStreamVolume(
//                            keyCode == KeyEvent.KEYCODE_VOLUME_UP
//                                    ? AudioManager.ADJUST_RAISE
//                                    : AudioManager.ADJUST_LOWER,
//                            mVolumeControlStreamType,
//                            AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_VIBRATE);
//                }
                return true;
            }


            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                /* Suppress PLAYPAUSE toggle when phone is ringing or in-call
                 * to avoid music playback */
//                if (mTelephonyManager == null) {
//                    mTelephonyManager = (TelephonyManager) getContext().getSystemService(
//                            Context.TELEPHONY_SERVICE);
//                }
//                if (mTelephonyManager != null &&
//                        mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
//                    return true;  // suppress key event
//                }
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
                getContext().sendOrderedBroadcast(intent, null);
                return true;
            }

            case KeyEvent.KEYCODE_CAMERA: {
//                if (getKeyguardManager().inKeyguardRestrictedInputMode()
//                        || dispatcher == null) {
//                    break;
//                }
//                if (event.getRepeatCount() == 0) {
//                    dispatcher.startTracking(event, this);
//                } else if (event.isLongPress() && dispatcher.isTracking(event)) {
//                    dispatcher.performedLongPress(event);
//                    mDecor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//                    sendCloseSystemWindows();
//                    // Broadcast an intent that the Camera button was longpressed
//                    Intent intent = new Intent(Intent.ACTION_CAMERA_BUTTON, null);
//                    intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
//                    getContext().sendOrderedBroadcast(intent, null);
//                }
                return true;
            }

            case KeyEvent.KEYCODE_MENU: {
                onKeyDownPanel((featureId < 0) ? FEATURE_OPTIONS_PANEL : featureId, event);
                return true;
            }

            case KeyEvent.KEYCODE_BACK: {
                if (event.getRepeatCount() > 0) break;
                if (featureId < 0) break;
                // Currently don't do anything with long press.
                dispatcher.startTracking(event, this);
                return true;
            }

            case KeyEvent.KEYCODE_CALL: {
//                if (getKeyguardManager().inKeyguardRestrictedInputMode()
//                        || dispatcher == null) {
//                    break;
//                }
//                if (event.getRepeatCount() == 0) {
//                    dispatcher.startTracking(event, this);
//                } else if (event.isLongPress() && dispatcher.isTracking(event)) {
//                    dispatcher.performedLongPress(event);
//                    mDecor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//                    // launch the VoiceDialer
//                    Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    try {
//                        sendCloseSystemWindows();
//                        getContext().startActivity(intent);
//                    } catch (ActivityNotFoundException e) {
//                        startCallActivity();
//                    }
//                }
                return true;
            }

            case KeyEvent.KEYCODE_SEARCH: {
//                if (getKeyguardManager().inKeyguardRestrictedInputMode()
//                        || dispatcher == null) {
//                    break;
//                }
//                if (event.getRepeatCount() == 0) {
//                    dispatcher.startTracking(event, this);
//                } else if (event.isLongPress() && dispatcher.isTracking(event)) {
//                    Configuration config = getContext().getResources().getConfiguration(); 
//                    if (config.keyboard == Configuration.KEYBOARD_NOKEYS
//                            || config.hardKeyboardHidden
//                                    == Configuration.HARDKEYBOARDHIDDEN_YES) {
//                        // launch the search activity
//                        Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        try {
//                            mDecor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//                            sendCloseSystemWindows();
//                            getSearchManager().stopSearch();
//                            getContext().startActivity(intent);
//                            // Only clear this if we successfully start the
//                            // activity; otherwise we will allow the normal short
//                            // press action to be performed.
//                            dispatcher.performedLongPress(event);
//                            return true;
//                        } catch (ActivityNotFoundException e) {
//                            // Ignore
//                        }
//                    }
//                }
//                break;
            }
        }

        return false;
    }

    protected boolean onKeyUp(int featureId, int keyCode, KeyEvent event) {
        final KeyEvent.DispatcherState dispatcher =
                mDecor != null ? mDecor.getKeyDispatcherState() : null;
        if (dispatcher != null) {
            dispatcher.handleUpEvent(event);
        }
        //Log.i(TAG, "Key up: repeat=" + event.getRepeatCount()
        //        + " flags=0x" + Integer.toHexString(event.getFlags()));
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
//                if (!event.isCanceled()) {
//                    AudioManager audioManager = (AudioManager) getContext().getSystemService(
//                            Context.AUDIO_SERVICE);
//                    if (audioManager != null) {
//                        /*
//                         * Play a sound. This is done on key up since we don't want the
//                         * sound to play when a user holds down volume down to mute.
//                         */
//                        audioManager.adjustSuggestedStreamVolume(
//                                AudioManager.ADJUST_SAME,
//                                mVolumeControlStreamType,
//                                AudioManager.FLAG_PLAY_SOUND);
//                        mVolumeKeyUpTime = SystemClock.uptimeMillis();
//                    }
//                }
                return true;
            }

            case KeyEvent.KEYCODE_MENU: {
                onKeyUpPanel(featureId < 0 ? FEATURE_OPTIONS_PANEL : featureId, event);
                return true;
            }

            case KeyEvent.KEYCODE_BACK: {
                if (featureId < 0) break;
                if (event.isTracking() && !event.isCanceled()) {
                    if (featureId == FEATURE_OPTIONS_PANEL) {
                        PanelFeatureState st = getPanelState(featureId, false);
                        if (st != null && st.isInExpandedMode) {
                            // If the user is in an expanded menu and hits back, it
                            // should go back to the icon menu
                            reopenMenu(true);
                            return true;
                        }
                    }
                    closePanel(featureId);
                    return true;
                }
                break;
            }

            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
                getContext().sendOrderedBroadcast(intent, null);
                return true;
            }

            case KeyEvent.KEYCODE_CAMERA: {
//                if (getKeyguardManager().inKeyguardRestrictedInputMode()) {
//                    break;
//                }
                if (event.isTracking() && !event.isCanceled()) {
                    // Add short press behavior here if desired
                }
                return true;
            }

            case KeyEvent.KEYCODE_CALL: {
//                if (getKeyguardManager().inKeyguardRestrictedInputMode()) {
//                    break;
//                }
//                if (event.isTracking() && !event.isCanceled()) {
//                    startCallActivity();
//                }
                return true;
            }

            case KeyEvent.KEYCODE_SEARCH: {
                /*
                 * Do this in onKeyUp since the Search key is also used for
                 * chording quick launch shortcuts.
                 */
//                if (getKeyguardManager().inKeyguardRestrictedInputMode()) {
//                    break;
//                }
//                if (event.isTracking() && !event.isCanceled()) {
//                    launchDefaultSearch();
//                }
                return true;
            }
        }

        return false;
    }

    private void onKeyUpPanel(int featureId, KeyEvent event) {
        // The panel key was released, so clear the chording key
        if (event.isCanceled()) {
            return;
        }

        // boolean playSoundEffect = false;
        PanelFeatureState st = getPanelState(featureId, true);
        if (st.isOpen || st.isHandled) {
            // Play the sound effect if the user closed an open menu (and not if
            // they just released a menu shortcut)
            // playSoundEffect = st.isOpen;

            // Close menu
            closePanel(st, true);

        } else if (st.isPrepared) {
            // Write 'menu opened' to event log
            // EventLog.writeEvent(50001, 0);

            // Show menu
            openPanel(st, event);

            // playSoundEffect = true;
        }

        // if (playSoundEffect) {
        // AudioManager audioManager = (AudioManager)
        // getContext().getSystemService(
        // Context.AUDIO_SERVICE);
        // if (audioManager != null) {
        // audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
        // } else {
        // Log.w(TAG, "Couldn't get audio manager");
        // }
        // }
    }

    public void closePanel(int featureId) {
        if (featureId == FEATURE_CONTEXT_MENU) {
            closeContextMenu();
        } else {
            closePanel(getPanelState(featureId, true), true);
        }
    }
    
    /**
     * Closes the context menu. This notifies the menu logic of the close, along
     * with dismissing it from the UI.
     */
    private synchronized void closeContextMenu() {
        if (mContextMenu != null) {
            mContextMenu.close();
            dismissContextMenu();
        }
    }
    
    
    /**
     * Dismisses just the context menu UI. To close the context menu, use
     * {@link #closeContextMenu()}.
     */
    private synchronized void dismissContextMenu() {
        mContextMenu = null;

        if (mContextMenuHelper != null) {
            mContextMenuHelper.dismiss();
            mContextMenuHelper = null;
        }
    }

    private void reopenMenu(boolean toggleMenuMode) {
        PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, true);

        // Save the future expanded mode state since closePanel will reset it
        boolean newExpandedMode = toggleMenuMode ? !st.isInExpandedMode : st.isInExpandedMode;

        st.refreshDecorView = true;
        closePanel(st, false);

        // Set the expanded mode state
        st.isInExpandedMode = newExpandedMode;

        openPanel(st, null);
    }

    public boolean superDispatchKeyEvent(KeyEvent event) {
        return mDecor.superDispatchKeyEvent(event);
    }

    public boolean superDispatchTouchEvent(MotionEvent event) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "Window superDispatchTouchEvent at time:" + start);
        return mDecor.superDispatchTouchEvent(event);
    }


	public Window(Context context) {
		mContext = context;
		mLayoutInflater = LayoutInflater.from(context);
		//		mRoot = new ViewRoot(context);
	}

	public View findViewByElementId(int viewElementId) {
		return getDecorView().findViewByElementId(viewElementId);
	}

    public void togglePanel(int featureId, KeyEvent event) {
        PanelFeatureState st = getPanelState(featureId, true);
        if (st.isOpen) {
            closePanel(st, true);
        } else {
            openPanel(st, event);
        }
        //togglePanel(getPanelState(featureId, true), event);
    }

//	private boolean mCreateMenu = true;

//	private void togglePanel(PanelFeatureState st, KeyEvent event) {
//		// Already open, close
//		if (st.isOpen) {
//			closePanel(st, true);
//			return;
//		}
//
//		Callback cb = getCallback();
//		if ((cb != null) && (!cb.onMenuOpened(st.featureId, st.menu))) {
//			// Callback doesn't want the menu to open, reset any state
//			closePanel(st, true);
//			return;
//		}
//
//		// Prepare panel (should have been done before, but just in case)
//		if (!preparePanel(st, event)) {
//			return;
//		}
//
//		if (st.decorView == null || st.refreshDecorView) {
//			if (st.decorView == null) {
//				// Initialize the panel decor, this will populate st.decorView
//				if (!initializePanelDecor(st) || (st.decorView == null))
//					return;
//			} else if (st.refreshDecorView
//					&& (st.decorView.getChildCount() > 0)) {
//				// Decor needs refreshing, so remove its views
//				st.decorView.removeAllViews();
//			}
//
//			// This will populate st.shownPanelView
//			if (!initializePanelContent(st) || (st.shownPanelView == null)) {
//				return;
//			}
//
//			ViewGroup.LayoutParams lp = st.shownPanelView.getLayoutParams();
//			if (lp == null) {
//				lp = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
//						LayoutParams.WRAP_CONTENT);
//			}
//
//			st.shownPanelView.setLayoutParams(lp);
//		}
//
//		if (mCreateMenu == false) {
//			st.isOpen = true;
//			st.decorView.setVisibility(View.VISIBLE);
//			st.shownPanelView.requestLayout();
//		} else {
//			st.isOpen = false;
//			st.decorView.setVisibility(View.GONE);
//			mCreateMenu = false;
//			st.isHandled = false;
//
//			WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
//					LayoutParams.WRAP_CONTENT,
//					LayoutParams.WRAP_CONTENT,
//					st.x,
//					st.y,
//					WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
//					WindowManager.LayoutParams.FLAG_DITHER
//							| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
//					st.decorView.mDefaultOpacity);
//
//			lp.gravity = st.gravity;
//			lp.windowAnimations = st.windowAnimations;
//
//			st.decorView.setLayoutParams(lp);
//		}
//	}

    public final void openPanel(int featureId, KeyEvent event) {
        openPanel(getPanelState(featureId, true), event);
    }
    
    private void openPanel(PanelFeatureState st, KeyEvent event) {
        // System.out.println("Open panel: isOpen=" + st.isOpen);
        // Already open, return
        if (st.isOpen) {
            return;
        }

        Callback cb = getCallback();
        if ((cb != null) && (!cb.onMenuOpened(st.featureId, st.menu))) {
            // Callback doesn't want the menu to open, reset any state
            closePanel(st, true);
            return;
        }

        final WindowManager wm = getWindowManager();
        if (wm == null) {
            return;
        }
        // Prepare panel (should have been done before, but just in case)
        if (!preparePanel(st, event)) {
            return;
        }

        if (st.decorView == null || st.refreshDecorView) {
            if (st.decorView == null) {
                // Initialize the panel decor, this will populate st.decorView
                if (!initializePanelDecor(st) || (st.decorView == null))
                    return;
            } else if (st.refreshDecorView && (st.decorView.getChildCount() > 0)) {
                // Decor needs refreshing, so remove its views
                st.decorView.removeAllViews();
            }
            // This will populate st.shownPanelView
            if (!initializePanelContent(st) || (st.shownPanelView == null)) {
                return;
            }
            ViewGroup.LayoutParams lp = st.shownPanelView.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            }

            int backgroundResId;
            if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                // If the contents is fill parent for the width, set the
                // corresponding background
                backgroundResId = st.fullBackground;
            } else {
                // Otherwise, set the normal panel background
                backgroundResId = st.background;
            }
            st.decorView.setWindowBackground(getContext().getResources().getDrawable(
                    backgroundResId));


            st.decorView.addView(st.shownPanelView, lp);

            /*
             * Give focus to the view, if it or one of its children does not
             * already have it.
             */
            if (!st.shownPanelView.hasFocus()) {
                st.shownPanelView.requestFocus();
            }
        }

        st.isOpen = true;
        st.isHandled = false;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT,
                st.x, st.y, WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
                WindowManager.LayoutParams.FLAG_DITHER
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                st.decorView.mDefaultOpacity);

        lp.gravity = st.gravity;
        lp.windowAnimations = st.windowAnimations;
        wm.addView(st.decorView, lp);
        // Log.v(TAG, "Adding main menu to window manager.");
    }

	private boolean initializePanelDecor(PanelFeatureState st) {
		st.decorView = new DecorView(getContext(), st.featureId);
		st.gravity = Gravity.CENTER | Gravity.BOTTOM;
		st.setStyle(getContext());

		mDecor.addView(st.decorView, true);

		return true;
	}

    private void closePanel(PanelFeatureState st, boolean doCallback) {
        final ViewManager wm = getWindowManager();
        if ((wm != null) && st.isOpen) {
            if (st.decorView != null) {
                st.decorView.removeView(st.shownPanelView);
                mDecor.removeView(st.decorView);
                wm.removeView(st.decorView);
                st.decorView = null;
            }

            if (doCallback) {
                callOnPanelClosed(st.featureId, st, null);
            }
        }

        st.isOpen = false;
        st.isPrepared = false;
        st.isHandled = false;
        // This view is no longer shown, so null it out
        st.shownPanelView = null;

        if (st.isInExpandedMode) {
            // Next time the menu opens, it should not be in expanded mode, so
            // force a refresh of the decor
            st.refreshDecorView = true;
            st.isInExpandedMode = false;
        }

        if (mPreparedPanel == st) {
            mPreparedPanel = null;
            //mPanelChordingKey = 0;
        }
     }

     /**
     * Helper method for calling the {@link Callback#onPanelClosed(int, MenuBuilder)}
     * callback. This method will grab whatever extra state is needed for the
     * callback that isn't given in the parameters. If the panel is not open,
     * this will not perform the callback.
     *
     * @param featureId Feature ID of the panel that was closed. Must be given.
     * @param panel Panel that was closed. Optional but useful if there is no
     *            menu given.
     * @param menu The menu that was closed. Optional, but give if you have.
     */
    private void callOnPanelClosed(int featureId, PanelFeatureState panel, MenuBuilder menu) {
        final Callback cb = getCallback();
        if (cb == null)
            return;

        // Try to get a menu
        if (menu == null) {
            // Need a panel to grab the menu, so try to get that
            if (panel == null) {
                if ((featureId >= 0) && (featureId < mPanels.length)) {
                    panel = mPanels[featureId];
                }
            }

            if (panel != null) {
                // menu still may be null, which is okay--we tried our best
                menu = panel.menu;
            }
        }

        // If the panel is not open, do not callback
        if ((panel != null) && (!panel.isOpen))
            return;

        cb.onPanelClosed(featureId, menu);
    }

	private void rSetViewGone(View v) {
		v.setVisibility(View.GONE);
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			View tmp = null;
			for (int i = 0; i < vg.getChildCount(); ++i) {
				tmp = vg.getChildAt(i);
				rSetViewGone(tmp);
			}
		}
	}

	public final void closeAllPanels() {
		final ViewManager wm = getWindowManager();
		if (wm == null) {
			return;
		}

		final PanelFeatureState[] panels = mPanels;
		final int N = panels != null ? panels.length : 0;
		for (int i = 0; i < N; i++) {
			final PanelFeatureState panel = panels[i];
			if (panel != null) {
				closePanel(panel, true);
			}
		}
	}

	private boolean initializePanelContent(PanelFeatureState st) {
		if (st.createdPanelView != null) {
			st.shownPanelView = st.createdPanelView;
			return true;
		}

		final MenuBuilder menu = (MenuBuilder) st.menu;
		if (menu == null) {
			return false;
		}

		st.shownPanelView = menu.getMenuView(
				(st.isInExpandedMode) ? MenuBuilder.TYPE_EXPANDED : MenuBuilder.TYPE_ICON,
				st.decorView);

		if (st.shownPanelView != null) {
			// Use the menu View's default animations if it has any
			final int defaultAnimations = ((MenuView) st.shownPanelView)
					.getWindowAnimations();
			if (defaultAnimations != 0) {
				st.windowAnimations = defaultAnimations;
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean preparePanel(PanelFeatureState st, KeyEvent event) {
		if (st.isPrepared)
			return true;

        if ((mPreparedPanel != null) && (mPreparedPanel != st)) {
            // Another Panel is prepared and possibly open, so close it
            closePanel(mPreparedPanel, false);
        }
		
		final Callback cb = getCallback();

		if (cb != null) {
			st.createdPanelView = cb.onCreatePanelView(st.featureId);
		}

		if (st.createdPanelView == null) {
			// Init the panel state's menu--return false if init failed
			if (st.menu == null) {
				if (!initializePanelMenu(st) || (st.menu == null)) {
					return false;
				}
				// Call callback, and return if it doesn't want to display menu
				if ((cb == null)
						|| !cb.onCreatePanelMenu(st.featureId, st.menu)) {
					// Ditch the menu created above
					st.menu = null;

					return false;
				}
			}

			// Callback and return if the callback does not want to show the
			// menu
			if (!cb.onPreparePanel(st.featureId, st.createdPanelView, st.menu)) {
				return false;
			}

            // Set the proper keymap
            st.qwertyMode = true;
            st.menu.setQwertyMode(st.qwertyMode);
		}

		// Set other state
		st.isPrepared = true;
		st.isHandled = false;
		mPreparedPanel = st;

		return true;
	}
	
	
	
	public boolean requestFeature(int featureId) {
        final int flag = 1<<featureId;
        mFeatures |= flag;
        mLocalFeatures |= mContainer != null ? (flag&~mContainer.mFeatures) : flag;
        return (mFeatures&flag) != 0;
    }
	
    /**
    * Initializes the menu associated with the given panel feature state. You
    * must at the very least set PanelFeatureState.menu to the Menu to be
    * associated with the given panel state. The default implementation creates
    * a new menu for the panel state.
    *
    * @param st The panel whose menu is being initialized.
    * @return Whether the initialization was successful.
    */
	private boolean initializePanelMenu(PanelFeatureState st) {
		final MenuBuilder menu = new MenuBuilder(getContext());

		menu.setCallback(this);
		st.setMenu(menu);

		return true;
	}

    public final void setBackgroundDrawable(Drawable drawable) {
        if (drawable != mBackgroundDrawable || mBackgroundResource != 0) {
            mBackgroundResource = 0;
            mBackgroundDrawable = drawable;
            if (mDecor != null) {
                mDecor.setWindowBackground(drawable);
            }
        }
    }

    public final void setFeatureDrawableResource(int featureId, int resId) {
        if (resId != 0) {
            DrawableFeatureState st = getDrawableState(featureId, true);
            if (st.resid != resId) {
                st.resid = resId;
                st.uri = null;
                st.local = getContext().getResources().getDrawable(resId);
                updateDrawable(featureId, st, false);
            }
        } else {
            setFeatureDrawable(featureId, null);
        }
    }

    public final void setFeatureDrawableUri(int featureId, Uri uri) {
        if (uri != null) {
            DrawableFeatureState st = getDrawableState(featureId, true);
            if (st.uri == null || !st.uri.equals(uri)) {
                st.resid = 0;
                st.uri = uri;
                st.local = loadImageURI(uri);
                updateDrawable(featureId, st, false);
            }
        } else {
            setFeatureDrawable(featureId, null);
        }
    }

    public final void setFeatureDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        st.resid = 0;
        st.uri = null;
        if (st.local != drawable) {
            st.local = drawable;
            updateDrawable(featureId, st, false);
        }
    }

    public void setFeatureDrawableAlpha(int featureId, int alpha) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        if (st.alpha != alpha) {
            st.alpha = alpha;
            updateDrawable(featureId, st, false);
        }
    }

    protected final void setFeatureDefaultDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        if (st.def != drawable) {
            st.def = drawable;
            updateDrawable(featureId, st, false);
        }
    }

    public final void setFeatureInt(int featureId, int value) {
        // XXX Should do more management (as with drawable features) to
        // deal with interactions between multiple window policies.
        updateInt(featureId, value, false);
    }

    public final void setChildDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        st.child = drawable;
        updateDrawable(featureId, st, false);
    }

    public final void setChildInt(int featureId, int value) {
        updateInt(featureId, value, false);
    }
    
    /**
     * Update the state of a drawable feature. This should be called, for every
     * drawable feature supported, as part of onActive(), to make sure that the
     * contents of a containing window is properly updated.
     *
     * @see #onActive
     * @param featureId The desired drawable feature to change.
     * @param fromActive Always true when called from onActive().
     */
    protected final void updateDrawable(int featureId, boolean fromActive) {
        final DrawableFeatureState st = getDrawableState(featureId, false);
        if (st != null) {
            updateDrawable(featureId, st, fromActive);
        }
    }
    
    /**
     * Called when a Drawable feature changes, for the window to update its
     * graphics.
     *
     * @param featureId The feature being changed.
     * @param drawable The new Drawable to show, or null if none.
     * @param alpha The new alpha blending of the Drawable.
     */
    protected void onDrawableChanged(int featureId, Drawable drawable, int alpha) {
        ImageView view;
        if (featureId == FEATURE_LEFT_ICON) {
            view = getLeftIconView();
        } else if (featureId == FEATURE_RIGHT_ICON) {
            view = getRightIconView();
        } else {
            return;
        }

        if (drawable != null) {
            drawable.setAlpha(alpha);
            view.setImageDrawable(drawable);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
    * The panel that is prepared or opened (the most recent one if there are
    * multiple panels). Shortcuts will go to this panel. It gets set in
    * {@link #preparePanel} and cleared in {@link #closePanel}.
    */
    private PanelFeatureState mPreparedPanel;

	private PanelFeatureState getPanelState(int featureId, boolean required) {
		return getPanelState(featureId, required, null);
	}

	/**
	 * Gets a panel's state based on its feature ID.
	 *
	 * @param featureId The feature ID of the panel.
	 * @param required Whether the panel is required (if it is required and it
	 *            isn't in our features, this throws an exception).
	 * @param convertPanelState Optional: If the panel state does not exist, use
	 *            this as the panel state.
	 * @return The panel state.
	 */
	private PanelFeatureState getPanelState(int featureId, boolean required,
			PanelFeatureState convertPanelState) {
        if ((getFeatures() & (1 << featureId)) == 0) {
            if (!required) {
                return null;
            }
            throw new RuntimeException("The feature has not been requested");
        }
		PanelFeatureState[] ar;
		if ((ar = mPanels) == null || ar.length <= featureId) {
			PanelFeatureState[] nar = new PanelFeatureState[featureId + 1];
			if (ar != null) {
				System.arraycopy(ar, 0, nar, 0, ar.length);
			}
			mPanels = ar = nar;
		}

		PanelFeatureState st = ar[featureId];
		if (st == null) {
			ar[featureId] = st = (convertPanelState != null) ? convertPanelState
					: new PanelFeatureState(featureId);
		}
		return st;
    }

    /**
     * Return the feature bits that are enabled.  This is the set of features
     * that were given to requestFeature(), and are being handled by this
     * Window itself or its container.  That is, it is the set of
     * requested features that you can actually use.
     *
     * <p>To do: add a public version of this API that allows you to check for
     * features by their feature ID.
     *
     * @return int The feature bits.
     */
    protected final int getFeatures()
    {
        return mFeatures;
    }
    
    /**
     * Return the feature bits that are being implemented by this Window.
     * This is the set of features that were given to requestFeature(), and are
     * being handled by only this Window itself, not by its containers.
     *
     * @return int The feature bits.
     */
    protected final int getLocalFeatures()
    {
        return mLocalFeatures;
    }
    /**
     * remove BackKey in LocalFeature.
     */
    public void removeBackKeyInLocalFeature() {
        int features = getLocalFeatures();
        if ((features & (1 << FEATURE_BACKKEY)) != 0) {
            this.mLocalFeatures =  features ^ (1 << FEATURE_BACKKEY);
        }
    }
    private void updateDrawable(int featureId, DrawableFeatureState st, boolean fromResume) {
        // Do nothing if the decor is not yet installed... an update will
        // need to be forced when we eventually become active.
        if (mContentParent == null) {
            return;
        }

        final int featureMask = 1 << featureId;

        if ((getFeatures() & featureMask) == 0 && !fromResume) {
            return;
        }

        Drawable drawable = null;
        if (st != null) {
            drawable = st.child;
            if (drawable == null)
                drawable = st.local;
            if (drawable == null)
                drawable = st.def;
        }
        if ((getLocalFeatures() & featureMask) == 0) {
            if (getContainer() != null) {
                if (isActive() || fromResume) {
                    getContainer().setChildDrawable(featureId, drawable);
                }
            }
        } else if (st != null && (st.cur != drawable || st.curAlpha != st.alpha)) {
            // System.out.println("Drawable changed: old=" + st.cur
            // + ", new=" + drawable);
            st.cur = drawable;
            st.curAlpha = st.alpha;
            onDrawableChanged(featureId, drawable, st.alpha);
        }
    }
    
    private ImageView getLeftIconView() {
        if (mLeftIconView != null) {
            return mLeftIconView;
        }
        if (mContentParent == null) {
            installDecor();
        }
        return (mLeftIconView = (ImageView)findViewById(com.android.internal.R.id.left_icon));
    }

    private TextView getTitleView() {
        if (mTitleView != null) {
            return mTitleView;
        }
        return (mTitleView = (TextView) findViewById(com.android.internal.R.id.title));
    }

    private ImageView getRightIconView() {
        if (mRightIconView != null) {
            return mRightIconView;
        }
        if (mContentParent == null) {
            installDecor();
        }
        return (mRightIconView = (ImageView)findViewById(com.android.internal.R.id.right_icon));
    }
    
    private static final class DrawableFeatureState {
        DrawableFeatureState(int _featureId) {
            featureId = _featureId;
        }

        final int featureId;

        int resid;

        Uri uri;

        Drawable local;

        Drawable child;

        Drawable def;

        Drawable cur;

        int alpha = 255;

        int curAlpha = 255;
    }
    
	private static final class PanelFeatureState {
		/** Feature ID for this panel. */
		int featureId;

		// Information pulled from the style for this panel.

		@SuppressWarnings("unused")
		int background;

		/** The background when the panel spans the entire available width. */
		@SuppressWarnings("unused")
		int fullBackground;

		int gravity;

		int x;

		int y;

		int windowAnimations;

		/** Dynamic state of the panel. */
		DecorView decorView;

		/** The panel that was returned by onCreatePanelView(). */
		View createdPanelView;

		/** The panel that we are actually showing. */
		View shownPanelView;

		/** Use {@link #setMenu} to set this. */
		MenuBuilder menu;

		/**
		 * Whether the panel has been prepared (see
		 * {@link PhoneWindow#preparePanel}).
		 */
		boolean isPrepared;

		/**
		 * Whether an item's action has been performed. This happens in obvious
		 * scenarios (user clicks on menu item), but can also happen with
		 * chording menu+(shortcut key).
		 */
		@SuppressWarnings("unused")
		boolean isHandled;

		boolean isOpen;

		/**
		 * True if the menu is in expanded mode, false if the menu is in icon
		 * mode
		 */
		boolean isInExpandedMode;

		@SuppressWarnings("unused")
		public boolean qwertyMode;

		boolean refreshDecorView;

		@SuppressWarnings("unused")
		boolean wasLastOpen;

		@SuppressWarnings("unused")
		boolean wasLastExpanded;
        
        /**
         * Contains the state of the menu when told to freeze.
         */
        Bundle frozenMenuState;

		PanelFeatureState(int featureId) {
			this.featureId = featureId;

			refreshDecorView = false;
		}

		void setStyle(Context context) {
			TypedArray a = context
					.obtainStyledAttributes(com.android.internal.R.styleable.Theme);
			background = a.getResourceId(
					com.android.internal.R.styleable.Theme_panelBackground, 0);
			fullBackground = a.getResourceId(
					com.android.internal.R.styleable.Theme_panelFullBackground,
					0);
			windowAnimations = a
					.getResourceId(
							com.android.internal.R.styleable.Theme_windowAnimationStyle,
							0);
			a.recycle();
		}

        void setMenu(MenuBuilder menu) {
            this.menu = menu;

            if (frozenMenuState != null) {
                ((MenuBuilder) menu).restoreHierarchyState(frozenMenuState);
                frozenMenuState = null;
            }
        }

        Parcelable onSaveInstanceState() {
            SavedState savedState = new SavedState();
            savedState.featureId = featureId;
            savedState.isOpen = isOpen;
            savedState.isInExpandedMode = isInExpandedMode;

            if (menu != null) {
                savedState.menuState = new Bundle();
                ((MenuBuilder) menu).saveHierarchyState(savedState.menuState);
            }
            return savedState;
        }
    
        void onRestoreInstanceState(Parcelable state) {
            SavedState savedState = (SavedState) state;
            featureId = savedState.featureId;
            wasLastOpen = savedState.isOpen;
            wasLastExpanded = savedState.isInExpandedMode;
            frozenMenuState = savedState.menuState;

            /*
             * A LocalActivityManager keeps the same instance of this class around.
             * The first time the menu is being shown after restoring, the
             * Activity.onCreateOptionsMenu should be called. But, if it is the
             * same instance then menu != null and we won't call that method.
             * So, clear this.  Also clear any cached views.
             */
            menu = null;
            createdPanelView = null;
            shownPanelView = null;
            decorView = null;
        }

        private static class SavedState implements Parcelable {
            int featureId;
            boolean isOpen;
            boolean isInExpandedMode;
            Bundle menuState;

            public int describeContents() {
                return 0;
            }

            public void writeToParcel(Parcel dest, int flags) {
                dest.writeInt(featureId);
                dest.writeInt(isOpen ? 1 : 0);
                dest.writeInt(isInExpandedMode ? 1 : 0);

                if (isOpen) {
                    dest.writeBundle(menuState);
                }
            }

            private static SavedState readFromParcel(Parcel source) {
                SavedState savedState = new SavedState();
                savedState.featureId = source.readInt();
                savedState.isOpen = source.readInt() == 1;
                savedState.isInExpandedMode = source.readInt() == 1;

                if (savedState.isOpen) {
                    savedState.menuState = source.readBundle();
                }

                return savedState;
            }

            public static final Parcelable.Creator<SavedState> CREATOR
                    = new Parcelable.Creator<SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return readFromParcel(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }
    }

	/**/
	@Override
	public boolean onMenuItemSelected(MenuBuilder menu, MenuItemImpl item) {
		final Callback cb = getCallback();
		if (cb != null) {
			final PanelFeatureState panel = findMenuPanel(menu.getRootMenu());
			if (panel != null) {
				boolean retVal = cb.onMenuItemSelected(panel.featureId, item);
				this.closePanel(getPanelState(panel.featureId, true), true);
				return retVal;
			}
		}
		return false;
	}

	public PanelFeatureState findMenuPanel(MenuBuilder menu) {
		final PanelFeatureState[] panels = mPanels;
		final int N = panels != null ? panels.length : 0;
		for (int i = 0; i < N; i++) {
			final PanelFeatureState panel = panels[i];
			if (panel != null && panel.menu == menu) {
				return panel;
			}
		}
		return null;
	}

	@Override
	public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
		final PanelFeatureState panel = findMenuPanel(menu);
		if (panel != null) {
			closePanel(panel, allMenusAreClosing);
		}
	}

    private void updateInt(int featureId, int value, boolean fromResume) {

        // Do nothing if the decor is not yet installed... an update will
        // need to be forced when we eventually become active.
        if (mContentParent == null) {
            return;
        }

        final int featureMask = 1 << featureId;

        if ((getFeatures() & featureMask) == 0 && !fromResume) {
            return;
        }

        if ((getLocalFeatures() & featureMask) == 0) {
            if (getContainer() != null) {
                getContainer().setChildInt(featureId, value);
            }
        } else {
            onIntChanged(featureId, value);
        }
    }

    /**
     * Called when an int feature changes, for the window to update its
     * graphics.
     *
     * @param featureId The feature being changed.
     * @param value The new integer value.
     */
    protected void onIntChanged(int featureId, int value) {
        if (featureId == FEATURE_PROGRESS || featureId == FEATURE_INDETERMINATE_PROGRESS) {
            updateProgressBars(value);
        } else if (featureId == FEATURE_CUSTOM_TITLE) {
            FrameLayout titleContainer = (FrameLayout) findViewById(com.android.internal.R.id.title_container);
            if (titleContainer != null) {
                mLayoutInflater.inflate(value, titleContainer);
            }
        }
    }

    /**
     * Updates the progress bars that are shown in the title bar.
     *
     * @param value Can be one of {@link Window#PROGRESS_VISIBILITY_ON},
     *            {@link Window#PROGRESS_VISIBILITY_OFF},
     *            {@link Window#PROGRESS_INDETERMINATE_ON},
     *            {@link Window#PROGRESS_INDETERMINATE_OFF}, or a value
     *            starting at {@link Window#PROGRESS_START} through
     *            {@link Window#PROGRESS_END} for setting the default
     *            progress (if {@link Window#PROGRESS_END} is given,
     *            the progress bar widgets in the title will be hidden after an
     *            animation), a value between
     *            {@link Window#PROGRESS_SECONDARY_START} -
     *            {@link Window#PROGRESS_SECONDARY_END} for the
     *            secondary progress (if
     *            {@link Window#PROGRESS_SECONDARY_END} is given, the
     *            progress bar widgets will still be shown with the secondary
     *            progress bar will be completely filled in.)
     */
    private void updateProgressBars(int value) {
        ProgressBar circularProgressBar = getCircularProgressBar(true);
        ProgressBar horizontalProgressBar = getHorizontalProgressBar(true);

        final int features = getLocalFeatures();
        if (value == PROGRESS_VISIBILITY_ON) {
            if ((features & (1 << FEATURE_PROGRESS)) != 0) {
                int level = horizontalProgressBar.getProgress();
                int visibility = (horizontalProgressBar.isIndeterminate() || level < 10000) ?
                        View.VISIBLE : View.INVISIBLE;
                horizontalProgressBar.setVisibility(visibility);
            }
            if ((features & (1 << FEATURE_INDETERMINATE_PROGRESS)) != 0) {
                circularProgressBar.setVisibility(View.VISIBLE);
            }
        } else if (value == PROGRESS_VISIBILITY_OFF) {
            if ((features & (1 << FEATURE_PROGRESS)) != 0) {
                horizontalProgressBar.setVisibility(View.GONE);
            }
            if ((features & (1 << FEATURE_INDETERMINATE_PROGRESS)) != 0) {
                circularProgressBar.setVisibility(View.GONE);
            }
        } else if (value == PROGRESS_INDETERMINATE_ON) {
            horizontalProgressBar.setIndeterminate(true);
        } else if (value == PROGRESS_INDETERMINATE_OFF) {
            horizontalProgressBar.setIndeterminate(false);
        } else if (PROGRESS_START <= value && value <= PROGRESS_END) {
            // We want to set the progress value before testing for visibility
            // so that when the progress bar becomes visible again, it has the
            // correct level.
            horizontalProgressBar.setProgress(value - PROGRESS_START);

            if (value < PROGRESS_END) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
            }
        } else if (PROGRESS_SECONDARY_START <= value && value <= PROGRESS_SECONDARY_END) {
            horizontalProgressBar.setSecondaryProgress(value - PROGRESS_SECONDARY_START);

            showProgressBars(horizontalProgressBar, circularProgressBar);
        }

    }

    private ProgressBar getCircularProgressBar(boolean shouldInstallDecor) {
        if (mCircularProgressBar != null) {
            return mCircularProgressBar;
        }
        if (mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        mCircularProgressBar = (ProgressBar)findViewById(com.android.internal.R.id.progress_circular);
        mCircularProgressBar.setVisibility(View.INVISIBLE);
        return mCircularProgressBar;
    }

    private ProgressBar getHorizontalProgressBar(boolean shouldInstallDecor) {
        if (mHorizontalProgressBar != null) {
            return mHorizontalProgressBar;
        }
        if (mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        mHorizontalProgressBar = (ProgressBar)findViewById(com.android.internal.R.id.progress_horizontal);
        mHorizontalProgressBar.setVisibility(View.INVISIBLE);
        return mHorizontalProgressBar;
    }

    private void showProgressBars(ProgressBar horizontalProgressBar, ProgressBar spinnyProgressBar) {
        final int features = getLocalFeatures();
        if ((features & (1 << FEATURE_INDETERMINATE_PROGRESS)) != 0 &&
                spinnyProgressBar.getVisibility() == View.INVISIBLE) {
            spinnyProgressBar.setVisibility(View.VISIBLE);
        }
        // Only show the progress bars if the primary progress is not complete
        if ((features & (1 << FEATURE_PROGRESS)) != 0 &&
                horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBars(ProgressBar horizontalProgressBar, ProgressBar spinnyProgressBar) {
        final int features = getLocalFeatures();
        Animation anim = AnimationUtils.loadAnimation(getContext(), com.android.internal.R.anim.fade_out);
        anim.setDuration(1000);
        if ((features & (1 << FEATURE_INDETERMINATE_PROGRESS)) != 0 &&
                spinnyProgressBar.getVisibility() == View.VISIBLE) {
            spinnyProgressBar.startAnimation(anim);
            spinnyProgressBar.setVisibility(View.INVISIBLE);
        }
        if ((features & (1 << FEATURE_PROGRESS)) != 0 &&
                horizontalProgressBar.getVisibility() == View.VISIBLE) {
            horizontalProgressBar.startAnimation(anim);
            horizontalProgressBar.setVisibility(View.INVISIBLE);
        }
    }
	/**/

	private IBinder mAppToken;
	private String mAppName;
	private WindowManager mWindowManager;

	private Window mContainer;
	private boolean mHasChildren;

    /**
     * Set the container for this window.  If not set, the DecorWindow
     * operates as a top-level window; otherwise, it negotiates with the
     * container to display itself appropriately.
     *
     * @param container The desired containing Window.
     */
	public void setContainer(Window container) {
		mContainer = container;
		if (container != null) {
			// Embedded screens never have a title.
			mFeatures |= 1 << FEATURE_NO_TITLE;
			mLocalFeatures |= 1 << FEATURE_NO_TITLE;
			container.mHasChildren = true;
		}
	}
	
    /**
     * Return the container for this Window.
     *
     * @return Window The containing window, or null if this is a
     *         top-level window.
     */
    public final Window getContainer() {
        return mContainer;
    }

	public WindowManager getWindowManager() {
		return mWindowManager;
	}

	public final View peekDecorView() {
		return mDecor;
	}

	/**
	 * Set the window manager for use by this Window to, for example,
	 * display panels.  This is <em>not</em> used for displaying the
	 * Window itself -- that must be done by the client.
	 *
	 * @param wm The ViewManager for adding new windows.
	 */
	public void setWindowManager(WindowManager wm, IBinder appToken,
			String appName) {
		mAppToken = appToken;
		mAppName = appName;
		if (wm == null) {
			wm = WindowManagerImpl.getDefault();
		}
		mWindowManager = new LocalWindowManager(wm);
	}

	private class LocalWindowManager extends WindowManager {
		LocalWindowManager(WindowManager wm) {
			mWindowManager = wm;
		}

        public final void addView(View view, ViewGroup.LayoutParams params) {
            // Let this throw an exception on a bad params.
            WindowManager.LayoutParams wp = (WindowManager.LayoutParams) params;
            ((WindowManagerImpl) mWindowManager).addView(view, params);
        }

		public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
			mWindowManager.updateViewLayout(view, params);
		}

		public final void removeView(View view) {
			mWindowManager.removeView(view);
		}

		public Display getDefaultDisplay() {
			return mWindowManager.getDefaultDisplay();
		}

		public void removeViewImmediate(View view) {
			mWindowManager.removeViewImmediate(view);
		}

		private final WindowManager mWindowManager;
	}

    /**
     * Set the width and height layout parameters of the window. The default for
     * both of these is MATCH_PARENT; you can change them to WRAP_CONTENT to
     * make a window that is not full-screen.
     * 
     * @param width The desired layout width of the window.
     * @param height The desired layout height of the window.
     */
    public void setLayout(int width, int height)
    {
        final WindowManager.LayoutParams attrs = getAttributes();
        attrs.width = width;
        attrs.height = height;
        if (mCallback != null) {
            mCallback.onWindowAttributesChanged(attrs);
        }
    }

    public void setGravity(int gravity)
    {
        final WindowManager.LayoutParams attrs = getAttributes();
        attrs.gravity = gravity;
        if (mCallback != null) {
            mCallback.onWindowAttributesChanged(attrs);
        }
    }

    private int mForcedWindowFlags = 0;

    public void setFlags(int flags, int mask) {
        final WindowManager.LayoutParams attrs = getAttributes();
        attrs.flags = (attrs.flags & ~mask) | (flags & mask);
        mForcedWindowFlags |= mask;
        if (mCallback != null) {
            mCallback.onWindowAttributesChanged(attrs);
        }
    }

    public void clearFlags(int flags) {
        setFlags(0, flags);
    }

    /**
     * Specify custom window attributes. <strong>PLEASE NOTE:</strong> the
     * layout params you give here should generally be from values previously
     * retrieved with {@link #getAttributes()}; you probably do not want to
     * blindly create and apply your own, since this will blow away any values
     * set by the framework that you are not interested in.
     * 
     * @param a The new window attributes, which will completely override any
     *            current values.
     */
    public void setAttributes(WindowManager.LayoutParams a) {
        mWindowAttributes.copyFrom(a);
        if (mCallback != null) {
            mCallback.onWindowAttributesChanged(mWindowAttributes);
        }
    }

    /**
     * Retrieve the current window attributes associated with this panel.
     *
     * @return WindowManager.LayoutParams Either the existing window
     *         attributes object, or a freshly created one if there is none.
     */
    public final WindowManager.LayoutParams getAttributes() {
        return mWindowAttributes;
    }
    /**
     * Called when the panel key is pushed down.
     * @param featureId The feature ID of the relevant panel (defaults to FEATURE_OPTIONS_PANEL}.
     * @param event The key event.
     * @return Whether the key was handled.
     */
    public final boolean onKeyDownPanel(int featureId, KeyEvent event) {
        final int keyCode = event.getKeyCode();
        
        if (event.getRepeatCount() == 0) {
            // The panel key was pushed, so set the chording key
//            mPanelChordingKey = keyCode;
//            mPanelMayLongPress = false;
            
            PanelFeatureState st = getPanelState(featureId, true);
            if (!st.isOpen) {
//                if (getContext().getResources().getConfiguration().keyboard
//                        == Configuration.KEYBOARD_NOKEYS) {
//                    mPanelMayLongPress = true;
//                }
                return preparePanel(st, event);
            }
            
        } else if ((event.getFlags()&KeyEvent.FLAG_LONG_PRESS) != 0) {
            // We have had a long press while in a state where this
            // should be executed...  do it!
//            mPanelChordingKey = 0;
//            mPanelMayLongPress = false;
//            InputMethodManager imm = (InputMethodManager)
//                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//            if (imm != null) {
//                mDecor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
//            }
            
        }

        return false;
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
        if ((st != null) && (st.menu != null)) {
            final MenuBuilder menuBuilder = (MenuBuilder) st.menu;

            if (st.isOpen) {
                // Freeze state
                final Bundle state = new Bundle();
                menuBuilder.saveHierarchyState(state);

                // Remove the menu views since they need to be recreated
                // according to the new configuration
                clearMenuViews(st);

                // Re-open the same menu
                reopenMenu(false);

                // Restore state
                menuBuilder.restoreHierarchyState(state);

            } else {
                // Clear menu views so on next menu opening, it will use
                // the proper layout
                clearMenuViews(st);
            }
        }

    }
    
    private static void clearMenuViews(PanelFeatureState st) {

        // This can be called on config changes, so we should make sure
        // the views will be reconstructed based on the new orientation, etc.

        // Allow the callback to create a new panel view
        st.createdPanelView = null;

        // Causes the decor view to be recreated
        st.refreshDecorView = true;

        ((MenuBuilder) st.menu).clearMenuViews();
    }
    
    public boolean onSubMenuSelected(final SubMenuBuilder subMenu) {
        if (!subMenu.hasVisibleItems()) {
            return true;
        }

        // The window manager will give us a valid window token
        new MenuDialogHelper(subMenu).show(null);

        return true;
    }

    public void onCloseSubMenu(SubMenuBuilder subMenu) {
        final MenuBuilder parentMenu = subMenu.getRootMenu();
        final PanelFeatureState panel = findMenuPanel(parentMenu);

        // Callback
        if (panel != null) {
            callOnPanelClosed(panel.featureId, panel, parentMenu);
            closePanel(panel, true);
        }
        
    }

    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(true);
    }
    
    
    /**
     * Simple implementation of MenuBuilder.Callback that:
     * <li> Opens a submenu when selected.
     * <li> Calls back to the callback's onMenuItemSelected when an item is
     * selected.
     */
    private final class ContextMenuCallback implements MenuBuilder.Callback {
        private int mFeatureId;
        private MenuDialogHelper mSubMenuHelper;

        public ContextMenuCallback(int featureId) {
            mFeatureId = featureId;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            if (allMenusAreClosing) {
                Callback callback = getCallback();
                if (callback != null) callback.onPanelClosed(mFeatureId, menu);

                if (menu == mContextMenu) {
                    dismissContextMenu();
                }

                // Dismiss the submenu, if it is showing
                if (mSubMenuHelper != null) {
                    mSubMenuHelper.dismiss();
                    mSubMenuHelper = null;
                }
            }
        }

        public void onCloseSubMenu(SubMenuBuilder menu) {
            Callback callback = getCallback();
            if (callback != null) callback.onPanelClosed(mFeatureId, menu.getRootMenu());
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItemImpl item) {
            Callback callback = getCallback();
            return (callback != null) && callback.onMenuItemSelected(mFeatureId, item);
        }

        public void onMenuModeChange(MenuBuilder menu) {
        }

        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            // Set a simple callback for the submenu
            subMenu.setCallback(this);

            // The window manager will give us a valid window token
            mSubMenuHelper = new MenuDialogHelper(subMenu);
            mSubMenuHelper.show(null);

            return true;
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: superDispatchTrackballEvent");
     */
    @MayloonStubAnnotation()
    public boolean superDispatchTrackballEvent(MotionEvent event) {
        System.out.println("Stub" + " Function : superDispatchTrackballEvent");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: takeInputQueue");
     */
    @MayloonStubAnnotation()
    public void takeInputQueue(InputQueue.Callback callback) {
        System.out.println("Stub" + " Function : takeInputQueue");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: makeActive");
     */
    @MayloonStubAnnotation()
    public final void makeActive() {
        System.out.println("Stub" + " Function : makeActive");
        return;
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
     * console.log("Missing method: onActive");
     */
    @MayloonStubAnnotation()
    protected void onActive() {
        System.out.println("Stub" + " Function : onActive");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setType");
     */
    @MayloonStubAnnotation()
    public void setType(int type) {
        System.out.println("Stub" + " Function : setType");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setBackgroundDrawableResource");
     */
    @MayloonStubAnnotation()
    public void setBackgroundDrawableResource(int resid) {
        System.out.println("Stub" + " Function : setBackgroundDrawableResource");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: hasSoftInputMode");
     */
    @MayloonStubAnnotation()
    protected final boolean hasSoftInputMode() {
        System.out.println("Stub" + " Function : hasSoftInputMode");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setDefaultWindowFormat");
     */
    @MayloonStubAnnotation()
    protected void setDefaultWindowFormat(int format) {
        System.out.println("Stub" + " Function : setDefaultWindowFormat");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isFloating");
     */
    @MayloonStubAnnotation()
    public boolean isFloating() {
        System.out.println("Stub" + " Function : isFloating");
        return true;
    }

    public void addFlags(int flags) {
        setFlags(flags, flags);
    }

    /**
     * @j2sNative
     * console.log("Missing method: setSoftInputMode");
     */
    @MayloonStubAnnotation()
    public void setSoftInputMode(int mode) {
        System.out.println("Stub" + " Function : setSoftInputMode");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getVolumeControlStream");
     */
    @MayloonStubAnnotation()
    public int getVolumeControlStream() {
        System.out.println("Stub" + " Function : getVolumeControlStream");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setFormat");
     */
    @MayloonStubAnnotation()
    public void setFormat(int format) {
        System.out.println("Stub" + " Function : setFormat");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setVolumeControlStream");
     */
    @MayloonStubAnnotation()
    public void setVolumeControlStream(int streamType) {
        System.out.println("Stub" + " Function : setVolumeControlStream");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setWindowAnimations");
     */
    @MayloonStubAnnotation()
    public void setWindowAnimations(int resId) {
        System.out.println("Stub" + " Function : setWindowAnimations");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: hasChildren");
     */
    @MayloonStubAnnotation()
    public final boolean hasChildren() {
        System.out.println("Stub" + " Function : hasChildren");
        return true;
    }

    public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
        return performPanelShortcut(getPanelState(featureId, true), keyCode, event, flags);
    }

    private boolean performPanelShortcut(PanelFeatureState st, int keyCode, KeyEvent event,
            int flags) {

        boolean handled = false;

        // Only try to perform menu shortcuts if preparePanel returned true (possible false
        // return value from application not wanting to show the menu).
        if ((st.isPrepared || preparePanel(st, event)) && st.menu != null) {
            // The menu is prepared now, perform the shortcut on it
            handled = st.menu.performShortcut(keyCode, event, flags);
        }

        if (handled) {
            // Mark as handled
            st.isHandled = true;

            if ((flags & Menu.FLAG_PERFORM_NO_CLOSE) == 0) {
                closePanel(st, true);
            }
        }

        return handled;
    }

    public boolean getBeContentOfTab() {
        return this.beContentOfTab;
    }

    public void setBeContentOfTab(boolean beContentOfTab) {
        this.beContentOfTab = beContentOfTab;
    }
}
