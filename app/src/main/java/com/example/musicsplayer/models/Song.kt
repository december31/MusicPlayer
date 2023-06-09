package com.example.musicsplayer.models

import com.google.gson.annotations.SerializedName

data class Song (
	@SerializedName("id")           val id: Int,
	@SerializedName("category_id")  val categoryId: Int,
	@SerializedName("title")        val title: String,
	@SerializedName("duration")     val duration: String,
	@SerializedName("thumnail_url") val thumbnailUrl: String,
	@SerializedName("mp3")          val mp3Url: String,
	@SerializedName("licence")      val licence: String,
	@SerializedName("mp3_file_name") var mp3FileName: String?,
	@SerializedName("thumbnail_file_name") var thumbnailFileName: String?,
	) {
}