package com.browxer.feature.download_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.browxer.core.common.BrowxerLocale
import com.browxer.core.domain.MediaCandidate
import com.browxer.core.ui.QualitySheetState
import com.browxer.core.ui.theme.HotPink
import com.browxer.core.ui.theme.NeonPurple
import com.browxer.core.ui.theme.Surface1

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

    val unknownRes = stringResource(R.string.sheet_unknown_res)
    val qualityLabel = stringResource(R.string.sheet_quality_label)
    val sizeLabel = formatSizeLabel(selected?.sizeBytes ?: -1L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
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
                (selected?.resolution ?: unknownRes) + " - " + sizeLabel,
                style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            // Honest unsupported state — DRM / Live / Encrypted / DASH-unsupported
            if (state.isUnsupported) {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color(0x33FF1493), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "! " + state.errorMessage.orEmpty(),
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
                        value = selected?.let { it.resolution + " - " + formatSizeLabel(it.sizeBytes) }
                            ?: qualityLabel,
                        onValueChange = {}, readOnly = true,
                        label = { Text(qualityLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        state.allCandidates.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.resolution + " - " + formatSizeLabel(c.sizeBytes)) },
                                onClick = { selected = c; expanded = false }
                            )
                        }
                    }
                }
            }

            TextButton(onClick = { renameOpen = !renameOpen }) {
                Text(stringResource(R.string.sheet_rename), color = NeonPurple) }
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
                Text(stringResource(R.string.sheet_download_action),
                    style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Locale-aware size: Formatter.formatShortFileSize renders native digits and
 * units automatically (1234 -> "1.2 MB" / Persian digits in fa). Never
 * hardcode "%.1f MB" — this is the single formatting path for the sheet.
 */
@Composable
private fun formatSizeLabel(bytes: Long): String {
    val context = LocalContext.current
    val unknown = stringResource(R.string.sheet_unknown_size)
    return remember(bytes, unknown) {
        if (bytes <= 0) unknown else BrowxerLocale.formatFileSize(context, bytes)
    }
}
