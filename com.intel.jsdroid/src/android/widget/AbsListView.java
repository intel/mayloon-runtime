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

package android.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import com.android.internal.R;
import com.intel.mpt.annotation.MayloonStubAnnotation;

/**
 * Base class that can be used to implement virtualized lists of items. A list does
 * not have a spatial definition here. For instance, subclases of this class can
 * display the content of the list in a grid, in a carousel, as stack, etc.
 *
 */
public abstract class AbsListView extends AdapterView<ListAdapter> {
	
    /**
     * Indicates that we are not in the middle of a touch gesture
     */
    static final int TOUCH_MODE_REST = -1;

    /**
     * Indicates we just received the touch event and we are waiting to see if the it is a tap or a
     * scroll gesture.
     */
    static final int TOUCH_MODE_DOWN = 0;

    /**
     * Indicates the touch has been recognized as a tap and we are now waiting to see if the touch
     * is a longpress
     */
    static final int TOUCH_MODE_TAP = 1;

    /**
     * Indicates we have waited for everything we can wait for, but the user's finger is still down
     */
    static final int TOUCH_MODE_DONE_WAITING = 2;

    /**
     * Indicates the touch gesture is a scroll
     */
    static final int TOUCH_MODE_SCROLL = 3;
    
    /**
     * Indicates the view is in the process of being flung
     */
    static final int TOUCH_MODE_FLING = 4;

    /**
     * Indicates the touch gesture is an overscroll - a scroll beyond the beginning or end.
     */
    static final int TOUCH_MODE_OVERSCROLL = 5;

    /**
     * Indicates the view is being flung outside of normal content bounds
     * and will spring back.
     */
    static final int TOUCH_MODE_OVERFLING = 6;	
	
	/**
	 * Regular layout - usually an unsolicited layout from the view system
	 */
	static final int LAYOUT_NORMAL = 0;

	/**
	 * Show the first item
	 */
	static final int LAYOUT_FORCE_TOP = 1;

	/**
	 * Force the selected item to be on somewhere on the screen
	 */
	static final int LAYOUT_SET_SELECTION = 2;

	/**
	 * Show the last item
	 */
	static final int LAYOUT_FORCE_BOTTOM = 3;

	/**
	 * Make a mSelectedItem appear in a specific location and build the rest of
	 * the views from there. The top is specified by mSpecificTop.
	 */
	static final int LAYOUT_SPECIFIC = 4;

	/**
	 * Layout to sync as a result of a data change. Restore mSyncPosition to have its top
	 * at mSpecificTop
	 */
	static final int LAYOUT_SYNC = 5;

	/**
	 * Layout as a result of using the navigation keys
	 */
	static final int LAYOUT_MOVE_SELECTION = 6;

	/**
	 * Controls how the next layout will happen
	 */
	int mLayoutMode = LAYOUT_NORMAL;

	/**
	 * Should be used by subclasses to listen to changes in the dataset
	 */
	AdapterDataSetObserver mDataSetObserver;

	/**
	 * The adapter containing the data to be displayed by this view
	 */
	ListAdapter mAdapter;

	/**
	 * Indicates whether the list selector should be drawn on top of the children or behind
	 */
	boolean mDrawSelectorOnTop = false;

    /**
     * The drawable used to draw the selector
     */
    Drawable mSelector;

    /**
     * Defines the selector's location and dimension at drawing time
     */
    Rect mSelectorRect = new Rect();
	/**
	 * The selection's left padding
	 */
	int mSelectionLeftPadding = 0;

	/**
	 * The selection's top padding
	 */
	int mSelectionTopPadding = 0;

	/**
	 * The selection's right padding
	 */
	int mSelectionRightPadding = 0;

	/**
	 * The selection's bottom padding
	 */
	int mSelectionBottomPadding = 0;

	/**
	 * This view's padding
	 */
	Rect mListPadding = new Rect();

	/**
	 * Subclasses must retain their measure spec from onMeasure() into this member
	 */
	int mWidthMeasureSpec = 0;
	
    /**
     * When the view is scrolling, this flag is set to true to indicate subclasses that
     * the drawing cache was enabled on the children
     */
    boolean mCachingStarted;

	/**
	 * Y value from on the previous motion event (if any)
	 */
	int mLastY;

	/**
	 * How far the finger moved before we started scrolling
	 */
	int mMotionCorrection;

	/**
	 * The offset in pixels form the top of the AdapterView to the top
	 * of the currently selected view. Used to save and restore state.
	 */
	int mSelectedTop = 0;

	/**
	 * Indicates whether the list is stacked from the bottom edge or
	 * the top edge.
	 */
	boolean mStackFromBottom;

	/**
	 * When set to true, the list automatically discards the children's
	 * bitmap cache after scrolling.
	 */
	boolean mScrollingCacheEnabled;

	/**
	 * Whether or not to enable the fast scroll feature on this list
	 */
	boolean mFastScrollEnabled;

	/**
	 * Optional callback to notify client when scroll position has changed
	 */
	private OnScrollListener mOnScrollListener;

	/**
	 * Used with type filter window
	 */
	EditText mTextFilter;

	/**
	 * Indicates whether to use pixels-based or position-based scrollbar
	 * properties.
	 */
	private boolean mSmoothScrollbarEnabled = true;

	/**
	 * The position to resurrect the selected position to.
	 */
	int mResurrectToPosition = INVALID_POSITION;

	/**
	 * Maximum distance to record overscroll
	 */
	int mOverscrollMax;

	/**
	 * Content height divided by this is the overscroll limit.
	 */
	static final int OVERSCROLL_LIMIT_DIVISOR = 3;

	/**
	 * The last scroll state reported to clients through {@link OnScrollListener}.
	 */
	private int mLastScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	private int mTouchSlop;
	
	/**
	 * Maximum distance to overscroll by during edge effects
	 */
	int mOverscrollDistance;

	/**
	 * Maximum distance to overfling during edge effects
	 */
	int mOverflingDistance;

    /**
     * The position of the view that received the down motion event
     */
    int mMotionPosition;
    
    /**
     * The offset to the top of the mMotionPosition view when the down motion event was received
     */
    int mMotionViewOriginalTop;

    /**
     * The desired offset to the top of the mMotionPosition view after a scroll
     */
    int mMotionViewNewTop;

    /**
     * The X value associated with the the down motion event
     */
    int mMotionX;

    /**
     * The Y value associated with the the down motion event
     */
    int mMotionY;

    /**
     * One of TOUCH_MODE_REST, TOUCH_MODE_DOWN, TOUCH_MODE_TAP, TOUCH_MODE_SCROLL, or
     * TOUCH_MODE_DONE_WAITING
     */
    int mTouchMode = TOUCH_MODE_REST;
    
    /**
     * Indicates that this list is always drawn on top of a solid, single-color, opaque
     * background
     */
    private int mCacheColorHint;
    
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;
    
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    
    /**
     * Acts upon click
     */
    private AbsListView.PerformClick mPerformClick;
    
    /**
     * The select child's view (from the adapter's getView) is enabled.
     */
    private boolean mIsChildViewEnabled;

    private ContextMenuInfo mContextMenuInfo = null;

	/**
	 * Interface definition for a callback to be invoked when the list or grid
	 * has been scrolled.
	 */
	public interface OnScrollListener {

		/**
		 * The view is not scrolling. Note navigating the list using the trackball counts as
		 * being in the idle state since these transitions are not animated.
		 */
		public static int SCROLL_STATE_IDLE = 0;

		/**
		 * The user is scrolling using touch, and their finger is still on the screen
		 */
		public static int SCROLL_STATE_TOUCH_SCROLL = 1;

		/**
		 * The user had previously been scrolling using touch and had performed a fling. The
		 * animation is now coasting to a stop
		 */
		public static int SCROLL_STATE_FLING = 2;

		/**
		 * Callback method to be invoked while the list view or grid view is being scrolled. If the
		 * view is being scrolled, this method will be called before the next frame of the scroll is
		 * rendered. In particular, it will be called before any calls to
		 * {@link Adapter#getView(int, View, ViewGroup)}.
		 *
		 * @param view The view whose scroll state is being reported
		 *
		 * @param scrollState The current scroll state. One of {@link #SCROLL_STATE_IDLE},
		 * {@link #SCROLL_STATE_TOUCH_SCROLL} or {@link #SCROLL_STATE_IDLE}.
		 */
		public void onScrollStateChanged(AbsListView view, int scrollState);

		/**
		 * Callback method to be invoked when the list or grid has been scrolled. This will be
		 * called after the scroll has completed
		 * @param view The view whose scroll state is being reported
		 * @param firstVisibleItem the index of the first visible cell (ignore if
		 *        visibleItemCount == 0)
		 * @param visibleItemCount the number of visible cells
		 * @param totalItemCount the number of items in the list adaptor
		 */
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount);
	}

	public AbsListView(Context context) {
		super(context);
		initAbsListView();

		setVerticalScrollBarEnabled(true);
	}

	public AbsListView(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.absListViewStyle);
	}

	public AbsListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAbsListView();

		TypedArray a = context.obtainStyledAttributes(attrs,
				com.android.internal.R.styleable.AbsListView, defStyle, 0);

		mDrawSelectorOnTop = a.getBoolean(
				com.android.internal.R.styleable.AbsListView_drawSelectorOnTop,
				false);

		boolean stackFromBottom = a.getBoolean(
				R.styleable.AbsListView_stackFromBottom, false);
		setStackFromBottom(stackFromBottom);

		boolean smoothScrollbar = a.getBoolean(
				R.styleable.AbsListView_smoothScrollbar, true);
		setSmoothScrollbarEnabled(smoothScrollbar);

		a.recycle();
	}

	private void initAbsListView() {
		// Setting focusable in touch mode will set the focusable property to true
		setClickable(true);
		setFocusableInTouchMode(true);
		setWillNotDraw(false);

		final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();
		mOverscrollDistance = configuration.getScaledOverscrollDistance();
		mOverflingDistance = configuration.getScaledOverflingDistance();
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTouchables(ArrayList<View> views) {
        final int count = getChildCount();
        final int firstPosition = mFirstPosition;
        final ListAdapter adapter = mAdapter;

        if (adapter == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (adapter.isEnabled(firstPosition + i)) {
                views.add(child);
            }
            child.addTouchables(views);
        }
    }

	/**
	 * @return true if all list content currently fits within the view boundaries
	 */
	private boolean contentFits() {
		final int childCount = getChildCount();
		if (childCount != mItemCount) {
			return false;
		}

		return getChildAt(0).getTop() >= 0
				&& getChildAt(childCount - 1).getBottom() <= mBottom;
	}

	/**
	 * Returns the current state of the fast scroll feature.
	 * @see #setFastScrollEnabled(boolean)
	 * @return true if fast scroll is enabled, false otherwise
	 */
	public boolean isFastScrollEnabled() {
		return mFastScrollEnabled;
	}

	/**
	 * When smooth scrollbar is enabled, the position and size of the scrollbar thumb
	 * is computed based on the number of visible pixels in the visible items. This
	 * however assumes that all list items have the same height. If you use a list in
	 * which items have different heights, the scrollbar will change appearance as the
	 * user scrolls through the list. To avoid this issue, you need to disable this
	 * property.
	 *
	 * When smooth scrollbar is disabled, the position and size of the scrollbar thumb
	 * is based solely on the number of items in the adapter and the position of the
	 * visible items inside the adapter. This provides a stable scrollbar as the user
	 * navigates through a list of items with varying heights.
	 *
	 * @param enabled Whether or not to enable smooth scrollbar.
	 *
	 * @see #setSmoothScrollbarEnabled(boolean)
	 * @attr ref android.R.styleable#AbsListView_smoothScrollbar
	 */
	public void setSmoothScrollbarEnabled(boolean enabled) {
		mSmoothScrollbarEnabled = enabled;
	}

	/**
	 * Returns the current state of the fast scroll feature.
	 *
	 * @return True if smooth scrollbar is enabled is enabled, false otherwise.
	 *
	 * @see #setSmoothScrollbarEnabled(boolean)
	 */
	public boolean isSmoothScrollbarEnabled() {
		return mSmoothScrollbarEnabled;
	}

	/**
	 * Set the listener that will receive notifications every time the list scrolls.
	 *
	 * @param l the scroll listener
	 */
	public void setOnScrollListener(OnScrollListener l) {
		mOnScrollListener = l;
		invokeOnItemScrollListener();
	}

	/**
	 * Notify our scroll listener (if there is one) of a change in scroll state
	 */
	void invokeOnItemScrollListener() {
		if (mOnScrollListener != null) {
			mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(),
					mItemCount);
		}
	}

	@Override
	public void getFocusedRect(Rect r) {
		View view = getSelectedView();
		if (view != null && view.getParent() == this) {
			// the focused rectangle of the selected view offset into the
			// coordinate space of this view.
			view.getFocusedRect(r);
			offsetDescendantRectToMyCoords(view, r);
		} else {
			// otherwise, just the norm
			super.getFocusedRect(r);
		}
	}

	/**
	 * Indicates whether the content of this view is pinned to, or stacked from,
	 * the bottom edge.
	 *
	 * @return true if the content is stacked from the bottom edge, false otherwise
	 */
	public boolean isStackFromBottom() {
		return mStackFromBottom;
	}

	/**
	 * When stack from bottom is set to true, the list fills its content starting from
	 * the bottom of the view.
	 *
	 * @param stackFromBottom true to pin the view's content to the bottom edge,
	 *        false to pin the view's content to the top edge
	 */
	public void setStackFromBottom(boolean stackFromBottom) {
		if (mStackFromBottom != stackFromBottom) {
			mStackFromBottom = stackFromBottom;
			requestLayoutIfNecessary();
		}
	}

	void requestLayoutIfNecessary() {
		if (getChildCount() > 0) {
			resetList();
			requestLayout();
			invalidate();
		}
	}

    @Override
    public void requestLayout() {
        if (!mBlockLayoutRequests && !mInLayout) {
            super.requestLayout();
        }
    }

	/**
	 * The list is empty. Clear everything out.
	 */
	void resetList() {
		removeAllViewsInLayout();
		mFirstPosition = 0;
		mDataChanged = false;
		mNeedSync = false;
		mOldSelectedPosition = INVALID_POSITION;
		mOldSelectedRowId = INVALID_ROW_ID;
		setSelectedPositionInt(INVALID_POSITION);
		setNextSelectedPositionInt(INVALID_POSITION);
		mSelectedTop = 0;
		mSelectorRect.setEmpty();
		invalidate();
	}

	@Override
	protected int computeVerticalScrollExtent() {
		final int count = getChildCount();
		if (count > 0) {
			if (mSmoothScrollbarEnabled) {
				int extent = count * 100;

				View view = getChildAt(0);
				final int top = view.getTop();
				int height = view.getHeight();
				if (height > 0) {
					extent += (top * 100) / height;
				}

				view = getChildAt(count - 1);
				final int bottom = view.getBottom();
				height = view.getHeight();
				if (height > 0) {
					extent -= ((bottom - getHeight()) * 100) / height;
				}

				return extent;
			} else {
				return 1;
			}
		}
		return 0;
	}

	@Override
	protected int computeVerticalScrollOffset() {
		final int firstPosition = mFirstPosition;
		final int childCount = getChildCount();
		if (firstPosition >= 0 && childCount > 0) {
			if (mSmoothScrollbarEnabled) {
				final View view = getChildAt(0);
				final int top = view.getTop();
				int height = view.getHeight();
				if (height > 0) {
					return Math.max(firstPosition
							* 100
							- (top * 100)
							/ height
							+ (int) ((float) mScrollY / getHeight()
									* mItemCount * 100), 0);
				}
			} else {
				int index;
				final int count = mItemCount;
				if (firstPosition == 0) {
					index = 0;
				} else if (firstPosition + childCount == count) {
					index = count;
				} else {
					index = firstPosition + childCount / 2;
				}
				return (int) (firstPosition + childCount
						* (index / (float) count));
			}
		}
		return 0;
	}

	@Override
	protected int computeVerticalScrollRange() {
		int result;
		if (mSmoothScrollbarEnabled) {
			result = Math.max(mItemCount * 100, 0);
			if (mScrollY != 0) {
				// Compensate for overscroll
				result += Math.abs((int) ((float) mScrollY / getHeight()
						* mItemCount * 100));
			}
		} else {
			result = mItemCount;
		}
		return result;
	}

    private void useDefaultSelector() {
        setSelector(getResources().getDrawable(
                com.android.internal.R.drawable.list_selector_background));
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSelector == null) {
            useDefaultSelector();
        }
        final Rect listPadding = mListPadding;
        listPadding.left = mSelectionLeftPadding + mPaddingLeft;
        listPadding.top = mSelectionTopPadding + mPaddingTop;
        listPadding.right = mSelectionRightPadding + mPaddingRight;
        listPadding.bottom = mSelectionBottomPadding + mPaddingBottom;
    }

	/**
	 * Subclasses should NOT override this method but
	 *  {@link #layoutChildren()} instead.
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		//		System.out.println("AbsListView::onLayout");
		mInLayout = true;
		if (changed) {
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				getChildAt(i).forceLayout();
			}
		}

		layoutChildren();
		mInLayout = false;

		mOverscrollMax = (b - t) / OVERSCROLL_LIMIT_DIVISOR;
	}

	/**
	 * Subclasses must override this method to layout their children.
	 */
	protected void layoutChildren() {
	}

	@Override
	public View getSelectedView() {
		if (mItemCount > 0 && mSelectedPosition >= 0) {
			return getChildAt(mSelectedPosition - mFirstPosition);
		} else {
			return null;
		}
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingTop()
	 * @see #getSelector()
	 *
	 * @return The top list padding.
	 */
	public int getListPaddingTop() {
		return mListPadding.top;
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingBottom()
	 * @see #getSelector()
	 *
	 * @return The bottom list padding.
	 */
	public int getListPaddingBottom() {
		return mListPadding.bottom;
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingLeft()
	 * @see #getSelector()
	 *
	 * @return The left list padding.
	 */
	public int getListPaddingLeft() {
		return mListPadding.left;
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingRight()
	 * @see #getSelector()
	 *
	 * @return The right list padding.
	 */
	public int getListPaddingRight() {
		return mListPadding.right;
	}

	/**
	 * Get a view and have it show the data associated with the specified
	 * position. This is called when we have already discovered that the view is
	 * not available for reuse in the recycle bin. The only choices left are
	 * converting an old view or making a new one.
	 *
	 * @param position The position to display
	 * 
	 * @return A view displaying the data associated with the specified position
	 */
	View obtainView(int position) {
		return mAdapter.getView(position, null, this);
	}

    void positionSelector(View sel) {
        final Rect selectorRect = mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
        positionSelector(selectorRect.left, selectorRect.top, selectorRect.right,
                selectorRect.bottom);

        final boolean isChildViewEnabled = mIsChildViewEnabled;
        if (sel.isEnabled() != isChildViewEnabled) {
            mIsChildViewEnabled = !isChildViewEnabled;
            refreshDrawableState();
        }
    }

    private void positionSelector(int l, int t, int r, int b) {
        mSelectorRect.set(l - mSelectionLeftPadding, t - mSelectionTopPadding, r
                + mSelectionRightPadding, b + mSelectionBottomPadding);
    }
  

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int saveCount = 0;
        final boolean clipToPadding = (mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK;
        if (clipToPadding) {
            saveCount = canvas.save();
            final int scrollX = mScrollX;
            final int scrollY = mScrollY;
            canvas.clipRect(scrollX + mPaddingLeft, scrollY + mPaddingTop,
                    scrollX + mRight - mLeft - mPaddingRight,
                    scrollY + mBottom - mTop - mPaddingBottom);
            mGroupFlags &= ~CLIP_TO_PADDING_MASK;
        }

        final boolean drawSelectorOnTop = mDrawSelectorOnTop;
        if (!drawSelectorOnTop) {
            drawSelector(canvas);
        }

        super.dispatchDraw(canvas);

        if (drawSelectorOnTop) {
            drawSelector(canvas);
        }

        if (clipToPadding) {
            canvas.restoreToCount(saveCount);
            mGroupFlags |= CLIP_TO_PADDING_MASK;
        }
    }
    
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (getChildCount() > 0) {
			mDataChanged = true;
			rememberSyncState();
		}
	}


    /**
     * @return True if the current touch mode requires that we draw the selector in the pressed
     *         state.
     */
    boolean touchModeDrawsInPressedState() {
        // FIXME use isPressed for this
        switch (mTouchMode) {
        case TOUCH_MODE_DOWN: // MayLoon Added
        case TOUCH_MODE_TAP:
        case TOUCH_MODE_DONE_WAITING:
            return true;
        default:
            return false;
        }
    }
    
    /**
     * Indicates whether this view is in a state where the selector should be drawn. This will
     * happen if we have focus but are not in touch mode, or we are in the middle of displaying
     * the pressed state for an item.
     *
     * @return True if the selector should be shown
     */
    boolean shouldShowSelector() {
        return (hasFocus() && !isInTouchMode()) || touchModeDrawsInPressedState();
    }
    
    private void drawSelector(Canvas canvas) {
        if (shouldShowSelector() && mSelectorRect != null && !mSelectorRect.isEmpty()) {
            final Drawable selector = mSelector;
            selector.setBounds(mSelectorRect);
            selector.draw(canvas);
        }
    }
    
    /**
     * Controls whether the selection highlight drawable should be drawn on top of the item or
     * behind it.
     *
     * @param onTop If true, the selector will be drawn on the item it is highlighting. The default
     *        is false.
     *
     * @attr ref android.R.styleable#AbsListView_drawSelectorOnTop
     */
    public void setDrawSelectorOnTop(boolean onTop) {
        mDrawSelectorOnTop = onTop;
    }
    
    /**
     * Set a Drawable that should be used to highlight the currently selected item.
     *
     * @param resID A Drawable resource to use as the selection highlight.
     *
     * @attr ref android.R.styleable#AbsListView_listSelector
     */
    public void setSelector(int resID) {
        setSelector(getResources().getDrawable(resID));
    }

    public void setSelector(Drawable sel) {
        if (mSelector != null) {
            mSelector.setCallback(null);
            unscheduleDrawable(mSelector);
        }
        mSelector = sel;
        Rect padding = new Rect();
        sel.getPadding(padding);
        mSelectionLeftPadding = padding.left;
        mSelectionTopPadding = padding.top;
        mSelectionRightPadding = padding.right;
        mSelectionBottomPadding = padding.bottom;
        sel.setCallback(this);
        sel.setState(getDrawableState());
    }

    /**
     * Returns the selector {@link android.graphics.drawable.Drawable} that is used to draw the
     * selection in the list.
     *
     * @return the drawable used to display the selector
     */
    public Drawable getSelector() {
        return mSelector;
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			if (!isEnabled()) {
				return true;
			}
			if (isClickable() && isPressed() && mSelectedPosition >= 0
					&& mAdapter != null
					&& mSelectedPosition < mAdapter.getCount()) {

				final View view = getChildAt(mSelectedPosition - mFirstPosition);
				if (view != null) {
					performItemClick(view, mSelectedPosition, mSelectedRowId);
					view.setPressed(false);
				}
				setPressed(false);
				return true;
			}
			break;
		}
		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Fires an "on scroll state changed" event to the registered
	 * {@link android.widget.AbsListView.OnScrollListener}, if any. The state change
	 * is fired only if the specified state is different from the previously known state.
	 *
	 * @param newState The new scroll state.
	 */
	void reportScrollStateChange(int newState) {
		if (newState != mLastScrollState) {
			if (mOnScrollListener != null) {
				mOnScrollListener.onScrollStateChanged(this, newState);
				mLastScrollState = newState;
			}
		}
	}

	/**
	 * Returns the number of header views in the list. Header views are special views
	 * at the top of the list that should not be recycled during a layout.
	 *
	 * @return The number of header views, 0 in the default implementation.
	 */
	int getHeaderViewsCount() {
		return 0;
	}

	/**
	 * Returns the number of footer views in the list. Footer views are special views
	 * at the bottom of the list that should not be recycled during a layout.
	 *
	 * @return The number of footer views, 0 in the default implementation.
	 */
	int getFooterViewsCount() {
		return 0;
	}

	/**
	 * Fills the gap left open by a touch-scroll. During a touch scroll, children that
	 * remain on screen are shifted and the other ones are discarded. The role of this
	 * method is to fill the gap thus created by performing a partial layout in the
	 * empty space.
	 *
	 * @param down true if the scroll is going down, false if it is going up
	 */
	abstract void fillGap(boolean down);

	void hideSelector() {
		if (mSelectedPosition != INVALID_POSITION) {
			if (mLayoutMode != LAYOUT_SPECIFIC) {
				mResurrectToPosition = mSelectedPosition;
			}
			if (mNextSelectedPosition >= 0
					&& mNextSelectedPosition != mSelectedPosition) {
				mResurrectToPosition = mNextSelectedPosition;
			}
			setSelectedPositionInt(INVALID_POSITION);
			setNextSelectedPositionInt(INVALID_POSITION);
			mSelectedTop = 0;
		}
	}

	/**
	 * @return A position to select. First we try mSelectedPosition. If that has been clobbered by
	 * entering touch mode, we then try mResurrectToPosition. Values are pinned to the range
	 * of items available in the adapter
	 */
	int reconcileSelectedPosition() {
		int position = mSelectedPosition;
		if (position < 0) {
			position = mResurrectToPosition;
		}
		position = Math.max(0, position);
		position = Math.min(position, mItemCount - 1);
		return position;
	}

	/**
	 * Find the row closest to y. This row will be used as the motion row when scrolling
	 *
	 * @param y Where the user touched
	 * @return The position of the first (or only) item in the row containing y
	 */
	abstract int findMotionRow(int y);

	/**
	 * Find the row closest to y. This row will be used as the motion row when scrolling.
	 * 
	 * @param y Where the user touched
	 * @return The position of the first (or only) item in the row closest to y
	 */
	int findClosestMotionRow(int y) {
		final int childCount = getChildCount();
		if (childCount == 0) {
			return INVALID_POSITION;
		}

		final int motionRow = findMotionRow(y);
		return motionRow != INVALID_POSITION ? motionRow : mFirstPosition
				+ childCount - 1;
	}

	/**
	 * Causes all the views to be rebuilt and redrawn.
	 */
	public void invalidateViews() {
		mDataChanged = true;
		rememberSyncState();
		requestLayout();
		invalidate();
	}

	/**
	 * Makes the item at the supplied position selected.
	 *
	 * @param position the position of the new selection
	 */
	abstract void setSelectionInt(int position);

	@Override
	protected void handleDataChanged() {
		int count = mItemCount;
		if (count > 0) {

			int newPos;

			int selectablePos;

			// Find the row we are supposed to sync to
			if (mNeedSync) {
				// Update this first, since setNextSelectedPositionInt inspects it
				mNeedSync = false;

				switch (mSyncMode) {
				case SYNC_SELECTED_POSITION:
					if (isInTouchMode()) {
						// We saved our state when not in touch mode. (We know this because
						// mSyncMode is SYNC_SELECTED_POSITION.) Now we are trying to
						// restore in touch mode. Just leave mSyncPosition as it is (possibly
						// adjusting if the available range changed) and return.
						mLayoutMode = LAYOUT_SYNC;
						mSyncPosition = Math.min(Math.max(0, mSyncPosition),
								count - 1);

						return;
					} else {
						// See if we can find a position in the new data with the same
						// id as the old selection. This will change mSyncPosition.
						newPos = findSyncPosition();
						if (newPos >= 0) {
							// Found it. Now verify that new selection is still selectable
							selectablePos = lookForSelectablePosition(newPos,
									true);
							if (selectablePos == newPos) {
								// Same row id is selected
								mSyncPosition = newPos;

								if (mSyncHeight == getHeight()) {
									// If we are at the same height as when we saved state, try
									// to restore the scroll position too.
									mLayoutMode = LAYOUT_SYNC;
								} else {
									// We are not the same height as when the selection was saved, so
									// don't try to restore the exact position
									mLayoutMode = LAYOUT_SET_SELECTION;
								}

								// Restore selection
								setNextSelectedPositionInt(newPos);
								return;
							}
						}
					}
					break;
				case SYNC_FIRST_POSITION:
					// Leave mSyncPosition as it is -- just pin to available range
					mLayoutMode = LAYOUT_SYNC;
					mSyncPosition = Math.min(Math.max(0, mSyncPosition),
							count - 1);

					return;
				}
			}

			if (!isInTouchMode()) {
				// We couldn't find matching data -- try to use the same position
				newPos = getSelectedItemPosition();

				// Pin position to the available range
				if (newPos >= count) {
					newPos = count - 1;
				}
				if (newPos < 0) {
					newPos = 0;
				}

				// Make sure we select something selectable -- first look down
				selectablePos = lookForSelectablePosition(newPos, true);

				if (selectablePos >= 0) {
					setNextSelectedPositionInt(selectablePos);
					return;
				} else {
					// Looking down didn't work -- try looking up
					selectablePos = lookForSelectablePosition(newPos, false);
					if (selectablePos >= 0) {
						setNextSelectedPositionInt(selectablePos);
						return;
					}
				}
			} else {

				// We already know where we want to resurrect the selection
				if (mResurrectToPosition >= 0) {
					return;
				}
			}

		}

		// Nothing is selected. Give up and reset everything.
		mLayoutMode = mStackFromBottom ? LAYOUT_FORCE_BOTTOM : LAYOUT_FORCE_TOP;
		mSelectedPosition = INVALID_POSITION;
		mSelectedRowId = INVALID_ROW_ID;
		mNextSelectedPosition = INVALID_POSITION;
		mNextSelectedRowId = INVALID_ROW_ID;
		mNeedSync = false;
		checkSelectionChanged();
	}

	/**
	 * What is the distance between the source and destination rectangles given the direction of
	 * focus navigation between them? The direction basically helps figure out more quickly what is
	 * self evident by the relationship between the rects...
	 *
	 * @param source the source rectangle
	 * @param dest the destination rectangle
	 * @param direction the direction
	 * @return the distance between the rectangles
	 */
	static int getDistance(Rect source, Rect dest, int direction) {
		int sX, sY; // source x, y
		int dX, dY; // dest x, y
		switch (direction) {
		case View.FOCUS_RIGHT:
			sX = source.right;
			sY = source.top + source.height() / 2;
			dX = dest.left;
			dY = dest.top + dest.height() / 2;
			break;
		case View.FOCUS_DOWN:
			sX = source.left + source.width() / 2;
			sY = source.bottom;
			dX = dest.left + dest.width() / 2;
			dY = dest.top;
			break;
		case View.FOCUS_LEFT:
			sX = source.left;
			sY = source.top + source.height() / 2;
			dX = dest.right;
			dY = dest.top + dest.height() / 2;
			break;
		case View.FOCUS_UP:
			sX = source.left + source.width() / 2;
			sY = source.top;
			dX = dest.left + dest.width() / 2;
			dY = dest.bottom;
			break;
		default:
			throw new IllegalArgumentException("direction must be one of "
					+ "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
		}
		int deltaX = dX - sX;
		int deltaY = dY - sY;
		return deltaY * deltaY + deltaX * deltaX;
	}

	/**
	 * For filtering we proxy an input connection to an internal text editor,
	 * and this allows the proxying to happen.
	 */
	@Override
	public boolean checkInputConnectionProxy(View view) {
		return view == mTextFilter;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(
			ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new AbsListView.LayoutParams(getContext(), attrs);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof AbsListView.LayoutParams;
	}

    /**
     * When set to a non-zero value, the cache color hint indicates that this list is always drawn
     * on top of a solid, single-color, opaque background
     *
     * @param color The background color
     */
    public void setCacheColorHint(int color) {
        if (color != mCacheColorHint) {
            mCacheColorHint = color;
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).setDrawingCacheBackgroundColor(color);
            }
            //mRecycler.setCacheColorHint(color);
        }
    }

    /**
     * When set to a non-zero value, the cache color hint indicates that this list is always drawn
     * on top of a solid, single-color, opaque background
     *
     * @return The cache color hint
     */
    public int getCacheColorHint() {
        return mCacheColorHint;
    }
    
	/**
	 * Move all views (excluding headers and footers) held by this AbsListView into the supplied
	 * List. This includes views displayed on the screen as well as views stored in AbsListView's
	 * internal view recycler.
	 *
	 * @param views A list into which to put the reclaimed views
	 */
	public void reclaimViews(List<View> views) {
		int childCount = getChildCount();

		// Reclaim views on screen
		for (int i = 0; i < childCount; i++) {
			View child = getChildAt(i);
			AbsListView.LayoutParams lp = (AbsListView.LayoutParams) child
					.getLayoutParams();
			// Don't reclaim header or footer views, or views that should be ignored
			if (lp != null) {
				views.add(child);
			}
		}
		removeAllViewsInLayout();
	}

	/**
	 * AbsListView extends LayoutParams to provide a place to hold the view type.
	 */
	public static class LayoutParams extends ViewGroup.LayoutParams {
		/**
		 * View type for this view, as returned by
		 * {@link android.widget.Adapter#getItemViewType(int) }
		 */
		int viewType;

		/**
		 * When this boolean is set, the view has been added to the AbsListView
		 * at least once. It is used to know whether headers/footers have already
		 * been added to the list view and whether they should be treated as
		 * recycled views or not.
		 */
		boolean recycledHeaderFooter;

		/**
		 * When an AbsListView is measured with an AT_MOST measure spec, it needs
		 * to obtain children views to measure itself. When doing so, the children
		 * are not attached to the window, but put in the recycler which assumes
		 * they've been attached before. Setting this flag will force the reused
		 * view to be attached to the window rather than just attached to the
		 * parent.
		 */
		boolean forceAdd;

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(int w, int h) {
			super(w, h);
		}

		public LayoutParams(int w, int h, int viewType) {
			super(w, h);
			this.viewType = viewType;
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
	}
	
    /**
     * Maps a point to a position in the list.
     *
     * @param x X in local coordinate
     * @param y Y in local coordinate
     * @return The position of the item which contains the specified point, or
     *         {@link #INVALID_POSITION} if the point does not intersect an item.
     */
    public int pointToPosition(int x, int y) {
    	Rect frame = new Rect();
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return mFirstPosition + i;
                }
            }
        }
        return INVALID_POSITION;
    }
    
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mSelector != null) {
            mSelector.setState(getDrawableState());
        }
    }
 
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        View v;

//        if (mFastScroller != null) {
//            boolean intercepted = mFastScroller.onInterceptTouchEvent(ev);
//            if (intercepted) {
//                return true;
//            }
//        }
        
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            int touchMode = mTouchMode;
            if (touchMode == TOUCH_MODE_OVERFLING || touchMode == TOUCH_MODE_OVERSCROLL) {
                mMotionCorrection = 0;
                return true;
            }
            
            final int x = (int) ev.getX();
            final int y = (int) ev.getY();
            mActivePointerId = ev.getPointerId(0);
            
            int motionPosition = findMotionRow(y);
            if (touchMode != TOUCH_MODE_FLING && motionPosition >= 0) {
                // User clicked on an actual view (and was not stopping a fling).
                // Remember where the motion event started
                v = getChildAt(motionPosition - mFirstPosition);
                mMotionViewOriginalTop = v.getTop();
                mMotionX = x;
                mMotionY = y;
                mMotionPosition = motionPosition;
                mTouchMode = TOUCH_MODE_DOWN;
                //clearScrollingCache();
            }
            mLastY = Integer.MIN_VALUE;
            if (touchMode == TOUCH_MODE_FLING) {
                return true;
            }
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            switch (mTouchMode) {
            case TOUCH_MODE_DOWN:
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final int y = (int) ev.getY(pointerIndex);
                if (startScrollIfNeeded(y - mMotionY)) {
                    return true;
                }
                break;
            }
            break;
        }

        case MotionEvent.ACTION_UP: {
            mTouchMode = TOUCH_MODE_REST;
            mActivePointerId = INVALID_POINTER;
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            break;
        }
        
        }

        return false;
    }

    private boolean startScrollIfNeeded(int deltaY) {
        // Check if we have moved far enough that it looks more like a
        // scroll than a tap
        final int distance = Math.abs(deltaY);
        final boolean overscroll = mScrollY != 0;
        if (/*overscroll || */distance > mTouchSlop) {
            //createScrollingCache();
            mTouchMode = /*overscroll ? TOUCH_MODE_OVERSCROLL : */TOUCH_MODE_SCROLL;
            mMotionCorrection = deltaY;
//            final Handler handler = getHandler();
//            // Handler should not be null unless the AbsListView is not attached to a
//            // window, which would make it very hard to scroll it... but the monkeys
//            // say it's possible.
//            if (handler != null) {
//                handler.removeCallbacks(mPendingCheckForLongPress);
//            }
            setPressed(false);
            View motionView = getChildAt(mMotionPosition - mFirstPosition);
            if (motionView != null) {
                motionView.setPressed(false);
            }
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            // Time to start stealing events! Once we've stolen them, don't let anyone
            // steal from us
            requestDisallowInterceptTouchEvent(true);
            return true;
        }

        return false;
    }
    
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return isClickable() || isLongClickable();
        }

        final int action = ev.getAction();

        View v;
        int deltaY;

        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            final int x = (int) ev.getX();
            final int y = (int) ev.getY();
            mMotionPosition = pointToPosition(x, y);
/*            switch (mTouchMode) {
            case TOUCH_MODE_OVERFLING: {
                mFlingRunnable.endFling();
                mTouchMode = TOUCH_MODE_OVERSCROLL;
                mMotionY = mLastY = (int) ev.getY();
                mMotionCorrection = 0;
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            default: {
                mActivePointerId = ev.getPointerId(0);
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                int motionPosition = pointToPosition(x, y);
                if (!mDataChanged) {
                    if ((mTouchMode != TOUCH_MODE_FLING) && (motionPosition >= 0)
                            && (getAdapter().isEnabled(motionPosition))) {
                        // User clicked on an actual view (and was not stopping a fling). It might be a
                        // click or a scroll. Assume it is a click until proven otherwise
                        mTouchMode = TOUCH_MODE_DOWN;
                        // FIXME Debounce
                        if (mPendingCheckForTap == null) {
                            mPendingCheckForTap = new CheckForTap();
                        }
                        postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                    } else {
                        if (ev.getEdgeFlags() != 0 && motionPosition < 0) {
                            // If we couldn't find a view to click on, but the down event was touching
                            // the edge, we will bail out and try again. This allows the edge correcting
                            // code in ViewRoot to try to find a nearby view to select
                            return false;
                        }

                        if (mTouchMode == TOUCH_MODE_FLING) {
                            // Stopped a fling. It is a scroll.
                            createScrollingCache();
                            mTouchMode = TOUCH_MODE_SCROLL;
                            mMotionCorrection = 0;
                            motionPosition = findMotionRow(y);
                            reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                        }
                    }
                }*/

                if (mMotionPosition >= 0) {
                    // Remember where the motion event started
                    v = getChildAt(mMotionPosition - mFirstPosition);
                    mMotionViewOriginalTop = v.getTop();

                    if (v != null && !v.hasFocusable()) {
                        if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
                            if (!mDataChanged && mAdapter.isEnabled(mMotionPosition)) {
                                setSelectedPositionInt(mMotionPosition);
                                v.setPressed(true);
                                positionSelector(v);
                                if (mSelector != null) {
                                    Drawable d = mSelector.getCurrent();
                                    if (d != null && d instanceof TransitionDrawable) {
                                        ((TransitionDrawable) d).resetTransition();
                                    }
                                }
                                invalidate();
                            } else {
                                mTouchMode = TOUCH_MODE_REST;
                            }
                            return true;
                        }
                    }

                }
                mMotionX = x;
                mMotionY = y;
                //mMotionPosition = motionPosition;
                mLastY = Integer.MIN_VALUE;
                mTouchMode = TOUCH_MODE_DOWN;
                break;
            /*}
            }*/
            //break;
        }

        case MotionEvent.ACTION_MOVE: {
            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
            final int y = (int) ev.getY(pointerIndex);
            deltaY = y - mMotionY;
            switch (mTouchMode) {
            case TOUCH_MODE_DOWN:
            case TOUCH_MODE_TAP:
            case TOUCH_MODE_DONE_WAITING:
                // Check if we have moved far enough that it looks more like a
                // scroll than a tap
                startScrollIfNeeded(deltaY);
                break;
            case TOUCH_MODE_SCROLL:
//                if (PROFILE_SCROLLING) {
//                    if (!mScrollProfilingStarted) {
//                        Debug.startMethodTracing("AbsListViewScroll");
//                        mScrollProfilingStarted = true;
//                    }
//                }

                if (y != mLastY) {
                    // We may be here after stopping a fling and continuing to scroll.
                    // If so, we haven't disallowed intercepting touch events yet.
                    // Make sure that we do so in case we're in a parent that can intercept.
                    if ((mGroupFlags & FLAG_DISALLOW_INTERCEPT) == 0 &&
                            Math.abs(deltaY) > mTouchSlop) {
                        requestDisallowInterceptTouchEvent(true);
                    }

                    final int rawDeltaY = deltaY;
                    deltaY -= mMotionCorrection;
                    int incrementalDeltaY = mLastY != Integer.MIN_VALUE ? y - mLastY : deltaY;
                    
                    final int motionIndex;
                    if (mMotionPosition >= 0) {
                        motionIndex = mMotionPosition - mFirstPosition;
                    } else {
                        // If we don't have a motion position that we can reliably track,
                        // pick something in the middle to make a best guess at things below.
                        motionIndex = getChildCount() / 2;
                    }

                    int motionViewPrevTop = 0;
                    View motionView = this.getChildAt(motionIndex);
                    if (motionView != null) {
                        motionViewPrevTop = motionView.getTop();
                    }

                    // No need to do all this work if we're not going to move anyway
                    boolean atEdge = false;
                    if (incrementalDeltaY != 0) {
                        atEdge = trackMotionScroll(deltaY, incrementalDeltaY);
                    }

                    // Check to see if we have bumped into the scroll limit
                    motionView = this.getChildAt(motionIndex);
                    if (motionView != null) {
                        // Check if the top of the motion view is where it is
                        // supposed to be
                        final int motionViewRealTop = motionView.getTop();
//                        if (atEdge) {
//                            // Apply overscroll
//
//                            int overscroll = -incrementalDeltaY -
//                                    (motionViewRealTop - motionViewPrevTop);
//                            overScrollBy(0, overscroll, 0, mScrollY, 0, 0,
//                                    0, mOverscrollDistance, true);
//                            if (Math.abs(mOverscrollDistance) == Math.abs(mScrollY)) {
//                                // Don't allow overfling if we're at the edge.
//                                mVelocityTracker.clear();
//                            }
//
//                            final int overscrollMode = getOverScrollMode();
//                            if (overscrollMode == OVER_SCROLL_ALWAYS ||
//                                    (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS &&
//                                            !contentFits())) {
//                                mDirection = 0; // Reset when entering overscroll.
//                                mTouchMode = TOUCH_MODE_OVERSCROLL;
//                            }
//                        }
                        mMotionY = y;
                        invalidate();
                    }
                    mLastY = y;
                }
                break;

//            case TOUCH_MODE_OVERSCROLL:
//                if (y != mLastY) {
//                    final int rawDeltaY = deltaY;
//                    deltaY -= mMotionCorrection;
//                    int incrementalDeltaY = mLastY != Integer.MIN_VALUE ? y - mLastY : deltaY;
//
//                    final int oldScroll = mScrollY;
//                    final int newScroll = oldScroll - incrementalDeltaY;
//                    int newDirection = y > mLastY ? 1 : -1;
//
//                    if (mDirection == 0) {
//                        mDirection = newDirection;
//                    }
//
//                    if (mDirection != newDirection) {
//                        // Coming back to 'real' list scrolling
//                        incrementalDeltaY = -newScroll;
//                        mScrollY = 0;
//
//                        // No need to do all this work if we're not going to move anyway
//                        if (incrementalDeltaY != 0) {
//                            trackMotionScroll(incrementalDeltaY, incrementalDeltaY);
//                        }
//
//                        // Check to see if we are back in
//                        View motionView = this.getChildAt(mMotionPosition - mFirstPosition);
//                        if (motionView != null) {
//                            mTouchMode = TOUCH_MODE_SCROLL;
//
//                            // We did not scroll the full amount. Treat this essentially like the
//                            // start of a new touch scroll
//                            final int motionPosition = findClosestMotionRow(y);
//
//                            mMotionCorrection = 0;
//                            motionView = getChildAt(motionPosition - mFirstPosition);
//                            mMotionViewOriginalTop = motionView.getTop();
//                            mMotionY = y;
//                            mMotionPosition = motionPosition;
//                        }
//                    } else {
//                        overScrollBy(0, -incrementalDeltaY, 0, mScrollY, 0, 0,
//                                0, mOverscrollDistance, true);
//                        final int overscrollMode = getOverScrollMode();
//                        if (overscrollMode == OVER_SCROLL_ALWAYS ||
//                                (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS &&
//                                        !contentFits())) {
//                            invalidate();
//                        }
//                        if (Math.abs(mOverscrollDistance) == Math.abs(mScrollY)) {
//                            // Don't allow overfling if we're at the edge.
//                            mVelocityTracker.clear();
//                        }
//                    }
//                    mLastY = y;
//                    mDirection = newDirection;
//                }
//                break;
            }
            break;
        }

        case MotionEvent.ACTION_UP: {
            final int x = (int) ev.getX();
            final int y = (int) ev.getY();
            mMotionPosition = pointToPosition(x, y);
            switch (mTouchMode) {
            case TOUCH_MODE_DOWN:
            case TOUCH_MODE_TAP:
            case TOUCH_MODE_DONE_WAITING:
                final int motionPosition = mMotionPosition;
                final View child = getChildAt(motionPosition - mFirstPosition);
                if (child != null && !child.hasFocusable()) {
                    if (mTouchMode != TOUCH_MODE_DOWN) {
                        child.setPressed(false);
                    }

                    if (mPerformClick == null) {
                        mPerformClick = new PerformClick();
                    }

                    final AbsListView.PerformClick performClick = mPerformClick;
                    performClick.mChild = child;
                    performClick.mClickMotionPosition = motionPosition;
                    performClick.rememberWindowAttachCount();

                    mResurrectToPosition = motionPosition;

                    if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
//                        final Handler handler = getHandler();
//                        if (handler != null) {
//                            handler.removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ?
//                                    mPendingCheckForTap : mPendingCheckForLongPress);
//                        }
                        mLayoutMode = LAYOUT_NORMAL;
                        if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
                            mTouchMode = TOUCH_MODE_TAP;
                            setSelectedPositionInt(mMotionPosition);
                            //layoutChildren();
                            child.setPressed(true);
                            positionSelector(child);
                            //avoid all list items turn orange
                            //setPressed(true);
                            if (mSelector != null) {
                                Drawable d = mSelector.getCurrent();
                                if (d != null && d instanceof TransitionDrawable) {
                                    ((TransitionDrawable) d).resetTransition();
                                }
                            }
                            invalidate();
                            
                            postDelayed(new Runnable() {
                                public void run() {
                                    child.setPressed(false);
                                    setPressed(false);
                                    setSelectedPositionInt(INVALID_POSITION);
                                    if (!mDataChanged) {
                                        post(performClick);
                                        child.clearFocus();
                                        invalidate();
                                    }
                                    mTouchMode = TOUCH_MODE_REST;
                                }
                            }, ViewConfiguration.getPressedStateDuration());
                        } else {
                            mTouchMode = TOUCH_MODE_REST;
                        }
                        return true;
                    } else if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
                        post(performClick);
                        child.clearFocus();
                        invalidate();
                    }
                }
                mTouchMode = TOUCH_MODE_REST;
                break;
            case TOUCH_MODE_SCROLL:
                /*final int childCount = getChildCount();
                if (childCount > 0) {
                    final int firstChildTop = getChildAt(0).getTop();
                    final int lastChildBottom = getChildAt(childCount - 1).getBottom();
                    final int contentTop = mListPadding.top;
                    final int contentBottom = getHeight() - mListPadding.bottom;
                    if (mFirstPosition == 0 && firstChildTop >= contentTop &&
                            mFirstPosition + childCount < mItemCount &&
                            lastChildBottom <= getHeight() - contentBottom) {
                        mTouchMode = TOUCH_MODE_REST;
                        reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    } else {
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        final int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);

                        // Fling if we have enough velocity and we aren't at a boundary.
                        // Since we can potentially overfling more than we can overscroll, don't
                        // allow the weird behavior where you can scroll to a boundary then
                        // fling further.
                        if (Math.abs(initialVelocity) > mMinimumVelocity &&
                                !((mFirstPosition == 0 &&
                                        firstChildTop == contentTop - mOverscrollDistance) ||
                                  (mFirstPosition + childCount == mItemCount &&
                                        lastChildBottom == contentBottom + mOverscrollDistance))) {
                            if (mFlingRunnable == null) {
                                mFlingRunnable = new FlingRunnable();
                            }
                            reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                            
                            mFlingRunnable.start(-initialVelocity);
                        } else {
                            mTouchMode = TOUCH_MODE_REST;
                            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                        }
                    }
                } else */{
                    mTouchMode = TOUCH_MODE_REST;
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }
                break;

            /*case TOUCH_MODE_OVERSCROLL:
                if (mFlingRunnable == null) {
                    mFlingRunnable = new FlingRunnable();
                }
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);

                reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                if (Math.abs(initialVelocity) > mMinimumVelocity) {
                    mFlingRunnable.startOverfling(-initialVelocity);
                } else {
                    mFlingRunnable.startSpringback();
                }

                break;*/
            }

            setPressed(false);

//            if (mEdgeGlowTop != null) {
//                mEdgeGlowTop.onRelease();
//                mEdgeGlowBottom.onRelease();
//            }

            // Need to redraw since we probably aren't drawing the selector anymore
            invalidate();

//            final Handler handler = getHandler();
//            if (handler != null) {
//                handler.removeCallbacks(mPendingCheckForLongPress);
//            }
//
//            if (mVelocityTracker != null) {
//                mVelocityTracker.recycle();
//                mVelocityTracker = null;
//            }
//            
//            mActivePointerId = INVALID_POINTER;
//
//            if (PROFILE_SCROLLING) {
//                if (mScrollProfilingStarted) {
//                    Debug.stopMethodTracing();
//                    mScrollProfilingStarted = false;
//                }
//            }
            break;
        }

        case MotionEvent.ACTION_CANCEL: {
//            switch (mTouchMode) {
//            case TOUCH_MODE_OVERSCROLL:
//                if (mFlingRunnable == null) {
//                    mFlingRunnable = new FlingRunnable();
//                }
//                mFlingRunnable.startSpringback();
//                break;
//
//            case TOUCH_MODE_OVERFLING:
//                // Do nothing - let it play out.
//                break;
//
//            default:
                mTouchMode = TOUCH_MODE_REST;
                setPressed(false);
                View motionView = this.getChildAt(mMotionPosition - mFirstPosition);
                if (motionView != null) {
                    motionView.setPressed(false);
                }
//                clearScrollingCache();
//
//                final Handler handler = getHandler();
//                if (handler != null) {
//                    handler.removeCallbacks(mPendingCheckForLongPress);
//                }
//
//                if (mVelocityTracker != null) {
//                    mVelocityTracker.recycle();
//                    mVelocityTracker = null;
//                }
//            }
//            
//            if (mEdgeGlowTop != null) {
//                mEdgeGlowTop.onRelease();
//                mEdgeGlowBottom.onRelease();
//            }
            mActivePointerId = INVALID_POINTER;
            break;
        }
        
/*        case MotionEvent.ACTION_POINTER_UP: {
            onSecondaryPointerUp(ev);
            final int x = mMotionX;
            final int y = mMotionY;
            final int motionPosition = pointToPosition(x, y);
            if (motionPosition >= 0) {
                // Remember where the motion event started
                v = getChildAt(motionPosition - mFirstPosition);
                mMotionViewOriginalTop = v.getTop();
                mMotionPosition = motionPosition;
            }
            mLastY = y;
            break;
        }*/
        }

        return super.onTouchEvent(ev);
    }

    /**
     * Track a motion scroll
     *
     * @param deltaY Amount to offset mMotionView. This is the accumulated delta since the motion
     *        began. Positive numbers mean the user's finger is moving down the screen.
     * @param incrementalDeltaY Change in deltaY from the previous event.
     * @return true if we're already at the beginning/end of the list and have nothing to do.
     */
    boolean trackMotionScroll(int deltaY, int incrementalDeltaY) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return true;
        }

        final int firstTop = getChildAt(0).getTop();
        final int lastBottom = getChildAt(childCount - 1).getBottom();

        final Rect listPadding = mListPadding;

         // FIXME account for grid vertical spacing too?
        final int spaceAbove = listPadding.top - firstTop;
        final int end = getHeight() - listPadding.bottom;
        final int spaceBelow = lastBottom - end;

        final int height = getHeight() - mPaddingBottom - mPaddingTop;
        if (deltaY < 0) {
            deltaY = Math.max(-(height - 1), deltaY);
        } else {
            deltaY = Math.min(height - 1, deltaY);
        }

        if (incrementalDeltaY < 0) {
            incrementalDeltaY = Math.max(-(height - 1), incrementalDeltaY);
        } else {
            incrementalDeltaY = Math.min(height - 1, incrementalDeltaY);
        }

        final int firstPosition = mFirstPosition;

        // Update our guesses for where the first and last views are
//        if (firstPosition == 0) {
//            mFirstPositionDistanceGuess = firstTop - mListPadding.top;
//        } else {
//            mFirstPositionDistanceGuess += incrementalDeltaY;
//        }
//        if (firstPosition + childCount == mItemCount) {
//            mLastPositionDistanceGuess = lastBottom + mListPadding.bottom;
//        } else {
//            mLastPositionDistanceGuess += incrementalDeltaY;
//        }

        if (firstPosition == 0 && firstTop >= listPadding.top && incrementalDeltaY >= 0) {
            // Don't need to move views down if the top of the first position
            // is already visible
            return incrementalDeltaY != 0;
        }

        if (firstPosition + childCount == mItemCount && lastBottom <= end &&
                incrementalDeltaY <= 0) {
            // Don't need to move views up if the bottom of the last position
            // is already visible
            return incrementalDeltaY != 0;
        }

        final boolean down = incrementalDeltaY < 0;

        final boolean inTouchMode = isInTouchMode();
        if (inTouchMode) {
            hideSelector();
        }

        final int headerViewsCount = getHeaderViewsCount();
        final int footerViewsStart = mItemCount - getFooterViewsCount();

        int start = 0;
        int count = 0;

        if (down) {
            final int top = listPadding.top - incrementalDeltaY;
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getBottom() >= top) {
                    break;
                } else {
                    count++;
                    int position = firstPosition + i;
                    if (position >= headerViewsCount && position < footerViewsStart) {
//                        mRecycler.addScrapView(child);

//                        if (ViewDebug.TRACE_RECYCLER) {
//                            ViewDebug.trace(child,
//                                    ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
//                                    firstPosition + i, -1);
//                        }
                    }
                }
            }
        } else {
            final int bottom = getHeight() - listPadding.bottom - incrementalDeltaY;
            for (int i = childCount - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                if (child.getTop() <= bottom) {
                    break;
                } else {
                    start = i;
                    count++;
                    int position = firstPosition + i;
                    if (position >= headerViewsCount && position < footerViewsStart) {
//                        mRecycler.addScrapView(child);
//
//                        if (ViewDebug.TRACE_RECYCLER) {
//                            ViewDebug.trace(child,
//                                    ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
//                                    firstPosition + i, -1);
//                        }
                    }
                }
            }
        }

        mMotionViewNewTop = mMotionViewOriginalTop + deltaY;

        mBlockLayoutRequests = true;

        if (count > 0) {
            detachViewsFromParent(start, count);
        }
        offsetChildrenTopAndBottom(incrementalDeltaY);

        if (down) {
            mFirstPosition += count;
        }
        
        invalidate();

        final int absIncrementalDeltaY = Math.abs(incrementalDeltaY);
        if (spaceAbove < absIncrementalDeltaY || spaceBelow < absIncrementalDeltaY) {
            fillGap(down);
        }

        if (!inTouchMode && mSelectedPosition != INVALID_POSITION) {
            final int childIndex = mSelectedPosition - mFirstPosition;
            if (childIndex >= 0 && childIndex < getChildCount()) {
                //positionSelector(getChildAt(childIndex));
            }
        }

        mBlockLayoutRequests = false;

        invokeOnItemScrollListener();
        awakenScrollBars();
        
        return false;
    }
    
    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getPositionForView(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = mAdapter.getItemId(longPressPosition);
            boolean handled = false;

            if (mOnItemLongClickListener != null) {
                handled = mOnItemLongClickListener.onItemLongClick(AbsListView.this, originalView,
                        longPressPosition, longPressId);
            }
            if (!handled) {
                mContextMenuInfo = createContextMenuInfo(
                        getChildAt(longPressPosition - mFirstPosition),
                        longPressPosition, longPressId);
                handled = super.showContextMenuForChild(originalView);
            }

            return handled;
        }
        return false;
    }

    /**
     * Creates the ContextMenuInfo returned from {@link #getContextMenuInfo()}. This
     * methods knows the view, position and ID of the item that received the
     * long press.
     *
     * @param view The view that received the long press.
     * @param position The position of the item that received the long press.
     * @param id The ID of the item that received the long press.
     * @return The extra information that should be returned by
     *         {@link #getContextMenuInfo()}.
     */
    ContextMenuInfo createContextMenuInfo(View view, int position, long id) {
        return new AdapterContextMenuInfo(view, position, id);
    }

    /**
     * @j2sNative
     * console.log("Missing method: afterTextChanged");
     */
    @MayloonStubAnnotation()
    public void afterTextChanged(Editable s) {
        System.out.println("Stub" + " Function : afterTextChanged");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setRecyclerListener");
     */
    @MayloonStubAnnotation()
    public void setRecyclerListener(Object listener) {
        System.out.println("Stub" + " Function : setRecyclerListener");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setFilterText");
     */
    @MayloonStubAnnotation()
    public void setFilterText(String filterText) {
        System.out.println("Stub" + " Function : setFilterText");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getTranscriptMode");
     */
    @MayloonStubAnnotation()
    public int getTranscriptMode() {
        System.out.println("Stub" + " Function : getTranscriptMode");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: smoothScrollToPosition");
     */
    @MayloonStubAnnotation()
    public void smoothScrollToPosition(int position, int boundPosition) {
        System.out.println("Stub" + " Function : smoothScrollToPosition");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setScrollingCacheEnabled");
     */
    @MayloonStubAnnotation()
    public void setScrollingCacheEnabled(boolean enabled) {
        System.out.println("Stub" + " Function : setScrollingCacheEnabled");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setFastScrollEnabled");
     */
    @MayloonStubAnnotation()
    public void setFastScrollEnabled(boolean enabled) {
        System.out.println("Stub" + " Function : setFastScrollEnabled");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onFilterComplete");
     */
    @MayloonStubAnnotation()
    public void onFilterComplete(int count) {
        System.out.println("Stub" + " Function : onFilterComplete");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clearTextFilter");
     */
    @MayloonStubAnnotation()
    public void clearTextFilter() {
        System.out.println("Stub" + " Function : clearTextFilter");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onTouchModeChanged");
     */
    @MayloonStubAnnotation()
    public void onTouchModeChanged(boolean isInTouchMode) {
        System.out.println("Stub" + " Function : onTouchModeChanged");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isTextFilterEnabled");
     */
    @MayloonStubAnnotation()
    public boolean isTextFilterEnabled() {
        System.out.println("Stub" + " Function : isTextFilterEnabled");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: hasTextFilter");
     */
    @MayloonStubAnnotation()
    public boolean hasTextFilter() {
        System.out.println("Stub" + " Function : hasTextFilter");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isScrollingCacheEnabled");
     */
    @MayloonStubAnnotation()
    public boolean isScrollingCacheEnabled() {
        System.out.println("Stub" + " Function : isScrollingCacheEnabled");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clear");
     */
    @MayloonStubAnnotation()
    void clear() {
        System.out.println("Stub" + " Function : clear");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setTranscriptMode");
     */
    @MayloonStubAnnotation()
    public void setTranscriptMode(int mode) {
        System.out.println("Stub" + " Function : setTranscriptMode");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setTextFilterEnabled");
     */
    @MayloonStubAnnotation()
    public void setTextFilterEnabled(boolean textFilterEnabled) {
        System.out.println("Stub" + " Function : setTextFilterEnabled");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: smoothScrollToPosition");
     */
    @MayloonStubAnnotation()
    public void smoothScrollToPosition(int position) {
        System.out.println("Stub" + " Function : smoothScrollToPosition");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getTextFilter");
     */
    @MayloonStubAnnotation()
    public CharSequence getTextFilter() {
        System.out.println("Stub" + " Function : getTextFilter");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onGlobalLayout");
     */
    @MayloonStubAnnotation()
    public void onGlobalLayout() {
        System.out.println("Stub" + " Function : onGlobalLayout");
        return;
    }

    /**
     * A base class for Runnables that will check that their view is still attached to
     * the original window as when the Runnable was created.
     *
     */
    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return hasWindowFocus() && getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        View mChild;
        int mClickMotionPosition;

        public void run() {
            // The data has changed since we posted this action in the event queue,
            // bail out before bad things happen
            if (mDataChanged) return;

            final ListAdapter adapter = mAdapter;
            final int motionPosition = mClickMotionPosition;
            if (adapter != null && mChild != null)
/*            if (adapter != null && mItemCount > 0 &&
                    motionPosition != INVALID_POSITION &&
                    motionPosition < adapter.getCount() && sameWindow())*/ {
                performItemClick(mChild, motionPosition, adapter.getItemId(motionPosition));
            }
        }
    }
}
