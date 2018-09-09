package me.proxer.app.forum

import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toParsedPost
import me.proxer.app.util.extension.toTopicMetaData

/**
 * @author Ruben Gees
 */
class TopicViewModel(private val id: String) : PagedViewModel<ParsedPost>() {

    override val itemsOnPage = 10

    override val dataSingle: Single<List<ParsedPost>>
        get() = api.forum().topic(id)
            .page(page)
            .limit(itemsOnPage)
            .buildSingle()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { metaData.value = it.toTopicMetaData() }
            .observeOn(Schedulers.computation())
            .map { it.posts.map { post -> post.toParsedPost() } }

    val metaData = MutableLiveData<TopicMetaData>()
}
