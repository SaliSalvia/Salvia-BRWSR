package com.browxer.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val NeonPurple = Color(0xFF8A2BE2)
private val HotPink = Color(0xFFFF1493)
private val DeepBlack = Color(0xFF0A0A0A)
private val CardBg = Color(0xFF141414)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val maxParallel by viewModel.maxParallel.collectAsState()
    val context = LocalContext.current
    val version = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = CardBg, shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_wifi_only),
                            color = Color.White)
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = viewModel::setWifiOnly,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HotPink,
                            checkedTrackColor = NeonPurple))
                }
            }

            Surface(color = CardBg, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.settings_max_parallel), color = Color.White)
                    Text(maxParallel.toString(), color = NeonPinkLabel,
                        style = MaterialTheme.typography.titleLarge)
                    Slider(
                        value = maxParallel.toFloat(),
                        onValueChange = { viewModel.setMaxParallel(it.toInt().coerceIn(1, 5)) },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = HotPink,
                            activeTrackColor = NeonPurple))
                }
            }

            Surface(color = CardBg, shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_version),
                        color = Color.White, modifier = Modifier.weight(1f))
                    Text("Browxer " + version, color = Color.Gray)
                }
            }
        }
    }
}

private val NeonPinkLabel = Color(0xFFFF1493)
