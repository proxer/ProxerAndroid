package com.proxerme.app.data

import com.proxerme.library.connection.messenger.entity.Conference
import org.jetbrains.anko.db.RowParser

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferenceParser : RowParser<Conference> {

    override fun parseRow(columns: Array<Any?>): Conference {
        return Conference(columns[0].toString(), columns[1].toString(), columns[2].toString(),
                (columns[3] as Long).toInt(), columns[4].toString(), columns[5].toString(),
                columns[6] == 1L, columns[7] == 1L, columns[8] as Long,
                (columns[9] as Long).toInt(), columns[10].toString())
    }
}