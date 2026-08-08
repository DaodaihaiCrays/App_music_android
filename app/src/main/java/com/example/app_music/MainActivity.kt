package com.example.app_music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.app_music.ui.screen.HomeScreen
import com.example.app_music.ui.theme.AppMusicPlayerTheme

class MainActivity : ComponentActivity() {

    private var showBatteryDialog by mutableStateOf(false)
    private var batteryPercent by mutableStateOf(0)

    // Lắng nghe tín hiệu pin yếu từ MusicService
    private val lowBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.app_music.LOW_BATTERY") {
                batteryPercent = intent.getIntExtra("percent", 0)
                showBatteryDialog = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Đăng ký nhận Broadcast nội bộ
        val filter = IntentFilter("com.example.app_music.LOW_BATTERY")
        ContextCompat.registerReceiver(
            this,
            lowBatteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Kiểm tra pin ngay khi app vừa mở
        checkInitialBatteryStatus()

        setContent {
            AppMusicPlayerTheme(darkTheme = true) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(modifier = Modifier.padding(innerPadding))

                    // Hiển thị Popup khi pin yếu
                    if (showBatteryDialog) {
                        AlertDialog(
                            onDismissRequest = { showBatteryDialog = false },
                            title = { Text(text = "Cảnh báo pin yếu") },
                            text = { Text(text = "Pin của bạn hiện còn $batteryPercent%. Vui lòng kết nối bộ sạc để tiếp tục nghe nhạc.") },
                            confirmButton = {
                                Button(onClick = { showBatteryDialog = false }) {
                                    Text("Đã hiểu")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkInitialBatteryStatus() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val pct = (level * 100 / scale.toFloat()).toInt()
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            if (pct <= 20 && !isCharging) {
                batteryPercent = pct
                showBatteryDialog = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(lowBatteryReceiver)
    }
}
