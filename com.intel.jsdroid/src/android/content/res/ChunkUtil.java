package android.content.res;

import java.io.IOException;

class ChunkUtil {
	public static final void checkType(int type, int expectedType)
			throws IOException {
		if (type != expectedType) {
			throw new IOException("Expected chunk of type 0x"
					+ Integer.toHexString(expectedType) + ", read 0x"
					+ Integer.toHexString(type) + ".");
		}
	}
}
