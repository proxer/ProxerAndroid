package me.proxer.app.ui.view.bbcode

import me.proxer.app.ui.view.bbcode.prototype.BBPrototype
import me.proxer.app.ui.view.bbcode.prototype.ConditionalTextMutatorPrototype
import me.proxer.app.ui.view.bbcode.prototype.ContentPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextMutatorPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import me.proxer.app.util.extension.unsafeLazy

/**
 * @author Ruben Gees
 */
class BBTree(
    val prototype: BBPrototype,
    val parent: BBTree?,
    val children: MutableList<BBTree> = mutableListOf(),
    val args: BBArgs = BBArgs()
) {

    fun endsWith(code: String) = prototype.endRegex.matches(code)

    fun makeViews(parent: BBCodeView, args: BBArgs) = prototype.makeViews(parent, children, args + this.args)

    fun optimize(args: BBArgs = BBArgs()) = recursiveOptimize(args).first()

    fun isBlank(): Boolean {
        if (prototype is ContentPrototype && !prototype.isBlank(args)) {
            return false
        }

        return children.all { it.isBlank() }
    }

    private fun recursiveOptimize(args: BBArgs): List<BBTree> {
        val recursiveNewChildren by unsafeLazy { getRecursiveChildren(children) }
        val canOptimize = prototype !is ConditionalTextMutatorPrototype || prototype.canOptimize(recursiveNewChildren)

        if (prototype is TextMutatorPrototype && canOptimize) {
            recursiveNewChildren.forEach {
                if (it.prototype == TextPrototype) {
                    val mergedArgs = args + this.args + it.args

                    it.args.text = prototype.mutate(it.args.safeText.toSpannableStringBuilder(), mergedArgs)
                }
            }
        }

        val newChildren = mergeChildren(children.flatMap { it.recursiveOptimize(args) })

        return when {
            canOptimize && prototype is TextMutatorPrototype -> newChildren.map {
                BBTree(it.prototype, parent, it.children, it.args)
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
                    current.args.text = current.args.safeText.toSpannableStringBuilder().append(next.args.safeText)
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
