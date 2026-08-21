package com.example.staybuddy.ui.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.staybuddy.data.model.ChatRoom
import com.example.staybuddy.ui.preview.PreviewMockData
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.components.ChatListShimmer
import com.example.staybuddy.ui.components.ChatListRowShimmer
import com.example.staybuddy.ui.components.mouseWheelScroll
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showUnreadOnly by remember { mutableStateOf(false) }
    val chatListState = rememberLazyListState()

    val unreadTotal = uiState.chatRooms.sumOf { it.unreadCount[uiState.currentUserId] ?: 0 }
    val filteredRooms = uiState.chatRooms
        .filter { room ->
            val otherUserId = room.participants.firstOrNull { it != uiState.currentUserId } ?: ""
            val otherUserName = uiState.userNames[otherUserId] ?: "User"
            val matchesSearch = searchQuery.isBlank() ||
                otherUserName.contains(searchQuery, ignoreCase = true) ||
                room.lastMessage.contains(searchQuery, ignoreCase = true)
            val matchesUnread = !showUnreadOnly || (room.unreadCount[uiState.currentUserId] ?: 0) > 0
            matchesSearch && matchesUnread
        }
        .sortedByDescending { it.lastMessageTime }

    var selectedRoomForOptions by remember { mutableStateOf<ChatRoom?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Header ──
            ChatListHeader(
                searchQuery = searchQuery,
                unreadTotal = unreadTotal,
                showUnreadOnly = showUnreadOnly,
                onSearchChange = { searchQuery = it },
                onClearSearch = { searchQuery = "" },
                onToggleUnread = { showUnreadOnly = !showUnreadOnly }
            )

            // ── Content ──
            when {
                uiState.isLoading -> {
                    ChatListShimmer()
                }

                uiState.error != null -> {
                    ChatListEmptyState(
                        title = "Could not load messages",
                        subtitle = uiState.error ?: "Please try again in a moment."
                    )
                }

                uiState.chatRooms.isEmpty() -> {
                    ChatListEmptyState(
                        title = "No messages yet",
                        subtitle = "When you contact owners or roommates, conversations will appear here."
                    )
                }

                filteredRooms.isEmpty() -> {
                    ChatListEmptyState(
                        title = "No chats match",
                        subtitle = "Try a different search or turn off the unread filter."
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .mouseWheelScroll(chatListState),
                        state = chatListState,
                        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                    ) {
                        items(filteredRooms, key = { it.roomId }) { room ->
                            val otherUserId =
                                room.participants.firstOrNull { it != uiState.currentUserId } ?: ""
                            val hasNameLoaded = uiState.userNames.containsKey(otherUserId)
                            
                            if (!hasNameLoaded) {
                                ChatListRowShimmer()
                            } else {
                                val otherUserName = uiState.userNames[otherUserId] ?: "User"
                                val otherUserAvatar = uiState.userAvatars[otherUserId]
                                val unreadCount = room.unreadCount[uiState.currentUserId] ?: 0

                                ChatRoomItem(
                                    room = room,
                                    otherUserName = otherUserName,
                                    otherUserAvatar = otherUserAvatar,
                                    unreadCount = unreadCount,
                                    onClick = { onNavigateToChat(room.roomId) },
                                    onLongClick = { selectedRoomForOptions = room }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (selectedRoomForOptions != null) {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { selectedRoomForOptions = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Chat Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Mark as Read") },
                    leadingContent = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedRoomForOptions?.let {
                            viewModel.markAsRead(it.roomId)
                        }
                        selectedRoomForOptions = null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                
                ListItem(
                    headlineContent = { Text("Clear Chat") },
                    leadingContent = { Icon(Icons.Default.ClearAll, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedRoomForOptions?.let {
                            viewModel.clearChat(it.roomId)
                        }
                        selectedRoomForOptions = null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                
                ListItem(
                    headlineContent = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        selectedRoomForOptions?.let {
                            viewModel.deleteChat(it.roomId)
                        }
                        selectedRoomForOptions = null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

// ─── Header ───
@Composable
private fun ChatListHeader(
    searchQuery: String,
    unreadTotal: Int,
    showUnreadOnly: Boolean,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleUnread: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (unreadTotal > 0)
                            "$unreadTotal unread message${if (unreadTotal == 1) "" else "s"}"
                        else "All caught up ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unreadTotal > 0)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilterChip(
                    selected = showUnreadOnly,
                    onClick = onToggleUnread,
                    label = {
                        Text(
                            "Unread",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = if (showUnreadOnly) {
                        {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search conversations...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }

        // Bottom separator
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

// ─── Chat Room Item ───
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRoomItem(
    room: ChatRoom,
    otherUserName: String,
    otherUserAvatar: String?,
    unreadCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeString = formatChatListTime(room.lastMessageTime)
    val hasUnread = unreadCount > 0
    val isRoommateChat = room.roommatePostId.isNotBlank()

    val bgColor by animateColorAsState(
        targetValue = if (hasUnread)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else Color.Transparent,
        animationSpec = tween(300),
        label = "chatBg"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar with online-style accent
            Box {
                ChatAvatar(
                    name = otherUserName,
                    avatarUrl = otherUserAvatar,
                    isHighlighted = hasUnread,
                    modifier = Modifier.size(52.dp)
                )
                // Unread dot indicator on avatar
                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherUserName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message preview + badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (room.lastMessage.isNotBlank()) room.lastMessage else "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasUnread) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal
                    )

                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        UnreadBadge(count = unreadCount)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tags row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChatTag(
                        text = if (isRoommateChat) "Roommate" else "Listing",
                        color = if (isRoommateChat)
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        textColor = if (isRoommateChat)
                            MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                    if (room.confirmedBy.size >= 2 || room.isMatchConfirmed) {
                        ChatTag(
                            text = "Confirmed ✓",
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Divider between items
        HorizontalDivider(
            modifier = Modifier.padding(start = 86.dp, end = 20.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

// ─── Tag Chip ───
@Composable
private fun ChatTag(
    text: String,
    color: Color,
    textColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ─── Avatar ───
@Composable
private fun ChatAvatar(
    name: String,
    avatarUrl: String?,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = if (isHighlighted)
            MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                val initials = name.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")

                if (initials.isNotBlank()) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = if (isHighlighted)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Unread Badge ───
@Composable
private fun UnreadBadge(count: Int) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            modifier = Modifier
                .widthIn(min = 22.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Empty State ───
@Composable
private fun ChatListEmptyState(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                maxLines = 3
            )
        }
    }
}

// ─── Time Formatter ───
private fun formatChatListTime(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }

        messageDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            messageDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> {
            "Yesterday"
        }

        else -> {
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

// ─── Preview ───
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatListScreenPreview() {
    StayBuddyTheme {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ChatListHeader(
                    searchQuery = "",
                    unreadTotal = 2,
                    showUnreadOnly = false,
                    onSearchChange = {},
                    onClearSearch = {},
                    onToggleUnread = {}
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp)
                ) {
                    items(PreviewMockData.sampleChatRooms) { room ->
                        ChatRoomItem(
                            room = room,
                            otherUserName = if (room.roomId == "chat_001") "Rahul Sharma" else "Karan Mehta",
                            otherUserAvatar = null,
                            unreadCount = room.unreadCount["user_001"] ?: 0,
                            onClick = {},
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}
