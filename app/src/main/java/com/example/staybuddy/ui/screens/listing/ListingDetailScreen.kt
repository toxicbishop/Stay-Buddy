package com.example.staybuddy.ui.screens.listing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.staybuddy.AutoSizeText
import com.example.staybuddy.R
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.ui.components.ErrorBanner
import com.example.staybuddy.ui.components.FreshnessTag
import com.example.staybuddy.ui.components.ListingCardSkeleton
import com.example.staybuddy.ui.components.LocalAnimatedVisibilityScope
import com.example.staybuddy.ui.components.LocalSharedTransitionScope
import com.example.staybuddy.ui.components.LocalSnackbarHostState
import com.example.staybuddy.ui.components.MapboxMapView
import com.example.staybuddy.ui.components.PriceTag
import com.example.staybuddy.ui.components.VerifiedBadge
import com.example.staybuddy.ui.theme.RatingAmber
import com.example.staybuddy.ui.theme.WarningAmber
import com.example.staybuddy.util.WhatsAppUtils
import com.example.staybuddy.utils.ResponsiveUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
// osmdroid removed — using Mapbox

// ─────────────────────────────────────────────────────────────────────────────
// PUBLIC ENTRY POINT
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListingDetailScreen(
    listingId: String,
    origin: String = "home",
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ListingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showReportSheet by remember { mutableStateOf(false) }
    var fullscreenImageIndex by remember { mutableIntStateOf(-1) }
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    // Top bar fades in as user scrolls past the image.
    val topBarAlpha = (scrollState.value / 300f).coerceIn(0f, 1f)

    // ── Side effects ────────────────────────────────────────────────────────
    LaunchedEffect(uiState.reportError, uiState.isReportSent) {
        if (uiState.isReportSent) {
            snackbarHostState.showSnackbar("Listing reported to admins")
            showReportSheet = false
        }
        uiState.reportError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.onChatNavigated()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isInquirySent) {
        if (uiState.isInquirySent) {
            snackbarHostState.showSnackbar("Inquiry sent successfully!")
        }
    }

    // ── Dialogs / sheets ────────────────────────────────────────────────────
    if (uiState.showInquiryDialog && uiState.listing != null) {
        InquiryDialog(
            listing = uiState.listing!!,
            onDismiss = { viewModel.setInquiryDialogVisible(false) },
            onConfirm = { date, roomType, message ->
                viewModel.sendInquiry(date, roomType, message)
                viewModel.setInquiryDialogVisible(false)
            }
        )
    }

    if (showReportSheet) {
        ReportBottomSheet(
            hasAlreadyReported = uiState.hasAlreadyReported,
            onDismiss = { showReportSheet = false },
            onReport = { reason ->
                viewModel.reportListing(reason)
                showReportSheet = false
            }
        )
    }

    // Share intent
    val shareContext = LocalContext.current
    val triggerShare = { title: String, city: String, price: Int, deposit: Int ->
        val shareText = buildString {
            append("Check out this PG on StayBuddy 🏠\n\n")
            append("📍 $title\n   $city\n")
            append("💰 ₹$price/month")
            if (deposit > 0) append(" · Deposit ₹$deposit")
            append("\n\nShared via StayBuddy")
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        shareContext.startActivity(android.content.Intent.createChooser(intent, "Share listing"))
    }

    // ── Fullscreen image viewer ─────────────────────────────────────────────
    if (fullscreenImageIndex >= 0 && uiState.listing != null) {
        FullscreenImageViewer(
            images = uiState.listing!!.images,
            initialIndex = fullscreenImageIndex,
            onDismiss = { fullscreenImageIndex = -1 }
        )
    }

    // ── Scaffold ────────────────────────────────────────────────────────────
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val listing = uiState.listing
            if (listing != null) {
                val context = LocalContext.current
                DetailBottomBar(
                    listing = listing,
                    hasExistingInquiry = uiState.hasExistingInquiry,
                    onWhatsApp = {
                        val phone = listing.ownerPhone
                        val msg = "Hi, I'm interested in your property ${listing.title} listed on StayBuddy."
                        WhatsAppUtils.openWhatsApp(context, phone, msg)
                    },
                    onChat = { viewModel.createChat() },
                    onInquire = { viewModel.setInquiryDialogVisible(true) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when {
                uiState.listing != null -> DetailContent(
                    listing = uiState.listing!!,
                    origin = origin,
                    scrollState = scrollState,
                    onImageClick = { fullscreenImageIndex = it },
                    similarListings = uiState.similarListings,
                    userReview = uiState.userReview,
                    isReviewSubmitting = uiState.isReviewSubmitting,
                    onSubmitReview = viewModel::submitReview
                )
                uiState.isLoading -> LoadingContent()
                uiState.error != null -> ErrorContent(message = uiState.error!!)
            }

            DetailTopBar(
                title = uiState.listing?.title ?: "",
                isFavorite = uiState.isFavorite,
                showFavorite = !uiState.isOwnerView,
                topBarAlpha = topBarAlpha,
                onBack = onNavigateBack,
                onReport = { showReportSheet = true },
                onFavorite = { viewModel.toggleFavorite() },
                onShare = {
                    uiState.listing?.let { l ->
                        triggerShare(l.title, l.city, l.price, l.deposit)
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        ListingCardSkeleton()
        ListingCardSkeleton()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ErrorBanner(message = message)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailContent(
    listing: PgListing,
    origin: String,
    scrollState: androidx.compose.foundation.ScrollState,
    onImageClick: (Int) -> Unit,
    similarListings: List<PgListing> = emptyList(),
    userReview: com.example.staybuddy.data.model.Review? = null,
    isReviewSubmitting: Boolean = false,
    onSubmitReview: (Int, String) -> Unit = { _, _ -> }
) {
    val pagerState = rememberPagerState(
        pageCount = { if (listing.images.isEmpty()) 1 else listing.images.size }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Reserve space equal to the floating top bar height.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
        )

        ImageCarousel(
            listing = listing,
            origin = origin,
            pagerState = pagerState,
            onImageClick = onImageClick
        )

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { 40 }) + fadeIn(),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val screenPadding = ResponsiveUtils.screenPadding()
            Column(modifier = Modifier.padding(horizontal = screenPadding, vertical = 20.dp)) {
                TitleHeader(listing = listing)
                Spacer(modifier = Modifier.height(12.dp))
                // Quick facts + freshness combined in one row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickFactsRow(listing = listing)
                    Spacer(modifier = Modifier.weight(1f))
                    FreshnessAndViewsRow(listing = listing)
                }
                Spacer(modifier = Modifier.height(16.dp))
                PriceStrip(listing = listing)
                Spacer(modifier = Modifier.height(20.dp))
                AboutSection(listing = listing)
                Spacer(modifier = Modifier.height(20.dp))
                AmenitiesSection(listing = listing)
                Spacer(modifier = Modifier.height(20.dp))
                LifestyleSection(listing = listing)
                Spacer(modifier = Modifier.height(20.dp))
                NeighborhoodSection(listing = listing)
                Spacer(modifier = Modifier.height(20.dp))
                OwnerCard(listing = listing)
                Spacer(modifier = Modifier.height(20.dp))
                ReviewsSection(
                    listing = listing,
                    userReview = userReview,
                    isSubmitting = isReviewSubmitting,
                    onSubmitReview = onSubmitReview
                )
                if (similarListings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SimilarListingsRow(listings = similarListings)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IMAGE CAROUSEL
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImageCarousel(
    listing: PgListing,
    origin: String,
    pagerState: PagerState,
    onImageClick: (Int) -> Unit = {}
) {
    // Auto-cycle the first few images to hint that the carousel is swipeable.
    // Stops permanently the moment the user drags manually.
    var userInteracted by remember(listing.listingId) { mutableStateOf(false) }
    LaunchedEffect(pagerState.interactionSource) {
        pagerState.interactionSource.interactions.collect { userInteracted = true }
    }
    val autoScrollCount = listing.images.size.coerceAtMost(4)
    LaunchedEffect(listing.listingId, autoScrollCount, userInteracted) {
        if (userInteracted || autoScrollCount <= 1) return@LaunchedEffect
        delay(1500)
        while (!userInteracted) {
            val next = (pagerState.currentPage + 1) % autoScrollCount
            pagerState.animateScrollToPage(next)
            delay(3000)
        }
    }

    val isCompact = ResponsiveUtils.isCompactScreen()
    val imageAspectRatio = if (isCompact) 1.2f else 1.6f

    val imageInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(imageAspectRatio)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = imageInteraction,
                indication = null
            ) { onImageClick(pagerState.currentPage) }
    ) {
        CarouselPager(
            listing = listing,
            origin = origin,
            pagerState = pagerState
        )

        if (listing.images.size > 1) {
            PagerIndicator(
                pageCount = listing.images.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )

            // Image counter pill — top-right
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${listing.images.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CarouselPager(
    listing: PgListing,
    origin: String,
    pagerState: PagerState
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        var imageModifier: Modifier = Modifier.fillMaxSize()

        if (sharedTransitionScope != null && animatedVisibilityScope != null && page == 0) {
            with(sharedTransitionScope) {
                imageModifier = imageModifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "listing_image_${listing.listingId}_$origin"
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(28.dp))
                )
            }
        }

        val context = LocalContext.current
        val rawImageData = if (listing.images.isNotEmpty()) listing.images[page] else null
        val imageData = remember(rawImageData) {
            com.example.staybuddy.utils.ImageUtils.optimizeCloudinaryUrl(rawImageData, width = 1200)
        }
        val imageRequest = remember(imageData) {
            ImageRequest.Builder(context)
                .data(imageData)
                .crossfade(false)   // prevents micro-flash on navigation (card & detail use different URLs)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = "Listing Image",
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { iteration ->
            val width by animateDpAsState(
                targetValue = if (currentPage == iteration) 18.dp else 6.dp,
                label = "indicatorWidth"
            )
            val alpha by animateFloatAsState(
                targetValue = if (currentPage == iteration) 1f else 0.5f,
                label = "indicatorAlpha"
            )
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
                    .height(6.dp)
                    .width(width)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailTopBar(
    title: String,
    isFavorite: Boolean,
    showFavorite: Boolean = true,
    topBarAlpha: Float,
    onBack: () -> Unit,
    onReport: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha),
                        MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chipBg = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            val chipBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(chipBg, CircleShape)
                    .border(1.dp, chipBorder, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (topBarAlpha > 0.7f) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Overflow menu (Report, Share in future)
            var showOverflow by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showOverflow = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(chipBg, CircleShape)
                        .border(1.dp, chipBorder, CircleShape)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = showOverflow,
                    onDismissRequest = { showOverflow = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share listing") },
                        onClick = {
                            showOverflow = false
                            onShare()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Report listing") },
                        onClick = {
                            showOverflow = false
                            onReport()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    )
                }
            }

            if (showFavorite) {
                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .size(44.dp)
                        .background(chipBg, CircleShape)
                        .border(1.dp, chipBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailBottomBar(
    listing: PgListing,
    hasExistingInquiry: Boolean,
    onWhatsApp: () -> Unit,
    onChat: () -> Unit,
    onInquire: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 24.dp,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriceTag(
                price = listing.price,
                large = true,
                modifier = Modifier.weight(1f, fill = false)
            )

            FilledTonalIconButton(
                onClick = onWhatsApp,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF25D366).copy(alpha = 0.15f),
                    contentColor = Color(0xFF25D366)
                )
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChat()
                },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Message",
                    modifier = Modifier.size(20.dp)
                )
            }

            Button(
                onClick = onInquire,
                enabled = !hasExistingInquiry,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                colors = if (hasExistingInquiry) {
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            ) {
                if (hasExistingInquiry) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sent", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                } else {
                    Text("Inquire", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTENT SECTIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TitleHeader(listing: PgListing) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: Title + Rating (or NEW badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (listing.isVerified) {
                        Spacer(Modifier.width(8.dp))
                        VerifiedBadge(size = 16.dp)
                    }
                }
            }

            // Rating pill (right side, same line as title)
            Surface(
                color = if (listing.rating > 0) RatingAmber.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (listing.rating > 0) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "%.1f".format(listing.rating),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    } else {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Row 2: Location subtitle (always below title)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = listOf(listing.area, listing.city).filter { it.isNotBlank() }.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickFactsRow(listing: PgListing) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Badge(listing.roomType.uppercase(), MaterialTheme.colorScheme.primary)
        Badge(listing.genderAllowed.uppercase(), MaterialTheme.colorScheme.secondary)
        if (listing.availableBeds > 0) {
            Badge("${listing.availableBeds} BEDS LEFT", MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun FreshnessAndViewsRow(listing: PgListing) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FreshnessTag(createdAt = listing.createdAt)
        if (listing.viewCount > 0) {
            Text(
                text = "·",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${listing.viewCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PriceStrip(listing: PgListing) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Price — massive, clay color, impossible to miss
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "₹%,d".format(listing.price),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "/mo",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        // Deposit — smaller, muted, secondary info
        if (listing.deposit > 0) {
            Text(
                text = "₹${listing.deposit} deposit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationCard(listing: PgListing) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = listing.area,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listing.city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutSection(listing: PgListing) {
    SectionTitle("About this PG")
    Text(
        text = listing.description,
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = 26.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AmenitiesSection(listing: PgListing) {
    val priorityOrder = listOf("wifi", "ac", "power", "food", "laundry")

    val allUniqueAmenities = remember(listing.amenities) {
        listing.amenities
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    val essentials = remember(allUniqueAmenities) {
        allUniqueAmenities
            .sortedBy { amenity ->
                val idx = priorityOrder.indexOfFirst { amenity.lowercase().contains(it) }
                if (idx >= 0) idx else priorityOrder.size
            }
            .take(4)
    }

    val remaining = remember(allUniqueAmenities, essentials) {
        allUniqueAmenities.filterNot { essentials.contains(it) }
    }

    var showAll by remember { mutableStateOf(false) }

    if (allUniqueAmenities.isNotEmpty()) {
        SectionTitle("Amenities")

        // Hero essentials — top 4 distinct large icon cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            essentials.forEach { amenity ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    HeroAmenityItem(amenity)
                }
            }
            // Fill empty slots if fewer than 4 essentials
            repeat(4 - essentials.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Remaining amenities grid (expandable)
        if (remaining.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            if (!showAll) {
                TextButton(
                    onClick = { showAll = true },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "See all ${allUniqueAmenities.size} amenities",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    remaining.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { amenity ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MinimalAmenityItem(amenity)
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewsSection(
    listing: PgListing,
    userReview: com.example.staybuddy.data.model.Review? = null,
    isSubmitting: Boolean = false,
    onSubmitReview: (Int, String) -> Unit = { _, _ -> }
) {
    var showReviewSheet by remember { mutableStateOf(false) }
    val hasReviewed = userReview != null

    // Always show the section — even with 0 reviews, to show the "Write a Review" CTA
    SectionTitle("Reviews")

    // Summary card
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Big rating number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (listing.rating > 0) "%.1f".format(listing.rating) else "—",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = if (listing.rating > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (listing.rating > 0) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = RatingAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = "${listing.reviewCount} review${if (listing.reviewCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Summary text
                Column(modifier = Modifier.weight(1f)) {
                    if (listing.reviewCount > 5) {
                        Text(
                            text = "Established listing with ${listing.reviewCount} reviews",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (listing.reviewCount > 0) {
                        Text(
                            text = "New listing — early reviews are coming in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No reviews yet — be the first!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Write / Edit Review button
            Button(
                onClick = { showReviewSheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                enabled = !isSubmitting
            ) {
                Icon(
                    imageVector = if (hasReviewed) Icons.Default.Edit else Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasReviewed) "Edit your review" else "Write a review"
                )
            }

            // Show user's existing review preview
            if (hasReviewed) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < userReview!!.rating) RatingAmber
                                           else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your review",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (userReview!!.comment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = userReview.comment,
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

    // Review submission bottom sheet
    if (showReviewSheet) {
        ReviewSubmissionSheet(
            existingRating = userReview?.rating ?: 0,
            existingComment = userReview?.comment ?: "",
            isSubmitting = isSubmitting,
            onDismiss = { showReviewSheet = false },
            onSubmit = { rating, comment ->
                onSubmitReview(rating, comment)
                showReviewSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSubmissionSheet(
    existingRating: Int = 0,
    existingComment: String = "",
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(existingRating) }
    var comment by remember { mutableStateOf(existingComment) }
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
                text = if (existingRating > 0) "Edit your review" else "Rate this listing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Star rating
            Text(
                text = "Your rating",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    IconButton(
                        onClick = { rating = index + 1 },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "${index + 1} star${if (index > 0) "s" else ""}",
                            tint = if (index < rating) RatingAmber
                                   else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Comment
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Share your experience (optional)") },
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.small
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit
            Button(
                onClick = { onSubmit(rating, comment) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
                enabled = rating > 0 && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (existingRating > 0) "Update review" else "Submit review",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifestyleSection(listing: PgListing) {
    val prefs = listing.lifestylePreferences
    val tags = buildList {
        if (prefs.isVegetarian) add("🌱 Vegetarian" to MaterialTheme.colorScheme.primary)
        if (prefs.isNonSmoker) add("🚭 Non-smokers" to MaterialTheme.colorScheme.secondary)
        if (prefs.isStudent) add("🎓 Students" to MaterialTheme.colorScheme.tertiary)
        if (prefs.isWorkingProfessional) add("💼 Professionals" to MaterialTheme.colorScheme.secondary)
        if (prefs.isPetFriendly) add("🐾 Pet-friendly" to MaterialTheme.colorScheme.tertiary)
    }

    if (tags.isNotEmpty()) {
        SectionTitle("Who lives here")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { (label, color) ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = color.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarListingsRow(listings: List<PgListing>) {
    SectionTitle("Similar PGs nearby")
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(listings.size) { index ->
            val similar = listings[index]
            SimilarListingCard(listing = similar)
        }
    }
}

@Composable
private fun SimilarListingCard(listing: PgListing) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.width(200.dp)
    ) {
        Column {
            // Thumbnail
            if (listing.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(listing.images.firstOrNull())
                        .crossfade(true)
                        .build(),
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${listing.price}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "/mo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (listing.rating > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "%.1f".format(listing.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeighborhoodSection(listing: PgListing) {
    if (listing.latitude == 0.0 && listing.longitude == 0.0) return
    SectionTitle("Neighborhood")
    val mapHeight = if (ResponsiveUtils.isCompactScreen()) 240.dp else 360.dp

    var isMapVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isMapVisible = true
    }

    // Consume all nested scroll inside the map so the parent Column never
    // intercepts touch events while the user is interacting with the map.
    val mapScrollLock = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource) = available
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(mapHeight)
            .nestedScroll(mapScrollLock),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isMapVisible && listing.latitude != 0.0 && listing.longitude != 0.0) {
                MapboxMapView(
                    listings = listOf(listing),
                    currentLocation = com.mapbox.geojson.Point.fromLngLat(listing.longitude, listing.latitude),
                    onMarkerClick = {}
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerCard(listing: PgListing) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (listing.ownerProfileImage.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(listing.ownerProfileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Owner Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.ownerName.ifEmpty { "Property Owner" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (listing.isVerified) "Verified Property Owner" else "Property Owner",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (listing.isVerified) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VerifiedBadge(size = 14.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Verified",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = { WhatsAppUtils.makePhoneCall(context, listing.ownerPhone) },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call Owner",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED PIECES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Badge(text: String, containerColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, containerColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = containerColor.copy(alpha = 1f).takeOrElse { MaterialTheme.colorScheme.primary },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun MinimalAmenityItem(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForAmenity(text),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text.trim(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroAmenityItem(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconForAmenity(text),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text.trim(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getIconForAmenity(amenity: String): ImageVector {
    val lower = amenity.lowercase().trim()
    return when {
        lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet") -> Icons.Default.Wifi
        lower.contains("power") || lower.contains("backup") || lower.contains("generator") -> Icons.Default.ElectricBolt
        lower.contains("washroom") || lower.contains("bathroom") || lower.contains("bath") || lower.contains("toilet") -> Icons.Default.Bathtub
        lower.contains("clean") || lower.contains("housekeeping") || lower.contains("maid") -> Icons.Default.CleaningServices
        lower.contains("food") || lower.contains("mess") || lower.contains("meal") || lower.contains("breakfast") || lower.contains("dinner") -> Icons.Default.Restaurant
        lower.contains("laundry") || lower.contains("washing") -> Icons.Default.LocalLaundryService
        lower.contains("geyser") || lower.contains("hot water") || lower.contains("heater") -> Icons.Default.HotTub
        lower.contains("fridge") || lower.contains("refrigerator") -> Icons.Default.Kitchen
        lower.contains("water") || lower.contains("purifier") || lower.contains("ro") -> Icons.Default.WaterDrop
        lower.contains("cctv") || lower.contains("security") || lower.contains("warden") || lower.contains("guard") -> Icons.Default.Security
        lower.contains("parking") -> Icons.Default.DirectionsCar
        lower.contains("lift") || lower.contains("elevator") -> Icons.Default.Elevator
        lower.contains("gym") || lower.contains("fitness") -> Icons.Default.FitnessCenter
        lower.contains("kitchen") -> Icons.Default.SoupKitchen
        Regex("\\b(ac|air conditioner|air conditioning)\\b").containsMatchIn(lower) -> Icons.Default.AcUnit
        Regex("\\b(tv|television)\\b").containsMatchIn(lower) -> Icons.Default.Tv
        else -> Icons.Default.CheckCircle
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOGS / SHEETS
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryDialog(
    listing: PgListing,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String) -> Unit
) {
    val todayUtcMidnight = remember {
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var moveInDate by remember { mutableLongStateOf(todayUtcMidnight) }
    var selectedRoomType by remember { mutableStateOf(listing.roomType) }
    var message by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = moveInDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    moveInDate = datePickerState.selectedDateMillis ?: moveInDate
                    showDatePicker = false
                }) { Text("Confirm") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Property Inquiry", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Let the owner know you're interested in ${listing.title}.")

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Preferred Move-in Date", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(moveInDate)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedRoomType,
                    onValueChange = { selectedRoomType = it },
                    label = { Text("Preferred Room Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message to Owner (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("e.g. I'm a student at MSU...") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(moveInDate, selectedRoomType, message) }) {
                        Text("Confirm Inquiry")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    hasAlreadyReported: Boolean,
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val reportReasons = listOf(
        "Fake listing",
        "Outdated / no longer available",
        "Wrong price or details",
        "Duplicate listing",
        "Scam / Fraud"
    )
    var selectedReason by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Report this listing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (hasAlreadyReported) {
                Text(
                    text = "You've already reported this listing. Our team is reviewing it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Text(
                    text = "Why are you reporting this listing?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                reportReasons.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        onClick = { selectedReason = reason }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = reason },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { selectedReason?.let { onReport(it) } },
                    enabled = selectedReason != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FULLSCREEN IMAGE VIEWER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FullscreenImageViewer(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { images.size }
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Pager with pinch-zoom
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    val transformer = rememberTransformableState { zoomChange, panChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        offset += panChange
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(images[page])
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full screen image ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformer)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Page counter
                if (images.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 12.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
