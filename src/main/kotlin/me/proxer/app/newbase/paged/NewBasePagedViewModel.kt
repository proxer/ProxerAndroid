package me.proxer.app.newbase.paged

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import me.proxer.app.newbase.NetworkState
import me.proxer.app.newbase.NewBaseViewModel

/**
 * @author Ruben Gees
 */
abstract class NewBasePagedViewModel<T, D : NewBaseDataSource<T>> : NewBaseViewModel<PagedList<T>>() {

    final override val networkState: LiveData<NetworkState>

    val dataSourceFactory: MutableLiveData<NewBaseDataSourceFactory<T, D>>
    val dataSource: LiveData<D>

    init {
        dataSourceFactory = MutableLiveData<NewBaseDataSourceFactory<T, D>>().apply {
            value = object : NewBaseDataSourceFactory<T, D>() {
                override fun doCreate() = createDataSource()
            }
        }

        dataSource = Transformations.switchMap(dataSourceFactory) { it.dataSource }
        networkState = Transformations.switchMap(dataSource) { it.networkState }
    }

    override fun retry() {
        dataSource.value?.retry()
    }

    override fun invalidate() {
        dataSource.value?.invalidate()
    }

    abstract fun createDataSource(): D
}
