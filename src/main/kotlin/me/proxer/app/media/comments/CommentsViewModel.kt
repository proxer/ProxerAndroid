package me.proxer.app.media.comments

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
import me.proxer.app.util.extension.toParsedComment
import me.proxer.library.enums.CommentSortCriteria
import org.threeten.bp.Instant
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

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private var deleteDisposable: Disposable? = null

    fun deleteComment(comment: ParsedComment) {
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
