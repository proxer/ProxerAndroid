package me.proxer.app.chat.prv.conference

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.R
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.newbase.NetworkState
import me.proxer.app.newbase.NewBaseViewModel
import me.proxer.app.newbase.paged.NewBaseSimpleDataSourceFactory
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import org.koin.standalone.inject
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ConferenceViewModel(query: String) : NewBaseViewModel<PagedList<ConferenceWithMessage>>() {

    override val isLoginRequired = true

    override val networkState = MutableLiveData<NetworkState>()
    override val data: LiveData<PagedList<ConferenceWithMessage>>

    val dataSourceFactory: MutableLiveData<NewBaseSimpleDataSourceFactory<ConferenceWithMessage>>

    var searchQuery by Delegates.observable(query) { _, old, new ->
        if (old != new) {
            invalidate()
        }
    }

    private val boundaryCallback = object : PagedList.BoundaryCallback<ConferenceWithMessage>() {
        override fun onZeroItemsLoaded() {
            if (!storageHelper.areConferencesSynchronized) {
                if (storageHelper.isLoggedIn) {
                    networkState.postValue(NetworkState.Loading)

                    if (!MessengerWorker.isRunning()) {
                        MessengerWorker.enqueueSynchronization()
                    }
                } else {
                    networkState.postValue(NetworkState.Error(ErrorUtils.handle(NotLoggedInException())))
                }
            } else {
                val errorAction = ErrorAction(R.string.error_no_data, ErrorAction.ACTION_MESSAGE_HIDE)

                networkState.postValue(NetworkState.Error(errorAction))
            }
        }
    }

    private val messengerDao by inject<MessengerDao>()

    init {
        dataSourceFactory = MutableLiveData<NewBaseSimpleDataSourceFactory<ConferenceWithMessage>>().apply {
            value = object : NewBaseSimpleDataSourceFactory<ConferenceWithMessage>() {
                override fun doCreate() = messengerDao.getConferencesDataSourceFactory(searchQuery)
            }
        }

        data = Transformations.switchMap(dataSourceFactory) {
            it.toLiveData(
                Config(20, enablePlaceholders = false, prefetchDistance = 5, initialLoadSizeHint = 20),
                boundaryCallback = boundaryCallback
            )
        }

        disposables += bus.register(MessengerWorker.SynchronizationEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (networkState.value == NetworkState.Loading) {
                    networkState.value = NetworkState.Idle
                }
            }
    }

    override fun invalidate() {
        dataSourceFactory.value?.dataSource?.value?.invalidate()
    }
}
