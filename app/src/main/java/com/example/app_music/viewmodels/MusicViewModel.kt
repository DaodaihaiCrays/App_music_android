package com.example.app_music.viewmodels

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.app_music.data.repository.MusicRepository
import com.example.app_music.model.Song
import com.example.app_music.player.MusicService
import com.google.common.util.concurrent.MoreExecutors
import android.util.Log

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)

    private var controller : MediaController? = null
    private var pendingSongToPlay: Song? = null


    var songs by mutableStateOf(emptyList<Song>())
    var isPlaying by mutableStateOf(false)
    var currentSong by mutableStateOf<Song?>(null)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var favoriteSongIds by mutableStateOf(emptySet<Long>())
        private set

    // Search State
    var searchQuery by mutableStateOf("")
        private set

    // Playback Modes
    var isShuffleEnabled by mutableStateOf(false)
    var repeatMode by mutableStateOf(RepeatMode.NONE)

    enum class RepeatMode { NONE, ALL, ONE }

    val filteredSongs: List<Song>
        get() = if (searchQuery.isEmpty()) {
            songs
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

    val favoriteSongs: List<Song>
        get() = songs.filter { it.id in favoriteSongIds }

    init {
        refreshFavoriteSongs()
        refreshSongs()
        setupMediaController(application)
    }

    fun refreshSongs() {
        songs = repository.getSongs()
    }

    private fun refreshFavoriteSongs() {
        favoriteSongIds = repository.getFavoriteSongIds()
    }

    fun isFavorite(song: Song): Boolean {
        return song.id in favoriteSongIds
    }

    fun toggleFavorite(song: Song) {
        if (isFavorite(song)) {
            repository.removeFavorite(song.id)
            favoriteSongIds = favoriteSongIds - song.id
        } else {
            repository.addFavorite(song.id)
            favoriteSongIds = favoriteSongIds + song.id
        }
    }

    fun isFirstSong(item: Song): Boolean {
        return item == filteredSongs.first()
    }

    fun isFinalSong(item: Song): Boolean {
        return item == filteredSongs.last()
    }


    private fun setupMediaController(application: Application) {
        val sessionToken = SessionToken(
            application,
            ComponentName(application, MusicService::class.java)
        )

        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture.addListener({
            runCatching {
                controllerFuture.get()
            }.onSuccess { builtController ->
                controller = builtController

                // Sync initial state from the service's player
                controller?.let { player ->
                    // Sync the song immediately on connect.
                    updateCurrentSongFromController()
                    isPlaying = player.isPlaying
                    isShuffleEnabled = player.shuffleModeEnabled

                    // Keep UI state in sync with playback changes.
                    player.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            updateCurrentSongFromController()
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                duration = player.duration
                            }
                        }
                    })

                    pendingSongToPlay?.let { song ->
                        pendingSongToPlay = null
                        startPlayback(song, player)
                    }
                }
            }.onFailure { throwable ->
                Log.e("MusicViewModel", "Failed to connect to MusicService", throwable)
            }
        }, MoreExecutors.directExecutor())
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    fun play(song: Song) {
        currentSong = song

        val player = controller
        if (player == null) {
            pendingSongToPlay = song
            return
        }

        startPlayback(song, player)
    }

    private fun startPlayback(song: Song, player: MediaController) {
        val mediaItems = songs.map { currentSong ->
            MediaItem.Builder()
                .setMediaId(currentSong.id.toString())
                .setUri(currentSong.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(currentSong.title)
                        .setArtist(currentSong.artist)
                        .build()
                )
                .build()
        }

        val index = songs.indexOf(song)
        if (index == -1) return

        player.setMediaItems(mediaItems, index, 0L)
        player.prepare()
        player.play()
        currentSong = song
        isPlaying = true
        duration = 0L
        currentPosition = 0L
    }

    fun togglePlayPause() {
        controller?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                val selectedSong = currentSong
                if (player.mediaItemCount == 0 && selectedSong != null) {
                    startPlayback(selectedSong, player)
                } else {
                    player.play()
                }
            }
        }
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.let { player ->
            if (player.currentPosition > 5000) {
                player.seekTo(0)
            } else {
                player.seekToPrevious()
            }
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        currentPosition = position
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        controller?.shuffleModeEnabled = isShuffleEnabled
    }

    fun toggleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }

        controller?.repeatMode = when (repeatMode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun updateProgress() {
        controller?.let { player ->
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
        }
    }

    private fun updateCurrentSongFromController() {
        if ((controller?.mediaItemCount ?: 0) == 0) return

        val index = controller?.currentMediaItemIndex ?: -1
        if (index in songs.indices) {
            currentSong = songs[index]
        }
    }


    override fun onCleared() {
        super.onCleared()
        controller?.release()
    }
}
