package com.example.musicsplayer.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.Duration
import java.util.Locale.Category

data class Song (
	@SerializedName("id")           val id: Int,
	@SerializedName("category_id")  val categoryId: Int,
	@SerializedName("title")        val title: String,
	@SerializedName("duration")     val duration: String,
	@SerializedName("thumnail_url") val thumbnailUrl: String,
	@SerializedName("mp3")          val mp3: String,
	@SerializedName("licence")      val licence: String,
	) {
}