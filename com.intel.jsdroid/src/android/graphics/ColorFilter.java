package android.graphics;


public class ColorFilter {

    protected void finalize() throws Throwable {
        //finalizer(native_instance);
    }

    private static native void finalizer(int native_instance);

    int native_instance;
}
