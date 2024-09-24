package com.example.musicplayer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongsAdapter(
    private var songs: List<Song>,

    private val onItemClick: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {


    fun getSongs(): List<Song> {
        return songs
    }

    fun shuffleSongs() {
        songs = songs.shuffled()
        notifyDataSetChanged()
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song)
        holder.itemView.setOnClickListener { onItemClick(song) }


        if (song.isPlaying) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.shuffleOnColor))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int = songs.size

    fun submitList(newList: List<Song>) {
        songs = newList
        notifyDataSetChanged()
    }




    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistTextView)
        private val albumImageView: ImageView = itemView.findViewById(R.id.albumImageView)


        fun bind(song: Song) {
            titleTextView.text = song.title
            artistTextView.text = song.artist
            titleTextView.setTextColor(Color.BLACK)
            artistTextView.setTextColor(Color.BLACK)
            Glide.with(itemView.context)
                .load(song.albumArtUri)
                .placeholder(R.drawable.audioicon)
                .error(R.drawable.audioicon)
                .into(albumImageView)
        }


    }
}


