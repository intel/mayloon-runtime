package android.widget;

import android.database.Cursor;

public interface CursorFilterClient {
    CharSequence convertToString(Cursor cursor);
    Cursor runQueryOnBackgroundThread(CharSequence constraint);
    Cursor getCursor();
    void changeCursor(Cursor cursor);
}
