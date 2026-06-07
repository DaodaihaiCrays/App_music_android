package com.example.app_music.model

import android.net.Uri

data class Song(
    val id:Long,
    val title:String,
    val artist:String,
    val albumArt: Uri?,
    val uri:Uri
)