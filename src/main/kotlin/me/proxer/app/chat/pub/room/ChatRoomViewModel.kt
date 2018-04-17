package me.proxer.app.chat.pub.room

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entity.chat.ChatRoom

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ChatRoomViewModel : BaseViewModel<List<ChatRoom>>() {

    private companion object {
        private val zipper = BiFunction { first: List<ChatRoom>, second: List<ChatRoom> -> first + second }
    }

    override val dataSingle: Single<List<ChatRoom>>
        get() = api.chat().publicRooms().buildSingle()
            .zipWith(api.chat().userRooms().buildSingle(), zipper)
            .map { it.distinctBy { it.id }.sortedBy { it.id } }
}
