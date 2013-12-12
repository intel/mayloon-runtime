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

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/**
 * FrameLayout is designed to block out an area on the screen to display a
 * single item. You can add multiple children to a FrameLayout, but all children
 * are pegged to the top left of the screen. Children are drawn in a stack, with
 * the most recently added child on top. The size of the frame layout is the
 * size of its largest child (plus padding), visible or not (if the
 * FrameLayout's parent permits). Views that are GONE are used for sizing only
 * if {@link #setMeasureAllChildren(boolean)
 * setConsiderGoneChildrenWhenMeasuring()} is set to true.
 * 
 * @attr ref android.R.styleable#FrameLayout_foreground
 * @attr ref android.R.styleable#FrameLayout_foregroundGravity
 * @attr ref android.R.styleable#FrameLayout_measureAllChildren
 */
public class FrameLayout extends ViewGroup {
	boolean mMeasureAllChildren = false;

	private Drawable mForeground = null;

	private int mForegroundPaddingLeft = 0;

	private int mForegroundPaddingTop = 0;

	private int mForegroundPaddingRight = 0;

	private int mForegroundPaddingBottom = 0;

	/** {@hide} */
	protected boolean mForegroundInPadding = true;

	boolean mForegroundBoundsChanged = false;

	public FrameLayout(Context context) {
		super(context);
	}

	public FrameLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs,
				com.android.internal.R.styleable.FrameLayout, defStyle, 0);

		//		final Drawable d = null;//TODO
		//		a.getDrawable(com.android.internal.R.styleable.FrameLayout_foreground);
		//		if (d != null) {
		//			setForeground(d);
		//		}

		if (a.getBoolean(
				com.android.internal.R.styleable.FrameLayout_measureAllChildren,
				false)) {
			setMeasureAllChildren(true);
		}

		mForegroundInPadding = a
				.getBoolean(
						com.android.internal.R.styleable.FrameLayout_foregroundInsidePadding,
						true);

		a.recycle();
	}

	/**
	 * Describes how the foreground is positioned. Defaults to FILL.
	 * 
	 * @param foregroundGravity
	 *            See {@link android.view.Gravity}
	 * 
	 * @attr ref android.R.styleable#FrameLayout_foregroundGravity
	 */

	//	public void setForegroundGravity(int foregroundGravity) {
	//		if (mForegroundGravity != foregroundGravity) {
	//			if ((foregroundGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
	//				foregroundGravity |= Gravity.LEFT;
	//			}
	//
	//			if ((foregroundGravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
	//				foregroundGravity |= Gravity.TOP;
	//			}
	//
	//			mForegroundGravity = foregroundGravity;
	//
	//			if (mForegroundGravity == Gravity.FILL && mForeground != null) {
	//				Rect padding = new Rect();
	//				if (mForeground.getPadding(padding)) {
	//					mForegroundPaddingLeft = padding.left;
	//					mForegroundPaddingTop = padding.top;
	//					mForegroundPaddingRight = padding.right;
	//					mForegroundPaddingBottom = padding.bottom;
	//				}
	//			} else {
	//				mForegroundPaddingLeft = 0;
	//				mForegroundPaddingTop = 0;
	//				mForegroundPaddingRight = 0;
	//				mForegroundPaddingBottom = 0;
	//			}
	//
	//			requestLayout();
	//		}
	//	}

	/**
	 * {@inheritDoc}
	 */
	public boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who) || (who == mForeground);
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawableStateChanged() {
		super.drawableStateChanged();
		if (mForeground != null && mForeground.isStateful()) {
			mForeground.setState(getDrawableState());
		}
	}

	/**
	 * Returns a set of layout parameters with a width of
	 * {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}, and a height of
	 * {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}.
	 */
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
	}

	/**
	 * Supply a Drawable that is to be rendered on top of all of the child views
	 * in the frame layout. Any padding in the Drawable will be taken into
	 * account by ensuring that the children are inset to be placed inside of
	 * the padding area.
	 * 
	 * @param drawable
	 *            The Drawable to be drawn on top of the children.
	 * 
	 * @attr ref android.R.styleable#FrameLayout_foreground
	 */
	public void setForeground(Drawable drawable) {
		// TODO
	}

	/**
	 * Returns the drawable used as the foreground of this FrameLayout. The
	 * foreground drawable, if non-null, is always drawn on top of the children.
	 * 
	 * @return A Drawable or null if no foreground was set.
	 */
	public Drawable getForeground() {
		return mForeground;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int count = getChildCount();
		int maxHeight = 0;
		int maxWidth = 0;

		// Find rightmost and bottommost child
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				measureChildWithMargins(child, widthMeasureSpec, 0,
						heightMeasureSpec, 0);
				maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
				maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
			}
		}

		// Account for padding too
		maxWidth += mPaddingLeft + mPaddingRight + mForegroundPaddingLeft
				+ mForegroundPaddingRight;
		maxHeight += mPaddingTop + mPaddingBottom + mForegroundPaddingTop
				+ mForegroundPaddingBottom;

		// Check against our minimum height and width
		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

		setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
				resolveSize(maxHeight, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		final int count = getChildCount();

		final int parentLeft = mPaddingLeft + mForegroundPaddingLeft;
		final int parentRight = right - left - mPaddingRight
				- mForegroundPaddingRight;

		final int parentTop = mPaddingTop + mForegroundPaddingTop;
		final int parentBottom = bottom - top - mPaddingBottom
				- mForegroundPaddingBottom;

		mForegroundBoundsChanged = true;

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				/**
				 @j2sNative 
				 if (isNaN(lp.leftMargin))
				 	lp.leftMargin = 0;
				 if (isNaN(lp.rightMargin))
				 	lp.rightMargin = 0;
				 if (isNaN(lp.topMargin)) 
				 	lp.topMargin = 0;
				 if (isNaN(lp.bottomMargin))
				 	lp.bottomMargin = 0;
				 */
				{
				}

				final int width = child.getMeasuredWidth();
				final int height = child.getMeasuredHeight();

				int childLeft = parentLeft;
				int childTop = parentTop;

				final int gravity = lp.gravity;

				if (gravity != -1) {
					final int horizontalGravity = gravity
							& Gravity.HORIZONTAL_GRAVITY_MASK;
					final int verticalGravity = gravity
							& Gravity.VERTICAL_GRAVITY_MASK;

					switch (horizontalGravity) {
					case Gravity.LEFT:
						childLeft = parentLeft + lp.leftMargin;
						break;
					case Gravity.CENTER_HORIZONTAL:
						childLeft = parentLeft
								+ (parentRight - parentLeft - width) / 2
								+ lp.leftMargin - lp.rightMargin;
						break;
					case Gravity.RIGHT:
						childLeft = parentRight - width - lp.rightMargin;
						break;
					default:
						childLeft = parentLeft + lp.leftMargin;
					}

					switch (verticalGravity) {
					case Gravity.TOP:
						childTop = parentTop + lp.topMargin;
						break;
					case Gravity.CENTER_VERTICAL:
						childTop = parentTop
								+ (parentBottom - parentTop - height) / 2
								+ lp.topMargin - lp.bottomMargin;
						break;
					case Gravity.BOTTOM:
						childTop = parentBottom - height - lp.bottomMargin;
						break;
					default:
						childTop = parentTop + lp.topMargin;
					}
				}

				child.layout(childLeft, childTop, childLeft + width, childTop
						+ height);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mForegroundBoundsChanged = true;
	}

	/**
	 * Determines whether to measure all children or just those in the VISIBLE
	 * or INVISIBLE state when measuring. Defaults to false.
	 * 
	 * @param measureAll
	 *            true to consider children marked GONE, false otherwise.
	 *            Default value is false.
	 * 
	 * @attr ref android.R.styleable#FrameLayout_measureAllChildren
	 */
	public void setMeasureAllChildren(boolean measureAll) {
		mMeasureAllChildren = measureAll;
	}

	/**
	 * Determines whether to measure all children or just those in the VISIBLE
	 * or INVISIBLE state when measuring.
	 */
	public boolean getConsiderGoneChildrenWhenMeasuring() {
		return mMeasureAllChildren;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new FrameLayout.LayoutParams(getContext(), attrs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(
			ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	/**
	 * Per-child layout information for layouts that support margins. See
	 * {@link com.intel.jsdroid.sample.R.styleable#FrameLayout_Layout
	 * FrameLayout Layout Attributes} for a list of all child view attributes
	 * that this class supports.
	 */
	public static class LayoutParams extends MarginLayoutParams {
		/**
		 * The gravity to apply with the View to which these layout parameters
		 * are associated.
		 * 
		 * @see android.view.Gravity
		 */
		public int gravity = -1;

		/**
		 * {@inheritDoc}
		 */
		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);

			TypedArray a = c.obtainStyledAttributes(attrs,
					com.android.internal.R.styleable.FrameLayout_Layout);
			gravity = a
					.getInt(com.android.internal.R.styleable.FrameLayout_Layout_layout_gravity,
							-1);
			a.recycle();
		}

		/**
		 * {@inheritDoc}
		 */
		public LayoutParams(int width, int height) {
			super(width, height);
		}

		/**
		 * Creates a new set of layout parameters with the specified width,
		 * height and weight.
		 * 
		 * @param width
		 *            the width, either {@link #MATCH_PARENT},
		 *            {@link #WRAP_CONTENT} or a fixed size in pixels
		 * @param height
		 *            the height, either {@link #MATCH_PARENT},
		 *            {@link #WRAP_CONTENT} or a fixed size in pixels
		 * @param gravity
		 *            the gravity
		 * 
		 * @see android.view.Gravity
		 */
		public LayoutParams(int width, int height, int gravity) {
			super(width, height);
			this.gravity = gravity;
		}

		/**
		 * {@inheritDoc}
		 */
		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		/**
		 * {@inheritDoc}
		 */
		public LayoutParams(ViewGroup.MarginLayoutParams source) {
			super(source);
		}
	}

	@Override
	public void childDrawableStateChanged(View child) {
		// TODO Auto-generated method stub
	}

	@Override
	public View focusSearch(View v, int direction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addView(View view, android.view.ViewGroup.LayoutParams params) {
		addView(view, params, true);
	}

    /**
     * @j2sNative
     * console.log("Missing method: gatherTransparentRegion");
     */
    @MayloonStubAnnotation()
    public boolean gatherTransparentRegion(Region region) {
        System.out.println("Stub" + " Function : gatherTransparentRegion");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setForegroundGravity");
     */
    @MayloonStubAnnotation()
    public void setForegroundGravity(int foregroundGravity) {
        System.out.println("Stub" + " Function : setForegroundGravity");
        return;
    }

}
