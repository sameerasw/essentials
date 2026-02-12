package com.sameerasw.essentials.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun EssentialsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pitchBlackTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && pitchBlackTheme) {
                dynamicScheme.copy(
                    background = androidx.compose.ui.graphics.Color.Black,
                    surface = androidx.compose.ui.graphics.Color.Black,
                    surfaceContainer = androidx.compose.ui.graphics.Color.Black,
                    surfaceContainerLowest = androidx.compose.ui.graphics.Color.Black,
                    surfaceContainerLow = androidx.compose.ui.graphics.Color.Black
                )
            } else {
                dynamicScheme
            }
        }

        darkTheme -> {
            if (pitchBlackTheme) {
                DarkColorScheme.copy(
                    background = androidx.compose.ui.graphics.Color.Black,
                    surface = androidx.compose.ui.graphics.Color.Black,
                    surfaceContainer = androidx.compose.ui.graphics.Color.Black,
                    surfaceContainerLowest = androidx.compose.ui.graphics.Color.Black,
                    surfaceContainerLow = androidx.compose.ui.graphics.Color.Black
                )
            } else {
                DarkColorScheme
            }
        }

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}