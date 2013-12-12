package android.content.res;

import java.io.IOException;

/**
 * Simple helper class that allows reading of integers.
 */
public final class IntReader {
	public IntReader(byte[] data, int offset, boolean bigEndian) {
		reset(data, offset, bigEndian);
	}

	public final void reset(byte[] data, int offset, boolean bigEndian) {
		mData = data;
		mBigEndian = bigEndian;
		mPosition = offset;
	}

	public final void close() {
		reset(null, -1, false);
	}

	public final byte[] getData() {
		return mData;
	}

	public final boolean isBigEndian() {
		return mBigEndian;
	}

	public final void setBigEndian(boolean bigEndian) {
		mBigEndian = bigEndian;
	}

	public final byte readByte() throws IOException {
		return (byte) (readInt(1) & 0xff);
	}

	public final short readShort() throws IOException {
		return (short) (readInt(2) & 0xffff);
	}

	public final int readInt() throws IOException {
		return readInt(4);
	}

	public final int readInt(int length) throws IOException {
		if (length < 0 || length > 4) {
			throw new IllegalArgumentException();
		}
		int result = 0;
		if (mBigEndian) {
			for (int i = (length - 1) * 8; i >= 0; i -= 8) {
				byte b = mData[mPosition];
				mPosition += 1;
				result += (b & 0xff) << i;
			}
		} else {
			length *= 8;
			for (int i = 0; i != length; i += 8) {
				byte b = mData[mPosition];
				mPosition += 1;
				result += (b & 0xff) << i;
			}
		}
		return result;
	}

	public final int[] readIntArray(int length) throws IOException {
		int[] array = new int[length];
		readIntArray(array, 0, length);
		return array;
	}

	public final void readIntArray(int[] array, int offset, int length)
			throws IOException {
		for (; length > 0; length -= 1) {
			array[offset++] = readInt();
		}
	}

	public final byte[] readByteArray(int length) throws IOException {
		byte[] array = new byte[length];
		for (int i = 0; i < length; ++i)
			array[i] = mData[mPosition + i];
		mPosition += length;
		return array;
	}

	// TODO Need to test, char_16 in C, but here use char(8-bit)
	public final char[] readWCharArray(int length) throws IOException {
		char[] array = new char[length];
		int offset = 0;
		for (; length > 0; length -= 1) {
			array[offset++] = (char) (readInt(2) & 0xff);
		}
		return array;
	}

	public final void skip(int bytes) throws IOException {
		if (bytes <= 0) {
			return;
		}
		mPosition += bytes;
	}

	public final void skipInt() throws IOException {
		skip(4);
	}

	public final int getPosition() {
		return mPosition;
	}

	public final void setPosition(int position) {
		mPosition = position;
	}

	/////////////////////////////////// data
	private byte[] mData = null;
	private boolean mBigEndian = false;
	private int mPosition = -1;
}
