package com.example.musicsplayer.api

import com.example.musicsplayer.models.Song
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface SongServices {
	@GET("rings")
	fun getSongsDetails(): Call<List<Song>>

	@Streaming
	@GET
	fun getMp3s(@Url fileUrl: String): Call<ResponseBody>

	@Streaming
	@GET
	fun getThumbnail(@Url fileUrl: String): Call<ResponseBody>
}