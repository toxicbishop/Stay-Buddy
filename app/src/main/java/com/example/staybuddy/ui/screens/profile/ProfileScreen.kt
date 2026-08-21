package com.example.staybuddy.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.staybuddy.BuildConfig
import com.example.staybuddy.data.model.User
import com.example.staybuddy.utils.Constants
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.preview.PreviewMockData
import com.example.staybuddy.ui.components.ProfileScreenShimmer
import com.example.staybuddy.ui.components.mouseWheelScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToFavorites: () -> Unit,
    onNavigateToOwnerDashboard: () -> Unit,
    onNavigateToChatList: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToCompatibilityQuiz: () -> Unit,
    onNavigateToSupportTicket: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshUserProfile()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
                ProfileScreenShimmer(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                )
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else {
            val user = uiState.user
            if (user != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .mouseWheelScroll(scrollState)
                            .verticalScroll(scrollState)
                    ) {
                        ProfileContentBody(
                            user = user,
                            favoritesCount = uiState.favoritesCount,
                            inquiriesCount = uiState.inquiriesCount,
                            hasUnreadSupport = uiState.hasUnreadSupport,
                            onNavigateToFavorites = onNavigateToFavorites,
                            onNavigateToOwnerDashboard = onNavigateToOwnerDashboard,
                            onNavigateToChatList = onNavigateToChatList,
                            onNavigateToEditProfile = onNavigateToEditProfile,
                            onNavigateToCompatibilityQuiz = onNavigateToCompatibilityQuiz,
                            onShowThemeDialog = { showThemeDialog = true },
                            onShowHelpSheet = { showHelpSheet = true },
                            onLogout = { showLogoutDialog = true },
                            onResolveAvatarUrl = viewModel::resolveDisplayUrl
                        )
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Log out?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "You'll need to sign in again to access your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Log Out", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    val themeOptions = listOf(
                        Triple("LIGHT", "Light", Icons.Default.WbSunny),
                        Triple("SYSTEM", "Auto", Icons.Default.SettingsSuggest),
                        Triple("DARK", "Dark", Icons.Default.DarkMode)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        themeOptions.forEach { (value, label, icon) ->
                            val isSelected = themePreference == value
                            val animSpec = tween<Color>(250)
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLowest,
                                animationSpec = animSpec, label = "bg"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                animationSpec = animSpec, label = "border"
                            )
                            val borderWidth by animateDpAsState(
                                targetValue = if (isSelected) 2.dp else 1.dp,
                                animationSpec = tween(250), label = "borderW"
                            )
                            val iconTint by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = animSpec, label = "iconTint"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                animationSpec = animSpec, label = "textColor"
                            )
                            Surface(
                                onClick = {
                                    viewModel.setThemePreference(value)
                                },
                                shape = MaterialTheme.shapes.large,
                                color = bgColor,
                                border = BorderStroke(borderWidth, borderColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = iconTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
        
        var supportDialogType by remember { mutableStateOf<String?>(null) }

        if (showHelpSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHelpSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                HelpAndSupportSheetContent(
                    onDismiss = { showHelpSheet = false },
                    onShowDialog = { type -> supportDialogType = type },
                    onNavigateToSupportTicket = {
                        showHelpSheet = false
                        onNavigateToSupportTicket()
                    },
                    hasUnreadSupport = uiState.hasUnreadSupport
                )
            }
        }

        if (supportDialogType != null) {
            AlertDialog(
                onDismissRequest = { supportDialogType = null },
                title = {
                    Text(
                        text = when (supportDialogType) {
                            "FAQ" -> "Frequently Asked Questions"
                            "Privacy" -> "Privacy Policy"
                            "Terms" -> "Terms of Service"
                            "DeleteAccount" -> "Account & Data Deletion"
                            else -> ""
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .mouseWheelScroll(scrollState)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (supportDialogType) {
                            "FAQ" -> {
                                FaqItem(
                                    question = "How do I list my PG or property?",
                                    answer = "Go to the 'Dashboard' tab at the bottom and tap on 'Add Listing'. Fill in the details about your property, upload clear photos, and publish. It will instantly be visible to seekers."
                                )
                                FaqItem(
                                    question = "How can I find the perfect roommate?",
                                    answer = "Head over to the 'Roommates' tab. You can browse through posts, take our compatibility quiz to find like-minded people, and chat directly to see if you're a match before making any commitments."
                                )
                                FaqItem(
                                    question = "Are there any hidden charges or brokerage fees?",
                                    answer = "No, StayBuddy is completely free to use! We don't charge any brokerage or hidden fees for browsing listings, matching with roommates, or chatting with owners."
                                )
                                FaqItem(
                                    question = "How do I save a listing for later?",
                                    answer = "Just tap the heart icon (♡) on any PG listing or roommate post. You can easily view all your saved favorites anytime from your Profile screen."
                                )
                                FaqItem(
                                    question = "How can I contact a PG owner directly?",
                                    answer = "When viewing a property, tap the 'Chat' button to instantly start a secure conversation with the owner. You can also send a direct inquiry to schedule a visit."
                                )
                            }
                            "Privacy" -> {
                                Text(
                                    text = "StayBuddy values your privacy. We collect basic profile information and chat data solely for the purpose of matching you with potential roommates and PGs. We do not sell your personal data to third parties. All your communications within the app are secured and can only be accessed by the participants of the chat.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "Terms" -> {
                                Text(
                                    text = "By using StayBuddy, you agree to provide accurate information and interact respectfully with other users. StayBuddy is a platform to connect PG owners and seekers, but we are not responsible for the actual agreements or disputes between parties. Any abusive behavior may result in a permanent ban.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "DeleteAccount" -> {
                                Text(
                                    text = "Google Play Policy requires apps to provide a transparent way for users to request account and data deletion.\n\nTo request permanent deletion of your StayBuddy account, profile, posted listings, and chat history:\n\n1. Open 'Support Tickets' from Help & Support.\n2. Create a ticket under category 'Account Deletion Request'.\n3. Our support team will process your request and delete all personal data within 7 days.\n\nYou may also send an email to support from your registered email address.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { supportDialogType = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

// ─── Main Body ───
@Composable
private fun ProfileContentBody(
    user: User,
    favoritesCount: Int,
    inquiriesCount: Int,
    hasUnreadSupport: Boolean,
    onNavigateToFavorites: () -> Unit,
    onNavigateToOwnerDashboard: () -> Unit,
    onNavigateToChatList: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToCompatibilityQuiz: () -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowHelpSheet: () -> Unit,
    onLogout: () -> Unit,
    onResolveAvatarUrl: (String?) -> String? = { it }
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.onPrimaryContainer
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val headerGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B251E), // Deep midnight evergreen
                Color(0xFF144D3F)  // Dark forest green
            ),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(primaryDark, primary),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    }

    // ── Modern Premium Header ──
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
        ) {
            // Subtle decorative circles for a premium "abstract" feel
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = w * 0.4f,
                    center = Offset(w * 0.85f, h * 0.1f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = w * 0.3f,
                    center = Offset(w * 0.1f, h * 0.8f)
                )
            }

            // Profile info overlaid on header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 64.dp), // Extra bottom space for floating card
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Edit profile button top-right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        onClick = onNavigateToEditProfile,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Avatar
                ProfileAvatarPremium(
                    user = user,
                    resolvedUrl = onResolveAvatarUrl(user.profileImage)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name + Verified Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (user.isOwnerVerified || user.verificationStatus == "VERIFIED") {
                        Spacer(Modifier.width(8.dp))
                        com.example.staybuddy.ui.components.VerifiedBadge(showLabel = true)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Email chip (Frosted Glass style)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Bio
                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "\"${user.bio}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    val isOwner = user.role == Constants.ROLE_OWNER || user.role.equals("owner", ignoreCase = true)

    // ── Floating Stats Card ──
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-32).dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOwner) {
                ProfileStatItem(
                    value = inquiriesCount.toString(),
                    label = "Inquiries",
                    icon = Icons.Default.QuestionAnswer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
                StatDivider()
                ProfileStatItem(
                    value = if (user.isOwnerVerified || user.verificationStatus == "VERIFIED") "Verified" else "Unverified",
                    label = "Verification",
                    icon = Icons.Default.VerifiedUser,
                    iconTint = if (user.isOwnerVerified || user.verificationStatus == "VERIFIED") com.example.staybuddy.ui.theme.VerifiedTeal else MaterialTheme.colorScheme.error
                )
                StatDivider()
                ProfileStatItem(
                    value = "Owner",
                    label = "Role",
                    icon = Icons.Default.Dashboard,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            } else {
                ProfileStatItem(
                    value = favoritesCount.toString(),
                    label = "Favorites",
                    icon = Icons.Default.Favorite,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
                StatDivider()
                ProfileStatItem(
                    value = inquiriesCount.toString(),
                    label = "Inquiries",
                    icon = Icons.Default.QuestionAnswer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
                StatDivider()
                ProfileStatItem(
                    value = user.role.replaceFirstChar { it.uppercase() },
                    label = "Role",
                    icon = Icons.Default.VerifiedUser,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // ── Content below stats (offset to compensate for negative offset above) ──
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-16).dp)
            .padding(horizontal = 20.dp)
    ) {
        // Section label
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // ── Menu Group 1 ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                if (isOwner) {
                    ProfileMenuItem(
                        title = "Owner Dashboard",
                        subtitle = "Manage your listings, inquiries & revenue",
                        icon = Icons.Default.Dashboard,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToOwnerDashboard
                    )
                    MenuItemDivider()
                    ProfileMenuItem(
                        title = "Messages",
                        subtitle = "Chats with prospective tenants",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        onClick = onNavigateToChatList
                    )
                } else {
                    ProfileMenuItem(
                        title = "My Favorites",
                        subtitle = "Saved PGs and hostels",
                        icon = Icons.Default.Favorite,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        onClick = onNavigateToFavorites
                    )
                    MenuItemDivider()
                    ProfileMenuItem(
                        title = "Messages",
                        subtitle = "Chats with owners and roommates",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToChatList
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section label
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // ── Menu Group 2 ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                ProfileMenuItem(
                    title = "Account Settings",
                    subtitle = "Privacy, security and profile",
                    icon = Icons.Default.Settings,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onNavigateToEditProfile
                )
                MenuItemDivider()
                ProfileMenuItem(
                    title = "Appearance",
                    subtitle = "Light, dark, or system theme",
                    icon = Icons.Default.Palette,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    onClick = onShowThemeDialog
                )
                if (user.role == Constants.ROLE_STUDENT) {
                    MenuItemDivider()
                    ProfileMenuItem(
                        title = "Compatibility Quiz",
                        subtitle = "Find your perfect roommate match",
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToCompatibilityQuiz
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section label
        Text(
            text = "Support",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // ── Menu Group 3 ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                ProfileMenuItem(
                    title = "Help & Support",
                    subtitle = "FAQ, Privacy Policy, Terms",
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    showBadge = hasUnreadSupport,
                    onClick = { onShowHelpSheet() }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Logout Card (Clean & Distinct Design System Match) ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            ProfileMenuItem(
                title = "Log Out",
                subtitle = "Sign out of your StayBuddy account",
                icon = Icons.AutoMirrored.Filled.Logout,
                iconBgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                iconTint = MaterialTheme.colorScheme.error,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onLogout
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Minimal Version Display ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "StayBuddy",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportSheetContent(
    onDismiss: () -> Unit,
    onShowDialog: (String) -> Unit,
    onNavigateToSupportTicket: () -> Unit,
    hasUnreadSupport: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp, top = 8.dp)
    ) {
        // Header
        Text(
            text = "Help & Support",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Get help from our team or explore quick resources.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero: Support Tickets
        SupportTicketsHeroCard(
            hasUnread = hasUnreadSupport,
            onClick = onNavigateToSupportTicket
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contact us
        SupportSectionLabel("Contact us")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ContactTile(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "WhatsApp",
                subtitle = "Fastest reply",
                accent = com.example.staybuddy.ui.theme.SuccessGreen,
                modifier = Modifier.weight(1f),
                onClick = {
                    val url = "https://api.whatsapp.com/send?phone=+919998800368&text=Hi%20StayBuddy%20Support,%20I%20need%20help..."
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                    onDismiss()
                }
            )
            ContactTile(
                icon = Icons.Default.Email,
                title = "Email",
                subtitle = "Reply within 24h",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:support@staybuddy.com")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "StayBuddy Support Request")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Send Email"))
                    onDismiss()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Resources
        SupportSectionLabel("Resources")
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column {
                ResourceRow(
                    icon = Icons.Default.QuestionAnswer,
                    title = "FAQs",
                    onClick = {
                        onShowDialog("FAQ")
                        onDismiss()
                    }
                )
                ResourceDivider()
                ResourceRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    onClick = {
                        onShowDialog("Privacy")
                        onDismiss()
                    }
                )
                ResourceDivider()
                ResourceRow(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    onClick = {
                        onShowDialog("Terms")
                        onDismiss()
                    }
                )
                ResourceDivider()
                ResourceRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Account & Data Deletion",
                    onClick = {
                        onShowDialog("DeleteAccount")
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SupportSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SupportTicketsHeroCard(
    hasUnread: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Support Tickets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "New reply",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Raise an issue, track status & get replies in-app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ContactTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResourceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}

@Composable
private fun ResourceRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── Avatar with double ring ───
@Composable
private fun ProfileAvatarPremium(user: User, resolvedUrl: String? = null) {
    Box(contentAlignment = Alignment.Center) {
        // Outer soft glow
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
        // Middle ring
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        )
        // Photo container
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val displayImage = resolvedUrl ?: user.profileImage
            if (displayImage.isNotEmpty()) {
                val optimizedUrl =
                    com.example.staybuddy.utils.ImageUtils.optimizeCloudinaryUrl(
                        displayImage, width = 300
                    )
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(optimizedUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Stat Item with icon ───
@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    icon: ImageVector,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 80.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

// ─── Menu Item ───
@Composable
fun ProfileOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    ProfileMenuItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconBgColor = iconColor.copy(alpha = 0.1f),
        iconTint = iconColor,
        onClick = onClick
    )
}

@Composable
private fun ProfileMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored icon container
        Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.small,
            color = iconBgColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (showBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MenuItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 74.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

// ─── Public composables kept for backward compat ───
@Composable
fun ProfileHeader(user: User) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileAvatarPremium(user = user)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun StatsRow(favoritesCount: Int, inquiriesCount: Int, role: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileStatItem("Favorites", favoritesCount.toString(), Icons.Default.Favorite, MaterialTheme.colorScheme.tertiary)
            StatDivider()
            ProfileStatItem("Inquiries", inquiriesCount.toString(), Icons.Default.QuestionAnswer, MaterialTheme.colorScheme.secondary)
            StatDivider()
            ProfileStatItem("Role", role.replaceFirstChar { it.uppercase() }, Icons.Default.VerifiedUser, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = 1.dp, 
            color = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ─── Preview ───
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
    StayBuddyTheme {
        val user = PreviewMockData.sampleUser
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileContentBody(
                    user = user,
                    favoritesCount = 7,
                    inquiriesCount = 6,
                    hasUnreadSupport = true,
                    onNavigateToFavorites = {},
                    onNavigateToOwnerDashboard = {},
                    onNavigateToChatList = {},
                    onNavigateToEditProfile = {},
                    onNavigateToCompatibilityQuiz = {},
                    onShowThemeDialog = {},
                    onShowHelpSheet = {},
                    onLogout = {}
                )
            }
        }
    }
}

