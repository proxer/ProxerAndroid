package me.proxer.app.profile.comment

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedViewModel
import me.proxer.app.comment.LocalComment
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toParsedUserComment
import me.proxer.library.enums.Category
import me.proxer.library.enums.CommentContentType
import org.threeten.bp.Instant
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
                    .hasContent(CommentContentType.COMMENT, CommentContentType.RATING)
                    .buildSingle()
            }
            .observeOn(Schedulers.computation())
            .map { it.map { comment -> comment.toParsedUserComment() } }

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private var deleteDisposable: Disposable? = null

    fun deleteComment(comment: ParsedUserComment) {
        deleteDisposable?.dispose()

        deleteDisposable = api.comment.update(comment.id)
            .comment("")
            .rating(0)
            .buildOptionalSingle()
            .doOnSuccess { storageHelper.deleteCommentDraft(comment.entryId) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors(
                {
                    data.value = data.value?.filter { it.id != comment.id }
                },
                {
                    itemDeletionError.value = ErrorUtils.handle(it)
                }
            )
    }

    fun updateComment(comment: LocalComment) {
        data.value = data.value?.map {
            if (it.id == comment.id) {
                it.copy(
                    ratingDetails = comment.ratingDetails,
                    parsedContent = comment.parsedContent,
                    overallRating = comment.overallRating,
                    instant = Instant.now()
                )
            } else {
                it
            }
        }
    }
}
