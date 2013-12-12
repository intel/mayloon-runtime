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

package android.text.method;

import android.view.KeyEvent;
import android.view.View;
import android.text.*;

public abstract class BaseKeyListener
implements KeyListener {
    /* package */ static final Object OLD_SEL_START = new NoCopySpan.Concrete();

    /**
     * Base implementation handles ACTION_MULTIPLE KEYCODE_UNKNOWN by inserting
     * the event's text into the content.
     */
    public boolean onKeyOther(View view, Editable content, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_MULTIPLE
                || event.getKeyCode() != KeyEvent.KEYCODE_UNKNOWN) {
            // Not something we are interested in.
            return false;
        }

        int selStart, selEnd;

        {
            int a = Selection.getSelectionStart(content);
            int b = Selection.getSelectionEnd(content);

            selStart = Math.min(a, b);
            selEnd = Math.max(a, b);
        }

        CharSequence text = event.getCharacters();
        if (text == null) {
            return false;
        }

//        content.replace(selStart, selEnd, text);
        return true;
    }
}

