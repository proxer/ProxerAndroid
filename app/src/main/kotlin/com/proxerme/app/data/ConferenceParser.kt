package com.proxerme.app.data

import com.proxerme.app.entitiy.LocalConference
import org.jetbrains.anko.db.RowParser

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferenceParser : RowParser<LocalConference> {

    override fun parseRow(columns: Array<Any?>): LocalConference {
        return LocalConference(columns[0] as Long, columns[1].toString(), columns[2].toString(),
                columns[3].toString(), (columns[4] as Long).toInt(), columns[5].toString(),
                columns[6].toString(), columns[7] == 1L, columns[8] == 1L, columns[9] == 1L,
                columns[10] as Long, (columns[11] as Long).toInt(), columns[12].toString())
    }
}