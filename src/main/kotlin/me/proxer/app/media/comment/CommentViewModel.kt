package me.proxer.app.media.comment

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toParsedComment
import me.proxer.library.enums.CommentSortCriteria
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class CommentViewModel(
        private val entryId: String,
        sortCriteria: CommentSortCriteria
) : PagedViewModel<ParsedComment>() {

    override val itemsOnPage = 10

    override val dataSingle: Single<List<ParsedComment>>
        get() = Single.fromCallable { validate() }
                .flatMap {
                    api.info().comments(entryId)
                            .sort(sortCriteria)
                            .page(page)
                            .limit(itemsOnPage)
                            .buildSingle()
                }
                .map { it.map { it.toParsedComment() } }

    var sortCriteria by Delegates.observable(sortCriteria, { _, old, new ->
        if (old != new) reload()
    })
}
