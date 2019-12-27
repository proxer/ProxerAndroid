package me.proxer.app.profile.history

import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.enums.Medium
import java.util.Date

/**
 * @author Ruben Gees
 */
open class LocalUserHistoryEntry(
    override val id: String,
    open val entryId: String,
    open val name: String,
    open val language: MediaLanguage,
    open val medium: Medium,
    open val category: Category,
    open val episode: Int
) : ProxerIdItem {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalUserHistoryEntry) return false

        if (id != other.id) return false
        if (entryId != other.entryId) return false
        if (name != other.name) return false
        if (language != other.language) return false
        if (medium != other.medium) return false
        if (category != other.category) return false
        if (episode != other.episode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + entryId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + episode
        return result
    }

    override fun toString(): String {
        return "LocalUserHistoryEntry(id='$id', entryId='$entryId', name='$name', language=$language, " +
            "medium=$medium, category=$category, episode=$episode)"
    }

    data class Ucp(
        override val id: String,
        override val entryId: String,
        override val name: String,
        override val language: MediaLanguage,
        override val medium: Medium,
        override val category: Category,
        override val episode: Int,
        val date: Date
    ) : LocalUserHistoryEntry(id, entryId, name, language, medium, category, episode)
}
