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

package android.database;



import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.content.ContentValues;
import android.content.Context;


import android.database.sqlite.SQLiteDatabase;

import android.os.Parcel;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;

/**
 * Static utility methods for dealing with databases and {@link Cursor}s.
 */
public class DatabaseUtils {
    private static final String TAG = "DatabaseUtils";

    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    /**
     * Special function for writing an exception result at the header of
     * a parcel, to be used when returning an exception from a transaction.
     * exception will be re-thrown by the function in another process
     * @param reply Parcel to write to
     * @param e The Exception to be written.
     * @see Parcel#writeNoException
     * @see Parcel#writeException
     */


    /**
     * Special function for reading an exception result from the header of
     * a parcel, to be used after receiving the result of a transaction.  This
     * will throw the exception for you if it had been written to the Parcel,
     * otherwise return and let you read the normal result data from the Parcel.
     * @param reply Parcel to read from
     * @see Parcel#writeNoException
     * @see Parcel#readException
     */
    public static final void readExceptionFromParcel(Parcel reply) {
      //  int code = reply.readExceptionCode();
      //  if (code == 0) return;
       // String msg = reply.readString();
       // DatabaseUtils.readExceptionFromParcel(reply, msg, code);
    }

    public static void readExceptionWithFileNotFoundExceptionFromParcel(
            Parcel reply) throws FileNotFoundException {
     /*   int code = reply.readExceptionCode();
        if (code == 0) return;
        String msg = reply.readString();
        if (code == 1) {
            throw new FileNotFoundException(msg);
        } else {
            DatabaseUtils.readExceptionFromParcel(reply, msg, code);
        }
        */
    }

    public static void readExceptionWithOperationApplicationExceptionFromParcel(
            Parcel reply)  {
       /* int code = reply.readExceptionCode();
        if (code == 0) return;
        String msg = reply.readString();
        if (code == 10) {
            throw new OperationApplicationException(msg);
        } else {
            DatabaseUtils.readExceptionFromParcel(reply, msg, code);
        }*/
    }



    /**
     * Binds the given Object to the given SQLiteProgram using the proper
     * typing. For example, bind numbers as longs/doubles, and everything else
     * as a string by call toString() on it.
     *
     * @param prog the program to bind the object to
     * @param index the 1-based index to bind at
     * @param value the value to bind

    /**
     * Appends an SQL string to the given StringBuilder, including the opening
     * and closing single quotes. Any single quotes internal to sqlString will
     * be escaped.
     *
     * This method is deprecated because we want to encourage everyone
     * to use the "?" binding form.  However, when implementing a
     * ContentProvider, one may want to add WHERE clauses that were
     * not provided by the caller.  Since "?" is a positional form,
     * using it in this case could break the caller because the
     * indexes would be shifted to accomodate the ContentProvider's
     * internal bindings.  In that case, it may be necessary to
     * construct a WHERE clause manually.  This method is useful for
     * those cases.
     *
     * @param sb the StringBuilder that the SQL string will be appended to
     * @param sqlString the raw string to be appended, which may contain single
     *                  quotes
     */
    public static void appendEscapedSQLString(StringBuilder sb, String sqlString) {
        sb.append('\'');
        if (sqlString.indexOf('\'') != -1) {
            int length = sqlString.length();
            for (int i = 0; i < length; i++) {
                char c = sqlString.charAt(i);
                if (c == '\'') {
                    sb.append('\'');
                }
                sb.append(c);
            }
        } else
            sb.append(sqlString);
        sb.append('\'');
    }

    /**
     * SQL-escape a string.
     */
    public static String sqlEscapeString(String value) {
        StringBuilder escaper = new StringBuilder();

        DatabaseUtils.appendEscapedSQLString(escaper, value);

        return escaper.toString();
    }

    /**
     * Appends an Object to an SQL string with the proper escaping, etc.
     */
    public static final void appendValueToSql(StringBuilder sql, Object value) {
        if (value == null) {
            sql.append("NULL");
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean)value;
            if (bool) {
                sql.append('1');
            } else {
                sql.append('0');
            }
        } else {
            appendEscapedSQLString(sql, value.toString());
        }
    }

    /**
     * Concatenates two SQL WHERE clauses, handling empty or null values.
     * @hide
     */
    public static String concatenateWhere(String a, String b) {
        if (TextUtils.isEmpty(a)) {
            return b;
        }
        if (TextUtils.isEmpty(b)) {
            return a;
        }

        return "(" + a + ") AND (" + b + ")";
    }

    /**
     * return the collation key in hex format
     * @param name
     * @return the collation key in hex format
     */
   /* tqi3 public static String getHexCollationKey(String name) {
        byte [] arr = getCollationKeyInBytes(name);
        char[] keys = Hex.encodeHex(arr);
        return new String(keys, 0, getKeyLen(arr) * 2);
    } */

    private static int getKeyLen(byte[] arr) {
        if (arr[arr.length - 1] != 0) {
            return arr.length;
        } else {
            // remove zero "termination"
            return arr.length-1;
        }
    }

    /**
     * Prints the contents of a Cursor to System.out. The position is restored
     * after printing.
     *
     * @param cursor the cursor to print
     */


    /**
     * Prints the contents of a Cursor to a PrintSteam. The position is restored
     * after printing.
     *
     * @param cursor the cursor to print
     * @param stream the stream to print to
     */


    /**
     * Prints the contents of a Cursor to a StringBuilder. The position
     * is restored after printing.
     *
     * @param cursor the cursor to print
     * @param sb the StringBuilder to print to
     */


    /**
     * Prints the contents of a Cursor to a String. The position is restored
     * after printing.
     *
     * @param cursor the cursor to print
     * @return a String that contains the dumped cursor
     */


    /**
     * Prints the contents of a Cursor's current row to System.out.
     *
     * @param cursor the cursor to print from
     */


    /**
     * Prints the contents of a Cursor's current row to a PrintSteam.
     *
     * @param cursor the cursor to print
     * @param stream the stream to print to
     */


    /**
     * Dump the contents of a Cursor's current row to a String.
     *
     * @param cursor the cursor to print
     * @return a String that contains the dumped cursor row


    /**
     * Reads a String out of a field in a Cursor and writes it to a Map.
     *
     * @param cursor The cursor to read from
     * @param field The TEXT field to read
     * @param values The {@link ContentValues} to put the value into, with the field as the key

    /**
     * Reads a String out of a field in a Cursor and writes it to an InsertHelper.
     *
     * @param cursor The cursor to read from
     * @param field The TEXT field to read
     * @param inserter The InsertHelper to bind into
     * @param index the index of the bind entry in the InsertHelper


    /**
     * Reads a Integer out of a field in a Cursor and writes it to a Map.
     *
     * @param cursor The cursor to read from
     * @param field The INTEGER field to read
     * @param values The {@link ContentValues} to put the value into, with the field as the key
     * @param key The key to store the value with in the map
     */
  /**  public static void cursorIntToContentValues(Cursor cursor, String field, ContentValues values,
            String key) {
        int colIndex = cursor.getColumnIndex(field);
        if (!cursor.isNull(colIndex)) {
            values.put(key, cursor.getInt(colIndex));
        } else {
            values.put(key, (Integer) null);
        }
    }
    */

    /**
     * Reads a Long out of a field in a Cursor and writes it to a Map.
     *
     * @param cursor The cursor to read from
     * @param field The INTEGER field to read
     * @param values The {@link ContentValues} to put the value into, with the field as the key
     */
    /** public static void cursorLongToContentValues(Cursor cursor, String field, ContentValues values)
    {
        cursorLongToContentValues(cursor, field, values, field);
    } */

    /**
     * Reads a Long out of a field in a Cursor and writes it to a Map.
     *
     * @param cursor The cursor to read from
     * @param field The INTEGER field to read
     * @param values The {@link ContentValues} to put the value into
     * @param key The key to store the value with in the map
     */

    /**public static void cursorLongToContentValues(Cursor cursor, String field, ContentValues values,
            String key) {
        int colIndex = cursor.getColumnIndex(field);
        if (!cursor.isNull(colIndex)) {
            Long value = Long.valueOf(cursor.getLong(colIndex));
            values.put(key, value);
        } else {
            values.put(key, (Long) null);
        }
    }
    */

    /**
     * Reads a Double out of a field in a Cursor and writes it to a Map.
     *
     * @param cursor The cursor to read from
     * @param field The REAL field to read
     * @param values The {@link ContentValues} to put the value into
     */
   /** public static void cursorDoubleToCursorValues(Cursor cursor, String field, ContentValues values)
    {
        cursorDoubleToContentValues(cursor, field, values, field);
    } */

    /**
     * Reads a Double out of a field in a Cursor and writes it to a Map.
     *
     * @param cursor The cursor to read from
     * @param field The REAL field to read
     * @param values The {@link ContentValues} to put the value into
     * @param key The key to store the value with in the map
     */
  /**  public static void cursorDoubleToContentValues(Cursor cursor, String field,
            ContentValues values, String key) {
        int colIndex = cursor.getColumnIndex(field);
        if (!cursor.isNull(colIndex)) {
            values.put(key, cursor.getDouble(colIndex));
        } else {
            values.put(key, (Double) null);
        }
    } */

    /**
     * @j2sNative
     * console.log("Missing method: dumpCurrentRowToString");
     */
    @MayloonStubAnnotation()
    public static String dumpCurrentRowToString(Cursor cursor) {
        System.out.println("Stub" + " Function : dumpCurrentRowToString");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getCollationKey");
     */
    @MayloonStubAnnotation()
    public static String getCollationKey(String name) {
        System.out.println("Stub" + " Function : getCollationKey");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getHexCollationKey");
     */
    @MayloonStubAnnotation()
    public static String getHexCollationKey(String name) {
        System.out.println("Stub" + " Function : getHexCollationKey");
        return null;
    }
}
