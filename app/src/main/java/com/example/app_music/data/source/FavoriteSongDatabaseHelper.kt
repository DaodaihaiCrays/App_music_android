package com.example.app_music.data.source

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FavoriteSongDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVORITES (
                $COLUMN_SONG_ID INTEGER PRIMARY KEY
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        onCreate(db)
    }

    fun getFavoriteSongIds(): Set<Long> {
        val favoriteIds = mutableSetOf<Long>()
        readableDatabase.query(
            TABLE_FAVORITES,
            arrayOf(COLUMN_SONG_ID),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            val songIdColumn = cursor.getColumnIndexOrThrow(COLUMN_SONG_ID)
            while (cursor.moveToNext()) {
                favoriteIds.add(cursor.getLong(songIdColumn))
            }
        }
        return favoriteIds
    }

    fun addFavorite(songId: Long) {
        val values = ContentValues().apply {
            put(COLUMN_SONG_ID, songId)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_FAVORITES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun removeFavorite(songId: Long) {
        writableDatabase.delete(
            TABLE_FAVORITES,
            "$COLUMN_SONG_ID = ?",
            arrayOf(songId.toString())
        )
    }

    companion object {
        private const val DATABASE_NAME = "music.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_FAVORITES = "favorite_songs"
        private const val COLUMN_SONG_ID = "song_id"
    }
}
