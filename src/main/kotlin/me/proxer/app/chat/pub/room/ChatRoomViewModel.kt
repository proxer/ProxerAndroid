package me.proxer.app.chat.pub.room

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.chat.ChatRoom

@GeneratedProvider
class ChatRoomViewModel : BaseContentViewModel<List<ChatRoom>>() {

    override val isLoginRequired = true

    override val endpoint: Endpoint<List<ChatRoom>>
        get() = api.chat().publicRooms()
}
