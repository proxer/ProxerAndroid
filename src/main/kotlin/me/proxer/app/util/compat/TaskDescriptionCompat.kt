@file:Suppress("DEPRECATION")

package me.proxer.app.util.compat

import android.app.Activity
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.ColorInt
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
object TaskDescriptionCompat {

    fun setTaskDescription(activity: Activity, @ColorInt primaryColor: Int) {
        val activityInfo = activity.packageManager.getActivityInfo(
            activity.componentName,
            PackageManager.GET_META_DATA
        )

        val taskDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager.TaskDescription(
                activity.getString(R.string.app_name),
                activityInfo.icon,
                primaryColor
            )
        } else {
            ActivityManager.TaskDescription(
                activity.getString(R.string.app_name),
                BitmapFactory.decodeResource(activity.resources, activityInfo.icon),
                primaryColor
            )
        }

        activity.setTaskDescription(taskDescription)
    }
}
