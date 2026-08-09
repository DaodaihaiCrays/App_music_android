package com.example.app_music.ui.screen

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_music.ui.theme.NeonGreen
import com.example.app_music.ui.components.MiniPlayer
import com.example.app_music.ui.components.SongList
import com.example.app_music.viewmodels.MusicViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    var isPlayerScreenVisible by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    var isVolumeSliderVisible by remember { mutableStateOf(false) }
    var volumeInteractionKey by remember { mutableIntStateOf(0) }

    // Tự động ẩn Slider sau 3 giây
    LaunchedEffect(volumeInteractionKey) {
        if (isVolumeSliderVisible) {
            delay(3000)
            isVolumeSliderVisible = false
        }
    }
    val mediaPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (results[audioPermission] == true) {
            viewModel.refreshSongs()
        }
    }

    LaunchedEffect(Unit) {
        val deniedPermissions = mediaPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isEmpty()) {
            viewModel.refreshSongs()
        } else {
            permissionLauncher.launch(deniedPermissions.toTypedArray())
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            Color(0xFF0F3460)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = viewModel.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text("Search songs or artists...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        } else {
                            Text(
                                text = "App Music",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) viewModel.onSearchQueryChange("")
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearching) "Close Search" else "Search Music"
                            )
                        }

                        if (!isSearching) {
                            IconButton(onClick = { isMoreMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options"
                                )
                            }
                            DropdownMenu(
                                expanded = isMoreMenuExpanded,
                                onDismissRequest = { isMoreMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Danh sách tất cả bài hát") },
                                    onClick = {
                                        viewModel.updateShowFavoritesOnly(false)
                                        isMoreMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Danh sách bài hát yêu thích") },
                                    onClick = {
                                        viewModel.updateShowFavoritesOnly(true)
                                        isMoreMenuExpanded = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                if (viewModel.currentSong != null) {
                    MiniPlayer(
                        viewModel = viewModel,
                        onPlayerClick = {
                            isPlayerScreenVisible = true
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // IMPORTANT: Ensure your SongList component uses viewModel.filteredSongs
                // If SongList takes a list parameter, pass it like: songs = viewModel.filteredSongs
                SongList(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Thanh tăng giảm âm lượng bên phải màn hình
        Box(
            modifier = Modifier
                .fillMaxHeight(0.3f)
                .align(Alignment.CenterEnd)
                .width(50.dp) // Vùng nhận diện long press
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
                        volumeInteractionKey++ // Reset bộ đếm 3 giây khi đang trượt
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
                        .fillMaxHeight(0.6f),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonGreen,
                        activeTrackColor = NeonGreen,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }
        }

        if (isPlayerScreenVisible) {
            PlayerScreen(
                viewModel = viewModel,
                onBackClick = {
                    isPlayerScreenVisible = false
                }
            )
        }
    }
}
