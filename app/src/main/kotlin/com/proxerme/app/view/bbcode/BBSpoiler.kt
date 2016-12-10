package com.proxerme.app.view.bbcode

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.proxerme.app.R
import org.jetbrains.anko.find
import kotlin.properties.Delegates

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class BBSpoiler(val parent: ViewGroup) {

    var listener: ((isExpanded: Boolean) -> Unit)? = null

    val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_bbcode_spoiler, parent, false) as ViewGroup

    private val toggle: TextView
    private val decoration: ViewGroup
    private val container: ViewGroup

    var expanded by Delegates.observable(false, { property, old, new ->
        handleExpanded()

        if (old != new) {
            listener?.invoke(new)
        }
    })

    init {
        toggle = root.find(R.id.spoilerToggle)
        decoration = root.find(R.id.spoilerDecoration)
        container = root.find(R.id.spoilerContainer)

        toggle.setOnClickListener {
            expanded = !expanded
        }

        handleExpanded()
    }

    fun addViews(views: Iterable<View>) {
        views.forEach {
            container.addView(it)
        }
    }

    private fun handleExpanded() {
        if (expanded) {
            decoration.visibility = View.VISIBLE
            toggle.text = parent.context.getString(R.string.spoiler_hide)
        } else {
            decoration.visibility = View.GONE
            toggle.text = parent.context.getString(R.string.spoiler_show)
        }
    }
}