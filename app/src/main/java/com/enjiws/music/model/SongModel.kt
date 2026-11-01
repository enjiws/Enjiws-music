package com.enjiws.music.model

data class SongModel(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String,
    val lyricPath: String? = null
)
