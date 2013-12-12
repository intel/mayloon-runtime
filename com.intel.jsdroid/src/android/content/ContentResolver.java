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

package android.content;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

import com.intel.mpt.annotation.MayloonStubAnnotation;

import android.app.ActivityManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;


public abstract class ContentResolver {

    @Deprecated
    public static final String SYNC_EXTRAS_ACCOUNT = "account";
    public static final String SYNC_EXTRAS_EXPEDITED = "expedited";
    @Deprecated
    public static final String SYNC_EXTRAS_FORCE = "force";
    private static boolean DEBUG_PROVIDER = false;
    /**
     * If this extra is set to true then the sync settings (like getSyncAutomatically())
     * are ignored by the sync scheduler.
     */
    public static final String SYNC_EXTRAS_IGNORE_SETTINGS = "ignore_settings";

    /**
     * If this extra is set to true then any backoffs for the initial attempt (e.g. due to retries)
     * are ignored by the sync scheduler. If this request fails and gets rescheduled then the
     * retries will still honor the backoff.
     */
    public static final String SYNC_EXTRAS_IGNORE_BACKOFF = "ignore_backoff";

    /**
     * If this extra is set to true then the request will not be retried if it fails.
     */
    public static final String SYNC_EXTRAS_DO_NOT_RETRY = "do_not_retry";

    /**
     * Setting this extra is the equivalent of setting both {@link #SYNC_EXTRAS_IGNORE_SETTINGS}
     * and {@link #SYNC_EXTRAS_IGNORE_BACKOFF}
     */
    public static final String SYNC_EXTRAS_MANUAL = "force";

    public static final String SYNC_EXTRAS_UPLOAD = "upload";
    public static final String SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS = "deletions_override";
    public static final String SYNC_EXTRAS_DISCARD_LOCAL_DELETIONS = "discard_deletions";

    /**
     * Set by the SyncManager to request that the SyncAdapter initialize itself for
     * the given account/authority pair. One required initialization step is to
     * ensure that {@link #setIsSyncable(android.accounts.Account, String, int)} has been
     * called with a >= 0 value. When this flag is set the SyncAdapter does not need to
     * do a full sync, though it is allowed to do so.
     */
    public static final String SYNC_EXTRAS_INITIALIZE = "initialize";

    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_ANDROID_RESOURCE = "android.resource";
    public static final String SCHEME_FILE = "file";

    /**
     * This is the Android platform's base MIME type for a content: URI
     * containing a Cursor of a single item.  Applications should use this
     * as the base type along with their own sub-type of their content: URIs
     * that represent a particular item.  For example, hypothetical IMAP email
     * client may have a URI
     * <code>content://com.company.provider.imap/inbox/1</code> for a particular
     * message in the inbox, whose MIME type would be reported as
     * <code>CURSOR_ITEM_BASE_TYPE + "/vnd.company.imap-msg"</code>
     *
     * <p>Compare with {@link #CURSOR_DIR_BASE_TYPE}.
     */
    public static final String CURSOR_ITEM_BASE_TYPE = "vnd.android.cursor.item";

    /**
     * This is the Android platform's base MIME type for a content: URI
     * containing a Cursor of zero or more items.  Applications should use this
     * as the base type along with their own sub-type of their content: URIs
     * that represent a directory of items.  For example, hypothetical IMAP email
     * client may have a URI
     * <code>content://com.company.provider.imap/inbox</code> for all of the
     * messages in its inbox, whose MIME type would be reported as
     * <code>CURSOR_DIR_BASE_TYPE + "/vnd.company.imap-msg"</code>
     *
     * <p>Note how the base MIME type varies between this and
     * {@link #CURSOR_ITEM_BASE_TYPE} depending on whether there is
     * one single item or multiple items in the data set, while the sub-type
     * remains the same because in either case the data structure contained
     * in the cursor is the same.
     */
    public static final String CURSOR_DIR_BASE_TYPE = "vnd.android.cursor.dir";

    /** @hide */
    public static final int SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS = 1;
    /** @hide */
    public static final int SYNC_ERROR_AUTHENTICATION = 2;
    /** @hide */
    public static final int SYNC_ERROR_IO = 3;
    /** @hide */
    public static final int SYNC_ERROR_PARSE = 4;
    /** @hide */
    public static final int SYNC_ERROR_CONFLICT = 5;
    /** @hide */
    public static final int SYNC_ERROR_TOO_MANY_DELETIONS = 6;
    /** @hide */
    public static final int SYNC_ERROR_TOO_MANY_RETRIES = 7;
    /** @hide */
    public static final int SYNC_ERROR_INTERNAL = 8;

    public static final int SYNC_OBSERVER_TYPE_SETTINGS = 1<<0;
    public static final int SYNC_OBSERVER_TYPE_PENDING = 1<<1;
    public static final int SYNC_OBSERVER_TYPE_ACTIVE = 1<<2;
    /** @hide */
    public static final int SYNC_OBSERVER_TYPE_STATUS = 1<<3;
    /** @hide */
    public static final int SYNC_OBSERVER_TYPE_ALL = 0x7fffffff;

    // Always log queries which take 500ms+; shorter queries are
    // sampled accordingly.
    private static final int SLOW_THRESHOLD_MILLIS = 500;
    private final Random mRandom = new Random();  // guarded by itself

    public ContentResolver(Context context) {
        mContext = context;
    }

    /** @hide */
    protected abstract ContentProvider acquireProvider(Context c, String name);
    protected abstract void removeProvider(Context c, String name);

    protected ContentProvider acquireExistingProvider(Context c, String name) {
        return acquireProvider(c, name);
    }
    /** @hide */
    public abstract boolean releaseProvider(ContentProvider icp);

    public final String getType(Uri url) {
        ContentProvider provider = /*acquireExistingProvider(url)*/acquireProvider(url);
        System.out.println("provider:"+url+provider);
        if (provider != null) {
            try {
                return provider.getType(url);
            } catch (java.lang.Exception e) {
                Log.w(TAG, "Failed to get type for: " + url + " (" + e.getMessage() + ")");
                return null;
            } finally {
                releaseProvider(provider);
            }
        }

        if (!SCHEME_CONTENT.equals(url.getScheme())) {
            return null;
        }

        try {
        	//TODO: Need to be done
            String type = ((ActivityManager)Context.getSystemContext().getSystemService(Context.ACTIVITY_SERVICE)).getProviderMimeType(url);
            return type;
        } catch (java.lang.Exception e) {
            Log.w(TAG, "Failed to get type for: " + url + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * Query for the possible MIME types for the representations the given
     * content URL can be returned when opened as as stream with
     * {@link #openTypedAssetFileDescriptor}.  Note that the types here are
     * not necessarily a superset of the type returned by {@link #getType} --
     * many content providers can not return a raw stream for the structured
     * data that they contain.
     *
     * @param url A Uri identifying content (either a list or specific type),
     * using the content:// scheme.
     * @param mimeTypeFilter The desired MIME type.  This may be a pattern,
     * such as *\/*, to query for all available MIME types that match the
     * pattern.
     * @return Returns an array of MIME type strings for all availablle
     * data streams that match the given mimeTypeFilter.  If there are none,
     * null is returned.
     */
    public String[] getStreamTypes(Uri url, String mimeTypeFilter) {
        ContentProvider provider = acquireProvider(url);
        if (provider == null) {
            return null;
        }

        try {
            return provider.getStreamTypes(url, mimeTypeFilter);
        } catch (Exception e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
            return null;
        } finally {
            releaseProvider(provider);
        }
    }

    public final Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        ContentProvider provider = acquireProvider(uri);
        if (provider == null) {
            return null;
        }
        try {
            Cursor qCursor = provider.query(uri, projection, selection, selectionArgs, sortOrder);
            if (qCursor == null) {
                releaseProvider(provider);
                return null;
            }
            // force query execution
            qCursor.getCount();
            // Wrap the cursor object into CursorWrapperInner object
            return new CursorWrapperInner(qCursor, provider);
        } catch (Exception e) {
            releaseProvider(provider);
            return null;
        }
    }

    private final class CursorWrapperInner extends CursorWrapper {
        private ContentProvider mContentProvider;
        public static final String TAG="CursorWrapperInner";
        private boolean mCloseFlag = false;

        CursorWrapperInner(Cursor cursor, ContentProvider icp) {
            super(cursor);
            mContentProvider = icp;
        }

        @Override
        public void close() {
            super.close();
            ContentResolver.this.releaseProvider(mContentProvider);
            mCloseFlag = true;
        }

        @Override
        protected void finalize() throws Throwable {
            // TODO: integrate CloseGuard support.
            try {
                if(!mCloseFlag) {
                    Log.w(TAG, "Cursor finalized without prior close()");
                    close();
                }
            } finally {
                super.finalize();
            }
        }
    }


    /** @hide */
    static public int modeToMode(Uri uri, String mode) throws FileNotFoundException {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new FileNotFoundException("Bad mode for " + uri + ": "
                    + mode);
        }
        return modeBits;
    }

    /**
     * Inserts a row into a table at the given URL.
     *
     * If the content provider supports transactions the insertion will be atomic.
     *
     * @param url The URL of the table to insert into.
     * @param values The initial values for the newly inserted row. The key is the column name for
     *               the field. Passing an empty ContentValues will create an empty row.
     * @return the URL of the newly created row.
     */
    public final Uri insert(Uri url, ContentValues values)
    {
    	if(DEBUG_PROVIDER)System.out.println(TAG+ ":acquireProvider");
        ContentProvider provider = acquireProvider(url);
        if (provider == null) {
            System.out.println("Content Provider is null");
        }
        try {
            Uri createdRow = provider.insert(url, values);
            return createdRow;
        } catch (Exception e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.

        } finally {
            releaseProvider(provider);
        }
    	return null;
    }

    /**
     * Applies each of the {@link ContentProviderOperation} objects and returns an array
     * of their results. Passes through OperationApplicationException, which may be thrown
     * by the call to {@link ContentProviderOperation#apply}.
     * If all the applications succeed then a {@link ContentProviderResult} array with the
     * same number of elements as the operations will be returned. It is implementation-specific
     * how many, if any, operations will have been successfully applied if a call to
     * apply results in a {@link OperationApplicationException}.
     * @param authority the authority of the ContentProvider to which this batch should be applied
     * @param operations the operations to apply
     * @return the results of the applications
     * @throws OperationApplicationException thrown if an application fails.
     * See {@link ContentProviderOperation#apply} for more information.
     * @throws Exception thrown if a Exception is encountered while attempting
     *   to communicate with a remote provider.
     */
//    public ContentProviderResult[] applyBatch(String authority,
//            ArrayList<ContentProviderOperation> operations)
//             {
//        ContentProviderClient provider = acquireContentProviderClient(authority);
//        if (provider == null) {
//            throw new IllegalArgumentException("Unknown authority " + authority);
//        }
//        try {
//            return provider.applyBatch(operations);
//        } finally {
//            provider.release();
//        }
//    	return null;
//    }

    /**
     * Inserts multiple rows into a table at the given URL.
     *
     * This function make no guarantees about the atomicity of the insertions.
     *
     * @param url The URL of the table to insert into.
     * @param values The initial values for the newly inserted rows. The key is the column name for
     *               the field. Passing null will create an empty row.
     * @return the number of newly created rows.
     */
    public final int bulkInsert(Uri url, ContentValues[] values)
    {
//        ContentProvider provider = acquireProvider(url);
//        if (provider == null) {
//            throw new IllegalArgumentException("Unknown URL " + url);
//        }
//        try {
//            long startTime = SystemClock.uptimeMillis();
//            int rowsCreated = provider.bulkInsert(url, values);
//            long durationMillis = SystemClock.uptimeMillis() - startTime;
//            maybeLogUpdateToEventLog(durationMillis, url, "bulkinsert", null /* where */);
//            return rowsCreated;
//        } catch (Exception e) {
//            // Arbitrary and not worth documenting, as Activity
//            // Manager will kill this process shortly anyway.
//            return 0;
//        } finally {
//            releaseProvider(provider);
//        }
    	return 0;
    }

    /**
     * Deletes row(s) specified by a content URI.
     *
     * If the content provider supports transactions, the deletion will be atomic.
     *
     * @param url The URL of the row to delete.
     * @param where A filter to apply to rows before deleting, formatted as an SQL WHERE clause
                    (excluding the WHERE itself).
     * @return The number of rows deleted.
     */
    public final int delete(Uri url, String where, String[] selectionArgs)
    {
        ContentProvider provider = acquireProvider(url);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
        try {
            int rowsDeleted = provider.delete(url, where, selectionArgs);
            return rowsDeleted;
        } catch (Exception e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
            return -1;
        } finally {
            releaseProvider(provider);
        }
    }

    /**
     * Update row(s) in a content URI.
     *
     * If the content provider supports transactions the update will be atomic.
     *
     * @param uri The URI to modify.
     * @param values The new field values. The key is the column name for the field.
                     A null value will remove an existing field value.
     * @param where A filter to apply to rows before updating, formatted as an SQL WHERE clause
                    (excluding the WHERE itself).
     * @return the number of rows updated.
     * @throws NullPointerException if uri or values are null
     */
    public final int update(Uri uri, ContentValues values, String where,
            String[] selectionArgs) {
        ContentProvider provider = acquireProvider(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            int rowsUpdated = provider.update(uri, values, where, selectionArgs);
            return rowsUpdated;
        } catch (Exception e) {
            return -1;
        } finally {
            releaseProvider(provider);
        }

    }

    /**
     * Call an provider-defined method.  This can be used to implement
     * read or write interfaces which are cheaper than using a Cursor and/or
     * do not fit into the traditional table model.
     *
     * @param method provider-defined method name to call.  Opaque to
     *   framework, but must be non-null.
     * @param arg provider-defined String argument.  May be null.
     * @param extras provider-defined Bundle argument.  May be null.
     * @return a result Bundle, possibly null.  Will be null if the ContentProvider
     *   does not implement call.
     * @throws NullPointerException if uri or method is null
     * @throws IllegalArgumentException if uri is not known
     */
    public final Bundle call(Uri uri, String method, String arg, Bundle extras) {
        if (uri == null) {
            throw new NullPointerException("uri == null");
        }
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        ContentProvider provider = acquireProvider(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            return provider.call(method, arg, extras);
        } catch (Exception e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
            return null;
        } finally {
            releaseProvider(provider);
        }
    }

    /**
     * Returns the content provider for the given content URI.
     *
     * @param uri The URI to a content provider
     * @return The ContentProvider for the given URI, or null if no content provider is found.
     * @hide
     */
    public final ContentProvider acquireProvider(Uri uri) {
    	if(DEBUG_PROVIDER)System.out.println(TAG+" Calling acquireProvider(uri)");
        if (!SCHEME_CONTENT.equals(uri.getScheme())) {
        	System.out.println(TAG+"byte");
            return null;
        }
        String auth = uri.getAuthority();
        if (auth != null) {
        	if(DEBUG_PROVIDER)System.out.println(TAG+" Calling acquireProvider(mContext, uri.getAuthority())");
            return acquireProvider(mContext, uri.getAuthority());
        }
        if(DEBUG_PROVIDER)System.out.println(TAG+"bye...");
        return null;
    }

    /**
     * Returns the content provider for the given content URI if the process
     * already has a reference on it.
     *
     * @param uri The URI to a content provider
     * @return The ContentProvider for the given URI, or null if no content provider is found.
     * @hide
     */
    public final ContentProvider acquireExistingProvider(Uri uri) {
        if (!SCHEME_CONTENT.equals(uri.getScheme())) {
            return null;
        }
        String auth = uri.getAuthority();
        if (auth != null) {
            return acquireExistingProvider(mContext, uri.getAuthority());
        }
        return null;
    }

    /**
     * @hide
     */
    public final ContentProvider acquireProvider(String name) {
        if (name == null) {
            return null;
        }
        return acquireProvider(mContext, name);
    }

    /**
     * Returns a {@link ContentProviderClient} that is associated with the {@link ContentProvider}
     * that services the content at uri, starting the provider if necessary. Returns
     * null if there is no provider associated wih the uri. The caller must indicate that they are
     * done with the provider by calling {@link ContentProviderClient#release} which will allow
     * the system to release the provider it it determines that there is no other reason for
     * keeping it active.
     * @param uri specifies which provider should be acquired
     * @return a {@link ContentProviderClient} that is associated with the {@link ContentProvider}
     * that services the content at uri or null if there isn't one.
     */
//    public final ContentProviderClient acquireContentProviderClient(Uri uri) {
//        ContentProvider provider = acquireProvider(uri);
//        if (provider != null) {
//            return new ContentProviderClient(this, provider);
//        }
//
//        return null;
//    }

    /**
     * Returns a {@link ContentProviderClient} that is associated with the {@link ContentProvider}
     * with the authority of name, starting the provider if necessary. Returns
     * null if there is no provider associated wih the uri. The caller must indicate that they are
     * done with the provider by calling {@link ContentProviderClient#release} which will allow
     * the system to release the provider it it determines that there is no other reason for
     * keeping it active.
     * @param name specifies which provider should be acquired
     * @return a {@link ContentProviderClient} that is associated with the {@link ContentProvider}
     * with the authority of name or null if there isn't one.
     */
//    public final ContentProviderClient acquireContentProviderClient(String name) {
//        ContentProvider provider = acquireProvider(name);
//        if (provider != null) {
//            return new ContentProviderClient(this, provider);
//        }
//
//        return null;
//    }

    /**
     * Register an observer class that gets callbacks when data identified by a
     * given content URI changes.
     *
     * @param uri The URI to watch for changes. This can be a specific row URI, or a base URI
     * for a whole class of content.
     * @param notifyForDescendents If <code>true</code> changes to URIs beginning with <code>uri</code>
     * will also cause notifications to be sent. If <code>false</code> only changes to the exact URI
     * specified by <em>uri</em> will cause notifications to be sent. If true, than any URI values
     * at or below the specified URI will also trigger a match.
     * @param observer The object that receives callbacks when changes occur.
     * @see #unregisterContentObserver
     */
    public final void registerContentObserver(Uri uri, boolean notifyForDescendents,
            ContentObserver observer)
    {
        try {
            getContentService().registerContentObserver(uri, notifyForDescendents,
                    observer.getContentObserver());
        } catch (Exception e) {
        }
    }
    public ContentService getContentService(){
    	return ContentService.getContentService();
    }

    /**
     * Unregisters a change observer.
     *
     * @param observer The previously registered observer that is no longer needed.
     * @see #registerContentObserver
     */
    public final void unregisterContentObserver(ContentObserver observer) {
        try {
            ContentObserver contentObserver = observer.releaseContentObserver();
            if (contentObserver != null) {
                getContentService().unregisterContentObserver(
                        contentObserver);
            }
        } catch (Exception e) {
        }

    }

    /**
     * Notify registered observers that a row was updated.
     * To register, call {@link #registerContentObserver(android.net.Uri , boolean, android.database.ContentObserver) registerContentObserver()}.
     * By default, CursorAdapter objects will get this notification.
     *
     * @param uri
     * @param observer The observer that originated the change, may be <code>null</null>
     */
    public void notifyChange(Uri uri, ContentObserver observer) {
        notifyChange(uri, observer, true /* sync to network */);
    }

    /**
     * Notify registered observers that a row was updated.
     * To register, call {@link #registerContentObserver(android.net.Uri , boolean, android.database.ContentObserver) registerContentObserver()}.
     * By default, CursorAdapter objects will get this notification.
     *
     * @param uri
     * @param observer The observer that originated the change, may be <code>null</null>
     * @param syncToNetwork If true, attempt to sync the change to the network.
     */
    public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
        getContentService().notifyChange(
                uri, observer == null ? null : observer.getContentObserver(),
                observer != null && observer.deliverSelfNotifications(), syncToNetwork);

    }

    /**
     * Start an asynchronous sync operation. If you want to monitor the progress
     * of the sync you may register a SyncObserver. Only values of the following
     * types may be used in the extras bundle:
     * <ul>
     * <li>Integer</li>
     * <li>Long</li>
     * <li>Boolean</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>String</li>
     * </ul>
     *
     * @param uri the uri of the provider to sync or null to sync all providers.
     * @param extras any extras to pass to the SyncAdapter.
     * @deprecated instead use
     * {@link #requestSync(android.accounts.Account, String, android.os.Bundle)}
     */
    @Deprecated
    public void startSync(Uri uri, Bundle extras) {
//        Account account = null;
//        if (extras != null) {
//            String accountName = extras.getString(SYNC_EXTRAS_ACCOUNT);
//            if (!TextUtils.isEmpty(accountName)) {
//                account = new Account(accountName, "com.google");
//            }
//            extras.remove(SYNC_EXTRAS_ACCOUNT);
//        }
//        requestSync(account, uri != null ? uri.getAuthority() : null, extras);
    }

    /**
     * Start an asynchronous sync operation. If you want to monitor the progress
     * of the sync you may register a SyncObserver. Only values of the following
     * types may be used in the extras bundle:
     * <ul>
     * <li>Integer</li>
     * <li>Long</li>
     * <li>Boolean</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>String</li>
     * </ul>
     *
     * @param account which account should be synced
     * @param authority which authority should be synced
     * @param extras any extras to pass to the SyncAdapter.
     */
//    public static void requestSync(Account account, String authority, Bundle extras) {
//        validateSyncExtrasBundle(extras);
//        try {
//            getContentService().requestSync(account, authority, extras);
//        } catch (Exception e) {
//        }
//    }



    /**
     * Cancel any active or pending syncs that match account and authority. The account and
     * authority can each independently be set to null, which means that syncs with any account
     * or authority, respectively, will match.
     *
     * @param account filters the syncs that match by this account
     * @param authority filters the syncs that match by this authority
     */
//    public static void cancelSync(Account account, String authority) {
//        try {
//            getContentService().cancelSync(account, authority);
//        } catch (Exception e) {
//        }
//    }

    /**
     * Get information about the SyncAdapters that are known to the system.
     * @return an array of SyncAdapters that have registered with the system
     */
//    public static SyncAdapterType[] getSyncAdapterTypes() {
//        try {
//            return getContentService().getSyncAdapterTypes();
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Check if the provider should be synced when a network tickle is received
     *
     * @param account the account whose setting we are querying
     * @param authority the provider whose setting we are querying
     * @return true if the provider should be synced when a network tickle is received
     */
//    public static boolean getSyncAutomatically(Account account, String authority) {
//        try {
//            return getContentService().getSyncAutomatically(account, authority);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Set whether or not the provider is synced when it receives a network tickle.
     *
     * @param account the account whose setting we are querying
     * @param authority the provider whose behavior is being controlled
     * @param sync true if the provider should be synced when tickles are received for it
     */
//    public static void setSyncAutomatically(Account account, String authority, boolean sync) {
//        try {
//            getContentService().setSyncAutomatically(account, authority, sync);
//        } catch (Exception e) {
//            // exception ignored; if this is thrown then it means the runtime is in the midst of
//            // being restarted
//        }
//    }

    /**
     * Specifies that a sync should be requested with the specified the account, authority,
     * and extras at the given frequency. If there is already another periodic sync scheduled
     * with the account, authority and extras then a new periodic sync won't be added, instead
     * the frequency of the previous one will be updated.
     * <p>
     * These periodic syncs honor the "syncAutomatically" and "masterSyncAutomatically" settings.
     * Although these sync are scheduled at the specified frequency, it may take longer for it to
     * actually be started if other syncs are ahead of it in the sync operation queue. This means
     * that the actual start time may drift.
     * <p>
     * Periodic syncs are not allowed to have any of {@link #SYNC_EXTRAS_DO_NOT_RETRY},
     * {@link #SYNC_EXTRAS_IGNORE_BACKOFF}, {@link #SYNC_EXTRAS_IGNORE_SETTINGS},
     * {@link #SYNC_EXTRAS_INITIALIZE}, {@link #SYNC_EXTRAS_FORCE},
     * {@link #SYNC_EXTRAS_EXPEDITED}, {@link #SYNC_EXTRAS_MANUAL} set to true.
     * If any are supplied then an {@link IllegalArgumentException} will be thrown.
     *
     * @param account the account to specify in the sync
     * @param authority the provider to specify in the sync request
     * @param extras extra parameters to go along with the sync request
     * @param pollFrequency how frequently the sync should be performed, in seconds.
     * @throws IllegalArgumentException if an illegal extra was set or if any of the parameters
     * are null.
     */
//    public static void addPeriodicSync(Account account, String authority, Bundle extras,
//            long pollFrequency) {
//        validateSyncExtrasBundle(extras);
//        if (account == null) {
//            throw new IllegalArgumentException("account must not be null");
//        }
//        if (authority == null) {
//            throw new IllegalArgumentException("authority must not be null");
//        }
//        if (extras.getBoolean(SYNC_EXTRAS_MANUAL, false)
//                || extras.getBoolean(SYNC_EXTRAS_DO_NOT_RETRY, false)
//                || extras.getBoolean(SYNC_EXTRAS_IGNORE_BACKOFF, false)
//                || extras.getBoolean(SYNC_EXTRAS_IGNORE_SETTINGS, false)
//                || extras.getBoolean(SYNC_EXTRAS_INITIALIZE, false)
//                || extras.getBoolean(SYNC_EXTRAS_FORCE, false)
//                || extras.getBoolean(SYNC_EXTRAS_EXPEDITED, false)) {
//            throw new IllegalArgumentException("illegal extras were set");
//        }
//        try {
//            getContentService().addPeriodicSync(account, authority, extras, pollFrequency);
//        } catch (Exception e) {
//            // exception ignored; if this is thrown then it means the runtime is in the midst of
//            // being restarted
//        }
//    }

    /**
     * Remove a periodic sync. Has no affect if account, authority and extras don't match
     * an existing periodic sync.
     *
     * @param account the account of the periodic sync to remove
     * @param authority the provider of the periodic sync to remove
     * @param extras the extras of the periodic sync to remove
     */
//    public static void removePeriodicSync(Account account, String authority, Bundle extras) {
//        validateSyncExtrasBundle(extras);
//        if (account == null) {
//            throw new IllegalArgumentException("account must not be null");
//        }
//        if (authority == null) {
//            throw new IllegalArgumentException("authority must not be null");
//        }
//        try {
//            getContentService().removePeriodicSync(account, authority, extras);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Get the list of information about the periodic syncs for the given account and authority.
     *
     * @param account the account whose periodic syncs we are querying
     * @param authority the provider whose periodic syncs we are querying
     * @return a list of PeriodicSync objects. This list may be empty but will never be null.
     */
//    public static List<PeriodicSync> getPeriodicSyncs(Account account, String authority) {
//        if (account == null) {
//            throw new IllegalArgumentException("account must not be null");
//        }
//        if (authority == null) {
//            throw new IllegalArgumentException("authority must not be null");
//        }
//        try {
//            return getContentService().getPeriodicSyncs(account, authority);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Check if this account/provider is syncable.
     * @return >0 if it is syncable, 0 if not, and <0 if the state isn't known yet.
     */
//    public static int getIsSyncable(Account account, String authority) {
//        try {
//            return getContentService().getIsSyncable(account, authority);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Set whether this account/provider is syncable.
     * @param syncable >0 denotes syncable, 0 means not syncable, <0 means unknown
     */
//    public static void setIsSyncable(Account account, String authority, int syncable) {
//        try {
//            getContentService().setIsSyncable(account, authority, syncable);
//        } catch (Exception e) {
//            // exception ignored; if this is thrown then it means the runtime is in the midst of
//            // being restarted
//        }
//    }

    /**
     * Gets the master auto-sync setting that applies to all the providers and accounts.
     * If this is false then the per-provider auto-sync setting is ignored.
     *
     * @return the master auto-sync setting that applies to all the providers and accounts
     */
//    public static boolean getMasterSyncAutomatically() {
//        try {
//            return getContentService().getMasterSyncAutomatically();
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Sets the master auto-sync setting that applies to all the providers and accounts.
     * If this is false then the per-provider auto-sync setting is ignored.
     *
     * @param sync the master auto-sync setting that applies to all the providers and accounts
     */
//    public static void setMasterSyncAutomatically(boolean sync) {
//        try {
//            getContentService().setMasterSyncAutomatically(sync);
//        } catch (Exception e) {
//            // exception ignored; if this is thrown then it means the runtime is in the midst of
//            // being restarted
//        }
//    }

    /**
     * Returns true if there is currently a sync operation for the given
     * account or authority in the pending list, or actively being processed.
     * @param account the account whose setting we are querying
     * @param authority the provider whose behavior is being queried
     * @return true if a sync is active for the given account or authority.
     */
//    public static boolean isSyncActive(Account account, String authority) {
//        try {
//            return getContentService().isSyncActive(account, authority);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * If a sync is active returns the information about it, otherwise returns null.
     * <p>
     * @return the SyncInfo for the currently active sync or null if one is not active.
     * @deprecated
     * Since multiple concurrent syncs are now supported you should use
     * {@link #getCurrentSyncs()} to get the accurate list of current syncs.
     * This method returns the first item from the list of current syncs
     * or null if there are none.
     */
//    @Deprecated
//    public static SyncInfo getCurrentSync() {
//        try {
//            final List<SyncInfo> syncs = getContentService().getCurrentSyncs();
//            if (syncs.isEmpty()) {
//                return null;
//            }
//            return syncs.get(0);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Returns a list with information about all the active syncs. This list will be empty
     * if there are no active syncs.
     * @return a List of SyncInfo objects for the currently active syncs.
     */
//    public static List<SyncInfo> getCurrentSyncs() {
//        try {
//            return getContentService().getCurrentSyncs();
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Returns the status that matches the authority.
     * @param account the account whose setting we are querying
     * @param authority the provider whose behavior is being queried
     * @return the SyncStatusInfo for the authority, or null if none exists
     * @hide
     */
//    public static SyncStatusInfo getSyncStatus(Account account, String authority) {
//        try {
//            return getContentService().getSyncStatus(account, authority);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Return true if the pending status is true of any matching authorities.
     * @param account the account whose setting we are querying
     * @param authority the provider whose behavior is being queried
     * @return true if there is a pending sync with the matching account and authority
     */
//    public static boolean isSyncPending(Account account, String authority) {
//        try {
//            return getContentService().isSyncPending(account, authority);
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Request notifications when the different aspects of the SyncManager change. The
     * different items that can be requested are:
     * <ul>
     * <li> {@link #SYNC_OBSERVER_TYPE_PENDING}
     * <li> {@link #SYNC_OBSERVER_TYPE_ACTIVE}
     * <li> {@link #SYNC_OBSERVER_TYPE_SETTINGS}
     * </ul>
     * The caller can set one or more of the status types in the mask for any
     * given listener registration.
     * @param mask the status change types that will cause the callback to be invoked
     * @param callback observer to be invoked when the status changes
     * @return a handle that can be used to remove the listener at a later time
     */
//    public static Object addStatusChangeListener(int mask, final SyncStatusObserver callback) {
//        if (callback == null) {
//            throw new IllegalArgumentException("you passed in a null callback");
//        }
//        try {
//            ISyncStatusObserver.Stub observer = new ISyncStatusObserver.Stub() {
//                public void onStatusChanged(int which) throws Exception {
//                    callback.onStatusChanged(which);
//                }
//            };
//            getContentService().addStatusChangeListener(mask, observer);
//            return observer;
//        } catch (Exception e) {
//            throw new RuntimeException("the ContentService should always be reachable", e);
//        }
//    }

    /**
     * Remove a previously registered status change listener.
     * @param handle the handle that was returned by {@link #addStatusChangeListener}
     */
//    public static void removeStatusChangeListener(Object handle) {
//        if (handle == null) {
//            throw new IllegalArgumentException("you passed in a null handle");
//        }
//        try {
//            getContentService().removeStatusChangeListener((ISyncStatusObserver.Stub) handle);
//        } catch (Exception e) {
//            // exception ignored; if this is thrown then it means the runtime is in the midst of
//            // being restarted
//        }
//    }



    /** @hide */
    public static final String CONTENT_SERVICE_NAME = "content";


    private final Context mContext;
    private static final String TAG = "ContentResolver";

	public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String string)
	throws FileNotFoundException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public ParcelFileDescriptor openFileDescriptor(Uri uri, String string)
	throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public OutputStream openOutputStream(Uri url) {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Open a stream on to the content associated with a content URI.  If there
     * is no data associated with the URI, FileNotFoundException is thrown.
     *
     * <h5>Accepts the following URI schemes:</h5>
     * <ul>
     * <li>content ({@link #SCHEME_CONTENT})</li>
     * <li>android.resource ({@link #SCHEME_ANDROID_RESOURCE})</li>
     * <li>file ({@link #SCHEME_FILE})</li>
     * </ul>
     *
     * <p>See {@link #openAssetFileDescriptor(Uri, String)} for more information
     * on these schemes.
     *
     * @param uri The desired URI.
     * @return InputStream
     * @throws FileNotFoundException if the provided URI could not be opened.
     * @see #openAssetFileDescriptor(Uri, String)
     */
    public InputStream openInputStream(Uri uri)
            throws FileNotFoundException {
        String scheme = uri.getScheme();
        if (SCHEME_ANDROID_RESOURCE.equals(scheme)) {
            // Note: left here to avoid breaking compatibility. May be removed
            // with sufficient testing.
            OpenResourceIdResult r = getResourceId(uri);
            try {
                InputStream stream = r.r.openRawResource(r.id);
                return stream;
            } catch (Resources.NotFoundException ex) {
                throw new FileNotFoundException("Resource does not exist: " + uri);
            }
        } else if (SCHEME_FILE.equals(scheme)) {
            // Note: left here to avoid breaking compatibility. May be removed
            // with sufficient testing.
            return new FileInputStream(uri.getPath());
        } else {
            AssetFileDescriptor fd = openAssetFileDescriptor(uri, "r");
            try {
                return fd != null ? fd.createInputStream() : null;
            } catch (IOException e) {
                throw new FileNotFoundException("Unable to create stream");
            }
        }
    }

    /**
     * A resource identified by the {@link Resources} that contains it, and a resource id.
     *
     * @hide
     */
    public class OpenResourceIdResult {
        public Resources r;
        public int id;
    }

    /**
     * Resolves an android.resource URI to a {@link Resources} and a resource id.
     *
     * @hide
     */
    public OpenResourceIdResult getResourceId(Uri uri) throws FileNotFoundException {
        String authority = uri.getAuthority();
        Resources r;
        if (TextUtils.isEmpty(authority)) {
            throw new FileNotFoundException("No authority: " + uri);
        } else {
            try {
                r = mContext.getPackageManager().getResourcesForApplication(authority);
            } catch (NameNotFoundException ex) {
                throw new FileNotFoundException("No package found for authority: " + uri);
            }
        }
        List<String> path = uri.getPathSegments();
        if (path == null) {
            throw new FileNotFoundException("No path: " + uri);
        }
        int len = path.size();
        int id;
        if (len == 1) {
            try {
                id = Integer.parseInt(path.get(0));
            } catch (NumberFormatException e) {
                throw new FileNotFoundException("Single path segment is not a resource ID: " + uri);
            }
        } else if (len == 2) {
            id = r.getIdentifier(path.get(1), path.get(0), authority);
        } else {
            throw new FileNotFoundException("More than two path segments: " + uri);
        }
        if (id == 0) {
            throw new FileNotFoundException("No resource found for: " + uri);
        }
        OpenResourceIdResult res = new OpenResourceIdResult();
        res.r = r;
        res.id = id;
        return res;
    }

    public void shutdown(Uri uri) {
        ContentProvider provider = acquireProvider(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            provider.shutdown();
            removeProvider(uri);
        } catch (Exception e) {
        } finally {
            releaseProvider(provider);
        }
    }
    public final void removeProvider(Uri uri) {
        String auth = uri.getAuthority();
        if (auth != null) {
            removeProvider(mContext, auth);
        }
    }

    /**
     * @j2sNative
     * console.log("Missing method: setMasterSyncAutomatically");
     */
    @MayloonStubAnnotation()
    public static void setMasterSyncAutomatically(boolean sync) {
        System.out.println("Stub" + " Function : setMasterSyncAutomatically");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getCurrentSync");
     */
    @MayloonStubAnnotation()
    public static Object getCurrentSync() {
        System.out.println("Stub" + " Function : getCurrentSync");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: getMasterSyncAutomatically");
     */
    @MayloonStubAnnotation()
    public static boolean getMasterSyncAutomatically() {
        System.out.println("Stub" + " Function : getMasterSyncAutomatically");
        return true;
    }

    /**
     * @j2sNative
     * console.log("Missing method: removeStatusChangeListener");
     */
    @MayloonStubAnnotation()
    public static void removeStatusChangeListener(Object handle) {
        System.out.println("Stub" + " Function : removeStatusChangeListener");
        return;
    }
}
