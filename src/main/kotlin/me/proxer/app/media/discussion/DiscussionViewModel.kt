package me.proxer.app.media.discussion

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.ForumDiscussion

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class DiscussionViewModel(private val entryId: String) : BaseContentViewModel<List<ForumDiscussion>>() {

    override val endpoint: Endpoint<List<ForumDiscussion>>
        get() = api.info().forumDiscussions(entryId)
}
