package me.proxer.app.chat.prv.sync

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.Person
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.PrvMessengerActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.safeInject
import me.proxer.library.util.ProxerUrls
import org.koin.core.KoinComponent
import kotlin.math.min

/**
 * @author Ruben Gees
 */
object MessengerShortcuts : KoinComponent {

    private const val SHARE_TARGET_CATEGORY = "me.proxer.app.sharingshortcuts.category.TEXT_SHARE_TARGET"

    private val messengerDao by safeInject<MessengerDao>()

    fun updateShareTargets(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val componentName = ComponentName(context, MainActivity::class.java)

            val shortcutsLeft = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = requireNotNull(context.getSystemService<ShortcutManager>())
                val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
                val usedShortcuts = shortcutManager.dynamicShortcuts.plus(shortcutManager.manifestShortcuts)
                    .filterNot { it.categories == setOf(SHARE_TARGET_CATEGORY) }
                    .count { shortcutInfo -> shortcutInfo.activity == componentName }

                min(4, maxShortcuts - usedShortcuts)
            } else {
                2
            }

            val newShortcuts = messengerDao.getMostRecentConferences(shortcutsLeft).map {
                val icon = IconCompat.createWithBitmap(getConferenceIcon(context, it))
                val intent = PrvMessengerActivity.getIntent(context, it.id.toString()).setAction(Intent.ACTION_DEFAULT)

                ShortcutInfoCompat.Builder(context, it.id.toString())
                    .setShortLabel(it.topic)
                    .setIcon(icon)
                    .setIntent(intent)
                    .setCategories(setOf(SHARE_TARGET_CATEGORY))
                    .apply {
                        if (!it.isGroup) {
                            setPerson(
                                Person.Builder()
                                    .setName(it.topic)
                                    .setIcon(icon)
                                    .setUri(ProxerUrls.webBase.newBuilder("/messages?id=${it.id}").toString())
                                    .setKey(it.id.toString())
                                    .build()
                            )
                        }
                    }
                    .build()
            }

            val needsUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = requireNotNull(context.getSystemService<ShortcutManager>())

                newShortcuts.map { it.id } != shortcutManager.dynamicShortcuts.map { it.id }
            } else {
                true
            }

            if (needsUpdate) {
                ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                ShortcutManagerCompat.addDynamicShortcuts(context, newShortcuts)
            }
        }
    }

    private fun getConferenceIcon(context: Context, conference: LocalConference) = when {
        conference.image.isNotBlank() -> Utils.getCircleBitmapFromUrl(
            context, ProxerUrls.userImage(conference.image)
        )
        conference.isGroup -> BitmapFactory.decodeResource(context.resources, R.drawable.ic_shortcut_messenger_group)
        else -> BitmapFactory.decodeResource(context.resources, R.drawable.ic_shortcut_messenger_person)
    }
}
