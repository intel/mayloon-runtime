package android.content.res;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.util.XmlUtils;
import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.content.res.AssetManager.AssetInputStream;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.Bundle;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;

public class Resources {
    public static final String TAG = "Resources";
    private static final boolean DEBUG_LOAD = false;
    private static final boolean DEBUG_CONFIG = false;
    // Use the current SDK version code.  If we are a development build,
    // also allow the previous SDK version + 1.
    private static final int sSdkVersion = VERSION.SDK_INT
            + ("REL".equals(VERSION.CODENAME) ? 0 : 1);

    // These are protected by the mTmpValue lock.
    private static final HashMap<String, WeakReference<Drawable.ConstantState> > mDrawableCache
            = new HashMap<String, WeakReference<Drawable.ConstantState> >();
    private final SparseArray<WeakReference<ColorStateList> > mColorStateListCache
    = new SparseArray<WeakReference<ColorStateList> >();
    
    /**
     * This exception is thrown by the resource APIs when a requested resource
     * can not be found.
     */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }
    
    public Resources(AssetManager assets) {
        mAssets = assets;
        updateConfiguration(null, null);
        assets.ensureStringBlocks();
    }

    /**
     * Create a new Resources object on top of an existing set of assets in an
     * AssetManager.
     * 
     * @param assets Previously created AssetManager. 
     * @param metrics Current display metrics to consider when 
     *                selecting/computing resource values.
     * @param config Desired device configuration to consider when 
     *               selecting/computing resource values (optional).
     */
    public Resources(AssetManager assets, DisplayMetrics metrics,
            Configuration config) {
        mAssets = assets;
        mMetrics.setToDefaults();
        updateConfiguration(config, metrics);
        assets.ensureStringBlocks();
    }

	AssetManager mAssets = null;
	private final Configuration mConfiguration = new Configuration();

	TypedValue mTmpValue = new TypedValue();

	/*package*/TypedArray mCachedStyledAttributes = null;

	private int mLastCachedXmlBlockIndex = -1;
	private final int[] mCachedXmlBlockIds = { 0, 0, 0, 0 };
	private final XmlBlock[] mCachedXmlBlocks = new XmlBlock[4];
	
	private static final Object mSync = new Object();
	PluralRules mPluralRule;


	/*package*/final DisplayMetrics mMetrics = new DisplayMetrics();

	public XmlResourceParser getLayout(int id) {
		return loadXmlResourceParser(id, "layout");
	}

    XmlResourceParser loadXmlResourceParser(int id, String type) throws NotFoundException {
        TypedValue value = mTmpValue;
        getValue(id, value, true);
        if (value.type == TypedValue.TYPE_STRING) {
            return loadXmlResourceParser(value.string.toString(), id,
                    value.assetCookie, type);
        }
        return null;
    }

    public void getValue(int id, TypedValue outValue, boolean resolveRefs) {
        boolean found = mAssets.getResourceValue(id, outValue, resolveRefs);
        if (found) {
            return;
        }
        throw new NotFoundException("Resource ID #0x"
                + Integer.toHexString(id));
    }

    /**
     * Return the raw data associated with a particular resource ID.
     * See getIdentifier() for information on how names are mapped to resource
     * IDs, and getString(int) for information on how string resources are
     * retrieved.
     * 
     * <p>Note: use of this function is discouraged.  It is much more
     * efficient to retrieve resources by identifier than by name.
     * 
     * @param name The name of the desired resource.  This is passed to
     *             getIdentifier() with a default type of "string".
     * @param outValue Object in which to place the resource data.
     * @param resolveRefs If true, a resource that is a reference to another
     *                    resource will be followed so that you receive the
     *                    actual final resource data.  If false, the TypedValue
     *                    will be filled in with the reference itself.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     *
     */
    public void getValue(String name, TypedValue outValue, boolean resolveRefs)
            throws NotFoundException {
        int id = getIdentifier(name, "string", null);
        if (id != 0) {
            getValue(id, outValue, resolveRefs);
            return;
        }
        throw new NotFoundException("String resource name " + name);
    }

    public float getFraction(int id, int base, int pbase) {
        synchronized (mTmpValue) {
            TypedValue value = mTmpValue;
            getValue(id, value, true);
            if (value.type == TypedValue.TYPE_FRACTION) {
                return TypedValue.complexToFraction(value.data, base, pbase);
            }
            throw new NotFoundException(
                    "Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                    + Integer.toHexString(value.type) + " is not valid");
        }
    }

    public void parseBundleExtras(XmlResourceParser parser, Bundle outBundle)
            throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            String nodeName = parser.getName();
            if (nodeName.equals("extra")) {
                parseBundleExtra("extra", parser, outBundle);
                XmlUtils.skipCurrentTag(parser);

            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
    }

    /**
     * Parse a name/value pair out of an XML tag holding that data.  The
     * AttributeSet must be holding the data defined by
     * {@link android.R.styleable#Extra}.  The following value types are supported:
     * <ul>
     * <li> {@link TypedValue#TYPE_STRING}:
     * {@link Bundle#putCharSequence Bundle.putCharSequence()}
     * <li> {@link TypedValue#TYPE_INT_BOOLEAN}:
     * {@link Bundle#putCharSequence Bundle.putBoolean()}
     * <li> {@link TypedValue#TYPE_FIRST_INT}-{@link TypedValue#TYPE_LAST_INT}:
     * {@link Bundle#putCharSequence Bundle.putBoolean()}
     * <li> {@link TypedValue#TYPE_FLOAT}:
     * {@link Bundle#putCharSequence Bundle.putFloat()}
     * </ul>
     * 
     * @param tagName The name of the tag these attributes come from; this is
     * only used for reporting error messages.
     * @param attrs The attributes from which to retrieve the name/value pair.
     * @param outBundle The Bundle in which to place the parsed value.
     * @throws XmlPullParserException If the attributes are not valid.
     */
    public void parseBundleExtra(String tagName, AttributeSet attrs,
            Bundle outBundle) throws XmlPullParserException {
        TypedArray sa = obtainAttributes(attrs,
                com.android.internal.R.styleable.Extra);

        String name = sa.getString(
                com.android.internal.R.styleable.Extra_name);
        if (name == null) {
            sa.recycle();
            throw new XmlPullParserException("<" + tagName
                    + "> requires an android:name attribute at "
                    + attrs.getPositionDescription());
        }

        TypedValue v = sa.peekValue(
                com.android.internal.R.styleable.Extra_value);
        if (v != null) {
            if (v.type == TypedValue.TYPE_STRING) {
                CharSequence cs = v.coerceToString();
                outBundle.putCharSequence(name, cs);
            } else if (v.type == TypedValue.TYPE_INT_BOOLEAN) {
                outBundle.putBoolean(name, v.data != 0);
            } else if (v.type >= TypedValue.TYPE_FIRST_INT
                    && v.type <= TypedValue.TYPE_LAST_INT) {
                outBundle.putInt(name, v.data);
            } else if (v.type == TypedValue.TYPE_FLOAT) {
                outBundle.putFloat(name, v.getFloat());
            } else {
                sa.recycle();
                throw new XmlPullParserException("<" + tagName
                        + "> only supports string, integer, float, color, and boolean at "
                        + attrs.getPositionDescription());
            }
        } else {
            sa.recycle();
            throw new XmlPullParserException("<" + tagName
                    + "> requires an android:value or android:resource attribute at "
                    + attrs.getPositionDescription());
        }

        sa.recycle();
    }

	XmlResourceParser loadXmlResourceParser(String file, int id,
			int assetCookie, String type) {
		if (id != 0) {
			try {
				// These may be compiled...
				// First see if this block is in our cache.
				final int num = mCachedXmlBlockIds.length;
				for (int i = 0; i < num; i++) {
					if (mCachedXmlBlockIds[i] == id) {
						return mCachedXmlBlocks[i].newParser();
					}
				}

				// Not in the cache, create a new block and put it at
				// the next slot in the cache.
				XmlBlock block = mAssets.openXmlBlockAsset(assetCookie, file);
				if (block != null) {
					int pos = mLastCachedXmlBlockIndex + 1;
					if (pos >= num)
						pos = 0;
					mLastCachedXmlBlockIndex = pos;
					XmlBlock oldBlock = mCachedXmlBlocks[pos];
					if (oldBlock != null) {
						oldBlock.close();
					}
					mCachedXmlBlockIds[pos] = id;
					mCachedXmlBlocks[pos] = block;
					return block.newParser();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

    /**
     * Retrieve a set of basic attribute values from an AttributeSet, not
     * performing styling of them using a theme and/or style resources.
     * 
     * @param set The current attribute values to retrieve.
     * @param attrs The specific attributes to be retrieved.
     * @return Returns a TypedArray holding an array of the attribute values.
     * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
     * when done with it.
     * 
     * @see Theme#obtainStyledAttributes(AttributeSet, int[], int, int)
     */
	public TypedArray obtainAttributes(AttributeSet set, int[] attrs) {
		int len = attrs.length;
		TypedArray array = getCachedStyledAttributes(len);

		// XXX note that for now we only work with compiled XML files.
		// To support generic XML files we will need to manually parse
		// out the attributes from the XML file (applying type information
		// contained in the resources and such).
		XmlBlock.Parser parser = (XmlBlock.Parser) set;
		mAssets.retrieveAttributes(parser.mParseState, attrs, array.mData,
				array.mIndices);

		array.mRsrcs = attrs;
		array.mXml = parser;

		return array;
	}
	
    /**
     * Store the newly updated configuration.
     */
    public void updateConfiguration(Configuration config,
            DisplayMetrics metrics) {
        synchronized (mTmpValue) {
            int configChanges = 0xfffffff;
            if (config != null) {
                configChanges = mConfiguration.updateFrom(config);
            }
            if (mConfiguration.locale == null) {
                //mConfiguration.locale = Locale.getDefault();
            }
            if (metrics != null) {
                mMetrics.setTo(metrics);
//                mMetrics.updateMetrics(mCompatibilityInfo,
//                        mConfiguration.orientation, mConfiguration.screenLayout);
            }
            mMetrics.scaledDensity = mMetrics.density * mConfiguration.fontScale;

            String locale = null;
            if (mConfiguration.locale != null) {
                locale = mConfiguration.locale.getLanguage();
                if (mConfiguration.locale.getCountry() != null) {
                    locale += "-" + mConfiguration.locale.getCountry();
                }
            }
            int width, height;
            if (mMetrics.widthPixels >= mMetrics.heightPixels) {
                width = mMetrics.widthPixels;
                height = mMetrics.heightPixels;
            } else {
                //noinspection SuspiciousNameCombination
                width = mMetrics.heightPixels;
                //noinspection SuspiciousNameCombination
                height = mMetrics.widthPixels;
            }
            int keyboardHidden = mConfiguration.keyboardHidden;
            if (keyboardHidden == Configuration.KEYBOARDHIDDEN_NO
                    && mConfiguration.hardKeyboardHidden
                            == Configuration.HARDKEYBOARDHIDDEN_YES) {
                keyboardHidden = Configuration.KEYBOARDHIDDEN_SOFT;
            }
            mAssets.setConfiguration(mConfiguration.mcc, mConfiguration.mnc,
                    locale, mConfiguration.orientation,
                    mConfiguration.touchscreen,
                    (int)(mMetrics.density*160), mConfiguration.keyboard,
                    keyboardHidden, mConfiguration.navigation, width, height,
                    mConfiguration.screenLayout, mConfiguration.uiMode, sSdkVersion);

//            clearDrawableCache(mDrawableCache, configChanges);
//            clearDrawableCache(mColorDrawableCache, configChanges);
//
            mColorStateListCache.clear();
//
//
//            flushLayoutCache();
        }
        synchronized (mSync) {
            if (mPluralRule != null) {
                mPluralRule = PluralRules.ruleForLocale(config.locale);
            }
        }
    }

	private TypedArray getCachedStyledAttributes(int len) {
		TypedArray attrs = mCachedStyledAttributes;
		if (attrs != null) {
			mCachedStyledAttributes = null;

			attrs.mLength = len;
			int fullLen = len * AssetManager.STYLE_NUM_ENTRIES;
			if (attrs.mData.length >= fullLen) {
				return attrs;
			}
			attrs.mData = new int[fullLen];
			attrs.mIndices = new int[1 + len];
			return attrs;
		}
		return new TypedArray(this, new int[len
				* AssetManager.STYLE_NUM_ENTRIES], new int[1 + len], len);
	}

    public int getColor(int id) throws NotFoundException {
        TypedValue value = mTmpValue;
        getValue(id, value, true);
        if (value.type >= TypedValue.TYPE_FIRST_INT
                && value.type <= TypedValue.TYPE_LAST_INT) {
            return value.data;
        } else if (value.type == TypedValue.TYPE_STRING) {
            ColorStateList csl = loadColorStateList(mTmpValue, id);
            return csl.getDefaultColor();
        }
        throw new NotFoundException(
                "Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                + Integer.toHexString(value.type) + " is not valid");
    }

    public String getString(int id) {
        try {
            CharSequence res = getText(id);
            if (res != null) {
                return res.toString();
            }
        } catch (RuntimeException e) {
        }
        throw new NotFoundException("String resource ID #0x"
                + Integer.toHexString(id));
    }

	/**
     * Return the string value associated with a particular resource ID,
     * substituting the format arguments as defined in {@link java.util.Formatter}
     * and {@link java.lang.String#format}. It will be stripped of any styled text
     * information.
     * {@more}
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     *           
     * @param formatArgs The format arguments that will be used for substitution.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     *
     * @return String The string data associated with the resource,
     * stripped of styled text information.
     */
    public String getString(int id, Object... formatArgs) throws NotFoundException {
        String raw = getString(id);
        return String.format(raw, formatArgs);
    }


	
    /**
     * Return an XmlResourceParser through which you can read an animation
     * description for the given resource ID.  This parser has limited
     * functionality -- in particular, you can't change its input, and only
     * the high-level events are available.
     * 
     * <p>This function is really a simple wrapper for calling
     * {@link #getXml} with an animation resource.
     * 
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     * 
     * @return A new parser object through which you can read
     *         the XML data.
     *         
     * @see #getXml
     */
    public XmlResourceParser getAnimation(int id) throws NotFoundException {
        return loadXmlResourceParser(id, "anim");
    }

    public XmlResourceParser getXml(int id) throws NotFoundException {
        return loadXmlResourceParser(id, "xml");
    }

	
	 public int getIdentifier(String name, String defType, String defPackage) {
	        try {
	        	Log.d(TAG, name);
	            return Integer.parseInt(name);
	        } catch (Exception e) {
	            // Ignore
	        }
	        return mAssets.getResourceIdentifier(name, defType, defPackage);
	    }

    public CharSequence getText(int id) {
        CharSequence res = mAssets.getResourceText(id);
        if (res != null) {
            return res;
        }

        throw new NotFoundException("Text resource ID #0x"
                + Integer.toHexString(id));
    }

    /**
     * Return the string array associated with a particular resource ID.
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     *
     * @return The string array associated with the resource.
     */
    public String[] getStringArray(int id) throws NotFoundException {
        String[] res = mAssets.getResourceStringArray(id);
        if (res != null) {
            return res;
        }
        throw new NotFoundException("String array resource ID #0x"
                                    + Integer.toHexString(id));
    }

    public int getDimensionPixelSize(int id) throws NotFoundException {
        
        TypedValue value = mTmpValue;
        getValue(id, value, true);
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelSize(
                    value.data, mMetrics);
        }
        throw new NotFoundException(
                "Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                + Integer.toHexString(value.type) + " is not valid");   
    }
	

    public CharSequence[] getTextArray(int id) throws NotFoundException {
        CharSequence[] res = mAssets.getResourceTextArray(id);
        if (res != null) {
            return res;
        }
        throw new NotFoundException("Text array resource ID #0x"
                                    + Integer.toHexString(id));
    }
    
	public Drawable getDrawable(int id) {
		TypedValue value = mTmpValue;
		getValue(id, value, true);
		return loadDrawable(value, id);
	}

    Drawable loadDrawable(TypedValue value, int id) {
        //final long key = (((long) value.assetCookie) << 32) | value.data;
        //final String key = Integer.toString(value.assetCookie) + Integer.toString(value.data);
        String key = null;
        String cookieName = mAssets.getCookieName(value.assetCookie);
        if (cookieName != null && value.string != null) {
            key = cookieName + value.string;
        }
        Drawable dr = getCachedDrawable(key);

        if (dr != null) {
            return dr;
        }
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
//          System.err.println("loading ColorDrawable, data: "
//                  + Integer.toHexString(value.data));
            dr = new ColorDrawable(value.data);
        }
        if (dr == null) {
            String file = value.string.toString();

//          System.err.println("Loading drawable for cookie "
//                  + value.assetCookie + ": " + file);

            if (file.endsWith(".xml")) {
                XmlResourceParser rp = loadXmlResourceParser(file, id,
                        value.assetCookie, "drawable");
                try {
                    dr = Drawable.createFromXml(this, rp);
                } catch (XmlPullParserException e) {
                    //                  e.printStackTrace();
                    dr = null;
                } catch (IOException e) {
                    //                  e.printStackTrace();
                    dr = null;
                }
                rp.close();
            } else {
                try {
                    /**
                     * @j2sNative
                     * // If we have yeild method, we will use the browser to load the image.
                     * // if (window.yield) {
                     * if (false) { // Currently yield can't work well, we will never receive the onload event.
                     *              // After we have find a sync mechasim, we will replace it with our new Sync API.
                     *     dr = android.graphics.drawable.Drawable.createFromResource(this, value, file, id, null);
                     *     return dr;
                     * }
                     */{}
                     
                    InputStream is = mAssets.openNonAsset(
                            value.assetCookie, file, AssetManager.ACCESS_STREAMING);
    //                System.out.println("Opened file " + file + ": " + is);
                    AssetInputStream ais = (AssetInputStream)is;
                    dr = Drawable.createFromResourceStream(this, value, is,ais.getAssetInt().getAssetSource(), null);
                    is.close();
                } catch (Exception e) {
                    // MayLoon TODO:
                }

            }
        }
        if (dr == null) {
            Log.e(TAG, "Can't get resource: " + value.string.toString());
        }
        if (dr != null) {
            dr.setChangingConfigurations(value.changingConfigurations);
            ConstantState cs = dr.getConstantState();
            if (cs != null) {
/*                if (mPreloading) {
                    sPreloadedDrawables.put(key, cs);
                } else */{
                    if  (key != null) {
                        //Log.i(TAG, "Saving cached drawable @ #" +
                        //        Integer.toHexString(key.intValue())
                        //        + " in " + this + ": " + cs);
                        mDrawableCache.put(key, new WeakReference<Drawable.ConstantState> (cs));
                    }
                }
            }
        }

        return dr;
    }

    private Drawable getCachedDrawable(String key) {
        synchronized (mTmpValue) {
            WeakReference<Drawable.ConstantState> wr = mDrawableCache.get(key);
            if (wr != null) {   // we have the key
                Drawable.ConstantState entry = wr.get();
                if (entry != null) {
                    //Log.i(TAG, "Returning cached drawable @ #" +
                    //        Integer.toHexString(((Integer)key).intValue())
                    //        + " in " + this + ": " + entry);
                    return entry.newDrawable(this);
                }
                else {  // our entry has been purged
                    //mDrawableCache.remove(key);
                }
            }
        }
        return null;
    }
    
    /**
     * Open a data stream for reading a raw resource.  This can only be used
     * with resources whose value is the name of an asset file -- that is, it can be
     * used to open drawable, sound, and raw resources; it will fail on string
     * and color resources.
     *
     * @param id The resource identifier to open, as generated by the appt tool.
     * @param value The TypedValue object to hold the resource information.
     *
     * @return InputStream Access to the resource data.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     */
    public InputStream openRawResource(int id, TypedValue value)  {
        getValue(id, value, true);

        try {
            return mAssets.openNonAsset(value.assetCookie, value.string.toString(),
                    AssetManager.ACCESS_STREAMING);
        } catch (Exception e) {
        	System.out.println("openRawResource Exception");
        	return null;
        }
    }
    
    public AssetFileDescriptor openRawResourceFd(int id)  {
        synchronized (mTmpValue) {
            TypedValue value = mTmpValue;
            getValue(id, value, true);
            String raw = "";
            /** 
             * @j2sNative
             * 
             * // This is safe because the app's asset path is always added secondly
             * raw += this.mAssets.am.mAssetPaths.array[1].path;
             * raw += "";
             */ { }
            raw += value.string.toString();
            return new AssetFileDescriptor(null, 0, 0, raw);
        }
    }

    /**
     * Open a data stream for reading a raw resource.  This can only be used
     * with resources whose value is the name of an asset files -- that is, it can be
     * used to open drawable, sound, and raw resources; it will fail on string
     * and color resources.
     * 
     * @param id The resource identifier to open, as generated by the appt
     *           tool.
     * 
     * @return InputStream Access to the resource data.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     * 
     */
    public InputStream openRawResource(int id) throws NotFoundException {
        synchronized (mTmpValue) {
            return openRawResource(id, mTmpValue);
        }
    }
    
    /*package*/ ColorStateList loadColorStateList(TypedValue value, int id)
            throws NotFoundException {
        final int key = (value.assetCookie << 24) | value.data;

        ColorStateList csl;

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            csl = ColorStateList.valueOf(value.data);
            return csl;
        }

        csl = getCachedColorStateList(key);
        if (csl != null) {
            return csl;
        }

        if (value.string == null) {
            throw new NotFoundException(
                    "Resource is not a ColorStateList (color or path): " + value);
        }
        
        String file = value.string.toString();

        if (file.endsWith(".xml")) {
            try {
                XmlResourceParser rp = loadXmlResourceParser(
                        file, id, value.assetCookie, "colorstatelist"); 
                csl = ColorStateList.createFromXml(this, rp);
                rp.close();
            } catch (Exception e) {
                NotFoundException rnf = new NotFoundException(
                    "File " + file + " from color state list resource ID #0x"
                    + Integer.toHexString(id));
                rnf.initCause(e);
                throw rnf;
            }
        } else {
            throw new NotFoundException(
                    "File " + file + " from drawable resource ID #0x"
                    + Integer.toHexString(id) + ": .xml extension required");
        }

        if (csl != null) {
            synchronized (mTmpValue) {
                // Log.i(TAG, "Saving cached color state list @ #" +
                // Integer.toHexString(key.intValue())
                // + " in " + this + ": " + csl);
                mColorStateListCache.put(
                        key, new WeakReference<ColorStateList>(csl));
            }
        }

        return csl;
    }
    
    private ColorStateList getCachedColorStateList(int key) {
        synchronized (mTmpValue) {
            WeakReference<ColorStateList> wr = mColorStateListCache.get(key);
            if (wr != null) {   // we have the key
                ColorStateList entry = wr.get();
                if (entry != null) {
                    //Log.i(TAG, "Returning cached color state list @ #" +
                    //        Integer.toHexString(((Integer)key).intValue())
                    //        + " in " + this + ": " + entry);
                    return entry;
                }
                else {  // our entry has been purged
                    mColorStateListCache.delete(key);
                }
            }
        }
        return null;
    }

    public String getResourceName(int resid) {
        String str = mAssets.getResourceName(resid);
        return str;
    }

    public DisplayMetrics getDisplayMetrics() {
        return mMetrics;
    }

    /**
     * Update the system resources configuration if they have previously been
     * initialized.
     * 
     * @hide
     */
    public static void updateSystemConfiguration(Configuration config, DisplayMetrics metrics) {
        if (mSystem != null) {
            mSystem.updateConfiguration(config, metrics);
            // Log.i(TAG, "Updated system resources " + mSystem
            // + ": " + mSystem.getConfiguration());
        }
    }

    private static Resources mSystem = null;

    public static Resources getSystem() {
        Resources ret = mSystem;
        if (ret == null) {
            ret = new Resources();
            mSystem = ret;
        }

        return ret;
    }

    private Resources() {
        mAssets = AssetManager.getSystem();
        // NOTE: Intentionally leaving this uninitialized (all values set
        // to zero), so that anyone who tries to do something that requires
        // metrics will get a very wrong value.
        mConfiguration.setToDefaults();
        mMetrics.setToDefaults();
        updateConfiguration(null, null);
        mAssets.ensureStringBlocks();
        //mCompatibilityInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
    }

	public final class Theme {
		/**
		 * Place new attribute values into the theme.  The style resource
		 * specified by <var>resid</var> will be retrieved from this Theme's
		 * resources, its values placed into the Theme object.
		 * 
		 * <p>The semantics of this function depends on the <var>force</var>
		 * argument:  If false, only values that are not already defined in
		 * the theme will be copied from the system resource; otherwise, if
		 * any of the style's attributes are already defined in the theme, the
		 * current values in the theme will be overwritten.
		 * 
		 * @param resid The resource ID of a style resource from which to
		 *              obtain attribute values.
		 * @param force If true, values in the style resource will always be
		 *              used in the theme; otherwise, they will only be used
		 *              if not already defined in the theme.
		 */
		public void applyStyle(int resid, boolean force) {
			AssetManager.applyThemeStyle(mTheme, resid, force);
		}

		/**
		 * Set this theme to hold the same contents as the theme
		 * <var>other</var>.  If both of these themes are from the same
		 * Resources object, they will be identical after this function
		 * returns.  If they are from different Resources, only the resources
		 * they have in common will be set in this theme.
		 * 
		 * @param other The existing Theme to copy from.
		 */
        public void setTo(Theme other) {
            AssetManager.copyTheme(mTheme, other.mTheme);
        }

		/**
		 * Return a StyledAttributes holding the values defined by
		 * <var>Theme</var> which are listed in <var>attrs</var>.
		 * 
		 * <p>Be sure to call StyledAttributes.recycle() when you are done with
		 * the array.
		 * 
		 * @param attrs The desired attributes.
		 *
		 * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
		 * 
		 * @return Returns a TypedArray holding an array of the attribute values.
		 * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
		 * when done with it.
		 * 
		 * @see Resources#obtainAttributes
		 * @see #obtainStyledAttributes(int, int[])
		 * @see #obtainStyledAttributes(AttributeSet, int[], int, int)
		 */
        public TypedArray obtainStyledAttributes(int[] attrs) {
            int len = attrs.length;
            TypedArray array = getCachedStyledAttributes(len);
            array.mRsrcs = attrs;

            AssetManager.applyStyle(mTheme, 0, 0, null, attrs, array.mData,
                    array.mIndices);
            return array;
        }

		/**
		 * Return a StyledAttributes holding the values defined by the style
		 * resource <var>resid</var> which are listed in <var>attrs</var>.
		 * 
		 * <p>Be sure to call StyledAttributes.recycle() when you are done with
		 * the array.
		 * 
		 * @param resid The desired style resource.
		 * @param attrs The desired attributes in the style.
		 * 
		 * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
		 * 
		 * @return Returns a TypedArray holding an array of the attribute values.
		 * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
		 * when done with it.
		 * 
		 * @see Resources#obtainAttributes
		 * @see #obtainStyledAttributes(int[])
		 * @see #obtainStyledAttributes(AttributeSet, int[], int, int)
		 */
		public TypedArray obtainStyledAttributes(int resid, int[] attrs) {
			int len = attrs.length;
			TypedArray array = getCachedStyledAttributes(len);
			// TODO obtainAttributes

			array.mRsrcs = attrs;

			AssetManager.applyStyle(mTheme, 0, resid, null, attrs, array.mData,
					array.mIndices);
			if (false) {
				int[] data = array.mData;

				System.out
						.println("**********************************************************");
				System.out
						.println("**********************************************************");
				System.out
						.println("**********************************************************");
				System.out.println("Attributes:");
				String s = "  Attrs:";
				int i;
				for (i = 0; i < attrs.length; i++) {
					s = s + " 0x" + Integer.toHexString(attrs[i]);
				}
				System.out.println(s);
				s = "  Found:";
				TypedValue value = new TypedValue();
				for (i = 0; i < attrs.length; i++) {
					int d = i * AssetManager.STYLE_NUM_ENTRIES;
					value.type = data[d + AssetManager.STYLE_TYPE];
					value.data = data[d + AssetManager.STYLE_DATA];
					value.assetCookie = data[d
							+ AssetManager.STYLE_ASSET_COOKIE];
					value.resourceId = data[d + AssetManager.STYLE_RESOURCE_ID];
					s = s + " 0x" + Integer.toHexString(attrs[i]) + "=" + value;
				}
				System.out.println(s);
			}
			return array;
		}

		/**
		 * Return a StyledAttributes holding the attribute values in
		 * <var>set</var>
		 * that are listed in <var>attrs</var>.  In addition, if the given
		 * AttributeSet specifies a style class (through the "style" attribute),
		 * that style will be applied on top of the base attributes it defines.
		 * 
		 * <p>Be sure to call StyledAttributes.recycle() when you are done with
		 * the array.
		 * 
		 * <p>When determining the final value of a particular attribute, there
		 * are four inputs that come into play:</p>
		 * 
		 * <ol>
		 *     <li> Any attribute values in the given AttributeSet.
		 *     <li> The style resource specified in the AttributeSet (named
		 *     "style").
		 *     <li> The default style specified by <var>defStyleAttr</var> and
		 *     <var>defStyleRes</var>
		 *     <li> The base values in this theme.
		 * </ol>
		 * 
		 * <p>Each of these inputs is considered in-order, with the first listed
		 * taking precedence over the following ones.  In other words, if in the
		 * AttributeSet you have supplied <code>&lt;Button
		 * textColor="#ff000000"&gt;</code>, then the button's text will
		 * <em>always</em> be black, regardless of what is specified in any of
		 * the styles.
		 * 
		 * @param set The base set of attribute values.  May be null.
		 * @param attrs The desired attributes to be retrieved.
		 * @param defStyleAttr An attribute in the current theme that contains a
		 *                     reference to a style resource that supplies
		 *                     defaults values for the StyledAttributes.  Can be
		 *                     0 to not look for defaults.
		 * @param defStyleRes A resource identifier of a style resource that
		 *                    supplies default values for the StyledAttributes,
		 *                    used only if defStyleAttr is 0 or can not be found
		 *                    in the theme.  Can be 0 to not look for defaults.
		 * 
		 * @return Returns a TypedArray holding an array of the attribute values.
		 * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
		 * when done with it.
		 * 
		 * @see Resources#obtainAttributes
		 * @see #obtainStyledAttributes(int[])
		 * @see #obtainStyledAttributes(int, int[])
		 */
		public TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs,
				int defStyleAttr, int defStyleRes) {
			int len = attrs.length;
			TypedArray array = getCachedStyledAttributes(len);

			// XXX note that for now we only work with compiled XML files.
			// To support generic XML files we will need to manually parse
			// out the attributes from the XML file (applying type information
			// contained in the resources and such).
			//			XmlBlock.Parser parser = (XmlBlock.Parser) set;
			AssetManager.applyStyle(mTheme, defStyleAttr, defStyleRes, set,
					attrs, array.mData, array.mIndices);
			//			mAssets.retrieveAttributes(parser.mParseState, attrs, array.mData,
			//					array.mIndices);

			array.mRsrcs = attrs;
			array.mXml = (XmlBlock.Parser) set;

			if (false) {
				int[] data = array.mData;

				System.out.println("Attributes:");
				String s = "  Attrs:";
				int i;
				for (i = 0; i < set.getAttributeCount(); i++) {
					s = s + " " + set.getAttributeName(i);
					int id = set.getAttributeNameResource(i);
					if (id != 0) {
						s = s + "(0x" + Integer.toHexString(id) + ")";
					}
					s = s + "=" + set.getAttributeValue(i);
					s += "\n";
				}
				System.out.println(s);
				s = "  Found:";
				TypedValue value = new TypedValue();
				for (i = 0; i < attrs.length; i++) {
					int d = i * AssetManager.STYLE_NUM_ENTRIES;
					value.type = data[d + AssetManager.STYLE_TYPE];
					value.data = data[d + AssetManager.STYLE_DATA];
					value.assetCookie = data[d
							+ AssetManager.STYLE_ASSET_COOKIE];
					value.resourceId = data[d + AssetManager.STYLE_RESOURCE_ID];
					s = s + " 0x" + Integer.toHexString(attrs[i]) + "=" + value;
					s += "\n";
				}
				System.out.println(s);
			}

			return array;
		}
		
		 
		/**
		 * Retrieve the value of an attribute in the Theme.  The contents of
		 * <var>outValue</var> are ultimately filled in by
		 * {@link Resources#getValue}.
		 * 
		 * @param resid The resource identifier of the desired theme
		 *              attribute.
		 * @param outValue Filled in with the ultimate resource value supplied
		 *                 by the attribute.
		 * @param resolveRefs If true, resource references will be walked; if
		 *                    false, <var>outValue</var> may be a
		 *                    TYPE_REFERENCE.  In either case, it will never
		 *                    be a TYPE_ATTRIBUTE.
		 * 
		 * @return boolean Returns true if the attribute was found and
		 *         <var>outValue</var> is valid, else false.
		 */
		public boolean resolveAttribute(int resid, TypedValue outValue,
				boolean resolveRefs) {
//						boolean got = mAssets.getThemeValue(mTheme, resid, outValue,
//								resolveRefs);
//						if (false) {
//							System.out.println("resolveAttribute #"
//									+ Integer.toHexString(resid) + " got=" + got
//									+ ", type=0x" + Integer.toHexString(outValue.type)
//									+ ", data=0x" + Integer.toHexString(outValue.data));
//						}
//						return got;
			return false;
		}

		/**
		 * Print contents of this theme out to the log.  For debugging only.
		 * 
		 * @param priority The log priority to use.
		 * @param tag The log tag to use.
		 * @param prefix Text to prefix each line printed.
		 */
		public void dump(int priority, String tag, String prefix) {
			//			AssetManager.dumpTheme(mTheme, priority, tag, prefix);
		}

		protected void finalize() throws Throwable {
			super.finalize();
			//			mAssets.releaseTheme(mTheme);
		}

		/*package*/Theme() {
			mAssets = Resources.this.mAssets;
			mTheme = mAssets.createTheme();
		}

		private final AssetManager mAssets;
		private ResTable.Theme mTheme = null;
	}

	public final Theme newTheme() {
		return new Theme();
	}

    /**
     * Return a boolean associated with a particular resource ID.  This can be
     * used with any integral resource value, and will return true if it is
     * non-zero.
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     *
     * @return Returns the boolean value contained in the resource.
     */
    public boolean getBoolean(int id) throws NotFoundException {
        synchronized (mTmpValue) {
            TypedValue value = mTmpValue;
            getValue(id, value, true);
            if (value.type >= TypedValue.TYPE_FIRST_INT
                && value.type <= TypedValue.TYPE_LAST_INT) {
                return value.data != 0;
            }
            throw new NotFoundException(
                "Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                + Integer.toHexString(value.type) + " is not valid");
        }
    }
    
    /**
     * Return the string value associated with a particular resource ID for a particular
     * numerical quantity.
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @param quantity The number used to get the correct string for the current language's
     *           plural rules.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     *
     * @return String The string data associated with the resource,
     * stripped of styled text information.
     */
    public String getQuantityString(int id, int quantity) throws NotFoundException {
        return getQuantityText(id, quantity).toString();
    }
    
    /**
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     *
     * @return CharSequence The string data associated with the resource, plus
     *         possibly styled text information.
     */
    public CharSequence getQuantityText(int id, int quantity) throws NotFoundException {
        PluralRules rule = getPluralRule();
        CharSequence res = mAssets.getResourceBagText(id, rule.attrForNumber(quantity));
        if (res != null) {
            return res;
        }
        res = mAssets.getResourceBagText(id, PluralRules.ID_OTHER);
        if (res != null) {
            return res;
        }
        throw new NotFoundException("Plural resource ID #0x" + Integer.toHexString(id)
                + " quantity=" + quantity
                + " item=" + PluralRules.stringForQuantity(rule.quantityForNumber(quantity)));
    }

    private PluralRules getPluralRule() {
        synchronized (mSync) {
            if (mPluralRule == null) {
                mPluralRule = PluralRules.ruleForLocale(mConfiguration.locale);
            }
            return mPluralRule;
        }
    }

    /**
     * Return the current configuration that is in effect for this resource 
     * object.  The returned object should be treated as read-only.
     * 
     * @return The resource's current configuration. 
     */
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * Retrieve underlying AssetManager storage for these resources.
     */
    public final AssetManager getAssets() {
        return mAssets;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getMovie");
     */
    @MayloonStubAnnotation()
    public Object getMovie(int id) {
        System.out.println("Stub" + " Function : getMovie");
        return null;
    }

    public String getResourceTypeName(int resid) {
        String str = mAssets.getResourceTypeName(resid);
        return str;
    }

    /**
     * Return the int array associated with a particular resource ID.
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     *
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     *
     * @return The int array associated with the resource.
     */
    public int[] getIntArray(int id) {
        int[] res = mAssets.getArrayIntResource(id);
        if (res != null) {
            return res;
        }
        throw new NotFoundException("Int array resource ID #0x"
                                    + Integer.toHexString(id));
    }

    public int getInteger(int id) {
        synchronized (mTmpValue) {
            TypedValue value = mTmpValue;
            getValue(id, value, true);
            if (value.type >= TypedValue.TYPE_FIRST_INT
                && value.type <= TypedValue.TYPE_LAST_INT) {
                return value.data;
            }
            throw new NotFoundException(
                "Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                + Integer.toHexString(value.type) + " is not valid");
        }
    }

    public float getDimension(int id) throws NotFoundException {
        TypedValue value = mTmpValue;
        getValue(id, value, true);
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimension(value.data, mMetrics);
        }
        throw new NotFoundException(
                "Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                + Integer.toHexString(value.type) + " is not valid");
    }

    /**
     * @j2sNative
     * console.log("Missing method: finishPreloading");
     */
    @MayloonStubAnnotation()
    public final void finishPreloading() {
        System.out.println("Stub" + " Function : finishPreloading");
        return;
    }

    public String getResourceEntryName(int resid) {
        String str = mAssets.getResourceEntryName(resid);
        return str;
    }

    public String getResourcePackageName(int resid) {
        String str = mAssets.getResourcePackageName(resid);
        return str;
    }

    /**
     * @j2sNative
     * console.log("Missing method: flushLayoutCache");
     */
    @MayloonStubAnnotation()
    public final void flushLayoutCache() {
        System.out.println("Stub" + " Function : flushLayoutCache");
        return;
    }

    public int getDimensionPixelOffset(int id) {
        synchronized (mTmpValue) {
            TypedValue value = mTmpValue;
            getValue(id, value, true);
            if (value.type == TypedValue.TYPE_DIMENSION) {
                return TypedValue.complexToDimensionPixelOffset(
                        value.data, mMetrics);
            }
            throw new NotFoundException(
                    "Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                    + Integer.toHexString(value.type) + " is not valid");
        }
    }

    public ColorStateList getColorStateList(int id) {
        synchronized (mTmpValue) {
            TypedValue value = mTmpValue;
            getValue(id, value, true);
            return loadColorStateList(value, id);
        }
    }

    public TypedArray obtainTypedArray(int id) {
        int len = mAssets.getArraySize(id);
        if (len < 0) {
            throw new NotFoundException("Array resource ID #0x"
                    + Integer.toHexString(id));
        }

        TypedArray array = getCachedStyledAttributes(len);
        array.mLength = mAssets.retrieveArray(id, array.mData);
        array.mIndices[0] = 0;

        return array;
    }
}
