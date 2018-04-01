package me.proxer.app.media.list

import android.arch.lifecycle.MutableLiveData
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.extension.buildSingle
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
    var tags: List<ParcelableTag>,
    var excludedTags: List<ParcelableTag>
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

    val tagData = MutableLiveData<List<ParcelableTag>>()

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
        // TODO: Add caching
        tagsDisposable?.dispose()
        tagsDisposable = api.list().tagList().type(TagType.TAG).buildSingle()
            .map { it.map { it.toParcelableTag() } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors { tagData.value = it }
    }
}
