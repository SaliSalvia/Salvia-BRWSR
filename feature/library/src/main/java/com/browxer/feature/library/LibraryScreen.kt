package com.browxer.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.browxer.core.database.DownloadEntity

private val NeonPurple = Color(0xFF8A2BE2)
private val DeepBlack = Color(0xFF0A0A0A)
private val CardBg = Color(0xFF141414)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit = {},
    onPlay: (Long) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val items by viewModel.downloads.collectAsState()

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.library_empty), color = Color.Gray)
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    DownloadCard(item = item, onPlay = onPlay, onDelete = viewModel::delete)
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadEntity,
    onPlay: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val statusLabel = when (item.status) {
        DownloadEntity.QUEUED -> stringResource(R.string.status_queued)
        DownloadEntity.DOWNLOADING -> stringResource(R.string.status_downloading)
        DownloadEntity.COMPLETED -> stringResource(R.string.status_completed)
        DownloadEntity.FAILED -> stringResource(R.string.status_failed)
        DownloadEntity.PAUSED -> stringResource(R.string.status_paused)
        else -> item.status
    }
    val statusColor = when (item.status) {
        DownloadEntity.COMPLETED -> NeonPurple
        DownloadEntity.FAILED -> Color(0xFFFF4444)
        else -> Color.Gray
    }

    Surface(color = CardBg, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(item.fileName, color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(statusLabel, color = statusColor,
                    style = MaterialTheme.typography.bodySmall)
                if (item.errorMessage != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(item.errorMessage.orEmpty(), color = Color(0xFFFF4444),
                        style = MaterialTheme.typography.bodySmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            if (item.status == DownloadEntity.DOWNLOADING) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    color = NeonPurple,
                    modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (item.status == DownloadEntity.COMPLETED) {
                    FilledTonalIconButton(onClick = { onPlay(item.id) }) {
                        Icon(Icons.Default.PlayArrow,
                            stringResource(R.string.library_play), tint = NeonPurple)
                    }
                }
                IconButton(onClick = { onDelete(item.id) }) {
                    Icon(Icons.Default.Delete,
                        stringResource(R.string.library_delete), tint = Color.Gray)
                }
            }
        }
    }
}
