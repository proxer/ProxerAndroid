package me.proxer.app.newbase.paged

import androidx.paging.DataSource

/**
 * @author Ruben Gees
 */
abstract class NewBaseSimpleDataSourceFactory<T> : NewBaseDataSourceFactory<T, DataSource<Int, T>>()
