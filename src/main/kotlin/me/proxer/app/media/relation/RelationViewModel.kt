package me.proxer.app.media.relation

import android.app.Application
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Relation
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class RelationViewModel(application: Application) : BaseContentViewModel<List<Relation>>(application) {

    override val endpoint: Endpoint<List<Relation>>
        get() = api.info().relations(entryId)
                .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(globalContext)
                        && StorageHelper.user != null)

    override val dataSingle: Single<List<Relation>>
        get() = super.dataSingle
                .map { it.filterNot { it.id == entryId }.sortedByDescending { it.clicks } }

    var entryId by Delegates.notNull<String>()
}
