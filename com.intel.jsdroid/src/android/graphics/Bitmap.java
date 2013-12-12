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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.graphics.BitmapFactory.Res_png_9patch;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;


/**
 * this class holds an array of image data for a bitmap, which can be used
 * by HTML5 canvas to draw
 * @author qzhang8
 *
 */
public final class Bitmap  {
    /**
     * Indicates that the bitmap was created for an unknown pixel density.
     *
     * @see Bitmap#getDensity()
     * @see Bitmap#setDensity(int)
     */
    public static final int DENSITY_NONE = 0;

    private boolean mIsMutable;
    private Res_png_9patch mNinePatch;   // may be null
    private int mWidth = -1;
    private int mHeight = -1;
    private boolean mRecycled;

    public int[] mImageData;
    public byte[] mRawData;
    /**
     * @j2sNative
     * this.mCachedCanvas = null;    // save data into this canvas
     * this.mCachedImageData = null; // all setPixel/getPixel will get data from it as read from cached canvas
     *                               // may be too slow every time. 
     */{}
    private boolean mIsImageDataDirty = false;
    private boolean mNeedUpdateIntoCachedCanvas = false;
    private boolean mIsCachedCanvasDirty = false;
    public String mDataURL;
    private Config mConfig;

    //debug information
    public String fileName;
    public int resID;
    public static int idCnt = 0;
    public int id;

    public Bitmap() {
        id = idCnt++;
        resID = -1;
    }
    // Package-scoped for fast access.
    int mDensity = sDefaultDensity = getDefaultDensity();



    private static volatile int sDefaultDensity = -1;

    /**
     * For backwards compatibility, allows the app layer to change the default
     * density when running old apps.
     * @hide
     */
    public static void setDefaultDensity(int density) {
        sDefaultDensity = density;
    }

    /*package*/ static int getDefaultDensity() {
        if (sDefaultDensity >= 0) {
            return sDefaultDensity;
        }
        sDefaultDensity = DisplayMetrics.DENSITY_DEVICE;
        return sDefaultDensity;
    }

    /**
     * <p>Returns the density for this bitmap.</p>
     *
     * <p>The default density is the same density as the current display,
     * unless the current application does not support different screen
     * densities in which case it is
     * {@link android.util.DisplayMetrics#DENSITY_DEFAULT}.  Note that
     * compatibility mode is determined by the application that was initially
     * loaded into a process -- applications that share the same process should
     * all have the same compatibility, or ensure they explicitly set the
     * density of their bitmaps appropriately.</p>
     *
     * @return A scaling factor of the default density or {@link #DENSITY_NONE}
     *         if the scaling factor is unknown.
     *
     * @see #setDensity(int)
     * @see android.util.DisplayMetrics#DENSITY_DEFAULT
     * @see android.util.DisplayMetrics#densityDpi
     * @see #DENSITY_NONE
     */
    public int getDensity() {
        return mDensity;
    }

    /**
     * <p>Specifies the density for this bitmap.  When the bitmap is
     * drawn to a Canvas that also has a density, it will be scaled
     * appropriately.</p>
     *
     * @param density The density scaling factor to use with this bitmap or
     *        {@link #DENSITY_NONE} if the density is unknown.
     *
     * @see #getDensity()
     * @see android.util.DisplayMetrics#DENSITY_DEFAULT
     * @see android.util.DisplayMetrics#densityDpi
     * @see #DENSITY_NONE
     */
    public void setDensity(int density) {
        mDensity = density;
    }

    /**
     * Sets the nine patch chunk.
     *
     * @param chunk The definition of the nine patch
     *
     * @hide
     */
    public void setNinePatch(Res_png_9patch chunk) {
        mNinePatch = chunk;
    }

    /**
     * Free up the memory associated with this bitmap's pixels, and mark the
     * bitmap as "dead", meaning it will throw an exception if getPixels() or
     * setPixels() is called, and will draw nothing. This operation cannot be
     * reversed, so it should only be called if you are sure there are no
     * further uses for the bitmap. This is an advanced call, and normally need
     * not be called, since the normal GC process will free up this memory when
     * there are no more references to this bitmap.
     */
    public void recycle() {
        if (!mRecycled) {
            mNinePatch = null;
            mRecycled = true;
        }
    }

    /**
     * Returns true if this bitmap has been recycled. If so, then it is an error
     * to try to access its pixels, and the bitmap will not draw.
     *
     * @return true if the bitmap has been recycled
     */
    public final boolean isRecycled() {
        return mRecycled;
    }

    /**
     * This is called by methods that want to throw an exception if the bitmap
     * has already been recycled.
     */
    private void checkRecycled(String errorMessage) {
        if (mRecycled) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Common code for checking that x and y are >= 0
     *
     * @param x x coordinate to ensure is >= 0
     * @param y y coordinate to ensure is >= 0
     */
    private static void checkXYSign(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
    }

    /**
     * Common code for checking that width and height are > 0
     *
     * @param width  width to ensure is > 0
     * @param height height to ensure is > 0
     */
    private static void checkWidthHeight(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
    }

    public static class Config {
        // these native values must match up with the enum in SkBitmap.h
        public static final Config ALPHA_8 = new Config(2);
        public static final Config RGB_565 = new Config(4);
        public static final Config ARGB_4444 = new Config(5);
        public static final Config ARGB_8888 = new Config(6);
        public static final int CONFIG_ALPHA_8 = 2;
        public static final int CONFIG_RGB_565 = 4;
        public static final int CONFIG_ARGB_4444 = 5;
        public static final int CONFIG_ARGB_8888 = 6;

        public Config(int ni) {
            this.nativeInt = ni;
        }
        public final int nativeInt;

        /* package */ static Config nativeToConfig(int ni) {
            return sConfigs[ni];
        }

        private static Config sConfigs[] = {
            null, null, ALPHA_8, null, RGB_565, ARGB_4444, ARGB_8888
        };

        public static Config valueOf(String value) {
            if ("ALPHA_8".equals(value)) {
                return Config.ALPHA_8;
            } else if ("RGB_565".equals(value)) {
                return Config.RGB_565;
            } else if ("ARGB_4444".equals(value)) {
                return Config.ARGB_4444;
            } else if ("ARGB_8888".equals(value)) {
                return Config.ARGB_8888;
            } else {
                return null;
            }
        }

        public static Config[] values() {
            Config[] config = new Config[4];
            config[0] = Config.ALPHA_8;
            config[1] = Config.RGB_565;
            config[2] = Config.ARGB_4444;
            config[3] = Config.ARGB_8888;
            return config;
        }
    }

    /**
     * Copy the bitmap's pixels into the specified buffer (allocated by the
     * caller). An exception is thrown if the buffer is not large enough to
     * hold all of the pixels (taking into account the number of bytes per
     * pixel) or if the Buffer subclass is not one of the support types
     * (ByteBuffer, ShortBuffer, IntBuffer).
     */
    public void copyPixelsToBuffer(Buffer dst) {
        int elements = dst.remaining();
        int shift;
        if (dst instanceof ByteBuffer) {
            shift = 0;
        } else if (dst instanceof ShortBuffer) {
            shift = 1;
        } else if (dst instanceof IntBuffer) {
            shift = 2;
        } else {
            throw new RuntimeException("unsupported Buffer subclass");
        }

        long bufferSize = (long)elements << shift;
        long pixelSize = (long)getRowBytes() * getHeight();

        if (bufferSize < pixelSize) {
            throw new RuntimeException("Buffer not large enough for pixels");
        }

//        nativeCopyPixelsToBuffer(mNativeBitmap, dst);

        // now update the buffer's position
        int position = dst.position();
        position += pixelSize >> shift;
        dst.position(position);
    }

    /**
     * Copy the pixels from the buffer, beginning at the current position,
     * overwriting the bitmap's pixels. The data in the buffer is not changed
     * in any way (unlike setPixels(), which converts from unpremultipled 32bit
     * to whatever the bitmap's native format is.
     */
    public void copyPixelsFromBuffer(Buffer src) {
        checkRecycled("copyPixelsFromBuffer called on recycled bitmap");

        int elements = src.remaining();
        int shift;
        if (src instanceof ByteBuffer) {
            shift = 0;
        } else if (src instanceof ShortBuffer) {
            shift = 1;
        } else if (src instanceof IntBuffer) {
            shift = 2;
        } else {
            throw new RuntimeException("unsupported Buffer subclass");
        }

        long bufferBytes = (long)elements << shift;
        long bitmapBytes = (long)getRowBytes() * getHeight();

        if (bufferBytes < bitmapBytes) {
            throw new RuntimeException("Buffer not large enough for pixels");
        }

        nativeCopyPixelsFromBuffer(src, shift);
    }

    private void nativeCopyPixelsFromBuffer(Buffer src, int shift) {
        int[] pixels = (int[]) src.array();
        if(null == pixels) return;
        
        checkRecycled("Can't call setPixels() on a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        if (mWidth == 0 || mHeight == 0) {
            return; // nothing to do
        }
        checkPixelsAccess(0, 0, mWidth, mHeight, 0, mWidth, pixels);
        if (!ensureCachedCanvas(false, false)) return;
        mIsImageDataDirty = true;
        setPixelsFromBuffer(pixels, shift);
    }

    private void setPixelsFromBuffer(int[] pixels, int shift) {
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; (j < mWidth)
                    && (i * mHeight + j < pixels.length); j++) {
                checkPixelAccess(j, i);
                if (!ensureCachedCanvas(false, false))  return;

                 /**
                  * @j2sNative
                  *  var data = this.mCachedImageData.data;
                  *  var index = i * 4 * this.mWidth + j * 4;
                  *  var color = pixels[i * this.mWidth + j];
                  *  
                  *  //shift stands for data buffer type,
                  *  //as assigned in method "copyPixelsFromBuffer"
                  *  //similar to the effect of mConfig
                  *  switch(shift) {
                  *      case 0:    //byte, not implemented
                  *         break;
                  *      case 1:    //short
                  *         if(this.mConfig.nativeInt == 
                  *             android.graphics.Bitmap.Config.CONFIG_RGB_565) {
                  *             data[index] = ((color >> 11) & 0x1F) / 0x1F * 0xFF;
                  *             data[index+1] = ((color >> 5) & 0x3F) / 0x3F * 0xFF;
                  *             data[index+2] = (color & 0x1F) / 0x1F * 0xFF;
                  *             data[index+3] = 0xFF;
                  *         }
                  *         else if(this.mConfig.nativeInt == 
                  *             android.graphics.Bitmap.Config.CONFIG_ARGB_4444) {
                  *             data[index] = ((color >> 12) & 0xF) / 16 * 256;
                  *             data[index+1] = ((color >> 8) & 0xF) / 16 * 256;
                  *             data[index+2] = ((color >> 4) & 0xF) / 16 * 256;
                  *             data[index+3] = (color & 0xF) / 16 * 256;
                  *         }
                  *         break;
                  *      case 2:    //int, the config is ARGB_8888
                  *         data[index] = android.graphics.Color.red(color);;
                  *         data[index+1] = android.graphics.Color.green(color);
                  *         data[index+2] = android.graphics.Color.blue(color);
                  *         data[index+3] = android.graphics.Color.alpha(color);;
                  *         break;
                  *  }
                  */{}
            }
        }
    }

    /**
     * Tries to make a new bitmap based on the dimensions of this bitmap,
     * setting the new bitmap's config to the one specified, and then copying
     * this bitmap's pixels into the new bitmap. If the conversion is not
     * supported, or the allocator fails, then this returns NULL.  The returned
     * bitmap initially has the same density as the original.
     *
     * @param config    The desired config for the resulting bitmap
     * @param isMutable True if the resulting bitmap should be mutable (i.e.
     *                  its pixels can be modified)
     * @return the new bitmap, or null if the copy could not be made.
     */
    public Bitmap copy(Config config, boolean isMutable) {
        checkRecycled("Can't copy a recycled bitmap");
        Bitmap bitmap = createBitmap(this.getWidth(), this.getHeight(), config);
        if (!ensureCachedCanvas(false, true))
            return null;
        if (!bitmap.ensureCachedCanvas(true, false))
            return null;
        /**
         * @j2sNative 
         * var activeCanvas = bitmap.mCachedCanvas;
         * var activeContext = activeCanvas.getContext("2d");
         * activeContext.drawImage(this.mCachedCanvas, 0, 0);
         */{}
        bitmap.mDensity = mDensity;
        return bitmap;
    }

    /**
     * Call this function every time before use the cached canvas.
     * 
     * @param changeCachedCanvas: If you are going to change the cached canvas,
     *            such as draw something into a bitmap based canvas. set this
     *            flag to true to indicate the in data in cached canvas is dirty.
     * @param needUpdateIntoCachedCanvas: If you are going to use the cached canvas,
     *            such as draw the bitmap into some other canvas or bitmap. set this
     *            flag to true, we will copy the data from cachedImagedata into cachedCanvas.       
     * @return If fail to create cached canvas, return false
     */
    public Boolean ensureCachedCanvas(Boolean changeCachedCanvas, Boolean needUpdateIntoCachedCanvas) {
         /**
          * @j2sNative
          * if (this.mCachedCanvas == null) {
          *     var canvas = document.createElement('canvas');
          *     canvas.width = this.getWidth();
          *     canvas.height = this.getHeight();
          *     this.mCachedCanvas = canvas;
          *     if (this.mCachedCanvas == null) {
          *         console.error("Fail to create cached canvas for this bitmap");
          *         return false;
          *     }
          *
          *     var context = this.mCachedCanvas.getContext('2d');
          *     var imageData = context.createImageData(this.mCachedCanvas.width, this.mCachedCanvas.height);
          *     this.mCachedImageData = imageData;
          *     if (this.mRawData) {
          *        var png = new PNG(this.mRawData, false);
          *        // Decode the data from png into imageData
          *        var pixelBytes = png.pixelBitlength / 8;
          *        if (pixelBytes >= 3 && !png.palette.length && png.colors != 1) { // go to fast path for RGBA mode
          *            png.decodePixels(null, imageData.data); // directly decode the pixels into imageData, avoid copy
          *        } else {
          *            png.copyToImageData(imageData, png.decodePixels());
          *        }
          *        this.mIsImageDataDirty = true;
          *        needUpdateIntoCachedCanvas = true;
          *        // this.mDataURL = this.mCachedCanvas.toDataURL(); // not needed, will remove it
          *        this.mCachedCanvas = canvas;
          *     } else {
          *         //context.clearRect(0, 0, this.getWidth(), this.getHeight());
          *     }
          * }
          * 
          * // update the ImageData into CachedCanvas if we need to update and data is dirty. 
          * if (needUpdateIntoCachedCanvas == true && this.mIsImageDataDirty == true) {
          *     this.mCachedCanvas.getContext("2d").putImageData(this.mCachedImageData,0,0);
          *     this.mIsImageDataDirty = false;
          * }
          * 
          * if (changeCachedCanvas == true) {
          *     // If the cached canvas is changed. Next time we read the pixels from mCachedImageData,
          *     // we need to get the latest data from cached canvas.
          *     this.mIsCachedCanvasDirty = true;
          * }
          */{}
         return true;
    }
    
    public void checkCachedCanvasDirty() {
        // If the cached canvas has been modified, we need to update the data into cached image data
        // as getPixel will read pixels from the cached image data.
        if (this.mIsCachedCanvasDirty) {
            /**
             * @j2sNative
             * this.mCachedImageData = this.mCachedCanvas.getContext("2d").getImageData(0, 0, this.getWidth(), this.getHeight());
             * this.mIsCachedCanvasDirty = false;
             */{}
        }
    }
    
    /**
     * Returns an immutable bitmap from the source bitmap. The new bitmap may
     * be the same object as source, or a copy may have been made.  It is
     * initialized with the same density as the original bitmap.
     */
    public static Bitmap createBitmap(Bitmap src) {
        return createBitmap(src, 0, 0, src.getWidth(), src.getHeight());
    }

    /**
     * Returns an immutable bitmap from the specified subset of the source
     * bitmap. The new bitmap may be the same object as source, or a copy may
     * have been made.  It is
     * initialized with the same density as the original bitmap.
     *
     * @param source   The bitmap we are subsetting
     * @param x        The x coordinate of the first pixel in source
     * @param y        The y coordinate of the first pixel in source
     * @param width    The number of pixels in each row
     * @param height   The number of rows
     */
    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        return createBitmap(source, x, y, width, height, null, false);
    }


    /**
     * Returns a mutable bitmap with the specified width and height.  Its
     * initial density is as per {@link #getDensity}.
     *
     * @param width    The width of the bitmap
     * @param height   The height of the bitmap
     * @param config   The bitmap config to create.
     * @throws IllegalArgumentException if the width or height are <= 0
     */
    public static Bitmap createBitmap(int width, int height, Config config) {
        Bitmap bm = nativeCreateBitmap(null, 0, width, width, height, config, true);
        return bm;
    }

    /**
     * Returns an immutable bitmap from subset of the source bitmap,
     * transformed by the optional matrix.  It is
     * initialized with the same density as the original bitmap.
     *
     * @param source   The bitmap we are subsetting
     * @param x        The x coordinate of the first pixel in source
     * @param y        The y coordinate of the first pixel in source
     * @param width    The number of pixels in each row
     * @param height   The number of rows
     * @param m        Optional matrix to be applied to the pixels
     * @param filter   true if the source should be filtered.
     *                   Only applies if the matrix contains more than just
     *                   translation.
     * @return A bitmap that represents the specified subset of source
     * @throws IllegalArgumentException if the x, y, width, height values are
     *         outside of the dimensions of the source bitmap.
     */
    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height,
            Matrix m, boolean filter) {
        checkXYSign(x, y);
        checkWidthHeight(width, height);
        if (x + width > source.getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > source.getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }

        // check if we can just return our argument unchanged
        if (!source.isMutable() && x == 0 && y == 0 && width == source.getWidth() &&
                height == source.getHeight() && (m == null || m.isIdentity())) {
            return source;
        }

        int neww = width;
        int newh = height;
        Canvas canvas = new Canvas();
        Bitmap bitmap;
        Paint paint;

        Rect srcR = new Rect(x, y, x + width, y + height);
        RectF dstR = new RectF(0, 0, width, height);

        if (m == null || m.isIdentity()) {
            bitmap = createBitmap(neww, newh,
                    source.hasAlpha() ? Config.ARGB_8888 : Config.RGB_565);
            canvas.setBitmap(bitmap);
            paint = null;   // not needed
        } else {
            /*  the dst should have alpha if the src does, or if our matrix
                doesn't preserve rectness
            */
            boolean hasAlpha = source.hasAlpha() || !m.rectStaysRect();
            RectF deviceR = new RectF();
            m.mapRect(deviceR, dstR);
            neww = Math.round(deviceR.width());
            newh = Math.round(deviceR.height());
            bitmap = createBitmap(neww, newh, hasAlpha ? Config.ARGB_8888 : Config.RGB_565);
//            if (hasAlpha) {
//                bitmap.eraseColor(0);
//            }
            canvas.setBitmap(bitmap);
            canvas.translate(-deviceR.left, -deviceR.top);
            canvas.concat(m);
            paint = new Paint();
            paint.setFilterBitmap(filter);
            if (!m.rectStaysRect()) {
                paint.setAntiAlias(true);
            }
        }

        // The new bitmap was created from a known bitmap source so assume that
        // they use the same density
        bitmap.mDensity = source.mDensity;

        canvas.drawBitmap(source, srcR, dstR, paint);

        return bitmap;
    }

    /**
     * Creates a new bitmap, scaled from an existing bitmap.
     *
     * @param src       The source bitmap.
     * @param dstWidth  The new bitmap's desired width.
     * @param dstHeight The new bitmap's desired height.
     * @param filter    true if the source should be filtered.
     * @return the new scaled bitmap.
     */
    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth,
            int dstHeight, boolean filter) {
        if (!src.ensureCachedCanvas(false, true))
            return null;
        Bitmap bitmap = createBitmap(dstWidth, dstHeight, Config.ARGB_8888);
        bitmap.mDensity = src.mDensity;
        if (!bitmap.ensureCachedCanvas(false, false))
            return null;
        /**
         * @j2sNative 
         * var activeCanvas = bitmap.mCachedCanvas; 
         * var activeContext = activeCanvas.getContext("2d");
         * activeContext.drawImage(src.mCachedCanvas, 0, 0, bitmap.getWidth(), bitmap.getHeight());
         * bitmap.mIsCachedCanvasDirty = true;
         */{}
        return bitmap;
    }
    /**
     * Returns a immutable bitmap with the specified width and height, with each
     * pixel value set to the corresponding value in the colors array.  Its
     * initial density is as per {@link #getDensity}.
     *
     * @param colors   Array of {@link Color} used to initialize the pixels.
     * @param offset   Number of values to skip before the first color in the
     *                 array of colors.
     * @param stride   Number of colors in the array between rows (must be >=
     *                 width or <= -width).
     * @param width    The width of the bitmap
     * @param height   The height of the bitmap
     * @param config   The bitmap config to create. If the config does not
     *                 support per-pixel alpha (e.g. RGB_565), then the alpha
     *                 bytes in the colors[] will be ignored (assumed to be FF)
     * @throws IllegalArgumentException if the width or height are <= 0, or if
     *         the color array's length is less than the number of pixels.
     */
    public static Bitmap createBitmap(int colors[], int offset, int stride,
            int width, int height, Config config) {

        checkWidthHeight(width, height);
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int lastScanline = offset + (height - 1) * stride;
        int length = colors.length;
        if (offset < 0 || (offset + width > length) || lastScanline < 0 ||
                (lastScanline + width > length)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return nativeCreateBitmap(colors, offset, stride, width, height, config, false);
    }

    /**
     * Returns a immutable bitmap with the specified width and height, with each
     * pixel value set to the corresponding value in the colors array.  Its
     * initial density is as per {@link #getDensity}.
     *
     * @param colors   Array of {@link Color} used to initialize the pixels.
     *                 This array must be at least as large as width * height.
     * @param width    The width of the bitmap
     * @param height   The height of the bitmap
     * @param config   The bitmap config to create. If the config does not
     *                 support per-pixel alpha (e.g. RGB_565), then the alpha
     *                 bytes in the colors[] will be ignored (assumed to be FF)
     * @throws IllegalArgumentException if the width or height are <= 0, or if
     *         the color array's length is less than the number of pixels.
     */
    public static Bitmap createBitmap(int colors[], int width, int height, Config config) {
        return createBitmap(colors, 0, width, width, height, config);
    }

    public static Bitmap createBitmap(String dataURL) {
    	Bitmap bm = new Bitmap();
    	bm.mDataURL = dataURL;

    	return bm;
    }
    /**
     * Returns an optional array of private data, used by the UI system for
     * some bitmaps. Not intended to be called by applications.
     */
    public Res_png_9patch getNinePatch() {
        return mNinePatch;
    }
    
    /**
     * Returns an optional array of private data, used by the UI system for
     * some bitmaps. Not intended to be called by applications.
     */
    public byte[] getNinePatchChunk() {
        if (mNinePatch != null) {
            return mNinePatch.chunk;
        } else {
            return null;
        }
    }

    /**
     * Specifies the known formats a bitmap can be compressed into
     */
    public static class CompressFormat {
        public static final CompressFormat JPEG = new CompressFormat(0);
        public static final CompressFormat PNG = new CompressFormat(1);
        CompressFormat(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;

        public static CompressFormat valueOf(String value) {
            if ("JPEG".equals(value)) {
                return CompressFormat.JPEG;
            } else if ("PNG".equals(value)) {
                return CompressFormat.PNG;
            } else {
                return null;
            }
        }

        public static CompressFormat[] values() {
            CompressFormat[] compressformat = new CompressFormat[2];
            compressformat[0] = CompressFormat.JPEG;
            compressformat[1] = CompressFormat.PNG;
            return compressformat;
        }
    }

    /**
     * Number of bytes of temp storage we use for communicating between the
     * native compressor and the java OutputStream.
     */
    private final static int WORKING_COMPRESS_STORAGE = 4096;

    /**
     * Write a compressed version of the bitmap to the specified outputstream.
     * If this returns true, the bitmap can be reconstructed by passing a
     * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
     * all Formats support all bitmap configs directly, so it is possible that
     * the returned bitmap from BitmapFactory could be in a different bitdepth,
     * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
     * pixels).
     *
     * @param format   The format of the compressed image
     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
     *                 small size, 100 meaning compress for max quality. Some
     *                 formats, like PNG which is lossless, will ignore the
     *                 quality setting
     * @param stream   The outputstream to write the compressed data.
     * @return true if successfully compressed to the specified stream.
     */
    public boolean compress(CompressFormat formater, int quality, OutputStream stream) {
        checkRecycled("Can't compress a recycled bitmap");
        // do explicit check before calling the native method
        if (stream == null) {
            throw new NullPointerException();
        }
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("quality must be 0..100");
        }

//        return nativeCompress(mNativeBitmap, format.nativeInt, quality,
//                              stream, new byte[WORKING_COMPRESS_STORAGE]);
        if (!ensureCachedCanvas(false, true))
            return false;
        String data = "";
        int format = formater.nativeInt;
       /**
        * @j2sNative
        * switch(format){
        * //As we only support PNG decoding, 
        * //we will encode the data into PNG format
        * //no matter what the requested format is
        * case 0: //jpeg 
        * data=this.mCachedCanvas.toDataURL("image/png");
        * data=data.substring(22); // "data:image/png;base64,"
        * break;
        * case 1: //png
        * data=this.mCachedCanvas.toDataURL("image/png");
        * data=data.substring(22); // "data:image/png;base64,"
        * break;
        * }
        */{}
       if("".equals(data)) return false;
       byte[] imageByte=Base64.decode(data, Base64.DEFAULT);
       try {
			stream.write(imageByte);
			return true;
		} catch (IOException e) {
			Log.e("Bitmap", "compress error");
		}
       return false;
    }

    /**
     * Returns true if the bitmap is marked as mutable (i.e. can be drawn into)
     */
    public final boolean isMutable() {
        return mIsMutable;
    }

    /** Returns the bitmap's width */
    public final int getWidth() {
    	return mWidth;
    }

    /** Returns the bitmap's height */
    public final int getHeight() {
        return mHeight;
    }

    /** Returns the bitmap's width */
    public final void setWidth(int width) {
    	mWidth = width;
    }

    /** Returns the bitmap's height */
    public final void setHeight(int height) {
        mHeight = height;
    }


    /**
     * Convenience method that returns the width of this bitmap divided
     * by the density scale factor.
     *
     * @param targetDensity The density of the target canvas of the bitmap.
     * @return The scaled width of this bitmap, according to the density scale factor.
     */
    public int getScaledWidth(int targetDensity) {
        return scaleFromDensity(getWidth(), mDensity, targetDensity);
    }

    /**
     * Convenience method that returns the height of this bitmap divided
     * by the density scale factor.
     *
     * @param targetDensity The density of the target canvas of the bitmap.
     * @return The scaled height of this bitmap, according to the density scale factor.
     */
    public int getScaledHeight(int targetDensity) {
        return scaleFromDensity(getHeight(), mDensity, targetDensity);
    }

    /**
     * @hide
     */
    static public int scaleFromDensity(int size, int sdensity, int tdensity) {
        if (sdensity == DENSITY_NONE || sdensity == tdensity) {
            return size;
        }

        // Scale by tdensity / sdensity, rounding up.
        return ((size * tdensity) + (sdensity >> 1)) / sdensity;
    }

    /**
     * Return the number of bytes between rows in the bitmap's pixels. Note that
     * this refers to the pixels as stored natively by the bitmap. If you call
     * getPixels() or setPixels(), then the pixels are uniformly treated as
     * 32bit values, packed according to the Color class.
     *
     * @return number of bytes between rows of the native bitmap pixels.
     */
    public final int getRowBytes() {
    	int pixelSize = 0;
    	switch (mConfig.nativeInt) {
        case Config.CONFIG_RGB_565 :
        case Config.CONFIG_ARGB_4444:
    		pixelSize = 2;
    		break;
        case Config.CONFIG_ARGB_8888:
    		pixelSize = 4;
    		break;
        case Config.CONFIG_ALPHA_8:
    		pixelSize = 1;
    		break;
    	}
    	return pixelSize*mWidth;
    }

    /**
     * If the bitmap's internal config is in one of the public formats, return
     * that config, otherwise return null.
     */
    public final Config getConfig() {
    	return mConfig;
    }

    /** Returns true if the bitmap's config supports per-pixel alpha, and
     * if the pixels may contain non-opaque alpha values. For some configs,
     * this is always false (e.g. RGB_565), since they do not support per-pixel
     * alpha. However, for configs that do, the bitmap may be flagged to be
     * known that all of its pixels are opaque. In this case hasAlpha() will
     * also return false. If a config such as ARGB_8888 is not so flagged,
     * it will return true by default.
     */
    public final boolean hasAlpha() {
    	switch (mConfig.nativeInt) {
        case Config.CONFIG_RGB_565 :
    		return false;
        case Config.CONFIG_ARGB_4444:
        case Config.CONFIG_ARGB_8888:
        case Config.CONFIG_ALPHA_8:
    		return true;
    	}
        return false;
    }

    /**
     * Tell the bitmap if all of the pixels are known to be opaque (false)
     * or if some of the pixels may contain non-opaque alpha values (true).
     * Note, for some configs (e.g. RGB_565) this call is ignore, since it does
     * not support per-pixel alpha values.
     *
     * This is meant as a drawing hint, as in some cases a bitmap that is known
     * to be opaque can take a faster drawing case than one that may have
     * non-opaque per-pixel alpha values.
     *
     * @hide
     */
    public void setHasAlpha(boolean hasAlpha) {

    }

    /**
     * Fills the bitmap's pixels with the specified {@link Color}.
     *
     * @throws IllegalStateException if the bitmap is not mutable.
     */
    public void eraseColor(int c) {
        checkRecycled("Can't erase a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException("cannot erase immutable bitmaps");
        }
        if (!ensureCachedCanvas(false, false))
            return;
        nativeErase(c);
    }

    /**
     * Returns the {@link Color} at the specified location. Throws an exception
     * if x or y are out of bounds (negative or >= to the width or height
     * respectively).
     *
     * @param x    The x coordinate (0...width-1) of the pixel to return
     * @param y    The y coordinate (0...height-1) of the pixel to return
     * @return     The argb {@link Color} at the specified coordinate
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     */
    public int getPixel(int x, int y) {
        checkRecycled("Can't call getPixel() on a recycled bitmap");
        checkPixelAccess(x, y);
        if (!ensureCachedCanvas(false, false)) {
            return 0;
        }
        /**
         * @j2sNative 
         * this.checkCachedCanvasDirty();
         * var data = this.mCachedImageData.data; 
         * var index = y * 4 * this.mWidth + x * 4; 
         * return android.graphics.Color.argb(data[index+3], data[index], data[index+1], data[index+2]);
         */{}
        return 0;
    }

    /**
     * Returns in pixels[] a copy of the data in the bitmap. Each value is
     * a packed int representing a {@link Color}. The stride parameter allows
     * the caller to allow for gaps in the returned pixels array between
     * rows. For normal packed results, just pass width for the stride value.
     *
     * @param pixels   The array to receive the bitmap's colors
     * @param offset   The first index to write into pixels[]
     * @param stride   The number of entries in pixels[] to skip between
     *                 rows (must be >= bitmap's width). Can be negative.
     * @param x        The x coordinate of the first pixel to read from
     *                 the bitmap
     * @param y        The y coordinate of the first pixel to read from
     *                 the bitmap
     * @param width    The number of pixels to read from each row
     * @param height   The number of rows to read
     * @throws IllegalArgumentException if x, y, width, height exceed the
     *         bounds of the bitmap, or if abs(stride) < width.
     * @throws ArrayIndexOutOfBoundsException if the pixels array is too small
     *         to receive the specified number of pixels.
     */
    public void getPixels(int[] pixels, int offset, int stride,
                          int x, int y, int width, int height) {
        checkRecycled("Can't call getPixels() on a recycled bitmap");
        if (width == 0 || height == 0) {
            return; // nothing to do
        }

        checkPixelsAccess(x, y, width, height, offset, stride, pixels);
        if (!ensureCachedCanvas(false, false))
            return;
        nativeGetPixels(pixels, offset, stride, x, y, width, height);
    }

    /**
     * Shared code to check for illegal arguments passed to getPixel()
     * or setPixel()
     * @param x x coordinate of the pixel
     * @param y y coordinate of the pixel
     */
    private void checkPixelAccess(int x, int y) {
        checkXYSign(x, y);
        if (x >= getWidth()) {
            throw new IllegalArgumentException("x must be < bitmap.width()");
        }
        if (y >= getHeight()) {
            throw new IllegalArgumentException("y must be < bitmap.height()");
        }
    }

    /**
     * Shared code to check for illegal arguments passed to getPixels()
     * or setPixels()
     *
     * @param x left edge of the area of pixels to access
     * @param y top edge of the area of pixels to access
     * @param width width of the area of pixels to access
     * @param height height of the area of pixels to access
     * @param offset offset into pixels[] array
     * @param stride number of elements in pixels[] between each logical row
     * @param pixels array to hold the area of pixels being accessed
    */
    private void checkPixelsAccess(int x, int y, int width, int height,
                                   int offset, int stride, int pixels[]) {
        checkXYSign(x, y);
        if (width < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
        if (x + width > getWidth()) {
            throw new IllegalArgumentException(
                    "x + width must be <= bitmap.width()");
        }
        if (y + height > getHeight()) {
            throw new IllegalArgumentException(
                    "y + height must be <= bitmap.height()");
        }
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int lastScanline = offset + (height - 1) * stride;
        int length = pixels.length;
        if (offset < 0 || (offset + width > length)
                || lastScanline < 0
                || (lastScanline + width > length)) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Write the specified {@link Color} into the bitmap (assuming it is
     * mutable) at the x,y coordinate.
     *
     * @param x     The x coordinate of the pixel to replace (0...width-1)
     * @param y     The y coordinate of the pixel to replace (0...height-1)
     * @param color The {@link Color} to write into the bitmap
     * @throws IllegalStateException if the bitmap is not mutable
     * @throws IllegalArgumentException if x, y are outside of the bitmap's
     *         bounds.
     */
    public void setPixel(int x, int y, int color) {
        checkRecycled("Can't call setPixel() on a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        checkPixelAccess(x, y);
        if (!ensureCachedCanvas(false, false))
            return;

         /**
          * @j2sNative
          *  var data = this.mCachedImageData.data;
          *  var index = y * 4 * this.mWidth + x * 4;
          *  data[index] = android.graphics.Color.red(color);;
          *  data[index+1] = android.graphics.Color.green(color);
          *  data[index+2] = android.graphics.Color.blue(color);
          *  data[index+3] = android.graphics.Color.alpha(color);;
          */{}
         mIsImageDataDirty = true;

    }

    /**
     * Replace pixels in the bitmap with the colors in the array. Each element
     * in the array is a packed int prepresenting a {@link Color}
     *
     * @param pixels   The colors to write to the bitmap
     * @param offset   The index of the first color to read from pixels[]
     * @param stride   The number of colors in pixels[] to skip between rows.
     *                 Normally this value will be the same as the width of
     *                 the bitmap, but it can be larger (or negative).
     * @param x        The x coordinate of the first pixel to write to in
     *                 the bitmap.
     * @param y        The y coordinate of the first pixel to write to in
     *                 the bitmap.
     * @param width    The number of colors to copy from pixels[] per row
     * @param height   The number of rows to write to the bitmap
     * @throws IllegalStateException if the bitmap is not mutable
     * @throws IllegalArgumentException if x, y, width, height are outside of
     *         the bitmap's bounds.
     * @throws ArrayIndexOutOfBoundsException if the pixels array is too small
     *         to receive the specified number of pixels.
     */
    public void setPixels(int[] pixels, int offset, int stride,
                          int x, int y, int width, int height) {
        checkRecycled("Can't call setPixels() on a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        if (width == 0 || height == 0) {
            return; // nothing to do
        }
        checkPixelsAccess(x, y, width, height, offset, stride, pixels);
        if (!ensureCachedCanvas(false, false))
            return;
        nativeSetPixels(pixels, offset, stride, x, y, width, height);
        mIsImageDataDirty = true;
    }

    /**
     * Returns a new bitmap that captures the alpha values of the original.
     * This may be drawn with Canvas.drawBitmap(), where the color(s) will be
     * taken from the paint that is passed to the draw call.
     *
     * @return new bitmap containing the alpha channel of the original bitmap.
     */
    public Bitmap extractAlpha() {
        return extractAlpha(null, null);
    }

    /**
     * Returns a new bitmap that captures the alpha values of the original.
     * These values may be affected by the optional Paint parameter, which
     * can contain its own alpha, and may also contain a MaskFilter which
     * could change the actual dimensions of the resulting bitmap (e.g.
     * a blur maskfilter might enlarge the resulting bitmap). If offsetXY
     * is not null, it returns the amount to offset the returned bitmap so
     * that it will logically align with the original. For example, if the
     * paint contains a blur of radius 2, then offsetXY[] would contains
     * -2, -2, so that drawing the alpha bitmap offset by (-2, -2) and then
     * drawing the original would result in the blur visually aligning with
     * the original.
     *
     * <p>The initial density of the returned bitmap is the same as the original's.
     *
     * @param paint Optional paint used to modify the alpha values in the
     *              resulting bitmap. Pass null for default behavior.
     * @param offsetXY Optional array that returns the X (index 0) and Y
     *                 (index 1) offset needed to position the returned bitmap
     *                 so that it visually lines up with the original.
     * @return new bitmap containing the (optionally modified by paint) alpha
     *         channel of the original bitmap. This may be drawn with
     *         Canvas.drawBitmap(), where the color(s) will be taken from the
     *         paint that is passed to the draw call.
     */
    public Bitmap extractAlpha(Paint paint, int[] offsetXY) {
        checkRecycled("Can't extractAlpha on a recycled bitmap");
        return null;
    }

    /**
     *  Given another bitmap, return true if it has the same dimensions, config,
     *  and pixel data as this bitmap. If any of those differ, return false.
     *  If other is null, return false.
     *
     * @hide (just needed internally right now)
     */
    public boolean sameAs(Bitmap other) {
        return false;
    }

    /**
     * Rebuilds any caches associated with the bitmap that are used for
     * drawing it. In the case of purgeable bitmaps, this call will attempt to
     * ensure that the pixels have been decoded.
     * If this is called on more than one bitmap in sequence, the priority is
     * given in LRU order (i.e. the last bitmap called will be given highest
     * priority).
     *
     * For bitmaps with no associated caches, this call is effectively a no-op,
     * and therefore is harmless.
     */
    public void prepareToDraw() {

    }

    @Override
    protected void finalize() throws Throwable {
        try {
        } finally {
            super.finalize();
        }
    }

    public static Bitmap nativeCreateBitmap(int[] colors, int offset,
            int stride, int width, int height, Config config, boolean mutable) {
        Bitmap bitmap = new Bitmap();
        bitmap.mConfig = config;
        //make sure the bitmap is mutable in order to setPixels
        bitmap.mIsMutable = true;
        bitmap.setWidth(width);
        bitmap.setHeight(height);
        
        if(null != colors) {
            bitmap.setPixels(colors, offset, stride, 0, 0, width, height);
        }
        
        //resume the status of mutable
        bitmap.mIsMutable = mutable;
        return bitmap;
    }

    public void nativeErase(int color) {
        String fillColor = Color.toString(color);
        /**
         * @j2sNative
         * if (!this.ensureCachedCanvas(false, false)) return;
         * var context = this.mCachedCanvas.getContext("2d");
         * context.save();
         * context.fillStyle = fillColor;
         * context.fillRect(0, 0, this.mCachedCanvas.width, this.mCachedCanvas.height);
         * context.restore();
         */{}
         this.mIsCachedCanvasDirty = true;
    }

    public void nativeSetPixels(int[] colors, int offset, int stride, int x,
            int y, int width, int height) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; (j < width)
                    && (i * stride + j + offset < colors.length); j++) {
                setPixel(x + j, y + i, colors[i * stride + j + offset]);
            }
        }
    }

    public void nativeGetPixels(int[] pixels, int offset, int stride, int x,
            int y, int width, int height) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; (j < width)
                    && (i * stride + j + offset < pixels.length); j++) {
                pixels[i * stride + j + offset] = getPixel(x + j, y + i);
            }
        }

    }

    /**
     * @j2sNative
     * console.log("Missing method: getScaledHeight");
     */
    @MayloonStubAnnotation()
    public int getScaledHeight(DisplayMetrics metrics) {
        System.out.println("Stub" + " Function : getScaledHeight");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: describeContents");
     */
    @MayloonStubAnnotation()
    public int describeContents() {
        System.out.println("Stub" + " Function : describeContents");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getScaledWidth");
     */
    @MayloonStubAnnotation()
    public int getScaledWidth(Canvas canvas) {
        System.out.println("Stub" + " Function : getScaledWidth");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getScaledHeight");
     */
    @MayloonStubAnnotation()
    public int getScaledHeight(Canvas canvas) {
        System.out.println("Stub" + " Function : getScaledHeight");
        return 0;
    }
}
