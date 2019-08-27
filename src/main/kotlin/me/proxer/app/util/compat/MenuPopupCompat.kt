package me.proxer.app.util.compat

import android.annotation.SuppressLint
import android.content.Context
import android.view.Menu
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper

/**
 * @author Ruben Gees
 */
class MenuPopupCompat(private val context: Context, private val menu: Menu, private val anchorView: View) {

    private var forceShowIcon: Boolean = false

    fun forceShowIcon() = this.apply { forceShowIcon = true }

    @SuppressLint("RestrictedApi")
    fun show(x: Int = 0, y: Int = 0) = MenuPopupHelper(context, menu as MenuBuilder, anchorView)
        .apply { setForceShowIcon(forceShowIcon) }
        .show(x, y)
}
