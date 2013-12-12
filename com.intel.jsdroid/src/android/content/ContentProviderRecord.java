package android.content;

import java.util.HashSet;

import android.app.ActivityManager.ContentProviderHolder;
import android.app.ProcessRecord;
//import android.content.ContentService.ContentProviderHolder;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;

public class ContentProviderRecord extends ContentProviderHolder{

    // All attached clients
    final HashSet<ProcessRecord> clients = new HashSet<ProcessRecord>();
    final int uid;
    final ApplicationInfo appInfo;
    final ComponentName name;
    public int externals;     // number of non-framework processes supported by this provider
    ProcessRecord app; // if non-null, hosting application
    public ProcessRecord launchingApp; // if non-null, waiting for this app to be launched.
    String stringName;
    
    public ContentProviderRecord(ProviderInfo _info, ApplicationInfo ai) {
        super(_info);
        uid = ai.uid;
        appInfo = ai;
        name = new ComponentName(_info.packageName, _info.name);
        noReleaseNeeded = true/*uid == 0 || uid == Process.SYSTEM_UID*/;
    }
    public ContentProviderRecord(ContentProviderRecord cpr) {
        super(cpr.info);
        uid = cpr.uid;
        appInfo = cpr.appInfo;
        name = cpr.name;
        noReleaseNeeded = cpr.noReleaseNeeded;
    }
    public boolean canRunHere(ProcessRecord app) {
        return true/*(info.multiprocess || info.processName.equals(app.processName))
                && (uid == Process.SYSTEM_UID || uid == app.info.uid)*/;
    }
}
