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

import android.database.CursorWindow;
import android.os.SystemClock;
import android.util.Log;

/**
 * A SQLite program that represents a query that reads the resulting rows into a CursorWindow.
 * This class is used by SQLiteCursor and isn't useful itself.
 *
 * SQLiteQuery is not internally synchronized so code using a SQLiteQuery from multiple
 * threads should perform its own synchronization when using the SQLiteQuery.
 */
public class SQLiteQuery extends SQLiteProgram {
    private static final String TAG = "Cursor";

    /** The index of the unbound OFFSET parameter */
    private int mOffsetIndex;
    
    /** Args to bind on requery */
    private String[] mBindArgs;

    private boolean mClosed = false;

    private static final int SQLITE_TEXT = 0;
    private static final int SQLITE_INTEGER = 1;
    private static final int SQLITE_FLOAT = 2;
    private static final int SQLITE_BLOB = 3;
    private static final int SQLITE_NULL = 4;

    private int column_count = 0;

    /**
     * @j2sNative
     * this.datas = null;
     * this.keys = null;
     * this.types = null;
     */{}

    /**
     * Create a persistent query object.
     * 
     * @param db The database that this query object is associated with
     * @param query The SQL string for this query. 
     * @param offsetIndex The 1-based index to the OFFSET parameter, 
     */
    /* package */ SQLiteQuery(SQLiteDatabase db, String query, int offsetIndex, String[] bindArgs) {
        super(db, query);

        mOffsetIndex = offsetIndex;
        mBindArgs = bindArgs;
    }

    /**
     * Reads rows into a buffer. This method acquires the database lock.
     *
     * @param window The window to fill into
     * @return number of total rows in the query
     */
    /* package */ int fillWindow(CursorWindow window,
            int maxRead, int lastPos) {
        long timeStart = SystemClock.uptimeMillis();
//        mDatabase.lock();
//        mDatabase.logTimeStat(mSql, timeStart, SQLiteDatabase.GET_LOCK_LOG_PREFIX);
        try {
            acquireReference();
            try {
                window.acquireReference();
                // if the start pos is not equal to 0, then most likely window is
                // too small for the data set, loading by another thread
                // is not safe in this situation. the native code will ignore maxRead
                int numRows = native_fill_window(window, window.getStartPosition(), mOffsetIndex,
                        maxRead, lastPos);
                return numRows;
            } catch (IllegalStateException e){
                // simply ignore it
                return 0;
            } catch (SQLiteDatabaseCorruptException e) {
               // mDatabase.onCorruption();
                throw e;
            } finally {
                window.releaseReference();
            }
        } finally {
            releaseReference();
         // mDatabase.unlock();
        }
    }

    /**
     * Get the column count for the statement. Only valid on query based
     * statements. The database must be locked
     * when calling this method.
     * 
     * @return The number of column in the statement's result set.
     */
    /* package */ int columnCountLocked() {
        acquireReference();
        try {
            return native_column_count();
        } finally {
            releaseReference();
        }
    }

    /**
     * Retrieves the column name for the given column index. The database must be locked
     * when calling this method.
     * 
     * @param columnIndex the index of the column to get the name for
     * @return The requested column's name
     */
    /* package */ String columnNameLocked(int columnIndex) {
        acquireReference();
        try {
            return native_column_name(columnIndex);
        } finally {
            releaseReference();
        }
    }
    
    @Override
    public String toString() {
        return "SQLiteQuery: " + mSql;
    }
    
    @Override
    public void close() {
        super.close();
        mClosed = true;
    }

    /**
     * Called by SQLiteCursor when it is requeried.
     */
    /* package */ void requery() {
        query();
        if (mBindArgs != null) {
            int len = mBindArgs.length;
            try {
                for (int i = 0; i < len; i++) {
                    super.bindString(i + 1, mBindArgs[i]);
                }
            } catch (SQLiteMisuseException e) {
                StringBuilder errMsg = new StringBuilder("mSql " + mSql);
                for (int i = 0; i < len; i++) {
                    errMsg.append(" ");
                    errMsg.append(mBindArgs[i]);
                }
                errMsg.append(" ");
                IllegalStateException leakProgram = new IllegalStateException(
                        errMsg.toString(), e);
                throw leakProgram;                
            }
        }
    }

    @Override
    public void bindNull(int index) {
        mBindArgs[index - 1] = null;
        if (!mClosed) super.bindNull(index);
    }

    @Override
    public void bindLong(int index, long value) {
        mBindArgs[index - 1] = Long.toString(value);
        if (!mClosed) super.bindLong(index, value);
    }

    @Override
    public void bindDouble(int index, double value) {
        mBindArgs[index - 1] = Double.toString(value);
        if (!mClosed) super.bindDouble(index, value);
    }

    @Override
    public void bindString(int index, String value) {
        mBindArgs[index - 1] = value;
        if (!mClosed) super.bindString(index, value);
    }
    
    //TQI3:Add method to get the mSql and mBindArgs
    
    public String[] getBindArgs()
    {
    	return mBindArgs;
    }
    
    public String getmSql()
    {
    	return mSql;
    }

    private final int native_fill_window(CursorWindow javaWindow,
            int startPos, int offsetParam, int maxRead, int lastPos) {
        int err = 0;
//      sqlite3_stmt * statement = GET_STATEMENT(env, object);
        int numRows = lastPos;
        maxRead += lastPos;
        int numColumns;
//        int retryCount;
//        int boundParams;
        CursorWindow window;

        query();
//            if (statement == NULL) {
//                LOGE("Invalid statement in fillWindow()");
//                jniThrowException(env, "java/lang/IllegalStateException",
//                                  "Attempting to access a deactivated, closed, or empty cursor");
//                return 0;
//            }

            // Only do the binding if there is a valid offsetParam. If no binding needs to be done
            // offsetParam will be set to 0, an invliad value.
//            if(offsetParam > 0) {
//                // Bind the offset parameter, telling the program which row to start with
//                err = sqlite3_bind_int(statement, offsetParam, startPos);
//                if (err != SQLITE_OK) {
//                    LOGE("Unable to bind offset position, offsetParam = %d", offsetParam);
//                    jniThrowException(env, "java/lang/IllegalArgumentException",
//                                      sqlite3_errmsg(GET_HANDLE(env, object)));
//                    return 0;
//                }
//            } else {
//            }

            window = javaWindow;
            /**
             * @j2sNative
             * if (this.datas) {
             *     window.QueryResult = this.datas;
             *     window.rowNum = this.datas.length;
             * }
             */{}
            // Get the native window
//            window = get_window_from_object(env, javaWindow);
//            if (null == window) {
//                return 0;
//            }
            numColumns = column_count;
            if (!window.setNumColumns(numColumns)) {
                return 0;
            }
//            retryCount = 0;

            if (startPos > 0) {
//                int num = skip_rows(statement, startPos);
//                if (num < 0) {
//                    throw_sqlite3_exception(env, GET_HANDLE(env, object));
//                    return 0;
//                } else if (num < startPos) {
//                    LOGE("startPos %d > actual rows %d", startPos, num);
//                    return num;
//                }
            }

              while(startPos != 0 || numRows < maxRead) {
//                  err = sqlite3_step(statement);
                  /**
                   * @j2sNative
                   * if (this.datas && this.datas.length > 0) {
                   *     if (numRows < this.datas.length) {
                   *         err = 1;
                   *     } else {
                   *         err = 2;
                   *     }
                   * } else {
                   *     err = 3;
                   * }
                   */{}
                  if (err == 1) {//SQLITE_ROW
                      // Log.i(TAG, "\nStepped statement %p to row %d", statement, startPos + numRows);
//                      retryCount = 0;

                      // Allocate a new field directory for the row. This pointer is not reused
                      // since it mey be possible for it to be relocated on a call to alloc() when
                      // the field data is being allocated.
//                      {
//                          field_slot_t * fieldDir = window->allocRow();
//                          if (!fieldDir) {
//                              LOGE("Failed allocating fieldDir at startPos %d row %d", startPos, numRows);
//                              return startPos + numRows + finish_program_and_get_row_count(statement) + 1;
//                          }
//                      }

                      // Pack the row into the window
                      int i;
                      /**
                       * @j2sNative
                       * if (this.types == null) {
                       *     numRows++;
                       *     continue;
                       * }
                       */{}
                      for (i = 0; i < numColumns; i++) {
//                          int type = sqlite3_column_type(statement, i);
                          int type = -1;
                          /**
                           * @j2sNative
                           * var type_value = this.types[numRows][i].value;
                           * if (type_value == 'text') {
                           *     type = 0;//TEXT
                           * } else if (type_value == 'integer') {
                           *     type = 1;//INTEGER
                           * } else if (type_value == 'real') {
                           *     type = 2;//FLOAT
                           * } else if (type_value == 'blob') {
                           *     type = 3;//BLOB
                           * } else if (type_value == 'null') {
                           *     type = 4;//NULL
                           * }
                           */{}

                          if (type == SQLITE_TEXT) {
                              // TEXT data
//              #if WINDOW_STORAGE_UTF8
//                              uint8_t const * text = (uint8_t const *)sqlite3_column_text(statement, i);
                              // SQLite does not include the NULL terminator in size, but does
                              // ensure all strings are NULL terminated, so increase size by
                              // one to make sure we store the terminator.
//                              size_t size = sqlite3_column_bytes(statement, i) + 1;
//              #else
//                              uint8_t const * text = (uint8_t const *)sqlite3_column_text16(statement, i);
//                              size_t size = sqlite3_column_bytes16(statement, i);
//              #endif
//                              int offset = window->alloc(size);
//                              if (!offset) {
//                                  window->freeLastRow();
//                                  return startPos + numRows + finish_program_and_get_row_count(statement) + 1;
//                              }

//                              window->copyIn(offset, text, size);

                              // This must be updated after the call to alloc(), since that
                              // may move the field around in the window
//                              field_slot_t * fieldSlot = window->getFieldSlot(numRows, i);
//                              fieldSlot->type = FIELD_TYPE_STRING;
//                              fieldSlot->data.buffer.offset = offset;
//                              fieldSlot->data.buffer.size = size;

                              // Log.i(TAG,"%d,%d is TEXT with %u bytes", startPos + numRows, i, size);

                              String value;
                              /**
                               * @j2sNative
                               * value = this.datas[numRows][i].value;
                               * window.putString(value, numRows, i);
                               */{}
                          } else if (type == SQLITE_INTEGER) {
                              // INTEGER data
//                              int64_t value = sqlite3_column_int64(statement, i);
                              int value;
                              /**
                               * @j2sNative
                               * value = this.datas[numRows][i].value;
                               * window.putLong(value, numRows, i);
                               */{}
//                              if (!window.putLong(value, numRows, i)) {
//                                  window.freeLastRow();
//                                  return startPos + numRows + finish_program_and_get_row_count(statement) + 1;
//                              }
                          } else if (type == SQLITE_FLOAT) {
                              // FLOAT data
//                              double value = sqlite3_column_double(statement, i);
                              double value;
                              /**
                               * @j2sNative
                               * value = this.datas[numRows][i].value;
                               * window.putDouble(value, numRows, i);
                               */{}
//                              if(!window.putDouble(value, numRows, i)) {
//                                  window.freeLastRow();
//                                  return startPos + numRows + finish_program_and_get_row_count(statement) + 1;
//                              }
                          } else if (type == SQLITE_BLOB) {
                              // BLOB data
//                              uint8_t const * blob = (uint8_t const *)sqlite3_column_blob(statement, i);
//                              size_t size = sqlite3_column_bytes16(statement, i);
//                              int offset = window->alloc(size);
//                              if (!offset) {
//                                  window->freeLastRow();
//                                  return startPos + numRows + finish_program_and_get_row_count(statement) + 1;
//                              }

//                              window->copyIn(offset, blob, size);

                              // This must be updated after the call to alloc(), since that
                              // may move the field around in the window
//                              field_slot_t * fieldSlot = window->getFieldSlot(numRows, i);
//                              fieldSlot->type = FIELD_TYPE_BLOB;
//                              fieldSlot->data.buffer.offset = offset;
//                              fieldSlot->data.buffer.size = size;

                              // Log.i(TAG, "%d,%d is Blob with %u bytes @ %d", startPos + numRows, i, size, offset);
                          } else if (type == SQLITE_NULL) {
                              // NULL field
                              window.putNull(numRows, i);
                          } else {
                              // Unknown data
                              //throw new SQLiteException("Unknown column type when filling window");
                              break;
                          }
                      }

                      if (i < numColumns) {
                          // Not all the fields fit in the window
                          // Unknown data error happened
                          break;
                      }

                      // Mark the row as complete in the window
                      numRows++;
                  } else if (err == 2) {//SQLITE_DONE
                      // All rows processed, bail
                      Log.i(TAG, "Processed all rows");
                      break;
//                  } else if (err == SQLITE_LOCKED || err == SQLITE_BUSY) {
                      // The table is locked, retry
                      // Log.i(TAG, "Database locked, retrying");
//                      if (retryCount > 50) {
//                          LOGE("Bailing on database busy rety");
//                          break;
//                      }

                      // Sleep to give the thread holding the lock a chance to finish
//                      usleep(1000);

//                      retryCount++;
//                      continue;
                  } else {
//                      throw new SQLiteException(env, GET_HANDLE(env, object));
                      break;
                  }
              }

//        if (err == SQLITE_ROW) {
//            return -1;
//        } else {
//            //sqlite3_reset(statement);
//            return startPos + numRows;
//        }

        return startPos + numRows;
    }

    public void query () {
        String tableName = null;
        String sql = getmSql();
        String[] BindArgs = getBindArgs();

        if (BindArgs != null) {
            for (int i=0; i<BindArgs.length; i++) {
                sql = sql.replaceFirst("\\?", "'" + BindArgs[i] + "'");
            }
        }
        /**
         * @j2sNative
         * try {
         *     var key = new Array();
         *     var db = this.mDatabase.getSQLDB();
         *     var data = db.exec(sql);
         *     if(!data||data.length==0) {
         *         var DISTINCT_index = sql.indexOf("DISTINCT");
         *         var FROM_index = sql.indexOf("FROM");
         *         if(DISTINCT_index >= 0) {
         *             var temp_columns = sql.substring(DISTINCT_index+8, FROM_index-1);
         *             var sql_left = sql.slice(FROM_index+5);
         *             if(sql_left.indexOf(" ")>=0) {
         *                 var sql_left_parts = sql_left.split(" ");
         *                 tableName = sql_left_parts[0];
         *             } else {
         *                 tableName = sql_left;
         *             }
         *         } else {
         *             var temp_columns = sql.substring(7, FROM_index-1);
         *             var sql_left = sql.slice(FROM_index+5);
         *             if(sql_left.indexOf(" ")>=0) {
         *                 var sql_left_parts = sql_left.split(" ");
         *                 tableName = sql_left_parts[0];
         *             } else {
         *                 tableName = sql_left;
         *             }
         *         }
         *         if(temp_columns.indexOf("*")<0) {
         *             this.keys = temp_columns.split(", ");
         *         } else {
         *             this.keys = this.getColumnFromTable(tableName);
         *         }
         *         this.column_count = this.keys.length;
         *         this.datas = null;
         *         this.types = null;
         *     } else {
         *         var typeSql = "SELECT ";
         *         for (var i=0;i<data[0].length;i++) {
         *             key[i] = data[0][i].column;
         *             if (i != data[0].length-1) {
         *                 typeSql += " typeof(" + key[i] + "), ";
         *             } else {
         *                 typeSql += " typeof(" + key[i] + ") ";
         *             }
         *         }
         *         var FROM_index = sql.indexOf("FROM");
         *         typeSql += sql.substring(FROM_index);
         *         try {
         *             this.types = db.exec(typeSql);
         *         } catch (e) {
         *             this.types = null;
         *         }
         *         this.column_count = key.length;
         *         this.datas = data;
         *         this.keys = key;
         *     }
         * } catch (e) {
         *     throw(e.message);
         *     this.column_count = null;
         * }
         */{}
    }

    private final int native_column_count() {
        query();
        return column_count;
    }

    public String[] getColumnFromTable(String tableName) {
        String q = "SELECT sql FROM sqlite_master WHERE tbl_name = '" + tableName + "'";
        /**
         * @j2sNative
         * try {
         *     var db = this.mDatabase.getSQLDB();
         *     var data = db.exec(q);
         *     var sql_create = data[0][0].value;
         *     var position1 = sql_create.indexOf("(");
         *     var position2 = sql_create.lastIndexOf(")");
         *     var sql_key = sql_create.substring(position1+1, position2);
         *     var arr = new Array();
         *     var column = new Array();
         *     var flag = 0;
         *     var count = 0;
         *     var pre = 0;
         *     var length = sql_key.length;
         *     var i = 0;
         *     for(i=0;i<length;i++) {
         *         if(flag==0&&sql_key.charAt(i)==",") {
         *             arr[count] = sql_key.substring(pre,i);
         *             pre = i + 1;
         *             count++;
         *         }
         *         if(sql_key.charAt(i)=="(")
         *             flag++;
         *         if(sql_key.charAt(i)==")")
         *             flag--;
         *     }
         *     arr[count] = sql_key.slice(pre);
         *     i = 0;
         *     var length_column = 0;
         *     while(i<=count) {
         *         while(arr[i].charAt(0) == " ")
         *             arr[i] = arr[i].slice(1);
         *         var temp_sql = arr[i].split(" ");
         *         if(temp_sql[0]!="PRIMARY KEY"&&temp_sql[0]!="UNIQUE"&&temp_sql[0]!="CHECK"&&temp_sql[0]!="DEFAUL"&&temp_sql[0]!="COLLATE") {
         *             column[length_column] = temp_sql[0];
         *             length_column++;
         *         }
         *         i++;
         *     }
         *     return column;
         *     //Notice! There might be a Bug  ****  mColumns and column get the real values  ****
         * } catch (e) {
         *     throw(e.message);
         *     return null;
         * }
         */{return null;}
    }

    private final String native_column_name(int columnIndex) {
        /**
         * @j2sNative
         * for (var i=0; i<this.keys.length; i++) {
         *     return this.keys[columnIndex];
         * }
         */{return null;}
    }
}
