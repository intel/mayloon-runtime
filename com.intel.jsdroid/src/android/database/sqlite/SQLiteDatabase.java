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

package android.database.sqlite;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.os.SystemProperties;
import android.util.Log;

/**
 * Exposes methods to manage a SQLite database.
 * <p>SQLiteDatabase has methods to create, delete, execute SQL commands, and
 * perform other common database management tasks.
 * <p>See the Notepad sample application in the SDK for an example of creating
 * and managing a database.
 * <p> Database names must be unique within an application, not across all
 * applications.
 *
 * <h3>Localized Collation - ORDER BY</h3>
 * <p>In addition to SQLite's default <code>BINARY</code> collator, Android supplies
 * two more, <code>LOCALIZED</code>, which changes with the system's current locale
 * if you wire it up correctly (XXX a link needed!), and <code>UNICODE</code>, which
 * is the Unicode Collation Algorithm and not tailored to the current locale.
 */
public class SQLiteDatabase extends SQLiteClosable {
    private static final String TAG = "Database";
    private static boolean DEBUG_NODB = false;
    private static final int EVENT_DB_OPERATION = 52000;
    private static final int EVENT_DB_CORRUPT = 75004;
    public static HashMap sqlDBList = new HashMap();
    public static int sqlDBsize;
    /**
     * @j2sNative
     * var sqlDB = null;
     * var storage = null;
     */{}
    public static String mName ;  // used to construct indexedDB
    /**
     * Algorithms used in ON CONFLICT clause
     * http://www.sqlite.org/lang_conflict.html
     */
    /**
     *  When a constraint violation occurs, an immediate ROLLBACK occurs,
     * thus ending the current transaction, and the command aborts with a
     * return code of SQLITE_CONSTRAINT. If no transaction is active
     * (other than the implied transaction that is created on every command)
     *  then this algorithm works the same as ABORT.
     */
    public static final int CONFLICT_ROLLBACK = 1;

    /**
     * When a constraint violation occurs,no ROLLBACK is executed
     * so changes from prior commands within the same transaction
     * are preserved. This is the default behavior.
     */
    public static final int CONFLICT_ABORT = 2;

    /**
     * When a constraint violation occurs, the command aborts with a return
     * code SQLITE_CONSTRAINT. But any changes to the database that
     * the command made prior to encountering the constraint violation
     * are preserved and are not backed out.
     */
    public static final int CONFLICT_FAIL = 3;

    /**
     * When a constraint violation occurs, the one row that contains
     * the constraint violation is not inserted or changed.
     * But the command continues executing normally. Other rows before and
     * after the row that contained the constraint violation continue to be
     * inserted or updated normally. No error is returned.
     */
    public static final int CONFLICT_IGNORE = 4;

    /**
     * When a UNIQUE constraint violation occurs, the pre-existing rows that
     * are causing the constraint violation are removed prior to inserting
     * or updating the current row. Thus the insert or update always occurs.
     * The command continues executing normally. No error is returned.
     * If a NOT NULL constraint violation occurs, the NULL value is replaced
     * by the default value for that column. If the column has no default
     * value, then the ABORT algorithm is used. If a CHECK constraint
     * violation occurs then the IGNORE algorithm is used. When this conflict
     * resolution strategy deletes rows in order to satisfy a constraint,
     * it does not invoke delete triggers on those rows.
     *  This behavior might change in a future release.
     */
    public static final int CONFLICT_REPLACE = 5;

    /**
     * use the following when no conflict action is specified.
     */
    public static final int CONFLICT_NONE = 0;
    private static final String[] CONFLICT_VALUES = new String[]
            {"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE "};

    /**
     * Maximum Length Of A LIKE Or GLOB Pattern
     * The pattern matching algorithm used in the default LIKE and GLOB implementation
     * of SQLite can exhibit O(N^2) performance (where N is the number of characters in
     * the pattern) for certain pathological cases. To avoid denial-of-service attacks
     * the length of the LIKE or GLOB pattern is limited to SQLITE_MAX_LIKE_PATTERN_LENGTH bytes.
     * The default value of this limit is 50000. A modern workstation can evaluate
     * even a pathological LIKE or GLOB pattern of 50000 bytes relatively quickly.
     * The denial of service problem only comes into play when the pattern length gets
     * into millions of bytes. Nevertheless, since most useful LIKE or GLOB patterns
     * are at most a few dozen bytes in length, paranoid application developers may
     * want to reduce this parameter to something in the range of a few hundred
     * if they know that external users are able to generate arbitrary patterns.
     */
    public static final int SQLITE_MAX_LIKE_PATTERN_LENGTH = 50000;

    /**
     * Flag for {@link #openDatabase} to open the database for reading and writing.
     * If the disk is full, this may fail even before you actually write anything.
     *
     * {@more} Note that the value of this flag is 0, so it is the default.
     */
    public static final int OPEN_READWRITE = 0x00000000;          // update native code if changing

    /**
     * Flag for {@link #openDatabase} to open the database for reading only.
     * This is the only reliable way to open a database if the disk may be full.
     */
    public static final int OPEN_READONLY = 0x00000001;           // update native code if changing

    private static final int OPEN_READ_MASK = 0x00000001;         // update native code if changing

    /**
     * Flag for {@link #openDatabase} to open the database without support for localized collators.
     *
     * {@more} This causes the collator <code>LOCALIZED</code> not to be created.
     * You must be consistent when using this flag to use the setting the database was
     * created with.  If this is set, {@link #setLocale} will do nothing.
     */
    public static final int NO_LOCALIZED_COLLATORS = 0x00000010;  // update native code if changing

    /**
     * Flag for {@link #openDatabase} to create the database file if it does not already exist.
     */
    public static final int CREATE_IF_NECESSARY = 0x10000000;     // update native code if changing

    /**
     * Indicates whether the most-recently started transaction has been marked as successful.
     */
    private boolean mInnerTransactionIsSuccessful;

    /**
     * Valid during the life of a transaction, and indicates whether the entire transaction (the
     * outer one and all of the inner ones) so far has been successful.
     */
    private boolean mTransactionIsSuccessful;

    /**
     * Valid during the life of a transaction.
     */
    //private SQLiteTransactionListener mTransactionListener;

    /** Synchronize on this when accessing the database */
    //private final ReentrantLock mLock = new ReentrantLock(true);

    //private long mLockAcquiredWallTime = 0L;
    //private long mLockAcquiredThreadTime = 0L;

    // limit the frequency of complaints about each database to one within 20 sec
    // unless run command adb shell setprop log.tag.Database VERBOSE
    private static final int LOCK_WARNING_WINDOW_IN_MS = 20000;
    /** If the lock is held this long then a warning will be printed when it is released. */
    private static final int LOCK_ACQUIRED_WARNING_TIME_IN_MS = 300;
    private static final int LOCK_ACQUIRED_WARNING_THREAD_TIME_IN_MS = 100;
    private static final int LOCK_ACQUIRED_WARNING_TIME_IN_MS_ALWAYS_PRINT = 2000;

    private static final int SLEEP_AFTER_YIELD_QUANTUM = 1000;

    // The pattern we remove from database filenames before
    // potentially logging them.
    private static final Pattern EMAIL_IN_DB_PATTERN = Pattern.compile("[\\w\\.\\-]+@[\\w\\.\\-]+");

    //private long mLastLockMessageTime = 0L;

    // Things related to query logging/sampling for debugging
    // slow/frequent queries during development.  Always log queries
    // which take (by default) 500ms+; shorter queries are sampled
    // accordingly.  Commit statements, which are typically slow, are
    // logged together with the most recently executed SQL statement,
    // for disambiguation.  The 500ms value is configurable via a
    // SystemProperty, but developers actively debugging database I/O
    // should probably use the regular log tunable,
    // LOG_SLOW_QUERIES_PROPERTY, defined below.
    private static int sQueryLogTimeInMillis = 0;  // lazily initialized
    private static final int QUERY_LOG_SQL_LENGTH = 64;
    private static final String COMMIT_SQL = "COMMIT;";
    private final Random mRandom = new Random();
    private String mLastSqlStatement = null;

    // String prefix for slow database query EventLog records that show
    // lock acquistions of the database.
    /* package */ static final String GET_LOCK_LOG_PREFIX = "GETLOCK:";

    /** Used by native code, do not rename */
    /* package */ int mNativeHandle = 0;

    /** Used to make temp table names unique */
    /* package */ int mTempTableSequence = 0;

    /** The path for the database file */
    private String mPath;

    /** The anonymized path for the database file for logging purposes */
    private String mPathForLogs = null;  // lazily populated

    /** The flags passed to open/create */
    private int mFlags;
    
    private CursorFactory mFactory;



    private WeakHashMap<SQLiteClosable, Object> mPrograms;

    /**
     * for each instance of this class, a cache is maintained to store
     * the compiled query statement ids returned by sqlite database.
     *     key = sql statement with "?" for bind args
     *     value = {@link SQLiteCompiledSql}
     * If an application opens the database and keeps it open during its entire life, then
     * there will not be an overhead of compilation of sql statements by sqlite.
     *
     * why is this cache NOT static? because sqlite attaches compiledsql statements to the
     * struct created when {@link SQLiteDatabase#openDatabase(String, CursorFactory, int)} is
     * invoked.
     *
     * this cache has an upper limit of mMaxSqlCacheSize (settable by calling the method
     * (@link setMaxCacheSize(int)}). its default is 0 - i.e., no caching by default because
     * most of the apps don't use "?" syntax in their sql, caching is not useful for them.
     */
    // tqi3
    /* package */ Map<String, SQLiteCompiledSql> mCompiledQueries = new HashMap<String, SQLiteCompiledSql>();
    /**
     * @hide
     */
    public static final int MAX_SQL_CACHE_SIZE = 250;
    private int mMaxSqlCacheSize = MAX_SQL_CACHE_SIZE; // max cache size per Database instance
    private int mCacheFullWarnings;
    private static final int MAX_WARNINGS_ON_CACHESIZE_CONDITION = 1;

    /** maintain stats about number of cache hits and misses */
    private int mNumCacheHits;
    private int mNumCacheMisses;

    /** the following 2 members maintain the time when a database is opened and closed */
    private String mTimeOpened = null;
    private String mTimeClosed = null;

    /** Used to find out where this object was created in case it never got closed. */
    private Throwable mStackTrace = null;

    // System property that enables logging of slow queries. Specify the threshold in ms.
    private static final String LOG_SLOW_QUERIES_PROPERTY = "db.log.slow_query_threshold";
   // private final int mSlowQueryThreshold;


    public interface CursorFactory {
        /**
         * See
         * {@link SQLiteCursor#SQLiteCursor(SQLiteDatabase, SQLiteCursorDriver,
         * String, SQLiteQuery)}.
         */
    	 public Cursor newCursor(SQLiteDatabase db,
                 SQLiteCursorDriver masterQuery, String editTable,
                 SQLiteQuery query);

    }

    /**
     * Open the database according to the flags {@link #OPEN_READWRITE}
     * {@link #OPEN_READONLY} {@link #CREATE_IF_NECESSARY} and/or {@link #NO_LOCALIZED_COLLATORS}.
     *
     * <p>Sets the locale of the database to the  the system's current locale.
     * Call {@link #setLocale} if you would like something else.</p>
     *
     * @param path to database file to open and/or create
     * @param factory an optional factory class that is called to instantiate a
     *            cursor when query is called, or null for default
     * @param flags to control database access mode
     * @return the newly opened database
     * @throws SQLiteException if the database cannot be opened
     */
    //TODO: MayLoon: need to implement it.
    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags) {
        SQLiteDatabase sqliteDatabase = null;
        try {
            // Open the database.
            sqliteDatabase = new SQLiteDatabase(path, factory, flags);
        } catch (Exception e) {
            // Try to recover from this, if we can.
            // TODO: should we do this for other open failures?
            Log.e(TAG, "Deleting and re-creating corrupt database " + path, e);
            if (!path.equalsIgnoreCase(":memory")) {
                // delete is only for non-memory database files
                new File(path).delete();
            }
            sqliteDatabase = new SQLiteDatabase(path, factory, flags);
        }
        //ActiveDatabases.getInstance().mActiveDatabases.add(
        //        new WeakReference<SQLiteDatabase>(sqliteDatabase));
        return sqliteDatabase;
    }

//    public static SQLiteDatabase openOrCreateDatabase(String name)
//    {
//
//    	 SQLiteDatabase sqliteDatabase = null;
//         sqliteDatabase = new SQLiteDatabase(name);
//         return sqliteDatabase;
//    }
    /**
     * Equivalent to openDatabase(file.getPath(), factory, CREATE_IF_NECESSARY).
     */
    public static SQLiteDatabase openOrCreateDatabase(File file, CursorFactory factory) {
        return openOrCreateDatabase(file.getPath(), factory);
    }

    /**
     * Equivalent to openDatabase(path, factory, CREATE_IF_NECESSARY).
     */
    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory) {
        return openDatabase(path, factory, CREATE_IF_NECESSARY);
    }

    public void close() {
        if (mName != null) {
            if (sqlDBList.containsKey(mName) && isOpen()) {
                if (sqlDBsize == 1) {
                    sqlDBsize = 0;
                    mNativeHandle = 0;
                    /**
                     * @j2sNative
                     * var data = this.sqlDB.exportData();
                     * this.sqlDB.close();
                     * if (this.mName != null) {
                     *     this.sqlDBList.remove(this.mName);
                     *     this.storage.removeItem(this.mName);
                     *     this.storage.setItem(this.mName, JSON.stringify(data));
                     * }
                     */{}
                } else {
                    sqlDBsize--;
                }
            }
        }
    }

    /**
     * Gets the database version.
     *
     * @return the database version
     */
    public int getVersion() {
        if(DEBUG_NODB) return 1;
    	int version = 0;
        /**
         * @j2sNative
         * var notifier;
         * try {
         *     var data = this.sqlDB.exec("SELECT version FROM version;");
         *     version = parseInt(data[0][0].value);
         *     notifier = 1;
         * } catch(e) {
         *     notifier = -1;
         *     throw(e.message);
         * }
         */{}
         return version;
    }

    /**
     * Sets the database version.
     *
     * @param version the new database version
     */
    public void setVersion(int version) {
    	if(DEBUG_NODB) return;
        /**
         * @j2sNative
         * var notifier;
         * try {
         *     var q = "SELECT sql FROM sqlite_master WHERE tbl_name = 'version'";
         *     var data = this.sqlDB.exec(q);
         *     if (!data||data.length==0) {
         *         this.sqlDB.exec("CREATE TABLE version (version INTEGER);");
         *         this.sqlDB.exec("INSERT INTO version (version) VALUES ( " + version + " );");
         *     } else {
         *         this.sqlDB.exec("DELETE FROM version");
         *         this.sqlDB.exec("INSERT INTO version (version) VALUES ( " + version + " );");
         *     }
         *     notifier = 1;
         * } catch (e) {
         *     notifier = -1;
         *     throw(e.message);
         * }
         */{}
    }

    /**
     * Returns the maximum size the database may grow to.
     *
     * @return the new maximum database size
     */
    public long getMaximumSize() {
		return mCacheFullWarnings;

    }

    /**
     * Sets the maximum size the database will grow to. The maximum size cannot
     * be set below the current size.
     *
     * @param numBytes the maximum database size, in bytes
     * @return the new maximum database size
     */
    public long setMaximumSize(long numBytes) {
		return numBytes;

    }

    /**
     * Returns the current database page size, in bytes.
     *
     * @return the database page size, in bytes
     */
    public long getPageSize() {
		return mCacheFullWarnings;

    }

    /**
     * Sets the database page size. The page size must be a power of two. This
     * method does not work if any data has been written to the database file,
     * and must be called right after the database has been created.
     *
     * @param numBytes the database page size, in bytes
     */
    public void setPageSize(long numBytes) {
        execSQL("PRAGMA page_size = " + numBytes);
    }






    /**
     * Finds the name of the first table, which is editable.
     *
     * @param tables a list of tables
     * @return the first table listed
     */
    public static String findEditTable(String tables) {
            int spacepos = tables.indexOf(' ');
            int commapos = tables.indexOf(',');

            if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
                return tables.substring(0, spacepos);
            } else if (commapos > 0 && (commapos < spacepos || spacepos < 0) ) {
                return tables.substring(0, commapos);
            }
            return tables;

    }





   /**
    * Query the given URL, returning a {@link Cursor} over the result set.
    *
    * @param distinct true if you want each row to be unique, false otherwise.
    * @param table The table name to compile the query against.
    * @param columns A list of which columns to return. Passing null will
    *            return all columns, which is discouraged to prevent reading
    *            data from storage that isn't going to be used.
    * @param selection A filter declaring which rows to return, formatted as an
    *            SQL WHERE clause (excluding the WHERE itself). Passing null
    *            will return all rows for the given table.
    * @param selectionArgs You may include ?s in selection, which will be
    *         replaced by the values from selectionArgs, in order that they
    *         appear in the selection. The values will be bound as Strings.
    * @param groupBy A filter declaring how to group rows, formatted as an SQL
    *            GROUP BY clause (excluding the GROUP BY itself). Passing null
    *            will cause the rows to not be grouped.
    * @param having A filter declare which row groups to include in the cursor,
    *            if row grouping is being used, formatted as an SQL HAVING
    *            clause (excluding the HAVING itself). Passing null will cause
    *            all row groups to be included, and is required when row
    *            grouping is not being used.
    * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
    *            (excluding the ORDER BY itself). Passing null will use the
    *            default sort order, which may be unordered.
    * @param limit Limits the number of rows returned by the query,
    *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
    * @return A {@link Cursor} object, which is positioned before the first entry. Note that
    * {@link Cursor}s are not synchronized, see the documentation for more details.
    * @see Cursor
    */
   public Cursor query(boolean distinct, String table, String[] columns,
           String selection, String[] selectionArgs, String groupBy,
           String having, String orderBy, String limit) {
       return queryWithFactory(null, distinct, table, columns, selection, selectionArgs,
               groupBy, having, orderBy, limit);
   }

   /**
    * Query the given URL, returning a {@link Cursor} over the result set.
    *
    * @param cursorFactory the cursor factory to use, or null for the default factory
    * @param distinct true if you want each row to be unique, false otherwise.
    * @param table The table name to compile the query against.
    * @param columns A list of which columns to return. Passing null will
    *            return all columns, which is discouraged to prevent reading
    *            data from storage that isn't going to be used.
    * @param selection A filter declaring which rows to return, formatted as an
    *            SQL WHERE clause (excluding the WHERE itself). Passing null
    *            will return all rows for the given table.
    * @param selectionArgs You may include ?s in selection, which will be
    *         replaced by the values from selectionArgs, in order that they
    *         appear in the selection. The values will be bound as Strings.
    * @param groupBy A filter declaring how to group rows, formatted as an SQL
    *            GROUP BY clause (excluding the GROUP BY itself). Passing null
    *            will cause the rows to not be grouped.
    * @param having A filter declare which row groups to include in the cursor,
    *            if row grouping is being used, formatted as an SQL HAVING
    *            clause (excluding the HAVING itself). Passing null will cause
    *            all row groups to be included, and is required when row
    *            grouping is not being used.
    * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
    *            (excluding the ORDER BY itself). Passing null will use the
    *            default sort order, which may be unordered.
    * @param limit Limits the number of rows returned by the query,
    *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
    * @return A {@link Cursor} object, which is positioned before the first entry. Note that
    * {@link Cursor}s are not synchronized, see the documentation for more details.
    * @see Cursor
    */
   public Cursor queryWithFactory(CursorFactory cursorFactory,
           boolean distinct, String table, String[] columns,
           String selection, String[] selectionArgs, String groupBy,
           String having, String orderBy, String limit) {
	   if(DEBUG_NODB) return null;
       String sql = SQLiteQueryBuilder.buildQueryString(
               distinct, table, columns, selection, groupBy, having, orderBy, limit);
       return rawQueryWithFactory(
               cursorFactory, sql, selectionArgs, findEditTable(table));
   }

   /**
    * Query the given table, returning a {@link Cursor} over the result set.
    *
    * @param table The table name to compile the query against.
    * @param columns A list of which columns to return. Passing null will
    *            return all columns, which is discouraged to prevent reading
    *            data from storage that isn't going to be used.
    * @param selection A filter declaring which rows to return, formatted as an
    *            SQL WHERE clause (excluding the WHERE itself). Passing null
    *            will return all rows for the given table.
    * @param selectionArgs You may include ?s in selection, which will be
    *         replaced by the values from selectionArgs, in order that they
    *         appear in the selection. The values will be bound as Strings.
    * @param groupBy A filter declaring how to group rows, formatted as an SQL
    *            GROUP BY clause (excluding the GROUP BY itself). Passing null
    *            will cause the rows to not be grouped.
    * @param having A filter declare which row groups to include in the cursor,
    *            if row grouping is being used, formatted as an SQL HAVING
    *            clause (excluding the HAVING itself). Passing null will cause
    *            all row groups to be included, and is required when row
    *            grouping is not being used.
    * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
    *            (excluding the ORDER BY itself). Passing null will use the
    *            default sort order, which may be unordered.
    * @return A {@link Cursor} object, which is positioned before the first entry. Note that
    * {@link Cursor}s are not synchronized, see the documentation for more details.
    * @see Cursor
    */
   public Cursor query(String table, String[] columns, String selection,
           String[] selectionArgs, String groupBy, String having,
           String orderBy) {
       return query(false, table, columns, selection, selectionArgs, groupBy,
               having, orderBy, null /* limit */);
   }

   /**
    * Query the given table, returning a {@link Cursor} over the result set.
    *
    * @param table The table name to compile the query against.
    * @param columns A list of which columns to return. Passing null will
    *            return all columns, which is discouraged to prevent reading
    *            data from storage that isn't going to be used.
    * @param selection A filter declaring which rows to return, formatted as an
    *            SQL WHERE clause (excluding the WHERE itself). Passing null
    *            will return all rows for the given table.
    * @param selectionArgs You may include ?s in selection, which will be
    *         replaced by the values from selectionArgs, in order that they
    *         appear in the selection. The values will be bound as Strings.
    * @param groupBy A filter declaring how to group rows, formatted as an SQL
    *            GROUP BY clause (excluding the GROUP BY itself). Passing null
    *            will cause the rows to not be grouped.
    * @param having A filter declare which row groups to include in the cursor,
    *            if row grouping is being used, formatted as an SQL HAVING
    *            clause (excluding the HAVING itself). Passing null will cause
    *            all row groups to be included, and is required when row
    *            grouping is not being used.
    * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
    *            (excluding the ORDER BY itself). Passing null will use the
    *            default sort order, which may be unordered.
    * @param limit Limits the number of rows returned by the query,
    *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
    * @return A {@link Cursor} object, which is positioned before the first entry. Note that
    * {@link Cursor}s are not synchronized, see the documentation for more details.
    * @see Cursor
    */
   public Cursor query(String table, String[] columns, String selection,
           String[] selectionArgs, String groupBy, String having,
           String orderBy, String limit) {

       return query(false, table, columns, selection, selectionArgs, groupBy,
               having, orderBy, limit);
   }


    /**
     * Runs the provided SQL and returns a {@link Cursor} over the result set.
     *
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     *     which will be replaced by the values from selectionArgs. The
     *     values will be bound as Strings.
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     */
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return rawQueryWithFactory(null, sql, selectionArgs, null);
    }

    /**
     * Runs the provided SQL and returns a cursor over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     *     which will be replaced by the values from selectionArgs. The
     *     values will be bound as Strings.
     * @param editTable the name of the first table, which is editable
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     */
    public Cursor rawQueryWithFactory(
            CursorFactory cursorFactory, String sql, String[] selectionArgs,
            String editTable) {   
    		SQLiteCursorDriver driver = new SQLiteDirectCursorDriver(this, sql, editTable);
    		Cursor cursor = null;
    		try {
                cursor = driver.query(
                        cursorFactory != null ? cursorFactory : mFactory,
                        selectionArgs);
            } finally {            
            }
            
        	return cursor;
    		
    }

    /**
     * Runs the provided SQL and returns a cursor over the result set.
     * The cursor will read an initial set of rows and the return to the caller.
     * It will continue to read in batches and send data changed notifications
     * when the later batches are ready.
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     *     which will be replaced by the values from selectionArgs. The
     *     values will be bound as Strings.
     * @param initialRead set the initial count of items to read from the cursor
     * @param maxRead set the count of items to read on each iteration after the first
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     *
     * This work is incomplete and not fully tested or reviewed, so currently
     * hidden.
     * @hide
     */
    public Cursor rawQuery(String sql, String[] selectionArgs,
            int initialRead, int maxRead) {
        Cursor c = rawQueryWithFactory(
                null, sql, selectionArgs, null);

        return c;
    }





    /**
     * Convenience method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>values</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>values</code> is empty.
     * @param values this map contains the initial column values for the
     *            row. The keys should be the column names and the values the
     *            column values
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    public long insert(String table, String nullColumnHack, ContentValues values) {
    	if(DEBUG_NODB) return 1;
        try {
        	long temp = 0;


            temp = insertWithOnConflict(table, nullColumnHack, values, this.CONFLICT_NONE);


        	return temp;
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + values, e);
            return -1;
        }
    }

    /**
     * Convenience method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>values</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>values</code> is empty.
     * @param values this map contains the initial column values for the
     *            row. The keys should be the column names and the values the
     *            column values
     * @throws SQLException
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values)
            throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    /**
     * Convenience method for replacing a row in the database.
     *
     * @param table the table in which to replace the row
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>initialValues</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>initialValues</code> is empty.
     * @param initialValues this map contains the initial column values for
     *   the row.
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues,
                    CONFLICT_REPLACE);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + initialValues, e);
            return -1;
        }
    }

    /**
     * Convenience method for replacing a row in the database.
     *
     * @param table the table in which to replace the row
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>initialValues</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>initialValues</code> is empty.
     * @param initialValues this map contains the initial column values for
     *   the row. The key
     * @throws SQLException
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    public long replaceOrThrow(String table, String nullColumnHack,
            ContentValues initialValues) throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, initialValues,
                CONFLICT_REPLACE);
    }

    /**
     * General method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>initialValues</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>initialValues</code> is empty.
     * @param initialValues this map contains the initial column values for the
     *            row. The keys should be the column names and the values the
     *            column values
     * @param conflictAlgorithm for insert conflict resolver
     * @return the row ID of the newly inserted row
     * OR the primary key of the existing row if the input param 'conflictAlgorithm' =
     * {@link #CONFLICT_IGNORE}
     * OR -1 if any error
     */
    public long insertWithOnConflict(String table, String nullColumnHack,
            ContentValues initialValues, int conflictAlgorithm) {
    	if(DEBUG_NODB) return 1;
        // Measurements show most sql lengths <= 152
        StringBuilder sql = new StringBuilder(152);
        sql.append("INSERT");
        sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(" INTO ");
        sql.append(table);
        // Measurements show most values lengths < 40
        StringBuilder values = new StringBuilder(40);

        Set<Map.Entry<String, Object>> entrySet = null;
        if (initialValues != null && initialValues.size() > 0) {
            entrySet = initialValues.valueSet();
            Iterator<Map.Entry<String, Object>> entriesIter = entrySet.iterator();
            sql.append('(');

            boolean needSeparator = false;
            while (entriesIter.hasNext()) {
                if (needSeparator) {
                    sql.append(", ");
                    values.append(", ");
                }
                needSeparator = true;
                Map.Entry<String, Object> entry = entriesIter.next();
                sql.append("'"+entry.getKey()+"'");
                try {
                    String value = URLEncoder.encode(entry.getValue().toString(), "UTF-8");
                    values.append("'"+value+"'");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            sql.append(')');
        } else {
            sql.append("(" + nullColumnHack + ") ");
            values.append("NULL");
        }

        sql.append(" VALUES (");
        sql.append(values);
        sql.append(");");
        long insertId = 0;
        String temp = sql.toString();
        /**
         * @j2sNative
         * var notifier;
         * try {
         *     this.sqlDB.exec(temp);
         *     var data = this.sqlDB.exec("SELECT LAST_INSERT_ROWID();");
         *     notifier = data[0][0].value;
         * } catch(e) {
         *     notifier = -1;
         *     throw(e.message);
         * }
         * insertId = parseInt(notifier);
         */{}

        Log.v("insertedId", "" + insertId);
		return insertId;

    }

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting.
     *            Passing null will delete all rows.
     * @return the number of rows affected if a whereClause is passed in, 0
     *         otherwise. To remove all rows and get a count pass "1" as the
     *         whereClause.
     */
    public int delete(String table, String whereClause, String[] whereArgs) {
        if(DEBUG_NODB) return 1;
        String sql = "DELETE FROM " + table;
        int result = 0;
        if(whereClause != null) {
            sql += " WHERE ";
            sql += whereClause;
        }
        if(whereArgs != null) {
            for (int i=0; i<whereArgs.length; i++) {
                sql = sql.replaceFirst("\\?", "'" + whereArgs[i] + "'");
            }
        }
        /**
         * @j2sNative
         * var notifier;
         * try {
         *     this.sqlDB.exec(sql);
         *     notifier = 1;
         * } catch(e) {
         *     notifier = -1;
         *     throw(e.message);
         * }
         * result = parseInt(notifier);
         */{}
		return result;
    }

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     *            valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     *            Passing null will update all rows.
     * @return the number of rows affected
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return updateWithOnConflict(table, values, whereClause, whereArgs, CONFLICT_NONE);
    }

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     *            valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     *            Passing null will update all rows.
     * @param conflictAlgorithm for update conflict resolver
     * @return the number of rows affected
     */
    public int updateWithOnConflict(String table, ContentValues values,
            String whereClause, String[] whereArgs, int conflictAlgorithm) {
    	if(DEBUG_NODB) return 0;
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("Empty values");
        }

        int result = 0;

        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(table);
        sql.append(" SET ");

        Set<Map.Entry<String, Object>> entrySet = values.valueSet();
        Iterator<Map.Entry<String, Object>> entriesIter = entrySet.iterator();

        while (entriesIter.hasNext()) {
            Map.Entry<String, Object> entry = entriesIter.next();
            sql.append(entry.getKey());
            try {
                String value = URLEncoder.encode(entry.getValue().toString(), "UTF-8");
                sql.append("='"+value+"'");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (entriesIter.hasNext()) {
                sql.append(", ");
            }
        }

        if(whereClause != null) {
            sql.append(" WHERE ");
            sql.append(whereClause);
        }
        String temp = sql.toString();
        if (whereArgs != null) {
            for (int i=0; i<whereArgs.length; i++) {
                temp = temp.replaceFirst("\\?", "'" + whereArgs[i] + "'");
            }
        }
        /**
         * @j2sNative
         * var notifier;
         * try {
         *     this.sqlDB.exec(temp);
         *     //var data = this.sqlDB.exec("SELECT change_count();");
         *     notifier = 1;
         * } catch(e) {
         *     notifier = -1;
         *     throw(e.message);
         * }
         * result = parseInt(notifier);
         */{}

		return result;

    }

    /**
     * Execute a single SQL statement that is not a query. For example, CREATE
     * TABLE, DELETE, INSERT, etc. Multiple statements separated by semicolons are not
     * supported.  Takes a write lock.
     *
     * @throws SQLException if the SQL string is invalid
     */
    public void execSQL(String sql) {
    	if(DEBUG_NODB) return;
    	/**
         * @j2sNative
         * try {
         *     var sql = encodeURI(sql).replaceAll("%20", " ");
         *     this.sqlDB.exec(sql);
         * } catch(e) {
         *     if (this.mTransactionIsSuccessful) {
         *         this.endTransaction();
         *     }
         *     this.sqlDB.close();
         *     throw(e.message);
         * }
         */{}
    }

    /**
     * Execute a single SQL statement that is not a query. For example, CREATE
     * TABLE, DELETE, INSERT, etc. Multiple statements separated by semicolons are not
     * supported.  Takes a write lock.
     *
     * @param sql
     * @param bindArgs only byte[], String, Long and Double are supported in bindArgs.
     * @throws SQLException if the SQL string is invalid
     */
    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
    	if(DEBUG_NODB) return;
        if (bindArgs == null) {
            throw new IllegalArgumentException("Empty bindArgs");
        } else {
            if(bindArgs != null) {
                for (int i=0; i<bindArgs.length; i++) {
                    sql = sql.replaceFirst("\\?", "'" + bindArgs[i].toString() + "'");
                }
            }
        }
        /**
         * @j2sNative
         * try {
         *     this.sqlDB.exec(sql);
         * } catch(e) {
         *     this.sqlDB.close();
         *     throw(e.message);
         * }
         */{}
    }

    @Override
    protected void finalize() {
        if (isOpen()) {
            Log.e(TAG, "close() was never explicitly called on database '" +
                    mPath + "' ", mStackTrace);
           // closeClosable();
            close();
            onAllReferencesReleased();
        }
    }

    /**
     * Private constructor. See {@link #create} and {@link #openDatabase}.
     *
     * @param path The full path to the database
     * @param factory The factory to use when creating cursors, may be NULL.
     * @param flags 0 or {@link #NO_LOCALIZED_COLLATORS}.  If the database file already
     *              exists, mFlags will be updated appropriately.
     */
    private SQLiteDatabase(String path, CursorFactory factory, int flags) {
    	if (path == null) {
            throw new IllegalArgumentException("path should not be null");
        }
        mFlags = flags;
        mPath = path;
        mFactory = factory;
        String name = null;
        //TQI3: the path should be "/data/data/<package_name>/database/<database_name>"
    	//the web SQL databse name is set as <package_name>+"_"+<database_name>

        try {
            // Dynamic load the sql.js
            Object SQL = Class.forName("android.database.sqlite.SqlJS").newInstance();
            /**
             * @j2sNative
             * SQL = SQL.getSql();
             */{}
        } catch (Exception e) {
            throw new RuntimeException("Can't load database");
        }

        /**
         * @j2sNative
         * var temp = path;
         * var names = temp.split("/");
         * name = names[3]+"_"+names[5];
         */ {}
        if (name == null)
            throw new IllegalArgumentException("Name should not be null");
    	mName = name;

        /**
         * @j2sNative
         * this.storage = window.localStorage;
         * if(this.sqlDBList.get(name) != null) {
         *     this.sqlDB = this.sqlDBList.get(name);
         *     android.database.sqlite.SQLiteDatabase.sqlDBsize += 1;
         * } else {
         *     var temp = this.storage.getItem(this.mName);
         *     var data;
         *     if(temp!=null) {
         *         data = JSON.parse(temp);
         *         if (!data.length) {
         *             var start = temp.lastIndexOf(",") + 1;
         *             var stop = temp.lastIndexOf(":");
         *             data.length = parseInt(temp.substring(start, stop).replace(/\"/g, '')) + 1;
         *         }
         *     }
         *     if(data!=null) {
         *         this.sqlDB = SQL.open(data);
         *     } else {
         *         this.sqlDB = SQL.open();
         *     }
         *     this.sqlDBList.put(name, this.sqlDB);
         *     android.database.sqlite.SQLiteDatabase.sqlDBsize = 1;
         * }
         * try {
         *     var q = "SELECT sql FROM sqlite_master WHERE tbl_name = 'version'";
         *     var data = this.sqlDB.exec(q);
         *     if (!data||data.length==0) {
         *         this.sqlDB.exec("CREATE TABLE version (version INTEGER);");
         *         this.sqlDB.exec("INSERT INTO version (version) VALUES ( 0 );");
         *     }
         *     this.mNativeHandle = 1;
         * } catch (e) {
         *     throw(e.message);
         * }
         */{}
    }

    /**
     * return whether the DB is opened as read only.
     * @return true if DB is opened as read only
     */
    public boolean isReadOnly() {
        return (mFlags & OPEN_READ_MASK) == OPEN_READONLY;
    }

    /**
     * @return true if the DB is currently open (has not been closed)
     */
    public boolean isOpen() {
        return mNativeHandle != 0;
    }

    public boolean needUpgrade(int newVersion) {
        return newVersion > getVersion();
    }

    /**
     * Getter for the path to the database file.
     *
     * @return the path to our database file.
     */
    public final String getPath() {
        return mPath;
    }





    /**
     * Removes email addresses from database filenames before they're
     * logged to the EventLog where otherwise apps could potentially
     * read them.
     */
    private String getPathForLogs() {
        if (mPathForLogs != null) {
            return mPathForLogs;
        }
        if (mPath == null) {
            return null;
        }
        if (mPath.indexOf('@') == -1) {
            mPathForLogs = mPath;
        } else {
            mPathForLogs = EMAIL_IN_DB_PATTERN.matcher(mPath).replaceAll("XX@YY");
        }
        return mPathForLogs;
    }



    /**
     * return the current maxCacheSqlCacheSize
     * @hide
     */
    public synchronized int getMaxSqlCacheSize() {
        return mMaxSqlCacheSize;
    }


	@Override
	protected void onAllReferencesReleased() {
		// TODO Auto-generated method stub

	}


	public void onUpgrade(int version, int mNewVersion) {
		if(DEBUG_NODB) return;
	}

    public SQLiteDatabase getSQLDB() {
        SQLiteDatabase db = null;
        /**
         * @j2sNative
         * db = this.sqlDB;
         */{}
        return db;
    }

    /**
     * @j2sNative
     * console.log("Missing method: compileStatement");
     */
    @MayloonStubAnnotation()
    public Object compileStatement(String sql) {
        System.out.println("Stub" + " Function : compileStatement");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: yieldIfContendedSafely");
     */
    @MayloonStubAnnotation()
    public boolean yieldIfContendedSafely() {
        System.out.println("Stub" + " Function : yieldIfContendedSafely");
        return true;
    }

    /**
     * End a transaction. See beginTransaction for notes about how to use this and when transactions
     * are committed and rolled back.
     */
    public void endTransaction() {
//        if (!isOpen()) {
//            throw new IllegalStateException("database not open");
//        }
        if (mInnerTransactionIsSuccessful) {
            mInnerTransactionIsSuccessful = false;
        } else {
            mTransactionIsSuccessful = false;
        }
        if (mTransactionIsSuccessful) {
            execSQL(COMMIT_SQL);
        } else {
            execSQL("ROLLBACK;");
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: getSyncedTables");
     */
    @MayloonStubAnnotation()
    public Map<String, String> getSyncedTables() {
        System.out.println("Stub" + " Function : getSyncedTables");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: inTransaction");
     */
    @MayloonStubAnnotation()
    public boolean inTransaction() {
        System.out.println("Stub" + " Function : inTransaction");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setLocale");
     */
    @MayloonStubAnnotation()
    public void setLocale(Locale locale) {
        System.out.println("Stub" + " Function : setLocale");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: releaseMemory");
     */
    @MayloonStubAnnotation()
    public static int releaseMemory() {
        System.out.println("Stub" + " Function : releaseMemory");
        return 0;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isDbLockedByCurrentThread");
     */
    @MayloonStubAnnotation()
    public boolean isDbLockedByCurrentThread() {
        System.out.println("Stub" + " Function : isDbLockedByCurrentThread");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: yieldIfContended");
     */
    @MayloonStubAnnotation()
    public boolean yieldIfContended() {
        System.out.println("Stub" + " Function : yieldIfContended");
        return true;
    }

    /**
     * Marks the current transaction as successful. Do not do any more database work between
     * calling this and calling endTransaction. Do as little non-database work as possible in that
     * situation too. If any errors are encountered between this and endTransaction the transaction
     * will still be committed.
     *
     * @throws IllegalStateException if the current thread is not in a transaction or the
     * transaction is already marked as successful.
     */
    public void setTransactionSuccessful() {
//        if (!isOpen()) {
//            throw new IllegalStateException("database not open");
//        }
        if (mInnerTransactionIsSuccessful) {
//            throw new IllegalStateException(
//                    "setTransactionSuccessful may only be called once per call to beginTransaction");
        }
        mInnerTransactionIsSuccessful = true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: create");
     */
    @MayloonStubAnnotation()
    public static SQLiteDatabase create(CursorFactory factory) {
        System.out.println("Stub" + " Function : create");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: isDbLockedByOtherThreads");
     */
    @MayloonStubAnnotation()
    public boolean isDbLockedByOtherThreads() {
        System.out.println("Stub" + " Function : isDbLockedByOtherThreads");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: setLockingEnabled");
     */
    @MayloonStubAnnotation()
    public void setLockingEnabled(boolean lockingEnabled) {
        System.out.println("Stub" + " Function : setLockingEnabled");
        return;
    }

    public void beginTransaction() {
        beginTransactionWithListener(null);
    }

    public void beginTransactionWithListener(SQLiteTransactionListener transactionListener) {
//        if (!isOpen()) {
//            throw new IllegalStateException("database not open");
//        }
        execSQL("BEGIN EXCLUSIVE;");
        mTransactionIsSuccessful = true;
        mInnerTransactionIsSuccessful = false;
    }

    /**
     * @j2sNative
     * console.log("Missing method: yieldIfContendedSafely");
     */
    @MayloonStubAnnotation()
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        System.out.println("Stub" + " Function : yieldIfContendedSafely");
        return true;
    }
}
