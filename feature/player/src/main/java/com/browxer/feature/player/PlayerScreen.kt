package com.browxer.feature.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.browxer.core.data.DownloadRepository
import com.browxer.core.database.DownloadEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val loading: Boolean = true,
    val mediaUri: String? = null,
    val title: String = "",
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val downloads: DownloadRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val entity: DownloadEntity? = downloads.getById(id)
            if (entity == null || entity.filePath.isNullOrBlank() ||
                entity.status != DownloadEntity.COMPLETED
            ) {
                _state.value = PlayerUiState(loading = false,
                    error = "media_not_found")
            } else {
                _state.value = PlayerUiState(loading = false,
                    mediaUri = entity.filePath, title = entity.fileName)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    downloadId: Long,
    onBack: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(downloadId) { viewModel.load(downloadId) }

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(state.mediaUri) {
        state.mediaUri?.let { path ->
            player.setMediaItem(MediaItem.fromUri(Uri.parse(path)))
            player.prepare()
            player.playWhenReady = true
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = {
                    Text(state.title, color = Color.White, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141414))
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()
            .background(Color.Black), contentAlignment = Alignment.Center) {
            when {
                state.loading -> CircularProgressIndicator(color = Color(0xFF8A2BE2))
                state.error != null -> Text(
                    stringResource(R.string.player_not_found), color = Color.Gray)
                else -> AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    update = { view -> view.player = player }
                )
            }
        }
    }
}
