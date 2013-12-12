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

package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Shader;
//import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import java.io.IOException;

/**
 * A Drawable that wraps a bitmap and can be tiled, stretched, or aligned. You can create a
 * BitmapDrawable from a file path, an input stream, through XML inflation, or from
 * a {@link android.graphics.Bitmap} object.
 * <p>It can be defined in an XML file with the <code>&lt;bitmap></code> element.  For more
 * information, see the guide to <a
 * href="{@docRoot}guide/topics/resources/drawable-resource.html">Drawable Resources</a>.</p>
 * <p>
 * Also see the {@link android.graphics.Bitmap} class, which handles the management and 
 * transformation of raw bitmap graphics, and should be used when drawing to a 
 * {@link android.graphics.Canvas}.
 * </p>
 *
 * @attr ref android.R.styleable#BitmapDrawable_src
 * @attr ref android.R.styleable#BitmapDrawable_antialias
 * @attr ref android.R.styleable#BitmapDrawable_filter
 * @attr ref android.R.styleable#BitmapDrawable_dither
 * @attr ref android.R.styleable#BitmapDrawable_gravity
 * @attr ref android.R.styleable#BitmapDrawable_tileMode
 */
public class BitmapDrawable extends Drawable {
    BitmapState mBitmapState;
    private static final int DEFAULT_PAINT_FLAGS =
            Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;
    private Bitmap mBitmap;
    private int mTargetDensity;

    private final Rect mDstRect = new Rect();   // Gravity.apply() sets this

    private boolean mApplyGravity;
    private boolean mRebuildShader;
    private boolean mMutated;
    
     // These are scaled to match the target density.
    private int mBitmapWidth;
    private int mBitmapHeight;
    
    /**
     * Create an empty drawable, not dealing with density.
     * @deprecated Use {@link #BitmapDrawable(Resources)} to ensure
     * that the drawable has correctly set its target density.
     */
    @Deprecated
    public BitmapDrawable() {
        mBitmapState = new BitmapState((Bitmap) null);
    }

    /**
     * Create an empty drawable, setting initial target density based on
     * the display metrics of the resources.
     */
    public BitmapDrawable(Resources res) {
        mBitmapState = new BitmapState((Bitmap) null);
        mBitmapState.mTargetDensity = mTargetDensity;
    }

    /**
     * Create drawable from a bitmap, not dealing with density.
     * @deprecated Use {@link #BitmapDrawable(Resources, Bitmap)} to ensure
     * that the drawable has correctly set its target density.
     */
    @Deprecated
    public BitmapDrawable(Bitmap bitmap) {
        this(new BitmapState((Bitmap)bitmap), null);
    }

    /**
     * Create drawable from a bitmap, setting initial target density based on
     * the display metrics of the resources.
     */
    public BitmapDrawable(Resources res, Bitmap bitmap) {
        // Workaround: j2s can't handle method overload well.
        this();
        //this(new BitmapState(bitmap), res);
        this.mBitmapState.mBitmap = bitmap;
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            mTargetDensity = mBitmapState.mTargetDensity;
        }
        setBitmap(mBitmapState.mBitmap);
    }

    /**
     * Create a drawable by opening a given file path and decoding the bitmap.
     * @deprecated Use {@link #BitmapDrawable(Resources, String)} to ensure
     * that the drawable has correctly set its target density.
     */
    @Deprecated
    public BitmapDrawable(String filepath) {
        this(new BitmapState(BitmapFactory.decodeFile(filepath)), null);
        if (mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + filepath);
        }
    }

    /**
     * Create a drawable by opening a given file path and decoding the bitmap.
     */
    public BitmapDrawable(Resources res, String filepath) {
        this(new BitmapState(BitmapFactory.decodeFile(filepath)), null);
        mBitmapState.mTargetDensity = mTargetDensity;
        if (mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + filepath);
        }
    }

    /**
     * Create a drawable by decoding a bitmap from the given input stream.
     * @deprecated Use {@link #BitmapDrawable(Resources, java.io.InputStream)} to ensure
     * that the drawable has correctly set its target density.
     */
    @Deprecated
    public BitmapDrawable(java.io.InputStream is) {
        this(new BitmapState(BitmapFactory.decodeStream(is)), null);
        if (mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + is);
        }
    }

    /**
     * Create a drawable by decoding a bitmap from the given input stream.
     */
    public BitmapDrawable(Resources res, java.io.InputStream is) {
        this(new BitmapState(BitmapFactory.decodeStream(is)), null);
        mBitmapState.mTargetDensity = mTargetDensity;
        if (mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + is);
        }
    }

    public final Bitmap getBitmap() {
        return mBitmap;
    }

    private void computeBitmapSize() {
        mBitmapWidth = mBitmap.getScaledWidth(mTargetDensity);
        mBitmapHeight = mBitmap.getScaledHeight(mTargetDensity);
    }
    
    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        if (bitmap != null) {
            computeBitmapSize();
        } else {
            mBitmapWidth = mBitmapHeight = -1;
        }
    }


    /**
     * Set the density scale at which this drawable will be rendered.
     *
     * @param metrics The DisplayMetrics indicating the density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(DisplayMetrics metrics) {
        mTargetDensity = metrics.densityDpi;
        if (mBitmap != null) {
            computeBitmapSize();
        }
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param density The density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(int density) {
        mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
        if (mBitmap != null) {
            computeBitmapSize();
        }
    }

    /** Get the gravity used to position/stretch the bitmap within its bounds.
     * See android.view.Gravity
     * @return the gravity applied to the bitmap
     */
    public int getGravity() {
        return mBitmapState.mGravity;
    }
    
    /** Set the gravity used to position/stretch the bitmap within its bounds.
        See android.view.Gravity
     * @param gravity the gravity
     */
    public void setGravity(int gravity) {
        mBitmapState.mGravity = gravity;
        mApplyGravity = true;
    }
   
    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mBitmapState.mChangingConfigurations;
    }
    
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mApplyGravity = true;
    }

    @Override
    public void draw(Canvas canvas) {
        Bitmap bitmap = mBitmap;
        if (bitmap != null) {
            final BitmapState state = mBitmapState;
            if (mRebuildShader) {
                Shader.TileMode tmx = state.mTileModeX;
                Shader.TileMode tmy = state.mTileModeY;

                if (tmx == null && tmy == null) {
                    state.mPaint.setShader(null);
                } else {
                    Shader s = new BitmapShader(bitmap,
                            tmx == null ? Shader.TileMode.CLAMP : tmx,
                            tmy == null ? Shader.TileMode.CLAMP : tmy);
                    state.mPaint.setShader(s);
                }
                mRebuildShader = false;
                copyBounds(mDstRect);
            }

            Shader shader = state.mPaint.getShader();
            if (shader == null) {
                if (mApplyGravity) {
                    Gravity.apply(state.mGravity, mBitmapWidth, mBitmapHeight,
                            getBounds(), mDstRect);
                    mApplyGravity = false;
                }
                canvas.drawBitmap(bitmap, null, mDstRect, state.mPaint);
            } else {
                if (mApplyGravity) {
                    mDstRect.set(getBounds());
                    mApplyGravity = false;
                }
                canvas.drawRect(mDstRect, state.mPaint);
            }
        }
    }

    /**
     * A mutable BitmapDrawable still shares its Bitmap with any other Drawable
     * that comes from the same resource.
     *
     * @return This drawable.
     */
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mBitmapState = new BitmapState(mBitmapState);
            mMutated = true;
        }
        return this;
    }
    
    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs);

        TypedArray a = r.obtainAttributes(attrs, com.android.internal.R.styleable.BitmapDrawable);

        final int id = a.getResourceId(com.android.internal.R.styleable.BitmapDrawable_src, 0);
        if (id == 0) {
            throw new XmlPullParserException(parser.getPositionDescription() +
                    ": <bitmap> requires a valid src attribute");
        }
        final Bitmap bitmap = BitmapFactory.decodeResource(r, id);
        if (bitmap == null) {
            throw new XmlPullParserException(parser.getPositionDescription() +
                    ": <bitmap> requires a valid src attribute");
        }
        
        a.recycle();
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    @Override
    public final ConstantState getConstantState() {
        mBitmapState.mChangingConfigurations = super.getChangingConfigurations();
        return mBitmapState;
    }
    
    final static class BitmapState extends ConstantState {
        Bitmap mBitmap;
        int mChangingConfigurations;
        int mGravity = Gravity.FILL;
        Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);
        Shader.TileMode mTileModeX;
        Shader.TileMode mTileModeY;
        int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

        BitmapState(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        BitmapState(BitmapState bitmapState) {
            this(bitmapState.mBitmap);
            mChangingConfigurations = bitmapState.mChangingConfigurations;
            mGravity = bitmapState.mGravity;
            mTileModeX = bitmapState.mTileModeX;
            mTileModeY = bitmapState.mTileModeY;
            mTargetDensity = bitmapState.mTargetDensity;
            mPaint = new Paint(bitmapState.mPaint);
        }

        @Override
        public Drawable newDrawable() {
            return new BitmapDrawable(this, null);
        }
        
        @Override
        public Drawable newDrawable(Resources res) {
            return new BitmapDrawable(this, res);
        }
        
        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
    }
        
    private BitmapDrawable(BitmapState state, Resources res) {
        mBitmapState = state;
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            mTargetDensity = state.mTargetDensity;
        }
        setBitmap(state.mBitmap);
    }

    /**
     * @j2sNative
     * console.log("Missing method: setAntiAlias");
     */
    @MayloonStubAnnotation()
    public void setAntiAlias(boolean aa) {
        System.out.println("Stub" + " Function : setAntiAlias");
        return;
    }
}
