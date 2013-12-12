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

import android.graphics.BitmapFactory.Res_png_9patch;
import android.util.Log;


/**
 * The NinePatch class permits drawing a bitmap in nine sections.
 * The four corners are unscaled; the four edges are scaled in one axis,
 * and the middle is scaled in both axes. Normally, the middle is 
 * transparent so that the patch can provide a selection about a rectangle.
 * Essentially, it allows the creation of custom graphics that will scale the 
 * way that you define, when content added within the image exceeds the normal
 * location of the graphic. For a thorough explanation of a NinePatch image, 
 * read the discussion in the 
 * <a href="{@docRoot}guide/topics/graphics/2d-graphics.html#nine-patch">2D
 * Graphics</a> document.
 * <p>
 * The <a href="{@docRoot}guide/developing/tools/draw9patch.html">Draw 9-Patch</a> 
 * tool offers an extremely handy way to create your NinePatch images,
 * using a WYSIWYG graphics editor.
 * </p>
 */
public class NinePatch {
    public static final boolean debug = true;
    /** 
     * Create a drawable projection from a bitmap to nine patches.
     *
     * @param bitmap    The bitmap describing the patches.
     * @param chunk     The 9-patch data chunk describing how the underlying
     *                  bitmap is split apart and drawn.
     * @param srcName   The name of the source for the bitmap. Might be null.
     */
    public NinePatch(Bitmap bitmap, byte[] chunk, String srcName) {
        mBitmap = bitmap;
        mChunk = Res_png_9patch.deserialize(chunk);
        mSrcName = srcName;
        validateNinePatchChunk(mBitmap, mChunk);
    }
    
    /** 
     * Create a drawable projection from a bitmap to nine patches.
     *
     * @param bitmap    The bitmap describing the patches.
     * @param chunk     The 9-patch data chunk describing how the underlying
     *                  bitmap is split apart and drawn.
     * @param srcName   The name of the source for the bitmap. Might be null.
     */
    public NinePatch(Bitmap bitmap, Res_png_9patch chunk, String srcName) {
        mBitmap = bitmap;
        mChunk = chunk;
        mSrcName = srcName;
        validateNinePatchChunk(mBitmap, chunk);
    }

    /**
     * @hide
     */
    public NinePatch(NinePatch patch) {
        mBitmap = patch.mBitmap;
        mChunk = patch.mChunk;
        mSrcName = patch.mSrcName;
        if (patch.mPaint != null) {
            mPaint = new Paint(patch.mPaint);
        }
        validateNinePatchChunk(mBitmap, mChunk);
    }

    public void setPaint(Paint p) {
        mPaint = p;
    }
    
    /** 
     * Draw a bitmap of nine patches.
     *
     * @param canvas    A container for the current matrix and clip used to draw the bitmap.
     * @param location  Where to draw the bitmap.
     */
    public void draw(Canvas canvas, RectF location) {
        draw(canvas, location, mPaint, canvas.mDensity, mBitmap.getDensity());
    }
    
    /** 
     * Draw a bitmap of nine patches.
     *
     * @param canvas    A container for the current matrix and clip used to draw the bitmap.
     * @param location  Where to draw the bitmap.
     */
    public void draw(Canvas canvas, Rect location) {
        draw(canvas, new RectF(location), mPaint, canvas.mDensity, mBitmap.getDensity());
    }

    /** 
     * Draw a bitmap of nine patches.
     *
     * @param canvas    A container for the current matrix and clip used to draw the bitmap.
     * @param location  Where to draw the bitmap.
     * @param paint     The Paint to draw through.
     */
    public void draw(Canvas canvas, Rect location, Paint paint) {
        draw(canvas, new RectF(location), paint, canvas.mDensity, mBitmap.getDensity());
    }
    
    private void draw(Canvas canvas, RectF location, Paint paint, int destDensity, int srcDensity) {
        Res_png_9patch chunk = mBitmap.getNinePatch();

        if (destDensity == srcDensity || destDensity == 0
                || srcDensity == 0) {
            Log.v("NinePatch", "Drawing unscaled 9-patch: (" + location.left + "," + location.top
                    + ")-(" + location.right + "," + location.bottom + ")");
            NinePatch_Draw(canvas, location, mBitmap, chunk, paint);
        } else {
            canvas.save();

            float scale = destDensity / srcDensity;
            canvas.translate(location.left, location.top);
            canvas.scale(scale, scale);

            location.right = (location.right - location.left) / scale;
            location.bottom = (location.bottom - location.top) / scale;
            location.left = location.top = 0;
            Log.v("NinePatch", "Drawing unscaled 9-patch: (" + location.left + "," + location.top
                    + ")-(" + location.right + "," + location.bottom + ")" + " " + "srcDensity="
                    + srcDensity + " destDensity=" + destDensity);

            NinePatch_Draw(canvas, location, mBitmap, chunk, paint);

            canvas.restore();
        }
        
        // In MayLoon, just draw the original bitmap.
        //canvas.drawBitmap(mBitmap, null, location, paint);
    }
    
    private void NinePatch_Draw(Canvas canvas, RectF location, Bitmap bitmap, Res_png_9patch chunk,
            Paint paint) {
        Paint defaultPaint = new Paint();
        if (null == paint) {
            // matches default dither in NinePatchDrawable.java.
            defaultPaint.setDither(true);
            paint = defaultPaint;
        }

        if (debug) {
            Log.v("NinePatch",
                    "======== ninepatch location [" + location.width() + " " + location.height()
                            + "]");
            Log.v("NinePatch",
                    "======== ninepatch paint bm [" + bitmap.getWidth() + "," + bitmap.getHeight()
                            + "]");
            Log.v("NinePatch", "======== ninepatch xDivs [" + chunk.xDivs[0] + "," + chunk.xDivs[1]
                    + "]");
            Log.v("NinePatch", "======== ninepatch yDivs [" + chunk.yDivs[0] + "," + chunk.yDivs[1]
                    + "]");
        }

        if (location.isEmpty() ||
                bitmap.getWidth() == 0 || bitmap.getHeight() == 0 /*
                                                                   * || (paint
                                                                   * && paint->
                                                                   * getXfermode
                                                                   * () == NULL
                                                                   * &&
                                                                   * paint->getAlpha
                                                                   * () == 0)
                                                                   */)
        {
            if (debug)
                Log.v("NinePatch", "======== abort ninepatch draw");
            return;
        }

        // boolean hasXfer = paint.getXfermode() != null;
        boolean hasXfer = false; // MayLoon does not support xfermode now
        RectF dst = new RectF();
        RectF src = new RectF();

        int x0 = chunk.xDivs[0];
        int y0 = chunk.yDivs[0];
        int initColor = paint.getColor();
        byte numXDivs = chunk.numXDivs;
        byte numYDivs = chunk.numYDivs;
        int i;
        int j;
        int colorIndex = 0;
        int color;
        boolean xIsStretchable;
        boolean initialXIsStretchable = (x0 == 0);
        boolean yIsStretchable = (y0 == 0);
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        float[] dstRights = new float[numXDivs + 1];
        boolean dstRightsHaveBeenCached = false;

        int numStretchyXPixelsRemaining = 0;
        for (i = 0; i < numXDivs; i += 2) {
            numStretchyXPixelsRemaining += chunk.xDivs[i + 1] - chunk.xDivs[i];
        }
        int numFixedXPixelsRemaining = bitmapWidth - numStretchyXPixelsRemaining;
        int numStretchyYPixelsRemaining = 0;
        for (i = 0; i < numYDivs; i += 2) {
            numStretchyYPixelsRemaining += chunk.yDivs[i + 1] - chunk.yDivs[i];
        }
        int numFixedYPixelsRemaining = bitmapHeight - numStretchyYPixelsRemaining;

        src.top = 0;
        dst.top = location.top;
        // The first row always starts with the top being at y=0 and the bottom
        // being either yDivs[1] (if yDivs[0]=0) of yDivs[0]. In the former case
        // the first row is stretchable along the Y axis, otherwise it is fixed.
        // The last row always ends with the bottom being bitmap.height and the
        // top being either yDivs[numYDivs-2] (if yDivs[numYDivs-1]=bitmap.height)
        // or yDivs[numYDivs-1]. In the former case the last row is stretchable
        // along the Y axis, otherwise it is fixed.
        //
        // The first and last columns are similarly treated with respect to the X
        // axis.
        // The above is to help explain some of the special casing that goes on
        // the code below.

        // The initial yDiv and whether the first row is considered stretchable
        // or not depends on whether yDiv[0] was zero or not.
        for (j = yIsStretchable ? 1 : 0; j <= numYDivs && src.top < bitmapHeight; j++, yIsStretchable = !yIsStretchable) {
            src.left = 0;
            dst.left = location.left;
            if (j == numYDivs) {
                src.bottom = bitmapHeight;
                dst.bottom = location.bottom;
            } else {
                src.bottom = chunk.yDivs[j];
                float srcYSize = src.bottom - src.top;
                if (yIsStretchable) {
                    dst.bottom = dst.top + calculateStretch(location.bottom, dst.top,
                            srcYSize,
                            numStretchyYPixelsRemaining,
                            numFixedYPixelsRemaining);
                    numStretchyYPixelsRemaining -= srcYSize;
                } else {
                    dst.bottom = dst.top + srcYSize;
                    numFixedYPixelsRemaining -= srcYSize;
                }
            }

            xIsStretchable = initialXIsStretchable;
            // The initial xDiv and whether the first column is considered
            // stretchable or not depends on whether xDiv[0] was zero or not.
            for (i = xIsStretchable ? 1 : 0; i <= numXDivs && src.left < bitmapWidth; i++, xIsStretchable = !xIsStretchable) {
                color = chunk.colors[colorIndex++];
                if (i == numXDivs) {
                    src.right = bitmapWidth;
                    dst.right = location.right;
                } else {
                    src.right = chunk.xDivs[i];
                    if (dstRightsHaveBeenCached) {
                        dst.right = dstRights[i];
                    } else {
                        float srcXSize = src.right - src.left;
                        if (xIsStretchable) {
                            dst.right = dst.left + calculateStretch(location.right, dst.left,
                                    srcXSize,
                                    numStretchyXPixelsRemaining,
                                    numFixedXPixelsRemaining);
                            numStretchyXPixelsRemaining -= srcXSize;
                        } else {
                            dst.right = dst.left + srcXSize;
                            numFixedXPixelsRemaining -= srcXSize;
                        }
                        dstRights[i] = dst.right;
                    }
                }
                // If this horizontal patch is too small to be displayed, leave
                // the destination left edge where it is and go on to the next
                // patch
                // in the source.
                if (src.left >= src.right) {
                    src.left = src.right;
                    continue;
                }
                // Make sure that we actually have room to draw any bits
                if (dst.right <= dst.left || dst.bottom <= dst.top) {
                    // goto nextDiv;
                } else {
                    // Not handle this in MayLoon now.
                    // // If this patch is transparent, skip and don't draw.
                    // if (color == Res_png_9patch.TRANSPARENT_COLOR &&
                    // !hasXfer) {
                    // if (outRegion) {
                    // if (*outRegion == NULL) {
                    // *outRegion = new SkRegion();
                    // }
                    // SkIRect idst;
                    // dst.round(&idst);
                    // //LOGI("Adding trans rect: (%d,%d)-(%d,%d)\n",
                    // // idst.left, idst.top, idst.right, idst.bottom);
                    // (*outRegion)->op(idst, SkRegion::kUnion_Op);
                    // }
                    // goto nextDiv;
                    // }
                    if (canvas != null) {
                        drawStretchyPatch(canvas, src, dst, bitmap, paint, initColor,
                                color, hasXfer);
                    }
                }

                src.left = src.right;
                dst.left = dst.right;
            }
            src.top = src.bottom;
            dst.top = dst.bottom;
            dstRightsHaveBeenCached = true;
        }
    }

    private float calculateStretch(float boundsLimit, float startingPoint,
            float srcSpace, float numStrechyPixelsRemaining,
            float numFixedPixelsRemaining) {
        float spaceRemaining = boundsLimit - startingPoint;
        float stretchySpaceRemaining =
                spaceRemaining - numFixedPixelsRemaining;
        return srcSpace * stretchySpaceRemaining /
                numStrechyPixelsRemaining;
    }
    
    private void drawStretchyPatch(Canvas canvas, RectF src, RectF dst,
            Bitmap bitmap, Paint paint,
            int initColor, int colorHint,
            boolean hasXfer) {
        if (colorHint != Res_png_9patch.NO_COLOR) {
            int modAlpha = (int) (Color.alpha(colorHint) * paint.getAlpha() / 256.0f);
            int color = Color.argb(modAlpha, Color.red(colorHint), Color.green(colorHint), Color.blue(colorHint));
            paint.setColor(color);
            canvas.drawRect(dst, paint);
            paint.setColor(initColor);
        } else if (src.width() == 1 && src.height() == 1) {
            int c = bitmap.getPixel((int)src.left, (int)src.top);
            if (0 != c || hasXfer) {
                int prev = paint.getColor();
                paint.setColor(c);
                canvas.drawRect(dst, paint);
                paint.setColor(prev);
            }
        } else {
            Rect tmpSrc = new Rect((int) src.left, (int) src.top, (int) src.right, (int) src.bottom);
            canvas.drawBitmap(bitmap, tmpSrc, dst, paint);
        }
    }
    
    /**
     * Return the underlying bitmap's density, as per
     * {@link Bitmap#getDensity() Bitmap.getDensity()}.
     */
    public int getDensity() {
        return mBitmap.mDensity;
    }
    
    public int getWidth() {
        return mBitmap.getWidth();
    }

    public int getHeight() {
        return mBitmap.getHeight();
    }

    public final boolean hasAlpha() {
        return mBitmap.hasAlpha();
    }

    public final Region getTransparentRegion(Rect location) {
//        int r = nativeGetTransparentRegion(mBitmap.ni(), mChunk, location);
//        return r != 0 ? new Region(r) : null;
        Log.e("NinePatch", "Not implemented yet!");
        return null;
    }
    
    public static boolean isNinePatchChunk(byte[] chunk) {
        if (chunk == null) {
            return false;
        }
        
        if (chunk.length < 32) {
            return false;
        }
        
        if (chunk[0] != 0) {
            return false;
        }
        
        return true;
    }

    private final Bitmap mBitmap;
    private final Res_png_9patch mChunk;
    private Paint        mPaint;
    private String       mSrcName;  // Useful for debugging

    private static void validateNinePatchChunk(Bitmap bitmap, Res_png_9patch chunk) {
//        if (chunk.length < 32) {
//            throw new RuntimeException("Array too small for chunk.");
//        }
        // XXX Also check that dimensions are correct.
        // In MayLoon, the chunk should have been deserialized already.
        if (chunk.wasDeserialized != 1) {
            throw new RuntimeException("NinePatchChunk is not deserialized!.");
        }
    }
    
    private static native int nativeGetTransparentRegion(
            int bitmap, byte[] chunk, Rect location);
}
