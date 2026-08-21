package com.example.staybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel       
import androidx.compose.ui.draw.drawWithContent
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.navigation.compose.rememberNavController
import com.example.staybuddy.ui.navigation.BottomNavItems
import com.example.staybuddy.ui.navigation.NavGraph
import com.example.staybuddy.ui.navigation.Screen
import com.example.staybuddy.ui.theme.StayBuddyTheme
import androidx.compose.foundation.layout.Box
import com.example.staybuddy.ui.components.UpdateDialog
import com.example.staybuddy.ui.components.InAppNotificationBanner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val _intentFlow = kotlinx.coroutines.flow.MutableStateFlow<android.content.Intent?>(null)
    val intentFlow: kotlinx.coroutines.flow.StateFlow<android.content.Intent?> = _intentFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        _intentFlow.value = intent
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themePreference by viewModel.themePreference.collectAsState()
            
            StayBuddyTheme(themePreference = themePreference) {
                MainApp(viewModel, _intentFlow)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _intentFlow.value = intent
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel = hiltViewModel(), intentFlow: kotlinx.coroutines.flow.StateFlow<android.content.Intent?>) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        // Ask only after user reaches home — they have context by then, accept rate is higher
        LaunchedEffect(currentRoute) {
            if (currentRoute == Screen.Home.route && !permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Home.route || currentRoute == Screen.Profile.route) {
            viewModel.refreshSession()
        }
    }

    val currentIntent by intentFlow.collectAsState()

    // Tracks whether the initial (cold-start) intent has been processed. On cold
    // start NavController auto-resolves a launch deep link, so we only manually
    // resolve warm-start (onNewIntent) deep links to avoid double navigation.
    var initialIntentHandled by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(currentIntent) {
        val intent = currentIntent ?: return@LaunchedEffect
        val routeType = intent.getStringExtra("routeType")
        val routeId = intent.getStringExtra("routeId")
        val rawRoute = intent.getStringExtra("route")
            ?: intent.getStringExtra("deepLink")
            ?: intent.getStringExtra("url")
        val isInitialIntent = !initialIntentHandled

        if (!rawRoute.isNullOrBlank()) {
            if (rawRoute.startsWith("http://") || rawRoute.startsWith("https://")) {
                try {
                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rawRoute)).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to open notification URL: $rawRoute", e)
                }
            } else if (rawRoute.startsWith("staybuddy://")) {
                val deepLinkIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rawRoute))
                navController.handleDeepLink(deepLinkIntent)
            }
            intent.removeExtra("route")
            intent.removeExtra("deepLink")
            intent.removeExtra("url")
        } else if (routeType == "chat" && !routeId.isNullOrBlank()) {
            navController.navigate(Screen.Chat(routeId).route)
            intent.removeExtra("routeType")
            intent.removeExtra("routeId")
        } else if (routeType == "support" || routeType == "ticket") {
            navController.navigate(Screen.SupportTicket.route)
            intent.removeExtra("routeType")
        } else if (!isInitialIntent && intent.data?.scheme == "staybuddy") {
            // Notification tapped while the app is already running → resolve deep link
            navController.handleDeepLink(intent)
            intent.data = null
        }

        initialIntentHandled = true
    }

    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    if (updateState.isUpdateAvailable) {
        UpdateDialog(
            updateInfo = updateState,
            onUpdateClick = { viewModel.appUpdater.downloadUpdate() },
            onDismiss = { viewModel.appUpdater.dismissUpdate() }
        )
    }

    // Real-time ban dialog — if user gets banned mid-session
    if (uiState.isBanned) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            icon = {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Account Suspended",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.banReason ?: "Your account has been suspended by the admin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Contact support for more information.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Sign In with Another Account",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Screens that show the bottom navigation bar
    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.RoommateList.route,
        Screen.AddListing.createRoute(null),
        Screen.OwnerDashboard.route,
        Screen.OwnerInquiries.route,
        Screen.ChatList.route,
        Screen.Profile.route
    )

    val showBottomBar = currentRoute in bottomNavRoutes

    val bottomNavItems = if (uiState.userRole.equals("owner", ignoreCase = true)) {
        BottomNavItems.ownerItems
    } else {
        BottomNavItems.studentItems
    }

    var currentInAppNotification by remember { mutableStateOf<com.example.staybuddy.notifications.InAppNotification?>(null) }
    var inAppNotificationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.example.staybuddy.notifications.InAppNotificationManager.notificationEvents.collect { notification ->
            currentInAppNotification = notification
            inAppNotificationVisible = true
        }
    }

    CompositionLocalProvider(com.example.staybuddy.ui.components.LocalSnackbarHostState provides snackbarHostState) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            bottomNavItems.forEach { item ->
                                val isSelected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.navigationRoute) {
                                                popUpTo(Screen.Home.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        val unreadCount = when (item.route) {
                                            Screen.ChatList.route -> uiState.unreadChatCount
                                            Screen.OwnerInquiries.route -> uiState.unreadInquiryCount
                                            else -> 0
                                        }
                                        BadgedBox(
                                            badge = {
                                                if (unreadCount > 0) {
                                                    Badge {
                                                        Text(unreadCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    },
                                    label = { 
                                        AutoSizeText(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavGraph(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            
            InAppNotificationBanner(
                notification = currentInAppNotification,
                isVisible = inAppNotificationVisible,
                onDismiss = { inAppNotificationVisible = false },
                onClick = { notification ->
                    inAppNotificationVisible = false
                    if (notification.url != null) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(notification.url))
                        navController.context.startActivity(intent)
                    } else if (notification.chatId != null) {
                        navController.navigate(Screen.Chat(notification.chatId).route)
                    } else if (notification.inquiryId != null) {
                        navController.navigate(Screen.OwnerInquiries.route)
                    }
                }
            )
        }
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelSmall,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    maxLines: Int = 1
) {
    var resizedTextStyle by remember { mutableStateOf(style) }
    var shouldDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        softWrap = false,
        maxLines = maxLines,
        style = resizedTextStyle,
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                resizedTextStyle = resizedTextStyle.copy(
                    fontSize = resizedTextStyle.fontSize * 0.9f
                )
            } else {
                shouldDraw = true
            }
        }
    )
}
