/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.pm;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.AttributeSet;
import android.util.Config;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.android.internal.util.XmlUtils;

/**
 * Package archive parsing
 *
 * {@hide}
 */
public class PackageParser {
	private ParsePackageItemArgs mParseInstrumentationArgs;

	/** If set to true, we will only allow package files that exactly match
     *  the DTD.  Otherwise, we try to get as much from the package as we
     *  can without failing.  This should normally be set to false, to
     *  support extensions to the DTD in future versions. */
    private static final boolean RIGID_PARSER = false;


	private static final String TAG = "PackageParser";

	public static class NewPermissionInfo {
		public final String name;
		public final int sdkVersion;
		public final int fileVersion;

		public NewPermissionInfo(String name, int sdkVersion, int fileVersion) {
			this.name = name;
			this.sdkVersion = sdkVersion;
			this.fileVersion = fileVersion;
		}
	}

	private String mArchiveSourcePath;
	private String[] mSeparateProcesses;

	private int mParseError = PackageManager.INSTALL_SUCCEEDED;

	private static boolean sCompatibilityModeEnabled = true;
	private static final int PARSE_DEFAULT_INSTALL_LOCATION = PackageInfo.INSTALL_LOCATION_UNSPECIFIED;

	static class ParsePackageItemArgs {
		final Package owner;
		final String[] outError;
		final int nameRes;
		final int labelRes;
		final int iconRes;
		final int logoRes;

		String tag;
		TypedArray sa;

		ParsePackageItemArgs(Package _owner, String[] _outError, int _nameRes,
				int _labelRes, int _iconRes, int _logoRes) {
			owner = _owner;
			outError = _outError;
			nameRes = _nameRes;
			labelRes = _labelRes;
			iconRes = _iconRes;
			logoRes = _logoRes;
		}
	}

	static class ParseComponentArgs extends ParsePackageItemArgs {
		final String[] sepProcesses;
		final int processRes;
		final int descriptionRes;
		final int enabledRes;
		int flags;

		ParseComponentArgs(Package _owner, String[] _outError, int _nameRes,
				int _labelRes, int _iconRes, int _logoRes,
				String[] _sepProcesses, int _processRes, int _descriptionRes,
				int _enabledRes) {
			super(_owner, _outError, _nameRes, _labelRes, _iconRes, _logoRes);
			sepProcesses = _sepProcesses;
			processRes = _processRes;
			descriptionRes = _descriptionRes;
			enabledRes = _enabledRes;
		}
	}

	/* Light weight package info.
	 * @hide
	 */
	public static class PackageLite {
		public String packageName;
		public int installLocation;
		public String mScanPath;

		public PackageLite(String packageName, int installLocation) {
			this.packageName = packageName;
			this.installLocation = installLocation;
		}
	}

	private ParseComponentArgs mParseActivityArgs;
	private ParseComponentArgs mParseProviderArgs;
	private ParseComponentArgs mParseServiceArgs;
    private ParseComponentArgs mParseActivityAliasArgs;

	public PackageParser(String archiveSourcePath) {
		mArchiveSourcePath = archiveSourcePath;
	}

	public void setSeparateProcesses(String[] procs) {
		mSeparateProcesses = procs;
	}

	public final static int PARSE_IS_SYSTEM = 1 << 0;
	public final static int PARSE_CHATTY = 1 << 1;
	public final static int PARSE_MUST_BE_APK = 1 << 2;
	public final static int PARSE_IGNORE_PROCESSES = 1 << 3;
	public final static int PARSE_FORWARD_LOCK = 1 << 4;
	public final static int PARSE_ON_SDCARD = 1 << 5;
	public final static int PARSE_IS_SYSTEM_DIR = 1 << 6;
	private static final String ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android";

	public int getParseError() {
		return mParseError;
	}

	public Package parsePackage(String sourceFile, String destCodePath,
			DisplayMetrics metrics, int flags) {
		mParseError = PackageManager.INSTALL_SUCCEEDED;

		mArchiveSourcePath = sourceFile;

		XmlResourceParser parser = null;
		AssetManager assmgr = null;
		boolean assetError = true;
		try {
			assmgr = new AssetManager();
			int cookie = assmgr.addAssetPath(mArchiveSourcePath);
//			System.out.println("mASP: " + mArchiveSourcePath);
			if (cookie != 0) {
				parser = assmgr.openXmlResourceParser(cookie,
						"AndroidManifest.xml");
				assetError = false;
			} else {
				Log.w(TAG, "Failed adding asset path:" + mArchiveSourcePath);
			}
			assetError = false;
		} catch (Exception e) {
			Log.w(TAG, "Unable to read AndroidManifest.xml of "
					+ mArchiveSourcePath, e);
		}
		if (assetError) {
			mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST;
			return null;
		}
		String[] errorText = new String[1];
		Package pkg = null;
		try {
			Resources res = new Resources(assmgr, metrics, null);
			pkg = parsePackage(res, parser, flags, errorText);
			if (pkg == null)
				System.err.println(errorText[0]);
//			else
//				System.err.println("pkg != null");
			pkg.mResource = res;
		} catch (Exception e) {
			mParseError = PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION;
		}

		if (pkg == null) {
			parser.close();
			//			assmgr.close();
			if (mParseError == PackageManager.INSTALL_SUCCEEDED) {
				mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
			}
			return null;
		}

		parser.close();
		//		assmgr.close();

		// Set code and resource paths
		pkg.mPath = destCodePath;
		pkg.mScanPath = mArchiveSourcePath;
		//pkg.applicationInfo.sourceDir = destCodePath;
		//pkg.applicationInfo.publicSourceDir = destRes;

		return pkg;
	}

	private static String validateName(String name, boolean requiresSeparator) {
		return null;
		//		final int N = name.length();
		//		boolean hasSep = false;
		//		boolean front = true;
		//		for (int i = 0; i < N; i++) {
		//			final char c = name.charAt(i);
		//			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
		//				front = false;
		//				continue;
		//			}
		//			if (!front) {
		//				if ((c >= '0' && c <= '9') || c == '_') {
		//					continue;
		//				}
		//			}
		//			if (c == '.') {
		//				hasSep = true;
		//				front = true;
		//				continue;
		//			}
		//			return "bad character '" + c + "'";
		//		}
		//		return hasSep || !requiresSeparator ? null
		//				: "must have at least one '.' separator";
	}

	private static String parsePackageName(XmlPullParser parser,
			AttributeSet attrs, int flags, String[] outError)
			throws IOException {
		int type = -1;
		try {
			while ((type = parser.next()) != XmlPullParser.START_TAG
					&& type != XmlPullParser.END_DOCUMENT) {
				;
			}
			//			if (type == XmlPullParser.START_TAG)
			//				System.out.println("find start tag<" + parser.getName() + ">");
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}

		if (type != XmlPullParser.START_TAG) {
			outError[0] = "No start tag found";
			return null;
		}
		if ((flags & PARSE_CHATTY) != 0 && Config.LOGV)
			Log.v(TAG, "Root element name: '" + parser.getName() + "'");
		if (!parser.getName().equals("manifest")) {
			outError[0] = "No <manifest> tag";
			return null;
		}
		//		System.err.println(((XmlPullParser) attrs).getName());
		String pkgName = attrs.getAttributeValue(null, "package");
		if (pkgName == null || pkgName.length() == 0) {
			outError[0] = "<manifest> does not specify package";
			return null;
		}
		String nameError = validateName(pkgName, true);
		if (nameError != null && !"android".equals(pkgName)) {
			outError[0] = "<manifest> specifies bad package name \"" + pkgName
					+ "\": " + nameError;
			return null;
		}

		return pkgName.intern();
	}

	private Package parsePackage(Resources res, XmlResourceParser parser,
			int flags, String[] outError) throws IOException {
	    /**
	     * @Mayloon update
	     * Initialize mParseInstrumentaionArgs
	     */
		mParseInstrumentationArgs = null;
		AttributeSet attrs = parser;
		mParseActivityArgs = null;

		String pkgName = parsePackageName(parser, attrs, flags, outError);
		if (pkgName == null) {
			mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
			return null;
		}
//		System.out.println(TAG + "packageName: " + pkgName);

		int type = -1;
		boolean foundApp = false;
		final Package pkg = new Package(pkgName);
		pkg.installLocation = PARSE_DEFAULT_INSTALL_LOCATION;
		pkg.applicationInfo.installLocation = pkg.installLocation;

		int outerDepth = parser.getDepth();
		try {
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
					&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
				if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
					continue;
				}

				String tagName = parser.getName();
//				System.out.println("DEBUG PackageParser>>>" + "get Tag: "
//						+ tagName);
				if (tagName.equals("application")) {
					if (foundApp) {
						outError[0] = "<manifest> has more than one <application>";
						mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
						return null;
					}

					foundApp = true;
					if (!parseApplication(pkg, res, parser, attrs, flags,
							outError)) {
						return null;
					}
                } else if (tagName.equals("instrumentation")) {
                    if (parseInstrumentation(pkg, res, parser, attrs, outError) == null) {
                        return null;
                    }

                }

			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		if (foundApp == false) {
			outError[0] = "<manifest> does not contain an <application> or <instrumentation>";
			mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY;
			return null;
		}
		return pkg;
	}

	private static String buildClassName(String pkg, String clsSeq,
			String[] outError) {
		if (clsSeq == null || clsSeq.length() <= 0) {
			outError[0] = "Empty class name in package " + pkg;
			return null;
		}
		String cls = clsSeq.toString();
		char c = cls.charAt(0);
		if (c == '.') {
			return (pkg + cls).intern();
		}
		if (cls.indexOf('.') < 0) {
			StringBuilder b = new StringBuilder(pkg);
			b.append('.');
			b.append(cls);
			return b.toString().intern();
		}
		if (c >= 'a' && c <= 'z') {
			return cls.intern();
		}
		outError[0] = "Bad class name " + cls + " in package " + pkg;
		return null;
	}

	private static String buildCompoundName(String pkg, CharSequence procSeq,
			String type, String[] outError) {
		String proc = procSeq.toString();
		char c = proc.charAt(0);
		if (pkg != null && c == ':') {
			if (proc.length() < 2) {
				outError[0] = "Bad " + type + " name " + proc + " in package "
						+ pkg + ": must be at least two characters";
				return null;
			}
			String subName = proc.substring(1);
			String nameError = validateName(subName, false);
			if (nameError != null) {
				outError[0] = "Invalid " + type + " name " + proc
						+ " in package " + pkg + ": " + nameError;
				return null;
			}
			return (pkg + proc).intern();
		}
		String nameError = validateName(proc, true);
		if (nameError != null && !"system".equals(proc)) {
			outError[0] = "Invalid " + type + " name " + proc + " in package "
					+ pkg + ": " + nameError;
			return null;
		}
		return proc.intern();
	}

	private static String buildProcessName(String pkg, String defProc,
			CharSequence procSeq, int flags, String[] separateProcesses,
			String[] outError) {
		if ((flags & PARSE_IGNORE_PROCESSES) != 0 && !"system".equals(procSeq)) {
			return defProc != null ? defProc : pkg;
		}
		if (separateProcesses != null) {
			for (int i = separateProcesses.length - 1; i >= 0; i--) {
				String sp = separateProcesses[i];
				if (sp.equals(pkg) || sp.equals(defProc) || sp.equals(procSeq)) {
					return pkg;
				}
			}
		}
		if (procSeq == null || procSeq.length() <= 0) {
			return defProc;
		}
		return buildCompoundName(pkg, procSeq, "process", outError);
	}

    private Instrumentation parseInstrumentation(Package owner, Resources res,
            XmlPullParser parser, AttributeSet attrs, String[] outError)
            throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(attrs,
                com.android.internal.R.styleable.AndroidManifestInstrumentation);

        if (mParseInstrumentationArgs == null) {
            mParseInstrumentationArgs = new ParsePackageItemArgs(owner, outError,
                    com.android.internal.R.styleable.AndroidManifestInstrumentation_name,
                    com.android.internal.R.styleable.AndroidManifestInstrumentation_label,
                    com.android.internal.R.styleable.AndroidManifestInstrumentation_icon, 0);
            mParseInstrumentationArgs.tag = "<instrumentation>";
        }

        mParseInstrumentationArgs.sa = sa;

        Instrumentation a = new Instrumentation(mParseInstrumentationArgs,
                new InstrumentationInfo());
        if (outError[0] != null) {
            sa.recycle();
            mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }

        String str;
        // Note: don't allow this value to be a reference to a resource
        // that may change.
        str = sa.getNonResourceString(
                com.android.internal.R.styleable.AndroidManifestInstrumentation_targetPackage);
        a.info.targetPackage = str != null ? str.intern() : null;

        a.info.handleProfiling = sa.getBoolean(
                com.android.internal.R.styleable.AndroidManifestInstrumentation_handleProfiling,
                false);

        a.info.functionalTest = sa.getBoolean(
                com.android.internal.R.styleable.AndroidManifestInstrumentation_functionalTest,
                false);

        sa.recycle();

        if (a.info.targetPackage == null) {
            outError[0] = "<instrumentation> does not specify targetPackage";
            mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }

        if (!parseAllMetaData(res, parser, attrs, "<instrumentation>", a,
                outError)) {
            mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }

        owner.instrumentation.add(a);

        return a;
    }


	private boolean parseApplication(Package owner, Resources res,
			XmlPullParser parser, AttributeSet attrs, int flags,
			String[] outError) throws IOException {
        final ApplicationInfo ai = owner.applicationInfo;
        final String pkgName = owner.applicationInfo.packageName;

        TypedArray sa = res.obtainAttributes(attrs,
                com.android.internal.R.styleable.AndroidManifestApplication);

        TypedValue v = sa.peekValue(
                com.android.internal.R.styleable.AndroidManifestApplication_label);

        String name = sa.getNonConfigurationString(
                com.android.internal.R.styleable.AndroidManifestApplication_name, 0);
        if (name != null) {
            ai.className = buildClassName(pkgName, name, outError);
            if (ai.className == null) {
                sa.recycle();
                mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
        }

        ai.icon = sa.getResourceId(
                com.android.internal.R.styleable.AndroidManifestApplication_icon, 0);
        ai.theme = sa.getResourceId(
                com.android.internal.R.styleable.AndroidManifestApplication_theme, 0);
        ai.descriptionRes = sa.getResourceId(
                com.android.internal.R.styleable.AndroidManifestApplication_description, 0);

        if (v != null) {
            ai.labelRes=v.resourceId;
            ai.nonLocalizedLabel = v.coerceToString();
        }

		if (outError[0] != null) {
			mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
			return false;
		}
		final int innerDepth = parser.getDepth();

		int type = -1;
		try {
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
					&& (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth)) {
				if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
					continue;
				}

				String tagName = parser.getName();
				if (tagName.equals("activity")) {
					Activity a = parseActivity(owner, res, parser, attrs,
							flags, outError, false);
					if (a == null) {
						mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
						return false;
					}
					owner.activities.add(a);
				}
				//parse broadcast receiver
				else if (tagName.equals("receiver")) {
	                Activity a = parseActivity(owner, res, parser, attrs, flags, outError, true);
	                if (a == null) {
	                    mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
	                    return false;
	                }

	                owner.receivers.add(a);
				}
				//parse provider
				else if(tagName.equals("provider")){
					//TODO: start parsing...
	                Provider p = parseProvider(owner, res, parser, attrs, flags, outError);
	                if (p == null) {
	                    mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
	                    return false;
	                }
	                owner.providers.add(p);
	                Log.i(TAG, "Provider parse is:"+p.className+":"+p.getComponentShortName());
				}
				else if (tagName.equals("service")) {
					Service a = parseService(owner, res, parser, attrs,
							flags, outError, false);
					if (a == null) {
						mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
						return false;
					}

//					System.out.print("service::: " + 1+"     ");
					owner.services.add(a);
				}
                else if (tagName.equals("activity-alias")) {
                    Activity a = parseActivityAlias(owner, res, parser, attrs, flags, outError);
                    if (a == null) {
                        mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }

                    owner.activities.add(a);
                }

			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return true;
	}

    private Activity parseActivityAlias(Package owner, Resources res,
            XmlPullParser parser, AttributeSet attrs, int flags, String[] outError)
            throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(attrs,
                com.android.internal.R.styleable.AndroidManifestActivityAlias);

        String targetActivity = sa.getNonConfigurationString(
                com.android.internal.R.styleable.AndroidManifestActivityAlias_targetActivity, 0);
        if (targetActivity == null) {
            outError[0] = "<activity-alias> does not specify android:targetActivity";
            sa.recycle();
            return null;
        }

        targetActivity = buildClassName(owner.applicationInfo.packageName,
                targetActivity, outError);
        if (targetActivity == null) {
            sa.recycle();
            return null;
        }

        if (mParseActivityAliasArgs == null) {
            mParseActivityAliasArgs = new ParseComponentArgs(owner, outError,
                    com.android.internal.R.styleable.AndroidManifestActivityAlias_name,
                    com.android.internal.R.styleable.AndroidManifestActivityAlias_label,
                    com.android.internal.R.styleable.AndroidManifestActivityAlias_icon, 0,
                    mSeparateProcesses,
                    0,
                    com.android.internal.R.styleable.AndroidManifestActivityAlias_description,
                    com.android.internal.R.styleable.AndroidManifestActivityAlias_enabled);
            mParseActivityAliasArgs.tag = "<activity-alias>";
        }

        mParseActivityAliasArgs.sa = sa;
        mParseActivityAliasArgs.flags = flags;

        Activity target = null;

        final int NA = owner.activities.size();
        for (int i=0; i<NA; i++) {
            Activity t = owner.activities.get(i);
            if (targetActivity.equals(t.info.name)) {
                target = t;
                break;
            }
        }

        if (target == null) {
            outError[0] = "<activity-alias> target activity " + targetActivity
                    + " not found in manifest";
            sa.recycle();
            return null;
        }

        ActivityInfo info = new ActivityInfo();
        info.targetActivity = targetActivity;
        info.configChanges = target.info.configChanges;
        info.flags = target.info.flags;
        info.icon = target.info.icon;
        info.logo = target.info.logo;
        info.labelRes = target.info.labelRes;
        info.nonLocalizedLabel = target.info.nonLocalizedLabel;
        info.launchMode = target.info.launchMode;
        info.processName = target.info.processName;
        if (info.descriptionRes == 0) {
            info.descriptionRes = target.info.descriptionRes;
        }
        info.screenOrientation = target.info.screenOrientation;
        info.taskAffinity = target.info.taskAffinity;
        info.theme = target.info.theme;

        Activity a = new Activity(mParseActivityAliasArgs, info);
        if (outError[0] != null) {
            sa.recycle();
            return null;
        }

        final boolean setExported = sa.hasValue(
                com.android.internal.R.styleable.AndroidManifestActivityAlias_exported);
        if (setExported) {
            a.info.exported = sa.getBoolean(
                    com.android.internal.R.styleable.AndroidManifestActivityAlias_exported, false);
        }

        String str;
        str = sa.getNonConfigurationString(
                com.android.internal.R.styleable.AndroidManifestActivityAlias_permission, 0);
        if (str != null) {
            a.info.permission = str.length() > 0 ? str.toString().intern() : null;
        }

        sa.recycle();

        if (outError[0] != null) {
            return null;
        }

        int outerDepth = parser.getDepth();
        int type;
        while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
               && (type != XmlPullParser.END_TAG
                       || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            if (parser.getName().equals("intent-filter")) {
                ActivityIntentInfo intent = new ActivityIntentInfo(a);
                if (!parseIntent(res, parser, attrs, flags, intent, outError, true)) {
                    return null;
                }
                if (intent.countActions() == 0) {
                    Log.w(TAG, "No actions in intent filter at "
                            + mArchiveSourcePath + " "
                            + parser.getPositionDescription());
                } else {
                    a.intents.add(intent);
                }
            } else if (parser.getName().equals("meta-data")) {
                if ((a.metaData=parseMetaData(res, parser, attrs, a.metaData,
                        outError)) == null) {
                    return null;
                }
            } else {
                if (!RIGID_PARSER) {
                    Log.w(TAG, "Unknown element under <activity-alias>: " + parser.getName()
                            + " at " + mArchiveSourcePath + " "
                            + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                    continue;
                }
                outError[0] = "Bad element under <activity-alias>: " + parser.getName();
                return null;
            }
        }

        if (!setExported) {
            a.info.exported = a.intents.size() > 0;
        }

        return a;
    }

	private boolean parsePackageItemInfo(Package owner,
			PackageItemInfo outInfo, String[] outError, String tag,
			TypedArray sa, int nameRes, int labelRes, int iconRes, int logoRes) {
		String name = sa.getNonConfigurationString(nameRes, 0);
		if (name == null) {
			outError[0] = tag + " does not specify android:name";
			return false;
		}

		outInfo.name = buildClassName(owner.applicationInfo.packageName, name,
				outError);
		if (outInfo.name == null) {
			return false;
		}

		int iconVal = sa.getResourceId(iconRes, 0);
		if (iconVal != 0) {
			outInfo.icon = iconVal;
			outInfo.nonLocalizedLabel = null;
		}

		TypedValue v = sa.peekValue(labelRes);
		if (v != null && (outInfo.labelRes = v.resourceId) == 0) {
			outInfo.nonLocalizedLabel = v.coerceToString();
		}

		outInfo.packageName = owner.packageName;

		return true;
	}
	   private Provider parseProvider(Package owner, Resources res,
	            XmlPullParser parser, AttributeSet attrs, int flags, String[] outError)
	            throws XmlPullParserException, IOException {
	        TypedArray sa = res.obtainAttributes(attrs,
	                com.android.internal.R.styleable.AndroidManifestProvider);

	        if (mParseProviderArgs == null) {
	            mParseProviderArgs = new ParseComponentArgs(owner, outError,
	                    com.android.internal.R.styleable.AndroidManifestProvider_name,
	                    com.android.internal.R.styleable.AndroidManifestProvider_label,
	                    com.android.internal.R.styleable.AndroidManifestProvider_icon, 0,
	                    mSeparateProcesses,
	                    com.android.internal.R.styleable.AndroidManifestProvider_process,
	                    com.android.internal.R.styleable.AndroidManifestProvider_description,
	                    com.android.internal.R.styleable.AndroidManifestProvider_enabled);
	            mParseProviderArgs.tag = "<provider>";
	        }

	        mParseProviderArgs.sa = sa;
	        mParseProviderArgs.flags = flags;

	        Provider p = new Provider(mParseProviderArgs, new ProviderInfo());
	        if (outError[0] != null) {
	            sa.recycle();
	            return null;
	        }

	        p.info.exported = sa.getBoolean(
	                com.android.internal.R.styleable.AndroidManifestProvider_exported, true);

	        String cpname = sa.getNonConfigurationString(
	                com.android.internal.R.styleable.AndroidManifestProvider_authorities, 0);

	        p.info.isSyncable = sa.getBoolean(
	                com.android.internal.R.styleable.AndroidManifestProvider_syncable,
	                false);

	        String permission = sa.getNonConfigurationString(
	                com.android.internal.R.styleable.AndroidManifestProvider_permission, 0);
	        String str = sa.getNonConfigurationString(
	                com.android.internal.R.styleable.AndroidManifestProvider_readPermission, 0);
	        if (str == null) {
	            str = permission;
	        }
	        if (str == null) {
	            p.info.readPermission = owner.applicationInfo.permission;
	        } else {
	            p.info.readPermission =
	                str.length() > 0 ? str.toString().intern() : null;
	        }
	        str = sa.getNonConfigurationString(
	                com.android.internal.R.styleable.AndroidManifestProvider_writePermission, 0);
	        if (str == null) {
	            str = permission;
	        }
	        if (str == null) {
	            p.info.writePermission = owner.applicationInfo.permission;
	        } else {
	            p.info.writePermission =
	                str.length() > 0 ? str.toString().intern() : null;
	        }

	        p.info.grantUriPermissions = sa.getBoolean(
	                com.android.internal.R.styleable.AndroidManifestProvider_grantUriPermissions,
	                false);

	        p.info.multiprocess = sa.getBoolean(
	                com.android.internal.R.styleable.AndroidManifestProvider_multiprocess,
	                false);

	        p.info.initOrder = sa.getInt(
	                com.android.internal.R.styleable.AndroidManifestProvider_initOrder,
	                0);

	        sa.recycle();

	        if ((owner.applicationInfo.flags&ApplicationInfo.FLAG_CANT_SAVE_STATE) != 0) {
	            // A heavy-weight application can not have providers in its main process
	            // We can do direct compare because we intern all strings.
	            if (p.info.processName == owner.packageName) {
	                outError[0] = "Heavy-weight applications can not have providers in main process";
	                return null;
	            }
	        }

	        if (cpname == null) {
	            outError[0] = "<provider> does not incude authorities attribute";
	            return null;
	        }
	        p.info.authority = cpname.intern();

	        //TODO: i ignore its child node currently
	        if (!parseProviderTags(res, parser, attrs, p, outError)) {
	            return null;
	        }

	        return p;
	    }

    private boolean parseAllMetaData(Resources res,
                XmlPullParser parser, AttributeSet attrs, String tag,
                Component outInfo, String[] outError)
                throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                   && (type != XmlPullParser.END_TAG
                           || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            if (parser.getName().equals("meta-data")) {
                if ((outInfo.metaData = parseMetaData(res, parser, attrs,
                            outInfo.metaData, outError)) == null) {
                    return false;
                }
            } else {
                if (!RIGID_PARSER) {
                    Log.w(TAG, "Unknown element under " + tag + ": "
                                + parser.getName() + " at " + mArchiveSourcePath + " "
                                + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                    continue;
                }
                outError[0] = "Bad element under " + tag + ": "
                        + parser.getName();
                return false;
            }
        }
        return true;
    }


	   private Bundle parseMetaData(Resources res,
	            XmlPullParser parser, AttributeSet attrs,
	            Bundle data, String[] outError)
	            throws XmlPullParserException, IOException {

	        TypedArray sa = res.obtainAttributes(attrs,
	                com.android.internal.R.styleable.AndroidManifestMetaData);

	        if (data == null) {
	            data = new Bundle();
	        }

	        String name = sa.getNonConfigurationString(
	                com.android.internal.R.styleable.AndroidManifestMetaData_name, 0);
	        if (name == null) {
	            outError[0] = "<meta-data> requires an android:name attribute";
	            sa.recycle();
	            return null;
	        }

	        name = name.intern();

	        TypedValue v = sa.peekValue(
	                com.android.internal.R.styleable.AndroidManifestMetaData_resource);
	        if (v != null && v.resourceId != 0) {
	            //Log.i(TAG, "Meta data ref " + name + ": " + v);
	            data.putInt(name, v.resourceId);
	        } else {
	            v = sa.peekValue(
	                    com.android.internal.R.styleable.AndroidManifestMetaData_value);
	            //Log.i(TAG, "Meta data " + name + ": " + v);
	            if (v != null) {
	                if (v.type == TypedValue.TYPE_STRING) {
	                    CharSequence cs = v.coerceToString();
	                    data.putString(name, cs != null ? cs.toString().intern() : null);
	                } else if (v.type == TypedValue.TYPE_INT_BOOLEAN) {
	                    data.putBoolean(name, v.data != 0);
	                } else if (v.type >= TypedValue.TYPE_FIRST_INT
	                        && v.type <= TypedValue.TYPE_LAST_INT) {
	                    data.putInt(name, v.data);
	                } else if (v.type == TypedValue.TYPE_FLOAT) {
	                    data.putFloat(name, v.getFloat());
	                } else {
	                        outError[0] = "<meta-data> only supports string, integer, float, color, boolean, and resource reference types";
	                        data = null;

	                }
	            } else {
	                outError[0] = "<meta-data> requires an android:value or android:resource attribute";
	                data = null;
	            }
	        }

	        sa.recycle();

	        XmlUtils.skipCurrentTag(parser);

	        return data;
	    }
	    private boolean parseProviderTags(Resources res,
	            XmlPullParser parser, AttributeSet attrs,
	            Provider outInfo, String[] outError)
	            throws XmlPullParserException, IOException {

	        int outerDepth = parser.getDepth();
	        int type;
	        while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
	               && (type != XmlPullParser.END_TAG
	                       || parser.getDepth() > outerDepth)) {
	            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
	                continue;
	            }

	            if (parser.getName().equals("meta-data")) {
	                if ((outInfo.metaData=parseMetaData(res, parser, attrs,
	                        outInfo.metaData, outError)) == null) {
	                    return false;
	                }

	            } else if (parser.getName().equals("grant-uri-permission")) {
	                TypedArray sa = res.obtainAttributes(attrs,
	                        com.android.internal.R.styleable.AndroidManifestGrantUriPermission);

	                PatternMatcher pa = null;

	                String str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestGrantUriPermission_path, 0);
	                if (str != null) {
	                    pa = new PatternMatcher(str, PatternMatcher.PATTERN_LITERAL);
	                }

	                str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestGrantUriPermission_pathPrefix, 0);
	                if (str != null) {
	                    pa = new PatternMatcher(str, PatternMatcher.PATTERN_PREFIX);
	                }

	                str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestGrantUriPermission_pathPattern, 0);
	                if (str != null) {
	                    pa = new PatternMatcher(str, PatternMatcher.PATTERN_SIMPLE_GLOB);
	                }

	                sa.recycle();

	                if (pa != null) {
	                    if (outInfo.info.uriPermissionPatterns == null) {
	                        outInfo.info.uriPermissionPatterns = new PatternMatcher[1];
	                        outInfo.info.uriPermissionPatterns[0] = pa;
	                    } else {
	                        final int N = outInfo.info.uriPermissionPatterns.length;
	                        PatternMatcher[] newp = new PatternMatcher[N+1];
	                        System.arraycopy(outInfo.info.uriPermissionPatterns, 0, newp, 0, N);
	                        newp[N] = pa;
	                        outInfo.info.uriPermissionPatterns = newp;
	                    }
	                    outInfo.info.grantUriPermissions = true;
	                } else {
	                    outError[0] = "No path, pathPrefix, or pathPattern for <path-permission>";
	                    return false;
	                }
	                XmlUtils.skipCurrentTag(parser);

	            } else {
	                outError[0] = "Bad element under <provider>: "
	                    + parser.getName();
	                return false;
	            }
	        }
	        return true;

	    }
	private Activity parseActivity(Package owner, Resources res,
			XmlPullParser parser, AttributeSet attrs, int flags,
			String[] outError, boolean receiver) throws IOException {
		TypedArray sa = res.obtainAttributes(attrs,
				com.android.internal.R.styleable.AndroidManifestActivity);

		if (mParseActivityArgs == null) {
			mParseActivityArgs = new ParseComponentArgs(
					owner,
					outError,
					com.android.internal.R.styleable.AndroidManifestActivity_name,
					com.android.internal.R.styleable.AndroidManifestActivity_label,
					com.android.internal.R.styleable.AndroidManifestActivity_icon,
					0,
					mSeparateProcesses,
					com.android.internal.R.styleable.AndroidManifestActivity_process,
					com.android.internal.R.styleable.AndroidManifestActivity_description,
					com.android.internal.R.styleable.AndroidManifestActivity_enabled);
		}

		mParseActivityArgs.tag = receiver ? "<receiver>" : "<activity>";
		mParseActivityArgs.sa = sa;
		mParseActivityArgs.flags = flags;

		Activity a = new Activity(mParseActivityArgs, new ActivityInfo());
		//		System.out.println("creating activity: " + a.className);
		if (outError[0] != null) {
			System.out.println(TAG + outError[0]);
			sa.recycle();
			return null;
		}

		a.info.exported = true;

        a.info.theme = sa.getResourceId(
                com.android.internal.R.styleable.AndroidManifestActivity_theme, 0);
		sa.recycle();

		if (outError[0] != null) {
			return null;
		}
		int outerDepth = parser.getDepth();
		int type = -1;
		try {
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
					&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
				if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
					continue;
				}

				if (parser.getName().equals("intent-filter")) {
					ActivityIntentInfo intent = new ActivityIntentInfo(a);
					if (!parseIntent(res, parser, attrs, flags, intent,
							outError, !receiver)) {
						return null;
					}
					if (intent.countActions() == 0) {
						Log.w(TAG,
								"No actions in intent filter at "
										+ mArchiveSourcePath + " "
										+ parser.getPositionDescription());
					} else {
						a.intents.add(intent);
					}
                } else if (parser.getName().equals("meta-data")) {
                    if ((a.metaData = parseMetaData(res, parser, attrs, a.metaData,
                            outError)) == null) {
                        return null;
                    }
                }
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return a;
	}

	private Service parseService(Package owner, Resources res,
			XmlPullParser parser, AttributeSet attrs, int flags,
			String[] outError, boolean receiver) throws IOException {
		TypedArray sa = res.obtainAttributes(attrs,
				com.android.internal.R.styleable.AndroidManifestService);

		if (mParseServiceArgs == null) {
			mParseServiceArgs = new ParseComponentArgs(
					owner,
					outError,
					com.android.internal.R.styleable.AndroidManifestService_name,
					com.android.internal.R.styleable.AndroidManifestService_label,
					com.android.internal.R.styleable.AndroidManifestService_icon,
					0,
					mSeparateProcesses,
					com.android.internal.R.styleable.AndroidManifestService_process,
					com.android.internal.R.styleable.AndroidManifestService_description,
					com.android.internal.R.styleable.AndroidManifestService_enabled);
			mParseServiceArgs.tag = "<services>";
		}

//		mParseServiceArgs.tag = receiver ? "<receiver>" : "<Service>";
		mParseServiceArgs.sa = sa;
		mParseServiceArgs.flags = flags;

		Service s = new Service(mParseServiceArgs, new ServiceInfo());
		//		System.out.println("creating Service: " + a.className);
		if (outError[0] != null) {
			System.out.println(TAG + outError[0]);
			sa.recycle();
			return null;
		}

		s.info.exported = true;
		sa.recycle();
		if (s.info.processName == null)
			s.info.processName = owner.packageName;

		if (outError[0] != null) {
			return null;
		}
		int outerDepth = parser.getDepth();
		int type = -1;
		try {
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
					&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
				if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
					continue;
				}

				if (parser.getName().equals("intent-filter")) {
					ServiceIntentInfo intent = new ServiceIntentInfo(s);
					if (!parseIntent(res, parser, attrs, flags, intent,
							outError, !receiver)) {
						return null;
					}
					if (intent.countActions() == 0) {
						Log.w(TAG,
								"No actions in intent filter at "
										+ mArchiveSourcePath + " "
										+ parser.getPositionDescription());
					} else {
						s.intents.add(intent);
					}
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return s;
	}

	private boolean parseIntent(Resources res, XmlPullParser parser,
			AttributeSet attrs, int flags, IntentInfo outInfo,
			String[] outError, boolean isActivity) throws IOException {
		/**/
		TypedArray sa = res.obtainAttributes(attrs,
				com.android.internal.R.styleable.AndroidManifestIntentFilter);

		int priority = sa
				.getInt(com.android.internal.R.styleable.AndroidManifestIntentFilter_priority,
						0);
		if (priority > 0 && isActivity && (flags & PARSE_IS_SYSTEM) == 0) {
			System.out.println(TAG
					+ "Activity with priority > 0, forcing to 0 at "
					+ mArchiveSourcePath);
			priority = 0;
		}
		outInfo.setPriority(priority);

		TypedValue v = sa
				.peekValue(com.android.internal.R.styleable.AndroidManifestIntentFilter_label);
		if (v != null && (outInfo.labelRes = v.resourceId) == 0) {
			outInfo.nonLocalizedLabel = v.coerceToString();
		}

		outInfo.icon = sa
				.getResourceId(
						com.android.internal.R.styleable.AndroidManifestIntentFilter_icon,
						0);
		sa.recycle();
		/**/
		int outerDepth = parser.getDepth();
		int type = -1;
		try {
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
					&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
				if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
					continue;
				}

				String nodeName = parser.getName();
				if (nodeName.equals("action")) {
					String value = attrs.getAttributeValue(ANDROID_RESOURCES,
							"name");
					if (value == null || value == "") {
						outError[0] = "No value supplied for <android:name>";
						return false;
					}
					XmlUtils.skipCurrentTag(parser);

					outInfo.addAction(value);
				} else if (nodeName.equals("category")) {
					String value = attrs.getAttributeValue(ANDROID_RESOURCES,
							"name");
					if (value == null || value == "") {
						outError[0] = "No value supplied for <android:name>";
						return false;
					}
					XmlUtils.skipCurrentTag(parser);

					outInfo.addCategory(value);
				}else if(nodeName.equals("data")){

	                sa = res.obtainAttributes(attrs,
	                        com.android.internal.R.styleable.AndroidManifestData);

	                String str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestData_mimeType, 0);
	                if (str != null) {
	                    try {
	                        outInfo.addDataType(str);
	                    } catch (IntentFilter.MalformedMimeTypeException e) {
	                        outError[0] = e.toString();
	                        sa.recycle();
	                        return false;
	                    }
	                }

	                str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestData_scheme, 0);
	                if (str != null) {
	                    outInfo.addDataScheme(str);
	                }

	                String host = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestData_host, 0);
	                String port = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestData_port, 0);
	                if (host != null) {
	                    outInfo.addDataAuthority(host, port);
	                }

	                str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestData_path, 0);
	                if (str != null) {
	                    outInfo.addDataPath(str, PatternMatcher.PATTERN_LITERAL);
	                }

	                str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestData_pathPrefix, 0);
	                if (str != null) {
	                    outInfo.addDataPath(str, PatternMatcher.PATTERN_PREFIX);
	                }

	                str = sa.getNonConfigurationString(
	                        com.android.internal.R.styleable.AndroidManifestData_pathPattern, 0);
	                if (str != null) {
	                    outInfo.addDataPath(str, PatternMatcher.PATTERN_SIMPLE_GLOB);
	                }

	                sa.recycle();
	                XmlUtils.skipCurrentTag(parser);

				}
				else {
					outError[0] = "Bad element under <intent-filter>:"+nodeName;
					return false;
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		outInfo.hasDefault = outInfo.hasCategory(Intent.CATEGORY_DEFAULT);
		return true;
	}

	public final static class Package {
		// TODO remove in the future
		public Resources mResource;

		public String packageName;

		// For now we only support one application per package.
		public final ApplicationInfo applicationInfo = new ApplicationInfo();

		public final ArrayList<Activity> activities = new ArrayList<Activity>(0);
		public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
		public final ArrayList<Provider> providers = new ArrayList<Provider>(0);
		public final ArrayList<Service> services = new ArrayList<Service>(0);
		public final ArrayList<Instrumentation> instrumentation = new ArrayList<Instrumentation>(0);

		public ArrayList<String> protectedBroadcasts;

		public ArrayList<String> usesLibraries = null;
		public ArrayList<String> usesOptionalLibraries = null;
		public String[] usesLibraryFiles = null;

		public ArrayList<String> mOriginalPackages = null;
		public String mRealPackage = null;
		public ArrayList<String> mAdoptPermissions = null;

		// We store the application meta-data independently to avoid multiple unwanted references
		public Bundle mAppMetaData = null;

		// If this is a 3rd party app, this is the path of the zip file.
		public String mPath;

		// The version code declared for this package.
		public int mVersionCode;

		// The version name declared for this package.
		public String mVersionName;

		// The shared user id that this package wants to use.
		public String mSharedUserId;

		// The shared user label that this package wants to use.
		public int mSharedUserLabel;

		// For use by package manager service for quick lookup of
		// preferred up order.
		public int mPreferredOrder = 0;

		// For use by the package manager to keep track of the path to the
		// file an app came from.
		public String mScanPath;

		// For use by package manager to keep track of where it has done dexopt.
		public boolean mDidDexOpt;

		// User set enabled state.
		public int mSetEnabled = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;

		// Additional data supplied by callers.
		public Object mExtras;

		// Whether an operation is currently pending on this package
		public boolean mOperationPending;

		/*
		 *  Applications hardware preferences
		 */
		/*public final ArrayList<ConfigurationInfo> configPreferences =
		        new ArrayList<ConfigurationInfo>();*/

		public int installLocation;

		public Package(String _name) {
			packageName = _name;
			applicationInfo.packageName = _name;
			applicationInfo.uid = -1;
		}

		public void setPackageName(String newName) {
			packageName = newName;
			applicationInfo.packageName = newName;
			for (int i = activities.size() - 1; i >= 0; i--) {
				activities.get(i).setPackageName(newName);
			}
			for (int i = receivers.size() - 1; i >= 0; i--) {
				receivers.get(i).setPackageName(newName);
			}
			for (int i = services.size() - 1; i >= 0; i--) {
				services.get(i).setPackageName(newName);
			}
		}

		public String toString() {
			return "Package{"
					//+ Integer.toHexString(System.identityHashCode(this)) + " "
					+ packageName + "}";
		}
	}

	public static class Component<II extends IntentInfo> {
		public final Package owner;
		public final ArrayList<II> intents;
		public final String className;
		public Bundle metaData;

		ComponentName componentName;
		String componentShortName;

		public Component(Package _owner) {
			owner = _owner;
			intents = null;
			className = null;
		}

		public Component(final ParsePackageItemArgs args, final PackageItemInfo outInfo) {
            owner = args.owner;
            intents = new ArrayList<II>(0);
            String name = args.sa.getNonConfigurationString(args.nameRes, 0);
            if (name == null) {
                className = null;
                args.outError[0] = args.tag + " does not specify android:name";
                return;
            }

            outInfo.name
                = buildClassName(owner.applicationInfo.packageName, name, args.outError);
            if (outInfo.name == null) {
                className = null;
                args.outError[0] = args.tag + " does not have valid android:name";
                return;
            }

            className = outInfo.name;

            int iconVal = args.sa.getResourceId(args.iconRes, 0);
            if (iconVal != 0) {
                outInfo.icon = iconVal;
                outInfo.nonLocalizedLabel = null;
            }

            TypedValue v = args.sa.peekValue(args.labelRes);
            if (v != null && (outInfo.labelRes=v.resourceId) == 0) {
                outInfo.nonLocalizedLabel = v.coerceToString();
            }

            outInfo.packageName = owner.packageName;
        }


		public Component(final ParseComponentArgs args,
				final ComponentInfo outInfo) {
			/**/
			owner = args.owner;
			intents = new ArrayList<II>(0);
			String name = args.sa.getNonConfigurationString(args.nameRes, 0);
			if (name == null) {
				className = null;
				args.outError[0] = args.tag + " does not specify android:name";
				return;
			}

			//			System.out.println("name got: " + name + ", length() returns "
			//					+ name.length());
			outInfo.name = buildClassName(owner.applicationInfo.packageName,
					name, args.outError);
			if (outInfo.name == null) {
				className = null;
				args.outError[0] = args.tag
						+ " does not have valid android:name";
				return;
			}

			className = outInfo.name;

			int iconVal = args.sa.getResourceId(args.iconRes, 0);
			if (iconVal != 0) {
				outInfo.icon = iconVal;
				outInfo.nonLocalizedLabel = null;
			}

			TypedValue v = args.sa.peekValue(args.labelRes);
			if (v != null && (outInfo.labelRes = v.resourceId) == 0) {
				outInfo.nonLocalizedLabel = v.coerceToString();
			}

			outInfo.packageName = owner.packageName;
			/**/
			if (args.outError[0] != null) {
				return;
			}

			if (args.processRes != 0) {
				CharSequence pname;
				pname = args.sa.getNonConfigurationString(args.processRes, 0);
				outInfo.processName = buildProcessName(
						owner.applicationInfo.packageName,
						owner.applicationInfo.processName, pname, args.flags,
						args.sepProcesses, args.outError);
			}

			if (args.descriptionRes != 0) {
				outInfo.descriptionRes = args.sa.getResourceId(
						args.descriptionRes, 0);
			}

			outInfo.enabled = args.sa.getBoolean(args.enabledRes, true);
		}

		public Component(Component<II> clone) {
			owner = clone.owner;
			intents = clone.intents;
			className = clone.className;
			componentName = clone.componentName;
			componentShortName = clone.componentShortName;
		}

		public ComponentName getComponentName() {
			if (componentName != null) {
				return componentName;
			}
			if (className != null) {
				componentName = new ComponentName(
						owner.applicationInfo.packageName, className);
			}
			return componentName;
		}

		public String getComponentShortName() {
			if (componentShortName != null) {
				return componentShortName;
			}
			ComponentName component = getComponentName();
			if (component != null) {
				componentShortName = component.flattenToShortString();
			}
			return componentShortName;
		}

		public void setPackageName(String packageName) {
			componentName = null;
			componentShortName = null;
		}
	}

	private static boolean copyNeeded(int flags, Package p, Bundle metaData) {
		if (p.mSetEnabled != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
			boolean enabled = p.mSetEnabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
			if (p.applicationInfo.enabled != enabled) {
				return true;
			}
		}
		if ((flags & PackageManager.GET_META_DATA) != 0
				&& (metaData != null || p.mAppMetaData != null)) {
			return true;
		}
		if ((flags & PackageManager.GET_SHARED_LIBRARY_FILES) != 0
				&& p.usesLibraryFiles != null) {
			return true;
		}
		return false;
	}

	public static ApplicationInfo generateApplicationInfo(Package p, int flags) {
		if (p == null)
			return null;
		if (!copyNeeded(flags, p, null)) {
			// CompatibilityMode is global state. It's safe to modify the instance
			// of the package.
			if (!sCompatibilityModeEnabled) {
				p.applicationInfo.disableCompatibilityMode();
			}
			return p.applicationInfo;
		}

		// Make shallow copy so we can store the metadata/libraries safely
		ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
		if ((flags & PackageManager.GET_META_DATA) != 0) {
			ai.metaData = p.mAppMetaData;
		}
		if ((flags & PackageManager.GET_SHARED_LIBRARY_FILES) != 0) {
			ai.sharedLibraryFiles = p.usesLibraryFiles;
		}
		if (!sCompatibilityModeEnabled) {
			ai.disableCompatibilityMode();
		}
		ai.enabled = p.mSetEnabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		return ai;
	}

	public final static class Activity extends Component<ActivityIntentInfo> {
		public final ActivityInfo info;

		public Activity(final ParseComponentArgs args, final ActivityInfo _info) {
			super(args, _info);
			info = _info;
			info.applicationInfo = args.owner.applicationInfo;
		}

		public void setPackageName(String packageName) {
			super.setPackageName(packageName);
			info.packageName = packageName;
		}

		public String toString() {
			return "Activity{"
					//+ Integer.toHexString(System.identityHashCode(this)) + " "
					+ getComponentShortName() + "}";
		}
	}

	public final static class Service extends Component<ServiceIntentInfo> {
		public final ServiceInfo info;

		public Service(final ParseComponentArgs args, final ServiceInfo _info) {
			super(args, _info);
			info = _info;
			info.applicationInfo = args.owner.applicationInfo;
		}

		public void setPackageName(String packageName) {
			super.setPackageName(packageName);
			info.packageName = packageName;
		}

		public String toString() {
			return "Service{"
					//+ Integer.toHexString(System.identityHashCode(this)) + " "
					+ getComponentShortName() + "}";
		}
	}

	public static final ActivityInfo generateActivityInfo(Activity a, int flags) {
		if (a == null)
			return null;
		if (!copyNeeded(flags, a.owner, a.metaData)) {
			return a.info;
		}
		// Make shallow copies so we can store the metadata safely
		ActivityInfo ai = new ActivityInfo(a.info);
		ai.metaData = a.metaData;
		ai.applicationInfo = generateApplicationInfo(a.owner, flags);
		return ai;
	}

	public static final ServiceInfo generateServiceInfo(Service a, int flags) {
		if (a == null)
			return null;
		if (!copyNeeded(flags, a.owner, a.metaData)) {
			return a.info;
		}
		// Make shallow copies so we can store the metadata safely
		ServiceInfo ai = new ServiceInfo(a.info);
		ai.metaData = a.metaData;
		ai.applicationInfo = generateApplicationInfo(a.owner, flags);
		return ai;
	}

	public static class IntentInfo extends IntentFilter {
		public boolean hasDefault;
		public int labelRes;
		public CharSequence nonLocalizedLabel;
		public int icon;
		public int logo;
	}

	public final static class ActivityIntentInfo extends IntentInfo {
		public final Activity activity;

		public ActivityIntentInfo(Activity _activity) {
			activity = _activity;
		}

		public String toString() {
			return "ActivityIntentInfo{"
					//+ Integer.toHexString(System.identityHashCode(this)) + " "
					+ activity.info.name + "}";
		}
	}

	public final static class ServiceIntentInfo extends IntentInfo {
		public final Service service;

		public ServiceIntentInfo(Service _service) {
			service = _service;
		}

		public String toString() {
			return "ActivityIntentInfo{"
					//+ Integer.toHexString(System.identityHashCode(this)) + " "
					+ service.info.name + "}";
		}
	}

	public static void setCompatibilityModeEnabled(
			boolean compatibilityModeEnabled) {
		sCompatibilityModeEnabled = compatibilityModeEnabled;
	}

	//Content Provider
    public final static class Provider extends Component {
        public final ProviderInfo info;
        public boolean syncable;

        public Provider(final ParseComponentArgs args, final ProviderInfo _info) {
            super(args, _info);
            info = _info;
            info.applicationInfo = args.owner.applicationInfo;
            syncable = false;
        }

        public Provider(Provider existingProvider) {
            super(existingProvider);
            this.info = existingProvider.info;
            this.syncable = existingProvider.syncable;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            info.packageName = packageName;
        }

        public String toString() {
            return "Provider{"
                //+ Integer.toHexString(System.identityHashCode(this))
                + " " + info.name + "}";
        }
    }
    public static final ProviderInfo generateProviderInfo(Provider p,
            int flags) {
        if (p == null) return null;
        if (!copyNeeded(flags, p.owner, p.metaData)
                && ((flags & PackageManager.GET_URI_PERMISSION_PATTERNS) != 0
                        || p.info.uriPermissionPatterns == null)) {
            return p.info;
        }
        // Make shallow copies so we can store the metadata safely
        ProviderInfo pi = new ProviderInfo(p.info);
        pi.metaData = p.metaData;
        if ((flags & PackageManager.GET_URI_PERMISSION_PATTERNS) == 0) {
            pi.uriPermissionPatterns = null;
        }
        pi.applicationInfo = generateApplicationInfo(p.owner, flags);
        return pi;
    }

    public final static class Instrumentation extends Component {
        public final InstrumentationInfo info;

        public Instrumentation(final ParsePackageItemArgs args, final InstrumentationInfo _info) {
            super(args, _info);
            info = _info;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            info.packageName = packageName;
        }

        public String toString() {
            return "Instrumentation{"
                + Integer.toHexString(System.identityHashCode(this))
                + " " + getComponentShortName() + "}";
        }
    }


    public static final InstrumentationInfo generateInstrumentationInfo(
            Instrumentation i, int flags) {

        if (i == null) return null;
        if ((flags&PackageManager.GET_META_DATA) == 0) {
            return i.info;
        }

        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;

        return null;

    }

}
