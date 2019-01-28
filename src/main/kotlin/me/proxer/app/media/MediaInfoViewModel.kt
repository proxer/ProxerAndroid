package me.proxer.app.media

import androidx.lifecycle.MutableLiveData
import com.gojuno.koptional.rxjava2.filterSome
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
import me.proxer.app.util.extension.buildOptionalSingle
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
                        .buildOptionalSingle()
                        .filterSome()
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
            .skip(1)
            .subscribe {
                if (it && error.value?.buttonAction == ButtonAction.LOGIN) {
                    reload()
                }
            }

        disposables += preferenceHelper.isAgeRestrictedMediaAllowedObservable
            .skip(1)
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
    fun markAsFavorite() = updateUserInfo(UserInfoUpdateType.FAVORITE)
    fun markAsFinished() = updateUserInfo(UserInfoUpdateType.FINISHED)

    private fun updateUserInfo(updateType: UserInfoUpdateType) {
        val endpoint = when (updateType) {
            UserInfoUpdateType.NOTE -> api.info.note(entryId)
            UserInfoUpdateType.FAVORITE -> api.info.markAsFavorite(entryId)
            UserInfoUpdateType.FINISHED -> api.info.markAsFinished(entryId)
        }

        userInfoDisposable?.dispose()
        userInfoUpdateDisposable?.dispose()
        userInfoUpdateDisposable = Single.fromCallable { validators.validateLogin() }
            .flatMap { endpoint.buildOptionalSingle() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors({
                userInfoUpdateError.value = null
                userInfoUpdateData.value = Unit

                userInfoData.value?.also {
                    userInfoData.value = applyMediaUserInfoChanges(it, updateType)
                }
            }, {
                userInfoUpdateData.value = null
                userInfoUpdateError.value = ErrorUtils.handle(it)
            })
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
            else -> data.isTopTen
        }

        MediaUserInfo(isNoted, isFinished, data.isCanceled, isTopTen)
    }

    private enum class UserInfoUpdateType {
        NOTE, FAVORITE, FINISHED
    }
}
