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

import android.text.Layout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.android.internal.util.FastMath;
import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.BoringLayout;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.GetChars;
import android.text.GraphicsOperations;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.Styled;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.MovementMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TransformationMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;

/**
 * Displays text to the user and optionally allows them to edit it. A TextView
 * is a complete text editor, however the basic class is configured to not allow
 * editing; see {@link EditText} for a subclass that configures the text view
 * for editing.
 */
public class TextView extends View {
    static final String LOG_TAG = "TextView";
    static final boolean DEBUG_EXTRACT = false;
	private static int PRIORITY = 100;

    private ColorStateList mTextColor;
    private int mCurTextColor;
    private ColorStateList mHintTextColor;
    private ColorStateList mLinkTextColor;
    private int mCurHintTextColor;
    private boolean                 mLinksClickable = true;

    private int mLines = 0;
    private int mContentLines = 0;
    private int mTextCharNum = 0;
    private int mOneLineChNum = -1;
    private int mDefaultWidth = -1;
    private boolean mIsChanged = false;
    private static int mDefaultSize = 30;

	protected int mMaxWidth = Integer.MAX_VALUE;
	private int mMaxWidthMode = PIXELS;
	protected int mMinWidth = -1;
	private int mMinWidthMode = PIXELS;
    protected int mScrollX;
    protected int mScrollY;
    private long mLastScroll;
    private Scroller mScroller = null;
    private BoringLayout.Metrics mBoring;
    private BoringLayout.Metrics mHintBoring;

    private static final int        LINES = 1;
    private static final int        EMS = LINES;
    private static final int        PIXELS = 2;

    private int                     mMaximum = Integer.MAX_VALUE;
    private int                     mMaxMode = LINES;
    private int                     mMinimum = 0;
    private int                     mMinMode = LINES;

	final int[] mTempCoords = new int[2];
	Rect mTempRect;
	private boolean mFreezesText;
	private boolean mTemporaryDetach;
	private boolean mDispatchTemporaryDetach;
	private boolean mShowErrorAfterAttach;

	private static final int PREDRAW_NOT_REGISTERED = 0;
    private static final int PREDRAW_PENDING = 1;
    private static final int PREDRAW_DONE = 2;
    private int mPreDrawState = PREDRAW_NOT_REGISTERED;

    private Layout                  mLayout;

    private float mShadowRadius, mShadowDx, mShadowDy;
    private float mSpacingMult = 1;
    private float mSpacingAdd = 0;
    private CharWrapper mCharWrapper = null;
    private TransformationMethod    mTransformation;

	private TextUtils.TruncateAt mEllipsize = null;

    // Enum for the "typeface" XML parameter.
    // TODO: How can we get this from the XML instead of hardcoding it here?
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

	int mTextSelectHandleLeftRes;
	int mTextSelectHandleRightRes;
	int mTextSelectHandleRes;
	protected int mInputType = InputType.TYPE_NULL;
    private KeyListener             mInput;
    private boolean                 mEditable;
	// display attributes
    private final TextPaint         mTextPaint;
    private boolean                 mUserSetTextScaleX;
    //private final Paint             mHighlightPaint;
    private int                     mHighlightColor = 0xCC475925;

    private long                    mShowCursor;
    private Blink                   mBlink;
    private boolean                 mCursorVisible = true;
    private boolean mScrolled = false;

    private static final int        BLINK = 500;
    private static final int ANIMATED_SCROLL_GAP = 250;
    private static int clipMoveX = 0;
    private static int clipMoveY = 0;
    private static int clipWidth = 0;
    private static int clipHeight = 0;
    

	// Cursor Controllers. Null when disabled.
    private CursorController        mInsertionPointCursorController;
    private CursorController        mSelectionModifierCursorController;
    private boolean                 mInsertionControllerEnabled;
    private boolean                 mSelectionControllerEnabled;
    private boolean                 mInBatchEditControllers;
    private boolean                 mIsInTextSelectionMode = false;

    private CharSequence mError;
    private boolean mErrorWasChanged;
    private ErrorPopup mPopup;

	private String                  mHint;
	private Layout                  mHintLayout;
	protected boolean mSingleLine = false;
	private boolean                 mIncludePad = true;
	protected int mMaxLength = -1;
	private boolean mEatTouchRelease = false;
    private ArrayList<TextWatcher>  mListeners = null;

    Drawable mSelectHandleLeft;
    Drawable mSelectHandleRight;
    Drawable mSelectHandleCenter;
    private Spannable.Factory mSpannableFactory = Spannable.Factory.getInstance();
    private MovementMethod          mMovement;
    
    class InputContentType {
        int imeOptions = EditorInfo.IME_NULL;
        String privateImeOptions;
        CharSequence imeActionLabel;
        int imeActionId;
        Bundle extras;
        OnEditorActionListener onEditorActionListener;
        boolean enterDown;
    }
    InputContentType mInputContentType;

	/**
	 * Interface definition for a callback to be invoked when an action is
	 * performed on the editor.
	 */
	public interface OnEditorActionListener {
		/**
		 * Called when an action is being performed.
		 *
		 * @param v
		 *            The view that was clicked.
		 * @param actionId
		 *            Identifier of the action. This will be either the
		 *            identifier you supplied, or {@link EditorInfo#IME_NULL
		 *            EditorInfo.IME_NULL} if being called due to the enter key
		 *            being pressed.
		 * @param event
		 *            If triggered by an enter key, this is the event;
		 *            otherwise, this is null.
		 * @return Return true if you have consumed the action, else false.
		 */
		boolean onEditorAction(TextView v, int actionId, KeyEvent event);
	}
    /**
     * Return the baseline for the specified line (0...getLineCount() - 1)
     * If bounds is not null, return the top, left, right, bottom extents
     * of the specified line in it. If the internal Layout has not been built,
     * return 0 and set bounds to (0, 0, 0, 0)
     * @param line which line to examine (0..getLineCount() - 1)
     * @param bounds Optional. If not null, it returns the extent of the line
     * @return the Y-coordinate of the baseline
     */
    public int getLineBounds(int line, Rect bounds) {

        if (bounds != null) {
            bounds.set(0, 0, 0, 0);
        }

        int baseline = mCurHeight;
//        int baseline = mLayout.getLineBounds(line, bounds);

        int voffset = getExtendedPaddingTop();
//        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
//            voffset += getVerticalOffset(true);
//        }
        if (bounds != null) {
            bounds.offset(getCompoundPaddingLeft(), voffset);
        }
        return baseline + voffset;

    }

    public int getLineCount() {
        return mLayout != null ? mLayout.getLineCount() : 0;
    }

    public TextView(Context context) {
        this(context, null);
    }

	// TODO will cause error in j2s?
	public TextView(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.textViewStyle);
	}

	public TextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mText = "";

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = getResources().getDisplayMetrics().density;

        TypedArray a =
                context.obtainStyledAttributes(
                    attrs, com.android.internal.R.styleable.TextView, defStyle, 0);

        int textColorHighlight = 0;
        ColorStateList textColor = null;
        ColorStateList textColorHint = null;
        ColorStateList textColorLink = null;
        int textSize = 15;
        int typefaceIndex = -1;
        int styleIndex = -1;

		/*
		 * Look the appearance up without checking first if it exists because
		 * almost every TextView has one and it greatly simplifies the logic
		 * to be able to parse the appearance first and then let specific tags
		 * for this View override it.
		 */
		TypedArray appearance = null;
		int ap = a.getResourceId(
				com.android.internal.R.styleable.TextView_textAppearance, -1);
		if (ap != -1) {
			appearance = context.obtainStyledAttributes(ap,
					com.android.internal.R.styleable.TextAppearance);
		}

		if (appearance != null) {
			int n = appearance.getIndexCount();
			for (int i = 0; i < n; i++) {
				int attr = appearance.getIndex(i);

				switch (attr) {
                case com.android.internal.R.styleable.TextAppearance_textColorHighlight:
                    textColorHighlight = appearance.getColor(attr, textColorHighlight);
                    break;

                case com.android.internal.R.styleable.TextAppearance_textColor:
                    textColor = appearance.getColorStateList(attr);
                    break;

                case com.android.internal.R.styleable.TextAppearance_textColorHint:
                    textColorHint = appearance.getColorStateList(attr);
                    break;

                case com.android.internal.R.styleable.TextAppearance_textColorLink:
                    textColorLink = appearance.getColorStateList(attr);
                    break;

                case com.android.internal.R.styleable.TextAppearance_textSize:
                    textSize = appearance.getDimensionPixelSize(attr, textSize);
                    break;

                case com.android.internal.R.styleable.TextAppearance_typeface:
                    typefaceIndex = appearance.getInt(attr, -1);
                    break;

                case com.android.internal.R.styleable.TextAppearance_textStyle:
                    styleIndex = appearance.getInt(attr, -1);
                    break;

				}
			}

			appearance.recycle();
		}

        boolean editable = getDefaultEditable();
        CharSequence text = "";
        int inputType = InputType.TYPE_NULL;
        boolean singleLine = false;
        int maxlength = -1;
        boolean password = false;
        int n = a.getIndexCount();
		for (int i = 0; i < n; i++) {
			int attr = a.getIndex(i);

			switch (attr) {
            case com.android.internal.R.styleable.TextView_editable:
                editable = a.getBoolean(attr, editable);
                break;
			case com.android.internal.R.styleable.TextView_text:
				text = a.getText(attr);
				break;
			case com.android.internal.R.styleable.TextView_textSize:
				textSize = a.getDimensionPixelSize(attr, textSize);
				break;
			case com.android.internal.R.styleable.TextView_textColor:
				textColor = a.getColorStateList(attr);
				break;
			case com.android.internal.R.styleable.TextView_textColorHint:
				textColorHint = a.getColorStateList(attr);
				break;
			case com.android.internal.R.styleable.TextView_textColorLink:
				textColorLink = a.getColorStateList(attr);
				break;
			case com.android.internal.R.styleable.TextView_lines:
				setLines(a.getInt(attr, -1));
				break;
			case com.android.internal.R.styleable.TextView_gravity:
				setGravity(a.getInt(attr, -1));
				break;
			case com.android.internal.R.styleable.TextView_inputType:
				inputType = a.getInt(attr, mInputType);
				break;
            case com.android.internal.R.styleable.TextView_singleLine:
                singleLine = a.getBoolean(attr, singleLine);
                mSingleLine = singleLine;
                break;
            case com.android.internal.R.styleable.TextView_maxLength:
                maxlength = a.getInt(attr, -1);
                mMaxLength = maxlength;
                break;
            case com.android.internal.R.styleable.TextView_freezesText:
                mFreezesText = a.getBoolean(attr, false);
                break;
            case com.android.internal.R.styleable.TextView_hint:
                mHint = (String)a.getText(attr);
                break;
			case com.android.internal.R.styleable.TextView_width:
				setWidth(a.getDimensionPixelSize(attr, -1));
				break;
            case com.android.internal.R.styleable.TextView_height:
                setHeight(a.getDimensionPixelSize(attr, -1));
                break;
            case com.android.internal.R.styleable.TextView_scrollHorizontally:
                if (a.getBoolean(attr, false)) {
                    setHorizontallyScrolling(true);
                }
                break;
			case com.android.internal.R.styleable.TextView_typeface:
				typefaceIndex = a.getInt(attr, typefaceIndex);
				break;

			case com.android.internal.R.styleable.TextView_textStyle:
				styleIndex = a.getInt(attr, styleIndex);
				break;

            case com.android.internal.R.styleable.TextView_password:
                password = a.getBoolean(attr, password);
                break;

            case com.android.internal.R.styleable.TextView_lineSpacingExtra:
                mSpacingAdd = a.getDimensionPixelSize(attr, (int) mSpacingAdd);
                break;

            case com.android.internal.R.styleable.TextView_lineSpacingMultiplier:
                mSpacingMult = a.getFloat(attr, mSpacingMult);
                break;

			}
		}
		a.recycle();

        if ((inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION))
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)) {
            password = true;
        }
		mEditable = editable;

		setRawTextSize(textSize);

		if (inputType != InputType.TYPE_NULL) {
			setInputType(inputType);
			singleLine = (inputType & (InputType.TYPE_MASK_CLASS | InputType.TYPE_TEXT_FLAG_MULTI_LINE)) != (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		} else if (editable) {
            mInputType = InputType.TYPE_CLASS_TEXT;
            if (!singleLine) {
                mInputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            }
        }
        if (password && (mInputType&EditorInfo.TYPE_MASK_CLASS)
                == EditorInfo.TYPE_CLASS_TEXT) {
            mInputType = (mInputType & ~(EditorInfo.TYPE_MASK_VARIATION))
                | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
        }
        if (singleLine) {
            setLines(1);
            mSingleLine = true;
        }

		setTypefaceByIndex(typefaceIndex, styleIndex);

		setTextColor(textColor != null ? textColor : ColorStateList
				.valueOf(0xFF000000));
		setHintTextColor(textColorHint);
		setLinkTextColor(textColorLink);
		setText(text);
	}

	private void setTypefaceByIndex(int typefaceIndex, int styleIndex) {
		Typeface tf = null;
		switch (typefaceIndex) {
		case SANS:
			tf = Typeface.SANS_SERIF;
			break;

		case SERIF:
			tf = Typeface.SERIF;
			break;

		case MONOSPACE:
			tf = Typeface.MONOSPACE;
			break;
		}

		setTypeface(tf, styleIndex);
	}

	/**
	 * @return the base paint used for the text.  Please use this only to
	 * consult the Paint's properties and not to change them.
	 */
	public TextPaint getPaint() {
		return mTextPaint;
	}

	/**
	 * @return the flags on the Paint being used to display the text.
	 * @see Paint#getFlags
	 */
	public int getPaintFlags() {
		return mTextPaint.getFlags();
	}

	/**
	 * Sets flags on the Paint being used to display the text and
	 * reflows the text if they are different from the old flags.
	 * @see Paint#setFlags
	 */
	public void setPaintFlags(int flags) {
		if (mTextPaint.getFlags() != flags) {
			mTextPaint.setFlags(flags);

			nullLayouts();
			requestLayout();
			invalidate();
		}
	}

	/**
	 * @return the extent by which text is currently being stretched
	 * horizontally.  This will usually be 1.
	 */
	public float getTextScaleX() {
		return mTextPaint.getTextScaleX();
	}

	/**
	 * @return the size (in pixels) of the default text size in this TextView.
	 */
	public float getTextSize() {
		return mTextPaint.getTextSize();
	}

    /**
     * Set the default text size to the given value, interpreted as "scaled
     * pixel" units.  This size is adjusted based on the current density and
     * user font size preference.
     *
     * @param size The scaled pixel size.
     *
     * @attr ref android.R.styleable#TextView_textSize
     */
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

	/**
	 * Set the default text size to a given unit and value.  See {@link
	 * TypedValue} for the possible dimension units.
	 *
	 * @param unit The desired dimension unit.
	 * @param size The desired size in the given units.
	 *
	 * @attr ref android.R.styleable#TextView_textSize
	 */
	public void setTextSize(int unit, float size) {
		Context c = getContext();
		Resources r;

		if (c == null)
			r = Resources.getSystem();
		else
			r = c.getResources();

		setRawTextSize(TypedValue.applyDimension(unit, size,
				r.getDisplayMetrics()));
	}

	/**
	 * @return the current typeface and style in which the text is being
	 * displayed.
	 */
	public Typeface getTypeface() {
		return mTextPaint.getTypeface();
	}

    private void setRawTextSize(float size) {
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);

            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

	/**
	 * Sets the extent by which text should be stretched horizontally.
	 *
	 * @attr ref android.R.styleable#TextView_textScaleX
	 */
	public void setTextScaleX(float size) {
		if (size != mTextPaint.getTextScaleX()) {
			mTextPaint.setTextScaleX(size);

			nullLayouts();
			requestLayout();
			invalidate();
		}
	}

	/**
	 * Sets the typeface and style in which the text should be displayed.
	 * Note that not all Typeface families actually have bold and italic
	 * variants, so you may need to use
	 * {@link #setTypeface(Typeface, int)} to get the appearance
	 * that you actually want.
	 *
	 * @attr ref android.R.styleable#TextView_typeface
	 * @attr ref android.R.styleable#TextView_textStyle
	 */
	public void setTypeface(Typeface tf) {
		if (mTextPaint.getTypeface() != tf) {
			mTextPaint.setTypeface(tf);

			nullLayouts();
			requestLayout();
			invalidate();

		}
	}

	/**
	 * Sets the typeface and style in which the text should be displayed,
	 * and turns on the fake bold and italic bits in the Paint if the
	 * Typeface that you provided does not have all the bits in the
	 * style that you specified.
	 *
	 * @attr ref android.R.styleable#TextView_typeface
	 * @attr ref android.R.styleable#TextView_textStyle
	 */
	public void setTypeface(Typeface tf, int style) {
		if (style > 0) {
			if (tf == null) {
				tf = Typeface.defaultFromStyle(style);
			} else {
				tf = Typeface.create(tf, style);
			}

			setTypeface(tf);
			// now compute what (if any) algorithmic styling is needed
			int typefaceStyle = tf != null ? tf.getStyle() : 0;
			int need = style & ~typefaceStyle;
			mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
			mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
		} else {
			mTextPaint.setFakeBoldText(false);
			mTextPaint.setTextSkewX(0);
			setTypeface(tf);
		}
	}

    /**
     * Subclasses override this to specify that they have a KeyListener
     * by default even if not specifically called for in the XML options.
     */
    protected boolean getDefaultEditable() {
        return false;
    }

    public void setTextColor(int color) {
        mTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    /**
     * Sets the text color.
     *
     * @attr ref android.R.styleable#TextView_textColor
     */
    public void setTextColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }
        mTextColor = colors;
        updateTextColors();
    }

    /**
     * Returns the TextView_textColor attribute from the
     * Resources.StyledAttributes, if set, or the TextAppearance_textColor
     * from the TextView_textAppearance attribute, if TextView_textColor
     * was not set directly.
     */
    public static ColorStateList getTextColors(Context context, TypedArray attrs) {
        ColorStateList colors;
        colors = attrs.getColorStateList(com.android.internal.R.styleable.
                                         TextView_textColor);

        if (colors == null) {
            int ap = attrs.getResourceId(com.android.internal.R.styleable.
                                         TextView_textAppearance, -1);
            if (ap != -1) {
                TypedArray appearance;
                appearance = context.obtainStyledAttributes(ap,
                                            com.android.internal.R.styleable.TextAppearance);
                colors = appearance.getColorStateList(com.android.internal.R.styleable.
                                                  TextAppearance_textColor);
                appearance.recycle();
            }
        }

        return colors;
    }

    private void updateTextColors() {
        boolean inval = false;
        int color = mTextColor.getColorForState(getDrawableState(), 0);
        if (color != mCurTextColor) {
            mCurTextColor = color;
            inval = true;
        }

        if (mLinkTextColor != null) {
            color = mLinkTextColor.getColorForState(getDrawableState(), 0);
            if (color != mTextPaint.linkColor) {
                mTextPaint.linkColor = color;
                inval = true;
            }
        }
        if (mHintTextColor != null) {
            color = mHintTextColor.getColorForState(getDrawableState(), 0);
            if (color != mCurHintTextColor && mText.length() == 0) {
                mCurHintTextColor = color;
                inval = true;
            }
        }
        if (inval) {
            invalidate();
        }
    }

	public void setWidth(int pixels) {
		//		System.out.println(TAG + "setWidth, pixels: " + pixels);
		mMaxWidth = mMinWidth = pixels;

		requestLayout();
		invalidate();
	}

    public void setLineSpacing(float add, float mult) {
        mSpacingMult = mult;
        mSpacingAdd = add;

        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public void setHeight(int pixels) {
        mMaximum = mMinimum = pixels;
        mMaxMode = mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /** Mayloon workaround
     *  we need to setVisibility for our only DOM textarea except canvas
     */
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        String visible = "visible";
        if (visibility != VISIBLE)
            visible = "hidden";
        int thisViewId = getUIElementID();
        /**
         * @j2sNative
         * var thisView = document.getElementById(thisViewId);
         * if (thisView != null) thisView.style.visibility = visible;
         */{}
    }

    class Drawables {
        final Rect mCompoundRect = new Rect();
        Drawable mDrawableTop, mDrawableBottom, mDrawableLeft, mDrawableRight;
        int mDrawableSizeTop, mDrawableSizeBottom, mDrawableSizeLeft, mDrawableSizeRight;
        int mDrawableWidthTop, mDrawableWidthBottom, mDrawableHeightLeft, mDrawableHeightRight;
        int mDrawablePadding;
    }
    private Drawables mDrawables;

	public void setCompoundDrawables(Drawable left, Drawable top,
			Drawable right, Drawable bottom) {
		Drawables dr = mDrawables;

		final boolean drawables = left != null || top != null || right != null
				|| bottom != null;

		if (!drawables) {
			if (dr != null) {
				if (dr.mDrawablePadding == 0) {
					mDrawables = null;
				} else {
					// We need to retain the last set padding, so just clear
					// out all of the fields in the existing structure.
					if (dr.mDrawableLeft != null)
						dr.mDrawableLeft.setCallback(null);
					dr.mDrawableLeft = null;
					if (dr.mDrawableTop != null)
						dr.mDrawableTop.setCallback(null);
					dr.mDrawableTop = null;
					if (dr.mDrawableRight != null)
						dr.mDrawableRight.setCallback(null);
					dr.mDrawableRight = null;
					if (dr.mDrawableBottom != null)
						dr.mDrawableBottom.setCallback(null);
					dr.mDrawableBottom = null;
					dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
					dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
					dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
					dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
				}
			}
		} else {
			if (dr == null) {
				mDrawables = dr = new Drawables();
			}

			if (dr.mDrawableLeft != left && dr.mDrawableLeft != null) {
				dr.mDrawableLeft.setCallback(null);
			}
			dr.mDrawableLeft = left;

			if (dr.mDrawableTop != top && dr.mDrawableTop != null) {
				dr.mDrawableTop.setCallback(null);
			}
			dr.mDrawableTop = top;

			if (dr.mDrawableRight != right && dr.mDrawableRight != null) {
				dr.mDrawableRight.setCallback(null);
			}
			dr.mDrawableRight = right;

			if (dr.mDrawableBottom != bottom && dr.mDrawableBottom != null) {
				dr.mDrawableBottom.setCallback(null);
			}
			dr.mDrawableBottom = bottom;

			final Rect compoundRect = dr.mCompoundRect;
			int[] state;

			state = getDrawableState();

			if (left != null) {
				left.setState(state);
				left.copyBounds(compoundRect);
				left.setCallback(this);
				dr.mDrawableSizeLeft = compoundRect.width();
				dr.mDrawableHeightLeft = compoundRect.height();
			} else {
				dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
			}

			if (right != null) {
				right.setState(state);
				right.copyBounds(compoundRect);
				right.setCallback(this);
				dr.mDrawableSizeRight = compoundRect.width();
				dr.mDrawableHeightRight = compoundRect.height();
			} else {
				dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
			}

			if (top != null) {
				top.setState(state);
				top.copyBounds(compoundRect);
				top.setCallback(this);
				dr.mDrawableSizeTop = compoundRect.height();
				dr.mDrawableWidthTop = compoundRect.width();
			} else {
				dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
			}

			if (bottom != null) {
				bottom.setState(state);
				bottom.copyBounds(compoundRect);
				bottom.setCallback(this);
				dr.mDrawableSizeBottom = compoundRect.height();
				dr.mDrawableWidthBottom = compoundRect.width();
			} else {
				dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
			}
		}

		requestLayout();
		invalidate();
	}
    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top,
            Drawable right, Drawable bottom) {

        if (left != null) {
        	System.out.println("left is not null");
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (right != null) {
        	System.out.println("right is not null");
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
        }
        if (top != null) {
        	System.out.println("top is not null");
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
        	System.out.println("bottom is not null");
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawables(left, top, right, bottom);
    }

	public void setInputType(int type) {
	    boolean isPassword = type == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        boolean multiLine = (type&(EditorInfo.TYPE_MASK_CLASS
                        | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE)) ==
                (EditorInfo.TYPE_CLASS_TEXT
                        | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
        
        // We need to update the single line mode if it has changed or we
        // were previously in password mode.
        if (mSingleLine == multiLine) {
            // Change single line mode, but only change the transformation if
            // we are not in password mode.
            applySingleLine(!multiLine, !isPassword);
        }
        mInputType = type;
        
        /**
         * @j2sNative
         * var thisView = document.getElementById(this.getUIElementID());
         * if(!thisView) return;
         * if(isPassword) {
         *     thisView.type="password";
         * }
         *  else {
         *     thisView.type="text";
         * }
         */{}
	}

    public void setLines(int lines) {
        mLines = mMaximum = mMinimum = lines;
        mMaxMode = mMinMode = LINES;

        requestLayout();
        invalidate();
    }

	/**
	 * Return the text the TextView is displaying. If setText() was called with
	 * an argument of BufferType.SPANNABLE or BufferType.EDITABLE, you can cast
	 * the return value from this method to Spannable or Editable, respectively.
	 *
	 * Note: The content of the return value should not be modified. If you want
	 * a modifiable one, you should make your own copy first.
	 */
	public CharSequence getText() {
		return mText;
	}

	/**
	 * Returns the length, in characters, of the text managed by this TextView
	 */
	public int length() {
		return mText.toString().length();
	}

	/**
	 * Returns the top padding of the view, plus space for the top Drawable if
	 * any.
	 */
	public int getCompoundPaddingTop() {
		final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableTop == null) {
            return mPaddingTop;
        } else {
            return mPaddingTop + dr.mDrawablePadding + dr.mDrawableSizeTop;
        }
	}

	/**
	 * Returns the bottom padding of the view, plus space for the bottom
	 * Drawable if any.
	 */
	public int getCompoundPaddingBottom() {
		final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableBottom == null) {
            return mPaddingBottom;
        } else {
            return mPaddingBottom + dr.mDrawablePadding + dr.mDrawableSizeBottom;
        }
	}

	/**
	 * Returns the left padding of the view, plus space for the left Drawable if
	 * any.
	 */
	public int getCompoundPaddingLeft() {
		final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableLeft == null) {
            return mPaddingLeft;
        } else {
            return mPaddingLeft + dr.mDrawablePadding + dr.mDrawableSizeLeft;
        }
	}

	/**
	 * Returns the right padding of the view, plus space for the right Drawable
	 * if any.
	 */
	public int getCompoundPaddingRight() {
		final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableRight == null) {
            return mPaddingRight;
        } else {
            return mPaddingRight + dr.mDrawablePadding + dr.mDrawableSizeRight;
        }
	}

    /**
     * Returns the extended top padding of the view, including both the top
     * Drawable if any and any extra space to keep more than maxLines of text
     * from showing. It is only valid to call this after measuring.
     */
    public int getExtendedPaddingTop() {
        if (mMaxMode != LINES) {
            return getCompoundPaddingTop();
        }

        // if (mLayout.getLineCount() <= mMaximum) {
        if (this.getLineCount() <= mMaximum) {
            return getCompoundPaddingTop();
        }

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = getHeight() - top - bottom;
        int layoutht = mLayout.getLineTop(mMaximum);

        if (layoutht >= viewht) {
            return top;
        }

        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) {
            return top;
        } else if (gravity == Gravity.BOTTOM) {
            return top + viewht - layoutht;
        } else { // (gravity == Gravity.CENTER_VERTICAL)
            return top + (viewht - layoutht) / 2;
        }
    }

    /**
     * Returns the extended bottom padding of the view, including both the
     * bottom Drawable if any and any extra space to keep more than maxLines of
     * text from showing. It is only valid to call this after measuring.
     */
    public int getExtendedPaddingBottom() {
        if (mMaxMode != LINES) {
            return getCompoundPaddingBottom();
        }

        //if (mLayout.getLineCount() <= mMaximum) {
        if (this.getLineCount() <= mMaximum) {
            return getCompoundPaddingBottom();
        }

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = getHeight() - top - bottom;
        int layoutht = mLayout.getLineTop(mMaximum);

        if (layoutht >= viewht) {
            return bottom;
        }

        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) {
            return bottom + viewht - layoutht;
        } else if (gravity == Gravity.BOTTOM) {
            return bottom;
        } else { // (gravity == Gravity.CENTER_VERTICAL)
            return bottom + (viewht - layoutht) / 2;
        }
    }

	/**
	 * Returns the total left padding of the view, including the left Drawable
	 * if any.
	 */
	public int getTotalPaddingLeft() {
		return getCompoundPaddingLeft();
	}

	/**
	 * Returns the total right padding of the view, including the right Drawable
	 * if any.
	 */
	public int getTotalPaddingRight() {
		return getCompoundPaddingRight();
	}

	/**
	 * Returns the total top padding of the view, including the top Drawable if
	 * any, the extra space to keep more than maxLines from showing, and the
	 * vertical offset for gravity, if any.
	 */
	public int getTotalPaddingTop() {
		return getExtendedPaddingTop();
	}

	/**
	 * Returns the total bottom padding of the view, including the bottom
	 * Drawable if any, the extra space to keep more than maxLines from showing,
	 * and the vertical offset for gravity, if any.
	 */
	public int getTotalPaddingBottom() {
		return getExtendedPaddingBottom();
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
	    if (left != mPaddingLeft ||
	            right != mPaddingRight ||
	            top != mPaddingTop ||
	            bottom != mPaddingBottom) {
	            nullLayouts();
	        }

		// the super call will requestLayout()
		super.setPadding(left, top, right, bottom);
		invalidate();
	}

	/**
	 * Convenience method: Append the specified text to the TextView's display
	 * buffer, upgrading it to BufferType.EDITABLE if it was not already
	 * editable.
	 */
	public final void append(CharSequence text) {
		append(text, 0, text.toString().length());
	}

	/**
	 * Convenience method: Append the specified text slice to the TextView's
	 * display buffer, upgrading it to BufferType.EDITABLE if it was not already
	 * editable.
	 */
	public void append(CharSequence text, int start, int end) {
//	 	if(this.mSingleLine&&(text.equals("\r\n")||text.equals("\n"))){
//	 	     return;
//	 	}
	    
       //replace line feed character "\n" in java by "<br/>" in javascript
        int cnt = 0;
        for (int i = start; i <= end; i++) {
            if (text.charAt(i) == '\n') cnt++;
        }
        CharSequence prefix = text.subSequence(0, start - 1);
        CharSequence strTmp = text.subSequence(start, end);
        /**
         * @j2sNative strTmp = strTmp.replace("\n", "<br/>");
         */{}
        text = prefix.toString() + strTmp.toString();
        end += 4 * cnt;

        if (!(mText instanceof Editable)) {
            setText(mText, BufferType.EDITABLE);
        }
        ((Editable) mText).append(text, start, end);
	}

	/**
	 * Control whether this text view saves its entire text contents when
	 * freezing to an icicle, in addition to dynamic state such as cursor
	 * position. By default this is false, not saving the text. Set to true if
	 * the text in the text view is not being saved somewhere else in persistent
	 * storage (such as in a content provider) so that if the view is later
	 * thawed the user will not lose their data.
	 *
	 * @param freezesText
	 *            Controls whether a frozen icicle should include the entire
	 *            text data: true to include it, false to not.
	 *
	 * @attr ref android.R.styleable#TextView_freezesText
	 */
	public void setFreezesText(boolean freezesText) {
		mFreezesText = freezesText;
	}

	/**
	 * Return whether this text view is including its entire text contents in
	 * frozen icicles.
	 *
	 * @return Returns true if text is included, false if it isn't.
	 *
	 * @see #setFreezesText
	 */
	public boolean getFreezesText() {
		return mFreezesText;
	}

	/**
	 * Sets the Factory used to create new Editables.
	 */
	public void setEditableFactory(Editable.Factory factory) {
		mEditableFactory = factory;
		setText(mText);
	}

    /**
     * Like {@link #setText(CharSequence)}, except that the cursor position (if
     * any) is retained in the new text.
     * 
     * @param text The new text to place in the text view.
     * @see #setText(CharSequence)
     */
    public final void setTextKeepState(CharSequence text) {
        setTextKeepState(text, mBufferType);
    }

    /**
     * Like {@link #setText(CharSequence, android.widget.TextView.BufferType)},
     * except that the cursor position (if any) is retained in the new text.
     *
     * @see #setText(CharSequence, android.widget.TextView.BufferType)
     */
    public final void setTextKeepState(CharSequence text, BufferType type) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int len = text.length();

        setText(text, type);

        if (start >= 0 || end >= 0) {
            if (mText instanceof Spannable) {
                Selection.setSelection((Spannable) mText,
                                       Math.max(0, Math.min(start, len)),
                                       Math.max(0, Math.min(end, len)));
            }
        }
    }

    public final void setText(int resid) {
        CharSequence text = getContext().getResources().getText(resid);
        if (text == null) {
            this.setText("");
        } else {
            this.setText(text);
        }
    }

    public final void setText(int resid, BufferType type) {
        CharSequence text = getContext().getResources().getText(resid);
        if (text == null) {
            this.setText("", type);
        } else {
            this.setText(text, type);
        }
    }

    /**
     * Sets the string value of the TextView. TextView <em>does not</em> accept
     * HTML-like formatting, which you can do with text strings in XML resource
     * files. To style your strings, attach android.text.style.* objects to a
     * {@link android.text.SpannableString SpannableString}, or see the <a
     * href="{@docRoot}
     * guide/topics/resources/available-resources.html#stringresources">
     * Available Resource Types</a> documentation for an example of setting
     * formatted text in the XML resource file.
     *
     * @attr ref android.R.styleable#TextView_text
     */
    public final void setText(CharSequence text) {
        setText(text, mBufferType);
    }

    public void setText(CharSequence text, BufferType type) {
        setText(text, type, true, 0);
    }

    /**
     * Sets the TextView to display the specified slice of the specified
     * char array.  You must promise that you will not change the contents
     * of the array except for right before another call to setText(),
     * since the TextView has no way to know that the text
     * has changed and that it needs to invalidate and re-layout.
     */
    public final void setText(char[] text, int start, int len) {
        int oldlen = 0;

        if (start < 0 || len < 0 || start + len > text.length) {
            throw new IndexOutOfBoundsException(start + ", " + len);
        }

        /*
         * We must do the before-notification here ourselves because if
         * the old text is a CharWrapper we destroy it before calling
         * into the normal path.
         */
        if (mText != null) {
            oldlen = mText.length();
            // sendBeforeTextChanged(mText, 0, oldlen, len);
        }
        // else {
        // sendBeforeTextChanged("", 0, 0, len);
        // }

        if (mCharWrapper == null) {
            mCharWrapper = new CharWrapper(text, start, len);
        } else {
            mCharWrapper.set(text, start, len);
        }

        setText(mCharWrapper, mBufferType, false, oldlen);
    }

    /**
     * Sets the text to be displayed when the text of the TextView is empty.
     * Null means to use the normal empty text. The hint does not currently
     * participate in determining the size of the view.
     *
     * @attr ref android.R.styleable#TextView_hint
     */
    public final void setHint(CharSequence hint) {
        //mHint = TextUtils.stringOrSpannedString(hint);
        mHint = (String) hint;

        if (mLayout != null) {
            // TODO: checkForRelayout();
        }

        if (mText.length() == 0) {
            invalidate();
        }
    }

    /**
     * Sets the text to be displayed when the text of the TextView is empty,
     * from a resource.
     *
     * @attr ref android.R.styleable#TextView_hint
     */
    public final void setHint(int resid) {
        setHint(getContext().getResources().getText(resid));
    }

    /**
     * Sets whether the text should be allowed to be wider than the View is. If
     * false, it will be wrapped to the width of the View.
     *
     * @attr ref android.R.styleable#TextView_scrollHorizontally
     */
    public void setHorizontallyScrolling(boolean whether) {
        // mHorizontallyScrolling = whether;
        if (whether) {
            setLines(1);
            mSingleLine = true;
        }
        // if (mLayout != null) {
        nullLayouts();
        requestLayout();
        invalidate();
        // }
    }

    /**
     * Returns the hint that is displayed when the text of the TextView
     * is empty.
     *
     * @attr ref android.R.styleable#TextView_hint
     */
    public CharSequence getHint() {
        return mHint;
    }

	@Override
	protected boolean isPaddingOffsetRequired() {
		return mShadowRadius != 0;
	}

	@Override
	protected int getLeftPaddingOffset() {
		return getCompoundPaddingLeft() - mPaddingLeft
				+ (int) Math.min(0, mShadowDx - mShadowRadius);
	}

	@Override
	protected int getTopPaddingOffset() {
		return (int) Math.min(0, mShadowDy - mShadowRadius);
	}

	@Override
	protected int getBottomPaddingOffset() {
		return (int) Math.max(0, mShadowDy + mShadowRadius);
	}

	@Override
	protected int getRightPaddingOffset() {
		return -(getCompoundPaddingRight() - mPaddingRight)
				+ (int) Math.max(0, mShadowDx + mShadowRadius);
	}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = 0;
        int height = 0;

        BoringLayout.Metrics boring = UNKNOWN_BORING;
        BoringLayout.Metrics hintBoring = UNKNOWN_BORING;

        int des = -1;
        boolean fromexisting = false;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = (int) FloatMath.ceil(Layout.getDesiredWidth(this.mText, mTextPaint));
            if (width > widthSize)
                mOneLineChNum = (int) Math.floor(mTextCharNum * (float) widthSize / width);
            width = widthSize;
        } else {
            if (mLayout != null && mEllipsize == null) {
                des = desired(mLayout);
            }
            if (des < 0) {
                boring = BoringLayout.isBoring(mTransformed, mTextPaint, mBoring);
                if (boring != null) {
                    // The textarea or input is larger than div 2 pixels by each side that in the same font.
                    if (mEditable) {
                        boring.width = boring.width + 4;
                        boring.bottom = boring.bottom + 4;
                    }
                    mBoring = boring;
                }
            } else {
                fromexisting = true;
            }
            if (boring == null || boring == UNKNOWN_BORING) {
                if (des < 0) {
                    des = (int) FloatMath.ceil(Layout.getDesiredWidth(mTransformed, mTextPaint));
                }
                width = des;
            } else {
                width = boring.width;
            }

            final Drawables dr = mDrawables;
            int dWidth = 0;
            if (dr != null) {
                dWidth = Math.max(width, dr.mDrawableWidthTop);
                dWidth = Math.max(width, dr.mDrawableWidthBottom);
            }

            width += dWidth;

            width += getCompoundPaddingLeft() + getCompoundPaddingRight();
            if (width > mMaxWidth) {
                mOneLineChNum = (int) Math.floor(mTextCharNum * (float) mMaxWidth / width);
                width = mMaxWidth;
            }
            // Check against our minimum width
            int minWidth = Math.max(mMinWidth, getSuggestedMinimumWidth());
            if (width < minWidth) {
                mOneLineChNum = (int) Math.floor(mTextCharNum * (float) minWidth / width);
                width = minWidth;
            }
            if (widthMode == MeasureSpec.AT_MOST) {
                if (width > widthSize) {
                    mOneLineChNum = (int) Math.floor(mTextCharNum * (float) widthSize / width);
                    width = widthSize;
                }
            }
        }

        int want = width - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int unpaddedWidth = want;
        int hintWant = want;

        int hintWidth = mHintLayout == null ? hintWant : mHintLayout.getWidth();

        if (mLayout == null) {
            makeNewLayout(want, hintWant, boring, hintBoring,
                          width - getCompoundPaddingLeft() - getCompoundPaddingRight(), false);
        } else if ((mLayout.getWidth() != want) || (hintWidth != hintWant)) {
            if (mHint == null && mEllipsize == null && want > mLayout.getWidth() && (mLayout instanceof BoringLayout || (fromexisting && des >= 0 && des <= want))) {
                mLayout.increaseWidthTo(want);
            } else {
                makeNewLayout(want, hintWant, boring, hintBoring, width - getCompoundPaddingLeft() - getCompoundPaddingRight(), false);
            }
        } else {
            // Width has not changed.
        }

        mCurWidth = width;

        int desired = getDesiredHeight(this.mText, true);
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = desired;
            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(desired, heightSize);
            }
        }
        // Mayloon : When size of text in INPUT tag is close to the height of INPUT tag, 
        //           it will cause shaking after inputing something or changing text in INPUT
        //           so that we cannot see the complete text in INPUT.
        //           this method just make sure we can see the complete text,
        //           we still need to figure out why INPUT is shaking
        int pad = getCompoundPaddingTop() + getCompoundPaddingBottom();
        boolean textarea = true;
        if (mSingleLine && this instanceof EditText)
            textarea = false;
        if (!textarea && mTextPaint.getTextSize() * 0.9 >= height - pad)
            height = (int) (mTextPaint.getTextSize() * 0.9 + pad);
        mCurHeight = height;
        setMeasuredDimension(width, height);
    }

	private int mCurWidth = 0;
	private int mCurHeight = 0;
	private float mOneLineHeight = -1;

    public int getDesiredHeight(CharSequence text, boolean cap) {
        if (mLayout == null) {
            return 0;
        }
        int linecount = mLayout.getLineCount();
        int pad = getCompoundPaddingTop() + getCompoundPaddingBottom();
        int desired = mLayout.getLineTop(linecount);

        final Drawables dr = mDrawables;
        if (dr != null) {
            desired = Math.max(desired, dr.mDrawableHeightLeft);
            desired = Math.max(desired, dr.mDrawableHeightRight);
        }

        desired += pad;

        if (mMaxMode == LINES) {
            /*
             * Don't cap the hint to a certain number of lines.
             * (Do cap it, though, if we have a maximum pixel height.)
             */
            if (cap) {
                if (linecount > mMaximum) {
                    // TODO: Handle multiple lines
                    desired = mLayout.getLineTop(mMaximum);
                    // layout.getBottomPadding();

                    if (dr != null) {
                        desired = Math.max(desired, dr.mDrawableHeightLeft);
                        desired = Math.max(desired, dr.mDrawableHeightRight);
                    }

                    desired += pad;
                    linecount = mMaximum;
                }
            }
        } else {
            desired = Math.min(desired, mMaximum);
        }

        if (mMinMode == LINES) {
            if (linecount < mMinimum) {
                desired += getLineHeight() * (mMinimum - linecount);
            }
        } else {
            desired = Math.max(desired, mMinimum);
        }

        // Check against our minimum height
        desired = Math.max(desired, getSuggestedMinimumHeight());
        return desired;
    }

	protected CharSequence mText = null;
	private CharSequence mTransformed = null;
	private ChangeWatcher mChangeWatcher;

	protected void onTextChanged(CharSequence text, int start, int before, int after) {
        if (before != 0 && after == 0) {
            mIsChanged = true;
        }
        mTextCharNum = 0;
        for (int i = 0; i < text.toString().length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0001 && c <= 0x00ff)
                mTextCharNum++;
            else
                mTextCharNum += 2;
        }
		int thisViewId = getUIElementID();
		int width = (int) FloatMath.ceil(Layout.getDesiredWidth(text, mTextPaint));
		int height = getDesiredHeight(text, true);
		//		System.out.println(thisViewId
		//				+ " onTextChanged>>>this.mMeasuredWidth: "
		//				+ this.mMeasuredWidth + ", getDesiredWidth: " + width);
		if ((mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
            /**
             * @j2sNative
             * var thisView = document.getElementById(thisViewId);
             * if (null != thisView) {
             *    thisView.style.textAlign = "right";
             * }
             */{}
		}
		if ((mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL) {
            /**
             * @j2sNative
             * var thisView = document.getElementById(thisViewId);
             * if (null != thisView) {
             *    thisView.style.textAlign = "center";
             * }
             */{}
		}
		if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL) {
			/**
			@j2sNative
			//			myself = document.getElementById(this.mUIElementID + "_text");
			//			if (myself != null) {
			//				// vertical
			//				if (this.mTextSize > 0) {
			//					// myself.style.fontSize = this.mTextSize + "px";
			//					padding = this.mMeasuredHeight - this.mTextSize;
			//					if (padding % 2 != 0)
			//						padding = padding + 1;
			//					padding = padding / 2;
			//					if (padding > 0) {
			//						myself.childNodes[0].style.paddingTop = padding + "px";
			//						myself.childNodes[0].style.paddingBottom = padding + "px";
			//					}
			//				}
			//			}
			*/{}
		}
	}

    /**
     * Adds a TextWatcher to the list of those whose methods are called
     * whenever this TextView's text changes.
     * <p>
     * In 1.0, the {@link TextWatcher#afterTextChanged} method was erroneously
     * not called after {@link #setText} calls.  Now, doing {@link #setText}
     * if there are any text changed listeners forces the buffer type to
     * Editable if it would not otherwise be and does call this method.
     */
    public void addTextChangedListener(TextWatcher watcher) {
        if (mListeners == null) {
            mListeners = new ArrayList<TextWatcher>();
        }

        mListeners.add(watcher);
    }

	private void setText(CharSequence text, BufferType type,
			boolean notifyBefore, int oldlen) {
		if (text == null) {
			text = "";
		}
        if (!mUserSetTextScaleX) mTextPaint.setTextScaleX(1.0f);

        if (text instanceof Spanned &&
                ((Spanned) text).getSpanStart(TextUtils.TruncateAt.MARQUEE) >= 0) {
            setHorizontalFadingEdgeEnabled(true);
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        }

        int n = mFilters.length;
        for (int i = 0; i < n; i++) {
            CharSequence out = mFilters[i].filter(text, 0, text.length(),
                                                  EMPTY_SPANNED, 0, 0);
            if (out != null) {
                text = out;
            }
        }

		if (notifyBefore) {
			if (mText != null) {
				oldlen = mText.toString().length();
				sendBeforeTextChanged(mText, 0, oldlen, text.length());
			} else {
				sendBeforeTextChanged("", 0, 0, text.length());
			}
		}
        boolean needEditableForNotification = false;

        if (mListeners != null && mListeners.size() != 0) {
            needEditableForNotification = true;
        }
        if (type == BufferType.EDITABLE) {
            Editable t = mEditableFactory.newEditable(text);
            text = t;
        } else if (type == BufferType.SPANNABLE) {
            text = mSpannableFactory.newSpannable(text);
        } else if (!(text instanceof CharWrapper)) {
            text = TextUtils.stringOrSpannedString(text);
        }

        mBufferType = type;
        mText = text;

        if (mTransformation == null)
            mTransformed = text;
        else
            mTransformed = mTransformation.getTransformation(text, this);

		final int textLength = text.toString().length();

		if (text instanceof Spannable) {
			Spannable sp = (Spannable) text;

			// Remove any ChangeWatchers that might have come
			// from other TextViews.
			final ChangeWatcher[] watchers = sp.getSpans(0, sp.toString()
					.length(), ChangeWatcher.class);
			final int count = watchers.length;
			for (int i = 0; i < count; i++)
				sp.removeSpan(watchers[i]);

			if (mChangeWatcher == null)
				mChangeWatcher = new ChangeWatcher();

			sp.setSpan(mChangeWatcher, 0, textLength,
					Spanned.SPAN_INCLUSIVE_INCLUSIVE
							| (PRIORITY << Spanned.SPAN_PRIORITY_SHIFT));
		}

		if (mLayout != null) {
		    checkForRelayout();
		}
//        sendOnTextChanged(text, 0, oldlen, textLength);
        onTextChanged(text, 0, oldlen, textLength);

//        if (needEditableForNotification) {
//            sendAfterTextChanged((Editable) text);
//        }

        // SelectionModifierCursorController depends on textCanBeSelected, which depends on text
        prepareCursorControllers();
        handleTextChanged(text, 0, oldlen, textLength);
        requestLayout();  // necessary if size of textarea changed
        invalidate();
	}

	//	@Override
	//	protected void setMeasuredDimension(int measuredWidth, int measuredHeight) {
	//		this.basicSetDimension(measuredWidth, measuredHeight);
	//
	//		/**
	//		 @j2sNative
	//		 thisView = document.getElementById(this.mUIElementID);
	//		 if (thisView.childNodes[0] != null) {
	//			 if (this.mMinWidth > 0) {
	//			 	contentWidth = this.mMinWidth;
	//			 	if (this.mMeasuredWidth < this.mMinWidth)
	//			 		contentWidth = this.mMeasuredWidth;
	//			 	thisView.childNodes[0].style.width = contentWidth + "px";
	//			 } else {
	//			 	thisView.childNodes[0].style.width = this.mMeasuredWidth + "px";
	//			 }
	//			 thisView.childNodes[0].style.height = this.mMeasuredHeight + "px";
	//		 }
	//		 */
	//		{
	//		}
	//	}

	public static class BufferType {
		private BufferType(int iValue) {
			mValue = iValue;
		}

		public static BufferType NORMAL = new BufferType(0);
		public static BufferType SPANNABLE = new BufferType(1);
		public static BufferType EDITABLE = new BufferType(2);
		private final int mValue;
	}

	private BufferType mBufferType = BufferType.NORMAL;
	private Editable.Factory mEditableFactory = Editable.Factory.getInstance();

	private class ChangeWatcher implements TextWatcher, SpanWatcher {
		private CharSequence mBeforeText;

		public void beforeTextChanged(CharSequence buffer, int start,
				int before, int after) {
			mBeforeText = buffer.toString();
		}

		public void onTextChanged(CharSequence buffer, int start, int before,
				int after) {
            if (DEBUG_EXTRACT)
                Log.v(LOG_TAG, "onTextChanged start=" + start + " before="
                        + before + " after=" + after + ": " + buffer);
			TextView.this.handleTextChanged(buffer, start, before, after);
			mBeforeText = null;
		}

		public void afterTextChanged(Editable buffer) {

		}

		public void onSpanChanged(Spannable buf, Object what, int s, int e,
				int st, int en) {
		}

		public void onSpanAdded(Spannable buf, Object what, int s, int e) {
		}

		public void onSpanRemoved(Spannable buf, Object what, int s, int e) {
		}
	}

    public void handleTextChanged(CharSequence buffer, int start, int before,
            int after) {
        /**
         * @j2sNative
         * var thisText = document.getElementById(this.getUIElementID());
         * if (thisText != null) {
         *    if (thisText.tagName == "DIV") {
         *        thisText.innerHTML = this.mText.toString();
         *    } else {
         *        thisText.value = this.mText.toString();
         *    }
         *    // For bug 848, scrollLeft when text changed
         *    if (this.mSingleLine) {
         *       if (thisText.style.textAlign == "right") {
         *          thisText.scrollLeft = thisText.scrollWidth - thisText.clientWidth 
         *                                + this.getCompoundPaddingRight() + this.getCompoundPaddingLeft();
         *       }
         *    }
         * }
         */{}

        onTextChanged(buffer, start, before, after);
    }

	protected void onDraw(Canvas canvas) {
        // Draw the background for this view
        super.onDraw(canvas);

        final int compoundPaddingLeft = getCompoundPaddingLeft();
        final int compoundPaddingTop = getCompoundPaddingTop();
        final int compoundPaddingRight = getCompoundPaddingRight();
        final int compoundPaddingBottom = getCompoundPaddingBottom();
        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        final int right = mRight;
        final int left = mLeft;
        final int bottom = mBottom;
        final int top = mTop;

        final Drawables dr = mDrawables;

        if (dr != null) {
            /*
             * Compound, not extended, because the icon is not clipped
             * if the text height is smaller.
             */

            int vspace = bottom - top - compoundPaddingBottom - compoundPaddingTop;
            int hspace = right - left - compoundPaddingRight - compoundPaddingLeft;

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableLeft != null) {
                canvas.save();
                canvas.translate(scrollX + mPaddingLeft,
                                 scrollY + compoundPaddingTop +
                                 (vspace - dr.mDrawableHeightLeft) / 2);
                dr.mDrawableLeft.draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableRight != null) {
                canvas.save();
                canvas.translate(scrollX + right - left - mPaddingRight - dr.mDrawableSizeRight,
                         scrollY + compoundPaddingTop + (vspace - dr.mDrawableHeightRight) / 2);
                dr.mDrawableRight.draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableTop != null) {
                canvas.save();
                float tempX=scrollX + compoundPaddingLeft + (hspace - dr.mDrawableWidthTop)/2;
                float tempY=scrollY + mPaddingTop;
                System.out.println("canvas translate X:"+tempX);
                System.out.println("canvas translate Y:"+tempY);
                canvas.translate(scrollX + compoundPaddingLeft + (hspace - dr.mDrawableWidthTop) / 2,
                        scrollY + mPaddingTop);
                dr.mDrawableTop.draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableBottom != null) {
                canvas.save();
                canvas.translate(scrollX + compoundPaddingLeft +
                        (hspace - dr.mDrawableWidthBottom) / 2,
                         scrollY + bottom - top - mPaddingBottom - dr.mDrawableSizeBottom);
                dr.mDrawableBottom.draw(canvas);
                canvas.restore();
            }
        }

        // draw the text element
        drawText();
	}

    private int getTextHeight() {
        int cNum = mOneLineChNum > 0 ? mOneLineChNum : mTextCharNum;
        boolean textarea = true;
        String temp = "";
        Object text = null;
        if (mSingleLine && this instanceof EditText)
            textarea = false;
        /**
         * @j2sNative
         * text = document.getElementById(this.getUIElementID());
         */{}
        if (text != null) {
            if (textarea) {
                if (mEditable) {
                    /**
                     * @j2sNative
                     * temp = text.value;
                     */{}
                     return getTextareaParams(temp, cNum, 1, 2);
                } else {
                    /**
                     * @j2sNative
                     * temp = text.innerHTML;
                     */{}
                    return getDivParams(temp, mSingleLine, 2);
                }
            } else {
                /**
                 * @j2sNative
                 * temp = text.value;
                 */{}
                return getInputParams(temp, 2);
            }
        }
         return 0;
    }

    /////////////////////////////////////////////////////////////////////////

    private int getVerticalOffset(boolean forceNormal) {
        int voffset = 0;
        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        Layout l = mLayout;
//        if (!forceNormal && mText.length() == 0 && mHintLayout != null) {
//            l = mHintLayout;
//        }

        if (gravity != Gravity.TOP) {
            int boxht;

/*            if (l == mHintLayout) {
                boxht = getMeasuredHeight() - getCompoundPaddingTop() -
                        getCompoundPaddingBottom();
            } else*/ {
                boxht = getMeasuredHeight() - getExtendedPaddingTop() - getExtendedPaddingBottom();
            }

            int textht = 0;
            if (l != null) {
                textht = l.getHeight();
            }

            if (textht < boxht) {
                if (gravity == Gravity.BOTTOM)
                    voffset = boxht - textht;
                else // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxht - textht) >> 1;
            }
        }
        return voffset;
    }

    public void setTextAppearance(Context context, int resid) {
        TypedArray appearance =
                context.obtainStyledAttributes(resid,
                                           com.android.internal.R.styleable.TextAppearance);

        int color;
        ColorStateList colors;
        int ts;

//        color = appearance.getColor(com.android.internal.R.styleable.TextAppearance_textColorHighlight, 0);
//        if (color != 0) {
//            setHighlightColor(color);
//        }

        colors = appearance.getColorStateList(com.android.internal.R.styleable.
                                              TextAppearance_textColor);
        if (colors != null) {
            setTextColor(colors);
        }

        ts = appearance.getDimensionPixelSize(com.android.internal.R.styleable.
                                              TextAppearance_textSize, 0);
        if (ts != 0) {
            setRawTextSize(ts);
        }

//        colors = appearance.getColorStateList(com.android.internal.R.styleable.TextAppearance_textColorHint);
//        if (colors != null) {
//            setHintTextColor(colors);
//        }

        colors = appearance.getColorStateList(com.android.internal.R.styleable.TextAppearance_textColorLink);
        if (colors != null) {
            setLinkTextColor(colors);
        }

        int typefaceIndex, styleIndex;

        typefaceIndex = appearance.getInt(com.android.internal.R.styleable.
                                          TextAppearance_typeface, -1);
        styleIndex = appearance.getInt(com.android.internal.R.styleable.
                                       TextAppearance_textStyle, -1);

        setTypefaceByIndex(typefaceIndex, styleIndex);
        appearance.recycle();
    }

    /**
     * @j2sKeep
     */
    private void setTextAreaAlignment() {
        // From spec, text alignment only take effect when text size is smaller than View block
        int voffsetText = 0;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            voffsetText = getVerticalOffset(false);
        }
        if ((mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) != Gravity.LEFT) {
            CharSequence gravity = "center";
            if ((mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
                gravity = "right";
            }
            /**
             * @j2sNative
             * text = document.getElementById(this.getUIElementID());
             * text.style.textAlign = gravity;
             */{}
        }
        int x = getCompoundPaddingLeft();
        int y = getExtendedPaddingTop() + voffsetText;
        int paddingRight = getCompoundPaddingRight();
        int paddingBottom = getExtendedPaddingBottom();
        /**
         * @j2sNative
         * text = document.getElementById(this.getUIElementID());
         * if (x > 0)
         *     text.style.paddingLeft = x + "px";
         * if (y > 0)
         *     text.style.paddingTop = y + "px";
         * if (paddingRight > 0)
         *     text.style.paddingRight = paddingRight + "px";
         * if (paddingBottom > 0)
         *     text.style.paddingBottom = paddingBottom + "px";
         * text.style.height = this.mMeasuredHeight - y - paddingBottom + "px";
         * text.style.width = this.mMeasuredWidth - x - paddingRight + "px";
         */{}
    }

	/**
	 * @j2sKeep
	 */
    private void setFontProperties(String id) {
        Typeface tf = mTextPaint.getTypeface();
        String color = Color.toString(this.mCurTextColor);
        /**
         * @j2sNative
         * var thisText = document.getElementById(id);
         * // set font style
         * if (tf != null) {
         *     switch (tf.getStyle()) {
         *         case 0:
         *             thisText.style.fontStyle = "normal";
         *             break;
         *         case 1:
         *             thisText.style.fontWeight = "bold";
         *             break;
         *         case 2:
         *             thisText.style.fontStyle = "italic";
         *             break;
         *         case 3:
         *             thisText.style.fontWeight = "bold";
         *             thisText.style.fontStyle = "italic";
         *             break;
         *         default:
         *             thisText.style.fontWeight = "bold";
         *             thisText.style.fontStyle = "italic";
         *             break;
         *     }
         *     // set font family
         *     if (tf.getFamilyName() == "sans-serif" || tf.getFamilyName() == "serif" || tf.getFamilyName() == "monospace") {
         *         thisText.style.fontFamily = tf.getFamilyName();
         *     } else if (tf.getFamilyName()) {
         *         thisText.style.fontFamily = "sans-serif";
         *       //   console.loge("We don't support this font: " + tf.getFamilyName());
         *     }
         * } else {
         *     // if tf is null thus set font default
         *     thisText.style.fontWeight = "normal";
         *     thisText.style.fontStyle = "normal";
         *     thisText.style.fontFamily = "serif";
         * }
         * // set font color and size
         * thisText.style.fontSize = this.mTextPaint.getTextSize() + "px";
         * thisText.style.color = color;
         */{}
    }

    private void drawText() {
        int cNum = mOneLineChNum > 0 ? mOneLineChNum : mTextCharNum;
        final int typeNull = InputType.TYPE_NULL;
        String mTemp;
        if (mText instanceof SpannedString) {
            mTemp = Html.toHtml((SpannedString) mText);
        } else {
            mTemp = mText.toString();
        }
        String repText = mTemp;
        if (mSingleLine) {
            repText = repText.replaceAll("<br/>", " ");
        }

        /**
         * @j2sNative
         * var thisText = document.getElementById(this.getUIElementID());
         * if (thisText == null || thisText.tagName == "SPAN" || thisText.tagName == "DIV") {
         *     if (this.mEditable) {
         *         var text = document.createElement("textarea");
         *         text.value = this.mText.toString();
         *     } else {
         *         var text = document.createElement("div");
         *         if (repText.length > cNum) {
         *             text.style.wordWrap = "break-word";
         *         }
         *         if (repText.isHtml) {
         *             text.innerHTML = repText;
         *         } else {
         *             text.innerText = repText;
         *         }
         *     }
         *     if (this.mSingleLine) {
         *         text.style.whiteSpace = "nowrap";
         *     } else {
         *         text.style.whiteSpace = "pre-wrap";
         *     }
         *     text.style.textAlign = "left";
         *     text.style.background = "transparent";
         *     text.style.overflow = "hidden";
         *     text.style.border = "none";
         *     text.style.resize = "none";
         *     text.style.position = "absolute";
         *     text.style.outline = "none";
         *     text.style.cursor = "default";
         *     text.style.padding = "0px";
         *     text.style.margin = "0px";
         *     text.id = this.getUIElementID();
         *     var parentId = this.getParent().getUIElementID();
         *     var curView = document.getElementById(parentId);
         *     if (thisText == null) {
         *        var viewRootId = this.getRootView().getParent().getViewRootID();
         *        var viewRoot = document.getElementById(viewRootId);
         *        if( curView != null) {
         *            curView.appendChild(text);
         *        } else {
         *            viewRoot.childNodes[1].appendChild(text); // append to decorview
         *        }
         *     } else {
         *         thisText.parentNode.replaceChild(text, thisText);
         *         text.style.left = thisText.style.left;
         *         text.style.top = thisText.style.top;
         *         text.style.right = thisText.style.right;
         *         text.style.bottom = thisText.style.bottom;
         *     }
         *     thisText = text;
         * }
         * if (!android.util.DebugUtils.DEBUG_VIEW_IN_BROWSER) {
         *    thisText.style.left = this.getAbsoluteLeft() + "px";
         *    thisText.style.top = this.getAbsoluteTop() + "px";
         *    thisText.style.right = this.getAbsoluteLeft() + this.getWidth() + "px";
         *    thisText.style.bottom = this.getAbsoluteTop() + this.getHeight() + "px";
         *    if (this.mAttachInfo != null && this.mAttachInfo.getScrollContainers() != null) {
         *        var length = this.mAttachInfo.getScrollContainers().size();
         *        var scrollContainers = this.mAttachInfo.getScrollContainers();
         *        for (var i = 0; i < length; i++) {
         *            if (thisText != null && thisText.tagName == "DIV" && (thisText.id == scrollContainers.get(i).getUIElementID() + 1 || parentId == scrollContainers.get(i).getUIElementID() + 1)) {
         *                this.clipHeight = scrollContainers.get(i).getHeight();
         *                this.clipWidth = scrollContainers.get(i).getWidth();
         *                this.clipMoveY = scrollContainers.get(i).getScrollY();
         *                this.clipMoveX = scrollContainers.get(i).getScrollX();
         *                var clipTop = this.clipMoveY + "px,";
         *                var clipRight = this.clipMoveX + this.getWidth() + "px,";
         *                var clipBottom = this.clipMoveY + this.clipHeight +"px,";
         *                var clipLeft = this.clipMoveX + "px";
         *                if(thisText.id == scrollContainers.get(i).getUIElementID() + 1) {
         *                    thisText.style.clip = "rect(" + clipTop + clipRight + clipBottom + clipLeft + ")";
         *                } else if (parentId == scrollContainers.get(i).getUIElementID() + 1 && curView != null) {
         *                    curView.style.clip = "rect(0px," + this.clipWidth+ "px," + this.clipHeight + "px,0px)";
         *                }
         *                break;
         *            }
         *        }
         *    }
         * }
         * thisText.placeholder = this.mHint;
         * this.setFontProperties(thisText.id);
         */{}
         setTextAreaAlignment();
	}

	/**
	 * Convenience for {@link Selection#getSelectionStart}.
	 */
	public int getSelectionStart() {
		return Selection.getSelectionStart(getText());
	}

	/**
	 * Convenience for {@link Selection#getSelectionEnd}.
	 */
	public int getSelectionEnd() {
		return Selection.getSelectionEnd(getText());
	}

	/**
	 * Return true iff there is a selection inside this text view.
	 */
	public boolean hasSelection() {
		final int selectionStart = getSelectionStart();
		final int selectionEnd = getSelectionEnd();

		return selectionStart >= 0 && selectionStart != selectionEnd;
	}

    /**
     * Causes words in the text that are longer than the view is wide
     * to be ellipsized instead of broken in the middle.  You may also
     * want to {@link #setSingleLine} or {@link #setHorizontallyScrolling}
     * to constrain the text to a single line.  Use <code>null</code>
     * to turn off ellipsizing.
     *
     * @attr ref android.R.styleable#TextView_ellipsize
     */
    public void setEllipsize(TextUtils.TruncateAt where) {
        mEllipsize = where;

        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }
    
    /**
     * Returns where, if anywhere, words that are longer than the view
     * is wide should be ellipsized.
     */
    public TextUtils.TruncateAt getEllipsize() {
        return mEllipsize;
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(DEBUG_EXTRACT) System.out.println("TextView onKeyDown");
		int which = doKeyDown(keyCode, event, null);
		if (which == 0) {
			// Go through default dispatching.
			return super.onKeyDown(keyCode, event);
		}

		return true;
	}

	// This is just a demo Move BaseKeyListener's implementation here temporary,
	// For formal product we should keep android's original architecture
	// instead.
	/**
	 * Performs the action that happens when you press the DEL key in a
	 * TextView. If there is a selection, deletes the selection; otherwise, DEL
	 * alone deletes the character before the cursor, if any; ALT+DEL deletes
	 * everything on the line the cursor is on.
	 *
	 * @return true if anything was deleted; false otherwise.
	 */
	public boolean backspace(Editable content, int keyCode, KeyEvent event) {
		System.out.println("backspace1");
		int selStart, selEnd;
		boolean result = true;

		{
			System.out.println("backspace2");
			int a = Selection.getSelectionStart(content);
			int b = Selection.getSelectionEnd(content);

			selStart = Math.min(a, b);
			selEnd = Math.max(a, b);
			System.out.println("a " + a + " b " + b);
			System.out.println("selStart " + selStart + " selEnd " + selEnd);
		}

		if (selStart != selEnd) {
			System.out.println("backspace3");
			content.delete(selStart, selEnd);
		} else {
			int to = TextUtils.getOffsetBefore(content, selEnd);

			if (to != selEnd) {
				content.delete(Math.min(to, selEnd), Math.max(to, selEnd));
			} else {
				result = false;
			}
		}

		System.out.println("backspace4");

		return result;
	}

    private int doKeyDown(int keyCode, KeyEvent event, KeyEvent otherEvent) {
        if (DEBUG_EXTRACT)
            System.out.println("doKeyDown");
        if (!isEnabled()) {
            if (DEBUG_EXTRACT)
                System.out.println("doKeyDown 1");
            return 0;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            System.out.println("mText class " + mText.getClass().getName()
                    + " " + (mText instanceof Editable));
            if (mBufferType == BufferType.EDITABLE && mText != "" && mText != null) {
                System.out.println("backspace");
                Editable et = (Editable) mText;
                    backspace(et, keyCode, event);
                return 1;
            }
        }
        if (DEBUG_EXTRACT)
            System.out.println("keyCode: " + keyCode);
        return 0;
    }

	protected int mGravity = Gravity.TOP | Gravity.LEFT;

	/**
	 * Returns the horizontal and vertical alignment of this TextView.
	 *
	 * @see android.view.Gravity
	 * @attr ref android.R.styleable#TextView_gravity
	 */
	public int getGravity() {
		return mGravity;
	}

	/**
	 * Sets the horizontal alignment of the text and the
	 * vertical gravity that will be used when there is extra space
	 * in the TextView beyond what is required for the text itself.
	 *
	 * @see android.view.Gravity
	 * @attr ref android.R.styleable#TextView_gravity
	 */
	public void setGravity(int gravity) {
		if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
			gravity |= Gravity.LEFT;
		}
		if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
			gravity |= Gravity.TOP;
		}

		boolean newLayout = false;

		if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) != 
		    (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK)) {
		    newLayout = true;
		    }

		if (gravity != mGravity) {
			invalidate();
		}

		mGravity = gravity;

		if (mLayout != null && newLayout) {
		 // XXX this is heavy-handed because no actual content changes.
		    int want = mLayout.getWidth();
		    int hintWant = mHintLayout == null ? 0 : mHintLayout.getWidth();

		    makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING, 
		            mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight(), true);
		}
	}

    /**
     * Makes the TextView at least this many pixels tall
     *
     * @attr ref android.R.styleable#TextView_minHeight
     */
    public void setMinHeight(int minHeight) {
        mMinimum = minHeight;
        mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView at most this many lines tall
     *
     * @attr ref android.R.styleable#TextView_maxLines
     */
    public void setMaxLines(int maxlines) {
        mMaximum = maxlines;
        mMaxMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView at most this many pixels tall
     *
     * @attr ref android.R.styleable#TextView_maxHeight
     */
    public void setMaxHeight(int maxHeight) {
        mMaximum = maxHeight;
        mMaxMode = PIXELS;

        requestLayout();
        invalidate();
    }

	@Override
    protected boolean verifyDrawable(Drawable who) {
        final boolean verified = super.verifyDrawable(who);
        if (!verified && mDrawables != null) {
            return who == mDrawables.mDrawableLeft || who == mDrawables.mDrawableTop ||
                    who == mDrawables.mDrawableRight || who == mDrawables.mDrawableBottom;
        }
        return verified;
    }

	@Override
    public boolean performLongClick() {
        if (super.performLongClick()) {
            mEatTouchRelease = true;
            return true;
        }

        return false;
    }

	@Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mTemporaryDetach = false;

        if (mShowErrorAfterAttach) {
            //showError();
            mShowErrorAfterAttach = false;
        }

        final ViewTreeObserver observer = getViewTreeObserver();
        if (observer != null) {
            if (mInsertionPointCursorController != null) {
                observer.addOnTouchModeChangeListener(mInsertionPointCursorController);
            }
            if (mSelectionModifierCursorController != null) {
                observer.addOnTouchModeChangeListener(mSelectionModifierCursorController);
            }
        }
    }

	@Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        final ViewTreeObserver observer = getViewTreeObserver();
        if (observer != null) {
            if (mPreDrawState != PREDRAW_NOT_REGISTERED) {
                //observer.removeOnPreDrawListener(this);
                mPreDrawState = PREDRAW_NOT_REGISTERED;
            }
            if (mInsertionPointCursorController != null) {
                observer.removeOnTouchModeChangeListener(mInsertionPointCursorController);
            }
            if (mSelectionModifierCursorController != null) {
                observer.removeOnTouchModeChangeListener(mSelectionModifierCursorController);
            }
        }

        if (mError != null) {
            //hideError();
        }

        if (mBlink != null) {
            mBlink.cancel();
        }

        if (mInsertionPointCursorController != null) {
            mInsertionPointCursorController.onDetached();
        }

        if (mSelectionModifierCursorController != null) {
            mSelectionModifierCursorController.onDetached();
        }

        //hideControllers();
    }

	@Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();
        // Only track when onStartTemporaryDetach() is called directly,
        // usually because this instance is an editable field in a list
        if (!mDispatchTemporaryDetach) mTemporaryDetach = true;
    }

    @Override
    public void onFinishTemporaryDetach() {
        super.onFinishTemporaryDetach();
        // Only track when onStartTemporaryDetach() is called directly,
        // usually because this instance is an editable field in a list
        if (!mDispatchTemporaryDetach) mTemporaryDetach = false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (!isShown()) {
            return false;
        }

        // TODO: final boolean isPassword = isPasswordInputType(mInputType);
        final boolean isPassword = false;

        if (!isPassword) {
            CharSequence text = getText();
            if (TextUtils.isEmpty(text)) {
                text = getHint();
            }
            if (!TextUtils.isEmpty(text)) {
                if (text.length() > AccessibilityEvent.MAX_TEXT_LENGTH) {
                    text = text.subSequence(0, AccessibilityEvent.MAX_TEXT_LENGTH + 1);
                }
                event.getText().add(text);
            }
        } else {
            event.setPassword(isPassword);
        }
        return false;
    }


    /**
     * A CursorController instance can be used to control a cursor in the text.
     * It is not used outside of {@link TextView}.
     * @hide
     */
    private interface CursorController extends ViewTreeObserver.OnTouchModeChangeListener {
        /**
         * Makes the cursor controller visible on screen. Will be drawn by {@link #draw(Canvas)}.
         * See also {@link #hide()}.
         */
        public void show();

        /**
         * Hide the cursor controller from screen.
         * See also {@link #show()}.
         */
        public void hide();

        /**
         * @return true if the CursorController is currently visible
         */
        public boolean isShowing();

        /**
         * Update the controller's position.
         */
        public void updatePosition(HandleView handle, int x, int y);

        public void updatePosition();

        /**
         * This method is called by {@link #onTouchEvent(MotionEvent)} and gives the controller
         * a chance to become active and/or visible.
         * @param event The touch event
         */
        public boolean onTouchEvent(MotionEvent event);

        /**
         * Called when the view is detached from window. Perform house keeping task, such as
         * stopping Runnable thread that would otherwise keep a reference on the context, thus
         * preventing the activity to be recycled.
         */
        public void onDetached();
    }

    private class HandleView extends View {
        private boolean mPositionOnTop = false;
        private Drawable mDrawable;
        //private PopupWindow mContainer;
        private int mPositionX;
        private int mPositionY;
        private CursorController mController;
        private boolean mIsDragging;
        private float mTouchToWindowOffsetX;
        private float mTouchToWindowOffsetY;
        private float mHotspotX;
        private float mHotspotY;
        private int mHeight;
        private float mTouchOffsetY;
        private int mLastParentX;
        private int mLastParentY;

        public static final int LEFT = 0;
        public static final int CENTER = 1;
        public static final int RIGHT = 2;

        public HandleView(CursorController controller, int pos) {
            super(TextView.this.mContext);
            mController = controller;
            /*
            mContainer = new PopupWindow(TextView.this.mContext, null,
                    com.android.internal.R.attr.textSelectHandleWindowStyle);
            mContainer.setSplitTouchEnabled(true);
            mContainer.setClippingEnabled(false);
            mContainer.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
			*/
            setOrientation(pos);
        }

        public void setOrientation(int pos) {
            int handleWidth;
            switch (pos) {
            case LEFT: {
                if (mSelectHandleLeft == null) {
                    mSelectHandleLeft = mContext.getResources().getDrawable(
                            mTextSelectHandleLeftRes);
                }
                mDrawable = mSelectHandleLeft;
                handleWidth = mDrawable.getIntrinsicWidth();
                mHotspotX = (handleWidth * 3) / 4;
                break;
            }

            case RIGHT: {
                if (mSelectHandleRight == null) {
                    mSelectHandleRight = mContext.getResources().getDrawable(
                            mTextSelectHandleRightRes);
                }
                mDrawable = mSelectHandleRight;
                handleWidth = mDrawable.getIntrinsicWidth();
                mHotspotX = handleWidth / 4;
                break;
            }

            case CENTER:
            default: {
                if (mSelectHandleCenter == null) {
                    mSelectHandleCenter = mContext.getResources().getDrawable(
                            mTextSelectHandleRes);
                }
                mDrawable = mSelectHandleCenter;
                handleWidth = mDrawable.getIntrinsicWidth();
                mHotspotX = handleWidth / 2;
                break;
            }
            }

            final int handleHeight = mDrawable.getIntrinsicHeight();

            mTouchOffsetY = -handleHeight * 0.3f;
            mHotspotY = 0;
            mHeight = handleHeight;
            invalidate();
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(mDrawable.getIntrinsicWidth(),
                    mDrawable.getIntrinsicHeight());
        }

        public void show() {
            if (!isPositionVisible()) {
                hide();
                return;
            }
            //mContainer.setContentView(this);
            final int[] coords = mTempCoords;
            TextView.this.getLocationInWindow(coords);
            coords[0] += mPositionX;
            coords[1] += mPositionY;
            //mContainer.showAtLocation(TextView.this, 0, coords[0], coords[1]);
        }

        public void hide() {
            mIsDragging = false;
            //mContainer.dismiss();
        }

        public boolean isShowing() {
        	return false;
            //return mContainer.isShowing();
        }

        private boolean isPositionVisible() {
            // Always show a dragging handle.
            if (mIsDragging) {
                return true;
            }

            /*
            if (isInBatchEditMode()) {
                return false;
            }
            */

            final int extendedPaddingTop = getExtendedPaddingTop();
            final int extendedPaddingBottom = getExtendedPaddingBottom();
            final int compoundPaddingLeft = getCompoundPaddingLeft();
            final int compoundPaddingRight = getCompoundPaddingRight();

            final TextView hostView = TextView.this;
            final int left = 0;
            final int right = hostView.getWidth();
            final int top = 0;
            final int bottom = hostView.getHeight();

            if (mTempRect == null) {
                mTempRect = new Rect();
            }
            final Rect clip = mTempRect;
            clip.left = left + compoundPaddingLeft;
            clip.top = top + extendedPaddingTop;
            clip.right = right - compoundPaddingRight;
            clip.bottom = bottom - extendedPaddingBottom;

            final ViewParent parent = hostView.getParent();
            if (parent == null || !parent.getChildVisibleRect(hostView, clip, null)) {
                return false;
            }

            final int[] coords = mTempCoords;
            hostView.getLocationInWindow(coords);
            final int posX = coords[0] + mPositionX + (int) mHotspotX;
            final int posY = coords[1] + mPositionY + (int) mHotspotY;

            return posX >= clip.left && posX <= clip.right &&
                    posY >= clip.top && posY <= clip.bottom;
        }

        private void moveTo(int x, int y) {
            mPositionX = x - TextView.this.mScrollX;
            mPositionY = y - TextView.this.mScrollY;
            if (isPositionVisible()) {
                int[] coords = null;
                /*
                if (mContainer.isShowing()) {
                    coords = mTempCoords;
                    TextView.this.getLocationInWindow(coords);
                    mContainer.update(coords[0] + mPositionX, coords[1] + mPositionY,
                            mRight - mLeft, mBottom - mTop);
                } else {
                    show();
                }
                */

                if (mIsDragging) {
                    if (coords == null) {
                        coords = mTempCoords;
                        TextView.this.getLocationInWindow(coords);
                    }
                    if (coords[0] != mLastParentX || coords[1] != mLastParentY) {
                        mTouchToWindowOffsetX += coords[0] - mLastParentX;
                        mTouchToWindowOffsetY += coords[1] - mLastParentY;
                        mLastParentX = coords[0];
                        mLastParentY = coords[1];
                    }
                }
            } else {
                hide();
            }
        }

        @Override
        public void onDraw(Canvas c) {
            mDrawable.setBounds(0, 0, mRight - mLeft, mBottom - mTop);
            if (mPositionOnTop) {
                c.save();
                c.rotate(180, (mRight - mLeft) / 2, (mBottom - mTop) / 2);
                mDrawable.draw(c);
                c.restore();
            } else {
                mDrawable.draw(c);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                final float rawX = ev.getRawX();
                final float rawY = ev.getRawY();
                mTouchToWindowOffsetX = rawX - mPositionX;
                mTouchToWindowOffsetY = rawY - mPositionY;
                final int[] coords = mTempCoords;
                TextView.this.getLocationInWindow(coords);
                mLastParentX = coords[0];
                mLastParentY = coords[1];
                mIsDragging = true;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float rawX = ev.getRawX();
                final float rawY = ev.getRawY();
                final float newPosX = rawX - mTouchToWindowOffsetX + mHotspotX;
                final float newPosY = rawY - mTouchToWindowOffsetY + mHotspotY + mTouchOffsetY;

                mController.updatePosition(this, Math.round(newPosX), Math.round(newPosY));

                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
            }
            return true;
        }

        public boolean isDragging() {
            return mIsDragging;
        }

        /*
        void positionAtCursor(final int offset, boolean bottom) {
            final int width = mDrawable.getIntrinsicWidth();
            final int height = mDrawable.getIntrinsicHeight();
            final int line = mLayout.getLineForOffset(offset);
            final int lineTop = mLayout.getLineTop(line);
            final int lineBottom = mLayout.getLineBottom(line);

            final Rect bounds = sCursorControllerTempRect;
            bounds.left = (int) (mLayout.getPrimaryHorizontal(offset) - mHotspotX)
                + TextView.this.mScrollX;
            bounds.top = (bottom ? lineBottom : lineTop - mHeight) + TextView.this.mScrollY;

            bounds.right = bounds.left + width;
            bounds.bottom = bounds.top + height;

            convertFromViewportToContentCoordinates(bounds);
            moveTo(bounds.left, bounds.top);
        }
        */
    }

    private static class Blink extends Handler implements Runnable {
        private final WeakReference<TextView> mView;
        private boolean mCancelled;

        public Blink(TextView v) {
            mView = new WeakReference<TextView>(v);
        }

        public void run() {
            if (mCancelled) {
                return;
            }

            removeCallbacks(Blink.this);

            TextView tv = mView.get();

            if (tv != null && tv.isFocused()) {
                int st = tv.getSelectionStart();
                int en = tv.getSelectionEnd();

                if (st == en && st >= 0 && en >= 0) {
                	/*
                    if (tv.mLayout != null) {
                        tv.invalidateCursorPath();
                    }
					*/
                    postAtTime(this, SystemClock.uptimeMillis() + BLINK);
                }
            }
        }

        void cancel() {
            if (!mCancelled) {
                removeCallbacks(Blink.this);
                mCancelled = true;
            }
        }

        void uncancel() {
            mCancelled = false;
        }
    }

    /**
     * @return the Layout that is currently being used to display the text.
     * This can be null if the text or width has recently changes.
     */
    public final Layout getLayout() {
        return mLayout;
    }

    /**
     * Sets the key listener to be used with this TextView.  This can be null
     * to disallow user input.  Note that this method has significant and
     * subtle interactions with soft keyboards and other input method:
     * see {@link KeyListener#getInputType() KeyListener.getContentType()}
     * for important details.  Calling this method will replace the current
     * content type of the text view with the content type returned by the
     * key listener.
     * <p>
     * Be warned that if you want a TextView with a key listener or movement
     * method not to be focusable, or if you want a TextView without a
     * key listener or movement method to be focusable, you must call
     * {@link #setFocusable} again after calling this to get the focusability
     * back the way you want it.
     *
     * @attr ref android.R.styleable#TextView_numeric
     * @attr ref android.R.styleable#TextView_digits
     * @attr ref android.R.styleable#TextView_phoneNumber
     * @attr ref android.R.styleable#TextView_inputMethod
     * @attr ref android.R.styleable#TextView_capitalize
     * @attr ref android.R.styleable#TextView_autoText
     */
    public void setKeyListener(KeyListener input) {
        setKeyListenerOnly(input);
//        fixFocusableAndClickableSettings();

        if (input != null) {
            try {
                mInputType = mInput.getInputType();
            } catch (IncompatibleClassChangeError e) {
                mInputType = InputType.TYPE_CLASS_TEXT;
            }
            if ((mInputType&InputType.TYPE_MASK_CLASS)
                    == InputType.TYPE_CLASS_TEXT) {
                if (mSingleLine) {
                    mInputType &= ~InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                } else {
                    mInputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                }
            }
        } else {
            mInputType = InputType.TYPE_NULL;
        }
    }

    private void setKeyListenerOnly(KeyListener input) {
        mInput = input;
//        if (mInput != null && !(mText instanceof Editable))
//            setText(mText);

//        setFilters((Editable) mText, mFilters);
    }
    
    /**
     * @return the movement method being used for this TextView.
     * This will frequently be null for non-EditText TextViews.
     */
    public final MovementMethod getMovementMethod() {
        return mMovement;
    }

    /**
     * Sets the movement method (arrow key handler) to be used for
     * this TextView.  This can be null to disallow using the arrow keys
     * to move the cursor or scroll the view.
     * <p>
     * Be warned that if you want a TextView with a key listener or movement
     * method not to be focusable, or if you want a TextView without a
     * key listener or movement method to be focusable, you must call
     * {@link #setFocusable} again after calling this to get the focusability
     * back the way you want it.
     */
    public final void setMovementMethod(MovementMethod movement) {
        mMovement = movement;

        if (mMovement != null && !(mText instanceof Spannable))
            setText(mText);

        fixFocusableAndClickableSettings();

        // SelectionModifierCursorController depends on textCanBeSelected, which depends on mMovement
        prepareCursorControllers();
    }
    
    private void fixFocusableAndClickableSettings() {
        if ((mMovement != null) || mInput != null) {
            setFocusable(true);
            setClickable(true);
            setLongClickable(true);
        } else {
            setFocusable(false);
            setClickable(false);
            setLongClickable(false);
        }
    }
    
    private void prepareCursorControllers() {
        boolean windowSupportsHandles = false;

        ViewGroup.LayoutParams params = getRootView().getLayoutParams();
        if (params instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams) params;
            windowSupportsHandles = windowParams.type < WindowManager.LayoutParams.FIRST_SUB_WINDOW
                    || windowParams.type > WindowManager.LayoutParams.LAST_SUB_WINDOW;
        }

        // TODO Add an extra android:cursorController flag to disable the controller?
        mInsertionControllerEnabled = windowSupportsHandles && mCursorVisible && mLayout != null;
        mSelectionControllerEnabled = windowSupportsHandles && textCanBeSelected() &&
                mLayout != null;

        if (!mInsertionControllerEnabled) {
            mInsertionPointCursorController = null;
        }

        if (!mSelectionControllerEnabled) {
            // Stop selection mode if the controller becomes unavailable.
            stopTextSelectionMode();
            mSelectionModifierCursorController = null;
        }
    }
    
    private boolean textCanBeSelected() {
        // prepareCursorController() relies on this method.
        // If you change this condition, make sure prepareCursorController is called anywhere
        // the value of this condition might be changed.
        return (mText instanceof Spannable &&
                mMovement != null &&
                mMovement.canSelectArbitrarily());
    }
    
    private void stopTextSelectionMode() {
        if (mIsInTextSelectionMode) {
            Selection.setSelection((Spannable) mText, getSelectionEnd());
            hideSelectionModifierCursorController();
            mIsInTextSelectionMode = false;
        }
    }
    
    private void hideSelectionModifierCursorController() {
        if (mSelectionModifierCursorController != null) {
            mSelectionModifierCursorController.hide();
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: setMarqueeRepeatLimit");
     */
    @MayloonStubAnnotation()
    public void setMarqueeRepeatLimit(int marqueeLimit) {
        System.out.println("Stub" + " Function : setMarqueeRepeatLimit");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setRawInputType");
     */
    @MayloonStubAnnotation()
    public void setRawInputType(int type) {
        System.out.println("Stub" + " Function : setRawInputType");
        return;
    }

    public void setOnEditorActionListener(OnEditorActionListener l) {
        if (mInputContentType == null) {
            mInputContentType = new InputContentType();
        }
        mInputContentType.onEditorActionListener = l;
    }

    public void setEms(int ems) {
        mMaxWidth = mMinWidth = (ems * getLineHeight());
//        mMaxWidthMode = mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * @j2sNative
     * console.log("Missing method: onTextContextMenuItem");
     */
    @MayloonStubAnnotation()
    public boolean onTextContextMenuItem(int id) {
        System.out.println("Stub" + " Function : onTextContextMenuItem");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getImeActionLabel");
     */
    @MayloonStubAnnotation()
    public CharSequence getImeActionLabel() {
        System.out.println("Stub" + " Function : getImeActionLabel");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clearComposingText");
     */
    @MayloonStubAnnotation()
    public void clearComposingText() {
        System.out.println("Stub" + " Function : clearComposingText");
        return;
    }

    public void setMinWidth(int minpixels) {
        mMinWidth = minpixels;
//        mMinWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    public void setMinLines(int minlines) {
        mMinimum = minlines;
        mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * @j2sNative
     * console.log("Missing method: onBeginBatchEdit");
     */
    @MayloonStubAnnotation()
    public void onBeginBatchEdit() {
        System.out.println("Stub" + " Function : onBeginBatchEdit");
        return;
    }

    public final void setLinksClickable(boolean whether) {
        mLinksClickable = whether;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isInputMethodTarget");
     */
    @MayloonStubAnnotation()
    public boolean isInputMethodTarget() {
        System.out.println("Stub" + " Function : isInputMethodTarget");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: beginBatchEdit");
     */
    @MayloonStubAnnotation()
    public void beginBatchEdit() {
        System.out.println("Stub" + " Function : beginBatchEdit");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onEditorAction");
     */
    @MayloonStubAnnotation()
    public void onEditorAction(int actionCode) {
        System.out.println("Stub" + " Function : onEditorAction");
        return;
    }

    public final void setLinkTextColor(int color) {
        mLinkTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    /**
     * Sets the color of links in the text.
     *
     * @attr ref android.R.styleable#TextView_textColorLink
     */
    public final void setLinkTextColor(ColorStateList colors) {
        mLinkTextColor = colors;
        updateTextColors();
    }

    /**
     * <p>Returns the color used to paint links in the text.</p>
     *
     * @return Returns the list of link text colors.
     */
    public final ColorStateList getLinkTextColors() {
        return mLinkTextColor;
    }
    /**
     * @j2sNative
     * console.log("Missing method: getCompoundDrawablePadding");
     */
    @MayloonStubAnnotation()
    public int getCompoundDrawablePadding() {
        System.out.println("Stub" + " Function : getCompoundDrawablePadding");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setSelectAllOnFocus");
     */
    @MayloonStubAnnotation()
    public void setSelectAllOnFocus(boolean selectAllOnFocus) {
        System.out.println("Stub" + " Function : setSelectAllOnFocus");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getPrivateImeOptions");
     */
    @MayloonStubAnnotation()
    public String getPrivateImeOptions() {
        System.out.println("Stub" + " Function : getPrivateImeOptions");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getInputType");
     */
    @MayloonStubAnnotation()
    public int getInputType() {
        System.out.println("Stub" + " Function : getInputType");
        return 0;
    }

    public final void setHintTextColor(int color) {
        mHintTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    /**
     * Sets the color of the hint text.
     *
     * @attr ref android.R.styleable#TextView_textColorHint
     */
    public final void setHintTextColor(ColorStateList colors) {
        mHintTextColor = colors;
        updateTextColors();
    }

    /**
     * <p>Return the color used to paint the hint text.</p>
     *
     * @return Returns the list of hint text colors.
     */
    public final ColorStateList getHintTextColors() {
        return mHintTextColor;
    }

    /**
     * @j2sNative
     * console.log("Missing method: endBatchEdit");
     */
    @MayloonStubAnnotation()
    public void endBatchEdit() {
        System.out.println("Stub" + " Function : endBatchEdit");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: set");
     */
    @MayloonStubAnnotation()
    void set(char[] chars, int start, int len) {
        System.out.println("Stub" + " Function : set");
        return;
    }

    public void setSingleLine() {
        setSingleLine(true);
    }

    public int getLineHeight() {
        return FastMath.round(mTextPaint.getFontMetricsInt(null) * mSpacingMult + mSpacingAdd);
    }

    private int getTextareaParams() {
        int value = 0;
        /**
         * @j2sNative
         * var div = document.createElement("textarea");
         * div.rows = 1;
         * div.cols = this.mDefaultSize;
         * div.id = "measure_div";
         * div.style.visibility = "hidden";
         * document.body.appendChild(div);
         * div.style.position = "absolute";
         * div.style.overflow = "hidden";
         * div.style.border = "none";
         * div.style.paddingBottom = div.style.paddingTop = div.style.paddingLeft = div.style.paddingRight = "0px";
         * div.style.marginBottom = div.style.marginTop = div.style.marginLeft = div.style.marginRight = "0px";
         * this.setFontProperties(div.id);
         * value = div.offsetWidth;
         * div.parentNode.removeChild(div);
         */{}
         return value;
    }
    /**
     * @param rows   In general there are 3, first 1 lines, second min lines, third max lines
     * @param style  1.width   2.height
     * @return
     */
    private int getTextareaParams(CharSequence text, int cNum, int rows, int style) {
        int value = 0;
        if (mDefaultWidth == mDefaultSize) {
            cNum = mDefaultSize;
        }
        /**
         * @j2sNative
         * var div = document.createElement("textarea");
         * div.value = text.toString();
         * div.rows = rows;
         * div.cols = cNum;
         * div.id = "measure_div";
         * div.style.visibility = "hidden";
         * document.body.appendChild(div);
         * div.style.position = "absolute";
         * div.style.overflow = "hidden";
         * div.style.border = "none";
         * div.style.padding = "0px";
         * div.style.margin = "0px";
         * this.setFontProperties(div.id);
         * if (style == 1) {
         *     value = cNum > 0 ? div.offsetWidth : 0;
         * } else if (style == 2) {
         *     if (rows == 1) {
         *         this.mOneLineHeight = div.offsetHeight;
         *         div.style.posHeight = div.scrollHeight;
         *         this.mLines = Math.ceil(div.scrollHeight / this.mOneLineHeight);
         *         value = div.scrollHeight;
         *     } else {
         *         value = div.offsetHeight;
         *     }
         * }
         * div.parentNode.removeChild(div);
         */{}
        return value;
    }
    /**
     * @param text
     * @param singleLine
     * @param rows    In general there are 3, first 1 lines, second min lines, third max lines
     * @param style 0. lineHeight   1.width   2.height
     * @return
     */
    private int getDivParams(CharSequence text, Boolean singleLine, int style) {
        int value = 0;
        /**
         * @j2sNative
         * var div = document.createElement("div");
         * var temp = text.toString();
         * if (style != 0) {
         *     var lineHeight = this.getLineHeight();
         *     div.style.height = lineHeight + "px";
         * }
         * if (style == 2 && this.mCurWidth > 0) {
         *     div.style.width = this.mCurWidth + "px";
         * }
         * if (temp == " ") {
         *     temp = temp.replaceAll(" ", "&nbsp;");
         * } else if (temp == "<type>") {
         *     temp = "&lt;" + "type" + "&gt;";
         * } else if (temp == "<untitled>") {
         *     temp = "&lt;" + "untitled" + "&gt;";
         * } else {
         *     temp = temp.replaceAll("\r\n", "<br/>");
         *     temp = temp.replaceAll("\n", "<br/>");
         *     temp = temp.replaceAll("<br />", "<br/>");
         * }
         * if ((singleLine).booleanValue ()) {
         *     temp = temp.replaceAll("<br/>", " ");
         *     div.style.whiteSpace = "nowrap";
         * }
         * div.innerHTML = temp;
         * div.id = "measure_div";
         * div.style.visibility = "hidden";
         * document.body.appendChild(div);
         * div.style.position = "absolute";
         * div.style.overflow = "hidden";
         * div.style.border = "none";
         * div.style.paddingBottom = div.style.paddingTop = div.style.paddingLeft = div.style.paddingRight = "0px";
         * div.style.marginBottom = div.style.marginTop = div.style.marginLeft = div.style.marginRight = "0px";
         * this.setFontProperties(div.id);
         * if (style == 1) {
         *     value = div.offsetWidth;
         * } else if (style == 2) {
         *     this.mOneLineHeight = div.offsetHeight;
         *     div.style.posHeight = div.scrollHeight;
         *     this.mLines = Math.ceil(div.scrollHeight / this.mOneLineHeight);
         *     value = div.scrollHeight;
         * } else if (style == 0) {
         *     value = this.mOneLineHeight = div.offsetHeight;
         * }
         * div.parentNode.removeChild(div);
         */{}
         return value;
    }

    /**
     * Return width or height according to the text
     * @param style 1.width   2.height
     */
    private int getInputParams(CharSequence text, int style) {
        int value = 0;
        /**
         * @j2sNative
         * var div = document.createElement("input");
         * div.type = "text";
         * div.value = text.toString();
         * div.id = "measure_div";
         * div.style.visibility = "hidden";
         * document.body.appendChild(div);
         * div.style.position = "absolute";
         * div.style.border = "none";
         * div.style.paddingBottom = div.style.paddingTop = div.style.paddingLeft = div.style.paddingRight = "0px";
         * div.style.marginBottom = div.style.marginTop = div.style.marginLeft = div.style.marginRight = "0px";
         * this.setFontProperties(div.id);
         * if (style == 1) {
         *     value = div.offsetWidth;
         * } else {
         *     this.mLines = 1;
         *     value = this.mOneLineHeight = div.offsetHeight;
         * }
         * div.parentNode.removeChild(div);
         */{}
        return value;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getImeActionId");
     */
    @MayloonStubAnnotation()
    public int getImeActionId() {
        System.out.println("Stub" + " Function : getImeActionId");
        return 0;
    }

    public void setMinEms(int minems) {
        mMinWidth = (minems * getLineHeight());
//        mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    public final int getCurrentHintTextColor() {
        return mHintTextColor != null ? mCurHintTextColor : mCurTextColor;
    }

    /**
     * Set whether the TextView includes extra top and bottom padding to make
     * room for accents that go above the normal ascent and descent.
     * The default is true.
     *
     * @attr ref android.R.styleable#TextView_includeFontPadding
     */
    public void setIncludeFontPadding(boolean includepad) {
        mIncludePad = includepad;

        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public boolean moveCursorToVisibleOffset() {
        if (!(mText instanceof Spannable)) {
            return false;
        }
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start != end) {
            return false;
        }
        
        // First: make sure the line is visible on screen:
        
        int line = mLayout.getLineForOffset(start);

        final int top = mLayout.getLineTop(line);
        final int bottom = mLayout.getLineTop(line + 1);
        final int vspace = mBottom - mTop - getExtendedPaddingTop() - getExtendedPaddingBottom();
        int vslack = (bottom - top) / 2;
        if (vslack > vspace / 4)
            vslack = vspace / 4;
        final int vs = mScrollY;

        if (top < (vs+vslack)) {
            line = mLayout.getLineForVertical(vs+vslack+(bottom-top));
        } else if (bottom > (vspace+vs-vslack)) {
            line = mLayout.getLineForVertical(vspace+vs-vslack-(bottom-top));
        }
        
        // Next: make sure the character is visible on screen:
        
        final int hspace = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();
        final int hs = mScrollX;
        final int leftChar = mLayout.getOffsetForHorizontal(line, hs);
        final int rightChar = mLayout.getOffsetForHorizontal(line, hspace+hs);
        
        int newStart = start;
        if (newStart < leftChar) {
            newStart = leftChar;
        } else if (newStart > rightChar) {
            newStart = rightChar;
        }
        
        if (newStart != start) {
            Selection.setSelection((Spannable)mText, newStart);
            return true;
        }
        
        return false;
    }

    public final ColorStateList getTextColors() {
        return mTextColor;
    }

    public final int getCurrentTextColor() {
        return mCurTextColor;
    }

    public void setError(CharSequence error) {
        if (error == null) {
            setError(null, null);
        } else {
            Drawable dr = getContext().getResources().
                getDrawable(com.android.internal.R.drawable.
                            indicator_input_error);

            dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
            setError(error, dr);
        }
    }

    /**
     * Sets the right-hand compound drawable of the TextView to the specified
     * icon and sets an error message that will be displayed in a popup when
     * the TextView has focus.  The icon and error message will be reset to
     * null when any key events cause changes to the TextView's text.  The
     * drawable must already have had {@link Drawable#setBounds} set on it.
     * If the <code>error</code> is <code>null</code>, the error message will
     * be cleared (and you should provide a <code>null</code> icon as well).
     */
    public void setError(CharSequence error, Drawable icon) {
        error = TextUtils.stringOrSpannedString(error);

        mError = error;
        mErrorWasChanged = true;
        final Drawables dr = mDrawables;
        if (dr != null) {
            setCompoundDrawables(dr.mDrawableLeft, dr.mDrawableTop,
                                 icon, dr.mDrawableBottom);
        } else {
            setCompoundDrawables(null, null, icon, null);
        }

        if (error == null) {
            if (mPopup != null) {
                if (mPopup.isShowing()) {
                    mPopup.dismiss();
                }

                mPopup = null;
            }
        } else {
            if (isFocused()) {
                showError();
            }
        }
    }

    private void showError() {
        // if (getWindowToken() == null) {
        // mShowErrorAfterAttach = true;
        // return;
        // }

        if (mPopup == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            final TextView err = (TextView) inflater.inflate(
                    com.android.internal.R.layout.textview_hint,
                    null);

            final float scale = getResources().getDisplayMetrics().density;
            mPopup = new ErrorPopup(err, (int) (200 * scale + 0.5f),
                    (int) (50 * scale + 0.5f));
            mPopup.setFocusable(false);
            // The user is entering text, so the input method is needed. We
            // don't want the popup to be displayed on top of it.
            mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        }

        TextView tv = (TextView) mPopup.getContentView();
        // chooseSize(mPopup, mError, tv);
        tv.setText(mError);

        // mPopup.showAsDropDown(this, getErrorX(), getErrorY());
        mPopup.fixDirection(mPopup.isAboveAnchor());
    }

    private static class ErrorPopup extends PopupWindow {
        private boolean mAbove = false;
        private final TextView mView;

        ErrorPopup(TextView v, int width, int height) {
            super(v, width, height);
            mView = v;
        }

        void fixDirection(boolean above) {
            mAbove = above;

            if (above) {
                mView.setBackgroundResource(com.android.internal.R.drawable.popup_inline_error_above);
            } else {
                mView.setBackgroundResource(com.android.internal.R.drawable.popup_inline_error);
            }
        }

        @Override
        public void update(int x, int y, int w, int h, boolean force) {
            super.update(x, y, w, h, force);

            boolean above = isAboveAnchor();
            if (above != mAbove) {
                fixDirection(above);
            }
        }
    }

    @Override
    public int getBaseline() {
        if (mLayout == null) {
            return super.getBaseline();
        }
        int voffset = 0;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            if (!mSingleLine) {
                voffset = getVerticalOffset(true);
            }
        }
        return getExtendedPaddingTop() + voffset;
//        return getExtendedPaddingTop() + voffset + mLayout.getLineBaseline(0);
    }

    /**
     * @j2sNative
     * console.log("Missing method: onPreDraw");
     */
    @MayloonStubAnnotation()
    public boolean onPreDraw() {
        System.out.println("Stub" + " Function : onPreDraw");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setPrivateImeOptions");
     */
    @MayloonStubAnnotation()
    public void setPrivateImeOptions(String type) {
        System.out.println("Stub" + " Function : setPrivateImeOptions");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: Marquee");
     */
    @MayloonStubAnnotation()
    void Marquee(TextView v) {
        System.out.println("Stub" + " Function : Marquee");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setAutoLinkMask");
     */
    @MayloonStubAnnotation()
    public final void setAutoLinkMask(int mask) {
        System.out.println("Stub" + " Function : setAutoLinkMask");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getAutoLinkMask");
     */
    @MayloonStubAnnotation()
    public final int getAutoLinkMask() {
        System.out.println("Stub" + " Function : getAutoLinkMask");
        return 0;
    }

    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;
        mMaxWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * @j2sNative
     * console.log("Missing method: setCompoundDrawablePadding");
     */
    @MayloonStubAnnotation()
    public void setCompoundDrawablePadding(int pad) {
        System.out.println("Stub" + " Function : setCompoundDrawablePadding");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setCursorVisible");
     */
    @MayloonStubAnnotation()
    public void setCursorVisible(boolean visible) {
        System.out.println("Stub" + " Function : setCursorVisible");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: cancel");
     */
    @MayloonStubAnnotation()
    void cancel() {
        System.out.println("Stub" + " Function : cancel");
        return;
    }

    public void setMaxEms(int maxems) {
        mMaxWidth = (maxems * getLineHeight());
//        mMaxWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * @j2sNative
     * console.log("Missing method: getImeOptions");
     */
    @MayloonStubAnnotation()
    public int getImeOptions() {
        System.out.println("Stub" + " Function : getImeOptions");
        return 0;
    }

    public CharSequence getError() {
        return mError;
    }

    /**
     * @j2sNative
     * console.log("Missing method: didTouchFocusSelect");
     */
    @MayloonStubAnnotation()
    public boolean didTouchFocusSelect() {
        System.out.println("Stub" + " Function : didTouchFocusSelect");
        return true;
    }

    public void setHighlightColor(int color) {
        if (mHighlightColor != color) {
            mHighlightColor = color;
            invalidate();
        }
    }

    /**
     * Return the text the TextView is displaying as an Editable object.  If
     * the text is not editable, null is returned.
     *
     * @see #getText
     */
    public Editable getEditableText() {
        return (mText instanceof Editable) ? (Editable)mText : null;
    }

    /**
     * Removes the specified TextWatcher from the list of those whose
     * methods are called
     * whenever this TextView's text changes.
     */
    public void removeTextChangedListener(TextWatcher watcher) {
        if (mListeners != null) {
            int i = mListeners.indexOf(watcher);

            if (i >= 0) {
                mListeners.remove(i);
            }
        }
    }

    /**
     * Gives the text a shadow of the specified radius and color, the specified
     * distance from its normal position.
     *
     * @attr ref android.R.styleable#TextView_shadowColor
     * @attr ref android.R.styleable#TextView_shadowDx
     * @attr ref android.R.styleable#TextView_shadowDy
     * @attr ref android.R.styleable#TextView_shadowRadius
     */
    public void setShadowLayer(float radius, float dx, float dy, int color) {

        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;

        invalidate();
    }

    public final boolean getLinksClickable() {
        return mLinksClickable;
    }

    public void setSingleLine(boolean singleLine) {
        if ((mInputType&EditorInfo.TYPE_MASK_CLASS)
                == EditorInfo.TYPE_CLASS_TEXT) {
            if (singleLine) {
                mInputType &= ~EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
            } else {
                mInputType |= EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
            }
        }
        applySingleLine(singleLine, true);
    }

    private void applySingleLine(boolean singleLine, boolean applyTransformation) {
        mSingleLine = singleLine;
        if (singleLine) {
            setLines(1);
            setHorizontallyScrolling(true);
            if (applyTransformation) {
                setTransformationMethod(SingleLineTransformationMethod.
                                        getInstance());
            }
        } else {
            setMaxLines(Integer.MAX_VALUE);
            setHorizontallyScrolling(false);
            if (applyTransformation) {
                setTransformationMethod(null);
            }
        }
    }

    /**
     * Sets the transformation that is applied to the text that this
     * TextView is displaying.
     *
     * @attr ref android.R.styleable#TextView_password
     * @attr ref android.R.styleable#TextView_singleLine
     */
    public final void setTransformationMethod(TransformationMethod method) {
        if (method == mTransformation) {
            // Avoid the setText() below if the transformation is
            // the same.
            return;
        }
        if (mTransformation != null) {
            if (mText instanceof Spannable) {
                ((Spannable) mText).removeSpan(mTransformation);
            }
        }

        mTransformation = method;

        setText(mText);
    }

    /**
     * @return the current transformation method for this TextView.
     * This will frequently be null except for single-line and password
     * fields.
     */
    public final TransformationMethod getTransformationMethod() {
        return mTransformation;
    }

    /**
     * Returns the list of URLSpans attached to the text
     * (by {@link Linkify} or otherwise) if any.  You can call
     * {@link URLSpan#getURL} on them to find where they link to
     * or use {@link Spanned#getSpanStart} and {@link Spanned#getSpanEnd}
     * to find the region of the text they are attached to.
     */
    public URLSpan[] getUrls() {
        if (mText instanceof Spanned) {
            return ((Spanned) mText).getSpans(0, mText.length(), URLSpan.class);
        } else {
            return new URLSpan[0];
        }
    }

    /**
     * Retrieve the input extras currently associated with the text view, which
     * can be viewed as well as modified.
     *
     * @param create If true, the extras will be created if they don't already
     * exist.  Otherwise, null will be returned if none have been created.
     * @see #setInputExtras(int)
     * @see EditorInfo#extras
     * @attr ref android.R.styleable#TextView_editorExtras
     */
    public Bundle getInputExtras(boolean create) {
        if (mInputContentType == null) {
            if (!create) return null;
            mInputContentType = new InputContentType();
        }
        if (mInputContentType.extras == null) {
            if (!create) return null;
            mInputContentType.extras = new Bundle();
        }
        return mInputContentType.extras;
    }

    /**
     * @j2sNative
     * console.log("Missing method: debug");
     */
    @MayloonStubAnnotation()
    public void debug(int depth) {
        System.out.println("Stub" + " Function : debug");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onEndBatchEdit");
     */
    @MayloonStubAnnotation()
    public void onEndBatchEdit() {
        System.out.println("Stub" + " Function : onEndBatchEdit");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setImeOptions");
     */
    @MayloonStubAnnotation()
    public void setImeOptions(int imeOptions) {
        System.out.println("Stub" + " Function : setImeOptions");
        return;
    }

    public boolean bringPointIntoView(int offset) {
        boolean changed = false;

        int line = mLayout.getLineForOffset(offset);

        // FIXME: Is it okay to truncate this, or should we round?
        final int x = (int)mLayout.getPrimaryHorizontal(offset);
        final int top = mLayout.getLineTop(line);
        final int bottom = mLayout.getLineTop(line + 1);

        int left = (int) FloatMath.floor(mLayout.getLineLeft(line));
        int right = (int) FloatMath.ceil(mLayout.getLineRight(line));
        int ht = mLayout.getHeight();

        int grav;

        switch (mLayout.getParagraphAlignment(line)) {
            case ALIGN_NORMAL:
                grav = 1;
                break;

            case ALIGN_OPPOSITE:
                grav = -1;
                break;

            default:
                grav = 0;
        }

        grav *= mLayout.getParagraphDirection(line);

        int hspace = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int vspace = mBottom - mTop - getExtendedPaddingTop() - getExtendedPaddingBottom();

        int hslack = (bottom - top) / 2;
        int vslack = hslack;

        if (vslack > vspace / 4)
            vslack = vspace / 4;
        if (hslack > hspace / 4)
            hslack = hspace / 4;

        int hs = mScrollX;
        int vs = mScrollY;

        if (top - vs < vslack)
            vs = top - vslack;
        if (bottom - vs > vspace - vslack)
            vs = bottom - (vspace - vslack);
        if (ht - vs < vspace)
            vs = ht - vspace;
        if (0 - vs > 0)
            vs = 0;

        if (grav != 0) {
            if (x - hs < hslack) {
                hs = x - hslack;
            }
            if (x - hs > hspace - hslack) {
                hs = x - (hspace - hslack);
            }
        }

        if (grav < 0) {
            if (left - hs > 0)
                hs = left;
            if (right - hs < hspace)
                hs = right - hspace;
        } else if (grav > 0) {
            if (right - hs < hspace)
                hs = right - hspace;
            if (left - hs > 0)
                hs = left;
        } else /* grav == 0 */ {
            if (right - left <= hspace) {
                /*
                 * If the entire text fits, center it exactly.
                 */
                hs = left - (hspace - (right - left)) / 2;
            } else if (x > right - hslack) {
                /*
                 * If we are near the right edge, keep the right edge
                 * at the edge of the view.
                 */
                hs = right - hspace;
            } else if (x < left + hslack) {
                /*
                 * If we are near the left edge, keep the left edge
                 * at the edge of the view.
                 */
                hs = left;
            } else if (left > hs) {
                /*
                 * Is there whitespace visible at the left?  Fix it if so.
                 */
                hs = left;
            } else if (right < hs + hspace) {
                /*
                 * Is there whitespace visible at the right?  Fix it if so.
                 */
                hs = right - hspace;
            } else {
                /*
                 * Otherwise, float as needed.
                 */
                if (x - hs < hslack) {
                    hs = x - hslack;
                }
                if (x - hs > hspace - hslack) {
                    hs = x - (hspace - hslack);
                }
            }
        }

        if (hs != mScrollX || vs != mScrollY) {
            if (mScroller == null) {
                scrollTo(hs, vs);
            } else {
                long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
                int dx = hs - mScrollX;
                int dy = vs - mScrollY;

                if (duration > ANIMATED_SCROLL_GAP) {
                    mScroller.startScroll(mScrollX, mScrollY, dx, dy);
                    awakenScrollBars(mScroller.getDuration());
                    invalidate();
                } else {
                    if (!mScroller.isFinished()) {
                        mScroller.abortAnimation();
                    }

                    scrollBy(dx, dy);
                }

                mLastScroll = AnimationUtils.currentAnimationTimeMillis();
            }

            changed = true;
        }

        if (isFocused()) {
            // This offsets because getInterestingRect() is in terms of
            // viewport coordinates, but requestRectangleOnScreen()
            // is in terms of content coordinates.

            Rect r = new Rect(x, top, x + 1, bottom);
            getInterestingRect(r, line);
            r.offset(mScrollX, mScrollY);

            if (requestRectangleOnScreen(r)) {
                changed = true;
            }
        }

        return changed;
    }

    private void getInterestingRect(Rect r, int line) {
        convertFromViewportToContentCoordinates(r);

        // Rectangle can can be expanded on first and last line to take
        // padding into account.
        // TODO Take left/right padding into account too?
        if (line == 0) r.top -= getExtendedPaddingTop();
        if (line == mLayout.getLineCount() - 1) r.bottom += getExtendedPaddingBottom();
    }

    private void convertFromViewportToContentCoordinates(Rect r) {
        final int horizontalOffset = viewportToContentHorizontalOffset();
        r.left += horizontalOffset;
        r.right += horizontalOffset;

        final int verticalOffset = viewportToContentVerticalOffset();
        r.top += verticalOffset;
        r.bottom += verticalOffset;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mScrolled = true;
    }

    private int viewportToContentHorizontalOffset() {
        return getCompoundPaddingLeft() - mScrollX;
    }

    private int viewportToContentVerticalOffset() {
        int offset = getExtendedPaddingTop() - mScrollY;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            offset += getVerticalOffset(false);
        }
        return offset;
    }

    private static class CharWrapper
            implements CharSequence, GetChars, GraphicsOperations {
        private char[] mChars;
        private int mStart, mLength;

        public CharWrapper(char[] chars, int start, int len) {
            mChars = chars;
            mStart = start;
            mLength = len;
        }

        /* package */void set(char[] chars, int start, int len) {
            mChars = chars;
            mStart = start;
            mLength = len;
        }

        public int length() {
            return mLength;
        }

        public char charAt(int off) {
            return mChars[off + mStart];
        }

        @Override
        public String toString() {
            return new String(mChars, mStart, mLength);
        }

        public CharSequence subSequence(int start, int end) {
            if (start < 0 || end < 0 || start > mLength || end > mLength) {
                throw new IndexOutOfBoundsException(start + ", " + end);
            }

            return new String(mChars, start + mStart, end - start);
        }

        public void getChars(int start, int end, char[] buf, int off) {
            if (start < 0 || end < 0 || start > mLength || end > mLength) {
                throw new IndexOutOfBoundsException(start + ", " + end);
            }

            System.arraycopy(mChars, start + mStart, buf, off, end - start);
        }

        public void drawText(Canvas c, int start, int end,
                float x, float y, Paint p) {
            c.drawText(mChars, start + mStart, end - start, x, y, p);
        }

        public float measureText(int start, int end, Paint p) {
            return p.measureText(mChars, start + mStart, end - start);
        }

        public int getTextWidths(int start, int end, float[] widths, Paint p) {
            return p.getTextWidths(mChars, start + mStart, end - start, widths);
        }
    }

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private InputFilter[] mFilters = NO_FILTERS;
    private static final Spanned EMPTY_SPANNED = new SpannedString("");

    protected MovementMethod getDefaultMovementMethod() {
        return null;
    }

    public static int getTextColor(Context context, TypedArray attrs, int def) {
        ColorStateList colors = getTextColors(context, attrs);

        if (colors == null) {
            return def;
        } else {
            return colors.getDefaultColor();
        }
    }

    private static final BoringLayout.Metrics UNKNOWN_BORING = new BoringLayout.Metrics();

    /**
     * The width passed in is now the desired layout width,
     * not the full view width with padding.
     * {@hide}
     */
    protected void makeNewLayout(int w, int hintWidth,
                                 BoringLayout.Metrics boring,
                                 BoringLayout.Metrics hintBoring,
                                 int ellipsisWidth, boolean bringIntoView) {

        if (w < 0) {
            w = 0;
        }
        if (hintWidth < 0) {
            hintWidth = 0;
        }

        Layout.Alignment alignment;
        switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                alignment = Layout.Alignment.ALIGN_CENTER;
                break;

            case Gravity.RIGHT:
                alignment = Layout.Alignment.ALIGN_OPPOSITE;
                break;

            default:
                alignment = Layout.Alignment.ALIGN_NORMAL;
        }

        mEllipsize = null;/** mayloon does not support ellipsis now */
        boolean shouldEllipsize = mEllipsize != null && mInput == null;

        if (mText instanceof Spannable) {//like input and textarea;
            mLayout = new DynamicLayout(mText, mTransformed, mTextPaint, w, alignment, mSpacingMult,
                    mSpacingAdd, mIncludePad, mInput == null ? mEllipsize : null, ellipsisWidth);
        } else {
            if (boring == UNKNOWN_BORING) {
                boring = BoringLayout.isBoring(mTransformed, mTextPaint, mBoring);
                if (boring != null) {
                    // The textarea or input is larger than div 2 pixels by each side that in the same font.
                    if (mEditable) {
                        boring.width = boring.width + 4;
                        boring.bottom = boring.bottom + 4;
                    }
                    mBoring = boring;
                }
            }

            if (boring != null) {
                if (boring.width <= w && (mEllipsize == null || boring.width <= ellipsisWidth)) {
                    mLayout = BoringLayout.make(mTransformed, mTextPaint, w, alignment, mSpacingMult, mSpacingAdd, boring, mIncludePad);
                } else {//single but too long so should wordWrap
                    mLayout = new StaticLayout(mTransformed, mTextPaint, w, alignment, mSpacingMult, mSpacingAdd, mIncludePad);
                }
            } else {//div and mult
                mLayout = new StaticLayout(mTransformed, mTextPaint, w, alignment, mSpacingMult, mSpacingAdd, mIncludePad);
            }
        }

        // CursorControllers need a non-null mLayout
        prepareCursorControllers();
    }

    private int desired(Layout layout) {
        CharSequence text = layout.getText();
        if (!text.toString().contains("\n")) {
            return -1;
        } else {
            float max = layout.getDesiredWidth(text, mTextPaint);
            return (int) FloatMath.ceil(max);
        }
    }

    private void nullLayouts() {
        mLayout = mHintLayout = null;
    }

    /**
     * Check whether entirely new text requires a new view layout
     * or merely a new text layout.
     */
    private void checkForRelayout() {
        // If we have a fixed width, we can just swap in a new text layout
        // if the text height stays the same or if the view height is fixed.
        if ((mLayoutParams.width != LayoutParams.WRAP_CONTENT ||
                (mMaxWidth == mMinWidth)) &&
                (mHint == null || mHintLayout != null) &&
                (mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight() > 0)) {
            // Static width, so try making a new text layout.

            int oldht = this.getHeight();
            int want = mLayout.getWidth();
            int hintWant = mHintLayout == null ? 0 : mHintLayout.getWidth();

            /*
             * No need to bring the text into view, since the size is not
             * changing (unless we do the requestLayout(), in which case it
             * will happen at measure).
             */
            makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING,
                          mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight(),
                          false);

            // We lose: the height has changed and we have a dynamic height.
            // Request a new view layout using our new text layout.
        } else {
            // Dynamic width, so we have no choice but to request a new
            // view layout with a new text layout.

            nullLayouts();
        }
    }
    
    private void sendBeforeTextChanged(CharSequence text, int start, int before,
            int after) {
        if (mListeners != null) {
            final ArrayList<TextWatcher> list = mListeners;
            final int count = list.size();
            for (int i = 0; i < count; i++) {
                list.get(i).beforeTextChanged(text, start, before, after);
            }
        }
    }
}
