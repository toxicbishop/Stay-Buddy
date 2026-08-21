package com.example.staybuddy.ui.screens.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerInquiriesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: OwnerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Navigate to chat when inquiry is accepted
    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.onChatNavigated()
        }
    }

    // Show success/error snackbar
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredInquiries = remember(uiState.inquiries, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> uiState.inquiries.filter { it.status == "PENDING" }
            "ACCEPTED" -> uiState.inquiries.filter { it.status == "ACCEPTED" }
            "REJECTED" -> uiState.inquiries.filter { it.status == "REJECTED" }
            else -> uiState.inquiries
        }
    }

    val pendingCount = remember(uiState.inquiries) { uiState.inquiries.count { it.status == "PENDING" } }
    val acceptedCount = remember(uiState.inquiries) { uiState.inquiries.count { it.status == "ACCEPTED" } }
    val rejectedCount = remember(uiState.inquiries) { uiState.inquiries.count { it.status == "REJECTED" } }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tenant Inquiries",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Filter Chips Horizontally Scrollable Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text("All (${uiState.inquiries.size})", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "PENDING",
                                onClick = { selectedFilter = "PENDING" },
                                label = { Text("Pending ($pendingCount)", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "ACCEPTED",
                                onClick = { selectedFilter = "ACCEPTED" },
                                label = { Text("Accepted ($acceptedCount)", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "REJECTED",
                                onClick = { selectedFilter = "REJECTED" },
                                label = { Text("Declined ($rejectedCount)", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            com.example.staybuddy.ui.components.GenericScreenShimmer(Modifier.padding(paddingValues))
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                com.example.staybuddy.ui.components.ErrorBanner(
                    message = uiState.error!!,
                    onRetry = { viewModel.clearMessages() }
                )
            }
        } else if (filteredInquiries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.inquiries.isEmpty()) "No inquiries yet.\nThey will appear here when students express interest."
                    else "No ${selectedFilter.lowercase()} inquiries found.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredInquiries) { inquiry ->
                    InquiryCard(
                        inquiry = inquiry,
                        onAccept = { viewModel.updateInquiryStatus(inquiry.inquiryId, "ACCEPTED", inquiry) },
                        onReject = { viewModel.updateInquiryStatus(inquiry.inquiryId, "REJECTED", inquiry) },
                        onSendQuickReply = { text -> viewModel.sendQuickReply(inquiry, text) }
                    )
                }
            }
        }
    }
}

@Composable
fun InquiryCard(
    inquiry: com.example.staybuddy.data.model.Inquiry,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSendQuickReply: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Student Name, Room Type, Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (inquiry.userPhotoUrl.isNotEmpty()) {
                        coil.compose.AsyncImage(
                            model = inquiry.userPhotoUrl,
                            contentDescription = "User avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null, 
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        val displayName = inquiry.userName.ifBlank { "Student" }
                        Text(
                            text = displayName, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = inquiry.roomType, 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Clean Status Badge
                Surface(
                    shape = CircleShape,
                    color = when(inquiry.status) {
                        "PENDING" -> com.example.staybuddy.ui.theme.WarningAmber.copy(alpha = 0.12f)
                        "ACCEPTED" -> com.example.staybuddy.ui.theme.SuccessGreen.copy(alpha = 0.12f)
                        "REJECTED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when(inquiry.status) {
                            "PENDING" -> "Pending"
                            "ACCEPTED" -> "Accepted"
                            "REJECTED" -> "Declined"
                            else -> inquiry.status
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when(inquiry.status) {
                            "PENDING" -> com.example.staybuddy.ui.theme.WarningAmber
                            "ACCEPTED" -> com.example.staybuddy.ui.theme.SuccessGreen
                            "REJECTED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
            
            // Move-in Request Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp), 
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Requested Move-in: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(inquiry.moveInDate))}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Message Quote
            if (inquiry.message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "\"${inquiry.message}\"",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Quick Reply Templates & Actions
            if (inquiry.status == "PENDING") {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "QUICK RESPONSES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        AssistChip(
                            onClick = { onSendQuickReply("Hi! Room is available. You are welcome to visit anytime today.") },
                            label = { Text("💬 Room Available", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onSendQuickReply("Hi! When would you like to schedule a visit to check the room?") },
                            label = { Text("📅 Schedule Visit", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Decline", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept & Chat", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (inquiry.status == "ACCEPTED") {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Chat with Student", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
