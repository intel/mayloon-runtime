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

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
import static android.view.WindowManager.LayoutParams.TYPE_INPUT_METHOD;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View.MeasureSpec;
import android.view.Window.DecorView;

/**
 * The top of a view hierarchy, implementing the needed protocol between View
 * and the WindowManager. This is for the most part an internal implementation
 * detail of {@link Window}.
 *
 */
public final class ViewRoot extends Handler implements ViewParent, View.AttachInfo.Callbacks {
	private static final boolean DBG = false;

	private static final String TAG = "ViewRoot";

	static long sInstanceCount = 0;

	static boolean mInitialized = false;
	static RunQueue sRunQueues = new RunQueue();
    static WindowSession sWindowSession;
	final int[] mTmpLocation = new int[2];

	final SparseArray<Object> mPendingEvents = new SparseArray<Object>();
	int mPendingEventSeq = 0;
	private static long sDrawTime;
	private static boolean DBG_FPS = false;
	final WindowManager.LayoutParams mWindowAttributes = new WindowManager.LayoutParams();
    final W mWindow;
	View mView;
	View mFocusedView;
	View mRealFocusedView; // this is not set to null in touch mode
	int mViewVisibility;
	boolean mAppVisible = true;

	boolean mIsCreating;
	boolean mDrawingAllowed;

	int mWidth;
	int mHeight;

	boolean mIsAnimating;

	final View.AttachInfo mAttachInfo;
	InputChannel mInputChannel;
	InputQueue.Callback mInputQueueCallback;
	InputQueue mInputQueue;

	boolean mTraversalScheduled;
	boolean mWillDrawSoon;
	boolean mLayoutRequested;
	boolean mFirst;
	boolean mReportNextDraw;
	boolean mFullRedrawNeeded;
	boolean mNewSurfaceNeeded;
	boolean mHasHadWindowFocus;
	boolean mLastWasImTarget;

	boolean mWindowAttributesChanged = false;

	boolean mAdded;
	boolean mAddedTouchMode;

	Rect mWinFrame; // frame given by window manager.

	/* package */int mAddNesting;

	boolean mScrollMayChange;
	int mSoftInputMode;
	View mLastScrolledFocus;
	int mScrollY;
	int mCurScrollY;
	boolean mUseGL;
	boolean mGlWanted;
	private boolean mAttached = false;
	final ViewConfiguration mViewConfiguration;

	String mViewRootID;
    public static int mDialogCount = 0;

	public String getViewRootID() {
		return this.mViewRootID;
	}

	private String getCanvasSuffix() {
		return "_AppCanvas";
	}

	public String getCanvasId() {
		return getViewRootID() + getCanvasSuffix();
	}

    Rect mTempRect; // used in the transaction to not thrash the heap.
    Rect mVisRect; // used to retrieve visible rect of focused view.
	private static int viewRootID = 0;

    private static int getAViewRootID() {
        return viewRootID++;
    }

	private static ArrayList viewRootList = new ArrayList();

	private final Canvas mCanvas;

	private Rect mDirty;

    public static WindowSession getWindowSession() {
            if (sWindowSession == null) {
                sWindowSession = new WindowSession();
            }
            return sWindowSession;
    }

	public ViewRoot(Context context) {
		mWidth = -1;
		mHeight = -1;
		mFirst = true; // true for the first time the view is added
		mAdded = false;
		int w = 480, h = 800;
		/**
		 * @j2sNative
		 *
		 * w = window.innerWidth;
		 * h = window.innerHeight;
		 */{}
		mWinFrame = new Rect(0, 0, w, h);
        getWindowSession();
        mWindow = new W(this, context);
        mAttachInfo = new View.AttachInfo(sWindowSession, mWindow, this, this);
		mViewConfiguration = ViewConfiguration.get(context);
		mDirty = new Rect();
		mViewRootID = "ViewRoot_" + ViewRoot.getAViewRootID();
        mTempRect = new Rect();
        mVisRect = new Rect();
		/**
		 @j2sNative
		 var rootView = document.getElementById(this.mViewRootID);
		 if (rootView != null)
		     console.log("rootView != null ??!! rootViewID: " + this.mViewRootID);

		 rootView = document.createElement("span");
		 rootView.style.position = "absolute";
		 rootView.style.display = "block";
		 rootView.style.visibility = "hidden";
		 rootView.id = this.mViewRootID;
		 rootView.style.left = "0px";
		 rootView.style.top = "0px";
		 rootView.tabIndex = "-1";
//		 rootView.style["background-color"] = "silver";
		 document.body.appendChild(rootView);
		 // Canvas to draw on
		 var canvas = document.createElement("canvas");
		 canvas.id = this.mViewRootID + this.getCanvasSuffix();
		 canvas.style.position = "absolute";
		 canvas.getContext("2d").globalCompositeOperation = 'destination-atop';
		 canvas.getContext("2d").fillStyle = 'rgba(255,255,255,1)';
		 rootView.appendChild(canvas);
		 */{}
        attachHandlerToView(this.mViewRootID);
        mCanvas = new Canvas(this.getCanvasId());
        viewRootList.add(this);
        /**
         * @j2sNative
         * // We'd like to disable the html scroll because MayLoon has its own scrollbars
         * document.body.scroll = "no";
         * document.body.style.overflow = 'hidden';
         * document.height = window.innerHeight;
         * // Disable text selection in web page
         * document.onselectstart = function () {return false};
         */{}
    }

	public void attachHandlerToView(String uid) {
		/**
		 @j2sNative
		var elem = document.getElementById(uid);
		if (elem == null) return;
		*/
		{
		}

		ViewRoot viewRoot = this;
		Log.d(TAG, "Attaching handlers");
		for (int i = 0; i < HTML5Event.eventType.length; i++) {
			String eventTypeName = HTML5Event.eventType[i];
			/**
			 @j2sNative
			 elem.addEventListener(eventTypeName, function(event){viewRoot.eventForwarder(event);}, true);
			*/
			{
			}

		}
	}

    /**
     * simulated KeyDownEvent (keyCode = KEYCODE_BACK , eventType = keydown )
     * In Activity's global ReturnButton and click it to simulated KEYCODE_BACK KEY
     */
    public static void simulateBack() {
        KeyEvent keyEvent = HTML5Event.toKeyEvent("keydown", KeyEvent.KEYCODE_BACK);
        ViewRoot viewRoot = (ViewRoot) viewRootList.get(viewRootList.size() - 1);
        viewRoot.deliverKeyEventToViewHierarchy(keyEvent, false);
    }

    public static void simulateMenu() {
        KeyEvent keyEvent = HTML5Event.toKeyEvent("keydown", KeyEvent.KEYCODE_MENU);
        ViewRoot viewRoot = (ViewRoot) viewRootList.get(viewRootList.size() - 1);
        viewRoot.deliverKeyEventToViewHierarchy(keyEvent, false);
        keyEvent = HTML5Event.toKeyEvent("keyup", KeyEvent.KEYCODE_MENU);
        viewRoot.deliverKeyEventToViewHierarchy(keyEvent, false);
    }

	public static long getInstanceCount() {
		return sInstanceCount;
	}

    /**
     * Each ViewRoot has three children: one background canvas, one foreground
     * canvas and a DecorView object which is the real container for the view
     * hierachey. The problem is, according to HTML5 event model, all event will
     * only be dispatched to the TopMostElement, which is the foreground canvas
     * in our case, but we really want the DecorView to handle the event
     * (globalEventHandler), so we need to forward the event to the DecorView.
     * The solution is basically borrowed from
     * http://www.vinylfox.com/forwarding-mouse-events-through-layers/
     */
    public void eventForwarder(Object e) {
        String eventType = null;
        String divID = "";

        /**
         * @j2sNative 
         * spanID = e.target.id; 
         * eventType = e.type; 
         * var span = document.getElementById(spanID); 
         * if (span == null) {
         *     return; 
         * } 
         * //span.hidden = "hidden";
         * if (android.view.HTML5Event.isMouseEvent(eventType)) { 
         *     var targetElem = document.elementFromPoint(e.clientX, e.clientY); 
         *     if (targetElem == null) return; 
         * } 
         * //span.hidden = null;
         */{}

        int keyCode = -1;

        if (HTML5Event.isKeyEvent(eventType)) {
            // global key event
            /**
             * @j2sNative 
             * keyCode = e.keyCode ? e.keyCode : e.which;
             */{}

            KeyEvent keyEvent = HTML5Event.toKeyEvent(eventType, keyCode);
            ViewRoot viewRoot = (ViewRoot) viewRootList.get(viewRootList.size() - 1);
            viewRoot.deliverKeyEventToViewHierarchy(keyEvent, false);
        } else if (eventType =="focusout") {
            // Mayloon Workaround : Fixed bug 848
            // Calculator cannot scroll when the length of number is larger then editText
            /**
             * @j2sNative
             * var thisText = e.target;
             * if ((thisText.tagName == "INPUT") && (thisText.style.textAlign == "right")) {
             *    thisText.scrollLeft = thisText.scrollWidth - thisText.clientWidth
             *                          + thisText.style.paddingRight.substr(0, thisText.style.paddingRight.length - 2)
             *                          + thisText.style.paddingLeft.substr(0, thisText.style.paddingLeft.length - 2);
             *}
             */{}
        } else {
            int x = 0, y = 0;
            /**
             * @j2sNative 
             * x = e.pageX; y = e.pageY;
             */{}
            // viewRoot.
            dispatchPointerEvent(e, eventType, x, y);
        }
    }

    /**
     * See if the key event means we should leave touch mode (and leave touch
     * mode if so).
     * @param event The key event.
     * @return Whether this key event should be consumed (meaning the act of
     *   leaving touch mode alone is considered the event).
     */
    private boolean checkForLeavingTouchModeAndConsume(KeyEvent event) {
        final int action = event.getAction();
        if (action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_MULTIPLE) {
            return false;
        }
        if ((event.getFlags()&KeyEvent.FLAG_KEEP_TOUCH_MODE) != 0) {
            return false;
        }

        // only relevant if we are in touch mode
        if (!mAttachInfo.mInTouchMode) {
            return false;
        }

        // if something like an edit text has focus and the user is typing,
        // leave touch mode
        //
        // note: the condition of not being a keyboard key is kind of a hacky
        // approximation of whether we think the focused view will want the
        // key; if we knew for sure whether the focused view would consume
        // the event, that would be better.
//        if (isKeyboardKey(event) && mView != null && mView.hasFocus()) {
//            mFocusedView = mView.findFocus();
//            if ((mFocusedView instanceof ViewGroup)
//                    && ((ViewGroup) mFocusedView).getDescendantFocusability() ==
//                    ViewGroup.FOCUS_AFTER_DESCENDANTS) {
//                // something has focus, but is holding it weakly as a container
//                return false;
//            }
//            if (ensureTouchMode(false)) {
//                throw new IllegalStateException("should not have changed focus "
//                        + "when leaving touch mode while a view has focus.");
//            }
//            return false;
//        }

        if (isDirectional(event.getKeyCode())) {
            // no view has focus, so we leave touch mode (and find something
            // to give focus to).  the event is consumed if we were able to
            // find something to give focus to.
            return ensureTouchMode(false);
        }
        return false;
    }

    /**
     * @param keyCode The key code
     * @return True if the key is directional.
     */
    static boolean isDirectional(int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
            return true;
        }
        return false;
    }

    private void deliverKeyEventToViewHierarchy(KeyEvent event, boolean sendDone) {
        try {
            if (mView != null && mAdded && event != null) {
                final int action = event.getAction();
                boolean isDown = (action == KeyEvent.ACTION_DOWN);
                if (checkForLeavingTouchModeAndConsume(event)) {
                    return;
                }

                boolean keyHandled = mView.dispatchKeyEvent(event);

                if (!keyHandled && isDown) {
                    int direction = 0;
                    switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        direction = View.FOCUS_LEFT;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        direction = View.FOCUS_RIGHT;
                        break;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        direction = View.FOCUS_UP;
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        direction = View.FOCUS_DOWN;
                        break;
                    }

                    if (direction != 0) {

                        View focused = mView != null ? mView.findFocus() : null;
                        if (focused != null) {
                            View v = focused.focusSearch(direction);
                            boolean focusPassed = false;
                            if (v != null && v != focused) {
                                // do the math the get the interesting rect
                                // of previous focused into the coord system of
                                // newly focused view
                                focused.getFocusedRect(mTempRect);
                                if (mView instanceof ViewGroup) {
                                    ((ViewGroup) mView).offsetDescendantRectToMyCoords(
                                            focused, mTempRect);
                                    ((ViewGroup) mView).offsetRectIntoDescendantCoords(
                                            v, mTempRect);
                                }
                                focusPassed = v.requestFocus(direction, mTempRect);
                            }

                            if (!focusPassed) {
                                mView.dispatchUnhandledMove(focused, direction);
                            }
                        }
                    }
                }
            }

        } finally {
            // Let the exception fall through -- the looper will catch
            // it and take care of the bad app for us.
        }
    }

    public void dispatchPointerEvent(Object e, String eventType, int x, int y) {

        /**
         * @j2sNative 
         * e.stopPropagation();
         */{}

        Activity cur = ((ActivityManager) Context.getSystemContext()
                .getSystemService(Context.ACTIVITY_SERVICE)).mCurActivity;
        if (cur == null) {
            Log.e(TAG, "mCurActivity is null?!");
            return;

        }

        // View rootView = cur.getWindow().getDecorView().getRootView();
        //
        // View thisView = rootView.findViewByElementId(viewElementId);
        // if (null != thisView) {
        // if (eventType.equals("click")) {
        // while (thisView.performClick() == false) {
        // if (thisView.getParent() != null
        // && thisView.getParent() instanceof View)
        // thisView = (View) thisView.getParent();
        // else
        // break;
        // }
        // } else if (eventType.equals("touchstart")) {
        // int x = 0, y = 0;
        // /**
        // @j2sNative
        // x = e.touches.item(0).clientX;
        // y = e.touches.item(0).clientY;
        // */
        // {
        // }
        // MotionEvent event = MotionEvent.obtain(0, 0,
        // android.view.MotionEvent.ACTION_DOWN, x, y, 0, 0, 0, 0,
        // 0, 0, 0);
        // while (thisView.onTouchEvent(event) == false) {
        // if (thisView.getParent() != null
        // && thisView.getParent() instanceof View)
        // thisView = (View) thisView.getParent();
        // else
        // break;
        // }
        // } else if (eventType.equals("touchend")) {
        // int x = 0, y = 0;
        // /**
        // @j2sNative
        // x = e.changedTouches.item(0).clientX;
        // y = e.changedTouches.item(0).clientY;
        // */
        // {
        // }
        // MotionEvent event = MotionEvent.obtain(0, 0,
        // android.view.MotionEvent.ACTION_UP, x, y, 0, 0, 0, 0,
        // 0, 0, 0);
        // while (thisView.onTouchEvent(event) == false) {
        // if (thisView.getParent() != null
        // && thisView.getParent() instanceof View)
        // thisView = (View) thisView.getParent();
        // else
        // break;
        // }
        // }
        // }
        int action;
        if (eventType.equals("mousedown")) {
            action = MotionEvent.ACTION_DOWN;
        } else if (eventType.equals("mouseup")) {
            action = MotionEvent.ACTION_UP;
        } else if (eventType.equals("mouseleave") || eventType.equals("mouseout")) {
            action = MotionEvent.ACTION_OUTSIDE;
        } else if (eventType.equals("mouseenter") || eventType.equals("mouseover")) {
            // ignore by now
            return;
        } else if (eventType.equals("mousemove")) {
            action = MotionEvent.ACTION_MOVE;
        } else {
            return;
        }

        // Dispatch motion event staring from DecroView
        if (mView != null) {
            // enter touch mode on the down
            boolean isDown = action == MotionEvent.ACTION_DOWN;
            if (isDown) {
                ensureTouchMode(true);
            }
            // Compensate the position first because our Window maybe not at (0, 0)
            x = x - this.mAttachInfo.mWindowLeft;
            y = y - this.mAttachInfo.mWindowTop;
            long time = SystemClock.uptimeMillis();
            mView.dispatchTouchEvent(MotionEvent.obtain(time, time, action, x, y, 0));
        }
    }

    /**
     * Indicates whether we are in touch mode. Calling this method triggers an
     * IPC call and should be avoided whenever possible.
     * 
     * @return True, if the device is in touch mode, false otherwise.
     * @hide
     */
    // FIXME: what this return value means??
    static boolean isInTouchMode() {
        WindowManagerImpl mWindowManager = (WindowManagerImpl)WindowManagerImpl.getDefault();
        return mWindowManager.getInTouchMode();
    }

	public View getView() {
		return mView;
	}

	public void requestLayout() {
		mLayoutRequested = true;
		scheduleTraversals();
	}

	public boolean isLayoutRequested() {
		return mLayoutRequested;
	}

	public ViewParent getParent() {
		return null;
	}

	public void bringChildToFront(View child) {
	}

    public void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            sendEmptyMessage(DO_TRAVERSAL);
        }
    }

    public void unscheduleTraversals() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            removeMessages(DO_TRAVERSAL);
        }
    }

    public void setView(View view, WindowManager.LayoutParams attrs,
            View panelParentView) throws Exception {
        if (!(view instanceof DecorView)) {
            throw new IllegalArgumentException(
                    "view must be an instance of DecorView");
        }
        if (mView == null) {
            mView = view;
            mWindowAttributes.copyFrom(attrs);
            attrs = mWindowAttributes;
            mSoftInputMode = attrs.softInputMode;
            mWindowAttributesChanged = true;
            mAttachInfo.mRootView = view;
            mAttachInfo.mApplicationScale = 1.0f;
            mAdded = true;
            WindowManagerImpl mWindowManager = (WindowManagerImpl)WindowManagerImpl.getDefault();
            mAddedTouchMode = mWindowManager.getInTouchMode();
            makeHostView(view);
            requestLayout();
        }
    }

	private void makeHostView(View view) {
		view.assignParent(this,true);
		if (!(view instanceof DecorView)) {
			view.zIndex = 10;
			/**
			  @j2sNative
			 var thisView = document.getElementById(view.getUIElementID());
			 if (null == thisView) {
			 	thisView = document.createElement("span");
				thisView.style.position = "absolute";
				thisView.id = view.getUIElementID();
				thisView.style.display = "block";
			 }
			 document.body.appendChild(thisView);
			 */{}
			 return;
		}
		int decorViewID = view.getUIElementID();
		int decorZIndex = view.getZIndex();
		if(view.getWidth()!=0&&view.getHeight()!=0){
			mWinFrame = new Rect(0,0,view.getWidth(),view.getHeight());
		}
		/**
		 @j2sNative
		 var rootView = document.getElementById(this.mViewRootID);
		 rootView.style.zIndex = decorZIndex;
		 // Canvas to draw on 
		 rootView.childNodes[0].style.zIndex = decorZIndex - 1;

		 // attach decorView
		 var decorView = document.getElementById(decorViewID);

         if (decorView == null) {
              decorView = document.createElement("span");
              decorView.style.position = "absolute";
              decorView.id = decorViewID;
              decorView.style.display = "block";
              document.body.appendChild(decorView);
         }

		 if (!android.util.DebugUtils.DEBUG_VIEW_IN_BROWSER) {
		     decorView.id = this.mViewRootID + "_DecorView";
		 }
		 decorView.style.zIndex = decorZIndex;
		 rootView.appendChild(decorView);
		 */
		{
		}
	}

    private void setOuterDimension(int width, int height) {
        mWidth = width;
        mHeight = height;
        
        /**
         * @j2sNative 
         * var rootView = document.getElementById(this.mViewRootID);
         * if (rootView != null) { 
         *     rootView.style.width = width + "px"; 
         *     rootView.style.height = height + "px";
         *     var appCanvas = rootView.childNodes[0];
         *     if (appCanvas != null) {
         *         if (appCanvas.width != width || appCanvas.height != height) {
         *             appCanvas.width = width;
         *             appCanvas.height = height;
         *         } 
         *     }
         * }
         */{}
    }
    
    private void setOuterPosition(int left, int top) {
        /**
         * @j2sNative 
         * var rootView = document.getElementById(this.mViewRootID);
         * if (rootView != null) { 
         *     rootView.style.left = left + "px"; 
         *     rootView.style.top = top + "px";
         * }
         */{}
    }

	public void setVisibility(int visibility) {
		String visible = "visible";
		if (visibility != View.VISIBLE)
			visible = "hidden";
		/**
		 @j2sNative
		 var thisView = document.getElementById(this.mViewRootID);
		 if (thisView != null)
		 	thisView.style.visibility = visible;
		*/
		{
		}
	}

    private void performTraversals() {
        // cache mView since it is used so much below...
        final View host = mView;
        if (DBG) {
            System.out.println("======================================");
            System.out.println("performTraversals Start");
        }

        if (host == null || !mAdded)
            return;

        mTraversalScheduled = false;
        mWillDrawSoon = true;
        boolean windowResizesToFitContent = false;
        boolean fullRedrawNeeded = mFullRedrawNeeded;
        boolean newSurface = false;
        boolean surfaceChanged = false;
        WindowManager.LayoutParams lp = mWindowAttributes;

        int desiredWindowWidth;
        int desiredWindowHeight;
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        final View.AttachInfo attachInfo = mAttachInfo;

        final int viewVisibility = getHostVisibility();
        this.setVisibility(viewVisibility);
        boolean viewVisibilityChanged = mViewVisibility != viewVisibility
                || mNewSurfaceNeeded;

        WindowManager.LayoutParams params = null;
        if (mWindowAttributesChanged) {
            mWindowAttributesChanged = false;
            surfaceChanged = true;
            params = lp;
        }
        Rect frame = mWinFrame;
        if (mFirst) {
            newSurface = true;
            fullRedrawNeeded = true;
            mLayoutRequested = true;

            DisplayMetrics packageMetrics = mView.getContext().getResources()
                    .getDisplayMetrics();
            desiredWindowWidth = packageMetrics.widthPixels;
            desiredWindowHeight = packageMetrics.heightPixels;
            // System.out.println("desiredWindowWidth: " + desiredWindowWidth
            // + ", desiredWindowHeight: " + desiredWindowHeight);

            // For the very first time, tell the view hierarchy that it
            // is attached to the window. Note that at this point the surface
            // object is not initialized to its backing store, but soon it
            // will be (assuming the window is visible).
            attachInfo.mHasWindowFocus = false;
            attachInfo.mWindowVisibility = viewVisibility;
            attachInfo.mRecomputeGlobalAttributes = false;
            attachInfo.mKeepScreenOn = false;
            viewVisibilityChanged = false;
            if (!mAttached) {
                mAttached = true;
                host.dispatchAttachedToWindow(attachInfo, 0);
            }
        } else {
            desiredWindowWidth = frame.width();
            desiredWindowHeight = frame.height();
            if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {
                fullRedrawNeeded = true;
                mLayoutRequested = true;
                windowResizesToFitContent = true;
            }
        }

        if (viewVisibilityChanged) {
            Log.d(TAG, "viewVisibilityChanged: " + viewVisibility);
            attachInfo.mWindowVisibility = viewVisibility;
            host.dispatchWindowVisibilityChanged(viewVisibility);
            if (viewVisibility == View.GONE) {
                // After making a window gone, we will count it as being
                // shown for the first time the next time it gets focus.
                mHasHadWindowFocus = false;
            }
        }

        boolean insetsChanged = false;

        if (mLayoutRequested) {
            // Execute enqueued actions on every layout in case a view that was
            // detached
            // enqueued an action after being detached
            if (mFirst) {
                // make sure touch mode code executes by setting cached value
                // to opposite of the added touch mode.
                mAttachInfo.mInTouchMode = !mAddedTouchMode;
                ensureTouchModeLocally(mAddedTouchMode);
            } else {
                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                        || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    windowResizesToFitContent = true;

                    DisplayMetrics packageMetrics = mView.getContext()
                            .getResources().getDisplayMetrics();
                    desiredWindowWidth = packageMetrics.widthPixels;
                    desiredWindowHeight = packageMetrics.heightPixels;
                }
            }

            this.setOuterDimension(desiredWindowWidth, desiredWindowHeight);

            childWidthMeasureSpec = getRootMeasureSpec(desiredWindowWidth,
                    lp.width);
            childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight,
                    lp.height);

            // Ask host how big it wants to be
            // here our host is DecorView (extends FrameLayout)
            host.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            if (DBG) {
                System.out.println("======================================");
                System.out.println("performTraversals -- after measure");
            }
        }

        if (attachInfo.mRecomputeGlobalAttributes) {
            // Log.i(TAG, "Computing screen on!");
            attachInfo.mRecomputeGlobalAttributes = false;
            boolean oldVal = attachInfo.mKeepScreenOn;
            attachInfo.mKeepScreenOn = false;
            host.dispatchCollectViewAttributes(0);
            if (attachInfo.mKeepScreenOn != oldVal) {
                params = lp;
                // Log.i(TAG, "Keep screen on changed: " +
                // attachInfo.mKeepScreenOn);
            }
        }

        if (mFirst || attachInfo.mViewVisibilityChanged) {
            attachInfo.mViewVisibilityChanged = false;
            int resizeMode = mSoftInputMode
                    & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
            // If we are in auto resize mode, then we need to determine
            // what mode to use now.
            if (resizeMode == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {
                final int N = attachInfo.mScrollContainers.size();
                for (int i = 0; i < N; i++) {
                    if (attachInfo.mScrollContainers.get(i).isShown()) {
                        resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                    }
                }
                if (resizeMode == 0) {
                    resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                }
                if ((lp.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) != resizeMode) {
                    lp.softInputMode = (lp.softInputMode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                            | resizeMode;
                    params = lp;
                }
            }
        }

        if (params != null
                && (host.mPrivateFlags & View.REQUEST_TRANSPARENT_REGIONS) != 0) {
            if (!PixelFormat.formatHasAlpha(params.format)) {
                params.format = PixelFormat.TRANSLUCENT;
            }
        }

        boolean windowShouldResize = mLayoutRequested
                && windowResizesToFitContent
                && ((mWidth != host.mMeasuredWidth || mHeight != host.mMeasuredHeight)
                        || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                                && frame.width() < desiredWindowWidth && frame
                                .width() != mWidth) || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT
                        && frame.height() < desiredWindowHeight && frame
                        .height() != mHeight));

        final boolean computesInternalInsets = attachInfo.mTreeObserver
                .hasComputeInternalInsetsListeners();
        boolean insetsPending = false;
        int relayoutResult = 0;
        if (mFirst || windowShouldResize || insetsChanged
                || viewVisibilityChanged || params != null) {
            boolean contentInsetsChanged = false;
            int fl = 0;
            if (params != null) {
                fl = params.flags;
                if (attachInfo.mKeepScreenOn) {
                    params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                }
            }

            // notify focus change.
            boolean visible  = ((getHostVisibility() & View.VISIBILITY_MASK) == View.VISIBLE);
            windowFocusChanged(visible, isInTouchMode());
            
            if (params != null) {
                relayoutResult = relayoutWindow(params, mView.mMeasuredWidth,
                        mView.mMeasuredHeight, viewVisibility, insetsPending, frame,
                        null, null);
            }

            if (params != null) {
                params.flags = fl;
            }

            attachInfo.mWindowLeft = frame.left;
            attachInfo.mWindowTop = frame.top;

            this.setOuterDimension(frame.width(), frame.height());

            // !!FIXME!! This next section handles the case where we did not get
            // the
            // window size we asked for. We should avoid this by getting a
            // maximum size from
            // the window session beforehand.
            mWidth = frame.width();
            mHeight = frame.height();

            boolean focusChangedDueToTouchMode = ensureTouchModeLocally(
                    (relayoutResult&WindowManagerImpl.RELAYOUT_IN_TOUCH_MODE) != 0);
            if (focusChangedDueToTouchMode || mWidth != host.mMeasuredWidth
                    || mHeight != host.mMeasuredHeight || contentInsetsChanged) {
                childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
                childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);

                // Ask host how big it wants to be
                host.measure(childWidthMeasureSpec, childHeightMeasureSpec);

                // Implementation of weights from WindowManager.LayoutParams
                // We just grow the dimensions as needed and re-measure if
                // needs be
                int width = host.mMeasuredWidth;
                int height = host.mMeasuredHeight;
                boolean measureAgain = false;

                if (lp.horizontalWeight > 0.0f) {
                    width += (int) ((mWidth - width) * lp.horizontalWeight);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
                            MeasureSpec.EXACTLY);
                    measureAgain = true;
                }
                if (lp.verticalWeight > 0.0f) {
                    height += (int) ((mHeight - height) * lp.verticalWeight);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            height, MeasureSpec.EXACTLY);
                    measureAgain = true;
                }

                if (measureAgain) {
                    System.out.println("Measure Again");
                    host.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                }

                mLayoutRequested = true;
            }
        }

        final boolean didLayout = mLayoutRequested;
        boolean triggerGlobalLayoutListener = didLayout
                || attachInfo.mRecomputeGlobalAttributes;
        if (didLayout) {
            mLayoutRequested = false;
            mScrollMayChange = true;
            // layout our DecorView first because DecorView may be not at (0,
            // 0).
            this.setOuterPosition(attachInfo.mWindowLeft,
                    attachInfo.mWindowTop);

            host.layout(0, 0, host.mMeasuredWidth, host.mMeasuredHeight);

            // By this point all views have been sized and positionned
            // We can compute the transparent area

            if ((host.mPrivateFlags & View.REQUEST_TRANSPARENT_REGIONS) != 0) {
                // start out transparent
                // TODO: AVOID THAT CALL BY CACHING THE RESULT?
                host.getLocationInWindow(mTmpLocation);
            }

            if (DBG) {
                System.out.println("======================================");
                System.out.println("performTraversals -- after setFrame");
            }
        }

        if (triggerGlobalLayoutListener) {
            attachInfo.mRecomputeGlobalAttributes = false;
            attachInfo.mTreeObserver.dispatchOnGlobalLayout();
        }

        if (computesInternalInsets) {
            ViewTreeObserver.InternalInsetsInfo insets = attachInfo.mGivenInternalInsets;
            final Rect givenContent = attachInfo.mGivenInternalInsets.contentInsets;
            final Rect givenVisible = attachInfo.mGivenInternalInsets.visibleInsets;
            givenContent.left = givenContent.top = givenContent.right = givenContent.bottom = givenVisible.left = givenVisible.top = givenVisible.right = givenVisible.bottom = 0;
            attachInfo.mTreeObserver.dispatchOnComputeInternalInsets(insets);
        }

        if (mFirst) {
            // handle first focus request
            if (mView != null) {
                if (!mView.hasFocus()) {
                    mView.requestFocus(View.FOCUS_FORWARD);
                    mFocusedView = mRealFocusedView = mView.findFocus();
                } else {
                    mRealFocusedView = mView.findFocus();
                }
            }
        }

        mFirst = false;
        mWillDrawSoon = false;
        mNewSurfaceNeeded = false;
        mViewVisibility = viewVisibility;

        if (mAttachInfo.mHasWindowFocus) {
            final boolean imTarget = WindowManager.LayoutParams
                    .mayUseInputMethod(mWindowAttributes.flags);
            if (imTarget != mLastWasImTarget) {
                mLastWasImTarget = imTarget;
            }
        }

        boolean cancelDraw = attachInfo.mTreeObserver.dispatchOnPreDraw();
        if (!cancelDraw && !newSurface) {
            mFullRedrawNeeded = false;
            draw(fullRedrawNeeded || mDialogCount > 0);
        } else {
            // We were supposed to report when we are done drawing. Since we
            // canceled the
            // draw, remember it here.
            if (fullRedrawNeeded) {
                mFullRedrawNeeded = true;
            }
            if (newSurface)
                newSurface = false;
            // Try again
            scheduleTraversals();
        }

        if (DBG) {
            System.out.println("======================================");
            System.out.println("performTraversals Finish");
        }
    }

	// TODO very IMPORTANT here
	int getHostVisibility() {
		return mAppVisible ? mView.getVisibility() : View.GONE;
	}

	public void requestTransparentRegion(View child) {
		if (mView == child) {
			mView.mPrivateFlags |= View.REQUEST_TRANSPARENT_REGIONS;
			// Need to make sure we re-evaluate the window attributes next
			// time around, to ensure the window has the correct format.
			mWindowAttributesChanged = true;
			requestLayout();
		}
	}

	/**
	 * Figures out the measure spec for the root view in a window based on it's
	 * layout params.
	 *
	 * @param windowSize
	 *            The available width or height of the window
	 *
	 * @param rootDimension
	 *            The layout params for one dimension (width or height) of the
	 *            window.
	 *
	 * @return The measure spec to use to measure the root view.
	 */
	private int getRootMeasureSpec(int windowSize, int rootDimension) {
		int measureSpec;
		switch (rootDimension) {

		case ViewGroup.LayoutParams.MATCH_PARENT:
			// Window can't resize. Force root view to be windowSize.
			measureSpec = MeasureSpec.makeMeasureSpec(windowSize,
					MeasureSpec.EXACTLY);
			break;
		case ViewGroup.LayoutParams.WRAP_CONTENT:
			// Window can resize. Set max size for root view.
			measureSpec = MeasureSpec.makeMeasureSpec(windowSize,
					MeasureSpec.AT_MOST);
			break;
		default:
			// Window wants to be an exact size. Force root view to be that
			// size.
			measureSpec = MeasureSpec.makeMeasureSpec(rootDimension,
					MeasureSpec.EXACTLY);
			break;
		}
		return measureSpec;
	}

	@Override
	public void playSoundEffect(int effectId) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean performHapticFeedback(int effectId, boolean always) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void invalidateChild(View child, Rect dirty) {
		if(DBG)System.out.println("invalidateChild");
        if (mCurScrollY != 0) {
            mTempRect.set(dirty);
            dirty = mTempRect;
            if (mCurScrollY != 0) {
               dirty.offset(0, -mCurScrollY);
            }
            if (mAttachInfo.mScalingRequired) {
                dirty.inset(-1, -1);
            }
        }
        mDirty.union(dirty);
        if (!mWillDrawSoon) {
            scheduleTraversals();
        }
	}

	@Override
	public ViewParent invalidateChildInParent(int[] location, final Rect dirty) {
        invalidateChild(null, dirty);
        return null;
	}

    public void requestChildFocus(View child, View focused) {
        //checkThread();
        if (mFocusedView != focused) {
            mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(mFocusedView, focused);
            scheduleTraversals();
        }
        mFocusedView = mRealFocusedView = focused;
        if (false) Log.v(TAG, "Request child focus: focus now "
                + mFocusedView);
    }

    public void recomputeViewAttributes(View child) {
        //checkThread();
        if (mView == child) {
            mAttachInfo.mRecomputeGlobalAttributes = true;
            if (!mWillDrawSoon) {
                scheduleTraversals();
            }
        }
    }

    public void clearChildFocus(View child) {
        //checkThread();

        View oldFocus = mFocusedView;

        if (false) Log.v(TAG, "Clearing child focus");
        mFocusedView = mRealFocusedView = null;
        if (mView != null && !mView.hasFocus()) {
            // If a view gets the focus, the listener will be invoked from requestChildFocus()
            if (!mView.requestFocus(View.FOCUS_FORWARD)) {
                mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, null);
            }
        } else if (oldFocus != null) {
            mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, null);
        }
    }

	@Override
	public boolean getChildVisibleRect(View child, Rect r, Point offset) {
        if (child != mView) {
            throw new RuntimeException("child is not mine, honest!");
        }
        // Note: don't apply scroll offset, because we want to know its
        // visibility in the virtual canvas being given to the view hierarchy.
        return r.intersect(0, 0, mWidth, mHeight);
	}

    /**
     * {@inheritDoc}
     */
    public View focusSearch(View focused, int direction) {
        //checkThread();
        if (!(mView instanceof ViewGroup)) {
            return null;
        }
        return FocusFinder.getInstance().findNextFocus((ViewGroup) mView, focused, direction);
    }

    public void focusableViewAvailable(View v) {
        //checkThread();

        if (mView != null && !mView.hasFocus()) {
            v.requestFocus();
        } else {
            // the one case where will transfer focus away from the current one
            // is if the current view is a view group that prefers to give focus
            // to its children first AND the view is a descendant of it.
            mFocusedView = mView.findFocus();
            boolean descendantsHaveDibsOnFocus =
                    (mFocusedView instanceof ViewGroup) &&
                        (((ViewGroup) mFocusedView).getDescendantFocusability() ==
                                ViewGroup.FOCUS_AFTER_DESCENDANTS);
            if (descendantsHaveDibsOnFocus && isViewDescendantOf(v, mFocusedView)) {
                // If a view gets the focus, the listener will be invoked from requestChildFocus()
                v.requestFocus();
            }
        }
    }
    
    /**
     * Return true if child is an ancestor of parent, (or equal to the parent).
     */
    private static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }

	@Override
	public boolean showContextMenuForChild(View originalView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void childDrawableStateChanged(View child) {
		// TODO Auto-generated method stub
	}

	@Override
	public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
			boolean immediate) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setLayoutParams(WindowManager.LayoutParams attrs,
			boolean newView) {
		int oldSoftInputMode = mWindowAttributes.softInputMode;
		// preserve compatible window flag if exists.
		int compatibleWindowFlag = mWindowAttributes.flags
				& WindowManager.LayoutParams.FLAG_COMPATIBLE_WINDOW;
		mWindowAttributes.copyFrom(attrs);
		mWindowAttributes.flags |= compatibleWindowFlag;

		if (newView) {
			mSoftInputMode = attrs.softInputMode;
			requestLayout();
		}
		// Don't lose the mode we last auto-computed.
		if ((attrs.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {
			mWindowAttributes.softInputMode = (mWindowAttributes.softInputMode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
					| (oldSoftInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);
		}
		mWindowAttributesChanged = true;
		scheduleTraversals();
	}

    private int relayoutWindow(WindowManager.LayoutParams params, int requestedWidth,
            int requestedHeight, int viewVisibility, boolean insetsPending, Rect outFrame,
            Rect outContentInsets, Rect outVisibleInsets) {

        float appScale = mAttachInfo.mApplicationScale;
        boolean restore = false;
//        if (params != null && mTranslator != null) {
//            restore = true;
//            params.backup();
//            mTranslator.translateWindowLayout(params);
//        }
        if (params != null) {
            if (DBG) Log.d(TAG, "WindowLayout in layoutWindow:" + params);
        }
//        mPendingConfiguration.seq = 0;
//        //Log.d(TAG, ">>>>>> CALLING relayout");
//        int relayoutResult = mWindow.relayout(
//                mWindow, params,
//                (int) (mView.mMeasuredWidth * appScale + 0.5f),
//                (int) (mView.mMeasuredHeight * appScale + 0.5f),
//                viewVisibility, insetsPending, mWinFrame,
//                mPendingContentInsets, mPendingVisibleInsets,
//                mPendingConfiguration, mSurface);
        // MayLoon: The relayout logic is too complex, only apply WindowManagerService.computeFrameLw here
        performLayoutLockedInner(params, requestedWidth, requestedHeight, viewVisibility, insetsPending, outFrame, outContentInsets, outVisibleInsets);
        //Log.d(TAG, "<<<<<< BACK FROM relayout");
//        if (restore) {
//            params.restore();
//        }
//        
//        if (mTranslator != null) {
//            mTranslator.translateRectInScreenToAppWinFrame(mWinFrame);
//            mTranslator.translateRectInScreenToAppWindow(mPendingContentInsets);
//            mTranslator.translateRectInScreenToAppWindow(mPendingVisibleInsets);
//        }
        int relayoutResult = 0;
        if (isInTouchMode()) {
            relayoutResult = 1;
        }
        return relayoutResult;
    }
    
    private void performLayoutLockedInner(WindowManager.LayoutParams attrs, int requestedWidth,
            int requestedHeight, int viewVisibility, boolean insetsPending, Rect outFrame,
            Rect outContentInsets, Rect outVisibleInset) {
        if (viewVisibility == View.GONE) {
            return;
        }

        //beginLayoutLw(dw, dh));
        // The current size of the screen.
        int mW, mH;
        // During layout, the current screen borders with all outer decoration
        // (status bar, input method dock) accounted for.
        int mCurLeft, mCurTop, mCurRight, mCurBottom;
        // During layout, the frame in which content should be displayed
        // to the user, accounting for all screen decoration except for any
        // space they deem as available for other content.  This is usually
        // the same as mCur*, but may be larger if the screen decor has supplied
        // content insets.
        int mContentLeft, mContentTop, mContentRight, mContentBottom;
        // During layout, the current screen borders along with input method
        // windows are placed.
        int mDockLeft, mDockTop, mDockRight, mDockBottom;
        
        DisplayMetrics packageMetrics = mView.getContext().getResources()
                .getDisplayMetrics();
        mW = packageMetrics.widthPixels;
        mH = packageMetrics.heightPixels;
        
        mDockLeft = mContentLeft = mCurLeft = 0;
        mDockTop = mContentTop = mCurTop = 0;
        mDockRight = mContentRight = mCurRight = mW;
        mDockBottom = mContentBottom = mCurBottom = mH;

        // decide where the status bar goes ahead of time
//        if (mStatusBar != null) {
//            final Rect pf = mTmpParentFrame;
//            final Rect df = mTmpDisplayFrame;
//            final Rect vf = mTmpVisibleFrame;
//            pf.left = df.left = vf.left = 0;
//            pf.top = df.top = vf.top = 0;
//            pf.right = df.right = vf.right = displayWidth;
//            pf.bottom = df.bottom = vf.bottom = displayHeight;
//            
//            mStatusBar.computeFrameLw(pf, df, vf, vf);
//            if (mStatusBar.isVisibleLw()) {
//                // If the status bar is hidden, we don't want to cause
//                // windows behind it to scroll.
//                mDockTop = mContentTop = mCurTop = mStatusBar.getFrameLw().bottom;
//                if (DEBUG_LAYOUT) Log.v(TAG, "Status bar: mDockBottom="
//                        + mDockBottom + " mContentBottom="
//                        + mContentBottom + " mCurBottom=" + mCurBottom);
//            }
//        }
        
        final int fl = attrs.flags;
        final int sim = attrs.softInputMode;
        
        final Rect pf = new Rect();
        final Rect df = new Rect();
        final Rect cf = new Rect();
        final Rect vf = new Rect();
        boolean attached = false;//attached != null
        if (attrs.type == TYPE_INPUT_METHOD) {
            pf.left = df.left = cf.left = vf.left = mDockLeft;
            pf.top = df.top = cf.top = vf.top = mDockTop;
            pf.right = df.right = cf.right = vf.right = mDockRight;
            pf.bottom = df.bottom = cf.bottom = vf.bottom = mDockBottom;
            // IM dock windows always go to the bottom of the screen.
            attrs.gravity = Gravity.BOTTOM;
            //mDockLayer = win.getSurfaceLayer();
        } else {
            if ((fl &
                    (FLAG_LAYOUT_IN_SCREEN | FLAG_FULLSCREEN | FLAG_LAYOUT_INSET_DECOR))
                    == (FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_INSET_DECOR)) {
                // This is the case for a normal activity window: we want it
                // to cover all of the screen space, and it can take care of
                // moving its contents to account for screen decorations that
                // intrude into that space.
                if (attached) {
                    // If this window is attached to another, our display
                    // frame is the same as the one we are attached to.
                    //setAttachedWindowFrames(win, fl, sim, attached, true, pf, df, cf, vf);
                } else {
                    pf.left = df.left = 0;
                    pf.top = df.top = 0;
                    pf.right = df.right = mW;
                    pf.bottom = df.bottom = mH;
                    if ((sim & SOFT_INPUT_MASK_ADJUST) != SOFT_INPUT_ADJUST_RESIZE) {
                        cf.left = mDockLeft;
                        cf.top = mDockTop;
                        cf.right = mDockRight;
                        cf.bottom = mDockBottom;
                    } else {
                        cf.left = mContentLeft;
                        cf.top = mContentTop;
                        cf.right = mContentRight;
                        cf.bottom = mContentBottom;
                    }
                    vf.left = mCurLeft;
                    vf.top = mCurTop;
                    vf.right = mCurRight;
                    vf.bottom = mCurBottom;
                }
            } else if ((fl & FLAG_LAYOUT_IN_SCREEN) != 0) {
                // A window that has requested to fill the entire screen just
                // gets everything, period.
                pf.left = df.left = cf.left = 0;
                pf.top = df.top = cf.top = 0;
                pf.right = df.right = cf.right = mW;
                pf.bottom = df.bottom = cf.bottom = mH;
                vf.left = mCurLeft;
                vf.top = mCurTop;
                vf.right = mCurRight;
                vf.bottom = mCurBottom;
            } else if (attached) {
                // A child window should be placed inside of the same visible
                // frame that its parent had.
                //setAttachedWindowFrames(win, fl, sim, attached, false, pf, df, cf, vf);
            } else {
                // Otherwise, a normal window must be placed inside the content
                // of all screen decorations.
                pf.left = mContentLeft;
                pf.top = mContentTop;
                pf.right = mContentRight;
                pf.bottom = mContentBottom;
                if ((sim & SOFT_INPUT_MASK_ADJUST) != SOFT_INPUT_ADJUST_RESIZE) {
                    df.left = cf.left = mDockLeft;
                    df.top = cf.top = mDockTop;
                    df.right = cf.right = mDockRight;
                    df.bottom = cf.bottom = mDockBottom;
                } else {
                    df.left = cf.left = mContentLeft;
                    df.top = cf.top = mContentTop;
                    df.right = cf.right = mContentRight;
                    df.bottom = cf.bottom = mContentBottom;
                }
                vf.left = mCurLeft;
                vf.top = mCurTop;
                vf.right = mCurRight;
                vf.bottom = mCurBottom;
            }
        }
        
//        if ((fl & FLAG_LAYOUT_NO_LIMITS) != 0) {
//            df.left = df.top = cf.left = cf.top = vf.left = vf.top = -10000;
//            df.right = df.bottom = cf.right = cf.bottom = vf.right = vf.bottom = 10000;
//        }
        
        computeFrameLw(attrs, requestedWidth, requestedHeight, outFrame, outContentInsets, outVisibleInset, pf, df, cf, vf);
    }
    
    public void computeFrameLw(WindowManager.LayoutParams mAttrs, int requestedWidth,
            int requestedHeight, Rect outFrame, Rect outContentInsets, Rect outVisibleInset, Rect pf, Rect df, Rect cf, Rect vf) {
        int mRequestedWidth = requestedWidth;
        int mRequestedHeight = requestedHeight;
        
        final Rect container = new Rect();
        container.set(pf);

        final Rect display = new Rect();
        display.set(df);

//        if ((mAttrs.flags & WindowManager.LayoutParams.FLAG_COMPATIBLE_WINDOW) != 0) {
//            container.intersect(mCompatibleScreenFrame);
//            if ((mAttrs.flags & WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) == 0) {
//                display.intersect(mCompatibleScreenFrame);
//            }
//        }

        final int pw = container.right - container.left;
        final int ph = container.bottom - container.top;

        int w,h;
        if ((mAttrs.flags & mAttrs.FLAG_SCALED) != 0) {
            w = mAttrs.width < 0 ? pw : mAttrs.width;
            h = mAttrs.height< 0 ? ph : mAttrs.height;
        } else {
            w = mAttrs.width == mAttrs.MATCH_PARENT ? pw : mRequestedWidth;
            h = mAttrs.height== mAttrs.MATCH_PARENT ? ph : mRequestedHeight;
        }

        final Rect content = new Rect();
        content.set(cf);

        final Rect visible = new Rect();;
        visible.set(vf);

        final Rect frame = new Rect();
//        final int fw = frame.width();
//        final int fh = frame.height();

        //System.out.println("In: w=" + w + " h=" + h + " container=" +
        //                   container + " x=" + mAttrs.x + " y=" + mAttrs.y);

        Gravity.apply(mAttrs.gravity, w, h, container,
                (int) (mAttrs.x + mAttrs.horizontalMargin * pw),
                (int) (mAttrs.y + mAttrs.verticalMargin * ph), frame);

        // Now make sure the window fits in the overall display.
        Gravity.applyDisplay(mAttrs.gravity, df, frame);

        // Make sure the content and visible frames are inside of the
        // final window frame.
        if (content.left < frame.left) content.left = frame.left;
        if (content.top < frame.top) content.top = frame.top;
        if (content.right > frame.right) content.right = frame.right;
        if (content.bottom > frame.bottom) content.bottom = frame.bottom;
        if (visible.left < frame.left) visible.left = frame.left;
        if (visible.top < frame.top) visible.top = frame.top;
        if (visible.right > frame.right) visible.right = frame.right;
        if (visible.bottom > frame.bottom) visible.bottom = frame.bottom;

        final Rect contentInsets = new Rect();
        contentInsets.left = content.left-frame.left;
        contentInsets.top = content.top-frame.top;
        contentInsets.right = frame.right-content.right;
        contentInsets.bottom = frame.bottom-content.bottom;

        final Rect visibleInsets = new Rect();
        visibleInsets.left = visible.left-frame.left;
        visibleInsets.top = visible.top-frame.top;
        visibleInsets.right = frame.right-visible.right;
        visibleInsets.bottom = frame.bottom-visible.bottom;

//        if (mIsWallpaper && (fw != frame.width() || fh != frame.height())) {
//            updateWallpaperOffsetLocked(this, mDisplay.getWidth(),
//                    mDisplay.getHeight(), false);
//        }
        
        if (outFrame != null) outFrame.set(frame);
        if (outContentInsets != null) outContentInsets.set(contentInsets);
        if (outVisibleInset != null) outVisibleInset.set(visibleInsets);
    }
        
	public void die(boolean b) {
		//		if (mAdded && !mFirst) {
		//			int viewVisibility = mView.getVisibility();
		//			boolean viewVisibilityChanged = mViewVisibility != viewVisibility;
		//			if (mWindowAttributesChanged || viewVisibilityChanged) {
		//				// If layout params have been changed, first give them
		//				// to the window manager to make sure it has the correct
		//				// animation info.
		//				try {
		//					if ((relayoutWindow(mWindowAttributes, viewVisibility,
		//							false) & WindowManagerImpl.RELAYOUT_FIRST_TIME) != 0) {
		//						sWindowSession.finishDrawing(mWindow);
		//					}
		//				} catch (RemoteException e) {
		//				}
		//			}
		//
		//			mSurface.release();
		//		}
		if (mAdded) {
			mAdded = false;
			dispatchDetachedFromWindow();
		}
	}

    void dispatchDetachedFromWindow() {
        Log.i(TAG, "dispatchDetachedFromWindow");

        if (mView != null && mAttached) {
            mView.dispatchDetachedFromWindow();
            mAttached = false;
        }
		mView = null;
		mAttachInfo.mRootView = null;

		if (mInputChannel != null) {
			if (mInputQueueCallback != null) {
				mInputQueueCallback.onInputQueueDestroyed(mInputQueue);
				mInputQueueCallback = null;
			} else {
				InputQueue.unregisterInputChannel(mInputChannel);
			}
		}

		// Dispose the input channel after removing the window so the Window Manager
		// doesn't interpret the input channel being closed as an abnormal termination.
		if (mInputChannel != null) {
			mInputChannel.dispose();
			mInputChannel = null;
		}
		reset();
	}

    static float gPrevDur = 0;

    private void draw(boolean fullRedrawNeeded) {
        if (mAttachInfo.mViewScrollChanged) {
            mAttachInfo.mViewScrollChanged = false;
            mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
        }
        int yoff = mScrollY;
        if (mCurScrollY != yoff) {
            mCurScrollY = yoff;
            fullRedrawNeeded = true;
        }
        float appScale = mAttachInfo.mApplicationScale;
        Rect dirty = mDirty;

        if (fullRedrawNeeded) {
            mAttachInfo.mIgnoreDirtyState = true;
            dirty.union(0, 0, (int) (mWidth * appScale), (int) (mHeight * appScale));
        }
        mCanvas.setDimension(mWidth, mHeight);
        if (!dirty.isEmpty() || mIsAnimating) {

            Canvas canvas;

            int left = dirty.left;
            int top = dirty.top;
            int right = dirty.right;
            int bottom = dirty.bottom;

            canvas = mCanvas;

            // If this bitmap's format includes an alpha channel, we
            // need to clear it before drawing so that the child will
            // properly re-composite its drawing on a transparent
            // background. This automatically respects the clip/dirty region
            // or
            // If we are applying an offset, we need to clear the area
            // where the offset doesn't appear to avoid having garbage
            // left in the blank areas.
            //if (/*!canvas.isOpaque() || */yoff != 0) {
                // Clear the background first.
//                canvas.chooseCanvas(Canvas.BACKGROUND_CANVAS);
//                canvas.drawColor(0);
            //}

            dirty.setEmpty();
            mIsAnimating = false;
            mAttachInfo.mDrawingTime = SystemClock.uptimeMillis();
            mView.mPrivateFlags |= View.DRAWN;
            int saveCount = canvas.save();
            try {
                canvas.clipRect(left, top, right, bottom);
                canvas.translate(0, -yoff);
                mView.draw(canvas);
            } finally {
                mAttachInfo.mIgnoreDirtyState = false;
                canvas.restoreToCount(saveCount);
            }
        }
        long over = System.currentTimeMillis();
        Log.d(TAG,"draw over at time:" + over);

        if (DBG_FPS) {
            long now = System.currentTimeMillis();
            if (sDrawTime != 0) {
                float duration = now - sDrawTime;
                duration = (gPrevDur + duration) / 2;
                gPrevDur = duration;
                duration = 1000 / duration;
                
                Log.w(TAG, "Show FPS: " + duration);
            }
            sDrawTime = now;
            
        }
    }

    public final static int DO_TRAVERSAL = 1000;
    public final static int DIE = 1001;
    public final static int RESIZED = 1002;
    public final static int RESIZED_REPORT = 1003;
    public final static int WINDOW_FOCUS_CHANGED = 1004;
    public final static int DISPATCH_KEY = 1005;
    public final static int DISPATCH_POINTER = 1006;
    public final static int DISPATCH_TRACKBALL = 1007;
    public final static int DISPATCH_APP_VISIBILITY = 1008;
    public final static int DISPATCH_GET_NEW_SURFACE = 1009;
    public final static int FINISHED_EVENT = 1010;
    public final static int DISPATCH_KEY_FROM_IME = 1011;
    public final static int FINISH_INPUT_CONNECTION = 1012;
    public final static int CHECK_FOCUS = 1013;
    public final static int CLOSE_SYSTEM_DIALOGS = 1014;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case View.AttachInfo.INVALIDATE_MSG:
            ((View) msg.obj).invalidate();
            break;
        case View.AttachInfo.INVALIDATE_RECT_MSG:
            final View.AttachInfo.InvalidateInfo info = (View.AttachInfo.InvalidateInfo) msg.obj;
            info.target.invalidate(info.left, info.top, info.right, info.bottom);
            info.release();
            break;
        case DO_TRAVERSAL:
//            if (mProfile) {
//                Debug.startMethodTracing("ViewRoot");
//            }

            performTraversals();

//            if (mProfile) {
//                Debug.stopMethodTracing();
//                mProfile = false;
//            }
            break;
        case FINISHED_EVENT:
            //handleFinishedEvent(msg.arg1, msg.arg2 != 0);
            Log.e(TAG, "FINISHED_EVENT is not handled now!");
            break;
        case DISPATCH_KEY:
//            if (LOCAL_LOGV) Log.v(
//                TAG, "Dispatching key "
//                + msg.obj + " to " + mView);
//            deliverKeyEvent((KeyEvent)msg.obj, msg.arg1 != 0);
            Log.e(TAG, "DISPATCH_KEY is not handled now!");
            break;
        case DISPATCH_POINTER: {
            MotionEvent event = (MotionEvent) msg.obj;
//            try {
//                deliverPointerEvent(event);
//            } finally {
//                event.recycle();
//                if (msg.arg1 != 0) {
//                    finishInputEvent();
//                }
//                if (LOCAL_LOGV || WATCH_POINTER) Log.i(TAG, "Done dispatching!");
//            }
            Log.e(TAG, "DISPATCH_POINTER is not handled now!");
        } break;
        case DISPATCH_TRACKBALL: {
            MotionEvent event = (MotionEvent) msg.obj;
//            try {
//                deliverTrackballEvent(event);
//            } finally {
//                event.recycle();
//                if (msg.arg1 != 0) {
//                    finishInputEvent();
//                }
//            }
            Log.e(TAG, "DISPATCH_TRACKBALL is not handled now!");
        } break;
        case DISPATCH_APP_VISIBILITY:
            //handleAppVisibility(msg.arg1 != 0);
            Log.e(TAG, "DISPATCH_APP_VISIBILITY is not handled now!");
            break;
        case DISPATCH_GET_NEW_SURFACE:
            //handleGetNewSurface();
            Log.e(TAG, "DISPATCH_GET_NEW_SURFACE is not handled now!");
            break;
        case RESIZED:
//            ResizedInfo ri = (ResizedInfo)msg.obj;
//
//            if (mWinFrame.width() == msg.arg1 && mWinFrame.height() == msg.arg2
//                    && mPendingContentInsets.equals(ri.coveredInsets)
//                    && mPendingVisibleInsets.equals(ri.visibleInsets)
//                    && ((ResizedInfo)msg.obj).newConfig == null) {
//                break;
//            }
            Log.e(TAG, "RESIZED is not handled now!");
            // fall through...
        case RESIZED_REPORT:
//            if (mAdded) {
//                Configuration config = ((ResizedInfo)msg.obj).newConfig;
//                if (config != null) {
//                    updateConfiguration(config, false);
//                }
//                mWinFrame.left = 0;
//                mWinFrame.right = msg.arg1;
//                mWinFrame.top = 0;
//                mWinFrame.bottom = msg.arg2;
//                mPendingContentInsets.set(((ResizedInfo)msg.obj).coveredInsets);
//                mPendingVisibleInsets.set(((ResizedInfo)msg.obj).visibleInsets);
//                if (msg.what == RESIZED_REPORT) {
//                    mReportNextDraw = true;
//                }
//
//                if (mView != null) {
//                    forceLayout(mView);
//                }
//                requestLayout();
//            }
            Log.e(TAG, "RESIZED_REPORT is not handled now!");
            break;
        case WINDOW_FOCUS_CHANGED: {
            if (mAdded) {
                boolean hasWindowFocus = msg.arg1 != 0;
                mAttachInfo.mHasWindowFocus = hasWindowFocus;
                if (hasWindowFocus) {
                    boolean inTouchMode = msg.arg2 != 0;
                    ensureTouchModeLocally(inTouchMode);
                }

                mLastWasImTarget = WindowManager.LayoutParams
                        .mayUseInputMethod(mWindowAttributes.flags);

//                InputMethodManager imm = InputMethodManager.peekInstance();
                if (mView != null) {
//                    if (hasWindowFocus && imm != null && mLastWasImTarget) {
//                        imm.startGettingWindowFocus(mView);
//                    }
                    mAttachInfo.mKeyDispatchState.reset();
                    mView.dispatchWindowFocusChanged(hasWindowFocus);
                }

                // Note: must be done after the focus change callbacks,
                // so all of the view state is set up correctly.
                if (hasWindowFocus) {
//                    if (imm != null && mLastWasImTarget) {
//                        imm.onWindowFocus(mView, mView.findFocus(),
//                                mWindowAttributes.softInputMode,
//                                !mHasHadWindowFocus, mWindowAttributes.flags);
//                    }
                    // Clear the forward bit.  We can just do this directly, since
                    // the window manager doesn't care about it.
                    mWindowAttributes.softInputMode &=
                            ~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION;
                    ((WindowManager.LayoutParams)mView.getLayoutParams())
                            .softInputMode &=
                                ~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION;
                    mHasHadWindowFocus = true;
                }

//                if (hasWindowFocus && mView != null) {
//                    sendAccessibilityEvents();
//                }
            }
        } break;
        case DIE:
            //doDie();
            Log.e(TAG, "DIE is not handled now!");
            break;
        case DISPATCH_KEY_FROM_IME: {
//            if (LOCAL_LOGV) Log.v(
//                TAG, "Dispatching key "
//                + msg.obj + " from IME to " + mView);
//            KeyEvent event = (KeyEvent)msg.obj;
//            if ((event.getFlags()&KeyEvent.FLAG_FROM_SYSTEM) != 0) {
//                // The IME is trying to say this event is from the
//                // system!  Bad bad bad!
//                event = KeyEvent.changeFlags(event,
//                        event.getFlags()&~KeyEvent.FLAG_FROM_SYSTEM);
//            }
//            deliverKeyEventToViewHierarchy((KeyEvent)msg.obj, false);
            Log.e(TAG, "DISPATCH_KEY_FROM_IME is not handled now!");
        } break;
        case FINISH_INPUT_CONNECTION: {
//            InputMethodManager imm = InputMethodManager.peekInstance();
//            if (imm != null) {
//                imm.reportFinishInputConnection((InputConnection)msg.obj);
//            }
            Log.e(TAG, "FINISH_INPUT_CONNECTION is not handled now!");
        } break;
        case CHECK_FOCUS: {
//            InputMethodManager imm = InputMethodManager.peekInstance();
//            if (imm != null) {
//                imm.checkFocus();
//            }
            Log.e(TAG, "CHECK_FOCUS is not handled now!");
        } break;
        case CLOSE_SYSTEM_DIALOGS: {
//            if (mView != null) {
//                mView.onCloseSystemDialogs((String)msg.obj);
//            }
            Log.e(TAG, "CLOSE_SYSTEM_DIALOGS is not handled now!");
        } break;
        }
    }
    
    /**
     * Something in the current window tells us we need to change the touch mode.  For
     * example, we are not in touch mode, and the user touches the screen.
     *
     * If the touch mode has changed, tell the window manager, and handle it locally.
     *
     * @param inTouchMode Whether we want to be in touch mode.
     * @return True if the touch mode changed and focus changed was changed as a result
     */
    boolean ensureTouchMode(boolean inTouchMode) {
        if (DBG) Log.d("touchmode", "ensureTouchMode(" + inTouchMode + "), current "
                + "touch mode is " + mAttachInfo.mInTouchMode);
        if (mAttachInfo.mInTouchMode == inTouchMode) return false;

        // tell the window manager
//        try {
//            sWindowSession.setInTouchMode(inTouchMode);
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
        WindowManagerImpl mWindowManager = (WindowManagerImpl)WindowManagerImpl.getDefault();
        mWindowManager.setInTouchMode(inTouchMode);

        // handle the change
        return ensureTouchModeLocally(inTouchMode);
    }
    
    /**
     * Ensure that the touch mode for this window is set, and if it is changing,
     * take the appropriate action.
     * @param inTouchMode Whether we want to be in touch mode.
     * @return True if the touch mode changed and focus changed was changed as a result
     */
    private boolean ensureTouchModeLocally(boolean inTouchMode) {
        if (DBG) Log.d("touchmode", "ensureTouchModeLocally(" + inTouchMode + "), current "
                + "touch mode is " + mAttachInfo.mInTouchMode);

        if (mAttachInfo.mInTouchMode == inTouchMode) return false;

        mAttachInfo.mInTouchMode = inTouchMode;
        mAttachInfo.mTreeObserver.dispatchOnTouchModeChanged(inTouchMode);

        return (inTouchMode) ? enterTouchMode() : leaveTouchMode();
    }
    
    private boolean enterTouchMode() {
        if (mView != null) {
            if (mView.hasFocus()) {
                // note: not relying on mFocusedView here because this could
                // be when the window is first being added, and mFocused isn't
                // set yet.
                final View focused = mView.findFocus();
                if (focused != null && !focused.isFocusableInTouchMode()) {

                    final ViewGroup ancestorToTakeFocus =
                            findAncestorToTakeFocusInTouchMode(focused);
                    if (ancestorToTakeFocus != null) {
                        // there is an ancestor that wants focus after its descendants that
                        // is focusable in touch mode.. give it focus
                        return ancestorToTakeFocus.requestFocus();
                    } else {
                        // nothing appropriate to have focus in touch mode, clear it out
                        mView.unFocus();
                        mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(focused, null);
                        mFocusedView = null;
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Find an ancestor of focused that wants focus after its descendants and is
     * focusable in touch mode.
     * @param focused The currently focused view.
     * @return An appropriate view, or null if no such view exists.
     */
    private ViewGroup findAncestorToTakeFocusInTouchMode(View focused) {
        ViewParent parent = focused.getParent();
        while (parent instanceof ViewGroup) {
            final ViewGroup vgParent = (ViewGroup) parent;
            if (vgParent.getDescendantFocusability() == ViewGroup.FOCUS_AFTER_DESCENDANTS
                    && vgParent.isFocusableInTouchMode()) {
                return vgParent;
            }
            if (vgParent.isRootNamespace()) {
                return null;
            } else {
                parent = vgParent.getParent();
            }
        }
        return null;
    }
    
    private boolean leaveTouchMode() {
        if (mView != null) {
            if (mView.hasFocus()) {
                // i learned the hard way to not trust mFocusedView :)
                mFocusedView = mView.findFocus();
                if (!(mFocusedView instanceof ViewGroup)) {
                    // some view has focus, let it keep it
                    return false;
                } else if (((ViewGroup)mFocusedView).getDescendantFocusability() !=
                        ViewGroup.FOCUS_AFTER_DESCENDANTS) {
                    // some view group has focus, and doesn't prefer its children
                    // over itself for focus, so let them keep it.
                    return false;
                }
            }

            // find the best view to give focus to in this brave new non-touch-mode
            // world
            final View focused = focusSearch(null, View.FOCUS_DOWN);
            if (focused != null) {
                return focused.requestFocus(View.FOCUS_DOWN);
            }
        }
        return false;
    }
    
    public void windowFocusChanged(boolean hasFocus, boolean inTouchMode) {
        Message msg = Message.obtain();
        msg.what = WINDOW_FOCUS_CHANGED;
        msg.arg1 = hasFocus ? 1 : 0;
        msg.arg2 = inTouchMode ? 1 : 0;
        sendMessage(msg);
    }
    
	public void reset() {
		Log.d(TAG, "In reset");
		viewRootList.remove(this);
		/**
		 @j2sNative
		 var rootView = document.getElementById(this.mViewRootID);
		 if (rootView != null) {
		 	rootView.parentNode.removeChild(rootView);
		 }
		 */
		{
		}
	}

    static RunQueue getRunQueue() {
        RunQueue rq = sRunQueues;
        if (rq != null) {
            return rq;
        }
        rq = new RunQueue();
        sRunQueues = rq;
        return rq;
    }

    /**
     * @hide
     */
    static final class RunQueue {
        private final ArrayList<HandlerAction> mActions = new ArrayList<HandlerAction>();

        void post(Runnable action) {
            postDelayed(action, 0);
        }

        void postDelayed(Runnable action, long delayMillis) {
            HandlerAction handlerAction = new HandlerAction();
            handlerAction.action = action;
            handlerAction.delay = delayMillis;

            synchronized (mActions) {
                mActions.add(handlerAction);
            }
        }

        void removeCallbacks(Runnable action) {
            final HandlerAction handlerAction = new HandlerAction();
            handlerAction.action = action;

            synchronized (mActions) {
                final ArrayList<HandlerAction> actions = mActions;

                while (actions.remove(handlerAction)) {
                    // Keep going
                }
            }
        }

        void executeActions(Handler handler) {
            synchronized (mActions) {
                final ArrayList<HandlerAction> actions = mActions;
                final int count = actions.size();

                for (int i = 0; i < count; i++) {
                    final HandlerAction handlerAction = actions.get(i);
                    handler.postDelayed(handlerAction.action, handlerAction.delay);
                }

                actions.clear();
            }
        }

        private static class HandlerAction {
            Runnable action;
            long delay;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                HandlerAction that = (HandlerAction) o;
                return !(action != null ? !action.equals(that.action) : that.action != null);

            }

            @Override
            public int hashCode() {
                int result = action != null ? action.hashCode() : 0;
                result = 31 * result + (int) (delay ^ (delay >>> 32));
                return result;
            }
        }
    }

    static class W extends Window {
        private final WeakReference<ViewRoot> mViewRoot;

        public W(ViewRoot viewRoot, Context context) {
            super(context);
            mViewRoot = new WeakReference<ViewRoot>(viewRoot);
        }

        public void resized(int w, int h, Rect coveredInsets,
                Rect visibleInsets, boolean reportDraw, Configuration newConfig) {
            final ViewRoot viewRoot = mViewRoot.get();
        }

        public void dispatchAppVisibility(boolean visible) {
            final ViewRoot viewRoot = mViewRoot.get();
        }

        public void dispatchGetNewSurface() {
            final ViewRoot viewRoot = mViewRoot.get();
        }

        public void windowFocusChanged(boolean hasFocus, boolean inTouchMode) {
            final ViewRoot viewRoot = mViewRoot.get();
        }

        private static int checkCallingPermission(String permission) {
            return 0;
        }

        public void executeCommand(String command, String parameters, ParcelFileDescriptor out) {
            final ViewRoot viewRoot = mViewRoot.get();
        }

        public void closeSystemDialogs(String reason) {
        }

        public void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep,
                boolean sync) {
        }

        public void dispatchWallpaperCommand(String action, int x, int y,
                int z, Bundle extras, boolean sync) {
        }
    }
}
