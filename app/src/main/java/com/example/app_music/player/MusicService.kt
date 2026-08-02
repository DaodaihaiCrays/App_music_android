package com.example.app_music.player

import android.app.PendingIntent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService: MediaSessionService() {

    // De lam viec voi Android system, Android system không giao tiep truc tiep voi expo
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()

        // 1. Create an Intent that opens your MainActivity when the notification is clicked
        val intent = packageManager.getLaunchIntentForPackage(packageName)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. Build the session and link the PendingIntent
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    // Return the session to the system
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // Essential for letting the system manage the foreground state
//        false: Service không còn chạy foreground, Android có thể giữ nó thêm một lúc hoặc tự hủy khi cần RAM/tài nguyên. Không phải false là xóa ngay.
//        true: Service chạy foreground nên được ưu tiên giữ sống cao hơn, thường dùng khi nhạc đang phát nền.


        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

/**
SessionToken = địa chỉ của MusicService/MediaSession
MediaController = điều khiển từ xa
MediaSession = bộ phận tiếp nhận lệnh
ExoPlayer = thiết bị thực sự phát nhạc
 **/