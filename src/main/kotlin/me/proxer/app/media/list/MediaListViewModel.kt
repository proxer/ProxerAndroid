package me.proxer.app.media.list

import androidx.lifecycle.MutableLiveData
import com.gojuno.koptional.Optional
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.media.LocalTag
import me.proxer.app.media.TagDao
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.enumSetOf
import me.proxer.app.util.extension.isAgeRestricted
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toParcelableTag
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.entity.list.Tag
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import me.proxer.library.enums.TagRateFilter
import me.proxer.library.enums.TagSpoilerFilter
import me.proxer.library.enums.TagType
import org.koin.standalone.inject
import org.threeten.bp.LocalDateTime
import java.util.Date
import java.util.EnumSet
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MediaListViewModel(
    sortCriteria: MediaSearchSortCriteria,
    type: MediaType,
    var searchQuery: Optional<String>,
    var language: Optional<Language>,
    var genres: List<LocalTag>,
    var excludedGenres: List<LocalTag>,
    var fskConstraints: EnumSet<FskConstraint>,
    var tags: List<LocalTag>,
    var excludedTags: List<LocalTag>,
    var tagRateFilter: TagRateFilter?,
    var tagSpoilerFilter: TagSpoilerFilter?
) : PagedContentViewModel<MediaListEntry>() {

    override val itemsOnPage = 30

    override val isLoginRequired: Boolean
        get() = type.isAgeRestricted()

    override val isAgeConfirmationRequired: Boolean
        get() = isLoginRequired

    override val endpoint: PagingLimitEndpoint<List<MediaListEntry>>
        get() = api.list().mediaSearch()
            .sort(sortCriteria)
            .name(searchQuery.toNullable())
            .language(language.toNullable())
            .genreTags(genres.asSequence().map { it.id }.toSet())
            .excludedGenreTags(excludedGenres.asSequence().map { it.id }.toSet())
            .fskConstraints(fskConstraints)
            .tags(tags.asSequence().map { it.id }.toSet())
            .excludedTags(excludedTags.asSequence().map { it.id }.toSet())
            .tagRateFilter(tagRateFilter)
            .tagSpoilerFilter(tagSpoilerFilter)
            .type(type)

    var sortCriteria by Delegates.observable(sortCriteria) { _, old, new ->
        if (old != new) reload()
    }

    var type by Delegates.observable(type) { _, old, new ->
        if (old != new) {
            reload()

            if (
                (old.isAgeRestricted() && new.isAgeRestricted().not()) ||
                (old.isAgeRestricted().not() && new.isAgeRestricted())
            ) {
                loadTags()
            }
        }
    }

    val genreData = MutableLiveData<List<LocalTag>>()
    val tagData = MutableLiveData<List<LocalTag>>()

    private val tagDao by inject<TagDao>()

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
            .fromCallable { tagDao.getTags() }
            .flatMap { cachedTags ->
                when {
                    shouldUpdateTags() || cachedTags.isEmpty() -> tagSingle()
                        .map { remoteTags -> remoteTags.map { it.toParcelableTag() } }
                        .doOnSuccess {
                            tagDao.replaceTags(it)

                            storageHelper.lastTagUpdateDate = Date()
                        }
                    else -> Single.just(cachedTags)
                }
            }
            .map {
                val tagsToFilter = when (type) {
                    MediaType.HENTAI, MediaType.HMANGA -> enumSetOf(TagType.TAG, TagType.H_TAG)
                    else -> enumSetOf(TagType.TAG)
                }

                val genreTags = it.filter { tag -> tag.type == TagType.GENRE }
                val entryTags = it.filter { tag -> tag.type in tagsToFilter }

                TagContainer(genreTags, entryTags)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors { tags ->
                genreData.value = tags.genreTags
                tagData.value = tags.entryTags
            }
    }

    private fun tagSingle(): Single<List<Tag>> {
        return Single.zip(
            api.list().tagList().buildSingle(),
            api.list().tagList().type(TagType.H_TAG).buildSingle(),
            BiFunction { first: List<Tag>, second: List<Tag> -> first + second }
        )
    }

    private fun shouldUpdateTags() = storageHelper.lastTagUpdateDate.convertToDateTime()
        .isBefore(LocalDateTime.now().minusDays(15))

    private class TagContainer(val genreTags: List<LocalTag>, val entryTags: List<LocalTag>)
}
