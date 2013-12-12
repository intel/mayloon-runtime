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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DebugUtils;
import android.view.View;

public class EditText extends TextView {

    private boolean needFocus = false;

	public EditText(Context context) {
        this(context, null);
    }

	public EditText(Context context, AttributeSet attrs) {
		super(context, attrs, com.android.internal.R.attr.editTextStyle);
		this.setClickable(true);
	}

	public EditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.setClickable(true);
	}

	@Override
	protected boolean getDefaultEditable() {
		return true;
	}

	@Override
	public Editable getText() {
		String inputValue = "";
        /**
         * @j2sNative
         * var thisView = document.getElementById(this.getUIElementID());
         * if (thisView != null) {
         *    inputValue = thisView.value;
         *    // Fix bug #644
         *    if (Clazz.instanceOf(this.mText, android.text.Editable)) {
         *       if (!(this.mText.toString()).equals(inputValue)) {
         *           (this.mText).clear();
         *           (this.mText).append(inputValue);
         *       }
         *    } else {
         *        this.setText(inputValue);
         *    }
         * }
         */{}
        return (Editable) mText;
    }

    @Override
    public final boolean requestFocus() {
        /**
         * @j2sNative
         * var thisText = document.getElementById(this.getUIElementID());
         * if (thisText == null) {
         *     this.needFocus = true;
         * } else {
         *     thisText.focus();
         * }
         */{}
        return super.requestFocus();
    }

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, BufferType.EDITABLE);
	}

	/**
	 * Convenience for {@link Selection#setSelection(Spannable, int, int)}.
	 */
	public void setSelection(int start, int stop) {
		Selection.setSelection(getText(), start, stop);
	}

	/**
	 * Convenience for {@link Selection#setSelection(Spannable, int)}.
	 */
	public void setSelection(int index) {
		Selection.setSelection(getText(), index);
	}

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSingleLine) {
            /**
             * @j2sNative
             * var thisText = document.getElementById(this.getUIElementID());
             * if (thisText == null)
             *    console.log("EditText.onDraw, thisSpan == null");
             * if (thisText.tagName != "INPUT") { // replace it when first time
             *     var inputText = document.createElement("input");
             *     old_attributes = thisText.attributes;
             *     if ((this.mInputType & 128) != 0) {
             *         inputText.type = "password";
             *     } else {
             *         inputText.type = "text";
             *     }
             *     for(var i = 0, len = old_attributes.length; i < len; i++) {
             *         inputText.setAttribute(old_attributes[i].name, old_attributes[i].value);
             *     }
             *     inputText.value = thisText.value;
             *     var child = thisText.firstChild;
             *     if (child != null) {
             *         do {
             *             inputText.appendChild(child);
             *         } while (child = child.nextSibling);
             *     }
             *     thisText.parentNode.replaceChild(inputText, thisText);
             * }
             */{}
        } else {
            if (this.mMaxLength != -1) {
                /**
                 * @j2sNative
                 * var thisText = document.getElementById(this.getUIElementID());
                 * if (thisText == null)
                 *     console.log("EditText.onDraw, thisSpan == null");
                 * if (thisText.tagName == "TEXTAREA") {
                 *     thisText.setAttribute("maxlength", this.mMaxLength);
                 * }
                 */{}
            }
        }
        
        EditText thisObj = this;
        /**
         * @j2sNative
         * var thisText = document.getElementById(this.getUIElementID());
         * if (this.needFocus) {
         *     thisText.focus();
         *     this.needFocus = false;
         * }
         * thisText.oninput = function() {
         *     thisObj.setText(thisText.value);
         * }
         */{}
    }

	public boolean performClick() {
		//System.out.println("in EditText.performClick()");
        /**
         * @j2sNative
         * var thisText = document.getElementById(this.getUIElementID());
         * if (thisText == null) {
         *     this.needFocus = true;
         * } else {
         *     thisText.focus();
         * }
         * //console.log("Edit Text is clicked!");
         */{}
		return super.performClick();
	}

	/**
     * Convenience for {@link Selection#selectAll}.
     */
    public void selectAll() {
        Selection.selectAll(getText());
    }

    /**
     * Convenience for {@link Selection#extendSelection}.
     */
    public void extendSelection(int index) {
        Selection.extendSelection(getText(), index);
    }
}
