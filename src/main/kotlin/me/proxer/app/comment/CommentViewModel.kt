package me.proxer.app.comment

import androidx.lifecycle.MutableLiveData
import com.gojuno.koptional.Optional
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalComment
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ServerErrorType
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.UserMediaProgress

/**
 * @author Ruben Gees
 */
class CommentViewModel(
    private val id: String?,
    private val entryId: String?
) : BaseViewModel<LocalComment>() {

    companion object {
        private const val MAX_LENGTH = 20_000

        private val defaultComment = LocalComment(
            id = "", entryId = "", mediaProgress = UserMediaProgress.WATCHED,
            ratingDetails = RatingDetails(genre = 0, story = 0, animation = 0, characters = 0, music = 0),
            content = "", overallRating = 0, episode = 0
        )
    }

    override val dataSingle: Single<LocalComment>
        get() = when (id == null && entryId == null) {
            true -> Single.just(defaultComment)
            false -> api.comment.comment(id, entryId).buildSingle()
                .map { it.toLocalComment() }
                .onErrorResumeNext { error: Throwable ->
                    when (error.isInvalidCommentError) {
                        true -> Single.just(defaultComment)
                        false -> Single.error<Nothing>(error)
                    }
                }
                .doOnSuccess { isUpdate.postValue(it.id.isNotBlank() && it.content.isNotBlank()) }
        }

    private val publishSingle
        get() = data.value.let { comment ->
            when {
                comment == null || comment.content.isBlank() -> null
                comment.content.length > MAX_LENGTH -> Single.error<Optional<Unit>>(CommentTooLongException())
                comment.id.isNotEmpty() -> api.comment.update(comment.id)
                    .comment(comment.content.trim())
                    .rating(comment.overallRating)
                    .buildOptionalSingle()

                else -> when (val it = entryId) {
                    null -> null
                    else -> api.comment.create(it)
                        .comment(comment.content.trim())
                        .rating(comment.overallRating)
                        .buildOptionalSingle()
                }
            }
        }

    val isUpdate = MutableLiveData(id.isNullOrBlank().not())

    val publishResult = ResettingMutableLiveData<Unit?>()
    val publishError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var hasFocused = false

    private var updateDisposable: Disposable? = null
    private var publishDisposable: Disposable? = null

    override fun onCleared() {
        updateDisposable?.dispose()
        publishDisposable?.dispose()

        updateDisposable = null
        publishDisposable = null

        super.onCleared()
    }

    fun updateRating(rating: Float) {
        require(rating in 0.0..5.0)

        data.value = data.value?.copy(overallRating = (rating * 2).toInt())
    }

    fun updateContent(newContent: String) {
        updateDisposable?.dispose()

        updateDisposable = Single.just(data.value.toOptional())
            .filterSome()
            .map { it.copy(content = newContent) }
            .subscribeOn(Schedulers.computation())
            .subscribeAndLogErrors { data.postValue(it) }
    }

    fun publish() {
        publishDisposable?.dispose()

        publishDisposable = publishSingle
            ?.doOnSubscribe { isLoading.postValue(true) }
            ?.doOnSubscribe { publishResult.postValue(null) }
            ?.doOnSubscribe { publishError.postValue(null) }
            ?.doAfterTerminate { isLoading.postValue(false) }
            ?.subscribeOn(Schedulers.io())
            ?.subscribeAndLogErrors({
                publishResult.postValue(Unit)
            }, {
                publishError.postValue(ErrorUtils.handle(it))
            })
    }

    private val Throwable.isInvalidCommentError
        get() = this is ProxerException && serverErrorType == ServerErrorType.COMMENT_INVALID_COMMENT
}
