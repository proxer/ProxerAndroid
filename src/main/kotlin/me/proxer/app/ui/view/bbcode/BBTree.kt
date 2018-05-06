package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.text.Spanned
import me.proxer.app.GlideRequests
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype
import me.proxer.app.ui.view.bbcode.prototype.ConditionalTextMutatorPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextMutatorPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import me.proxer.app.util.extension.unsafeLazy

/**
 * @author Ruben Gees
 */
class BBTree(
    val prototype: BBPrototype,
    val parent: BBTree?,
    var isFinished: Boolean = false,
    val children: MutableList<BBTree> = mutableListOf(),
    args: MutableMap<String, Any?> = mutableMapOf()
) {

    companion object {
        internal const val GLIDE_ARGUMENT = "glide"
        internal const val USER_ID_ARGUMENT = "userId"
        internal const val ENABLE_EMOTICONS_ARGUMENT = "enable_emoticons"
    }

    private val internalArgs: MutableMap<String, Any?> = args

    val args: Map<String, Any?>
        get() = internalArgs.plus(arrayOf(
            GLIDE_ARGUMENT to glide,
            USER_ID_ARGUMENT to userId,
            ENABLE_EMOTICONS_ARGUMENT to enableEmoticons
        ))

    var glide: GlideRequests? = null
        set(value) {
            field = value

            children.forEach {
                it.glide = value
            }
        }

    var userId: String? = null
        set(value) {
            field = value

            children.forEach {
                it.userId = value
            }
        }

    var enableEmoticons: Boolean = false

    fun endsWith(code: String) = prototype.endRegex.matches(code)

    fun makeViews(context: Context) = prototype.makeViews(context, children, args)

    fun optimize() = recursiveOptimize().first()

    private fun recursiveOptimize(): List<BBTree> {
        val newChildren = mergeChildren(children.flatMap { it.recursiveOptimize() })

        val recursiveNewChildren by unsafeLazy { getRecursiveChildren(newChildren) }
        val canOptimize = prototype !is ConditionalTextMutatorPrototype || prototype.canOptimize(recursiveNewChildren)

        if (prototype is TextMutatorPrototype && canOptimize) {
            recursiveNewChildren.forEach {
                if (it.prototype == TextPrototype) {
                    val text = TextPrototype.getText(it.internalArgs).toSpannableStringBuilder()
                    val mutatedText = prototype.mutate(text, internalArgs)

                    TextPrototype.updateText(mutatedText, it.internalArgs)
                }
            }
        }

        return when {
            canOptimize && prototype is TextMutatorPrototype -> newChildren.map {
                BBTree(it.prototype, parent, isFinished, it.children, it.internalArgs)
            }
            else -> {
                children.clear()
                children.addAll(newChildren)

                listOf(this)
            }
        }
    }

    private fun mergeChildren(newChildren: List<BBTree>): List<BBTree> {
        val result = mutableListOf<BBTree>()

        if (newChildren.isNotEmpty()) {
            var current = newChildren.first()

            newChildren.drop(1).forEach { next ->
                if (current.prototype == TextPrototype && next.prototype == TextPrototype) {
                    val currentText = TextPrototype.getText(current.internalArgs).toSpannableStringBuilder()
                    val mergedText = currentText.append(TextPrototype.getText(next.internalArgs))

                    TextPrototype.updateText(mergedText, current.internalArgs)
                } else {
                    result += current
                    current = next
                }
            }

            result += current
        }

        return result
    }

    private fun getRecursiveChildren(current: List<BBTree>): List<BBTree> = current
        .plus(current.flatMap { getRecursiveChildren(it.children) })

    @Suppress("EqualsAlwaysReturnsTrueOrFalse") // False positive
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BBTree

        if (prototype != other.prototype) return false
        if (children != other.children) return false
        if (internalArgs.size != other.internalArgs.size) return false

        // Work around a bug in the SpannableStringBuilder (and possibly others) implementation.
        // See: https://stackoverflow.com/a/46403431/4279995.
        internalArgs.forEach { (key, value) ->
            other.internalArgs.forEach { (otherKey, otherValue) ->
                if (key != otherKey) return false

                if (value is Spanned) {
                    if (otherValue !is Spanned) return false
                    if (value.toString() != otherValue.toString()) return false
                } else if (value != otherValue) {
                    return false
                }
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = prototype.hashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + internalArgs.hashCode()
        return result
    }
}
