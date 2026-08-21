package com.example.staybuddy.ui.screens.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.staybuddy.data.model.PgListing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.preview.PreviewMockData
import com.example.staybuddy.utils.ResponsiveUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    onNavigateToAddListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToInquiries: () -> Unit,
    viewModel: OwnerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var listingToDelete by remember { mutableStateOf<PgListing?>(null) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Business",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToAddListing()
                },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Listing", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        val docPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            uri?.let { viewModel.uploadVerificationDocument(it) }
        }

        if (uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
                contentPadding = PaddingValues(ResponsiveUtils.screenPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(3) {
                    com.example.staybuddy.ui.components.OwnerPropertyCardSkeleton()
                }
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                com.example.staybuddy.ui.components.ErrorBanner(
                    message = uiState.error!!,
                    onRetry = { viewModel.clearMessages() }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // Stats Grid — Compact 2x2 Analytics Layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Total PGs",
                            value = uiState.totalListings.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Active Live",
                            value = uiState.activeListings.toString(),
                            color = com.example.staybuddy.ui.theme.SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pendingCount = uiState.inquiries.count { it.status == "PENDING" }
                        StatCard(
                            title = "Inquiries",
                            value = if (pendingCount > 0) "$pendingCount New" else "0",
                            color = if (pendingCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Default.MarkEmailUnread,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToInquiries() }
                        )
                        StatCard(
                            title = "Tenant Views",
                            value = "${uiState.totalViews}",
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.Visibility,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Owner Verification Card (Persistently dismissable when verified)
                val user = uiState.currentUser
                val isVerified = user?.isOwnerVerified == true || user?.verificationStatus == "VERIFIED"
                val isPending = user?.verificationStatus == "PENDING"
                val isRejected = user?.verificationStatus == "REJECTED"

                if (!uiState.isBannerDismissed || !isVerified) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isVerified -> com.example.staybuddy.ui.theme.VerifiedTeal.copy(alpha = 0.12f)
                                isPending -> com.example.staybuddy.ui.theme.WarningAmber.copy(alpha = 0.12f)
                                isRejected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = when {
                                isVerified -> com.example.staybuddy.ui.theme.VerifiedTeal
                                isPending -> com.example.staybuddy.ui.theme.WarningAmber
                                isRejected -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = when {
                                            isVerified -> "Verified Owner Profile"
                                            isPending -> "Verification Under Review"
                                            isRejected -> "Verification Resubmission Needed"
                                            else -> "Get Verified Owner Badge"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isVerified) {
                                        com.example.staybuddy.ui.components.VerifiedBadge(showLabel = false)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        isVerified -> "Your owner profile is verified! All your listings feature the official Verified mark."
                                        isPending -> "Your document was submitted and is being reviewed by StayBuddy Admin."
                                        isRejected -> "Your uploaded document could not be verified. Please upload a clear electricity bill or deed."
                                        else -> "Upload electricity bill or property deed to get a verified badge and 3x tenant inquiries."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isVerified) {
                                IconButton(
                                    onClick = { viewModel.dismissVerifiedBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss banner",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else if (!isPending) {
                                Button(
                                    onClick = { docPickerLauncher.launch("image/*") },
                                    enabled = !uiState.isUploadingDoc,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (uiState.isUploadingDoc) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text(if (isRejected) "Re-upload" else "Upload Proof", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Manage Listings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 6.dp)
                )
                
                if (uiState.listings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "You haven't added any properties yet.\nTap 'Add Listing' to get started!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = ResponsiveUtils.screenPadding(), end = ResponsiveUtils.screenPadding(), bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(uiState.listings, key = { _, it -> it.listingId }) { index, listing ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                            ) {
                                OwnerPropertyCard(
                                    listing = listing,
                                    onClick = { onNavigateToDetail(listing.listingId) },
                                    onEditClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNavigateToEditListing(listing.listingId)
                                    },
                                    onToggleStatus = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleListingActiveStatus(listing)
                                    },
                                    onDeleteClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        listingToDelete = listing
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (listingToDelete != null) {
        AlertDialog(
            onDismissRequest = { listingToDelete = null },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete '${listingToDelete?.title}'? This action cannot be undone, and all images will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        listingToDelete?.let { viewModel.deleteListing(it.listingId) }
                        listingToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { listingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun OwnerPropertyCard(
    listing: PgListing,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val rawImageUrl = if (listing.images.isNotEmpty()) listing.images[0] else null
                val imageUrl = com.example.staybuddy.utils.ImageUtils.optimizeCloudinaryUrl(rawImageUrl)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Status Badge - sleek pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = (if (listing.isActive) com.example.staybuddy.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error).copy(alpha = 0.9f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (listing.isActive) "LIVE" else "HIDDEN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = listing.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (listing.isVerified) {
                                Spacer(Modifier.width(4.dp))
                                com.example.staybuddy.ui.components.VerifiedBadge(showLabel = false)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        com.example.staybuddy.ui.components.PriceTag(price = listing.price, large = false)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (listing.isActive) "Active" else "Hidden",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (listing.isActive) com.example.staybuddy.ui.theme.SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = listing.isActive,
                            onCheckedChange = { onToggleStatus() },
                            modifier = Modifier.scale(0.75f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Stats sub-row (Views + Available Beds)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${listing.viewCount} tenant views",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (listing.availableBeds > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${listing.availableBeds} beds available",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OwnerDashboardScreenPreview() {
    StayBuddyTheme {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp, tonalElevation = 1.dp) {
                    Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Text("Property Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Manage your listings and inquiries", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Add Listing")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total Listings", "3", MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        StatCard("Pending Inquiries", "5", MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Text("Your Properties", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(PreviewMockData.sampleListings) { listing ->
                    OwnerPropertyCard(
                        listing = listing,
                        onClick = {},
                        onEditClick = {},
                        onToggleStatus = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
    }
}
