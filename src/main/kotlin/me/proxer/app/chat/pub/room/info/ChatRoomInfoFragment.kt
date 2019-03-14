package me.proxer.app.chat.pub.room.info

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.library.entity.chat.ChatRoomUser
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@ContentView(R.layout.fragment_chat_room_info)
class ChatRoomInfoFragment : BaseContentFragment<List<ChatRoomUser>>() {

    companion object {
        fun newInstance() = ChatRoomInfoFragment().apply {
            this.arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<ChatRoomInfoViewModel> { parametersOf(chatRoomId) }

    override val hostingActivity: ChatRoomInfoActivity
        get() = activity as ChatRoomInfoActivity

    private val chatRoomId: String
        get() = hostingActivity.chatRoomId

    private var adapter by Delegates.notNull<ChatRoomUserAdapter>()

    private val userList: RecyclerView by bindView(R.id.userList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ChatRoomUserAdapter()

        adapter.participantClickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, item) ->
                ProfileActivity.navigateTo(
                    requireActivity(), item.id, item.name, item.image,
                    if (view.drawable != null && item.image.isNotBlank()) view else null
                )
            }

        adapter.statusLinkClickSubject
            .autoDisposable(this.scope())
            .subscribe { showPage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        userList.isNestedScrollingEnabled = false
        userList.layoutManager = LinearLayoutManager(context)
        userList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        viewModel.resumePolling()
    }

    override fun onPause() {
        viewModel.pausePolling()

        super.onPause()
    }

    override fun showData(data: List<ChatRoomUser>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data)
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
