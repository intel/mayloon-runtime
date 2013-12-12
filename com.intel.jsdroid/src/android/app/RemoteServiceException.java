package android.app;

import android.util.AndroidRuntimeException;

final class RemoteServiceException extends AndroidRuntimeException {
    public RemoteServiceException(String msg) {
        super(msg);
    }
}