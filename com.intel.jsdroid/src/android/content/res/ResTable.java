package android.content.res;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.res.ResourceTypes.ResChunk_header;
import android.content.res.ResourceTypes.ResPointers;
import android.content.res.ResourceTypes.ResStringPool_ref;
import android.content.res.ResourceTypes.ResTable_config;
import android.content.res.ResourceTypes.ResTable_entry;
import android.content.res.ResourceTypes.ResTable_header;
import android.content.res.ResourceTypes.ResTable_map;
import android.content.res.ResourceTypes.ResTable_map_entry;
import android.content.res.ResourceTypes.ResTable_package;
import android.content.res.ResourceTypes.ResTable_ref;
import android.content.res.ResourceTypes.ResTable_type;
import android.content.res.ResourceTypes.ResTable_typeSpec;
import android.content.res.ResourceTypes.ResTable_value_entry;
import android.content.res.ResourceTypes.Res_value;
import android.util.Errors;
import android.util.Log;
import android.util.TypedValue;

public class ResTable {
    private static HashMap<String, ResTable> sharedRes = new HashMap<String, ResTable>();
    private static final String TAG = "ResTable";

	//	private static int[] caredTypeResouces = { 5, 6, 7, 8, 9, 13, 14 };

	static ResTable getSharedRes(String path) {
		return sharedRes.get(path);
	}

	static void addShared(String path, ResTable res) {
		sharedRes.put(path, res);
	}

	public ResTable() {
		for (int i = 0; i < 256; ++i)
			mPackageMap[i] = 0;
	}

	public ResTable(byte[] data, int offset, int size, int cookie,
			boolean copyData) {
		for (int i = 0; i < 256; ++i)
			mPackageMap[i] = 0;
	}

	public int getTableCount() {
		return mHeaders.size();
	}

	public ResStringPool getTableStringBlock(int index) {
		return mHeaders.get(index).values;
	}

	public int getTableCookie(int index) {
		return mHeaders.get(index).cookie;
	}

	public int add(Object inData, int offset, int size, int cookie,
			boolean copyData) {
		if (inData == null)
			return Errors.NO_ERROR;
		byte[] data = (byte[]) inData;
		Header header = new Header(this);
		header.index = mHeaders.size();
		header.cookie = cookie;
		mHeaders.add(header);

		int curPackage = 0;
		IntReader reader = new IntReader(data, offset, false);
		try {
			header.header = new ResTable_header(new ResChunk_header(data,
					reader.getPosition(), reader.readInt(2), reader.readInt(2),
					reader.readInt()), reader.readInt());
			header.size = header.header.header.size;
			Log.d("ResTable", "Loading ResTable...");
			if (header.header.header.headerSize > header.size
					|| header.size > size) {
				return (mError = Errors.BAD_TYPE);
			}
			header.dataEnd = header.header.header.pointer.offset + header.size;
			// TODO need to debug
			reader.setPosition(header.header.header.pointer.offset
					+ header.header.header.headerSize);
			ResChunk_header chunk = new ResChunk_header(data,
					reader.getPosition(), reader.readInt(2), reader.readInt(2),
					reader.readInt());
			while (chunk.pointer.offset <= (header.dataEnd - ResChunk_header
					.sizeof())
					&& chunk.pointer.offset <= (header.dataEnd - chunk.size)) {
				// TODO validate chunk
				int csize = chunk.size;
				int ctype = chunk.type;
				if (ctype == ResourceTypes.RES_STRING_POOL_TYPE) {
					if (header.values.getError() != Errors.NO_ERROR) {
//						System.err.println("before values.setTo: "
//								+ Time.getCurrentTime());
						int err = header.values.setTo(data,
								chunk.pointer.offset, csize, false);
//						System.err.println("after values.setTo: "
//								+ Time.getCurrentTime());
						if (err != Errors.NO_ERROR)
							return (mError = err);
					} else {
						System.out
								.println("Multiple string chunks found in resource table.");
					}
				} else if (ctype == ResourceTypes.RES_TABLE_PACKAGE_TYPE) {
					if (curPackage >= header.header.packageCount) {
						System.out
								.println("More package chunks were found than the "
										+ header.header.packageCount
										+ " declared in the header.");
						return (mError = Errors.BAD_TYPE);
					}
//					System.err.println("before parsePackage: "
//							+ Time.getCurrentTime());
					if (parsePackage(data, chunk.pointer.offset, header) != Errors.NO_ERROR) {
						return mError;
					}
//					System.err.println("after parsePackage: "
//							+ Time.getCurrentTime());
					curPackage++;
				} else {
					System.out.println("Unknown chunk type!");
				}
				reader.setPosition(chunk.pointer.offset + chunk.size);
				if (reader.getPosition() >= header.dataEnd)
					break;
				chunk = new ResChunk_header(data, reader.getPosition(),
						reader.readInt(2), reader.readInt(2), reader.readInt());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (curPackage < header.header.packageCount) {
			return (mError = Errors.BAD_TYPE);
		}
		mError = header.values.getError();
		if (mError != Errors.NO_ERROR)
			System.out.println("No string values found in resource table!");
		return mError;
	}

	public int parsePackage(byte[] data, int base, Header header) {
		try {
			IntReader reader = new IntReader(data, base, false);
			ResTable_package pkg = new ResTable_package(new ResChunk_header(
					data, reader.getPosition(), reader.readInt(2),
					reader.readInt(2), reader.readInt()), reader.readInt(),
					reader.readWCharArray(128), reader.readInt(),
					reader.readInt(), reader.readInt(), reader.readInt());
			int err = Errors.NO_ERROR;
			int pkgSize = pkg.header.size;

			if (pkg.typeStrings >= pkgSize) {
				System.out.println("ResTable_package type strings at "
						+ pkg.typeStrings + " are past chunk size " + pkgSize);
				return (mError = Errors.BAD_TYPE);
			}
			if ((pkg.typeStrings & 0x3) != 0) {
				System.out.println("ResTable_package type strings at "
						+ pkg.typeStrings + " is not on an integer boundary.");
				return (mError = Errors.BAD_TYPE);
			}
			if (pkg.keyStrings >= pkgSize) {
				System.out.println("ResTable_package key strings at "
						+ pkg.keyStrings + " are past chunk size " + pkgSize);
				return (mError = Errors.BAD_TYPE);
			}
			if ((pkg.keyStrings & 0x3) != 0) {
				System.out.println("ResTable_package key strings at "
						+ pkg.keyStrings + " is not on an integer boundary.");
				return (mError = Errors.BAD_TYPE);
			}

			Package _package = null;
			PackageGroup group = null;
			int id = pkg.id;
			if (id != 0 && id < 256) {
				_package = new Package(this, header, pkg);
				int idx = mPackageMap[id];
				if (idx == 0) {
					idx = mPackageGroups.size() + 1;
					group = new PackageGroup(this, pkg.name, id);
					err = _package.typeStrings.setTo(data, base
							+ pkg.typeStrings, header.dataEnd
							- (base + pkg.typeStrings), false);
					if (err != Errors.NO_ERROR)
						return (mError = err);
					err = _package.keyStrings.setTo(data,
							base + pkg.keyStrings, header.dataEnd
									- (base + pkg.keyStrings), false);
					if (err != Errors.NO_ERROR)
						return (mError = err);
					mPackageGroups.add(group);
					group.basePackage = _package;
					mPackageMap[id] = idx;
                } else {
                    group = mPackageGroups.get(idx - 1);
                    if (group == null) {
                        return (mError = Errors.UNKNOWN_ERROR);
                    }
                }

				if (false == group.packages.add(_package))
					return (mError = Errors.NO_MEMORY);
			} else {
				System.out.println("impossible here!");
				return Errors.NO_ERROR;
			}

			// Iterate through all chunks
			reader.setPosition(pkg.header.pointer.offset
					+ pkg.header.headerSize);
			ResChunk_header chunk = new ResChunk_header(data,
					reader.getPosition(), reader.readInt(2), reader.readInt(2),
					reader.readInt());
			int endPos = pkg.header.pointer.offset + pkg.header.size;
			while (chunk.pointer.offset <= (endPos - ResChunk_header.sizeof())
					&& chunk.pointer.offset <= (endPos - chunk.size)) {
				int csize = chunk.size;
				int ctype = chunk.type;
				if (ctype == ResourceTypes.RES_TABLE_TYPE_SPEC_TYPE) {
					ResTable_typeSpec typeSpec = new ResTable_typeSpec(chunk,
							reader.readInt(1), reader.readInt(1),
							reader.readInt(2), reader.readInt(4));
					// int typeSpecSize = typeSpec.header.size;
					if (typeSpec.id == 0) {
						System.out.println("ResTable_type has an id of 0.");
						return (mError = Errors.BAD_TYPE);
					}
					while (_package.types.size() < typeSpec.id) {
						_package.types.add(null);
					}
					Type t = _package.types.get(typeSpec.id - 1);
					//					System.out.println("public static final class "
					//							+ _package.typeStrings.get(typeSpec.id - 1)
					//							+ ", typeSpec.id = " + typeSpec.id);
					if (t == null) {
						t = new Type(header, _package, typeSpec.entryCount);
						_package.types.set(typeSpec.id - 1, t);
					} else {
						System.out
								.println("ResTable_typeSpec entry count inconsistent: given "
										+ typeSpec.entryCount
										+ ", previously "
										+ t.entryCount);
						return (mError = Errors.BAD_TYPE);
					}
					reader.setPosition(typeSpec.header.pointer.offset
							+ typeSpec.header.headerSize);
					t.typeSpecFlags = new IntArray(reader.getData(), reader.getPosition(), typeSpec.entryCount);
				} else if (ctype == ResourceTypes.RES_TABLE_TYPE_TYPE) {
					ResTable_type type = new ResTable_type(chunk,
							reader.readInt(1), reader.readInt(1),
							reader.readInt(2), reader.readInt(),
							reader.readInt());
					int typeSize = type.header.size;
					if (type.header.headerSize + 4 * type.entryCount > typeSize) {
						System.out
								.println("ResTable_type entry index to "
										+ (type.header.headerSize + 4 * type.entryCount)
										+ " extends beyond chunk end "
										+ typeSize);
						return (mError = Errors.BAD_TYPE);
					}
					if (type.entryCount != 0
							&& type.entriesStart > (typeSize - ResTable_entry
									.sizeof())) {
						System.out.println("ResTable_type entriesStart at "
								+ type.entriesStart
								+ " extends beyond chunk end " + typeSize);
						return (mError = Errors.BAD_TYPE);
					}

					if (type.id == 0) {
						System.out.println("ResTable_type has an id of 0.");
						return (mError = Errors.BAD_TYPE);
					}

					while (_package.types.size() < type.id) {
						_package.types.add(null);
					}
					Type t = _package.types.get(type.id - 1);
					if (t == null) {
						t = new Type(header, _package, type.entryCount);
						_package.types.set(type.id - 1, t);
					} else if (type.entryCount != t.entryCount) {
						System.out
								.println("ResTable_type entry count inconsistent: given "
										+ type.entryCount
										+ ", previously "
										+ t.entryCount);
						return (mError = Errors.BAD_TYPE);
					}

					//					boolean care = false;
					//					for (int i = 0; i < caredTypeResouces.length; ++i) {
					//						if (caredTypeResouces[i] == type.id) {
					//							care = true;
					//							break;
					//						}
					//					}
					//
					//					if (care == true) {
					type.config = this.readConfigFlags(reader);
					//					System.out.println(type.id + "'s config: "
					//							+ type.config.toString());
					if (type.config.language[0] == '\00'
							&& type.config.country[0] == '\00') {
						IntArray entryOffsets = new IntArray(data, reader.getPosition(), type.entryCount);

						reader.setPosition(reader.getPosition() + type.entryCount * 4);
						type.entryOffsets = entryOffsets;

						type.resPointers = new ResPointers(reader.getPosition(), data);

						t.configs.add(type);
					}
					//					}
				} else {
					// System.out.println("ResTable_package:unknown");
				}
				reader.setPosition(chunk.pointer.offset + csize);
				if (reader.getPosition() >= endPos)
					break;
				chunk = new ResChunk_header(data, reader.getPosition(),
						reader.readInt(2), reader.readInt(2), reader.readInt());
			}

			if (group.typeCount == 0)
				group.typeCount = _package.types.size();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Errors.NO_ERROR;
	}

	private ResTable_config readConfigFlags(IntReader mIn) throws IOException {
		int size = mIn.readInt();
		if (size < 28) {
			//	            throw new AndrolibException("Config size < 28");
			System.out.println("Config size < 28!");
			return null;
		}

		boolean isInvalid = false;

		short mcc = mIn.readShort();
		short mnc = mIn.readShort();

		char[] language = new char[] { (char) mIn.readByte(),
				(char) mIn.readByte() };
		char[] country = new char[] { (char) mIn.readByte(),
				(char) mIn.readByte() };

		byte orientation = mIn.readByte();
		byte touchscreen = mIn.readByte();
		short density = mIn.readShort();

		byte keyboard = mIn.readByte();
		byte navigation = mIn.readByte();
		byte inputFlags = mIn.readByte();
		mIn.skip(1);

		short screenWidth = mIn.readShort();
		short screenHeight = mIn.readShort();

		short sdkVersion = mIn.readShort();
		mIn.skip(2);

		byte screenLayout = 0;
		byte uiMode = 0;
		short smallestScreenWidthDp = 0;
		if (size >= 32) {
			screenLayout = mIn.readByte();
			uiMode = mIn.readByte();
			smallestScreenWidthDp = mIn.readShort();
		}

		short screenWidthDp = 0;
		short screenHeightDp = 0;
		if (size >= 36) {
			screenWidthDp = mIn.readShort();
			screenHeightDp = mIn.readShort();
		}

		int exceedingSize = size - KNOWN_CONFIG_BYTES;
		if (exceedingSize > 0) {
			//			byte[] buf = new byte[exceedingSize];
			//			mIn.readFully(buf);
			//			BigInteger exceedingBI = new BigInteger(1, buf);
			//
			//			if (exceedingBI.equals(BigInteger.ZERO)) {
			//	                LOGGER.fine(String.format(
			//	                    "Config flags size > %d, but exceeding bytes are all zero, so it should be ok.",
			//	                    KNOWN_CONFIG_BYTES));
			//			} else {
			//	                LOGGER.warning(String.format(
			//	                    "Config flags size > %d. Exceeding bytes: 0x%X.",
			//	                    KNOWN_CONFIG_BYTES, exceedingBI));
			//				isInvalid = true;
			//			}
			System.out.println("Impossible ?!");
		}

		return new ResTable_config(mcc, mnc, language, country, orientation,
				touchscreen, density, keyboard, navigation, inputFlags,
				screenWidth, screenHeight, sdkVersion, screenLayout, uiMode,
				smallestScreenWidthDp, screenWidthDp, screenHeightDp, isInvalid);
	}

	private static final int KNOWN_CONFIG_BYTES = 36;

	public int add(Asset asset, int cookie, boolean copyData) {
		byte[] data = asset.getBuffer(true);
		if (data == null)
			return Errors.UNKNOWN_ERROR;
		int size = asset.getLength();
		return add(data, 0, size, cookie, copyData);
	}

	public int add(ResTable src) {
		mError = src.mError;

		for (int i = 0; i < src.mHeaders.size(); i++) {
			mHeaders.add(src.mHeaders.get(i));
		}

		for (int i = 0; i < src.mPackageGroups.size(); i++) {
			PackageGroup srcPg = src.mPackageGroups.get(i);
			PackageGroup pg = new PackageGroup(this, srcPg.name, srcPg.id);
			for (int j = 0; j < srcPg.packages.size(); j++) {
				pg.packages.add(srcPg.packages.get(j));
			}
			pg.basePackage = srcPg.basePackage;
			pg.typeCount = srcPg.typeCount;
			mPackageGroups.add(pg);
		}

		for (int i = 0; i < MAX_PACKAGE_NUM; ++i) {
			System.arraycopy(src.mPackageMap, 0, mPackageMap, 0,
					MAX_PACKAGE_NUM);
		}

		return mError;
	}

	public int getError() {
		return mError;
	}

    public boolean getResourceName(int resID, resource_name outName) {
        if (resID < 0)
            return false;
        int p = getResourcePackageIndex(resID);
        int t = Res_GETTYPE(resID);
        int e = Res_GETENTRY(resID);
        if (p < 0 || t < 0)
            return false;
        PackageGroup grp = mPackageGroups.get(p);
        if (grp == null)
            return false;
        int ip = grp.packages.size();
        if (ip > 0) {
            Package curPackage = grp.packages.get(0);
            ArrayList<ResTable_type> type = new ArrayList<ResTable_type>();
            ArrayList<ResTable_entry> entry = new ArrayList<ResTable_entry>();
            int offset = getEntry(curPackage, t, e, null, type, entry,
                    null);
            if (offset <= 0) {
                return false;
            }
            outName.mPackage = getTrimString(grp.name);
            outName.mType = getTrimString(grp.basePackage.typeStrings.stringAt(t));
            outName.mName = getTrimString(grp.basePackage.keyStrings.stringAt(entry.get(0).key.index));
            return true;
        }
        return false;
    }

    public static final String getTrimString(String str) {
        for (int i = 0; str != null && i < str.length(); i++) {
            if (Integer.valueOf(str.charAt(i)) == 0) {
                return str.substring(0, i);
            }
        }
        return str;
    }

    public int identifierForName(String name, String type, String defPackage) {
        String realName, realType, realPackage;
        int packageEnd = -1;
        int typeEnd = -1;
        int nameEnd = name.length();
        int p = 0;
        if (name == null) {
            return Errors.BAD_INDEX;
        }
        while (p < nameEnd) {
            if (name.charAt(p) == ':')
                packageEnd = p;
            else if (name.charAt(p) == '/')
                typeEnd = p;
            p++;
        }

        if (name.contains(":") && name.contains("/")) {
            packageEnd = name.indexOf(':');
            typeEnd = name.indexOf('/');
            if (packageEnd > typeEnd) {
                return 0;
            }
            realPackage = name.substring(0, packageEnd);
            realType = name.substring(packageEnd + 1, typeEnd);
            realName = name.substring(typeEnd + 1, nameEnd);
        } else if ((!name.contains(":")) && name.contains("/")) {
            typeEnd = name.indexOf('/');
            realPackage = defPackage;
            realType = name.substring(0, typeEnd);
            realName = name.substring(typeEnd + 1, nameEnd);
        } else {
            realName = name;
            realType = type;
            realPackage = defPackage;
        }

        int NG = mPackageGroups.size();
        for (int ig = 0; ig < NG; ig++) {
            PackageGroup group = mPackageGroups.get(ig);
            String groupname = getTrimString(group.name);
            if (realPackage != null && !realPackage.equals(groupname)) {
                continue;
            }

            int ti = group.basePackage.typeStrings.indexOfString(realType, realType.length());
            if (ti < 0) {
                continue;
            }

            int ei = group.basePackage.keyStrings.indexOfString(realName, realName.length());
            if (ei < 0) {
                continue;
            }

            Type typeConfigs = group.packages.get(0).getType(ti);
            if (typeConfigs == null || typeConfigs.configs.size() <= 0) {
                Log.w(TAG, String.format(
                        "Expected type structure not found in package %s for idnex %d\n",
                        group.name, ti));
            }
            int NTC = typeConfigs.configs.size();
            for (int tci = 0; tci < NTC; tci++) {
                ResTable_type ty = typeConfigs.configs.get(tci);
                if (ty == null)
                    continue;
                int NE = ty.entryCount;
                for (int i = 0; i < NE; i++) {
                    resource_name outName = new resource_name(null,null,null);
                    int entryResID = (group.id << 24) | (((ti + 1) & 0xFF) << 16) | (i & 0xFFFF);
                    this.getResourceName(entryResID, outName);
                    if (outName != null && outName.mPackage!=null && outName.mPackage.equals(realPackage)) {
                        if (outName.mType !=null && outName.mName != null && outName.mType.equals(realType) && outName.mName.equals(realName)) {
                              return entryResID;
                        }
                    }
                }
            }
        }

        return 0;
    }

	// TODO change to use getEntry
	public int getResource(int resID, Res_value outValue, boolean mayBeBag,
			ArrayList<Integer> outSpecFlags, ResTable_config outConfig) {
	    if (resID < 0 ) return Errors.BAD_INDEX;
		int p = getResourcePackageIndex(resID);
		int t = Res_GETTYPE(resID);
		int e = Res_GETENTRY(resID);

		if (p < 0 || t < 0)
			return Errors.BAD_INDEX;

		Res_value bestValue = null;
		Package bestPackage = null;
		ResTable_config bestItem = new ResTable_config();

		if (outSpecFlags != null)
			outSpecFlags.set(0, new Integer(0));

		PackageGroup grp = mPackageGroups.get(p);
		if (grp == null)
			return Errors.BAD_INDEX;
		int ip = grp.packages.size();
		while (ip > 0) {
			ip--;

			Package curPackage = grp.packages.get(ip);
			ArrayList<ResTable_type> type = new ArrayList<ResTable_type>();
			ArrayList<ResTable_entry> entry = new ArrayList<ResTable_entry>();
			ArrayList<Type> typeClass = new ArrayList<Type>();
			int offset = getEntry(curPackage, t, e, mParams, type, entry,
					typeClass);
			if (offset <= 0) {
				if (offset < 0)
					return offset;
				continue;
			}

			if ((entry.get(0).flags & ResTable_entry.FLAG_COMPLEX) != 0) {
				//				System.out.println("Requesting resource " + resID
				//						+ "failed because it is complex.");
				continue;
			}

			if (offset > (type.get(0).header.size - Res_value.sizeof()))
				return Errors.BAD_TYPE;

			Res_value item = ((ResTable_value_entry) entry.get(0)).value;
			ResTable_config thisConfig = type.get(0).config;

			if (outSpecFlags != null) {
				if (typeClass.get(0).typeSpecFlags != null) {
					int old = outSpecFlags.get(0);
					outSpecFlags
							.set(0, old | typeClass.get(0).typeSpecFlags.getEntry(e));
				} else {
					outSpecFlags.set(0, -1);
				}
			}

			bestValue = item;
			bestItem = thisConfig;
			bestPackage = curPackage;
			if (bestValue != null)
				break;
		}

		if (bestValue != null) {
			outValue.copyFrom(bestValue);
			if (outConfig != null)
				outConfig.copyFrom(bestItem);
			return bestPackage.header.index;
		}

		return Errors.BAD_VALUE;
		//		int pkgId = (resID & 0xff000000) >> 24;
		//		int typeId = (resID & 0x00ff0000) >> 16;
		//		int elementId = resID & 0x0000ffff;
		//		int idx = mPackageMap[pkgId] - 1;
		//
		//		Package pkg = mPackageGroups.get(idx).basePackage;
		//		ArrayList<Res_value> cache = null;
		//		for (int i = 0; i < pkg.types.size(); ++i) {
		//			Type t = pkg.getType(i);
		//			if (t.typeSpec.id == typeId) {
		//				for (int j = 0; j < t.configs.size(); ++j) {
		//					cache = t.configs.get(j).resources;
		//					if (cache.size() > elementId) {
		//						if (null != cache.get(elementId)) {
		//							//							System.out.println("in this config, " + elementId
		//							//									+ " is found not null");
		//							outValue.copyFrom(cache.get(elementId));
		//							return pkg.header.index;
		//						} else {
		//							//							System.out.println("in this config, " + elementId
		//							//									+ " is null");
		//						}
		//					}
		//				}
		//			}
		//		}
	}

	public int getResource(ResTable_ref res, Res_value outValue,
			ArrayList<Integer> outSpecFlags) {
		return getResource(res.ident, outValue, false, outSpecFlags, null);
	}

	public int resolveReference(Res_value value, int blockIndex,
			ArrayList<Integer> outLastRef,
			ArrayList<Integer> inoutTypeSpecFlags, ResTable_config outConfig) {
		int count = 0;
		while (blockIndex >= 0 && value.dataType == TypedValue.TYPE_REFERENCE
				&& value.data != 0 && count < 20) {
			if (outLastRef != null) {
				outLastRef.set(0, new Integer(value.data));
			}
			ArrayList<Integer> newFlags = new ArrayList<Integer>();
			newFlags.add(new Integer(0));
			int newIndex = getResource(value.data, value, true, newFlags,
					outConfig);
			if (newIndex == Errors.BAD_INDEX)
				return Errors.BAD_INDEX;
			if (inoutTypeSpecFlags != null)
				inoutTypeSpecFlags.set(0, newFlags.get(0));
			if (newIndex < 0)
				return blockIndex;
			blockIndex = newIndex;
			count++;
		}

		return blockIndex;
	}

	// TODO 5.3
	public int getResourcePackageIndex(int resID) {
		return mPackageMap[Res_GETPACKAGE(resID) + 1] - 1;
	}

	// TODO 5.3
	public int getBagLocked(int resID, ArrayList<bag_entry> outBag,
			ArrayList<Integer> outTypeSpecFlags) {
        if (resID < 0) {
            return Errors.BAD_INDEX;
        }

		if (mError != Errors.NO_ERROR)
			return mError;

		int p = getResourcePackageIndex(resID);
		int t = Res_GETTYPE(resID);
		int e = Res_GETENTRY(resID);

		if (p < 0 || t < 0)
			return Errors.BAD_INDEX;

		PackageGroup grp = mPackageGroups.get(p);
		if (grp == null)
			return Errors.BAD_INDEX;
		if (t >= grp.typeCount)
			return Errors.BAD_INDEX;

		// what's the definition of basePackage??
		Package basePackage = grp.packages.get(0);
		Type typeConfigs = basePackage.getType(t);
		int NENTRY = typeConfigs.entryCount;
		if (e >= NENTRY)
			return Errors.BAD_INDEX;

		if (grp.bags != null) {
			Bag[] typeSet = grp.bags[t];
			if (typeSet != null) {
				Bag set = typeSet[e];
				if (set != null) {
					// TODO isFFFFFFFF need to be test
					if (set.isFFFFFFFF != true) {
						if (outTypeSpecFlags != null)
							outTypeSpecFlags.set(0,
									set.mBagHeader.typeSpecFlags);
                        for (int i = 0; i < set.mBagEntries.size(); ++i) {
                            if (outBag != null) {
                                outBag.add(set.mBagEntries.get(i));
                            }
                        }
						return set.mBagHeader.numAttrs;
					}
					return Errors.BAD_INDEX;
				}
			}
		}

		if (grp.bags == null) {
			grp.bags = new Bag[grp.typeCount][];
			for (int i = 0; i < grp.typeCount; ++i)
				grp.bags[i] = null;
		}

		Bag[] typeSet = grp.bags[t];
		if (typeSet == null) {
			typeSet = new Bag[NENTRY];
			for (int i = 0; i < NENTRY; ++i)
				typeSet[i] = null;
			grp.bags[t] = typeSet;
		}

		// This is what we are building
		Bag set = null;
		Bag badBag = new Bag();
		//                                           ,                                          ,
		//                                           
		badBag.isFFFFFFFF = true;
		typeSet[e] = badBag;
		int ip = grp.packages.size();
		while (ip > 0) {
			ip--;

			Package _package = grp.packages.get(ip);
			// TODO heavy class had better to use ArrayList? don't need to copy
			ArrayList<ResTable_type> type = new ArrayList<ResTable_type>();
			ArrayList<ResTable_entry> entry = new ArrayList<ResTable_entry>();
			ArrayList<Type> typeClass = new ArrayList<Type>();
			// entry               ResTable_entry                        
			int offset = getEntry(_package, t, e, mParams, type, entry,
					typeClass);
			if (offset <= 0) {
				if (offset < 0)
					return offset;
				continue;
			}
			if ((entry.get(0).flags & ResTable_entry.FLAG_COMPLEX) == 0)
				continue;
			//                      size                     
			int entrySize = entry.get(0).size;
			//                sizeof            instance_sizeof         entry         map_entry
			int parent = entrySize >= ResTable_map_entry.sizeof() ? ((ResTable_map_entry) entry
					.get(0)).parent.ident : 0;
			int count = entrySize >= ResTable_map_entry.sizeof() ? ((ResTable_map_entry) entry
					.get(0)).count : 0;
			int N = count;
			if (set == null) {
				if (parent != 0) {
					ArrayList<bag_entry> parentBag = new ArrayList<bag_entry>();
					ArrayList<Integer> parentTypeSpecFlags = new ArrayList<Integer>();
					parentTypeSpecFlags.add(new Integer(0));
					int NP = getBagLocked(parent, parentBag,
							parentTypeSpecFlags);
					int NT = ((NP >= 0) ? NP : 0) + N;
					set = new Bag();
					set.mBagEntries = new ArrayList<bag_entry>(NT);
					for (int i = 0; i < NT; ++i)
						set.mBagEntries.add(new bag_entry());
					if (NP > 0) {
						for (int i = 0; i < NP; ++i) {
                            set.mBagEntries.set(i, new bag_entry(parentBag.get(i)));
						}
						set.mBagHeader.numAttrs = NP;
					} else {
						set.mBagHeader.numAttrs = 0;
					}
					set.mBagHeader.availAttrs = NT;
					set.mBagHeader.typeSpecFlags = parentTypeSpecFlags.get(0);
				} else {
					set = new Bag();
					set.mBagEntries = new ArrayList<bag_entry>(N);
					for (int i = 0; i < N; ++i)
						set.mBagEntries.add(new bag_entry());
					set.mBagHeader = new bag_set(0, N, 0);
				}
			}

			if (typeClass.get(0).typeSpecFlags != null) {
				set.mBagHeader.typeSpecFlags |= typeClass.get(0).typeSpecFlags.getEntry(e);
			} else {
				set.mBagHeader.typeSpecFlags = -1;
			}

			// Now merge in the new attributes
			int curOff = offset;
			ResTable_map map = null;
			ArrayList<bag_entry> entries = set.mBagEntries;
			int curEntry = 0;
			int pos = 0;
			while (pos < count) {
				if (curOff > (type.get(0).header.size - ResTable_map.sizeof())) {
					return Errors.BAD_TYPE;
				}
				// TODO need to test
				map = ((ResTable_map_entry) entry.get(0)).entries[pos];
				N++;
				int newName = map.name.ident;
				boolean isInside;
				int oldName = 0;
				while ((isInside = (curEntry < set.mBagHeader.numAttrs))
						&& (oldName = entries.get(curEntry).map.name.ident) < newName) {
					curEntry++;
				}
				if ((isInside == false) || oldName != newName) {
					// This is a new attribute...  figure out what to do with it
					if (set.mBagHeader.numAttrs >= set.mBagHeader.availAttrs) {
						// need to alloc more memory...
						int newAvail = set.mBagHeader.availAttrs + N;
						for (int i = 0; i < N; ++i) {
							set.mBagEntries.add(null);
						}
						set.mBagHeader.availAttrs = newAvail;
						entries = set.mBagEntries;
					}
					if (isInside == true) {
						entries.add(curEntry, new bag_entry());
						set.mBagHeader.numAttrs++;
					}
					//					System.out.println("Inserting new attribute at position: "
					//							+ curEntry);
				} else {
					//					System.out.println("Replacing new attribute");
				}
				bag_entry cur = entries.get(curEntry);
				if (cur == null) {
					System.out.println("Shit!");
				}
				cur.stringBlock = _package.header.index;
				cur.map.name.ident = newName;
				cur.map.value.copyFrom(map.value);
				curEntry++;
				pos++;
				int size = map.value.size;
				curOff += size + ResTable_map.sizeof() - Res_value.sizeof();
			}
			if (curEntry > set.mBagHeader.numAttrs) {
				set.mBagHeader.numAttrs = curEntry;
			}
		}

		typeSet[e] = set;
		if (set != null) {
			if (outTypeSpecFlags != null)
				outTypeSpecFlags.set(0, set.mBagHeader.typeSpecFlags);
            for (int i = 0; i < set.mBagEntries.size(); ++i) {
                if (outBag != null) {
                    outBag.add(set.mBagEntries.get(i));
                }
            }
			return set.mBagHeader.numAttrs;
		}
		return Errors.BAD_INDEX;
	}

    public void setParameters(ResTable_config params)
    {
        mParams = params;
        for (int i = 0; i < mPackageGroups.size(); i++) {
            Log.i(TAG, "CLEARING BAGS FOR GROUP" + i + "!");
            mPackageGroups.get(i).clearBagCache();
        }
    }

    public void getParameters(ResTable_config params)
    {
        params = mParams;
    }

    public int getEntry(Package _package, int typeIndex, int entryIndex,
            ResTable_config config, ArrayList<ResTable_type> outType,
            ArrayList<ResTable_entry> outEntry, ArrayList<Type> outTypeClass) {
        // ResTable_package pkg = _package.mPackage;
        Type allTypes = _package.getType(typeIndex);
        if (allTypes == null)
            return 0;
        if (entryIndex >= allTypes.entryCount)
            return Errors.BAD_TYPE;

        ResTable_type type = null;
        int offset = ResTable_type.NO_ENTRY;
        ResTable_config bestConfig = new ResTable_config();
        int NT = allTypes.configs.size();
        for (int i = 0; i < NT; ++i) {
            ResTable_type thisType = allTypes.configs.get(i);
            if (thisType == null)
                continue;
            ResTable_config thisConfig = thisType.config;

            // Check to make sure this one is valid for the current parameters.
//            if (config && !thisConfig.match(*config)) {
//                Log.i(TAG, "Does not match config!");
//                continue;
//            }

            // Check if there is the desired entry in this type.
            int thisOffset = ResTable_type.NO_ENTRY;
            if (thisType.entryOffsets != null)
                thisOffset = thisType.entryOffsets.getEntry(entryIndex);
            if (thisOffset == ResTable_type.NO_ENTRY) {
//                Log.i(TAG, "Skipping because it is not defined!");
                continue;
            }

            if (type != null) {
                // Check if this one is less specific than the last found.  If so,
                // we will skip it.  We check starting with things we most care
                // about to those we least care about.
                if (!thisConfig.isBetterThan(bestConfig, config)) {
                    Log.i(TAG, "This config is worse than last!");
                    continue;
                }
            }

            type = thisType;
            offset = thisOffset;
            bestConfig = thisConfig;
//            Log.i(TAG, "Best entry so far -- using it!");
            if (config == null)
                break;
        }

        if (type == null) {
            Log.e(TAG, "No value found for requested entry!");
            return Errors.BAD_INDEX;
        }

        ResTable_entry entry = type.resources.get(offset);
        if (entry == null) {
            try {
                Res_value res = null;
                IntReader reader = new IntReader(type.resPointers.data, type.resPointers.base + offset, false);
                int size = reader.readInt(2);
                int flags = reader.readInt(2);
                int specNamesId = reader.readInt();
                ResTable_entry newEntry = null;
                if ((flags & ResourceTypes.ENTRY_FLAG_COMPLEX) == 0) {
                    res = new Res_value(reader.readInt(2), reader.readByte(),
                            reader.readByte(), reader.readInt());
                    newEntry = new ResTable_value_entry(size, flags,
                            new ResStringPool_ref(specNamesId));
                    ((ResTable_value_entry) newEntry).value.copyFrom(res);
                } else {
                    /**
                     * complex value                   style, array, plurals
                     *                                         <type> <item
                     * name="xxx">xxx</item> </type>
                     * ResTable_map_entry
                     */
                    int ident = reader.readInt();
                    int count = reader.readInt();
                    // if (count > 0) {
                    // System.out.println("count: " + count);
                    // }
                    newEntry = new ResTable_map_entry(size, flags,
                            new ResStringPool_ref(specNamesId),
                            new ResTable_ref(ident), count);
                    ResTable_map[] items = new ResTable_map[count];

                    for (int k = 0; k < count; ++k) {
                        items[k] = new ResTable_map(new ResTable_ref(
                                reader.readInt()), new Res_value(
                                reader.readInt(2), reader.readInt(1),
                                reader.readInt(1), reader.readInt()));
                    }

                    ((ResTable_map_entry) newEntry).entries = items;
                }
                type.resources.put(offset, newEntry);
                entry = newEntry;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        outType.add(type);
        outEntry.add(entry);
        if (outTypeClass != null) {
            outTypeClass.add(allTypes);
        }

        return offset + entry.size;
    }

	public static final class resource_name {
		public String mPackage;
		public String mType;
		public String mName;

		public resource_name(String _package, String _type, String _name) {
			mPackage = _package;
			mType = _type;
			mName = _name;
		}
	}

	public int mError = Errors.NO_INIT;
	public ArrayList<Header> mHeaders = new ArrayList<Header>();
	public ArrayList<PackageGroup> mPackageGroups = new ArrayList<PackageGroup>();
	private static final int MAX_PACKAGE_NUM = 256;
	private static final int Res_MAXPACKAGE = 255;
	public int[] mPackageMap = new int[MAX_PACKAGE_NUM];
	public ResTable_config mParams = new ResTable_config();

	static int Res_GETPACKAGE(int id) {
		return ((id >> 24) - 1);
	}

	static int Res_GETTYPE(int id) {
		return (((id >> 16) & 0xFF) - 1);
	}

	static int Res_GETENTRY(int id) {
		return (id & 0xffff);
	}

	// TODO 5.3
	public static final class Theme {
		public Theme(ResTable table) {
			mTable = table;
			for (int i = 0; i < RES_MAXPACKAGE; ++i) {
				mPackages[i] = null;
			}
		}

		public ResTable getResTable() {
			return mTable;
		}

		public int applyStyle(int resID, boolean force) {
			// true list
			ArrayList<bag_entry> bag = new ArrayList<bag_entry>();
			// fake list, almost every Integer ArrayList is a fake one here
			ArrayList<Integer> bagTypeSpecFlags = new ArrayList<Integer>();
			bagTypeSpecFlags.add(new Integer(0));
			int N = mTable.getBagLocked(resID, bag, bagTypeSpecFlags);
			if (N < 0)
				return N;

			int curPackage = 0xffffffff;
			int curPackageIndex = 0;
			package_info curPI = null;
			int curType = 0xffffffff;
			int numEntries = 0;
			theme_entry[] curEntries = null;

			int i = 0;
			while (i < N) {
				bag_entry cache = bag.get(i);
				int attrRes = cache.map.name.ident;
				int p = Res_GETPACKAGE(attrRes);
				int t = Res_GETTYPE(attrRes);
				int e = Res_GETENTRY(attrRes);

				if (curPackage != p) {
					int pidx = mTable.getResourcePackageIndex(attrRes);
					if (pidx < 0) {
						i++;
						continue;
					}
					curPackage = p;
					curPackageIndex = pidx;
					curPI = mPackages[pidx];
					if (curPI == null) {
						PackageGroup grp = mTable.mPackageGroups.get(pidx);
						int cnt = grp.typeCount;
						curPI = new package_info();
						curPI.types = new type_info[cnt];
						for (int k = 0; k < cnt; ++k)
							curPI.types[k] = new type_info();
						curPI.numTypes = cnt;
						mPackages[pidx] = curPI;
					}
					curType = 0xffffffff;
				}
				if (curType != t) {
					if (t >= curPI.numTypes) {
						i++;
						continue;
					}
					curType = t;
					curEntries = curPI.types[t].entries;
					if (curEntries == null) {
						PackageGroup grp = mTable.mPackageGroups
								.get(curPackageIndex);
						Type type = grp.packages.get(0).getType(t);
						int cnt = type != null ? type.entryCount : 0;
						curEntries = new theme_entry[cnt];
						for (int k = 0; k < cnt; ++k)
							curEntries[k] = new theme_entry();
						curPI.types[t].numEntries = cnt;
						curPI.types[t].entries = curEntries;
					}
					numEntries = curPI.types[t].numEntries;
				}
				if (e >= numEntries) {
					i++;
					continue;
				}
				theme_entry curEntry = curEntries[e];
				if (force || curEntry.value.dataType == TypedValue.TYPE_NULL) {
					curEntry.stringBlock = bag.get(i).stringBlock;
					curEntry.typeSpecFlags |= bagTypeSpecFlags.get(0);
					curEntry.value.copyFrom(bag.get(i).map.value);
				}

				i++;
			}

			return Errors.NO_ERROR;
		}

		public int setTo(Theme other) {
		    //LOGI("Setting theme %p from theme %p...\n", this, &other);
		    //dumpToLog();
		    //other.dumpToLog();

		    if (mTable == other.mTable) {
		        for (int i = 0; i < Res_MAXPACKAGE; i++) {
		            if (mPackages[i] != null) {
		                mPackages[i] = null;
		            }
		            if (other.mPackages[i] != null) {
		                mPackages[i] = copy_package(other.mPackages[i]);
		            } else {
		                mPackages[i] = null;
		            }
		        }
		    } else {
		        // @todo: need to really implement this, not just copy
		        // the system package (which is still wrong because it isn't
		        // fixing up resource references).
		        for (int i = 0; i < Res_MAXPACKAGE; i++) {
		            if (mPackages[i] != null) {
		                mPackages[i] = null;
		            }
		            if (i == 0 && other.mPackages[i] != null) {
		                mPackages[i] = copy_package(other.mPackages[i]);
		            } else {
		                mPackages[i] = null;
		            }
		        }
		    }

		    //LOGI("Final theme:");
		    //dumpToLog();

		    return 0;
		}

		public int getAttribute(int resID, Res_value outValue,
				ArrayList<Integer> outTypeSpecFlags) {
			//			System.out.println("getAttribute not tested");
			int cnt = 20;

			if (outTypeSpecFlags != null)
				outTypeSpecFlags.set(0, new Integer(0));

			do {
				int p = mTable.getResourcePackageIndex(resID);
				int t = Res_GETTYPE(resID);
				int e = Res_GETENTRY(resID);

				//		        TABLE_THEME(LOGI("Looking up attr 0x%08x in theme %p", resID, this));

				if (p >= 0) {
					package_info pi = mPackages[p];
					//		            TABLE_THEME(LOGI("Found package: %p", pi));
					if (pi != null) {
						//		                TABLE_THEME(LOGI("Desired type index is %ld in avail %d", t, pi->numTypes));
						if (t < pi.numTypes) {
							type_info ti = pi.types[t];
							//		                    TABLE_THEME(LOGI("Desired entry index is %ld in avail %d", e, ti.numEntries));
							if (e < ti.numEntries) {
								theme_entry te = ti.entries[e];
								if (outTypeSpecFlags != null) {
									outTypeSpecFlags.set(0,
											outTypeSpecFlags.get(0)
													| te.typeSpecFlags);
								}
								//		                        TABLE_THEME(LOGI("Theme value: type=0x%x, data=0x%08x",
								//		                                te.value.dataType, te.value.data));
								int type = te.value.dataType;
								if (type == TypedValue.TYPE_ATTRIBUTE) {
									if (cnt > 0) {
										cnt--;
										resID = te.value.data;
										continue;
									}
									//		                            LOGW("Too many attribute references, stopped at: 0x%08x\n", resID);
									return Errors.BAD_INDEX;
								} else if (type != TypedValue.TYPE_NULL) {
									outValue.copyFrom(te.value);
									return te.stringBlock;
								}
								return Errors.BAD_INDEX;
							}
						}
					}
				}
				break;

			} while (true);

			return Errors.BAD_INDEX;
		}

		public int resolveAttributeReference(Res_value inOutValue,
				int blockIndex, ArrayList<Integer> outLastRef,
				ArrayList<Integer> inoutTypeSpecFlags,
				ResTable_config inoutConfig) {
			//			System.out.println("resolveAttributeReference not tested");
			if (inOutValue.dataType == TypedValue.TYPE_ATTRIBUTE) {
				ArrayList<Integer> newTypeSpecFlags = new ArrayList<Integer>();
				newTypeSpecFlags.add(new Integer(0));
				blockIndex = getAttribute(inOutValue.data, inOutValue,
						newTypeSpecFlags);
				if (inoutTypeSpecFlags != null)
					inoutTypeSpecFlags.set(0, inoutTypeSpecFlags.get(0)
							| newTypeSpecFlags.get(0));
				if (blockIndex < 0)
					return blockIndex;
			}
			return mTable.resolveReference(inOutValue, blockIndex, outLastRef,
					inoutTypeSpecFlags, inoutConfig);
		}

		public void dumpToLog() {

		}

		public static final int RES_MAXPACKAGE = 255;
		private ResTable mTable = null;
		private package_info[] mPackages = new package_info[RES_MAXPACKAGE];

		private static final class theme_entry {
			int stringBlock;
			int typeSpecFlags;
			Res_value value = new Res_value();

			public void copyFrom(theme_entry entry) {
				stringBlock = entry.stringBlock;
				typeSpecFlags = entry.typeSpecFlags;
				value.copyFrom(entry.value);
			}
		}

		private static final class type_info {
            type_info() {
                this.numEntries = 0;
                this.entries = null;
            }

            int numEntries;
            theme_entry[] entries;
        }

		private static final class package_info {
			int numTypes;
			type_info[] types = null;
		}

		package_info copy_package(package_info pi) {
			package_info newpi = new package_info();
			newpi.types = new type_info[pi.numTypes];
			for (int i = 0; i < pi.numTypes; i++) {
			    newpi.types[i] = new type_info();
			}
			newpi.numTypes = pi.numTypes;
			for (int j = 0; j < newpi.numTypes; j++) {
				int cnt = pi.types[j].numEntries;
				newpi.types[j].numEntries = cnt;
				theme_entry[] te = pi.types[j].entries;
				if (te != null) {
					theme_entry[] newte = new theme_entry[cnt];
					for (int i = 0; i < cnt; i++) {
					    newte[i] = new theme_entry();
					}
					newpi.types[j].entries = newte;
					for (int i = 0; i < cnt; ++i) {
						newte[i].copyFrom(te[i]);
					}
				} else {
					newpi.types[j].entries = null;
				}
			}
			return newpi;
		}
	}

	private static final class Header {
		public ResTable_header header = null;
		public int size;
		public int dataEnd;
		public int index;
		public int cookie; // type unknown
		public ResStringPool values = new ResStringPool();

		public Header(ResTable _owner) {
		}
	}

	private static final class Type {
		public int entryCount;
		public IntArray typeSpecFlags;
		public ArrayList<ResTable_type> configs = new ArrayList<ResTable_type>();

		public Type(Header _header, Package _package, int count) {
			entryCount = count;
		}
	}

	private static final class Package {
		public Header header;
		// dependency loop, will cause error in j2s?
		public ArrayList<Type> types = new ArrayList<Type>();
		public ResStringPool typeStrings = new ResStringPool();
		public ResStringPool keyStrings = new ResStringPool();

		public Package(ResTable _owner, Header _header,
				ResTable_package _package) {
			header = _header;
		}

		public Type getType(int idx) {
			return idx < types.size() ? types.get(idx) : null;
		}
	}

	public static final class bag_set {
		public int numAttrs = 0;
		public int availAttrs = 0;
		public int typeSpecFlags = 0;

		public bag_set(int _numAttrs, int _availAttrs, int _typeSpecFlags) {
			numAttrs = _numAttrs;
			availAttrs = _availAttrs;
			typeSpecFlags = _typeSpecFlags;
		}

		public bag_set() {
		}
	}

    public static final class bag_entry {
        int stringBlock;
        ResTable_map map;

        public bag_entry(bag_entry _bag_entry) {
            stringBlock = _bag_entry.stringBlock;
            map = new ResTable_map(_bag_entry.map.name, _bag_entry.map.value);
        }

        public bag_entry() {
            map = new ResTable_map();
        }
    }

	public static final class Bag {
		public bag_set mBagHeader = new bag_set();
		public ArrayList<bag_entry> mBagEntries = null;
		public boolean isFFFFFFFF = false;
	}

	private static final class PackageGroup {
		public String name;
		public int id;
		public ArrayList<Package> packages = new ArrayList<Package>();
		public Package basePackage = null;
		public int typeCount = 0;
		public Bag[][] bags = null;

		public PackageGroup(ResTable _owner, String _name, int _id) {
			name = _name;
			id = _id;
		}

        public void clearBagCache() {
            bags = null;
        }
    }


}
