package me.proxer.app.util.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
class InstantJsonAdapter : JsonAdapter<Instant>() {
    override fun fromJson(reader: JsonReader): Instant? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> null
            else -> Instant.ofEpochMilli(reader.nextLong())
        }
    }

    override fun toJson(writer: JsonWriter, value: Instant?) {
        when (value) {
            null -> writer.nullValue()
            else -> writer.value(value.toEpochMilli())
        }
    }
}
