package com.proxerme.app.manager

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object SectionManager {
    enum class Section {
        NONE, NEWS, PROFILE, TOPTEN, CONFERENCES, CHAT, USER_MEDIA_LIST, MEDIA_LIST,
        CONFERENCE_INFO, NEW_CHAT, HISTORY, UCP_OVERVIEW, MEDIA_INFO, REMINDER, COMMENTS, RELATIONS,
        EPISODES, MANGA, ANIME, TRANSLATOR_GROUP_INFO, INDUSTRY_INFO, TRANSLATOR_GROUP_PROJECTS,
        INDUSTRY_PROJECTS
    }

    private val resumedSections = ArrayList<Section>()

    fun notifySectionResumed(section: Section) = resumedSections.add(section)
    fun notifySectionPaused(section: Section) = resumedSections.remove(section)
    fun isSectionResumed(section: Section) = resumedSections.contains(section)
}