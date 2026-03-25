package com.example.backend

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val trend: String,
    val country: String?,
    val videoUrl: String? = null
)
