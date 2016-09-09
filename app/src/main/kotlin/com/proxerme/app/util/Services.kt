package com.proxerme.app.util

import android.content.Context
import android.view.inputmethod.InputMethodManager

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */

val android.content.Context.inputMethodManager: android.view.inputmethod.InputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

val Context.notificationManager: android.app.NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

val Context.alarmManager: android.app.AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager