package com.example.staybuddy.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * StayBuddy "Hearth" palette.
 *
 * Warm paper surfaces + deep evergreen primary (trust, home) + clay accent
 * (price, highlights) + sage secondary. Tuned by hand for Material 3 roles.
 */

// ----- Primary · Evergreen -----
val Evergreen = Color(0xFF16705B)
val OnEvergreen = Color(0xFFFFFFFF)
val EvergreenContainer = Color(0xFFA3F2DC)
val OnEvergreenContainer = Color(0xFF00201A)
val EvergreenBright = Color(0xFF87D6BC) // dark-mode primary

// ----- Secondary · Sage -----
val Sage = Color(0xFF4C6358)
val OnSage = Color(0xFFFFFFFF)
val SageContainer = Color(0xFFCEE9DB)
val OnSageContainer = Color(0xFF092017)
val SageBright = Color(0xFFB3CCBF)

// ----- Tertiary · Clay (price & warm highlights) -----
val Clay = Color(0xFF9C4A20)
val OnClay = Color(0xFFFFFFFF)
val ClayContainer = Color(0xFFFFDBC9)
val OnClayContainer = Color(0xFF360F00)
val ClayBright = Color(0xFFFFB68F)

// ----- Error -----
val ErrorRed = Color(0xFFBA1A1A)
val OnErrorRed = Color(0xFFFFFFFF)
val ErrorRedContainer = Color(0xFFFFDAD6)
val OnErrorRedContainer = Color(0xFF410002)
val ErrorRedBright = Color(0xFFFFB4AB)

// ----- Neutrals · Light (warm paper) -----
val PaperBackground = Color(0xFFFAF7F1)
val PaperSurface = Color(0xFFFAF7F1)
val PaperSurfaceVariant = Color(0xFFE7E3D6)
val InkOnPaper = Color(0xFF1C1B17)
val InkSoftOnPaper = Color(0xFF4B4A41)
val PaperOutline = Color(0xFF7B7A6E)
val PaperOutlineVariant = Color(0xFFDDD9CB)
val PaperContainerLowest = Color(0xFFFFFFFF)
val PaperContainerLow = Color(0xFFF5F1E9)
val PaperContainer = Color(0xFFEFEBE2)
val PaperContainerHigh = Color(0xFFE9E5DC)
val PaperContainerHighest = Color(0xFFE3E0D6)

// ----- Neutrals · Dark (green-tinted charcoal) -----
val CharBackground = Color(0xFF131511)
val CharSurface = Color(0xFF131511)
val CharSurfaceVariant = Color(0xFF404943)
val InkOnChar = Color(0xFFE3E1D8)
val InkSoftOnChar = Color(0xFFC0C9C1)
val CharOutline = Color(0xFF8A938C)
val CharOutlineVariant = Color(0xFF404943)
val CharContainerLowest = Color(0xFF0E100C)
val CharContainerLow = Color(0xFF1B1D19)
val CharContainer = Color(0xFF1F221D)
val CharContainerHigh = Color(0xFF2A2C27)
val CharContainerHighest = Color(0xFF353731)

// ----- Semantic (non-M3 roles used by badges & tags) -----
val SuccessGreen = Color(0xFF2E7D32)
val WarningAmber = Color(0xFFB26A00)
val RatingAmber = Color(0xFFE8A000)
val VerifiedTeal = Color(0xFF0E7B6C)

// ----- Legacy aliases (kept so existing screens re-skin to the new brand) -----
@Deprecated("Use MaterialTheme.colorScheme.primary", ReplaceWith("Evergreen"))
val Primary = Evergreen
@Deprecated("Use MaterialTheme.colorScheme.secondary", ReplaceWith("Sage"))
val Secondary = Sage
@Deprecated("Use MaterialTheme.colorScheme.primary", ReplaceWith("Evergreen"))
val RoyalBlue = Evergreen
@Deprecated("Use MaterialTheme.colorScheme.background", ReplaceWith("PaperBackground"))
val SoftOffWhite = PaperBackground
