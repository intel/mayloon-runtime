/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

/**
 * @hide
 */
public class AnimatedRotateDrawable extends Drawable implements Drawable.Callback, Runnable{

    private AnimatedRotateState mState;
    private boolean mMutated;
    private float mCurrentDegrees;
    private float mIncrement;
    private boolean mRunning;

    public AnimatedRotateDrawable() {
        this(null, null);
    }

    private AnimatedRotateDrawable(AnimatedRotateState rotateState, Resources res) {
        mState = new AnimatedRotateState(rotateState, this, res);
        init();
    }

    private void init() {
        final AnimatedRotateState state = mState;
        mIncrement = 360.0f / (float) state.mFramesCount;
        final Drawable drawable = state.mDrawable;
        if (drawable != null) {
            drawable.setFilterBitmap(true);
            if (drawable instanceof BitmapDrawable) {
                //((BitmapDrawable) drawable).setAntiAlias(true);
            }
        }
    }

    public void draw(Canvas canvas) {
        int saveCount = canvas.save();

        final AnimatedRotateState st = mState;
        final Drawable drawable = st.mDrawable;
        final Rect bounds = drawable.getBounds();

        int w = bounds.right - bounds.left;
        int h = bounds.bottom - bounds.top;

        float px = st.mPivotXRel ? (w * st.mPivotX) : st.mPivotX;
        float py = st.mPivotYRel ? (h * st.mPivotY) : st.mPivotY;

        canvas.rotate(mCurrentDegrees, px, py);

        drawable.draw(canvas);

        canvas.restoreToCount(saveCount);
    }

    public void start() {
        if (!mRunning) {
            mRunning = true;
            nextFrame();
        }
    }

    public void stop() {
        mRunning = false;
        unscheduleSelf(this);
    }

    public boolean isRunning() {
        return mRunning;
    }

    private void nextFrame() {
        // unscheduleSelf(this);
        scheduleSelf(this, SystemClock.uptimeMillis() + mState.mFrameDuration);
    }
    
    public void run() {
        // TODO: This should be computed in draw(Canvas), based on the amount
        // of time since the last frame drawn 
        mCurrentDegrees += mIncrement;
        if (mCurrentDegrees > (360.0f - mIncrement)) {
            mCurrentDegrees = 0.0f;
        }
        invalidateSelf();
        nextFrame();
    }
    
    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        mState.mDrawable.setVisible(visible, restart);
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            if (changed || restart) {
                mCurrentDegrees = 0.0f;
                nextFrame();
            }
        } else {
            unscheduleSelf(this);
        }
        return changed;
    }    
    
    /**
     * Returns the drawable rotated by this RotateDrawable.
     */
    public Drawable getDrawable() {
        return mState.mDrawable;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations()
                | mState.mChangingConfigurations
                | mState.mDrawable.getChangingConfigurations();
    }
    
    public void setAlpha(int alpha) {
        mState.mDrawable.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter cf) {
        mState.mDrawable.setColorFilter(cf);
    }

    public int getOpacity() {
        return mState.mDrawable.getOpacity();
    }

    public void invalidateDrawable(Drawable who) {
        if (mCallback != null) {
            mCallback.invalidateDrawable(this);
        }
    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (mCallback != null) {
            mCallback.scheduleDrawable(this, what, when);
        }
    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (mCallback != null) {
            mCallback.unscheduleDrawable(this, what);
        }
    }

    @Override
    public boolean getPadding(Rect padding) {
        return mState.mDrawable.getPadding(padding);
    }
    
    @Override
    public boolean isStateful() {
        return mState.mDrawable.isStateful();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mState.mDrawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    @Override
    public int getIntrinsicWidth() {
        return mState.mDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mState.mDrawable.getIntrinsicHeight();
    }

    @Override
    public ConstantState getConstantState() {
        if (mState.canConstantState()) {
            mState.mChangingConfigurations = super.getChangingConfigurations();
            return mState;
        }
        return null;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {

        final TypedArray a = r.obtainAttributes(attrs, com.android.internal.R.styleable.AnimatedRotateDrawable);

        super.inflateWithAttributes(r, parser, a, com.android.internal.R.styleable.AnimatedRotateDrawable_visible);
        
        TypedValue tv = a.peekValue(com.android.internal.R.styleable.AnimatedRotateDrawable_pivotX);
        final boolean pivotXRel = tv.type == TypedValue.TYPE_FRACTION;
        final float pivotX = pivotXRel ? tv.getFraction(1.0f, 1.0f) : tv.getFloat();
        
        tv = a.peekValue(com.android.internal.R.styleable.AnimatedRotateDrawable_pivotY);
        final boolean pivotYRel = tv.type == TypedValue.TYPE_FRACTION;
        final float pivotY = pivotYRel ? tv.getFraction(1.0f, 1.0f) : tv.getFloat();
        
        final int framesCount = a.getInt(com.android.internal.R.styleable.AnimatedRotateDrawable_framesCount, 12);
        final int frameDuration = a.getInt(com.android.internal.R.styleable.AnimatedRotateDrawable_frameDuration, 150);
        
        int res = a.getResourceId(com.android.internal.R.styleable.AnimatedRotateDrawable_drawable, 0);
        Drawable drawable = null;
        if (res > 0) {
            drawable = r.getDrawable(res);
        }
        a.recycle();
        int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT &&
               (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
        	System.out.println("NONONONO");
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if ((drawable = Drawable.createFromXmlInner(r, parser, attrs)) == null) {
                Log.w("drawable", "Bad element under <animated-rotate>: "
                        + parser .getName());
            }
        }

        if (drawable == null) {
            Log.w("drawable", "No drawable specified for <animated-rotate>");
        }

        final AnimatedRotateState rotateState = mState;
        rotateState.mDrawable = drawable;
        rotateState.mPivotXRel = pivotXRel;
        rotateState.mPivotX = pivotX;
        rotateState.mPivotYRel = pivotYRel;
        rotateState.mPivotY = pivotY;
        rotateState.mFramesCount = framesCount;
        rotateState.mFrameDuration = frameDuration;

        init();

        if (drawable != null) {
            drawable.setCallback(this);
        }
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mState.mDrawable.mutate();
            mMutated = true;
        }
        return this;
    }

    final static class AnimatedRotateState extends Drawable.ConstantState {
        Drawable mDrawable;

        int mChangingConfigurations;
        
        boolean mPivotXRel;
        float mPivotX;
        boolean mPivotYRel;
        float mPivotY;
        int mFrameDuration;
        int mFramesCount;

        private boolean mCanConstantState;
        private boolean mCheckedConstantState;        

        public AnimatedRotateState(AnimatedRotateState source, AnimatedRotateDrawable owner,
                Resources res) {
        	System.out.println("AnimatedRotateState()");
            if (source != null) {
                if (res != null) {
                    mDrawable = source.mDrawable.getConstantState().newDrawable(res);
                } else {
                    mDrawable = source.mDrawable.getConstantState().newDrawable();
                }
                /**
                 * @j2sNative
                 * console.log(this.mDrawable);
                 */{}
                mDrawable.setCallback(owner);
                mPivotXRel = source.mPivotXRel;
                mPivotX = source.mPivotX;
                mPivotYRel = source.mPivotYRel;
                mPivotY = source.mPivotY;
                mFramesCount = source.mFramesCount;
                mFrameDuration = source.mFrameDuration;
                mCanConstantState = mCheckedConstantState = true;
            }
        }

        @Override
        public Drawable newDrawable() {
            return new AnimatedRotateDrawable(this, null);
        }
        
        @Override
        public Drawable newDrawable(Resources res) {
            return new AnimatedRotateDrawable(this, res);
        }
        
        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        public boolean canConstantState() {
            if (!mCheckedConstantState) {
                mCanConstantState = mDrawable.getConstantState() != null;
                mCheckedConstantState = true;
            }

            return mCanConstantState;
        }
    }
}