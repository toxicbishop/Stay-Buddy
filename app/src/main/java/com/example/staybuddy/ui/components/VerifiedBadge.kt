package com.example.staybuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.theme.VerifiedTeal

/**
 * Trust mark for listings checked by StayBuddy. Uses the brand teal so it
 * reads as part of the product, not a social-media checkmark.
 */
@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    showLabel: Boolean = false
) {
    if (showLabel) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraSmall,
            color = Color.White.copy(alpha = 0.92f),
            contentColor = VerifiedTeal
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    modifier = Modifier.size(size)
                )
                Text(
                    text = "Verified",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    } else {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Verified",
            tint = VerifiedTeal,
            modifier = modifier.size(size)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVerifiedBadge() {
    StayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VerifiedBadge(showLabel = true)
            VerifiedBadge(showLabel = false)
        }
    }
}
