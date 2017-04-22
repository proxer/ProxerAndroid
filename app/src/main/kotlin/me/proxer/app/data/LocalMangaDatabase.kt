package me.proxer.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import me.proxer.app.entity.MangaChapterInfo
import me.proxer.library.entitiy.manga.Chapter
import me.proxer.library.entitiy.manga.Page
import me.proxer.library.enums.Language
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

        private const val CHAPTER_TABLE = "chapter"
        private const val PAGE_TABLE = "page"

        private const val CHAPTER_LOCAL_ID_COLUMN = "_id"
        private const val CHAPTER_ID_COLUMN = "id"
        private const val CHAPTER_EPISODE_COLUMN = "episode"
        private const val CHAPTER_LANGUAGE_COLUMN = "language"
        private const val CHAPTER_ENTRY_ID_COLUMN = "entry_id"
        private const val CHAPTER_ENTRY_NAME_COLUMN = "entry_name"
        private const val CHAPTER_TITLE_COLUMN = "title"
        private const val CHAPTER_UPLOADER_ID_COLUMN = "uploader_id"
        private const val CHAPTER_UPLOADER_NAME_COLUMN = "uploader_name"
        private const val CHAPTER_DATE_COLUMN = "date"
        private const val CHAPTER_SCAN_GROUP_ID_COLUMN = "scan_group_id"
        private const val CHAPTER_SCAN_GROUP_NAME_COLUMN = "scan_group_name"
        private const val CHAPTER_SERVER_COLUMN = "server"
        private const val CHAPTER_EPISODE_AMOUNT_COLUMN = "episode_amount"

        private const val PAGE_LOCAL_ID_COLUMN = "_id"
        private const val PAGE_NAME_COLUMN = "name"
        private const val PAGE_HEIGHT_COLUMN = "height"
        private const val PAGE_WIDTH_COLUMN = "width"
        private const val PAGE_CHAPTER_ID_COLUMN = "chapter_id"

        private val chapterParser = rowParser { localId: Long, id: String, episode: Int, language: String,
                                                entryId: String, entryName: String, title: String, uploaderId: String,
                                                uploaderName: String, date: Long, scanGroupId: String?,
                                                scanGroupName: String?, server: String, episodeAmount: Int ->

            val parsedLanguage = ProxerUtils.toApiEnum(Language::class.java, language) ?:
                    throw IllegalArgumentException("Unknown value for language: $language")

            IntermediateChapter(localId, id, episode, parsedLanguage, entryId, entryName, title, uploaderId,
                    uploaderName, Date(date), scanGroupId, scanGroupName, server, episodeAmount)
        }

        private val pageParser = rowParser { _: Long, name: String, height: Int, width: Int, _: Long ->
            Page(name, height, width)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(CHAPTER_TABLE, true,
                CHAPTER_LOCAL_ID_COLUMN to INTEGER + PRIMARY_KEY + UNIQUE + NOT_NULL,
                CHAPTER_ID_COLUMN to TEXT + NOT_NULL,
                CHAPTER_EPISODE_COLUMN to INTEGER + NOT_NULL,
                CHAPTER_LANGUAGE_COLUMN to TEXT + NOT_NULL,
                CHAPTER_ENTRY_ID_COLUMN to TEXT + NOT_NULL,
                CHAPTER_ENTRY_NAME_COLUMN to TEXT + NOT_NULL,
                CHAPTER_TITLE_COLUMN to TEXT + NOT_NULL,
                CHAPTER_UPLOADER_ID_COLUMN to TEXT + NOT_NULL,
                CHAPTER_UPLOADER_NAME_COLUMN to TEXT + NOT_NULL,
                CHAPTER_DATE_COLUMN to INTEGER + NOT_NULL,
                CHAPTER_SCAN_GROUP_ID_COLUMN to TEXT,
                CHAPTER_SCAN_GROUP_NAME_COLUMN to TEXT,
                CHAPTER_SERVER_COLUMN to TEXT + NOT_NULL,
                CHAPTER_EPISODE_AMOUNT_COLUMN to INTEGER + NOT_NULL)

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

    fun insert(chapterInfo: MangaChapterInfo, episode: Int, language: Language) {
        use {
            if (find(chapterInfo.chapter.entryId, episode, language) == null) {
                transaction {
                    val id = insertOrThrow(CHAPTER_TABLE,
                            CHAPTER_ID_COLUMN to chapterInfo.chapter.id,
                            CHAPTER_EPISODE_COLUMN to episode,
                            CHAPTER_LANGUAGE_COLUMN to ProxerUtils.getApiEnumName(language),
                            CHAPTER_ENTRY_ID_COLUMN to chapterInfo.chapter.entryId,
                            CHAPTER_ENTRY_NAME_COLUMN to chapterInfo.name,
                            CHAPTER_TITLE_COLUMN to chapterInfo.chapter.title,
                            CHAPTER_UPLOADER_ID_COLUMN to chapterInfo.chapter.uploaderId,
                            CHAPTER_UPLOADER_NAME_COLUMN to chapterInfo.chapter.uploaderName,
                            CHAPTER_DATE_COLUMN to chapterInfo.chapter.date.time,
                            CHAPTER_SCAN_GROUP_ID_COLUMN to chapterInfo.chapter.scanGroupId,
                            CHAPTER_SCAN_GROUP_NAME_COLUMN to chapterInfo.chapter.scanGroupName,
                            CHAPTER_SERVER_COLUMN to chapterInfo.chapter.server,
                            CHAPTER_EPISODE_AMOUNT_COLUMN to chapterInfo.episodeAmount)

                    chapterInfo.chapter.pages.forEach {
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

    fun find(entryId: String, episode: Int, language: Language): MangaChapterInfo? {
        return use {
            val chapter = select(CHAPTER_TABLE)
                    .where("$CHAPTER_ENTRY_ID_COLUMN = \"$entryId\" and $CHAPTER_EPISODE_COLUMN = $episode " +
                            "and $CHAPTER_LANGUAGE_COLUMN = \"${ProxerUtils.getApiEnumName(language)}\"")
                    .parseOpt(chapterParser)

            if (chapter != null) {
                val pages = select(PAGE_TABLE)
                        .where("$PAGE_CHAPTER_ID_COLUMN = ${chapter.localId}")
                        .parseList(pageParser)

                MangaChapterInfo(chapter.toNonLocalChapter(pages), chapter.entryName, chapter.episodeAmount)
            } else {
                null
            }
        }
    }

    private data class IntermediateChapter(val localId: Long, val id: String, val episode: Int, val language: Language,
                                           val entryId: String, val entryName: String, val title: String,
                                           val uploaderId: String, val uploaderName: String, val date: Date,
                                           val scanGroupId: String?, val scanGroupName: String?, val server: String,
                                           val episodeAmount: Int) {

        fun toNonLocalChapter(pages: List<Page>): Chapter {
            return Chapter(id, entryId, title, uploaderId, uploaderName, date, scanGroupId, scanGroupName,
                    server, pages)
        }
    }
}