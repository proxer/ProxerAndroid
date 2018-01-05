package me.proxer.app.ui.view.bbcode

import android.content.Context
import me.proxer.app.GlideRequests
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype

/**
 * @author Ruben Gees
 */
open class BBTree(
        val prototype: BBPrototype,
        val parent: BBTree?,
        val children: MutableList<BBTree> = mutableListOf(),
        val args: Map<String, Any?> = mapOf()
) {

    companion object {
        internal const val GLIDE_ARGUMENT = "glide"
    }

    var glide: GlideRequests? = null
        set(value) {
            field = value

            children.forEach {
                it.glide = value
            }
        }

    fun endsWith(code: String) = prototype.endRegex.matches(code)
    fun makeViews(context: Context) = prototype.makeViews(context, children, args.plus(GLIDE_ARGUMENT to glide))

    @Suppress("EqualsAlwaysReturnsTrueOrFalse") // False positive
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BBTree

        if (prototype != other.prototype) return false
        if (children != other.children) return false
        if (args != other.args) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prototype.hashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + args.hashCode()
        return result
    }
}
