package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.design.widget.Snackbar.LENGTH_LONG
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.activity.IndustryActivity
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.activity.TranslatorGroupActivity
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.util.*
import com.proxerme.app.util.extension.multilineSnackbar
import com.proxerme.app.util.extension.snackbar
import com.proxerme.library.connection.info.entity.Entry
import com.proxerme.library.connection.info.entity.EntryIndustry
import com.proxerme.library.connection.info.entity.EntrySeason
import com.proxerme.library.connection.info.entity.Synonym
import com.proxerme.library.connection.info.request.EntryRequest
import com.proxerme.library.connection.info.request.SetUserInfoRequest
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.SynonymTypeParameter
import com.proxerme.library.parameters.ViewStateParameter
import org.apmem.tools.layouts.FlowLayout

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaInfoFragment : SingleLoadingFragment<String, Entry>() {

    companion object {
        fun newInstance(): MediaInfoFragment {
            return MediaInfoFragment()
        }
    }

    private val userInfoSuccess = { _: Void? ->
        if (view != null) {
            snackbar(root, R.string.fragment_set_user_info_success)
        }
    }

    private val userInfoException = { exception: Exception ->
        if (view != null) {
            val action = ErrorUtils.handle(context as MainActivity, exception)

            multilineSnackbar(root,
                    getString(R.string.fragment_set_user_info_error, getString(action.message)),
                    LENGTH_LONG, action.buttonMessage, action.buttonAction)
        }
    }

    override val section = Section.MEDIA_INFO

    private val mediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = mediaActivity.id

    private var name: String?
        get() = mediaActivity.name
        set(value) {
            mediaActivity.name = value
        }

    private var category: String
        get() = mediaActivity.category
        set(value) {
            mediaActivity.category = value
        }

    private val userInfoTask = constructUserInfoTask()

    private var showUnratedTags = false
    private var showSpoilerTags = false

    private val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
    private val rating: RatingBar by bindView(R.id.rating)
    private val ratingAmount: TextView by bindView(R.id.ratingAmount)

    private val originalTitle: TextView by bindView(R.id.originalTitle)
    private val originalTitleRow: TableRow by bindView(R.id.originalTitleRow)
    private val englishTitle: TextView by bindView(R.id.englishTitle)
    private val englishTitleRow: TableRow by bindView(R.id.englishTitleRow)
    private val germanTitle: TextView by bindView(R.id.germanTitle)
    private val germanTitleRow: TableRow by bindView(R.id.germanTitleRow)
    private val japaneseTitle: TextView by bindView(R.id.japaneseTitle)
    private val japaneseTitleRow: TableRow by bindView(R.id.japaneseTitleRow)
    private val seasonStart: TextView by bindView(R.id.seasonStart)
    private val seasonEnd: TextView by bindView(R.id.seasonEnd)
    private val seasonsRow: TableRow by bindView(R.id.seasonsRow)
    private val status: TextView by bindView(R.id.status)
    private val license: TextView by bindView(R.id.license)

    private val genres: FlowLayout by bindView(R.id.genres)
    private val genresTitle: TextView by bindView(R.id.genresTitle)
    private val tags: FlowLayout by bindView(R.id.tags)
    private val tagsTitle: TextView by bindView(R.id.tagsTitle)
    private val unratedTagsButton: Button by bindView(R.id.unratedTagsButton)
    private val spoilerTagsButton: Button by bindView(R.id.spoilerTagsButton)
    private val fsk: FlowLayout by bindView(R.id.fsk)
    private val fskTitle: TextView  by bindView(R.id.fskTitle)
    private val groups: FlowLayout by bindView(R.id.groups)
    private val groupsTitle: TextView by bindView(R.id.groupsTitle)
    private val publishers: FlowLayout by bindView(R.id.publishers)
    private val publishersTitle: TextView by bindView(R.id.publishersTitle)

    private val noteContainer: ViewGroup by bindView(R.id.noteContainer)
    private val note: ImageView by bindView(R.id.note)
    private val favorContainer: ViewGroup by bindView(R.id.favorContainer)
    private val favor: ImageView by bindView(R.id.favor)
    private val finishContainer: ViewGroup by bindView(R.id.finishContainer)
    private val finish: ImageView by bindView(R.id.finish)

    private val description: TextView by bindView(R.id.description)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_info, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        note.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_clock)
                .sizeDp(24)
                .colorRes(R.color.icon))
        favor.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_star)
                .sizeDp(24)
                .colorRes(R.color.icon))
        finish.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_check)
                .sizeDp(24)
                .colorRes(R.color.icon))

        noteContainer.setOnClickListener {
            userInfoTask.execute(UserInfoInput(id, ViewStateParameter.WATCHLIST))
        }
        favorContainer.setOnClickListener {
            userInfoTask.execute(UserInfoInput(id, ViewStateParameter.FAVOURITE))
        }
        finishContainer.setOnClickListener {
            userInfoTask.execute(UserInfoInput(id, ViewStateParameter.FINISHED))
        }
    }

    override fun onDestroy() {
        userInfoTask.destroy()

        super.onDestroy()
    }

    override fun constructTask(): Task<String, Entry> {
        return ProxerLoadingTask(::EntryRequest)
    }

    override fun constructInput(): String {
        return id
    }

    override fun present(data: Entry) {
        name = data.name
        category = data.category

        if (data.rating > 0) {
            ratingContainer.visibility = View.VISIBLE
            rating.rating = data.rating / 2.0f
            ratingAmount.visibility = View.VISIBLE
            ratingAmount.text = getString(R.string.fragment_media_info_rate_count, data.rateCount)
        } else {
            ratingContainer.visibility = View.GONE
            ratingAmount.visibility = View.GONE
        }

        buildSynonymsView(data.synonyms)
        buildSeasonsView(data.seasons)

        status.text = ParameterMapper.mediaState(context, data.state)
        license.text = ParameterMapper.licence(context, data.license)

        buildBadgeView(genres, data.genres, { it }, { _, genre ->
            showPage(ProxerUrlHolder.getWikiUrl(genre))
        }, genresTitle)

        buildTagsView(data)
        buildFskView(data.fsk)

        buildBadgeView(groups, data.translatorGroups, { it.name }, { _, translatorGroup ->
            TranslatorGroupActivity.navigateTo(activity, translatorGroup.id, translatorGroup.name)
        }, groupsTitle)

        buildBadgeView(publishers, data.industries, { constructIndustryString(it) }, { _, industry ->
            IndustryActivity.navigateTo(activity, industry.id, industry.name)
        }, publishersTitle)

        description.text = data.description
    }

    private fun buildSynonymsView(synonyms: Array<Synonym>) {
        synonyms.forEach {
            when (it.type) {
                SynonymTypeParameter.ORIGINAL -> {
                    originalTitle.text = it.name
                    originalTitleRow.visibility = View.VISIBLE
                }

                SynonymTypeParameter.ENGLISH -> {
                    englishTitle.text = it.name
                    englishTitleRow.visibility = View.VISIBLE
                }

                SynonymTypeParameter.GERMAN -> {
                    germanTitle.text = it.name
                    germanTitleRow.visibility = View.VISIBLE
                }

                SynonymTypeParameter.JAPANESE -> {
                    japaneseTitle.text = it.name
                    japaneseTitleRow.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun buildSeasonsView(seasons: Array<EntrySeason>) {
        if (seasons.isNotEmpty()) {
            seasonStart.text = ParameterMapper.seasonStart(context, seasons[0])

            if (seasons.size >= 2) {
                seasonEnd.text = ParameterMapper.seasonEnd(context, seasons[1])
            } else {
                seasonEnd.visibility = View.GONE
            }
        } else {
            seasonsRow.visibility = View.GONE
        }
    }

    private fun buildTagsView(data: Entry) {
        if (data.tags.isEmpty()) {
            tagsTitle.visibility = View.GONE
            unratedTagsButton.visibility = View.GONE
            spoilerTagsButton.visibility = View.GONE
        } else {
            tagsTitle.visibility = View.VISIBLE

            updateUnratedButton()
            unratedTagsButton.setOnClickListener {
                showUnratedTags = !showUnratedTags
                buildTagsView(data)
            }

            updateSpoilerButton()
            spoilerTagsButton.setOnClickListener {
                showSpoilerTags = !showSpoilerTags

                buildTagsView(data)
            }
        }

        buildBadgeView(tags, data.tags.filter {
            if (it.isRated) {
                if (it.isSpoiler) {
                    showSpoilerTags
                } else {
                    true
                }
            } else {
                if (showUnratedTags) {
                    if (it.isSpoiler) {
                        showSpoilerTags
                    } else {
                        true
                    }
                } else {
                    false
                }
            }
        }.toTypedArray(), { it.name }, { _, tag ->
            multilineSnackbar(root, tag.description)
        })
    }

    private fun updateUnratedButton() {
        unratedTagsButton.text = getString(when (showUnratedTags) {
            true -> R.string.tags_unrated_hide
            false -> R.string.tags_unrated_show
        })
    }

    private fun updateSpoilerButton() {
        spoilerTagsButton.text = getString(when (showSpoilerTags) {
            true -> R.string.tags_spoiler_hide
            false -> R.string.tags_spoiler_show
        })
    }

    private fun buildFskView(fskEntries: Array<String>) {
        fsk.removeAllViews()

        if (fskEntries.isEmpty()) {
            fskTitle.visibility = View.GONE
            fsk.visibility = View.GONE
        } else {
            fskEntries.forEach { fskEntry ->
                val imageView = LayoutInflater.from(context).inflate(R.layout.item_badge,
                        fsk, false) as ImageView

                imageView.setImageDrawable(ParameterMapper.fskImage(context, fskEntry))
                imageView.setOnClickListener {
                    multilineSnackbar(root, ParameterMapper.fskDescription(context, fskEntry))
                }

                fsk.addView(imageView)
            }
        }
    }

    private fun <T> buildBadgeView(badgeContainer: ViewGroup, items: Array<T>,
                                   transform: (T) -> String, onClick: ((View, T) -> Unit)? = null,
                                   vararg viewsToHideIfEmpty: View) {
        badgeContainer.removeAllViews()

        if (items.isEmpty()) {
            badgeContainer.visibility = View.GONE
            viewsToHideIfEmpty.forEach {
                it.visibility = View.GONE
            }
        } else {
            badgeContainer.visibility = View.VISIBLE
            viewsToHideIfEmpty.forEach {
                it.visibility = View.VISIBLE
            }

            ViewUtils.populateBadgeView(badgeContainer, items, transform, onClick)
        }
    }

    private fun constructIndustryString(industry: EntryIndustry): String {
        return "${industry.name} (${industry.type.replace("_", " ").split(" ")
                .map(String::capitalize).joinToString(separator = " ")})"
    }

    private fun constructUserInfoTask(): Task<UserInfoInput, Void?> {
        return ValidatingTask(ProxerLoadingTask({
            SetUserInfoRequest(it.id, it.type)
        }), { Validators.validateLogin() }, userInfoSuccess, userInfoException)
    }

    private class UserInfoInput(val id: String, val type: String)
}
