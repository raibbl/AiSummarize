package com.raibbl.AiAnalyze.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Base palette ──────────────────────────────────────────────

// Dark theme M3 slots (lighter tones for on-dark surfaces)
val Blue80 = Color(0xFF8ECAE6)
val Teal80 = Color(0xFF64D8CB)
val Cyan80 = Color(0xFFA5D8FF)

// Light theme M3 slots (deeper tones for on-light surfaces)
val Blue40 = Color(0xFF0A6FA2)
val Teal40 = Color(0xFF00897B)
val Cyan40 = Color(0xFF1B8BBE)

// Container / surface base colors
val BluePrimaryContainer80 = Color(0xFF142D48)
val OnBluePrimaryContainer80 = Color(0xFFD0E8F9)
val TealSecondaryContainer80 = Color(0xFF1A3D3A)
val OnTealSecondaryContainer80 = Color(0xFFC4EDE8)
val DarkSurfaceVariant = Color(0xFF1A2840)
val OnDarkSurfaceVariant = Color(0xFFC0CDD9)

val BluePrimaryContainer40 = Color(0xFFD4ECFA)
val OnBluePrimaryContainer40 = Color(0xFF063352)
val TealSecondaryContainer40 = Color(0xFFCCF2ED)
val OnTealSecondaryContainer40 = Color(0xFF003731)
val LightSurfaceVariant = Color(0xFFDDE9F0)
val OnLightSurfaceVariant = Color(0xFF3A4E5C)

val DarkSurface = Color(0xFF0F1A2E)
val DarkBackground = Color(0xFF0A1222)
val LightSurface = Color(0xFFF4F9FC)
val LightBackground = Color(0xFFECF4F9)

// ── Semantic app-level colors ─────────────────────────────────
// Every custom color a component might need lives here.
// UI code reads AppTheme.colors — never references raw hex values.

@Immutable
data class AppColors(
    // Card accent strips (left border)
    val accentLink: Color,
    val accentScreenshot: Color,

    // Type badge backgrounds + text
    val badgeLinkBg: Color,
    val badgeLinkText: Color,
    val badgeScreenshotBg: Color,
    val badgeScreenshotText: Color,
    val badgeInfoBg: Color,
    val badgeInfoText: Color,

    // Card accent for info/other types
    val accentInfo: Color,

    // Timestamp pill
    val timestampBg: Color,
    val timestampText: Color,

    // Top bar
    val topBarContainer: Color,
    val topBarContent: Color,

    // Card border
    val cardBorder: Color,

    // Empty-state icon + text
    val emptyIcon: Color,
    val emptyText: Color,
)

val DarkAppColors = AppColors(
    accentLink = Blue80,
    accentScreenshot = Teal80,
    accentInfo = Cyan80,
    badgeLinkBg = Color(0xFF0D3E66),
    badgeLinkText = Blue80,
    badgeScreenshotBg = Color(0xFF0D3D36),
    badgeScreenshotText = Teal80,
    badgeInfoBg = Color(0xFF1A2F4A),
    badgeInfoText = Cyan80,
    timestampBg = Color(0xFF1E2F48),
    timestampText = Color(0xFF9AB4C8),
    topBarContainer = Color(0xFF0F1F36),
    topBarContent = Blue80,
    cardBorder = Color(0xFF263F58),
    emptyIcon = Color(0xFF3A5068),
    emptyText = Color(0xFF6A8299),
)

val LightAppColors = AppColors(
    accentLink = Blue40,
    accentScreenshot = Teal40,
    accentInfo = Cyan40,
    badgeLinkBg = Color(0xFFD4ECFA),
    badgeLinkText = Color(0xFF063352),
    badgeScreenshotBg = Color(0xFFCCF2ED),
    badgeScreenshotText = Color(0xFF003731),
    badgeInfoBg = Color(0xFFDDE9F0),
    badgeInfoText = Color(0xFF1B5E7A),
    timestampBg = Color(0xFFDDE9F0),
    timestampText = Color(0xFF3A5E6C),
    topBarContainer = Blue40,
    topBarContent = Color.White,
    cardBorder = Color(0xFFCBDAE4),
    emptyIcon = Color(0xFFB8CDD9),
    emptyText = Color(0xFF6A8299),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
