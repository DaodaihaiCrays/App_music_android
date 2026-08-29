package com.example.app_music.player

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.size
import androidx.glance.action.clickable
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.color.ColorProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.Action
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import com.example.app_music.MainActivity

class MusicWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val songTitle = prefs[MusicWidgetState.KEY_TITLE] ?: "Không có bài hát"
            val progress = prefs[MusicWidgetState.KEY_PROGRESS] ?: 0f
            val isPlaying = prefs[MusicWidgetState.KEY_IS_PLAYING] ?: false
            
            MusicWidgetContent(songTitle, progress, isPlaying)
        }
    }

    @Composable
    private fun MusicWidgetContent(songTitle: String, progress: Float, isPlaying: Boolean) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(ColorProvider(day = Color(0xFF1A1A1A), night = Color(0xFF1A1A1A)))
                .cornerRadius(16.dp)
                .padding(8.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = songTitle,
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(day = Color.White, night = Color.White),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
            
            Spacer(modifier = GlanceModifier.height(6.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp),
                color = ColorProvider(day = Color(0xFF1DB954), night = Color(0xFF1DB954)),
                backgroundColor = ColorProvider(day = Color.DarkGray, night = Color.DarkGray)
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    iconRes = android.R.drawable.ic_media_previous,
                    onClick = actionRunCallback<MusicActionCallback>(
                        actionParametersOf(MusicAction.PARAMETER_KEY to MusicAction.ACTION_PREV)
                    )
                )
                
                Spacer(modifier = GlanceModifier.width(16.dp))
                
                IconButton(
                    iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    onClick = actionRunCallback<MusicActionCallback>(
                        actionParametersOf(MusicAction.PARAMETER_KEY to MusicAction.ACTION_PLAY_PAUSE)
                    ),
                    isMain = true
                )
                
                Spacer(modifier = GlanceModifier.width(16.dp))
                
                IconButton(
                    iconRes = android.R.drawable.ic_media_next,
                    onClick = actionRunCallback<MusicActionCallback>(
                        actionParametersOf(MusicAction.PARAMETER_KEY to MusicAction.ACTION_NEXT)
                    )
                )
            }
        }
    }

    @Composable
    private fun IconButton(
        iconRes: Int,
        onClick: Action,
        isMain: Boolean = false
    ) {
        val buttonSize = if (isMain) 44.dp else 36.dp
        val iconSize = if (isMain) 24.dp else 20.dp
        
        Box(
            modifier = GlanceModifier
                .size(buttonSize)
                .background(
                    ColorProvider(
                        day = if (isMain) Color(0xFF1DB954) else Color(0xFF333333),
                        night = if (isMain) Color(0xFF1DB954) else Color(0xFF333333)
                    )
                )
                .cornerRadius(buttonSize / 2)
                .clickable(onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(iconSize)
            )
        }
    }
}

class MusicActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[MusicAction.PARAMETER_KEY]
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }
}

object MusicWidgetState {
    val KEY_TITLE = stringPreferencesKey("song_title")
    val KEY_PROGRESS = floatPreferencesKey("progress")
    val KEY_IS_PLAYING = booleanPreferencesKey("is_playing")
}

object MusicAction {
    const val ACTION_PLAY_PAUSE = "com.example.app_music.PLAY_PAUSE"
    const val ACTION_PREV = "com.example.app_music.PREV"
    const val ACTION_NEXT = "com.example.app_music.NEXT"
    val PARAMETER_KEY = ActionParameters.Key<String>("music_action")
}
