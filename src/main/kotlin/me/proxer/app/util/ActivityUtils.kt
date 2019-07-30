package me.proxer.app.util

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat

/**
 * @author Ruben Gees
 */
object ActivityUtils {

    private const val TRANSITION_NAME_EXTRA = "transition_name"

    fun navigateToWithImageTransition(intent: Intent, context: Activity, imageView: ImageView?) {
        if (imageView?.drawable != null) {
            intent.putExtra(TRANSITION_NAME_EXTRA, imageView.transitionName)

            context.startActivity(
                intent,
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    context,
                    imageView,
                    imageView.transitionName
                ).toBundle()
            )
        } else {
            context.startActivity(intent)
        }
    }

    fun getTransitionName(context: Activity): String? = context.intent.getStringExtra(TRANSITION_NAME_EXTRA)
}
