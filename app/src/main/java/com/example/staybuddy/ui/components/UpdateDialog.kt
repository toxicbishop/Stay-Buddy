package com.example.staybuddy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

import com.example.staybuddy.utils.UpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!updateInfo.isUpdateAvailable) return
    AlertDialog(
        onDismissRequest = { 
            if (!updateInfo.isMandatory && !updateInfo.isDownloading) onDismiss() 
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isMandatory && !updateInfo.isDownloading,
            dismissOnClickOutside = !updateInfo.isMandatory && !updateInfo.isDownloading
        ),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (updateInfo.isMandatory) "Update Required" else "Update Available",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = updateInfo.message.takeIf { it.isNotBlank() }
                        ?: "A new version of StayBuddy is available. Please update to get the latest features and bug fixes.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (updateInfo.isDownloading) {
                    if (updateInfo.downloadProgress > 0f) {
                        CircularProgressIndicator(progress = { updateInfo.downloadProgress })
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Downloading... ${(updateInfo.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Starting download...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!updateInfo.isDownloading) {
                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Now")
                }
            }
        },
        dismissButton = {
            if (!updateInfo.isMandatory && !updateInfo.isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
