package xyz.polyserv.memos.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.polyserv.memos.data.model.SyncStatus
import xyz.polyserv.memos.R
import androidx.compose.ui.res.stringResource

@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (icon, color, label) = when (syncStatus) {
        SyncStatus.SYNCED -> Triple(Icons.Default.CloudDone, Color(0xFF4CAF50), stringResource(id = R.string.synced))
        SyncStatus.PENDING -> Triple(Icons.Default.CloudOff, Color(0xFFFFC107), stringResource(id = R.string.waiting))
        SyncStatus.SYNCING -> Triple(Icons.Default.CloudSync, Color(0xFF2196F3), stringResource(id = R.string.sync))
        SyncStatus.FAILED -> Triple(Icons.Default.Error, Color(0xFFF44336), stringResource(id = R.string.error))
    }

    if (compact) {
        Row(
            modifier = modifier
                .background(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = modifier.size(16.dp)
            )
        }
    } else {
        Row(
            modifier = modifier
                .background(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun OfflineBanner(
    onSyncClick: () -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFC107).copy(alpha = 0.9f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = stringResource(id = R.string.offline),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(id = R.string.offline_mode),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            androidx.compose.material3.Button(
                onClick = onSyncClick,
                enabled = !isSyncing,
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = if (isSyncing) stringResource(id = R.string.syncing) else stringResource(id = R.string.sync),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
