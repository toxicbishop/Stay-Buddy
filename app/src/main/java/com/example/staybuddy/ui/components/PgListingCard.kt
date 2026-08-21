package com.example.staybuddy.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.tooling.preview.Preview
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.ui.theme.RatingAmber
import com.example.staybuddy.ui.theme.WarningAmber
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.preview.PreviewMockData

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.staybuddy.ui.components.LocalAnimatedVisibilityScope
import com.example.staybuddy.ui.components.LocalSharedTransitionScope

/**
 * The listing card — StayBuddy's core surface. Photo with a clay PriceTag
 * and trust badges on top, then title, location and a quiet meta row.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PgListingCard(
    listing: PgListing,
    isFavorite: Boolean = false,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    distanceKm: Double? = null,
    targetAnchor: com.example.staybuddy.domain.model.TargetAnchor? = null,
    userArea: String? = null,
    showFreshnessTag: Boolean = true,
    showFavorite: Boolean = true,
    cityAvgPrice: Int = 0,
    origin: String = "home",
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    @OptIn(ExperimentalSharedTransitionApi::class)
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onCardClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                var imageModifier: Modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        imageModifier = imageModifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "listing_image_${listing.listingId}_$origin"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(22.dp))
                        )
                    }
                }

                val context = LocalContext.current
                val optimizedUrl = remember(listing.images.firstOrNull()) {
                    com.example.staybuddy.utils.ImageUtils.optimizeCloudinaryUrl(listing.images.firstOrNull(), width = 800)
                }
                val imageRequest = remember(optimizedUrl) {
                    ImageRequest.Builder(context)
                        .data(optimizedUrl)
                        .crossfade(true)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Photo of ${listing.title}",
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                )

                // One soft gradient at the bottom so the price tag always reads.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                // Trust badges — top start
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (listing.isPremium) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = WarningAmber,
                            contentColor = Color.White
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Premium",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (listing.isVerified) {
                        VerifiedBadge(showLabel = true)
                    }
                    if (listing.genderAllowed.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = Color.White.copy(alpha = 0.92f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = listing.genderAllowed.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Favorite — top end (hidden for owner's own listings)
                if (showFavorite) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onFavoriteClick()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Signature price tag — bottom start, over the gradient
                PriceTag(
                    price = listing.price,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )

                // Owner edit affordance — bottom end
                if (onEditClick != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit listing",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = RatingAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (listing.rating > 0) "%.1f".format(listing.rating) else "New",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = listOf(listing.area, listing.city)
                            .filter { it.isNotBlank() }
                            .joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (distanceKm != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            val distText = if (targetAnchor != null && targetAnchor.type != com.example.staybuddy.domain.model.AnchorType.CURRENT_GPS) {
                                val icon = if (targetAnchor.type == com.example.staybuddy.domain.model.AnchorType.UNIVERSITY) "🎓" else "📍"
                                "$icon %.1f km from ${targetAnchor.name}".format(distanceKm)
                            } else {
                                "📍 %.1f km from Your Location".format(distanceKm)
                            }
                            Text(
                                text = distText,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    // Area badge — "Your Area" when listing is in user's detected neighborhood
                    if (userArea != null && listing.area.isNotBlank() && listing.area.equals(userArea, ignoreCase = true)) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "Your Area",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    // Smart badges — contextual, helpful info
                    if (cityAvgPrice > 0 && listing.price > 0 && listing.price < cityAvgPrice * 0.85) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = com.example.staybuddy.ui.theme.SuccessGreen.copy(alpha = 0.12f),
                            contentColor = com.example.staybuddy.ui.theme.SuccessGreen
                        ) {
                            Text(
                                text = "Below avg",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (listing.viewCount > 50) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = "Popular",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    val isJustListed = (System.currentTimeMillis() - listing.createdAt) < 24 * 60 * 60 * 1000
                    if (isJustListed) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (showFreshnessTag) {
                        FreshnessTag(createdAt = listing.createdAt)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPgListingCard() {
    StayBuddyTheme {
        PgListingCard(
            listing = PreviewMockData.sampleListings[0],
            isFavorite = false,
            onCardClick = {},
            onFavoriteClick = {},
            distanceKm = 1.2,
            modifier = Modifier.padding(16.dp)
        )
    }
}
