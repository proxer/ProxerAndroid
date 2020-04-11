package me.proxer.app.chat.pub.room.info

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.commitNow
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity

/**
 * @author Ruben Gees
 */
class ChatRoomInfoActivity : DrawerActivity() {

    companion object {
        private const val CHAT_ROOM_ID_EXTRA = "chat_room_id"
        private const val CHAT_ROOM_NAME_EXTRA = "chat_room_name"

        fun navigateTo(context: Activity, chatRoomId: String, chatRoomName: String) {
            context.startActivity<ChatRoomInfoActivity>(
                CHAT_ROOM_ID_EXTRA to chatRoomId,
                CHAT_ROOM_NAME_EXTRA to chatRoomName
            )
        }
    }

    val chatRoomId: String
        get() = intent.getSafeStringExtra(CHAT_ROOM_ID_EXTRA)

    val chatRoomName: String
        get() = intent.getSafeStringExtra(CHAT_ROOM_NAME_EXTRA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = chatRoomName

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, ChatRoomInfoFragment.newInstance())
            }
        }
    }
}
