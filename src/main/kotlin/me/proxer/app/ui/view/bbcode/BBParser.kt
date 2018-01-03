package me.proxer.app.ui.view.bbcode

import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.bold.BoldPrototype
import me.proxer.app.ui.view.bbcode.center.CenterPrototype
import me.proxer.app.ui.view.bbcode.color.ColorPrototype
import me.proxer.app.ui.view.bbcode.divider.DividerPrototype
import me.proxer.app.ui.view.bbcode.image.ImagePrototype
import me.proxer.app.ui.view.bbcode.italic.ItalicPrototype
import me.proxer.app.ui.view.bbcode.left.LeftPrototype
import me.proxer.app.ui.view.bbcode.quote.QuotePrototype
import me.proxer.app.ui.view.bbcode.right.RightPrototype
import me.proxer.app.ui.view.bbcode.size.SizePrototype
import me.proxer.app.ui.view.bbcode.spoiler.SpoilerPrototype
import me.proxer.app.ui.view.bbcode.strikethrough.StrikethroughPrototype
import me.proxer.app.ui.view.bbcode.table.TableCellPrototype
import me.proxer.app.ui.view.bbcode.table.TablePrototype
import me.proxer.app.ui.view.bbcode.table.TableRowPrototype
import me.proxer.app.ui.view.bbcode.underline.UnderlinePrototype
import me.proxer.app.ui.view.bbcode.url.UrlPrototype
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
object BBParser {

    private val prototypes = arrayOf(BoldPrototype, ItalicPrototype, UnderlinePrototype, StrikethroughPrototype,
            SizePrototype, ColorPrototype, LeftPrototype, CenterPrototype, RightPrototype, SpoilerPrototype,
            QuotePrototype, UrlPrototype, ImagePrototype, DividerPrototype,
            TablePrototype, TableRowPrototype, TableCellPrototype)

    private val prototypeRegex
        get() = prototypes.joinToString("|") {
            when (it.canHaveChildren) {
                true -> it.startRegex.pattern + "|" + it.endRegex.pattern
                false -> it.startRegex.pattern
            }
        }

    private val regex = Regex("${quote("[")}(($prototypeRegex)?)${quote("]")}", REGEX_OPTIONS)

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

            if (currentTree.endsWith(part)) {
                currentTree = currentTree.parent
                        ?: throw IllegalStateException("tree does not have a parent: $currentTree")
            } else {
                var prototypeFound = false

                for (prototype in prototypes) {
                    val newTree = prototype.fromCode(it.groupValues[1], currentTree)

                    if (newTree != null) {
                        currentTree.children.add(newTree)
                        prototypeFound = true

                        if (prototype.canHaveChildren) currentTree = newTree

                        break
                    }
                }

                if (!prototypeFound) {
                    // If nothing found assume a user error and look for a fitting end tag in the existing tree.
                    if (currentTree.parent?.endsWith(part) == true) {
                        currentTree = currentTree.parent?.parent
                                ?: throw IllegalStateException("tree does not have a parent: $currentTree")
                    } else {
                        val unknownString = trimmedInput.substring(it.range.first, it.range.endInclusive + 1)

                        currentTree.children.add(TextLeaf(unknownString, currentTree))
                    }
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
