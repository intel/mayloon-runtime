package android.util;
/**
 * This exception is thrown when a call to {@link Context#startActivity} or
 * one of its variants fails because an Activity can not be found to execute
 * the given Intent.
 */
public class ActivityNotFoundException extends RuntimeException
{
    public ActivityNotFoundException()
    {
    }

    public ActivityNotFoundException(String name)
    {
        super(name);
    }
};
