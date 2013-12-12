package android.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * An easy adapter that creates views defined in an XML file. You can specify
 * the XML file that defines the appearance of the views.
 */
public abstract class ResourceCursorAdapter extends CursorAdapter {
    private int mLayout;

    private int mDropDownLayout;
    
    private LayoutInflater mInflater;
    
    /**
     * Constructor.
     * 
     * @param context The context where the ListView associated with this
     *            SimpleListItemFactory is running
     * @param layout resource identifier of a layout file that defines the views
     *            for this list item.  Unless you override them later, this will
     *            define both the item views and the drop down views.
     */
    public ResourceCursorAdapter(Context context, int layout, Cursor c) {
        super(context, c);
        mLayout = mDropDownLayout = layout;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    /**
     * Constructor.
     * 
     * @param context The context where the ListView associated with this
     *            SimpleListItemFactory is running
     * @param layout resource identifier of a layout file that defines the views
     *            for this list item.  Unless you override them later, this will
     *            define both the item views and the drop down views.
     * @param c The cursor from which to get the data.
     * @param autoRequery If true the adapter will call requery() on the
     *                    cursor whenever it changes so the most recent
     *                    data is always displayed.
     */
    public ResourceCursorAdapter(Context context, int layout, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mLayout = mDropDownLayout = layout;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Inflates view(s) from the specified XML file.
     * 
     * @see android.widget.CursorAdapter#newView(android.content.Context,
     *      android.database.Cursor, ViewGroup)
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(mLayout, parent, false);
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(mDropDownLayout, parent, false);
    }

    /**
     * <p>Sets the layout resource of the item views.</p>
     *
     * @param layout the layout resources used to create item views
     */
    public void setViewResource(int layout) {
        mLayout = layout;
    }
    
    /**
     * <p>Sets the layout resource of the drop down views.</p>
     *
     * @param dropDownLayout the layout resources used to create drop down views
     */
    public void setDropDownViewResource(int dropDownLayout) {
        mDropDownLayout = dropDownLayout;
    }
}
