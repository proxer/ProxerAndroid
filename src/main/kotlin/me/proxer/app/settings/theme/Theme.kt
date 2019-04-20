package me.proxer.app.settings.theme

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import me.proxer.app.R
import me.proxer.app.util.extension.resolveColor

/**
 * @author Ruben Gees
 */
enum class Theme(
    val preferenceId: String,
    @StringRes val themeName: Int,
    @StyleRes val main: Int,
    @StyleRes val noBackground: Int
) {
    CLASSIC(
        "0",
        R.string.theme_classic,
        R.style.Theme_App,
        R.style.Theme_App_NoBackground
    ),
    BLUE_GREEN(
        "1",
        R.string.theme_bg,
        R.style.Theme_App_BG,
        R.style.Theme_App_BG_NoBackground
    ),
    GLOOMY(
        "2",
        R.string.theme_gloomy,
        R.style.Theme_App_Gloomy,
        R.style.Theme_App_Gloomy_NoBackground
    );

    @ColorInt
    fun primaryColor(context: Context): Int {
        return context.resolveColor(R.attr.colorPrimary, nativeTheme(context))
    }

    @ColorInt
    fun secondaryColor(context: Context): Int {
        return context.resolveColor(R.attr.colorSecondary, nativeTheme(context))
    }

    @ColorInt
    fun colorOnSecondary(context: Context): Int {
        return context.resolveColor(R.attr.colorOnSecondary, nativeTheme(context))
    }

    private fun nativeTheme(context: Context) = context.resources.newTheme().apply {
        applyStyle(main, true)
    }
}
