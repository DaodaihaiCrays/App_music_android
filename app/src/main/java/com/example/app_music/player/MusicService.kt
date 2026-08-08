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

class MusicService: MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

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
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onDestroy() {
        // Hủy đăng ký receiver để tránh rò rỉ bộ nhớ
        unregisterReceiver(batteryReceiver)

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
