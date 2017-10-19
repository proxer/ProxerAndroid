package me.proxer.app.util.extension

import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage
import me.proxer.app.manga.local.LocalMangaChapter
import me.proxer.library.entity.info.EntryCore

/**
 * @author Ruben Gees
 */

typealias CompleteLocalMangaEntry = Pair<EntryCore, List<LocalMangaChapter>>
typealias LocalConferenceMap = Map<LocalConference, List<LocalMessage>>

typealias ProxerNotification = me.proxer.library.entity.notifications.Notification
