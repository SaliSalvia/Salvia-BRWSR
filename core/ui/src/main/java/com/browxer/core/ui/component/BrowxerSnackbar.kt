package com.browxer.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browxer.core.ui.theme.HotPink
import com.browxer.core.ui.theme.Surface3
import com.browxer.core.ui.theme.TextPrimary

/**
 * Neon-accent Snackbar host. Critical feedback never uses Toast —
 * callers show messages through this host so actions stay tappable.
 */
@Composable
fun BrowxerSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val actionLabel = data.visuals.actionLabel
        Snackbar(
            containerColor = Surface3,
            contentColor = TextPrimary,
            shape = MaterialTheme.shapes.medium,
            action = if (actionLabel != null) {
                {
                    Text(
                        actionLabel,
                        color = HotPink,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { data.performAction() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            } else {
                null
            }
        ) {
            Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
