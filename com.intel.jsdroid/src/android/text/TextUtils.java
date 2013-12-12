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


import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.style.ReplacementSpan;
import android.util.Printer;

import java.util.regex.Pattern;
import java.util.Iterator;

public class TextUtils {
	private TextUtils() { /* cannot be instantiated */
	}

	private static String[] EMPTY_STRING_ARRAY = new String[] {};
	
	public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE,
    }

	/** @hide */
    public static final int ALIGNMENT_SPAN = 1;
    /** @hide */
    public static final int FOREGROUND_COLOR_SPAN = 2;
    /** @hide */
    public static final int RELATIVE_SIZE_SPAN = 3;
    /** @hide */
    public static final int SCALE_X_SPAN = 4;
    /** @hide */
    public static final int STRIKETHROUGH_SPAN = 5;
    /** @hide */
    public static final int UNDERLINE_SPAN = 6;
    /** @hide */
    public static final int STYLE_SPAN = 7;
    /** @hide */
    public static final int BULLET_SPAN = 8;
    /** @hide */
    public static final int QUOTE_SPAN = 9;
    /** @hide */
    public static final int LEADING_MARGIN_SPAN = 10;
    /** @hide */
    public static final int URL_SPAN = 11;
    /** @hide */
    public static final int BACKGROUND_COLOR_SPAN = 12;
    /** @hide */
    public static final int TYPEFACE_SPAN = 13;
    /** @hide */
    public static final int SUPERSCRIPT_SPAN = 14;
    /** @hide */
    public static final int SUBSCRIPT_SPAN = 15;
    /** @hide */
    public static final int ABSOLUTE_SIZE_SPAN = 16;
    /** @hide */
    public static final int TEXT_APPEARANCE_SPAN = 17;
    /** @hide */
    public static final int ANNOTATION = 18;



	public static void getChars(CharSequence s, int start, int end,
			char[] dest, int destoff) {
		Class c = s.getClass();

		if (c == String.class)
			((String) s).getChars(start, end, dest, destoff);
		else if (c == StringBuilder.class)
			((StringBuilder) s).getChars(start, end, dest, destoff);
		else {
            if (0 <= start && start <= end) {
                for (int i = start; i < end; i++)
                    dest[destoff++] = s.charAt(i);
            }
            else
            {
                throw new StringIndexOutOfBoundsException();
            }
		}
	}

	public static int indexOf(CharSequence s, String ch) {
		return indexOf(s, ch, 0);
	}

	public static int indexOf(CharSequence s, String ch, int start) {
		Class c = s.getClass();

		if (c == String.class)
			return ((String) s).indexOf(ch, start);

		return indexOf(s, ch, start, s.length());
	}

	public static int indexOf(CharSequence s, String ch, int start, int end) {

		for (int i = start; i < end; i++)
			if (s.charAt(i) == ch.charAt(0))
				return i;

		return -1;
	}

	public static int lastIndexOf(CharSequence s, char ch) {
		return 0;
	}

    public static int lastIndexOf(CharSequence s, char ch, int last) {
        Class c = s.getClass();

        if (c == String.class)
            return ((String) s).lastIndexOf(ch, last);

        return lastIndexOf(s, ch, 0, last);
    }

    public static int lastIndexOf(CharSequence s, char ch, int start, int last) {
        if (last < 0)
            return -1;
        if (last >= s.length())
            last = s.length() - 1;

        int end = last + 1;

        Class c = s.getClass();

        if (s instanceof GetChars || c == StringBuffer.class || c == StringBuilder.class || c == String.class) {
            final int INDEX_INCREMENT = 500;
            char[] temp = obtain(INDEX_INCREMENT);

            while (start < end) {
                int segstart = end - INDEX_INCREMENT;
                if (segstart < start)
                    segstart = start;

                getChars(s, segstart, end, temp, 0);

                int count = end - segstart;
                for (int i = count - 1; i >= 0; i--) {
                    if (temp[i] == ch) {
                        recycle(temp);
                        return i + segstart;
                    }
                }
                end = segstart;
            }

            recycle(temp);
            return -1;
        }

        for (int i = end - 1; i >= start; i--)
            if (s.charAt(i) == ch)
                return i;

        return -1;
    }

	private static Object sLock = new Object();
	private static char[] sTemp = null;

	public static void writeToParcel(CharSequence val, Parcel parcel, int i) {
		// TODO Auto-generated method stub

	}
	
	public static boolean isEmpty(CharSequence str) {
		if(str==null){
			return true;
		}
        if (getLength(str)== 0)
            return true;
        else
            return false;
    }
	public static int getLength(CharSequence str){
		if(str==null){
			return 0;
		}
		/**
		 * @j2sNative
		 * return String(str).length;
		 */{}
		 return 0;
	}
	
	
	/**
     * Returns a string containing the tokens joined by delimiters.
     * @param tokens an array objects to be joined. Strings will be formed from
     *     the objects by calling object.toString().
     */
    public static String join(CharSequence delimiter, Object[] tokens) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token: tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(token);
        }
        return sb.toString();
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     * @param tokens an array objects to be joined. Strings will be formed from
     *     the objects by calling object.toString().
     */
    public static String join(CharSequence delimiter, Iterable tokens) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token: tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(token);
        }
        return sb.toString();
    }

    /**
     * String.split() returns [''] when the string to be split is empty. This returns []. This does
     * not remove any empty strings from the result. For example split("a,", ","  ) returns {"a", ""}.
     *
     * @param text the string to split
     * @param expression the regular expression to match
     * @return an array of strings. The array will be empty if text is empty
     *
     * @throws NullPointerException if expression or text is null
     */
    public static String[] split(String text, String expression) {
        if (text.length() == 0) {
            return EMPTY_STRING_ARRAY;
        } else {
            return text.split(expression, -1);
        }
    }

    /**
     * Splits a string on a pattern. String.split() returns [''] when the string to be
     * split is empty. This returns []. This does not remove any empty strings from the result.
     * @param text the string to split
     * @param pattern the regular expression to match
     * @return an array of strings. The array will be empty if text is empty
     *
     * @throws NullPointerException if expression or text is null
     */
    public static String[] split(String text, Pattern pattern) {
        if (text.length() == 0) {
            return EMPTY_STRING_ARRAY;
        } else {
            return pattern.split(text, -1);
        }
    }
    
    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) return true;
        int length;
        if (a != null && b != null && (length = a.length()) == b.length()) {
            if (a instanceof String && b instanceof String) {
                return a.equals(b);
            } else {
                for (int i = 0; i < length; i++) {
                    if (a.charAt(i) != b.charAt(i)) return false;
                }
                return true;
            }
        }
        return false;
    }

    public static int getOffsetBefore(CharSequence text, int offset) {
        if (offset == 0)
            return 0;
        if (offset == 1)
            return 0;

        char c = text.charAt(offset - 1);

        if (c >= '\uDC00' && c <= '\uDFFF') {
            char c1 = text.charAt(offset - 2);

            if (c1 >= '\uD800' && c1 <= '\uDBFF')
                offset -= 2;
            else
                offset -= 1;
        } else {
            offset -= 1;
        }

        /*if (text instanceof Spanned) {
            ReplacementSpan[] spans = ((Spanned) text).getSpans(offset, offset,
                                                       ReplacementSpan.class);

            for (int i = 0; i < spans.length; i++) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);

                if (start < offset && end > offset)
                    offset = start;
            }
        }*/

        return offset;
    }
    
    /**
     * Copies the spans from the region <code>start...end</code> in
     * <code>source</code> to the region
     * <code>destoff...destoff+end-start</code> in <code>dest</code>.
     * Spans in <code>source</code> that begin before <code>start</code>
     * or end after <code>end</code> but overlap this range are trimmed
     * as if they began at <code>start</code> or ended at <code>end</code>.
     *
     * @throws IndexOutOfBoundsException if any of the copied spans
     * are out of range in <code>dest</code>.
     */
    public static void copySpansFrom(Spanned source, int start, int end,
                                     Class kind,
                                     Spannable dest, int destoff) {
        if (kind == null) {
            kind = Object.class;
        }

        Object[] spans = source.getSpans(start, end, kind);

        for (int i = 0; i < spans.length; i++) {
            int st = source.getSpanStart(spans[i]);
            int en = source.getSpanEnd(spans[i]);
            int fl = source.getSpanFlags(spans[i]);

            if (st < start)
                st = start;
            if (en > end)
                en = end;

            dest.setSpan(spans[i], st - start + destoff, en - start + destoff,
                         fl);
        }
    }

    /* package */ static char[] obtain(int len) {
        char[] buf;

        synchronized (sLock) {
            buf = sTemp;
            sTemp = null;
        }

        if (buf == null || buf.length < len)
            buf = new char[ArrayUtils.idealCharArraySize(len)];

        return buf;
    }

    /* package */ static void recycle(char[] temp) {
        if (temp.length > 1000)
            return;

        synchronized (sLock) {
            sTemp = temp;
        }
    }
    
    public static int getOffsetAfter(CharSequence text, int offset) {
        int len = text.length();

        if (offset == len)
            return len;
        if (offset == len - 1)
            return len;

        char c = text.charAt(offset);

        if (c >= '\uD800' && c <= '\uDBFF') {
            char c1 = text.charAt(offset + 1);

            if (c1 >= '\uDC00' && c1 <= '\uDFFF')
                offset += 2;
            else
                offset += 1;
        } else {
            offset += 1;
        }

        if (text instanceof Spanned) {
            ReplacementSpan[] spans = ((Spanned) text).getSpans(offset, offset,
                                                       ReplacementSpan.class);

            for (int i = 0; i < spans.length; i++) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);

                if (start < offset && end > offset)
                    offset = end;
            }
        }

        return offset;
    }

 // XXX currently this only reverses chars, not spans
    public static CharSequence getReverse(CharSequence source,
                                          int start, int end) {
        return new Reverser(source, start, end);
    }

    private static class Reverser
    implements CharSequence, GetChars
    {
        public Reverser(CharSequence source, int start, int end) {
            mSource = source;
            mStart = start;
            mEnd = end;
        }

        public int length() {
            return mEnd - mStart;
        }

        public CharSequence subSequence(int start, int end) {
            char[] buf = new char[end - start];

            getChars(start, end, buf, 0);
            return new String(buf);
        }

        public String toString() {
            return subSequence(0, length()).toString();
        }

        public char charAt(int off) {
            return AndroidCharacter.getMirror(mSource.charAt(mEnd - 1 - off));
        }

        public void getChars(int start, int end, char[] dest, int destoff) {
            TextUtils.getChars(mSource, start + mStart, end + mStart,
                               dest, destoff);
            AndroidCharacter.mirror(dest, 0, end - start);

            int len = end - start;
            int n = (end - start) / 2;
            for (int i = 0; i < n; i++) {
                char tmp = dest[destoff + i];

                dest[destoff + i] = dest[destoff + len - i - 1];
                dest[destoff + len - i - 1] = tmp;
            }
            
        }
        private CharSequence mSource;
        private int mStart;
        private int mEnd;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getTrimmedLength");
     */
    @MayloonStubAnnotation()
    public static int getTrimmedLength(CharSequence s) {
        System.out.println("Stub" + " Function : getTrimmedLength");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: htmlEncode");
     */
    @MayloonStubAnnotation()
    public static String htmlEncode(String s) {
        System.out.println("Stub" + " Function : htmlEncode");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isGraphic");
     */
    @MayloonStubAnnotation()
    public static boolean isGraphic(char c) {
        System.out.println("Stub" + " Function : isGraphic");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isDigitsOnly");
     */
    @MayloonStubAnnotation()
    public static boolean isDigitsOnly(CharSequence str) {
        System.out.println("Stub" + " Function : isDigitsOnly");
        return true;
    }

    public static CharSequence stringOrSpannedString(CharSequence source) {
        if (source == null)
            return null;
        if (source instanceof SpannedString)
            return source;
        if (source instanceof Spanned)
            return new SpannedString(source);

        return source.toString();
    }

    /**
     * @j2sNative
     * console.log("Missing method: isGraphic");
     */
    @MayloonStubAnnotation()
    public static boolean isGraphic(CharSequence str) {
        System.out.println("Stub" + " Function : isGraphic");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: concat");
     */
    @MayloonStubAnnotation()
    public static CharSequence concat(CharSequence text) {
        System.out.println("Stub" + " Function : concat");
        return null;
    }
}
