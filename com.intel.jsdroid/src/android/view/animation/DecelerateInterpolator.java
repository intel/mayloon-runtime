package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

/**
 * An interpolator where the rate of change starts out quickly and 
 * and then decelerates.
 *
 */
public class DecelerateInterpolator implements Interpolator {
    public DecelerateInterpolator() {
    }

    /**
     * Constructor
     * 
     * @param factor Degree to which the animation should be eased. Setting factor to 1.0f produces
     *        an upside-down y=x^2 parabola. Increasing factor above 1.0f makes exaggerates the
     *        ease-out effect (i.e., it starts even faster and ends evens slower)
     */
    public DecelerateInterpolator(float factor) {
        mFactor = factor;
    }
    
    public DecelerateInterpolator(Context context, AttributeSet attrs) {
        TypedArray a =
            context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.DecelerateInterpolator);
        
        mFactor = a.getFloat(com.android.internal.R.styleable.DecelerateInterpolator_factor, 1.0f);
        
        a.recycle();
    }
    
    public float getInterpolation(float input) {
        if (mFactor == 1.0f) {
            return (float)(1.0f - (1.0f - input) * (1.0f - input));
        } else {
            return (float)(1.0f - Math.pow((1.0f - input), 2 * mFactor));
        }
    }
    
    private float mFactor = 1.0f;
}
