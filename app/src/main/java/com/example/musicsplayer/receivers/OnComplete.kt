package com.example.musicsplayer.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.musicsplayer.MainActivity
import java.util.logging.Logger

class OnComplete: BroadcastReceiver() {
	private val log = Logger.getLogger(javaClass.name)
	override fun onReceive(ctx: Context?, intent: Intent?) {
		val referenceId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
		log.info("referenceId: $referenceId")
		(ctx as MainActivity).setupFilePath()
	}
}