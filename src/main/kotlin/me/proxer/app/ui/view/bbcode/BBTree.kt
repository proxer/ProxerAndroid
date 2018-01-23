package me.proxer.app.ui.view.bbcode

import android.content.Context
import me.proxer.app.GlideRequests
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype
import me.proxer.app.ui.view.bbcode.prototype.ConditionalTextMutatorPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextMutatorPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype

/**
 * @author Ruben Gees
 */
class BBTree(
        val prototype: BBPrototype,
        val parent: BBTree?,
        val children: MutableList<BBTree> = mutableListOf(),
        val args: MutableMap<String, Any?> = mutableMapOf()
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

    fun optimize() = recursiveOptimize().first()

    private fun recursiveOptimize(): List<BBTree> {
        var canOptimize = true

        if (prototype is TextMutatorPrototype) {
            val recursiveChildren = getRecursiveChildren(children)

            canOptimize = prototype !is ConditionalTextMutatorPrototype || prototype.canOptimize(recursiveChildren)

            if (canOptimize) {
                recursiveChildren.forEach {
                    if (it.prototype == TextPrototype) {
                        val text = TextPrototype.getText(it.args).toSpannableStringBuilder()
                        val mutatedText = prototype.mutate(text, args)

                        TextPrototype.updateText(mutatedText, it.args)
                    }
                }
            }
        }

        val newChildren = children.flatMap { it.recursiveOptimize() }

        if (newChildren.isNotEmpty()) {
            val result = mutableListOf<BBTree>()
            var current = newChildren.first()

            newChildren.drop(1).forEach { next ->
                if (current.prototype == TextPrototype && next.prototype == TextPrototype) {
                    val currentText = TextPrototype.getText(current.args).toSpannableStringBuilder()
                    val mergedText = currentText.append(TextPrototype.getText(next.args))

                    TextPrototype.updateText(mergedText, current.args)
                } else {
                    result += current
                    current = next
                }
            }

            result += current

            return when {
                canOptimize && prototype is TextMutatorPrototype -> result.map {
                    BBTree(it.prototype, parent, it.children, it.args)
                }
                else -> {
                    children.clear()
                    children.addAll(result)

                    listOf(this)
                }
            }
        } else {
            return when {
                canOptimize && prototype is TextMutatorPrototype -> emptyList()
                else -> listOf(this)
            }
        }
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
