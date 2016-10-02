package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.media.CommentAdapter
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.connection.info.request.CommentRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CommentFragment : EasyPagingFragment<Comment, CommentAdapter.CommentAdapterCallback>() {

    companion object {

        const val ITEMS_ON_PAGE = 25

        private const val ARGUMENT_ID = "id"

        fun newInstance(id: String): CommentFragment {
            return CommentFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.COMMENTS
    override val itemsOnPage = ITEMS_ON_PAGE

    override lateinit var adapter: CommentAdapter
    override lateinit var layoutManager: LinearLayoutManager

    private lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments.getString(ARGUMENT_ID)

        adapter = CommentAdapter(savedInstanceState)
        adapter.callback = object : CommentAdapter.CommentAdapterCallback() {
            override fun onUserClick(item: Comment) {
                UserActivity.navigateTo(activity, item.userId, item.username, item.imageId)
            }
        }

        layoutManager = LinearLayoutManager(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<Comment>> {
        return LoadingRequest(CommentRequest(id)
                .withPage(page)
                .withLimit(itemsOnPage))
    }
}