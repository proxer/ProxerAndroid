package me.proxer.app.media.comment

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.info.Comment
import me.proxer.library.enums.CommentSortCriteria
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class CommentViewModel(private val entryId: String, sortCriteria: CommentSortCriteria) :
        PagedContentViewModel<Comment>() {

    override val itemsOnPage = 10

    override val endpoint: PagingLimitEndpoint<List<Comment>>
        get() = api.info().comments(entryId)
                .sort(sortCriteria)

    var sortCriteria by Delegates.observable(sortCriteria, { _, old, new ->
        if (old != new) reload()
    })
}
