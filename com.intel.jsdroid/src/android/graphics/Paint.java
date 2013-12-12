package android.graphics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.GraphicsOperations;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;

import com.intel.mpt.annotation.MayloonStubAnnotation;

/**
 * In HTML5, there is no separate object to store those drawing settings. Instead, they are
 * stored together with the canvasContext object. So we have to create a Canvas element for measureText to
 * populate those settings, and also do populate settings when this Paint is referenced by some other canvas
 * @author qzhang8
 *
 */
public class Paint {

    private Style mStyle;
    private int mColor;
    private Shader      mShader;
    private ShaderType  mShaderType;
    private Xfermode    mXfermode;
    private float mStrokeWidth;
    private Cap mStrokeCap;
    private float mStrokeMiter;
    private Join mStrokeJoin;
    private Align mTextAlign;
    private float mTextSize;
    private float mTextSkewX = 0;
    private float mTextScaleX = (float) 1.0;
	private boolean mHasCompatScaling;
	private float mCompatScaling;
	private float mInvCompatScaling;

	private int mFlags;
	private Typeface mTypeface;

	/** bit mask for the flag enabling antialiasing */
	public static final int ANTI_ALIAS_FLAG = 0x01;
	/** bit mask for the flag enabling bitmap filtering */
	public static final int FILTER_BITMAP_FLAG = 0x02;
	/** bit mask for the flag enabling dithering */
	public static final int DITHER_FLAG = 0x04;
	/** bit mask for the flag enabling underline text */
	public static final int UNDERLINE_TEXT_FLAG = 0x08;
	/** bit mask for the flag enabling strike-thru text */
	public static final int STRIKE_THRU_TEXT_FLAG = 0x10;
	/** bit mask for the flag enabling fake-bold text */
	public static final int FAKE_BOLD_TEXT_FLAG = 0x20;
	/** bit mask for the flag enabling linear-text (no caching) */
	public static final int LINEAR_TEXT_FLAG = 0x40;
	/** bit mask for the flag enabling subpixel-text */
	public static final int SUBPIXEL_TEXT_FLAG = 0x80;
	/** bit mask for the flag enabling device kerning for text */
	public static final int DEV_KERN_TEXT_FLAG = 0x100;

	// we use this when we first create a paint
	private static final int DEFAULT_PAINT_FLAGS = DEV_KERN_TEXT_FLAG;

    public int top = 0;
    public int ascent = 0;
    public int descent = 0;
    public int bottom = 0;
    public int leading = 0;

    /**
     * Align specifies how drawText aligns its text relative to the
     * [x,y] coordinates. The default is LEFT.
     */
    public static class Align {
        /**
         * The text is drawn to the right of the x,y origin
         */
        public static final Align LEFT = new Align(0);
        /**
         * The text is drawn centered horizontally on the x,y origin
         */
        public static final Align CENTER = new Align(1);
        /**
         * The text is drawn to the left of the x,y origin
         */
        public static final Align RIGHT = new Align(2);

        Align(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;

        public static Align valueOf(String value) {
            if ("LEFT".equals(value)) {
                return Align.LEFT;
            } else if ("CENTER".equals(value)) {
                return Align.CENTER;
            } else if ("RIGHT".equals(value)) {
                return Align.RIGHT;
            } else {
                return null;
            }
        }

        public static Align[] values() {
            Align[] align = new Align[3];
            align[0] = Align.LEFT;
            align[1] = Align.CENTER;
            align[2] = Align.RIGHT;
            return align;
        }
    }

    /**
     * The Join specifies the treatment where lines and curve segments
     * join on a stroked path. The default is MITER.
     */
    public static class Join {
        /**
         * The outer edges of a join meet at a sharp angle
         */
        public static final Join MITER = new Join(0);
        /**
         * The outer edges of a join meet in a circular arc.
         */
        public static final Join ROUND = new Join(1);
        /**
         * The outer edges of a join meet with a straight line
         */
        public static final Join BEVEL = new Join(2);

        private Join(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;

        public static Join valueOf(String value) {
            if ("MITER".equals(value)) {
                return Join.MITER;
            } else if ("ROUND".equals(value)) {
                return Join.ROUND;
            } else if ("BEVEL".equals(value)) {
                return Join.BEVEL;
            } else {
                return null;
            }
        }

        public static Join[] values() {
            Join[] join = new Join[3];
            join[0] = Join.MITER;
            join[1] = Join.ROUND;
            join[2] = Join.BEVEL;
            return join;
        }
    }

    /**
     * The Cap specifies the treatment for the beginning and ending of
     * stroked lines and paths. The default is BUTT.
     */
    public static class Cap {
        /**
         * The stroke ends with the path, and does not project beyond it.
         */
        public static final Cap BUTT = new Cap(0);
        /**
         * The stroke projects out as a semicircle, with the center at the
         * end of the path.
         */
        public static final Cap ROUND = new Cap(1);
        /**
         * The stroke projects out as a square, with the center at the end
         * of the path.
         */
        public static final Cap SQUARE = new Cap(2);

        private Cap(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;

        public static Cap valueOf(String value) {
            if ("BUTT".equals(value)) {
                return Cap.BUTT;
            } else if ("ROUND".equals(value)) {
                return Cap.ROUND;
            } else if ("SQUARE".equals(value)) {
                return Cap.SQUARE;
            } else {
                return null;
            }
        }

        public static Cap[] values() {
            Cap[] cap = new Cap[3];
            cap[0] = Cap.BUTT;
            cap[1] = Cap.ROUND;
            cap[2] = Cap.SQUARE;
            return cap;
        }
    }

//	private static final int[] sStyleArray = { FILL, STROKE, FILL_AND_STROKE };
//	private static final int[] sCapArray = { BUTT, ROUND, SQUARE };
//	private static final int[] sJoinArray = { MITER, ROUND, BEVEL };
//	private static final int[] sAlignArray = { LEFT, CENTER, RIGHT };

    public enum ShaderType {
        BITMAPSHADER   (0),
        COMPOSESHADER  (1),
        LINEARGRADIENT (2), 
        RADIALGRADIENT (3), 
        SWEEPGRADIENT  (4);
        
        ShaderType(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }
	
    /**
     * Create a new paint with default settings.
     */
    public Paint() {
        this(0);   
    }
    
	/**
	 * Create a new paint with the specified flags. Use setFlags() to change
	 * these after the paint is created.
	 *
	 * @param flags initial flag bits, as if they were passed via setFlags().
	 */
	public Paint(int flags) {
		setFlags(flags | DEFAULT_PAINT_FLAGS);
		mCompatScaling = mInvCompatScaling = 1;
		
        String hashCode = String.valueOf(hashCode());

        /**
         * @j2sNative
         * var _canvas = document.getElementById(hashCode);
         * if (_canvas == null) {
         *   _canvas = document.createElement("canvas");
         *   _canvas.id = hashCode;
         *   document.body.appendChild(_canvas);
         * }
         */
        {
        }
        
		init();
	}

	/**
	 * Create a new paint, initialized with the attributes in the specified
	 * paint parameter.
	 *
	 * @param paint Existing paint used to initialized the attributes of the
	 *              new paint.
	 */
	public Paint(Paint paint) {
		this(0);
		mHasCompatScaling = paint.mHasCompatScaling;
		mCompatScaling = paint.mCompatScaling;
		mInvCompatScaling = paint.mInvCompatScaling;
	}

	/** Restores the paint to its default settings. */
	public void reset() {
	    init();
		setFlags(DEFAULT_PAINT_FLAGS);
		mHasCompatScaling = false;
		mCompatScaling = mInvCompatScaling = 1;
	}

	/**
	 * Copy the fields from src into this paint. This is equivalent to calling
	 * get() on all of the src fields, and calling the corresponding set()
	 * methods on this.
	 */
	public void set(Paint src) {
		if (this != src) {
			// copy over the native settings
			mHasCompatScaling = src.mHasCompatScaling;
			mCompatScaling = src.mCompatScaling;
			mInvCompatScaling = src.mInvCompatScaling;
			mTypeface = src.mTypeface;
			
	        mTextSize = src.mTextSize;
	        // call this function to calc the HTML5 font
	        setTextSize(mTextSize);      
	        mTextAlign = src.mTextAlign;
	        mTextScaleX = src.mTextScaleX;
	        mTextSkewX = src.mTextSkewX;

	        mStrokeWidth = src.mStrokeWidth;
	        mColor = src.mColor;
	        mStyle = src.mStyle;
	        mStrokeCap = src.mStrokeCap;
	        mStrokeJoin = src.mStrokeJoin;
	        mStrokeMiter = src.mStrokeMiter;  
	        mXfermode = src.mXfermode;
	        mShader = src.mShader;
	        mShaderType = src.mShaderType;
	        mFlags = src.mFlags;
		}
	}
	
	private void init() {
        // default set for Android
        mTextSize = 12;
        // call this function to calc the HTML5 font
        setTextSize(mTextSize);
        mTextAlign = Align.LEFT;
        mTextScaleX = 1.0f;
        mTextSkewX = 0.0f;

        mStrokeWidth = 1.0f;
        mColor = 0xFF000000; // black
        mStyle = Style.FILL;
        mStrokeCap = Cap.BUTT;
        mStrokeJoin = Join.MITER;
        mStrokeMiter = 4.0f; 
        mShader = null;
        mXfermode = null;
	}

	/** @hide */
	public void setCompatibilityScaling(float factor) {
		if (factor == 1.0) {
			mHasCompatScaling = false;
			mCompatScaling = mInvCompatScaling = 1.0f;
		} else {
			mHasCompatScaling = true;
			mCompatScaling = factor;
			mInvCompatScaling = 1.0f / factor;
		}
	}

	  /**
     * The Style specifies if the primitive being drawn is filled,
     * stroked, or both (in the same color). The default is FILL.
     */
    public static class Style {
        /**
         * Geometry and text drawn with this style will be filled, ignoring all
         * stroke-related settings in the paint.
         */
        public static final Style FILL = new Style(0);
        /**
         * Geometry and text drawn with this style will be stroked, respecting
         * the stroke-related fields on the paint.
         */
        public static final Style STROKE = new Style(1);
        /**
         * Geometry and text drawn with this style will be both filled and
         * stroked at the same time, respecting the stroke-related fields on
         * the paint.
         */
        public static final Style FILL_AND_STROKE = new Style(2);

        Style(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;

        public static Style valueOf(String value) {
            if ("FILL".equals(value)) {
                return Style.FILL;
            } else if ("STROKE".equals(value)) {
                return Style.STROKE;
            } else if ("FILL_AND_STROKE".equals(value)) {
                return Style.FILL_AND_STROKE;
            } else {
                return null;
            }
        }

        public static Style[] values() {
            Style[] style = new Style[3];
            style[0] = Style.FILL;
            style[1] = Style.STROKE;
            style[2] = Style.FILL_AND_STROKE;
            return style;
        }
    }
	/**
	 * Return the paint's flags. Use the Flag enum to test flag values.
	 *
	 * @return the paint's flags (see enums ending in _Flag for bit masks)
	 */
	public int getFlags() {
		return mFlags;
	}

	/**
	 * Set the paint's flags. Use the Flag enum to specific flag values.
	 *
	 * @param flags The new flag bits for the paint
	 */
	public void setFlags(int flags) {
		mFlags = flags;
	}

	/**
	 * Helper for getFlags(), returning true if ANTI_ALIAS_FLAG bit is set
	 * AntiAliasing smooths out the edges of what is being drawn, but is has
	 * no impact on the interior of the shape. See setDither() and
	 * setFilterBitmap() to affect how colors are treated.
	 *
	 * @return true if the antialias bit is set in the paint's flags.
	 */
	public final boolean isAntiAlias() {
		return (getFlags() & ANTI_ALIAS_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the ANTI_ALIAS_FLAG bit
	 * AntiAliasing smooths out the edges of what is being drawn, but is has
	 * no impact on the interior of the shape. See setDither() and
	 * setFilterBitmap() to affect how colors are treated.
	 *
	 * @param aa true to set the antialias bit in the flags, false to clear it
	 */
	public void setAntiAlias(boolean aa) {
		if (aa == true) {
			mFlags |= ANTI_ALIAS_FLAG;
		} else
			mFlags = mFlags & ~ANTI_ALIAS_FLAG;
	}

	/**
	 * Helper for getFlags(), returning true if DITHER_FLAG bit is set
	 * Dithering affects how colors that are higher precision than the device
	 * are down-sampled. No dithering is generally faster, but higher precision
	 * colors are just truncated down (e.g. 8888 -> 565). Dithering tries to
	 * distribute the error inherent in this process, to reduce the visual
	 * artifacts.
	 *
	 * @return true if the dithering bit is set in the paint's flags.
	 */
	public final boolean isDither() {
		return (getFlags() & DITHER_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the DITHER_FLAG bit
	 * Dithering affects how colors that are higher precision than the device
	 * are down-sampled. No dithering is generally faster, but higher precision
	 * colors are just truncated down (e.g. 8888 -> 565). Dithering tries to
	 * distribute the error inherent in this process, to reduce the visual
	 * artifacts.
	 *
	 * @param dither true to set the dithering bit in flags, false to clear it
	 */
	public void setDither(boolean dither) {
		if (dither == true) {
			mFlags |= DITHER_FLAG;
		} else
			mFlags = mFlags & ~DITHER_FLAG;

	}

	/**
	 * Helper for getFlags(), returning true if LINEAR_TEXT_FLAG bit is set
	 *
	 * @return true if the lineartext bit is set in the paint's flags
	 */
	public final boolean isLinearText() {
		return (getFlags() & LINEAR_TEXT_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the LINEAR_TEXT_FLAG bit
	 *
	 * @param linearText true to set the linearText bit in the paint's flags,
	 *                   false to clear it.
	 */
	public void setLinearText(boolean linearText) {
		if (linearText == true) {
			mFlags |= LINEAR_TEXT_FLAG;
		} else
			mFlags = mFlags & ~LINEAR_TEXT_FLAG;

	}

	/**
	 * Helper for getFlags(), returning true if SUBPIXEL_TEXT_FLAG bit is set
	 *
	 * @return true if the subpixel bit is set in the paint's flags
	 */
	public final boolean isSubpixelText() {
		return (getFlags() & SUBPIXEL_TEXT_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the SUBPIXEL_TEXT_FLAG bit
	 *
	 * @param subpixelText true to set the subpixelText bit in the paint's
	 *                     flags, false to clear it.
	 */
	public void setSubpixelText(boolean subpixelText) {
		if (subpixelText == true) {
			mFlags |= SUBPIXEL_TEXT_FLAG;
		} else
			mFlags = mFlags & ~SUBPIXEL_TEXT_FLAG;

	}

	/**
	 * Helper for getFlags(), returning true if UNDERLINE_TEXT_FLAG bit is set
	 *
	 * @return true if the underlineText bit is set in the paint's flags.
	 */
	public final boolean isUnderlineText() {
		return (getFlags() & UNDERLINE_TEXT_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the UNDERLINE_TEXT_FLAG bit
	 *
	 * @param underlineText true to set the underlineText bit in the paint's
	 *                      flags, false to clear it.
	 */
	public void setUnderlineText(boolean underlineText) {
		if (underlineText == true) {
			mFlags |= UNDERLINE_TEXT_FLAG;
		} else
			mFlags = mFlags & ~UNDERLINE_TEXT_FLAG;

	}

	/**
	 * Helper for getFlags(), returning true if STRIKE_THRU_TEXT_FLAG bit is set
	 *
	 * @return true if the strikeThruText bit is set in the paint's flags.
	 */
	public final boolean isStrikeThruText() {
		return (getFlags() & STRIKE_THRU_TEXT_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the STRIKE_THRU_TEXT_FLAG bit
	 *
	 * @param strikeThruText true to set the strikeThruText bit in the paint's
	 *                       flags, false to clear it.
	 */
	public void setStrikeThruText(boolean strikeThruText) {
		if (strikeThruText == true) {
			mFlags |= STRIKE_THRU_TEXT_FLAG;
		} else
			mFlags = mFlags & ~STRIKE_THRU_TEXT_FLAG;

	}

	/**
	 * Helper for getFlags(), returning true if FAKE_BOLD_TEXT_FLAG bit is set
	 *
	 * @return true if the fakeBoldText bit is set in the paint's flags.
	 */
	public final boolean isFakeBoldText() {
		return (getFlags() & FAKE_BOLD_TEXT_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the STRIKE_THRU_TEXT_FLAG bit
	 *
	 * @param fakeBoldText true to set the fakeBoldText bit in the paint's
	 *                     flags, false to clear it.
	 */
	public void setFakeBoldText(boolean fakeBoldText) {
		if (fakeBoldText == true) {
			mFlags |= FAKE_BOLD_TEXT_FLAG;
		} else
			mFlags = mFlags & ~FAKE_BOLD_TEXT_FLAG;

	}

	/**
	 * Whether or not the bitmap filter is activated.
	 * Filtering affects the sampling of bitmaps when they are transformed.
	 * Filtering does not affect how the colors in the bitmap are converted into
	 * device pixels. That is dependent on dithering and xfermodes.
	 *
	 * @see #setFilterBitmap(boolean) setFilterBitmap()
	 */
	public final boolean isFilterBitmap() {
		return (getFlags() & FILTER_BITMAP_FLAG) != 0;
	}

	/**
	 * Helper for setFlags(), setting or clearing the FILTER_BITMAP_FLAG bit.
	 * Filtering affects the sampling of bitmaps when they are transformed.
	 * Filtering does not affect how the colors in the bitmap are converted into
	 * device pixels. That is dependent on dithering and xfermodes.
	 * 
	 * @param filter true to set the FILTER_BITMAP_FLAG bit in the paint's
	 *               flags, false to clear it.
	 */
	public void setFilterBitmap(boolean filter) {
		if (filter == true) {
			mFlags |= FILTER_BITMAP_FLAG;
		} else
			mFlags = mFlags & ~FILTER_BITMAP_FLAG;

	}

    public Style getStyle() {
        return mStyle;
    }

    /**
     * Set the paint's style, used for controlling how primitives'
     * geometries are interpreted (except for drawBitmap, which always assumes
     * Fill).
     *
     * @param style The new style to set in the paint
     */
    public void setStyle(Style style) {
        mStyle = style;
    }

	/**
	 * Return the paint's color. Note that the color is a 32bit value
	 * containing alpha as well as r,g,b. This 32bit value is not premultiplied,
	 * meaning that its alpha can be any value, regardless of the values of
	 * r,g,b. See the Color class for more details.
	 *
	 * @return the paint's color (and alpha).
	 */
	public int getColor() {
		return mColor;
	}

	/**
	 * Set the paint's color. Note that the color is an int containing alpha
	 * as well as r,g,b. This 32bit value is not premultiplied, meaning that
	 * its alpha can be any value, regardless of the values of r,g,b.
	 * See the Color class for more details.
	 *
	 * @param color The new color (including alpha) to set in the paint.
	 */
	public void setColor(int color) {
		mColor = color;
	}

	/**
	 * Helper to getColor() that just returns the color's alpha value. This is
	 * the same as calling getColor() >>> 24. It always returns a value between
	 * 0 (completely transparent) and 255 (completely opaque).
	 *
	 * @return the alpha component of the paint's color.
	 */
	public int getAlpha() {
		return (getColor() >> 24) & 0xff;
	}

	/**
	 * Helper to setColor(), that only assigns the color's alpha value,
	 * leaving its r,g,b values unchanged. Results are undefined if the alpha
	 * value is outside of the range [0..255]
	 *
	 * @param a set the alpha component [0..255] of the paint's color.
	 */
	public void setAlpha(int a) {
		mColor = mColor | a << 24;
	}

	/**
	 * Helper to setColor(), that takes a,r,g,b and constructs the color int
	 *
	 * @param a The new alpha component (0..255) of the paint's color.
	 * @param r The new red component (0..255) of the paint's color.
	 * @param g The new green component (0..255) of the paint's color.
	 * @param b The new blue component (0..255) of the paint's color.
	 */
	public void setARGB(int a, int r, int g, int b) {
		setColor((a << 24) | (r << 16) | (g << 8) | b);
	}

	/**
	 * Return the width for stroking.
	 * <p />
	 * A value of 0 strokes in hairline mode.
	 * Hairlines always draws a single pixel independent of the canva's matrix.
	 *
	 * @return the paint's stroke width, used whenever the paint's style is
	 *         Stroke or StrokeAndFill.
	 */
	public float getStrokeWidth() {
		return mStrokeWidth;
	}

    /**
     * Set the width for stroking. Pass 0 to stroke in hairline mode. Hairlines
     * always draws a single pixel independent of the canva's matrix.
     * 
     * @param width set the paint's stroke width, used whenever the paint's
     *            style is Stroke or StrokeAndFill.
     */
    public void setStrokeWidth(float width) {
        if (width == 0) {
            mStrokeWidth = 1;
        } else if (width > 0) {
            mStrokeWidth = width;
        }
    }

	/**
	 * Return the paint's stroke miter value. Used to control the behavior
	 * of miter joins when the joins angle is sharp.
	 *
	 * @return the paint's miter limit, used whenever the paint's style is
	 *         Stroke or StrokeAndFill.
	 */
	public float getStrokeMiter() {
		return mStrokeMiter;
	}

	/**
	 * Set the paint's stroke miter value. This is used to control the behavior
	 * of miter joins when the joins angle is sharp. This value must be >= 0.
	 *
	 * @param miter set the miter limit on the paint, used whenever the paint's
	 *              style is Stroke or StrokeAndFill.
	 */
	public void setStrokeMiter(float miter) {
        if (miter > 0) {
            mStrokeMiter = miter;
        }
    }

    /**
     * Return the paint's Cap, controlling how the start and end of stroked
     * lines and paths are treated.
     *
     * @return the line cap style for the paint, used whenever the paint's style
     *         is Stroke or StrokeAndFill.
     */
    public Cap getStrokeCap() {
        return mStrokeCap;
    }

    /**
     * Set the paint's Cap.
     *
     * @param cap set the paint's line cap style, used whenever the paint's
     *            style is Stroke or StrokeAndFill.
     */
    public void setStrokeCap(Cap cap) {
        mStrokeCap = cap;
    }

    /**
     * Return the paint's stroke join type.
     *
     * @return the paint's Join.
     */
    public Join getStrokeJoin() {
        return mStrokeJoin;
    }

    /**
     * Set the paint's Join.
     *
     * @param join set the paint's Join, used whenever the paint's style is
     *            Stroke or StrokeAndFill.
     */
    public void setStrokeJoin(Join join) {
        mStrokeJoin = join;
    }
    
    /**
     * Get the paint's shader object.
     *
     * @return the paint's shader (or null)
     */
    public Shader getShader() {
        return mShader;
    }

    public ShaderType getShaderType() {
        return mShaderType;
    }
    
    /**
     * Set or clear the shader object.
     * <p />
     * Pass null to clear any previous shader.
     * As a convenience, the parameter passed is also returned.
     *
     * @param shader May be null. the new shader to be installed in the paint
     * @return       shader
     */
    public Shader setShader(Shader shader) {
        mShader = shader;
        if (shader instanceof BitmapShader) {
            mShaderType = ShaderType.BITMAPSHADER;
        } 
//        if (shader instanceof  ComposeShader) {
//            mShaderType = ShaderType.COMPOSESHADER;
//        } 
        if (shader instanceof LinearGradient) {
            mShaderType = ShaderType.LINEARGRADIENT;
        } 
        if (shader instanceof RadialGradient) {
            mShaderType = ShaderType.RADIALGRADIENT;
        } 
        if (shader instanceof SweepGradient) {
            mShaderType = ShaderType.SWEEPGRADIENT;
        }
        return shader;
    }
    
    /**
     * Get the paint's xfermode object.
     *
     * @return the paint's xfermode (or null)
     */
    public Xfermode getXfermode() {
        return mXfermode;
    }

    /**
     * Set or clear the xfermode object.
     * <p />
     * Pass null to clear any previous xfermode.
     * As a convenience, the parameter passed is also returned.
     *
     * @param xfermode May be null. The xfermode to be installed in the paint
     * @return         xfermode
     */
    public Xfermode setXfermode(Xfermode xfermode) {
        mXfermode = xfermode;
        return xfermode;
    }
    
	/**
	 * Temporary API to clear the shadow layer.
	 */
	public void clearShadowLayer() {
	    throw new RuntimeException("Not implemented!");
	}

	/**
	 * Return the paint's Align value for drawing text. This controls how the
	 * text is positioned relative to its origin. LEFT align means that all of
	 * the text will be drawn to the right of its origin (i.e. the origin
	 * specifieds the LEFT edge of the text) and so on.
	 *
	 * @return the paint's Align value for drawing text.
	 */
	public Align getTextAlign() {
		return mTextAlign;
	}

	/**
	 * Set the paint's text alignment. This controls how the
	 * text is positioned relative to its origin. LEFT align means that all of
	 * the text will be drawn to the right of its origin (i.e. the origin
	 * specifieds the LEFT edge of the text) and so on.
	 *
	 * @param align set the paint's Align value for drawing text.
	 */
	public void setTextAlign(Align align) {
		mTextAlign = align;
	}

	/**
	 * Return the paint's text size.
	 *
	 * @return the paint's text size.
	 */
	public float getTextSize() {
		return mTextSize;
	}

    /**
     * Set the paint's text size. This value must be > 0
     * 
     * @param textSize set the paint's text size.
     */
    public void setTextSize(float textSize) {
        mTextSize = textSize;
    }

	/**
	 * Return the paint's horizontal scale factor for text. The default value
	 * is 1.0.
	 *
	 * @return the paint's scale factor in X for drawing/measuring text
	 */
	public float getTextScaleX() {
		return mTextScaleX;
	}

	/**
	 * Set the paint's horizontal scale factor for text. The default value
	 * is 1.0. Values > 1.0 will stretch the text wider. Values < 1.0 will
	 * stretch the text narrower.
	 *
	 * @param scaleX set the paint's scale in X for drawing/measuring text.
	 */
	public void setTextScaleX(float scaleX) {
		mTextScaleX = scaleX;
	}

	/**
	 * Return the paint's horizontal skew factor for text. The default value
	 * is 0.
	 *
	 * @return         the paint's skew factor in X for drawing text.
	 */
	public float getTextSkewX() {
		return mTextSkewX;
	}

	/**
	 * Set the paint's horizontal skew factor for text. The default value
	 * is 0. For approximating oblique text, use values around -0.25.
	 *
	 * @param skewX set the paint's skew factor in X for drawing text.
	 */
	public void setTextSkewX(float skewX) {
		mTextSkewX = skewX;
	}

    /**
     * Return the distance above (negative) the baseline (ascent) based on the
     * current typeface and text size.
     * 
     * @return the distance above (negative) the baseline (ascent) based on the
     *         current typeface and text size.
     */
    public float ascent() {
        // MayLoon: Not very precise, we just return the text height, because we can't 
        // get exact font Metrics.
        Rect bounds = new Rect();
        this.measureText("0", bounds, 0, 1);
        return -bounds.height() * 3 / 4.0f ;
    }

    /**
     * Return the distance below (positive) the baseline (descent) based on the
     * current typeface and text size.
     * 
     * @return the distance below (positive) the baseline (descent) based on the
     *         current typeface and text size.
     */
    public float descent() {
        // MayLoon: Not very precise, we just return the text height, because we can't 
        // get exact font Metrics.
        Rect bounds = new Rect();
        this.measureText("0", bounds, 0, 1);
        return bounds.height() / 4.0f;
    }

	/**
	 * Get the paint's typeface object.
	 * <p />
	 * The typeface object identifies which font to use when drawing or
	 * measuring text.
	 *
	 * @return the paint's typeface (or null)
	 */
	public Typeface getTypeface() {
		return mTypeface;
	}

	/**
	 * Set or clear the typeface object.
	 * <p />
	 * Pass null to clear any previous typeface.
	 * As a convenience, the parameter passed is also returned.
	 *
	 * @param typeface May be null. The typeface to be installed in the paint
	 * @return         typeface
	 */
	public Typeface setTypeface(Typeface typeface) {
		mTypeface = typeface;
		return typeface;
	}

	/**
	 * Class that describes the various metrics for a font at a given text size.
	 * Remember, Y values increase going down, so those values will be positive,
	 * and values that measure distances going up will be negative. This class
	 * is returned by getFontMetrics().
	 */
	public static class FontMetrics {
		/**
		 * The maximum distance above the baseline for the tallest glyph in 
		 * the font at a given text size.
		 */
		public float top;
		/**
		 * The recommended distance above the baseline for singled spaced text.
		 */
		public float ascent;
		/**
		 * The recommended distance below the baseline for singled spaced text.
		 */
		public float descent;
		/**
		 * The maximum distance below the baseline for the lowest glyph in 
		 * the font at a given text size.
		 */
		public float bottom;
		/**
		 * The recommended additional space to add between lines of text.
		 */
		public float leading;
	}

    /**
     * Return the font's recommended interline spacing, given the Paint's
     * settings for typeface, textSize, etc. If metrics is not null, return the
     * fontmetric values in it.
     * 
     * @param metrics If this object is not null, its fields are filled with the
     *            appropriate values given the paint's text attributes.
     * @return the font's recommended interline spacing.
     */
    public float getFontMetrics(FontMetrics metrics) {
        if (metrics != null) {
            metrics.top = metrics.ascent = this.ascent();
            metrics.bottom = metrics.descent = this.descent();
        }
        return 0;
    }

	/**
	 * Allocates a new FontMetrics object, and then calls getFontMetrics(fm)
	 * with it, returning the object.
	 */
	public FontMetrics getFontMetrics() {
		FontMetrics fm = new FontMetrics();
		getFontMetrics(fm);
		return fm;
	}

	/**
	 * Convenience method for callers that want to have FontMetrics values as
	 * integers.
	 */
	public static class FontMetricsInt {
		public int top;
		public int ascent;
		public int descent;
		public int bottom;
		public int leading;

		@Override
		public String toString() {
			return "FontMetricsInt: top=" + top + " ascent=" + ascent
					+ " descent=" + descent + " bottom=" + bottom + " leading="
					+ leading;
		}
	}

	/**
	 * Return the recommend line spacing based on the current typeface and
	 * text size.
	 *
	 * @return  recommend line spacing based on the current typeface and
	 *          text size.
	 */
	public float getFontSpacing() {
		return getFontMetrics(null);
	}

    /**
     * Return the width of the text.
     * 
     * @param text The text to measure
     * @param start The index of the first character to start measuring
     * @param end 1 beyond the index of the last character to measure
     * @return The width of the text
     */
    public float measureText(CharSequence text, int start, int end) {
        if (text instanceof String) {
            if ((start | end | (end - start) | (text.length() - end)) < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (end - start > text.toString().length()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            float textSize = 0;
            textSize = measureText(text.toString(), null, start, end);

            return textSize;
        }
        return 1;
    }

    /**
     * Return the width of the text.
     * 
     * @param text The text to measure
     * @param start The index of the first character to start measuring
     * @param end 1 beyond the index of the last character to measure
     * @return The width of the text
     */
    public float measureText(char[] text, int start, int end) {
        if ((start | end | (end - start) | (text.length - end)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (end - start > text.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        float textSize = 0;
        textSize = measureText(text.toString(), null, start, end);

        return textSize;
    }

    /**
     * Return the width of the text.
     *
     * @param text  The text to measure
     * @return      The width of the text
     */
    public float measureText(String text) {
        if (!mHasCompatScaling) return measureText(text, null, 0, text.length());
        final float oldSize = getTextSize();
        setTextSize(oldSize*mCompatScaling);
        float w = measureText(text, null, 0, text.length());
        setTextSize(oldSize);
        return w*mInvCompatScaling;
    }

    public String setFontCanvasProperties() {
        // use the font of the paint.
        String hashCode = String.valueOf(this.hashCode());
        String font = null;
        /**
         * @j2sNative 
         * var _canvas = document.getElementById(hashCode); 
         * if (_canvas == null) console.e("Cant't get Paint's font canvas!");
         * var _context = _canvas.getContext("2d"); 
         * font = _context.font; 
         */{}

        // the default font string looks like bold 10px sans-serif, so we
        // need to extract the size
        Pattern p = Pattern.compile("\\d+\\.?\\d*");
        Matcher m = p.matcher(font);
        String result = null;
        if (m.find()) {
            result = font.substring(0, m.start() - 1);
            result += this.getTextSize();
            result += font.substring(m.end());
        }
        
        /**
         * @j2sNative 
         * if (result != null) { 
         *     _context.font = result; 
         * }
         * return _context.font;
         */{}
         return font;
    }

    private float measureText(String string, Rect bounds, int start, int end) {
        float width = 10f;
        float height = 0f;
        String hashCode = String.valueOf(this.hashCode());
        if ((start | end | (end - start) | (string.length() - end)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        string = string.substring(start, end);
        String fontStyle = "normal";
        String fontWeight = "normal";
        String fontFamily = "serif";
        Typeface tf = getTypeface();
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
        /**
         * @j2sNative
         * var div = document.createElement("div");
         * div.innerText = string;
         * div.style.whiteSpace = "pre";
         * div.id = "measure_div";
         * document.body.appendChild(div);
         * div.style.visibility = "hidden";
         * div.style.position = "absolute";
         * div.style.overflow = "hidden";
         * div.style.border = "none";
         * div.style.padding = "0px";
         * div.style.margin = "0px";
         * div.style.fontSize = this.mTextSize + "px";
         * div.style.fontStyle = fontStyle;
         * div.style.fontWeight = fontWeight;
         * div.style.fontFamily = fontFamily;
         * div.style.width = "auto";
         * div.style.height = "auto";
         * width = div.offsetWidth;
         * height = div.clientHeight;
         * div.parentNode.removeChild(div);
         * var adjustConstant = this.mTextSize * (-3) / 4;
         * if(bounds != null) {
         *     bounds.left = 0;
         *     bounds.top = adjustConstant;
         *     bounds.right = width;
         *     bounds.bottom = height + adjustConstant;
         * } else {
         *     this.left = 0;
         *     this.top = 0;
         *     this.right = width;
         *     this.bottom = height;
         * }
         */{}
        return width;
    }

    /**
     * Return the advance widths for the characters in the string.
     * 
     * @param text The text to measure
     * @param start The index of the first char to to measure
     * @param end The end of the text slice to measure
     * @param widths array to receive the advance widths of the characters. Must
     *            be at least a large as (end - start).
     * @return the actual number of widths returned.
     */
    public int getTextWidths(CharSequence text, int start, int end,
            float[] widths) {
        if (text instanceof String) {
            return getTextWidths((String) text, start, end, widths);
        }
        if (text instanceof SpannedString ||
                text instanceof SpannableString) {
            return getTextWidths(text.toString(), start, end, widths);
        }
        if (text instanceof GraphicsOperations) {
            return ((GraphicsOperations) text).getTextWidths(start, end,
                    widths, this);
        }
        char[] buf = TemporaryBuffer.obtain(end - start);
        TextUtils.getChars(text.toString(), start, end, buf, 0);
        int result = getTextWidths(buf, 0, end - start, widths);
        TemporaryBuffer.recycle(buf);
        return result;
    }

    /**
     * Return the advance widths for the characters in the string.
     * 
     * @param text The text to measure
     * @param start The index of the first char to to measure
     * @param end The end of the text slice to measure
     * @param widths array to receive the advance widths of the characters. Must
     *            be at least a large as (end - start).
     * @return the actual number of widths returned.
     */
    public int getTextWidths(char[] text, int index, int count,
            float[] widths) {
        if ((index | count) < 0 || index + count > text.length
                || count > widths.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final float oldSize = getTextSize();
        setTextSize(oldSize * mCompatScaling);
        int res = count;
        setTextSize(oldSize);
        for (int i = 0; i < res; i++) {
            widths[i] = measureText(text.toString(), null, i, i + 1);
        }
        return res;
    }

    /**
     * Return the advance widths for the characters in the string.
     *
     * @param text   The text to measure
     * @param start  The index of the first char to to measure
     * @param end    The end of the text slice to measure
     * @param widths array to receive the advance widths of the characters.
     *               Must be at least a large as the text.
     * @return       the number of unichars in the specified text.
     */
    public int getTextWidths(String text, int start, int end, float[] widths) {
        if ((start | end | (end - start) | (text.length() - end)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (end - start > widths.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final float oldSize = getTextSize();
        setTextSize(oldSize * mCompatScaling);
        int res = end - start;
        setTextSize(oldSize);
        for (int i = 0; i < res; i++) {
            widths[i] = measureText(text, null, i, i + 1);
        }
        return res;
    }

	/**
	 * Return the advance widths for the characters in the string.
	 *
	 * @param text   The text to measure
	 * @param widths array to receive the advance widths of the characters.
	 *               Must be at least a large as the text.
	 * @return       the number of unichars in the specified text.
	 */
	public int getTextWidths(String text, float[] widths) {
		return getTextWidths(text, 0, text.length(), widths);
	}

    /**
     * Return in bounds (allocated by the caller) the smallest rectangle that
     * encloses all of the characters, with an implied origin at (0,0).
     * 
     * @param text String to measure and return its bounds
     * @param start Index of the first char in the string to measure
     * @param end 1 past the last char in the string measure
     * @param bounds Returns the unioned bounds of all the text. Must be
     *            allocated by the caller.
     */
    public void getTextBounds(String text, int start, int end, Rect bounds) {
        if ((start | end | (end - start) | (text.length() - end)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (bounds == null) {
            throw new NullPointerException("need bounds Rect");
        }

        measureText(text, bounds, start, end);
    }

	/**
	 * Return in bounds (allocated by the caller) the smallest rectangle that
	 * encloses all of the characters, with an implied origin at (0,0).
	 *
	 * @param text  Array of chars to measure and return their unioned bounds
	 * @param index Index of the first char in the array to measure
	 * @param count The number of chars, beginning at index, to measure
	 * @param bounds Returns the unioned bounds of all the text. Must be
	 *               allocated by the caller.
	 */
	public void getTextBounds(char[] text, int index, int count, Rect bounds) {
		if ((index | count) < 0 || index + count > text.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (bounds == null) {
			throw new NullPointerException("need bounds Rect");
		}
	}

	/**
     * Return the font's interline spacing, given the Paint's settings for
     * typeface, textSize, etc. If metrics is not null, return the fontmetric
     * values in it. Note: all values have been converted to integers from
     * floats, in such a way has to make the answers useful for both spacing
     * and clipping. If you want more control over the rounding, call
     * getFontMetrics().
     *
     * @return the font's interline spacing.
     */
    public int getFontMetricsInt(FontMetricsInt fmi) {
        if (fmi == null) {
            measureText("A");
            fmi = new FontMetricsInt();
            fmi.top = this.top;
            fmi.bottom = this.bottom;
            return fmi.bottom = this.bottom;
        }
        fmi.top = this.top;
        fmi.bottom = this.bottom;
        return fmi.bottom - fmi.top;
    }

    public FontMetricsInt getFontMetricsInt() {
        FontMetricsInt fm = new FontMetricsInt();
        getFontMetricsInt(fm);
        return fm;
    }
    
    /**
     * Set or clear the patheffect object.
     * <p />
     * Pass null to clear any previous patheffect.
     * As a convenience, the parameter passed is also returned.
     *
     * @param effect May be null. The patheffect to be installed in the paint
     * @return       effect
     */
    /**
     * @Mayloon update
     */
//    public PathEffect setPathEffect(PathEffect effect) {
//        int effectNative = 0;
//        if (effect != null) {
//            effectNative = effect.native_instance;
//        }
//        native_setPathEffect(mNativePaint, effectNative);
//        mPathEffect = effect;
//        return effect;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: setRasterizer");
     */
    @MayloonStubAnnotation()
    public Object setRasterizer(Object rasterizer) {
        System.out.println("Stub" + " Function : setRasterizer");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: finalize");
     */
    @MayloonStubAnnotation()
    protected void finalize() {
        System.out.println("Stub" + " Function : finalize");
        return;
    }

}
