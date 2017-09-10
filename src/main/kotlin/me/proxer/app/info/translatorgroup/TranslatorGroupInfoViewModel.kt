package me.proxer.app.info.translatorgroup

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.TranslatorGroup

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class TranslatorGroupInfoViewModel(private val translatorGroupId: String) : BaseContentViewModel<TranslatorGroup>() {

    override val endpoint: Endpoint<TranslatorGroup>
        get() = api.info().translatorGroup(translatorGroupId)
}
