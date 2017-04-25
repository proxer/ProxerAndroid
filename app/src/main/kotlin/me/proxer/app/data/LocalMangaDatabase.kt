package me.proxer.app.data

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import me.proxer.app.entity.LocalMangaChapter
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.entitiy.manga.Chapter
import me.proxer.library.entitiy.manga.Page
import me.proxer.library.enums.*
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.db.*
import java.util.*

/**
 * @author Ruben Gees
 */
class LocalMangaDatabase(context: Context) : ManagedSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "manga.db"
        private const val DATABASE_VERSION = 1

        private const val ENTRY_TABLE = "entry"
        private const val CHAPTER_TABLE = "chapter"
        private const val PAGE_TABLE = "page"

        private const val ENTRY_ID_COLUMN = "id"
        private const val ENTRY_NAME_COLUMN = "name"
        private const val ENTRY_EPISODE_AMOUNT_COLUMN = "episode_amount"

        private const val CHAPTER_LOCAL_ID_COLUMN = "_id"
        private const val CHAPTER_ID_COLUMN = "id"
        private const val CHAPTER_EPISODE_COLUMN = "episode"
        private const val CHAPTER_LANGUAGE_COLUMN = "language"
        private const val CHAPTER_ENTRY_ID_COLUMN = "entry_id"
        private const val CHAPTER_TITLE_COLUMN = "title"
        private const val CHAPTER_UPLOADER_ID_COLUMN = "uploader_id"
        private const val CHAPTER_UPLOADER_NAME_COLUMN = "uploader_name"
        private const val CHAPTER_DATE_COLUMN = "date"
        private const val CHAPTER_SCAN_GROUP_ID_COLUMN = "scan_group_id"
        private const val CHAPTER_SCAN_GROUP_NAME_COLUMN = "scan_group_name"
        private const val CHAPTER_SERVER_COLUMN = "server"

        private const val PAGE_LOCAL_ID_COLUMN = "_id"
        private const val PAGE_NAME_COLUMN = "name"
        private const val PAGE_HEIGHT_COLUMN = "height"
        private const val PAGE_WIDTH_COLUMN = "width"
        private const val PAGE_CHAPTER_ID_COLUMN = "chapter_id"

        private val entryParser = rowParser { id: String, name: String, episodeAmount: Int ->
            EntryCore(id, name, emptySet(), emptySet(), "", Medium.ANIMESERIES, episodeAmount, MediaState.AIRING,
                    0, 0, 0, Category.ANIME, License.UNKNOWN)
        }

        private val chapterParser = rowParser { localId: Long, id: String, episode: Int, language: String,
                                                entryId: String, title: String, uploaderId: String,
                                                uploaderName: String, date: Long, scanGroupId: String?,
                                                scanGroupName: String?, server: String ->

            val parsedLanguage = ProxerUtils.toApiEnum(Language::class.java, language) ?:
                    throw IllegalArgumentException("Unknown value for language: $language")

            LocalMangaChapter(localId, id, episode, parsedLanguage, entryId, title, uploaderId, uploaderName, Date(date),
                    scanGroupId, scanGroupName, server)
        }

        private val pageParser = rowParser { _: Long, name: String, height: Int, width: Int, _: Long ->
            Page(name, height, width)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(ENTRY_TABLE, true,
                ENTRY_ID_COLUMN to TEXT + PRIMARY_KEY + UNIQUE + NOT_NULL,
                ENTRY_NAME_COLUMN to TEXT + NOT_NULL,
                ENTRY_EPISODE_AMOUNT_COLUMN to INTEGER + NOT_NULL)

        db.createTable(CHAPTER_TABLE, true,
                CHAPTER_LOCAL_ID_COLUMN to INTEGER + PRIMARY_KEY + UNIQUE + NOT_NULL,
                CHAPTER_ID_COLUMN to TEXT + NOT_NULL,
                CHAPTER_EPISODE_COLUMN to INTEGER + NOT_NULL,
                CHAPTER_LANGUAGE_COLUMN to TEXT + NOT_NULL,
                CHAPTER_ENTRY_ID_COLUMN to TEXT + NOT_NULL,
                CHAPTER_TITLE_COLUMN to TEXT + NOT_NULL,
                CHAPTER_UPLOADER_ID_COLUMN to TEXT + NOT_NULL,
                CHAPTER_UPLOADER_NAME_COLUMN to TEXT + NOT_NULL,
                CHAPTER_DATE_COLUMN to INTEGER + NOT_NULL,
                CHAPTER_SCAN_GROUP_ID_COLUMN to TEXT,
                CHAPTER_SCAN_GROUP_NAME_COLUMN to TEXT,
                CHAPTER_SERVER_COLUMN to TEXT + NOT_NULL,
                FOREIGN_KEY(CHAPTER_ENTRY_ID_COLUMN, ENTRY_TABLE, ENTRY_ID_COLUMN))

        db.createTable(PAGE_TABLE, true,
                PAGE_LOCAL_ID_COLUMN to INTEGER + PRIMARY_KEY + AUTOINCREMENT + NOT_NULL,
                PAGE_NAME_COLUMN to TEXT + NOT_NULL,
                PAGE_HEIGHT_COLUMN to INTEGER + NOT_NULL,
                PAGE_WIDTH_COLUMN to INTEGER + NOT_NULL,
                PAGE_CHAPTER_ID_COLUMN to INTEGER,
                FOREIGN_KEY(PAGE_CHAPTER_ID_COLUMN, CHAPTER_TABLE, CHAPTER_LOCAL_ID_COLUMN))
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Not needed yet.
    }

    fun insertEntry(entry: EntryCore) {
        use {
            if (!containsEntry(entry.id)) {
                insertOrThrow(ENTRY_TABLE,
                        ENTRY_ID_COLUMN to entry.id,
                        ENTRY_NAME_COLUMN to entry.name,
                        ENTRY_EPISODE_AMOUNT_COLUMN to entry.episodeAmount)
            }
        }
    }

    fun insertChapter(chapter: Chapter, episode: Int, language: Language) {
        use {
            val existingChapterCount = DatabaseUtils.queryNumEntries(this, CHAPTER_TABLE,
                    "$CHAPTER_ID_COLUMN = \"${chapter.id}\" " +
                            "and $CHAPTER_EPISODE_COLUMN = $episode " +
                            "and $CHAPTER_LANGUAGE_COLUMN = \"${ProxerUtils.getApiEnumName(language)}\"")

            if (existingChapterCount <= 0) {
                transaction {
                    val id = insertOrThrow(CHAPTER_TABLE,
                            CHAPTER_ID_COLUMN to chapter.id,
                            CHAPTER_EPISODE_COLUMN to episode,
                            CHAPTER_LANGUAGE_COLUMN to ProxerUtils.getApiEnumName(language),
                            CHAPTER_ENTRY_ID_COLUMN to chapter.entryId,
                            CHAPTER_TITLE_COLUMN to chapter.title,
                            CHAPTER_UPLOADER_ID_COLUMN to chapter.uploaderId,
                            CHAPTER_UPLOADER_NAME_COLUMN to chapter.uploaderName,
                            CHAPTER_DATE_COLUMN to chapter.date.time,
                            CHAPTER_SCAN_GROUP_ID_COLUMN to chapter.scanGroupId,
                            CHAPTER_SCAN_GROUP_NAME_COLUMN to chapter.scanGroupName,
                            CHAPTER_SERVER_COLUMN to chapter.server)

                    chapter.pages.forEach {
                        replaceOrThrow(PAGE_TABLE,
                                PAGE_NAME_COLUMN to it.name,
                                PAGE_WIDTH_COLUMN to it.width,
                                PAGE_HEIGHT_COLUMN to it.height,
                                PAGE_CHAPTER_ID_COLUMN to id)
                    }
                }
            }
        }
    }

    fun findEntry(id: String): EntryCore? {
        return use {
            select(ENTRY_TABLE)
                    .where("$ENTRY_ID_COLUMN = \"$id\"")
                    .parseOpt(entryParser)
        }
    }

    fun findChapter(entryId: String, episode: Int, language: Language): Chapter? {
        return use {
            val chapter = select(CHAPTER_TABLE)
                    .where("$CHAPTER_ENTRY_ID_COLUMN = \"$entryId\" and $CHAPTER_EPISODE_COLUMN = $episode " +
                            "and $CHAPTER_LANGUAGE_COLUMN = \"${ProxerUtils.getApiEnumName(language)}\"")
                    .parseOpt(chapterParser)

            chapter?.toNonLocalChapter(findPagesForChapter(chapter))
        }
    }

    fun getAll(): List<CompleteLocalMangaEntry> {
        return use {
            val entries = select(ENTRY_TABLE)
                    .parseList(entryParser)
                    .reversed()

            entries.associate {
                it to select(CHAPTER_TABLE)
                        .where("$CHAPTER_ENTRY_ID_COLUMN = \"${it.id}\"")
                        .parseList(chapterParser)
                        .sortedBy { it.episode }
            }.filterNot { it.value.isEmpty() }.toList()
        }
    }

    fun containsEntry(id: String): Boolean {
        return use {
            DatabaseUtils.queryNumEntries(this, ENTRY_TABLE, "$ENTRY_ID_COLUMN = \"$id\"") > 0
        }
    }

    fun containsChapter(entryId: String, episode: Int, language: Language): Boolean {
        return use {
            DatabaseUtils.queryNumEntries(this, CHAPTER_TABLE,
                    "$CHAPTER_ENTRY_ID_COLUMN = \"$entryId\" " +
                            "and $CHAPTER_EPISODE_COLUMN = $episode " +
                            "and $CHAPTER_LANGUAGE_COLUMN = \"${ProxerUtils.getApiEnumName(language)}\"") > 0
        }
    }

    fun removeChapter(entry: EntryCore, chapter: LocalMangaChapter) {
        use {
            transaction {
                delete(CHAPTER_TABLE, "$CHAPTER_LOCAL_ID_COLUMN = ${chapter.localId}")
                delete(PAGE_TABLE, "$PAGE_CHAPTER_ID_COLUMN = ${chapter.id}")

                if (DatabaseUtils.queryNumEntries(this, CHAPTER_TABLE,
                        "$CHAPTER_ENTRY_ID_COLUMN = \"${entry.id}\"") <= 0) {
                    delete(ENTRY_TABLE, "$ENTRY_ID_COLUMN = \"${entry.id}\"")
                }
            }
        }
    }

    fun clear() {
        use {
            transaction {
                delete(ENTRY_TABLE)
                delete(CHAPTER_TABLE)
                delete(PAGE_TABLE)
            }
        }
    }

    private fun findPagesForChapter(chapter: LocalMangaChapter): List<Page> {
        return use {
            select(PAGE_TABLE)
                    .where("$PAGE_CHAPTER_ID_COLUMN = ${chapter.localId}")
                    .parseList(pageParser)
        }
    }
}