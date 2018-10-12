package me.proxer.app.chat.pub.message

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.transaction
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.pub.room.info.ChatRoomInfoActivity
import me.proxer.app.util.extension.startActivity

/**
 * @author Ruben Gees
 */
class ChatActivity : DrawerActivity() {

    companion object {
        private const val CHAT_ROOM_ID_EXTRA = "chat_room_id"
        private const val CHAT_ROOM_NAME_EXTRA = "chat_room_name"
        private const val CHAT_ROOM_IS_READ_ONLY_EXTRA = "chat_room_is_read_only"

        fun navigateTo(context: Activity, chatRoomId: String, chatRoomName: String, chatRoomIsReadOnly: Boolean) {
            context.startActivity<ChatActivity>(
                CHAT_ROOM_ID_EXTRA to chatRoomId,
                CHAT_ROOM_NAME_EXTRA to chatRoomName,
                CHAT_ROOM_IS_READ_ONLY_EXTRA to chatRoomIsReadOnly
            )
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
            supportFragmentManager.transaction(now = true) {
                replace(R.id.container, ChatFragment.newInstance())
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = chatRoomName

        toolbar.clicks()
            .autoDisposable(this.scope())
            .subscribe {
                ChatRoomInfoActivity.navigateTo(this, chatRoomId, chatRoomName)
            }
    }
}
