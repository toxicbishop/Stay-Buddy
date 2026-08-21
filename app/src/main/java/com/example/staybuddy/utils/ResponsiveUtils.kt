package com.example.staybuddy.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ResponsiveUtils {
    
    /**
     * Returns true if the device is a compact phone (width < 600dp).
     */
    @Composable
    fun isCompactScreen(): Boolean {
        return LocalConfiguration.current.screenWidthDp < 600
    }

    /**
     * Dynamic horizontal padding based on screen size.
     * Compact phones get 16.dp, tablets/foldables get 24.dp to prevent stretched out content.
     */
    @Composable
    fun screenPadding(): Dp {
        return if (isCompactScreen()) 16.dp else 24.dp
    }

    /**
     * Dynamic card elevation based on screen size.
     * Foldables might prefer slightly flatter interfaces to accommodate multi-pane.
     */
    @Composable
    fun cardElevation(): Dp {
        return if (isCompactScreen()) 2.dp else 1.dp
    }

    /**
     * Screen width as Dp.
     */
    @Composable
    fun screenWidth(): Dp {
        return LocalConfiguration.current.screenWidthDp.dp
    }

    /**
     * Screen height as Dp.
     */
    @Composable
    fun screenHeight(): Dp {
        return LocalConfiguration.current.screenHeightDp.dp
    }

    /**
     * Scale a font size dynamically but bound within scaling limits (0.85 to 1.3).
     */
    @Composable
    fun dynamicFontSize(baseSizeSp: Float): Float {
        val widthDp = LocalConfiguration.current.screenWidthDp
        val scale = (widthDp / 360f).coerceIn(0.85f, 1.3f)
        return baseSizeSp * scale
    }

    /**
     * Recommended aspect ratio for listing card images on current device.
     */
    @Composable
    fun cardImageAspectRatio(): Float {
        val heightDp = LocalConfiguration.current.screenHeightDp
        return if (heightDp > 800) 1.85f else 1.77f
    }

    /**
     * Adaptive column count based on available screen width.
     */
    @Composable
    fun gridColumns(): Int {
        val widthDp = LocalConfiguration.current.screenWidthDp
        return when {
            widthDp >= 840 -> 3
            widthDp >= 600 -> 2
            else -> 1
        }
    }
}
