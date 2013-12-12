package android.content;

import java.util.ArrayList;

import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;



/**
 * I make the content service be a singleton
 * @author wenhaoli
 *
 */
public class ContentService {
    private static final String TAG = " ContentService<< ";
    private static boolean DEBUG = true;
    private Context mContext;
    private static ContentService instance;
    private final ObserverNode mRootNode = new ObserverNode("");
    /*package*/ ContentService(Context context) {
        mContext = context;
    }
    public static ContentService main(Context context) {
    	instance = new ContentService(context);
        return instance;
    }
    public static ContentService getContentService(){
    	return instance;
    }
    public void registerContentObserver(Uri uri, boolean notifyForDescendents,
            ContentObserver observer) {
        if (observer == null || uri == null) {
            throw new IllegalArgumentException("You must pass a valid uri and observer");
        }
        if(DEBUG){
        	System.out.println(TAG+"Register Content Observer...");
        }
        mRootNode.addObserverLocked(uri, observer, notifyForDescendents, mRootNode);
    }
    public static final class ObserverCall {
        final ObserverNode mNode;
        final ContentObserver mObserver;
        final boolean mSelfNotify;

        ObserverCall(ObserverNode node, ContentObserver observer,
                boolean selfNotify) {
            mNode = node;
            mObserver = observer;
            mSelfNotify = selfNotify;
        }
    }
    public void notifyChange(Uri uri, ContentObserver observer,
            boolean observerWantsSelfNotifications, boolean syncToNetwork) {
    	if(DEBUG) System.out.println(TAG+"Notify change...");

        ArrayList<ObserverCall> calls = new ArrayList<ObserverCall>();
        
        mRootNode.collectObserversLocked(uri, 0, observer, observerWantsSelfNotifications,
                calls);
        final int numCalls = calls.size();
        for (int i=0; i<numCalls; i++) {
            ObserverCall oc = calls.get(i);
            try {
                oc.mObserver.onChange(oc.mSelfNotify);
                if (DEBUG) {
                    Log.v(TAG, "Notified " + oc.mObserver + " of " + "update at " + uri);
                }
            } catch (Exception ex) {
                //Remove dead observers
                final ArrayList<ObserverNode.ObserverEntry> list
                        = oc.mNode.mObservers;
                int numList = list.size();
                for (int j=0; j<numList; j++) {
                    ObserverNode.ObserverEntry oe = list.get(j);
                    if (oe.observer == oc.mObserver||oc.mObserver==null) {
                        list.remove(j);
                        j--;
                        numList--;
                    }
                }
            }
        }    
    }
    public static final class ObserverNode {
    	private String mName;
        private ArrayList<ObserverNode> mChildren = new ArrayList<ObserverNode>();
        private ArrayList<ObserverEntry> mObservers = new ArrayList<ObserverEntry>();
        private class ObserverEntry{
            public final boolean notifyForDescendents;
            public final ContentObserver observer;
            ObserverEntry(ContentObserver o, boolean n, Object observersLock){
            	notifyForDescendents = n;
            	observer = o;
            }
        }
        public ObserverNode(String name) {
            mName = name;
        }
        private int countUriSegments(Uri uri) {
            if (uri == null) {
                return 0;
            }
            return uri.getPathSegments().size() + 1;
        }
        private String getUriSegment(Uri uri, int index) {
            if (uri != null) {
                if (index == 0) {
                    return uri.getAuthority();
                } else {
                    return uri.getPathSegments().get(index - 1);
                }
            } else {
                return null;
            }
        }
        public void addObserverLocked(Uri uri, ContentObserver observer,
                boolean notifyForDescendents, Object observersLock) {
            addObserverLocked(uri, 0, observer, notifyForDescendents, observersLock);
        }
        public boolean removeObserverLocked(ContentObserver observer) {
            int size = mChildren.size();
            for (int i = 0; i < size; i++) {
                boolean empty = mChildren.get(i).removeObserverLocked(observer);
                if (empty) {
                    mChildren.remove(i);
                    i--;
                    size--;
                }
            }

            size = mObservers.size();
            for (int i = 0; i < size; i++) {
                ObserverEntry entry = mObservers.get(i);
                if (entry.observer == observer) {
                    mObservers.remove(i);
                    break;
                }
            }

            if (mChildren.size() == 0 && mObservers.size() == 0) {
                return true;
            }
            return false;
        }

        private void addObserverLocked(Uri uri, int index, ContentObserver observer,
                boolean notifyForDescendents, Object observersLock) {
            // If this is the leaf node add the observer
            if (index == countUriSegments(uri)) {
                mObservers.add(new ObserverEntry(observer, notifyForDescendents, observersLock));
                return;
            }

            // Look to see if the proper child already exists
            String segment = getUriSegment(uri, index);
            if (segment == null) {
                throw new IllegalArgumentException("Invalid Uri (" + uri + ") used for observer");
            }
            int N = mChildren.size();
            for (int i = 0; i < N; i++) {
                ObserverNode node = mChildren.get(i);
                if (node.mName.equals(segment)) {
                    node.addObserverLocked(uri, index + 1, observer, notifyForDescendents, observersLock);
                    return;
                }
            }

            // No child found, create one
            ObserverNode node = new ObserverNode(segment);
            mChildren.add(node);
            node.addObserverLocked(uri, index + 1, observer, notifyForDescendents, observersLock);
        }
        private void collectMyObserversLocked(boolean leaf, ContentObserver observer,
                boolean selfNotify, ArrayList<ObserverCall> calls) {
            int N = mObservers.size();
            for (int i = 0; i < N; i++) {
                ObserverEntry entry = mObservers.get(i);

                // Don't notify the observer if it sent the notification and isn't interesed
                // in self notifications
                if (entry.observer== observer && !selfNotify) {
                    continue;
                }

                // Make sure the observer is interested in the notification
                if (leaf || (!leaf && entry.notifyForDescendents)) {
                    calls.add(new ObserverCall(this, entry.observer, selfNotify));
                }
            }
        }
        public void collectObserversLocked(Uri uri, int index, ContentObserver observer,
                boolean selfNotify, ArrayList<ObserverCall> calls){

            String segment = null;
            int segmentCount = countUriSegments(uri);
            if (index >= segmentCount) {
                // This is the leaf node, notify all observers
                collectMyObserversLocked(true, observer, selfNotify, calls);
            } else if (index < segmentCount){
                segment = getUriSegment(uri, index);
                // Notify any observers at this level who are interested in descendents
                collectMyObserversLocked(false, observer, selfNotify, calls);
            }

            int N = mChildren.size();
            for (int i = 0; i < N; i++) {
                ObserverNode node = mChildren.get(i);
                if (segment == null || node.mName.equals(segment)) {
                    // We found the child,
                    node.collectObserversLocked(uri, index + 1, observer, selfNotify, calls);
                    if (segment != null) {
                        break;
                    }
                }
            }
        
        }
    }
	public void unregisterContentObserver(ContentObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("You must pass a valid observer");
        }
        
        mRootNode.removeObserverLocked(observer);
        if (DEBUG) Log.v(TAG, "Unregistered observer " + observer);
    
		
	}
}
