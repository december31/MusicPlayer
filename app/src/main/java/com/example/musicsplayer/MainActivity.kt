package com.example.musicsplayer

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.RelativeLayout
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.util.*
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
	private val log = Logger.getLogger(javaClass.name)
	private var songs: List<Song>? = null
	private lateinit var songsDownloadIdMap: MutableMap<Long, Song>
	private lateinit var mediaPlayer: MediaPlayer
	private val filename = "saved.txt"
	private lateinit var songListItemFragments: ArrayList<SongListItem>
	private lateinit var downloadManager: DownloadManager
	private lateinit var retrofit: Retrofit
	private lateinit var songServices: SongServices

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		songsDownloadIdMap = mutableMapOf()
		songListItemFragments = ArrayList()
		downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		mediaPlayer = MediaPlayer()
		registerReceiver(OnComplete(), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

		GlobalScope.launch {
			downloadDataUsingRetrofit()
		}

	}

	private fun downloadDataUsingRetrofit() {
		retrofit = Retrofit.Builder()
			.baseUrl("https://api.dev.bkplus.tech/ringtone/")
			.addConverterFactory(GsonConverterFactory.create())
			.build()
		songServices = retrofit.create(SongServices::class.java)

		songServices.getSongsDetails().enqueue(object : Callback<List<Song>> {
			override fun onResponse(call: Call<List<Song>>, response: Response<List<Song>>) {
				songs = response.body()
				if (songs == null) return
				runOnUiThread {
					showSongsList()
				}
			}

			override fun onFailure(call: Call<List<Song>>, t: Throwable) {
				log.warning(t.message)
			}
		})
	}

	fun retrofitStartDownload(song: Song) {
		findViewById<RelativeLayout>(R.id.downloading)?.visibility = View.VISIBLE

		songServices.getMp3s(song.mp3Url).enqueue(object : Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
				log.info("get ${song.id}: ${song.title}'s mp3")
				saveFile(response.body(), song, onComplete =   {
					playMusic(song)
					findViewById<RelativeLayout>(R.id.downloading)?.visibility = View.INVISIBLE
				})
				saveSongDetails()
			}

			override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
				log.warning("Mp3 Download: ${t.message}")
			}
		})

		songServices.getThumbnail(song.thumbnailUrl).enqueue(object : Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
				log.info("get ${song.title}'s thumbnail")
				saveFile(response.body(), song, onComplete = {})
				saveSongDetails()
			}

			override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
				log.warning("Thumbnail Download: ${t.message}")
			}

		})
	}
	private fun saveFile(body: ResponseBody?, song: Song, onComplete: (() -> Unit)) {

		if (body == null) return

		val filename = UUID.randomUUID()
		// get file extension
		val fileExtension = song.mp3Url.substring(song.mp3Url.lastIndexOf("."), song.mp3Url.length)
		val file = File(cacheDir, "${filename}$fileExtension")

		if (body.contentType().toString().startsWith("audio")) {
			song.mp3FileName = file.name
		} else {
			song.thumbnailFileName = file.name
		}

		val fos = FileOutputStream(file)

		GlobalScope.launch {
			fos.use {
				it.write(body.bytes())
			}
			runOnUiThread {
				onComplete.invoke()
			}
		} }

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
		}
		songs = MusicServices().getSongs()

		if (songs != null) {
			for (song in songs!!) {
				val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${song.title}.mp3")
				if (!file.exists()) {
					GlobalScope.launch {
						startDownload(song)
					}
				}
			}
			runOnUiThread {
				showSongsList()
			}
		}
	}

	private fun startDownload(song: Song): Long {

		val downloadReference: Long
		val request = DownloadManager.Request(Uri.parse(song.mp3Url))

		request.setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, "${song.title}.mp3")

		downloadReference = downloadManager.enqueue(request)

		val prevValue = songsDownloadIdMap.put(downloadReference, song)
		log.info("Previous value in map: $prevValue")
		return downloadReference
	}

	fun saveSongDetails() {
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

		val file = File(cacheDir, song.mp3FileName!!)
		if (!file.exists()) {
			Toast.makeText(this, "Không tìm thấy tài nguyên", Toast.LENGTH_LONG).show()
			return
		}
		mediaPlayer.reset()
		mediaPlayer.setDataSource(this@MainActivity, file.toUri())
		mediaPlayer.prepare()

		mediaPlayer.setOnPreparedListener {
			log.info("media player prepared")
			mediaPlayer.start()
		}
	}

	fun stopMusic() {
		if (mediaPlayer.isPlaying) mediaPlayer.stop()
	}

	private fun showSongsList() {
		val trans = supportFragmentManager.beginTransaction()
		for (song in songs!!) {
			val frag = SongListItem(song)
			trans.add(R.id.songsList, frag)
			songListItemFragments.add(frag)
		}
		trans.commit()
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
