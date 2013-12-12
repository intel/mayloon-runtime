/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.nio;

/**
 * HeapByteBuffer, ReadWriteHeapByteBuffer and ReadOnlyHeapByteBuffer compose
 * the implementation of array based byte buffers.
 * <p>
 * HeapByteBuffer implements all the shared readonly methods and is extended by
 * the other two classes.
 * </p>
 * <p>
 * All methods are marked final for runtime performance.
 * </p>
 *
 */
abstract class HeapByteBuffer extends BaseByteBuffer {

    protected final int offset;

    HeapByteBuffer(byte[] backingArray) {
        this(backingArray, backingArray.length, 0);
    }

    HeapByteBuffer(int capacity) {
        //this(new byte[capacity], capacity, 0);
        super(capacity);
        /**
         * @j2sNative
         * this.backingArray = new DataView(new ArrayBuffer(capacity));
         */{}
         this.offset = 0;
    }

    HeapByteBuffer(byte[] backingArray, int capacity, int offset) {
        super(capacity);

        /**
         * @j2sNative
         * this.backingArray = new DataView(new ArrayBuffer(backingArray.length));
         * for (var i = 0; i < backingArray.length; i++) {
         *     var value = backingArray[i];
         *     this.backingArray.setInt8(i, value);
         * }
         */{}

        this.offset = offset;

        if (offset + capacity > backingArray.length) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    HeapByteBuffer(HeapByteBuffer other, int capacity, int offset) {
        super(capacity);

        /**
         * @j2sNative
         * this.backingArray = other.backingArray;
         */{}

        this.offset = offset;

        if (offset + capacity > this.capacity) {
            throw new IndexOutOfBoundsException();
        }
    }

    /*
     * Override ByteBuffer.get(byte[], int, int) to improve performance.
     *
     * (non-Javadoc)
     *
     * @see java.nio.ByteBuffer#get(byte[], int, int)
     */
    @Override
    public final ByteBuffer get(byte[] dst, int off, int len) {
        int length = dst.length;
        if (off < 0 || len < 0 || (long) off + (long) len > length) {
            throw new IndexOutOfBoundsException();
        }
        if (len > remaining()) {
            throw new BufferUnderflowException();
        }

        /**
         * @j2sNative
         * for (var i = 0; i < len; i++) {
         *     dst[off + i] = this.backingArray.getInt8(this.offset + this.position() + i);
         * }
         */{}

        position += len;
        return this;
    }

    @Override
    public final byte get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        byte item = 0;
        /**
         * @j2sNative
         * item = this.backingArray.getInt8(this.offset + this.position());
         */{}
         this.position++;
         return item;
    }

    @Override
    public final byte get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        byte item = 0;
        /**
         * @j2sNative
         * item = this.backingArray.getInt8(this.offset + index);
         */{}
         return item;
    }

    @Override
    public final double getDouble() {
        int newPosition = position + 8;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        double result = loadDouble(position);
        position = newPosition;
        return result;
    }

    @Override
    public final double getDouble(int index) {
        if (index < 0 || index + 8 > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadDouble(index);
    }

    @Override
    public final float getFloat() {
        int newPosition = position + 4;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        float result = loadFloat(position);
        position = newPosition;
        return result;
    }

    @Override
    public final float getFloat(int index) {
        if (index < 0 || index + 4 > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadFloat(index);
    }

    @Override
    public final int getInt() {
        int newPosition = position + 4;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        int result = loadInt(position);
        position = newPosition;
        return result;
    }

    @Override
    public final int getInt(int index) {
        if (index < 0 || index + 4 > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadInt(index);
    }

    @Override
    public final long getLong() {
        int newPosition = position + 8;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        long result = loadLong(position);
        position = newPosition;
        return result;
    }

    @Override
    public final long getLong(int index) {
        if (index < 0 || index + 8 > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadLong(index);
    }

    @Override
    public final short getShort() {
        int newPosition = position + 2;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        short result = loadShort(position);
        position = newPosition;
        return result;
    }

    @Override
    public final short getShort(int index) {
        if (index < 0 || index + 2 > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadShort(index);
    }

    @Override
    public final boolean isDirect() {
        return false;
    }

    protected final int loadInt(int index) {
        int baseOffset = offset + index;
        int bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * bytes = this.backingArray.getInt32(baseOffset, false);
             */{}
        } else {
            /**
             * @j2sNative
             * bytes = this.backingArray.getInt32(baseOffset, true);
             */{}
        }
        return bytes;
    }
    
    protected final float loadFloat(int index) {
        int baseOffset = offset + index;
        float bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * bytes = this.backingArray.getFloat32(baseOffset, false);
             */{}
        } else {
            /**
             * @j2sNative
             * bytes = this.backingArray.getFloat32(baseOffset, true);
             */{}
        }
        return bytes;
    }
    
    protected final double loadDouble(int index) {
        int baseOffset = offset + index;
        double bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * bytes = this.backingArray.getFloat64(baseOffset, false);
             */{}
        } else {
            /**
             * @j2sNative
             * bytes = this.backingArray.getFloat64(baseOffset, true);
             */{}
        }
        return bytes;
    }

    protected final long loadLong(int index) {
        int baseOffset = offset + index;
        long bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * var _bytes = this.backingArray.getFloat64(baseOffset, false);
             * bytes = parseInt(_bytes);
             */{}
        } else {
            /**
             * @j2sNative
             * var _bytes = this.backingArray.getFloat64(baseOffset, true);
             * bytes = parseInt(_bytes);
             */{}
        }
        return bytes;
    }

    protected final short loadShort(int index) {
        int baseOffset = offset + index;
        short bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * bytes = this.backingArray.getInt16(baseOffset, false);
             */{}
        } else {
            /**
             * @j2sNative
             * bytes = this.backingArray.getInt16(baseOffset, true);
             */{}
        }
        return bytes;
    }

    protected final void storeInt(int index, int value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * this.backingArray.setInt32(baseOffset, value, false);
             */{}
        } else {
            /**
             * @j2sNative
             * this.backingArray.setInt32(baseOffset, value, true);
             */{}
        }
    }

    protected final void storeLong(int index, long value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * var _bytes = parseFloat(value);
             * this.backingArray.setFloat64(baseOffset, _bytes, false);
             */{}
        } else {
            /**
             * @j2sNative
             * var _bytes = parseFloat(value);
             * this.backingArray.setFloat64(baseOffset, _bytes, true);
             */{}
        }
    }

    protected final void storeShort(int index, short value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * this.backingArray.setInt16(baseOffset, value, false);
             */{}
        } else {
            /**
             * @j2sNative
             * this.backingArray.setInt16(baseOffset, value, true);
             */{}
        }
    }
    
    protected final void storeDouble(int index, double value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * this.backingArray.setFloat64(baseOffset, value, false);
             */{}
        } else {
            /**
             * @j2sNative
             * this.backingArray.setFloat64(baseOffset, value, true);
             */{}
        }
    }
    
    protected final void storeFloat(int index, float value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            /**
             * @j2sNative
             * this.backingArray.setFloat32(baseOffset, value, false);
             */{}
        } else {
            /**
             * @j2sNative
             * this.backingArray.setFloat32(baseOffset, value, true);
             */{}
        }
    }
}
