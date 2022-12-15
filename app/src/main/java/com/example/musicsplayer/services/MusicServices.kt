package com.example.musicsplayer.services

import com.example.musicsplayer.models.Song
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL

class MusicServices {

	private val api = "https://api.dev.bkplus.tech/ringtone/rings"

	fun getSongs(): List<Song>? {
		try {
			val url = URL(api)
			val connection = url.openConnection() as HttpURLConnection
			connection.requestMethod = "GET"

			val reader = BufferedReader(InputStreamReader(connection.inputStream))
			val buffer = StringBuilder()

			var byte = reader.read()
			while (byte != -1) {
				buffer.append(byte.toChar())
				byte = reader.read()
			}
			val songs = Gson().fromJson(buffer.toString(), Array<Song>::class.java)

			return songs.asList()

		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}
}