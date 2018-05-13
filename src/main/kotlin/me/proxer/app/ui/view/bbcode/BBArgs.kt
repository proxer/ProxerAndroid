package me.proxer.app.ui.view.bbcode

import android.text.Spanned
import me.proxer.app.GlideRequests

/**
 * @author Ruben Gees
 */
class BBArgs {

    private companion object {
        private const val TEXT_ARGUMENT = "text"
        private const val GLIDE_ARGUMENT = "glide"
        private const val USER_ID_ARGUMENT = "userId"
        private const val ENABLE_EMOTICONS_ARGUMENT = "enable_emoticons"

        private val predefinedKeys = arrayOf(TEXT_ARGUMENT, GLIDE_ARGUMENT, USER_ID_ARGUMENT, ENABLE_EMOTICONS_ARGUMENT)
    }

    var text: CharSequence?
        get() = internalArgs[TEXT_ARGUMENT] as? CharSequence
        set(value) {
            internalArgs[TEXT_ARGUMENT] = value
        }

    val glide get() = internalArgs[GLIDE_ARGUMENT] as? GlideRequests
    val userId get() = internalArgs[USER_ID_ARGUMENT] as? String
    val enableEmoticons get() = internalArgs[ENABLE_EMOTICONS_ARGUMENT] as? Boolean ?: false

    val safeText get() = text ?: throw IllegalStateException("text is null")
    val safeUserId get() = userId ?: throw IllegalStateException("userId is null")

    private val internalArgs: MutableMap<String, Any?>

    constructor() {
        internalArgs = mutableMapOf()
    }

    constructor(
        text: CharSequence? = null,
        glide: GlideRequests? = null,
        userId: String? = null,
        enableEmoticons: Boolean? = null,
        vararg custom: Pair<String, Any?>
    ) {
        internalArgs = mutableMapOf()

        if (text != null) internalArgs[TEXT_ARGUMENT] = text
        if (glide != null) internalArgs[GLIDE_ARGUMENT] = glide
        if (userId != null) internalArgs[USER_ID_ARGUMENT] = userId
        if (enableEmoticons != null) internalArgs[ENABLE_EMOTICONS_ARGUMENT] = enableEmoticons

        custom.forEach { (key, value) -> internalArgs[key] = value }
    }

    constructor(args: Map<String, Any?>) {
        internalArgs = args.toMutableMap()
    }

    operator fun get(key: String): Any? {
        return internalArgs[key]
    }

    operator fun plus(other: BBArgs): BBArgs {
        return BBArgs(internalArgs + other.internalArgs)
    }

    fun put(key: String, value: Any?) {
        if (key in predefinedKeys) {
            throw IllegalArgumentException("Do not use put for the predefined key $key")
        }

        internalArgs[key] = value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BBArgs

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
        return internalArgs.hashCode()
    }

    override fun toString(): String {
        return "BBArgs(internalArgs=$internalArgs)"
    }
}
