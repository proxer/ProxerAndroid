package me.proxer.app.media.comments

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toParsedComment
import me.proxer.library.enums.CommentSortCriteria
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class CommentsViewModel(
    private val entryId: String,
    sortCriteria: CommentSortCriteria
) : PagedViewModel<ParsedComment>() {

    override val itemsOnPage = 10

    override val dataSingle: Single<List<ParsedComment>>
        get() = Single.fromCallable { validate() }
            .flatMap {
                api.info.comments(entryId)
                    .sort(sortCriteria)
                    .page(page)
                    .limit(itemsOnPage)
                    .buildSingle()
            }
            .observeOn(Schedulers.computation())
            .map { it.map { comment -> comment.toParsedComment() } }

    var sortCriteria by Delegates.observable(sortCriteria) { _, old, new ->
        if (old != new) reload()
    }
}
