/*
 * Copyright (c) 2017 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */
package me.zhanghai.android.materialprogressbar

import android.content.Context
import android.graphics.drawable.Drawable
import me.zhanghai.android.materialprogressbar.MaterialProgressBar.DETERMINATE_CIRCULAR_PROGRESS_STYLE_NORMAL

/**
 * Adjusted drawable to make stroke thinner.
 *
 * @author Ruben Gees
 */
internal class ThinCircularProgressDrawable(
    context: Context
) : BaseProgressLayerDrawable<SingleCircularProgressDrawable, CircularProgressBackgroundDrawable>(
    arrayOf<Drawable>(
        CircularProgressBackgroundDrawable(),
        ThinSingleCircularProgressDrawable(DETERMINATE_CIRCULAR_PROGRESS_STYLE_NORMAL),
        ThinSingleCircularProgressDrawable(DETERMINATE_CIRCULAR_PROGRESS_STYLE_NORMAL)
    ),
    context
)
