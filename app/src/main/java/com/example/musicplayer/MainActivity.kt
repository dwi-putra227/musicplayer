package com.example.musicplayer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.SearchView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var songsAdapter: SongsAdapter
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private var currentSongIndex = -1
    private lateinit var visualizerView: CustomVisualizerView

    private val NOTIFICATION_ID = 1
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var wakeLock: PowerManager.WakeLock

    private var isShuffleOn = false
    private var isRepeatOneOn = false
    private var isRepeatAllOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "MusicPlayer::WakeLock"
        )
        wakeLock.acquire()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        songsAdapter = SongsAdapter(emptyList()) { song ->
            playSong(song)
        }
        recyclerView.adapter = songsAdapter

        recyclerView.scrollToPosition(30)

        seekBar = findViewById(R.id.seekBar)
        setupSeekBarListener()

        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener {
                it.start()
                seekBar.max = it.duration
                updateSeekBar()
            }
            setOnErrorListener { mp, what, extra ->
                Toast.makeText(
                    this@MainActivity,
                    "Error occurred while playing the song.",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            loadSongs()
            setupVisualizer()
        }

        val pauseResumeButton: Button = findViewById(R.id.pauseResumeButton)
        val previousButton: Button = findViewById(R.id.previousButton)
        val nextButton: Button = findViewById(R.id.nextButton)
        val shuffleButton: Button = findViewById(R.id.shuffleButton)
        val repeatButton: Button = findViewById(R.id.reapet_button)
        val playlist_button: Button = findViewById(R.id.playlist_button)
        val searchButton: Button = findViewById(R.id.search_button)
        val searchView: SearchView = findViewById(R.id.searchView)
        val controlPanel: LinearLayout = findViewById(R.id.controlPanel)
        val Playing_Song_Cardview = findViewById<CardView>(R.id.Playing_Song_Cardview)
        val heading = findViewById<TextView>(R.id.heading)
        val heading2 = findViewById<TextView>(R.id.heading2)
        val Playing_Song_Cardview2: CardView = findViewById(R.id.Playing_Song_Cardview)
        visualizerView = findViewById(R.id.visualizerView)

        searchView.visibility = View.GONE

        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()

        var isPlaylistVisible = true

        fun hideSearchView() {
            if (searchView.visibility == View.VISIBLE) {
                searchView.visibility = View.GONE
                controlPanel.visibility = View.VISIBLE

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
            }
        }

        pauseResumeButton.setOnClickListener {
            hideSearchView()
            togglePlayback()
        }

        previousButton.setOnClickListener {
            hideSearchView()
            playPreviousSong()
        }

        nextButton.setOnClickListener {
            hideSearchView()
            playNextSong()
        }

        shuffleButton.setOnClickListener {
            hideSearchView()
            toggleShuffle()
        }

        repeatButton.setOnClickListener {
            hideSearchView()
            toggleRepeat()
        }

        playlist_button.setOnClickListener {
            hideSearchView()
            heading2.visibility = View.GONE
            if (isPlaylistVisible) {
                Playing_Song_Cardview.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                playlist_button.setBackgroundResource(R.drawable.playlist)
                heading.setText("Now Playing")
                visualizerView.visibility = View.VISIBLE
                controlPanel.visibility = View.VISIBLE
            } else {
                Playing_Song_Cardview.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                playlist_button.setBackgroundResource(R.drawable.playing_button)
                heading.setText("All Songs")
                visualizerView.visibility = View.GONE
                controlPanel.visibility = View.VISIBLE
            }
            isPlaylistVisible = !isPlaylistVisible
        }

        searchButton.setOnClickListener {
            if (searchView.visibility == View.VISIBLE) {
                searchView.visibility = View.GONE
                controlPanel.visibility = View.VISIBLE
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
            } else {
                searchView.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
                Playing_Song_Cardview2.visibility = View.GONE
                controlPanel.visibility = View.GONE
                searchView.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }


        searchButton.performClick()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val filteredList = songsAdapter.getSongs().filter { song ->
                    song.title.contains(newText, ignoreCase = true) || song.artist.contains(newText, ignoreCase = true)
                }
                songsAdapter.submitList(filteredList)
                return true
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchView.setQuery("", false)
                loadSongs()
            }
        }
    }

    private fun setupVisualizer() {
        if (::visualizerView.isInitialized) {
            visualizerView.setPlayer(mediaPlayer.audioSessionId)
        }
    }

    private fun loadSongs() {
        val songsList = mutableListOf<Song>()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val musicCursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )

        musicCursor?.use { cursor ->
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val path = cursor.getString(pathColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                songsList.add(Song(title, artist, path, albumArtUri))
            }
        }

        songsList.reverse()
        songsAdapter = SongsAdapter(songsList) { song ->
            currentSongIndex = songsList.indexOf(song)
            playSong(song)
        }
        recyclerView.adapter = songsAdapter
        recyclerView.scrollToPosition(0)
    }

    private fun toggleShuffle() {
        isShuffleOn = !isShuffleOn


        val shuffleButton: Button = findViewById(R.id.shuffleButton)
        shuffleButton.setBackgroundResource(if (isShuffleOn) R.drawable.shuffle_on else R.drawable.shuffle_off)


        if (isShuffleOn) {
            songsAdapter.shuffleSongs()
        }
    }

    private fun toggleRepeat() {
        val repeat_button: Button = findViewById(R.id.reapet_button)

        if (isRepeatAllOn) {
            isRepeatAllOn = false
            isRepeatOneOn = false
            repeat_button.setBackgroundResource(R.drawable.repeat)
        } else if (isRepeatOneOn) {

            isRepeatAllOn = true
            isRepeatOneOn = false
            repeat_button.setBackgroundResource(R.drawable.repeat_on)
        } else {

            isRepeatOneOn = true
            repeat_button.setBackgroundResource(R.drawable.repeat_one)
        }


        if (isRepeatAllOn) {
            isShuffleOn = false
        }
    }

    private fun playNextSong() {

        if (isShuffleOn) {
            val randomIndex = (0 until songsAdapter.itemCount).random()
            val nextSong = songsAdapter.getSongs()[randomIndex]
            currentSongIndex = randomIndex
            playSong(nextSong)
        } else if (isRepeatOneOn) {
            val currentSong = songsAdapter.getSongs()[currentSongIndex]
            playSong(currentSong)
        } else if (currentSongIndex < songsAdapter.itemCount - 1) {
            val nextSong = songsAdapter.getSongs()[currentSongIndex + 1]
            currentSongIndex++
            playSong(nextSong)
        } else if (isRepeatAllOn) {
            currentSongIndex = 0
            val nextSong = songsAdapter.getSongs()[currentSongIndex]
            playSong(nextSong)
        }
    }

    private fun playSong(song: Song) {
        val pauseResumeButton: Button = findViewById(R.id.pauseResumeButton)
        val playingSongImageView: ImageView = findViewById(R.id.Playing_Song_Imageview)
        val songArtistTextView: TextView = findViewById(R.id.song_artist)
        val cardview: CardView = findViewById(R.id.Playing_Song_Cardview)
        val heading = findViewById<TextView>(R.id.heading)
        val playlist_button = findViewById<Button>(R.id.playlist_button)

        try {
            songsAdapter.getSongs().forEach { it.isPlaying = false }

            val index = songsAdapter.getSongs().indexOf(song)
            if (index != -1) {
                songsAdapter.getSongs()[index].isPlaying = true
                songsAdapter.notifyDataSetChanged()
            }

            mediaPlayer.reset()
            mediaPlayer.setDataSource(song.path)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                it.start()
                seekBar.max = it.duration
                updateSeekBar()
                findViewById<TextView>(R.id.song_title).apply {
                    text = song.title
                    setOnClickListener {
                        recyclerView.smoothScrollToPosition(index)
                    }
                }
                pauseResumeButton.setBackgroundResource(R.drawable.pause)


                val albumArtUri = Uri.parse(song.albumArtUri)
                if (albumArtUri != null) {
                    playingSongImageView.setImageURI(albumArtUri)
                } else {
                    playingSongImageView.setImageResource(R.drawable.play)
                }

                songArtistTextView.text = song.artist
            }

            mediaPlayer.setOnCompletionListener {
                playNextSong()
            }

            showNotification(song)
            visualizerView.visibility = View.VISIBLE
            cardview.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            heading.setText("Now Playing")
            playlist_button.setBackgroundResource(R.drawable.playlist)
            val searchView: SearchView = findViewById(R.id.searchView)
            val controlPanel: LinearLayout = findViewById(R.id.controlPanel)
            searchView.visibility = View.GONE
            controlPanel.visibility = View.VISIBLE

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this@MainActivity,
                "Error occurred while playing the song.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun togglePlayback() {
        val pauseResumeButton: Button = findViewById(R.id.pauseResumeButton)
        if (mediaPlayer.isPlaying) {
            Log.d("MainActivity", "Pausing playback")
            mediaPlayer.pause()
            notificationManager.cancel(NOTIFICATION_ID)
            pauseResumeButton.setBackgroundResource(R.drawable.play)
            visualizerView.visibility = View.GONE
        } else {
            Log.d("MainActivity", "Resuming playback")
            mediaPlayer.start()
            if (currentSongIndex != -1) {
                val song = songsAdapter.getSongs()[currentSongIndex]
                showNotification(song)
            }
            pauseResumeButton.setBackgroundResource(R.drawable.pause)
            visualizerView.visibility = View.VISIBLE
        }
    }

    private fun playPreviousSong() {
        val previousIndex = currentSongIndex - 1
        if (previousIndex >= 0) {
            val previousSong = songsAdapter.getSongs()[previousIndex]
            currentSongIndex = previousIndex
            playSong(previousSong)
        }
    }

    private fun updateSeekBar() {
        if (mediaPlayer.isPlaying) {
            val currentPosition = mediaPlayer.currentPosition
            seekBar.progress = currentPosition

            val positivePlaybackTime = formatTime(currentPosition)
            findViewById<TextView>(R.id.positive_playback_timer).text = positivePlaybackTime
            val remainingTime = mediaPlayer.duration - currentPosition
            val negativePlaybackTime = formatTime(remainingTime)
            findViewById<TextView>(R.id.negative_playback_timer).text = "-$negativePlaybackTime"
            handler.postDelayed({ updateSeekBar() }, 1000)
        }
    }

    private fun setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(song: Song) {
        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification_layout)
        notificationLayout.setTextViewText(R.id.notification_title, song.title)
        notificationLayout.setTextViewText(R.id.notification_artist, song.artist)

        val albumArtUri = Uri.parse(song.albumArtUri)
        val contentResolver = applicationContext.contentResolver
        try {
            val inputStream = contentResolver.openInputStream(albumArtUri)
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                notificationLayout.setImageViewBitmap(R.id.notification_album_art, bitmap)
                inputStream.close()
            } else {
                notificationLayout.setImageViewResource(R.id.notification_album_art, R.drawable.audioicon)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            notificationLayout.setImageViewResource(R.id.notification_album_art, R.drawable.audioicon)
        } catch (e: IOException) {
            e.printStackTrace()
            notificationLayout.setImageViewResource(R.id.notification_album_art, R.drawable.audioicon)
        }


        val pauseIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = MusicService.ACTION_TOGGLE_PLAYBACK
        }
        val pendingPauseIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.notification_play_pause, pendingPauseIntent)

        val nextIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = MusicService.ACTION_PLAY_NEXT
        }
        val pendingNextIntent = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.notification_next, pendingNextIntent)

        val prevIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = MusicService.ACTION_PLAY_PREVIOUS
        }
        val pendingPrevIntent = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.notification_previous, pendingPrevIntent)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.audioicon)
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(null)
            .setOnlyAlertOnce(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Handle the case where the app doesn't have permission to post notifications
            return
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        notificationManager.cancel(NOTIFICATION_ID)
        val visualizerView: CustomVisualizerView = findViewById(R.id.visualizerView)
        visualizerView.releaseVisualizer()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs()
            } else {
                Toast.makeText(
                    this,
                    "Permission Denied! Please grant permission to access storage.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private const val CHANNEL_ID = "MusicPlayerChannel"
        const val ACTION_TOGGLE_PLAYBACK = "com.example.musicplayer.TOGGLE_PLAYBACK"
        const val ACTION_PLAY_PREVIOUS = "com.example.musicplayer.PLAY_PREVIOUS"
        const val ACTION_PLAY_NEXT = "com.example.musicplayer.PLAY_NEXT"
        const val NOTIFICATION_ID = 1
    }
}
