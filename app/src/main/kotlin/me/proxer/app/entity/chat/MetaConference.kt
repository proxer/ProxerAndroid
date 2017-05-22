package me.proxer.app.entity.chat

import me.proxer.library.entitiy.messenger.Conference

/**
 * @author Ruben Gees
 */
class MetaConference(val conference: Conference, val isFullyLoaded: Boolean = false) {

    operator fun component1() = conference
    operator fun component2() = isFullyLoaded

    fun toLocalConference(localId: Long) = conference.toLocalConference(localId, isFullyLoaded)
}

fun Conference.toMetaConference(isFullyLoaded: Boolean) = MetaConference(this, isFullyLoaded)