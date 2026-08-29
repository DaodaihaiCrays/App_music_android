package com.example.app_music.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.GlanceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MusicService: MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var lastBatteryPct = -1

    // Receiver lắng nghe trạng thái pin từ hệ thống
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                if (level != -1 && scale != -1) {
                    val batteryPct = (level * 100 / scale.toFloat()).toInt()
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

                    // Chỉ thông báo nếu pin <= 20%, không sạc và phần trăm có thay đổi
                    if (batteryPct <= 20 && !isCharging && batteryPct != lastBatteryPct) {
                        lastBatteryPct = batteryPct
                        val lowBatteryIntent = Intent("com.example.app_music.LOW_BATTERY")
                        lowBatteryIntent.putExtra("percent", batteryPct)
                        lowBatteryIntent.setPackage(packageName) // Chỉ gửi nội bộ trong app
                        sendBroadcast(lowBatteryIntent)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)

        // Đăng ký nhận thông báo pin từ hệ thống
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(this, batteryReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
        
        // Cập nhật trạng thái widget ngay khi khởi tạo
        updateWidgetState()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MusicAction.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            MusicAction.ACTION_PREV -> {
                player.seekToPrevious()
            }
            MusicAction.ACTION_NEXT -> {
                player.seekToNext()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var progressJob: kotlinx.coroutines.Job? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_POSITION_DISCONTINUITY
                )
            ) {
                updateWidgetState()
            }
            
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                if (player.isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }
        }
    }

    private fun updateWidgetState() {
        val hasMedia = player.mediaItemCount > 0
        val playbackState = player.playbackState
        
        val songTitle = if (!hasMedia || playbackState == Player.STATE_IDLE) {
            "Không có bài hát"
        } else {
            player.currentMediaItem?.mediaMetadata?.title?.toString()
                ?: player.currentMediaItem?.localConfiguration?.uri?.lastPathSegment
                ?: "Không có bài hát"
        }

        val isPlaying = player.isPlaying
        val duration = player.duration
        val progress = if (hasMedia && playbackState != Player.STATE_IDLE && duration > 0) {
            player.currentPosition.toFloat() / duration
        } else {
            0f
        }

        serviceScope.launch {
            val manager = GlanceAppWidgetManager(this@MusicService)
            val glanceIds = manager.getGlanceIds(MusicWidget::class.java)
            for (glanceId in glanceIds) {
                updateAppWidgetState(this@MusicService, glanceId) { prefs ->
                    prefs[MusicWidgetState.KEY_TITLE] = songTitle
                    prefs[MusicWidgetState.KEY_IS_PLAYING] = isPlaying
                    prefs[MusicWidgetState.KEY_PROGRESS] = progress
                }
            }
            MusicWidget().updateAll(this@MusicService)
        }
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = serviceScope.launch {
            while (player.isPlaying) {
                val duration = player.duration
                if (duration > 0) {
                    val progress = player.currentPosition.toFloat() / duration
                    val manager = GlanceAppWidgetManager(this@MusicService)
                    val glanceIds = manager.getGlanceIds(MusicWidget::class.java)
                    for (glanceId in glanceIds) {
                        updateAppWidgetState(this@MusicService, glanceId) { prefs ->
                            prefs[MusicWidgetState.KEY_PROGRESS] = progress
                        }
                    }
                    MusicWidget().updateAll(this@MusicService)
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        resetWidgetState()
        stopSelf()
    }

    override fun onDestroy() {
        // Hủy đăng ký receiver để tránh rò rỉ bộ nhớ
        unregisterReceiver(batteryReceiver)
        
        resetWidgetState()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun resetWidgetState() {
        // Sử dụng GlobalScope hoặc một scope không bị hủy ngay lập tức để đảm bảo widget được cập nhật
        CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(this@MusicService)
            val glanceIds = manager.getGlanceIds(MusicWidget::class.java)
            for (glanceId in glanceIds) {
                updateAppWidgetState(this@MusicService, glanceId) { prefs ->
                    prefs[MusicWidgetState.KEY_TITLE] = "Không có bài hát"
                    prefs[MusicWidgetState.KEY_IS_PLAYING] = false
                    prefs[MusicWidgetState.KEY_PROGRESS] = 0f
                }
            }
            MusicWidget().updateAll(this@MusicService)
        }
    }
}
