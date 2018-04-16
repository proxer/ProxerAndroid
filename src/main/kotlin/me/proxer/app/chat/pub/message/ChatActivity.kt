package me.proxer.app.chat.pub.message

import android.app.Activity
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.pub.room.info.ChatRoomInfoActivity
import me.proxer.app.util.extension.autoDispose
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class ChatActivity : DrawerActivity() {

    companion object {
        private const val CHAT_ROOM_ID_EXTRA = "chat_room_id"
        private const val CHAT_ROOM_NAME_EXTRA = "chat_room_name"
        private const val CHAT_ROOM_IS_READ_ONLY_EXTRA = "chat_room_is_read_only"

        fun navigateTo(context: Activity, chatRoomId: String, chatRoomName: String, chatRoomIsReadOnly: Boolean) {
            context.startActivity(context.intentFor<ChatActivity>(
                CHAT_ROOM_ID_EXTRA to chatRoomId,
                CHAT_ROOM_NAME_EXTRA to chatRoomName,
                CHAT_ROOM_IS_READ_ONLY_EXTRA to chatRoomIsReadOnly
            ))
        }
    }

    val chatRoomId: String
        get() = intent.getStringExtra(CHAT_ROOM_ID_EXTRA)

    val chatRoomName: String
        get() = intent.getStringExtra(CHAT_ROOM_NAME_EXTRA)

    val chatRoomIsReadOnly: Boolean
        get() = intent.getBooleanExtra(CHAT_ROOM_IS_READ_ONLY_EXTRA, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ChatFragment.newInstance())
                .commitNow()
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = chatRoomName

        toolbar.clicks()
            .autoDispose(this)
            .subscribe {
                ChatRoomInfoActivity.navigateTo(this, chatRoomId, chatRoomName)
            }
    }
}
