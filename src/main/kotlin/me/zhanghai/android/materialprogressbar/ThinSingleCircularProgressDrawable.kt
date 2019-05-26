/*
 * Copyright (c) 2017 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */
package me.zhanghai.android.materialprogressbar

import android.graphics.Canvas
import android.graphics.Paint

/**
 * Adjusted drawable to make stroke thinner.
 *
 * @author Ruben Gees
 */
internal class ThinSingleCircularProgressDrawable(style: Int) : SingleCircularProgressDrawable(style) {

    override fun onDrawRing(canvas: Canvas, paint: Paint) {
        super.onDrawRing(canvas, paint.apply { strokeWidth = 2f })
    }
}
