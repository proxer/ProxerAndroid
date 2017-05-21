package me.proxer.app.entity.chat

import me.proxer.library.entitiy.messenger.Conference

/**
 * @author Ruben Gees
 */
class MetaConference(val conference: Conference, val isLoadedFully: Boolean = false) {

    operator fun component1() = conference
    operator fun component2() = isLoadedFully

    fun toLocalConference(localId: Long) = conference.toLocalConference(localId, isLoadedFully)
}

fun Conference.toMetaConference(isLoadedFully: Boolean) = MetaConference(this, isLoadedFully)