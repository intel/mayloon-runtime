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

package android.view;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Interpolator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.DebugUtils;
import android.util.LocaleUtil;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ScrollBarDrawable;
import android.widget.ScrollView;

import com.android.internal.R;
import com.android.internal.R.styleable;
import com.intel.mpt.annotation.MayloonStubAnnotation;

/**
 * <p>
 * This class represents the basic building block for user interface components. A View
 * occupies a rectangular area on the screen and is responsible for drawing and
 * event handling. View is the base class for <em>widgets</em>, which are
 * used to create interactive UI components (buttons, text fields, etc.). The
 * {@link android.view.ViewGroup} subclass is the base class for <em>layouts</em>, which
 * are invisible containers that hold other Views (or other ViewGroups) and define
 * their layout properties.
 *
 * @see android.view.ViewGroup
 */
/**
 * In kona2, we do not handle all the graphics operations directly for the time being, so this view class
 * will not implements the drawable interface. In the future, if the Web Runtime exposes the Canvas object
 * we may support customized drawing
 */
public class View implements Drawable.Callback, KeyEvent.Callback {
	public int zIndex = 0;
	public float alpha = -1;

	public int getZIndex() {
		View root = this.getRootView();
		return root.zIndex;
	}

	private static final boolean DBG = false;

	/**
	 * The logging tag used by this class with android.util.Log.@
	 */
	protected static final String VIEW_LOG_TAG = "View";

	/**
	 * Used to mark a View that has no ID.
	 */
	public static final int NO_ID = -1;

	/**
	 * This view does not want keystrokes. Use with TAKES_FOCUS_MASK when
	 * calling setFlags.
	 */
	private static final int NOT_FOCUSABLE = 0x00000000;

	/**
	 * This view wants keystrokes. Use with TAKES_FOCUS_MASK when calling
	 * setFlags.
	 */
	private static final int FOCUSABLE = 0x00000001;

	/**
	 * Mask for use with setFlags indicating bits used for focus.
	 */
	private static final int FOCUSABLE_MASK = 0x00000001;

	/**
	 * This view will adjust its padding to fit sytem windows (e.g. status bar)
	 */
	private static final int FITS_SYSTEM_WINDOWS = 0x00000002;

	/**
	 * This view is visible.  Use with {@link #setVisibility}.
	 */
	public static final int VISIBLE = 0x00000000;

	/**
	 * This view is invisible, but it still takes up space for layout purposes.
	 * Use with {@link #setVisibility}.
	 */
	public static final int INVISIBLE = 0x00000004;

	/**
	 * This view is invisible, and it doesn't take any space for layout
	 * purposes. Use with {@link #setVisibility}.
	 */
	public static final int GONE = 0x00000008;

	/**
	 * Mask for use with setFlags indicating bits used for visibility.
	 * {@hide}
	 */
	static final int VISIBILITY_MASK = 0x0000000C;

	private static final int[] VISIBILITY_FLAGS = { VISIBLE, INVISIBLE, GONE };

	/**
	 * This view is enabled. Intrepretation varies by subclass.
	 * Use with ENABLED_MASK when calling setFlags.
	 * {@hide}
	 */
	static final int ENABLED = 0x00000000;

	/**
	 * This view is disabled. Interpretation varies by subclass.
	 * Use with ENABLED_MASK when calling setFlags.
	 * {@hide}
	 */
	static final int DISABLED = 0x00000020;

	/**
	 * Mask for use with setFlags indicating bits used for indicating whether
	 * this view is enabled
	 * {@hide}
	 */
	static final int ENABLED_MASK = 0x00000020;

	/**
	 * This view won't draw. {@link #onDraw} won't be called and further
	 * optimizations
	 * will be performed. It is okay to have this flag set and a background.
	 * Use with DRAW_MASK when calling setFlags.
	 * {@hide}
	 */
	static final int WILL_NOT_DRAW = 0x00000080;

	/**
	 * Mask for use with setFlags indicating bits used for indicating whether
	 * this view is will draw
	 * {@hide}
	 */
	static final int DRAW_MASK = 0x00000080;

	/**
	 * <p>This view doesn't show scrollbars.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_NONE = 0x00000000;

	/**
	 * <p>This view shows horizontal scrollbars.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_HORIZONTAL = 0x00000100;

	/**
	 * <p>This view shows vertical scrollbars.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_VERTICAL = 0x00000200;

	/**
	 * <p>Mask for use with setFlags indicating bits used for indicating which
	 * scrollbars are enabled.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_MASK = 0x00000300;

	/**
	 * Indicates that the view should filter touches when its window is obscured.
	 * Refer to the class comments for more information about this security feature.
	 * {@hide}
	 */
	static final int FILTER_TOUCHES_WHEN_OBSCURED = 0x00000400;

	// note flag value 0x00000800 is now available for next flags...

	/**
	 * <p>This view doesn't show fading edges.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_NONE = 0x00000000;

	/**
	 * <p>This view shows horizontal fading edges.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_HORIZONTAL = 0x00001000;

	/**
	 * <p>This view shows vertical fading edges.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_VERTICAL = 0x00002000;

	/**
	 * <p>Mask for use with setFlags indicating bits used for indicating which
	 * fading edges are enabled.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_MASK = 0x00003000;

	/**
	 * <p>Indicates this view can be clicked. When clickable, a View reacts
	 * to clicks by notifying the OnClickListener.<p>
	 * {@hide}
	 */
	static final int CLICKABLE = 0x00004000;

	/**
	 * <p>Indicates this view is caching its drawing into a bitmap.</p>
	 * {@hide}
	 */
	static final int DRAWING_CACHE_ENABLED = 0x00008000;

	/**
	 * <p>Indicates that no icicle should be saved for this view.<p>
	 * {@hide}
	 */
	static final int SAVE_DISABLED = 0x000010000;

	/**
	 * <p>Mask for use with setFlags indicating bits used for the saveEnabled
	 * property.</p>
	 * {@hide}
	 */
	static final int SAVE_DISABLED_MASK = 0x000010000;

	/**
	 * <p>Indicates that no drawing cache should ever be created for this view.<p>
	 * {@hide}
	 */
	static final int WILL_NOT_CACHE_DRAWING = 0x000020000;

	/**
	 * <p>Indicates this view can take / keep focus when int touch mode.</p>
	 * {@hide}
	 */
	static final int FOCUSABLE_IN_TOUCH_MODE = 0x00040000;

	/**
	 * <p>Enables low quality mode for the drawing cache.</p>
	 */
	public static final int DRAWING_CACHE_QUALITY_LOW = 0x00080000;

	/**
	 * <p>Enables high quality mode for the drawing cache.</p>
	 */
	public static final int DRAWING_CACHE_QUALITY_HIGH = 0x00100000;

	/**
	 * <p>Enables automatic quality mode for the drawing cache.</p>
	 */
	public static final int DRAWING_CACHE_QUALITY_AUTO = 0x00000000;

    private static final int[] DRAWING_CACHE_QUALITY_FLAGS = {
        DRAWING_CACHE_QUALITY_AUTO, DRAWING_CACHE_QUALITY_LOW, DRAWING_CACHE_QUALITY_HIGH
    };

	/**
	 * <p>Mask for use with setFlags indicating bits used for the cache
	 * quality property.</p>
	 * {@hide}
	 */
	static final int DRAWING_CACHE_QUALITY_MASK = 0x00180000;

	/**
	 * <p>
	 * Indicates this view can be long clicked. When long clickable, a View
	 * reacts to long clicks by notifying the OnLongClickListener or showing a
	 * context menu.
	 * </p>
	 * {@hide}
	 */
	static final int LONG_CLICKABLE = 0x00200000;

	/**
	 * <p>Indicates that this view gets its drawable states from its direct parent
	 * and ignores its original internal states.</p>
	 *
	 * @hide
	 */
	static final int DUPLICATE_PARENT_STATE = 0x00400000;

	/**
	 * The scrollbar style to display the scrollbars inside the content area,
	 * without increasing the padding. The scrollbars will be overlaid with
	 * translucency on the view's content.
	 */
	public static final int SCROLLBARS_INSIDE_OVERLAY = 0;

	/**
	 * The scrollbar style to display the scrollbars inside the padded area,
	 * increasing the padding of the view. The scrollbars will not overlap the
	 * content area of the view.
	 */
	public static final int SCROLLBARS_INSIDE_INSET = 0x01000000;

	/**
	 * The scrollbar style to display the scrollbars at the edge of the view,
	 * without increasing the padding. The scrollbars will be overlaid with
	 * translucency.
	 */
	public static final int SCROLLBARS_OUTSIDE_OVERLAY = 0x02000000;

	/**
	 * The scrollbar style to display the scrollbars at the edge of the view,
	 * increasing the padding of the view. The scrollbars will only overlap the
	 * background, if any.
	 */
	public static final int SCROLLBARS_OUTSIDE_INSET = 0x03000000;

	/**
	 * Mask to check if the scrollbar style is overlay or inset.
	 * {@hide}
	 */
	static final int SCROLLBARS_INSET_MASK = 0x01000000;

	/**
	 * Mask to check if the scrollbar style is inside or outside.
	 * {@hide}
	 */
	static final int SCROLLBARS_OUTSIDE_MASK = 0x02000000;

	/**
	 * Mask for scrollbar style.
	 * {@hide}
	 */
	static final int SCROLLBARS_STYLE_MASK = 0x03000000;

	/**
	 * View flag indicating that the screen should remain on while the
	 * window containing this view is visible to the user.  This effectively
	 * takes care of automatically setting the WindowManager's
	 * {@link WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON}.
	 */
	public static final int KEEP_SCREEN_ON = 0x04000000;

	/**
	 * View flag indicating whether this view should have sound effects enabled
	 * for events such as clicking and touching.
	 */
	public static final int SOUND_EFFECTS_ENABLED = 0x08000000;

	/**
	 * View flag indicating whether this view should have haptic feedback
	 * enabled for events such as long presses.
	 */
	public static final int HAPTIC_FEEDBACK_ENABLED = 0x10000000;

	/**
	 * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
	 * should add all focusable Views regardless if they are focusable in touch mode.
	 */
	public static final int FOCUSABLES_ALL = 0x00000000;

	/**
	 * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
	 * should add only Views focusable in touch mode.
	 */
	public static final int FOCUSABLES_TOUCH_MODE = 0x00000001;

	/**
	 * Use with {@link #focusSearch}. Move focus to the previous selectable
	 * item.
	 */
	public static final int FOCUS_BACKWARD = 0x00000001;

	/**
	 * Use with {@link #focusSearch}. Move focus to the next selectable
	 * item.
	 */
	public static final int FOCUS_FORWARD = 0x00000002;

	/**
	 * Use with {@link #focusSearch}. Move focus to the left.
	 */
	public static final int FOCUS_LEFT = 0x00000011;

	/**
	 * Use with {@link #focusSearch}. Move focus up.
	 */
	public static final int FOCUS_UP = 0x00000021;

	/**
	 * Use with {@link #focusSearch}. Move focus to the right.
	 */
	public static final int FOCUS_RIGHT = 0x00000042;

	/**
	 * Use with {@link #focusSearch}. Move focus down.
	 */
	public static final int FOCUS_DOWN = 0x00000082;

	/**
	 * Base View state sets
	 */
	// Singles
	/**
	 * Indicates the view has no states set. States are used with
	 * {@link android.graphics.drawable.Drawable} to change the drawing of the
	 * view depending on its state.
	 *
	 * @see android.graphics.drawable.Drawable
	 * @see #getDrawableState()
	 */
	protected static final int[] EMPTY_STATE_SET = {};
	/**
	 * Indicates the view is enabled. States are used with
	 * {@link android.graphics.drawable.Drawable} to change the drawing of the
	 * view depending on its state.
	 *
	 * @see android.graphics.drawable.Drawable
	 * @see #getDrawableState()
	 */
	protected static final int[] ENABLED_STATE_SET = { R.attr.state_enabled };
	/**
	 * Indicates the view is focused. States are used with
	 * {@link android.graphics.drawable.Drawable} to change the drawing of the
	 * view depending on its state.
	 *
	 * @see android.graphics.drawable.Drawable
	 * @see #getDrawableState()
	 */
	protected static final int[] FOCUSED_STATE_SET = { R.attr.state_focused };
	/**
	 * Indicates the view is selected. States are used with
	 * {@link android.graphics.drawable.Drawable} to change the drawing of the
	 * view depending on its state.
	 *
	 * @see android.graphics.drawable.Drawable
	 * @see #getDrawableState()
	 */
	protected static final int[] SELECTED_STATE_SET = { R.attr.state_selected };
	/**
	 * Indicates the view is pressed. States are used with
	 * {@link android.graphics.drawable.Drawable} to change the drawing of the
	 * view depending on its state.
	 *
	 * @see android.graphics.drawable.Drawable
	 * @see #getDrawableState()
	 * @hide
	 */
	protected static final int[] PRESSED_STATE_SET = { R.attr.state_pressed };
	/**
	 * Indicates the view's window has focus. States are used with
	 * {@link android.graphics.drawable.Drawable} to change the drawing of the
	 * view depending on its state.
	 *
	 * @see android.graphics.drawable.Drawable
	 * @see #getDrawableState()
	 */
	protected static final int[] WINDOW_FOCUSED_STATE_SET = { R.attr.state_window_focused };
	// Doubles
	/**
	 * Indicates the view is enabled and has the focus.
	 *
	 * @see #ENABLED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 */
	protected static final int[] ENABLED_FOCUSED_STATE_SET = stateSetUnion(
			ENABLED_STATE_SET, FOCUSED_STATE_SET);
	/**
	 * Indicates the view is enabled and selected.
	 *
	 * @see #ENABLED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 */
	protected static final int[] ENABLED_SELECTED_STATE_SET = stateSetUnion(
			ENABLED_STATE_SET, SELECTED_STATE_SET);
	/**
	 * Indicates the view is enabled and that its window has focus.
	 *
	 * @see #ENABLED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			ENABLED_STATE_SET, WINDOW_FOCUSED_STATE_SET);
	/**
	 * Indicates the view is focused and selected.
	 *
	 * @see #FOCUSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 */
	protected static final int[] FOCUSED_SELECTED_STATE_SET = stateSetUnion(
			FOCUSED_STATE_SET, SELECTED_STATE_SET);
	/**
	 * Indicates the view has the focus and that its window has the focus.
	 *
	 * @see #FOCUSED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			FOCUSED_STATE_SET, WINDOW_FOCUSED_STATE_SET);
	/**
	 * Indicates the view is selected and that its window has the focus.
	 *
	 * @see #SELECTED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			SELECTED_STATE_SET, WINDOW_FOCUSED_STATE_SET);
	// Triples
	/**
	 * Indicates the view is enabled, focused and selected.
	 *
	 * @see #ENABLED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 */
	protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET = stateSetUnion(
			ENABLED_FOCUSED_STATE_SET, SELECTED_STATE_SET);
	/**
	 * Indicates the view is enabled, focused and its window has the focus.
	 *
	 * @see #ENABLED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			ENABLED_FOCUSED_STATE_SET, WINDOW_FOCUSED_STATE_SET);
	/**
	 * Indicates the view is enabled, selected and its window has the focus.
	 *
	 * @see #ENABLED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			ENABLED_SELECTED_STATE_SET, WINDOW_FOCUSED_STATE_SET);
	/**
	 * Indicates the view is focused, selected and its window has the focus.
	 *
	 * @see #FOCUSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			FOCUSED_SELECTED_STATE_SET, WINDOW_FOCUSED_STATE_SET);
	/**
	 * Indicates the view is enabled, focused, selected and its window
	 * has the focus.
	 *
	 * @see #ENABLED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			ENABLED_FOCUSED_SELECTED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed and its window has the focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Mask for use with setFlags indicating bits used for horizontalDirection.
	 * {@hide}
	 */
	static final int LAYOUT_DIRECTION_MASK = 0xC0000000;

	/**
	 * Indicates the view is pressed and selected.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 */
	protected static final int[] PRESSED_SELECTED_STATE_SET = stateSetUnion(
			PRESSED_STATE_SET, SELECTED_STATE_SET);

	/**
	 * Indicates the view is pressed, selected and its window has the focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_SELECTED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed and focused.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_STATE_SET, FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed, focused and its window has the focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_FOCUSED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed, focused and selected.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET = stateSetUnion(
			PRESSED_FOCUSED_STATE_SET, SELECTED_STATE_SET);

	/**
	 * Indicates the view is pressed, focused, selected and its window has the focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_FOCUSED_SELECTED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed and enabled.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_STATE_SET = stateSetUnion(
			PRESSED_STATE_SET, ENABLED_STATE_SET);

	/**
	 * Indicates the view is pressed, enabled and its window has the focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_ENABLED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed, enabled and selected.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET = stateSetUnion(
			PRESSED_ENABLED_STATE_SET, SELECTED_STATE_SET);

	/**
	 * Indicates the view is pressed, enabled, selected and its window has the
	 * focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_ENABLED_SELECTED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed, enabled and focused.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_ENABLED_STATE_SET, FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed, enabled, focused and its window has the
	 * focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_ENABLED_FOCUSED_STATE_SET, WINDOW_FOCUSED_STATE_SET);

	/**
	 * Indicates the view is pressed, enabled, focused and selected.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = stateSetUnion(
			PRESSED_ENABLED_FOCUSED_STATE_SET, SELECTED_STATE_SET);

	/**
	 * Indicates the view is pressed, enabled, focused, selected and its window
	 * has the focus.
	 *
	 * @see #PRESSED_STATE_SET
	 * @see #ENABLED_STATE_SET
	 * @see #SELECTED_STATE_SET
	 * @see #FOCUSED_STATE_SET
	 * @see #WINDOW_FOCUSED_STATE_SET
	 */
	protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = stateSetUnion(
			PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET,
			WINDOW_FOCUSED_STATE_SET);

	/**
	 * The order here is very important to {@link #getDrawableState()}
	 */
	private static final int[][] VIEW_STATE_SETS = { EMPTY_STATE_SET, // 0 0 0 0 0
			WINDOW_FOCUSED_STATE_SET, // 0 0 0 0 1
			SELECTED_STATE_SET, // 0 0 0 1 0
			SELECTED_WINDOW_FOCUSED_STATE_SET, // 0 0 0 1 1
			FOCUSED_STATE_SET, // 0 0 1 0 0
			FOCUSED_WINDOW_FOCUSED_STATE_SET, // 0 0 1 0 1
			FOCUSED_SELECTED_STATE_SET, // 0 0 1 1 0
			FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET, // 0 0 1 1 1
			ENABLED_STATE_SET, // 0 1 0 0 0
			ENABLED_WINDOW_FOCUSED_STATE_SET, // 0 1 0 0 1
			ENABLED_SELECTED_STATE_SET, // 0 1 0 1 0
			ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET, // 0 1 0 1 1
			ENABLED_FOCUSED_STATE_SET, // 0 1 1 0 0
			ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET, // 0 1 1 0 1
			ENABLED_FOCUSED_SELECTED_STATE_SET, // 0 1 1 1 0
			ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET, // 0 1 1 1 1
			PRESSED_STATE_SET, // 1 0 0 0 0
			PRESSED_WINDOW_FOCUSED_STATE_SET, // 1 0 0 0 1
			PRESSED_SELECTED_STATE_SET, // 1 0 0 1 0
			PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET, // 1 0 0 1 1
			PRESSED_FOCUSED_STATE_SET, // 1 0 1 0 0
			PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET, // 1 0 1 0 1
			PRESSED_FOCUSED_SELECTED_STATE_SET, // 1 0 1 1 0
			PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET, // 1 0 1 1 1
			PRESSED_ENABLED_STATE_SET, // 1 1 0 0 0
			PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET, // 1 1 0 0 1
			PRESSED_ENABLED_SELECTED_STATE_SET, // 1 1 0 1 0
			PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET, // 1 1 0 1 1
			PRESSED_ENABLED_FOCUSED_STATE_SET, // 1 1 1 0 0
			PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET, // 1 1 1 0 1
			PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET, // 1 1 1 1 0
			PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET, // 1 1 1 1 1
	};

	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is showing the last item.
	 * @hide
	 */
	protected static final int[] LAST_STATE_SET = { R.attr.state_last };
	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is showing the first item.
	 * @hide
	 */
	protected static final int[] FIRST_STATE_SET = { R.attr.state_first };
	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is showing the middle item.
	 * @hide
	 */
	protected static final int[] MIDDLE_STATE_SET = { R.attr.state_middle };
	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is showing only one item.
	 * @hide
	 */
	protected static final int[] SINGLE_STATE_SET = { R.attr.state_single };
	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is pressed and showing the last item.
	 * @hide
	 */
	protected static final int[] PRESSED_LAST_STATE_SET = { R.attr.state_last,
			R.attr.state_pressed };
	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is pressed and showing the first item.
	 * @hide
	 */
	protected static final int[] PRESSED_FIRST_STATE_SET = {
			R.attr.state_first, R.attr.state_pressed };
	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is pressed and showing the middle item.
	 * @hide
	 */
	protected static final int[] PRESSED_MIDDLE_STATE_SET = {
			R.attr.state_middle, R.attr.state_pressed };
	/**
	 * Used by views that contain lists of items. This state indicates that
	 * the view is pressed and showing only one item.
	 * @hide
	 */
	protected static final int[] PRESSED_SINGLE_STATE_SET = {
			R.attr.state_single, R.attr.state_pressed };

	/**
	 * Temporary Rect currently for use in setBackground().  This will probably
	 * be extended in the future to hold our own class with more than just
	 * a Rect. :)
	 */
	static final Rect sThreadLocal = new Rect();

	/**
	 * Map used to store views' tags.
	 */
	private static WeakHashMap<View, SparseArray<Object>> sTags;

	/**
	 * Lock used to access sTags.
	 */
	private static final Object sTagsLock = new Object();

	/**
	 * The animation currently associated with this view.
	 * @hide
	 */
	protected Animation mCurrentAnimation = null;

	/**
	 * Width as measured during measure pass.
	 * {@hide}
	 */
	protected int mMeasuredWidth;

	/**
	 * Height as measured during measure pass.
	 * {@hide}
	 */
	protected int mMeasuredHeight;

	/**
	 * The view's identifier.
	 * {@hide}
	 *
	 * @see #setId(int)
	 * @see #getId()
	 */
	int mID = NO_ID;

	/**
	 * The view's tag.
	 * {@hide}
	 *
	 * @see #setTag(Object)
	 * @see #getTag()
	 */
	protected Object mTag;

	private ScrollabilityCache mScrollCache;

	// for mPrivateFlags:
	/** {@hide} */
	static final int WANTS_FOCUS = 0x00000001;
	/** {@hide} */
	static final int FOCUSED = 0x00000002;
	/** {@hide} */
	static final int SELECTED = 0x00000004;
	/** {@hide} */
	static final int IS_ROOT_NAMESPACE = 0x00000008;
	/** {@hide} */
	static final int HAS_BOUNDS = 0x00000010;
	/** {@hide} */
	static final int DRAWN = 0x00000020;
	/**
	 * When this flag is set, this view is running an animation on behalf of its
	 * children and should therefore not cancel invalidate requests, even if they
	 * lie outside of this view's bounds.
	 *
	 * {@hide}
	 */
	static final int DRAW_ANIMATION = 0x00000040;
	/** {@hide} */
	static final int SKIP_DRAW = 0x00000080;
	/** {@hide} */
	static final int ONLY_DRAWS_BACKGROUND = 0x00000100;
	/** {@hide} */
	static final int REQUEST_TRANSPARENT_REGIONS = 0x00000200;
	/** {@hide} */
	static final int DRAWABLE_STATE_DIRTY = 0x00000400;
	/** {@hide} */
	protected static final int MEASURED_DIMENSION_SET = 0x00000800;
	/** {@hide} */
	static final int FORCE_LAYOUT = 0x00001000;
	/** {@hide} */
	static final int LAYOUT_REQUIRED = 0x00002000;

	private static final int PRESSED = 0x00004000;

    /**
     * Indicates a prepressed state;
     * the short time between ACTION_DOWN and recognizing
     * a 'real' press. Prepressed is used to recognize quick taps
     * even when they are shorter than ViewConfiguration.getTapTimeout().
     *
     * @hide
     */
    private static final int PREPRESSED             = 0x02000000;

	/** {@hide} */
	static final int DRAWING_CACHE_VALID = 0x00008000;
	/**
	 * Flag used to indicate that this view should be drawn once more (and only once
	 * more) after its animation has completed.
	 * {@hide}
	 */
	static final int ANIMATION_STARTED = 0x00010000;

	private static final int SAVE_STATE_CALLED      = 0x00020000;

	/**
	 * Indicates that the View returned true when onSetAlpha() was called and that
	 * the alpha must be restored.
	 * {@hide}
	 */
	static final int ALPHA_SET = 0x00040000;

	/**
	 * Set by {@link #setScrollContainer(boolean)}.
	 */
	static final int SCROLL_CONTAINER = 0x00080000;

	/**
	 * Set by {@link #setScrollContainer(boolean)}.
	 */
	static final int SCROLL_CONTAINER_ADDED = 0x00100000;

	/**
	 * View flag indicating whether this view was invalidated (fully or partially.)
	 *
	 * @hide
	 */
	static final int DIRTY = 0x00200000;

	/**
	 * View flag indicating whether this view was invalidated by an opaque
	 * invalidate request.
	 *
	 * @hide
	 */
	static final int DIRTY_OPAQUE = 0x00400000;

	/**
	 * Mask for {@link #DIRTY} and {@link #DIRTY_OPAQUE}.
	 *
	 * @hide
	 */
	static final int DIRTY_MASK = 0x00600000;

	/**
	 * Indicates whether the background is opaque.
	 *
	 * @hide
	 */
	static final int OPAQUE_BACKGROUND = 0x00800000;

	/**
	 * Indicates whether the scrollbars are opaque.
	 *
	 * @hide
	 */
	static final int OPAQUE_SCROLLBARS = 0x01000000;

	/**
	 * Indicates whether the view is opaque.
	 *
	 * @hide
	 */
	static final int OPAQUE_MASK = 0x01800000;

	/**
	 * Indicates whether the view is temporarily detached.
	 *
	 * @hide
	 */
	static final int CANCEL_NEXT_UP_EVENT = 0x04000000;

	/**
	 * Horizontal direction of this view is from Left to Right.
	 * Use with {@link #setLayoutDirection}.
	 * {@hide}
	 */
	public static final int LAYOUT_DIRECTION_LTR = 0x00000000;

	/**
	 * Horizontal direction of this view is from Right to Left.
	 * Use with {@link #setLayoutDirection}.
	 * {@hide}
	 */
	public static final int LAYOUT_DIRECTION_RTL = 0x40000000;

	/**
	 * Horizontal direction of this view is from deduced from the default language
	 * script for the locale. Use with {@link #setLayoutDirection}.
	 * {@hide}
	 */
	public static final int LAYOUT_DIRECTION_LOCALE = 0xC0000000;

	/**
	 * Indicates that we should awaken scroll bars once attached
	 */
	private static final int AWAKEN_SCROLL_BARS_ON_ATTACH = 0x08000000;

	/**
	 * Always allow a user to over-scroll this view, provided it is a
	 * view that can scroll.
	 *
	 * @see #getOverScrollMode()
	 * @see #setOverScrollMode(int)
	 */
	public static final int OVER_SCROLL_ALWAYS = 0;

	/**
	 * Allow a user to over-scroll this view only if the content is large
	 * enough to meaningfully scroll, provided it is a view that can scroll.
	 *
	 * @see #getOverScrollMode()
	 * @see #setOverScrollMode(int)
	 */
	public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;

	/**
	 * Never allow a user to over-scroll this view.
	 *
	 * @see #getOverScrollMode()
	 * @see #setOverScrollMode(int)
	 */
	public static final int OVER_SCROLL_NEVER = 2;

	/**
	 * Controls the over-scroll mode for this view.
	 * See {@link #overScrollBy(int, int, int, int, int, int, int, int, boolean)},
	 * {@link #OVER_SCROLL_ALWAYS}, {@link #OVER_SCROLL_IF_CONTENT_SCROLLS},
	 * and {@link #OVER_SCROLL_NEVER}.
	 */
	private int mOverScrollMode;

	/**
	 * The parent this view is attached to.
	 * {@hide}
	 *
	 * @see #getParent()
	 */
	protected ViewParent mParent;

    /**
     * {@hide}
     */
    protected AttachInfo mAttachInfo;

	/**
	 * {@hide}
	 */
	protected int mPrivateFlags;
	int mPrivateFlags2;

	/**
	 * Count of how many windows this view has been attached to.
	 */
	int mWindowAttachCount;

	/**
	 * The layout parameters associated with this view and used by the parent
	 * {@link android.view.ViewGroup} to determine how this view should be
	 * laid out.
	 * {@hide}
	 */
	protected ViewGroup.LayoutParams mLayoutParams;

	/**
	 * The view flags hold various views states.
	 * {@hide}
	 */
	int mViewFlags;

	/**
	 * The distance in pixels from the left edge of this view's parent
	 * to the left edge of this view.
	 * {@hide}
	 */
	protected int mLeft;
	/**
	 * The distance in pixels from the left edge of this view's parent
	 * to the right edge of this view.
	 * {@hide}
	 */
	protected int mRight;
	/**
	 * The distance in pixels from the top edge of this view's parent
	 * to the top edge of this view.
	 * {@hide}
	 */
	protected int mTop;
	/**
	 * The distance in pixels from the top edge of this view's parent
	 * to the bottom edge of this view.
	 * {@hide}
	 */
	protected int mBottom;

	/**
	 * The offset, in pixels, by which the content of this view is scrolled
	 * horizontally.
	 * {@hide}
	 */
	protected int mScrollX;
	/**
	 * The offset, in pixels, by which the content of this view is scrolled
	 * vertically.
	 * {@hide}
	 */
	protected int mScrollY;

	/**
	 * The left padding in pixels, that is the distance in pixels between the
	 * left edge of this view and the left edge of its content.
	 * {@hide}
	 */
	protected int mPaddingLeft;
	/**
	 * The right padding in pixels, that is the distance in pixels between the
	 * right edge of this view and the right edge of its content.
	 * {@hide}
	 */

	protected int mPaddingRight;
	/**
	 * The top padding in pixels, that is the distance in pixels between the
	 * top edge of this view and the top edge of its content.
	 * {@hide}
	 */

	protected int mPaddingTop;
	/**
	 * The bottom padding in pixels, that is the distance in pixels between the
	 * bottom edge of this view and the bottom edge of its content.
	 * {@hide}
	 */

	protected int mPaddingBottom;

	/**
	 * Briefly describes the view and is primarily used for accessibility support.
	 */
	private CharSequence mContentDescription;

	/**
	 * Cache the paddingRight set by the user to append to the scrollbar's size.
	 */
	int mUserPaddingRight;

	/**
	 * Cache the paddingBottom set by the user to append to the scrollbar's size.
	 */
	int mUserPaddingBottom;

	/**
	 * @hide
	 */
	int mOldWidthMeasureSpec = Integer.MIN_VALUE;
	/**
	 * @hide
	 */
	int mOldHeightMeasureSpec = Integer.MIN_VALUE;

	private Resources mResources = null;

	private Drawable mBGDrawable = null;
	
    private int mBackgroundResource;
    private boolean mBackgroundSizeChanged;

	/**
	 * Listener used to dispatch focus change events.
	 * This field should be made private, so it is hidden from the SDK.
	 * {@hide}
	 */
	protected OnFocusChangeListener mOnFocusChangeListener;

	/**
	 * Listener used to dispatch click events.
	 * This field should be made private, so it is hidden from the SDK.
	 * {@hide}
	 */
	protected OnClickListener mOnClickListener;

	//    protected OnTouchListener mOnTouchListener;

	/**
	 * Listener used to dispatch long click events.
	 * This field should be made private, so it is hidden from the SDK.
	 * {@hide}
	 */
	protected OnLongClickListener mOnLongClickListener;

	/**
	 * Listener used to build the context menu.
	 * This field should be made private, so it is hidden from the SDK.
	 * {@hide}
	 */
	protected OnCreateContextMenuListener mOnCreateContextMenuListener;

	private OnKeyListener mOnKeyListener;

	/**
	 * The application environment this view lives in.
	 * This field should be made private, so it is hidden from the SDK.
	 * {@hide}
	 */
	protected Context mContext;

	//private ScrollabilityCache mScrollCache;

	private int[] mDrawableState = null;

	/**
	 * When this view has focus and the next focus is {@link #FOCUS_LEFT},
	 * the user may specify which view to go to next.
	 */
	private int mNextFocusLeftId = View.NO_ID;

	/**
	 * When this view has focus and the next focus is {@link #FOCUS_RIGHT},
	 * the user may specify which view to go to next.
	 */
	private int mNextFocusRightId = View.NO_ID;

	/**
	 * When this view has focus and the next focus is {@link #FOCUS_UP},
	 * the user may specify which view to go to next.
	 */
	private int mNextFocusUpId = View.NO_ID;

	/**
	 * When this view has focus and the next focus is {@link #FOCUS_DOWN},
	 * the user may specify which view to go to next.
	 */
	private int mNextFocusDownId = View.NO_ID;

    private CheckForLongPress mPendingCheckForLongPress;
    private CheckForTap mPendingCheckForTap = null;
    
    private UnsetPressedState mUnsetPressedState;
    private PerformClick mPerformClick;

	/**
	 * Whether the long press's action has been invoked.  The tap's action is invoked on the
	 * up event while a long press is invoked as soon as the long press duration is reached, so
	 * a long press could be performed before the tap is checked, in which case the tap's action
	 * should not be invoked.
	 */
	private boolean mHasPerformedLongPress;

	/**
	 * The minimum height of the view. We'll try our best to have the height
	 * of this view to at least this amount.
	 */
	private int mMinHeight;

	/**
	 * The minimum width of the view. We'll try our best to have the width
	 * of this view to at least this amount.
	 */
	private int mMinWidth;
	/**
	 * Indicates whether the view layout direction has been resolved and drawn to the
	 * right-to-left direction.
	 *
	 * @hide
	 */
	static final int LAYOUT_DIRECTION_RESOLVED_RTL = 0x00000004;

	/**
	 * Indicates whether the view layout direction has been resolved.
	 *
	 * @hide
	 */
	static final int LAYOUT_DIRECTION_RESOLVED = 0x00000008;

	/**
	 * Horizontal direction of this view is inherited from its parent.
	 * Use with {@link #setLayoutDirection}.
	 * {@hide}
	 */
	public static final int LAYOUT_DIRECTION_INHERIT = 0x80000000;

	/**
	 * Solid color to use as a background when creating the drawing cache. Enables
	 * the cache to use 16 bit bitmaps instead of 32 bit.
	 */
	private int mDrawingCacheBackgroundColor = 0;

    /**
     * The delegate to handle touch events that are physically in this view
     * but should be handled by another view.
     */
    private TouchDelegate mTouchDelegate = null;

    /**
     * Cache the touch slop from the context that created the view.
     */
    private int mTouchSlop;

	/**
	 * Simple constructor to use when creating a view from code.
	 *
	 * @param context The Context the view is running in, through which it can
	 *        access the current theme, resources, etc.
	 */
	public View(Context context) {
		if(DBG)System.out.println("constructing View10");
		mContext = context;
		mResources = context != null ? context.getResources() : null;
		mViewFlags = SOUND_EFFECTS_ENABLED | HAPTIC_FEEDBACK_ENABLED;
		setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);

		mUIElementID = View.getAnElementID();
	}

	/**
	 * Constructor that is called when inflating a view from XML. This is called
	 * when a view is being constructed from an XML file, supplying attributes
	 * that were specified in the XML file. This version uses a default style of
	 * 0, so the only attribute values applied are those in the Context's Theme
	 * and the given AttributeSet.
	 *
	 * <p>
	 * The method onFinishInflate() will be called after all children have been
	 * added.
	 *
	 * @param context The Context the view is running in, through which it can
	 *        access the current theme, resources, etc.
	 * @param attrs The attributes of the XML tag that is inflating the view.
	 * @see #View(Context, AttributeSet, int)
	 */
	public View(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * View's background color. Can't just use Color object for
	 * some strange bugs in J2S.
	 * Descendants can set this to their default value.
	 */
	protected String bgColor = "transparent";

    private static final String TAG = "View";

    /**
     * Base class for derived classes that want to save and restore their own
     * state in {@link android.view.View#onSaveInstanceState()}.
     */
    public static class BaseSavedState extends AbsSavedState {
        /**
         * Constructor used when reading from a parcel. Reads the state of the superclass.
         *
         * @param source
         */
        public BaseSavedState(Parcel source) {
            super(source);
        }

        /**
         * Constructor called by derived classes when creating their SavedState objects
         *
         * @param superState The state of the superclass of this view
         */
        public BaseSavedState(Parcelable superState) {
            super(superState);
        }

        public static final Creator<BaseSavedState> CREATOR =
                new Creator<BaseSavedState>() {
            public BaseSavedState createFromParcel(Parcel in) {
                return new BaseSavedState(in);
            }

            public BaseSavedState[] newArray(int size) {
                return new BaseSavedState[size];
            }
        };
    }

	/**
	 * Perform inflation from XML and apply a class-specific base style. This
	 * constructor of View allows subclasses to use their own base style when
	 * they are inflating. For example, a Button class's constructor would call
	 * this version of the super class constructor and supply
	 * <code>R.attr.buttonStyle</code> for <var>defStyle</var>; this allows
	 * the theme's button style to modify all of the base view attributes (in
	 * particular its background) as well as the Button class's attributes.
	 *
	 * @param context The Context the view is running in, through which it can
	 *        access the current theme, resources, etc.
	 * @param attrs The attributes of the XML tag that is inflating the view.
	 * @param defStyle The default style to apply to this view. If 0, no style
	 *        will be applied (beyond what is included in the theme). This may
	 *        either be an attribute resource, whose value will be retrieved
	 *        from the current theme, or an explicit style resource.
	 * @see #View(Context, AttributeSet)
	 */
	public View(Context context, AttributeSet attrs, int defStyle) {
		if(DBG)System.out.println("constructing View1");
		mContext = context;
		mResources = context != null ? context.getResources() : null;
		mViewFlags = SOUND_EFFECTS_ENABLED | HAPTIC_FEEDBACK_ENABLED;
        if (context == null) {
            throw new NullPointerException();
        }
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
		mUIElementID = View.getAnElementID();

		TypedArray a = context.obtainStyledAttributes(attrs,
				com.android.internal.R.styleable.View, defStyle, 0);

		Drawable background = null;

		int leftPadding = -1;
		int topPadding = -1;
		int rightPadding = -1;
		int bottomPadding = -1;

		int padding = -1;

		int viewFlagValues = 0;
		int viewFlagMasks = 0;

		boolean setScrollContainer = false;

		int x = 0;
		int y = 0;

		int scrollbarStyle = SCROLLBARS_INSIDE_OVERLAY;

		int overScrollMode = mOverScrollMode;
		final int N = a.getIndexCount();
       for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case com.android.internal.R.styleable.View_background:
                    background = a.getDrawable(attr);
                    break;
                case com.android.internal.R.styleable.View_padding:
                    padding = a.getDimensionPixelSize(attr, -1);
                    break;
                 case com.android.internal.R.styleable.View_paddingLeft:
                    leftPadding = a.getDimensionPixelSize(attr, -1);
                    break;
                case com.android.internal.R.styleable.View_paddingTop:
                    topPadding = a.getDimensionPixelSize(attr, -1);
                    break;
                case com.android.internal.R.styleable.View_paddingRight:
                    rightPadding = a.getDimensionPixelSize(attr, -1);
                    break;
                case com.android.internal.R.styleable.View_paddingBottom:
                    bottomPadding = a.getDimensionPixelSize(attr, -1);
                    break;
                case com.android.internal.R.styleable.View_scrollX:
                    x = a.getDimensionPixelOffset(attr, 0);
                    break;
                case com.android.internal.R.styleable.View_scrollY:
                    y = a.getDimensionPixelOffset(attr, 0);
                    break;
                case com.android.internal.R.styleable.View_id:
                    mID = a.getResourceId(attr, NO_ID);
                    break;
                case com.android.internal.R.styleable.View_tag:
                    mTag = a.getText(attr);
                    break;
                case com.android.internal.R.styleable.View_fitsSystemWindows:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= FITS_SYSTEM_WINDOWS;
                        viewFlagMasks |= FITS_SYSTEM_WINDOWS;
                    }
                    break;
                case com.android.internal.R.styleable.View_focusable:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= FOCUSABLE;
                        viewFlagMasks |= FOCUSABLE_MASK;
                    }
                    break;
                case com.android.internal.R.styleable.View_focusableInTouchMode:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= FOCUSABLE_IN_TOUCH_MODE | FOCUSABLE;
                        viewFlagMasks |= FOCUSABLE_IN_TOUCH_MODE | FOCUSABLE_MASK;
                    }
                    break;
                case com.android.internal.R.styleable.View_clickable:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= CLICKABLE;
                        viewFlagMasks |= CLICKABLE;
                    }
                    break;
                case com.android.internal.R.styleable.View_longClickable:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= LONG_CLICKABLE;
                        viewFlagMasks |= LONG_CLICKABLE;
                    }
                    break;
                case com.android.internal.R.styleable.View_saveEnabled:
                    if (!a.getBoolean(attr, true)) {
                        viewFlagValues |= SAVE_DISABLED;
                        viewFlagMasks |= SAVE_DISABLED_MASK;
                    }
                    break;
                case com.android.internal.R.styleable.View_duplicateParentState:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= DUPLICATE_PARENT_STATE;
                        viewFlagMasks |= DUPLICATE_PARENT_STATE;
                    }
                    break;
                case com.android.internal.R.styleable.View_visibility:
                    final int visibility = a.getInt(attr, 0);
                    if (visibility != 0) {
                        viewFlagValues |= VISIBILITY_FLAGS[visibility];
                        viewFlagMasks |= VISIBILITY_MASK;
                    }
                    break;
                case com.android.internal.R.styleable.View_drawingCacheQuality:
                    final int cacheQuality = a.getInt(attr, 0);
                    if (cacheQuality != 0) {
                        viewFlagValues |= DRAWING_CACHE_QUALITY_FLAGS[cacheQuality];
                        viewFlagMasks |= DRAWING_CACHE_QUALITY_MASK;
                    }
                    break;
                case com.android.internal.R.styleable.View_contentDescription:
                    mContentDescription = a.getString(attr);
                    break;
                case com.android.internal.R.styleable.View_soundEffectsEnabled:
                    if (!a.getBoolean(attr, true)) {
                        viewFlagValues &= ~SOUND_EFFECTS_ENABLED;
                        viewFlagMasks |= SOUND_EFFECTS_ENABLED;
                    }
                    break;
                case com.android.internal.R.styleable.View_hapticFeedbackEnabled:
                    if (!a.getBoolean(attr, true)) {
                        viewFlagValues &= ~HAPTIC_FEEDBACK_ENABLED;
                        viewFlagMasks |= HAPTIC_FEEDBACK_ENABLED;
                    }
                    break;
                case R.styleable.View_scrollbars:
                    final int scrollbars = a.getInt(attr, SCROLLBARS_NONE);
                    if (scrollbars != SCROLLBARS_NONE) {
                        viewFlagValues |= scrollbars;
                        viewFlagMasks |= SCROLLBARS_MASK;
                        initializeScrollbars(a);
                    }
                    break;
                case R.styleable.View_fadingEdge:
                    final int fadingEdge = a.getInt(attr, FADING_EDGE_NONE);
                    if (fadingEdge != FADING_EDGE_NONE) {
                        viewFlagValues |= fadingEdge;
                        viewFlagMasks |= FADING_EDGE_MASK;
                        initializeFadingEdge(a);
                    }
                    break;
                case R.styleable.View_scrollbarStyle:
                    scrollbarStyle = a.getInt(attr, SCROLLBARS_INSIDE_OVERLAY);
                    if (scrollbarStyle != SCROLLBARS_INSIDE_OVERLAY) {
                        viewFlagValues |= scrollbarStyle & SCROLLBARS_STYLE_MASK;
                        viewFlagMasks |= SCROLLBARS_STYLE_MASK;
                    }
                    break;
                case R.styleable.View_isScrollContainer:
                    setScrollContainer = true;
                    if (a.getBoolean(attr, false)) {
                        setScrollContainer(true);
                    }
                    break;
                case com.android.internal.R.styleable.View_keepScreenOn:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= KEEP_SCREEN_ON;
                        viewFlagMasks |= KEEP_SCREEN_ON;
                    }
                    break;
                case R.styleable.View_filterTouchesWhenObscured:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= FILTER_TOUCHES_WHEN_OBSCURED;
                        viewFlagMasks |= FILTER_TOUCHES_WHEN_OBSCURED;
                    }
                    break;
                case R.styleable.View_nextFocusLeft:
                    mNextFocusLeftId = a.getResourceId(attr, View.NO_ID);
                    break;
                case R.styleable.View_nextFocusRight:
                    mNextFocusRightId = a.getResourceId(attr, View.NO_ID);
                    break;
                case R.styleable.View_nextFocusUp:
                    mNextFocusUpId = a.getResourceId(attr, View.NO_ID);
                    break;
                case R.styleable.View_nextFocusDown:
                    mNextFocusDownId = a.getResourceId(attr, View.NO_ID);
                    break;
                case R.styleable.View_minWidth:
                    mMinWidth = a.getDimensionPixelSize(attr, 0);
                    break;
                case R.styleable.View_minHeight:
                    mMinHeight = a.getDimensionPixelSize(attr, 0);
                    break;
                case R.styleable.View_onClick:
                    if (context.isRestricted()) {
                        throw new IllegalStateException("The android:onClick attribute cannot "
                                + "be used within a restricted context");
                    }

                    final String handlerName = a.getString(attr);
                    if (handlerName != null) {
                        setOnClickListener(new OnClickListener() {
                            private Method mHandler;

                            public void onClick(View v) {
                                if (mHandler == null) {
                                    try {
                                        mHandler = getContext().getClass().getMethod(handlerName,
                                                View.class);
                                    } catch (NoSuchMethodException e) {
                                        int id = getId();
                                        String idText = id == NO_ID ? "" : " with id '"
                                                + id + "'";
                                        throw new IllegalStateException("Could not find a method " +
                                                handlerName + "(View) in the activity "
                                                + getContext().getClass() + " for onClick handler"
                                                + " on view " + View.this.getClass() + idText, e);
                                    }
                                }

                                try {
                                    mHandler.invoke(getContext(), View.this);
                                } catch (IllegalAccessException e) {
                                    throw new IllegalStateException("Could not execute non "
                                            + "public method of the activity", e);
                                } catch (Exception e) {
                                    throw new IllegalStateException("Could not execute "
                                            + "method of the activity", e);
                                }
                            }
                        });
                    }
                    break;
                case R.styleable.View_overScrollMode:
                    overScrollMode = a.getInt(attr, OVER_SCROLL_IF_CONTENT_SCROLLS);
                    break;
            }
        }


		setOverScrollMode(overScrollMode);
		if(DBG)System.out.println("constructing View3");
		if (background != null) {
			setBackgroundDrawable(background);
		}

		if (padding >= 0) {
			leftPadding = padding;
			topPadding = padding;
			rightPadding = padding;
			bottomPadding = padding;
		}

		// If the user specified the padding (either with android:padding or
		// android:paddingLeft/Top/Right/Bottom), use this padding, otherwise
		// use the default padding or the padding from the background drawable
		// (stored at this point in mPadding*)
		setPadding(leftPadding >= 0 ? leftPadding : mPaddingLeft,
				topPadding >= 0 ? topPadding : mPaddingTop,
				rightPadding >= 0 ? rightPadding : mPaddingRight,
				bottomPadding >= 0 ? bottomPadding : mPaddingBottom);

		if (viewFlagMasks != 0) {
			setFlags(viewFlagValues, viewFlagMasks);
		}

		// Needs to be called after mViewFlags is set
		if (scrollbarStyle != SCROLLBARS_INSIDE_OVERLAY) {
			recomputePadding();
		}

		if (x != 0 || y != 0) {
			//			scrollTo(x, y);
		}

		if (!setScrollContainer && (viewFlagValues & SCROLLBARS_VERTICAL) != 0) {
			setScrollContainer(true);
		}

		computeOpaqueFlags();

		a.recycle();
		if(DBG)System.out.println("constructing View2");
	}

    /**
     * Sets the background color for this view.
     * @param color the color of the background
     */
    public void setBackgroundColor(int color) {
        setBackgroundDrawable(new ColorDrawable(color));
    }

    /**
     * Set the background to a given Drawable, or remove the background. If the
     * background has padding, this View's padding is set to the background's
     * padding. However, when a background is removed, this View's padding isn't
     * touched. If setting the padding is desired, please use
     * {@link #setPadding(int, int, int, int)}.
     *
     * @param d The Drawable to use as the background, or null to remove the
     *        background
     */
    public void setBackgroundDrawable(Drawable d) {
        boolean requestLayout = false;

        mBackgroundResource = 0;

        /*
         * Regardless of whether we're setting a new background or not, we want
         * to clear the previous drawable.
         */
        if (mBGDrawable != null) {
            mBGDrawable.setCallback(null);
            unscheduleDrawable(mBGDrawable);
        }

        if (d != null) {
            Rect padding = new Rect();
            if (d.getPadding(padding)) {
                setPadding(padding.left, padding.top, padding.right, padding.bottom);
            }

            // Compare the minimum sizes of the old Drawable and the new.  If there isn't an old or
            // if it has a different minimum size, we should layout again
            if (mBGDrawable == null || mBGDrawable.getMinimumHeight() != d.getMinimumHeight() ||
                    mBGDrawable.getMinimumWidth() != d.getMinimumWidth()) {
                requestLayout = true;
            }

            d.setCallback(this);
            if (d.isStateful()) {
                d.setState(getDrawableState());
            }
            d.setVisible(getVisibility() == VISIBLE, false);
            mBGDrawable = d;

            if ((mPrivateFlags & SKIP_DRAW) != 0) {
                mPrivateFlags &= ~SKIP_DRAW;
                mPrivateFlags |= ONLY_DRAWS_BACKGROUND;
                requestLayout = true;
            }
        } else {
            /* Remove the background */
            mBGDrawable = null;

            if ((mPrivateFlags & ONLY_DRAWS_BACKGROUND) != 0) {
                /*
                 * This view ONLY drew the background before and we're removing
                 * the background, so now it won't draw anything
                 * (hence we SKIP_DRAW)
                 */
                mPrivateFlags &= ~ONLY_DRAWS_BACKGROUND;
                mPrivateFlags |= SKIP_DRAW;
            }

            /*
             * When the background is set, we try to apply its padding to this
             * View. When the background is removed, we don't touch this View's
             * padding. This is noted in the Javadocs. Hence, we don't need to
             * requestLayout(), the invalidate() below is sufficient.
             */

            // The old background's minimum size could have affected this
            // View's layout, so let's requestLayout
            requestLayout = true;
        }

        computeOpaqueFlags();

        if (requestLayout) {
            requestLayout();
        }

        mBackgroundSizeChanged = true;
        invalidate();
    }

	/**
	 * Non-public constructor for use in testing
	 */
	public View() {
	}

    /**
     * <p>
     * Initializes the fading edges from a given set of styled attributes. This
     * method should be called by subclasses that need fading edges and when an
     * instance of these subclasses is created programmatically rather than
     * being inflated from XML. This method is automatically called when the XML
     * is inflated.
     * </p>
     *
     * @param a the styled attributes set to initialize the fading edges from
     */
    protected void initializeFadingEdge(TypedArray a) {
        initScrollCache();

        mScrollCache.fadingEdgeLength = a.getDimensionPixelSize(
                R.styleable.View_fadingEdgeLength,
                ViewConfiguration.get(mContext).getScaledFadingEdgeLength());
    }

    /**
     * Returns the size of the vertical faded edges used to indicate that more
     * content in this view is visible.
     *
     * @return The size in pixels of the vertical faded edge or 0 if vertical
     *         faded edges are not enabled for this view.
     * @attr ref android.R.styleable#View_fadingEdgeLength
     */
    public int getVerticalFadingEdgeLength() {
        if (isVerticalFadingEdgeEnabled()) {
            ScrollabilityCache cache = mScrollCache;
            if (cache != null) {
                return cache.fadingEdgeLength;
            }
        }
        return 0;
    }

    /**
     * Set the size of the faded edge used to indicate that more content in this
     * view is available.  Will not change whether the fading edge is enabled; use
     * {@link #setVerticalFadingEdgeEnabled} or {@link #setHorizontalFadingEdgeEnabled}
     * to enable the fading edge for the vertical or horizontal fading edges.
     *
     * @param length The size in pixels of the faded edge used to indicate that more
     *        content in this view is visible.
     */
    public void setFadingEdgeLength(int length) {
        initScrollCache();
        mScrollCache.fadingEdgeLength = length;
    }

    /**
     * Returns the size of the horizontal faded edges used to indicate that more
     * content in this view is visible.
     *
     * @return The size in pixels of the horizontal faded edge or 0 if horizontal
     *         faded edges are not enabled for this view.
     * @attr ref android.R.styleable#View_fadingEdgeLength
     */
    public int getHorizontalFadingEdgeLength() {
        if (isHorizontalFadingEdgeEnabled()) {
            ScrollabilityCache cache = mScrollCache;
            if (cache != null) {
                return cache.fadingEdgeLength;
            }
        }
        return 0;
    }
    
	/**
	 * Returns the width of the vertical scrollbar.
	 *
	 * @return The width in pixels of the vertical scrollbar or 0 if there
	 *         is no vertical scrollbar.
	 */
//	public int getVerticalScrollbarWidth() {
//
//		return 0;
//	}

	/**
	 * Register a callback to be invoked when focus of this view changed.
	 *
	 * @param l The callback that will run.
	 */
	public void setOnFocusChangeListener(OnFocusChangeListener l) {
		mOnFocusChangeListener = l;
	}

	/**
	 * Returns the focus-change callback registered for this view.
	 *
	 * @return The callback, or null if one is not registered.
	 */
	public OnFocusChangeListener getOnFocusChangeListener() {
		return mOnFocusChangeListener;
	}

	/**
	 * Register a callback to be invoked when this view is clicked. If this view is not
	 * clickable, it becomes clickable.
	 *
	 * @param l The callback that will run
	 *
	 * @see #setClickable(boolean)
	 */
	public void setOnClickListener(OnClickListener l) {
		if (!isClickable()) {
			setClickable(true);
		}
		mOnClickListener = l;
	}

	/**
	 * Register a callback to be invoked when this view is clicked and held. If this view is not
	 * long clickable, it becomes long clickable.
	 *
	 * @param l The callback that will run
	 *
	 * @see #setLongClickable(boolean)
	 */
	public void setOnLongClickListener(OnLongClickListener l) {
		if (!isLongClickable()) {
			setLongClickable(true);
		}
		mOnLongClickListener = l;
	}

	/**
	 * Register a callback to be invoked when the context menu for this view is
	 * being built. If this view is not long clickable, it becomes long clickable.
	 *
	 * @param l The callback that will run
	 *
	 */
    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        mOnCreateContextMenuListener = l;
    }

    /**
     * Interface definition for a callback to be invoked when the context menu
     * for this view is being built.
     */
    public interface OnCreateContextMenuListener {
        /**
         * Called when the context menu for this view is being built. It is not
         * safe to hold onto the menu after this method returns.
         *
         * @param menu The context menu that is being built
         * @param v The view for which the context menu is being built
         * @param menuInfo Extra information about the item for which the
         *            context menu should be shown. This information will vary
         *            depending on the class of v.
         */
        void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo);
    }

    private final class UnsetPressedState implements Runnable {
        public void run() {
            setPressed(false);
        }
    }
    
	/**
	 * Call this view's OnClickListener, if it is defined.
	 *
	 * @return True there was an assigned OnClickListener that was called, false
	 *         otherwise is returned.
	 */
	public boolean performClick() {
		if (mOnClickListener != null) {
            long start = System.currentTimeMillis();
            Log.d(TAG, "View performClick at time" + start);
			mOnClickListener.onClick(this);
			return true;
		}

		return false;
	}

	/**
	 * Call this view's OnLongClickListener, if it is defined. Invokes the context menu if the
	 * OnLongClickListener did not consume the event.
	 *
	 * @return True if one of the above receivers consumed the event, false otherwise.
	 */
	public boolean performLongClick() {
		boolean handled = false;
		if (mOnLongClickListener != null) {
			handled = mOnLongClickListener.onLongClick(View.this);
		}
		if (!handled) {
			handled = showContextMenu();
		}
		return handled;
	}

	/**
	 * Bring up the context menu for this view.
	 *
	 * @return Whether a context menu was displayed.
	 */
	public boolean showContextMenu() {
		return getParent().showContextMenuForChild(this);
	}

	/**
	 * Register a callback to be invoked when a key is pressed in this view.
	 * @param l the key listener to attach to this view
	 */
	public void setOnKeyListener(OnKeyListener l) {
		mOnKeyListener = l;
	}

	/**
	 * Register a callback to be invoked when a touch event is sent to this view.
	 * @param l the touch listener to attach to this view
	 */
	//	public void setOnTouchListener(OnTouchListener l) {
	//		mOnTouchListener = l;
	//	}

	/**
	 * Give this view focus. This will cause {@link #onFocusChanged} to be called.
	 *
	 * Note: this does not check whether this {@link View} should get focus, it just
	 * gives it focus no matter what.  It should only be called internally by framework
	 * code that knows what it is doing, namely {@link #requestFocus(int, Rect)}.
	 *
	 * @param direction values are View.FOCUS_UP, View.FOCUS_DOWN,
	 *        View.FOCUS_LEFT or View.FOCUS_RIGHT. This is the direction which
	 *        focus moved when requestFocus() is called. It may not always
	 *        apply, in which case use the default View.FOCUS_DOWN.
	 * @param previouslyFocusedRect The rectangle of the view that had focus
	 *        prior in this View's coordinate system.
	 */
	void handleFocusGainInternal(int direction, Rect previouslyFocusedRect) {
		if (DBG) {
			System.out.println(this + " requestFocus()");
		}

		if ((mPrivateFlags & FOCUSED) == 0) {
			mPrivateFlags |= FOCUSED;

			if (mParent != null) {
				mParent.requestChildFocus(this, this);
			}

			onFocusChanged(true, direction, previouslyFocusedRect);
			//refreshDrawableState();
		}
	}

	/**
	 * Request that a rectangle of this view be visible on the screen,
	 * scrolling if necessary just enough.
	 *
	 * <p>A View should call this if it maintains some notion of which part
	 * of its content is interesting.  For example, a text editing view
	 * should call this when its cursor moves.
	 *
	 * @param rectangle The rectangle.
	 * @return Whether any parent scrolled.
	 */
	public boolean requestRectangleOnScreen(Rect rectangle) {
		return requestRectangleOnScreen(rectangle, false);
	}

	/**
	 * Request that a rectangle of this view be visible on the screen,
	 * scrolling if necessary just enough.
	 *
	 * <p>A View should call this if it maintains some notion of which part
	 * of its content is interesting.  For example, a text editing view
	 * should call this when its cursor moves.
	 *
	 * <p>When <code>immediate</code> is set to true, scrolling will not be
	 * animated.
	 *
	 * @param rectangle The rectangle.
	 * @param immediate True to forbid animated scrolling, false otherwise
	 * @return Whether any parent scrolled.
	 */
	public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
		View child = this;
		ViewParent parent = mParent;
		boolean scrolled = false;
		while (parent != null) {
			scrolled |= parent.requestChildRectangleOnScreen(child, rectangle,
					immediate);

			// offset rect so next call has the rectangle in the
			// coordinate system of its direct child.
			rectangle.offset(child.getLeft(), child.getTop());
			rectangle.offset(-child.getScrollX(), -child.getScrollY());

			if (!(parent instanceof View)) {
				break;
			}

			child = (View) parent;
			parent = child.getParent();
		}
		return scrolled;
	}

	/**
	 * Called when this view wants to give up focus. This will cause
	 * {@link #onFocusChanged} to be called.
	 */
	public void clearFocus() {
		if (DBG) {
			System.out.println(this + " clearFocus()");
		}

		if ((mPrivateFlags & FOCUSED) != 0) {
			mPrivateFlags &= ~FOCUSED;

			if (mParent != null) {
				mParent.clearChildFocus(this);
			}

			onFocusChanged(false, 0, null);
			refreshDrawableState();
		}
	}

	/**
	 * Called to clear the focus of a view that is about to be removed.
	 * Doesn't call clearChildFocus, which prevents this view from taking
	 * focus again before it has been removed from the parent
	 */
	void clearFocusForRemoval() {
		if ((mPrivateFlags & FOCUSED) != 0) {
			mPrivateFlags &= ~FOCUSED;

			onFocusChanged(false, 0, null);
			refreshDrawableState();
		}
	}

	/**
	 * Called internally by the view system when a new view is getting focus.
	 * This is what clears the old focus.
	 */
	void unFocus() {
		/*if (DBG) {
		    System.out.println(this + " unFocus()");
		}*/

		if ((mPrivateFlags & FOCUSED) != 0) {
			mPrivateFlags &= ~FOCUSED;

			onFocusChanged(false, 0, null);
			refreshDrawableState();
		}
	}

    /**
     * Call this to force a view to update its drawable state. This will cause
     * drawableStateChanged to be called on this view. Views that are interested
     * in the new state should call getDrawableState.
     *
     * @see #drawableStateChanged
     * @see #getDrawableState
     */
    public void refreshDrawableState() {
        mPrivateFlags |= DRAWABLE_STATE_DIRTY;
        drawableStateChanged();

        ViewParent parent = mParent;
        if (parent != null) {
            parent.childDrawableStateChanged(this);
        }
    }
	/**
	 * Returns true if this view has focus iteself, or is the ancestor of the
	 * view that has focus.
	 *
	 * @return True if this view has or contains focus, false otherwise.
	 */
	public boolean hasFocus() {
		return (mPrivateFlags & FOCUSED) != 0;
	}

	/**
	 * Returns true if this view is focusable or if it contains a reachable View
	 * for which {@link #hasFocusable()} returns true. A "reachable hasFocusable()"
	 * is a View whose parents do not block descendants focus.
	 *
	 * Only {@link #VISIBLE} views are considered focusable.
	 *
	 * @return True if the view is focusable or if the view contains a focusable
	 *         View, false otherwise.
	 *
	 * @see ViewGroup#FOCUS_BLOCK_DESCENDANTS
	 */
	public boolean hasFocusable() {
		return (mViewFlags & VISIBILITY_MASK) == VISIBLE && isFocusable();
	}

	/**
	 * Called by the view system when the focus state of this view changes.
	 * When the focus change event is caused by directional navigation, direction
	 * and previouslyFocusedRect provide insight into where the focus is coming from.
	 * When overriding, be sure to call up through to the super class so that
	 * the standard focus handling will occur.
	 *
	 * @param gainFocus True if the View has focus; false otherwise.
	 * @param direction The direction focus has moved when requestFocus()
	 *                  is called to give this view focus. Values are
	 *                  {@link #FOCUS_UP}, {@link #FOCUS_DOWN}, {@link #FOCUS_LEFT} or
	 *                  {@link #FOCUS_RIGHT}. It may not always apply, in which
	 *                  case use the default.
	 * @param previouslyFocusedRect The rectangle, in this view's coordinate
	 *        system, of the previously focused view.  If applicable, this will be
	 *        passed in as finer grained information about where the focus is coming
	 *        from (in addition to direction).  Will be <code>null</code> otherwise.
	 */
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		/*
		        InputMethodManager imm = InputMethodManager.peekInstance();
		        if (!gainFocus) {
		            if (isPressed()) {
		                setPressed(false);
		            }
		            if (imm != null && mAttachInfo != null
		                    && mAttachInfo.mHasWindowFocus) {
		                imm.focusOut(this);
		            }
		            onFocusLost();
		        } else if (imm != null && mAttachInfo != null
		                && mAttachInfo.mHasWindowFocus) {
		            imm.focusIn(this);
		        }
		*/
		invalidate();
		if (mOnFocusChangeListener != null) {
			mOnFocusChangeListener.onFocusChange(this, gainFocus);
		}

		if (mAttachInfo != null) {
			mAttachInfo.mKeyDispatchState.reset(this);
		}
	}

    /**
     * {@inheritDoc}
     */
    public void sendAccessibilityEvent(int eventType) {
        if (AccessibilityManager.getInstance(mContext).isEnabled()) {
            sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(eventType));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        event.setClassName(getClass().getName());
        event.setPackageName(getContext().getPackageName());
        event.setEnabled(isEnabled());
        event.setContentDescription(mContentDescription);

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED && mAttachInfo != null) {
            ArrayList<View> focusablesTempList = mAttachInfo.mFocusablesTempList;
            getRootView().addFocusables(focusablesTempList, View.FOCUS_FORWARD, FOCUSABLES_ALL);
            event.setItemCount(focusablesTempList.size());
            event.setCurrentItemIndex(focusablesTempList.indexOf(this));
            focusablesTempList.clear();
        }

        dispatchPopulateAccessibilityEvent(event);

        AccessibilityManager.getInstance(mContext).sendAccessibilityEvent(event);
    }

    /**
     * Dispatches an {@link AccessibilityEvent} to the {@link View} children
     * to be populated.
     *
     * @param event The event.
     *
     * @return True if the event population was completed.
     */
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }
    
	/**
	 * Gets the {@link View} description. It briefly describes the view and is
	 * primarily used for accessibility support. Set this property to enable
	 * better accessibility support for your application. This is especially
	 * true for views that do not have textual representation (For example,
	 * ImageButton).
	 *
	 * @return The content descriptiopn.
	 *
	 * @attr ref android.R.styleable#View_contentDescription
	 */
	public CharSequence getContentDescription() {
		return mContentDescription;
	}

	/**
	 * Sets the {@link View} description. It briefly describes the view and is
	 * primarily used for accessibility support. Set this property to enable
	 * better accessibility support for your application. This is especially
	 * true for views that do not have textual representation (For example,
	 * ImageButton).
	 *
	 * @param contentDescription The content description.
	 *
	 * @attr ref android.R.styleable#View_contentDescription
	 */
	public void setContentDescription(CharSequence contentDescription) {
		mContentDescription = contentDescription;
	}

	/**
	 * Invoked whenever this view loses focus, either by losing window focus or by losing
	 * focus within its window. This method can be used to clear any state tied to the
	 * focus. For instance, if a button is held pressed with the trackball and the window
	 * loses focus, this method can be used to cancel the press.
	 *
	 * Subclasses of View overriding this method should always call super.onFocusLost().
	 *
	 * @see #onFocusChanged(boolean, int, android.graphics.Rect)
	 * @see #onWindowFocusChanged(boolean)
	 *
	 * @hide pending API council approval
	 */
	protected void onFocusLost() {
	    resetPressedState();
	}

	private void resetPressedState() {
	    if ((mViewFlags & ENABLED_MASK) == DISABLED) {
	        return;
	    }

	    if (isPressed()) {
	        setPressed(false);

	        if (!mHasPerformedLongPress) {
	            removeLongPressCallback();
	        }
	    }
	}

	/**
	 * Returns true if this view has focus
	 *
	 * @return True if this view has focus, false otherwise.
	 */
	public boolean isFocused() {
		return (mPrivateFlags & FOCUSED) != 0;
	}

	/**
	 * Find the view in the hierarchy rooted at this view that currently has
	 * focus.
	 *
	 * @return The view that currently has focus, or null if no focused view can
	 *         be found.
	 */
	public View findFocus() {
		return (mPrivateFlags & FOCUSED) != 0 ? this : null;
	}

	/**
	 * Change whether this view is one of the set of scrollable containers in
	 * its window.  This will be used to determine whether the window can
	 * resize or must pan when a soft input area is open -- scrollable
	 * containers allow the window to use resize mode since the container
	 * will appropriately shrink.
	 */
	public void setScrollContainer(boolean isScrollContainer) {
		if (isScrollContainer) {
			if (mAttachInfo != null
					&& (mPrivateFlags & SCROLL_CONTAINER_ADDED) == 0) {
				mAttachInfo.mScrollContainers.add(this);
				mPrivateFlags |= SCROLL_CONTAINER_ADDED;
			}
			mPrivateFlags |= SCROLL_CONTAINER;
		} else {
			if ((mPrivateFlags & SCROLL_CONTAINER_ADDED) != 0) {
				mAttachInfo.mScrollContainers.remove(this);
			}
			mPrivateFlags &= ~(SCROLL_CONTAINER | SCROLL_CONTAINER_ADDED);
		}
	}

	/**
	 * Returns the quality of the drawing cache.
	 *
	 * @return One of {@link #DRAWING_CACHE_QUALITY_AUTO},
	 *         {@link #DRAWING_CACHE_QUALITY_LOW}, or {@link #DRAWING_CACHE_QUALITY_HIGH}
	 *
	 * @see #setDrawingCacheQuality(int)
	 * @see #setDrawingCacheEnabled(boolean)
	 * @see #isDrawingCacheEnabled()
	 *
	 * @attr ref android.R.styleable#View_drawingCacheQuality
	 */
	public int getDrawingCacheQuality() {
		return mViewFlags & DRAWING_CACHE_QUALITY_MASK;
	}

	/**
	 * Set the drawing cache quality of this view. This value is used only when the
	 * drawing cache is enabled
	 *
	 * @param quality One of {@link #DRAWING_CACHE_QUALITY_AUTO},
	 *        {@link #DRAWING_CACHE_QUALITY_LOW}, or {@link #DRAWING_CACHE_QUALITY_HIGH}
	 *
	 * @see #getDrawingCacheQuality()
	 * @see #setDrawingCacheEnabled(boolean)
	 * @see #isDrawingCacheEnabled()
	 *
	 * @attr ref android.R.styleable#View_drawingCacheQuality
	 */
	public void setDrawingCacheQuality(int quality) {
		setFlags(quality, DRAWING_CACHE_QUALITY_MASK);
	}

	/**
	 * Returns whether the screen should remain on, corresponding to the current
	 * value of {@link #KEEP_SCREEN_ON}.
	 *
	 * @return Returns true if {@link #KEEP_SCREEN_ON} is set.
	 *
	 * @see #setKeepScreenOn(boolean)
	 *
	 * @attr ref android.R.styleable#View_keepScreenOn
	 */
	public boolean getKeepScreenOn() {
		return (mViewFlags & KEEP_SCREEN_ON) != 0;
	}

	/**
	 * Controls whether the screen should remain on, modifying the
	 * value of {@link #KEEP_SCREEN_ON}.
	 *
	 * @param keepScreenOn Supply true to set {@link #KEEP_SCREEN_ON}.
	 *
	 * @see #getKeepScreenOn()
	 *
	 * @attr ref android.R.styleable#View_keepScreenOn
	 */
	public void setKeepScreenOn(boolean keepScreenOn) {
		setFlags(keepScreenOn ? KEEP_SCREEN_ON : 0, KEEP_SCREEN_ON);
	}

	/**
	 * @return The user specified next focus ID.
	 *
	 * @attr ref android.R.styleable#View_nextFocusLeft
	 */
	public int getNextFocusLeftId() {
		return mNextFocusLeftId;
	}

	/**
	 * Set the id of the view to use for the next focus
	 *
	 * @param nextFocusLeftId
	 *
	 * @attr ref android.R.styleable#View_nextFocusLeft
	 */
	public void setNextFocusLeftId(int nextFocusLeftId) {
		mNextFocusLeftId = nextFocusLeftId;
	}

	/**
	 * @return The user specified next focus ID.
	 *
	 * @attr ref android.R.styleable#View_nextFocusRight
	 */
	public int getNextFocusRightId() {
		return mNextFocusRightId;
	}

	/**
	 * Set the id of the view to use for the next focus
	 *
	 * @param nextFocusRightId
	 *
	 * @attr ref android.R.styleable#View_nextFocusRight
	 */
	public void setNextFocusRightId(int nextFocusRightId) {
		mNextFocusRightId = nextFocusRightId;
	}

	/**
	 * @return The user specified next focus ID.
	 *
	 * @attr ref android.R.styleable#View_nextFocusUp
	 */
	public int getNextFocusUpId() {
		return mNextFocusUpId;
	}

	/**
	 * Set the id of the view to use for the next focus
	 *
	 * @param nextFocusUpId
	 *
	 * @attr ref android.R.styleable#View_nextFocusUp
	 */
	public void setNextFocusUpId(int nextFocusUpId) {
		mNextFocusUpId = nextFocusUpId;
	}

	/**
	 * @return The user specified next focus ID.
	 *
	 * @attr ref android.R.styleable#View_nextFocusDown
	 */
	public int getNextFocusDownId() {
		return mNextFocusDownId;
	}

	/**
	 * Set the id of the view to use for the next focus
	 *
	 * @param nextFocusDownId
	 *
	 * @attr ref android.R.styleable#View_nextFocusDown
	 */
	public void setNextFocusDownId(int nextFocusDownId) {
		mNextFocusDownId = nextFocusDownId;
	}

	/**
	 * Returns the visibility of this view and all of its ancestors
	 *
	 * @return True if this view and all of its ancestors are {@link #VISIBLE}
	 */
	public boolean isShown() {
		View current = this;
		//noinspection ConstantConditions
		do {
			if ((current.mViewFlags & VISIBILITY_MASK) != VISIBLE) {
				return false;
			}
			ViewParent parent = current.mParent;
			if (parent == null) {
				return false; // We are not attached to the view root
			}
			if (!(parent instanceof View)) {
				return true;
			}
			current = (View) parent;
		} while (current != null);

		return false;
	}

	/**
	 * Apply the insets for system windows to this view, if the FITS_SYSTEM_WINDOWS flag
	 * is set
	 *
	 * @param insets Insets for system windows
	 *
	 * @return True if this view applied the insets, false otherwise
	 */
	protected boolean fitSystemWindows(Rect insets) {
		if ((mViewFlags & FITS_SYSTEM_WINDOWS) == FITS_SYSTEM_WINDOWS) {
			mPaddingLeft = insets.left;
			mPaddingTop = insets.top;
			mPaddingRight = insets.right;
			mPaddingBottom = insets.bottom;
			requestLayout();
			return true;
		}
		return false;
	}

	/**
	 * Determine if this view has the FITS_SYSTEM_WINDOWS flag set.
	 * @return True if window has FITS_SYSTEM_WINDOWS set
	 *
	 * @hide
	 */
	public boolean isFitsSystemWindowsFlagSet() {
		return (mViewFlags & FITS_SYSTEM_WINDOWS) == FITS_SYSTEM_WINDOWS;
	}

	/**
	 * Returns the visibility status for this view.
	 *
	 * @return One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
	 * @attr ref android.R.styleable#View_visibility
	 */
	public int getVisibility() {
		return mViewFlags & VISIBILITY_MASK;
	}

    /**
     * Set the enabled state of this view.
     * 
     * @param visibility One of {@link #VISIBLE}, {@link #INVISIBLE}, or
     *            {@link #GONE}.
     * @attr ref android.R.styleable#View_visibility
     */
    public void setVisibility(int visibility) {
        setFlags(visibility, VISIBILITY_MASK);
        String visible = "visible";
        if (visibility != VISIBLE)
            visible = "hidden";
        if (DebugUtils.DEBUG_VIEW_IN_BROWSER) {
            /**
             * @j2sNative 
             * var thisView = document.getElementById(this.mUIElementID);
             * if (thisView != null) thisView.style.visibility = visible;
             */{}
        } else {
            // Mayloon : Because we have HTML TAG, so we need to setVisibility for
            //           TextView recursively
            if (this instanceof ViewGroup)
                ((ViewGroup) this).setVisibilityRec(visible);
            if (mBGDrawable != null)
               mBGDrawable.setVisible(visibility == VISIBLE, false);
        }
        requestLayout();
        invalidate();
    }

	/**
	 * Returns the enabled status for this view. The interpretation of the
	 * enabled state varies by subclass.
	 *
	 * @return True if this view is enabled, false otherwise.
	 */
	public boolean isEnabled() {
		return (mViewFlags & ENABLED_MASK) == ENABLED;
	}

	/**
	 * Set the enabled state of this view. The interpretation of the enabled
	 * state varies by subclass.
	 *
	 * @param enabled True if this view is enabled, false otherwise.
	 */
	public void setEnabled(boolean enabled) {
		if (enabled == isEnabled())
			return;

		setFlags(enabled ? ENABLED : DISABLED, ENABLED_MASK);

		/*
		 * The View most likely has to change its appearance, so refresh
		 * the drawable state.
		 */
		refreshDrawableState();

		// Invalidate too, since the default behavior for views is to be
		// be drawn at 50% alpha rather than to change the drawable.
		invalidate();
	}

	/**
	 * Set whether this view can receive the focus.
	 *
	 * Setting this to false will also ensure that this view is not focusable
	 * in touch mode.
	 *
	 * @param focusable If true, this view can receive the focus.
	 *
	 * @see #setFocusableInTouchMode(boolean)
	 * @attr ref android.R.styleable#View_focusable
	 */
	public void setFocusable(boolean focusable) {
		if (!focusable) {
			setFlags(0, FOCUSABLE_IN_TOUCH_MODE);
		}
		setFlags(focusable ? FOCUSABLE : NOT_FOCUSABLE, FOCUSABLE_MASK);
	}

	/**
	 * Set whether this view can receive focus while in touch mode.
	 *
	 * Setting this to true will also ensure that this view is focusable.
	 *
	 * @param focusableInTouchMode If true, this view can receive the focus while
	 *   in touch mode.
	 *
	 * @see #setFocusable(boolean)
	 * @attr ref android.R.styleable#View_focusableInTouchMode
	 */
	public void setFocusableInTouchMode(boolean focusableInTouchMode) {
		// Focusable in touch mode should always be set before the focusable flag
		// otherwise, setting the focusable flag will trigger a focusableViewAvailable()
		// which, in touch mode, will not successfully request focus on this view
		// because the focusable in touch mode flag is not set
		setFlags(focusableInTouchMode ? FOCUSABLE_IN_TOUCH_MODE : 0,
				FOCUSABLE_IN_TOUCH_MODE);
		if (focusableInTouchMode) {
			setFlags(FOCUSABLE, FOCUSABLE_MASK);
		}
	}

	/**
	 * Set whether this view should have sound effects enabled for events such as
	 * clicking and touching.
	 *
	 * <p>You may wish to disable sound effects for a view if you already play sounds,
	 * for instance, a dial key that plays dtmf tones.
	 *
	 * @param soundEffectsEnabled whether sound effects are enabled for this view.
	 * @see #isSoundEffectsEnabled()
	 * @see #playSoundEffect(int)
	 * @attr ref android.R.styleable#View_soundEffectsEnabled
	 */
	public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
		setFlags(soundEffectsEnabled ? SOUND_EFFECTS_ENABLED : 0,
				SOUND_EFFECTS_ENABLED);
	}

	/**
	 * @return whether this view should have sound effects enabled for events such as
	 *     clicking and touching.
	 *
	 * @see #setSoundEffectsEnabled(boolean)
	 * @see #playSoundEffect(int)
	 * @attr ref android.R.styleable#View_soundEffectsEnabled
	 */
	public boolean isSoundEffectsEnabled() {
		return SOUND_EFFECTS_ENABLED == (mViewFlags & SOUND_EFFECTS_ENABLED);
	}

	/**
	 * Set whether this view should have haptic feedback for events such as
	 * long presses.
	 *
	 * <p>You may wish to disable haptic feedback if your view already controls
	 * its own haptic feedback.
	 *
	 * @param hapticFeedbackEnabled whether haptic feedback enabled for this view.
	 * @see #isHapticFeedbackEnabled()
	 * @see #performHapticFeedback(int)
	 * @attr ref android.R.styleable#View_hapticFeedbackEnabled
	 */
	public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
		setFlags(hapticFeedbackEnabled ? HAPTIC_FEEDBACK_ENABLED : 0,
				HAPTIC_FEEDBACK_ENABLED);
	}

	/**
	 * @return whether this view should have haptic feedback enabled for events
	 * long presses.
	 *
	 * @see #setHapticFeedbackEnabled(boolean)
	 * @see #performHapticFeedback(int)
	 * @attr ref android.R.styleable#View_hapticFeedbackEnabled
	 */
	public boolean isHapticFeedbackEnabled() {
		return HAPTIC_FEEDBACK_ENABLED == (mViewFlags & HAPTIC_FEEDBACK_ENABLED);
	}

	/**
	 * If this view doesn't do any drawing on its own, set this flag to
	 * allow further optimizations. By default, this flag is not set on
	 * View, but could be set on some View subclasses such as ViewGroup.
	 *
	 * Typically, if you override {@link #onDraw} you should clear this flag.
	 *
	 * @param willNotDraw whether or not this View draw on its own
	 */
	public void setWillNotDraw(boolean willNotDraw) {
		setFlags(willNotDraw ? WILL_NOT_DRAW : 0, DRAW_MASK);
	}

	/**
	 * Returns whether or not this View draws on its own.
	 *
	 * @return true if this view has nothing to draw, false otherwise
	 */
	public boolean willNotDraw() {
		return (mViewFlags & DRAW_MASK) == WILL_NOT_DRAW;
	}

	/**
	 * When a View's drawing cache is enabled, drawing is redirected to an
	 * offscreen bitmap. Some views, like an ImageView, must be able to
	 * bypass this mechanism if they already draw a single bitmap, to avoid
	 * unnecessary usage of the memory.
	 *
	 * @param willNotCacheDrawing true if this view does not cache its
	 *        drawing, false otherwise
	 */
	public void setWillNotCacheDrawing(boolean willNotCacheDrawing) {
		setFlags(willNotCacheDrawing ? WILL_NOT_CACHE_DRAWING : 0,
				WILL_NOT_CACHE_DRAWING);
	}

	/**
	 * Returns whether or not this View can cache its drawing or not.
	 *
	 * @return true if this view does not cache its drawing, false otherwise
	 */
	public boolean willNotCacheDrawing() {
		return (mViewFlags & WILL_NOT_CACHE_DRAWING) == WILL_NOT_CACHE_DRAWING;
	}

	/**
	 * Indicates whether this view reacts to click events or not.
	 *
	 * @return true if the view is clickable, false otherwise
	 *
	 * @see #setClickable(boolean)
	 * @attr ref android.R.styleable#View_clickable
	 */
	public boolean isClickable() {
		return (mViewFlags & CLICKABLE) == CLICKABLE;
	}

	/**
	 * Enables or disables click events for this view. When a view
	 * is clickable it will change its state to "pressed" on every click.
	 * Subclasses should set the view clickable to visually react to
	 * user's clicks.
	 *
	 * @param clickable true to make the view clickable, false otherwise
	 *
	 * @see #isClickable()
	 * @attr ref android.R.styleable#View_clickable
	 */
	public void setClickable(boolean clickable) {
		setFlags(clickable ? CLICKABLE : 0, CLICKABLE);
	}

	/**
	 * Indicates whether this view reacts to long click events or not.
	 *
	 * @return true if the view is long clickable, false otherwise
	 *
	 * @see #setLongClickable(boolean)
	 * @attr ref android.R.styleable#View_longClickable
	 */
	public boolean isLongClickable() {
		return (mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE;
	}

	/**
	 * Enables or disables long click events for this view. When a view is long
	 * clickable it reacts to the user holding down the button for a longer
	 * duration than a tap. This event can either launch the listener or a
	 * context menu.
	 *
	 * @param longClickable true to make the view long clickable, false otherwise
	 * @see #isLongClickable()
	 * @attr ref android.R.styleable#View_longClickable
	 */
	public void setLongClickable(boolean longClickable) {
		setFlags(longClickable ? LONG_CLICKABLE : 0, LONG_CLICKABLE);
	}

    /**
     * Sets the pressed that for this view.
     * 
     * @see #isClickable()
     * @see #setClickable(boolean)
     * @param pressed Pass true to set the View's internal state to "pressed", or
     *            false to reverts the View's internal state from a previously
     *            set "pressed" state.
     */
    public void setPressed(boolean pressed) {
        if (pressed) {
            mPrivateFlags |= PRESSED;
        } else {
            mPrivateFlags &= ~PRESSED;
        }
        refreshDrawableState();
        dispatchSetPressed(pressed);
    }

	/**
	 * Dispatch setPressed to all of this View's children.
	 *
	 * @see #setPressed(boolean)
	 *
	 * @param pressed The new pressed state
	 */
	protected void dispatchSetPressed(boolean pressed) {
	}

	/**
	 * Indicates whether the view is currently in pressed state. Unless
	 * {@link #setPressed(boolean)} is explicitly called, only clickable views can enter
	 * the pressed state.
	 *
	 * @see #setPressed
	 * @see #isClickable()
	 * @see #setClickable(boolean)
	 *
	 * @return true if the view is currently pressed, false otherwise
	 */
	public boolean isPressed() {
		return (mPrivateFlags & PRESSED) == PRESSED;
	}

	/**
	 * Indicates whether this view will save its state (that is,
	 * whether its {@link #onSaveInstanceState} method will be called).
	 *
	 * @return Returns true if the view state saving is enabled, else false.
	 *
	 * @see #setSaveEnabled(boolean)
	 * @attr ref android.R.styleable#View_saveEnabled
	 */
	public boolean isSaveEnabled() {
		return (mViewFlags & SAVE_DISABLED_MASK) != SAVE_DISABLED;
	}

	/**
	 * Controls whether the saving of this view's state is
	 * enabled (that is, whether its {@link #onSaveInstanceState} method
	 * will be called).  Note that even if freezing is enabled, the
	 * view still must have an id assigned to it (via {@link #setId setId()})
	 * for its state to be saved.  This flag can only disable the
	 * saving of this view; any child views may still have their state saved.
	 *
	 * @param enabled Set to false to <em>disable</em> state saving, or true
	 * (the default) to allow it.
	 *
	 * @see #isSaveEnabled()
	 * @see #setId(int)
	 * @see #onSaveInstanceState()
	 * @attr ref android.R.styleable#View_saveEnabled
	 */
	public void setSaveEnabled(boolean enabled) {
		setFlags(enabled ? 0 : SAVE_DISABLED, SAVE_DISABLED_MASK);
	}

	/**
	 * Gets whether the framework should discard touches when the view's
	 * window is obscured by another visible window.
	 * Refer to the {@link View} security documentation for more details.
	 *
	 * @return True if touch filtering is enabled.
	 *
	 * @see #setFilterTouchesWhenObscured(boolean)
	 * @attr ref android.R.styleable#View_filterTouchesWhenObscured
	 */
	public boolean getFilterTouchesWhenObscured() {
		return (mViewFlags & FILTER_TOUCHES_WHEN_OBSCURED) != 0;
	}

	/**
	 * Sets whether the framework should discard touches when the view's
	 * window is obscured by another visible window.
	 * Refer to the {@link View} security documentation for more details.
	 *
	 * @param enabled True if touch filtering should be enabled.
	 *
	 * @see #getFilterTouchesWhenObscured
	 * @attr ref android.R.styleable#View_filterTouchesWhenObscured
	 */
	public void setFilterTouchesWhenObscured(boolean enabled) {
		setFlags(enabled ? 0 : FILTER_TOUCHES_WHEN_OBSCURED,
				FILTER_TOUCHES_WHEN_OBSCURED);
	}

	/**
	 * Returns whether this View is able to take focus.
	 *
	 * @return True if this view can take focus, or false otherwise.
	 * @attr ref android.R.styleable#View_focusable
	 */
	public final boolean isFocusable() {
		return FOCUSABLE == (mViewFlags & FOCUSABLE_MASK);
	}

	/**
	 * When a view is focusable, it may not want to take focus when in touch mode.
	 * For example, a button would like focus when the user is navigating via a D-pad
	 * so that the user can click on it, but once the user starts touching the screen,
	 * the button shouldn't take focus
	 * @return Whether the view is focusable in touch mode.
	 * @attr ref android.R.styleable#View_focusableInTouchMode
	 */
	public final boolean isFocusableInTouchMode() {
		return FOCUSABLE_IN_TOUCH_MODE == (mViewFlags & FOCUSABLE_IN_TOUCH_MODE);
	}

	/**
	 * Find the nearest view in the specified direction that can take focus.
	 * This does not actually give focus to that view.
	 *
	 * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
	 *
	 * @return The nearest focusable in the specified direction, or null if none
	 *         can be found.
	 */
	public View focusSearch(int direction) {
		if (mParent != null) {
			return mParent.focusSearch(this, direction);
		} else {
			return null;
		}
	}

	/**
	 * This method is the last chance for the focused view and its ancestors to
	 * respond to an arrow key. This is called when the focused view did not
	 * consume the key internally, nor could the view system find a new view in
	 * the requested direction to give focus to.
	 *
	 * @param focused The currently focused view.
	 * @param direction The direction focus wants to move. One of FOCUS_UP,
	 *        FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT.
	 * @return True if the this view consumed this unhandled move.
	 */
	public boolean dispatchUnhandledMove(View focused, int direction) {
		return false;
	}

	/**
	 * If a user manually specified the next view id for a particular direction,
	 * use the root to look up the view.  Once a view is found, it is cached
	 * for future lookups.
	 * @param root The root view of the hierarchy containing this view.
	 * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
	 * @return The user specified next view, or null if there is none.
	 */
	View findUserSetNextFocus(View root, int direction) {
		switch (direction) {
		case FOCUS_LEFT:
			if (mNextFocusLeftId == View.NO_ID)
				return null;
			return findViewShouldExist(root, mNextFocusLeftId);
		case FOCUS_RIGHT:
			if (mNextFocusRightId == View.NO_ID)
				return null;
			return findViewShouldExist(root, mNextFocusRightId);
		case FOCUS_UP:
			if (mNextFocusUpId == View.NO_ID)
				return null;
			return findViewShouldExist(root, mNextFocusUpId);
		case FOCUS_DOWN:
			if (mNextFocusDownId == View.NO_ID)
				return null;
			return findViewShouldExist(root, mNextFocusDownId);
		}
		return null;
	}

	private static View findViewShouldExist(View root, int childViewId) {
		View result = root.findViewById(childViewId);
		if (result == null) {
		}
		return result;
	}

	/**
	 * Find and return all focusable views that are descendants of this view,
	 * possibly including this view if it is focusable itself.
	 *
	 * @param direction The direction of the focus
	 * @return A list of focusable views
	 */
	public ArrayList<View> getFocusables(int direction) {
		ArrayList<View> result = new ArrayList<View>(24);
		addFocusables(result, direction);
		return result;
	}

	/**
	 * Add any focusable views that are descendants of this view (possibly
	 * including this view if it is focusable itself) to views.  If we are in touch mode,
	 * only add views that are also focusable in touch mode.
	 *
	 * @param views Focusable views found so far
	 * @param direction The direction of the focus
	 */
	public void addFocusables(ArrayList<View> views, int direction) {
		addFocusables(views, direction, FOCUSABLES_TOUCH_MODE);
	}

	/**
	 * Adds any focusable views that are descendants of this view (possibly
	 * including this view if it is focusable itself) to views. This method
	 * adds all focusable views regardless if we are in touch mode or
	 * only views focusable in touch mode if we are in touch mode depending on
	 * the focusable mode paramater.
	 *
	 * @param views Focusable views found so far or null if all we are interested is
	 *        the number of focusables.
	 * @param direction The direction of the focus.
	 * @param focusableMode The type of focusables to be added.
	 *
	 * @see #FOCUSABLES_ALL
	 * @see #FOCUSABLES_TOUCH_MODE
	 */
	public void addFocusables(ArrayList<View> views, int direction,
			int focusableMode) {
		if (!isFocusable()) {
			return;
		}

		if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE
				&& isInTouchMode() && !isFocusableInTouchMode()) {
			return;
		}

		if (views != null) {
			views.add(this);
		}
	}

	/**
	 * Find and return all touchable views that are descendants of this view,
	 * possibly including this view if it is touchable itself.
	 *
	 * @return A list of touchable views
	 */
	public ArrayList<View> getTouchables() {
		ArrayList<View> result = new ArrayList<View>();
		addTouchables(result);
		return result;
	}

	/**
	 * Add any touchable views that are descendants of this view (possibly
	 * including this view if it is touchable itself) to views.
	 *
	 * @param views Touchable views found so far
	 */
	public void addTouchables(ArrayList<View> views) {
		final int viewFlags = mViewFlags;

		if (((viewFlags & CLICKABLE) == CLICKABLE || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)
				&& (viewFlags & ENABLED_MASK) == ENABLED) {
			views.add(this);
		}
	}

	/**
	 * Call this to try to give focus to a specific view or to one of its
	 * descendants.
	 *
	 * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns false),
	 * or if it is focusable and it is not focusable in touch mode ({@link #isFocusableInTouchMode})
	 * while the device is in touch mode.
	 *
	 * See also {@link #focusSearch}, which is what you call to say that you
	 * have focus, and you want your parent to look for the next one.
	 *
	 * This is equivalent to calling {@link #requestFocus(int, Rect)} with arguments
	 * {@link #FOCUS_DOWN} and <code>null</code>.
	 *
	 * @return Whether this view or one of its descendants actually took focus.
	 */
	public boolean requestFocus() {
		return requestFocus(View.FOCUS_DOWN);
	}

	/**
	 * Call this to try to give focus to a specific view or to one of its
	 * descendants and give it a hint about what direction focus is heading.
	 *
	 * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns false),
	 * or if it is focusable and it is not focusable in touch mode ({@link #isFocusableInTouchMode})
	 * while the device is in touch mode.
	 *
	 * See also {@link #focusSearch}, which is what you call to say that you
	 * have focus, and you want your parent to look for the next one.
	 *
	 * This is equivalent to calling {@link #requestFocus(int, Rect)} with
	 * <code>null</code> set for the previously focused rectangle.
	 *
	 * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
	 * @return Whether this view or one of its descendants actually took focus.
	 */
	public final boolean requestFocus(int direction) {
		return requestFocus(direction, null);
	}

	/**
	 * Call this to try to give focus to a specific view or to one of its descendants
	 * and give it hints about the direction and a specific rectangle that the focus
	 * is coming from.  The rectangle can help give larger views a finer grained hint
	 * about where focus is coming from, and therefore, where to show selection, or
	 * forward focus change internally.
	 *
	 * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns false),
	 * or if it is focusable and it is not focusable in touch mode ({@link #isFocusableInTouchMode})
	 * while the device is in touch mode.
	 *
	 * A View will not take focus if it is not visible.
	 *
	 * A View will not take focus if one of its parents has {@link android.view.ViewGroup#getDescendantFocusability()}
	 * equal to {@link ViewGroup#FOCUS_BLOCK_DESCENDANTS}.
	 *
	 * See also {@link #focusSearch}, which is what you call to say that you
	 * have focus, and you want your parent to look for the next one.
	 *
	 * You may wish to override this method if your custom {@link View} has an internal
	 * {@link View} that it wishes to forward the request to.
	 *
	 * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
	 * @param previouslyFocusedRect The rectangle (in this View's coordinate system)
	 *        to give a finer grained hint about where focus is coming from.  May be null
	 *        if there is no hint.
	 * @return Whether this view or one of its descendants actually took focus.
	 */
	public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
		// need to be focusable
		if ((mViewFlags & FOCUSABLE_MASK) != FOCUSABLE
				|| (mViewFlags & VISIBILITY_MASK) != VISIBLE) {
			return false;
		}

		// need to be focusable in touch mode if in touch mode
		if (isInTouchMode()
				&& (FOCUSABLE_IN_TOUCH_MODE != (mViewFlags & FOCUSABLE_IN_TOUCH_MODE))) {
			return false;
		}

		// need to not have any parents blocking us
		if (hasAncestorThatBlocksDescendantFocus()) {
			return false;
		}

		handleFocusGainInternal(direction, previouslyFocusedRect);
		return true;
	}

	/**
	 * Call this to try to give focus to a specific view or to one of its descendants. This is a
	 * special variant of {@link #requestFocus() } that will allow views that are not focuable in
	 * touch mode to request focus when they are touched.
	 *
	 * @return Whether this view or one of its descendants actually took focus.
	 *
	 * @see #isInTouchMode()
	 *
	 */
	public final boolean requestFocusFromTouch() {
		return requestFocus(View.FOCUS_DOWN);
	}

	/**
	 * @return Whether any ancestor of this view blocks descendant focus.
	 */
	private boolean hasAncestorThatBlocksDescendantFocus() {
		ViewParent ancestor = mParent;
		while (ancestor instanceof ViewGroup) {
			final ViewGroup vgAncestor = (ViewGroup) ancestor;
			if (vgAncestor.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
				return true;
			} else {
				ancestor = vgAncestor.getParent();
			}
		}
		return false;
	}

	public void dispatchStartTemporaryDetach() {
		onStartTemporaryDetach();
	}

	/**
	 * This is called when a container is going to temporarily detach a child, with
	 * {@link ViewGroup#detachViewFromParent(View) ViewGroup.detachViewFromParent}.
	 * It will either be followed by {@link #onFinishTemporaryDetach()} or
	 * {@link #onDetachedFromWindow()} when the container is done.
	 */
	public void onStartTemporaryDetach() {
		mPrivateFlags |= CANCEL_NEXT_UP_EVENT;
	}

	/**
	 * @hide
	 */
	public void dispatchFinishTemporaryDetach() {
		onFinishTemporaryDetach();
	}

	/**
	 * Called after {@link #onStartTemporaryDetach} when the container is done
	 * changing the view.
	 */
	public void onFinishTemporaryDetach() {
	}

	/**
	 * Return the global {@link KeyEvent.DispatcherState KeyEvent.DispatcherState}
	 * for this view's window.  Returns null if the view is not currently attached
	 * to the window.  Normally you will not need to use this directly, but
	 * just use the standard high-level event callbacks like {@link #onKeyDown}.
	 */
	public KeyEvent.DispatcherState getKeyDispatcherState() {
		return mAttachInfo != null ? mAttachInfo.mKeyDispatchState : null;
	}

	/**
	 * Dispatch a key event before it is processed by any input method
	 * associated with the view hierarchy.  This can be used to intercept
	 * key events in special situations before the IME consumes them; a
	 * typical example would be handling the BACK key to update the application's
	 * UI instead of allowing the IME to see it and close itself.
	 *
	 * @param event The key event to be dispatched.
	 * @return True if the event was handled, false otherwise.
	 */
	public boolean dispatchKeyEventPreIme(KeyEvent event) {
		return onKeyPreIme(event.getKeyCode(), event);
	}

	/**
	 * Dispatch a key event to the next view on the focus path. This path runs
	 * from the top of the view tree down to the currently focused view. If this
	 * view has focus, it will dispatch to itself. Otherwise it will dispatch
	 * the next node down the focus path. This method also fires any key
	 * listeners.
	 *
	 * @param event The key event to be dispatched.
	 * @return True if the event was handled, false otherwise.
	 */
	public boolean dispatchKeyEvent(KeyEvent event) {
		//    	System.out.println("dispatchKeyEvent " + event.getKeyCode());
		if (mOnKeyListener != null && (mViewFlags & ENABLED_MASK) == ENABLED
				&& mOnKeyListener.onKey(this, event.getKeyCode(), event)) {
			return true;
		}

		return event.dispatch(this,
				mAttachInfo != null ? mAttachInfo.mKeyDispatchState : null,
				this);
	}

	/**
	 * Dispatches a key shortcut event.
	 *
	 * @param event The key event to be dispatched.
	 * @return True if the event was handled by the view, false otherwise.
	 */
	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		return onKeyShortcut(event.getKeyCode(), event);
	}

	/**
	 * Called when the window containing this view gains or loses window focus.
	 * ViewGroups should override to route to their children.
	 *
	 * @param hasFocus True if the window containing this view now has focus,
	 *        false otherwise.
	 */
	public void dispatchWindowFocusChanged(boolean hasFocus) {
		onWindowFocusChanged(hasFocus);
	}
    /**
     * Dispatch a window visibility change down the view hierarchy.
     * ViewGroups should override to route to their children.
     *
     * @param visibility The new visibility of the window.
     *
     * @see #onWindowVisibilityChanged
     */
    public void dispatchWindowVisibilityChanged(int visibility) {
        onWindowVisibilityChanged(visibility);
    }
	/**
	 * Called when the window containing this view gains or loses focus.  Note
	 * that this is separate from view focus: to receive key events, both
	 * your view and its window must have focus.  If a window is displayed
	 * on top of yours that takes input focus, then your own window will lose
	 * focus but the view focus will remain unchanged.
	 *
	 * @param hasWindowFocus True if the window containing this view now has
	 *        focus, false otherwise.
	 */
	public void onWindowFocusChanged(boolean hasWindowFocus) {
	}

	/**
	 * Returns true if this view is in a window that currently has window focus.
	 * Note that this is not the same as the view itself having focus.
	 *
	 * @return True if this view is in a window that currently has window focus.
	 */
	public boolean hasWindowFocus() {
		return mAttachInfo != null && mAttachInfo.mHasWindowFocus;
	}

	/**
	 * Dispatch a view visibility change down the view hierarchy.
	 * ViewGroups should override to route to their children.
	 * @param changedView The view whose visibility changed. Could be 'this' or
	 * an ancestor view.
	 * @param visibility The new visibility of changedView: {@link #VISIBLE},
	 * {@link #INVISIBLE} or {@link #GONE}.
	 */
	/*protected void dispatchVisibilityChanged(View changedView, int visibility) {
	    onVisibilityChanged(changedView, visibility);
	}*/

	/**
	 * Called when the visibility of the view or an ancestor of the view is changed.
	 * @param changedView The view whose visibility changed. Could be 'this' or
	 * an ancestor view.
	 * @param visibility The new visibility of changedView: {@link #VISIBLE},
	 * {@link #INVISIBLE} or {@link #GONE}.
	 */
	/*protected void onVisibilityChanged(View changedView, int visibility) {
	    if (visibility == VISIBLE) {
	        if (mAttachInfo != null) {
	            initialAwakenScrollBars();
	        } else {
	            mPrivateFlags |= AWAKEN_SCROLL_BARS_ON_ATTACH;
	        }
	    }
	}*/

	/**
	 * Dispatch a hint about whether this view is displayed. For instance, when
	 * a View moves out of the screen, it might receives a display hint indicating
	 * the view is not displayed. Applications should not <em>rely</em> on this hint
	 * as there is no guarantee that they will receive one.
	 *
	 * @param hint A hint about whether or not this view is displayed:
	 * {@link #VISIBLE} or {@link #INVISIBLE}.
	 */
	public void dispatchDisplayHint(int hint) {
		onDisplayHint(hint);
	}

	/**
	 * Gives this view a hint about whether is displayed or not. For instance, when
	 * a View moves out of the screen, it might receives a display hint indicating
	 * the view is not displayed. Applications should not <em>rely</em> on this hint
	 * as there is no guarantee that they will receive one.
	 *
	 * @param hint A hint about whether or not this view is displayed:
	 * {@link #VISIBLE} or {@link #INVISIBLE}.
	 */
	protected void onDisplayHint(int hint) {
	}

	/**
	 * Private function to aggregate all per-view attributes in to the view
	 * root.
	 */
	void dispatchCollectViewAttributes(int visibility) {
		performCollectViewAttributes(visibility);
	}

	void performCollectViewAttributes(int visibility) {
		//noinspection PointlessBitwiseExpression
		if (((visibility | mViewFlags) & (VISIBILITY_MASK | KEEP_SCREEN_ON)) == (VISIBLE | KEEP_SCREEN_ON)) {
			mAttachInfo.mKeepScreenOn = true;
		}
	}

	void needGlobalAttributesUpdate(boolean force) {
		AttachInfo ai = mAttachInfo;
		if (ai != null) {
			if (ai.mKeepScreenOn || force) {
				ai.mRecomputeGlobalAttributes = true;
			}
		}
	}

	/**
	 * Returns whether the device is currently in touch mode.  Touch mode is entered
	 * once the user begins interacting with the device by touch, and affects various
	 * things like whether focus is always visible to the user.
	 *
	 * @return Whether the device is in touch mode.
	 */
	public boolean isInTouchMode() {
		if (mAttachInfo != null) {
			return mAttachInfo.mInTouchMode;
		} else {
			return ViewRoot.isInTouchMode();
		}
	}

	/**
	 * Returns the context the view is running in, through which it can
	 * access the current theme, resources, etc.
	 *
	 * @return The view's Context.
	 */
	public final Context getContext() {
		return mContext;
	}

	/**
	 * Handle a key event before it is processed by any input method
	 * associated with the view hierarchy.  This can be used to intercept
	 * key events in special situations before the IME consumes them; a
	 * typical example would be handling the BACK key to update the application's
	 * UI instead of allowing the IME to see it and close itself.
	 *
	 * @param keyCode The value in event.getKeyCode().
	 * @param event Description of the key event.
	 * @return If you handled the event, return true. If you want to allow the
	 *         event to be handled by the next receiver, return false.
	 */
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		return false;
	}

    /**
     * Implement this method to handle touch screen motion events.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    public boolean onTouchEvent(MotionEvent event) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "View onTouchEvent at time:" + start);
        final int viewFlags = mViewFlags;

        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return (((viewFlags & CLICKABLE) == CLICKABLE ||
                    (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE));
        }

        if (mTouchDelegate != null) {
            if (mTouchDelegate.onTouchEvent(event)) {
                return true;
            }
        }

        if (((viewFlags & CLICKABLE) == CLICKABLE || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    boolean prepressed = (mPrivateFlags & PREPRESSED) != 0;
                    if ((mPrivateFlags & PRESSED) != 0 || prepressed) {
                        // take focus if we don't have it already and we should in
                        // touch mode.
                        boolean focusTaken = false;
                        if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                            focusTaken = requestFocus();
                        }

                        // MayLoon adjust the sequence for the message queue implementation
                        if (mUnsetPressedState == null) {
                            mUnsetPressedState = new UnsetPressedState();
                        }

                        if (prepressed) {
                            //mPrivateFlags |= PRESSED;
                            refreshDrawableState();
                            postDelayed(mUnsetPressedState,
                                    ViewConfiguration.getPressedStateDuration());
                        } else if (!post(mUnsetPressedState)) {
                            // If the post failed, unpress right now
                            mUnsetPressedState.run();
                        }
                        
                        if (!mHasPerformedLongPress) {
                            // This is a tap, so remove the longpress check
                            removeLongPressCallback();

                            // Only perform take click actions if we were in the pressed state
                            if (!focusTaken) {
                                // Use a Runnable and post this rather than calling
                                // performClick directly. This lets other visual state
                                // of the view update before click actions start.
                                if (mPerformClick == null) {
                                    mPerformClick = new PerformClick();
                                }
                                if (!post(mPerformClick)) {
                                    performClick();
                                }
                            }
                        }
                        setPressed(false);      //reset press state on action up

                        removeTapCallback();
                    }
                    break;

                case MotionEvent.ACTION_DOWN:
                    if (mPendingCheckForTap == null) {
                        mPendingCheckForTap = new CheckForTap();
                    }
                    mPrivateFlags |= PREPRESSED;
                    mHasPerformedLongPress = false;
                    postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                    mPrivateFlags |= PRESSED;   //set press state on action down
                    break;

                case MotionEvent.ACTION_CANCEL:
                    mPrivateFlags &= ~PRESSED;
                    refreshDrawableState();
                    removeTapCallback();
                    break;

                case MotionEvent.ACTION_MOVE:
                    final int x = (int) event.getX();
                    final int y = (int) event.getY();

                    // Be lenient about moving outside of buttons
                    int slop = mTouchSlop;
                    if ((x < 0 - slop) || (x >= getWidth() + slop) ||
                            (y < 0 - slop) || (y >= getHeight() + slop)) {
                        // Outside button
                        removeTapCallback();
                        if ((mPrivateFlags & PRESSED) != 0) {
                            // Remove any future long press/tap checks
                            removeLongPressCallback();

                            // Need to switch from pressed to not pressed
                            mPrivateFlags &= ~PRESSED;
                            refreshDrawableState();
                        }
                    }
                    break;
            }
            return true;
        }

        return false;
    }

    /**
     * Remove the longpress detection timer.
     */
    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
          removeCallbacks(mPendingCheckForLongPress);
        }
    }
    
    /**
     * Remove the tap detection timer.
     */
    private void removeTapCallback() {
        if (mPendingCheckForTap != null) {
            mPrivateFlags &= ~PREPRESSED;
            removeCallbacks(mPendingCheckForTap);
        }
    }

	/**
	 * Default implementation of {@link KeyEvent.Callback#onKeyMultiple(int, int, KeyEvent)
	 * KeyEvent.Callback.onKeyMultiple()}: perform press of the view
	 * when {@link KeyEvent#KEYCODE_DPAD_CENTER} or {@link KeyEvent#KEYCODE_ENTER}
	 * is released, if the view is enabled and clickable.
	 *
	 * @param keyCode A key code that represents the button pressed, from
	 *                {@link android.view.KeyEvent}.
	 * @param event   The KeyEvent object that defines the button action.
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean result = false;

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER: {
			if ((mViewFlags & ENABLED_MASK) == DISABLED) {
				return true;
			}
			// Long clickable items don't necessarily have to be clickable
			if (((mViewFlags & CLICKABLE) == CLICKABLE || (mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)
					&& (event.getRepeatCount() == 0)) {
				setPressed(true);
				if ((mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE) {
					//postCheckForLongClick(0);
				}
				return true;
			}
			break;
		}
		}
		return result;
	}

	/**
	 * Default implementation of {@link KeyEvent.Callback#onKeyLongPress(int, KeyEvent)
	 * KeyEvent.Callback.onKeyLongPress()}: always returns false (doesn't handle
	 * the event).
	 */
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	/**
	 * Default implementation of {@link KeyEvent.Callback#onKeyMultiple(int, int, KeyEvent)
	 * KeyEvent.Callback.onKeyMultiple()}: perform clicking of the view
	 * when {@link KeyEvent#KEYCODE_DPAD_CENTER} or
	 * {@link KeyEvent#KEYCODE_ENTER} is released.
	 *
	 * @param keyCode A key code that represents the button pressed, from
	 *                {@link android.view.KeyEvent}.
	 * @param event   The KeyEvent object that defines the button action.
	 */
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		boolean result = false;

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER: {
			if ((mViewFlags & ENABLED_MASK) == DISABLED) {
				return true;
			}
			if ((mViewFlags & CLICKABLE) == CLICKABLE && isPressed()) {
				setPressed(false);

				if (!mHasPerformedLongPress) {
					result = performClick();
				}
			}
			break;
		}
		}
		return result;
	}

	/**
	 * Default implementation of {@link KeyEvent.Callback#onKeyMultiple(int, int, KeyEvent)
	 * KeyEvent.Callback.onKeyMultiple()}: always returns false (doesn't handle
	 * the event).
	 *
	 * @param keyCode     A key code that represents the button pressed, from
	 *                    {@link android.view.KeyEvent}.
	 * @param repeatCount The number of times the action was made.
	 * @param event       The KeyEvent object that defines the button action.
	 */
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return false;
	}

	/**
	 * Called when an unhandled key shortcut event occurs.
	 *
	 * @param keyCode The value in event.getKeyCode().
	 * @param event Description of the key event.
	 * @return If you handled the event, return true. If you want to allow the
	 *         event to be handled by the next receiver, return false.
	 */
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		return false;
	}

	/**
	 * Check whether the called view is a text editor, in which case it
	 * would make sense to automatically display a soft input window for
	 * it.  Subclasses should override this if they implement
	 * {@link #onCreateInputConnection(EditorInfo)} to return true if
	 * a call on that method would return a non-null InputConnection, and
	 * they are really a first-class editor that the user would normally
	 * start typing on when the go into a window containing your view.
	 *
	 * <p>The default implementation always returns false.  This does
	 * <em>not</em> mean that its {@link #onCreateInputConnection(EditorInfo)}
	 * will not be called or the user can not otherwise perform edits on your
	 * view; it is just a hint to the system that this is not the primary
	 * purpose of this view.
	 *
	 * @return Returns true if this view is a text editor, else false.
	 */
	public boolean onCheckIsTextEditor() {
		return false;
	}

	/**
	 * Called by the {@link android.view.inputmethod.InputMethodManager}
	 * when a view who is not the current
	 * input connection target is trying to make a call on the manager.  The
	 * default implementation returns false; you can override this to return
	 * true for certain views if you are performing InputConnection proxying
	 * to them.
	 * @param view The View that is making the InputMethodManager call.
	 * @return Return true to allow the call, false to reject.
	 */
	public boolean checkInputConnectionProxy(View view) {
		return false;
	}

	/**
	 * Removes the specified Runnable from the message queue.
	 *
	 * @param action The Runnable to remove from the message handling queue
	 *
	 * @return true if this view could ask the Handler to remove the Runnable,
	 *         false otherwise. When the returned value is true, the Runnable
	 *         may or may not have been actually removed from the message queue
	 *         (for instance, if the Runnable was not in the queue already.)
	 */
	public boolean removeCallbacks(Runnable action) {
		Handler handler;
		if (mAttachInfo != null) {
			handler = mAttachInfo.mHandler;
		} else {
			// Assume that post will succeed later
			ViewRoot.getRunQueue().removeCallbacks(action);
			return true;
		}

		handler.removeCallbacks(action);
		return true;
	}

	/**
	 * Set flags controlling behavior of this view.
	 *
	 * @param flags Constant indicating the value which should be set
	 * @param mask Constant indicating the bit range that should be changed
	 */
	void setFlags(int flags, int mask) {
		int old = mViewFlags;
		mViewFlags = (mViewFlags & ~mask) | (flags & mask);

		int changed = mViewFlags ^ old;
		if (changed == 0) {
			return;
		}
		int privateFlags = mPrivateFlags;

		/* Check if the FOCUSABLE bit has changed */
		if (((changed & FOCUSABLE_MASK) != 0)
				&& ((privateFlags & HAS_BOUNDS) != 0)) {
			if (((old & FOCUSABLE_MASK) == FOCUSABLE)
					&& ((privateFlags & FOCUSED) != 0)) {
				/* Give up focus if we are no longer focusable */
				clearFocus();
			} else if (((old & FOCUSABLE_MASK) == NOT_FOCUSABLE)
					&& ((privateFlags & FOCUSED) == 0)) {
				/*
				 * Tell the view system that we are now available to take focus
				 * if no one else already has it.
				 */
				if (mParent != null)
					mParent.focusableViewAvailable(this);
			}
		}

		if ((flags & VISIBILITY_MASK) == VISIBLE) {
			if ((changed & VISIBILITY_MASK) != 0) {
				/*
				 * If this view is becoming visible, set the DRAWN flag so that
				 * the next invalidate() will not be skipped.
				 */
				mPrivateFlags |= DRAWN;

				needGlobalAttributesUpdate(true);

				// a view becoming visible is worth notifying the parent
				// about in case nothing has focus.  even if this specific view
				// isn't focusable, it may contain something that is, so let
				// the root view try to give this focus if nothing else does.
				if ((mParent != null) && (mBottom > mTop) && (mRight > mLeft)) {
					mParent.focusableViewAvailable(this);
				}
			}
		}

		/* Check if the GONE bit has changed */
		if ((changed & GONE) != 0) {
			needGlobalAttributesUpdate(false);
			requestLayout();
			invalidate();

			if (((mViewFlags & VISIBILITY_MASK) == GONE)) {
				if (hasFocus())
					clearFocus();
			}
			if (mAttachInfo != null) {
				mAttachInfo.mViewVisibilityChanged = true;
			}
		}

		/* Check if the VISIBLE bit has changed */
		if ((changed & INVISIBLE) != 0) {
			needGlobalAttributesUpdate(false);
			invalidate();

			if (((mViewFlags & VISIBILITY_MASK) == INVISIBLE) && hasFocus()) {
				// root view becoming invisible shouldn't clear focus
				if (getRootView() != this) {
					clearFocus();
				}
			}
			if (mAttachInfo != null) {
				mAttachInfo.mViewVisibilityChanged = true;
			}
		}

		if ((changed & VISIBILITY_MASK) != 0) {
		}

		if ((changed & WILL_NOT_CACHE_DRAWING) != 0) {
		}

		if ((changed & DRAWING_CACHE_ENABLED) != 0) {
			mPrivateFlags &= ~DRAWING_CACHE_VALID;
		}

		if ((changed & DRAWING_CACHE_QUALITY_MASK) != 0) {
			//destroyDrawingCache();
			mPrivateFlags &= ~DRAWING_CACHE_VALID;
		}

        if ((changed & DRAW_MASK) != 0) {
            if ((mViewFlags & WILL_NOT_DRAW) != 0) {
                if (mBGDrawable != null) {
                    mPrivateFlags &= ~SKIP_DRAW;
                    mPrivateFlags |= ONLY_DRAWS_BACKGROUND;
                } else {
                    mPrivateFlags |= SKIP_DRAW;
                }
            } else {
                mPrivateFlags &= ~SKIP_DRAW;
            }
            requestLayout();
            invalidate();
        }

		if ((changed & KEEP_SCREEN_ON) != 0) {
			if (mParent != null) {
				mParent.recomputeViewAttributes(this);
			}
		}
	}

	/**
	 * Change the view's z order in the tree, so it's on top of other sibling
	 * views
	 */
	public void bringToFront() {
		if (mParent != null) {
			mParent.bringChildToFront(this);
		}
	}

	/**
	 * This is called in response to an internal scroll in this view (i.e., the
	 * view scrolled its own contents). This is typically as a result of
	 * {@link #scrollBy(int, int)} or {@link #scrollTo(int, int)} having been
	 * called.
	 *
	 * @param l Current horizontal scroll origin.
	 * @param t Current vertical scroll origin.
	 * @param oldl Previous horizontal scroll origin.
	 * @param oldt Previous vertical scroll origin.
	 */
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
	    mBackgroundSizeChanged = true;
		final AttachInfo ai = mAttachInfo;
		if (ai != null) {
			ai.mViewScrollChanged = true;
		}
	}

	/**
	 * This is called during layout when the size of this view has changed. If
	 * you were just added to the view hierarchy, you're called with the old
	 * values of 0.
	 *
	 * @param w Current width of this view.
	 * @param h Current height of this view.
	 * @param oldw Old width of this view.
	 * @param oldh Old height of this view.
	 */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	}

	/**
	 * Gets the parent of this view. Note that the parent is a
	 * ViewParent and not necessarily a View.
	 *
	 * @return Parent of this view.
	 */
	public final ViewParent getParent() {
		return mParent;
	}

	/**
	 * Return the scrolled left position of this view. This is the left edge of
	 * the displayed part of your view. You do not need to draw any pixels
	 * farther left, since those are outside of the frame of your view on
	 * screen.
	 *
	 * @return The left edge of the displayed part of your view, in pixels.
	 */
	public final int getScrollX() {
		return mScrollX;
	}

	/**
	 * Return the scrolled top position of this view. This is the top edge of
	 * the displayed part of your view. You do not need to draw any pixels above
	 * it, since those are outside of the frame of your view on screen.
	 *
	 * @return The top edge of the displayed part of your view, in pixels.
	 */
	public final int getScrollY() {
		return mScrollY;
	}

	/**
	 * Return the width of the your view.
	 *
	 * @return The width of your view, in pixels.
	 */
	public final int getWidth() {
		return mRight - mLeft;
	}

	/**
	 * Return the height of your view.
	 *
	 * @return The height of your view, in pixels.
	 */
	public final int getHeight() {
		return mBottom - mTop;
	}

	/**
	 * Return the visible drawing bounds of your view. Fills in the output
	 * rectangle with the values from getScrollX(), getScrollY(),
	 * getWidth(), and getHeight().
	 *
	 * @param outRect The (scrolled) drawing bounds of the view.
	 */
	public void getDrawingRect(Rect outRect) {
		outRect.left = mScrollX;
		outRect.top = mScrollY;
		outRect.right = mScrollX + (mRight - mLeft);
		outRect.bottom = mScrollY + (mBottom - mTop);
	}

	/**
	 * The width of this view as measured in the most recent call to measure().
	 * This should be used during measurement and layout calculations only. Use
	 * {@link #getWidth()} to see how wide a view is after layout.
	 *
	 * @return The measured width of this view.
	 */
	public final int getMeasuredWidth() {
		return mMeasuredWidth;
	}

	/**
	 * The height of this view as measured in the most recent call to measure().
	 * This should be used during measurement and layout calculations only. Use
	 * {@link #getHeight()} to see how tall a view is after layout.
	 *
	 * @return The measured height of this view.
	 */
	public final int getMeasuredHeight() {
		return mMeasuredHeight;
	}

	/**
	 * Top position of this view relative to its parent.
	 *
	 * @return The top of this view, in pixels.
	 */

	public final int getTop() {
		return mTop;
	}
	
    /**
     * Top absolute position of this view.
     * 
     * @return The top of this view, in pixels.
     */
    public final int getAbsoluteTop() {
        int absoluteTop = this.mTop - this.mScrollY;
        View parent = this;

        while (parent.mParent != null && parent.mParent instanceof View) {
            parent = (View) parent.mParent;
            absoluteTop += (parent.mTop - parent.mScrollY);
        }

        return absoluteTop;
    }

	/**
	 * Bottom position of this view relative to its root parent.
	 *
	 * @return The bottom of this view, in pixels.
	 */
	public final int getBottom() {
		return mBottom;
	}

	/**
	 * Left position of this view relative to its parent.
	 *
	 * @return The left edge of this view, in pixels.
	 */

	public final int getLeft() {
		return mLeft;
	}
	
    /**
     * Left absolute position of this view relative to its root parent.
     * 
     * @return The left edge of this view, in pixels.
     */

    public final int getAbsoluteLeft() {
        int absoluteLeft = this.mLeft - this.mScrollX;
        View parent = this;

        while (parent.mParent != null && parent.mParent instanceof View) {
            parent = (View) parent.mParent;
            absoluteLeft += (parent.mLeft - parent.mScrollX);
        }

        return absoluteLeft;
    }

	/**
	 * Right position of this view relative to its parent.
	 *
	 * @return The right edge of this view, in pixels.
	 */

	public final int getRight() {
		return mRight;
	}

	/**
	 * Hit rectangle in parent's coordinates
	 *
	 * @param outRect The hit rectangle of the view.
	 */
	public void getHitRect(Rect outRect) {
		outRect.set(mLeft, mTop, mRight, mBottom);
	}

	/**
	 * When a view has focus and the user navigates away from it, the next view is searched for
	 * starting from the rectangle filled in by this method.
	 *
	 * By default, the rectange is the {@link #getDrawingRect})of the view.  However, if your
	 * view maintains some idea of internal selection, such as a cursor, or a selected row
	 * or column, you should override this method and fill in a more specific rectangle.
	 *
	 * @param r The rectangle to fill in, in this view's coordinates.
	 */
	public void getFocusedRect(Rect r) {
		getDrawingRect(r);
	}

	/**
	 * If some part of this view is not clipped by any of its parents, then
	 * return that area in r in global (root) coordinates. To convert r to local
	 * coordinates, offset it by -globalOffset (e.g. r.offset(-globalOffset.x,
	 * -globalOffset.y)) If the view is completely clipped or translated out,
	 * return false.
	 *
	 * @param r If true is returned, r holds the global coordinates of the
	 *        visible portion of this view.
	 * @param globalOffset If true is returned, globalOffset holds the dx,dy
	 *        between this view and its root. globalOffet may be null.
	 * @return true if r is non-empty (i.e. part of the view is visible at the
	 *         root level.
	 */
	public boolean getGlobalVisibleRect(Rect r, Point globalOffset) {
		int width = mRight - mLeft;
		int height = mBottom - mTop;
		if (width > 0 && height > 0) {
			r.set(0, 0, width, height);
			if (globalOffset != null) {
				globalOffset.set(-mScrollX, -mScrollY);
			}
			return mParent == null
					|| mParent.getChildVisibleRect(this, r, globalOffset);
		}
		return false;
	}

	public final boolean getGlobalVisibleRect(Rect r) {
		return getGlobalVisibleRect(r, null);
	}

	public final boolean getLocalVisibleRect(Rect r) {
		Point offset = new Point();
		if (getGlobalVisibleRect(r, offset)) {
			r.offset(-offset.x, -offset.y); // make r local
			return true;
		}
		return false;
	}

	/**
	 * Offset this view's vertical location by the specified number of pixels.
	 *
	 * @param offset the number of pixels to offset the view by
	 */
	public void offsetTopAndBottom(int offset) {
		mTop += offset;
		mBottom += offset;
	}

	/**
	 * Offset this view's horizontal location by the specified amount of pixels.
	 *
	 * @param offset the numer of pixels to offset the view by
	 */
	public void offsetLeftAndRight(int offset) {
		mLeft += offset;
		mRight += offset;
	}

	/**
	 * Get the LayoutParams associated with this view. All views should have
	 * layout parameters. These supply parameters to the <i>parent</i> of this
	 * view specifying how it should be arranged. There are many subclasses of
	 * ViewGroup.LayoutParams, and these correspond to the different subclasses
	 * of ViewGroup that are responsible for arranging their children.
	 * @return The LayoutParams associated with this view
	 */

	public ViewGroup.LayoutParams getLayoutParams() {
		return mLayoutParams;
	}

	/**
	 * Set the layout parameters associated with this view. These supply
	 * parameters to the <i>parent</i> of this view specifying how it should be
	 * arranged. There are many subclasses of ViewGroup.LayoutParams, and these
	 * correspond to the different subclasses of ViewGroup that are responsible
	 * for arranging their children.
	 *
	 * @param params the layout parameters for this view
	 */
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		if (params == null) {
			throw new NullPointerException("params == null");
		}
		mLayoutParams = params;
		requestLayout();
	}

    /**
     * <p>Trigger the scrollbars to draw. When invoked this method starts an
     * animation to fade the scrollbars out after a default delay. If a subclass
     * provides animated scrolling, the start delay should equal the duration
     * of the scrolling animation.</p>
     *
     * <p>The animation starts only if at least one of the scrollbars is
     * enabled, as specified by {@link #isHorizontalScrollBarEnabled()} and
     * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
     * this method returns true, and false otherwise. If the animation is
     * started, this method calls {@link #invalidate()}; in that case the
     * caller should not call {@link #invalidate()}.</p>
     *
     * <p>This method should be invoked every time a subclass directly updates
     * the scroll parameters.</p>
     *
     * <p>This method is automatically invoked by {@link #scrollBy(int, int)}
     * and {@link #scrollTo(int, int)}.</p>
     *
     * @return true if the animation is played, false otherwise
     *
     * @see #awakenScrollBars(int)
     * @see #scrollBy(int, int)
     * @see #scrollTo(int, int)
     * @see #isHorizontalScrollBarEnabled()
     * @see #isVerticalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    protected boolean awakenScrollBars() {
        return mScrollCache != null &&
                awakenScrollBars(mScrollCache.scrollBarDefaultDelayBeforeFade, true);
    }

	/**
	 * <p>
	 * Trigger the scrollbars to draw. When invoked this method starts an
	 * animation to fade the scrollbars out after a fixed delay. If a subclass
	 * provides animated scrolling, the start delay should equal the duration of
	 * the scrolling animation.
	 * </p>
	 *
	 * <p>
	 * The animation starts only if at least one of the scrollbars is enabled,
	 * as specified by {@link #isHorizontalScrollBarEnabled()} and
	 * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
	 * this method returns true, and false otherwise. If the animation is
	 * started, this method calls {@link #invalidate()}; in that case the caller
	 * should not call {@link #invalidate()}.
	 * </p>
	 *
	 * <p>
	 * This method should be invoked everytime a subclass directly updates the
	 * scroll parameters.
	 * </p>
	 *
	 * @param startDelay the delay, in milliseconds, after which the animation
	 *        should start; when the delay is 0, the animation starts
	 *        immediately
	 * @return true if the animation is played, false otherwise
	 *
	 * @see #scrollBy(int, int)
	 * @see #scrollTo(int, int)
	 * @see #isHorizontalScrollBarEnabled()
	 * @see #isVerticalScrollBarEnabled()
	 * @see #setHorizontalScrollBarEnabled(boolean)
	 * @see #setVerticalScrollBarEnabled(boolean)
	 */
	protected boolean awakenScrollBars(int startDelay) {
		return awakenScrollBars(startDelay, true);
	}

	/**
	 * <p>
	 * Trigger the scrollbars to draw. When invoked this method starts an
	 * animation to fade the scrollbars out after a fixed delay. If a subclass
	 * provides animated scrolling, the start delay should equal the duration of
	 * the scrolling animation.
	 * </p>
	 *
	 * <p>
	 * The animation starts only if at least one of the scrollbars is enabled,
	 * as specified by {@link #isHorizontalScrollBarEnabled()} and
	 * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
	 * this method returns true, and false otherwise. If the animation is
	 * started, this method calls {@link #invalidate()} if the invalidate parameter
	 * is set to true; in that case the caller
	 * should not call {@link #invalidate()}.
	 * </p>
	 *
	 * <p>
	 * This method should be invoked everytime a subclass directly updates the
	 * scroll parameters.
	 * </p>
	 *
	 * @param startDelay the delay, in milliseconds, after which the animation
	 *        should start; when the delay is 0, the animation starts
	 *        immediately
	 *
	 * @param invalidate Wheter this method should call invalidate
	 *
	 * @return true if the animation is played, false otherwise
	 *
	 * @see #scrollBy(int, int)
	 * @see #scrollTo(int, int)
	 * @see #isHorizontalScrollBarEnabled()
	 * @see #isVerticalScrollBarEnabled()
	 * @see #setHorizontalScrollBarEnabled(boolean)
	 * @see #setVerticalScrollBarEnabled(boolean)
	 */
	protected boolean awakenScrollBars(int startDelay, boolean invalidate) {
		return false;
	}

	/**
	 * Mark the the area defined by dirty as needing to be drawn. If the view is
	 * visible, {@link #onDraw} will be called at some point in the future.
	 * This must be called from a UI thread. To call from a non-UI thread, call
	 * {@link #postInvalidate()}.
	 *
	 * WARNING: This method is destructive to dirty.
	 * @param dirty the rectangle representing the bounds of the dirty region
	 */
	public void invalidate(Rect dirty) {
		if ((mPrivateFlags & (DRAWN | HAS_BOUNDS)) == (DRAWN | HAS_BOUNDS)) {
			mPrivateFlags &= ~DRAWING_CACHE_VALID;
			final ViewParent p = mParent;
			final AttachInfo ai = mAttachInfo;
			if (p != null && ai != null) {
				final int scrollX = mScrollX;
				final int scrollY = mScrollY;
				final Rect r = ai.mTmpInvalRect;
				r.set(dirty.left - scrollX, dirty.top - scrollY, dirty.right
						- scrollX, dirty.bottom - scrollY);
				mParent.invalidateChild(this, r);
			}
		}
	}

	/**
	 * Mark the the area defined by the rect (l,t,r,b) as needing to be drawn.
	 * The coordinates of the dirty rect are relative to the view.
	 * If the view is visible, {@link #onDraw} will be called at some point
	 * in the future. This must be called from a UI thread. To call
	 * from a non-UI thread, call {@link #postInvalidate()}.
	 * @param l the left position of the dirty region
	 * @param t the top position of the dirty region
	 * @param r the right position of the dirty region
	 * @param b the bottom position of the dirty region
	 */
	public void invalidate(int l, int t, int r, int b) {

		if ((mPrivateFlags & (DRAWN | HAS_BOUNDS)) == (DRAWN | HAS_BOUNDS)) {
			mPrivateFlags &= ~DRAWING_CACHE_VALID;
			final ViewParent p = mParent;
			final AttachInfo ai = mAttachInfo;
			if (p != null && ai != null && l < r && t < b) {
				final int scrollX = mScrollX;
				final int scrollY = mScrollY;
				final Rect tmpr = ai.mTmpInvalRect;
				tmpr.set(l - scrollX, t - scrollY, r - scrollX, b - scrollY);
				p.invalidateChild(this, tmpr);
			}
		}
	}

	/**
	 * Invalidate the whole view. If the view is visible, {@link #onDraw} will
	 * be called at some point in the future. This must be called from a
	 * UI thread. To call from a non-UI thread, call {@link #postInvalidate()}.
	 */
	public void invalidate() {
		//MayLoon TODO: this is the bruteforce invalidate
        long start = System.currentTimeMillis();
        Log.d(TAG, "View invalidate at time:" + start);
		if ((mPrivateFlags & (DRAWN | HAS_BOUNDS)) == (DRAWN | HAS_BOUNDS)) {
			mPrivateFlags &= ~DRAWN & ~DRAWING_CACHE_VALID;
			final ViewParent p = mParent;
			final AttachInfo ai = mAttachInfo;
			if (p != null && ai != null) {
				final Rect r = ai.mTmpInvalRect;
				r.set(0, 0, mRight - mLeft, mBottom - mTop);
				// Don't call invalidate -- we don't want to internally scroll
				// our own bounds
				//System.out.println(r.toString());
				p.invalidateChild(this, r);
			}
		}
	}

	/**
	 * Indicates whether this View is opaque. An opaque View guarantees that it will
	 * draw all the pixels overlapping its bounds using a fully opaque color.
	 *
	 * Subclasses of View should override this method whenever possible to indicate
	 * whether an instance is opaque. Opaque Views are treated in a special way by
	 * the View hierarchy, possibly allowing it to perform optimizations during
	 * invalidate/draw passes.
	 *
	 * @return True if this View is guaranteed to be fully opaque, false otherwise.
	 */

	public boolean isOpaque() {
		return (mPrivateFlags & OPAQUE_MASK) == OPAQUE_MASK;
	}

    private void computeOpaqueFlags() {
        // Opaque if:
        // - Has a background
        // - Background is opaque
        // - Doesn't have scrollbars or scrollbars are inside overlay

        if (mBGDrawable != null && mBGDrawable.getOpacity() == PixelFormat.OPAQUE) {
            mPrivateFlags |= OPAQUE_BACKGROUND;
        } else {
            mPrivateFlags &= ~OPAQUE_BACKGROUND;
        }

        final int flags = mViewFlags;
        if (((flags & SCROLLBARS_VERTICAL) == 0 && (flags & SCROLLBARS_HORIZONTAL) == 0)
                || (flags & SCROLLBARS_STYLE_MASK) == SCROLLBARS_INSIDE_OVERLAY) {
            mPrivateFlags |= OPAQUE_SCROLLBARS;
        } else {
            mPrivateFlags &= ~OPAQUE_SCROLLBARS;
        }
    }

	/**
	 * @hide
	 */
	protected boolean hasOpaqueScrollbars() {
		return (mPrivateFlags & OPAQUE_SCROLLBARS) == OPAQUE_SCROLLBARS;
	}

	/**
	 * @return A handler associated with the thread running the View. This
	 * handler can be used to pump events in the UI events queue.
	 */
	public Handler getHandler() {
		if (mAttachInfo != null) {
			return mAttachInfo.mHandler;
		}
		return null;
	}

	/**
	 * Called by a parent to request that a child update its values for mScrollX
	 * and mScrollY if necessary. This will typically be done if the child is
	 * animating a scroll using a {@link android.widget.Scroller Scroller}
	 * object.
	 */
	public void computeScroll() {
	}

	/**
	 * <p>Indicate whether the horizontal edges are faded when the view is
	 * scrolled horizontally.</p>
	 *
	 * @return true if the horizontal edges should are faded on scroll, false
	 *         otherwise
	 *
	 * @see #setHorizontalFadingEdgeEnabled(boolean)
	 * @attr ref android.R.styleable#View_fadingEdge
	 */
	public boolean isHorizontalFadingEdgeEnabled() {
		return (mViewFlags & FADING_EDGE_HORIZONTAL) == FADING_EDGE_HORIZONTAL;
	}

	/**
	 * <p>Define whether the horizontal edges should be faded when this view
	 * is scrolled horizontally.</p>
	 *
	 * @param horizontalFadingEdgeEnabled true if the horizontal edges should
	 *                                    be faded when the view is scrolled
	 *                                    horizontally
	 *
	 * @see #isHorizontalFadingEdgeEnabled()
	 * @attr ref android.R.styleable#View_fadingEdge
	 */
	public void setHorizontalFadingEdgeEnabled(
			boolean horizontalFadingEdgeEnabled) {
		if (isHorizontalFadingEdgeEnabled() != horizontalFadingEdgeEnabled) {
			if (horizontalFadingEdgeEnabled) {
				//initScrollCache();
			}

			mViewFlags ^= FADING_EDGE_HORIZONTAL;
		}
	}

	/**
	 * <p>Indicate whether the vertical edges are faded when the view is
	 * scrolled horizontally.</p>
	 *
	 * @return true if the vertical edges should are faded on scroll, false
	 *         otherwise
	 *
	 * @see #setVerticalFadingEdgeEnabled(boolean)
	 * @attr ref android.R.styleable#View_fadingEdge
	 */
	public boolean isVerticalFadingEdgeEnabled() {
		return (mViewFlags & FADING_EDGE_VERTICAL) == FADING_EDGE_VERTICAL;
	}

	/**
	 * <p>Define whether the vertical edges should be faded when this view
	 * is scrolled vertically.</p>
	 *
	 * @param verticalFadingEdgeEnabled true if the vertical edges should
	 *                                  be faded when the view is scrolled
	 *                                  vertically
	 *
	 * @see #isVerticalFadingEdgeEnabled()
	 * @attr ref android.R.styleable#View_fadingEdge
	 */
	public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled) {
		if (isVerticalFadingEdgeEnabled() != verticalFadingEdgeEnabled) {
			if (verticalFadingEdgeEnabled) {
				//initScrollCache();
			}

			mViewFlags ^= FADING_EDGE_VERTICAL;
		}
	}

	/**
	 * Returns the strength, or intensity, of the top faded edge. The strength is
	 * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
	 * returns 0.0 or 1.0 but no value in between.
	 *
	 * Subclasses should override this method to provide a smoother fade transition
	 * when scrolling occurs.
	 *
	 * @return the intensity of the top fade as a float between 0.0f and 1.0f
	 */
	protected float getTopFadingEdgeStrength() {
		return computeVerticalScrollOffset() > 0 ? 1.0f : 0.0f;
	}

	/**
	 * Returns the strength, or intensity, of the bottom faded edge. The strength is
	 * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
	 * returns 0.0 or 1.0 but no value in between.
	 *
	 * Subclasses should override this method to provide a smoother fade transition
	 * when scrolling occurs.
	 *
	 * @return the intensity of the bottom fade as a float between 0.0f and 1.0f
	 */
	protected float getBottomFadingEdgeStrength() {
		return computeVerticalScrollOffset() + computeVerticalScrollExtent() < computeVerticalScrollRange() ? 1.0f
				: 0.0f;
	}

	/**
	 * Returns the strength, or intensity, of the left faded edge. The strength is
	 * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
	 * returns 0.0 or 1.0 but no value in between.
	 *
	 * Subclasses should override this method to provide a smoother fade transition
	 * when scrolling occurs.
	 *
	 * @return the intensity of the left fade as a float between 0.0f and 1.0f
	 */
	protected float getLeftFadingEdgeStrength() {
		return computeHorizontalScrollOffset() > 0 ? 1.0f : 0.0f;
	}

	/**
	 * Returns the strength, or intensity, of the right faded edge. The strength is
	 * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
	 * returns 0.0 or 1.0 but no value in between.
	 *
	 * Subclasses should override this method to provide a smoother fade transition
	 * when scrolling occurs.
	 *
	 * @return the intensity of the right fade as a float between 0.0f and 1.0f
	 */
	protected float getRightFadingEdgeStrength() {
		return computeHorizontalScrollOffset()
				+ computeHorizontalScrollExtent() < computeHorizontalScrollRange() ? 1.0f
				: 0.0f;
	}
	
	 public int getVerticalScrollbarWidth() {
	        ScrollabilityCache cache = mScrollCache;
	        if (cache != null) {
	            ScrollBarDrawable scrollBar = cache.scrollBar;
	            if (scrollBar != null) {
	                int size = scrollBar.getSize(true);
	                if (size <= 0) {
	                    size = cache.scrollBarSize;
	                }
	                return size;
	            }
	            return 0;
	        }
	        return 0;
	    }

	    /**
	     * Returns the height of the horizontal scrollbar.
	     *
	     * @return The height in pixels of the horizontal scrollbar or 0 if
	     *         there is no horizontal scrollbar.
	     */
	    protected int getHorizontalScrollbarHeight() {
	        ScrollabilityCache cache = mScrollCache;
	        if (cache != null) {
	            ScrollBarDrawable scrollBar = cache.scrollBar;
	            if (scrollBar != null) {
	                int size = scrollBar.getSize(false);
	                if (size <= 0) {
	                    size = cache.scrollBarSize;
	                }
	                return size;
	            }
	            return 0;
	        }
	        return 0;
	    }

	    /**
	     * <p>
	     * Initializes the scrollbars from a given set of styled attributes. This
	     * method should be called by subclasses that need scrollbars and when an
	     * instance of these subclasses is created programmatically rather than
	     * being inflated from XML. This method is automatically called when the XML
	     * is inflated.
	     * </p>
	     *
	     * @param a the styled attributes set to initialize the scrollbars from
	     */
	    protected void initializeScrollbars(TypedArray a) {
	        initScrollCache();

	        final ScrollabilityCache scrollabilityCache = mScrollCache;
	        
	        if (scrollabilityCache.scrollBar == null) {
	            scrollabilityCache.scrollBar = new ScrollBarDrawable();
	        }
	        
        // MayLoon: In MayLoon, we current only support no fading because fading
        // effect is complex and will need much render computation, may not be
        // smooth.
        final boolean fadeScrollbars = false/*a.getBoolean(R.styleable.View_fadeScrollbars, true)*/;

	        if (!fadeScrollbars) {
	            scrollabilityCache.state = ScrollabilityCache.ON;
	        }
	        scrollabilityCache.fadeScrollBars = fadeScrollbars;
	        
	        
	        scrollabilityCache.scrollBarFadeDuration = a.getInt(
	                R.styleable.View_scrollbarFadeDuration, ViewConfiguration
	                        .getScrollBarFadeDuration());
	        scrollabilityCache.scrollBarDefaultDelayBeforeFade = a.getInt(
	                R.styleable.View_scrollbarDefaultDelayBeforeFade,
	                ViewConfiguration.getScrollDefaultDelay());

	                
	        scrollabilityCache.scrollBarSize = a.getDimensionPixelSize(
	                com.android.internal.R.styleable.View_scrollbarSize,
	                ViewConfiguration.get(mContext).getScaledScrollBarSize());

	        Drawable track = a.getDrawable(R.styleable.View_scrollbarTrackHorizontal);
	        scrollabilityCache.scrollBar.setHorizontalTrackDrawable(track);

	        Drawable thumb = a.getDrawable(R.styleable.View_scrollbarThumbHorizontal);
	        if (thumb != null) {
	            scrollabilityCache.scrollBar.setHorizontalThumbDrawable(thumb);
	        }

	        boolean alwaysDraw = a.getBoolean(R.styleable.View_scrollbarAlwaysDrawHorizontalTrack,
	                false);
	        if (alwaysDraw) {
	            scrollabilityCache.scrollBar.setAlwaysDrawHorizontalTrack(true);
	        }

	        track = a.getDrawable(R.styleable.View_scrollbarTrackVertical);
	        scrollabilityCache.scrollBar.setVerticalTrackDrawable(track);

	        thumb = a.getDrawable(R.styleable.View_scrollbarThumbVertical);
	        if (thumb != null) {
	            scrollabilityCache.scrollBar.setVerticalThumbDrawable(thumb);
	        }

	        alwaysDraw = a.getBoolean(R.styleable.View_scrollbarAlwaysDrawVerticalTrack,
	                false);
	        if (alwaysDraw) {
	            scrollabilityCache.scrollBar.setAlwaysDrawVerticalTrack(true);
	        }

	        // Re-apply user/background padding so that scrollbar(s) get added
	        recomputePadding();
	    }

	    /**
	     * <p>
	     * Initalizes the scrollability cache if necessary.
	     * </p>
	     */
	    private void initScrollCache() {
	        if (mScrollCache == null) {
	            mScrollCache = new ScrollabilityCache(ViewConfiguration.get(mContext), this);
	        }
	    }

	/**
	 * <p>Indicate whether the horizontal scrollbar should be drawn or not. The
	 * scrollbar is not drawn by default.</p>
	 *
	 * @return true if the horizontal scrollbar should be painted, false
	 *         otherwise
	 *
	 * @see #setHorizontalScrollBarEnabled(boolean)
	 */
	public boolean isHorizontalScrollBarEnabled() {
		return (mViewFlags & SCROLLBARS_HORIZONTAL) == SCROLLBARS_HORIZONTAL;
	}

	/**
	 * <p>Define whether the horizontal scrollbar should be drawn or not. The
	 * scrollbar is not drawn by default.</p>
	 *
	 * @param horizontalScrollBarEnabled true if the horizontal scrollbar should
	 *                                   be painted
	 *
	 * @see #isHorizontalScrollBarEnabled()
	 */
	public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
		if (isHorizontalScrollBarEnabled() != horizontalScrollBarEnabled) {
			mViewFlags ^= SCROLLBARS_HORIZONTAL;
			computeOpaqueFlags();
			recomputePadding();
		}
	}

	/**
	 * <p>Indicate whether the vertical scrollbar should be drawn or not. The
	 * scrollbar is not drawn by default.</p>
	 *
	 * @return true if the vertical scrollbar should be painted, false
	 *         otherwise
	 *
	 * @see #setVerticalScrollBarEnabled(boolean)
	 */
	public boolean isVerticalScrollBarEnabled() {
		return (mViewFlags & SCROLLBARS_VERTICAL) == SCROLLBARS_VERTICAL;
	}

	/**
	 * <p>Define whether the vertical scrollbar should be drawn or not. The
	 * scrollbar is not drawn by default.</p>
	 *
	 * @param verticalScrollBarEnabled true if the vertical scrollbar should
	 *                                 be painted
	 *
	 * @see #isVerticalScrollBarEnabled()
	 */
	public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
		if (isVerticalScrollBarEnabled() != verticalScrollBarEnabled) {
			mViewFlags ^= SCROLLBARS_VERTICAL;
			computeOpaqueFlags();
			recomputePadding();
		}
	}

	private void recomputePadding() {
		setPadding(mPaddingLeft, mPaddingTop, mUserPaddingRight,
				mUserPaddingBottom);
	}

	/**
	 * <p>Specify the style of the scrollbars. The scrollbars can be overlaid or
	 * inset. When inset, they add to the padding of the view. And the scrollbars
	 * can be drawn inside the padding area or on the edge of the view. For example,
	 * if a view has a background drawable and you want to draw the scrollbars
	 * inside the padding specified by the drawable, you can use
	 * SCROLLBARS_INSIDE_OVERLAY or SCROLLBARS_INSIDE_INSET. If you want them to
	 * appear at the edge of the view, ignoring the padding, then you can use
	 * SCROLLBARS_OUTSIDE_OVERLAY or SCROLLBARS_OUTSIDE_INSET.</p>
	 * @param style the style of the scrollbars. Should be one of
	 * SCROLLBARS_INSIDE_OVERLAY, SCROLLBARS_INSIDE_INSET,
	 * SCROLLBARS_OUTSIDE_OVERLAY or SCROLLBARS_OUTSIDE_INSET.
	 * @see #SCROLLBARS_INSIDE_OVERLAY
	 * @see #SCROLLBARS_INSIDE_INSET
	 * @see #SCROLLBARS_OUTSIDE_OVERLAY
	 * @see #SCROLLBARS_OUTSIDE_INSET
	 */
	public void setScrollBarStyle(int style) {
		if (style != (mViewFlags & SCROLLBARS_STYLE_MASK)) {
			mViewFlags = (mViewFlags & ~SCROLLBARS_STYLE_MASK)
					| (style & SCROLLBARS_STYLE_MASK);
			computeOpaqueFlags();
			recomputePadding();
		}
	}

	/**
	 * <p>Returns the current scrollbar style.</p>
	 * @return the current scrollbar style
	 * @see #SCROLLBARS_INSIDE_OVERLAY
	 * @see #SCROLLBARS_INSIDE_INSET
	 * @see #SCROLLBARS_OUTSIDE_OVERLAY
	 * @see #SCROLLBARS_OUTSIDE_INSET
	 */
	public int getScrollBarStyle() {
		return mViewFlags & SCROLLBARS_STYLE_MASK;
	}

	/**
	 * <p>Compute the horizontal range that the horizontal scrollbar
	 * represents.</p>
	 *
	 * <p>The range is expressed in arbitrary units that must be the same as the
	 * units used by {@link #computeHorizontalScrollExtent()} and
	 * {@link #computeHorizontalScrollOffset()}.</p>
	 *
	 * <p>The default range is the drawing width of this view.</p>
	 *
	 * @return the total horizontal range represented by the horizontal
	 *         scrollbar
	 *
	 * @see #computeHorizontalScrollExtent()
	 * @see #computeHorizontalScrollOffset()
	 * @see android.widget.ScrollBarDrawable
	 */
	protected int computeHorizontalScrollRange() {
		return getWidth();
	}

	/**
	 * <p>Compute the horizontal offset of the horizontal scrollbar's thumb
	 * within the horizontal range. This value is used to compute the position
	 * of the thumb within the scrollbar's track.</p>
	 *
	 * <p>The range is expressed in arbitrary units that must be the same as the
	 * units used by {@link #computeHorizontalScrollRange()} and
	 * {@link #computeHorizontalScrollExtent()}.</p>
	 *
	 * <p>The default offset is the scroll offset of this view.</p>
	 *
	 * @return the horizontal offset of the scrollbar's thumb
	 *
	 * @see #computeHorizontalScrollRange()
	 * @see #computeHorizontalScrollExtent()
	 * @see android.widget.ScrollBarDrawable
	 */
	protected int computeHorizontalScrollOffset() {
		return mScrollX;
	}

	/**
	 * <p>Compute the horizontal extent of the horizontal scrollbar's thumb
	 * within the horizontal range. This value is used to compute the length
	 * of the thumb within the scrollbar's track.</p>
	 *
	 * <p>The range is expressed in arbitrary units that must be the same as the
	 * units used by {@link #computeHorizontalScrollRange()} and
	 * {@link #computeHorizontalScrollOffset()}.</p>
	 *
	 * <p>The default extent is the drawing width of this view.</p>
	 *
	 * @return the horizontal extent of the scrollbar's thumb
	 *
	 * @see #computeHorizontalScrollRange()
	 * @see #computeHorizontalScrollOffset()
	 * @see android.widget.ScrollBarDrawable
	 */
	protected int computeHorizontalScrollExtent() {
		return getWidth();
	}

	/**
	 * <p>Compute the vertical range that the vertical scrollbar represents.</p>
	 *
	 * <p>The range is expressed in arbitrary units that must be the same as the
	 * units used by {@link #computeVerticalScrollExtent()} and
	 * {@link #computeVerticalScrollOffset()}.</p>
	 *
	 * @return the total vertical range represented by the vertical scrollbar
	 *
	 * <p>The default range is the drawing height of this view.</p>
	 *
	 * @see #computeVerticalScrollExtent()
	 * @see #computeVerticalScrollOffset()
	 * @see android.widget.ScrollBarDrawable
	 */
	protected int computeVerticalScrollRange() {
		return getHeight();
	}

	/**
	 * <p>Compute the vertical offset of the vertical scrollbar's thumb
	 * within the horizontal range. This value is used to compute the position
	 * of the thumb within the scrollbar's track.</p>
	 *
	 * <p>The range is expressed in arbitrary units that must be the same as the
	 * units used by {@link #computeVerticalScrollRange()} and
	 * {@link #computeVerticalScrollExtent()}.</p>
	 *
	 * <p>The default offset is the scroll offset of this view.</p>
	 *
	 * @return the vertical offset of the scrollbar's thumb
	 *
	 * @see #computeVerticalScrollRange()
	 * @see #computeVerticalScrollExtent()
	 * @see android.widget.ScrollBarDrawable
	 */
	protected int computeVerticalScrollOffset() {
		return mScrollY;
	}

	/**
	 * <p>Compute the vertical extent of the horizontal scrollbar's thumb
	 * within the vertical range. This value is used to compute the length
	 * of the thumb within the scrollbar's track.</p>
	 *
	 * <p>The range is expressed in arbitrary units that must be the same as the
	 * units used by {@link #computeVerticalScrollRange()} and
	 * {@link #computeVerticalScrollOffset()}.</p>
	 *
	 * <p>The default extent is the drawing height of this view.</p>
	 *
	 * @return the vertical extent of the scrollbar's thumb
	 *
	 * @see #computeVerticalScrollRange()
	 * @see #computeVerticalScrollOffset()
	 * @see android.widget.ScrollBarDrawable
	 */
	protected int computeVerticalScrollExtent() {
		return getHeight();
	}

	/**
	 * Override this if the vertical scrollbar needs to be hidden in a subclass, like when
	 * FastScroller is visible.
	 * @return whether to temporarily hide the vertical scrollbar
	 * @hide
	 */
	protected boolean isVerticalScrollBarHidden() {
		return false;
	}
    
    /**
     * <p>
     * Draw the horizontal scrollbar if {@link #isHorizontalScrollBarEnabled()}
     * returns true.
     * </p>
     * 
     * @param canvas the canvas on which to draw the scrollbar
     * @param scrollBar the scrollbar's drawable
     * @see #isHorizontalScrollBarEnabled()
     * @see #computeHorizontalScrollRange()
     * @see #computeHorizontalScrollExtent()
     * @see #computeHorizontalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     * @hide
     */
    protected void onDrawHorizontalScrollBar(Canvas canvas,
                                             Drawable scrollBar,
                                             int l, int t, int r, int b) {
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }

    /**
     * <p>Draw the vertical scrollbar if {@link #isVerticalScrollBarEnabled()}
     * returns true.</p>
     *
     * @param canvas the canvas on which to draw the scrollbar
     * @param scrollBar the scrollbar's drawable
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #computeVerticalScrollRange()
     * @see #computeVerticalScrollExtent()
     * @see #computeVerticalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     * @hide
     */
    protected void onDrawVerticalScrollBar(Canvas canvas,
                                           Drawable scrollBar,
                                           int l, int t, int r, int b) {
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }
    
    /**
     * <p>Request the drawing of the horizontal and the vertical scrollbar. The
     * scrollbars are painted only if they have been awakened first.</p>
     *
     * @param canvas the canvas on which to draw the scrollbars
     * 
     * @see #awakenScrollBars(int)
     */
    protected final void onDrawScrollBars(Canvas canvas) {
        // scrollbars are drawn only when the animation is running
        final ScrollabilityCache cache = mScrollCache;
        if (cache != null) {
            
            int state = cache.state;
            
            if (state == ScrollabilityCache.OFF) {
                return;
            }
            
            boolean invalidate = false;
            
            if (state == ScrollabilityCache.FADING) {
                // We're fading -- get our fade interpolation
                if (cache.interpolatorValues == null) {
                    cache.interpolatorValues = new float[1];
                }
                
                float[] values = cache.interpolatorValues;
                
                // Stops the animation if we're done
                if (cache.scrollBarInterpolator.timeToValues(values) ==
                        Interpolator.Result.FREEZE_END) {
                    cache.state = ScrollabilityCache.OFF;
                } else {
                    cache.scrollBar.setAlpha(Math.round(values[0]));
                }
                
                // This will make the scroll bars inval themselves after 
                // drawing. We only want this when we're fading so that
                // we prevent excessive redraws
                invalidate = true;
            } else {
                // We're just on -- but we may have been fading before so
                // reset alpha
                cache.scrollBar.setAlpha(255);
            }

            
            final int viewFlags = mViewFlags;

            final boolean drawHorizontalScrollBar =
                (viewFlags & SCROLLBARS_HORIZONTAL) == SCROLLBARS_HORIZONTAL;
            final boolean drawVerticalScrollBar =
                (viewFlags & SCROLLBARS_VERTICAL) == SCROLLBARS_VERTICAL
                && !isVerticalScrollBarHidden();

            if (drawVerticalScrollBar || drawHorizontalScrollBar) {
                final int width = mRight - mLeft;
                final int height = mBottom - mTop;

                final ScrollBarDrawable scrollBar = cache.scrollBar;
                int size = scrollBar.getSize(false);
                if (size <= 0) {
                    size = cache.scrollBarSize;
                }

                final int scrollX = mScrollX;
                final int scrollY = mScrollY;
                final int inside = (viewFlags & SCROLLBARS_OUTSIDE_MASK) == 0 ? ~0 : 0;

                int left, top, right, bottom;
                
                if (drawHorizontalScrollBar) {
                    scrollBar.setParameters(computeHorizontalScrollRange(),
                                            computeHorizontalScrollOffset(),
                                            computeHorizontalScrollExtent(), false);
                    final int verticalScrollBarGap = drawVerticalScrollBar ?
                            getVerticalScrollbarWidth() : 0;
                    top = scrollY + height - size - (mUserPaddingBottom & inside);                         
                    left = scrollX + (mPaddingLeft & inside);
                    right = scrollX + width - (mUserPaddingRight & inside) - verticalScrollBarGap;
                    bottom = top + size;
                    onDrawHorizontalScrollBar(canvas, scrollBar, left, top, right, bottom);
                    if (invalidate) {
                        invalidate(left, top, right, bottom);
                    }
                }

                if (drawVerticalScrollBar) {
                    scrollBar.setParameters(computeVerticalScrollRange(),
                                            computeVerticalScrollOffset(),
                                            computeVerticalScrollExtent(), true);
                    // TODO: Deal with RTL languages to position scrollbar on left
                    left = scrollX + width - size - (mUserPaddingRight & inside);
                    top = scrollY + (mPaddingTop & inside);
                    right = left + size;
                    bottom = scrollY + height - (mUserPaddingBottom & inside);
                    onDrawVerticalScrollBar(canvas, scrollBar, left, top, right, bottom);
                    if (invalidate) {
                        invalidate(left, top, right, bottom);
                    }
                }
            }
        }
    }
    
	/**
	 * Implement this to do your drawing.
	 *
	 * @param canvas the canvas on which the background will be drawn
	 */
	protected void onDraw(Canvas canvas) {
	}

	public void assignParent(ViewParent parent, boolean modifyDom) {
		if (modifyDom == true) {
			if (mParent == null) {
				mParent = parent;
			} else if (parent == null) {
				mParent = null;
				return;
			} else {
				throw new RuntimeException("view " + this.getUIElementID()
						+ " being added, but" + " it already has a parent");
			}
		}

		int parentUIID = ((View) parent).getUIElementID();
		int thisViewId = getUIElementID();
		boolean pending = false;
        if (DebugUtils.DEBUG_VIEW_IN_BROWSER) {
    		/**
    		 @j2sNative
    		 var thisView = document.getElementById(this.mUIElementID);
    		 if (null == thisView) {
    		    thisView = document.createElement("span");
    		    thisView.style.position = "absolute";
    		    thisView.id = this.mUIElementID;
    		    if (modifyDom == true) {
    		    	thisView.style.display = "block";
    		    } else {
    		    	thisView.style.display = "none";
    		    }
                thisView.style.background = "transparent";
    
    		    var thisParent = document.getElementById(parentUIID);
    		    if(!thisParent){
    		    	// Assign it to the top element, temporarily
    				thisParent = document.getElementById(0);
    				pending = true;
    		    }
    		    thisParent.appendChild(thisView);
    
    		} else {
    			if (modifyDom == true) {
    		    	thisView.style.display = "block";
    		    } else {
    		    	thisView.style.display = "none";
    		    }
    		}
    		*/
    		{
    		}
		}

		if (pending){
			if(parent instanceof ViewGroup){
				ViewGroup group = (ViewGroup) parent;
				group.addPendingChild(this);
			}
		} else if (this instanceof ViewGroup){
			ViewGroup group = (ViewGroup) this;
			group.fixPendingChildren();
		}
	}

	public void fixParent(ViewParent parent, boolean modifyDom) {

		mParent = parent;

		if (DebugUtils.DEBUG_VIEW_IN_BROWSER) {
    		int parentUIID = ((View) parent).getUIElementID();
    		int thisViewId = getUIElementID();
    
    		/**
    		 @j2sNative
    			var thisView = document.getElementById(this.mUIElementID);
    		    var thisParent = document.getElementById(parentUIID);
    		    if(thisParent){
    		    	thisView.parentNode.removeChild(thisView);
    				thisParent.appendChild(thisView);
    		    }
    		*/
    		{
    		}
    
    
    		System.out.println("View fixParent done: Parent ID " + parentUIID + " this view ID "
    				+ thisViewId);
		}
	}

	/**
	 * This is called when the view is attached to a window.  At this point it
	 * has a Surface and will start drawing.  Note that this function is
	 * guaranteed to be called before {@link #onDraw}, however it may be called
	 * any time before the first onDraw -- including before or after
	 * {@link #onMeasure}.
	 *
	 * @see #onDetachedFromWindow()
	 */
	protected void onAttachedToWindow() {
		if ((mPrivateFlags & REQUEST_TRANSPARENT_REGIONS) != 0) {
			mParent.requestTransparentRegion(this);
		}
		if ((mPrivateFlags & AWAKEN_SCROLL_BARS_ON_ATTACH) != 0) {
			mPrivateFlags &= ~AWAKEN_SCROLL_BARS_ON_ATTACH;
		}
	}

	/**
	 * This is called when the view is detached from a window.  At this point it
	 * no longer has a surface for drawing.
	 *
	 * @see #onAttachedToWindow()
	 */
	protected void onDetachedFromWindow() {
		mPrivateFlags &= ~CANCEL_NEXT_UP_EVENT;
	}

	/**
	 * @return The number of times this view has been attached to a window
	 */
	protected int getWindowAttachCount() {
		return mWindowAttachCount;
	}

	/**
	 * @param info the {@link android.view.View.AttachInfo} to associated with
	 *        this view
	 */
	void dispatchAttachedToWindow(AttachInfo info, int visibility) {
		mAttachInfo = info;
		mWindowAttachCount++;
		if ((mPrivateFlags & SCROLL_CONTAINER) != 0) {
			mAttachInfo.mScrollContainers.add(this);
			mPrivateFlags |= SCROLL_CONTAINER_ADDED;
		}
		performCollectViewAttributes(visibility);
		onAttachedToWindow();
        int vis = info.mWindowVisibility;
        if (vis != GONE) {
            onWindowVisibilityChanged(vis);
        }
	}
    /**
     * Called when the window containing has change its visibility
     * (between {@link #GONE}, {@link #INVISIBLE}, and {@link #VISIBLE}).  Note
     * that this tells you whether or not your window is being made visible
     * to the window manager; this does <em>not</em> tell you whether or not
     * your window is obscured by other windows on the screen, even if it
     * is itself visible.
     *
     * @param visibility The new visibility of the window.
     */
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility == VISIBLE) {
            initialAwakenScrollBars();
        }
    }
    /**
     * Trigger the scrollbars to draw.
     * This method differs from awakenScrollBars() only in its default duration.
     * initialAwakenScrollBars() will show the scroll bars for longer than
     * usual to give the user more of a chance to notice them.
     *
     * @return true if the animation is played, false otherwise.
     */
    private boolean initialAwakenScrollBars() {
        return mScrollCache != null &&
                awakenScrollBars(mScrollCache.scrollBarDefaultDelayBeforeFade * 4, true);
    }
	void dispatchDetachedFromWindow() {
		onDetachedFromWindow();
		if ((mPrivateFlags & SCROLL_CONTAINER_ADDED) != 0) {
			mAttachInfo.mScrollContainers.remove(this);
			mPrivateFlags &= ~SCROLL_CONTAINER_ADDED;
		}
		mAttachInfo = null;
	}

	/**
	 * Store this view hierarchy's frozen state into the given container.
	 *
	 * @param container The SparseArray in which to save the view's state.
	 *
	 * @see #restoreHierarchyState
	 * @see #dispatchSaveInstanceState
	 * @see #onSaveInstanceState
	 */
	public void saveHierarchyState(SparseArray<Parcelable> container) {
	    dispatchSaveInstanceState(container);
	}

	/**
	 * Called by {@link #saveHierarchyState} to store the state for this view and its children.
	 * May be overridden to modify how freezing happens to a view's children; for example, some
	 * views may want to not store state for their children.
	 *
	 * @param container The SparseArray in which to save the view's state.
	 *
	 * @see #dispatchRestoreInstanceState
	 * @see #saveHierarchyState
	 * @see #onSaveInstanceState
	 */
	protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
	    if (mID != NO_ID && (mViewFlags & SAVE_DISABLED_MASK) == 0) {
	        mPrivateFlags &= ~SAVE_STATE_CALLED;
	        Parcelable state = onSaveInstanceState();
	        if ((mPrivateFlags & SAVE_STATE_CALLED) == 0) {
	            throw new IllegalStateException(
	                    "Derived class did not call super.onSaveInstanceState()");
	        }
	        if (state != null) {
	            // Log.i("View", "Freezing #" + Integer.toHexString(mID)
	            // + ": " + state);
	            container.put(mID, state);
	        }
	    }
	}

	/**
	 * Hook allowing a view to generate a representation of its internal state
	 * that can later be used to create a new instance with that same state.
	 * This state should only contain information that is not persistent or can
	 * not be reconstructed later. For example, you will never store your
	 * current position on screen because that will be computed again when a
	 * new instance of the view is placed in its view hierarchy.
	 * <p>
	 * Some examples of things you may store here: the current cursor position
	 * in a text view (but usually not the text itself since that is stored in a
	 * content provider or other persistent storage), the currently selected
	 * item in a list view.
	 *
	 * @return Returns a Parcelable object containing the view's current dynamic
	 *         state, or null if there is nothing interesting to save. The
	 *         default implementation returns null.
	 * @see #onRestoreInstanceState
	 * @see #saveHierarchyState
	 * @see #dispatchSaveInstanceState
	 * @see #setSaveEnabled(boolean)
	 */
	protected Parcelable onSaveInstanceState() {
	    mPrivateFlags |= SAVE_STATE_CALLED;
	    return BaseSavedState.EMPTY_STATE;
	}

	/**
	 * Restore this view hierarchy's frozen state from the given container.
	 *
	 * @param container The SparseArray which holds previously frozen states.
	 *
	 * @see #saveHierarchyState
	 * @see #dispatchRestoreInstanceState
	 * @see #onRestoreInstanceState
	 */
	public void restoreHierarchyState(SparseArray<Parcelable> container) {
	    dispatchRestoreInstanceState(container);
	}

	/**
	 * Called by {@link #restoreHierarchyState} to retrieve the state for this view and its
	 * children. May be overridden to modify how restoreing happens to a view's children; for
	 * example, some views may want to not store state for their children.
	 *
	 * @param container The SparseArray which holds previously saved state.
	 *
	 * @see #dispatchSaveInstanceState
	 * @see #restoreHierarchyState
	 * @see #onRestoreInstanceState
	 */
	protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
	    if (mID != NO_ID) {
	        Parcelable state = container.get(mID);
	        if (state != null) {
	            // Log.i("View", "Restoreing #" + Integer.toHexString(mID)
	            // + ": " + state);
	            mPrivateFlags &= ~SAVE_STATE_CALLED;
	            onRestoreInstanceState(state);
	            if ((mPrivateFlags & SAVE_STATE_CALLED) == 0) {
	                throw new IllegalStateException(
	                        "Derived class did not call super.onRestoreInstanceState()");
	            }
	        }
	    }
	}

	/**
	 * Hook allowing a view to re-apply a representation of its internal state that had previously
	 * been generated by {@link #onSaveInstanceState}. This function will never be called with a
	 * null state.
	 *
	 * @param state The frozen state that had previously been returned by
	 *        {@link #onSaveInstanceState}.
	 *
	 * @see #onSaveInstanceState
	 * @see #restoreHierarchyState
	 * @see #dispatchRestoreInstanceState
	 */
	protected void onRestoreInstanceState(Parcelable state) {
	    mPrivateFlags |= SAVE_STATE_CALLED;
	    if (state != BaseSavedState.EMPTY_STATE && state != null) {
	        throw new IllegalArgumentException("Wrong state class, expecting View State but "
	                + "received " + state.getClass().toString() + " instead. This usually happens "
	                + "when two views of different type have the same id in the same hierarchy. " /*
	                + "This view's id is " + ViewDebug.resolveId(mContext, getId()) + ". Make sure "
	                + "other views do not use the same id."*/);
	    }
	}

    /**
     * <p>Return the time at which the drawing of the view hierarchy started.</p>
     *
     * @return the drawing start time in milliseconds
     */
    public long getDrawingTime() {
        return mAttachInfo != null ? mAttachInfo.mDrawingTime : 0;
    }
    
	/**
	 * <p>Enables or disables the duplication of the parent's state into this view. When
	 * duplication is enabled, this view gets its drawable state from its parent rather
	 * than from its own internal properties.</p>
	 *
	 * <p>Note: in the current implementation, setting this property to true after the
	 * view was added to a ViewGroup might have no effect at all. This property should
	 * always be used from XML or set to true before adding this view to a ViewGroup.</p>
	 *
	 * <p>Note: if this view's parent addStateFromChildren property is enabled and this
	 * property is enabled, an exception will be thrown.</p>
	 *
	 * @param enabled True to enable duplication of the parent's drawable state, false
	 *                to disable it.
	 *
	 * @see #getDrawableState()
	 * @see #isDuplicateParentStateEnabled()
	 */
	public void setDuplicateParentStateEnabled(boolean enabled) {
		setFlags(enabled ? DUPLICATE_PARENT_STATE : 0, DUPLICATE_PARENT_STATE);
	}

	/**
	 * <p>Indicates whether this duplicates its drawable state from its parent.</p>
	 *
	 * @return True if this view's drawable state is duplicated from the parent,
	 *         false otherwise
	 *
	 * @see #getDrawableState()
	 * @see #setDuplicateParentStateEnabled(boolean)
	 */
	public boolean isDuplicateParentStateEnabled() {
		return (mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE;
	}

	/**
	 * <p>Enables or disables the drawing cache. When the drawing cache is enabled, the next call
	 * to {@link #getDrawingCache()} or {@link #buildDrawingCache()} will draw the view in a
	 * bitmap. Calling {@link #draw(android.graphics.Canvas)} will not draw from the cache when
	 * the cache is enabled. To benefit from the cache, you must request the drawing cache by
	 * calling {@link #getDrawingCache()} and draw it on screen if the returned bitmap is not
	 * null.</p>
	 *
	 * @param enabled true to enable the drawing cache, false otherwise
	 *
	 * @see #isDrawingCacheEnabled()
	 * @see #getDrawingCache()
	 * @see #buildDrawingCache()
	 */
	public void setDrawingCacheEnabled(boolean enabled) {
		setFlags(enabled ? DRAWING_CACHE_ENABLED : 0, DRAWING_CACHE_ENABLED);
	}

	/**
	 * <p>Indicates whether the drawing cache is enabled for this view.</p>
	 *
	 * @return true if the drawing cache is enabled
	 *
	 * @see #setDrawingCacheEnabled(boolean)
	 * @see #getDrawingCache()
	 */
	public boolean isDrawingCacheEnabled() {
		return (mViewFlags & DRAWING_CACHE_ENABLED) == DRAWING_CACHE_ENABLED;
	}


    public void setBackgroundResource(int resid) {
        if (resid != 0 && resid == mBackgroundResource) {
            return;
        }

        Drawable d= null;
        if (resid != 0) {
            d = mResources.getDrawable(resid);
        }
        setBackgroundDrawable(d);

        mBackgroundResource = resid;
    }

    /**
     * Pass the touch screen motion event down to the target view, or this
     * view if it is the target.
     *
     * @param event The motion event to be dispatched.
     * @return True if the event was handled by the view, false otherwise.
     */
    public boolean dispatchTouchEvent(MotionEvent event) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "View dispatchTouchEvent at time:" + start);
        if (mOnTouchListener != null && (mViewFlags & ENABLED_MASK) == ENABLED &&
                mOnTouchListener.onTouch(this, event)) {
            return true;
        }
        return onTouchEvent(event);
    }
    private OnTouchListener mOnTouchListener;

    /**
     * Register a callback to be invoked when a touch event is sent to this view.
     * @param l the touch listener to attach to this view
     */
    public void setOnTouchListener(OnTouchListener l) {
        mOnTouchListener = l;
    }

    /**
     * Gets the background drawable
     * @return The drawable used as the background for this view, if any.
     */
    public Drawable getBackground() {
        return mBGDrawable;
    }
	/**
	 * Setting a solid background color for the drawing cache's bitmaps will improve
	 * perfromance and memory usage. Note, though that this should only be used if this
	 * view will always be drawn on top of a solid color.
	 *
	 * @param color The background color to use for the drawing cache's bitmap
	 *
	 * @see #setDrawingCacheEnabled(boolean)
	 * @see #buildDrawingCache()
	 * @see #getDrawingCache()
	 */
	public void setDrawingCacheBackgroundColor(int color) {
		if (color != mDrawingCacheBackgroundColor) {
			mDrawingCacheBackgroundColor = color;
			mPrivateFlags &= ~DRAWING_CACHE_VALID;
		}
	}

	/**
	 * @see #setDrawingCacheBackgroundColor(int)
	 *
	 * @return The background color to used for the drawing cache's bitmap
	 */
	public int getDrawingCacheBackgroundColor() {
		return mDrawingCacheBackgroundColor;
	}

	/**
	 * Indicates whether this View is currently in edit mode. A View is usually
	 * in edit mode when displayed within a developer tool. For instance, if
	 * this View is being drawn by a visual user interface builder, this method
	 * should return true.
	 *
	 * Subclasses should check the return value of this method to provide
	 * different behaviors if their normal behavior might interfere with the
	 * host environment. For instance: the class spawns a thread in its
	 * constructor, the drawing code relies on device-specific features, etc.
	 *
	 * This method is usually checked in the drawing code of custom widgets.
	 *
	 * @return True if this View is in edit mode, false otherwise.
	 */
	public boolean isInEditMode() {
		return false;
	}

	/**
	 * If the View draws content inside its padding and enables fading edges,
	 * it needs to support padding offsets. Padding offsets are added to the
	 * fading edges to extend the length of the fade so that it covers pixels
	 * drawn inside the padding.
	 *
	 * Subclasses of this class should override this method if they need
	 * to draw content inside the padding.
	 *
	 * @return True if padding offset must be applied, false otherwise.
	 *
	 * @see #getLeftPaddingOffset()
	 * @see #getRightPaddingOffset()
	 * @see #getTopPaddingOffset()
	 * @see #getBottomPaddingOffset()
	 *
	 * @since CURRENT
	 */
	protected boolean isPaddingOffsetRequired() {
		return false;
	}

	/**
	 * Amount by which to extend the left fading region. Called only when
	 * {@link #isPaddingOffsetRequired()} returns true.
	 *
	 * @return The left padding offset in pixels.
	 *
	 * @see #isPaddingOffsetRequired()
	 *
	 * @since CURRENT
	 */
	protected int getLeftPaddingOffset() {
		return 0;
	}

	/**
	 * Amount by which to extend the right fading region. Called only when
	 * {@link #isPaddingOffsetRequired()} returns true.
	 *
	 * @return The right padding offset in pixels.
	 *
	 * @see #isPaddingOffsetRequired()
	 *
	 * @since CURRENT
	 */
	protected int getRightPaddingOffset() {
		return 0;
	}

	/**
	 * Amount by which to extend the top fading region. Called only when
	 * {@link #isPaddingOffsetRequired()} returns true.
	 *
	 * @return The top padding offset in pixels.
	 *
	 * @see #isPaddingOffsetRequired()
	 *
	 * @since CURRENT
	 */
	protected int getTopPaddingOffset() {
		return 0;
	}

	/**
	 * Amount by which to extend the bottom fading region. Called only when
	 * {@link #isPaddingOffsetRequired()} returns true.
	 *
	 * @return The bottom padding offset in pixels.
	 *
	 * @see #isPaddingOffsetRequired()
	 *
	 * @since CURRENT
	 */
	protected int getBottomPaddingOffset() {
		return 0;
	}

    /**
     * Manually render this view (and all of its children) to the given Canvas.
     * The view must have already done a full layout before this function is
     * called. When implementing a view, do not override this method; instead,
     * you should implement {@link #onDraw}.
     * 
     * @param canvas The Canvas to which the View is rendered.
     */
    public void draw(Canvas canvas) {

        if (this instanceof ScrollView) {
            /**
            *@j2sNative
            * var scrollViewElementId = this.getChildAt(0).getUIElementID();
            * var thisView = document.getElementById(scrollViewElementId);
            * if (null == thisView) {
            *     var thisScrollView = document.createElement("div");
            *     thisScrollView.style.position = "absolute";
            *     thisScrollView.id = scrollViewElementId;
            *     thisScrollView.style.display = "block";
            * }
            * 
            * if (thisView == null) {
            *     var viewRootId = this.getRootView().getParent().getViewRootID();
            *     var viewRoot = document.getElementById(viewRootId);
            *     viewRoot.childNodes[1].appendChild(thisScrollView); // append to decorview
            * }
            */{}
        }
        final int privateFlags = mPrivateFlags;
        final boolean dirtyOpaque = (privateFlags & DIRTY_MASK) == DIRTY_OPAQUE
                && (mAttachInfo == null || !mAttachInfo.mIgnoreDirtyState);
        mPrivateFlags = (privateFlags & ~DIRTY_MASK) | DRAWN;

        /*
         * Draw traversal performs several drawing steps which must be executed
         * in the appropriate order: 1. Draw the background 2. If necessary,
         * save the canvas' layers to prepare for fading 3. Draw view's content
         * 4. Draw children 5. If necessary, draw the fading edges and restore
         * layers 6. Draw decorations (scrollbars for instance)
         */

        // Step 1, draw the background, if needed
        int saveCount;

        if (!dirtyOpaque) {
            final Drawable background = mBGDrawable;
            if (background != null) {
                final int scrollX = mScrollX;
                final int scrollY = mScrollY;

                if (mBackgroundSizeChanged) {
                    background.setBounds(0, 0, mRight - mLeft, mBottom - mTop);
                    mBackgroundSizeChanged = false;
                }

                canvas.chooseCanvas(Canvas.APP_CANVAS);
                if ((scrollX | scrollY) == 0) {
                    if (alpha != -1) {
                        canvas.alpha = alpha;
                    }
                    background.draw(canvas);
                } else {
                    canvas.translate(scrollX, scrollY);
                    background.draw(canvas);
                    canvas.translate(-scrollX, -scrollY);
                }
            }
        }

        // skip step 2 & 5 if possible (common case)
        final int viewFlags = mViewFlags;
        boolean horizontalEdges = (viewFlags & FADING_EDGE_HORIZONTAL) != 0;
        boolean verticalEdges = (viewFlags & FADING_EDGE_VERTICAL) != 0;
        // MayLoon: In MayLoon, we will disable fading edge effect. So the full fledged routine will
        // never be executed.
        horizontalEdges = verticalEdges = false;
        if (!verticalEdges && !horizontalEdges) {
            // Step 3, draw the content
            if ((false == dirtyOpaque) && ((viewFlags & VISIBILITY_MASK) == VISIBLE)) {
                canvas.chooseCanvas(Canvas.APP_CANVAS);
                onDraw(canvas);
            }
            // Step 4, draw the children
            dispatchDraw(canvas);
            // Step 6, draw decorations (scrollbars)
            onDrawScrollBars(canvas);
            // we're done...
            return;
        }

//        /*
//         * Here we do the full fledged routine... (this is an uncommon case
//         * where speed matters less, this is why we repeat some of the tests
//         * that have been done above)
//         */
//        Log.d("View", "View.draw, Full fledged routine");
//        boolean drawTop = false;
//        boolean drawBottom = false;
//        boolean drawLeft = false;
//        boolean drawRight = false;
//
//        float topFadeStrength = 0.0f;
//        float bottomFadeStrength = 0.0f;
//        float leftFadeStrength = 0.0f;
//        float rightFadeStrength = 0.0f;
//
//        // Step 2, save the canvas' layers
//        int paddingLeft = mPaddingLeft;
//        int paddingTop = mPaddingTop;
//
//        final boolean offsetRequired = isPaddingOffsetRequired();
//        if (offsetRequired) {
//            paddingLeft += getLeftPaddingOffset();
//            paddingTop += getTopPaddingOffset();
//        }
//
//        int left = mScrollX + paddingLeft;
//        int right = left + mRight - mLeft - mPaddingRight - paddingLeft;
//        int top = mScrollY + paddingTop;
//        int bottom = top + mBottom - mTop - mPaddingBottom - paddingTop;
//
//        if (offsetRequired) {
//            right += getRightPaddingOffset();
//            bottom += getBottomPaddingOffset();
//        }
//
//        final ScrollabilityCache scrollabilityCache = mScrollCache;
//        int length = scrollabilityCache.fadingEdgeLength;
//
//        // clip the fade length if top and bottom fades overlap
//        // overlapping fades produce odd-looking artifacts
//        if (verticalEdges && (top + length > bottom - length)) {
//            length = (bottom - top) / 2;
//        }
//
//        // also clip horizontal fades if necessary
//        if (horizontalEdges && (left + length > right - length)) {
//            length = (right - left) / 2;
//        }
//
//        if (verticalEdges) {
//            topFadeStrength = Math.max(0.0f, Math.min(1.0f, getTopFadingEdgeStrength()));
//            drawTop = topFadeStrength >= 0.0f;
//            bottomFadeStrength = Math.max(0.0f, Math.min(1.0f, getBottomFadingEdgeStrength()));
//            drawBottom = bottomFadeStrength >= 0.0f;
//        }
//
//        if (horizontalEdges) {
//            leftFadeStrength = Math.max(0.0f, Math.min(1.0f, getLeftFadingEdgeStrength()));
//            drawLeft = leftFadeStrength >= 0.0f;
//            rightFadeStrength = Math.max(0.0f, Math.min(1.0f, getRightFadingEdgeStrength()));
//            drawRight = rightFadeStrength >= 0.0f;
//        }
//
//        saveCount = canvas.getSaveCount();
//
//        // int solidColor = getSolidColor();
//        // if (solidColor == 0) {
//        // final int flags = Canvas.HAS_ALPHA_LAYER_SAVE_FLAG;
//        //
//        // if (drawTop) {
//        // canvas.saveLayer(left, top, right, top + length, null, flags);
//        // }
//        //
//        // if (drawBottom) {
//        // canvas.saveLayer(left, bottom - length, right, bottom, null, flags);
//        // }
//        //
//        // if (drawLeft) {
//        // canvas.saveLayer(left, top, left + length, bottom, null, flags);
//        // }
//        //
//        // if (drawRight) {
//        // canvas.saveLayer(right - length, top, right, bottom, null, flags);
//        // }
//        // } else {
//        // scrollabilityCache.setFadeColor(solidColor);
//        // }
//
//        // Step 3, draw the content
//        if (!dirtyOpaque)
//            onDraw(canvas);
//
//        // Step 4, draw the children
//        dispatchDraw(canvas);
//
//        // Step 5, draw the fade effect and restore layers
//        // final Paint p = scrollabilityCache.paint;
//        // final Matrix matrix = scrollabilityCache.matrix;
//        // final Shader fade = scrollabilityCache.shader;
//        // final float fadeHeight = scrollabilityCache.fadingEdgeLength;
//        //
//        // if (drawTop) {
//        // matrix.setScale(1, fadeHeight * topFadeStrength);
//        // matrix.postTranslate(left, top);
//        // fade.setLocalMatrix(matrix);
//        // canvas.drawRect(left, top, right, top + length, p);
//        // }
//        //
//        // if (drawBottom) {
//        // matrix.setScale(1, fadeHeight * bottomFadeStrength);
//        // matrix.postRotate(180);
//        // matrix.postTranslate(left, bottom);
//        // fade.setLocalMatrix(matrix);
//        // canvas.drawRect(left, bottom - length, right, bottom, p);
//        // }
//        //
//        // if (drawLeft) {
//        // matrix.setScale(1, fadeHeight * leftFadeStrength);
//        // matrix.postRotate(-90);
//        // matrix.postTranslate(left, top);
//        // fade.setLocalMatrix(matrix);
//        // canvas.drawRect(left, top, left + length, bottom, p);
//        // }
//        //
//        // if (drawRight) {
//        // matrix.setScale(1, fadeHeight * rightFadeStrength);
//        // matrix.postRotate(90);
//        // matrix.postTranslate(right, top);
//        // fade.setLocalMatrix(matrix);
//        // canvas.drawRect(right - length, top, right, bottom, p);
//        // }
//
//        canvas.restoreToCount(saveCount);
//
//        // Step 6, draw decorations (scrollbars)
//         onDrawScrollBars(canvas);
    }

	/**
	 * Override this if your view is known to always be drawn on top of a solid color background,
	 * and needs to draw fading edges. Returning a non-zero color enables the view system to
	 * optimize the drawing of the fading edges. If you do return a non-zero color, the alpha
	 * should be set to 0xFF.
	 *
	 * @see #setVerticalFadingEdgeEnabled
	 * @see #setHorizontalFadingEdgeEnabled
	 *
	 * @return The known solid color background for this view, or 0 if the color may vary
	 */
	public int getSolidColor() {
		return 0;
	}

	/**
	 * Build a human readable string representation of the specified view flags.
	 *
	 * @param flags the view flags to convert to a string
	 * @return a String representing the supplied flags
	 */
	@SuppressWarnings("unused")
	private static String printFlags(int flags) {
		String output = "";
		int numFlags = 0;
		if ((flags & FOCUSABLE_MASK) == FOCUSABLE) {
			output += "TAKES_FOCUS";
			numFlags++;
		}

		switch (flags & VISIBILITY_MASK) {
		case INVISIBLE:
			if (numFlags > 0) {
				output += " ";
			}
			output += "INVISIBLE";
			// USELESS HERE numFlags++;
			break;
		case GONE:
			if (numFlags > 0) {
				output += " ";
			}
			output += "GONE";
			// USELESS HERE numFlags++;
			break;
		default:
			break;
		}
		return output;
	}

	public int getResolvedLayoutDirection() {
		resolveLayoutDirectionIfNeeded();
		return ((mPrivateFlags2 & LAYOUT_DIRECTION_RESOLVED_RTL) == LAYOUT_DIRECTION_RESOLVED_RTL) ? LAYOUT_DIRECTION_RTL
				: LAYOUT_DIRECTION_LTR;
	}

	/**
	 * Resolve and cache the layout direction. LTR is set initially. This is implicitly supposing
	 * that the parent directionality can and will be resolved before its children.
	 */
	private void resolveLayoutDirectionIfNeeded() {
		// Do not resolve if it is not needed
		if ((mPrivateFlags2 & LAYOUT_DIRECTION_RESOLVED) == LAYOUT_DIRECTION_RESOLVED)
			return;

		// Clear any previous layout direction resolution
		mPrivateFlags2 &= ~LAYOUT_DIRECTION_RESOLVED_RTL;

		// Reset also TextDirection as a change into LayoutDirection may impact the selected
		// TextDirectionHeuristic
		resetResolvedTextDirection();

		// Set resolved depending on layout direction
		switch (getLayoutDirection()) {
		case LAYOUT_DIRECTION_INHERIT:
			// We cannot do the resolution if there is no parent
			if (mParent == null)
				return;

			// If this is root view, no need to look at parent's layout dir.
			if (mParent instanceof ViewGroup) {
				ViewGroup viewGroup = ((ViewGroup) mParent);

				// Check if the parent view group can resolve
				if (!viewGroup.canResolveLayoutDirection()) {
					return;
				}
				if (viewGroup.getResolvedLayoutDirection() == LAYOUT_DIRECTION_RTL) {
					mPrivateFlags2 |= LAYOUT_DIRECTION_RESOLVED_RTL;
				}
			}
			break;
		case LAYOUT_DIRECTION_RTL:
			mPrivateFlags2 |= LAYOUT_DIRECTION_RESOLVED_RTL;
			break;
		case LAYOUT_DIRECTION_LOCALE:
			if (isLayoutDirectionRtl(Locale.getDefault())) {
				mPrivateFlags2 |= LAYOUT_DIRECTION_RESOLVED_RTL;
			}
			break;
		default:
			// Nothing to do, LTR by default
		}

		// Set to resolved
		mPrivateFlags2 |= LAYOUT_DIRECTION_RESOLVED;
	}

	public int getLayoutDirection() {
		return mViewFlags & LAYOUT_DIRECTION_MASK;
	}

	/**
	 * Return true if layout direction resolution can be done
	 *
	 * @hide
	 */
	protected boolean canResolveLayoutDirection() {
		switch (getLayoutDirection()) {
		case LAYOUT_DIRECTION_INHERIT:
			return (mParent != null);
		default:
			return true;
		}
	}

	/**
	 * Check if a Locale is corresponding to a RTL script.
	 *
	 * @param locale Locale to check
	 * @return true if a Locale is corresponding to a RTL script.
	 *
	 * @hide
	 */
	protected static boolean isLayoutDirectionRtl(Locale locale) {
		return (LocaleUtil.TEXT_LAYOUT_DIRECTION_RTL_DO_NOT_USE == LocaleUtil
				.getLayoutDirectionFromLocale(locale));
	}

	/**
	 * Build a human readable string representation of the specified private
	 * view flags.
	 *
	 * @param privateFlags the private view flags to convert to a string
	 * @return a String representing the supplied flags
	 */
	@SuppressWarnings("unused")
	private static String printPrivateFlags(int privateFlags) {
		String output = "";
		int numFlags = 0;

		if ((privateFlags & WANTS_FOCUS) == WANTS_FOCUS) {
			output += "WANTS_FOCUS";
			numFlags++;
		}

		if ((privateFlags & FOCUSED) == FOCUSED) {
			if (numFlags > 0) {
				output += " ";
			}
			output += "FOCUSED";
			numFlags++;
		}

		if ((privateFlags & SELECTED) == SELECTED) {
			if (numFlags > 0) {
				output += " ";
			}
			output += "SELECTED";
			numFlags++;
		}

		if ((privateFlags & IS_ROOT_NAMESPACE) == IS_ROOT_NAMESPACE) {
			if (numFlags > 0) {
				output += " ";
			}
			output += "IS_ROOT_NAMESPACE";
			numFlags++;
		}

		if ((privateFlags & HAS_BOUNDS) == HAS_BOUNDS) {
			if (numFlags > 0) {
				output += " ";
			}
			output += "HAS_BOUNDS";
			numFlags++;
		}

		if ((privateFlags & DRAWN) == DRAWN) {
			if (numFlags > 0) {
				output += " ";
			}
			output += "DRAWN";
			// USELESS HERE numFlags++;
		}
		return output;
	}

	/**
	 * <p>Indicates whether or not this view's layout will be requested during
	 * the next hierarchy layout pass.</p>
	 *
	 * @return true if the layout will be forced during next layout pass
	 */
	public boolean isLayoutRequested() {
		return (mPrivateFlags & FORCE_LAYOUT) == FORCE_LAYOUT;
	}

	/**
	 * Assign a size and position to a view and all of its
	 * descendants
	 *
	 * <p>This is the second phase of the layout mechanism.
	 * (The first is measuring). In this phase, each parent calls
	 * layout on all of its children to position them.
	 * This is typically done using the child measurements
	 * that were stored in the measure pass().
	 *
	 * Derived classes with children should override
	 * onLayout. In that method, they should
	 * call layout on each of their their children.
	 *
	 * @param l Left position, relative to parent
	 * @param t Top position, relative to parent
	 * @param r Right position, relative to parent
	 * @param b Bottom position, relative to parent
	 */
	public final void layout(int l, int t, int r, int b) {
		boolean changed = setFrame(l, t, r, b);
		if (changed || (mPrivateFlags & LAYOUT_REQUIRED) == LAYOUT_REQUIRED) {
			onLayout(changed, l, t, r, b);
			mPrivateFlags &= ~LAYOUT_REQUIRED;
		}
		mPrivateFlags &= ~FORCE_LAYOUT;
	}

	/**
	 * Called from layout when this view should
	 * assign a size and position to each of its children.
	 *
	 * Derived classes with children should override
	 * this method and call layout on each of
	 * their their children.
	 * @param changed This is a new size or position for this view
	 * @param left Left position, relative to parent
	 * @param top Top position, relative to parent
	 * @param right Right position, relative to parent
	 * @param bottom Bottom position, relative to parent
	 */
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
	}

	/**
	 * Assign a size and position to this view.
	 *
	 * This is called from layout.
	 *
	 * @param left Left position, relative to parent
	 * @param top Top position, relative to parent
	 * @param right Right position, relative to parent
	 * @param bottom Bottom position, relative to parent
	 * @return true if the new size and position are different than the
	 *         previous ones
	 * {@hide}
	 */

	public boolean setFrame(int left, int top, int right, int bottom) {
		boolean changed = false;

		if (mLeft != left || mRight != right || mTop != top
				|| mBottom != bottom) {
			changed = true;

			// Remember our drawn bit
			int drawn = mPrivateFlags & DRAWN;
			
            // Invalidate our old position
            invalidate();

			int oldWidth = mRight - mLeft;
			int oldHeight = mBottom - mTop;

            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;

			if (DebugUtils.DEBUG_VIEW_IN_BROWSER) {
    			/**
    			 @j2sNative
    			 thisView = document.getElementById(this.mUIElementID);
    			 thisView.style.left = this.mLeft + "px";
    			 thisView.style.top = this.mTop + "px";
    			 thisView.style.right = this.mRight + "px";
    			 thisView.style.bottom = this.mBottom + "px";
    			 */
    			{
    			}
			}

			mPrivateFlags |= HAS_BOUNDS;

			int newWidth = right - left;
			int newHeight = bottom - top;

			if (newWidth != oldWidth || newHeight != oldHeight) {
				onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
			}

			if ((mViewFlags & VISIBILITY_MASK) == VISIBLE) {
				// If we are visible, force the DRAWN bit to on so that
				// this invalidate will go through (at least to our parent).
				// This is because someone may have invalidated this view
				// before this call to setFrame came in, therby clearing
				// the DRAWN bit.
				mPrivateFlags |= DRAWN;
				invalidate();
			}

			// Reset drawn bit to original value (invalidate turns it off)
			mPrivateFlags |= drawn;
			
			mBackgroundSizeChanged = true;
		}
		return changed;
	}

	/**
	 * Finalize inflating a view from XML.  This is called as the last phase
	 * of inflation, after all child views have been added.
	 *
	 * <p>Even if the subclass overrides onFinishInflate, they should always be
	 * sure to call the super method, so that we get called.
	 */
	protected void onFinishInflate() {
	}

	/**
	 * Returns the resources associated with this view.
	 *
	 * @return Resources object.
	 */
	public Resources getResources() {
		return mResources;
	}

	/**
	 * Return an array of resource IDs of the drawable states representing the
	 * current state of the view.
	 *
	 * @return The current drawable state
	 *
	 * @see Drawable#setState
	 * @see #drawableStateChanged
	 * @see #onCreateDrawableState
	 */
	public final int[] getDrawableState() {
		if ((mDrawableState != null)
				&& ((mPrivateFlags & DRAWABLE_STATE_DIRTY) == 0)) {
			return mDrawableState;
		} else {
			mDrawableState = onCreateDrawableState(0);
			mPrivateFlags &= ~DRAWABLE_STATE_DIRTY;
			return mDrawableState;
		}
	}

	/**
	 * Generate the new {@link android.graphics.drawable.Drawable} state for
	 * this view. This is called by the view
	 * system when the cached Drawable state is determined to be invalid.  To
	 * retrieve the current state, you should use {@link #getDrawableState}.
	 *
	 * @param extraSpace if non-zero, this is the number of extra entries you
	 * would like in the returned array in which you can place your own
	 * states.
	 *
	 * @return Returns an array holding the current {@link Drawable} state of
	 * the view.
	 *
	 * @see #mergeDrawableStates
	 */
	protected int[] onCreateDrawableState(int extraSpace) {
		if ((mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE
				&& mParent instanceof View) {
			return ((View) mParent).onCreateDrawableState(extraSpace);
		}

		int[] drawableState;

		int privateFlags = mPrivateFlags;

		int viewStateIndex = (((privateFlags & PRESSED) != 0) ? 1 : 0);

		viewStateIndex = (viewStateIndex << 1)
				+ (((mViewFlags & ENABLED_MASK) == ENABLED) ? 1 : 0);

		viewStateIndex = (viewStateIndex << 1) + (isFocused() ? 1 : 0);

		viewStateIndex = (viewStateIndex << 1)
				+ (((privateFlags & SELECTED) != 0) ? 1 : 0);

		final boolean hasWindowFocus = hasWindowFocus();
		viewStateIndex = (viewStateIndex << 1) + (hasWindowFocus ? 1 : 0);

		drawableState = VIEW_STATE_SETS[viewStateIndex];

		if (extraSpace == 0) {
			return drawableState;
		}

		final int[] fullState;
		if (drawableState != null) {
			fullState = new int[drawableState.length + extraSpace];
			System.arraycopy(drawableState, 0, fullState, 0,
					drawableState.length);
		} else {
			fullState = new int[extraSpace];
		}

		return fullState;
	}

	/**
	 * Merge your own state values in <var>additionalState</var> into the base
	 * state values <var>baseState</var> that were returned by
	 * {@link #onCreateDrawableState}.
	 *
	 * @param baseState The base state values returned by
	 * {@link #onCreateDrawableState}, which will be modified to also hold your
	 * own additional state values.
	 *
	 * @param additionalState The additional state values you would like
	 * added to <var>baseState</var>; this array is not modified.
	 *
	 * @return As a convenience, the <var>baseState</var> array you originally
	 * passed into the function is returned.
	 *
	 * @see #onCreateDrawableState
	 */
	protected static int[] mergeDrawableStates(int[] baseState,
			int[] additionalState) {
		final int N = baseState.length;
		int i = N - 1;
		while (i >= 0 && baseState[i] == 0) {
			i--;
		}
		System.arraycopy(additionalState, 0, baseState, i + 1,
				additionalState.length);
		return baseState;
	}

	/**
	 * Sets the padding. The view may add on the space required to display
	 * the scrollbars, depending on the style and visibility of the scrollbars.
	 * So the values returned from {@link #getPaddingLeft}, {@link #getPaddingTop},
	 * {@link #getPaddingRight} and {@link #getPaddingBottom} may be different
	 * from the values set in this call.
	 *
	 * @attr ref android.R.styleable#View_padding
	 * @attr ref android.R.styleable#View_paddingBottom
	 * @attr ref android.R.styleable#View_paddingLeft
	 * @attr ref android.R.styleable#View_paddingRight
	 * @attr ref android.R.styleable#View_paddingTop
	 * @param left the left padding in pixels
	 * @param top the top padding in pixels
	 * @param right the right padding in pixels
	 * @param bottom the bottom padding in pixels
	 */
	public void setPadding(int left, int top, int right, int bottom) {
		boolean changed = false;

		mUserPaddingRight = right;
		mUserPaddingBottom = bottom;

		if (mPaddingLeft != left) {
			changed = true;
			mPaddingLeft = left;
		}
		if (mPaddingTop != top) {
			changed = true;
			mPaddingTop = top;
		}
		if (mPaddingRight != right) {
			changed = true;
			mPaddingRight = right;
		}
		if (mPaddingBottom != bottom) {
			changed = true;
			mPaddingBottom = bottom;
		}

		if (changed) {
			requestLayout();
		}
	}

	/**
	 * Returns the top padding of this view.
	 *
	 * @return the top padding in pixels
	 */
	public int getPaddingTop() {
		return mPaddingTop;
	}

	/**
	 * Text direction is inherited thru {@link ViewGroup}
	 * @hide
	 */
	public static final int TEXT_DIRECTION_INHERIT = 0;
	/**
	* Reset resolved text direction. Will be resolved during a call to getResolvedTextDirection().
	*
	* @hide
	*/
	private int mResolvedTextDirection = TEXT_DIRECTION_INHERIT;

	protected void resetResolvedTextDirection() {
		mResolvedTextDirection = TEXT_DIRECTION_INHERIT;
	}

	/**
	 * Returns the bottom padding of this view. If there are inset and enabled
	 * scrollbars, this value may include the space required to display the
	 * scrollbars as well.
	 *
	 * @return the bottom padding in pixels
	 */
	public int getPaddingBottom() {
		return mPaddingBottom;
	}

	/**
	 * Returns the left padding of this view. If there are inset and enabled
	 * scrollbars, this value may include the space required to display the
	 * scrollbars as well.
	 *
	 * @return the left padding in pixels
	 */
	public int getPaddingLeft() {
		return mPaddingLeft;
	}

	/**
	 * Returns the right padding of this view. If there are inset and enabled
	 * scrollbars, this value may include the space required to display the
	 * scrollbars as well.
	 *
	 * @return the right padding in pixels
	 */
	public int getPaddingRight() {
		return mPaddingRight;
	}

    /**
     * Changes the selection state of this view. A view can be selected or not.
     * Note that selection is not the same as focus. Views are typically
     * selected in the context of an AdapterView like ListView or GridView; the
     * selected view is the view that is highlighted.
     * 
     * @param selected true if the view must be selected, false otherwise
     */
    public void setSelected(boolean selected) {
        if (((mPrivateFlags & SELECTED) != 0) != selected) {
            mPrivateFlags = (mPrivateFlags & ~SELECTED)
                    | (selected ? SELECTED : 0);
            if (!selected)
                resetPressedState();
            invalidate();
            refreshDrawableState();
            dispatchSetSelected(selected);
        }
    }

	/**
	 * Dispatch setSelected to all of this View's children.
	 *
	 * @see #setSelected(boolean)
	 *
	 * @param selected The new selected state
	 */
	protected void dispatchSetSelected(boolean selected) {
	}

	/**
	 * Indicates the selection state of this view.
	 *
	 * @return true if the view is selected, false otherwise
	 */
	public boolean isSelected() {
		return (mPrivateFlags & SELECTED) != 0;
	}

    /**
     * Returns the ViewTreeObserver for this view's hierarchy. The view tree
     * observer can be used to get notifications when global events, like
     * layout, happen.
     *
     * The returned ViewTreeObserver observer is not guaranteed to remain
     * valid for the lifetime of this View. If the caller of this method keeps
     * a long-lived reference to ViewTreeObserver, it should always check for
     * the return value of {@link ViewTreeObserver#isAlive()}.
     *
     * @return The ViewTreeObserver for this view's hierarchy.
     */
    public ViewTreeObserver getViewTreeObserver() {
        if (mAttachInfo != null) {
            return mAttachInfo.mTreeObserver;
        }

        return null;
    }

	/**
	 * <p>Finds the topmost view in the current view hierarchy.</p>
	 *
	 * @return the topmost view containing this view
	 */
	public View getRootView() {
		//		if (mAttachInfo != null) {
		//			final View v = mAttachInfo.mRootView;
		//			if (v != null) {
		//				return v;
		//			}
		//		}
		View parent = this;

		while (parent.mParent != null && parent.mParent instanceof View) {
			parent = (View) parent.mParent;
		}

		return parent;
	}

	/**
	 * <p>Computes the coordinates of this view on the screen. The argument
	 * must be an array of two integers. After the method returns, the array
	 * contains the x and y location in that order.</p>
	 *
	 * @param location an array of two integers in which to hold the coordinates
	 */
	public void getLocationOnScreen(int[] location) {
		getLocationInWindow(location);

		final AttachInfo info = mAttachInfo;
		if (info != null) {
			location[0] += info.mWindowLeft;
			location[1] += info.mWindowTop;
		}
	}

	/**
	 * <p>Computes the coordinates of this view in its window. The argument
	 * must be an array of two integers. After the method returns, the array
	 * contains the x and y location in that order.</p>
	 *
	 * @param location an array of two integers in which to hold the coordinates
	 */
	public void getLocationInWindow(int[] location) {
		if (location == null || location.length < 2) {
			throw new IllegalArgumentException("location must be an array of "
					+ "two integers");
		}

		location[0] = mLeft;
		location[1] = mTop;

		ViewParent viewParent = mParent;
		while (viewParent instanceof View) {
			final View view = (View) viewParent;
			location[0] += view.mLeft - view.mScrollX;
			location[1] += view.mTop - view.mScrollY;
			viewParent = view.mParent;
		}

		if (viewParent instanceof ViewRoot) {
			// *cough*
			final ViewRoot vr = (ViewRoot) viewParent;
			location[1] -= vr.mCurScrollY;
		}
	}

	/**
	 * {@hide}
	 * @param id the id of the view to be found
	 * @return the view of the specified id, null if cannot be found
	 */
	protected View findViewTraversal(int id) {
		if (id == mID) {
			return this;
		}
		return null;
	}

	/**
	 * {@hide}
	 * @param tag the tag of the view to be found
	 * @return the view of specified tag, null if cannot be found
	 */
	protected View findViewWithTagTraversal(Object tag) {
		if (tag != null && tag.equals(mTag)) {
			return this;
		}
		return null;
	}

	/**
	 * Look for a child view with the given id.  If this view has the given
	 * id, return this view.
	 *
	 * @param id The id to search for.
	 * @return The view that has the given id in the hierarchy or null
	 */
	public final View findViewById(int id) {
		if (id < 0) {
			return null;
		}
		return findViewTraversal(id);
	}

	/**
	 * Look for a child view with the given tag.  If this view has the given
	 * tag, return this view.
	 *
	 * @param tag The tag to search for, using "tag.equals(getTag())".
	 * @return The View that has the given tag in the hierarchy or null
	 */
	public final View findViewWithTag(Object tag) {
		if (tag == null) {
			return null;
		}
		return findViewWithTagTraversal(tag);
	}

	/**
	 * Sets the identifier for this view. The identifier does not have to be
	 * unique in this view's hierarchy. The identifier should be a positive
	 * number.
	 *
	 * @see #NO_ID
	 * @see #getId
	 * @see #findViewById
	 *
	 * @param id a number used to identify the view
	 *
	 * @attr ref android.R.styleable#View_id
	 */
	public void setId(int id) {
		mID = id;
	}

	/**
	 * {@hide}
	 *
	 * @param isRoot true if the view belongs to the root namespace, false
	 *        otherwise
	 */
	public void setIsRootNamespace(boolean isRoot) {
		if (isRoot) {
			mPrivateFlags |= IS_ROOT_NAMESPACE;
		} else {
			mPrivateFlags &= ~IS_ROOT_NAMESPACE;
		}
	}

	/**
	 * {@hide}
	 *
	 * @return true if the view belongs to the root namespace, false otherwise
	 */
	public boolean isRootNamespace() {
		return (mPrivateFlags & IS_ROOT_NAMESPACE) != 0;
	}

	/**
	 * Returns this view's identifier.
	 *
	 * @return a positive integer used to identify the view or {@link #NO_ID}
	 *         if the view has no ID
	 *
	 * @see #setId
	 * @see #findViewById
	 * @attr ref android.R.styleable#View_id
	 */
	public int getId() {
		return mID;
	}

	/**
	 * Returns this view's tag.
	 *
	 * @return the Object stored in this view as a tag
	 *
	 * @see #setTag(Object)
	 * @see #getTag(int)
	 */
	public Object getTag() {
		return mTag;
	}

	/**
	 * Sets the tag associated with this view. A tag can be used to mark
	 * a view in its hierarchy and does not have to be unique within the
	 * hierarchy. Tags can also be used to store data within a view without
	 * resorting to another data structure.
	 *
	 * @param tag an Object to tag the view with
	 *
	 * @see #getTag()
	 * @see #setTag(int, Object)
	 */
	public void setTag(final Object tag) {
		mTag = tag;
	}

	/**
	 * Returns the tag associated with this view and the specified key.
	 *
	 * @param key The key identifying the tag
	 *
	 * @return the Object stored in this view as a tag
	 *
	 * @see #setTag(int, Object)
	 * @see #getTag()
	 */
	public Object getTag(int key) {
		SparseArray<Object> tags = null;
		synchronized (sTagsLock) {
			if (sTags != null) {
				tags = sTags.get(this);
			}
		}

		if (tags != null)
			return tags.get(key);
		return null;
	}

	/**
	 * Sets a tag associated with this view and a key. A tag can be used
	 * to mark a view in its hierarchy and does not have to be unique within
	 * the hierarchy. Tags can also be used to store data within a view
	 * without resorting to another data structure.
	 *
	 * The specified key should be an id declared in the resources of the
	 * application to ensure it is unique (see the <a
	 * href={@docRoot}guide/topics/resources/more-resources.html#Id">ID resource type</a>).
	 * Keys identified as belonging to
	 * the Android framework or not associated with any package will cause
	 * an {@link IllegalArgumentException} to be thrown.
	 *
	 * @param key The key identifying the tag
	 * @param tag An Object to tag the view with
	 *
	 * @throws IllegalArgumentException If they specified key is not valid
	 *
	 * @see #setTag(Object)
	 * @see #getTag(int)
	 */
	public void setTag(int key, final Object tag) {
		// If the package id is 0x00 or 0x01, it's either an undefined package
		// or a framework id
		if ((key >>> 24) < 2) {
			throw new IllegalArgumentException(
					"The key must be an application-specific " + "resource id.");
		}

		setTagInternal(this, key, tag);
	}

	/**
	 * Variation of {@link #setTag(int, Object)} that enforces the key to be a
	 * framework id.
	 *
	 * @hide
	 */
	public void setTagInternal(int key, Object tag) {
		if ((key >>> 24) != 0x1) {
			throw new IllegalArgumentException(
					"The key must be a framework-specific " + "resource id.");
		}

		setTagInternal(this, key, tag);
	}

	private static void setTagInternal(View view, int key, Object tag) {
		SparseArray<Object> tags = null;
		synchronized (sTagsLock) {
			if (sTags == null) {
				sTags = new WeakHashMap<View, SparseArray<Object>>();
			} else {
				tags = sTags.get(view);
			}
		}

		if (tags == null) {
			tags = new SparseArray<Object>(2);
			synchronized (sTagsLock) {
				sTags.put(view, tags);
			}
		}

		tags.put(key, tag);
	}

	/**
	 * @param consistency The type of consistency. See ViewDebug for more information.
	 *
	 * @hide
	 */
	protected boolean dispatchConsistencyCheck(int consistency) {
		return onConsistencyCheck(consistency);
	}

	/**
	 * Method that subclasses should implement to check their consistency. The type of
	 * consistency check is indicated by the bit field passed as a parameter.
	 *
	 * @param consistency The type of consistency. See ViewDebug for more information.
	 *
	 * @throws IllegalStateException if the view is in an inconsistent state.
	 *
	 * @hide
	 */
	protected boolean onConsistencyCheck(int consistency) {
		return false;
	}

	/**
	 * Creates an string of whitespaces used for indentation.
	 *
	 * @param depth the indentation level
	 * @return a String containing (depth * 2 + 3) * 2 white spaces
	 *
	 * @hide
	 */
	protected static String debugIndent(int depth) {
		StringBuilder spaces = new StringBuilder((depth * 2 + 3) * 2);
		for (int i = 0; i < (depth * 2) + 3; i++) {
			spaces.append(' ').append(' ');
		}
		return spaces.toString();
	}

	/**
	 * <p>Return the offset of the widget's text baseline from the widget's top
	 * boundary. If this widget does not support baseline alignment, this
	 * method returns -1. </p>
	 *
	 * @return the offset of the baseline within the widget's bounds or -1
	 *         if baseline alignment is not supported
	 */
	public int getBaseline() {
		return -1;
	}

	/**
	 * Call this when something has changed which has invalidated the
	 * layout of this view. This will schedule a layout pass of the view
	 * tree.
	 */
	public void requestLayout() {
		//		System.out.println("thisViewID: " + mUIElementID + " requestLayout");
		mPrivateFlags |= FORCE_LAYOUT;

		if (mParent != null && mParent.isLayoutRequested() == false) {
			mParent.requestLayout();
		}
	}

	/**
	 * Forces this view to be laid out during the next layout pass.
	 * This method does not call requestLayout() or forceLayout()
	 * on the parent.
	 */
	public void forceLayout() {
		mPrivateFlags |= FORCE_LAYOUT;
	}

	/**
	 * <p>
	 * This is called to find out how big a view should be. The parent
	 * supplies constraint information in the width and height parameters.
	 * </p>
	 *
	 * <p>
	 * The actual mesurement work of a view is performed in
	 * {@link #onMeasure(int, int)}, called by this method. Therefore, only
	 * {@link #onMeasure(int, int)} can and must be overriden by subclasses.
	 * </p>
	 *
	 *
	 * @param widthMeasureSpec Horizontal space requirements as imposed by the
	 *        parent
	 * @param heightMeasureSpec Vertical space requirements as imposed by the
	 *        parent
	 *
	 * @see #onMeasure(int, int)
	 */
	public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
		if ((mPrivateFlags & FORCE_LAYOUT) == FORCE_LAYOUT
				|| widthMeasureSpec != mOldWidthMeasureSpec
				|| heightMeasureSpec != mOldHeightMeasureSpec) {

			// first clears the measured dimension flag
			mPrivateFlags &= ~MEASURED_DIMENSION_SET;

			// System.out.println("I'm " + this.getUIElementID()
			// + ", my father gives me: width("
			// + (MeasureSpec.getMode(widthMeasureSpec) >> 30) + ", "
			// + MeasureSpec.getSize(widthMeasureSpec) + "), height("
			// + (MeasureSpec.getMode(heightMeasureSpec) >> 30) + ", "
			// + MeasureSpec.getSize(heightMeasureSpec) + ")");
			onMeasure(widthMeasureSpec, heightMeasureSpec);

			// flag not set, setMeasuredDimension() was not invoked, we raise
			// an exception to warn the developer
			if ((mPrivateFlags & MEASURED_DIMENSION_SET) != MEASURED_DIMENSION_SET) {
				throw new IllegalStateException("onMeasure() did not set the"
						+ " measured dimension by calling"
						+ " setMeasuredDimension()");
			}

			mPrivateFlags |= LAYOUT_REQUIRED;
		}

		mOldWidthMeasureSpec = widthMeasureSpec;
		mOldHeightMeasureSpec = heightMeasureSpec;
	}

	/**
	 * <p>
	 * Measure the view and its content to determine the measured width and the
	 * measured height. This method is invoked by {@link #measure(int, int)} and
	 * should be overriden by subclasses to provide accurate and efficient
	 * measurement of their contents.
	 * </p>
	 *
	 * <p>
	 * <strong>CONTRACT:</strong> When overriding this method, you
	 * <em>must</em> call {@link #setMeasuredDimension(int, int)} to store the
	 * measured width and height of this view. Failure to do so will trigger an
	 * <code>IllegalStateException</code>, thrown by
	 * {@link #measure(int, int)}. Calling the superclass'
	 * {@link #onMeasure(int, int)} is a valid use.
	 * </p>
	 *
	 * <p>
	 * The base class implementation of measure defaults to the background size,
	 * unless a larger size is allowed by the MeasureSpec. Subclasses should
	 * override {@link #onMeasure(int, int)} to provide better measurements of
	 * their content.
	 * </p>
	 *
	 * <p>
	 * If this method is overridden, it is the subclass's responsibility to make
	 * sure the measured height and width are at least the view's minimum height
	 * and width ({@link #getSuggestedMinimumHeight()} and
	 * {@link #getSuggestedMinimumWidth()}).
	 * </p>
	 *
	 * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
	 *                         The requirements are encoded with
	 *                         {@link android.view.View.MeasureSpec}.
	 * @param heightMeasureSpec vertical space requirements as imposed by the parent.
	 *                         The requirements are encoded with
	 *                         {@link android.view.View.MeasureSpec}.
	 *
	 * @see #getMeasuredWidth()
	 * @see #getMeasuredHeight()
	 * @see #setMeasuredDimension(int, int)
	 * @see #getSuggestedMinimumHeight()
	 * @see #getSuggestedMinimumWidth()
	 * @see android.view.View.MeasureSpec#getMode(int)
	 * @see android.view.View.MeasureSpec#getSize(int)
	 */
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(
				getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
				getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
	}

	/**
	 * <p>This mehod must be called by {@link #onMeasure(int, int)} to store the
	 * measured width and measured height. Failing to do so will trigger an
	 * exception at measurement time.</p>
	 *
	 * @param measuredWidth the measured width of this view
	 * @param measuredHeight the measured height of this view
	 */
	protected void setMeasuredDimension(int measuredWidth, int measuredHeight) {
		this.basicSetDimension(measuredWidth, measuredHeight);
	}

	protected void basicSetDimension(int measuredWidth, int measuredHeight) {
		mMeasuredWidth = measuredWidth;
		mMeasuredHeight = measuredHeight;

		if (DebugUtils.DEBUG_VIEW_IN_BROWSER) {
            /**
             * @j2sNative
             * thisView = document.getElementById(this.mUIElementID);
             * if(thisView != null && thisView.tagName != "TEXTAREA") {
             *     thisView.style.width = this.mMeasuredWidth + "px";
             *     thisView.style.height = this.mMeasuredHeight + "px";
             * }
             */{}
		}

		mPrivateFlags |= MEASURED_DIMENSION_SET;
	}

	/**
	 * Utility to reconcile a desired size with constraints imposed by a MeasureSpec.
	 * Will take the desired size, unless a different size is imposed by the constraints.
	 *
	 * @param size How big the view wants to be
	 * @param measureSpec Constraints imposed by the parent
	 * @return The size this view should be.
	 */
	public static int resolveSize(int size, int measureSpec) {
		int result = size;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			result = size;
			break;
		case MeasureSpec.AT_MOST:
			result = Math.min(size, specSize);
			break;
		case MeasureSpec.EXACTLY:
			result = specSize;
			break;
		}
		return result;
	}

	/**
	 * Utility to return a default size. Uses the supplied size if the
	 * MeasureSpec imposed no contraints. Will get larger if allowed
	 * by the MeasureSpec.
	 *
	 * @param size Default size for this view
	 * @param measureSpec Constraints imposed by the parent
	 * @return The size this view should be.
	 */
	public static int getDefaultSize(int size, int measureSpec) {
		int result = size;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			result = size;
			break;
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			result = specSize;
			break;
		}
		return result;
	}

	/**
	 * Returns the suggested minimum height that the view should use. This
	 * returns the maximum of the view's minimum height
	 * and the background's minimum height
	 * ({@link android.graphics.drawable.Drawable#getMinimumHeight()}).
	 * <p>
	 * When being used in {@link #onMeasure(int, int)}, the caller should still
	 * ensure the returned height is within the requirements of the parent.
	 *
	 * @return The suggested minimum height of the view.
	 */
	protected int getSuggestedMinimumHeight() {
        int suggestedMinHeight = mMinHeight;

        if (mBGDrawable != null) {
            final int bgMinHeight = mBGDrawable.getMinimumHeight();
            if (suggestedMinHeight < bgMinHeight) {
                suggestedMinHeight = bgMinHeight;
            }
        }

        return suggestedMinHeight;
	}

	/**
	 * Returns the suggested minimum width that the view should use. This
	 * returns the maximum of the view's minimum width)
	 * and the background's minimum width
	 *  ({@link android.graphics.drawable.Drawable#getMinimumWidth()}).
	 * <p>
	 * When being used in {@link #onMeasure(int, int)}, the caller should still
	 * ensure the returned width is within the requirements of the parent.
	 *
	 * @return The suggested minimum width of the view.
	 */
	protected int getSuggestedMinimumWidth() {
        int suggestedMinWidth = mMinWidth;

        if (mBGDrawable != null) {
            final int bgMinWidth = mBGDrawable.getMinimumWidth();
            if (suggestedMinWidth < bgMinWidth) {
                suggestedMinWidth = bgMinWidth;
            }
        }

        return suggestedMinWidth;
	}

	/**
	 * Sets the minimum height of the view. It is not guaranteed the view will
	 * be able to achieve this minimum height (for example, if its parent layout
	 * constrains it with less available height).
	 *
	 * @param minHeight The minimum height the view will try to be.
	 */
	public void setMinimumHeight(int minHeight) {
		mMinHeight = minHeight;
	}

	/**
	 * Sets the minimum width of the view. It is not guaranteed the view will
	 * be able to achieve this minimum width (for example, if its parent layout
	 * constrains it with less available width).
	 *
	 * @param minWidth The minimum width the view will try to be.
	 */
	public void setMinimumWidth(int minWidth) {
		mMinWidth = minWidth;
	}

    /**
     * Get the animation currently associated with this view.
     *
     * @return The animation that is currently playing or
     *         scheduled to play for this view.
     */
    public Animation getAnimation() {
        return mCurrentAnimation;
    }

    /**
     * Start the specified animation now.
     *
     * @param animation the animation to start now
     */
    public void startAnimation(Animation animation) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "View startAnimation at time:" + start);
        animation.setStartTime(Animation.START_ON_FIRST_FRAME);
        setAnimation(animation);
        invalidate();
    }

    /**
     * Cancels any animations for this view.
     */
    public void clearAnimation() {
        if (mCurrentAnimation != null) {
            mCurrentAnimation.detach();
        }
        mCurrentAnimation = null;
    }

    /**
     * Sets the next animation to play for this view.
     * If you want the animation to play immediately, use
     * startAnimation. This method provides allows fine-grained
     * control over the start time and invalidation, but you
     * must make sure that 1) the animation has a start time set, and
     * 2) the view will be invalidated when the animation is supposed to
     * start.
     *
     * @param animation The next animation, or null.
     */
    public void setAnimation(Animation animation) {
        mCurrentAnimation = animation;
        if (animation != null) {
            animation.reset();
        }
    }
    
	/**
	 * Invoked by a parent ViewGroup to notify the start of the animation
	 * currently associated with this view. If you override this method,
	 * always call super.onAnimationStart();
	 *
	 * @see #setAnimation(android.view.animation.Animation)
	 * @see #getAnimation()
	 */
	protected void onAnimationStart() {
		mPrivateFlags |= ANIMATION_STARTED;
	}

	/**
	 * Invoked by a parent ViewGroup to notify the end of the animation
	 * currently associated with this view. If you override this method,
	 * always call super.onAnimationEnd();
	 *
	 * @see #setAnimation(android.view.animation.Animation)
	 * @see #getAnimation()
	 */
	protected void onAnimationEnd() {
		mPrivateFlags &= ~ANIMATION_STARTED;
	}

	/**
	 * Invoked if there is a Transform that involves alpha. Subclass that can
	 * draw themselves with the specified alpha should return true, and then
	 * respect that alpha when their onDraw() is called. If this returns false
	 * then the view may be redirected to draw into an offscreen buffer to
	 * fulfill the request, which will look fine, but may be slower than if the
	 * subclass handles it internally. The default implementation returns false.
	 *
	 * @param alpha The alpha (0..255) to apply to the view's drawing
	 * @return true if the view can draw with the specified alpha.
	 */
	protected boolean onSetAlpha(int alpha) {
		return false;
	}

	/**
	 * Play a sound effect for this view.
	 *
	 * <p>The framework will play sound effects for some built in actions, such as
	 * clicking, but you may wish to play these effects in your widget,
	 * for instance, for internal navigation.
	 *
	 * <p>The sound effect will only be played if sound effects are enabled by the user, and
	 * {@link #isSoundEffectsEnabled()} is true.
	 *
	 * @param soundConstant One of the constants defined in {@link SoundEffectConstants}
	 */
	public void playSoundEffect(int soundConstant) {
		if (mAttachInfo == null || mAttachInfo.mRootCallbacks == null
				|| !isSoundEffectsEnabled()) {
			return;
		}
		mAttachInfo.mRootCallbacks.playSoundEffect(soundConstant);
	}

	/**
	 * This needs to be a better API (NOT ON VIEW) before it is exposed.  If
	 * it is ever exposed at all.
	 * @hide
	 */
	public void onCloseSystemDialogs(String reason) {
	}

    private void postCheckForLongClick(int delayOffset) {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress,
                ViewConfiguration.getLongPressTimeout() - delayOffset);
    }
    
	private static int[] stateSetUnion(final int[] stateSet1,
			final int[] stateSet2) {
		final int stateSet1Length = stateSet1.length;
		final int stateSet2Length = stateSet2.length;
		final int[] newSet = new int[stateSet1Length + stateSet2Length];
		int k = 0;
		int i = 0;
		int j = 0;
		// This is a merge of the two input state sets and assumes that the
		// input sets are sorted by the order imposed by ViewDrawableStates.
		for (int viewState : styleable.ViewDrawableStates) {
			if (i < stateSet1Length && stateSet1[i] == viewState) {
				newSet[k++] = viewState;
				i++;
			} else if (j < stateSet2Length && stateSet2[j] == viewState) {
				newSet[k++] = viewState;
				j++;
			}
			if (k > 1) {
				assert (newSet[k - 1] > newSet[k - 2]);
			}
		}
		return newSet;
	}

	/**
	 * Inflate a view from an XML resource.  This convenience method wraps the {@link
	 * LayoutInflater} class, which provides a full range of options for view inflation.
	 *
	 * @param context The Context object for your activity or application.
	 * @param resource The resource ID to inflate
	 * @param root A view group that will be the parent.  Used to properly inflate the
	 * layout_* parameters.
	 * @see LayoutInflater
	 */
	public static View inflate(Context context, int resource, ViewGroup root) {
		LayoutInflater factory = LayoutInflater.from(context);
		return factory.inflate(resource, root);
	}

	/**
	 * Scroll the view with standard behavior for scrolling beyond the normal
	 * content boundaries. Views that call this method should override
	 * {@link #onOverScrolled(int, int, boolean, boolean)} to respond to the
	 * results of an over-scroll operation.
	 *
	 * Views can use this method to handle any touch or fling-based scrolling.
	 *
	 * @param deltaX Change in X in pixels
	 * @param deltaY Change in Y in pixels
	 * @param scrollX Current X scroll value in pixels before applying deltaX
	 * @param scrollY Current Y scroll value in pixels before applying deltaY
	 * @param scrollRangeX Maximum content scroll range along the X axis
	 * @param scrollRangeY Maximum content scroll range along the Y axis
	 * @param maxOverScrollX Number of pixels to overscroll by in either direction
	 *          along the X axis.
	 * @param maxOverScrollY Number of pixels to overscroll by in either direction
	 *          along the Y axis.
	 * @param isTouchEvent true if this scroll operation is the result of a touch event.
	 * @return true if scrolling was clamped to an over-scroll boundary along either
	 *          axis, false otherwise.
	 */
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
			int scrollY, int scrollRangeX, int scrollRangeY,
			int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		final int overScrollMode = mOverScrollMode;
		final boolean canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
		final boolean canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent();
		final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS
				|| (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
		final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS
				|| (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

		int newScrollX = scrollX + deltaX;
		if (!overScrollHorizontal) {
			maxOverScrollX = 0;
		}

		int newScrollY = scrollY + deltaY;
		if (!overScrollVertical) {
			maxOverScrollY = 0;
		}

		// Clamp values if at the limits and record
		final int left = -maxOverScrollX;
		final int right = maxOverScrollX + scrollRangeX;
		final int top = -maxOverScrollY;
		final int bottom = maxOverScrollY + scrollRangeY;

		boolean clampedX = false;
		if (newScrollX > right) {
			newScrollX = right;
			clampedX = true;
		} else if (newScrollX < left) {
			newScrollX = left;
			clampedX = true;
		}

		boolean clampedY = false;
		if (newScrollY > bottom) {
			newScrollY = bottom;
			clampedY = true;
		} else if (newScrollY < top) {
			newScrollY = top;
			clampedY = true;
		}

		onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

		return clampedX || clampedY;
	}

	/**
	 * Called by {@link #overScrollBy(int, int, int, int, int, int, int, int, boolean)} to
	 * respond to the results of an over-scroll operation.
	 *
	 * @param scrollX New X scroll value in pixels
	 * @param scrollY New Y scroll value in pixels
	 * @param clampedX True if scrollX was clamped to an over-scroll boundary
	 * @param clampedY True if scrollY was clamped to an over-scroll boundary
	 */
	protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
			boolean clampedY) {
		// Intentionally empty.
	}

	/**
	 * Returns the over-scroll mode for this view. The result will be
	 * one of {@link #OVER_SCROLL_ALWAYS} (default), {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
	 * (allow over-scrolling only if the view content is larger than the container),
	 * or {@link #OVER_SCROLL_NEVER}.
	 *
	 * @return This view's over-scroll mode.
	 */
	public int getOverScrollMode() {
		return mOverScrollMode;
	}

	/**
	 * Set the over-scroll mode for this view. Valid over-scroll modes are
	 * {@link #OVER_SCROLL_ALWAYS} (default), {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
	 * (allow over-scrolling only if the view content is larger than the container),
	 * or {@link #OVER_SCROLL_NEVER}.
	 *
	 * Setting the over-scroll mode of a view will have an effect only if the
	 * view is capable of scrolling.
	 *
	 * @param overScrollMode The new over-scroll mode for this view.
	 */
	public void setOverScrollMode(int overScrollMode) {
		if (overScrollMode != OVER_SCROLL_ALWAYS
				&& overScrollMode != OVER_SCROLL_IF_CONTENT_SCROLLS
				&& overScrollMode != OVER_SCROLL_NEVER) {
			throw new IllegalArgumentException("Invalid overscroll mode "
					+ overScrollMode);
		}
		mOverScrollMode = overScrollMode;
	}

	/**
	 * A MeasureSpec encapsulates the layout requirements passed from parent to child.
	 * Each MeasureSpec represents a requirement for either the width or the height.
	 * A MeasureSpec is comprised of a size and a mode. There are three possible
	 * modes:
	 * <dl>
	 * <dt>UNSPECIFIED</dt>
	 * <dd>
	 * The parent has not imposed any constraint on the child. It can be whatever size
	 * it wants.
	 * </dd>
	 *
	 * <dt>EXACTLY</dt>
	 * <dd>
	 * The parent has determined an exact size for the child. The child is going to be
	 * given those bounds regardless of how big it wants to be.
	 * </dd>
	 *
	 * <dt>AT_MOST</dt>
	 * <dd>
	 * The child can be as large as it wants up to the specified size.
	 * </dd>
	 * </dl>
	 *
	 * MeasureSpecs are implemented as ints to reduce object allocation. This class
	 * is provided to pack and unpack the &lt;size, mode&gt; tuple into the int.
	 */
	public static class MeasureSpec {
		private static final int MODE_SHIFT = 30;
		private static final int MODE_MASK = 0x3 << MODE_SHIFT;

		/**
		 * Measure specification mode: The parent has not imposed any constraint
		 * on the child. It can be whatever size it wants.
		 */
		public static final int UNSPECIFIED = 0 << MODE_SHIFT;

		/**
		 * Measure specification mode: The parent has determined an exact size
		 * for the child. The child is going to be given those bounds regardless
		 * of how big it wants to be.
		 */
		public static final int EXACTLY = 1 << MODE_SHIFT;

		/**
		 * Measure specification mode: The child can be as large as it wants up
		 * to the specified size.
		 */
		public static final int AT_MOST = 2 << MODE_SHIFT;

		/**
		 * Creates a measure specification based on the supplied size and mode.
		 *
		 * The mode must always be one of the following:
		 * <ul>
		 *  <li>{@link android.view.View.MeasureSpec#UNSPECIFIED}</li>
		 *  <li>{@link android.view.View.MeasureSpec#EXACTLY}</li>
		 *  <li>{@link android.view.View.MeasureSpec#AT_MOST}</li>
		 * </ul>
		 *
		 * @param size the size of the measure specification
		 * @param mode the mode of the measure specification
		 * @return the measure specification based on size and mode
		 */
		public static int makeMeasureSpec(int size, int mode) {
			return size + mode;
		}

		/**
		 * Extracts the mode from the supplied measure specification.
		 *
		 * @param measureSpec the measure specification to extract the mode from
		 * @return {@link android.view.View.MeasureSpec#UNSPECIFIED},
		 *         {@link android.view.View.MeasureSpec#AT_MOST} or
		 *         {@link android.view.View.MeasureSpec#EXACTLY}
		 */
		public static int getMode(int measureSpec) {
			return (measureSpec & MODE_MASK);
		}

		/**
		 * Extracts the size from the supplied measure specification.
		 *
		 * @param measureSpec the measure specification to extract the size from
		 * @return the size in pixels defined in the supplied measure specification
		 */
		public static int getSize(int measureSpec) {
			return (measureSpec & ~MODE_MASK);
		}

		/**
		 * Returns a String representation of the specified measure
		 * specification.
		 *
		 * @param measureSpec the measure specification to convert to a String
		 * @return a String with the following format: "MeasureSpec: MODE SIZE"
		 */
		public static String toString(int measureSpec) {
			int mode = getMode(measureSpec);
			int size = getSize(measureSpec);

			StringBuilder sb = new StringBuilder("MeasureSpec: ");

			if (mode == UNSPECIFIED)
				sb.append("UNSPECIFIED ");
			else if (mode == EXACTLY)
				sb.append("EXACTLY ");
			else if (mode == AT_MOST)
				sb.append("AT_MOST ");
			else
				sb.append(mode).append(" ");

			sb.append(size);
			return sb.toString();
		}
	}

    class CheckForLongPress implements Runnable {

        private int mOriginalWindowAttachCount;

        public void run() {
            if (isPressed() && (mParent != null)
                    && mOriginalWindowAttachCount == mWindowAttachCount) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = mWindowAttachCount;
        }
    }
    
    private final class CheckForTap implements Runnable {
        public void run() {
            mPrivateFlags &= ~PREPRESSED;
            //mPrivateFlags |= PRESSED;
            refreshDrawableState();
            if ((mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE) {
                postCheckForLongClick(ViewConfiguration.getTapTimeout());
            }
        }
    }
    private final class PerformClick implements Runnable {
        public void run() {
            performClick();
        }
    }
    
	/**
	 * Interface definition for a callback to be invoked when a key event is
	 * dispatched to this view. The callback will be invoked before the key
	 * event is given to the view.
	 */
	public interface OnKeyListener {
		/**
		 * Called when a key is dispatched to a view. This allows listeners to
		 * get a chance to respond before the target view.
		 *
		 * @param v The view the key has been dispatched to.
		 * @param keyCode The code for the physical key that was pressed
		 * @param event The KeyEvent object containing full information about
		 *        the event.
		 * @return True if the listener has consumed the event, false otherwise.
		 */
		boolean onKey(View v, int keyCode, KeyEvent event);
	}

	/**
	 * Interface definition for a callback to be invoked when a view has been clicked and held.
	 */
	public interface OnLongClickListener {
		/**
		 * Called when a view has been clicked and held.
		 *
		 * @param v The view that was clicked and held.
		 *
		 * return True if the callback consumed the long click, false otherwise
		 */
		boolean onLongClick(View v);
	}

	/**
	 * Interface definition for a callback to be invoked when the focus state of
	 * a view changed.
	 */
	public interface OnFocusChangeListener {
		/**
		 * Called when the focus state of a view has changed.
		 *
		 * @param v The view whose state has changed.
		 * @param hasFocus The new focus state of v.
		 */
		void onFocusChange(View v, boolean hasFocus);
	}

	/**
	 * Interface definition for a callback to be invoked when a view is clicked.
	 */
	public interface OnClickListener {
		/**
		 * Called when a view has been clicked.
		 *
		 * @param v The view that was clicked.
		 */
		void onClick(View v);
	}

    /**
     * Interface definition for a callback to be invoked when a touch event is
     * dispatched to this view. The callback will be invoked before the touch
     * event is given to the view.
     */
    public interface OnTouchListener {
        /**
         * Called when a touch event is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * @param v The view the touch event has been dispatched to.
         * @param event The MotionEvent object containing full information about
         *        the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onTouch(View v, MotionEvent event);
    }

    /**
     * <p>ScrollabilityCache holds various fields used by a View when scrolling
     * is supported. This avoids keeping too many unused fields in most
     * instances of View.</p>
     */
    private static class ScrollabilityCache implements Runnable {

        /**
         * Scrollbars are not visible
         */
        public static final int OFF = 0;

        /**
         * Scrollbars are visible
         */
        public static final int ON = 1;

        /**
         * Scrollbars are fading away
         */
        public static final int FADING = 2;

        public boolean fadeScrollBars;

        public int fadingEdgeLength;
        public int scrollBarDefaultDelayBeforeFade;
        public int scrollBarFadeDuration;

        public int scrollBarSize;
        public ScrollBarDrawable scrollBar;
        public float[] interpolatorValues;
        public View host;

        public final Paint paint;
        public final Matrix matrix;
        public Shader shader;

        public final Interpolator scrollBarInterpolator = new Interpolator(1, 2);

        private final float[] mOpaque = {255.0f};
        private final float[] mTransparent = {0.0f};

        /**
         * When fading should start. This time moves into the future every time
         * a new scroll happens. Measured based on SystemClock.uptimeMillis()
         */
        public long fadeStartTime;


        /**
         * The current state of the scrollbars: ON, OFF, or FADING
         */
        public int state = OFF;

        private int mLastColor;

        public ScrollabilityCache(ViewConfiguration configuration, View host) {
            fadingEdgeLength = configuration.getScaledFadingEdgeLength();
            scrollBarSize = configuration.getScaledScrollBarSize();
            scrollBarDefaultDelayBeforeFade = ViewConfiguration.getScrollDefaultDelay();
            scrollBarFadeDuration = ViewConfiguration.getScrollBarFadeDuration();

            paint = new Paint();
            matrix = new Matrix();
            // use use a height of 1, and then wack the matrix each time we
            // actually use it.
            shader = new LinearGradient(0, 0, 0, 1, 0xFF000000, 0, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            this.host = host;
        }

        public void setFadeColor(int color) {
            if (color != 0 && color != mLastColor) {
                mLastColor = color;
                color |= 0xFF000000;

                shader = new LinearGradient(0, 0, 0, 1, color | 0xFF000000,
                        color & 0x00FFFFFF, Shader.TileMode.CLAMP);

                paint.setShader(shader);
                // Restore the default transfer mode (src_over)
                //paint.setXfermode(null);
            }
        }

        public void run() {
            long now = AnimationUtils.currentAnimationTimeMillis();
            if (now >= fadeStartTime) {

                // the animation fades the scrollbars out by changing
                // the opacity (alpha) from fully opaque to fully
                // transparent
                int nextFrame = (int) now;
                int framesCount = 0;

                Interpolator interpolator = scrollBarInterpolator;

                // Start opaque
                interpolator.setKeyFrame(framesCount++, nextFrame, mOpaque);

                // End transparent
                nextFrame += scrollBarFadeDuration;
                interpolator.setKeyFrame(framesCount, nextFrame, mTransparent);

                state = FADING;

                // Kick off the fade animation
                host.invalidate();
            }
        }

    }

    /**
     * A set of information given to a view when it is attached to its parent
     * window.
     */
    public static class AttachInfo {
		interface Callbacks {
			void playSoundEffect(int effectId);

			boolean performHapticFeedback(int effectId, boolean always);
		}

        /**
         * InvalidateInfo is used to post invalidate(int, int, int, int) messages
         * to a Handler. This class contains the target (View) to invalidate and
         * the coordinates of the dirty rectangle.
         *
         * For performance purposes, this class also implements a pool of up to
         * POOL_LIMIT objects that get reused. This reduces memory allocations
         * whenever possible.
         */
        static class InvalidateInfo /*implements Poolable<InvalidateInfo> */{
            private static final int POOL_LIMIT = 10;
//            private static final Pool<InvalidateInfo> sPool = Pools.synchronizedPool(
//                    Pools.finitePool(new PoolableManager<InvalidateInfo>() {
//                        public InvalidateInfo newInstance() {
//                            return new InvalidateInfo();
//                        }
//
//                        public void onAcquired(InvalidateInfo element) {
//                        }
//
//                        public void onReleased(InvalidateInfo element) {
//                        }
//                    }, POOL_LIMIT)
//            );

            private InvalidateInfo mNext;

            View target;

            int left;
            int top;
            int right;
            int bottom;

            public void setNextPoolable(InvalidateInfo element) {
                mNext = element;
            }

            public InvalidateInfo getNextPoolable() {
                return mNext;
            }

            static InvalidateInfo acquire() {
                //return sPool.acquire();
                return new InvalidateInfo();
            }

            void release() {
                //sPool.release(this);
            }
        }
        final WindowSession mSession;
		final Window mWindow;

		final Callbacks mRootCallbacks;

        final IBinder mWindowToken;

		/**
		 * The top view of the hierarchy.
		 */
		View mRootView;

		/**
		 * Scale factor used by the compatibility mode
		 */
		float mApplicationScale;

		/**
		 * Indicates whether the application is in compatibility mode
		 */
		boolean mScalingRequired;

		/**
		 * Left position of this view's window
		 */
		int mWindowLeft;

		/**
		 * Top position of this view's window
		 */
		int mWindowTop;

		/**
		 * Indicates whether views need to use 32-bit drawing caches
		 */
		boolean mUse32BitDrawingCache;

		/**
		 * For windows that are full-screen but using insets to layout inside
		 * of the screen decorations, these are the current insets for the
		 * content of the window.
		 */
		final Rect mContentInsets = new Rect();

		/**
		 * For windows that are full-screen but using insets to layout inside
		 * of the screen decorations, these are the current insets for the
		 * actual visible parts of the window.
		 */
		final Rect mVisibleInsets = new Rect();

		/**
		 * The internal insets given by this window.  This value is
		 * supplied by the client (through
		 * {@link ViewTreeObserver.OnComputeInternalInsetsListener}) and will
		 * be given to the window manager when changed to be used in laying
		 * out windows behind it.
		 */
		final ViewTreeObserver.InternalInsetsInfo mGivenInternalInsets = new ViewTreeObserver.InternalInsetsInfo();

		/**
		 * All views in the window's hierarchy that serve as scroll containers,
		 * used to determine if the window can be resized or must be panned
		 * to adjust for a soft input area.
		 */
		final ArrayList<View> mScrollContainers = new ArrayList<View>();

		final KeyEvent.DispatcherState mKeyDispatchState = new KeyEvent.DispatcherState();

		/**
		 * Indicates whether the view's window currently has the focus.
		 */
		boolean mHasWindowFocus;

		/**
		 * The current visibility of the window.
		 */
		int mWindowVisibility;

		/**
		 * Indicates the time at which drawing started to occur.
		 */
		long mDrawingTime;

		/**
		 * Indicates whether or not ignoring the DIRTY_MASK flags.
		 */
		boolean mIgnoreDirtyState;

		/**
		 * Indicates whether the view's window is currently in touch mode.
		 */
		boolean mInTouchMode;

		/**
		 * Indicates that ViewRoot should trigger a global layout change
		 * the next time it performs a traversal
		 */
		boolean mRecomputeGlobalAttributes;

		/**
		 * Set during a traveral if any views want to keep the screen on.
		 */
		boolean mKeepScreenOn;

		/**
		 * Set if the visibility of any views has changed.
		 */
		boolean mViewVisibilityChanged;

		/**
		 * Set to true if a view has been scrolled.
		 */
		boolean mViewScrollChanged;

		/**
		 * Global to the view hierarchy used as a temporary for dealing with
		 * x/y points in the transparent region computations.
		 */
		final int[] mTransparentLocation = new int[2];

		/**
		 * Global to the view hierarchy used as a temporary for dealing with
		 * x/y points in the ViewGroup.invalidateChild implementation.
		 */
		final int[] mInvalidateChildLocation = new int[2];

		/**
		 * The view tree observer used to dispatch global events like
		 * layout, pre-draw, touch mode change, etc.
		 */
		final ViewTreeObserver mTreeObserver = new ViewTreeObserver();

		/**
		 * A Canvas used by the view hierarchy to perform bitmap caching.
		 */
		Canvas mCanvas;

		/**
		 * A Handler supplied by a view's {@link android.view.ViewRoot}. This
		 * handler can be used to pump events in the UI events queue.
		 */
		final Handler mHandler;

		/**
		 * Identifier for messages requesting the view to be invalidated.
		 * Such messages should be sent to {@link #mHandler}.
		 */
		static final int INVALIDATE_MSG = 0x1;

		/**
		 * Identifier for messages requesting the view to invalidate a region.
		 * Such messages should be sent to {@link #mHandler}.
		 */
		static final int INVALIDATE_RECT_MSG = 0x2;

		/**
		 * Temporary for use in computing invalidate rectangles while
		 * calling up the hierarchy.
		 */
		final Rect mTmpInvalRect = new Rect();

		/**
		 * Temporary list for use in collecting focusable descendents of a view.
		 */
		final ArrayList<View> mFocusablesTempList = new ArrayList<View>(24);

        /**
         * Creates a new set of attachment information with the specified events
         * handler and thread.
         * 
         * @param handler the events handler the view must use
         */
        AttachInfo(WindowSession session, Window window, Handler handler, Callbacks effectPlayer) {
            mSession = session;
            mWindow = window;
            mHandler = handler;
            mWindowToken = (IBinder) window;
            mRootCallbacks = effectPlayer;
        }

        public ArrayList<View> getScrollContainers() {
            return mScrollContainers;
        }
    }

	// ElementID is different from android id,
	// every view has a unique elementid
	private static int sUIElementsID = 0;
	private int mUIElementID;

	public int getUIElementID() {
		return mUIElementID;
	}

	// Temporarily I assume user applications can not have IntMax
	// number of UIElement, so I just ++
	private static int getAnElementID() {
		/**
		 @j2sNative
		 * if (null == android.view.View.sUIElementsID)
		 * {
		 *     android.view.View.sUIElementsID = 0;
		 * }
		 */
		{
		}
		return sUIElementsID++;
	}

	public View findViewByElementId(int viewElementId) {
		if (viewElementId < 0) {
			return null;
		}
		return findElementViewTraversal(viewElementId);
	}

	protected View findElementViewTraversal(int viewElementId) {
		if (viewElementId == mUIElementID) {
			return this;
		}
		return null;
	}

    public void destroy() {
        if (DebugUtils.DEBUG_VIEW_IN_BROWSER) {
            /**
             * @j2sNative 
             * var thisView = document.getElementById(this.mUIElementID); 
             * if (thisView != null && thisView.parentNode != null) {
             *     thisView.parentNode.removeChild(thisView); 
             * }
             */{}
        }
    }

	/**
	 * Called by draw to draw the child views. This may be overridden
	 * by derived classes to gain control just before its children are drawn
	 * (but after its own view has been drawn).
	 * @param canvas the canvas on which to draw the view
	 */
	protected void dispatchDraw(Canvas canvas) {
	}

    protected boolean verifyDrawable(Drawable who) {
        return who == mBGDrawable;
    }

	@Override
	public void invalidateDrawable(Drawable who) {
        if (verifyDrawable(who)) {
            final Rect dirty = who.getBounds();
            final int scrollX = mScrollX;
            final int scrollY = mScrollY;

            invalidate(dirty.left + scrollX, dirty.top + scrollY,
                    dirty.right + scrollX, dirty.bottom + scrollY);
        }
	}

	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (verifyDrawable(who) && what != null) {
            if (mAttachInfo != null) {
                mAttachInfo.mHandler.postAtTime(what, who, when);
            }
        }
	}

	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
        if (verifyDrawable(who) && what != null) {
            if (mAttachInfo != null) {
                mAttachInfo.mHandler.removeCallbacks(what, who);
            }
        }
	}

    public void unscheduleDrawable(Drawable who) {
        if (mAttachInfo != null) {
            mAttachInfo.mHandler.removeCallbacksAndMessages(who);
        }
    }
    
    /**
     * This function is called whenever the state of the view changes in such
     * a way that it impacts the state of drawables being shown.
     *
     * <p>Be sure to call through to the superclass when overriding this
     * function.
     *
     * @see Drawable#setState
     */
    protected void drawableStateChanged() {
        Drawable d = mBGDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    /**
     * Set the scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param x the x position to scroll to
     * @param y the y position to scroll to
     */
    public void scrollTo(int x, int y) {
        if (mScrollX != x || mScrollY != y) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = x;
            mScrollY = y;
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (!awakenScrollBars()) {
                invalidate();
            }
        }
    }

    /**
     * Move the scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param x the amount of pixels to scroll by horizontally
     * @param y the amount of pixels to scroll by vertically
     */
    public void scrollBy(int x, int y) {
        scrollTo(mScrollX + x, mScrollY + y);
    }

    /**
     * Causes the Runnable to be added to the message queue.
     * The runnable will be run on the user interface thread.
     *
     * @param action The Runnable that will be executed.
     *
     * @return Returns true if the Runnable was successfully placed in to the
     *         message queue.  Returns false on failure, usually because the
     *         looper processing the message queue is exiting.
     */
    public boolean post(Runnable action) {
        Handler handler;
        if (mAttachInfo != null) {
            handler = mAttachInfo.mHandler;
        } else {
            // Assume that post will succeed later
            ViewRoot.getRunQueue().post(action);
            return true;
        }

        return handler.post(action);
    }

    /**
     * Causes the Runnable to be added to the message queue, to be run
     * after the specified amount of time elapses.
     * The runnable will be run on the user interface thread.
     *
     * @param action The Runnable that will be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *        will be executed.
     *
     * @return true if the Runnable was successfully placed in to the
     *         message queue.  Returns false on failure, usually because the
     *         looper processing the message queue is exiting.  Note that a
     *         result of true does not mean the Runnable will be processed --
     *         if the looper is quit before the delivery time of the message
     *         occurs then the message will be dropped.
     */
    public boolean postDelayed(Runnable action, long delayMillis) {
        Handler handler;
        if (mAttachInfo != null) {
            handler = mAttachInfo.mHandler;
        } else {
            // Assume that post will succeed later
            ViewRoot.getRunQueue().postDelayed(action, delayMillis);
            return true;
        }

        return handler.postDelayed(action, delayMillis);
    }
    
    /**
     * Cause an invalidate to happen on a subsequent cycle through the event loop.
     * Use this to invalidate the View from a non-UI thread.
     *
     * @see #invalidate()
     */
    public void postInvalidate() {
        postInvalidateDelayed(0);
    }

    /**
     * Cause an invalidate of the specified area to happen on a subsequent cycle
     * through the event loop. Use this to invalidate the View from a non-UI thread.
     *
     * @param left The left coordinate of the rectangle to invalidate.
     * @param top The top coordinate of the rectangle to invalidate.
     * @param right The right coordinate of the rectangle to invalidate.
     * @param bottom The bottom coordinate of the rectangle to invalidate.
     *
     * @see #invalidate(int, int, int, int)
     * @see #invalidate(Rect)
     */
    public void postInvalidate(int left, int top, int right, int bottom) {
        postInvalidateDelayed(0, left, top, right, bottom);
    }

    /**
     * Cause an invalidate to happen on a subsequent cycle through the event
     * loop. Waits for the specified amount of time.
     *
     * @param delayMilliseconds the duration in milliseconds to delay the
     *         invalidation by
     */
    public void postInvalidateDelayed(long delayMilliseconds) {
        // We try only with the AttachInfo because there's no point in invalidating
        // if we are not attached to our window
        if (mAttachInfo != null) {
            Message msg = Message.obtain();
            msg.what = AttachInfo.INVALIDATE_MSG;
            msg.obj = this;
            mAttachInfo.mHandler.sendMessageDelayed(msg, delayMilliseconds);
        }
    }

    /**
     * Cause an invalidate of the specified area to happen on a subsequent cycle
     * through the event loop. Waits for the specified amount of time.
     *
     * @param delayMilliseconds the duration in milliseconds to delay the
     *         invalidation by
     * @param left The left coordinate of the rectangle to invalidate.
     * @param top The top coordinate of the rectangle to invalidate.
     * @param right The right coordinate of the rectangle to invalidate.
     * @param bottom The bottom coordinate of the rectangle to invalidate.
     */
    public void postInvalidateDelayed(long delayMilliseconds, int left, int top,
            int right, int bottom) {

        // We try only with the AttachInfo because there's no point in invalidating
        // if we are not attached to our window
        if (mAttachInfo != null) {
            final AttachInfo.InvalidateInfo info = AttachInfo.InvalidateInfo.acquire();
            info.target = this;
            info.left = left;
            info.top = top;
            info.right = right;
            info.bottom = bottom;

            final Message msg = Message.obtain();
            msg.what = AttachInfo.INVALIDATE_RECT_MSG;
            msg.obj = info;
            mAttachInfo.mHandler.sendMessageDelayed(msg, delayMilliseconds);
        }
    }

    public void getWindowVisibleDisplayFrame(Rect outRect) {
        if (mAttachInfo != null) {
            mAttachInfo.mSession.getDisplayFrame(mAttachInfo.mWindow,
                    outRect);
            // XXX This is really broken, and probably all needs to be done
            // in the window manager, and we need to know more about whether
            // we want the area behind or in front of the IME.
            final Rect insets = mAttachInfo.mVisibleInsets;
            outRect.left += insets.left;
            outRect.top += insets.top;
            outRect.right -= insets.right;
            outRect.bottom -= insets.bottom;
            return;
        }
        Display d = WindowManagerImpl.getDefault().getDefaultDisplay();
        outRect.set(0, 0, d.getWidth(), d.getHeight());
    }

    /**
     * Retrieve a unique token identifying the window this view is attached to.
     * @return Return the window's token for use in
     * {@link WindowManager.LayoutParams#token WindowManager.LayoutParams.token}.
     */
    public IBinder getWindowToken() {
        return mAttachInfo != null ? mAttachInfo.mWindowToken : null;
    }

    /**
     * Show the context menu for this view. It is not safe to hold on to the
     * menu after returning from this method.
     *
     * You should normally not overload this method. Overload
     * {@link #onCreateContextMenu(ContextMenu)} or define an
     * {@link OnCreateContextMenuListener} to add items to the context menu.
     *
     * @param menu The context menu to populate
     */
    public void createContextMenu(ContextMenu menu) {
//        ContextMenuInfo menuInfo = getContextMenuInfo();
//
//        // Sets the current menu info so all items added to menu will have
//        // my extra info set.
//        ((MenuBuilder)menu).setCurrentMenuInfo(menuInfo);
//
//        onCreateContextMenu(menu);
//        if (mOnCreateContextMenuListener != null) {
//            mOnCreateContextMenuListener.onCreateContextMenu(menu, this, menuInfo);
//        }
//
//        // Clear the extra information so subsequent items that aren't mine don't
//        // have my extra info.
//        ((MenuBuilder)menu).setCurrentMenuInfo(null);
//
//        if (mParent != null) {
//            mParent.createContextMenu(menu);
//        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: onCreateContextMenu");
     */
    @MayloonStubAnnotation()
    void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        System.out.println("Stub" + " Function : onCreateContextMenu");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: destroyDrawingCache");
     */
    @MayloonStubAnnotation()
    public void destroyDrawingCache() {
        System.out.println("Stub" + " Function : destroyDrawingCache");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: cancelLongPress");
     */
    @MayloonStubAnnotation()
    public void cancelLongPress() {
        System.out.println("Stub" + " Function : cancelLongPress");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: onFilterTouchEventForSecurity");
     */
    @MayloonStubAnnotation()
    public boolean onFilterTouchEventForSecurity(MotionEvent event) {
        System.out.println("Stub" + " Function : onFilterTouchEventForSecurity");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: performHapticFeedback");
     */
    @MayloonStubAnnotation()
    public boolean performHapticFeedback(int feedbackConstant) {
        System.out.println("Stub" + " Function : performHapticFeedback");
        return true;
    }

    /**
     * Define whether scrollbars will fade when the view is not scrolling.
     * 
     * @param fadeScrollbars wheter to enable fading
     * 
     */
    public void setScrollbarFadingEnabled(boolean fadeScrollbars) {
        initScrollCache();
        final ScrollabilityCache scrollabilityCache = mScrollCache;
        // MayLoon: In MayLoon, we current only support no fading because fading
        // effect is complex and will need much render computation, may not be smooth.
        fadeScrollbars = false;
        scrollabilityCache.fadeScrollBars = fadeScrollbars;
        if (fadeScrollbars) {
            scrollabilityCache.state = ScrollabilityCache.OFF;
        } else {
            scrollabilityCache.state = ScrollabilityCache.ON;
        }
    }
    
    /**
     * 
     * Returns true if scrollbars will fade when this view is not scrolling
     * 
     * @return true if scrollbar fading is enabled
     */
    public boolean isScrollbarFadingEnabled() {
        return mScrollCache != null && mScrollCache.fadeScrollBars; 
    }

    /**
     * @j2sNative
     * console.log("Missing method: onTrackballEvent");
     */
    @MayloonStubAnnotation()
    public boolean onTrackballEvent(MotionEvent event) {
        System.out.println("Stub" + " Function : onTrackballEvent");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: performHapticFeedback");
     */
    @MayloonStubAnnotation()
    public boolean performHapticFeedback(int feedbackConstant, int flags) {
        System.out.println("Stub" + " Function : performHapticFeedback");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getWindowVisibility");
     */
    @MayloonStubAnnotation()
    public int getWindowVisibility() {
        System.out.println("Stub" + " Function : getWindowVisibility");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: buildDrawingCache");
     */
    @MayloonStubAnnotation()
    public void buildDrawingCache() {
        System.out.println("Stub" + " Function : buildDrawingCache");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: dispatchTrackballEvent");
     */
    @MayloonStubAnnotation()
    public boolean dispatchTrackballEvent(MotionEvent event) {
        System.out.println("Stub" + " Function : dispatchTrackballEvent");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: performHapticFeedback");
     */
    @MayloonStubAnnotation()
    boolean performHapticFeedback(int effectId, boolean always) {
        System.out.println("Stub" + " Function : performHapticFeedback");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: buildDrawingCache");
     */
    @MayloonStubAnnotation()
    public void buildDrawingCache(boolean autoScale) {
        System.out.println("Stub" + " Function : buildDrawingCache");
        return;
    }
}
