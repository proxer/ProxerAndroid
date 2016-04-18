package com.proxerme.app.util;

import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class Section {

    public static final int NONE = -1;
    public static final int NEWS = 0;
    public static final int CONFERENCES = 1;
    public static final int MESSAGES = 2;
    public static final int SETTINGS = 100;

    private Section() {

    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NONE, NEWS, CONFERENCES, MESSAGES, SETTINGS})
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    public @interface SectionId {

    }

}
