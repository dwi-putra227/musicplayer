package com.example.musicplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.io.IOException

class MusicService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private var isPrepared = false

    private var currentSong: Song? = null
    private val songsList: MutableList<Song> = mutableListOf()

    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        mediaPlayer.setOnCompletionListener {
            Log.d("MusicService", "Song completed, playing next song.")
            playNextSong()
        }

        mediaPlayer.setOnPreparedListener {
            Log.d("MusicService", "MediaPlayer is prepared, starting playback.")
            isPrepared = true
            mediaPlayer.start()
            currentSong?.let { song ->
                updateNotification(song)
            }
        }

        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e("MusicService", "Error: $what, $extra")
            mp.reset()
            isPrepared = false
            true
        }


        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAYBACK -> {
                Log.d("MusicService", "ACTION_TOGGLE_PLAYBACK received")
                if (mediaPlayer.isPlaying) {
                    Log.d("MusicService", "Pausing playback")
                    mediaPlayer.pause()
                } else if (isPrepared) {
                    Log.d("MusicService", "Resuming playback")
                    mediaPlayer.start()
                }
            }
            ACTION_PLAY_PREVIOUS -> {
                Log.d("MusicService", "ACTION_PLAY_PREVIOUS received")
                playPreviousSong()
            }
            ACTION_PLAY_NEXT -> {
                Log.d("MusicService", "ACTION_PLAY_NEXT received")
                playNextSong()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }



    private fun playPreviousSong() {
        if (currentSong != null && songsList.isNotEmpty()) {
            val currentIndex = songsList.indexOf(currentSong)
            val previousIndex = if (currentIndex > 0) currentIndex - 1 else songsList.size - 1
            currentSong = songsList[previousIndex]
            currentSong?.let {
                Log.d("MusicService", "Playing previous song: ${it.title}")
                playSong(it)
            }
        }
    }

    private fun playNextSong() {
        if (currentSong != null && songsList.isNotEmpty()) {
            val currentIndex = songsList.indexOf(currentSong)
            val nextIndex = if (songsList.size > 0) (currentIndex + 1) % songsList.size else 0
            currentSong = songsList[nextIndex]
            currentSong?.let {
                Log.d("MusicService", "Playing next song: ${it.title}")
                playSong(it)
            }
        }
    }

    private fun playSong(song: Song) {
        mediaPlayer.reset()
        isPrepared = false
        try {
            mediaPlayer.setDataSource(song.path)
            mediaPlayer.prepareAsync()
            Log.d("MusicService", "Preparing song: ${song.title}")
        } catch (e: IOException) {
            Log.e("MusicService", "Error setting data source", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(song: Song) {

        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification_layout)


        val bitmap = BitmapFactory.decodeFile(song.albumArtUri)
        notificationLayout.setImageViewBitmap(R.id.notification_album_art, bitmap)
        notificationLayout.setTextViewText(R.id.notification_title, song.title)
        notificationLayout.setTextViewText(R.id.notification_artist, song.artist)


        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_PLAY_PAUSE,
            Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_TOGGLE_PLAYBACK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationLayout.setOnClickPendingIntent(R.id.notification_play_pause, playPauseIntent)

        val previousIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_PLAY_PREVIOUS,
            Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_PLAY_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationLayout.setOnClickPendingIntent(R.id.notification_previous, previousIntent)

        val nextIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_PLAY_NEXT,
            Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_PLAY_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationLayout.setOnClickPendingIntent(R.id.notification_next, nextIntent)


        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .build()


        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_TOGGLE_PLAYBACK = "com.example.musicplayer.TOGGLE_PLAYBACK"
        const val ACTION_PLAY_PREVIOUS = "com.example.musicplayer.PLAY_PREVIOUS"
        const val ACTION_PLAY_NEXT = "com.example.musicplayer.PLAY_NEXT"
        const val CHANNEL_ID = "MusicPlayerChannel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        private const val REQUEST_CODE_PLAY_PAUSE = 1001
        private const val REQUEST_CODE_PLAY_PREVIOUS = 1002
        private const val REQUEST_CODE_PLAY_NEXT = 1003
    }
}
