package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("NotificationActionReceiver", "onReceive called with action: ${intent?.action}")
        when (intent?.action) {
            MusicService.ACTION_PLAY_PREVIOUS -> {
                Log.d("NotificationActionReceiver", "ACTION_PLAY_PREVIOUS received")
                val previousIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY_PREVIOUS
                }
                context?.startService(previousIntent)
            }
            MusicService.ACTION_TOGGLE_PLAYBACK -> {
                Log.d("NotificationActionReceiver", "ACTION_TOGGLE_PLAYBACK received")
                val toggleIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_TOGGLE_PLAYBACK
                }
                context?.startService(toggleIntent)
            }
            MusicService.ACTION_PLAY_NEXT -> {
                Log.d("NotificationActionReceiver", "ACTION_PLAY_NEXT received")
                val nextIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY_NEXT
                }
                context?.startService(nextIntent)
            }
            else -> {
                Log.d("NotificationActionReceiver", "Unknown action received")
            }
        }
    }
}
