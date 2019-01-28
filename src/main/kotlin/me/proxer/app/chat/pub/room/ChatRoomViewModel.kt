package me.proxer.app.chat.pub.room

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entity.chat.ChatRoom

/**
 * @author Ruben Gees
 */
class ChatRoomViewModel : BaseViewModel<List<ChatRoom>>() {

    private companion object {
        private val zipper = BiFunction { first: List<ChatRoom>, second: List<ChatRoom> -> first + second }
    }

    override val dataSingle: Single<List<ChatRoom>>
        get() = api.chat.publicRooms().buildSingle()
            .let { if (storageHelper.isLoggedIn) it.zipWith(api.chat.userRooms().buildSingle(), zipper) else it }
            .map {
                it
                    .asSequence()
                    .distinctBy { room -> room.id }
                    .sortedBy { room -> room.id }
                    .toList()
            }

    init {
        disposables += storageHelper.isLoggedInObservable
            .skip(1)
            .subscribe { reload() }
    }
}
