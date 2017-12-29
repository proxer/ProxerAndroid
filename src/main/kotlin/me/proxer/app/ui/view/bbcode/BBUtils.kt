package me.proxer.app.ui.view.bbcode

import android.view.View
import org.jetbrains.anko.childrenRecursiveSequence

inline fun <reified T> applyToViews(views: List<View>, operation: (T) -> Unit): List<View> {
    return views.map { view ->
        if (view is T) {
            operation(view)
        } else {
            view.childrenRecursiveSequence().plus(view)
                    .filterIsInstance(T::class.java)
                    .forEach(operation)
        }

        view
    }
}
