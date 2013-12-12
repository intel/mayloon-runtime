package android.content;

import android.os.Handler;


public class IIntentReceiver {

	public BroadcastReceiver receiver;
	public Context mOuterContext;
	public Handler mHander;
	public boolean registered;	
	
	public IIntentReceiver(){
		receiver=null;
		mOuterContext=null;
		mHander=null;
		registered=true;	
	}
	
	public IIntentReceiver(BroadcastReceiver _receiver,Context _c,Handler _h,boolean _registered){
		receiver=_receiver;
		mOuterContext=_c;
		mHander=_h;
		registered=_registered;
	}
}
