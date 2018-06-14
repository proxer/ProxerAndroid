package me.proxer.app.chat.pub.room.info

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.chat.ChatRoomUser
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ChatRoomInfoFragment : BaseContentFragment<List<ChatRoomUser>>() {

    companion object {
        fun newInstance() = ChatRoomInfoFragment().apply {
            this.arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { ChatRoomInfoViewModelProvider.get(this, chatRoomId) }

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
            .autoDispose(this)
            .subscribe { (view, item) ->
                ProfileActivity.navigateTo(requireActivity(), item.id, item.name, item.image,
                    if (view.drawable != null && item.image.isNotBlank()) view else null)
            }

        adapter.statusLinkClickSubject
            .autoDispose(this)
            .subscribe { showPage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat_room_info, container, false)
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
