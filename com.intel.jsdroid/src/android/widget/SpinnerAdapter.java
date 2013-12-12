package android.widget;

import android.view.View;
import android.view.ViewGroup;

/**
 * Extended {@link Adapter} that is the bridge between a
 * {@link android.widget.Spinner} and its data. A spinner adapter allows to
 * define two different views: one that shows the data in the spinner itself and
 * one that shows the data in the drop down list when the spinner is pressed.</p>
 */
public interface SpinnerAdapter extends Adapter {
    /**
     * <p>Get a {@link android.view.View} that displays in the drop down popup
     * the data at the specified position in the data set.</p>
     *
     * @param position      index of the item whose view we want.
     * @param convertView   the old view to reuse, if possible. Note: You should
     *        check that this view is non-null and of an appropriate type before
     *        using. If it is not possible to convert this view to display the
     *        correct data, this method can create a new view.
     * @param parent the parent that this view will eventually be attached to
     * @return a {@link android.view.View} corresponding to the data at the
     *         specified position.
     */
    public View getDropDownView(int position, View convertView, ViewGroup parent);
}