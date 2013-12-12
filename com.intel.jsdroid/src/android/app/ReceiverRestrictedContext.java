package android.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.ServiceConnection;
import android.os.Handler;

class ReceiverRestrictedContext extends ContextWrapper {
    ReceiverRestrictedContext(Context base) {
        super(base);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return registerReceiver(receiver, filter, null, null);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
            String broadcastPermission, Handler scheduler) {
        throw new ReceiverCallNotAllowedException(
                "IntentReceiver components are not allowed to register to receive intents");
        //ex.fillInStackTrace();
        //Log.e("IntentReceiver", ex.getMessage(), ex);
        //return mContext.registerReceiver(receiver, filter, broadcastPermission,
        //        scheduler);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        throw new ReceiverCallNotAllowedException(
                "IntentReceiver components are not allowed to bind to services");
        //ex.fillInStackTrace();
        //Log.e("IntentReceiver", ex.getMessage(), ex);
        //return mContext.bindService(service, interfaceName, conn, flags);
    }
}