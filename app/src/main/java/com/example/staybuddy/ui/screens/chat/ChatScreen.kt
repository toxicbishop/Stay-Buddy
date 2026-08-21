package com.example.staybuddy.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.staybuddy.data.model.Message
import com.example.staybuddy.data.model.RoommatePost
import com.example.staybuddy.ui.components.mouseWheelScroll
import com.example.staybuddy.ui.preview.PreviewMockData
import com.example.staybuddy.ui.theme.StayBuddyTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    chatId: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val sendCurrentMessage = {
        val trimmed = messageText.trim()
        if (trimmed.isNotBlank() && !uiState.isSending) {
            viewModel.sendMessage(trimmed)
            messageText = ""
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.isOtherUserTyping, uiState.isSending) {
        if (uiState.messages.isNotEmpty()) {
            kotlinx.coroutines.delay(80)
            val lastVisibleItem = listState.layoutInfo.totalItemsCount - 1
            if (lastVisibleItem >= 0) {
                listState.animateScrollToItem(lastVisibleItem)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    androidx.compose.runtime.DisposableEffect(chatId) {
        com.example.staybuddy.utils.ActiveSessionManager.currentActiveChatId = chatId
        onDispose {
            if (com.example.staybuddy.utils.ActiveSessionManager.currentActiveChatId == chatId) {
                com.example.staybuddy.utils.ActiveSessionManager.currentActiveChatId = null
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatHeader(
                name = uiState.otherUserName.ifBlank { "Chat" },
                avatarUrl = uiState.otherUserAvatarUrl,
                isTyping = uiState.isOtherUserTyping,
                isMatchConfirmed = uiState.isMatchConfirmed,
                isBlocked = uiState.isBlocked,
                onNavigateBack = onNavigateBack,
                onBlockUser = { viewModel.blockUser() },
                onUnblockUser = { viewModel.unblockUser() },
                onClearChat = { viewModel.clearChat() },
                onDeleteChat = { showDeleteChatDialog = true },
                onReportUser = { showReportDialog = true }
            )
        },
        bottomBar = {
            if (uiState.isBlocked) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You blocked this user",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.TextButton(onClick = { viewModel.unblockUser() }) {
                            Text("Unblock")
                        }
                    }
                }
            } else if (uiState.isBlockedByOther) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "You cannot reply to this conversation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                MessageComposer(
                    value = messageText,
                    isSending = uiState.isSending,
                    onValueChange = {
                        messageText = it
                        if (it.isNotBlank()) viewModel.onTyping()
                    },
                    onSend = sendCurrentMessage
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    ChatContent(
                        uiState = uiState,
                        listState = listState,
                        onConfirmMatch = viewModel::confirmMatch,
                        onSuggestedMessage = {
                            messageText = it
                            viewModel.onTyping()
                        },
                        onDeleteRequest = { messageToDelete = it }
                    )
                }
            }
        }

        messageToDelete?.let { message ->
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = { Text("Delete message?") },
                text = { Text("This message will be removed from the conversation.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteMessage(message.messageId)
                        messageToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = { Text("Report User") },
                text = {
                    Column {
                        Text("Please let us know why you are reporting this user.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            placeholder = { Text("Reason for reporting") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.reportUser(reportReason)
                            showReportDialog = false
                            reportReason = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("User reported.")
                            }
                        },
                        enabled = reportReason.isNotBlank()
                    ) {
                        Text("Submit Report", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteChatDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteChatDialog = false },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = { Text("Delete Chat?") },
                text = { Text("Are you sure you want to delete this chat? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteChat()
                        showDeleteChatDialog = false
                        onNavigateBack()
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteChatDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatHeader(
    name: String,
    avatarUrl: String,
    isTyping: Boolean,
    isMatchConfirmed: Boolean,
    isBlocked: Boolean,
    onNavigateBack: () -> Unit,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit,
    onClearChat: () -> Unit,
    onDeleteChat: () -> Unit,
    onReportUser: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            ProfileAvatar(
                name = name,
                avatarUrl = avatarUrl,
                modifier = Modifier.size(44.dp),
                isHighlighted = isTyping || isMatchConfirmed
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        isTyping -> "Typing..."
                        isMatchConfirmed -> "Match confirmed"
                        else -> "Roommate conversation"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (isBlocked) {
                        DropdownMenuItem(
                            text = { Text("Unblock") },
                            onClick = {
                                menuExpanded = false
                                onUnblockUser()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Block") },
                            onClick = {
                                menuExpanded = false
                                onBlockUser()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Report") },
                        onClick = {
                            menuExpanded = false
                            onReportUser()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Chat") },
                        onClick = {
                            menuExpanded = false
                            onClearChat()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Chat") },
                        onClick = {
                            menuExpanded = false
                            onDeleteChat()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onConfirmMatch: () -> Unit,
    onSuggestedMessage: (String) -> Unit,
    onDeleteRequest: (Message) -> Unit
) {
    val groupedMessages = uiState.messages.groupBy { formatMessageDate(it.timestamp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .mouseWheelScroll(listState),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.roommatePost != null || uiState.confirmedBy.isNotEmpty() || uiState.isMatchConfirmed) {
            item {
                MatchContextCard(
                    post = uiState.roommatePost,
                    isMatchConfirmed = uiState.isMatchConfirmed,
                    hasCurrentUserConfirmed = uiState.confirmedBy.contains(uiState.currentUserId),
                    onConfirmMatch = onConfirmMatch
                )
            }
        }

        if (uiState.messages.isEmpty()) {
            item {
                EmptyConversation(onSuggestedMessage = onSuggestedMessage)
            }
        } else {
            groupedMessages.forEach { (date, messagesForDate) ->
                item {
                    DateDivider(date = date)
                }

                items(messagesForDate) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == uiState.currentUserId,
                        onLongPress = {
                            if (message.senderId == uiState.currentUserId && !message.isDeleted) {
                                onDeleteRequest(message)
                            }
                        }
                    )
                }
            }
        }

        if (uiState.isOtherUserTyping) {
            item {
                TypingIndicator()
            }
        }
    }
}

@Composable
private fun MatchContextCard(
    post: RoommatePost?,
    isMatchConfirmed: Boolean,
    hasCurrentUserConfirmed: Boolean,
    onConfirmMatch: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isMatchConfirmed) Icons.Default.CheckCircle else Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isMatchConfirmed -> "Roommate match confirmed"
                            hasCurrentUserConfirmed -> "Waiting for their confirmation"
                            else -> "Roommate match"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = post?.let {
                            val location = listOf(it.location, it.city).filter { part -> part.isNotBlank() }.joinToString(", ")
                            val price = if (it.priceShare > 0) "Rs ${String.format(Locale.getDefault(), "%,d", it.priceShare)}/mo" else "Price not listed"
                            listOf(location, it.roomType, price).filter { part -> part.isNotBlank() }.joinToString(" | ")
                        } ?: "Confirm when both sides agree to move ahead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(visible = post != null || !isMatchConfirmed) {
                Column {
                    post?.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (post?.location?.isNotBlank() == true) {
                            AssistChip(
                                onClick = {},
                                label = { Text(post.location) },
                                leadingIcon = {
                                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = onConfirmMatch,
                            enabled = !isMatchConfirmed && !hasCurrentUserConfirmed,
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = when {
                                    isMatchConfirmed -> "Confirmed"
                                    hasCurrentUserConfirmed -> "Pending"
                                    else -> "Confirm"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversation(onSuggestedMessage: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Start the conversation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Send a quick note and keep the stay details in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onSuggestedMessage("Hi, is this still available?") },
                label = { Text("Availability") }
            )
            AssistChip(
                onClick = { onSuggestedMessage("Can we schedule a visit?") },
                label = { Text("Visit") }
            )
        }
    }
}

@Composable
private fun DateDivider(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onLongPress: () -> Unit = {}
) {
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    val isDeleted = message.isDeleted

    val bubbleColor = when {
        isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        isCurrentUser -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f)
    }

    val textColor = when {
        isDeleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
        isCurrentUser -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val roundedShape = if (isCurrentUser) {
        MaterialTheme.shapes.medium.copy(bottomEnd = CornerSize(5.dp))
    } else {
        MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(5.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 316.dp)
                .clip(roundedShape)
                .background(bubbleColor)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (isDeleted) "This message was deleted" else message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = if (isDeleted) FontStyle.Italic else FontStyle.Normal
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 5.dp)
            ) {
                Text(
                    text = timeString,
                    color = textColor.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall
                )

                if (isCurrentUser && !isDeleted) {
                    val tickColor = if (message.isRead) Color(0xFF34B7F1) else textColor.copy(alpha = 0.72f)
                    val icon = when {
                        message.isSyncing -> Icons.Outlined.Schedule
                        message.isRead -> Icons.Default.DoneAll
                        else -> Icons.Default.Done
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = if (message.isSyncing) "Sending" else if (message.isRead) "Read" else "Sent",
                        modifier = Modifier.size(14.dp),
                        tint = tickColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SendingStatus() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sending",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1000; 0f at 0; 1f at 200; 0f at 400 },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1000; 0f at 200; 1f at 400; 0f at 600 },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1000; 0f at 400; 1f at 600; 0f at 800 },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
            Box(modifier = Modifier.size(6.dp).offset(y = (-4).dp * dot1).background(color, CircleShape))
            Box(modifier = Modifier.size(6.dp).offset(y = (-4).dp * dot2).background(color, CircleShape))
            Box(modifier = Modifier.size(6.dp).offset(y = (-4).dp * dot3).background(color, CircleShape))
        }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = value.isNotBlank() && !isSending

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("Message") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(if (canSend) 1f else 0.48f),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
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
                Text(
                    text = initialsFor(name),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun initialsFor(name: String): String {
    return name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "U" }
}

fun formatMessageDate(timestamp: Long): String {
    val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"

        messageDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            messageDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"

        else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatScreenPreview() {
    StayBuddyTheme {
        Scaffold(
            topBar = {
                ChatHeader(
                    name = "Rahul Sharma",
                    avatarUrl = "",
                    isTyping = false,
                    isMatchConfirmed = true,
                    isBlocked = false,
                    onNavigateBack = {},
                    onBlockUser = {},
                    onUnblockUser = {},
                    onClearChat = {},
                    onDeleteChat = {},
                    onReportUser = {}
                )
            },
            bottomBar = {
                MessageComposer(
                    value = "",
                    isSending = false,
                    onValueChange = {},
                    onSend = {}
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MatchContextCard(
                        post = PreviewMockData.sampleRoommatePosts.first(),
                        isMatchConfirmed = false,
                        hasCurrentUserConfirmed = false,
                        onConfirmMatch = {}
                    )
                }
                items(PreviewMockData.sampleMessages) { message ->
                    MessageBubble(message = message, isCurrentUser = message.senderId == "user_001")
                }
            }
        }
    }
}
