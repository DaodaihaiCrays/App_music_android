package com.example.app_music.ui.screen


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.app_music.ui.theme.NeonGreen
import com.example.app_music.viewmodels.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit
) {
    val currentSong = viewModel.currentSong ?: return
    val isFavorite = viewModel.isFavorite(currentSong)
    var isVolumeSliderVisible by remember { mutableStateOf(false) }
    var volumeInteractionKey by remember { mutableIntStateOf(0) }

    // Tự động ẩn Slider sau 3 giây
    LaunchedEffect(volumeInteractionKey) {
        if (isVolumeSliderVisible) {
            delay(3000)
            isVolumeSliderVisible = false
        }
    }

    // Intercept system back gesture/button to call onBackClick instead of exiting the app
    BackHandler {
        onBackClick()
    }

    LaunchedEffect(viewModel.isPlaying) {
        while (isActive) {
            viewModel.updateProgress()
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                }

                Spacer(modifier = Modifier.height(32.dp))

                // Big Album Art
                AsyncImage(
                    model = currentSong.albumArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Song and Artist Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Seek Bar (Slider)
                Slider(
                    value = viewModel.currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..viewModel.duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(viewModel.currentPosition),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatTime(viewModel.duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Playback Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.toggleFavorite(currentSong)
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Previous
                    IconButton(onClick = { viewModel.previous() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Main Play/Pause Button
                    Surface(
                        onClick = { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(80.dp),
                        tonalElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    // Next
                    IconButton(onClick = { viewModel.next() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Repeat Mode Toggle
                    IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                        val (icon, tint) = when (viewModel.repeatMode) {
                            MusicViewModel.RepeatMode.NONE ->
                                Icons.Rounded.Repeat to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            MusicViewModel.RepeatMode.ALL ->
                                Icons.Rounded.Repeat to MaterialTheme.colorScheme.primary
                            MusicViewModel.RepeatMode.ONE ->
                                Icons.Rounded.RepeatOne to MaterialTheme.colorScheme.primary
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Thanh tăng giảm âm lượng bên phải màn hình (ẩn/hiện khi long press)
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.3f)
                    .align(Alignment.CenterEnd)
                    .width(60.dp) // Vùng nhận diện nhấn giữ rộng hơn một chút cho thoải mái
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                isVolumeSliderVisible = true
                                volumeInteractionKey++
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isVolumeSliderVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Slider(
                        value = viewModel.volume,
                        onValueChange = {
                            viewModel.updateVolume(it)
                            volumeInteractionKey++
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .rotate(-90f)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    Constraints(
                                        minWidth = constraints.minHeight,
                                        maxWidth = constraints.maxHeight,
                                        minHeight = constraints.minWidth,
                                        maxHeight = constraints.maxWidth,
                                    )
                                )
                                layout(placeable.height, placeable.width) {
                                    placeable.place(-((placeable.width - placeable.height) / 2), -((placeable.height - placeable.width) / 2))
                                }
                            }
                            .fillMaxHeight(0.5f),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}