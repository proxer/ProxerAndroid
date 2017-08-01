package me.proxer.app.media.info

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.library.api.Endpoint
import me.proxer.library.entitiy.info.Entry

/**
 * @author Ruben Gees
 */
class MediaInfoViewModel(application: Application) : BaseContentViewModel<Entry>(application) {

    override val endpoint: Endpoint<Entry>
        get() = MainApplication.api.info()
                .entry(entryId)

    val userInfoUpdateData = MutableLiveData<Unit?>()
    val userInfoUpdateError = MutableLiveData<ErrorAction?>()

    lateinit var entryId: String

    private var userInfoUpdateDisposable: Disposable? = null

    override fun onCleared() {
        userInfoUpdateDisposable?.dispose()
        userInfoUpdateDisposable = null

        super.onCleared()
    }

    fun note() = updateUserInfo(api.info().note(entryId))
    fun markAsFavorite() = updateUserInfo(api.info().markAsFavorite(entryId))
    fun markAsFinished() = updateUserInfo(api.info().markAsFinished(entryId))

    private fun updateUserInfo(endpoint: Endpoint<Void>) {
        userInfoUpdateDisposable?.dispose()
        userInfoUpdateDisposable = endpoint
                .buildOptionalSingle()
                .subscribeOn(Schedulers.io())
                .flatMap { Single.fromCallable { it.apply { Validators.validateLogin() } } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    userInfoUpdateError.value = null
                    userInfoUpdateData.value = Unit
                }, {
                    userInfoUpdateData.value = null
                    userInfoUpdateError.value = ErrorUtils.handle(it)
                })
    }
}