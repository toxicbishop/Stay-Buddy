package com.example.staybuddy.ui.screens.roommate

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.staybuddy.R
import com.example.staybuddy.data.model.RoommatePost
import com.example.staybuddy.data.model.RoommatePostType
import com.example.staybuddy.ui.components.FreshnessTag
import com.example.staybuddy.ui.components.PriceTag
import com.example.staybuddy.ui.components.RoommatePostCardSkeleton
import com.example.staybuddy.ui.components.mouseWheelScroll
import com.example.staybuddy.ui.theme.SuccessGreen
import com.example.staybuddy.ui.theme.WarningAmber
import com.example.staybuddy.util.WhatsAppUtils
import com.example.staybuddy.utils.ResponsiveUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoommateListScreen(
    onNavigateToAddPost: () -> Unit,
    onNavigateToEditPost: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToQuiz: () -> Unit,
    viewModel: RoommateListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedPostForMatch by remember { mutableStateOf<RoommatePost?>(null) }
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = com.example.staybuddy.ui.components.LocalSnackbarHostState.current
    val loadingListState = rememberLazyListState()
    val roommateListState = rememberLazyListState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.onChatNavigated()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
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
                            .padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Find Your Roommate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showFilterSheet = true
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Segment Switcher (Looking for Room vs Have a Room) - Enabled dynamically via RemoteConfig
                    if (uiState.isSeekerModeEnabled) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 4.dp)
                        ) {
                            SegmentedButton(
                                selected = uiState.selectedPostType == RoommatePostType.OFFER,
                                onClick = { viewModel.onPostTypeSelected(RoommatePostType.OFFER) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Has Room", fontWeight = FontWeight.Bold)
                            }
                            SegmentedButton(
                                selected = uiState.selectedPostType == RoommatePostType.SEEK,
                                onClick = { viewModel.onPostTypeSelected(RoommatePostType.SEEK) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Needs Room", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Search Bar - Sleek compact pill design (42dp height)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 4.dp)
                            .height(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search by city, area, name...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = viewModel::onSearchQueryChanged,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            if (uiState.searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.onSearchQueryChanged("") }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToAddPost()
                },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Post Yours", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        val screenPadding = ResponsiveUtils.screenPadding()
        val listContentPadding = PaddingValues(
            start = screenPadding,
            top = 12.dp,
            end = screenPadding,
            bottom = 120.dp // Ensures FAB doesn't cover last item or action buttons
        )

        if (uiState.isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .mouseWheelScroll(loadingListState),
                state = loadingListState,
                contentPadding = listContentPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(3) {
                    RoommatePostCardSkeleton()
                }
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (uiState.posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = "No roommate posts yet.\nBe the first to post!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.filteredPosts.isEmpty() && uiState.posts.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No roommate posts match your filters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .mouseWheelScroll(roommateListState),
                        state = roommateListState,
                        contentPadding = listContentPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Compatibility Quiz Banner
                        if (uiState.currentUserProfile != null && uiState.currentUserProfile!!.quizResults.isEmpty()) {
                            item {
                                CompatibilityQuizBanner(onNavigateToQuiz)
                            }
                        }

                        items(uiState.filteredPosts, key = { it.postId }) { post ->
                            val matchScore = uiState.matchScores[post.userId]
                            RoommatePostCard(
                                post = post,
                                isOwnedByMe = post.userId == uiState.currentUserId,
                                matchScore = matchScore,
                                targetAnchor = uiState.targetAnchor,
                                onMatchScoreClick = { selectedPostForMatch = post },
                                onEditClick = { onNavigateToEditPost(post.postId) },
                                onMessageClick = { viewModel.createChat(post.userId, post.postId) }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPostForMatch?.let { post ->
        val score = uiState.matchScores[post.userId] ?: 0
        RoommateMatchBottomSheet(
            post = post,
            matchScore = score,
            onDismiss = { selectedPostForMatch = null },
            onStartChat = { viewModel.createChat(post.userId, post.postId) }
        )
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            RoommateFilterSheetContent(
                uiState = uiState,
                onMaxBudgetChange = viewModel::onMaxBudgetChanged,
                onGenderPreferenceChange = viewModel::onGenderPreferenceChanged,
                onSortByMatchChange = viewModel::onSortByMatchChanged,
                onApply = { showFilterSheet = false }
            )
        }
    }
}

@Composable
fun CompatibilityQuizBanner(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Unlock Compatibility Scores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Take a quick 2-minute lifestyle quiz to see how well you match with potential roommates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClick,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Start Quiz", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun RoommateFilterSheetContent(
    uiState: RoommateListUiState,
    onMaxBudgetChange: (Float) -> Unit,
    onGenderPreferenceChange: (String) -> Unit,
    onSortByMatchChange: (Boolean) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text(
            text = "Refine Roommates",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Max Budget Slider
        Text(
            text = "Max Monthly Share: ₹${uiState.maxBudget.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = uiState.maxBudget,
            onValueChange = onMaxBudgetChange,
            valueRange = 0f..100000f,
            steps = 100,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Gender Preference
        Text(
            text = "Preferred Gender",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Any", "Male", "Female", "Other").forEach { gender ->
                FilterChip(
                    selected = uiState.genderPreference == gender,
                    onClick = { onGenderPreferenceChange(gender) },
                    label = { Text(gender) },
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sort by Compatibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Sort by Compatibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Put best matches at the top",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.sortByMatch,
                onCheckedChange = onSortByMatchChange,
                thumbContent = if (uiState.sortByMatch) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else null
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Show Results", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RoommatePostCard(
    post: RoommatePost,
    isOwnedByMe: Boolean,
    matchScore: Int? = null,
    targetAnchor: com.example.staybuddy.domain.model.TargetAnchor? = null,
    onMatchScoreClick: (() -> Unit)? = null,
    onEditClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Press animation — springy scale like PgListingCard
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    val distanceText: String? = remember(post.latitude, post.longitude, targetAnchor) {
        if (post.latitude != null && post.longitude != null && targetAnchor != null) {
            val distKm = com.example.staybuddy.utils.LocationUtils.calculateDistance(
                targetAnchor.lat, targetAnchor.lon,
                post.latitude!!, post.longitude!!
            )
            val icon = if (targetAnchor.type == com.example.staybuddy.domain.model.AnchorType.UNIVERSITY) "🎓" else "📍"
            "$icon ${String.format("%.1f", distKm)} km"
        } else null
    }

    // Urgency: last bed available
    val isLastBed = post.postType == RoommatePostType.OFFER && post.availableBeds == 1

    // Effective match: hide 0% — it's meaningless
    val effectiveMatch = if (matchScore != null && matchScore > 0 && !isOwnedByMe) matchScore else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = { onMessageClick() }
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {

            // ── Header ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar — 48dp, warm container
                Box(modifier = Modifier.size(48.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (post.userProfileImage.isNotEmpty()) {
                            AsyncImage(
                                model = post.userProfileImage,
                                contentDescription = "${post.userName}'s photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Name + meta lines
                Column(modifier = Modifier.weight(1f)) {
                    // Line 1: Name
                    Text(
                        text = post.userName.ifBlank { "Anonymous" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    // Line 2: Location & Distance
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = buildString {
                                append(post.location.ifBlank { post.city })
                                if (distanceText != null) {
                                    append(" · ")
                                    append(distanceText)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Line 3: Freshness & Gender Preference
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FreshnessTag(createdAt = post.createdAt)

                        if (post.genderPreference.isNotBlank() && post.genderPreference != "Any") {
                            val genderSymbol = when (post.genderPreference.lowercase()) {
                                "male"   -> "♂"
                                "female" -> "♀"
                                else     -> "⚧"
                            }
                            Text(
                                text = "$genderSymbol ${post.genderPreference}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Price + Match (right column)
                Column(horizontalAlignment = Alignment.End) {
                    PriceTag(price = post.priceShare, large = true)

                    if (effectiveMatch != null) {
                        Spacer(Modifier.height(6.dp))
                        val matchColor = when {
                            effectiveMatch >= 80 -> SuccessGreen
                            effectiveMatch >= 50 -> WarningAmber
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            onClick = { onMatchScoreClick?.invoke() },
                            shape = MaterialTheme.shapes.extraSmall,
                            color = matchColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "⚡ $effectiveMatch% match",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = matchColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // ── Description (people-first — above chips) ───────────
            if (post.description.isNotBlank()) {
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 76.dp, end = 16.dp, top = 6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Info Chips (horizontal scroll) ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Post type badge — always first, prominent
                val isOffer = post.postType == RoommatePostType.OFFER
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isOffer)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = if (isOffer) "🏠 Has Room" else "🔍 Needs Room",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOffer)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Beds chip — urgency-aware
                if (isOffer && post.totalBeds > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (isLastBed)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = if (isLastBed) "🛏 Last bed!" else "🛏 ${post.availableBeds}/${post.totalBeds}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isLastBed) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLastBed)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Key lifestyle chips — max 4 to avoid overwhelming
                if (post.foodPreference.isNotBlank() && post.foodPreference != "Any") {
                    RoommateChip("🥦 ${post.foodPreference}")
                }
                if (post.smokingDrinking.isNotBlank() && post.smokingDrinking != "Any") {
                    RoommateChip("🚭 ${post.smokingDrinking}")
                }
                if (post.petFriendly.isNotBlank() && post.petFriendly != "Any") {
                    RoommateChip("🐾 ${post.petFriendly}")
                }
                if (post.sleepSchedule.isNotBlank() && post.sleepSchedule != "Any") {
                    RoommateChip("🌙 ${post.sleepSchedule}")
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Action Bar (warm tinted footer) ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwnedByMe) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditClick()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Post", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    // Call
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            WhatsAppUtils.makePhoneCall(context, post.userPhone)
                        },
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(18.dp))
                    }

                    // WhatsApp
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val msg = "Hi ${post.userName}, I saw your post on StayBuddy for a roommate in ${post.location}, ${post.city}."
                            WhatsAppUtils.openWhatsApp(context, post.userPhone, msg)
                        },
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Primary CTA: Message
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMessageClick()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Message", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** Neutral lifestyle chip — small, unobtrusive. */
@Composable
private fun RoommateChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

