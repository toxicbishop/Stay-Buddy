package com.example.staybuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.theme.SuccessGreen
import com.example.staybuddy.ui.theme.WarningAmber
import java.util.concurrent.TimeUnit

/**
 * How recently a listing was posted. Green = fresh, amber = ageing,
 * error red = stale. Colors come from the shared semantic palette.
 */
@Composable
fun FreshnessTag(
    createdAt: Long,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val days = TimeUnit.MILLISECONDS.toDays(now - createdAt)

    val (label, color) = when {
        days < 1 -> "Today" to SuccessGreen
        days == 1L -> "Yesterday" to SuccessGreen
        days < 7 -> "$days days ago" to SuccessGreen
        days < 14 -> "1 week ago" to WarningAmber
        days < 30 -> "${days / 7} weeks ago" to WarningAmber
        days < 60 -> "1 month ago" to MaterialTheme.colorScheme.error
        else -> "${days / 30} months ago" to MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFreshnessTag() {
    StayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val now = System.currentTimeMillis()
            FreshnessTag(createdAt = now)
            FreshnessTag(createdAt = now - TimeUnit.DAYS.toMillis(8))
            FreshnessTag(createdAt = now - TimeUnit.DAYS.toMillis(40))
        }
    }
}
