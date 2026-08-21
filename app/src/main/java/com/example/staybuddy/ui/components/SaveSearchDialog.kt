package com.example.staybuddy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.staybuddy.data.model.SavedSearch

/**
 * Bottom sheet dialog for saving the current search with a name.
 * Shows a summary of what's being saved and a "Notify me" toggle.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SaveSearchDialog(
    currentSearch: SavedSearch,
    onDismiss: () -> Unit,
    onSave: (SavedSearch) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var notifyEnabled by remember { mutableStateOf(false) }
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
                text = "Save this search",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Give your search a name to find it quickly later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Budget PGs near Parul Uni") },
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter summary chips
            Text(
                text = "Filters being saved:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("₹${currentSearch.priceMin.toInt()} – ₹${currentSearch.priceMax.toInt()}") }
                )
                if (currentSearch.roomTypes.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(currentSearch.roomTypes.joinToString()) }
                    )
                }
                if (currentSearch.gender != "Any") {
                    AssistChip(
                        onClick = {},
                        label = { Text(currentSearch.gender) }
                    )
                }
                if (currentSearch.city.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(currentSearch.city) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notify toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notify me",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Get alerts when new listings match this search",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notifyEnabled,
                    onCheckedChange = { notifyEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(currentSearch.copy(name = name.trim(), notifyEnabled = notifyEnabled))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("Save")
            }
        }
    }
}
