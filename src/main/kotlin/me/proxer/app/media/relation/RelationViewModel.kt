package me.proxer.app.media.relation

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Relation

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class RelationViewModel(private val entryId: String) : BaseContentViewModel<List<Relation>>() {

    override val endpoint: Endpoint<List<Relation>>
        get() = api.info().relations(entryId)
                .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(globalContext)
                        && StorageHelper.user != null)

    override val dataSingle: Single<List<Relation>>
        get() = super.dataSingle
                .map { it.filterNot { it.id == entryId }.sortedByDescending { it.clicks } }
}
