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

package android.text;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.android.internal.util.ArrayUtils;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;

/**
 * StaticLayout is a Layout for text that will not be edited after it
 * is laid out.  Use {@link DynamicLayout} for text that may change.
 * <p>This is used by widgets to control text layout. You should not need
 * to use this class directly unless you are implementing your own widget
 * or custom display object, or would be tempted to call
 * {@link android.graphics.Canvas#drawText(java.lang.CharSequence, int, int, float, float, android.graphics.Paint)
 *  Canvas.drawText()} directly.</p>
 */
public class StaticLayout extends Layout {
    public StaticLayout(CharSequence source, TextPaint paint,
                        int width,
                        Alignment align, float spacingmult, float spacingadd,
                        boolean includepad) {
        this(source, 0, source.length(), paint, width, align,
             spacingmult, spacingadd, includepad);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend,
                        TextPaint paint, int outerwidth,
                        Alignment align,
                        float spacingmult, float spacingadd,
                        boolean includepad) {
        this(source, bufstart, bufend, paint, outerwidth, align,
             spacingmult, spacingadd, includepad, null, 0);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend,
                        TextPaint paint, int outerwidth,
                        Alignment align,
                        float spacingmult, float spacingadd,
                        boolean includepad,
                        TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        super((ellipsize == null) ? source : (source instanceof Spanned) ? new SpannedEllipsizer(source) : new Ellipsizer(source),
              paint, outerwidth, align, spacingmult, spacingadd);

        /*
         * This is annoying, but we can't refer to the layout until
         * superclass construction is finished, and the superclass
         * constructor wants the reference to the display text.
         * 
         * This will break if the superclass constructor ever actually
         * cares about the content instead of just holding the reference.
         */
        if (ellipsize != null) {
            Ellipsizer e = (Ellipsizer) getText();

            e.mLayout = this;
            e.mWidth = ellipsizedWidth;
            e.mMethod = ellipsize;
            mEllipsizedWidth = ellipsizedWidth;

            mColumns = COLUMNS_ELLIPSIZE;
        } else {
            mColumns = COLUMNS_NORMAL;
            mEllipsizedWidth = outerwidth;
        }

        mLines = new int[ArrayUtils.idealIntArraySize(2 * mColumns)];
        mLineDirections = new Directions[
                             ArrayUtils.idealIntArraySize(2 * mColumns)];

        generate(source, bufstart, bufend, paint, outerwidth, align,
                 spacingmult, spacingadd, includepad, includepad,
                 ellipsize != null, ellipsizedWidth, ellipsize);

        mChdirs = null;
        mChs = null;
        mWidths = null;
        mFontMetricsInt = null;
    }

    /* package */ StaticLayout(boolean ellipsize) {
        super(null, null, 0, null, 0, 0);

        mColumns = COLUMNS_ELLIPSIZE;
        mLines = new int[ArrayUtils.idealIntArraySize(2 * mColumns)];
        mLineDirections = new Directions[
                             ArrayUtils.idealIntArraySize(2 * mColumns)];
    }

    /* package */ void generate(CharSequence source, int bufstart, int bufend,
                        TextPaint paint, int outerwidth,
                        Alignment align,
                        float spacingmult, float spacingadd,
                        boolean includepad, boolean trackpad,
                        boolean breakOnlyAtSpaces,
                        float ellipsizedWidth, TextUtils.TruncateAt where) {
        mLineCount = 0;
        
        int singleHeight = 0;
        int totalHeight = 0;
        
        String fontStyle = "normal";
        String fontWeight = "normal";
        String fontFamily = "serif";
        Typeface tf = paint.getTypeface();
        if (tf != null) {
            switch (tf.getStyle()) {
                case 0:
                    fontStyle = "normal";
                    break;
                case 1:
                    fontWeight = "bold";
                    break;
                case 2:
                    fontStyle = "italic";
                    break;
                case 3:
                    fontWeight = "bold";
                    fontStyle = "italic";
                    break;
                default:
                    fontWeight = "bold";
                    fontStyle = "italic";
                    break;
            }
            if (tf.getFamilyName() == "sans-serif" || tf.getFamilyName() == "serif" || tf.getFamilyName() == "monospace") {
                fontFamily = tf.getFamilyName();
            } else if (null != tf.getFamilyName()) {
                //console.loge("We don't support this font: " + tf.getFamilyName());
            }
        }

         String value = source.toString();
         /**
          * @j2sNative
          * var div = document.createElement("div");
          * if (value.isHtml) {
          *     div.innerHTML = value;
          * } else {
          *     div.innerText = value;
          * }
          * div.id = "measure_div";
          * document.body.appendChild(div);
          * div.style.visibility = "hidden";
          * div.style.position = "absolute";
          * div.style.overflow = "hidden";
          * div.style.border = "none";
          * div.style.padding = "0px";
          * div.style.margin = "0px";
          * div.style.fontSize = paint.getTextSize() + "px";
          * div.style.fontStyle = fontStyle;
          * div.style.fontWeight = fontWeight;
          * div.style.fontFamily = fontFamily;
          * div.style.whiteSpace = "nowrap";
          * div.style.height = "auto";
          * singleHeight = div.offsetHeight;
          * if (value.isHtml) {
          *     div.innerHTML = value;
          * } else {
          *     div.innerText = value;
          * }
          * div.style.width = outerwidth + "px";
          * div.style.whiteSpace = "normal";
          * div.style.wordWrap = "break-word";
          * totalHeight = div.offsetHeight;
          * this.mLineCount = totalHeight / singleHeight;
          * div.parentNode.removeChild(div);
          */{}

          for (int i = 1; i <= mLineCount; i++) {
              mLines[i] = singleHeight * i;
          }
          if (mLines[mLineCount] != totalHeight) {
              mLines[mLineCount] = totalHeight;
          }
    }

    // Override the baseclass so we can directly access our members,
    // rather than relying on member functions.
    // The logic mirrors that of Layout.getLineForVertical
    // FIXME: It may be faster to do a linear search for layouts without many lines.
    public int getLineForVertical(int vertical) {
        int high = mLineCount;
        int low = -1;
        int guess;
        int[] lines = mLines;
        while (high - low > 1) {
            guess = (high + low) >> 1;
            if (lines[mColumns * guess + TOP] > vertical){
                high = guess;
            } else {
                low = guess;
            }
        }
        if (low < 0) {
            return 0;
        } else {
            return low;
        }
    }

    public int getLineCount() {
        return mLineCount;
    }

    public int getLineTop(int line) {
        return mLines[line];
    }

    public int getLineDescent(int line) {
        return mLines[mColumns * line + DESCENT];
    }

    public int getLineStart(int line) {
        return mLines[mColumns * line + START] & START_MASK;
    }

    public int getParagraphDirection(int line) {
        return mLines[mColumns * line + DIR] >> DIR_SHIFT;
    }

    public boolean getLineContainsTab(int line) {
        return (mLines[mColumns * line + TAB] & TAB_MASK) != 0;
    }

    public final Directions getLineDirections(int line) {
        return mLineDirections[line];
    }

    public int getTopPadding() {
        return mTopPadding;
    }

    public int getBottomPadding() {
        return mBottomPadding;
    }

    @Override
    public int getEllipsisCount(int line) {
        if (mColumns < COLUMNS_ELLIPSIZE) {
            return 0;
        }

        return mLines[mColumns * line + ELLIPSIS_COUNT];
    }

    @Override
    public int getEllipsisStart(int line) {
        if (mColumns < COLUMNS_ELLIPSIZE) {
            return 0;
        }

        return mLines[mColumns * line + ELLIPSIS_START];
    }

    @Override
    public int getEllipsizedWidth() {
        return mEllipsizedWidth;
    }

    private int mLineCount;
    private int mTopPadding, mBottomPadding;
    private int mColumns;
    private int mEllipsizedWidth;

    private static final int COLUMNS_NORMAL = 3;
    private static final int COLUMNS_ELLIPSIZE = 5;
    private static final int START = 0;
    private static final int DIR = START;
    private static final int TAB = START;
    private static final int TOP = 1;
    private static final int DESCENT = 2;
    private static final int ELLIPSIS_START = 3;
    private static final int ELLIPSIS_COUNT = 4;

    private int[] mLines;
    private Directions[] mLineDirections;

    private static final int START_MASK = 0x1FFFFFFF;
    private static final int DIR_MASK   = 0xC0000000;
    private static final int DIR_SHIFT  = 30;
    private static final int TAB_MASK   = 0x20000000;

    private static final char FIRST_RIGHT_TO_LEFT = '\u0590';

    /*
     * These are reused across calls to generate()
     */
    private byte[] mChdirs;
    private char[] mChs;
    private float[] mWidths;
    private Paint.FontMetricsInt mFontMetricsInt = new Paint.FontMetricsInt();
}
