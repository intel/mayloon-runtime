package android.content.res;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.app.ActivityThread;
import android.content.res.ResTable.bag_entry;
import android.content.res.ResourceTypes.ResTable_config;
import android.content.res.ResourceTypes.Res_value;
import android.util.AttributeSet;
import android.util.Errors;
import android.util.Log;
import android.util.Time;
import android.util.TypedValue;

public class AssetManager {
    private static final String TAG = "AssetManager";
	private nativeAssetManager am = null;
	private StringBlock mStringBlocks[] = null;
    private boolean mOpen = true;
	// enum AccessMode
	public static final int ACCESS_UNKNOWN = 0;
	public static final int ACCESS_RANDOM = 1;
	public static final int ACCESS_STREAMING = 2;
	public static final int ACCESS_BUFFER = 3;
    private final static HashMap<String, _FileAsset> mAssetCache = new HashMap<String, _FileAsset>();

	public AssetManager() {
		init();
		ensureSystemAssets();
	}

	public boolean getResourceValue(int ident, TypedValue outValue,
			boolean resolveRefs) {
		int block = loadResourceValue(ident, outValue, resolveRefs);
		if (block >= 0) {
			if (outValue.type != TypedValue.TYPE_STRING) {
				return true;
			}
			outValue.string = mStringBlocks[block].get(outValue.data);
			return true;
		}
		return false;
	}

	private TypedValue mValue = new TypedValue();

	/**
	* Retrieve the string value associated with a particular resource
	* identifier for the current configuration / skin.
	*/
	/* package */CharSequence getResourceText(int ident) {
		TypedValue tmpValue = mValue;
		int block = loadResourceValue(ident, tmpValue, true);
		if (block >= 0) {
			if (tmpValue.type == TypedValue.TYPE_STRING) {
				return mStringBlocks[block].get(tmpValue.data);
			}
			return tmpValue.coerceToString();
		}
		return null;
	}

    /**
     * Retrieve the text array associated with a particular resource
     * identifier.
     * @param id Resource id of the string array
     */
    /*package*/ final CharSequence[] getResourceTextArray(final int id) {
        int[] rawInfoArray = getArrayStringInfo(id);
        if (rawInfoArray == null) {
            throw new NullPointerException("AssetManager.getResourceTextArray rawInfoArray is null");
        }
        int rawInfoArrayLen = rawInfoArray.length;
        final int infoArrayLen = rawInfoArrayLen / 2;
        int block;
        int index;
        CharSequence[] retArray = new CharSequence[infoArrayLen];
        for (int i = 0, j = 0; i < rawInfoArrayLen; i = i + 2, j++) {
            block = rawInfoArray[i];
            index = rawInfoArray[i + 1];
            retArray[j] = index >= 0 ? mStringBlocks[block].get(index) : null;
        }
        return retArray;
    }

    /**
     * Retrieve the string array associated with a particular resource
     * identifier.
     * @param id Resource id of the string array
     */
    /*package*/ final String[] getResourceStringArray(final int id) {
        String[] retArray = getArrayStringResource(id);
        return retArray;
    }

    /* package */int[] getArrayIntResource(int arrayResId) {
        int[] result = null;
        if (am == null) {
            return result;
        }
        ResTable res = am.getResources(true);

        ArrayList<ResTable.bag_entry> startOfBag = new ArrayList<ResTable.bag_entry>();
        int N = res.getBagLocked(arrayResId, startOfBag, null);
        if (N < 0) {
            return result;
        }

        result = new int[N];
        Res_value value;
        for (int i = 0, j = 0; i < N; i++) {
            int stringBlock = 0;

            ResTable.bag_entry bag = startOfBag.get(i);
            value = bag.map.value;

            stringBlock = res.resolveReference(value, bag.stringBlock, null, null, null);
            if (value.dataType >= TypedValue.TYPE_FIRST_INT && value.dataType <= TypedValue.TYPE_LAST_INT){
                result[j++] = value.data;
            }
            if (stringBlock == Errors.BAD_INDEX) {
                return result;
            }
        }

        return result;
    }

    private final String[] getArrayStringResource(int arrayRes) {
        int[] rawInfoArray = getArrayStringInfo(arrayRes);
        if (rawInfoArray == null) {
            throw new NullPointerException("AssetManager.getArrayStringResource rawInfoArray is null");
        }
        int rawInfoArrayLen = rawInfoArray.length;
        final int infoArrayLen = rawInfoArrayLen / 2;
        int block;
        int index;
        String[] retArray = new String[infoArrayLen];
        for (int i = 0, j = 0; i < rawInfoArrayLen; i = i + 2, j++) {
            block = rawInfoArray[i];
            index = rawInfoArray[i + 1];
            retArray[j] = index >= 0 ? mStringBlocks[block].get(index).toString() : null;
        }
        return retArray;
    }

    private final int[] getArrayStringInfo(int arrayResId)
    {
        int[] result = null;
        if (am == null) {
            return result;
        }
        ResTable res = am.getResources(true);

        ArrayList<ResTable.bag_entry> startOfBag = new ArrayList<ResTable.bag_entry>();
        int N = res.getBagLocked(arrayResId, startOfBag, null);
        if (N < 0) {
            return result;
        }

        result = new int[N*2];
        Res_value value;
        for (int i = 0, j = 0; i < N; i++) {
        	int stringIndex = -1;
        	int stringBlock = 0;

        	ResTable.bag_entry bag = startOfBag.get(i);
        	value = bag.map.value;

        	stringBlock = res.resolveReference(value, bag.stringBlock, null, null, null);
        	if (value.dataType ==  TypedValue.TYPE_STRING) {
        		stringIndex = value.data;
        	}
        	if (stringBlock == Errors.BAD_INDEX) {
        		return result;
        	}

        	result[j++] = stringBlock;
        	result[j++] = stringIndex;
        }

        return result;
    }

	/*package*/final CharSequence getPooledString(int block, int id) {
		return mStringBlocks[block - 1].get(id);
	}

	public XmlBlock openXmlBlockAsset(int cookie, String fileName) {
		ResXMLTree rt = openXmlAssetNative(cookie, fileName);
		if (rt != null) {
			XmlBlock res = new XmlBlock(this, rt);
			return res;
		}
		return null;
	}

	final void ensureStringBlocks() {
		//		am.getResTable(false);
		if (mStringBlocks == null) {
			makeStringBlocks(true);
		}
	}

	private final void makeStringBlocks(boolean copyFromSystem) {
		final int sysNum = copyFromSystem ? sSystem.mStringBlocks.length : 0;
		if (sysNum > 0)
			Log.i(TAG, "makeStringBlocks, sysNum = " + sysNum);
		final int num = getStringBlockCount();
		mStringBlocks = new StringBlock[num];
		//		if (localLOGV)
		//			Log.v(TAG, "Making string blocks for " + this + ": " + num);
		for (int i = 0; i < num; i++) {
			if (i < sysNum) {
				mStringBlocks[i] = sSystem.mStringBlocks[i];
			} else {
				mStringBlocks[i] = new StringBlock(getNativeStringBlock(i),
						true);
			}
		}
	}

	public final int[] addAssetPaths(String[] paths) {
		if (paths == null) {
			return null;
		}

		int[] cookies = new int[paths.length];
		for (int i = 0; i < paths.length; i++) {
			cookies[i] = addAssetPath(paths[i]);
		}

		return cookies;
	}

	// native function ///////////////////////////////////////
	public int addAssetPath(String path) {
		if (am == null)
			return -1;
		ArrayList<Integer> cookie = new ArrayList<Integer>();
		cookie.add(new Integer(0));
		if (true == am.addAssetPath(path, cookie))
			return cookie.get(0).intValue();
		return 0;
	}

    final int getResourceIdentifier(String name, String type,
            String defPackage) {
        if (am == null)
            return 0;
        int ident = am.getResources(false).identifierForName(name, type, defPackage);
        return ident;
    }

    final String getResourceName(int resid) {
        // TODO: return the real resource name
        if (am == null)
            return null;
        ResTable.resource_name name = new ResTable.resource_name(null, null, null);
        if (!am.getResources(false).getResourceName(resid, name)) {
            return null;
        }
        String str = null;
        if (name.mPackage != null) {
            str = name.mPackage;
        }

        if (name.mType != null) {
            if (str.length() > 0) {
                str += ":";
            }
            str += name.mType;
        }

        if (name.mName != null) {
            if (str.length() > 0) {
                str += "/";
            }
            str += name.mName;
        }

        return str;
    }

    final String getResourcePackageName(int resid) {
        if (am == null)
            return null;
        ResTable.resource_name name = new ResTable.resource_name(null, null, null);
        if (!am.getResources(false).getResourceName(resid, name)) {
            return null;
        }
        
         return name.mPackage;
    }

    final String getResourceTypeName(int resid) {
        if (am == null)
            return null;
        ResTable.resource_name name = new ResTable.resource_name(null, null, null);
        if (!am.getResources(false).getResourceName(resid, name)) {
            return null;
        }
        return name.mType;
    }

    final String getResourceEntryName(int resid) {
        if (am == null)
            return null;
        ResTable.resource_name name = new ResTable.resource_name(null, null, null);
        if (!am.getResources(false).getResourceName(resid, name)) {
            return null;
        }
        return name.mName;
    }

    private native final int openAsset(String fileName, int accessMode);

    private native final int openNonAssetNative(int cookie, String fileName,
            int accessMode);

    /**
     * these functions are used to read resources under assets directory, will
     * not be used for now private native final void destroyAsset(int asset);
     * private native final int readAssetChar(int asset); private native final
     * int readAsset(int asset, byte[] b, int off, int len); private native
     * final long seekAsset(int asset, long offset, int whence); private native
     * final long getAssetLength(int asset); private native final long
     * getAssetRemainingLength(int asset); /
     **/

    private final int loadResourceValue(int ident, TypedValue outValue,
            boolean resolve) {
        if (am == null)
            return 0;
        ResTable res = am.getResources(false);
        Res_value value = new Res_value();
        ResTable_config config = new ResTable_config();
        ArrayList<Integer> typeSpecFlags = new ArrayList<Integer>();
        typeSpecFlags.add(new Integer(0));
        int block = res.getResource(ident, value, false, typeSpecFlags, config);
        if (block == Errors.BAD_INDEX) {
            return block;
        }
        ArrayList<Integer> ref = new ArrayList<Integer>();
        ref.add(new Integer(ident));
        if (resolve) {
            block = res.resolveReference(value, block, ref, null, null);
            if (block == Errors.BAD_INDEX)
                return 0;
        }

        if (block >= 0) {
            outValue.type = value.dataType;
            outValue.data = value.data;
            outValue.string = null;
            outValue.resourceId = ref.get(0);
            outValue.changingConfigurations = typeSpecFlags.get(0);
            outValue.assetCookie = res.getTableCookie(block);
            if (config != null) {
                outValue.density = config.density;
            }
        }

        return block;
    }

    /**
     * Returns true if the resource was found, filling in mRetStringBlock and
     * mRetData.
     */
    private native final int loadResourceBagValue(int ident, int bagEntryId, TypedValue outValue,
                                               boolean resolve);


	static final int STYLE_NUM_ENTRIES = 6;
	static final int STYLE_TYPE = 0;
	static final int STYLE_DATA = 1;
	static final int STYLE_ASSET_COOKIE = 2;
	static final int STYLE_RESOURCE_ID = 3;
	static final int STYLE_CHANGING_CONFIGURATIONS = 4;
	static final int STYLE_DENSITY = 5;

	static final boolean applyStyle(ResTable.Theme theme, int defStyleAttr,
			int defStyleRes, AttributeSet set, int[] attrs, int[] outValues,
			int[] outIndices) {
		ResTable res = theme.getResTable();
		ResXMLParser xmlParser = set != null ? ((XmlBlock.Parser) set).mParseState
				: null;
		ResTable_config config = new ResTable_config();
		Res_value value = new Res_value();
		if (attrs == null || outValues == null)
			return false;
		int NI = attrs.length;
		int NV = outValues.length;
		if (NV < (NI * STYLE_NUM_ENTRIES))
			return false;
		int[] indices = null;
		int indicesIdx = 0;
		if (outIndices != null)
			indices = outIndices;

		// Load default style from attribute, if specified...
		ArrayList<Integer> defStyleBagTypeSetFlags = new ArrayList<Integer>();
		defStyleBagTypeSetFlags.add(new Integer(0));
		if (defStyleAttr != 0) {
			Res_value tmp = new Res_value();
			if (theme.getAttribute(defStyleAttr, tmp, defStyleBagTypeSetFlags) >= 0) {
				if (tmp.dataType == TypedValue.TYPE_REFERENCE) {
					defStyleRes = tmp.data;
				}
			}
		}

		// Retrieve the style class associated with the current XML tag.
		int style = 0;
		ArrayList<Integer> styleBagTypeSetFlags = new ArrayList<Integer>();
		styleBagTypeSetFlags.add(new Integer(0));
		if (xmlParser != null) {
			int idx = xmlParser.indexOfStyle();
			if (idx >= 0 && xmlParser.getAttributeValue(idx, value) >= 0) {
				if (value.dataType == TypedValue.TYPE_ATTRIBUTE) {
					if (theme.getAttribute(value.data, value,
							styleBagTypeSetFlags) < 0) {
						value.dataType = TypedValue.TYPE_NULL;
					}
				}
				if (value.dataType == TypedValue.TYPE_REFERENCE) {
					style = value.data;
				}
			}
		}

		// Now start pulling stuff from it.

		// Retrieve the default style bag, if requested.
		ArrayList<bag_entry> defStyleEnt = new ArrayList<bag_entry>();
		ArrayList<Integer> defStyleTypeSetFlags = new ArrayList<Integer>();
		defStyleTypeSetFlags.add(new Integer(0));
		int bagOff = defStyleRes != 0 ? res.getBagLocked(defStyleRes,
				defStyleEnt, defStyleTypeSetFlags) : -1;
		int tmp = defStyleTypeSetFlags.get(0) | defStyleBagTypeSetFlags.get(0);
		int endDefStyleEnt = defStyleEnt.size();
		int iDefStyle = 0;
		//		if (defStyleAttr == 16842862) {
		//			System.out.println("defSytleRes: " + defStyleRes);
		//			printBag(defStyleEnt);
		//		}

		// Retrieve the style class bag, if requested.
		ArrayList<bag_entry> styleEnt = new ArrayList<bag_entry>();
		ArrayList<Integer> styleTypeSetFlags = new ArrayList<Integer>();
		styleTypeSetFlags.add(new Integer(0));
		bagOff = style != 0 ? res.getBagLocked(style, styleEnt,
				styleTypeSetFlags) : -1;
		tmp = styleTypeSetFlags.get(0) | styleBagTypeSetFlags.get(0);
		int endStyleEnt = styleEnt.size();
		int iStyle = 0;

		// Retrieve the XML attributes, if requested.
		int NX = (xmlParser != null) ? xmlParser.getAttributeCount() : 0;
		int ix = 0;
		int curXmlAttr = (xmlParser != null) ? xmlParser
				.getAttributeNameResID(ix) : 0;

		int kXmlBlock = 0x10000000;
		// Now iterate through all of the attributes that the client has requested,
		// filling in each with whatever data we can find.
		int block = 0;
		int indexOutValue = 0;
		ArrayList<Integer> typeSetFlags = new ArrayList<Integer>();
		typeSetFlags.add(new Integer(0));
		for (int ii = 0; ii < NI; ++ii) {
			int curIdent = attrs[ii];

			// Try to find a value for this attribute...  we prioritize values
			// coming from, first XML attributes, then XML style, then default
			// style, and finally the theme.
			value.dataType = TypedValue.TYPE_NULL;
			value.data = 0;
			typeSetFlags.set(0, new Integer(0));
			config.density = 0;

			// Skip through XML attributes until the end or the next possible match.
			while (ix < NX && curIdent > curXmlAttr) {
				ix++;
				curXmlAttr = xmlParser.getAttributeNameResID(ix);
			}

			// Retrieve the current XML attribute if it matches, and step to next.
			if (ix < NX && curIdent == curXmlAttr) {
				block = kXmlBlock;
				xmlParser.getAttributeValue(ix, value);
				ix++;
				curXmlAttr = xmlParser.getAttributeNameResID(ix);
			}

			// Skip through the style values until the end or the next possible match.
			while (iStyle < endStyleEnt
					&& curIdent > styleEnt.get(iStyle).map.name.ident) {
				iStyle++;
			}
            // Retrieve the current style attribute if it matches, and step to
            // next.
            if (iStyle < endStyleEnt
                        && curIdent == styleEnt.get(iStyle).map.name.ident) {
                if (value.dataType == TypedValue.TYPE_NULL) {
                    block = styleEnt.get(iStyle).stringBlock;
                    typeSetFlags.set(0, styleTypeSetFlags.get(0));
                    value.copyFrom(styleEnt.get(iStyle).map.value);
                }
                iStyle++;
            }

			// Skip through the default style values until the end or the next possible match.
			while (iDefStyle < endDefStyleEnt
					&& curIdent > defStyleEnt.get(iDefStyle).map.name.ident) {
				iDefStyle++;
			}
			// Retrieve the current default style attribute if it matches, and step to next.
			if (iDefStyle < endDefStyleEnt
					&& curIdent == defStyleEnt.get(iDefStyle).map.name.ident) {
				if (value.dataType == TypedValue.TYPE_NULL) {
					block = defStyleEnt.get(iDefStyle).stringBlock;
					typeSetFlags.set(0, defStyleTypeSetFlags.get(0));
					value.copyFrom(defStyleEnt.get(iDefStyle).map.value);
				}
				iDefStyle++;
			}

			ArrayList<Integer> resid = new ArrayList<Integer>();
			resid.add(new Integer(0));
			if (value.dataType != TypedValue.TYPE_NULL) {
				// Take care of resolving the found resource to its final value.
				int newBlock = theme.resolveAttributeReference(value, block,
						resid, typeSetFlags, config);
				if (newBlock >= 0)
					block = newBlock;
			} else {
				// If we still don't have a value for this attribute, try to find
				// it in the theme!
				int newBlock = theme
						.getAttribute(curIdent, value, typeSetFlags);
				if (newBlock >= 0) {
					newBlock = res.resolveReference(value, block, resid,
							typeSetFlags, config);
					if (newBlock == Errors.BAD_INDEX) {
						return false;
					}
					if (newBlock >= 0)
						block = newBlock;
				}
			}

			if (value.dataType == TypedValue.TYPE_REFERENCE && value.data == 0) {
				value.dataType = TypedValue.TYPE_NULL;
			}

			// Write the final value back to Java.
			outValues[indexOutValue + STYLE_TYPE] = value.dataType;
			outValues[indexOutValue + STYLE_DATA] = value.data;
			outValues[indexOutValue + STYLE_ASSET_COOKIE] = block != kXmlBlock ? res
					.getTableCookie(block) : -1;
			outValues[indexOutValue + STYLE_RESOURCE_ID] = resid.get(0);
			outValues[indexOutValue + STYLE_CHANGING_CONFIGURATIONS] = typeSetFlags
					.get(0);
			outValues[indexOutValue + STYLE_DENSITY] = config.density;

			if (indices != null && value.dataType != TypedValue.TYPE_NULL) {
				indicesIdx++;
				indices[indicesIdx] = ii;
			}

			indexOutValue += STYLE_NUM_ENTRIES;
		}

		if (indices != null)
			indices[0] = indicesIdx;

		return true;
	}

	//	private static void printBag(ArrayList<bag_entry> defStyleEnt) {
	//		System.out.println("printlnBag========================");
	//
	//		for (int i = 0; i < defStyleEnt.size(); ++i) {
	//			bag_entry cache = defStyleEnt.get(i);
	//			System.out.println(cache.map.value.toString());
	//		}
	//
	//		System.out.println("==================================");
	//	}

	final boolean retrieveAttributes(ResXMLParser xmlParser, int[] attrs,
			int[] outValues, int[] outIndices) {
		if (xmlParser == null || attrs == null || outValues == null
				|| am == null)
			return false;
		ResTable res = am.getResources(false);
		ResTable_config config = new ResTable_config();
		Res_value value = new Res_value();

		int NI = attrs.length;
		int NV = outValues.length;
		if (NV < NI * (STYLE_NUM_ENTRIES))
			return false;
		int NX = xmlParser.getAttributeCount();
		int ix = 0;
		int curXmlAttr = xmlParser.getAttributeNameResID(ix);
		int kXmlBlock = 0x10000000;
		int block = 0;
		ArrayList<Integer> typeSetFlags = new ArrayList<Integer>();
		typeSetFlags.add(new Integer(0));
		int offset = 0;
		int indicesIdx = 0;

		for (int ii = 0; ii < NI; ii++) {
			int curIdent = attrs[ii];
			// Try to find a value for this attribute...
			value.dataType = TypedValue.TYPE_NULL;
			value.data = 0;
			typeSetFlags.set(0, new Integer(0));
			//			config0.density = 0;

			// Skip through XML attributes until the end or the next possible match.
			while (ix < NX && curIdent > curXmlAttr) {
				ix++;
				curXmlAttr = xmlParser.getAttributeNameResID(ix);
			}
			// Retrieve the current XML attribute if it matches, and step to next.
			if (ix < NX && curIdent == curXmlAttr) {
				block = kXmlBlock;
				xmlParser.getAttributeValue(ix, value);
				ix++;
				curXmlAttr = xmlParser.getAttributeNameResID(ix);
			}

			//printf("Attribute 0x%08x: type=0x%x, data=0x%08x\n", curIdent, value.dataType, value.data);
			ArrayList<Integer> resid = new ArrayList<Integer>();
			resid.add(new Integer(0));
			if (value.dataType != TypedValue.TYPE_NULL) {
				// Take care of resolving the found resource to its final value.
				//printf("Resolving attribute reference\n");
				int newBlock = 0;

				newBlock = res.resolveReference(value, block, resid,
						typeSetFlags, config);

				if (newBlock == Errors.BAD_INDEX) {
					return false;
				}
				if (newBlock >= 0)
					block = newBlock;
			}
			// Deal with the special @null value -- it turns back to TYPE_NULL.
			if (value.dataType == TypedValue.TYPE_REFERENCE && value.data == 0) {
				value.dataType = TypedValue.TYPE_NULL;
			}

			//printf("Attribute 0x%08x: final type=0x%x, data=0x%08x\n", curIdent, value.dataType, value.data);

			// Write the final value back to Java.
			outValues[STYLE_TYPE + offset] = value.dataType;
			outValues[STYLE_DATA + offset] = value.data;
			outValues[STYLE_ASSET_COOKIE + offset] = block != kXmlBlock ? res
					.getTableCookie(block) : -1;
			outValues[STYLE_RESOURCE_ID + offset] = resid.get(0);
			outValues[STYLE_CHANGING_CONFIGURATIONS + offset] = typeSetFlags
					.get(0);
			outValues[STYLE_DENSITY + offset] = 0;

			if (outIndices != null && value.dataType != TypedValue.TYPE_NULL) {
				indicesIdx++;
				outIndices[indicesIdx] = ii;
			}

			offset += STYLE_NUM_ENTRIES;
		}

		if (outIndices != null) {
			outIndices[0] = indicesIdx;
		}

		return true;
	}

    final int getArraySize(int resource) {
        if (am == null)
            return 0;
        return am.getResources(false).getBagLocked(resource, null, null);
    }

    final int retrieveArray(int resource, int[] outValues) {
        if (am == null)
            return 0;
        if (outValues == null)
            return 0;
        ResTable res = am.getResources(false);
        ResTable_config config = new ResTable_config();
        Res_value value;
        int block;
        int NV = outValues.length;
        ArrayList<bag_entry> arrayEnt = new ArrayList<bag_entry>();
        ArrayList<Integer> arrayTypeSetFlags = new ArrayList<Integer>();
        arrayTypeSetFlags.add(new Integer(0));
        res.getBagLocked(resource, arrayEnt, arrayTypeSetFlags);
        int i = 0, j = 0;
        int indexOutValue = 0;
        ArrayList<Integer> typeSetFlags;
        while ((i < NV) && (j < arrayEnt.size())) {
            block = arrayEnt.get(j).stringBlock;
            typeSetFlags = arrayTypeSetFlags;
            config.density = 0;
            value = arrayEnt.get(j).map.value;

            ArrayList<Integer> resid = new ArrayList<Integer>();
            resid.add(new Integer(0));
            if (value.dataType != 0) {
                // Take care of resolving the found resource to its final value.
                // printf("Resolving attribute reference\n");
                int newBlock = res.resolveReference(value, block, resid,
                        typeSetFlags, config);
                if (newBlock == -1) {
                    return 0;
                }

                if (newBlock >= 0)
                    block = newBlock;
            }

            // Deal with the special @null value -- it turns back to TYPE_NULL.
            if (value.dataType == 1 && value.data == 0) {
                value.dataType = 0;
            }

            // printf("Attribute 0x%08x: final type=0x%x, data=0x%08x\n",
            // curIdent, value.dataType, value.data);

            // Write the final value back to Java.
            outValues[indexOutValue + STYLE_TYPE] = value.dataType;
            outValues[indexOutValue + STYLE_DATA] = value.data;
            outValues[indexOutValue + STYLE_ASSET_COOKIE] = res.getTableCookie(
                    block);
            outValues[indexOutValue + STYLE_RESOURCE_ID] = resid.get(0);
            outValues[indexOutValue + STYLE_CHANGING_CONFIGURATIONS] = typeSetFlags.get(0);
            outValues[indexOutValue + STYLE_DENSITY] = config.density;
            indexOutValue += STYLE_NUM_ENTRIES;
            i += STYLE_NUM_ENTRIES;
            j++;
        }

        i /= STYLE_NUM_ENTRIES;

        return i;
    }

	private int getStringBlockCount() {
		if (am == null)
			return 0;
		return am.getResources(false).getTableCount();
	}

	private ResStringPool getNativeStringBlock(int block) {
		if (am == null)
			return null;
		return am.getResources(false).getTableStringBlock(block);
	}

    public String getCookieName(int cookie) {
        int which = cookie - 1;
        if (which >= 0 && which < am.mAssetPaths.size()) {
            return am.mAssetPaths.get(which).path;
        }
        return null;
    }

	private ResXMLTree openXmlAssetNative(int cookie, String fileName) {
		if (am == null)
			return null;

		Asset a = (cookie == 0) ? am
				.openNonAsset(fileName, AssetManager.ACCESS_BUFFER) : am.openNonAsset(
				cookie, fileName, AssetManager.ACCESS_BUFFER);
		if (a == null)
			return null;
		ResXMLTree block = new ResXMLTree();
		int err = block.setTo(a.getBuffer(true), 0, a.getLength(), true);
		if (err != Errors.NO_ERROR) {
			return null;
		}
		return block;
	}

	/** may not support now *
	private native final String[] getArrayStringResource(int arrayRes);

	private native final int[] getArrayStringInfo(int arrayRes);

	native final int[] getArrayIntResource(int arrayRes);
	/**/

	private final void init() {
		am = new nativeAssetManager();
		am.addDefaultAssets();
	}

	//////////////////////////////////////////////////////////

    // jni AssetManager
    static final class nativeAssetManager {
        private ResTable mResources = null;
        private String mLocale = null;
		@SuppressWarnings("unused")
		private ResTable_config mConfig = new ResTable_config();
		private ArrayList<asset_path> mAssetPaths = new ArrayList<asset_path>();

		public boolean addDefaultAssets() {
			String kSystemAssets = "res_sys/framework-res.apk_FILES/";
			return addAssetPath(kSystemAssets, null);
		}

		public boolean addAssetPath(String path, ArrayList<Integer> cookie) {
			asset_path ap = new asset_path();
			ap.path = path;
			ap.type = Asset.kFileTypeDirectory;

			for (int i = 0; i < mAssetPaths.size(); ++i) {
				if (mAssetPaths.get(i).path.equals(ap.path)) {
					if (cookie != null) {
						cookie.set(0, new Integer(i + 1));
					}
					return true;
				}
			}

			mAssetPaths.add(ap);
			if (cookie != null) {
				cookie.set(0, new Integer(mAssetPaths.size()));
			}
			return true;
		}

		/*
		 * Set the current locale.  Use NULL to indicate no locale.
		 *
		 * Close and reopen Zip archives as appropriate, and reset cached
		 * information in the locale-specific sections of the tree.
		 */
		public void setLocale(String locale)
		{
		    this.setLocaleLocked(locale);
		}

        public void setLocaleLocked(String locale)
        {
            if (mLocale != null) {
                /* previously set, purge cached data */
                // purgeFileNameCacheLocked();
                // mZipSet.purgeLocale();
            }
            mLocale = locale;

            updateResourceParamsLocked();
        }

		public ResTable getResources(boolean required) {
			ResTable rt = getResTable(required);
			return rt;
		}

		public ResTable getResTable(boolean required) {
		    ResTable rt = mResources;
			if (rt != null)
				return rt;

			for (int i = 0; i < mAssetPaths.size(); ++i) {
				Asset ass = null;
				ResTable sharedRes = null;
				asset_path ap = mAssetPaths.get(i);
				if (i == 0) {
					sharedRes = ResTable.getSharedRes(ap.path);
					if (sharedRes == null) {
						Log.d(TAG, "load framework-res, this is the first and the last time");
						sharedRes = new ResTable();

                        Log.w(TAG, "System starts parsing framework-res.apk/resources.arsc at: "
                                + Time.getCurrentTime());

						Asset asset = openNonAssetInPathLocked(
								"resources.arsc", AssetManager.ACCESS_BUFFER, ap);
						sharedRes.add(asset, i + 1, false);

                        Log.w(TAG, "System finishes parsing framework-res.apk/resources.arsc at: "
                                + Time.getCurrentTime());

						ResTable.addShared(ap.path, sharedRes);
						if (rt == null)
							rt = sharedRes;
						return rt;
					}
				} else {
					ass = openNonAssetInPathLocked("resources.arsc",
							AssetManager.ACCESS_BUFFER, ap);
				}

				if (ass != null || sharedRes != null) {
                    if (rt == null) {
                        mResources = rt = new ResTable();
                        updateResourceParamsLocked();
                    }
					if (sharedRes != null)
						rt.add(sharedRes);
					else
						rt.add(ass, i + 1, false);
				}
			}

			if (rt == null)
				mResources = rt = new ResTable();
			return rt;
		}

		public Asset openNonAsset(String fileName, int mode) {
			int i = mAssetPaths.size();
			while (i > 0) {
				Asset pAsset = openNonAssetInPathLocked(fileName, mode,
						mAssetPaths.get(i));
				if (pAsset != null)
					return pAsset;
			}
			return null;
		}

		// TODO cookie
		public Asset openNonAsset(int cookie, String fileName, int mode) {
			int which = cookie - 1;
			if (which < mAssetPaths.size()) {
				Asset pAsset = openNonAssetInPathLocked(fileName, mode,
						mAssetPaths.get(which));
				if (pAsset != null)
					return pAsset;
			}
			return null;
		}

		public Asset openNonAssetInPathLocked(String fileName, int mode,
				asset_path ap) {
			Asset pAsset = null;
			if (ap.type == Asset.kFileTypeDirectory) {
				String path = ap.path + fileName;
				pAsset = openAssetFromFileLocked(path, mode);
				if (pAsset != null)
					pAsset.setAssetSource(path);
			}
			return pAsset;
		}

        public Asset openAssetFromFileLocked(String pathName, int mode) {
            Asset pAsset = null;
            pAsset = getCachedAsset(mAssetCache, pathName);

            if (pAsset != null) {
                ((_FileAsset) pAsset).mOffset = 0;
                return pAsset;
            }
            pAsset = Asset.createFromFile(pathName, mode);
            if (pAsset != null) {
                mAssetCache.put(pathName, (_FileAsset) pAsset);
            }
            return pAsset;
        }

        private _FileAsset getCachedAsset(HashMap<String, _FileAsset> mAssetCache, String fileName) {
            return mAssetCache.get(fileName);
        }

        private void clearCachedAsset(HashMap<String, _FileAsset> mAssetCache) {
            mAssetCache.clear();
        }

        public void setConfiguration(ResTable_config config, String locale) {
            mConfig = config;
            if (locale != null) {
                setLocaleLocked(locale);
            } else if (config.language[0] != 0) {
                char[] spec = new char[8];
                spec[0] = config.language[0];
                spec[1] = config.language[1];
                if (config.country[0] != 0) {
                    spec[2] = '_';
                    spec[3] = config.country[0];
                    spec[4] = config.country[1];
                    spec[5] = 0;
                } else {
                    spec[3] = 0;
                }
                 setLocaleLocked(spec.toString());
            } else {
                updateResourceParamsLocked();
            }
        }

        public void updateResourceParamsLocked()
        {
            ResTable res = mResources;
            if (res == null) {
                return;
            }

            int llen = mLocale != null ? mLocale.length() : 0;
            mConfig.language[0] = 0;
            mConfig.language[1] = 0;
            mConfig.country[0] = 0;
            mConfig.country[1] = 0;
            if (llen >= 2) {
                mConfig.language[0] = mLocale.charAt(0);
                mConfig.language[1] = mLocale.charAt(1);
            }
            if (llen >= 5) {
                mConfig.country[0] = mLocale.charAt(3);
                mConfig.country[1] = mLocale.charAt(4);
            }
            // mConfig.size = sizeof(*mConfig);

            res.setParameters(mConfig);
        }

		private static final class asset_path {
			public String path;
			public int type;
		}
	}

	private static AssetManager sSystem = null;

	private static void ensureSystemAssets() {
		if (sSystem == null) {
			AssetManager system = new AssetManager(true);
			system.addAssetPath("res_sys/framework-res.apk_FILES/");
			system.makeStringBlocks(false);
			sSystem = system;
		}
	}

	private AssetManager(boolean isSystem) {
		init();
	}

	public static AssetManager getSystem() {
		ensureSystemAssets();
		return sSystem;
	}

	public final XmlResourceParser openXmlResourceParser(int cookie,
			String fileName) {
		XmlBlock block = openXmlBlockAsset(cookie, fileName);
		XmlResourceParser rp = block.newParser();
		block.close();
		return rp;
	}

	// TODO 5.17
	public ResTable.Theme createTheme() {
        if (am == null)
            return null;
        return new ResTable.Theme(am.getResources(false));
	}

	public static void applyThemeStyle(ResTable.Theme mTheme, int resID,
			boolean force) {
		mTheme.applyStyle(resID, force);
	}

	public static void copyTheme(ResTable.Theme dest, ResTable.Theme source) {
	    dest.setTo(source);
	}

	public InputStream openNonAsset(int assetCookie, String string,
			int accessStreaming) {
		if (am == null)
			return null;
		Asset asset = am.openNonAsset(assetCookie, string, accessStreaming);
		return new AssetInputStream(asset);

	}

	public final class AssetInputStream extends InputStream {

		private Asset mAsset;

		public AssetInputStream(Asset asset) {
			mAsset = asset;
		}

		@Override
		public int read() throws IOException {
			return mAsset.read();
		}

        public final Asset getAssetInt() {
            return mAsset;
        }

        public final int available() throws IOException {
            long len = getAssetRemainingLength(mAsset);
            return len > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)len;
        }

        public final void reset() throws IOException {
        }

	}

	/**
     * Retrieve the string value associated with a particular resource
     * identifier for the current configuration / skin.
     */
    /*package*/ final CharSequence getResourceBagText(int ident, int bagEntryId) {
        synchronized (this) {
            TypedValue tmpValue = mValue;
            int block = loadResourceBagValue(ident, bagEntryId, tmpValue, true);
            if (block >= 0) {
                if (tmpValue.type == TypedValue.TYPE_STRING) {
                    return mStringBlocks[block].get(tmpValue.data);
                }
                return tmpValue.coerceToString();
            }
        }
        return null;
    }

    /**
     * Determine whether the state in this asset manager is up-to-date with
     * the files on the filesystem.  If false is returned, you need to
     * instantiate a new AssetManager class to see the new data.
     * {@hide}
     */
    public final boolean isUpToDate() {
        return true;
    }

    /**
     * Change the configuation used when retrieving resources.  Not for use by
     * applications.
     * {@hide}
     */
    public final void setConfiguration(int mcc, int mnc, String locale,
            int orientation, int touchscreen, int density, int keyboard,
            int keyboardHidden, int navigation, int screenWidth, int screenHeight,
            int screenLayout, int uiMode, int majorVersion) {
        if (am == null) {
            return;
        }

        ResTable_config config = new ResTable_config();
        config.mcc = (short) mcc;
        config.mnc = (short)mnc;
        config.orientation = (byte) orientation;
        config.touchscreen = (byte)touchscreen;
        config.density = (short)density;
        config.keyboard = (byte)keyboard;
        config.inputFlags = (byte)keyboardHidden;
        config.navigation = (byte)navigation;
        config.screenWidth = (short)screenWidth;
        config.screenHeight = (short)screenHeight;
        config.screenLayout = (byte)screenLayout;
        config.uiMode = (byte)uiMode;
        config.sdkVersion = (short)majorVersion;
        config.minorVersion = 0;
        am.setConfiguration(config, locale);
    }

    public final InputStream open(String fileName) throws IOException {
        return open(fileName, ACCESS_STREAMING);
    }

    /**
     * @j2sNative
     * console.log("Missing method: getLocales");
     */
    @MayloonStubAnnotation()
    public final String[] getLocales() {
        System.out.println("Stub" + " Function : getLocales");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: finalize");
     */
    @MayloonStubAnnotation()
    protected void finalize() {
        System.out.println("Stub" + " Function : finalize");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: openNonAsset");
     */
    @MayloonStubAnnotation()
    public final InputStream openNonAsset(String fileName) {
        System.out.println("Stub" + " Function : openNonAsset");
        return null;
    }

    public final InputStream open(String fileName, int accessMode) throws IOException {
        if (!mOpen) {
            throw new RuntimeException("Assetmanager has been closed");
        }
        String filedir = "bin/apps/"+ActivityThread.currentApplication().getPackageName() +"/assets/"+fileName;
        Asset asset = am.openAssetFromFileLocked(filedir, accessMode);
        if (asset != null) {
            AssetInputStream res = new AssetInputStream(asset);
            return res;
        }
        throw new FileNotFoundException("Asset file: " + fileName);
    }

    public final void close() {
        if (mOpen) {
            mOpen = false;
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: openNonAsset");
     */
    @MayloonStubAnnotation()
    public final InputStream openNonAsset(String fileName, int accessMode) {
        System.out.println("Stub" + " Function : openNonAsset");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: list");
     */
    @MayloonStubAnnotation()
    public final String[] list(String path) {
        System.out.println("Stub" + " Function : list");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: openNonAsset");
     */
    @MayloonStubAnnotation()
    public final InputStream openNonAsset(int cookie, String fileName) {
        System.out.println("Stub" + " Function : openNonAsset");
        return null;
    }

    private final long getAssetRemainingLength(Asset asset) {
        return asset.getAssetRemainingLength();
    }
}
