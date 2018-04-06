package me.proxer.app.media.list

import android.arch.lifecycle.MutableLiveData
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.tagDao
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.media.LocalTag
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toParcelableTag
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Genre
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import me.proxer.library.enums.TagType
import org.threeten.bp.LocalDateTime
import java.util.Date
import java.util.EnumSet
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class MediaListViewModel(
    sortCriteria: MediaSearchSortCriteria,
    type: MediaType,
    var searchQuery: String?,
    var language: Language?,
    var genres: EnumSet<Genre>,
    var excludedGenres: EnumSet<Genre>,
    var fskConstraints: EnumSet<FskConstraint>,
    var tags: List<LocalTag>,
    var excludedTags: List<LocalTag>
) : PagedContentViewModel<MediaListEntry>() {

    override val itemsOnPage = 30

    override val isLoginRequired: Boolean
        get() = type == MediaType.HENTAI || type == MediaType.HMANGA

    override val isAgeConfirmationRequired: Boolean
        get() = isLoginRequired

    override val endpoint: PagingLimitEndpoint<List<MediaListEntry>>
        get() = api.list().mediaSearch()
            .sort(sortCriteria)
            .name(searchQuery)
            .language(language)
            .genres(genres)
            .excludedGenres(excludedGenres)
            .fskConstraints(fskConstraints)
            .tags(tags.map { it.id }.toSet())
            .excludedTags(excludedTags.map { it.id }.toSet())
            .type(type)

    var sortCriteria by Delegates.observable(sortCriteria, { _, old, new ->
        if (old != new) reload()
    })

    var type by Delegates.observable(type, { _, old, new ->
        if (old != new) reload()
    })

    val tagData = MutableLiveData<List<LocalTag>>()

    private var tagsDisposable: Disposable? = null

    override fun onCleared() {
        tagsDisposable?.dispose()
        tagsDisposable = null

        super.onCleared()
    }

    override fun mergeNewDataWithExistingData(newData: List<MediaListEntry>, currentPage: Int): List<MediaListEntry> {
        return data.value?.plus(newData) ?: newData
    }

    fun loadTags() {
        tagsDisposable?.dispose()
        tagsDisposable = Single
            .fromCallable { tagDao.getTags(TagType.TAG) }
            .flatMap { cachedTags ->
                when {
                    shouldUpdateTags() || cachedTags.isEmpty() -> api.list().tagList().buildSingle()
                        .map { remoteTags -> remoteTags.map { it.toParcelableTag() } }
                        .doOnSuccess {
                            tagDao.replaceTags(it)

                            StorageHelper.lastTagUpdateDate = Date()
                        }
                    else -> Single.just(cachedTags)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors { tagData.value = it }
    }

    private fun shouldUpdateTags() = StorageHelper.lastTagUpdateDate.convertToDateTime()
        .isBefore(LocalDateTime.now().minusDays(15))
}
