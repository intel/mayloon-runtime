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

package android.graphics;

import java.util.ArrayList;
import java.util.HashMap;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.ShaderType;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.util.Log;

/**
 * The Canvas class holds the "draw" calls. To draw something, you need
 * 4 basic components: A Bitmap to hold the pixels, a Canvas to host
 * the draw calls (writing into the bitmap), a drawing primitive (e.g. Rect,
 * Path, text, Bitmap), and a paint (to describe the colors and styles for the
 * drawing).
 * MayLoon supports Canvas by using HTML5 Canvas object by setting its CSS position property
 * 
 */
public class Canvas {
    private static final String TAG = "Canvas";
    // Package-scoped for quick access.
    /*package*/ int mDensity = Bitmap.DENSITY_NONE;
	private int saveCount = 0;
	public float alpha = -1;
	
	static final boolean DEBUG = false;
	private ArrayList<CanvasState> transList = new ArrayList<CanvasState>();
    int _ddx;
    int _ddy;
    int _width;
    int _height;
    Matrix ctm = null; // current transformation matrix
    Rect mClipBounds = new Rect(); // current clipBounds

    private class CanvasState {
        public Rect _rect = null;
        public Rect _clipBounds = null;
        public Matrix _ctm = null; // save ctm
    }

	private Bitmap mBM;
	private final String appCanvasID;
	private final String surfaceViewCanvasID;

	public String getCanvasID() {
		return appCanvasID;
	}

	public String getSurfaceViewCanvasID() {
		return surfaceViewCanvasID;
	}
	
	public static int APP_CANVAS = 0;
	public static int SURFACEVIEW_CANVAS = 2;

	private int canvasType = 0;
	private String activeCanvas = null;
	private boolean opaqueFlag = false;
	
    // the SAVE_FLAG constants must match their native equivalents

    /** restore the current matrix when restore() is called */
    public static final int MATRIX_SAVE_FLAG = 0x01;
    /** restore the current clip when restore() is called */
    public static final int CLIP_SAVE_FLAG = 0x02;
    /** the layer needs to per-pixel alpha */
    public static final int HAS_ALPHA_LAYER_SAVE_FLAG = 0x04;
    /** the layer needs to 8-bits per color component */
    public static final int FULL_COLOR_LAYER_SAVE_FLAG = 0x08;
    /** clip against the layer's bounds */
    public static final int CLIP_TO_LAYER_SAVE_FLAG = 0x10;
    /** restore everything when restore() is called */
    public static final int ALL_SAVE_FLAG = 0x1F; 
	
	// Used to map Android Paint styles to HTML5 styles
    private static HashMap<Object, String> paintCapAndJoinMap = new HashMap();

    private void initPaintCapAndJoinMap() {
        paintCapAndJoinMap.put(Cap.BUTT, "butt");
        paintCapAndJoinMap.put(Cap.ROUND, "round");
        paintCapAndJoinMap.put(Cap.SQUARE, "square");
        paintCapAndJoinMap.put(Join.MITER, "miter");
        paintCapAndJoinMap.put(Join.ROUND, "round");
        paintCapAndJoinMap.put(Join.BEVEL, "bevel");
        paintCapAndJoinMap.put(Align.LEFT, "left");
        paintCapAndJoinMap.put(Align.CENTER, "center");
        paintCapAndJoinMap.put(Align.RIGHT, "right");
    }

    private void initSave() {
        saveCount++;
        CanvasState state = new CanvasState();
        state._rect = new Rect(_ddx, _ddy, _ddx + _width, _ddy + _height);
        state._clipBounds = new Rect(mClipBounds);
        state._ctm = new Matrix(this.ctm);
        transList.add(state);
    }

    public enum EdgeType {
        BW(0),  //!< treat edges by just rounding to nearest pixel boundary
        AA(1);  //!< treat edges by rounding-out, since they may be antialiased

        EdgeType(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public enum VertexMode {
        TRIANGLES(0),
        TRIANGLE_STRIP(1),
        TRIANGLE_FAN(2);

        VertexMode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public void chooseCanvas(int _canvasType) {
        canvasType = _canvasType;
        if (canvasType == APP_CANVAS) {
            activeCanvas = appCanvasID;
        } else if (canvasType == SURFACEVIEW_CANVAS) {
            activeCanvas = surfaceViewCanvasID;
        }
    }

    public String getHTML5CanvasID() {
        return activeCanvas;
    }

    /**
     * Construct a canvas with the specified bitmap to draw into. The bitmap
     * must be mutable.
     * <p>
     * The initial target density of the canvas is the same as the given
     * bitmap's density.
     * 
     * @param bitmap Specifies a mutable bitmap for the canvas to draw into.
     */
    public Canvas(Bitmap bitmap) {
        if (!bitmap.isMutable()) {
            throw new IllegalStateException(
                    "Immutable bitmap passed to Canvas constructor");
        }
        throwIfRecycled(bitmap);

        this.mBM = bitmap;
        this.appCanvasID = null;
        this.surfaceViewCanvasID = null;
        this.ctm = new Matrix(); // initialize the matrix with identity

        // Ensure Cached Canvas is created for this bitmap
        this.mBM.ensureCachedCanvas(false, false);
        initPaintCapAndJoinMap();
        mClipBounds.set(_ddx, _ddy, _ddx + bitmap.getWidth(), _ddy + bitmap.getHeight());
        initSave();
    }

    /**
     * create a canvas element named id, and then attach it to the document
     * 
     * @param id
     */
    public Canvas(String canvasID) {
        this.appCanvasID = canvasID;
        this.surfaceViewCanvasID = null;
        this.ctm = new Matrix();
        this.chooseCanvas(Canvas.APP_CANVAS);
        initPaintCapAndJoinMap();
        initSave();
    }

    public Canvas(String canvasID, String surfaceViewCanvasID) {
        this.appCanvasID = canvasID;
        this.surfaceViewCanvasID = surfaceViewCanvasID;
        this.ctm = new Matrix();
        initPaintCapAndJoinMap();
        initSave();
    }

    public Canvas() {
        this.appCanvasID = null;
        this.surfaceViewCanvasID = null;
        this.ctm = new Matrix();
        clipRect(0, 0, getWidth(), getHeight());
        initPaintCapAndJoinMap();
        initSave();
    }

	public void setOpaque(boolean opaque) {
	    this.opaqueFlag = opaque;
	}
	
	public boolean isOpaque() {
	    return this.opaqueFlag;
	}

    /**
     * set HTML5 Canvas Context properties according the paint
     * 
     * @param paint
     */
    private void setHTML5CanvasContext(Paint paint) {
        // line style
        String rgb = Color.toString(paint.getColor());
        float strokeWidth = paint.getStrokeWidth();
        String strokeCap = paintCapAndJoinMap.get(paint.getStrokeCap());
        String strokeJoin = paintCapAndJoinMap.get(paint.getStrokeJoin());
        float strokeMiter = paint.getStrokeMiter();
        
        // font style
        Typeface typeface = paint.getTypeface();
        String textAlign = paintCapAndJoinMap.get(paint.getTextAlign());
        
        String font = null;
        if (typeface != null) {
            // convert Typeface to HTML5 font string
            font = typeface.getStyleName();
            font += " ";
            font += paint.getTextSize() + "px";
            font += " ";
            font += typeface.getFamilyName();
        } else {
            // Set the font property into paint's canvas
            font = paint.setFontCanvasProperties();
        }

        // get Paint's shader
        Shader  shader = paint.getShader();
        ShaderType shaderType = paint.getShaderType();
        // get Paint's xfermode
        Xfermode xfermode = paint.getXfermode();
        if (xfermode != null && !(xfermode instanceof PorterDuffXfermode)) {
            // We only support PorterDuffXfermode now.
            xfermode = null;
        }
        
        /**
         * @j2sNative
         * var canvas = null;
         * if (this.mBM != null) {
         *    canvas = this.mBM.mCachedCanvas;
         * } else {
         *    canvas  = document.getElementById(this.activeCanvas);
         * }
         * var context = canvas.getContext("2d");
         * 
         * // line style setting
         * context.fillStyle   = rgb;
         * context.strokeStyle = rgb;
         * context.lineWidth   = strokeWidth;
         * context.lineCap     = strokeCap;
         * context.lineJoin    = strokeJoin;
         * context.miterLimit  = strokeMiter;
         * // font setting
         * if (font != null) {
         *    context.font = font;
         * }  
         * context.textAlign   = textAlign;
         * 
         * // set Shader if we have.
         * var gradient = null;
         * if (shader != null) {
         *     if (shaderType == shaderType.BITMAPSHADER) {
         *         // We should detect whether browser supports Pattern now. 
         *         if (context.createPattern == null) {
         *             android.util.Log.e(this.TAG, "This browser doesn't support createPattern");
         *             return;
         *         }
         *         if (!shader.mBitmap.ensureCachedCanvas(false, true)) return;
         *         if (shader.mTileMode == null) return; // only repeat mode is supported.
         *         var bitmapshader = context.createPattern(shader.mBitmap.mCachedCanvas, shader.mTileMode);
         *         context.fillStyle   = bitmapshader;
         *         context.strokeStyle = bitmapshader;
         *     }
         *     if (shaderType == shaderType.COMPOSESHADER) {
         *         android.util.Log.e(this.TAG, "ComposeShader is not implemented!");
         *     }
         *     if (shaderType == shaderType.LINEARGRADIENT) {
         *         gradient = context.createLinearGradient(shader.m_pts0.x, shader.m_pts0.y, 
         *                                                 shader.m_pts1.x, shader.m_pts1.y);
         *     }
         *     if (shaderType == shaderType.RADIALGRADIENT) {
         *         gradient = context.createRadialGradient(shader.m_center.x, shader.m_center.y, 0, 
         *                                                 shader.m_center.x, shader.m_center.y, shader.m_radius);
         *     }  
         *     if (shaderType == shaderType.SWEEPGRADIENT) {
         *         android.util.Log.e(this.TAG, "SweepGradient is not implemented!");
         *     }
         *     var alpha = 0;
         *     if (gradient != null) {
         *         if (shader.m_positions != null) {
         *             for (var i = 0; i < shader.m_positions.length; i++) {
         *                 gradient.addColorStop(shader.m_positions[i], android.graphics.Color.toString(shader.m_colors[i]));
         *                 alpha += android.graphics.Color.alpha(shader.m_colors[i]) / 255;
         *             }
         *
         *         } else {
         *             // Add color stop evenly.
         *             for (var i = 0; i < shader.m_colors.length; i++) {
         *                 gradient.addColorStop(i/(shader.m_colors.length-1), android.graphics.Color.toString(shader.m_colors[i]));
         *                 alpha += android.graphics.Color.alpha(shader.m_colors[i]) / 255;
         *             }
         *         }
         *
         *         if (shader.m_positions != null && shader.m_positions.length > 0) {
         *             alpha = alpha / shader.m_positions.length;
         *         } else if (shader.m_colors != null && shader.m_colors.length > 0) {
         *             alpha = alpha / shader.m_colors.length;
         *         } else {
         *             alpha = 1;
         *         }
         *
         *         context.globalAlpha = alpha;
         *         context.fillStyle   = gradient;
         *         context.strokeStyle = gradient;
         *     }
         * }
         * 
         * if (this.alpha != -1) {
         *     context.globalAlpha = this.alpha;
         * }
         * 
         * if (xfermode != null) {
         *     context.globalCompositeOperation = "source-over"; // default
         *     switch (xfermode.native_instance) {
         *         case android.graphics.PorterDuff.Mode.CLEAR.nativeInt:
         *             android.util.Log.e(this.TAG, "PorterDuff.Mode.CLEAR is not supported!");
         *             break;
         *         case android.graphics.PorterDuff.Mode.DARKEN.nativeInt:
         *             context.globalCompositeOperation = "darker";
         *             break;
         *         case android.graphics.PorterDuff.Mode.DST.nativeInt:
         *             android.util.Log.e(this.TAG, "PorterDuff.Mode.DST is not supported!");
         *             break;
         *         case android.graphics.PorterDuff.Mode.DST_ATOP.nativeInt:
         *             context.globalCompositeOperation = "destination-atop";
         *             break;
         *         case android.graphics.PorterDuff.Mode.DST_IN.nativeInt:
         *             context.globalCompositeOperation = "destination-in";
         *             break;
         *         case android.graphics.PorterDuff.Mode.DST_OUT.nativeInt:
         *             context.globalCompositeOperation = "destination-out";
         *             break;
         *         case android.graphics.PorterDuff.Mode.DST_OVER.nativeInt:
         *             context.globalCompositeOperation = "destination-over";
         *             break;
         *         case android.graphics.PorterDuff.Mode.LIGHTEN.nativeInt:
         *             context.globalCompositeOperation = "lighter";
         *             break;
         *         case android.graphics.PorterDuff.Mode.MULTIPLY.nativeInt:
         *             android.util.Log.e(this.TAG, "PorterDuff.Mode.MULTIPLY is not supported!");
         *             break;
         *         case android.graphics.PorterDuff.Mode.SCREEN.nativeInt:
         *             android.util.Log.e(this.TAG, "PorterDuff.Mode.SCREEN is not supported!");
         *             break; 
         *         case android.graphics.PorterDuff.Mode.SRC.nativeInt:
         *             context.globalCompositeOperation = "copy";
         *             break; 
         *         case android.graphics.PorterDuff.Mode.SRC_ATOP.nativeInt:
         *             context.globalCompositeOperation = "source-atop";
         *             break; 
         *         case android.graphics.PorterDuff.Mode.SRC_IN.nativeInt:
         *             context.globalCompositeOperation = "source-in";
         *             break; 
         *         case android.graphics.PorterDuff.Mode.SRC_OUT.nativeInt:
         *             context.globalCompositeOperation = "source-out";
         *             break; 
         *         case android.graphics.PorterDuff.Mode.SRC_OVER.nativeInt:
         *             context.globalCompositeOperation = "source-over"; // default
         *             break; 
         *         case android.graphics.PorterDuff.Mode.XOR.nativeInt:
         *             context.globalCompositeOperation = "xor";
         *             break; 
         *         default:
         *             break;
         *     }
         * }
         */{}  
    }

    /**
     * Fill the entire canvas' bitmap (restricted to the current clip) with the
     * specified RGB color, using srcover porterduff mode.
     *
     * @param r red component (0..255) of the color to draw onto the canvas
     * @param g green component (0..255) of the color to draw onto the canvas
     * @param b blue component (0..255) of the color to draw onto the canvas
     */
    public void drawRGB(int r, int g, int b) {
        this.drawARGB(0xFF, r, g, b);
    }
    
    /**
     * Fill the entire canvas' bitmap (restricted to the current clip) with the
     * specified ARGB color, using srcover porterduff mode.
     *
     * @param a alpha component (0..255) of the color to draw onto the canvas
     * @param r red component (0..255) of the color to draw onto the canvas
     * @param g green component (0..255) of the color to draw onto the canvas
     * @param b blue component (0..255) of the color to draw onto the canvas
     */
    public void drawARGB(int a, int r, int g, int b) {
        Paint paint = new Paint();
        paint.setARGB(a, r, g, b);
        
        setHTML5CanvasContext(paint);
        
        /**
         * @j2sNative
         * var _canvas = null;
         * if (this.mBM != null) {
         *    _canvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _canvas = document.getElementById(this.activeCanvas);
         * }
         * var _context = _canvas.getContext("2d");
         * _context.fillRect(0, 0, _canvas.width, _canvas.height); 
         */
        {}
    }
    
    /**
     * Draw the text, with origin at (x,y), using the specified paint. The
     * origin is interpreted based on the Align setting in the paint.
     *
     * @param text  The text to be drawn
     * @param x     The x-coordinate of the origin of the text being drawn
     * @param y     The y-coordinate of the origin of the text being drawn
     * @param paint The paint used for the text (e.g. color, size, style)
     */
    public void drawText(char[] text, int index, int count, float x, float y,
            Paint paint) {
        if ((index | count | (index + count) | (text.length - index - count)) < 0) {
            throw new IndexOutOfBoundsException();
        }

        this.drawText(new String(text), index, index + count, x, y, paint);
    }

    /**
     * Draw the text, with origin at (x,y), using the specified paint.
     * The origin is interpreted based on the Align setting in the paint.
     *
     * @param text  The text to be drawn
     * @param start The index of the first character in text to draw
     * @param end   (end - 1) is the index of the last character in text to draw
     * @param x     The x-coordinate of the origin of the text being drawn
     * @param y     The y-coordinate of the origin of the text being drawn
     * @param paint The paint used for the text (e.g. color, size, style)
     */
    public void drawText(String text, int start, int end, float x, float y,
                         Paint paint) {
        if ((start | end | (end - start) | (text.length() - end)) < 0) {
            throw new IndexOutOfBoundsException();
        }
              
        String subText = text.substring(start, end);
        if (paint != null) {
            Xfermode xfermode = paint.getXfermode();
            if (xfermode != null && xfermode instanceof PorterDuffXfermode) {
                if (xfermode.native_instance == PorterDuff.Mode.DST.nativeInt) {
                    // for dst mode, as HTML5 doesn't support DST mode natively,
                    // we just skip the drawing in MayLoon
                    return;
                }
            }
            setHTML5CanvasContext(paint);
        }
        
        /**
         * @j2sNative
         * var _canvas = null;
         * if (this.mBM != null) {
         *    _canvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _canvas = document.getElementById(this.activeCanvas);
         * }
         * var _context = _canvas.getContext("2d");
         * _context.fillText(subText, x, y);
         */{}
    }
    
    /**
     * Draw the specified range of text, specified by start/end, with its
     * origin at (x,y), in the specified Paint. The origin is interpreted
     * based on the Align setting in the Paint.
     *
     * @param text     The text to be drawn
     * @param start    The index of the first character in text to draw
     * @param end      (end - 1) is the index of the last character in text
     *                 to draw
     * @param x        The x-coordinate of origin for where to draw the text
     * @param y        The y-coordinate of origin for where to draw the text
     * @param paint The paint used for the text (e.g. color, size, style)
     */
    public void drawText(CharSequence text, int start, int end, float x,
            float y, Paint paint) {
        this.drawText(text.toString(), start, end, x, y, paint);
    }

    /**
     * Draw the text, with origin at (x,y), using the specified paint. The
     * origin is interpreted based on the Align setting in the paint.
     *
     * @param text  The text to be drawn
     * @param x     The x-coordinate of the origin of the text being drawn
     * @param y     The y-coordinate of the origin of the text being drawn
     * @param paint The paint used for the text (e.g. color, size, style)
     */
    public void drawText(String text, float x, float y, Paint paint) {
        this.drawText(text, 0, text.length(), x, y, paint);
    }
    
    /**
     * Draw the text in the array, with each character's origin specified by
     * the pos array.
     *
     * @param text     The text to be drawn
     * @param index    The index of the first character to draw
     * @param count    The number of characters to draw, starting from index.
     * @param pos      Array of [x,y] positions, used to position each
     *                 character
     * @param paint    The paint used for the text (e.g. color, size, style)
     */
    public void drawPosText(char[] text, int index, int count, float[] pos,
                            Paint paint) {
        if (index < 0 || index + count > text.length || count*2 > pos.length) {
            throw new IndexOutOfBoundsException();
        }
        for(int i = index; i < index + count; i++) {
            this.drawText(String.valueOf(text[i]), pos[2 * i], pos[2 * i + 1], paint);
        }
    }

    /**
     * Draw the text in the array, with each character's origin specified by
     * the pos array.
     *
     * @param text  The text to be drawn
     * @param pos   Array of [x,y] positions, used to position each character
     * @param paint The paint used for the text (e.g. color, size, style)
     */
    public void drawPosText(String text, float[] pos, Paint paint) {
        if (text.length()*2 > pos.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int len = text.length();
        for(int i = 0; i < len; i++) {
            this.drawText(text.substring(i, i + 1), pos[2 * i], pos[2 * i + 1], paint);
        }
    }

    /**
     * Fill the entire canvas' bitmap (restricted to the current clip) with the
     * specified color, using srcover porterduff mode.
     *
     * @param color the color to draw onto the canvas
     */
    public void drawColor(int color) {
        this.drawARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Fill the entire canvas' bitmap (restricted to the current clip) with the
     * specified color and porter-duff xfermode.
     *
     * @param color the color to draw with
     * @param mode  the porter-duff mode to apply to the color
     */
    public void drawColor(int color, PorterDuff.Mode mode) {
        if (mode == Mode.CLEAR) {
            this.drawColor(color);
        } else {
            // TODO: Implement it using mode
            Log.e(TAG, "Only Mode.CLEAR is implemented now!");
        }
    }
    
	public void drawGradient(int startColor, int endColor, int angle) {
		String start = Color.toString(startColor);
		String end = Color.toString(endColor);
		/**
		 * @j2sNative
		 * var _canvas = null;
         * if (this.mBM != null) {
         *    _canvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _canvas = document.getElementById(this.activeCanvas);
         * }
		 * var _context = _canvas.getContext("2d");
		 * var grad = _context.createLinearGradient(0, 0, 0, this._height);
		 * grad.addColorStop(0, start);
		 * grad.addColorStop(1, end);
		 * _context.fillStyle = grad;
		 * _context.fillRect(0, 0, this._width, this._height);
		 */
		{
		}
	}

    /**
     * Draw a line segment with the specified start and stop x,y coordinates,
     * using the specified paint. NOTE: since a line is always "framed", the
     * Style is ignored in the paint.
     *
     * @param startX The x-coordinate of the start point of the line
     * @param startY The y-coordinate of the start point of the line
     * @param paint  The paint used to draw the line
     */
    public void drawLine(float startX, float startY, float stopX, float stopY,
            Paint paint) {
        setHTML5CanvasContext(paint);
        /**
         * @j2sNative
         * var _canvas = null;
         * if (this.mBM != null) {
         *    _canvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _canvas = document.getElementById(this.activeCanvas);
         * }
         * var _context = _canvas.getContext("2d");
         * _context.beginPath();
         * _context.moveTo(startX,startY);
         * _context.lineTo(stopX,stopY);
         * _context.stroke();
         */
        {}
  
        if(DEBUG){
            Log.d(TAG, "Draw line from ("+startX+","+startY+") to ("+stopX+","+stopY+")");
        }
    }
    
    /**
     * Draw a series of lines. Each line is taken from 4 consecutive values
     * in the pts array. Thus to draw 1 line, the array must contain at least 4
     * values. This is logically the same as drawing the array as follows:
     * drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
     * drawLine(pts[4], pts[5], pts[6], pts[7]) and so on.
     *
     * @param pts      Array of points to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param paint    The paint used to draw the points
     */ 
    public void drawLines(float[] pts, Paint paint) {
        for (int i = 0; (i + 3) < pts.length; i += 4) {
            this.drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], paint);
        }
    }
    
    /**
     * Draw the bitmap using the specified matrix.
     *
     * @param bitmap The bitmap to draw
     * @param matrix The matrix used to transform the bitmap when it is drawn
     * @param paint  May be null. The paint used to draw the bitmap
     */
    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) { 
        // Compute the tranform matrix
        Matrix tmp = new Matrix();
        tmp.setConcat(this.ctm, matrix);
        
        // Draw bitmap 
        setHTML5CanvasMatrix(tmp);
        drawBitmap(bitmap, 0, 0, paint);
        
        // Restore the matrix
        setHTML5CanvasMatrix(this.ctm);
    }
	
    /**
     * Draw the specified bitmap, with its top/left corner at (x,y), using
     * the specified paint, transformed by the current matrix.
     * 
     * <p>Note: if the paint contains a maskfilter that generates a mask which
     * extends beyond the bitmap's original width/height (e.g. BlurMaskFilter),
     * then the bitmap will be drawn as if it were in a Shader with CLAMP mode.
     * Thus the color outside of the original width/height will be the edge
     * color replicated.
     *
     * <p>If the bitmap and canvas have different densities, this function
     * will take care of automatically scaling the bitmap to draw at the
     * same density as the canvas.
     * 
     * @param bitmap The bitmap to be drawn
     * @param left   The position of the left side of the bitmap being drawn
     * @param top    The position of the top side of the bitmap being drawn
     * @param paint  The paint used to draw the bitmap (may be null)
     */
    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
        throwIfRecycled(bitmap);
        if (this.activeCanvas == null && this.mBM == null) {
            return;
        }
        
        if (paint != null) {
            Xfermode xfermode = paint.getXfermode();
            if (xfermode != null && xfermode instanceof PorterDuffXfermode) {
                if (xfermode.native_instance == PorterDuff.Mode.DST.nativeInt) {
                    // for dst mode, as HTML5 doesn't support DST mode natively,
                    // we just skip the drawing in MayLoon
                    return;
                }
            }
            setHTML5CanvasContext(paint);
        }
        
        /**
         * @j2sNative
         * if (!bitmap.ensureCachedCanvas(false, true)) return;
         * // draw offscreen canvas into onscreen canvas
         * var _activeCanvas = null;
         * if (this.mBM != null) {
         *    _activeCanvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _activeCanvas = document.getElementById(this.activeCanvas);
         * }
         * var activeContext = _activeCanvas.getContext("2d");
         * activeContext.drawImage(bitmap.mCachedCanvas, left, top);
         */
        {}
    }
    
    /**
     * Draw the specified bitmap, scaling/translating automatically to fill
     * the destination rectangle. If the source rectangle is not null, it
     * specifies the subset of the bitmap to draw.
     * 
     * <p>Note: if the paint contains a maskfilter that generates a mask which
     * extends beyond the bitmap's original width/height (e.g. BlurMaskFilter),
     * then the bitmap will be drawn as if it were in a Shader with CLAMP mode.
     * Thus the color outside of the original width/height will be the edge
     * color replicated.
     *
     * <p>This function <em>ignores the density associated with the bitmap</em>.
     * This is because the source and destination rectangle coordinate
     * spaces are in their respective densities, so must already have the
     * appropriate scaling factor applied.
     * 
     * @param bitmap The bitmap to be drawn
     * @param src    May be null. The subset of the bitmap to be drawn
     * @param dst    The rectangle that the bitmap will be scaled/translated
     *               to fit into
     * @param paint  May be null. The paint used to draw the bitmap
     */
    public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {
        if (dst == null) {
            throw new NullPointerException();
        }
        throwIfRecycled(bitmap);
        
        if (src == null) {
            src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        
        if (src.isEmpty() || dst.isEmpty()) {
            return;
        }

        if (this.activeCanvas == null && this.mBM == null) {
            return;
        }

        if (paint != null) {
            Xfermode xfermode = paint.getXfermode();
            if (xfermode != null && xfermode instanceof PorterDuffXfermode) {
                if (xfermode.native_instance == PorterDuff.Mode.DST.nativeInt) {
                    // for dst mode, as HTML5 doesn't support DST mode natively,
                    // we just skip the drawing in MayLoon
                    return;
                }
            }
            setHTML5CanvasContext(paint);
        }

        /**
         * @j2sNative
         * if (!bitmap.ensureCachedCanvas(false, true)) return;
         * // draw offscreen canvas into onscreen canvas
         * var _activeCanvas = null;
         * if (this.mBM != null) {
         *    _activeCanvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _activeCanvas = document.getElementById(this.activeCanvas);
         * }
         * var activeContext = _activeCanvas.getContext("2d");
         * activeContext.drawImage(bitmap.mCachedCanvas, src.left, src.top, src.width(), src.height(),
         *                                               dst.left, dst.top, dst.width(), dst.height());
         */{}
    }

    
    /**
     * Draw the specified bitmap, scaling/translating automatically to fill
     * the destination rectangle. If the source rectangle is not null, it
     * specifies the subset of the bitmap to draw.
     * 
     * <p>Note: if the paint contains a maskfilter that generates a mask which
     * extends beyond the bitmap's original width/height (e.g. BlurMaskFilter),
     * then the bitmap will be drawn as if it were in a Shader with CLAMP mode.
     * Thus the color outside of the original width/height will be the edge
     * color replicated.
     *
     * <p>This function <em>ignores the density associated with the bitmap</em>.
     * This is because the source and destination rectangle coordinate
     * spaces are in their respective densities, so must already have the
     * appropriate scaling factor applied.
     * 
     * @param bitmap The bitmap to be drawn
     * @param src    May be null. The subset of the bitmap to be drawn
     * @param dst    The rectangle that the bitmap will be scaled/translated
     *               to fit into
     * @param paint  May be null. The paint used to draw the bitmap
     */
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) { 	
        if (dst == null) {
            throw new NullPointerException();
        }
        throwIfRecycled(bitmap);

        if (src == null) {
            src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        
        if (src.isEmpty() || dst.isEmpty()) {
            return;
        }

        if (this.activeCanvas == null && this.mBM == null) {
            return;
        }

        if (paint != null)
            setHTML5CanvasContext(paint);

        /**
         * @j2sNative
         * if (!bitmap.ensureCachedCanvas(false, true)) return;
         * // draw offscreen canvas into onscreen canvas
         * var _activeCanvas = null;
         * if (this.mBM != null) {
         *    _activeCanvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _activeCanvas = document.getElementById(this.activeCanvas);
         * }
         * var activeContext = _activeCanvas.getContext("2d");
         * activeContext.drawImage(bitmap.mCachedCanvas, src.left, src.top, src.width(), src.height(),
         *                                               dst.left, dst.top, dst.width(), dst.height());
         */{}
    }
    
    /**
     * Treat the specified array of colors as a bitmap, and draw it. This gives
     * the same result as first creating a bitmap from the array, and then
     * drawing it, but this method avoids explicitly creating a bitmap object
     * which can be more efficient if the colors are changing often.
     *
     * @param colors Array of colors representing the pixels of the bitmap
     * @param offset Offset into the array of colors for the first pixel
     * @param stride The number of colors in the array between rows (must be
     *               >= width or <= -width).
     * @param x The X coordinate for where to draw the bitmap
     * @param y The Y coordinate for where to draw the bitmap
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @param hasAlpha True if the alpha channel of the colors contains valid
     *                 values. If false, the alpha byte is ignored (assumed to
     *                 be 0xFF for every pixel).
     * @param paint  May be null. The paint used to draw the bitmap
     */
    public void drawBitmap(int[] colors, int offset, int stride, float x,
                           float y, int width, int height, boolean hasAlpha,
                           Paint paint) {
        Bitmap bitmap = Bitmap.createBitmap(colors, offset, stride, width, height, Config.ARGB_8888);
        drawBitmap(bitmap, x, y, paint);
    }
	
	/**
	 * *************************************************************
	 * In these functions, both fg and bg canvas need to change
	 */
	public void setDimension(int width, int height) {
		_width = width;
		_height = height;
	}

	public void translate(float dx, float dy) {
		_ddx += dx;
		_ddy += dy;
		if(this.mBM != null){
			/**
			 * @j2sNative
			 * var context = this.mBM.mCachedCanvas.getContext("2d");
		     * context.translate(dx, dy);
			 */{}
		     // track the transformation in ctm
		    this.ctm.preTranslate(dx, dy);
			return;
		} 
		
		/**
		  @j2sNative
		  var _canvas = document.getElementById(this.activeCanvas);
		  if (_canvas != null) {
		     var _context = _canvas.getContext("2d");
		     _context.translate(dx, dy);
		  }
		 */{}

        // track the transformation in ctm
        this.ctm.preTranslate(dx, dy);
	}

    /**
     * Preconcat the current matrix with the specified scale.
     * 
     * @param sx The amount to scale in X
     * @param sy The amount to scale in Y
     */
    public void scale(float sx, float sy) {
        if(this.mBM != null){
            /**
             * @j2sNative
             * var context = this.mBM.mCachedCanvas.getContext("2d");
             * context.scale(sx, sy);
             */{}
             // track the transformation in ctm
            this.ctm.preScale(sx, sy);
            return;
        }
        
        /**
          @j2sNative
          var _canvas = document.getElementById(this.activeCanvas);
          var _context = _canvas.getContext("2d");
          _context.scale(sx, sy);
         */{}

        // track the transformation in ctm
        this.ctm.preScale(sx, sy); 
    }

    /**
     * Preconcat the current matrix with the specified scale.
     * 
     * @param sx The amount to scale in X
     * @param sy The amount to scale in Y
     * @param px The x-coord for the pivot point (unchanged by the rotation)
     * @param py The y-coord for the pivot point (unchanged by the rotation)
     */
    public final void scale(float sx, float sy, float px, float py) {
        translate(px, py);
        scale(sx, sy);
        translate(-px, -py);
    }

	public void clear(){
		/** 
		 * @j2sNative
		 * var _activeCanvas = null;
         * if (this.mBM != null) {
         *    _activeCanvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _activeCanvas = document.getElementById(this.activeCanvas);
         * }
		 * if(!_activeCanvas){
		 * 	return;
		 * }
		  //MayLoon: Please refer to 
		  //http://jsperf.com/ctx-clearrect-vs-canvas-width-canvas-width/2
		  //http://www.html5rocks.com/en/tutorials/canvas/performance/
		  //for more detail about redraw a canvas
		  //_canvas.width = _canvas.width;
		 * var _context = _activeCanvas.getContext("2d");
		 * _context.clearRect(0,0,_activeCanvas.width,_activeCanvas.height);
		 */
		{
		}
	}
    /**
     * Preconcat the current matrix with the specified rotation.
     *
     * @param degrees The amount to rotate, in degrees
     * @param px The x-coord for the pivot point (unchanged by the rotation)
     * @param py The y-coord for the pivot point (unchanged by the rotation)
     */
    public final void rotate(float degrees, float px, float py) {
        translate(px, py);
        rotate(degrees);
        translate(-px, -py);
    }
    
    /**
     * Preconcat the current matrix with the specified rotation.
     *
     * @param degrees The amount to rotate, in degrees
     */
    public void rotate(float degrees) {
        if(this.mBM != null){
            /**
             * @j2sNative
             * var context = this.mBM.mCachedCanvas.getContext("2d");
             * context.rotate(degrees * Math.PI / 180);
             */{}
             // track the transformation in ctm
             this.ctm.preRotate(degrees);
            return;
        } 
        
        /**
         * @j2sNative 
         * var _canvas = document.getElementById(this.activeCanvas); 
         * var _context = _canvas.getContext("2d");
         * _context.rotate(degrees * Math.PI / 180); 
         */{}

        // track the transformation in ctm
        this.ctm.preRotate(degrees);
    }
    
    void setHTML5CanvasMatrix(Matrix ctm) {
        float[] mt = new float[9];
        ctm.getValues(mt);

        if (mt[Matrix.MPERSP_0] != 0 || mt[Matrix.MPERSP_1] != 0 || mt[Matrix.MPERSP_2] != 1) {
            Log.e(TAG, "Not support perspective matrix!");
        }
        
        float MSCALE_X = mt[Matrix.MSCALE_X];
        float MSKEW_Y  = mt[Matrix.MSKEW_Y];
        float MSKEW_X  = mt[Matrix.MSKEW_X];
        float MSCALE_Y = mt[Matrix.MSCALE_Y];
        float MTRANS_X = mt[Matrix.MTRANS_X];
        float MTRANS_Y = mt[Matrix.MTRANS_Y];
        
        if(this.mBM != null){
            /**
             * @j2sNative
             * var context = this.mBM.mCachedCanvas.getContext("2d");
             * context.setTransform(MSCALE_X, MSKEW_Y, MSKEW_X, 
             *                      MSCALE_Y, MTRANS_X, MTRANS_Y);
             */{}
             return;
        }

        /**
         * @j2sNative
         *  var _canvas = document.getElementById(this.activeCanvas);
         *  var _context = _canvas.getContext("2d");
         *  _context.setTransform(MSCALE_X, MSKEW_Y, MSKEW_X, 
         *                          MSCALE_Y, MTRANS_X, MTRANS_Y);
         */{}
    }
    
    public void skew(float sx, float sy) {
        // track the transformation in ctm
        this.ctm.preSkew(sx, sy);

        setHTML5CanvasMatrix(this.ctm);
    }

    /**
     * Preconcat the current matrix with the specified matrix.
     *
     * @param matrix The matrix to preconcatenate with the current matrix
     */
    public void concat(Matrix matrix) {
        // track the transformation in ctm
        this.ctm.preConcat(matrix);

        setHTML5CanvasMatrix(this.ctm);
    }
    
    /**
     * Completely replace the current matrix with the specified matrix. If the
     * matrix parameter is null, then the current matrix is reset to identity.
     *
     * @param matrix The matrix to replace the current matrix with. If it is
     *               null, set the current matrix to identity.
     */
    public void setMatrix(Matrix matrix) {
        // track the transformation in ctm
        if (matrix == null) {
            this.ctm.reset();
        } else {
            this.ctm.set(matrix);
        }

        setHTML5CanvasMatrix(this.ctm);
    }
    
    /**
     * Return, in ctm, the current transformation matrix. This does not alter
     * the matrix in the canvas, but just returns a copy of it.
     */
    public void getMatrix(Matrix ctm) {
        ctm.set(this.ctm);
    }
    
    /**
     * Return a new matrix with a copy of the canvas' current transformation
     * matrix.
     */
    public final Matrix getMatrix() {
        Matrix m = new Matrix();
        getMatrix(m);
        return m;
    }
    
    public int save() {
        /**
         * @j2sNative 
         * var canvas = null;
         * if (this.mBM != null) { 
         *     canvas = this.mBM.mCachedCanvas;
         * } else {
         *     canvas = document.getElementById(this.activeCanvas);
         * } 
         * if (canvas != null) {
         *    var context = canvas.getContext("2d");
         *    context.save();
         * }
         */{}

        saveCount++;
        CanvasState state = new CanvasState();
        state._rect = new Rect(_ddx, _ddy, _ddx + _width, _ddy + _height);
        state._clipBounds = new Rect(mClipBounds);
        state._ctm = new Matrix(this.ctm);
        transList.add(state);
        return saveCount;
    }
    
    /**
     * This behaves the same as save(), but in addition it allocates an
     * offscreen bitmap. All drawing calls are directed there, and only when
     * the balancing call to restore() is made is that offscreen transfered to
     * the canvas (or the previous layer). Subsequent calls to translate,
     * scale, rotate, skew, concat or clipRect, clipPath all operate on this
     * copy. When the balancing call to restore() is made, this copy is
     * deleted and the previous matrix/clip state is restored.
     *
     * @param bounds May be null. The maximum size the offscreen bitmap
     *               needs to be (in local coordinates)
     * @param paint  This is copied, and is applied to the offscreen when
     *               restore() is called.
     * @param saveFlags  see _SAVE_FLAG constants
     * @return       value to pass to restoreToCount() to balance this save()
     */
    public int saveLayer(RectF bounds, Paint paint, int saveFlags) {
        return save();
    }

    /**
     * Helper version of saveLayer() that takes 4 values rather than a RectF.
     */
    public int saveLayer(float left, float top, float right, float bottom,
                         Paint paint, int saveFlags) {
        //HTML5 supports save() and restore() menthod only
        return save();
    }

    public void restore() {
        if (saveCount <= 1) {
            throw new IllegalStateException("Underflow in restore");
        }
        /**
         * @j2sNative
         * var canvas = null;
         * if (this.mBM != null) {
         *     canvas = this.mBM.mCachedCanvas;
         * } else {
         *     canvas = document.getElementById(this.activeCanvas);
         * }
         * if (canvas != null) {
         *    var context = canvas.getContext("2d");
         *    context.restore();
         * }
         */{}
         
        Rect cache = ((CanvasState) transList.get(saveCount - 1))._rect;
        _ddx = cache.left;
        _ddy = cache.top;
        _width = cache.right - cache.left;
        _height = cache.bottom - cache.top;
        Rect clip = ((CanvasState) transList.get(saveCount - 1))._clipBounds;
        this.mClipBounds.copyFrom(clip);
        Matrix mt = ((CanvasState) transList.get(saveCount - 1))._ctm;
        this.ctm.set(mt);

        transList.remove(saveCount - 1);
        saveCount--;
    }

    public int getSaveCount() {
        return saveCount;
    }

    public void restoreToCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Underflow in restoreToCount");
        }
        while (saveCount >= count) {
            restore();
        }
    }

    public boolean clipRect(float left, float top,
                            float right, float bottom) {
        /**
         * @j2sNative 
         * var canvas = null;
         * if (this.mBM != null) { 
         *     canvas = this.mBM.mCachedCanvas;
         * } else { 
         *     canvas = document.getElementById(this.activeCanvas);
         * } 
         * if (canvas != null) {
         *    var _context = canvas.getContext("2d");
         *    _context.beginPath();
         *    _context.rect(left,top,right-left,bottom-top);
         *    _context.closePath();
         *    _context.clip();
         * }
         */{}

        mClipBounds.set((int)left, (int)top, (int)right, (int)bottom);
        return true;
    }
    
    /**
     * Intersect the current clip with the specified rectangle, which is
     * expressed in local coordinates.
     *
     * @param rect The rectangle to intersect with the current clip.
     * @return true if the resulting clip is non-empty
     */
    public boolean clipRect(Rect rect) {
        return this.clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }


    /**
     * Draw the specified Rect using the specified Paint. The rectangle
     * will be filled or framed based on the Style in the paint.
     *
     * @param r        The rectangle to be drawn.
     * @param paint    The paint used to draw the rectangle
     */
	public void drawRect(Rect r, Paint paint) {
	    drawRect(r.left, r.top, r.right, r.bottom, paint);
	}
	
    /**
     * Draw the specified Rect using the specified paint. The rectangle will
     * be filled or framed based on the Style in the paint.
     *
     * @param rect  The rect to be drawn
     * @param paint The paint used to draw the rect
     */
    public void drawRect(RectF rect, Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }
	 
    /**
     * Draw the specified Rect using the specified paint. The rectangle will
     * be filled or framed based on the Style in the paint.
     *
     * @param left   The left side of the rectangle to be drawn
     * @param top    The top side of the rectangle to be drawn
     * @param right  The right side of the rectangle to be drawn
     * @param bottom The bottom side of the rectangle to be drawn
     * @param paint  The paint used to draw the rect
     */
    public void drawRect(float left, float top, float right, float bottom,
                         Paint paint) {
        Path path = new Path();
        path.addRect(left, top, right, bottom, Direction.CW);
        this.drawPath(path, paint);
    }
	    
    /**
     * Draw the specified circle using the specified paint. If radius is <= 0,
     * then nothing will be drawn. The circle will be filled or framed based
     * on the Style in the paint.
     *
     * @param cx     The x-coordinate of the center of the cirle to be drawn
     * @param cy     The y-coordinate of the center of the cirle to be drawn
     * @param radius The radius of the cirle to be drawn
     * @param paint  The paint used to draw the circle
     */
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        Path path = new Path();
        path.addCircle(cx, cy, radius, Direction.CW);
        this.drawPath(path, paint);
    }
	    
    /**
     * Draw the specified oval using the specified paint. The oval will be
     * filled or framed based on the Style in the paint.
     *
     * @param oval The rectangle bounds of the oval to be drawn
     */
    public void drawOval(RectF oval, Paint paint) {
        if (oval == null) {
            throw new NullPointerException();
        }
        
        Path path = new Path();
        path.addOval(oval, Direction.CW);
        this.drawPath(path, paint);
    }
	
    /**
     * Draw the specified arc, which will be scaled to fit inside the
     * specified oval. If the sweep angle is >= 360, then the oval is drawn
     * completely. Note that this differs slightly from SkPath::arcTo, which
     * treats the sweep angle mod 360.
     *
     * @param oval       The bounds of oval used to define the shape and size
     *                   of the arc
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param useCenter If true, include the center of the oval in the arc, and
                        close it if it is being stroked. This will draw a wedge
     * @param paint      The paint used to draw the arc
     */
    public void drawArc(RectF oval, float startAngle, float sweepAngle,
                        boolean useCenter, Paint paint) {
        if (oval == null) {
            throw new NullPointerException();
        }
        
        if (sweepAngle >= 360) {
            this.drawOval(oval, paint);
        } else {
            Path path = new Path();
            if (useCenter) {
                path.moveTo(oval.centerX(), oval.centerY());
            }
            path.arcTo(oval, startAngle, sweepAngle, !useCenter);
            if (useCenter) {
                path.close();
            }
            this.drawPath(path, paint);
        }
    }
	    
    /**
     * Draw the specified round-rect using the specified paint. The roundrect
     * will be filled or framed based on the Style in the paint.
     *
     * @param rect  The rectangular bounds of the roundRect to be drawn
     * @param rx    The x-radius of the oval used to round the corners
     * @param ry    The y-radius of the oval used to round the corners
     * @param paint The paint used to draw the roundRect
     */
    public void drawRoundRect(RectF rect, float rx, float ry, Paint paint) {
        if (rect == null) {
            throw new NullPointerException();
        }
        
        if (rx > 0 && ry > 0) {
            Path path = new Path();
            path.addRoundRect(rect, rx, ry, Direction.CW);
            this.drawPath(path, paint);
        } else {
            this.drawRect(rect, paint);
        }
        
    }

    private static void throwIfRecycled(Bitmap bitmap) {
        if (bitmap.isRecycled()) {
            throw new RuntimeException(
                    "Canvas: trying to use a recycled bitmap " + bitmap);
        }
    }
    
    /**
     * Draw the specified path using the specified paint. The path will be
     * filled or framed based on the Style in the paint.
     *
     * @param path  The path to be drawn
     * @param paint The paint used to draw the path
     */
    public void drawPath(Path path, Paint paint) {
        if (paint != null) {
            Xfermode xfermode = paint.getXfermode();
            if (xfermode != null && xfermode instanceof PorterDuffXfermode) {
                if (xfermode.native_instance == PorterDuff.Mode.DST.nativeInt) {
                    // for dst mode, as HTML5 doesn't support DST mode natively,
                    // we just skip the drawing in MayLoon
                    return;
                }
            }
            setHTML5CanvasContext(paint);
        }
        
        if (this.mBM != null) {
            path.drawOnCanvas(null, this.mBM, paint);
        } else {
            path.drawOnCanvas(this.activeCanvas, null, paint);
        }
    }

    /**
     * Draw a series of points. Each point is centered at the coordinate
     * specified by pts[], and its diameter is specified by the paint's stroke
     * width (as transformed by the canvas' CTM), with special treatment for a
     * stroke width of 0, which always draws exactly 1 pixel (or at most 4 if
     * antialiasing is enabled). The shape of the point is controlled by the
     * paint's Cap type. The shape is a square, unless the cap type is Round, in
     * which case the shape is a circle.
     * 
     * @param pts Array of points to draw [x0 y0 x1 y1 x2 y2 ...]
     * @param offset Number of values to skip before starting to draw.
     * @param count The number of values to process, after skipping offset of
     *            them. Since one point uses two values, the number of "points"
     *            that are drawn is really (count >> 1).
     * @param paint The paint used to draw the points
     */
    public void drawPoints(float[] pts, int offset, int count,
            Paint paint) {
        if ((offset | count) < 0 || offset + count > pts.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        for (int i = offset; i < pts.length; i = i + 2) {
            drawPoint(pts[i], pts[i + 1], paint);
        }
    }

    /**
     * Helper for drawPoints() that assumes you want to draw the entire array
     */
    public void drawPoints(float[] pts, Paint paint) {
        drawPoints(pts, 0, pts.length, paint);
    }

    /**
     * Helper for drawPoints() for drawing a single point.
     */
    public void drawPoint(float x, float y, Paint paint) {
        float diameter = 1;
        if (paint != null) {
            Xfermode xfermode = paint.getXfermode();
            if (xfermode != null && xfermode instanceof PorterDuffXfermode) {
                if (xfermode.native_instance == PorterDuff.Mode.DST.nativeInt) {
                    // for dst mode, as HTML5 doesn't support DST mode natively,
                    // we just skip the drawing in MayLoon
                    return;
                }
            }
            setHTML5CanvasContext(paint);
            diameter = paint.getStrokeWidth();
        }
        
        Path path = new Path();
        float radius = diameter / 2;
        if (paint != null && paint.getStrokeCap().equals(Cap.ROUND)) {
            path.addCircle(x, y, radius, Direction.CW);
        } else {
            path.addRect(x - radius, y - radius, x + radius, y + radius, Direction.CW);
        }

        this.drawPath(path, paint);

    }

    public int getWidth() {
        /**
         * @j2sNative
         * var _canvas = null;
         * if (this.mBM != null) {
         *    _canvas = this.mBM.mCachedCanvas;
         * } else {
         *    _canvas = document.getElementById(this.activeCanvas);
         * }
         * if (_canvas != null) {
         *     this._width = _canvas.width;
         * } else {
         *     this._width = 0;
         * }
         */{}
        return _width;
    }

    public int getHeight() {
        /**
         * @j2sNative
         * var _canvas = null;
         * if (this.mBM != null) {
         *    _canvas = this.mBM.mCachedCanvas;
         * } else {
         *    _canvas = document.getElementById(this.activeCanvas);
         * }
         * if (_canvas != null) {
         *     this._height = _canvas.width;
         * } else {
         *     this._height = 0;
         * }
         */{}
        return _height;
    }

    /**
     * <p>Returns the target density of the canvas.  The default density is
     * derived from the density of its backing bitmap, or
     * {@link Bitmap#DENSITY_NONE} if there is not one.</p>
     *
     * @return Returns the current target density of the canvas, which is used
     * to determine the scaling factor when drawing a bitmap into it.
     *
     * @see #setDensity(int)
     * @see Bitmap#getDensity() 
     */
    public int getDensity() {
        return mDensity;
    }
	
	public void setBitmap(Bitmap bm){
		mBM = bm;
		if(mBM!=null){
	        // Ensure Cached Canvas is created for this bitmap
	        this.mBM.ensureCachedCanvas(false, false);
		}
	}
	
	 /**
     * Retrieve the clip bounds, returning true if they are non-empty.
     *
     * @param bounds Return the clip bounds here. If it is null, ignore it but
     *               still return true if the current clip is non-empty.
     * @return true if the current clip is non-empty.
     */
    public boolean getClipBounds(Rect bounds) {
        if (mClipBounds != null) {
            if (bounds != null) {
                bounds.copyFrom(mClipBounds);
            }
            return true;
        }
        return false;
    }

    /**
     * @j2sNative
     * console.log("Missing method: drawPicture");
     */
    @MayloonStubAnnotation()
    public void drawPicture(Object picture, Rect dst) {
        System.out.println("Stub" + " Function : drawPicture");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clipRegion");
     */
    @MayloonStubAnnotation()
    public boolean clipRegion(Region region, Region.Op op) {
        System.out.println("Stub" + " Function : clipRegion");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clipRegion");
     */
    @MayloonStubAnnotation()
    public boolean clipRegion(Region region) {
        System.out.println("Stub" + " Function : clipRegion");
        return true;
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

    /**
     * @j2sNative
     * console.log("Missing method: getClipBounds");
     */
    @MayloonStubAnnotation()
    public final Rect getClipBounds() {
        System.out.println("Stub" + " Function : getClipBounds");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setDensity");
     */
    @MayloonStubAnnotation()
    public void setDensity(int density) {
        System.out.println("Stub" + " Function : setDensity");
        return;
    }

    
    
    public void drawPaint(Paint paint) {
        //System.out.println("Stub" + " Function : drawPaint");
    	setHTML5CanvasContext(paint);
    	/**
         * @j2sNative
         * var _canvas = null;
         * if (this.mBM != null) {
         *    _canvas = this.mBM.mCachedCanvas;
         *    // Update the bitmap cached canvas dirty flag
         *    this.mBM.mIsCachedCanvasDirty = true;
         * } else {
         *    _canvas = document.getElementById(this.activeCanvas);
         * }
         * var _context = _canvas.getContext("2d");
         * _context.fillRect(0, 0, _canvas.width, _canvas.height); 
         */
        {}
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clipRect");
     */
    @MayloonStubAnnotation()
    public boolean clipRect(RectF rect) {
        System.out.println("Stub" + " Function : clipRect");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clipRect");
     */
    @MayloonStubAnnotation()
    public boolean clipRect(Rect rect, Region.Op op) {
        System.out.println("Stub" + " Function : clipRect");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clipRect");
     */
    @MayloonStubAnnotation()
    public boolean clipRect(RectF rect, Region.Op op) {
        System.out.println("Stub" + " Function : clipRect");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clipRect");
     */
    @MayloonStubAnnotation()
    public boolean clipRect(float left, float top, float right, float bottom,
            Region.Op op) {
        System.out.println("Stub" + " Function : clipRect");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: save");
     */
    @MayloonStubAnnotation()
    public int save(int saveFlags) {
        System.out.println("Stub" + " Function : save");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: freeGlCaches");
     */
    @MayloonStubAnnotation()
    public static void freeGlCaches() {
        System.out.println("Stub" + " Function : freeGlCaches");
        return;
    }

    public int saveLayerAlpha(int left, int top, int right, int bottom, int Alpha, int saveFlags) {
        /**
         * @j2sNative 
         * var canvas = null;
         * if (this.mBM != null) { 
         *     canvas = this.mBM.mCachedCanvas;
         * } else {
         *     canvas = document.getElementById(this.activeCanvas);
         * } 
         * if (canvas != null) {
         *    var context = canvas.getContext("2d");
         *    var alpha = (Alpha & 0xFF) / 255;
         *    context.globalAlpha = alpha;
         *    context.save();
         * }
         */{}

        saveCount++;
        CanvasState state = new CanvasState();
        _ddx = left;
        _ddy = top;
        _width = right - left;
        _height = bottom - top;
        state._rect = new Rect(_ddx, _ddy, _ddx + _width, _ddy + _height);
        state._clipBounds = new Rect(mClipBounds);
        state._ctm = new Matrix(this.ctm);
        transList.add(state);
        return saveCount;
    }
}
