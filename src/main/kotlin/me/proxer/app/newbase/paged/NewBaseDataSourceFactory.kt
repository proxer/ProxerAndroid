package me.proxer.app.newbase.paged

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource

/**
 * @author Ruben Gees
 */
abstract class NewBaseDataSourceFactory<T, D : DataSource<Int, T>> : DataSource.Factory<Int, T>() {

    val dataSource = MutableLiveData<D>()

    override fun create(): D {
        val result = doCreate()

        dataSource.postValue(result)

        return result
    }

    abstract fun doCreate(): D
}
