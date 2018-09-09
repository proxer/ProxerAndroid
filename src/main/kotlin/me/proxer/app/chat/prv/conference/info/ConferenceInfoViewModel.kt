package me.proxer.app.chat.prv.conference.info

import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.messenger.ConferenceInfo

/**
 * @author Ruben Gees
 */
class ConferenceInfoViewModel(private val conferenceId: String) : BaseContentViewModel<ConferenceInfo>() {

    override val endpoint: Endpoint<ConferenceInfo>
        get() = api.messenger().conferenceInfo(conferenceId)
}
