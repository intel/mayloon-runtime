package android.content;

import android.content.res.Configuration;

/**
 * The set of callback APIs that are common to all application components
 * ({@link android.app.Activity}, {@link android.app.Service},
 * {@link ContentProvider}, and {@link android.app.Application}).
 */
public interface ComponentCallbacks {
    /**
     * Called by the system when the device configuration changes while your
     * component is running.  Note that, unlike activities, other components
     * are never restarted when a configuration changes: they must always deal
     * with the results of the change, such as by re-retrieving resources.
     * 
     * <p>At the time that this function has been called, your Resources
     * object will have been updated to return resource values matching the
     * new configuration.
     * 
     * @param newConfig The new device configuration.
     */
    void onConfigurationChanged(Configuration newConfig);
    
    /**
     * This is called when the overall system is running low on memory, and
     * would like actively running process to try to tighten their belt.  While
     * the exact point at which this will be called is not defined, generally
     * it will happen around the time all background process have been killed,
     * that is before reaching the point of killing processes hosting
     * service and foreground UI that we would like to avoid killing.
     * 
     * <p>Applications that want to be nice can implement this method to release
     * any caches or other unnecessary resources they may be holding on to.
     * The system will perform a gc for you after returning from this method.
     */
    void onLowMemory();
}
