
package android.content.res;

import java.io.IOException;

class IntArray {
    byte[] data;
    int offset;
    int length;
    IntReader reader;

    IntArray(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;

        reader = new IntReader(data, offset, false);
    }

    int getEntry(int index) {
        int result = 0;
        reader.setPosition(offset + index * 4);
        try {
            result = reader.readInt();
        } catch (IOException e) {
        }
        return result;
    }
}
