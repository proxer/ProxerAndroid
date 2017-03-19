package me.proxer.app.util.extension

import android.app.AlarmManager
import android.content.Context
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

inline val Context.inputMethodManager: InputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

inline val Context.alarmManager: AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

inline val Context.windowManager: WindowManager
    get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
