package hu.toliver.muskli.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

// Tabular figures keep weights, reps and timers from jittering as digits change.
private const val TABULAR_FIGURES = "tnum"

internal val MuskliTypography = Typography(
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_FIGURES),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
    bodyMedium = base.bodyMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
)

/** Values typed into a set row; larger than body text so they read at arm's length. */
internal val SetValueTextStyle = TextStyle(
    fontSize = 18.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = TABULAR_FIGURES,
)
