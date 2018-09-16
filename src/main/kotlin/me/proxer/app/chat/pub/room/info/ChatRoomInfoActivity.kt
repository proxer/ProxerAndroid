package me.proxer.app.chat.pub.room.info

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.transaction
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import org.jetbrains.anko.startActivity

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
        get() = intent.getStringExtra(CHAT_ROOM_ID_EXTRA)

    val chatRoomName: String
        get() = intent.getStringExtra(CHAT_ROOM_NAME_EXTRA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.transaction(now = true) {
                replace(R.id.container, ChatRoomInfoFragment.newInstance())
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = chatRoomName
    }
}
