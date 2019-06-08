package me.proxer.app.settings.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
enum class ThemeVariant(
    val preferenceId: String,
    @StringRes val variantName: Int?,
    @AppCompatDelegate.NightMode val value: Int
) {
    LIGHT("0", R.string.theme_variant_light, AppCompatDelegate.MODE_NIGHT_NO),
    DARK("1", R.string.theme_variant_dark, AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(
        "2", null, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
    )
}
