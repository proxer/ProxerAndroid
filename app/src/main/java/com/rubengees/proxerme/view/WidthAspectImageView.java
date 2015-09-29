/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.rubengees.proxerme.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.rubengees.proxerme.R;

/**
 * And {@link ImageView}, which calculates it's height by the given width and an aspect.
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
