package com.example.staybuddy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.staybuddy.ui.screens.auth.LoginScreen
import com.example.staybuddy.ui.screens.auth.RegisterScreen
import com.example.staybuddy.ui.screens.auth.FinishRegistrationScreen
import com.example.staybuddy.ui.screens.chat.ChatListScreen
import com.example.staybuddy.ui.screens.chat.ChatScreen
import com.example.staybuddy.ui.screens.favorites.FavoritesScreen
import com.example.staybuddy.ui.screens.home.HomeScreen
import com.example.staybuddy.ui.screens.notifications.NotificationsScreen
import com.example.staybuddy.ui.screens.listing.ListingDetailScreen
import com.example.staybuddy.ui.screens.map.MapViewScreen
import com.example.staybuddy.ui.screens.onboarding.OnboardingScreen
import com.example.staybuddy.ui.screens.owner.AddListingScreen
import com.example.staybuddy.ui.screens.owner.OwnerDashboardScreen
import com.example.staybuddy.ui.screens.profile.EditProfileScreen
import com.example.staybuddy.ui.screens.profile.ProfileScreen
import com.example.staybuddy.ui.screens.profile.SupportTicketScreen
import com.example.staybuddy.ui.screens.roommate.AddRoommatePostScreen
import com.example.staybuddy.ui.screens.roommate.RoommateListScreen
import com.example.staybuddy.ui.screens.search.SearchScreen
import com.example.staybuddy.ui.screens.splash.SplashScreen
import com.example.staybuddy.ui.screens.quiz.CompatibilityQuizScreen

import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.CompositionLocalProvider
import com.example.staybuddy.ui.components.LocalAnimatedVisibilityScope
import com.example.staybuddy.ui.components.LocalSharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = modifier,
                enterTransition = {
                    if (targetState.destination.route?.startsWith("listing_detail") == true) {
                        fadeIn(animationSpec = tween(300))
                    } else {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    }
                },
                exitTransition = {
                    if (targetState.destination.route?.startsWith("listing_detail") == true) {
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("listing_detail") == true) {
                        fadeIn(animationSpec = tween(300))
                    } else {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    }
                },
                popExitTransition = {
                    if (initialState.destination.route?.startsWith("listing_detail") == true) {
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                }
            ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOwnerDashboard = {
                    navController.navigate(Screen.OwnerDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToFinishRegistration = {
                    navController.navigate(Screen.FinishRegistration.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginComplete = { role ->
                    val dest = if (role.equals("owner", ignoreCase = true)) Screen.OwnerDashboard.route else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToFinishRegistration = {
                    navController.navigate(Screen.FinishRegistration.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistrationComplete = { role ->
                    val dest = if (role.equals("owner", ignoreCase = true)) Screen.OwnerDashboard.route else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFinishRegistration = {
                    navController.navigate(Screen.FinishRegistration.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                HomeScreen(
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToListingDetail = { listingId, origin ->
                        navController.navigate(Screen.ListingDetail.createRoute(listingId, origin))
                    },
                    onNavigateToRoommates = { navController.navigate(Screen.RoommateList.route) },
                    onNavigateToOwnerDashboard = { navController.navigate(Screen.OwnerDashboard.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
                )
            }
        }

        composable(Screen.Notifications.route) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                NotificationsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { roomId -> navController.navigate(Screen.Chat(roomId).route) },
                    onNavigateToListing = { listingId -> navController.navigate(Screen.ListingDetail.createRoute(listingId, "notifications")) },
                    onNavigateToSupport = { navController.navigate(Screen.SupportTicket.route) }
                )
            }
        }

        composable(
            route = Screen.Search.route,
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "staybuddy://search" })
        ) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                SearchScreen(
                    onNavigateToListingDetail = { listingId ->
                        navController.navigate(Screen.ListingDetail.createRoute(listingId, "search"))
                    },
                    onNavigateToMapView = { navController.navigate(Screen.MapView.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.MapView.route) {
            MapViewScreen(
                onNavigateToListingDetail = { listingId ->
                    navController.navigate(Screen.ListingDetail.createRoute(listingId, "map"))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ListingDetail.ROUTE,
            arguments = listOf(
                navArgument("listingId") { type = NavType.StringType },
                navArgument("origin") { type = NavType.StringType; defaultValue = "home" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                val origin = backStackEntry.arguments?.getString("origin") ?: "home"
                ListingDetailScreen(
                    listingId = listingId,
                    origin = origin,
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.Chat(chatId).route)
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.Favorites.route,
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "staybuddy://favorites" })
        ) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                FavoritesScreen(
                    onNavigateToListingDetail = { listingId ->
                        navController.navigate(Screen.ListingDetail.createRoute(listingId, "favorites"))
                    }
                )
            }
        }

        composable(
            route = Screen.RoommateList.route,
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "staybuddy://roommates" })
        ) {
            RoommateListScreen(
                onNavigateToAddPost = { navController.navigate(Screen.AddRoommatePost.createRoute(null)) },
                onNavigateToEditPost = { postId -> navController.navigate(Screen.AddRoommatePost.createRoute(postId)) },
                onNavigateToChat = { chatId -> navController.navigate(Screen.Chat(chatId).route) },
                onNavigateToQuiz = { navController.navigate(Screen.CompatibilityQuiz.route) }
            )
        }

        composable(
            route = Screen.AddRoommatePost.ROUTE,
            arguments = listOf(navArgument("postId") {
                type = NavType.StringType
                defaultValue = "new"
            })
        ) {
            AddRoommatePostScreen(
                onNavigateBack = { navController.popBackStack() },
                onPostAdded = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddListing.ROUTE,
            arguments = listOf(navArgument("listingId") {
                type = NavType.StringType
                defaultValue = "new"
            })
        ) {
            AddListingScreen(
                onNavigateBack = { navController.popBackStack() },
                onListingAdded = { navController.popBackStack() }
            )
        }

        composable(Screen.OwnerDashboard.route) {
            OwnerDashboardScreen(
                onNavigateToAddListing = { navController.navigate(Screen.AddListing.createRoute(null)) },
                onNavigateToEditListing = { listingId -> 
                    navController.navigate(Screen.AddListing.createRoute(listingId))
                },
                onNavigateToDetail = { listingId ->
                    navController.navigate(Screen.ListingDetail(listingId).route)
                },
                onNavigateToInquiries = {
                    navController.navigate(Screen.OwnerInquiries.route)
                }
            )
        }

        composable(
            route = Screen.OwnerInquiries.route,
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "staybuddy://owner/inquiries" })
        ) {
            com.example.staybuddy.ui.screens.owner.OwnerInquiriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat(chatId).route)
                }
            )
        }

        composable(
            route = Screen.ChatList.route,
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "staybuddy://chatlist" })
        ) {
            ChatListScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat(chatId).route)
                }
            )
        }

        composable(
            route = Screen.Chat.ROUTE,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "staybuddy://chat/{chatId}" })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToOwnerDashboard = { navController.navigate(Screen.OwnerDashboard.route) },
                onNavigateToChatList = { navController.navigate(Screen.ChatList.route) },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToCompatibilityQuiz = { navController.navigate(Screen.CompatibilityQuiz.route) },
                onNavigateToSupportTicket = { navController.navigate("support_ticket") },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SupportTicket.route,
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "staybuddy://support" })
        ) {
            SupportTicketScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FinishRegistration.route) {
            FinishRegistrationScreen(
                onRegistrationComplete = { role ->
                    val dest = if (role.equals("owner", ignoreCase = true)) Screen.OwnerDashboard.route else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CompatibilityQuiz.route) {
            CompatibilityQuizScreen(
                onBack = { navController.popBackStack() },
                onQuizCompleted = { navController.popBackStack() }
            )
        }
    }
        }
    }
}
