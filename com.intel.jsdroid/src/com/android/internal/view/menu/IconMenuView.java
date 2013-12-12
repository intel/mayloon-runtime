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

package com.android.internal.view.menu;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.BaseSavedState;

import com.android.internal.view.menu.MenuItemImpl;
import com.android.internal.view.menu.IconMenuItemView;
import com.android.internal.view.menu.MenuBuilder.ItemInvoker;
import com.android.internal.view.menu.MenuBuilder;


/**
 * The icon menu view is an icon-based menu usually with a subset of all the menu items.
 * It is opened as the default menu, and shows either the first five or all six of the menu items
 * with text and icon.  In the situation of there being more than six items, the first five items
 * will be accompanied with a 'More' button that opens an {@link ExpandedMenuView} which lists
 * all the menu items. 
 * 
 * @attr ref android.R.styleable#IconMenuView_rowHeight
 * @attr ref android.R.styleable#IconMenuView_maxRows
 * @attr ref android.R.styleable#IconMenuView_maxItemsPerRow
 * 
 * @hide
 */
public final class IconMenuView extends ViewGroup implements ItemInvoker,
		MenuView {
	private MenuBuilder mMenu;

	/** Height of each row */
	private int mRowHeight;
	/** Maximum number of rows to be shown */
	private int mMaxRows;
	/** Maximum number of items to show in the icon menu. */
	private int mMaxItems;
	/** Maximum number of items per row */
	private int mMaxItemsPerRow;
	/** Actual number of items (the 'More' view does not count as an item) shown */
	private int mNumActualItemsShown;

    /** Divider that is drawn between all rows */
    private Drawable mHorizontalDivider;
	/** Height of the horizontal divider */
	private int mHorizontalDividerHeight;
    /** Set of horizontal divider positions where the horizontal divider will be drawn */
    private ArrayList<Rect> mHorizontalDividerRects;

    /** Divider that is drawn between all columns */
    private Drawable mVerticalDivider;
	/** Width of the vertical divider */
	private int mVerticalDividerWidth;
    
    /** Set of vertical divider positions where the vertical divider will be drawn */
    private ArrayList<Rect> mVerticalDividerRects;
    
    /** Icon for the 'More' button */
    private Drawable mMoreIcon;
    
    /** Item view for the 'More' button */
    private IconMenuItemView mMoreItemView;

    /** Background of each item (should contain the selected and focused states) */
    private Drawable mItemBackground;

	/** Default animations for this menu */
	private int mAnimations;

	/**
	 * Whether this IconMenuView has stale children and needs to update them.
	 * Set true by {@link #markStaleChildren()} and reset to false by
	 * {@link #onMeasure(int, int)}
	 */
	private boolean mHasStaleChildren;

	/**
	 * Longpress on MENU (while this is shown) switches to shortcut caption
	 * mode. When the user releases the longpress, we do not want to pass the
	 * key-up event up since that will dismiss the menu.
	 */
	private boolean mMenuBeingLongpressed = false;

	/**
	 * The layout to use for menu items. Each index is the row number (0 is the
	 * top-most). Each value contains the number of items in that row.
	 * <p>
	 * The length of this array should not be used to get the number of rows in
	 * the current layout, instead use {@link #mLayoutNumRows}.
	 */
	private int[] mLayout;

	/**
	 * The number of rows in the current layout. 
	 */
	private int mLayoutNumRows;

    /**
     * Instantiates the IconMenuView that is linked with the provided Menu.
     */
    public IconMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = Context.getSystemContext().getResources();
        TypedArray a = 
            context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.IconMenuView, 0, 0);
        mRowHeight = a.getDimensionPixelSize(com.android.internal.R.styleable.IconMenuView_rowHeight, 64);
        mMaxRows = a.getInt(com.android.internal.R.styleable.IconMenuView_maxRows, 2);
        mMaxItems = a.getInt(com.android.internal.R.styleable.IconMenuView_maxItems, 6);
        mMaxItemsPerRow = a.getInt(com.android.internal.R.styleable.IconMenuView_maxItemsPerRow, 3);
        mMoreIcon = res.getDrawable(com.android.internal.R.drawable.ic_menu_more);
        a.recycle();

        a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.MenuView, 0, 0);
        mItemBackground = res.getDrawable(com.android.internal.R.drawable.menu_selector);
        mHorizontalDivider = res.getDrawable(com.android.internal.R.drawable.divider_horizontal_dark);
        mHorizontalDividerRects = new ArrayList<Rect>();
        mVerticalDivider =  res.getDrawable(com.android.internal.R.drawable.divider_vertical_dark);
        mVerticalDividerRects = new ArrayList<Rect>();
        mAnimations = a.getResourceId(com.android.internal.R.styleable.MenuView_windowAnimationStyle, 0);
        a.recycle();
        
        if (mHorizontalDivider != null) {
            mHorizontalDividerHeight = mHorizontalDivider.getIntrinsicHeight();
            // Make sure to have some height for the divider
            if (mHorizontalDividerHeight == -1) mHorizontalDividerHeight = 1;
        }
        
        if (mVerticalDivider != null) {
            mVerticalDividerWidth = mVerticalDivider.getIntrinsicWidth();
            // Make sure to have some width for the divider
            if (mVerticalDividerWidth == -1) mVerticalDividerWidth = 1;
        }

        mLayout = new int[mMaxRows];

		// This view will be drawing the dividers        
		setWillNotDraw(false);

		// This is so we'll receive the MENU key in touch mode
		setFocusableInTouchMode(true);
		// This is so our children can still be arrow-key focused
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
	}

	/**
	 * Figures out the layout for the menu items.
	 * 
	 * @param width The available width for the icon menu.
	 */
	private void layoutItems(int width) {
        int numItems = getChildCount();
		if (numItems == 0) {
			mLayoutNumRows = 0;
			return;
		}

		// Start with the least possible number of rows
		int curNumRows = Math.min(
				(int) Math.ceil(numItems / (float) mMaxItemsPerRow), mMaxRows);

		/*
		 * Increase the number of rows until we find a configuration that fits
		 * all of the items' titles. Worst case, we use mMaxRows.
		 */
		for (; curNumRows <= mMaxRows; curNumRows++) {
			layoutItemsUsingGravity(curNumRows, numItems);

			if (curNumRows >= numItems) {
				// Can't have more rows than items
				break;
			}

			if (doItemsFit()) {
				// All the items fit, so this is a good configuration
				break;
			}
		}
	}

	/**
	 * Figures out the layout for the menu items by equally distributing, and
	 * adding any excess items equally to lower rows.
	 * 
	 * @param numRows The total number of rows for the menu view
	 * @param numItems The total number of items (across all rows) contained in
	 *            the menu view
	 * @return int[] Where the value of index i contains the number of items for row i
	 */
	private void layoutItemsUsingGravity(int numRows, int numItems) {
		int numBaseItemsPerRow = numItems / numRows;
		int numLeftoverItems = numItems % numRows;
		/**
		 * The bottom rows will each get a leftover item. Rows (indexed at 0)
		 * that are >= this get a leftover item. Note: if there are 0 leftover
		 * items, no rows will get them since this value will be greater than
		 * the last row.
		 */
		int rowsThatGetALeftoverItem = numRows - numLeftoverItems;

		int[] layout = mLayout;
		for (int i = 0; i < numRows; i++) {
			layout[i] = numBaseItemsPerRow;

			// Fill the bottom rows with a leftover item each
			if (i >= rowsThatGetALeftoverItem) {
				layout[i]++;
			}
		}

		mLayoutNumRows = numRows;
	}

	/**
	 * Checks whether each item's title is fully visible using the current
	 * layout.
	 * 
	 * @return True if the items fit (each item's text is fully visible), false
	 *         otherwise.
	 */
	private boolean doItemsFit() {
		int itemPos = 0;

		int[] layout = mLayout;
		int numRows = mLayoutNumRows;
		for (int row = 0; row < numRows; row++) {
			int numItemsOnRow = layout[row];

			/*
			 * If there is only one item on this row, increasing the
			 * number of rows won't help.
			 */
			if (numItemsOnRow == 1) {
				itemPos++;
				continue;
			}

			for (int itemsOnRowCounter = numItemsOnRow; itemsOnRowCounter > 0; itemsOnRowCounter--) {
                View child = getChildAt(itemPos++);
				LayoutParams lp = (LayoutParams) child.getLayoutParams();
				if (lp.maxNumItemsOnRow < numItemsOnRow) {
					return false;
				}
			}
		}

		return true;
	}

    /**
     * Adds an IconMenuItemView to this icon menu view.
     * 
     * @param itemView The item's view to add
     */
    private void addItemView(IconMenuItemView itemView) {
        // Set ourselves on the item view
        itemView.setIconMenuView(this);
        // Apply the background to the item view
        itemView.setBackgroundDrawable(
                mItemBackground.getConstantState().newDrawable(
                        getContext().getResources()));
        // This class is the invoker for all its item views
        itemView.setItemInvoker(this);

        // System.out.println("addItemView, before addView");
        addView(itemView, itemView.getTextAppropriateLayoutParams());
        // System.out.println("addItemView, after addView");
    }

    /**
     * Creates the item view for the 'More' button which is used to switch to
     * the expanded menu view. This button is a special case since it does not
     * have a MenuItemData backing it.
     * @return The IconMenuItemView for the 'More' button
     */
    private IconMenuItemView createMoreItemView() {
        LayoutInflater inflater = mMenu.getMenuType(MenuBuilder.TYPE_ICON).getInflater();
        
        final IconMenuItemView itemView = (IconMenuItemView) inflater.inflate(
                com.android.internal.R.layout.icon_menu_item_layout, null);
        
        Resources r = getContext().getResources();
        itemView.initialize(r.getText(com.android.internal.R.string.more_item_label), mMoreIcon);
        
        // Set up a click listener on the view since there will be no invocation sequence
        // due to the lack of a MenuItemData this view
        itemView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Switches the menu to expanded mode
                MenuBuilder.Callback cb = mMenu.getCallback();
                if (cb != null) {
                    // Call callback
                    cb.onMenuModeChange(mMenu);
                }
            }
        });
        
        return itemView;
    }

	public void initialize(MenuBuilder menu, int menuType) {
		mMenu = menu;
		updateChildren(true);
	}

	public void updateChildren(boolean cleared) {
		// This method does a clear refresh of children
		removeAllViews();

        final ArrayList<MenuItemImpl> itemsToShow = mMenu.getVisibleItems();
		final int numItems = itemsToShow.size();
		final int numItemsThatCanFit = mMaxItems;
		// Minimum of the num that can fit and the num that we have
		final int minFitMinus1AndNumItems = Math.min(numItemsThatCanFit - 1,
				numItems);

		MenuItemImpl itemData;
		// Traverse through all but the last item that can fit since that last item can either
		// be a 'More' button or a sixth item
		for (int i = 0; i < minFitMinus1AndNumItems; i++) {
			itemData = itemsToShow.get(i);
			addItemView((IconMenuItemView) itemData.getItemView(MenuBuilder.TYPE_ICON,
					this));
		}

        if (numItems > numItemsThatCanFit) {
            // If there are more items than we can fit, show the 'More' button to
            // switch to expanded mode
            if (mMoreItemView == null) {
                mMoreItemView = createMoreItemView();
            }
            
            addItemView(mMoreItemView);
            
            // The last view is the more button, so the actual number of items is one less than
            // the number that can fit
            mNumActualItemsShown = numItemsThatCanFit - 1;
        } else if (numItems == numItemsThatCanFit) {
            // There are exactly the number we can show, so show the last item
            final MenuItemImpl lastItemData = itemsToShow
                    .get(numItemsThatCanFit - 1);
            addItemView((IconMenuItemView) lastItemData.getItemView(
                    MenuBuilder.TYPE_ICON, this));

            // The items shown fit exactly
            mNumActualItemsShown = numItemsThatCanFit;
        }
	}

	/**
	 * The positioning algorithm that gets called from onMeasure.  It
	 * just computes positions for each child, and then stores them in the child's layout params.
	 * @param menuWidth The width of this menu to assume for positioning
	 * @param menuHeight The height of this menu to assume for positioning
	 */
	private void positionChildren(int menuWidth, int menuHeight) {
        // Clear the containers for the positions where the dividers should be drawn
        if (mHorizontalDivider != null) mHorizontalDividerRects.clear();
        if (mVerticalDivider != null) mVerticalDividerRects.clear();

		// Get the minimum number of rows needed
		final int numRows = mLayoutNumRows;
        final int numRowsMinus1 = numRows - 1;
		final int numItemsForRow[] = mLayout;

		// The item position across all rows
		int itemPos = 0;
		View child;
		IconMenuView.LayoutParams childLayoutParams = null;

		// Use float for this to get precise positions (uniform item widths
		// instead of last one taking any slack), and then convert to ints at last opportunity
		float itemLeft;
		float itemTop = 0;
		// Since each row can have a different number of items, this will be computed per row
		float itemWidth;
		// Subtract the space needed for the horizontal dividers
		final float itemHeight = (menuHeight - mHorizontalDividerHeight
				* (numRows - 1))
				/ (float) numRows;

		for (int row = 0; row < numRows; row++) {
			// Start at the left
			itemLeft = 0;

			// Subtract the space needed for the vertical dividers, and divide by the number of items
			itemWidth = (menuWidth - mVerticalDividerWidth
					* (numItemsForRow[row] - 1))
					/ (float) numItemsForRow[row];

			for (int itemPosOnRow = 0; itemPosOnRow < numItemsForRow[row]; itemPosOnRow++) {
				// Tell the child to be exactly this size
                child = getChildAt(itemPos);
				child.measure(MeasureSpec.makeMeasureSpec((int) itemWidth,
						MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
						(int) itemHeight, MeasureSpec.EXACTLY));

				// Remember the child's position for layout
				childLayoutParams = (IconMenuView.LayoutParams) child
						.getLayoutParams();
				childLayoutParams.left = (int) itemLeft;
				childLayoutParams.right = (int) (itemLeft + itemWidth);
				childLayoutParams.top = (int) itemTop;
				childLayoutParams.bottom = (int) (itemTop + itemHeight);

				// Increment by item width
				itemLeft += itemWidth;
				itemPos++;

                // Add a vertical divider to draw
                if (mVerticalDivider != null) {
                    mVerticalDividerRects.add(new Rect((int) itemLeft,
                            (int) itemTop, (int) (itemLeft + mVerticalDividerWidth),
                            (int) (itemTop + itemHeight)));
                }
				// Increment by divider width (even if we're not computing
				// dividers, since we need to leave room for them when
				// calculating item positions)
				itemLeft += mVerticalDividerWidth;
			}

			// Last child on each row should extend to very right edge
			if (childLayoutParams != null) {
				childLayoutParams.right = menuWidth;
			}

			itemTop += itemHeight;

            // Add a horizontal divider to draw
            if ((mHorizontalDivider != null) && (row < numRowsMinus1)) {
                mHorizontalDividerRects.add(new Rect(0, (int) itemTop, menuWidth,
                        (int) (itemTop + mHorizontalDividerHeight)));

                itemTop += mHorizontalDividerHeight;
            }
		}
	}

	private boolean mFirst = true;

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		System.out.println("IconMenuView::onMeasure");
		if (mFirst == false) {
			View v = null;
			for (int i = 0; i < mMenu.getItems().size(); ++i) {
                v = getChildAt(i);
				if (mMenu.getItem(i).isVisible() == true) {
					v.setVisibility(View.VISIBLE);
				} else {
					v.setVisibility(View.GONE);
				}
			}
		}

		int measuredWidth = resolveSize(Integer.MAX_VALUE, widthMeasureSpec);

		/**
		System.out.println(getUIElementID() + " onMeasure");
		int wspecMode = MeasureSpec.getMode(widthMeasureSpec);
		int wspecSize = MeasureSpec.getSize(widthMeasureSpec);
		System.out.println("WidthSpec is: " + (wspecMode >> 30) + ", " + wspecSize);
		int hspecMode = MeasureSpec.getMode(heightMeasureSpec);
		int hspecSize = MeasureSpec.getSize(heightMeasureSpec);
		System.out.println("HeightSpec is: " + (hspecMode >> 30) + ", " + hspecSize);
		/**/

		calculateItemFittingMetadata(measuredWidth);
		layoutItems(measuredWidth);

		// Get the desired height of the icon menu view (last row of items does
		// not have a divider below)
		final int layoutNumRows = mLayoutNumRows;
		final int desiredHeight = (mRowHeight + mHorizontalDividerHeight)
				* layoutNumRows - mHorizontalDividerHeight;

		// Maximum possible width and desired height
		setMeasuredDimension(measuredWidth,
				resolveSize(desiredHeight, heightMeasureSpec));

		//		System.out.println("IconMenuView measuredHeight is " + mMeasuredHeight
		//				+ ", mRowHeight " + mRowHeight + ", mLayoutNumRows "
		//				+ mLayoutNumRows);

		// Position the children
		if (layoutNumRows > 0) {
			positionChildren(mMeasuredWidth, mMeasuredHeight);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		System.out.println("IconMenuView::onLayout");
		View child;
		IconMenuView.LayoutParams childLayoutParams;

        for (int i = getChildCount() - 1; i >= 0; i--) {
            child = getChildAt(i);
			childLayoutParams = (IconMenuView.LayoutParams) child
					.getLayoutParams();

			// Layout children according to positions set during the measure
			child.layout(childLayoutParams.left, childLayoutParams.top,
					childLayoutParams.right, childLayoutParams.bottom);
		}

		mFirst = false;
	}

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = mHorizontalDivider;
        if (drawable != null) {
            // If we have a horizontal divider to draw, draw it at the remembered positions
            final ArrayList<Rect> rects = mHorizontalDividerRects;
            for (int i = rects.size() - 1; i >= 0; i--) {
                drawable.setBounds(rects.get(i));
                drawable.draw(canvas);
            }
        }

        drawable = mVerticalDivider;
        if (drawable != null) {
            // If we have a vertical divider to draw, draw it at the remembered positions
            final ArrayList<Rect> rects = mVerticalDividerRects;
            for (int i = rects.size() - 1; i >= 0; i--) {
                drawable.setBounds(rects.get(i));
                drawable.draw(canvas);
            }
        }
    }

	public boolean invokeItem(MenuItemImpl item) {
		return mMenu.performItemAction(item, 0);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new IconMenuView.LayoutParams(getContext(), attrs);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		// Override to allow type-checking of LayoutParams. 
		return p instanceof IconMenuView.LayoutParams;
	}

	/**
	 * Marks as having stale children.
	 */
	void markStaleChildren() {
		if (!mHasStaleChildren) {
			mHasStaleChildren = true;
			requestLayout();
		}
	}

	/**
	 * @return The number of actual items shown (those that are backed by an
	 *         {@link MenuView.ItemView} implementation--eg: excludes More
	 *         item).
	 */
	int getNumActualItemsShown() {
		return mNumActualItemsShown;
	}

	public int getWindowAnimations() {
		return mAnimations;
	}

	/**
	 * Returns the number of items per row.
	 * <p>
	 * This should only be used for testing.
	 * 
	 * @return The length of the array is the number of rows. A value at a
	 *         position is the number of items in that row.
	 * @hide
	 */
	public int[] getLayout() {
		return mLayout;
	}

	/**
	 * Returns the number of rows in the layout.
	 * <p>
	 * This should only be used for testing.
	 * 
	 * @return The length of the array is the number of rows. A value at a
	 *         position is the number of items in that row.
	 * @hide
	 */
	public int getLayoutNumRows() {
		return mLayoutNumRows;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
			if (event.getAction() == KeyEvent.ACTION_DOWN
					&& event.getRepeatCount() == 0) {
				//                removeCallbacks(this);
			} else if (event.getAction() == KeyEvent.ACTION_UP) {

				if (mMenuBeingLongpressed) {
					// It was in cycle mode, so reset it (will also remove us
					// from being called back)
					return true;

				} else {
					// Just remove us from being called back
					//                    removeCallbacks(this);
					// Fall through to normal processing too
				}
			}
		}

		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		requestFocus();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
	}

	/**
	 * For each item, calculates the most dense row that fully shows the item's
	 * title.
	 * 
	 * @param width The available width of the icon menu.
	 */
	private void calculateItemFittingMetadata(int width) {
		int maxNumItemsPerRow = mMaxItemsPerRow;
        int numItems = getChildCount();
        for (int i = 0; i < numItems; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i)
                    .getLayoutParams();
			// Start with 1, since that case does not get covered in the loop below
			lp.maxNumItemsOnRow = 1;
			for (int curNumItemsPerRow = maxNumItemsPerRow; curNumItemsPerRow > 0; curNumItemsPerRow--) {
				// Check whether this item can fit into a row containing curNumItemsPerRow
				if (lp.desiredWidth < width / curNumItemsPerRow) {
					// It can, mark this value as the most dense row it can fit into
					lp.maxNumItemsOnRow = curNumItemsPerRow;
					break;
				}
			}
		}
	}

	/**
	 * Layout parameters specific to IconMenuView (stores the left, top, right, bottom from the
	 * measure pass). 
	 */
	public static class LayoutParams extends ViewGroup.MarginLayoutParams {
		int left, top, right, bottom;
		int desiredWidth;
		int maxNumItemsOnRow;

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}
	}

	@Override
	public View focusSearch(View v, int direction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void childDrawableStateChanged(View child) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addView(View view, android.view.ViewGroup.LayoutParams params) {
		addView(view, params, true);
	}

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        View focusedView = getFocusedChild();

        for (int i = getChildCount() - 1; i >= 0; i--) {
            if (getChildAt(i) == focusedView) {
                return new SavedState(superState, i);
            }
        }

        return new SavedState(superState, -1);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.focusedPosition >= getChildCount()) {
            return;
        }

        View v = getChildAt(ss.focusedPosition);
        if (v != null) {
            v.requestFocus();
        }
    }

    private static class SavedState extends BaseSavedState {
        int focusedPosition;

        /**
         * Constructor called from {@link IconMenuView#onSaveInstanceState()}
         */
        public SavedState(Parcelable superState, int focusedPosition) {
            super(superState);
            this.focusedPosition = focusedPosition;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            focusedPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(focusedPosition);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

    }
}
