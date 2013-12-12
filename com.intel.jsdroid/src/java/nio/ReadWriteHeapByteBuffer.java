/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.nio;

/**
 * HeapByteBuffer, ReadWriteHeapByteBuffer and ReadOnlyHeapByteBuffer compose
 * the implementation of array based byte buffers.
 * <p>
 * ReadWriteHeapByteBuffer extends HeapByteBuffer with all the write methods.
 * </p>
 * <p>
 * This class is marked final for runtime performance.
 * </p>
 *
 */
final class ReadWriteHeapByteBuffer extends HeapByteBuffer {

    static ReadWriteHeapByteBuffer copy(HeapByteBuffer other, int markOfOther) {
        ReadWriteHeapByteBuffer buf = new ReadWriteHeapByteBuffer(
                other, other.capacity(), other.offset);
        buf.limit = other.limit();
        buf.position = other.position();
        buf.mark = markOfOther;
        buf.order(other.order());
        return buf;
    }

    ReadWriteHeapByteBuffer(byte[] backingArray) {
        super(backingArray);
    }

    ReadWriteHeapByteBuffer(int capacity) {
        super(capacity);
    }

    ReadWriteHeapByteBuffer(HeapByteBuffer other, int capacity, int arrayOffset) {
        super(other, capacity, arrayOffset);
    }

    @Override
    public ByteBuffer asReadOnlyBuffer() {
        //return ReadOnlyHeapByteBuffer.copy(this, mark);
        return null;
    }

    @Override
    public ByteBuffer compact() {
        /**
         * @j2sNative
         * var len = remaining();
         * for (var i = 0; i < len; i++) {
         *     var value = this.backingArray.getInt8(this.position() + this.offset + i);
         *     this.backingArray.setInt8(this.offset + i, value);
         * }
         */{}

        position = limit - position;
        limit = capacity;
        mark = UNSET_MARK;
        return this;
    }

    @Override
    public ByteBuffer duplicate() {
        return copy(this, mark);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    protected byte[] protectedArray() {
        byte[] byteArray = new byte[this.capacity];
        /**
         * @j2sNative
         * for (var i = 0; i < this.capacity; i++) {
         *     byteArray[i] = this.backingArray.getInt8(i);
         * }
         */{}
        return byteArray;
    }

    @Override
    protected int protectedArrayOffset() {
        return offset;
    }

    @Override
    protected boolean protectedHasArray() {
        return true;
    }

    @Override
    public ByteBuffer put(byte b) {
        if (position == limit) {
            throw new BufferOverflowException();
        }

        /**
         * @j2sNative
         * this.backingArray.setInt8(this.offset + this.position(), b);
         */{}
         this.position++;
        return this;
    }

    @Override
    public ByteBuffer put(int index, byte b) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }

        /**
         * @j2sNative
         * this.backingArray.setInt8(this.offset + index, b);
         */{}
        return this;
    }

    /*
     * Override ByteBuffer.put(byte[], int, int) to improve performance.
     *
     * (non-Javadoc)
     *
     * @see java.nio.ByteBuffer#put(byte[], int, int)
     */
    @Override
    public ByteBuffer put(byte[] src, int off, int len) {
        if (off < 0 || len < 0 || (long) off + (long) len > src.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len > remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }

        /**
         * @j2sNative
         * for (var i = 0; i < len; i++) {
         *     this.backingArray.setInt8(this.offset + this.position() + i, src[off + i]);
         * }
         */{}
         
        position += len;
        return this;
    }

    @Override
    public ByteBuffer putDouble(double value) {
        int newPosition = position + 8;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeDouble(position, value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putDouble(int index, double value) {
        if (index < 0 || (long) index + 8 > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeDouble(index, value);
        return this;
    }

    @Override
    public ByteBuffer putFloat(float value) {
        int newPosition = position + 4;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeFloat(position, value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putFloat(int index, float value) {
        if (index < 0 || (long) index + 4 > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeFloat(index, value);
        return this;
    }

    @Override
    public ByteBuffer putInt(int value) {
        int newPosition = position + 4;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeInt(position, value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putInt(int index, int value) {
        if (index < 0 || (long) index + 4 > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeInt(index, value);
        return this;
    }

    @Override
    public ByteBuffer putLong(int index, long value) {
        if (index < 0 || (long) index + 8 > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeLong(index, value);
        return this;
    }

    @Override
    public ByteBuffer putLong(long value) {
        int newPosition = position + 8;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeLong(position, value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putShort(int index, short value) {
        if (index < 0 || (long) index + 2 > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeShort(index, value);
        return this;
    }

    @Override
    public ByteBuffer putShort(short value) {
        int newPosition = position + 2;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeShort(position, value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer slice() {
        ReadWriteHeapByteBuffer slice = new ReadWriteHeapByteBuffer(
                this, remaining(), offset + position);
        slice.order = order;
        return slice;
    }
}
