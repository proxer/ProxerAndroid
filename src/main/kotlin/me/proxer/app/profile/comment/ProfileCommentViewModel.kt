package me.proxer.app.profile.comment

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toParsedUserComment
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ProfileCommentViewModel(
        private val userId: String?,
        private val username: String?,
        category: Category?
) : PagedViewModel<ParsedUserComment>() {

    override val itemsOnPage = 10

    override val dataSingle: Single<List<ParsedUserComment>>
        get() = Single.fromCallable { validate() }
                .flatMap {
                    api.user().comments(userId, username)
                            .category(category)
                            .page(page)
                            .limit(itemsOnPage)
                            .buildSingle()
                }
                .map { it.map { it.toParsedUserComment() } }

    var category by Delegates.observable(category, { _, old, new ->
        if (old != new) reload()
    })
}
