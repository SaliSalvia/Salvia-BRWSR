package com.browxer.feature.download_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.browxer.core.domain.MediaCandidate
import com.browxer.feature.browser.QualitySheetState

private val NeonPurple = Color(0xFF8A2BE2)
private val HotPink = Color(0xFFFF1493)
private val SheetBg = Color(0xFF141414)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaQualitySelectionSheet(
    state: QualitySheetState,
    onDismiss: () -> Unit,
    onDownload: (MediaCandidate, String) -> Unit
) {
    var fileName by remember { mutableStateOf(state.mainCandidate.suggestedName) }
    var renameOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(state.allCandidates.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            AsyncImage(
                model = state.mainCandidate.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(fileName, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                (selected?.resolution ?: "unknown") + " · " + formatSize(selected?.sizeBytes ?: -1),
                style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            // Honest unsupported state — DRM / Live / Encrypted / DASH-unsupported
            if (state.isUnsupported) {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color(0x33FF1493), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "⚠ " + state.errorMessage.orEmpty(),
                        color = HotPink, modifier = Modifier.padding(12.dp))
                }
            }

            if (state.isLoading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(color = NeonPurple, modifier = Modifier.fillMaxWidth())
            }

            if (state.allCandidates.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded, { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.let { it.resolution + " · " + formatSize(it.sizeBytes) }
                            ?: "Quality",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Quality") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        state.allCandidates.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.resolution + " · " + formatSize(c.sizeBytes)) },
                                onClick = { selected = c; expanded = false }
                            )
                        }
                    }
                }
            }

            TextButton(onClick = { renameOpen = !renameOpen }) {
                Text("✏️ Rename", color = NeonPurple) }
            if (renameOpen) {
                OutlinedTextField(
                    value = fileName, onValueChange = { fileName = it },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { selected?.let { onDownload(it, fileName) } },
                enabled = !state.isUnsupported,   // never fake a download
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("⬇ DOWNLOAD",
                    style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "unknown size"
    bytes < 1 shl 20 -> String.format("%.1f KB", bytes / 1024f)
    else -> String.format("%.1f MB", bytes / 1048576f)
}
