package com.example.staybuddy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.staybuddy.notifications.InAppNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import com.example.staybuddy.ui.theme.StayBuddyTheme

@Composable
fun InAppNotificationBanner(
    notification: InAppNotification?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onClick: (InAppNotification) -> Unit
) {
    var exitTransition by remember { mutableStateOf(slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400))) }
    
    AnimatedVisibility(
        visible = isVisible && notification != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 400)
        ),
        exit = exitTransition
    ) {
        if (notification != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            LaunchedEffect(notification, isPressed) {
                if (!isPressed) {
                    kotlinx.coroutines.delay(4000)
                    exitTransition = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400))
                    onDismiss()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = { onClick(notification) }
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                if (dragAmount.y < -5f) {
                                    exitTransition = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300))
                                    onDismiss()
                                } else if (dragAmount.x > 10f) {
                                    exitTransition = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                                    onDismiss()
                                } else if (dragAmount.x < -10f) {
                                    exitTransition = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300))
                                    onDismiss()
                                }
                            }
                        },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSupportTicket = notification.routeType == "ticket" || notification.routeType == "support" || notification.action == "ticket" || notification.action == "support"
                        
                        if (isSupportTicket) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ConfirmationNumber,
                                    contentDescription = "Support Ticket",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else if (!notification.avatarUrl.isNullOrBlank()) {
                            val optimizedUrl = com.example.staybuddy.utils.ImageUtils.optimizeCloudinaryUrl(notification.avatarUrl, width = 150)
                            coil.compose.SubcomposeAsyncImage(
                                model = optimizedUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = notification.title.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = notification.title.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = notification.title.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInAppNotificationBanner() {
    StayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            InAppNotificationBanner(
                notification = InAppNotification(
                    title = "New Message",
                    message = "Hi! Is the room still available?",
                    action = "CHAT",
                    avatarUrl = ""
                ),
                isVisible = true,
                onDismiss = {},
                onClick = {}
            )
        }
    }
}
