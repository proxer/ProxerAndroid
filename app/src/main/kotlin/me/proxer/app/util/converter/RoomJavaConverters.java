package me.proxer.app.util.converter;

import android.arch.persistence.room.TypeConverter;

import java.util.Set;

import me.proxer.library.enums.FskConstraint;
import me.proxer.library.enums.Genre;

/**
 * Class to fix Java/Kotlin interoperability issues.
 *
 * @author Ruben Gees
 */
public final class RoomJavaConverters {

    @TypeConverter
    public String fromGenres(Set<Genre> value) {
        return RoomConverters.Companion.fromGenres(value);
    }

    @TypeConverter
    public Set<Genre> toGenres(String value) {
        return RoomConverters.Companion.toGenres(value);
    }

    @TypeConverter
    public String fromFskConstraints(Set<FskConstraint> value) {
        return RoomConverters.Companion.fromFskConstraints(value);
    }

    @TypeConverter
    public Set<FskConstraint> toFskConstraints(String value) {
        return RoomConverters.Companion.toFskConstraints(value);
    }
}
