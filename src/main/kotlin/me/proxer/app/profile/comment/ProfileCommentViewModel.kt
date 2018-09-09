package me.proxer.app.profile.comment

import com.gojuno.koptional.Optional
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
    private val userId: Optional<String>,
    private val username: Optional<String>,
    category: Optional<Category>
) : PagedViewModel<ParsedUserComment>() {

    override val itemsOnPage = 10

    override val dataSingle: Single<List<ParsedUserComment>>
        get() = Single.fromCallable { validate() }
            .flatMap {
                api.user().comments(userId.toNullable(), username.toNullable())
                    .category(category.toNullable())
                    .page(page)
                    .limit(itemsOnPage)
                    .buildSingle()
            }
            .observeOn(Schedulers.computation())
            .map { it.map { comment -> comment.toParsedUserComment() } }

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }
}
