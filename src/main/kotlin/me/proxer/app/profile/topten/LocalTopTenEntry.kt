package me.proxer.app.profile.topten

import me.proxer.library.entity.ProxerIdItem
import me.proxer.library.enums.Category
import me.proxer.library.enums.Medium

/**
 * @author Ruben Gees
 */
open class LocalTopTenEntry(
    override val id: String,
    open val name: String,
    open val category: Category,
    open val medium: Medium
) : ProxerIdItem {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalTopTenEntry) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (category != other.category) return false
        if (medium != other.medium) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + medium.hashCode()
        return result
    }

    override fun toString(): String {
        return "LocalTopTenEntry(id='$id', name='$name', category=$category, medium=$medium)"
    }

    data class Ucp(
        override val id: String,
        override val name: String,
        override val category: Category,
        override val medium: Medium,
        val entryId: String
    ) : LocalTopTenEntry(id, name, category, medium)
}
