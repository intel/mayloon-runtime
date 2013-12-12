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

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.style.UpdateLayout;
import android.text.style.WrapTogetherSpan;

import java.lang.ref.WeakReference;

import com.android.internal.util.ArrayUtils;

/**
 * DynamicLayout is a text layout that updates itself as the text is edited.
 * <p>This is used by widgets to control text layout. You should not need
 * to use this class directly unless you are implementing your own widget
 * or custom display object, or need to call
 * {@link android.graphics.Canvas#drawText(java.lang.CharSequence, int, int, float, float, android.graphics.Paint)
 *  Canvas.drawText()} directly.</p>
 */
public class DynamicLayout extends Layout {
    private static final int PRIORITY = 128;
    private int mLineCount;
    private int[] mLines;

    /**
     * Make a layout for the specified text that will be updated as
     * the text is changed.
     */
    public DynamicLayout(CharSequence base,
                         TextPaint paint,
                         int width, Alignment align,
                         float spacingmult, float spacingadd,
                         boolean includepad) {
        this(base, base, paint, width, align, spacingmult, spacingadd, includepad);
    }

    /**
     * Make a layout for the transformed text (password transformation
     * being the primary example of a transformation)
     * that will be updated as the base text is changed.
     */
    public DynamicLayout(CharSequence base, CharSequence display,
                         TextPaint paint,
                         int width, Alignment align,
                         float spacingmult, float spacingadd,
                         boolean includepad) {
        this(base, display, paint, width, align, spacingmult, spacingadd, includepad, null, 0);
    }

    /**
     * Make a layout for the transformed text (password transformation
     * being the primary example of a transformation)
     * that will be updated as the base text is changed.
     * If ellipsize is non-null, the Layout will ellipsize the text
     * down to ellipsizedWidth.
     */
    public DynamicLayout(CharSequence base, CharSequence display,
                         TextPaint paint,
                         int width, Alignment align,
                         float spacingmult, float spacingadd,
                         boolean includepad,
                         TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        super((ellipsize == null) ? display : (display instanceof Spanned) ? new SpannedEllipsizer(display) : new Ellipsizer(display),
              paint, width, align, spacingmult, spacingadd);

        mBase = base;
        mDisplay = display;

        if (ellipsize != null) {
            mInts = new PackedIntVector(COLUMNS_ELLIPSIZE);
            mEllipsizedWidth = ellipsizedWidth;
            mEllipsizeAt = ellipsize;
        } else {
            mInts = new PackedIntVector(COLUMNS_NORMAL);
            mEllipsizedWidth = width;
            mEllipsizeAt = ellipsize;
        }

        mIncludePad = includepad;

        mLines = new int[ArrayUtils.idealIntArraySize(6)];
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

         String value = base.toString();
         if (null == value || "".equals(value)) {
             value = "A";
         }
         /**
          * @j2sNative
          * var div = document.createElement("div");
          * div.innerHTML = value;
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
          * div.innerText = value;
          * div.style.width = width + "px";
          * div.style.whiteSpace = "normal";
          *  //div.style.wordWrap = "break-word";
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

    public int getLineCount() {
        //return mInts.size() - 1;
        return mLineCount;
    }

    public int getLineTop(int line) {
        //return mInts.getValue(line, TOP);
        return mLines[line];
    }

    public int getLineDescent(int line) {
        return mInts.getValue(line, DESCENT);
    }

    public int getLineStart(int line) {
        return mInts.getValue(line, START) & START_MASK;
    }

    public boolean getLineContainsTab(int line) {
        return (mInts.getValue(line, TAB) & TAB_MASK) != 0;
    }

    public int getParagraphDirection(int line) {
        return mInts.getValue(line, DIR) >> DIR_SHIFT;
    }

    public final Directions getLineDirections(int line) {
        return mObjects.getValue(line, 0);
    }

    public int getTopPadding() {
        return mTopPadding;
    }

    public int getBottomPadding() {
        return mBottomPadding;
    }

    @Override
    public int getEllipsizedWidth() {
        return mEllipsizedWidth;
    }

    public int getEllipsisStart(int line) {
        if (mEllipsizeAt == null) {
            return 0;
        }

        return mInts.getValue(line, ELLIPSIS_START);
    }

    public int getEllipsisCount(int line) {
        if (mEllipsizeAt == null) {
            return 0;
        }

        return mInts.getValue(line, ELLIPSIS_COUNT);
    }

    private CharSequence mBase;
    private CharSequence mDisplay;
//    private ChangeWatcher mWatcher;
    private boolean mIncludePad;
    private boolean mEllipsize;
    private int mEllipsizedWidth;
    private TextUtils.TruncateAt mEllipsizeAt;

    private PackedIntVector mInts;
    private PackedObjectVector<Directions> mObjects;

    private int mTopPadding, mBottomPadding;

    private static StaticLayout sStaticLayout = new StaticLayout(true);
    private static Object sLock = new Object();

    private static final int START = 0;
    private static final int DIR = START;
    private static final int TAB = START;
    private static final int TOP = 1;
    private static final int DESCENT = 2;
    private static final int COLUMNS_NORMAL = 3;

    private static final int ELLIPSIS_START = 3;
    private static final int ELLIPSIS_COUNT = 4;
    private static final int COLUMNS_ELLIPSIZE = 5;

    private static final int START_MASK = 0x1FFFFFFF;
    private static final int DIR_MASK   = 0xC0000000;
    private static final int DIR_SHIFT  = 30;
    private static final int TAB_MASK   = 0x20000000;

    private static final int ELLIPSIS_UNDEFINED = 0x80000000;
}
