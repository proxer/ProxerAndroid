package me.proxer.app.ui.view.bbcode

import android.content.res.Resources
import android.text.Spanned
import me.proxer.app.GlideRequests
import java.lang.ref.WeakReference
import java.util.LinkedHashMap

/**
 * @author Ruben Gees
 */
class BBArgs : LinkedHashMap<String, Any?> {

    companion object {
        private const val TEXT_ARGUMENT = "text"
        private const val GLIDE_ARGUMENT = "glide"
        private const val USER_ID_ARGUMENT = "userId"
        private const val RESOURCES_ARGUMENT = "resources"
        private const val ENABLE_EMOTICONS_ARGUMENT = "enable_emoticons"
    }

    var text: CharSequence?
        get() = this[TEXT_ARGUMENT] as? CharSequence
        set(value) {
            this[TEXT_ARGUMENT] = value
        }

    val resources get() = this[RESOURCES_ARGUMENT] as? Resources?
    val glide get() = (this[GLIDE_ARGUMENT] as? WeakReference<*>)?.get() as? GlideRequests?
    val userId get() = this[USER_ID_ARGUMENT] as? String?
    val enableEmoticons get() = this[ENABLE_EMOTICONS_ARGUMENT] as? Boolean? ?: false

    val safeText get() = requireNotNull(text)
    val safeResources get() = requireNotNull(resources)
    val safeUserId get() = requireNotNull(userId)

    constructor(
        text: CharSequence? = null,
        resources: Resources? = null,
        glide: GlideRequests? = null,
        userId: String? = null,
        enableEmoticons: Boolean? = null,
        vararg custom: Pair<String, Any?>
    ) {
        if (text != null) this[TEXT_ARGUMENT] = text
        if (resources != null) this[RESOURCES_ARGUMENT] = resources
        if (glide != null) this[GLIDE_ARGUMENT] = WeakReference(glide)
        if (userId != null) this[USER_ID_ARGUMENT] = userId
        if (enableEmoticons != null) this[ENABLE_EMOTICONS_ARGUMENT] = enableEmoticons

        custom.forEach { (key, value) -> this[key] = value }
    }

    constructor(args: Map<String, Any?>) {
        putAll(args)
    }

    operator fun plus(other: BBArgs) = BBArgs().also {
        it.putAll(this)
        it.putAll(other)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BBArgs

        if (size != other.size) return false

        // Work around a bug in the SpannableStringBuilder's (and possibly others) implementation.
        // See: https://stackoverflow.com/a/46403431/4279995.
        this.forEach { (key, value) ->
            other.forEach { (otherKey, otherValue) ->
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

    @Suppress("RedundantOverride")
    override fun hashCode() = super.hashCode()

    override fun toString() = "BBArgs() ${super.toString()}"
}
