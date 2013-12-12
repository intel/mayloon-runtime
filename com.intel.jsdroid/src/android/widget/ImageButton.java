package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class ImageButton extends ImageView {
    public ImageButton(Context context) {
        this(context, null);
    }

    public ImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.imageButtonStyle);
    }

    public ImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        return false;
    }
}