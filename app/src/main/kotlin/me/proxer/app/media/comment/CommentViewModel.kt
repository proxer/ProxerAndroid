package me.proxer.app.media.comment

import android.app.Application
import me.proxer.app.MainApplication
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.info.Comment
import me.proxer.library.enums.CommentSortCriteria

/**
 * @author Ruben Gees
 */
class CommentViewModel(application: Application) : PagedContentViewModel<Comment>(application) {

    override val itemsOnPage = 10

    override val endpoint: PagingLimitEndpoint<List<Comment>>
        get() = MainApplication.api.info()
                .comments(entryId)
                .sort(sortCriteria)

    lateinit var entryId: String

    private var sortCriteria = CommentSortCriteria.RATING

    fun setSortCriteria(value: CommentSortCriteria, trigger: Boolean = true) {
        if (sortCriteria != value) {
            sortCriteria = value

            if (trigger) reload()
        }
    }
}
