package me.proxer.app.info.translatorgroup

import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.TranslatorGroup

/**
 * @author Ruben Gees
 */
class TranslatorGroupInfoViewModel(private val translatorGroupId: String) : BaseContentViewModel<TranslatorGroup>() {

    override val endpoint: Endpoint<TranslatorGroup>
        get() = api.info().translatorGroup(translatorGroupId)
}
