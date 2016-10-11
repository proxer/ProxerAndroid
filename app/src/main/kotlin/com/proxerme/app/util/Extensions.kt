package com.proxerme.app.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */

val Context.inputMethodManager: InputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val Context.alarmManager: AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

fun TextView.measureAndGetHeight(): Int {
    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((parent as ViewGroup).measuredWidth,
            View.MeasureSpec.AT_MOST)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT,
            View.MeasureSpec.UNSPECIFIED)

    measure(widthMeasureSpec, heightMeasureSpec)

    return measuredHeight
}