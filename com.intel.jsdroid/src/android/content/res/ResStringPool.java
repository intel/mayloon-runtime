package android.content.res;

import java.io.IOException;

import android.content.res.ResourceTypes.*;
import android.util.Errors;

public class ResStringPool {

	private int mEntriesOffset = -1;
//	private int[] mEntries = null;

	private IntArray mEntries = null;
	private int mStringsOffset = -1;
	private ResData_pointer mStrings = null;
	private int mEntryStylesOffset = -1;
    private IntArray mEntryStyles = null;
	private int mStylesOffset = -1;
    private IntArray mStyles = null;
	@SuppressWarnings("unused")
	private int mStylePoolSize;
	@SuppressWarnings("unused")
	private int mStringPoolSize;
	private int mError = Errors.NO_INIT;
	private ResStringPool_header mHeader = null;
	@SuppressWarnings("unused")
	private int mSize = -1;

	public ResStringPool(byte[] data, int offset, int size, boolean copyData)
			throws IOException {
		setTo(data, offset, size, copyData);
	}

	public int setTo(byte[] data, int offset, int size, boolean copyData)
			throws IOException {
		if (data == null || size <= 0)
			return (mError = Errors.BAD_TYPE);
		uninit();

		IntReader reader = new IntReader(data, offset, false);
		ResChunk_header chunk = new ResChunk_header(data, reader.getPosition(),
				reader.readInt(2), reader.readInt(2), reader.readInt());
		ChunkUtil.checkType(chunk.type, ResourceTypes.RES_STRING_POOL_TYPE);
		mHeader = new ResStringPool_header(chunk, reader.readInt(),
				reader.readInt(), reader.readInt(), reader.readInt(),
				reader.readInt());
		mSize = mHeader.header.size;
		mEntriesOffset = mHeader.header.pointer.offset
				+ mHeader.header.headerSize;
		reader.setPosition(mEntriesOffset);

		if (mHeader.stringCount > 0) {
//			mEntries = reader.readIntArray(mHeader.stringCount);

			mEntries = new IntArray(reader.getData(), reader.getPosition(), mHeader.stringCount);
			mStringsOffset = mHeader.header.pointer.offset
					+ mHeader.stringsStart;
			reader.setPosition(mStringsOffset);
			int readSize = ((mHeader.styleCount == 0) ? mHeader.header.size
					: mHeader.stylesStart) - mHeader.stringsStart;
			if ((readSize % 4) != 0) {
				throw new IOException("String data size is not multiple of 4 ("
						+ readSize + ").");
			}
			mStrings = new ResData_pointer(reader.getPosition(), data);
			//			mStrings = reader.readIntArray(readSize / 4);
		}
		if (mHeader.styleCount > 0) {
			mEntryStylesOffset = mEntriesOffset + mHeader.stringCount;
			reader.setPosition(mEntryStylesOffset);
            mEntryStyles = new IntArray(reader.getData(), reader.getPosition(), mHeader.styleCount);
			mStylesOffset = mHeader.header.pointer.offset + mHeader.stylesStart;
			reader.setPosition(mStylesOffset);
			int readSize = (mHeader.header.size - mHeader.stylesStart);
			if ((readSize % 4) != 0) {
				throw new IOException("Style data size is not multiple of 4 ("
						+ readSize + ").");
			}
            mStyles = new IntArray(reader.getData(), reader.getPosition(), readSize);
		}
		return (mError = Errors.NO_ERROR);
	}

	/**
	 * Returns number of strings in block.
	 */
	public int getCount() {
		return this.mHeader.stringCount;
	}

	/**
	 * Returns raw string (without any styling information) at specified index.
	 */
	public String stringAt(int index) {
		if (index < 0 || this.mEntries == null || index >= this.mEntries.length) {
			return null;
		}
		boolean isUTF8 = (mHeader.flags & ResStringPool_header.UTF8_FLAG) != 0;
//		int offset = this.mEntries[index];
		int offset = mEntries.getEntry(index);
		if (isUTF8 == true) {
			int length = getByte(mStrings, offset);
			offset += 1; // skip one
			StringBuilder result = new StringBuilder(length);
			for (; length != 0; length -= 1) {
				offset += 1;
				result.append((char) getByte(mStrings, offset));
			}
			return result.toString();
		} else {
			int length = getShort(mStrings, offset);
			StringBuilder result = new StringBuilder(length);
			for (; length != 0; length -= 1) {
				offset += 2;
				result.append((char) getShort(mStrings, offset));
			}
			return result.toString();
		}
	}

	/**
	 * Not yet implemented.
	 *
	 * Returns string with style information (if any).
	 */
	public CharSequence get(int index) {
		return stringAt(index);
	}

	/**
	 * Returns string with style tags (html-like).
	 */
	public String getHTML(int index) {
		String raw = stringAt(index);
		if (raw == null) {
			return raw;
		}
		int[] style = getStyle(index);
		if (style == null) {
			return raw;
		}
		StringBuilder html = new StringBuilder(raw.length() + 32);
		int offset = 0;
		while (true) {
			int i = -1;
			for (int j = 0; j != style.length; j += 3) {
				if (style[j + 1] == -1) {
					continue;
				}
				if (i == -1 || style[i + 1] > style[j + 1]) {
					i = j;
				}
			}
			int start = ((i != -1) ? style[i + 1] : raw.length());
			for (int j = 0; j != style.length; j += 3) {
				int end = style[j + 2];
				if (end == -1 || end >= start) {
					continue;
				}
				if (offset <= end) {
					html.append(raw, offset, end + 1);
					offset = end + 1;
				}
				style[j + 2] = -1;
				html.append('<');
				html.append('/');
				html.append(stringAt(style[j]));
				html.append('>');
			}
			if (offset < start) {
				html.append(raw, offset, start);
				offset = start;
			}
			if (i == -1) {
				break;
			}
			html.append('<');
			html.append(stringAt(style[i]));
			html.append('>');
			style[i + 1] = -1;
		}
		return html.toString();
	}

	/**
	 * Finds index of the string.
	 * Returns -1 if the string was not found.
	 */
	public int indexOfString(String string) {
		if (string == null) {
			return -1;
		}
		for (int i = 0; i != this.mEntries.length; ++i) {
//			int offset = this.mEntries[i];
			int offset = mEntries.getEntry(i);
			int length = getShort(mStrings, offset);
			if (length != string.length()) {
				continue;
			}
			int j = 0;
			for (; j != length; ++j) {
				offset += 2;
				if (string.charAt(j) != getShort(mStrings, offset)) {
					break;
				}
			}
			if (j == length) {
				return i;
			}
		}
		return -1;
	}

    public int indexOfString(String str , int length) {
        int len;
        // TODO optimize searching for UTF-8 strings taking into account
        // the cache fill to determine when to convert the searched-for
        // string key to UTF-8.

        // It is unusual to get the ID from an unsorted string block...
        // most often this happens because we want to get IDs for style
        // span tags; since those always appear at the end of the string
        // block, start searching at the back.
        for (int i = mHeader.stringCount - 1; i >= 0; i--) {
            String s = stringAt(i);
            if (s != null && s.equals(str)) {
                return i;
            }
        }

        return -1;
    }

	///////////////////////////////////////////// implementation

	public ResStringPool() {
	}

    /**
     * Returns style information - array of int triplets, where in each triplet:
     * * first int is index of tag name ('b','i', etc.) * second int is tag
     * start index in string * third int is tag end index in string
     */
    public int[] getStyle(int index) {
        if (this.mEntryStyles == null || mStyles == null
                || index >= this.mEntryStyles.length) {
            return null;
        }
        int style[];

        int startIndex = 0;
        int count = 0;
        for (int i = 0; i < mStyles.length; ++i) {
            if (mStyles.getEntry(i) == -1) {
                count += 1;
                if (count == index) {
                    startIndex = i + 1;
                    break;
                }
            }
        }
        count = 0;
        for (int i = startIndex; i < mStyles.length; ++i) {
            if (mStyles.getEntry(i) == -1) {
                break;
            }
            count += 1;
        }
        style = new int[count];
        for (int i = startIndex, j = 0; i < mStyles.length;) {
            if (mStyles.getEntry(i) == -1) {
                break;
            }
            style[j++] = mStyles.getEntry(i++);
        }
        return style;
    }

	private static final int getShort(ResData_pointer pointer, int offset) {
		IntReader reader = new IntReader(pointer.data, pointer.offset, false);
		int pos = reader.getPosition() + offset;
		reader.setPosition(pos);
		try {
			int value = reader.readInt();
			return (value & 0xFFFF);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static final int getByte(ResData_pointer pointer, int offset) {
		IntReader reader = new IntReader(pointer.data, pointer.offset, false);
		int pos = reader.getPosition() + offset;
		reader.setPosition(pos);
		try {
			int value = reader.readInt();
			//			int value = array[offset / 4];
			//			switch (pos % 4) {
			//			case 0:
			return (value & 0xff);
			//			case 1:
			//				return (value & 0xff00) >>> 8;
			//			case 2:
			//				return (value & 0xff0000) >>> 16;
			//			case 3:
			//				return (value & 0xff000000) >>> 24;
			//			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void uninit() {
		mError = Errors.NO_INIT;
	}

	public int getError() {
		return mError;
	}

	public int size() {
		return (mError == Errors.NO_ERROR) ? mHeader.stringCount : 0;
	}
}
