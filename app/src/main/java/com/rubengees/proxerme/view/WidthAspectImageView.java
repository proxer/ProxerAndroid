package com.rubengees.proxerme.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.rubengees.proxerme.R;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class WidthAspectImageView extends ImageView {

    private static final float DEFAULT_ASPECT = 1f;

    private float aspect;

    public WidthAspectImageView(Context context) {
        super(context);
    }

    public WidthAspectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.WidthAspectImageView);

        try {
            aspect = a.getFloat(R.styleable.WidthAspectImageView_aspect, DEFAULT_ASPECT);
        } finally {
            a.recycle();
        }
    }

    public WidthAspectImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();

        //noinspection SuspiciousNameCombination
        setMeasuredDimension(width, (int) (width * aspect));
    }
}
