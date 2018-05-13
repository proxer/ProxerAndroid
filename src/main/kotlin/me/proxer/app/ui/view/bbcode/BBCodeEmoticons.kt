package me.proxer.app.ui.view.bbcode

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ImageSpan
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.load.resource.gif.GifDrawable.LOOP_FOREVER
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import org.jetbrains.anko.dip
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
object BBCodeEmoticons {

    private val emoticons = listOf(
        BBCodeEmoticon("*o-angst*", R.drawable.o_angst),
        BBCodeEmoticon("*o-aua*", R.drawable.o_aua),
        BBCodeEmoticon("*o-blush*", R.drawable.o_blush),
        BBCodeEmoticon("*o-brav*", R.drawable.o_brav),
        BBCodeEmoticon("*o-büde*", R.drawable.o_buede),
        BBCodeEmoticon("*o-bye*", R.drawable.o_bye),
        BBCodeEmoticon("*o-8)*", R.drawable.o_cool),
        BBCodeEmoticon("*o-cryrun*", R.drawable.o_cryrun),
        BBCodeEmoticon("*o-DX*", R.drawable.o_dx1),
        BBCodeEmoticon("*o-Dx*", R.drawable.o_dx2),
        BBCodeEmoticon("*o-emo*", R.drawable.o_emo),
        BBCodeEmoticon("*o-ero1*", R.drawable.o_ero1),
        BBCodeEmoticon("*o-ero2*", R.drawable.o_ero2),
        BBCodeEmoticon("*o-fuu*", R.drawable.o_fuu),
        BBCodeEmoticon("*o-gj*", R.drawable.o_gj),
        BBCodeEmoticon("*o-gomen*", R.drawable.o_gomen),
        BBCodeEmoticon("*o-haha*", R.drawable.o_haha),
        BBCodeEmoticon("*o-hero*", R.drawable.o_hero),
        BBCodeEmoticon("*o-heul*", R.drawable.o_heul),
        BBCodeEmoticon("*o-hi*", R.drawable.o_hi),
        BBCodeEmoticon("*o-kihihi*", R.drawable.o_kihihi),
        BBCodeEmoticon("*o-kiss*", R.drawable.o_kiss),
        BBCodeEmoticon("*o-like*", R.drawable.o_like),
        BBCodeEmoticon("*o-love*", R.drawable.o_love1),
        BBCodeEmoticon("*o-love2*", R.drawable.o_love2),
        BBCodeEmoticon("*o-lw*", R.drawable.o_lw),
        BBCodeEmoticon("*o-noooo*", R.drawable.o_noooo1),
        BBCodeEmoticon("*o-noooo2*", R.drawable.o_noooo2),
        BBCodeEmoticon("*o-öhm*", R.drawable.o_oehm),
        BBCodeEmoticon("*o-omg*", R.drawable.o_omg),
        BBCodeEmoticon("*o-pfeif*", R.drawable.o_pfeif),
        BBCodeEmoticon("*o-puh*", R.drawable.o_puh),
        BBCodeEmoticon("*o-pwn*", R.drawable.o_pwn),
        BBCodeEmoticon("*o-roll*", R.drawable.o_roll),
        BBCodeEmoticon("*o-run*", R.drawable.o_run),
        BBCodeEmoticon("*o-sayajin*", R.drawable.o_sayajin),
        BBCodeEmoticon("*o-schleich*", R.drawable.o_schleich),
        BBCodeEmoticon("*o-schlürf*", R.drawable.o_schluerf),
        BBCodeEmoticon("*o-schrei*", R.drawable.o_schrei),
        BBCodeEmoticon("*o-stop*", R.drawable.o_stop1),
        BBCodeEmoticon("*o-stop!*", R.drawable.o_stop2),
        BBCodeEmoticon("*o-stress*", R.drawable.o_stress),
        BBCodeEmoticon("*o-toohappy*", R.drawable.o_toohappy),
        BBCodeEmoticon("*o-verwirr*", R.drawable.o_verwirr),
        BBCodeEmoticon("*o-wahaha*", R.drawable.o_wahaha),
        BBCodeEmoticon("*-___-*", R.drawable.o_wet),
        BBCodeEmoticon("*o-???*", R.drawable.o_what),
        BBCodeEmoticon("*o-XD*", R.drawable.o_xd),
        BBCodeEmoticon("*o-yay*", R.drawable.o_yay)
    )

    private val emoticonRegex = Regex(emoticons.joinToString(separator = "|") { quote(it.pattern) })

    fun replaceWithGifs(view: BetterLinkGifAwareEmojiTextView, glide: GlideRequests) {
        val text = view.text.toSpannableStringBuilder()
        val foundEmoticons = emoticonRegex.findAll(text).toList()

        foundEmoticons.forEach { emoticon ->
            val id = emoticons.find { it.pattern == emoticon.value }?.id
            val spanStart = emoticon.range.start
            val spanEnd = emoticon.range.endInclusive + 1

            glide.asGif()
                .load(id)
                .into(object : SimpleTarget<GifDrawable>() {
                    override fun onResourceReady(resource: GifDrawable, transition: Transition<in GifDrawable>?) {
                        resource.callback = view

                        resource.setBounds(0, 0, view.context.dip(24), view.context.dip(24))
                        resource.setLoopCount(LOOP_FOREVER)
                        resource.start()

                        view.text = text.apply {
                            setSpan(ImageSpan(resource), spanStart, spanEnd, SPAN_INCLUSIVE_EXCLUSIVE)
                        }
                    }
                })
        }
    }

    private data class BBCodeEmoticon(val pattern: String, val id: Int)
}
