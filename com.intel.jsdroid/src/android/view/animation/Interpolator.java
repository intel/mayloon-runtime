package android.view.animation;

/**
 * An interpolator defines the rate of change of an animation. This allows
 * the basic animation effects (alpha, scale, translate, rotate) to be 
 * accelerated, decelerated, repeated, etc.
 */
public interface Interpolator {
    
    /**
     * Maps a point on the timeline to a multiplier to be applied to the
     * transformations of an animation.
     * 
     * @param input A value between 0 and 1.0 indicating our current point
     *        in the animation where 0 represents the start and 1.0 represents
     *        the end
     * @return The interpolation value. This value can be more than 1.0 for
     *         Interpolators which overshoot their targets, or less than 0 for
     *         Interpolators that undershoot their targets.
     */
    float getInterpolation(float input);
    
}
