package android.content.pm;

import java.io.File;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.intel.mpt.annotation.MayloonStubAnnotation;


import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageParser.Activity;
import android.content.pm.PackageParser.ActivityIntentInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.AndroidException;
import android.util.Config;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LogPrinter;

public class PackageManager {

	   // Mapping from provider base names (first directory in content URI codePath)
    // to the provider information.
    final HashMap<String, PackageParser.Provider> mProviders =
            new HashMap<String, PackageParser.Provider>();
    // Keys are String (provider class name), values are Provider.
    final HashMap<ComponentName, PackageParser.Provider> mProvidersByComponent =
            new HashMap<ComponentName, PackageParser.Provider>();
    final HashMap<ResourceName, WeakReference<CharSequence> > sStringCache
    = new HashMap<ResourceName, WeakReference<CharSequence> >();

    private static long bootStartTime;
	/**
	* {@link PackageInfo} flag: return information about
	* activities in the package in {@link PackageInfo#activities}.
	*/
	public static final int GET_ACTIVITIES = 0x00000001;

	/**
	 * {@link PackageInfo} flag: return information about
	 * intent receivers in the package in
	 * {@link PackageInfo#receivers}.
	 */
	public static final int GET_RECEIVERS = 0x00000002;

	/**
	 * {@link PackageInfo} flag: return information about
	 * services in the package in {@link PackageInfo#services}.
	 */
	public static final int GET_SERVICES = 0x00000004;

	/**
	 * {@link PackageInfo} flag: return information about
	 * content providers in the package in
	 * {@link PackageInfo#providers}.
	 */
	public static final int GET_PROVIDERS = 0x00000008;

	/**
	 * {@link PackageInfo} flag: return information about
	 * instrumentation in the package in
	 * {@link PackageInfo#instrumentation}.
	 */
	public static final int GET_INSTRUMENTATION = 0x00000010;

	/**
	 * {@link PackageInfo} flag: return information about the
	 * intent filters supported by the activity.
	 */
	public static final int GET_INTENT_FILTERS = 0x00000020;

	/**
	 * {@link PackageInfo} flag: return information about the
	 * signatures included in the package.
	 */
	public static final int GET_SIGNATURES = 0x00000040;

	/**
	 * {@link ResolveInfo} flag: return the IntentFilter that
	 * was matched for a particular ResolveInfo in
	 * {@link ResolveInfo#filter}.
	 */
	public static final int GET_RESOLVED_FILTER = 0x00000040;

	/**
	 * {@link ComponentInfo} flag: return the {@link ComponentInfo#metaData}
	 * data {@link android.os.Bundle}s that are associated with a component.
	 * This applies for any API returning a ComponentInfo subclass.
	 */
	public static final int GET_META_DATA = 0x00000080;

	/**
	 * {@link PackageInfo} flag: return the
	 * {@link PackageInfo#gids group ids} that are associated with an
	 * application.
	 * This applies for any API returning an PackageInfo class, either
	 * directly or nested inside of another.
	 */
	public static final int GET_GIDS = 0x00000100;

	/**
	 * {@link PackageInfo} flag: include disabled components in the returned info.
	 */
	public static final int GET_DISABLED_COMPONENTS = 0x00000200;

	/**
	 * {@link ApplicationInfo} flag: return the
	 * {@link ApplicationInfo#sharedLibraryFiles paths to the shared libraries}
	 * that are associated with an application.
	 * This applies for any API returning an ApplicationInfo class, either
	 * directly or nested inside of another.
	 */
	public static final int GET_SHARED_LIBRARY_FILES = 0x00000400;

	/**
	 * {@link ProviderInfo} flag: return the
	 * {@link ProviderInfo#uriPermissionPatterns URI permission patterns}
	 * that are associated with a content provider.
	 * This applies for any API returning an ProviderInfo class, either
	 * directly or nested inside of another.
	 */
	public static final int GET_URI_PERMISSION_PATTERNS = 0x00000800;
	/**
	 * {@link PackageInfo} flag: return information about
	 * permissions in the package in
	 * {@link PackageInfo#permissions}.
	 */
	public static final int GET_PERMISSIONS = 0x00001000;

	/**
	 * Flag parameter to retrieve all applications(even uninstalled ones) with data directories.
	 * This state could have resulted if applications have been deleted with flag
	 * DONT_DELETE_DATA
	 * with a possibility of being replaced or reinstalled in future
	 */
	public static final int GET_UNINSTALLED_PACKAGES = 0x00002000;

	/**
	 * {@link PackageInfo} flag: return information about
	 * hardware preferences in
	 * {@link PackageInfo#configPreferences PackageInfo.configPreferences} and
	 * requested features in {@link PackageInfo#reqFeatures
	 * PackageInfo.reqFeatures}.
	 */
	public static final int GET_CONFIGURATIONS = 0x00004000;

	/**
	 * Resolution and querying flag: if set, only filters that support the
	 * {@link android.content.Intent#CATEGORY_DEFAULT} will be considered for
	 * matching.  This is a synonym for including the CATEGORY_DEFAULT in your
	 * supplied Intent.
	 */
	public static final int MATCH_DEFAULT_ONLY = 0x00010000;

	/**
	 * Permission check result: this is returned by {@link #checkPermission}
	 * if the permission has been granted to the given package.
	 */
	public static final int PERMISSION_GRANTED = 0;

	/**
	 * Permission check result: this is returned by {@link #checkPermission}
	 * if the permission has not been granted to the given package.
	 */
	public static final int PERMISSION_DENIED = -1;

	/**
	 * Signature check result: this is returned by {@link #checkSignatures}
	 * if all signatures on the two packages match.
	 */
	public static final int SIGNATURE_MATCH = 0;

	/**
	 * Signature check result: this is returned by {@link #checkSignatures}
	 * if neither of the two packages is signed.
	 */
	public static final int SIGNATURE_NEITHER_SIGNED = 1;

	/**
	 * Signature check result: this is returned by {@link #checkSignatures}
	 * if the first package is not signed but the second is.
	 */
	public static final int SIGNATURE_FIRST_NOT_SIGNED = -1;

	/**
	 * Signature check result: this is returned by {@link #checkSignatures}
	 * if the second package is not signed but the first is.
	 */
	public static final int SIGNATURE_SECOND_NOT_SIGNED = -2;

	/**
	 * Signature check result: this is returned by {@link #checkSignatures}
	 * if not all signatures on both packages match.
	 */
	public static final int SIGNATURE_NO_MATCH = -3;

	/**
	 * Signature check result: this is returned by {@link #checkSignatures}
	 * if either of the packages are not valid.
	 */
	public static final int SIGNATURE_UNKNOWN_PACKAGE = -4;

	public static final int COMPONENT_ENABLED_STATE_DEFAULT = 0;
	public static final int COMPONENT_ENABLED_STATE_ENABLED = 1;
	public static final int COMPONENT_ENABLED_STATE_DISABLED = 2;

	/**
	 * Flag parameter for {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} to
	 * indicate that this package should be installed as forward locked, i.e. only the app itself
	 * should have access to its code and non-resource assets.
	 * @hide
	 */
	public static final int INSTALL_FORWARD_LOCK = 0x00000001;

	/**
	 * Flag parameter for {@link #installPackage} to indicate that you want to replace an already
	 * installed package, if one exists.
	 * @hide
	 */
	public static final int INSTALL_REPLACE_EXISTING = 0x00000002;

	/**
	 * Flag parameter for {@link #installPackage} to indicate that you want to
	 * allow test packages (those that have set android:testOnly in their
	 * manifest) to be installed.
	 * @hide
	 */
	public static final int INSTALL_ALLOW_TEST = 0x00000004;

	/**
	 * Flag parameter for {@link #installPackage} to indicate that this
	 * package has to be installed on the sdcard.
	 * @hide
	 */
	public static final int INSTALL_EXTERNAL = 0x00000008;

	/**
	* Flag parameter for {@link #installPackage} to indicate that this
	* package has to be installed on the sdcard.
	* @hide
	*/
	public static final int INSTALL_INTERNAL = 0x00000010;

	/**
	 * Flag parameter for
	 * {@link #setComponentEnabledSetting(android.content.ComponentName, int, int)} to indicate
	 * that you don't want to kill the app containing the component.  Be careful when you set this
	 * since changing component states can make the containing application's behavior unpredictable.
	 */
	public static final int DONT_KILL_APP = 0x00000001;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} on success.
	 * @hide
	 */
	public static final int INSTALL_SUCCEEDED = 1;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the package is
	 * already installed.
	 * @hide
	 */
	public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the package archive
	 * file is invalid.
	 * @hide
	 */
	public static final int INSTALL_FAILED_INVALID_APK = -2;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the URI passed in
	 * is invalid.
	 * @hide
	 */
	public static final int INSTALL_FAILED_INVALID_URI = -3;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the package manager
	 * service found that the device didn't have enough storage space to install the app.
	 * @hide
	 */
	public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if a
	 * package is already installed with the same name.
	 * @hide
	 */
	public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the requested shared user does not exist.
	 * @hide
	 */
	public static final int INSTALL_FAILED_NO_SHARED_USER = -6;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * a previously installed package of the same name has a different signature
	 * than the new package (and the old package's data was not removed).
	 * @hide
	 */
	public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package is requested a shared user which is already installed on the
	 * device and does not have matching signature.
	 * @hide
	 */
	public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package uses a shared library that is not available.
	 * @hide
	 */
	public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package uses a shared library that is not available.
	 * @hide
	 */
	public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package failed while optimizing and validating its dex files,
	 * either because there was not enough storage or the validation failed.
	 * @hide
	 */
	public static final int INSTALL_FAILED_DEXOPT = -11;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package failed because the current SDK version is older than
	 * that required by the package.
	 * @hide
	 */
	public static final int INSTALL_FAILED_OLDER_SDK = -12;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package failed because it contains a content provider with the
	 * same authority as a provider already installed in the system.
	 * @hide
	 */
	public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package failed because the current SDK version is newer than
	 * that required by the package.
	 * @hide
	 */
	public static final int INSTALL_FAILED_NEWER_SDK = -14;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package failed because it has specified that it is a test-only
	 * package and the caller has not supplied the {@link #INSTALL_ALLOW_TEST}
	 * flag.
	 * @hide
	 */
	public static final int INSTALL_FAILED_TEST_ONLY = -15;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the package being installed contains native code, but none that is
	 * compatible with the the device's CPU_ABI.
	 * @hide
	 */
	public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package uses a feature that is not available.
	 * @hide
	 */
	public static final int INSTALL_FAILED_MISSING_FEATURE = -17;

	// ------ Errors related to sdcard
	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * a secure container mount point couldn't be accessed on external media.
	 * @hide
	 */
	public static final int INSTALL_FAILED_CONTAINER_ERROR = -18;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package couldn't be installed in the specified install
	 * location.
	 * @hide
	 */
	public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;

	/**
	 * Installation return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
	 * the new package couldn't be installed in the specified install
	 * location because the media is not available.
	 * @hide
	 */
	public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser was given a path that is not a file, or does not end with the expected
	 * '.apk' extension.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_NOT_APK = -100;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser was unable to retrieve the AndroidManifest.xml file.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser encountered an unexpected exception.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser did not find any certificates in the .apk.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser found inconsistent certificates on the files in the .apk.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser encountered a CertificateEncodingException in one of the
	 * files in the .apk.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser encountered a bad or missing package name in the manifest.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser encountered a bad shared user id name in the manifest.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser encountered some structural problem in the manifest.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;

	/**
	 * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the parser did not find any actionable tags (instrumentation or application)
	 * in the manifest.
	 * @hide
	 */
	public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;

	/**
	 * Installation failed return code: this is passed to the {@link IPackageInstallObserver} by
	 * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
	 * if the system failed to install the package because of system issues.
	 * @hide
	 */
	public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;

	/**
	 * Uninstallation return code: this is passed to the {@link IPackageDeleteObserver}
	 * on success.
	 * @hide
	 */
	public static final int UNINSTALL_SUCCEEDED = 1;

	/**
	 * Uninstallation return code: this is passed to the {@link IPackageDeleteObserver}
	 * if the package is not found.
	 * @hide
	 */
	public static final int UNINSTALL_FAILED_NOT_FOUND = -1;

	/**
	 * Uninstallation return code: this is passed to the {@link IPackageDeleteObserver}
	 * if the uninstallation failed and the package is a system package.
	 * @hide
	 */
	public static final int UNINSTALL_FAILED_SYSTEM = -2;

	/**
	 * Uninstallation failed return code: this is passed to the {@link IPackageDeleteObserver}
	 * if the system failed to uninstall the package because of generic errors.
	 * @hide
	 */
	public static final int UNINSTALL_FAILED_GENERIC_ERROR = -3;
	/**
	 * Flag parameter for {@link #deletePackage} to indicate that you don't want to delete the
	 * package's data directory.
	 *
	 * @hide
	 */
	public static final int DONT_DELETE_DATA = 0x00000001;

	/**
	 * Return code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)}
	 * when the package has been successfully moved by the system.
	 * @hide
	 */
	public static final int MOVE_SUCCEEDED = 1;
	/**
	 * Error code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)}
	 * when the package hasn't been successfully moved by the system
	 * because of insufficient memory on specified media.
	 * @hide
	 */
	public static final int MOVE_FAILED_INSUFFICIENT_STORAGE = -1;

	/**
	 * Error code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)}
	 * if the specified package doesn't exist.
	 * @hide
	 */
	public static final int MOVE_FAILED_DOESNT_EXIST = -2;

	/**
	 * Error code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)}
	 * if the specified package cannot be moved since its a system package.
	 * @hide
	 */
	public static final int MOVE_FAILED_SYSTEM_PACKAGE = -3;

	/**
	 * Error code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)}
	 * if the specified package cannot be moved since its forward locked.
	 * @hide
	 */
	public static final int MOVE_FAILED_FORWARD_LOCKED = -4;

	/**
	 * Error code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)}
	 * if the specified package cannot be moved to the specified location.
	 * @hide
	 */
	public static final int MOVE_FAILED_INVALID_LOCATION = -5;

	/**
	 * Error code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)}
	 * if the specified package cannot be moved to the specified location.
	 * @hide
	 */
	public static final int MOVE_FAILED_INTERNAL_ERROR = -6;

	/**
	 * Error code that is passed to the {@link IPackageMoveObserver} by
	 * {@link #movePackage(android.net.Uri, IPackageMoveObserver)} if the
	 * specified package already has an operation pending in the
	 * {@link PackageHandler} queue.
	 *
	 * @hide
	 */
	public static final int MOVE_FAILED_OPERATION_PENDING = -7;

	/**
	 * Flag parameter for {@link #movePackage} to indicate that
	 * the package should be moved to internal storage if its
	 * been installed on external media.
	 * @hide
	 */
	public static final int MOVE_INTERNAL = 0x00000001;

	/**
	 * Flag parameter for {@link #movePackage} to indicate that
	 * the package should be moved to external media.
	 * @hide
	 */
	public static final int MOVE_EXTERNAL_MEDIA = 0x00000002;

	public static final boolean SHOW_INFO = false;

	private static final Comparator<ResolveInfo> mResolvePrioritySorter = new Comparator<ResolveInfo>() {
		public int compare(ResolveInfo r1, ResolveInfo r2) {
			int v1 = r1.priority;
			int v2 = r2.priority;
			//System.out.println("Comparing: q1=" + q1 + " q2=" + q2);
			if (v1 != v2) {
				return (v1 > v2) ? -1 : 1;
			}
			v1 = r1.preferredOrder;
			v2 = r2.preferredOrder;
			if (v1 != v2) {
				return (v1 > v2) ? -1 : 1;
			}
			if (r1.isDefault != r2.isDefault) {
				return r1.isDefault ? -1 : 1;
			}
			v1 = r1.match;
			v2 = r2.match;
			//System.out.println("Comparing: m1=" + m1 + " m2=" + m2);
			return (v1 > v2) ? -1 : ((v1 < v2) ? 1 : 0);
		}
	};
	
	private static final Comparator<ProviderInfo> mProviderInitOrderSorter =
            new Comparator<ProviderInfo>() {
        public int compare(ProviderInfo p1, ProviderInfo p2) {
            final int v1 = p1.initOrder;
            final int v2 = p2.initOrder;
            return (v1 > v2) ? -1 : ((v1 < v2) ? 1 : 0);
        }
    };

	private final class ActivityIntentResolver extends
			IntentResolver<PackageParser.ActivityIntentInfo, ResolveInfo> {
		public List queryIntent(Intent intent, String resolvedType,
				boolean defaultOnly) {
			mFlags = defaultOnly ? PackageManager.MATCH_DEFAULT_ONLY : 0;
			return super.queryIntent(intent, resolvedType, defaultOnly);
		}

		public List<ResolveInfo> queryIntent(Intent intent,
				String resolvedType, int flags) {
			mFlags = flags;
			return super.queryIntent(intent, resolvedType,
					(flags & PackageManager.MATCH_DEFAULT_ONLY) != 0);
		}

		public List<ResolveInfo> queryIntentForPackage(Intent intent,
				String resolvedType, int flags,
				ArrayList<PackageParser.Activity> packageActivities) {
			if (packageActivities == null) {
				return null;
			}
			mFlags = flags;
			final boolean defaultOnly = (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0;
			int N = packageActivities.size();
			ArrayList<ArrayList<PackageParser.ActivityIntentInfo>> listCut = new ArrayList<ArrayList<PackageParser.ActivityIntentInfo>>(
					N);

			ArrayList<PackageParser.ActivityIntentInfo> intentFilters;
			for (int i = 0; i < N; ++i) {
				intentFilters = packageActivities.get(i).intents;
				if (intentFilters != null && intentFilters.size() > 0) {
					listCut.add(intentFilters);
				}
			}
			return super.queryIntentFromList(intent, resolvedType, defaultOnly,
					listCut);
		}

		public final void addActivity(PackageParser.Activity a, String type) {
			mActivities.put(a.getComponentName(), a);
			if (SHOW_INFO || Config.LOGV)
				Log.v(TAG,
						"  "
								+ type
								+ " "
								+ (a.info.nonLocalizedLabel != null ? a.info.nonLocalizedLabel
										: a.info.name) + ":");
			if (SHOW_INFO || Config.LOGV)
				Log.v(TAG, "    Class=" + a.info.name);
			int NI = a.intents.size();
			for (int j = 0; j < NI; j++) {
				PackageParser.ActivityIntentInfo intent = a.intents.get(j);
				if (SHOW_INFO || Config.LOGV) {
					Log.v(TAG, "    IntentFilter:");
					intent.dump(new LogPrinter(Log.VERBOSE, TAG), "      ");
				}
				if (!intent.debugCheck()) {
					Log.w(TAG, "==> For Activity " + a.info.name);
				}
				addFilter(intent);
			}
		}

		public final void removeActivity(PackageParser.Activity a, String type) {
			mActivities.remove(a.getComponentName());
			if (SHOW_INFO || Config.LOGV)
				Log.v(TAG,
						"  "
								+ type
								+ " "
								+ (a.info.nonLocalizedLabel != null ? a.info.nonLocalizedLabel
										: a.info.name) + ":");
			if (SHOW_INFO || Config.LOGV)
				Log.v(TAG, "    Class=" + a.info.name);
			int NI = a.intents.size();
			for (int j = 0; j < NI; j++) {
				PackageParser.ActivityIntentInfo intent = a.intents.get(j);
				if (SHOW_INFO || Config.LOGV) {
					Log.v(TAG, "    IntentFilter:");
					intent.dump(new LogPrinter(Log.VERBOSE, TAG), "      ");
				}
				removeFilter(intent);
			}
		}

		@Override
		protected boolean allowFilterResult(
				PackageParser.ActivityIntentInfo filter, List<ResolveInfo> dest) {
			ActivityInfo filterAi = filter.activity.info;
			for (int i = dest.size() - 1; i >= 0; i--) {
				ActivityInfo destAi = dest.get(i).activityInfo;
				if (destAi.name == filterAi.name
						&& destAi.packageName == filterAi.packageName) {
					return false;
				}
			}
			return true;
		}

		@Override
		protected String packageForFilter(PackageParser.ActivityIntentInfo info) {
			return info.activity.owner.packageName;
		}

		@Override
		protected ResolveInfo newResult(PackageParser.ActivityIntentInfo info,
				int match) {

			final PackageParser.Activity activity = info.activity;

			final ResolveInfo res = new ResolveInfo();
			res.activityInfo = PackageParser.generateActivityInfo(activity,
					mFlags);
			if ((mFlags & PackageManager.GET_RESOLVED_FILTER) != 0) {
				res.filter = info;
			}
			res.priority = info.getPriority();
			res.preferredOrder = activity.owner.mPreferredOrder;
			//System.out.println("Result: " + res.activityInfo.className +
			//                   " = " + res.priority);
			res.match = match;
			res.isDefault = info.hasDefault;
			res.labelRes = info.labelRes;
			res.nonLocalizedLabel = info.nonLocalizedLabel;
			res.icon = info.icon;
			return res;
		}

		@Override
		protected void sortResults(List<ResolveInfo> results) {
			Collections.sort(results, mResolvePrioritySorter);
		}

		@Override
		protected void dumpFilter(PrintWriter out, String prefix,
				PackageParser.ActivityIntentInfo filter) {
			out.print(prefix);
			//out.print(Integer.toHexString(System
			//		.identityHashCode(filter.activity)));
			out.print(' ');
			out.print(filter.activity.getComponentShortName());
			out.print(" filter ");
			//out.println(Integer.toHexString(System.identityHashCode(filter)));
		}

		//        List<ResolveInfo> filterEnabled(List<ResolveInfo> resolveInfoList) {
		//            final Iterator<ResolveInfo> i = resolveInfoList.iterator();
		//            final List<ResolveInfo> retList = Lists.newArrayList();
		//            while (i.hasNext()) {
		//                final ResolveInfo resolveInfo = i.next();
		//                if (isEnabledLP(resolveInfo.activityInfo)) {
		//                    retList.add(resolveInfo);
		//                }
		//            }
		//            return retList;
		//        }

		// Keys are String (activity class name), values are Activity.
		private final HashMap<ComponentName, PackageParser.Activity> mActivities = new HashMap<ComponentName, PackageParser.Activity>();
		private int mFlags;
	}

    private final class ServiceIntentResolver
    extends IntentResolver<PackageParser.ServiceIntentInfo, ResolveInfo> {
    	public List<ResolveInfo> queryIntent(Intent intent, String resolvedType,
    			boolean defaultOnly) {
    		mFlags = defaultOnly ? PackageManager.MATCH_DEFAULT_ONLY : 0;
    		return super.queryIntent(intent, resolvedType, defaultOnly);
    		}

    	public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags) {
    		mFlags = flags;
    		return super.queryIntent(intent, resolvedType,
    				(flags&PackageManager.MATCH_DEFAULT_ONLY) != 0);
    		}

	public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType,
	        int flags, ArrayList<PackageParser.Service> packageServices) {
	    if (packageServices == null) {
	        return null;
	    }
	    mFlags = flags;
	    final boolean defaultOnly = (flags&PackageManager.MATCH_DEFAULT_ONLY) != 0;
	    final int N = packageServices.size();
	    ArrayList<ArrayList<PackageParser.ServiceIntentInfo>> listCut =
	        new ArrayList<ArrayList<PackageParser.ServiceIntentInfo>>(N);

	    ArrayList<PackageParser.ServiceIntentInfo> intentFilters;
	    for (int i = 0; i < N; ++i) {
	        intentFilters = packageServices.get(i).intents;
	        if (intentFilters != null && intentFilters.size() > 0) {
	            listCut.add(intentFilters);
	        }
	    }
	    return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut);
	}

	public final void addService(PackageParser.Service a, String type) {
		mServices.put(a.getComponentName(), a);
		if (SHOW_INFO || Config.LOGV)
			Log.v(TAG,
					"  "
							+ type
							+ " "
							+ (a.info.nonLocalizedLabel != null ? a.info.nonLocalizedLabel
									: a.info.name) + ":");
		if (SHOW_INFO || Config.LOGV)
			Log.v(TAG, "    Class=" + a.info.name);
		int NI = a.intents.size();
		for (int j = 0; j < NI; j++) {
			PackageParser.ServiceIntentInfo intent = a.intents.get(j);
			if (SHOW_INFO || Config.LOGV) {
				Log.v(TAG, "    IntentFilter:");
				intent.dump(new LogPrinter(Log.VERBOSE, TAG), "      ");
			}
			if (!intent.debugCheck()) {
				Log.w(TAG, "==> For Activity " + a.info.name);
			}
			addFilter(intent);
		}
	}

	public final void removeService(PackageParser.Service a, String type) {
		mServices.remove(a.getComponentName());
		if (SHOW_INFO || Config.LOGV)
			Log.v(TAG,
					"  "
							+ type
							+ " "
							+ (a.info.nonLocalizedLabel != null ? a.info.nonLocalizedLabel
									: a.info.name) + ":");
		if (SHOW_INFO || Config.LOGV)
			Log.v(TAG, "    Class=" + a.info.name);
		int NI = a.intents.size();
		for (int j = 0; j < NI; j++) {
			PackageParser.ServiceIntentInfo intent = a.intents.get(j);
			if (SHOW_INFO || Config.LOGV) {
				Log.v(TAG, "    IntentFilter:");
				intent.dump(new LogPrinter(Log.VERBOSE, TAG), "      ");
			}
			removeFilter(intent);
		}
	}

	@Override
	protected boolean allowFilterResult(
	        PackageParser.ServiceIntentInfo filter, List<ResolveInfo> dest) {
	    ServiceInfo filterSi = filter.service.info;
	    for (int i=dest.size()-1; i>=0; i--) {
	        ServiceInfo destAi = dest.get(i).serviceInfo;
	        if (destAi.name == filterSi.name
	                && destAi.packageName == filterSi.packageName) {
	            return false;
	        }
	    }
	    return true;
	}

	@Override
	protected String packageForFilter(PackageParser.ServiceIntentInfo info) {
	    return info.service.owner.packageName;
	}

	@Override
	protected ResolveInfo newResult(PackageParser.ServiceIntentInfo info,
	        int match) {

	    final PackageParser.Service service = info.service;
	    final ResolveInfo res = new ResolveInfo();
	    res.serviceInfo = PackageParser.generateServiceInfo(service,
	            mFlags);
	    if ((mFlags&PackageManager.GET_RESOLVED_FILTER) != 0) {
	        res.filter = info;
	    }
	    res.priority = info.getPriority();
	    res.preferredOrder = service.owner.mPreferredOrder;
	    //System.out.println("Result: " + res.activityInfo.className +
	    //                   " = " + res.priority);
	    res.match = match;
	    res.isDefault = info.hasDefault;
	    res.labelRes = info.labelRes;
	    res.nonLocalizedLabel = info.nonLocalizedLabel;
	    res.icon = info.icon;
	//    res.system = isSystemApp(res.serviceInfo.applicationInfo);
	    return res;
	}

	@Override
	protected void sortResults(List<ResolveInfo> results) {
	    Collections.sort(results, mResolvePrioritySorter);
	}

	@Override
	protected void dumpFilter(PrintWriter out, String prefix,
	        PackageParser.ServiceIntentInfo filter) {
	    //out.print(prefix); out.print(
	            //Integer.toHexString(System.identityHashCode(filter.service)));
	            out.print(' ');
	            out.print(filter.service.getComponentShortName());
	            out.print(" filter ");
	            //out.println(Integer.toHexString(System.identityHashCode(filter)));
	}

	//List<ResolveInfo> filterEnabled(List<ResolveInfo> resolveInfoList) {
	//    final Iterator<ResolveInfo> i = resolveInfoList.iterator();
	//    final List<ResolveInfo> retList = Lists.newArrayList();
	//    while (i.hasNext()) {
	//        final ResolveInfo resolveInfo = (ResolveInfo) i;
	//        if (isEnabledLP(resolveInfo.serviceInfo)) {
	//            retList.add(resolveInfo);
	//        }
	//    }
	//    return retList;
	//}

	// Keys are String (activity class name), values are Activity.
	private final HashMap<ComponentName, PackageParser.Service> mServices
	        = new HashMap<ComponentName, PackageParser.Service>();
	private int mFlags;
};


	// All available activities, for your resolving pleasure.
	final ActivityIntentResolver mActivities = new ActivityIntentResolver();
	final ServiceIntentResolver mServices = new ServiceIntentResolver();

	private static final String TAG = "PackageManager";

    public PackageManager(/* Context context */) {
        /**
         * Traverse bin/apps for the list of "installed" packages
         */

        long start = System.currentTimeMillis();
        Log.w(TAG, "PackageManager begin to install packages at time:" + start);
        
        String appsDir = "bin/apps/";
        ArrayList<String> apps = new ArrayList<String>();

		/**
		@j2sNative
		try{
		 	var xmlhttp;
		 	if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		     	xmlhttp=new XMLHttpRequest();
		 	}
		 	else  {// code for IE6, IE5
		     	xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
		 	}

		 	xmlhttp.open("GET", appsDir, false);
		 	//xmlhttp.overrideMimeType("text/plain; charset=x-user-defined");
		 	xmlhttp.send();
		 	var operationsHTML = xmlhttp.responseText;

			//console.log(operationsHTML);

			// FIXME: hack, works for Chromium only! On other browsers this will be access denied.
			//
			//	http://code.google.com/p/chromium/issues/detail?id=20450
			//
			// The responseText looks like if get from local file system
			// <script>addRow("..","..",1,"4.0 kB","4/9/12 1:58:58 PM");</script>
			// <script>addRow("com.intel.jsdroid.account","com.intel.jsdroid.account",1,"4.0 kB","4/25/12 12:14:30 AM");</script>
			// <script>addRow("com.intel.jsdroid.calculator2","com.intel.jsdroid.calculator2",1,"4.0 kB","5/3/12 1:31:17 PM");</script>
			// <script>addRow("com.intel.jsdroid.sample","com.intel.jsdroid.sample",1,"4.0 kB","5/3/12 1:48:47 PM");</script>
			//
			// or looks like if get from remote server
			//
			//  <table><tr><th><img src="/icons/blank.gif" alt="[ICO]"></th><th><a href="?C=N;O=D">Name</a></th><th><a href="?C=M;O=A">Last modified</a></th><th><a href="?C=S;O=A">Size</a></th><th><a href="?C=D;O=A">Description</a></th></tr><tr><th colspan="5"><hr></th></tr>
			//  <tr><td valign="top"><img src="/icons/back.gif" alt="[DIR]"></td><td><a href="/mayloon/com.intel.jsdroid/bin/">Parent Directory</a></td><td>&nbsp;</td><td align="right">  - </td><td>&nbsp;</td></tr>
			//  <tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td><td><a href="com.example.android.snake/">com.example.android.snake/</a></td><td align="right">13-Jul-2012 15:55  </td><td align="right">  - </td><td>&nbsp;</td></tr>
			//  <tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td><td><a href="com.hykwok.StockPriceViewer/">com.hykwok.StockPriceViewer/</a></td><td align="right">13-Jul-2012 15:55  </td><td align="right">  - </td>
			//

			var offset = operationsHTML.indexOf('<script>addRow', 0);
			if (offset != -1) {
 			  operationsHTML = operationsHTML.substring(offset);
			}
			var match_results = operationsHTML.match(/"[A-z0-9]+(\.[A-z0-9]+)+(?=[\/]?")/g);
			for (var i=0; i<match_results.length; i++) {
 			var app = match_results[i].substring(1);
 			  if (!apps.contains(app)) {
   			    apps.add (app);
			  }
			}

			console.log(apps.size() + " applications found.");
		} catch (e){
		    console.log(e);
		}
		 */
		{
		}

		if(apps.isEmpty()){
//			apps.add("com.example.android.basicglsurfaceview");
//			apps.add("com.example.android.snake");
//			apps.add("com.hykwok.StockPriceViewer");
//			apps.add("com.intel.jsdroid.account");
//			apps.add("com.intel.jsdroid.baike");
//			apps.add("com.intel.jsdroid.calculator2");
//			apps.add("com.intel.jsdroid.com.dujin.amail");
//			apps.add("com.intel.jsdroid.fivechess");
//			apps.add("com.intel.jsdroid.home");
//			apps.add("com.intel.jsdroid.notepad");
//			apps.add("com.intel.jsdroid.timer");
//			apps.add("com.intel.jsdroid.weather");
//			apps.add("com.intel.linpack");
//			apps.add("com.m2m.chess.chessit");
//			apps.add("com.intel.jsdroid.fivechess");
//			apps.add("com.openglesbook.simplevertexshader.SimpleVertexShader");
		}
		/**
		File file = new File(appsDir);
		if (file.isDirectory() == false) {
			System.out.println("appsDir not found");
			return;
		}
		String[] filelist = file.list();
		for (int i = 0; i < filelist.length; ++i) {
			apps.add(filelist[i]);
		}
		/**/

        ArrayList<String> blackList = new ArrayList<String>();
        // blackList.add("com.m2m.chess");
        // blackList.add("com.intel.jsdroid.calculator2");
        // blackList.add("com.example.android.snake");
        blackList.add("com.test");
        blackList.add("com.intel.jsdroid.sample");
        blackList.add("com.intel.jsdroid.provider.test");
        blackList.add("com.intel.llf.hellobroadcast");
        blackList.add("com.intel.jsdroid.sample.service");
        blackList.add("com.intel.jsdroid.jsMemo");
        bootStartTime = SystemClock.uptimeMillis();
        for (String appName : apps) {
            if (blackList.indexOf(appName) < 0)
                installPackage(appName);
        }
        
        long end = System.currentTimeMillis();
        Log.w(TAG, "PackageManager finish to install packages at time:" + end);
        Log.w(TAG, "PackageManager installPackages: " + apps.toString() + " used time:" + (end-start) + "ms");
    }

    public static long getBootStartTime() {
        return bootStartTime;
    }

	/**
	* This exception is thrown when a given package, application, or component
	* name can not be found.
	*/
	@SuppressWarnings("serial")
	public static class NameNotFoundException extends AndroidException {
		public NameNotFoundException() {
		}

		public NameNotFoundException(String name) {
			super(name);
		}
	}

	// SIYU: for test
	public String[] getAllActivities(String packageName) {
		PackageParser.Package p = mPackages.get(packageName);
		if (p != null) {
			//			System.out.println("package installed");
		} else {
			Log.i(TAG,"package not installed");
			return null;
		}
		String[] activities = new String[p.activities.size()];
		for (int i = 0; i < p.activities.size(); ++i) {
			activities[i] = p.activities.get(i).className;
		}
		return activities;
	}

	public String getLauncherActivity(String packageName) {
		PackageParser.Package p = mPackages.get(packageName);
		if (p != null) {
			//			System.out.println("package installed");
		} else {
			Log.i(TAG,"package not installed");
			return null;
		}
		boolean findActionMain = false;
		boolean findCategoryLaunch = false;

		Activity activity = null;
		ActivityIntentInfo intentInfo = null;
		for (int i = 0; i < p.activities.size(); ++i) {
			activity = p.activities.get(i);
			for (int j = 0; j < activity.intents.size(); ++j) {
				intentInfo = activity.intents.get(j);
				findActionMain = intentInfo
						.hasAction(android.content.Intent.ACTION_MAIN);
				findCategoryLaunch = intentInfo
						.hasCategory(android.content.Intent.CATEGORY_LAUNCHER);
				if (findActionMain == true && findCategoryLaunch == true) {
					//					System.out.println("PM: " + activity.className);
					return activity.className;
				} else {
					findActionMain = false;
					findCategoryLaunch = false;
				}
			}
		}
		return null;
	}

	public CharSequence getApplicationLabel(ApplicationInfo info) {
		return info.loadLabel(this);
	}

    public CharSequence getText(String packageName, int resid,
            ApplicationInfo appInfo) {
        ResourceName name = new ResourceName(packageName, resid);
        String curAppPackageName = ActivityThread.currentApplication().getPackageName();
        if (!curAppPackageName.equals(name.packageName)) {
            return null;
        }

        CharSequence text = getCachedString(name);
        if (text != null) {
            return text;
        }
        if (appInfo == null) {
            try {
                appInfo = getApplicationInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                return null;
            }
        }
        try {
            Resources r = getResourcesForApplication(appInfo);
            text = r.getText(resid);
            putCachedString(name, text);
            return text;
        } catch (NameNotFoundException e) {
            Log.w("PackageManager", "Failure retrieving resources for"
                    + appInfo.packageName);
        } catch (RuntimeException e) {
            // If an exception was thrown, fall through to return
            // default icon.
            Log.w("PackageManager", "Failure retrieving text 0x"
                    + Integer.toHexString(resid) + " in package "
                    + packageName, e);
        }
        return null;
    }

	private static String fixProcessName(String defProcessName,
			String processName, int uid) {
		if (processName == null) {
			return defProcessName;
		}
		return processName;
	}

	public void installPackage(String packageName) {
		PackageParser pp = new PackageParser("");
		DisplayMetrics dm = new DisplayMetrics();
		dm.setToDefaults();
		Log.i(TAG, "Trying to install " + packageName);
		PackageParser.Package newPackage = pp.parsePackage("bin/apps/"
				+ packageName + "/", "", dm, 0);

		if (newPackage != null) {

			mPackages.put(newPackage.packageName, newPackage);

			int N = newPackage.activities.size();

			StringBuilder r = null;
			for (int i = 0; i < N; i++) {
				PackageParser.Activity a = newPackage.activities.get(i);
				a.info.processName = fixProcessName(
						newPackage.applicationInfo.processName,
						a.info.processName, newPackage.applicationInfo.uid);
				mActivities.addActivity(a, "activity");
				if (r == null) {
					r = new StringBuilder(256);
				} else {
					r.append(' ');
				}
				r.append(a.info.name);
			}
			if (r != null) {
				//				if (Config.LOGD)
				//					Log.d(TAG, "  Activities: " + r);
			}

			/**
			 * @Mayloon update
			 * Get instrumentation info and add it to mInstrumentation
			 */
            N = newPackage.instrumentation.size();
            r = null;
            for (int i = 0; i < N; i++) {
                PackageParser.Instrumentation a = newPackage.instrumentation.get(i);
                a.info.packageName = newPackage.applicationInfo.packageName;
                a.info.sourceDir = newPackage.applicationInfo.sourceDir;
                a.info.publicSourceDir = newPackage.applicationInfo.publicSourceDir;
                a.info.dataDir = newPackage.applicationInfo.dataDir;
                a.info.nativeLibraryDir = newPackage.applicationInfo.nativeLibraryDir;
                mInstrumentation.put(a.getComponentName(), a);

                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(a.info.name);

            }
            if (r != null) {
                // if (Config.LOGD) Log.d(TAG, "  Instrumentation: " + r);
            }

			//add new broadcast receivers
			N = newPackage.receivers.size();
			r = null;
			for (int i = 0; i < N; i++) {
				PackageParser.Activity a = newPackage.receivers.get(i);
				a.info.processName = fixProcessName(
						newPackage.applicationInfo.processName,
						a.info.processName, newPackage.applicationInfo.uid);
				mReceivers.addActivity(a, "receiver");
			}

			N = newPackage.services.size();
			Log.i(TAG, "Number of services " + N);
			r = null;
			for (int i1 = 0; i1 < N; i1++) {
				PackageParser.Service s = newPackage.services.get(i1);
				s.info.processName = fixProcessName(
						newPackage.applicationInfo.processName,
						s.info.processName, newPackage.applicationInfo.uid);
				mServices.addService(s, "service");

				if (r == null) {
					r = new StringBuilder(256);
				} else {
					r.append(' ');
				}
				r.append(s.info.name);
			}
			if (r != null) {
				if (Config.LOGD)
					Log.d(TAG, "  Services: " + r);
			}

			//add content provider
			N = newPackage.providers.size();
			r = null;
	        for (int i1=0; i1<N; i1++) {
	                PackageParser.Provider p = newPackage.providers.get(i1);
	                p.info.processName = fixProcessName(newPackage.applicationInfo.processName,
	                        p.info.processName, newPackage.applicationInfo.uid);
	                mProvidersByComponent.put(new ComponentName(p.info.packageName,
	                        p.info.name), p);
	                p.syncable = p.info.isSyncable;
	                if (p.info.authority != null) {
	                    String names[] = p.info.authority.split(";");
	                    p.info.authority = null;
	                    for (int j = 0; j < names.length; j++) {
	                        if (j == 1 && p.syncable) {
	                            // We only want the first authority for a provider to possibly be
	                            // syncable, so if we already added this provider using a different
	                            // authority clear the syncable flag. We copy the provider before
	                            // changing it because the mProviders object contains a reference
	                            // to a provider that we don't want to change.
	                            // Only do this for the second authority since the resulting provider
	                            // object can be the same for all future authorities for this provider.
	                            p = new PackageParser.Provider(p);
	                            p.syncable = false;
	                        }
	                        if (!mProviders.containsKey(names[j])) {
	                            mProviders.put(names[j], p);
	                            if (p.info.authority == null) {
	                                p.info.authority = names[j];
	                            } else {
	                                p.info.authority = p.info.authority + ";" + names[j];
	                            }
	                        } else {
	                            PackageParser.Provider other = mProviders.get(names[j]);
	                        }
	                    }
	                }
	                if (r == null) {
						r = new StringBuilder(256);
					} else {
						r.append(' ');
					}
	                r.append(p.info.name);

	            }
	            if (r != null) {
	                Log.i(TAG, "  Providers: " + r);
	            }


			Log.i(TAG, "successfully installed " + newPackage.packageName);
		} else {
			Log.e(TAG, packageName + " is not installed");
		}
	}

	private HashMap<String, PackageParser.Package> mPackages = new HashMap<String, PackageParser.Package>();
	private HashMap<ComponentName, PackageParser.Instrumentation> mInstrumentation = new HashMap<ComponentName, PackageParser.Instrumentation>();

	public Resources getPackageResources(String packageName) {
		return mPackages.get(packageName).mResource;
	}
	
	public Drawable getActivityIcon(ComponentName activityName)
            throws NameNotFoundException {
        return getActivityInfo(activityName, 0).loadIcon(this);
    }
	
	public Drawable getActivityIcon(Intent intent)
            throws NameNotFoundException {
        if (intent.getComponent() != null) {
            return getActivityIcon(intent.getComponent());
        }

        ResolveInfo info = resolveActivity(
            intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null) {
            return info.activityInfo.loadIcon(this);
        }

        throw new NameNotFoundException(intent.toURI());
    }

	public Drawable getDefaultActivityIcon() {
		return Resources.getSystem().getDrawable(
				com.android.internal.R.drawable.sym_def_app_icon);
	}
	
	public Drawable getApplicationIcon(ApplicationInfo info) {
        return info.loadIcon(this);
    }

    public Drawable getApplicationIcon(String packageName)
            throws NameNotFoundException {
        return getApplicationIcon(getApplicationInfo(packageName, 0));
    }

	public PackageInfo getPackageInfo(String packageName, int flags) {
		PackageInfo pi = null;
		PackageParser.Package pkg = mPackages.get(packageName);
		if (pkg != null) {
			pi = new PackageInfo();
			pi.packageName = pkg.packageName;
			pi.activities = new ActivityInfo[pkg.activities.size()];
			for (int i = 0; i < pkg.activities.size(); ++i) {
				pi.activities[i] = pkg.activities.get(i).info;
			}
		}
		return pi;
	}

	public ApplicationInfo getApplicationInfo(String packageName, int flags)
			throws NameNotFoundException {
		PackageParser.Package p = mPackages.get(packageName);
		if (Config.LOGV)
			Log.v(TAG, "getApplicationInfo " + packageName + ": " + p);
		if (p != null) {
			// Note: isEnabledLP() does not apply here - always return info
			return PackageParser.generateApplicationInfo(p, flags);
		}
		if ("android".equals(packageName) || "system".equals(packageName)) {
//			return mAndroidApplication;
		}
		return null;
	}

    public InstrumentationInfo getInstrumentationInfo(ComponentName name,
            int flags) {
        /**
         * @Mayloon update Remove synchronized
         */
        final PackageParser.Instrumentation i = mInstrumentation.get(name);
        return PackageParser.generateInstrumentationInfo(i, flags);
    }

    public ActivityInfo getActivityInfo(ComponentName cmpName, int flags)
            throws NameNotFoundException {
        String cls = cmpName.getClassName();
        PackageParser.Package pkg = mPackages.get(cmpName.getPackageName());
        ActivityInfo activityinfo = new ActivityInfo();
        if (pkg != null) {
            for (int i = 0; i < pkg.activities.size(); ++i) {
                if (pkg.activities.get(i).className.equals(cls)) {
                    activityinfo = pkg.activities.get(i).info;
                    activityinfo.metaData = pkg.activities.get(i).metaData;
                    return activityinfo;
                }
            }
        }
        throw new NameNotFoundException("Activity " + cls + " not found");
    }


    public ServiceInfo getServiceInfo(ComponentName cmpName, int flags) throws NameNotFoundException {
        	String cls = cmpName.getClassName();
    		PackageParser.Package pkg = mPackages.get(cmpName.getPackageName());
    		if (pkg != null) {
    			for (int i = 0; i < pkg.services.size(); ++i) {
    				if (pkg.services.get(i).className.equals(cls))
    				{
    						return pkg.services.get(i).info;
    				}
    			}
    		}

    		throw new NameNotFoundException("Service: " + cls + " not found");

    }

	public Drawable getDrawable(String packageName, int resid,
			ApplicationInfo appInfo) {

		Resources r = getPackageResources(packageName);
		Drawable dr = r.getDrawable(resid);
//		if(dr==null)
//			System.out.println("Impossible!!!");
//		else {
//			System.out.println("It's OK!!! Got it!!");
//		}
		return dr;
	}

    public XmlResourceParser getXml(String packageName, int resid,
            ApplicationInfo appInfo) {
        if (appInfo == null) {
            try {
                appInfo = getApplicationInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                return null;
            }
        }
        try {
            Resources r = getResourcesForApplication(appInfo);
            return r.getXml(resid);
        } catch (RuntimeException e) {
            // If an exception was thrown, fall through to return
            // default icon.
            Log.w("PackageManager", "Failure retrieving xml 0x"
                    + Integer.toHexString(resid) + " in package "
                    + packageName, e);
        } catch (NameNotFoundException e) {
            Log.w("PackageManager", "Failure retrieving resources for"
                    + appInfo.packageName);
        }
        return null;
    }
    
    public Resources getResourcesForActivity(
            ComponentName activityName) throws NameNotFoundException {
        return getResourcesForApplication(
            getActivityInfo(activityName, 0).applicationInfo);
    }

    public Resources getResourcesForApplication(
            ApplicationInfo app) throws NameNotFoundException {
        if (app.packageName.equals("system")) {
            return Context.getSystemContext().getResources();
        }

        Resources r = ActivityThread.currentApplication().getApplicationContext().getResources();
        if (r != null) {
            return r;
        }
        throw new NameNotFoundException("Unable to open " + app.publicSourceDir);
    }
    
    public Resources getResourcesForApplication(
            String appPackageName) throws NameNotFoundException {
        if (appPackageName.equals("system")) {
            return Context.getSystemContext().getResources();
        }

        Resources r = ActivityThread.currentApplication().getApplicationContext().getResources();
        if (r != null) {
            return r;
        }
        throw new NameNotFoundException("Unable to open " + appPackageName);
    }
    
	public ResolveInfo resolveIntent(Intent intent, String resolvedType,
			int flags) {
		Log.i(TAG, " resolveIntent1");
		List<ResolveInfo> query = queryIntentActivities(intent, resolvedType,
				flags);
		Log.i(TAG, " resolveIntent2");
		Log.i(TAG, ":"+query.size());
		return chooseBestActivity(intent, resolvedType, flags, query);
	}

	public ResolveInfo resolveActivity(Intent intent, int flags) {
		return resolveIntent(intent, null, flags);
	}
	
	public ResolveInfo resolveService(Intent intent, int flags) {
		return resolveService(intent,
				intent.resolveTypeIfNeeded(Context.getSystemContext().getContentResolver()),
				flags);
    }

	public ResolveInfo resolveService(Intent intent, String resolvedType, int flags) {
        List<ResolveInfo> query = queryIntentServices(intent, resolvedType, flags);
        if (query != null) {
            if (query.size() >= 1) {
                // If there is more than one service with the same priority,
                // just arbitrarily pick the first one.
                return query.get(0);
            }
        }
        return null;
    }



	private ResolveInfo chooseBestActivity(Intent intent, String resolvedType,
			int flags, List<ResolveInfo> query) {
		if (query != null) {
			final int N = query.size();
			if (N == 1) {
				return query.get(0);
			} else if (N > 1) {
				Log.i(TAG, "impossible for now! N: " + N);
				// If there is more than one activity with the same priority,
				// then let the user decide between them.
				
				ResolveInfo r0 = query.get(0);
				ResolveInfo r1 = query.get(1);
				if (false) {
					System.out
							.println(r0.activityInfo.name + "=" + r0.priority
									+ " vs " + r1.activityInfo.name + "="
									+ r1.priority);
				}
				// If the first activity has a higher priority, or a different
				// default, then it is always desireable to pick it.
				if (r0.priority != r1.priority
						|| r0.preferredOrder != r1.preferredOrder
						|| r0.isDefault != r1.isDefault) {
					return query.get(0);
				}
				/**
				// If we have saved a preference for a preferred activity for
				// this Intent, use that.
				ResolveInfo ri = findPreferredActivity(intent, resolvedType,
						flags, query, r0.priority);
				if (ri != null) {
					return ri;
				}
				return mResolveInfo;
				/**/
			}
		}
		return null;
	}

	public List<ResolveInfo> queryIntentActivities(Intent intent,
			String resolvedType, int flags) {
		ComponentName comp = intent.getComponent();
		if (comp != null) {
			List<ResolveInfo> list = new ArrayList<ResolveInfo>(1);
			ActivityInfo ai = null;
			try {
				ai = getActivityInfo(comp, flags);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			if (ai != null) {
				ResolveInfo ri = new ResolveInfo();
				ri.activityInfo = ai;
				list.add(ri);
			}
			return list;
		}

		String pkgName = intent.getPackage();
		Log.i(TAG,"pkgname:"+pkgName);
		if (pkgName == null) {
			return (List<ResolveInfo>) mActivities.queryIntent(intent,
					resolvedType, flags);
		}
		PackageParser.Package pkg = mPackages.get(pkgName);
		if (pkg != null) {
			return (List<ResolveInfo>) mActivities.queryIntentForPackage(
					intent, resolvedType, flags, pkg.activities);
		}
		return null;
	}
	
	 public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
		 return queryIntentServices(intent,
				 intent.resolveTypeIfNeeded(Context.getSystemContext().getContentResolver()),flags);
     }

	public List<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, int flags) {
        final ComponentName comp = intent.getComponent();
        if (comp != null) {
            final List<ResolveInfo> list = new ArrayList<ResolveInfo>(1);
            ServiceInfo si;
			try {
				si = getServiceInfo(comp, flags);

				 if (si != null) {
		                final ResolveInfo ri = new ResolveInfo();
		                ri.serviceInfo = si;
		                list.add(ri);
		            }
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            return list;
        }

        // reader
        String pkgName = intent.getPackage();
		if (pkgName == null) {
			return (List<ResolveInfo>) mServices.queryIntent(intent,
					resolvedType, flags);
		}
		PackageParser.Package pkg = mPackages.get(pkgName);
		if (pkg != null) {
			return (List<ResolveInfo>) mServices.queryIntentForPackage(
					intent, resolvedType, flags, pkg.services);
		}
		return null;
    }


	public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
		return queryIntentActivities(intent, null, flags);
	}

	public ProviderInfo resolveContentProvider(String name, int flags){
		final PackageParser.Provider provider = mProviders.get(name);
		return provider != null
        ? PackageParser.generateProviderInfo(provider, flags)
        : null;
	}
	
	public List<ProviderInfo> queryContentProviders(String processName,
            int uid, int flags) {
        ArrayList<ProviderInfo> finalList = null;

        synchronized (mPackages) {
            Iterator<PackageParser.Provider> i = mProvidersByComponent.values().iterator();
            while (i.hasNext()) {
                PackageParser.Provider p = i.next();
                if (p.info.authority != null
                    && (processName == null ||
                            (p.info.processName.equals(processName)
                                    && p.info.applicationInfo.uid == uid))) {
                    if (finalList == null) {
                        finalList = new ArrayList<ProviderInfo>(3);
                    }
                    finalList.add(PackageParser.generateProviderInfo(p,
                            flags));
                }
            }
        }

        if (finalList != null) {
            Collections.sort(finalList, mProviderInitOrderSorter);
        }

        return finalList;
    }
	
	public List<InstrumentationInfo> queryInstrumentation(String targetPackage,
            int flags) {
        ArrayList<InstrumentationInfo> finalList =
            new ArrayList<InstrumentationInfo>();

        synchronized (mPackages) {
            Iterator<PackageParser.Instrumentation> i = mInstrumentation.values().iterator();
            while (i.hasNext()) {
                PackageParser.Instrumentation p = i.next();
                if (targetPackage == null
                        || targetPackage.equals(p.info.targetPackage)) {
                    finalList.add(PackageParser.generateInstrumentationInfo(p,
                            flags));
                }
            }
        }

        return finalList;
    }

	// All available receivers, for your resolving pleasure.
    final ActivityIntentResolver mReceivers =
            new ActivityIntentResolver();

	public ActivityInfo getReceiverInfo(ComponentName component, int flags) {
        synchronized (mPackages) {
            PackageParser.Activity a = mReceivers.mActivities.get(component);
            if (a != null ) {
                return PackageParser.generateActivityInfo(a, flags);
            }
        }
        return null;
    }
	
	public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
		return queryIntentReceivers(intent,
				intent.resolveTypeIfNeeded(Context.getSystemContext().getContentResolver()),flags);
    }

	public List<ResolveInfo> queryIntentReceivers(Intent intent,
            String resolvedType, int flags) {
        ComponentName comp = intent.getComponent();
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<ResolveInfo>(1);
            ActivityInfo ai = getReceiverInfo(comp, flags);
            if (ai != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
            }
            return list;
        }

        /**
        /*@Mayloon update
        /**Remove synchronize
        **/
        String pkgName = intent.getPackage();


        if (pkgName == null) {
        	Log.i(TAG,"pkgName null");
            return (List<ResolveInfo>)mReceivers.queryIntent(intent,
                    resolvedType, flags);
        }
        PackageParser.Package pkg = mPackages.get(pkgName);
        if (pkg != null) {
            return (List<ResolveInfo>) mReceivers.queryIntentForPackage(intent,
                    resolvedType, flags, pkg.receivers);
        }
        return null;
    }

    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller,
            Intent[] specifics, String[] specificTypes, Intent intent,
            String resolvedType, int flags) {
        final String resultsAction = intent.getAction();

        List<ResolveInfo> results = queryIntentActivities(
            intent, resolvedType, flags|PackageManager.GET_RESOLVED_FILTER);
        if (Config.LOGV) Log.v(TAG, "Query " + intent + ": " + results);

        int specificsPos = 0;
        int N;

        // todo: note that the algorithm used here is O(N^2).  This
        // isn't a problem in our current environment, but if we start running
        // into situations where we have more than 5 or 10 matches then this
        // should probably be changed to something smarter...

        // First we go through and resolve each of the specific items
        // that were supplied, taking care of removing any corresponding
        // duplicate items in the generic resolve list.
        try{
            if (specifics != null) {
                for (int i=0; i<specifics.length; i++) {
                    final Intent sintent = specifics[i];
                    if (sintent == null) {
                        continue;
                    }

                    if (Config.LOGV) Log.v(TAG, "Specific #" + i + ": " + sintent);
                    String action = sintent.getAction();
                    if (resultsAction != null && resultsAction.equals(action)) {
                        // If this action was explicitly requested, then don't
                        // remove things that have it.
                        action = null;
                    }
                    ComponentName comp = sintent.getComponent();
                    ResolveInfo ri = null;
                    ActivityInfo ai = null;
                    if (comp == null) {
                        ri = resolveIntent(
                            sintent,
                            specificTypes != null ? specificTypes[i] : null,
                            flags);
                        if (ri == null) {
                            continue;
                        }
    //                    if (ri == mResolveInfo) {
    //                        // ACK!  Must do something better with this.
    //                    }
                        ai = ri.activityInfo;
                        comp = new ComponentName(ai.applicationInfo.packageName,
                                ai.name);
                    } else {
                        ai = getActivityInfo(comp, flags);
                        if (ai == null) {
                            continue;
                        }
                    }

                    // Look for any generic query activities that are duplicates
                    // of this specific one, and remove them from the results.
                    if (Config.LOGV) Log.v(TAG, "Specific #" + i + ": " + ai);
                    N = results.size();
                    int j;
                    for (j=specificsPos; j<N; j++) {
                        ResolveInfo sri = results.get(j);
                        if ((sri.activityInfo.name.equals(comp.getClassName())
                                && sri.activityInfo.applicationInfo.packageName.equals(
                                        comp.getPackageName()))
                            || (action != null && sri.filter.matchAction(action))) {
                            results.remove(j);
                            if (Config.LOGV) Log.v(
                                TAG, "Removing duplicate item from " + j
                                + " due to specific " + specificsPos);
                            if (ri == null) {
                                ri = sri;
                            }
                            j--;
                            N--;
                        }
                    }

                    // Add this specific item to its proper place.
                    if (ri == null) {
                        ri = new ResolveInfo();
                        ri.activityInfo = ai;
                    }
                    results.add(specificsPos, ri);
                    ri.specificIndex = i;
                    specificsPos++;
                }
            }

            // Now we go through the remaining generic results and remove any
            // duplicate actions that are found here.
            N = results.size();
            for (int i=specificsPos; i<N-1; i++) {
                final ResolveInfo rii = results.get(i);
                if (rii.filter == null) {
                    continue;
                }

                // Iterate over all of the actions of this result's intent
                // filter...  typically this should be just one.
                final Iterator<String> it = rii.filter.actionsIterator();
                if (it == null) {
                    continue;
                }
                while (it.hasNext()) {
                    final String action = it.next();
                    if (resultsAction != null && resultsAction.equals(action)) {
                        // If this action was explicitly requested, then don't
                        // remove things that have it.
                        continue;
                    }
                    for (int j=i+1; j<N; j++) {
                        final ResolveInfo rij = results.get(j);
                        if (rij.filter != null && rij.filter.hasAction(action)) {
                            results.remove(j);
                            if (Config.LOGV) Log.v(
                                TAG, "Removing duplicate item from " + j
                                + " due to action " + action + " at " + i);
                            j--;
                            N--;
                        }
                    }
                }

                // If the caller didn't request filter information, drop it now
                // so we don't have to marshall/unmarshall it.
                if ((flags&PackageManager.GET_RESOLVED_FILTER) == 0) {
                    rii.filter = null;
                }
            }

            // Filter out the caller activity if so requested.
            if (caller != null) {
                N = results.size();
                for (int i=0; i<N; i++) {
                    ActivityInfo ainfo = results.get(i).activityInfo;
                    if (caller.getPackageName().equals(ainfo.applicationInfo.packageName)
                            && caller.getClassName().equals(ainfo.name)) {
                        results.remove(i);
                        break;
                    }
                }
            }

            // If the caller didn't request filter information,
            // drop them now so we don't have to
            // marshall/unmarshall it.
            if ((flags&PackageManager.GET_RESOLVED_FILTER) == 0) {
                N = results.size();
                for (int i=0; i<N; i++) {
                    results.get(i).filter = null;
                }
            }

            if (Config.LOGV) Log.v(TAG, "Result: " + results);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Error parsing alias", e);
        }
        return results;
    }

    public List<ResolveInfo> queryIntentActivityOptions(
            ComponentName caller, Intent[] specifics, Intent intent,
            int flags) {
        final ContentResolver resolver = Context.getSystemContext().getContentResolver();

        String[] specificTypes = null;
        if (specifics != null) {
            final int N = specifics.length;
            for (int i=0; i<N; i++) {
                Intent sp = specifics[i];
                if (sp != null) {
                    String t = sp.resolveTypeIfNeeded(resolver);
                    if (t != null) {
                        if (specificTypes == null) {
                            specificTypes = new String[N];
                        }
                        specificTypes[i] = t;
                    }
                }
            }
        }
        return queryIntentActivityOptions(caller, specifics,
        		specificTypes, intent, intent.resolveTypeIfNeeded(resolver),
                flags);
    }
    
    private static final class ResourceName {
        final String packageName;
        final int iconId;

        ResourceName(String _packageName, int _iconId) {
            packageName = _packageName;
            iconId = _iconId;
        }

        ResourceName(ApplicationInfo aInfo, int _iconId) {
            this(aInfo.packageName, _iconId);
        }

        ResourceName(ComponentInfo cInfo, int _iconId) {
            this(cInfo.applicationInfo.packageName, _iconId);
        }

        ResourceName(ResolveInfo rInfo, int _iconId) {
            this(rInfo.activityInfo.applicationInfo.packageName, _iconId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceName that = (ResourceName) o;

            if (iconId != that.iconId) return false;
            return !(packageName != null ?
                    !packageName.equals(that.packageName) : that.packageName != null);

        }

        @Override
        public int hashCode() {
            int result;
            result = packageName.hashCode();
            result = 31 * result + iconId;
            return result;
        }

        @Override
        public String toString() {
            return "{ResourceName " + packageName + " / " + iconId + "}";
        }
    }

    private CharSequence getCachedString(ResourceName name) {
        WeakReference<CharSequence> wr = sStringCache.get(name);
        if (wr != null) { // we have the activity
            CharSequence cs = wr.get();
            if (cs != null) {
                return cs;
            }
            // our entry has been purged
            sStringCache.remove(name);
        }
        return null;
    }

    private void putCachedString(ResourceName name, CharSequence cs) {
        sStringCache.put(name, new WeakReference<CharSequence>(cs));
    }

    /**
     * @j2sNative
     * console.log("Missing method: movePackage");
     */
    @MayloonStubAnnotation()
    public void movePackage(String packageName, Object observer, int flags) {
        System.out.println("Stub" + " Function : movePackage");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: canonicalToCurrentPackageNames");
     */
    @MayloonStubAnnotation()
    public String[] canonicalToCurrentPackageNames(String[] names) {
        System.out.println("Stub" + " Function : canonicalToCurrentPackageNames");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getSystemSharedLibraryNames");
     */
    @MayloonStubAnnotation()
    public String[] getSystemSharedLibraryNames() {
        System.out.println("Stub" + " Function : getSystemSharedLibraryNames");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getInstalledApplications");
     */
    @MayloonStubAnnotation()
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        System.out.println("Stub" + " Function : getInstalledApplications");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: hasSystemFeature");
     */
    @MayloonStubAnnotation()
    public boolean hasSystemFeature(String name) {
        System.out.println("Stub" + " Function : hasSystemFeature");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getInstallerPackageName");
     */
    @MayloonStubAnnotation()
    public String getInstallerPackageName(String packageName) {
        System.out.println("Stub" + " Function : getInstallerPackageName");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getPackagesForUid");
     */
    @MayloonStubAnnotation()
    public String[] getPackagesForUid(int uid) {
        System.out.println("Stub" + " Function : getPackagesForUid");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getPackageGids");
     */
    @MayloonStubAnnotation()
    public int[] getPackageGids(String packageName) {
        System.out.println("Stub" + " Function : getPackageGids");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getPreferredPackages");
     */
    @MayloonStubAnnotation()
    public List<PackageInfo> getPreferredPackages(int flags) {
        System.out.println("Stub" + " Function : getPreferredPackages");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: addPackageToPreferred");
     */
    @MayloonStubAnnotation()
    public void addPackageToPreferred(String packageName) {
        System.out.println("Stub" + " Function : addPackageToPreferred");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: removePackageFromPreferred");
     */
    @MayloonStubAnnotation()
    public void removePackageFromPreferred(String packageName) {
        System.out.println("Stub" + " Function : removePackageFromPreferred");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getApplicationEnabledSetting");
     */
    @MayloonStubAnnotation()
    public int getApplicationEnabledSetting(String packageName) {
        System.out.println("Stub" + " Function : getApplicationEnabledSetting");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: clearPackagePreferredActivities");
     */
    @MayloonStubAnnotation()
    public void clearPackagePreferredActivities(String packageName) {
        System.out.println("Stub" + " Function : clearPackagePreferredActivities");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: removePermission");
     */
    @MayloonStubAnnotation()
    public void removePermission(String name) {
        System.out.println("Stub" + " Function : removePermission");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isSafeMode");
     */
    @MayloonStubAnnotation()
    public boolean isSafeMode() {
        System.out.println("Stub" + " Function : isSafeMode");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getNameForUid");
     */
    @MayloonStubAnnotation()
    public String getNameForUid(int uid) {
        System.out.println("Stub" + " Function : getNameForUid");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: currentToCanonicalPackageNames");
     */
    @MayloonStubAnnotation()
    public String[] currentToCanonicalPackageNames(String[] names) {
        System.out.println("Stub" + " Function : currentToCanonicalPackageNames");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getInstalledPackages");
     */
    @MayloonStubAnnotation()
    public List<PackageInfo> getInstalledPackages(int flags) {
        System.out.println("Stub" + " Function : getInstalledPackages");
        return null;
    }

}
