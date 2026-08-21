package com.example.staybuddy.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.staybuddy.data.model.Promotion

/**
 * A subtle, dismissable contextual banner for the home screen.
 * Shows city-specific info (new listings, seasonal tips, etc.).
 * Auto-hides after 5 seconds. Not an ad — it's helpful information.
 */
@Composable
fun PromotionBanner(
    promotion: Promotion?,
    onDismiss: () -> Unit,
    onPromotionClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (promotion == null) return

    // Auto-dismiss after 8 seconds
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(promotion.id) {
        visible = true
        kotlinx.coroutines.delay(8000)
        visible = false
        kotlinx.coroutines.delay(400) // Wait for exit animation
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable {
                    visible = false
                    onPromotionClick(promotion.deepLink)
                },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Text(
                    text = promotion.icon,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = promotion.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (promotion.subtitle.isNotBlank()) {
                        Text(
                            text = promotion.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                }

                // Dismiss
                IconButton(
                    onClick = {
                        visible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
