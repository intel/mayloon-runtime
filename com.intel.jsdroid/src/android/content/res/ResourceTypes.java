package android.content.res;

import java.util.HashMap;
import android.util.TypedValue;

// TODO: one problem here is java has no unsigned objects
public class ResourceTypes {
	public final static int ENTRY_FLAG_COMPLEX = 0x00000001;

	/**
	 * ********************************************************************
	 * Base Types
	 *
	 *********************************************************************** */
	public static final class ResData_pointer {
		public int offset;
		public byte[] data;

		public ResData_pointer(int _offset, byte[] _data) {
			offset = _offset;
			data = _data;
		}
	}

	public static final class ResChunk_header {
		public int type; // uint16_t
		public int headerSize; // uint16_t
		public int size; // uint32_t
		public ResData_pointer pointer;

		public ResChunk_header(byte[] _data, int _offset, int _type,
				int _headerSize, int _size) {
			pointer = new ResData_pointer(_offset, _data);
			type = _type;
			headerSize = _headerSize;
			size = _size;
		}

		public static int sizeof() {
			return 2 + 2 + 4;
		}
	}

	public static final class Res_value {
		public int size = 0; // uint16_t
		public int res0 = 0; // uint8_t
		public int dataType = TypedValue.TYPE_NULL; // uint8_t
		public int data = 0; // uint32_t

		public Res_value(int _size, int _res0, int _dataType, int _data) {
			size = _size;
			res0 = _res0;
			dataType = _dataType;
			data = _data;
		}

		public Res_value() {
		}

		public static int sizeof() {
			return 2 + 1 + 1 + 4;
		}

		public String toString() {
			return "t = " + dataType + ", d = " + data;
		}

		public void copyFrom(Res_value res) {
			size = res.size;
			res0 = res.res0;
			dataType = res.dataType;
			data = res.data;
		}
	}

	public static final class ResTable_ref {
		int ident; // uint32_t

		public ResTable_ref(int _ident) {
			ident = _ident;
		}

		public static int sizeof() {
			return 4;
		}
	}

	public static final class ResStringPool_ref {
		int index; // uint32_t

		public ResStringPool_ref(int _index) {
			index = _index;
		}

		public static int sizeof() {
			return 4;
		}
	}

	public static final int RES_NULL_TYPE = 0x0000;
	public static final int RES_STRING_POOL_TYPE = 0x0001;
	public static final int RES_TABLE_TYPE = 0x0002;
	public static final int RES_XML_TYPE = 0x0003;
	// Chunk types in RES_XML_TYPE
	public static final int RES_XML_FIRST_CHUNK_TYPE = 0x0100;
	public static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;
	public static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;
	public static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
	public static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
	public static final int RES_XML_CDATA_TYPE = 0x0104;
	public static final int RES_XML_LAST_CHUNK_TYPE = 0x017f;
	public static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;
	// Chunk types in RES_TABLE_TYPE
	public static final int RES_TABLE_PACKAGE_TYPE = 0x0200;
	public static final int RES_TABLE_TYPE_TYPE = 0x0201;
	public static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;

	/**
	 * ********************************************************************
	 * String Pool
	 *
	 *********************************************************************** */
	public static final class ResStringPool_header {
		public static final int UTF8_FLAG = 1 << 8;
		public ResChunk_header header;
		public int stringCount; // uint32_t
		public int styleCount; // uint32_t
		public int flags; // uint32_t
		public int stringsStart; // uint32_t
		public int stylesStart; // uint32_t

		public ResStringPool_header(ResChunk_header _header, int _stringCount,
				int _styleCount, int _flags, int _stringsStart, int _stylesStart) {
			header = _header;
			stringCount = _stringCount;
			styleCount = _styleCount;
			flags = _flags;
			stringsStart = _stringsStart;
			stylesStart = _stylesStart;
		}

		public static int sizeof() {
			return ResChunk_header.sizeof() + 5 * 4;
		}
	}

	public static final class ResStringPool_span {
		public ResStringPool_ref name;
		public int firstChar, lastChar;

		public ResStringPool_span(ResStringPool_ref _name, int _firstChar,
				int _lastChar) {
			name = _name;
			firstChar = _firstChar;
			lastChar = _lastChar;
		}

		public static int sizeof() {
			return ResStringPool_ref.sizeof() + 2 * 4;
		}
	}

	/**
	 * ******************************************************************** XML
	 * Tree
	 *
	 *********************************************************************** */
	public static final class ResXMLTree_header {
		public ResChunk_header header;

		public ResXMLTree_header(ResChunk_header _header) {
			header = _header;
		}

		public static int sizeof() {
			return ResChunk_header.sizeof();
		}
	}

	public static final class ResXMLTree_node {
		public ResChunk_header header;
		public int lineNumber;
		public ResStringPool_ref comment;

		public ResXMLTree_node(ResChunk_header _header, int _lineNumber,
				ResStringPool_ref _comment) {
			header = _header;
			lineNumber = _lineNumber;
			comment = _comment;
		}

		public static int sizeof() {
			return ResChunk_header.sizeof() + 4 + ResStringPool_ref.sizeof();
		}
	}

	public static final class ResXMLTree_cdataExt {
		public int offset;
		public ResStringPool_ref data;
		public Res_value typedData;

		public ResXMLTree_cdataExt(int _offset, ResStringPool_ref _data,
				Res_value _typedData) {
			data = _data;
			typedData = _typedData;
			offset = _offset;
		}

		public static int sizeof() {
			return ResStringPool_ref.sizeof() + Res_value.sizeof();
		}
	}

	public static final class ResXMLTree_namespaceExt {
		public int offset;
		public ResStringPool_ref prefix;
		public ResStringPool_ref uri;

		public ResXMLTree_namespaceExt(int _offset, ResStringPool_ref _prefix,
				ResStringPool_ref _uri) {
			prefix = _prefix;
			uri = _uri;
			offset = _offset;
		}

		public static int sizeof() {
			return ResStringPool_ref.sizeof() + ResStringPool_ref.sizeof();
		}
	}

	public static final class ResXMLTree_endElementExt {
		public int offset;
		public ResStringPool_ref ns;
		public ResStringPool_ref name;

		public ResXMLTree_endElementExt(int _offset, ResStringPool_ref _ns,
				ResStringPool_ref _name) {
			ns = _ns;
			name = _name;
			offset = _offset;
		}

		public static int sizeof() {
			return ResStringPool_ref.sizeof() + ResStringPool_ref.sizeof();
		}
	}

	public static final class ResXMLTree_attrExt {
		public int offset;
		public ResStringPool_ref ns;
		public ResStringPool_ref name;
		public int attributeStart; // uint16_t
		public int attributeSize; // uint16_t
		public int attributeCount; // uint16_t
		public int idIndex; // uint16_t
		public int classIndex; // uint16_t
		public int styleIndex; // uint16_t

		public ResXMLTree_attrExt(int _offset, ResStringPool_ref _ns,
				ResStringPool_ref _name, int _attributeStart,
				int _attributeSize, int _attributeCount, int _idIndex,
				int _classIndex, int _styleIndex) {
			ns = _ns;
			name = _name;
			attributeStart = _attributeStart;
			attributeSize = _attributeSize;
			attributeCount = _attributeCount;
			idIndex = _idIndex;
			classIndex = _classIndex;
			styleIndex = _styleIndex;
			offset = _offset;
		}

		public static int sizeof() {
			return ResStringPool_ref.sizeof() + ResStringPool_ref.sizeof() + 6
					* 2;
		}
	}

	public static final class ResXMLTree_attribute {
		public int offset;
		public ResStringPool_ref ns;
		public ResStringPool_ref name;
		public ResStringPool_ref rawValue;
		public Res_value typedValue;

		public ResXMLTree_attribute(int _offset, ResStringPool_ref _ns,
				ResStringPool_ref _name, ResStringPool_ref _rawValue,
				Res_value _typedValue) {
			ns = _ns;
			name = _name;
			rawValue = _rawValue;
			typedValue = _typedValue;
			offset = _offset;
		}

		public static int sizeof() {
			return ResStringPool_ref.sizeof() + ResStringPool_ref.sizeof()
					+ ResStringPool_ref.sizeof() + Res_value.sizeof();
		}
	}

	/**
	 * ********************************************************************
	 * RESOURCE TABLE
	 *
	 *********************************************************************** */
	public static final class ResTable_header {
		public ResChunk_header header;
		public int packageCount; // uint32_t

		public ResTable_header(ResChunk_header _header, int _packageCount) {
			header = _header;
			packageCount = _packageCount;
		}

		public static int sizeof() {
			return ResChunk_header.sizeof() + 4;
		}
	}

	public static final class ResTable_package {
		public ResChunk_header header;
		public int id;
		public String name; // char16_t * 128
		public int typeStrings;
		public int lastPublicType;
		public int keyStrings;
		public int lastPublicKey;

		public ResTable_package(ResChunk_header _header, int _id, char[] _name,
				int _typeStrings, int _lastPublicType, int _keyStrings,
				int _lastPublicKey) {
			header = _header;
			id = _id;
			name = String.valueOf(_name);
			typeStrings = _typeStrings;
			lastPublicType = _lastPublicType;
			keyStrings = _keyStrings;
			lastPublicKey = _lastPublicKey;
		}

		public static int sizeof() {
			return ResChunk_header.sizeof() + 5 * 4 + 128 * 2;
		}
	}

	// TODO maybe just a placeholder for the time being
	public static final class ResTable_config {
		public int size;
		public short mcc;
		public short mnc;

		public char[] language;
		public char[] country;

		public byte orientation;
		public byte touchscreen;
		public short density;

		public byte keyboard;
		public byte navigation;
		public byte inputFlags;

		public short screenWidth;
		public short screenHeight;

		public short sdkVersion;
		public short minorVersion;

		public byte screenLayout;
		public byte uiMode;
		public short smallestScreenWidthDp;

		public short screenWidthDp;
		public short screenHeightDp;

		public boolean isInvalid;

		private String mQualifiers;

		public ResTable_config() {
			mcc = 0;
			mnc = 0;
			language = new char[] { '\00', '\00' };
			country = new char[] { '\00', '\00' };
			orientation = ORIENTATION_ANY;
			touchscreen = TOUCHSCREEN_ANY;
			density = DENSITY_DEFAULT;
			keyboard = KEYBOARD_ANY;
			navigation = NAVIGATION_ANY;
			inputFlags = KEYSHIDDEN_ANY | NAVHIDDEN_ANY;
			screenWidth = 0;
			screenHeight = 0;
			sdkVersion = 0;
			screenLayout = SCREENLONG_ANY | SCREENSIZE_ANY;
			uiMode = UI_MODE_TYPE_ANY | UI_MODE_NIGHT_ANY;
			smallestScreenWidthDp = 0;
			screenWidthDp = 0;
			screenHeightDp = 0;
			isInvalid = false;
			mQualifiers = "";
		}

		public ResTable_config(short mcc, short mnc, char[] language,
				char[] country, byte orientation, byte touchscreen,
				short density, byte keyboard, byte navigation, byte inputFlags,
				short screenWidth, short screenHeight, short sdkVersion,
				byte screenLayout, byte uiMode, short smallestScreenWidthDp,
				short screenWidthDp, short screenHeightDp, boolean isInvalid) {
			if (orientation < 0 || orientation > 3) {
				//	            LOGGER.warning("Invalid orientation value: " + orientation);
				orientation = 0;
				isInvalid = true;
			}
			if (touchscreen < 0 || touchscreen > 3) {
				//	            LOGGER.warning("Invalid touchscreen value: " + touchscreen);
				touchscreen = 0;
				isInvalid = true;
			}
			if (density < -1) {
				//	            LOGGER.warning("Invalid density value: " + density);
				density = 0;
				isInvalid = true;
			}
			if (keyboard < 0 || keyboard > 3) {
				//	            LOGGER.warning("Invalid keyboard value: " + keyboard);
				keyboard = 0;
				isInvalid = true;
			}
			if (navigation < 0 || navigation > 4) {
				//	            LOGGER.warning("Invalid navigation value: " + navigation);
				navigation = 0;
				isInvalid = true;
			}

			this.mcc = mcc;
			this.mnc = mnc;
			this.language = language;
			this.country = country;
			this.orientation = orientation;
			this.touchscreen = touchscreen;
			this.density = density;
			this.keyboard = keyboard;
			this.navigation = navigation;
			this.inputFlags = inputFlags;
			this.screenWidth = screenWidth;
			this.screenHeight = screenHeight;
			this.sdkVersion = sdkVersion;
			this.screenLayout = screenLayout;
			this.uiMode = uiMode;
			this.smallestScreenWidthDp = smallestScreenWidthDp;
			this.screenWidthDp = screenWidthDp;
			this.screenHeightDp = screenHeightDp;
			this.isInvalid = isInvalid;
			mQualifiers = generateQualifiers();
		}

		public String getQualifiers() {
			return mQualifiers;
		}

		private String generateQualifiers() {
			StringBuilder ret = new StringBuilder();
			if (mcc != 0) {
				//				ret.append("-mcc").append(String.format("%03d", mcc));
				ret.append("-mcc").append(mcc);
				if (mnc != 0) {
					ret.append("-mnc").append(mnc);
				}
			}
			if (language[0] != '\00') {
				ret.append('-').append(language);
				if (country[0] != '\00') {
					ret.append("-r").append(country);
				}
			}
			if (smallestScreenWidthDp != 0) {
				ret.append("-sw").append(smallestScreenWidthDp).append("dp");
			}
			if (screenWidthDp != 0) {
				ret.append("-w").append(screenWidthDp).append("dp");
			}
			if (screenHeightDp != 0) {
				ret.append("-h").append(screenHeightDp).append("dp");
			}
			switch (screenLayout & MASK_SCREENSIZE) {
			case SCREENSIZE_SMALL:
				ret.append("-small");
				break;
			case SCREENSIZE_NORMAL:
				ret.append("-normal");
				break;
			case SCREENSIZE_LARGE:
				ret.append("-large");
				break;
			case SCREENSIZE_XLARGE:
				ret.append("-xlarge");
				break;
			}
			switch (screenLayout & MASK_SCREENLONG) {
			case SCREENLONG_YES:
				ret.append("-long");
				break;
			case SCREENLONG_NO:
				ret.append("-notlong");
				break;
			}
			switch (orientation) {
			case ORIENTATION_PORT:
				ret.append("-port");
				break;
			case ORIENTATION_LAND:
				ret.append("-land");
				break;
			case ORIENTATION_SQUARE:
				ret.append("-square");
				break;
			}
			switch (uiMode & MASK_UI_MODE_TYPE) {
			case UI_MODE_TYPE_CAR:
				ret.append("-car");
				break;
			case UI_MODE_TYPE_DESK:
				ret.append("-desk");
				break;
			case UI_MODE_TYPE_TELEVISION:
				ret.append("-television");
				break;
			}
			switch (uiMode & MASK_UI_MODE_NIGHT) {
			case UI_MODE_NIGHT_YES:
				ret.append("-night");
				break;
			case UI_MODE_NIGHT_NO:
				ret.append("-notnight");
				break;
			}
			switch (density) {
			case DENSITY_DEFAULT:
				break;
			case DENSITY_LOW:
				ret.append("-ldpi");
				break;
			case DENSITY_MEDIUM:
				ret.append("-mdpi");
				break;
			case DENSITY_HIGH:
				ret.append("-hdpi");
				break;
			case DENSITY_XHIGH:
				ret.append("-xhdpi");
				break;
			case DENSITY_NONE:
				ret.append("-nodpi");
				break;
			default:
				ret.append('-').append(density).append("dpi");
			}
			switch (touchscreen) {
			case TOUCHSCREEN_NOTOUCH:
				ret.append("-notouch");
				break;
			case TOUCHSCREEN_STYLUS:
				ret.append("-stylus");
				break;
			case TOUCHSCREEN_FINGER:
				ret.append("-finger");
				break;
			}
			switch (inputFlags & MASK_KEYSHIDDEN) {
			case KEYSHIDDEN_NO:
				ret.append("-keysexposed");
				break;
			case KEYSHIDDEN_YES:
				ret.append("-keyshidden");
				break;
			case KEYSHIDDEN_SOFT:
				ret.append("-keyssoft");
				break;
			}
			switch (keyboard) {
			case KEYBOARD_NOKEYS:
				ret.append("-nokeys");
				break;
			case KEYBOARD_QWERTY:
				ret.append("-qwerty");
				break;
			case KEYBOARD_12KEY:
				ret.append("-12key");
				break;
			}
			switch (inputFlags & MASK_NAVHIDDEN) {
			case NAVHIDDEN_NO:
				ret.append("-navexposed");
				break;
			case NAVHIDDEN_YES:
				ret.append("-navhidden");
				break;
			}
			switch (navigation) {
			case NAVIGATION_NONAV:
				ret.append("-nonav");
				break;
			case NAVIGATION_DPAD:
				ret.append("-dpad");
				break;
			case NAVIGATION_TRACKBALL:
				ret.append("-trackball");
				break;
			case NAVIGATION_WHEEL:
				ret.append("-wheel");
				break;
			}
			if (screenWidth != 0 && screenHeight != 0) {
				if (screenWidth > screenHeight) {
				    /**
				     * @Mayloon update
				     * String.format not implemented in mayloon
				     */
					//					ret.append(String.format("-%dx%d", screenWidth,
					//							screenHeight));
					ret.append("-" + screenWidth + "x"
							+ screenHeight);
				} else {
					//					ret.append(String.format("-%dx%d", screenHeight,
					//							screenWidth));
					ret.append("-" + screenHeight + "x"
							+ screenWidth);
				}
			}
			if (sdkVersion > getNaturalSdkVersionRequirement()) {
				ret.append("-v").append(sdkVersion);
			}

			return ret.toString();
		}

		private short getNaturalSdkVersionRequirement() {
			if (smallestScreenWidthDp != 0 || screenWidthDp != 0
					|| screenHeightDp != 0) {
				return 13;
			}
			if ((uiMode & (MASK_UI_MODE_TYPE | MASK_UI_MODE_NIGHT)) != 0) {
				return 8;
			}
			if ((screenLayout & (MASK_SCREENSIZE | MASK_SCREENLONG)) != 0
					|| density != DENSITY_DEFAULT) {
				return 4;
			}
			return 0;
		}

		public String toString() {
			return !getQualifiers().equals("") ? getQualifiers() : "[DEFAULT]";
		}

		// TODO can use in j2s??
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ResTable_config other = (ResTable_config) obj;
			return this.mQualifiers.equals(other.mQualifiers);
		}

		public int hashCode() {
			int hash = 3;
			hash = 97 * hash + this.mQualifiers.hashCode();
			return hash;
		}

		// TODO sth tricky here! should return this.size?
		public static int sizeof() {
			return 8 * 4;
		}

		public int instance_sizeof() {
			return size;
		}

		public void copyFrom(ResTable_config config) {
			this.size = config.size;
			this.mcc = config.mcc;
			this.mnc = config.mnc;
			this.language = config.language;
			this.country = config.country;
			this.orientation = config.orientation;
			this.touchscreen = config.touchscreen;
			this.density = config.density;
			this.keyboard = config.keyboard;
			this.navigation = config.navigation;
			this.inputFlags = config.inputFlags;
			this.screenWidth = config.screenWidth;
			this.screenHeight = config.screenHeight;
			this.sdkVersion = config.sdkVersion;
			this.screenLayout = config.screenLayout;
			this.uiMode = config.uiMode;
			this.smallestScreenWidthDp = config.smallestScreenWidthDp;
			this.screenWidthDp = config.screenWidthDp;
			this.screenHeightDp = config.screenHeightDp;
			this.isInvalid = config.isInvalid;
			mQualifiers = config.generateQualifiers();
		}

        // Return true if 'this' is a better match than 'o' for the 'requested'
        // configuration. This assumes that match() has already been used to
        // remove any configurations that don't match the requested
        // configuration
        // at all; if they are not first filtered, non-matching results can be
        // considered better than matching ones.
        // The general rule per attribute: if the request cares about an
        // attribute
        // (it normally does), if the two (this and o) are equal it's a tie. If
        // they are not equal then one must be generic because only generic and
        // '==requested' will pass the match() call. So if this is not generic,
        // it wins. If this IS generic, o wins (return false).
        boolean isBetterThan(ResTable_config o, ResTable_config requested) {
            if (requested != null) {
                // Only check density in MayLoon now
                if (density != o.density) {
                    // density is tough. Any density is potentially useful
                    // because the system will scale it. Scaling down
                    // is generally better than scaling up.
                    // Default density counts as 160dpi (the system default)
                    // TODO - remove 160 constants
                    int h = (density != 0 ? density : 160);
                    int l = (o.density != 0 ? o.density : 160);
                    boolean bImBigger = true;
                    if (l > h) {
                        int t = h;
                        h = l;
                        l = t;
                        bImBigger = false;
                    }

                    int reqValue = (requested.density != 0 ? requested.density : 160);
                    if (reqValue >= h) {
                        // requested value higher than both l and h, give h
                        return bImBigger;
                    }
                    if (l >= reqValue) {
                        // requested value lower than both l and h, give l
                        return !bImBigger;
                    }
                    // saying that scaling down is 2x better than up
                    if (((2 * l) - reqValue) * h > reqValue * reqValue) {
                        return !bImBigger;
                    } else {
                        return bImBigger;
                    }
                }

                return false;
            }

            // return isMoreSpecificThan(o);
            return false;
        }

		public final static byte ORIENTATION_ANY = 0;
		public final static byte ORIENTATION_PORT = 1;
		public final static byte ORIENTATION_LAND = 2;
		public final static byte ORIENTATION_SQUARE = 3;

		public final static byte TOUCHSCREEN_ANY = 0;
		public final static byte TOUCHSCREEN_NOTOUCH = 1;
		public final static byte TOUCHSCREEN_STYLUS = 2;
		public final static byte TOUCHSCREEN_FINGER = 3;

		public final static short DENSITY_DEFAULT = 0;
		public final static short DENSITY_LOW = 120;
		public final static short DENSITY_MEDIUM = 160;
		public final static short DENSITY_HIGH = 240;
		public final static short DENSITY_XHIGH = 320;
		public final static short DENSITY_NONE = -1;

		public final static byte KEYBOARD_ANY = 0;
		public final static byte KEYBOARD_NOKEYS = 1;
		public final static byte KEYBOARD_QWERTY = 2;
		public final static byte KEYBOARD_12KEY = 3;

		public final static byte NAVIGATION_ANY = 0;
		public final static byte NAVIGATION_NONAV = 1;
		public final static byte NAVIGATION_DPAD = 2;
		public final static byte NAVIGATION_TRACKBALL = 3;
		public final static byte NAVIGATION_WHEEL = 4;

		public final static byte MASK_KEYSHIDDEN = 0x3;
		public final static byte KEYSHIDDEN_ANY = 0x0;
		public final static byte KEYSHIDDEN_NO = 0x1;
		public final static byte KEYSHIDDEN_YES = 0x2;
		public final static byte KEYSHIDDEN_SOFT = 0x3;

		public final static byte MASK_NAVHIDDEN = 0xc;
		public final static byte NAVHIDDEN_ANY = 0x0;
		public final static byte NAVHIDDEN_NO = 0x4;
		public final static byte NAVHIDDEN_YES = 0x8;

		public final static byte MASK_SCREENSIZE = 0x0f;
		public final static byte SCREENSIZE_ANY = 0x00;
		public final static byte SCREENSIZE_SMALL = 0x01;
		public final static byte SCREENSIZE_NORMAL = 0x02;
		public final static byte SCREENSIZE_LARGE = 0x03;
		public final static byte SCREENSIZE_XLARGE = 0x04;

		public final static byte MASK_SCREENLONG = 0x30;
		public final static byte SCREENLONG_ANY = 0x00;
		public final static byte SCREENLONG_NO = 0x10;
		public final static byte SCREENLONG_YES = 0x20;

		public final static byte MASK_UI_MODE_TYPE = 0x0f;
		public final static byte UI_MODE_TYPE_ANY = 0x00;
		public final static byte UI_MODE_TYPE_NORMAL = 0x01;
		public final static byte UI_MODE_TYPE_DESK = 0x02;
		public final static byte UI_MODE_TYPE_CAR = 0x03;
		public final static byte UI_MODE_TYPE_TELEVISION = 0x04;

		public final static byte MASK_UI_MODE_NIGHT = 0x30;
		public final static byte UI_MODE_NIGHT_ANY = 0x00;
		public final static byte UI_MODE_NIGHT_NO = 0x10;
		public final static byte UI_MODE_NIGHT_YES = 0x20;
	}

	public static final class ResTable_typeSpec {
		public ResChunk_header header;
		public int id; // uint8_t
		public int res0; // uint8_t
		public int res1; // uint16_t
		public int entryCount; // uint32_t

		public ResTable_typeSpec(ResChunk_header _header, int _id, int _res0,
				int _res1, int _entryCount) {
			header = _header;
			id = _id;
			res0 = _res0;
			res1 = _res1;
			entryCount = _entryCount;
		}

		public static int sizeof() {
			return ResChunk_header.sizeof() + 2 * 1 + 2 + 4;
		}

		public static final int SPEC_PUBLIC = 0x40000000;
	}

	public static class ResPointers {
	    public int base; //base address of the resources
	    public byte[] data;

	    ResPointers(int base, byte[] data){
	        this.base = base;
	        this.data = data;
	    }
	}
	/**
	 * A collection of resource entries for a particular resource data
	 * type. Followed by an array of uint32_t defining the resource
	 * values, corresponding to the array of type strings in the
	 * ResTable_package::typeStrings string block. Each of these hold an
	 * index from entriesStart; a value of NO_ENTRY means that entry is
	 * not defined.
	 */
	public static final class ResTable_type {
		public ResChunk_header header;
		public int id; // uint8_t
		public int res0; // uint8_t
		public int res1; // uint16_t
		public int entryCount; // uint32_t
		public int entriesStart; // uint32_t
		public ResTable_config config;
		// (offset, entry)
		public ResPointers resPointers = null;
		public HashMap<Integer, ResTable_entry> resources = new HashMap<Integer, ResTable_entry>();
		public IntArray entryOffsets = null;

		public ResTable_type(ResChunk_header _header, int _id, int _res0,
				int _res1, int _entryCount, int _entriesStart) {
			header = _header;
			id = _id;
			res0 = _res0;
			res1 = _res1;
			entryCount = _entryCount;
			entriesStart = _entriesStart;
		}

		public static int sizeof() {
			return ResChunk_header.sizeof() + 2 * 1 + 2 + 4 * 2
					+ ResTable_config.sizeof();
		}

		//		public int instance_sizeof() {
		//			int totalSize = ResTable_type.sizeof();
		//			totalSize += Res_value.sizeof() * resources.size();
		//			return totalSize;
		//		}

		public static final int NO_ENTRY = 0xFFFFFFFF;
	}

	public static class ResTable_entry {
		public int size; // uint16_t
		public int flags; // uint16_t
		public ResStringPool_ref key;

		public static final int FLAG_COMPLEX = 0x0001;
		public static final int FLAG_PUBLIC = 0x0002;

		public ResTable_entry(int _size, int _flags, ResStringPool_ref _key) {
			size = _size;
			flags = _flags;
			key = _key;
		}

		public static int sizeof() {
			return 2 * 2 + ResStringPool_ref.sizeof();
		}

		public int instance_sizeof() {
			return size;
		}
	}

	public static class ResTable_value_entry extends ResTable_entry {
		public Res_value value = new Res_value();

		public ResTable_value_entry(int _size, int _flags,
				ResStringPool_ref _key) {
			super(_size, _flags, _key);
		}
	}

	public static class ResTable_map_entry extends ResTable_entry {
		public ResTable_ref parent;
		public int count; // uint32_t
		public ResTable_map[] entries = null;

		public ResTable_map_entry(int _size, int _flags,
				ResStringPool_ref _key, ResTable_ref _parent, int _count) {
			super(_size, _flags, _key);
			parent = _parent;
			count = _count;
		}

		public static int sizeof() {
			return ResTable_entry.sizeof() + ResTable_ref.sizeof() + 4;
		}

		//		public int instance_sizeof() {
		//			// TODO need to test
		//			return size;
		//			//			return ResTable_map_entry.sizeof() + count * ResTable_map.sizeof();
		//		}
	}

	// TODO 5.3
	public static class ResTable_map {
		// The resource identifier defining this mapping's name.  For attribute
		// resources, 'name' can be one of the following special resource types
		// to supply meta-data about the attribute; for all other resource types
		// it must be an attribute resource.
		ResTable_ref name = new ResTable_ref(0);

		// Special values for 'name' when defining attribute resources.
		// This entry holds the attribute's type code.
		// #define Res_MAKEINTERNAL(entry) (0x01000000 | (entry&0xFFFF))
		public static final int ATTR_TYPE = (0x01000000 | (0 & 0xFFFF));

		// For integral attributes; this is the minimum value it can hold.
		public static final int ATTR_MIN = (0x01000000 | (1 & 0xFFFF));

		// For integral attributes; this is the maximum value it can hold.
		public static final int ATTR_MAX = (0x01000000 | (2 & 0xFFFF));

		// Localization of this resource is can be encouraged or required with
		// an aapt flag if this is set
		public static final int ATTR_L10N = (0x01000000 | (3 & 0xFFFF));

		// for plural support; see android.content.res.PluralRules#attrForQuantity(int)
		public static final int ATTR_OTHER = (0x01000000 | (4 & 0xFFFF));
		public static final int ATTR_ZERO = (0x01000000 | (5 & 0xFFFF));
		public static final int ATTR_ONE = (0x01000000 | (6 & 0xFFFF));
		public static final int ATTR_TWO = (0x01000000 | (7 & 0xFFFF));
		public static final int ATTR_FEW = (0x01000000 | (8 & 0xFFFF));
		public static final int ATTR_MANY = (0x01000000 | (9 & 0xFFFF));

		// Bit mask of allowed types; for use with ATTR_TYPE.
		// No type has been defined for this attribute; use generic
		// type handling.  The low 16 bits are for types that can be
		// handled generically; the upper 16 require additional information
		// in the bag so can not be handled generically for TYPE_ANY.
		public static final int TYPE_ANY = 0x0000FFFF;

		// Attribute holds a references to another resource.
		public static final int TYPE_REFERENCE = 1 << 0;

		// Attribute holds a generic string.
		public static final int TYPE_STRING = 1 << 1;

		// Attribute holds an integer value.  ATTR_MIN and ATTR_MIN can
		// optionally specify a constrained range of possible integer values.
		public static final int TYPE_INTEGER = 1 << 2;

		// Attribute holds a boolean integer.
		public static final int TYPE_BOOLEAN = 1 << 3;

		// Attribute holds a color value.
		public static final int TYPE_COLOR = 1 << 4;

		// Attribute holds a floating point value.
		public static final int TYPE_FLOAT = 1 << 5;

		// Attribute holds a dimension value; such as "20px".
		public static final int TYPE_DIMENSION = 1 << 6;

		// Attribute holds a fraction value; such as "20%".
		public static final int TYPE_FRACTION = 1 << 7;

		// Attribute holds an enumeration.  The enumeration values are
		// supplied as additional entries in the map.
		public static final int TYPE_ENUM = 1 << 16;

		// Attribute holds a bitmaks of flags.  The flag bit values are
		// supplied as additional entries in the map.
		public static final int TYPE_FLAGS = 1 << 17;

		// Enum of localization modes; for use with ATTR_L10N.
		public static final int L10N_NOT_REQUIRED = 0;
		public static final int L10N_SUGGESTED = 1;

		// This mapping's value.
		Res_value value = new Res_value();

		public static int sizeof() {
			return ResTable_ref.sizeof() + Res_value.sizeof();
		}

		public ResTable_map(ResTable_ref _name, Res_value _value) {
			name.ident = _name.ident;
			value.copyFrom(_value);
		}

		public ResTable_map() {

		}
	}

}
