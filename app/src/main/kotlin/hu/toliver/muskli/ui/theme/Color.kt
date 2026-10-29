package hu.toliver.muskli.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF3A1300),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8F0E8),
    onSecondaryContainer = Color(0xFF00201C),
    background = Color(0xFFFAFAF9),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFAFAF9),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFEDEAE7),
    onSurfaceVariant = Color(0xFF57534E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F4F2),
    surfaceContainer = Color(0xFFF0EEEC),
    surfaceContainerHigh = Color(0xFFE9E6E3),
    surfaceContainerHighest = Color(0xFFE2DFDB),
    outline = Color(0xFF8A847F),
    outlineVariant = Color(0xFFD6D3D1),
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF3A1300),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0F4F49),
    onSecondaryContainer = Color(0xFFC8F0E8),
    background = Color(0xFF0C0A09),
    onBackground = Color(0xFFF5F5F4),
    surface = Color(0xFF0C0A09),
    onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF2B2826),
    onSurfaceVariant = Color(0xFFA8A29E),
    surfaceContainerLowest = Color(0xFF080706),
    surfaceContainerLow = Color(0xFF141210),
    surfaceContainer = Color(0xFF1C1917),
    surfaceContainerHigh = Color(0xFF262321),
    surfaceContainerHighest = Color(0xFF312D2A),
    outline = Color(0xFF78716C),
    outlineVariant = Color(0xFF3A3633),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

/** Workout-specific colors that have no slot in the Material color scheme. */
@Immutable
data class WorkoutColors(
    val completedRow: Color,
    val warmup: Color,
    val dropset: Color,
    val failure: Color,
)

internal val LightWorkoutColors = WorkoutColors(
    completedRow = Color(0xFFDDF4EF),
    warmup = Color(0xFFB45309),
    dropset = Color(0xFF6D28D9),
    failure = Color(0xFFB91C1C),
)

internal val DarkWorkoutColors = WorkoutColors(
    completedRow = Color(0xFF0F2E2A),
    warmup = Color(0xFFFBBF24),
    dropset = Color(0xFFA78BFA),
    failure = Color(0xFFF87171),
)

internal val LocalWorkoutColors = staticCompositionLocalOf { LightWorkoutColors }
