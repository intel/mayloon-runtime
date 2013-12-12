package android.view;

import android.graphics.PixelFormat;

public class WindowManagerImpl extends WindowManager {
	private static final String TAG = "WindowManagerImpl";
    /**
     * The user is navigating with keys (not the touch screen), so
     * navigational focus should be shown.
     */
    public static final int RELAYOUT_IN_TOUCH_MODE = 0x1;
	private static WindowManagerImpl mWindowManager = null;
	private boolean mInTouchMode = false;

	public static WindowManagerImpl getDefault() {
		if (mWindowManager == null)
			mWindowManager = new WindowManagerImpl();
		return mWindowManager;
	}

	private View[] mViews;
	private ViewRoot[] mRoots;
	private WindowManager.LayoutParams[] mParams;
	private Display mDisplay = new Display(0);

	public Display getDefaultDisplay() {
		return mDisplay;
	}

	public void addView(View view) {
		addView(view, new WindowManager.LayoutParams(
				WindowManager.LayoutParams.TYPE_APPLICATION, 0,
				PixelFormat.OPAQUE));
	}

	public void addView(View view, ViewGroup.LayoutParams params) {
		addView(view, params, false);
	}

	public void addViewNesting(View view, ViewGroup.LayoutParams params) {
		addView(view, params, false);
	}

	private void addView(View view, ViewGroup.LayoutParams params, boolean nest) {
		
		final WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;

		ViewRoot root;
		View panelParentView = null;

		// Here's an odd/questionable case: if someone tries to add a
		// view multiple times, then we simply bump up a nesting count
		// and they need to remove the view the corresponding number of
		// times to have it actually removed from the window manager.
		// This is useful specifically for the notification manager,
		// which can continually add/remove the same view as a
		// notification gets updated.
		int index = findViewLocked(view, false);
		if (index >= 0) {
			if (!nest) {
				throw new IllegalStateException("View " + view
						+ " has already been added to the window manager.");
			}
			root = mRoots[index];
			root.mAddNesting++;
			// Update layout parameters.
			view.setLayoutParams(wparams);
			root.setLayoutParams(wparams, true);
			return;
		}

		// If this is a panel window, then find the window it is being
		// attached to for future reference.
		if (wparams.type >= WindowManager.LayoutParams.FIRST_SUB_WINDOW
				&& wparams.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
			System.out.println(TAG + "should not go here");
			final int count = mViews != null ? mViews.length : 0;
			for (int i = 0; i < count; i++) {
//									if (mRoots[i].mWindow.asBinder() == wparams.token) {
//										panelParentView = mViews[i];
//									}
			}
		}

		root = new ViewRoot(view.getContext());
		root.mAddNesting = 1;

		view.setLayoutParams(wparams);

		if (mViews == null) {
			index = 1;
			mViews = new View[1];
			mRoots = new ViewRoot[1];
			mParams = new WindowManager.LayoutParams[1];
		} else {
			index = mViews.length + 1;
			Object[] old = mViews;
			mViews = new View[index];
			System.arraycopy(old, 0, mViews, 0, index - 1);
			old = mRoots;
			mRoots = new ViewRoot[index];
			System.arraycopy(old, 0, mRoots, 0, index - 1);
			old = mParams;
			mParams = new WindowManager.LayoutParams[index];
			System.arraycopy(old, 0, mParams, 0, index - 1);
		}
		index--;

		mViews[index] = view;
		mRoots[index] = root;
		mParams[index] = wparams;

		// do this last because it fires off messages to start doing things
		try {
			root.setView(view, wparams, panelParentView);
			
		} catch (Exception e) {
		}
	}

	private int findViewLocked(View view, boolean required) {
		final int count = mViews != null ? mViews.length : 0;
		for (int i = 0; i < count; i++) {
			if (mViews[i] == view) {
				return i;
			}
		}
		if (required) {
			throw new IllegalArgumentException(
					"View not attached to window manager");
		}
		return -1;
	}

	public void updateViewLayout(View view,
			android.view.ViewGroup.LayoutParams params) {
		if (!(params instanceof WindowManager.LayoutParams)) {
			throw new IllegalArgumentException(
					"Params must be WindowManager.LayoutParams");
		}

		final WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;

		view.setLayoutParams(wparams);

		int index = findViewLocked(view, true);
		ViewRoot root = mRoots[index];
		mParams[index] = wparams;
		root.setLayoutParams(wparams, false);
	}

	public void removeView(View view) {
		int index = findViewLocked(view, true);
		View curView = removeViewLocked(index);
		if (curView == view) {
			return;
		}

		throw new IllegalStateException("Calling with view " + view
				+ " but the ViewRoot is attached to " + curView);
	}

	View removeViewLocked(int index) {
		ViewRoot root = mRoots[index];
		View view = root.getView();

		// Don't really remove until we have matched all calls to add().
		root.mAddNesting--;
		if (root.mAddNesting > 0) {
			return view;
		}

		root.die(false);
		finishRemoveViewLocked(view, index);
		return view;
	}

	void finishRemoveViewLocked(View view, int index) {
		final int count = mViews.length;

		// remove it from the list
		View[] tmpViews = new View[count - 1];
		removeItem(tmpViews, mViews, index);
		mViews = tmpViews;

		ViewRoot[] tmpRoots = new ViewRoot[count - 1];
		removeItem(tmpRoots, mRoots, index);
		mRoots = tmpRoots;

		WindowManager.LayoutParams[] tmpParams = new WindowManager.LayoutParams[count - 1];
		removeItem(tmpParams, mParams, index);
		mParams = tmpParams;

		view.assignParent(null, true);
		// func doesn't allow null...  does it matter if we clear them?
//		view.setLayoutParams(null);
	}

	private static void removeItem(Object[] dst, Object[] src, int index) {
		if (dst.length > 0) {
			if (index > 0) {
				System.arraycopy(src, 0, dst, 0, index);
			}
			if (index < dst.length) {
				System.arraycopy(src, index + 1, dst, index, src.length - index
						- 1);
			}
		}
	}

	public WindowManager.LayoutParams getRootViewLayoutParameter(View view) {
		ViewParent vp = view.getParent();
		while (vp != null && !(vp instanceof ViewRoot)) {
			vp = vp.getParent();
		}

		if (vp == null)
			return null;

		ViewRoot vr = (ViewRoot) vp;

		int N = mRoots.length;
		for (int i = 0; i < N; ++i) {
			if (mRoots[i] == vr) {
				return mParams[i];
			}
		}

		return null;
	}

	//	public void onKeyDown(int keyCode) {
	//		switch (keyCode) {
	//		//		case 70:
	//		//			// f: tooggle Menu
	//		//			if (null != mStaticWD) {
	//		//				mStaticWD.togglePanel(Window.FEATURE_OPTIONS_PANEL, null);
	//		//			}
	//		//			break;
	//		default:
	//			// System.out.println("keyCode is " + keyCode);
	//			break;
	//		}
	//	}

	public void removeViewImmediate(View view) {
		int index = findViewLocked(view, true);
		ViewRoot root = mRoots[index];
		View curView = root.getView();

		root.mAddNesting = 0;
		root.die(true);
		finishRemoveViewLocked(curView, index);
		if (curView == view) {
			return;
		}

		throw new IllegalStateException("Calling with view " + view
				+ " but the ViewRoot is attached to " + curView);
	}
	
	public class ToastView extends View{
		
	}


    public void setInTouchMode(boolean inTouch) {
        mInTouchMode = inTouch;
    }
    
    public boolean getInTouchMode() {
        return mInTouchMode;
    }
}
