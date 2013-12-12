/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

/**
 * Container for a message (data and object references) that can
 * be sent through an IBinder.  A Parcel can contain both flattened data
 * that will be unflattened on the other side of the IPC (using the various
 * methods here for writing specific types, or the general
 * {@link Parcelable} interface), and references to live {@link IBinder}
 * objects that will result in the other side receiving a proxy IBinder
 * connected with the original IBinder in the Parcel.
 *
 * <p class="note">Parcel is <strong>not</strong> a general-purpose
 * serialization mechanism.  This class (and the corresponding
 * {@link Parcelable} API for placing arbitrary objects into a Parcel) is
 * designed as a high-performance IPC transport.  As such, it is not
 * appropriate to place any Parcel data in to persistent storage: changes
 * in the underlying implementation of any of the data in the Parcel can
 * render older data unreadable.</p>
 * 
 * <p>The bulk of the Parcel API revolves around reading and writing data
 * of various types.  There are six major classes of such functions available.</p>
 * 
 * <h3>Primitives</h3>
 * 
 * <p>The most basic data functions are for writing and reading primitive
 * data types: {@link #writeByte}, {@link #readByte}, {@link #writeDouble},
 * {@link #readDouble}, {@link #writeFloat}, {@link #readFloat}, {@link #writeInt},
 * {@link #readInt}, {@link #writeLong}, {@link #readLong},
 * {@link #writeString}, {@link #readString}.  Most other
 * data operations are built on top of these.  The given data is written and
 * read using the endianess of the host CPU.</p>
 * 
 * <h3>Primitive Arrays</h3>
 * 
 * <p>There are a variety of methods for reading and writing raw arrays
 * of primitive objects, which generally result in writing a 4-byte length
 * followed by the primitive data items.  The methods for reading can either
 * read the data into an existing array, or create and return a new array.
 * These available types are:</p>
 * 
 * <ul>
 * <li> {@link #writeBooleanArray(boolean[])},
 * {@link #readBooleanArray(boolean[])}, {@link #createBooleanArray()}
 * <li> {@link #writeByteArray(byte[])},
 * {@link #writeByteArray(byte[], int, int)}, {@link #readByteArray(byte[])},
 * {@link #createByteArray()}
 * <li> {@link #writeCharArray(char[])}, {@link #readCharArray(char[])},
 * {@link #createCharArray()}
 * <li> {@link #writeDoubleArray(double[])}, {@link #readDoubleArray(double[])},
 * {@link #createDoubleArray()}
 * <li> {@link #writeFloatArray(float[])}, {@link #readFloatArray(float[])},
 * {@link #createFloatArray()}
 * <li> {@link #writeIntArray(int[])}, {@link #readIntArray(int[])},
 * {@link #createIntArray()}
 * <li> {@link #writeLongArray(long[])}, {@link #readLongArray(long[])},
 * {@link #createLongArray()}
 * <li> {@link #writeStringArray(String[])}, {@link #readStringArray(String[])},
 * {@link #createStringArray()}.
 * <li> {@link #writeSparseBooleanArray(SparseBooleanArray)},
 * {@link #readSparseBooleanArray()}.
 * </ul>
 * 
 * <h3>Parcelables</h3>
 * 
 * <p>The {@link Parcelable} protocol provides an extremely efficient (but
 * low-level) protocol for objects to write and read themselves from Parcels.
 * You can use the direct methods {@link #writeParcelable(Parcelable, int)}
 * and {@link #readParcelable(ClassLoader)} or
 * {@link #writeParcelableArray} and
 * {@link #readParcelableArray(ClassLoader)} to write or read.  These
 * methods write both the class type and its data to the Parcel, allowing
 * that class to be reconstructed from the appropriate class loader when
 * later reading.</p>
 * 
 * <p>There are also some methods that provide a more efficient way to work
 * with Parcelables: {@link #writeTypedArray},
 * {@link #writeTypedList(List)},
 * {@link #readTypedArray} and {@link #readTypedList}.  These methods
 * do not write the class information of the original object: instead, the
 * caller of the read function must know what type to expect and pass in the
 * appropriate {@link Parcelable.Creator Parcelable.Creator} instead to
 * properly construct the new object and read its data.  (To more efficient
 * write and read a single Parceable object, you can directly call
 * {@link Parcelable#writeToParcel Parcelable.writeToParcel} and
 * {@link Parcelable.Creator#createFromParcel Parcelable.Creator.createFromParcel}
 * yourself.)</p>
 * 
 * <h3>Bundles</h3>
 * 
 * <p>A special type-safe container, called {@link Bundle}, is available
 * for key/value maps of heterogeneous values.  This has many optimizations
 * for improved performance when reading and writing data, and its type-safe
 * API avoids difficult to debug type errors when finally marshalling the
 * data contents into a Parcel.  The methods to use are
 * {@link #writeBundle(Bundle)}, {@link #readBundle()}, and
 * {@link #readBundle(ClassLoader)}.
 * 
 * <h3>Active Objects</h3>
 * 
 * <p>An unusual feature of Parcel is the ability to read and write active
 * objects.  For these objects the actual contents of the object is not
 * written, rather a special token referencing the object is written.  When
 * reading the object back from the Parcel, you do not get a new instance of
 * the object, but rather a handle that operates on the exact same object that
 * was originally written.  There are two forms of active objects available.</p>
 * 
 * <p>{@link Binder} objects are a core facility of Android's general cross-process
 * communication system.  The {@link IBinder} interface describes an abstract
 * protocol with a Binder object.  Any such interface can be written in to
 * a Parcel, and upon reading you will receive either the original object
 * implementing that interface or a special proxy implementation
 * that communicates calls back to the original object.  The methods to use are
 * {@link #writeStrongBinder(IBinder)},
 * {@link #writeStrongInterface(IInterface)}, {@link #readStrongBinder()},
 * {@link #writeBinderArray(IBinder[])}, {@link #readBinderArray(IBinder[])},
 * {@link #createBinderArray()},
 * {@link #writeBinderList(List)}, {@link #readBinderList(List)},
 * {@link #createBinderArrayList()}.</p>
 * 
 * <p>FileDescriptor objects, representing raw Linux file descriptor identifiers,
 * can be written and {@link ParcelFileDescriptor} objects returned to operate
 * on the original file descriptor.  The returned file descriptor is a dup
 * of the original file descriptor: the object and fd is different, but
 * operating on the same underlying file stream, with the same position, etc.
 * The methods to use are {@link #writeFileDescriptor(FileDescriptor)},
 * {@link #readFileDescriptor()}.
 * 
 * <h3>Untyped Containers</h3>
 * 
 * <p>A final class of methods are for writing and reading standard Java
 * containers of arbitrary types.  These all revolve around the
 * {@link #writeValue(Object)} and {@link #readValue(ClassLoader)} methods
 * which define the types of objects allowed.  The container methods are
 * {@link #writeArray(Object[])}, {@link #readArray(ClassLoader)},
 * {@link #writeList(List)}, {@link #readList(List, ClassLoader)},
 * {@link #readArrayList(ClassLoader)},
 * {@link #writeMap(Map)}, {@link #readMap(Map, ClassLoader)},
 * {@link #writeSparseArray(SparseArray)},
 * {@link #readSparseArray(ClassLoader)}.
 */
public final class Parcel {
	private static final boolean DEBUG_RECYCLE = false;
	private static final String TAG = "Parcel";

	@SuppressWarnings({ "UnusedDeclaration" })
	private int mObject; // used by native code
	@SuppressWarnings({ "UnusedDeclaration" })
	private int mOwnObject; // used by native code
	private RuntimeException mStack;

	private static final int POOL_SIZE = 6;
	private static final Parcel[] sOwnedPool = new Parcel[POOL_SIZE];
	private static final Parcel[] sHolderPool = new Parcel[POOL_SIZE];

	private static final int VAL_NULL = -1;
	private static final int VAL_STRING = 0;
	private static final int VAL_INTEGER = 1;
	private static final int VAL_MAP = 2;
	private static final int VAL_BUNDLE = 3;
	private static final int VAL_PARCELABLE = 4;
	private static final int VAL_SHORT = 5;
	private static final int VAL_LONG = 6;
	private static final int VAL_FLOAT = 7;
	private static final int VAL_DOUBLE = 8;
	private static final int VAL_BOOLEAN = 9;
	private static final int VAL_CHARSEQUENCE = 10;
	private static final int VAL_LIST = 11;
	private static final int VAL_SPARSEARRAY = 12;
	private static final int VAL_BYTEARRAY = 13;
	private static final int VAL_STRINGARRAY = 14;
	private static final int VAL_IBINDER = 15;
	private static final int VAL_PARCELABLEARRAY = 16;
	private static final int VAL_OBJECTARRAY = 17;
	private static final int VAL_INTARRAY = 18;
	private static final int VAL_LONGARRAY = 19;
	private static final int VAL_BYTE = 20;
	private static final int VAL_SERIALIZABLE = 21;
	private static final int VAL_SPARSEBOOLEANARRAY = 22;
	private static final int VAL_BOOLEANARRAY = 23;
	private static final int VAL_CHARSEQUENCEARRAY = 24;

	// The initial int32 in a Binder call's reply Parcel header:
	private static final int EX_SECURITY = -1;
	private static final int EX_BAD_PARCELABLE = -2;
	private static final int EX_ILLEGAL_ARGUMENT = -3;
	private static final int EX_NULL_POINTER = -4;
	private static final int EX_ILLEGAL_STATE = -5;
	private static final int EX_HAS_REPLY_HEADER = -128; // special; see below

	private String mAction;
    private Uri mData;
    private String mType;
    private String mPackage;
    private ComponentName mComponent;
    private int mFlags;
    private HashSet<String> mCategories;
    
    public String processName;
    public int descriptionRes;
    public boolean enabled = true;
    public boolean exported = false;
    public ApplicationInfo applicationInfo;
    
    public void writeToParcel(ComponentInfo componetInfo){
        processName=componetInfo.processName;
        descriptionRes=componetInfo.descriptionRes;
        enabled=componetInfo.enabled;
        exported=componetInfo.exported;
        applicationInfo=componetInfo.applicationInfo;
    }
    
    public Parcel setAction(String action) {
        mAction = action;
        return this;
    }
    
    public String getAction() {
        return mAction;
    }
    
    public Parcel setData(Uri data) {
        mData = data;
        mType = null;
        return this;
    }
    
    public Uri getData() {
        return mData;
    }
    
    public Parcel setType(String type) {
        mData = null;
        mType = type;
        return this;
    }
    
    public String getType() {
        return mType;
    }
    public Parcel setFlags(int flags) {
        mFlags = flags;
        return this;
    }
    
    public int getFlags() {
        return mFlags;
    }
    
    public Parcel setComponent(ComponentName component) {
        mComponent = component;
        return this;
    }
    
    public ComponentName getComponent() {
        return mComponent;
    }
    
    public Parcel addCategory(HashSet<String> category) {
        mCategories = category;
        return this;
    }
    
    public Set<String> getCategories() {
        return mCategories;
    }
    
	public final static Parcelable.Creator<String> STRING_CREATOR = new Parcelable.Creator<String>() {
		public String[] newArray(int size) {
			return new String[size];
		}

		@Override
		public String createFromParcel(Parcel source) {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	public final native int dataPosition();

	static FileDescriptor openFileDescriptor(String file, int mode)
			throws FileNotFoundException {
		return null;
	}

	static void closeFileDescriptor(FileDescriptor desc) throws IOException {
	}

	public void enforceInterface(String descriptor) {
		// TODO Auto-generated method stub
	}

	public IBinder readStrongBinder() {
		// TODO Auto-generated method stub
		return null;
	}

	public void writeString(String interfaceDescriptor) {
		// TODO Auto-generated method stub
	}

	public void setDataPosition(int i) {
		// TODO Auto-generated method stub
	}

	public void writeInt(int mRequestCode) {
		// TODO Auto-generated method stub
	}

	public String readString() {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Special function for writing information at the front of the Parcel
     * indicating that no exception occurred.
     *
     * @see #writeException
     * @see #readException
     */
    public final void writeNoException() {
        // Despite the name of this function ("write no exception"),
        // it should instead be thought of as "write the RPC response
        // header", but because this function name is written out by
        // the AIDL compiler, we're not going to rename it.
        //
        // The response header, in the non-exception case (see also
        // writeException above, also called by the AIDL compiler), is
        // either a 0 (the default case), or EX_HAS_REPLY_HEADER if
        // StrictMode has gathered up violations that have occurred
        // during a Binder call, in which case we write out the number
        // of violations and their details, serialized, before the
        // actual RPC respons data.  The receiving end of this is
        // readException(), below.
/*        if (StrictMode.hasGatheredViolations()) {
            writeInt(EX_HAS_REPLY_HEADER);
            final int sizePosition = dataPosition();
            writeInt(0);  // total size of fat header, to be filled in later
            StrictMode.writeGatheredViolationsToParcel(this);
            final int payloadPosition = dataPosition();
            setDataPosition(sizePosition);
            writeInt(payloadPosition - sizePosition);  // header size
            setDataPosition(payloadPosition);
        } else {
            writeInt(0);
        }*/
    }
    
    /**
     * Special function for reading an exception result from the header of
     * a parcel, to be used after receiving the result of a transaction.  This
     * will throw the exception for you if it had been written to the Parcel,
     * otherwise return and let you read the normal result data from the Parcel.
     *
     * @see #writeException
     * @see #writeNoException
     */
    public final void readException() {
//        int code = readExceptionCode();
//        if (code != 0) {
//            String msg = readString();
//            readException(code, msg);
//        }
    }
    
	public int readInt() {
		// TODO Auto-generated method stub
		return 0;
	}

	public static Parcel obtain() {
        return new Parcel();
	}

	public void writeInterfaceToken(String descriptor) {
		// TODO Auto-generated method stub
	}

	public void writeStringArray(String[] packages) {
		// TODO Auto-generated method stub
	}

	public void recycle() {
		// TODO Auto-generated method stub
	}

	public void writeStrongBinder(IBinder token) {
		// TODO Auto-generated method stub
	}

	public void writeTypedList(List intents) {
		// TODO Auto-generated method stub
	}

	public void writeBundle(Bundle state) {
		// TODO Auto-generated method stub

	}
    /**
     * Please use {@link #writeBundle} instead.  Flattens a Map into the parcel
     * at the current dataPosition(),
     * growing dataCapacity() if needed.  The Map keys must be String objects.
     * The Map values are written using {@link #writeValue} and must follow
     * the specification there.
     * 
     * <p>It is strongly recommended to use {@link #writeBundle} instead of
     * this method, since the Bundle class provides a type-safe API that
     * allows you to avoid mysterious type errors at the point of marshalling.
     */
    public final void writeMap(Map val) {
        writeMapInternal((Map<String,Object>) val);
    }
    /**
     * Flatten a Map into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.  The Map keys must be String objects.
     */
    /* package */ void writeMapInternal(Map<String,Object> val) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        Set<Map.Entry<String,Object>> entries = val.entrySet();
        writeInt(entries.size());
        for (Map.Entry<String,Object> e : entries) {
            writeValue(e.getKey());
            writeValue(e.getValue());
        }
    }

    /**
     * Read a typed object from a parcel.  The given class loader will be
     * used to load any enclosed Parcelables.  If it is null, the default class
     * loader will be used.
     */
    public final Object readValue(ClassLoader loader) {
        int type = readInt();

        switch (type) {
        case VAL_NULL:
            return null;

        case VAL_STRING:
            return readString();

        case VAL_INTEGER:
            return readInt();

        case VAL_MAP:
            return readHashMap(loader);

     //   case VAL_PARCELABLE:
     //       return readParcelable(loader);

        case VAL_SHORT:
            return (short) readInt();

      //  case VAL_LONG:
      //      return readLong();

     //   case VAL_FLOAT:
       //     return readFloat();

       // case VAL_DOUBLE:
      //      return readDouble();

        case VAL_BOOLEAN:
            return readInt() == 1;

      //  case VAL_CHARSEQUENCE:
       //     return readCharSequence();

     //   case VAL_LIST:
       //     return readArrayList(loader);

       // case VAL_BOOLEANARRAY:
        //    return createBooleanArray();        

      //  case VAL_BYTEARRAY:
      //      return createByteArray();

        case VAL_STRINGARRAY:
       //     return readStringArray();

        case VAL_CHARSEQUENCEARRAY:
        //    return readCharSequenceArray();

        case VAL_IBINDER:
            return readStrongBinder();

        case VAL_OBJECTARRAY:
      //      return readArray(loader);

        case VAL_INTARRAY:
       //     return createIntArray();

        case VAL_LONGARRAY:
      //      return createLongArray();

        case VAL_BYTE:
       //     return readByte();

        case VAL_SERIALIZABLE:
       //     return readSerializable();

        case VAL_PARCELABLEARRAY:
       //     return readParcelableArray(loader);

        case VAL_SPARSEARRAY:
          //  return readSparseArray(loader);

        case VAL_SPARSEBOOLEANARRAY:
          //  return readSparseBooleanArray();

        case VAL_BUNDLE:
           // return readBundle(loader); // loading will be deferred

        default:
            int off = dataPosition() - 4;
            throw new RuntimeException(
                "Parcel " + this + ": Unmarshalling unknown type code " + type + " at offset " + off);
        }
    }
    
    
    
    /**
     * Flatten a generic object in to a parcel.  The given Object value may
     * currently be one of the following types:
     *
     * <ul>
     * <li> null
     * <li> String
     * <li> Byte
     * <li> Short
     * <li> Integer
     * <li> Long
     * <li> Float
     * <li> Double
     * <li> Boolean
     * <li> String[]
     * <li> boolean[]
     * <li> byte[]
     * <li> int[]
     * <li> long[]
     * <li> Object[] (supporting objects of the same type defined here).
     * <li> {@link Bundle}
     * <li> Map (as supported by {@link #writeMap}).
     * <li> Any object that implements the {@link Parcelable} protocol.
     * <li> Parcelable[]
     * <li> CharSequence (as supported by {@link TextUtils#writeToParcel}).
     * <li> List (as supported by {@link #writeList}).
     * <li> {@link SparseArray} (as supported by {@link #writeSparseArray(SparseArray)}).
     * <li> {@link IBinder}
     * <li> Any object that implements Serializable (but see
     *      {@link #writeSerializable} for caveats).  Note that all of the
     *      previous types have relatively efficient implementations for
     *      writing to a Parcel; having to rely on the generic serialization
     *      approach is much less efficient and should be avoided whenever
     *      possible.
     * </ul>
     *
     * <p class="caution">{@link Parcelable} objects are written with
     * {@link Parcelable#writeToParcel} using contextual flags of 0.  When
     * serializing objects containing {@link ParcelFileDescriptor}s,
     * this may result in file descriptor leaks when they are returned from
     * Binder calls (where {@link Parcelable#PARCELABLE_WRITE_RETURN_VALUE}
     * should be used).</p>
     */
    public final void writeValue(Object v) {
        if (v == null) {
            writeInt(VAL_NULL);
        } else if (v instanceof String) {
            writeInt(VAL_STRING);
            writeString((String) v);
        } else if (v instanceof Integer) {
            writeInt(VAL_INTEGER);
            writeInt((Integer) v);
        } else if (v instanceof Map) {
            writeInt(VAL_MAP);
            writeMap((Map) v);
        } else if (v instanceof Bundle) {
            // Must be before Parcelable
            writeInt(VAL_BUNDLE);
            writeBundle((Bundle) v);
        } 
        
        /*
         * else if (v instanceof Parcelable) {
            writeInt(VAL_PARCELABLE);
            writeParcelable((Parcelable) v, 0);
        } else if (v instanceof Short) {
            writeInt(VAL_SHORT);
            writeInt(((Short) v).intValue());
        } else if (v instanceof Long) {
            writeInt(VAL_LONG);
            writeLong((Long) v);
        } else if (v instanceof Float) {
            writeInt(VAL_FLOAT);
            writeFloat((Float) v);
        } else if (v instanceof Double) {
            writeInt(VAL_DOUBLE);
            writeDouble((Double) v);
        } else if (v instanceof Boolean) {
            writeInt(VAL_BOOLEAN);
            writeInt((Boolean) v ? 1 : 0);
        } else if (v instanceof CharSequence) {
            // Must be after String
            writeInt(VAL_CHARSEQUENCE);
            writeCharSequence((CharSequence) v);
        } else if (v instanceof List) {
            writeInt(VAL_LIST);
            writeList((List) v);
        } else if (v instanceof SparseArray) {
            writeInt(VAL_SPARSEARRAY);
            writeSparseArray((SparseArray) v);
        } else if (v instanceof boolean[]) {
            writeInt(VAL_BOOLEANARRAY);
            writeBooleanArray((boolean[]) v);
        } else if (v instanceof byte[]) {
            writeInt(VAL_BYTEARRAY);
            writeByteArray((byte[]) v);
        } else if (v instanceof String[]) {
            writeInt(VAL_STRINGARRAY);
            writeStringArray((String[]) v);
        } else if (v instanceof CharSequence[]) {
            // Must be after String[] and before Object[]
            writeInt(VAL_CHARSEQUENCEARRAY);
            writeCharSequenceArray((CharSequence[]) v);
        } else if (v instanceof IBinder) {
            writeInt(VAL_IBINDER);
            writeStrongBinder((IBinder) v);
        } else if (v instanceof Parcelable[]) {
            writeInt(VAL_PARCELABLEARRAY);
            writeParcelableArray((Parcelable[]) v, 0);
        } else if (v instanceof Object[]) {
            writeInt(VAL_OBJECTARRAY);
            writeArray((Object[]) v);
        } else if (v instanceof int[]) {
            writeInt(VAL_INTARRAY);
            writeIntArray((int[]) v);
        } else if (v instanceof long[]) {
            writeInt(VAL_LONGARRAY);
            writeLongArray((long[]) v);
        } else if (v instanceof Byte) {
            writeInt(VAL_BYTE);
            writeInt((Byte) v);
        } else if (v instanceof Serializable) {
            // Must be last
            writeInt(VAL_SERIALIZABLE);
            writeSerializable((Serializable) v);
        } */
        
        
        
        else {
            throw new RuntimeException("Parcel: unable to marshal value " + v);
        }
    }

	public Bundle readBundle() {
		// TODO Auto-generated method stub
		return null;
	}
    /**
     * Please use {@link #readBundle(ClassLoader)} instead (whose data must have
     * been written with {@link #writeBundle}.  Read and return a new HashMap
     * object from the parcel at the current dataPosition(), using the given
     * class loader to load any enclosed Parcelables.  Returns null if
     * the previously written map object was null.
     */
    public final HashMap readHashMap(ClassLoader loader)
    {
        int N = readInt();
        if (N < 0) {
            return null;
        }
        HashMap m = new HashMap(N);
        readMapInternal(m, N, loader);
        return m;
    }
    /* package */ void readMapInternal(Map outVal, int N,
            ClassLoader loader) {
            while (N > 0) {
                Object key = readValue(loader);
                Object value = readValue(loader);
                outVal.put(key, value);
                N--;
            }
        }
    
    public final void writeParcelable(Parcelable p, int parcelableFlags) {
//        if (p == null) {
//            writeString(null);
//            return;
//        }
//        String name = p.getClass().getName();
//        writeString(name);
//        p.writeToParcel(this, parcelableFlags);
    }
    
    public final <T extends Parcelable> T readParcelable(ClassLoader loader) {
    	return null;
//        String name = readString();
//        if (name == null) {
//            return null;
//        }
//        Parcelable.Creator<T> creator;
//        //synchronized (mCreators) {
//            HashMap<String,Parcelable.Creator> map = mCreators.get(loader);
//            if (map == null) {
//                map = new HashMap<String,Parcelable.Creator>();
//                mCreators.put(loader, map);
//            }
//            creator = map.get(name);
//            if (creator == null) {
//                try {
//                    Class c = loader == null ?
//                        Class.forName(name) : Class.forName(name, true, loader);
//                    Field f = c.getField("CREATOR");
//                    f.setAccessible(true);
//                    creator = (Parcelable.Creator)f.get(null);
//                }
//                catch (SecurityException e) {
//                    Log.e(TAG, "Access denied when unmarshalling: "
//                                        + name + ", e: " + e);
//                    throw new BadParcelableException(
//                            "SecurityException when unmarshalling: " + name);
//                }
//                catch (IllegalAccessException e) {
//                    Log.e(TAG, "Class not found when unmarshalling: "
//                                        + name + ", e: " + e);
//                    throw new BadParcelableException(
//                            "IllegalAccessException when unmarshalling: " + name);
//                }
//                catch (ClassNotFoundException e) {
//                    Log.e(TAG, "Class not found when unmarshalling: "
//                                        + name + ", e: " + e);
//                    throw new BadParcelableException(
//                            "ClassNotFoundException when unmarshalling: " + name);
//                }
//                catch (ClassCastException e) {
//                    throw new BadParcelableException("Parcelable protocol requires a "
//                                        + "Parcelable.Creator object called "
//                                        + " CREATOR on class " + name);
//                }
//                catch (NoSuchFieldException e) {
//                    throw new BadParcelableException("Parcelable protocol requires a "
//                                        + "Parcelable.Creator object called "
//                                        + " CREATOR on class " + name);
//                }
//                if (creator == null) {
//                    throw new BadParcelableException("Parcelable protocol requires a "
//                                        + "Parcelable.Creator object called "
//                                        + " CREATOR on class " + name);
//                }
//
//                map.put(name, creator);
//            //}
//        }
//
//        return creator.createFromParcel(this);
    }
    
	/**
	* Read and return a new ArrayList containing a particular object type from
	* the parcel that was written with {@link #writeTypedList} at the
	* current dataPosition().  Returns null if the
	* previously written list object was null.  The list <em>must</em> have
	* previously been written via {@link #writeTypedList} with the same object
	* type.
	*
	* @return A newly created ArrayList containing objects with the same data
	*         as those that were previously written.
	*
	* @see #writeTypedList
	*/
	public final <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> c) {
		int N = readInt();
		if (N < 0) {
			return null;
		}
		ArrayList<T> l = new ArrayList<T>(N);
		while (N > 0) {
			if (readInt() != 0) {
				l.add(c.createFromParcel(this));
			} else {
				l.add(null);
			}
			N--;
		}
		return l;
	}

	public void writeLong(long mStartOffset) {
		// TODO Auto-generated method stub
		
	}

	public long readLong() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void writeDouble(double mStartOffset) {
		// TODO Auto-generated method stub
		
	}

	public double readDouble() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void writeFloat(double mStartOffset) {
		// TODO Auto-generated method stub
		
	}

	public long readFloat() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String[] readStringArray() {
		// TODO Auto-generated method stub
		return null;
	}

	public void writeTypedArray(PatternMatcher[] uriPermissionPatterns,
			int parcelableFlags) {
		// TODO Auto-generated method stub
		
	}

	public PatternMatcher[] createTypedArray(Parcelable.Creator<PatternMatcher> creator) {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * @j2sNative
     * console.log("Missing method: readParcelableArray");
     */
    @MayloonStubAnnotation()
    public final Parcelable[] readParcelableArray(ClassLoader loader) {
        System.out.println("Stub" + " Function : readParcelableArray");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeFloatArray");
     */
    @MayloonStubAnnotation()
    public final void writeFloatArray(float[] val) {
        System.out.println("Stub" + " Function : writeFloatArray");
        return;
    }

//    /**
//     * @j2sNative
//     * console.log("Missing method: readStringArray");
//     */
//    @MayloonStubAnnotation()
//    public final String[] readStringArray() {
//        System.out.println("Stub" + " Function : readStringArray");
//        return null;
//    }

//    /**
//     * @j2sNative console.log("Missing method: readBundle");
//     */
//    @MayloonStubAnnotation()
//    public final Bundle readBundle() {
//        System.out.println("Stub" + " Function : readBundle");
//        return null;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: writeFloat");
     */
    @MayloonStubAnnotation()
    public final void writeFloat(float val) {
        System.out.println("Stub" + " Function : writeFloat");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readSerializable");
     */
    @MayloonStubAnnotation()
    public final Serializable readSerializable() {
        System.out.println("Stub" + " Function : readSerializable");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createFloatArray");
     */
    @MayloonStubAnnotation()
    public final float[] createFloatArray() {
        System.out.println("Stub" + " Function : createFloatArray");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeCharArray");
     */
    @MayloonStubAnnotation()
    public final void writeCharArray(char[] val) {
        System.out.println("Stub" + " Function : writeCharArray");
        return;
    }

//    /**
//     * @j2sNative console.log("Missing method: writeString");
//     */
//    @MayloonStubAnnotation()
//    public final void writeString(String
//            interfaceDescriptor) {
//        System.out.println("Stub" +
//                " Function : writeString");
//        return;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: readException");
     */
    @MayloonStubAnnotation()
    public final void readException(int code, String msg) {
        System.out.println("Stub" + " Function : readException");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeByteArray");
     */
    @MayloonStubAnnotation()
    public final void writeByteArray(byte[] b, int offset, int len) {
        System.out.println("Stub" + " Function : writeByteArray");
        return;
    }

//    /**
//     * @j2sNative console.log("Missing method: readException");
//     */
//    @MayloonStubAnnotation()
//    public final void readException() {
//        System.out.println("Stub" + " Function : readException");
//        return;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: readByteArray");
     */
    @MayloonStubAnnotation()
    public final void readByteArray(byte[] val) {
        System.out.println("Stub" + " Function : readByteArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readByte");
     */
    @MayloonStubAnnotation()
    public final byte readByte() {
        System.out.println("Stub" + " Function : readByte");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: obtain");
     */
    @MayloonStubAnnotation()
    protected static final Parcel obtain(int obj) {
        System.out.println("Stub" + " Function : obtain");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: dataAvail");
     */
    @MayloonStubAnnotation()
    public final int dataAvail() {
        System.out.println("Stub" + " Function : dataAvail");
        return 0;
    }

//    /**
//     * @j2sNative console.log("Missing method: writeInt");
//     */
//    @MayloonStubAnnotation()
//    public final void writeInt(int
//            mRequestCode) {
//        System.out.println("Stub" +
//                " Function : writeInt");
//        return;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: Parcel");
     */
    @MayloonStubAnnotation()
    private void Parcel(int obj) {
        System.out.println("Stub" + " Function : Parcel");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readBinderList");
     */
    @MayloonStubAnnotation()
    public final void readBinderList(List<IBinder> list) {
        System.out.println("Stub" + " Function : readBinderList");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: marshall");
     */
    @MayloonStubAnnotation()
    public final byte[] marshall() {
        System.out.println("Stub" + " Function : marshall");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeBooleanArray");
     */
    @MayloonStubAnnotation()
    public final void writeBooleanArray(boolean[] val) {
        System.out.println("Stub" + " Function : writeBooleanArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeByte");
     */
    @MayloonStubAnnotation()
    public final void writeByte(byte val) {
        System.out.println("Stub" + " Function : writeByte");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createBinderArray");
     */
    @MayloonStubAnnotation()
    public final IBinder[] createBinderArray() {
        System.out.println("Stub" + " Function : createBinderArray");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeDoubleArray");
     */
    @MayloonStubAnnotation()
    public final void writeDoubleArray(double[] val) {
        System.out.println("Stub" + " Function : writeDoubleArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeArray");
     */
    @MayloonStubAnnotation()
    public final void writeArray(Object[] val) {
        System.out.println("Stub" + " Function : writeArray");
        return;
    }

//    /**
//     * @j2sNative console.log("Missing method: readFloat");
//     */
//    @MayloonStubAnnotation()
//    public final float
//            readFloat() {
//        System.out.println("Stub" + " Function : readFloat");
//        return 0;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: dataCapacity");
     */
    @MayloonStubAnnotation()
    public final int dataCapacity() {
        System.out.println("Stub" + " Function : dataCapacity");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readLongArray");
     */
    @MayloonStubAnnotation()
    public final void readLongArray(long[] val) {
        System.out.println("Stub" + " Function : readLongArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createCharArray");
     */
    @MayloonStubAnnotation()
    public final char[] createCharArray() {
        System.out.println("Stub" + " Function : createCharArray");
        return null;
    }

//    /**
//     * @j2sNative console.log("Missing method: readDouble");
//     */
//    @MayloonStubAnnotation()
//    public final double
//            readDouble() {
//        System.out.println("Stub" + " Function : readDouble");
//        return 0;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: readArray");
     */
    @MayloonStubAnnotation()
    public final Object[] readArray(ClassLoader loader) {
        System.out.println("Stub" + " Function : readArray");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readCharArray");
     */
    @MayloonStubAnnotation()
    public final void readCharArray(char[] val) {
        System.out.println("Stub" + " Function : readCharArray");
        return;
    }

//    /**
//     * @j2sNative console.log("Missing method: writeNoException");
//     */
//    @MayloonStubAnnotation()
//    public final void writeNoException() {
//        System.out.println("Stub" + " Function : writeNoException");
//        return;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: readBundle");
     */
    @MayloonStubAnnotation()
    public final Bundle readBundle(ClassLoader loader) {
        System.out.println("Stub" + " Function : readBundle");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createByteArray");
     */
    @MayloonStubAnnotation()
    public final byte[] createByteArray() {
        System.out.println("Stub" + " Function : createByteArray");
        return null;
    }

//    /**
//     * @j2sNative console.log("Missing method: writeDouble");
//     */
//    @MayloonStubAnnotation()
//    public final void writeDouble(double
//            mStartOffset) {
//        System.out.println("Stub" +
//                " Function : writeDouble");
//        return;
//    }

//    /**
//     * @j2sNative console.log("Missing method: readString");
//     */
//    @MayloonStubAnnotation()
//    public final String
//            readString() {
//        System.out.println("Stub" + " Function : readString");
//        return null;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: createStringArrayList");
     */
    @MayloonStubAnnotation()
    public final ArrayList<String> createStringArrayList() {
        System.out.println("Stub" + " Function : createStringArrayList");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readBinderArray");
     */
    @MayloonStubAnnotation()
    public final void readBinderArray(IBinder[] val) {
        System.out.println("Stub" + " Function : readBinderArray");
        return;
    }

//    /**
//     * @j2sNative console.log("Missing method: readInt");
//     */
//    @MayloonStubAnnotation()
//    public final int
//            readInt() {
//        System.out.println("Stub" + " Function : readInt");
//        return 0;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: writeList");
     */
    @MayloonStubAnnotation()
    public final void writeList(List val) {
        System.out.println("Stub" + " Function : writeList");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readStringArray");
     */
    @MayloonStubAnnotation()
    public final void readStringArray(String[] val) {
        System.out.println("Stub" + " Function : readStringArray");
        return;
    }

//    /**
//     * @j2sNative console.log("Missing method: readParcelable");
//     */
//    @MayloonStubAnnotation()
//    public final T readParcelable(ClassLoader loader) {
//        System.out.println("Stub" + " Function : readParcelable");
//        return null;
//    }

//    /**
//     * @j2sNative console.log("Missing method: writeLong");
//     */
//    @MayloonStubAnnotation()
//    public final void writeLong(long
//            mStartOffset) {
//        System.out.println("Stub" +
//                " Function : writeLong");
//        return;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: writeException");
     */
    @MayloonStubAnnotation()
    public final void writeException(Exception e) {
        System.out.println("Stub" + " Function : writeException");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readDoubleArray");
     */
    @MayloonStubAnnotation()
    public final void readDoubleArray(double[] val) {
        System.out.println("Stub" + " Function : readDoubleArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createIntArray");
     */
    @MayloonStubAnnotation()
    public final int[] createIntArray() {
        System.out.println("Stub" + " Function : createIntArray");
        return null;
    }

//    /**
//     * @j2sNative console.log("Missing method: readLong");
//     */
//    @MayloonStubAnnotation()
//    public final long
//            readLong() {
//        System.out.println("Stub" + " Function : readLong");
//        return 0;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: createStringArray");
     */
    @MayloonStubAnnotation()
    public final String[] createStringArray() {
        System.out.println("Stub" + " Function : createStringArray");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createBinderArrayList");
     */
    @MayloonStubAnnotation()
    public final ArrayList<IBinder> createBinderArrayList() {
        System.out.println("Stub" + " Function : createBinderArrayList");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createBooleanArray");
     */
    @MayloonStubAnnotation()
    public final boolean[] createBooleanArray() {
        System.out.println("Stub" + " Function : createBooleanArray");
        return null;
    }

//    /**
//     * @j2sNative console.log("Missing method: obtain");
//     */
//    @MayloonStubAnnotation()
//    public static Parcel obtain() {
//        System.out.println("Stub" + " Function : obtain");
//        return null;
//    }

    /**
     * @j2sNative
     * console.log("Missing method: readFloatArray");
     */
    @MayloonStubAnnotation()
    public final void readFloatArray(float[] val) {
        System.out.println("Stub" + " Function : readFloatArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readBooleanArray");
     */
    @MayloonStubAnnotation()
    public final void readBooleanArray(boolean[] val) {
        System.out.println("Stub" + " Function : readBooleanArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeStringList");
     */
    @MayloonStubAnnotation()
    public final void writeStringList(List<String> val) {
        System.out.println("Stub" + " Function : writeStringList");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readArrayList");
     */
    @MayloonStubAnnotation()
    public final ArrayList readArrayList(ClassLoader loader) {
        System.out.println("Stub" + " Function : readArrayList");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setDataSize");
     */
    @MayloonStubAnnotation()
    public final void setDataSize(int size) {
        System.out.println("Stub" + " Function : setDataSize");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeByteArray");
     */
    @MayloonStubAnnotation()
    public final void writeByteArray(byte[] b) {
        System.out.println("Stub" + " Function : writeByteArray");
        return;
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
     * console.log("Missing method: writeBinderList");
     */
    @MayloonStubAnnotation()
    public final void writeBinderList(List<IBinder> val) {
        System.out.println("Stub" + " Function : writeBinderList");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeSerializable");
     */
    @MayloonStubAnnotation()
    public final void writeSerializable(Serializable s) {
        System.out.println("Stub" + " Function : writeSerializable");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setDataCapacity");
     */
    @MayloonStubAnnotation()
    public final void setDataCapacity(int size) {
        System.out.println("Stub" + " Function : setDataCapacity");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createLongArray");
     */
    @MayloonStubAnnotation()
    public final long[] createLongArray() {
        System.out.println("Stub" + " Function : createLongArray");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readIntArray");
     */
    @MayloonStubAnnotation()
    public final void readIntArray(int[] val) {
        System.out.println("Stub" + " Function : readIntArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeLongArray");
     */
    @MayloonStubAnnotation()
    public final void writeLongArray(long[] val) {
        System.out.println("Stub" + " Function : writeLongArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeIntArray");
     */
    @MayloonStubAnnotation()
    public final void writeIntArray(int[] val) {
        System.out.println("Stub" + " Function : writeIntArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createDoubleArray");
     */
    @MayloonStubAnnotation()
    public final double[] createDoubleArray() {
        System.out.println("Stub" + " Function : createDoubleArray");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeBinderArray");
     */
    @MayloonStubAnnotation()
    public final void writeBinderArray(IBinder[] val) {
        System.out.println("Stub" + " Function : writeBinderArray");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: writeFileDescriptor");
     */
    @MayloonStubAnnotation()
    public final void writeFileDescriptor(FileDescriptor val) {
        System.out.println("Stub" + " Function : writeFileDescriptor");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: dataSize");
     */
    @MayloonStubAnnotation()
    public final int dataSize() {
        System.out.println("Stub" + " Function : dataSize");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: hasFileDescriptors");
     */
    @MayloonStubAnnotation()
    public final boolean hasFileDescriptors() {
        System.out.println("Stub" + " Function : hasFileDescriptors");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: readStringList");
     */
    @MayloonStubAnnotation()
    public final void readStringList(List<String> list) {
        System.out.println("Stub" + " Function : readStringList");
        return;
    }
}
