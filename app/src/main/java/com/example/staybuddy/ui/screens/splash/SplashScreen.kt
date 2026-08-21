package com.example.staybuddy.ui.screens.splash

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.staybuddy.R
import com.example.staybuddy.ui.theme.StayBuddyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Keep the brand moment short — users open the app to find rooms, not watch logos.
private const val MIN_SPLASH_TIME_MS = 1400L

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOwnerDashboard: () -> Unit,
    onNavigateToFinishRegistration: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val startedAt = remember { SystemClock.elapsedRealtime() }
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkAuthState()
    }

    LaunchedEffect(destination) {
        val target = destination ?: return@LaunchedEffect
        if (hasNavigated) return@LaunchedEffect

        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val remaining = MIN_SPLASH_TIME_MS - elapsed
        if (remaining > 0) delay(remaining)

        hasNavigated = true
        when (target) {
            SplashDestination.Onboarding -> onNavigateToOnboarding()
            SplashDestination.Login -> onNavigateToLogin()
            SplashDestination.Home -> onNavigateToHome()
            SplashDestination.OwnerDashboard -> onNavigateToOwnerDashboard()
            SplashDestination.FinishRegistration -> onNavigateToFinishRegistration()
            SplashDestination.MaintenanceMode -> { /* Show maintenance UI — handled below */ }
        }
    }

    if (destination == SplashDestination.MaintenanceMode) {
        MaintenanceScreen()
    } else {
        SplashContent()
    }
}

@Composable
private fun MaintenanceScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "maintenance")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            // Icon with animated glow background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer glow ring
                Surface(
                    modifier = Modifier
                        .size((140 * pulse).dp)
                        .alpha(0.15f),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {}
                // Inner circle
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Under Maintenance",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "We'll be back soon",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "We're making some improvements to serve you better. This won't take long.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Retry button
            Button(
                onClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SplashContent(modifier: Modifier = Modifier) {
    // Animation states
    val iconScale = remember { Animatable(0.6f) }
    val iconAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Icon pop-in
        launch {
            iconAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            iconScale.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
        // Brand name fades in after a beat
        delay(250)
        launch {
            textAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }
        // Tagline last
        delay(200)
        launch {
            taglineAlpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        }
    }

    // Evergreen gradient background matching the app icon's teal-green palette
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF16705B), // Evergreen primary
            Color(0xFF0F5E4B), // Slightly deeper
            Color(0xFF0B4536)  // Dark evergreen
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon — using the foreground mipmap (the nested home shape)
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "StayBuddy",
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Brand name
            Text(
                text = "StayBuddy",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tagline
            Text(
                text = "find your perfect stay",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    StayBuddyTheme {
        SplashContent()
    }
}
