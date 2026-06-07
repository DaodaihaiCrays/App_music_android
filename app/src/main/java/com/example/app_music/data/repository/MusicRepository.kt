package com.example.app_music.data.repository

import android.content.Context
import com.example.app_music.data.source.FavoriteSongDatabaseHelper
import com.example.app_music.data.source.MediaStorageSource
import com.example.app_music.model.Song

class MusicRepository (context: Context){
    private val source= MediaStorageSource(context)
    private val favoriteDatabase = FavoriteSongDatabaseHelper(context)

    fun getSongs(): List<Song> =source.loadSongs()

    fun getFavoriteSongIds(): Set<Long> = favoriteDatabase.getFavoriteSongIds()

    fun addFavorite(songId: Long) {
        favoriteDatabase.addFavorite(songId)
    }

    fun removeFavorite(songId: Long) {
        favoriteDatabase.removeFavorite(songId)
    }
}
