package android.view;

public class HTML5Event {

	// the list of HTML5 event type in DOM Level 3
	public static final String eventType[] = {
		"abort",
		"blur",
		"click",
		"compositionstart",
		"compositionupdate",
		"compositionend",
		"dblclick",
		"DOMActivate",
		"DOMAttributeNameChanged",
		"DOMAttrModified",
		"DOMCharacterDataModified",
		"DOMElementNameChanged",
		"DOMFocusIn",
		"DOMFocusOut",
		"DOMNodeInserted",
		"DOMNodeInsertedIntoDocument",
		"DOMNodeRemoved",
		"DOMNodeRemovedFromDocument",
	//	"DOMSubtreeModified",
		"error",
		"focus",
		"focusin",
		"focusout",
		"keydown",
		"keypress",
		"keyup",
		"load",
		"mousedown",
		"mouseenter",
		"mouseleave",
		"mousemove",
		"mouseout",
		"mouseover",
		"mouseup",
		"resize",
		"scroll",
		"select",
		"textinput",
		"unload",
		"wheel"
	};



	public static boolean isMouseEvent(String event) {
		event = event.trim();
		if (event.equals("mousedown")
			||event.equals("mousemove")
			||event.equals("mouseup")
			||event.equals("mouseenter")
			||event.equals("mouseleave")
			||event.equals("click")
			||event.equals("dblclick")
		)
			return true;
		else return false;
	}

	public static boolean isKeyEvent(String event) {
		event = event.trim();
		if (event.equals("keydown")
			||event.equals("keypress")
			||event.equals("keyup")
			)
			return true;
		else return false;
	}

    public static KeyEvent toKeyEvent(String eventType, int keyCode) {
        KeyEvent keyEvent = null;
        if (eventType.equals("keydown")) {
            keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        } else if (eventType.equals("keyup")) {
            keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        } else if (eventType.equals("keypress")) {
            // 'keypress event' has different keyCode from "keydown" and "keyup"
            // input s in editText will make the same effect as F4,
            // because keycode of "F4" is 115 in "keydown" when keycode of s is
            // 115 in keypress
            // keyEvent = new KeyEvent(KeyEvent.ACTION_MULTIPLE, keyCode);
        }

        return keyEvent;
    }
}
