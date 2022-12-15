package com.example.musicsplayer

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.example.musicsplayer.api.SongServices
import com.example.musicsplayer.fragments.SongListItem
import com.example.musicsplayer.models.Song
import com.example.musicsplayer.receivers.OnComplete
import com.example.musicsplayer.services.MusicServices
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileNotFoundException
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
	private val log = Logger.getLogger(javaClass.name)
	private var songs: List<Song>? = null
	private lateinit var songsDownloadIdMap: MutableMap<Long, Song>
	private lateinit var mediaPlayer: MediaPlayer
	private val filename = "saved.txt"
	private lateinit var songListItemFragments: ArrayList<SongListItem>
	private lateinit var downloadManager: DownloadManager

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		songsDownloadIdMap = mutableMapOf()
		songListItemFragments = ArrayList()
		downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		mediaPlayer = MediaPlayer()

		registerReceiver(OnComplete(), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

		GlobalScope.launch {

//			downloadDataUsingDownloadManager()
			downloadDataUsingRetrofit()
		}
	}

	private fun downloadDataUsingRetrofit() {
		val retrofit = Retrofit.Builder()
			.baseUrl("https://api.dev.bkplus.tech/ringtone/")
			.addConverterFactory(GsonConverterFactory.create())
			.build()
		val service = retrofit.create(SongServices::class.java)

		service.getSongsDetails().enqueue(object : Callback<List<Song>> {
			override fun onResponse(call: Call<List<Song>>, response: Response<List<Song>>) {
				songs = response.body()
				runOnUiThread {
					showSongsList()
				}
			}

			override fun onFailure(call: Call<List<Song>>, t: Throwable) {
				log.warning(t.message)
			}
		})
	}

	private fun downloadDataUsingDownloadManager() {
		try {
			val input = openFileInput(filename)
			val buffer = StringBuilder()
			input.use {
				var byte = input.read()
				while (byte != -1) {
					buffer.append(byte.toChar())
					byte = input.read()
				}
				songs = if (buffer.toString() == "null") {
					MusicServices().getSongs()
				} else {
					Gson().fromJson(buffer.toString(), Array<Song>::class.java).toList()
				}
			}
		} catch (e: FileNotFoundException) {
			e.printStackTrace()
			songs = MusicServices().getSongs()
		}

		if (songs != null) {
			for (song in songs!!) {
				val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${song.title}.mp3")
				if (!file.exists()) {
					startDownload(song)
				}
			}
		}
	}

	private fun showSongsList() {
		findViewById<ConstraintLayout>(R.id.root).removeView(findViewById<TextView>(R.id.downloading))
		val trans = supportFragmentManager.beginTransaction()
		for (song in songs!!) {
			val frag = SongListItem(song)
			trans.add(R.id.songsList, frag)
			songListItemFragments.add(frag)
		}
		trans.commit()
	}

	private fun startDownload(song: Song): Long {

		val downloadReference: Long
		val request = DownloadManager.Request(Uri.parse(song.mp3))

		request.setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, "${song.title}.mp3")

		downloadReference = downloadManager.enqueue(request)

		val prevValue = songsDownloadIdMap.put(downloadReference, song)
		log.info("Previous value in map: $prevValue")
		return downloadReference
	}

	fun setupFilePath() {
		val out = openFileOutput(filename, Context.MODE_PRIVATE)
		GlobalScope.launch {
			out.use {
				out.write(Gson().toJson(songs).toByteArray())
			}
		}
	}

	fun playMusic(song: Song) {
		for (frag in songListItemFragments) {
			frag.view?.findViewById<ImageView>(R.id.isPlaying)?.isVisible = false
		}
		stopMusic()

		val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${song.title}.mp3")
		if (!file.exists()) {
			Toast.makeText(this, "Không tìm thấy tài nguyên", Toast.LENGTH_LONG).show()
			return
		}
		mediaPlayer.reset()
		mediaPlayer.setDataSource(this@MainActivity, file.toUri())
		mediaPlayer.prepare()

		mediaPlayer.setOnPreparedListener {
			mediaPlayer.start()
		}
	}

	fun stopMusic() {
		if (mediaPlayer.isPlaying) mediaPlayer.stop()
	}

	private fun showYesNoDialogBox(message: String, positiveOpt: String, negativeOpt: String) {
		val builder = AlertDialog.Builder(this)

		builder.setTitle("error")
		builder.setMessage(message)
		TODO("later")
//		builder.setPositiveButton(positiveOpt, object: DialogInterface.OnClickListener {
//			override fun onClick(p0: DialogInterface?, p1: Int) {
//				startDownload(song)
//			}
//		}
	}

}