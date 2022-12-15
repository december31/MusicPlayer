package com.example.musicsplayer.fragments

import android.media.Image
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.musicsplayer.MainActivity
import com.example.musicsplayer.R
import com.example.musicsplayer.models.Song
import java.io.File

class SongListItem(private val song: Song): Fragment(), View.OnClickListener {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.song_list_item, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		view.findViewById<TextView>(R.id.title).text = song.title
		view.findViewById<TextView>(R.id.duration).text = song.duration
		view.setOnClickListener(this)
	}

	override fun onClick(view: View?) {
		val isPlaying = view?.findViewById<ImageView>(R.id.isPlaying)
		if (isPlaying != null) {
			if(isPlaying.isVisible) {
				(activity as MainActivity).stopMusic()
				isPlaying.isVisible = false
				return
			}
		}
		(activity as MainActivity).playMusic(song)
		if (isPlaying != null) {
			isPlaying.isVisible = !isPlaying.isVisible
		}
	}
}