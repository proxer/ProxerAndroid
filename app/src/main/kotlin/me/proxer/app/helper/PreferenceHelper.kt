package me.proxer.app.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.v7.app.AppCompatDelegate
import me.proxer.app.helper.MaterialDrawerHelper

/**
 * A helper class, which gives access to the [SharedPreferences].

 * @author Ruben Gees
 */
object PreferenceHelper {

    const val AGE_CONFIRMATION = "age_confirmation"
    const val START_PAGE = "start_page"
    const val THEME = "theme"

    fun isAgeRestrictedMediaAllowed(context: Context)
            = getDefaultSharedPreferences(context).getBoolean(AGE_CONFIRMATION, false)

    fun getStartPage(context: Context) = MaterialDrawerHelper.DrawerItem
            .fromOrDefault(getDefaultSharedPreferences(context).getString(START_PAGE, "0").toLongOrNull())

    fun setAgeRestrictedMediaAllowed(context: Context, isAllowed: Boolean)
            = getDefaultSharedPreferences(context).edit().putBoolean(AGE_CONFIRMATION, isAllowed).apply()

    @AppCompatDelegate.NightMode
    fun getNightMode(context: Context): Int {
        return when (getDefaultSharedPreferences(context).getString(THEME, "0")) {
            "0" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "1" -> AppCompatDelegate.MODE_NIGHT_AUTO
            "2" -> AppCompatDelegate.MODE_NIGHT_YES
            "3" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> throw RuntimeException("Invalid value")
        }
    }
}
