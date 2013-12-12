package android.view;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A {@link Parcelable} implementation that should be used by inheritance
 * hierarchies to ensure the state of all classes along the chain is saved.
 */
public abstract class AbsSavedState implements Parcelable {
    public static final AbsSavedState EMPTY_STATE = new AbsSavedState() {};

    private final Parcelable mSuperState;

    /**
     * Constructor used to make the EMPTY_STATE singleton
     */
    private AbsSavedState() {
        mSuperState = null;
    }

    /**
     * Constructor called by derived classes when creating their SavedState objects
     *
     * @param superState The state of the superclass of this view
     */
    protected AbsSavedState(Parcelable superState) {
        if (superState == null) {
            throw new IllegalArgumentException("superState must not be null");
        }
        mSuperState = superState != EMPTY_STATE ? superState : null;
    }

    /**
     * Constructor used when reading from a parcel. Reads the state of the superclass.
     *
     * @param source
     */
    protected AbsSavedState(Parcel source) {
        // FIXME need class loader
        Parcelable superState = source.readParcelable(null);

        mSuperState = superState != null ? superState : EMPTY_STATE;
    }

    final public Parcelable getSuperState() {
        return mSuperState;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
         dest.writeParcelable(mSuperState, flags);
    }

    public static final Creator<AbsSavedState> CREATOR
        = new Creator<AbsSavedState>() {

        public AbsSavedState createFromParcel(Parcel in) {
            Parcelable superState = in.readParcelable(null);
            if (superState != null) {
                throw new IllegalStateException("superState must be null");
            }
            return EMPTY_STATE;
        }

        public AbsSavedState[] newArray(int size) {
            return new AbsSavedState[size];
        }
    };
}
