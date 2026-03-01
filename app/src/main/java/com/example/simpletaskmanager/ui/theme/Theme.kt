package com.example.simpletaskmanager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark colour scheme ───────────────────────────────────────────────────────
// Dynamic colour is intentionally disabled so our curated palette is always used.
private val DarkColorScheme = darkColorScheme(
    primary            = Indigo80,
    onPrimary          = DarkBackground,
    primaryContainer   = Color(0xFF1E224A),
    onPrimaryContainer = Indigo80,

    secondary          = IndigoVariant80,
    tertiary           = Teal80,

    background         = DarkBackground,
    onBackground       = Color(0xFFE8EAF6),

    surface            = DarkSurface,
    onSurface          = Color(0xFFDDE0FF),
    surfaceVariant     = DarkSurfaceVar,
    onSurfaceVariant   = Color(0xFFB0B3C6),

    error              = Color(0xFFCF6679),
    outline            = Color(0xFF44475A)
)

// ─── Light colour scheme ──────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = Indigo40,
    onPrimary          = Color.White,
    primaryContainer   = LightSurfaceVar,
    onPrimaryContainer = Indigo40,

    secondary          = IndigoVariant40,
    tertiary           = Teal40,

    background         = LightBackground,
    onBackground       = Color(0xFF1C1B2E),

    surface            = LightSurface,
    onSurface          = Color(0xFF1C1B2E),
    surfaceVariant     = LightSurfaceVar,
    onSurfaceVariant   = Color(0xFF4A4A72),

    error              = Color(0xFFB00020),
    outline            = Color(0xFFB0B3C6)
)

/**
 * [SimpleTaskManagerTheme] wraps the app with a Material 3 theme.
 *
 * SECURE CODING NOTE 1 – Theme preference:
 *   The dark-mode preference is stored via [UserPreferencesRepository] using
 *   EncryptedSharedPreferences (AES-256-GCM). Even UI preferences benefit from
 *   encrypted storage so that device backup/restore cannot expose user intent
 *   metadata to third parties.
 *
 * @param darkTheme  When true, applies the dark colour scheme.
 * @param content    The composable subtree to theme.
 */
@Composable
fun SimpleTaskManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content:   @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Match the status bar to the background colour for a seamless look
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
