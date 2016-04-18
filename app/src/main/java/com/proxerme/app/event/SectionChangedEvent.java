package com.proxerme.app.event;

import com.proxerme.app.util.Section;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class SectionChangedEvent {

    @Section.SectionId
    private int newSection;

    public SectionChangedEvent(@Section.SectionId int newSection) {
        this.newSection = newSection;
    }

    @Section.SectionId
    public int getNewSection() {
        return newSection;
    }
}
