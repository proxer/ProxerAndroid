package me.proxer.app.util.data

import com.orhanobut.hawk.Parser
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

/**
 * @author Ruben Gees
 */
class HawkMoshiParser(private val moshi: Moshi) : Parser {
    override fun <T : Any?> fromJson(content: String, type: Type): T? {
        return moshi.adapter<T>(type).fromJson(content)
    }

    override fun toJson(body: Any): String? {
        return moshi.adapter(body.javaClass).toJson(body)
    }
}
