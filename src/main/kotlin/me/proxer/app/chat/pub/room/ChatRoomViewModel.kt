package me.proxer.app.chat.pub.room

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.data.StorageHelper
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
        get() = api.chat().publicRooms().buildSingle()
            .let { if (StorageHelper.isLoggedIn) it.zipWith(api.chat().userRooms().buildSingle(), zipper) else it }
            .map {
                it
                    .asSequence()
                    .distinctBy { room -> room.id }
                    .sortedBy { room -> room.id }
                    .toList()
            }

    init {
        disposables += Observable.merge(bus.register(LoginEvent::class.java), bus.register(LogoutEvent::class.java))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { reload() }
    }
}
