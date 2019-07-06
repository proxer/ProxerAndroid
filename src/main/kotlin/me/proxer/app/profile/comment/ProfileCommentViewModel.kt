package me.proxer.app.profile.comment

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toParsedUserComment
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ProfileCommentViewModel(
    private val userId: String?,
    private val username: String?,
    category: Category?
) : PagedViewModel<ParsedUserComment>() {

    override val itemsOnPage = 10

    override val dataSingle: Single<List<ParsedUserComment>>
        get() = Single.fromCallable { validate() }
            .flatMap {
                api.user.comments(userId, username)
                    .category(category)
                    .page(page)
                    .limit(itemsOnPage)
                    .minimumLength(1)
                    .buildSingle()
            }
            .observeOn(Schedulers.computation())
            .map { it.map { comment -> comment.toParsedUserComment() } }

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }
}
