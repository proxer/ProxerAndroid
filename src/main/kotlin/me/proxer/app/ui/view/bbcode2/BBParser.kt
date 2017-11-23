package me.proxer.app.ui.view.bbcode2

import me.proxer.app.ui.view.bbcode2.prototype.BoldPrototype
import me.proxer.app.ui.view.bbcode2.prototype.CenterPrototype
import me.proxer.app.ui.view.bbcode2.prototype.ColorPrototype
import me.proxer.app.ui.view.bbcode2.prototype.ItalicPrototype
import me.proxer.app.ui.view.bbcode2.prototype.LeftPrototype
import me.proxer.app.ui.view.bbcode2.prototype.RightPrototype
import me.proxer.app.ui.view.bbcode2.prototype.SpoilerPrototype
import me.proxer.app.ui.view.bbcode2.tree.BBTree
import me.proxer.app.ui.view.bbcode2.tree.TextLeaf
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
object BBParser {

    private val regex = Regex("${quote("[")}(.*?)${quote("]")}", RegexOption.DOT_MATCHES_ALL)

    private val prototypes = arrayOf(BoldPrototype, ItalicPrototype, ColorPrototype, LeftPrototype, CenterPrototype,
            RightPrototype, SpoilerPrototype)

    fun parse(input: String): BBTree {
        val result = BBTree(null, mutableListOf())
        val parts = regex.findAll(input)
        var currentTree = result
        var currentPosition = 0

        parts.forEach {
            val part = it.groupValues[1].trim()

            if (it.range.first > currentPosition) {
                currentTree.children.add(TextLeaf(currentTree, mutableListOf(),
                        input.substring(currentPosition, it.range.first)))
            }

            if (part.startsWith("/") && part.length >= 2) {
                if (currentTree.endsWith(part.substring(1))) {
                    currentTree = currentTree.parent
                            ?: throw IllegalStateException("tree does not have a parent: $currentTree")
                }
            } else {
                for (prototype in prototypes) {
                    val newTree = prototype.fromCode(it.groupValues[1], currentTree)

                    if (newTree != null) {
                        currentTree.children.add(newTree)
                        currentTree = newTree

                        break
                    }
                }
            }

            currentPosition = it.range.endInclusive + 1
        }

        if (currentPosition < input.length) {
            currentTree.children.add(TextLeaf(currentTree, mutableListOf(),
                    input.substring(currentPosition, input.length)))
        }

        return result
    }
}
