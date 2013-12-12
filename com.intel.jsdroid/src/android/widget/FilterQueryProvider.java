package android.widget;

import android.database.Cursor;

/**
 * This class can be used by external clients of CursorAdapter and
 * CursorTreeAdapter to define how the content of the adapter should be
 * filtered.
 * 
 * @see #runQuery(CharSequence)
 */
public interface FilterQueryProvider {
    /**
     * Runs a query with the specified constraint. This query is requested
     * by the filter attached to this adapter.
     *
     * Contract: when constraint is null or empty, the original results,
     * prior to any filtering, must be returned.
     *
     * @param constraint the constraint with which the query must
     *        be filtered
     *
     * @return a Cursor representing the results of the new query
     */
    Cursor runQuery(CharSequence constraint);
}
