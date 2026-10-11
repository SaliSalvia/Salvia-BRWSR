package com.browxer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browxer.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    val wifiOnly: StateFlow<Boolean> = settings.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val maxParallel: StateFlow<Int> = settings.maxSimultaneousDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { settings.setWifiOnly(enabled) }
    }

    fun setMaxParallel(count: Int) {
        viewModelScope.launch { settings.setMaxSimultaneousDownloads(count) }
    }
}
