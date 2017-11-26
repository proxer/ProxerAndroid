package me.proxer.app.ui.view.bbcode

import me.proxer.app.ui.view.bbcode.prototype.BoldPrototype
import me.proxer.app.ui.view.bbcode.prototype.CenterPrototype
import me.proxer.app.ui.view.bbcode.prototype.ColorPrototype
import me.proxer.app.ui.view.bbcode.prototype.ItalicPrototype
import me.proxer.app.ui.view.bbcode.prototype.LeftPrototype
import me.proxer.app.ui.view.bbcode.prototype.RightPrototype
import me.proxer.app.ui.view.bbcode.prototype.SizePrototype
import me.proxer.app.ui.view.bbcode.prototype.SpoilerPrototype
import me.proxer.app.ui.view.bbcode.prototype.StrikethroughPrototype
import me.proxer.app.ui.view.bbcode.prototype.UnderlinePrototype
import me.proxer.app.ui.view.bbcode.tree.BBTree
import me.proxer.app.ui.view.bbcode.tree.TextLeaf
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
object BBParser {

    private val regex = Regex("${quote("[")}(.*?)${quote("]")}", RegexOption.DOT_MATCHES_ALL)

    private val prototypes = arrayOf(BoldPrototype, ItalicPrototype, UnderlinePrototype, StrikethroughPrototype,
            SizePrototype, ColorPrototype, LeftPrototype, CenterPrototype, RightPrototype, SpoilerPrototype)

    fun parse(input: String): BBTree {
        val trimmedInput = input.trim()
        val result = BBTree(null)
        val parts = regex.findAll(trimmedInput)

        var currentTree = result
        var currentPosition = 0

        parts.forEach {
            val part = it.groupValues[1].trim()

            if (it.range.first > currentPosition) {
                val startString = trimmedInput.substring(currentPosition, it.range.first)

                currentTree.children.add(TextLeaf(startString, currentTree))
            }

            if (part.startsWith("/") && part.length >= 2) {
                if (currentTree.endsWith(part.substring(1))) {
                    currentTree = currentTree.parent
                            ?: throw IllegalStateException("tree does not have a parent: $currentTree")
                }
            } else {
                var treeFound = false

                for (prototype in prototypes) {
                    val newTree = prototype.fromCode(it.groupValues[1], currentTree)

                    if (newTree != null) {
                        currentTree.children.add(newTree)
                        currentTree = newTree
                        treeFound = true

                        break
                    }
                }

                if (!treeFound) {
                    currentTree.children.add(TextLeaf(it.value, currentTree))
                }
            }

            currentPosition = it.range.endInclusive + 1
        }

        if (currentPosition < trimmedInput.length) {
            val endString = trimmedInput.substring(currentPosition, trimmedInput.length)

            currentTree.children.add(TextLeaf(endString, currentTree))
        }

        return result
    }
}
