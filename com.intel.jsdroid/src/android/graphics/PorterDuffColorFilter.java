package android.graphics;

public class PorterDuffColorFilter extends ColorFilter {
    /**
     * Create a colorfilter that uses the specified color and porter-duff mode.
     *
     * @param srcColor       The source color used with the specified
     *                       porter-duff mode
     * @param mode           The porter-duff mode that is applied
     */
    public PorterDuffColorFilter(int srcColor, PorterDuff.Mode mode) {
        /*native_instance = native_CreatePorterDuffFilter(srcColor,
                                                        mode.nativeInt);*/
    }

    private static native int native_CreatePorterDuffFilter(int srcColor,
                                                            int porterDuffMode);
}
