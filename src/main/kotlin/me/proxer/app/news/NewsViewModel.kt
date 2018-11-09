package me.proxer.app.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import me.proxer.app.newbase.paged.NewBasePagedViewModel
import me.proxer.library.entity.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
class NewsViewModel : NewBasePagedViewModel<NewsArticle, NewsDataSource>() {

    override val data: LiveData<PagedList<NewsArticle>> = Transformations.switchMap(dataSourceFactory) {
        it.toLiveData(Config(15, prefetchDistance = 5, enablePlaceholders = false, initialLoadSizeHint = 15))
    }

    override fun createDataSource(): NewsDataSource {
        return NewsDataSource(api, storageHelper)
    }
}
