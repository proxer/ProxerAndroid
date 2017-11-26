package me.proxer.app.ui.view.bbcode.prototype

import android.graphics.Color
import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.ColorTree

/**
 * @author Ruben Gees
 */
object ColorPrototype : BBPrototype {

    private val availableColors = arrayOf(
            "black" to Color.parseColor("#000000"),
            "orange" to Color.parseColor("#f8a523"),
            "red" to Color.parseColor("#f50022"),
            "blue" to Color.parseColor("#3000fd"),
            "purple" to Color.parseColor("#7c007f"),
            "green" to Color.parseColor("#238107"),
            "white" to Color.parseColor("#ffffff"),
            "gray" to Color.parseColor("#7f7f7f")
    )

    override fun fromCode(code: String, parent: BBTree) = when (code.startsWith("color", ignoreCase = true)) {
        true -> {
            val value = code.substringAfter("=").trim()
            val color = when (value.startsWith("#")) {
                true -> try {
                    Color.parseColor(value)
                } catch (ignored: IllegalArgumentException) {
                    null
                }
                false -> availableColors.find { it.first == value }?.second
            }

            color?.let { ColorTree(color, parent) }
        }
        false -> null
    }
}
