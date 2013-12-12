/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.AssetManager.AssetInputStream;
import android.content.res.Resources;
import android.graphics.Bitmap.Config;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Creates Bitmap objects from various sources, including files, streams,
 * and byte-arrays.
 */
public class BitmapFactory {
    public static class Options {
        /**
         * Create a default Options object, which if left unchanged will give
         * the same result from the decoder as if null were passed.
         */
        public Options() {
            inDither = false;
            inScaled = true;
        }

        /**
         * If set to true, the decoder will return null (no bitmap), but
         * the out... fields will still be set, allowing the caller to query
         * the bitmap without having to allocate the memory for its pixels.
         */
        public boolean inJustDecodeBounds;

        /**
         * If set to a value > 1, requests the decoder to subsample the original
         * image, returning a smaller image to save memory. The sample size is
         * the number of pixels in either dimension that correspond to a single
         * pixel in the decoded bitmap. For example, inSampleSize == 4 returns
         * an image that is 1/4 the width/height of the original, and 1/16 the
         * number of pixels. Any value <= 1 is treated the same as 1. Note: the
         * decoder will try to fulfill this request, but the resulting bitmap
         * may have different dimensions that precisely what has been requested.
         * Also, powers of 2 are often faster/easier for the decoder to honor.
         */
        public int inSampleSize;

        /**
         * If this is non-null, the decoder will try to decode into this
         * internal configuration. If it is null, or the request cannot be met,
         * the decoder will try to pick the best matching config based on the
         * system's screen depth, and characteristics of the original image such
         * as if it has per-pixel alpha (requiring a config that also does).
         */
        public Bitmap.Config inPreferredConfig;

        /**
         * If dither is true, the decoder will attempt to dither the decoded
         * image.
         */
        public boolean inDither;

        /**
         * The pixel density to use for the bitmap.  This will always result
         * in the returned bitmap having a density set for it (see
         * {@link Bitmap#setDensity(int) Bitmap.setDensity(int)).  In addition,
         * if {@link #inScaled} is set (which it is by default} and this
         * density does not match {@link #inTargetDensity}, then the bitmap
         * will be scaled to the target density before being returned.
         * 
         * <p>If this is 0,
         * {@link BitmapFactory#decodeResource(Resources, int)}, 
         * {@link BitmapFactory#decodeResource(Resources, int, android.graphics.BitmapFactory.Options)},
         * and {@link BitmapFactory#decodeResourceStream}
         * will fill in the density associated with the resource.  The other
         * functions will leave it as-is and no density will be applied.
         *
         * @see #inTargetDensity
         * @see #inScreenDensity
         * @see #inScaled
         * @see Bitmap#setDensity(int)
         * @see android.util.DisplayMetrics#densityDpi
         */
        public int inDensity;

        /**
         * The pixel density of the destination this bitmap will be drawn to.
         * This is used in conjunction with {@link #inDensity} and
         * {@link #inScaled} to determine if and how to scale the bitmap before
         * returning it.
         * 
         * <p>If this is 0,
         * {@link BitmapFactory#decodeResource(Resources, int)}, 
         * {@link BitmapFactory#decodeResource(Resources, int, android.graphics.BitmapFactory.Options)},
         * and {@link BitmapFactory#decodeResourceStream}
         * will fill in the density associated the Resources object's
         * DisplayMetrics.  The other
         * functions will leave it as-is and no scaling for density will be
         * performed.
         * 
         * @see #inDensity
         * @see #inScreenDensity
         * @see #inScaled
         * @see android.util.DisplayMetrics#densityDpi
         */
        public int inTargetDensity;
        
        /**
         * The pixel density of the actual screen that is being used.  This is
         * purely for applications running in density compatibility code, where
         * {@link #inTargetDensity} is actually the density the application
         * sees rather than the real screen density.
         * 
         * <p>By setting this, you
         * allow the loading code to avoid scaling a bitmap that is currently
         * in the screen density up/down to the compatibility density.  Instead,
         * if {@link #inDensity} is the same as {@link #inScreenDensity}, the
         * bitmap will be left as-is.  Anything using the resulting bitmap
         * must also used {@link Bitmap#getScaledWidth(int)
         * Bitmap.getScaledWidth} and {@link Bitmap#getScaledHeight
         * Bitmap.getScaledHeight} to account for any different between the
         * bitmap's density and the target's density.
         * 
         * <p>This is never set automatically for the caller by
         * {@link BitmapFactory} itself.  It must be explicitly set, since the
         * caller must deal with the resulting bitmap in a density-aware way.
         * 
         * @see #inDensity
         * @see #inTargetDensity
         * @see #inScaled
         * @see android.util.DisplayMetrics#densityDpi
         */
        public int inScreenDensity;
        
        /**
         * When this flag is set, if {@link #inDensity} and
         * {@link #inTargetDensity} are not 0, the
         * bitmap will be scaled to match {@link #inTargetDensity} when loaded,
         * rather than relying on the graphics system scaling it each time it
         * is drawn to a Canvas.
         *
         * <p>This flag is turned on by default and should be turned off if you need
         * a non-scaled version of the bitmap.  Nine-patch bitmaps ignore this
         * flag and are always scaled.
         */
        public boolean inScaled;

        /**
         * If this is set to true, then the resulting bitmap will allocate its
         * pixels such that they can be purged if the system needs to reclaim
         * memory. In that instance, when the pixels need to be accessed again
         * (e.g. the bitmap is drawn, getPixels() is called), they will be
         * automatically re-decoded.
         *
         * For the re-decode to happen, the bitmap must have access to the
         * encoded data, either by sharing a reference to the input
         * or by making a copy of it. This distinction is controlled by
         * inInputShareable. If this is true, then the bitmap may keep a shallow
         * reference to the input. If this is false, then the bitmap will
         * explicitly make a copy of the input data, and keep that. Even if
         * sharing is allowed, the implementation may still decide to make a
         * deep copy of the input data.
         */
        public boolean inPurgeable;

        /**
         * This field works in conjuction with inPurgeable. If inPurgeable is
         * false, then this field is ignored. If inPurgeable is true, then this
         * field determines whether the bitmap can share a reference to the
         * input data (inputstream, array, etc.) or if it must make a deep copy.
         */
        public boolean inInputShareable;

        /**
         * Normally bitmap allocations count against the dalvik heap, which
         * means they help trigger GCs when a lot have been allocated. However,
         * in rare cases, the caller may want to allocate the bitmap outside of
         * that heap. To request that, set inNativeAlloc to true. In these
         * rare instances, it is solely up to the caller to ensure that OOM is
         * managed explicitly by calling bitmap.recycle() as soon as such a
         * bitmap is no longer needed.
         *
         * @hide pending API council approval
         */
        public boolean inNativeAlloc;

        /**
         * The resulting width of the bitmap, set independent of the state of
         * inJustDecodeBounds. However, if there is an error trying to decode,
         * outWidth will be set to -1.
         */
        public int outWidth;

        /**
         * The resulting height of the bitmap, set independent of the state of
         * inJustDecodeBounds. However, if there is an error trying to decode,
         * outHeight will be set to -1.
         */
        public int outHeight;

        /**
         * If known, this string is set to the mimetype of the decoded image.
         * If not know, or there is an error, it is set to null.
         */
        public String outMimeType;

        /**
         * Temp storage to use for decoding.  Suggest 16K or so.
         */
        public byte[] inTempStorage;

        private native void requestCancel();

        /**
         * Flag to indicate that cancel has been called on this object.  This
         * is useful if there's an intermediary that wants to first decode the
         * bounds and then decode the image.  In that case the intermediary
         * can check, inbetween the bounds decode and the image decode, to see
         * if the operation is canceled.
         */
        public boolean mCancel;

        /**
         *  This can be called from another thread while this options object is
         *  inside a decode... call. Calling this will notify the decoder that
         *  it should cancel its operation. This is not guaranteed to cancel
         *  the decode, but if it does, the decoder... operation will return
         *  null, or if inJustDecodeBounds is true, will set outWidth/outHeight
         *  to -1
         */
        public void requestCancelDecode() {
            mCancel = true;
            requestCancel();
        }
    }

    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName complete path name for the file to be decoded.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeFile(String pathName, Options opts) {
        Bitmap bm = null;
        InputStream stream = null;
        try {
            stream = new FileInputStream(pathName);
            bm = decodeStream(stream, null, opts, -1, pathName);
            bm.fileName = pathName;
        } catch (Exception e) {
            /*  do nothing.
                If the exception happened on open, bm will be null.
            */
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // do nothing here
                }
            }
        }
        return bm;
    }

    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName complete path name for the file to be decoded.
     * @return the resulting decoded bitmap, or null if it could not be decoded.
     */
    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }

    /**
     * Decode a new Bitmap from an InputStream. This InputStream was obtained from
     * resources, which we pass to be able to scale the bitmap accordingly.
     */
    public static Bitmap decodeResourceStream(Resources res, TypedValue value,
            InputStream is, Rect pad, Options opts, String srcName) {

        if (opts == null) {
            opts = new Options();
        }

        if (opts.inDensity == 0 && value != null) {
            final int density = value.density;
            if (density == TypedValue.DENSITY_DEFAULT) {
                opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;
            } else if (density != TypedValue.DENSITY_NONE) {
                opts.inDensity = density;
            }
        }
        
        if (opts.inTargetDensity == 0 && res != null) {
            opts.inTargetDensity = res.getDisplayMetrics().densityDpi;
        }
        
        return decodeStream(is, pad, opts, value.resourceId, srcName);
    }

    /**
     * Synonym for opening the given resource and calling
     * {@link #decodeResourceStream}.
     *
     * @param res   The resources object containing the image data
     * @param id The resource id of the image data
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeResource(Resources res, int id, Options opts) {
        Bitmap bm = null;
        InputStream is = null; 
        
        try {
            final TypedValue value = new TypedValue();
            is = res.openRawResource(id, value);
            AssetInputStream ais = (AssetInputStream)is;
            bm = decodeResourceStream(res, value, is, null, opts, ais.getAssetInt().getAssetSource());
            bm.resID = id;
        } catch (Exception e) {
            /*  do nothing.
                If the exception happened on open, bm will be null.
                If it happened on close, bm is still valid.
            */
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        return bm;
    }
    

    public static Bitmap decodeResourceFd(AssetFileDescriptor afd, int id) {
        Bitmap bm = null;
        
        try {
            String imagePath = afd.getRealPath();
            int width = -1;
            int height = -1;
            /**
             * @j2sNative
             * var img = document.createElement("img");
             * var loaded = false;
             * img.onload = function() {
             *                            loaded = true;
             *                            width = this.width;
             *                            height = this.height;
             *                            console.log("Image " + imagePath + " loaded.");
             * }
             * img.onerror = function() {
             *     loaded = true; // finish the loading.
             *     console.log("Image " + imagePath + " can't be loaded!");
             *     return bm;
             * }
             * 
             * try {
             *     img.src = imagePath;
             * } catch (e) {
             *     console.log("Image " + imagePath + " does not exist!");
             *     return bm;
             * }
             * console.log("Image " + imagePath + " begin to be loaded:" + new Date());
             * while (!loaded) {
             *     window.yield();
             *     console.log("yielding...");
             * }
             * console.log("Image " + imagePath + " loaded:" + new Date());   
             */{}

             bm = Bitmap.createBitmap(width, height, Config.ARGB_8888);
             bm.resID = id;
             /**
              * @j2sNative
              * if (bm.ensureCachedCanvas()) {
              *     var _context = bm.mCachedCanvas.getContext("2d");
              *     _context.drawImage(img, 0, 0);
              *     bm.mIsImageDataDirty = true;
              * } else {
              *     bm = null;
              * }
              */{}
        } catch (Exception e) {
            /*  do nothing.
                If the exception happened on open, bm will be null.
                If it happened on close, bm is still valid.
            */
        } 

        return bm;
    }

    /**
     * Synonym for {@link #decodeResource(Resources, int, android.graphics.BitmapFactory.Options)}
     * will null Options.
     *
     * @param res The resources object containing the image data
     * @param id The resource id of the image data
     * @return The decoded bitmap, or null if the image could not be decode.
     */
    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }

    /**
     * Decode an immutable bitmap from the specified byte array.
     *
     * @param data byte array of compressed image data
     * @param offset offset into imageData for where the decoder should begin
     *               parsing.
     * @param length the number of bytes, beginning at offset, to parse
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return BitmapDecoder.nativeDecodeByteArray(data, offset, length, opts);
    }

    /**
     * Decode an immutable bitmap from the specified byte array.
     *
     * @param data byte array of compressed image data
     * @param offset offset into imageData for where the decoder should begin
     *               parsing.
     * @param length the number of bytes, beginning at offset, to parse
     * @return The decoded bitmap, or null if the image could not be decode.
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }

    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts) {
        return decodeStream(is, outPadding, opts, -1, null);
    }
    /**
     * Decode an input stream into a bitmap. If the input stream is null, or
     * cannot be used to decode a bitmap, the function returns null.
     * The stream's position will be where ever it was after the encoded data
     * was read.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @param resourceId   Added by mayLoon, for debugging
     * @param fileName     Added by mayLoon, for debugging
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts, int resourceId, String fileName) {
        // we don't throw in this case, thus allowing the caller to only check
        // the cache, and not force the image to be decoded.
        if (is == null) {
            return null;
        }

        // we need mark/reset to work properly

        if (!is.markSupported()) {
            is = new BufferedInputStream(is, 16 * 1024);
        }

        // so we can call reset() if a given codec gives up after reading up to
        // this many bytes. FIXME: need to find out from the codecs what this
        // value should be.
        is.mark(1024);

        Bitmap  bm;

        //System.out.println("In decodestream");
        if (is instanceof AssetManager.AssetInputStream) {
            System.out.println("decoding asset");
            bm = BitmapDecoder.nativeDecodeAsset(((AssetManager.AssetInputStream) is).getAssetInt(),
                    outPadding, opts);
        } else {
            // pass some temp storage down to the native code. 1024 is made up,
            // but should be large enough to avoid too many small calls back
            // into is.read(...) This number is not related to the value passed
            // to mark(...) above.
            byte [] tempStorage = null;
            if (opts != null)
                tempStorage = opts.inTempStorage;
            if (tempStorage == null)
                tempStorage = new byte[16 * 1024];
            bm = BitmapDecoder.nativeDecodeStream(is, tempStorage, outPadding, opts, resourceId, fileName);
        }

        return finishDecode(bm, outPadding, opts);
    }

    private static Bitmap finishDecode(Bitmap bm, Rect outPadding, Options opts) {
        if (bm == null || opts == null) {
            return bm;
        }
        
        final int density = opts.inDensity;
        if (density == 0) {
            return bm;
        }
        
        bm.setDensity(density);
        final int targetDensity = opts.inTargetDensity;
        if (targetDensity == 0 || density == targetDensity
                || density == opts.inScreenDensity) {
            return bm;
        }
  
        Res_png_9patch np = bm.getNinePatch();
        final boolean isNinePatch = np != null;
        if (opts.inScaled || isNinePatch) {
            float scale = targetDensity / (float)density;
            // TODO: This is very inefficient and should be done in native by Skia
            final Bitmap oldBitmap = bm;
            bm = Bitmap.createScaledBitmap(oldBitmap, (int) (bm.getWidth() * scale + 0.5f),
                    (int) (bm.getHeight() * scale + 0.5f), true);
            oldBitmap.recycle();

            if (isNinePatch) {
                np = nativeScaleNinePatch(np, scale, outPadding);
                bm.setNinePatch(np);
            }
            bm.setDensity(targetDensity);
        }
        
        return bm;
    }
    
    private static Res_png_9patch nativeScaleNinePatch(Res_png_9patch np, float scale, Rect outPadding) {
        if (np != null) {
            np.paddingLeft = (int) (np.paddingLeft * scale);
            np.paddingTop = (int) (np.paddingTop * scale);
            np.paddingRight = (int) (np.paddingRight * scale);
            np.paddingBottom = (int) (np.paddingBottom * scale);
            
            for (int i = 0; i < np.numXDivs; i++) {
                np.xDivs[i] = (int) (np.xDivs[i] * scale);
                if (i > 0 && np.xDivs[i] == np.xDivs[i - 1]) {
                    np.xDivs[i]++;
                }
            }
            
            for (int i = 0; i < np.numYDivs; i++) {
                np.yDivs[i] = (int) (np.yDivs[i] * scale);
                if (i > 0 && np.yDivs[i] == np.yDivs[i - 1]) {
                    np.yDivs[i]++;
                }
            }
            
            if (outPadding != null) {
                outPadding.left = np.paddingLeft;
                outPadding.top = np.paddingTop;
                outPadding.right = np.paddingRight;
                outPadding.bottom = np.paddingBottom;
            }
        }
        return np;
    }
    /**
     * Decode an input stream into a bitmap. If the input stream is null, or
     * cannot be used to decode a bitmap, the function returns null.
     * The stream's position will be where ever it was after the encoded data
     * was read.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null, -1, null);
    }

    /**
     * Decode a bitmap from the file descriptor. If the bitmap cannot be decoded
     * return null. The position within the descriptor will not be changed when
     * this returns, so the descriptor can be used again as-is.
     *
     * @param fd The file descriptor containing the bitmap data to decode
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return the decoded bitmap, or null
     */
    public static Bitmap decodeFileDescriptor(FileDescriptor fd, Rect outPadding, Options opts) {
    	//MayLoon does not support MemoryFile
/*
    	try {
            if (MemoryFile.isMemoryFile(fd)) {
                int mappedlength = MemoryFile.getSize(fd);
                MemoryFile file = new MemoryFile(fd, mappedlength, "r");
                InputStream is = file.getInputStream();
                Bitmap bm = decodeStream(is, outPadding, opts);
                return finishDecode(bm, outPadding, opts);
            }
        } catch (IOException ex) {
            // invalid filedescriptor, no need to call nativeDecodeFileDescriptor()
            return null;
        }
*/        
        Bitmap bm = BitmapDecoder.nativeDecodeFileDescriptor(fd, outPadding, opts);
        return finishDecode(bm, outPadding, opts);
    }

    /**
     * Decode a bitmap from the file descriptor. If the bitmap cannot be decoded
     * return null. The position within the descriptor will not be changed when
     * this returns, so the descriptor can be used again as is.
     *
     * @param fd The file descriptor containing the bitmap data to decode
     * @return the decoded bitmap, or null
     */
    public static Bitmap decodeFileDescriptor(FileDescriptor fd) {
        return decodeFileDescriptor(fd, null, null);
    }

    /**
     * Set the default config used for decoding bitmaps. This config is
     * presented to the codec if the caller did not specify a preferred config
     * in their call to decode...
     *
     * The default value is chosen by the system to best match the device's
     * screen and memory constraints.
     *
     * @param config The preferred config for decoding bitmaps. If null, then
     *               a suitable default is chosen by the system.
     *
     * @hide - only called by the browser at the moment, but should be stable
     *   enough to expose if needed
     */
    public static void setDefaultConfig(Bitmap.Config config) {
        if (config == null) {
            // pick this for now, as historically it was our default.
            // However, if we have a smarter algorithm, we can change this.
            BitmapDecoder.nativeSetDefaultConfig(Config.RGB_565);
        }
    }
    
    /** ********************************************************************
     *  PNG Extensions
     *
     *  New private chunks that may be placed in PNG images.
     *
     *********************************************************************** */

    /**
     * This chunk specifies how to split an image into segments for
     * scaling.
     *
     * There are J horizontal and K vertical segments.  These segments divide
     * the image into J*K regions as follows (where J=4 and K=3):
     *
     *      F0   S0    F1     S1
     *   +-----+----+------+-------+
     * S2|  0  |  1 |  2   |   3   |
     *   +-----+----+------+-------+
     *   |     |    |      |       |
     *   |     |    |      |       |
     * F2|  4  |  5 |  6   |   7   |
     *   |     |    |      |       |
     *   |     |    |      |       |
     *   +-----+----+------+-------+
     * S3|  8  |  9 |  10  |   11  |
     *   +-----+----+------+-------+
     *
     * Each horizontal and vertical segment is considered to by either
     * stretchable (marked by the Sx labels) or fixed (marked by the Fy
     * labels), in the horizontal or vertical axis, respectively. In the
     * above example, the first is horizontal segment (F0) is fixed, the
     * next is stretchable and then they continue to alternate. Note that
     * the segment list for each axis can begin or end with a stretchable
     * or fixed segment.
     *
     * The relative sizes of the stretchy segments indicates the relative
     * amount of stretchiness of the regions bordered by the segments.  For
     * example, regions 3, 7 and 11 above will take up more horizontal space
     * than regions 1, 5 and 9 since the horizontal segment associated with
     * the first set of regions is larger than the other set of regions.  The
     * ratios of the amount of horizontal (or vertical) space taken by any
     * two stretchable slices is exactly the ratio of their corresponding
     * segment lengths.
     *
     * xDivs and yDivs point to arrays of horizontal and vertical pixel
     * indices.  The first pair of Divs (in either array) indicate the
     * starting and ending points of the first stretchable segment in that
     * axis. The next pair specifies the next stretchable segment, etc. So
     * in the above example xDiv[0] and xDiv[1] specify the horizontal
     * coordinates for the regions labeled 1, 5 and 9.  xDiv[2] and
     * xDiv[3] specify the coordinates for regions 3, 7 and 11. Note that
     * the leftmost slices always start at x=0 and the rightmost slices
     * always end at the end of the image. So, for example, the regions 0,
     * 4 and 8 (which are fixed along the X axis) start at x value 0 and
     * go to xDiv[0] and slices 2, 6 and 10 start at xDiv[1] and end at
     * xDiv[2].
     *
     * The array pointed to by the colors field lists contains hints for
     * each of the regions.  They are ordered according left-to-right and
     * top-to-bottom as indicated above. For each segment that is a solid
     * color the array entry will contain that color value; otherwise it
     * will contain NO_COLOR.  Segments that are completely transparent
     * will always have the value TRANSPARENT_COLOR.
     *
     * The PNG chunk type is "npTc".
     */
    public static class Res_png_9patch {
        Res_png_9patch() {
            chunk = null;
            wasDeserialized = 0;
            xDivs = null;
            yDivs = null;
            colors = null;
        }
        
        byte[] chunk; // to save the original data.

        byte wasDeserialized;
        byte numXDivs;
        byte numYDivs;
        byte numColors;

        // These tell where the next section of a patch starts.
        // For example, the first patch includes the pixels from
        // 0 to xDivs[0]-1 and the second patch includes the pixels
        // from xDivs[0] to xDivs[1]-1.
        // Note: allocation/free of these pointers is left to the caller.
        int[] xDivs;
        int[] yDivs;

        int paddingLeft, paddingRight;
        int paddingTop, paddingBottom;

        // The 9 patch segment is not a solid color.
        public static final int NO_COLOR = 0x00000001;
        // The 9 patch segment is completely transparent.
        public static final int TRANSPARENT_COLOR = 0x00000000;
        // Note: allocation/free of this pointer is left to the caller.
        int[] colors;

        // Convert data from device representation to PNG file representation.
        void deviceToFile() {
        }

        // Convert data from PNG file representation to device representation.
        void fileToDevice(byte[] inData) {
        }

        // Serialize/Marshall the patch data into a newly malloc-ed block
        byte[] serialize() {
            throw new RuntimeException("Not supported!");
        }

        // Serialize/Marshall the patch data
        void serialize(byte[] outData) {
            throw new RuntimeException("Not supported!");
        }

        // Deserialize/Unmarshall the patch data
        static Res_png_9patch  deserialize(byte[] data) {
            Res_png_9patch np = new Res_png_9patch();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.BIG_ENDIAN).position(0); // PNG use big endian.
            
            // deserialize wasDeserialized, numXDivs, numYDivs, numColors
            np.wasDeserialized = buffer.get(0);
            np.numXDivs = buffer.get(1);
            np.numYDivs = buffer.get(2);
            np.numColors = buffer.get(3);
            np.wasDeserialized = 1;
            // deserialize paddingXXXX
            np.paddingLeft = buffer.getInt(12);
            np.paddingRight = buffer.getInt(16);
            np.paddingTop = buffer.getInt(20);
            np.paddingBottom = buffer.getInt(24);
            
            // deserialize xDivs, yDivs, colors
            int offset = 32; 
            np.xDivs = new int[np.numXDivs];
            for (int i = 0; i < np.numXDivs; i++) {
                np.xDivs[i] = buffer.getInt(offset + i * 4);
            }
            offset += np.numXDivs * 4;
            np.yDivs = new int[np.numYDivs];
            for (int i = 0; i < np.numYDivs; i++) {
                np.yDivs[i] = buffer.getInt(offset + i * 4);
            }
            offset += np.numYDivs * 4;
            np.colors = new int[np.numColors];
            for (int i = 0; i < np.numColors; i++) {
                np.colors[i] = buffer.getInt(offset + i * 4);
            }
            
            np.chunk = data;
            return np;
        }

        // Compute the size of the serialized data structure
        int serializedSize() {
            return 32 + numXDivs * 4 + numYDivs * 4 + numColors * 4;
        }
    }

}

