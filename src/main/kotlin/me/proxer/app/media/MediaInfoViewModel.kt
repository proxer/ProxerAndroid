package me.proxer.app.media

import androidx.lifecycle.MutableLiveData
import com.gojuno.koptional.Some
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.BaseViewModel
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.isTrulyAgeRestricted
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.MediaUserInfo

/**
 * @author Ruben Gees
 */
class MediaInfoViewModel(private val entryId: String) : BaseViewModel<Entry>() {

    override val dataSingle: Single<Entry>
        get() = Single.fromCallable { validate() }
            .flatMap { api.info.entry(entryId).buildSingle() }
            .doOnSuccess {
                if (it.isTrulyAgeRestricted) {
                    if (!storageHelper.isLoggedIn) {
                        throw NotLoggedInException()
                    } else if (!preferenceHelper.isAgeRestrictedMediaAllowed) {
                        throw AgeConfirmationRequiredException()
                    }
                }
            }
            .doAfterSuccess {
                if (storageHelper.isLoggedIn) {
                    userInfoDisposable = api.info.userInfo(entryId)
                        .buildSingle()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeAndLogErrors { userInfoData.value = it }
                }
            }

    val userInfoData = MutableLiveData<MediaUserInfo?>()

    val userInfoUpdateData = ResettingMutableLiveData<Unit?>()
    val userInfoUpdateError = ResettingMutableLiveData<ErrorAction?>()

    private var userInfoDisposable: Disposable? = null
    private var userInfoUpdateDisposable: Disposable? = null

    init {
        disposables += storageHelper.isLoggedInObservable
            .subscribe {
                if (it && error.value?.buttonAction == ButtonAction.LOGIN) {
                    reload()
                }
            }

        disposables += preferenceHelper.isAgeRestrictedMediaAllowedObservable
            .subscribe {
                if (error.value?.buttonAction == ButtonAction.AGE_CONFIRMATION) {
                    reload()
                }
            }
    }

    override fun onCleared() {
        userInfoDisposable?.dispose()
        userInfoUpdateDisposable?.dispose()

        userInfoDisposable = null
        userInfoUpdateDisposable = null

        super.onCleared()
    }

    fun note() = updateUserInfo(UserInfoUpdateType.NOTE)
    fun markAsFinished() = updateUserInfo(UserInfoUpdateType.FINISHED)

    fun toggleFavorite() {
        if (userInfoData.value?.isTopTen == true) {
            updateUserInfo(UserInfoUpdateType.UNFAVORITE)
        } else {
            updateUserInfo(UserInfoUpdateType.FAVORITE)
        }
    }

    fun toggleSubscription() {
        if (userInfoData.value?.isSubscribed == true) {
            updateUserInfo(UserInfoUpdateType.UNSUBSCRIBE)
        } else {
            updateUserInfo(UserInfoUpdateType.SUBSCRIBE)
        }
    }

    private fun updateUserInfo(updateType: UserInfoUpdateType) {
        val endpoint = when (updateType) {
            UserInfoUpdateType.NOTE -> api.info.note(entryId).buildSingle()
            UserInfoUpdateType.FAVORITE -> api.info.markAsFavorite(entryId).buildSingle()
            UserInfoUpdateType.FINISHED -> api.info.markAsFinished(entryId).buildSingle()
            UserInfoUpdateType.SUBSCRIBE -> api.info.subscribe(entryId).buildSingle()
            UserInfoUpdateType.UNSUBSCRIBE -> api.ucp.deleteSubscription(entryId).buildSingle()
            UserInfoUpdateType.UNFAVORITE -> api.ucp.topTen().buildSingle().flatMap { topTenEntries ->
                val topTenId = topTenEntries.find { topTenEntry -> topTenEntry.entryId == entryId }?.id

                if (topTenId != null) {
                    api.ucp.deleteFavorite(topTenId).buildSingle()
                } else {
                    Single.just(Some(Unit))
                }
            }
        }

        userInfoDisposable?.dispose()
        userInfoUpdateDisposable?.dispose()
        userInfoUpdateDisposable = Single.fromCallable { validators.validateLogin() }
            .flatMap { endpoint }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors(
                {
                    userInfoUpdateError.value = null
                    userInfoUpdateData.value = Unit

                    userInfoData.value?.also {
                        userInfoData.value = applyMediaUserInfoChanges(it, updateType)
                    }
                },
                {
                    userInfoUpdateData.value = null
                    userInfoUpdateError.value = ErrorUtils.handle(it)
                }
            )
    }

    private fun applyMediaUserInfoChanges(data: MediaUserInfo, updateType: UserInfoUpdateType) = data.let {
        val isNoted = when (updateType) {
            UserInfoUpdateType.FINISHED -> false
            UserInfoUpdateType.NOTE -> true
            else -> data.isNoted
        }

        val isFinished = when (updateType) {
            UserInfoUpdateType.FINISHED -> true
            else -> data.isFinished
        }

        val isTopTen = when (updateType) {
            UserInfoUpdateType.FAVORITE -> true
            UserInfoUpdateType.UNFAVORITE -> false
            else -> data.isTopTen
        }

        val isSubscribed = when (updateType) {
            UserInfoUpdateType.SUBSCRIBE -> true
            UserInfoUpdateType.UNSUBSCRIBE -> false
            else -> data.isSubscribed
        }

        MediaUserInfo(isNoted, isFinished, data.isCanceled, isTopTen, isSubscribed)
    }

    private enum class UserInfoUpdateType {
        NOTE, FAVORITE, UNFAVORITE, FINISHED, SUBSCRIBE, UNSUBSCRIBE
    }
}
