package hu.toliver.muskli.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

// Own palette instead of dynamic color so the logging screen looks the same on every phone.
@Composable
fun MuskliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWorkoutColors provides if (darkTheme) DarkWorkoutColors else LightWorkoutColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = MuskliTypography,
            shapes = MuskliShapes,
            content = content,
        )
    }
}

object MuskliTheme {
    val workoutColors: WorkoutColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWorkoutColors.current
}
