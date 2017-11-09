package me.proxer.app.media.info

import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.Validators
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.MediaUserInfo

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class MediaInfoViewModel(private val entryId: String) : BaseViewModel<Pair<Entry, Optional<MediaUserInfo>>>() {

    override val dataSingle: Single<Pair<Entry, Optional<MediaUserInfo>>>
        get() = Single.fromCallable { validate() }
                .flatMap { api.info().entry(entryId).buildSingle() }
                .flatMap { entry ->
                    when (StorageHelper.user != null) {
                        true -> api.info().userInfo(entryId)
                                .buildOptionalSingle()
                                .map { entry to it }
                                .onErrorReturn { entry to None }
                        false -> Single.just(None).map { entry to it }
                    }
                }

    val userInfoUpdateData = ResettingMutableLiveData<Unit?>()
    val userInfoUpdateError = ResettingMutableLiveData<ErrorAction?>()

    private var userInfoUpdateDisposable: Disposable? = null

    override fun onCleared() {
        userInfoUpdateDisposable?.dispose()
        userInfoUpdateDisposable = null

        super.onCleared()
    }

    fun note() = updateUserInfo(UserInfoUpdateType.NOTE)
    fun markAsFavorite() = updateUserInfo(UserInfoUpdateType.FAVORITE)
    fun markAsFinished() = updateUserInfo(UserInfoUpdateType.FINISHED)

    private fun updateUserInfo(updateType: UserInfoUpdateType) {
        val endpoint = when (updateType) {
            MediaInfoViewModel.UserInfoUpdateType.NOTE -> api.info().note(entryId)
            MediaInfoViewModel.UserInfoUpdateType.FAVORITE -> api.info().markAsFavorite(entryId)
            MediaInfoViewModel.UserInfoUpdateType.FINISHED -> api.info().markAsFinished(entryId)
        }

        userInfoUpdateDisposable?.dispose()
        userInfoUpdateDisposable = endpoint
                .buildOptionalSingle()
                .subscribeOn(Schedulers.io())
                .flatMap { Single.fromCallable { it.apply { Validators.validateLogin() } } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors({
                    userInfoUpdateError.value = null
                    userInfoUpdateData.value = Unit

                    data.value?.let {
                        data.value = copyData(it, updateType)
                    }
                }, {
                    userInfoUpdateData.value = null
                    userInfoUpdateError.value = ErrorUtils.handle(it)
                })
    }

    private fun copyData(
            data: Pair<Entry, Optional<MediaUserInfo>>,
            updateType: UserInfoUpdateType
    ) = data.first to data.second.toNullable()?.let {
        MediaUserInfo(
                it.isNoted || updateType == UserInfoUpdateType.NOTE,
                it.isFinished || updateType == UserInfoUpdateType.FINISHED,
                it.isCanceled,
                it.isTopTen || updateType == UserInfoUpdateType.FAVORITE
        )
    }.toOptional()

    private enum class UserInfoUpdateType {
        NOTE, FAVORITE, FINISHED
    }
}
