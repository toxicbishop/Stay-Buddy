package com.example.staybuddy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.staybuddy.data.model.SavedSearch

/**
 * Bottom sheet listing all saved searches. Supports tap-to-apply, swipe-to-delete,
 * and per-item notification toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSearchesSheet(
    savedSearches: List<SavedSearch>,
    onDismiss: () -> Unit,
    onSelect: (SavedSearch) -> Unit,
    onDelete: (String) -> Unit,
    onToggleNotification: (String, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = "Saved Searches",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (savedSearches.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.BookmarkBorder,
                    title = "No saved searches",
                    message = "Save a search from the filter sheet to find it here."
                )
            } else {
                savedSearches.forEach { search ->
                    SavedSearchItem(
                        search = search,
                        onSelect = { onSelect(search) },
                        onDelete = { onDelete(search.id) },
                        onToggleNotification = { enabled ->
                            onToggleNotification(search.id, enabled)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SavedSearchItem(
    search: SavedSearch,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onToggleNotification: (Boolean) -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = search.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Filter summary
                val summary = buildList {
                    add("₹${search.priceMin.toInt()} – ₹${search.priceMax.toInt()}")
                    if (search.roomTypes.isNotEmpty()) add(search.roomTypes.joinToString())
                    if (search.gender != "Any") add(search.gender)
                    if (search.city.isNotBlank()) add(search.city)
                }.take(3).joinToString(" · ")
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onToggleNotification(!search.notifyEnabled) }) {
                Icon(
                    imageVector = if (search.notifyEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = if (search.notifyEnabled) "Disable notifications" else "Enable notifications",
                    tint = if (search.notifyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
