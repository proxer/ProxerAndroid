package me.proxer.app.ui.view.bbcode

import me.proxer.app.ui.view.bbcode.prototype.AgeRestrictionPrototype
import me.proxer.app.ui.view.bbcode.prototype.AttachmentPrototype
import me.proxer.app.ui.view.bbcode.prototype.AutoClosingPrototype
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.prototype.BoldPrototype
import me.proxer.app.ui.view.bbcode.prototype.BreakPrototype
import me.proxer.app.ui.view.bbcode.prototype.CenterPrototype
import me.proxer.app.ui.view.bbcode.prototype.CodePrototype
import me.proxer.app.ui.view.bbcode.prototype.ColorPrototype
import me.proxer.app.ui.view.bbcode.prototype.DividerPrototype
import me.proxer.app.ui.view.bbcode.prototype.FacebookPrototype
import me.proxer.app.ui.view.bbcode.prototype.HidePrototype
import me.proxer.app.ui.view.bbcode.prototype.ImagePrototype
import me.proxer.app.ui.view.bbcode.prototype.ItalicPrototype
import me.proxer.app.ui.view.bbcode.prototype.LeftPrototype
import me.proxer.app.ui.view.bbcode.prototype.ListItemPrototype
import me.proxer.app.ui.view.bbcode.prototype.MapPrototype
import me.proxer.app.ui.view.bbcode.prototype.OrderedListPrototype
import me.proxer.app.ui.view.bbcode.prototype.PdfPrototype
import me.proxer.app.ui.view.bbcode.prototype.PollPrototype
import me.proxer.app.ui.view.bbcode.prototype.QuotePrototype
import me.proxer.app.ui.view.bbcode.prototype.RightPrototype
import me.proxer.app.ui.view.bbcode.prototype.RootPrototype
import me.proxer.app.ui.view.bbcode.prototype.SizePrototype
import me.proxer.app.ui.view.bbcode.prototype.SpoilerPrototype
import me.proxer.app.ui.view.bbcode.prototype.StrikethroughPrototype
import me.proxer.app.ui.view.bbcode.prototype.SubscriptPrototype
import me.proxer.app.ui.view.bbcode.prototype.SuperscriptPrototype
import me.proxer.app.ui.view.bbcode.prototype.TableCellPrototype
import me.proxer.app.ui.view.bbcode.prototype.TablePrototype
import me.proxer.app.ui.view.bbcode.prototype.TableRowPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import me.proxer.app.ui.view.bbcode.prototype.TwitterPrototype
import me.proxer.app.ui.view.bbcode.prototype.UnderlinePrototype
import me.proxer.app.ui.view.bbcode.prototype.UnorderedListPrototype
import me.proxer.app.ui.view.bbcode.prototype.UrlPrototype
import me.proxer.app.ui.view.bbcode.prototype.VideoPrototype
import me.proxer.app.ui.view.bbcode.prototype.WikiPrototype
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
object BBParser {

    private val textOnlyPrototypes = setOf(
        BoldPrototype, ItalicPrototype, UnderlinePrototype, StrikethroughPrototype, SizePrototype, ColorPrototype,
        LeftPrototype, CenterPrototype, RightPrototype, SuperscriptPrototype, SubscriptPrototype
    )

    private val simplePrototypes = textOnlyPrototypes.plus(SpoilerPrototype)

    private val defaultPrototypes = simplePrototypes.plus(
        setOf(
            QuotePrototype, UrlPrototype, QuotePrototype, UrlPrototype, ImagePrototype, DividerPrototype,
            VideoPrototype, TablePrototype, TableRowPrototype, TableCellPrototype, CodePrototype, HidePrototype,
            UnorderedListPrototype, OrderedListPrototype, ListItemPrototype, MapPrototype, AttachmentPrototype,
            FacebookPrototype, TwitterPrototype, PollPrototype, BreakPrototype, PdfPrototype, AgeRestrictionPrototype,
            WikiPrototype
        )
    )

    private val regexCache = mutableMapOf<Set<BBPrototype>, Regex>()

    init {
        regexCache[textOnlyPrototypes] = constructRegex(textOnlyPrototypes)
        regexCache[simplePrototypes] = constructRegex(simplePrototypes)
        regexCache[defaultPrototypes] = constructRegex(defaultPrototypes)
    }

    fun parseSimple(input: String): BBTree {
        return parse(input, simplePrototypes)
    }

    fun parse(input: String, prototypes: Set<BBPrototype> = defaultPrototypes): BBTree {
        val trimmedInput = input.trim()
        val result = BBTree(RootPrototype, null)
        val parts = constructRegex(prototypes).findAll(trimmedInput)

        val finishedList = mutableListOf<BBTree>()
        var currentTree = result
        var currentPosition = 0

        parts.forEach {
            val part = it.groupValues[1].trim()

            if (it.range.first > currentPosition) {
                val startString = trimmedInput.substring(currentPosition, it.range.first)

                currentTree.children.add(TextPrototype.construct(startString, currentTree))
            }

            if (currentTree.endsWith(part)) {
                finishedList += currentTree

                currentTree = findNextUnfinishedTree(currentTree, finishedList)
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

                // If nothing found assume a user error and look for a fitting end tag in the existing tree.
                if (!prototypeFound) {
                    val fittingTree = findFittingTree(currentTree, part, finishedList)

                    if (fittingTree != null) {
                        finishedList += fittingTree

                        if (fittingTree.prototype is AutoClosingPrototype) {
                            currentTree = requireNotNull(fittingTree.parent)
                        }
                    } else {
                        val unknownString = trimmedInput.substring(it.range.first, it.range.last + 1)

                        currentTree.children.add(TextPrototype.construct(unknownString, currentTree))
                    }
                }
            }

            currentPosition = it.range.last + 1
        }

        if (currentPosition < trimmedInput.length) {
            val endString = trimmedInput.substring(currentPosition, trimmedInput.length)

            currentTree.children.add(TextPrototype.construct(endString, currentTree))
        }

        return result
    }

    private fun constructRegex(prototypes: Set<BBPrototype>) = regexCache.getOrPut(prototypes) {
        val prototypeRegex = prototypes.joinToString("|") {
            when (it.canHaveChildren) {
                true -> it.startRegex.pattern + "|" + it.endRegex.pattern
                false -> it.startRegex.pattern
            }
        }

        Regex("${quote("[")}(($prototypeRegex)?)${quote("]")}", REGEX_OPTIONS)
    }

    private fun findFittingTree(tree: BBTree, endTag: String, finishedList: List<BBTree>): BBTree? {
        var currentTree = tree.parent

        while (true) {
            if (currentTree?.endsWith(endTag) == true && finishedList.none { it === currentTree }) {
                return currentTree
            } else if (currentTree?.parent == null) {
                return null
            }

            currentTree = currentTree.parent
        }
    }

    private fun findNextUnfinishedTree(tree: BBTree, finishedList: List<BBTree>): BBTree {
        var currentTree = tree

        while (true) {
            if (finishedList.none { it === currentTree }) {
                return currentTree
            } else {
                currentTree = requireNotNull(currentTree.parent)
            }
        }
    }
}

fun String.toBBTree(args: BBArgs = BBArgs()) = BBParser.parse(this).optimize(args)
fun String.toSimpleBBTree(args: BBArgs = BBArgs()) = BBParser.parseSimple(this).optimize(args)
