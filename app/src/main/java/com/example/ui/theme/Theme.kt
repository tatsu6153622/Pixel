package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ChronoDarkColorScheme = darkColorScheme(
  primary = ElectricCyan,
  onPrimary = ObsidianVoid,
  primaryContainer = DeepCyanContainer,
  onPrimaryContainer = ElectricCyan,
  secondary = HorologyAmber,
  onSecondary = ObsidianVoid,
  secondaryContainer = DeepAmberContainer,
  onSecondaryContainer = HorologyAmber,
  tertiary = SolarCoral,
  onTertiary = ObsidianVoid,
  background = ObsidianVoid,
  onBackground = TitaniumText,
  surface = GraphiteBase,
  onSurface = TitaniumText,
  surfaceVariant = ElevatedSurface,
  onSurfaceVariant = SlateSecondary,
  surfaceContainer = ElevatedSurface,
  surfaceContainerHigh = HighestSurface,
  outline = OutlineHairline,
  outlineVariant = OutlineHairline.copy(alpha = 0.55f),
)

private val ChronoShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ChronoCanvasTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ChronoDarkColorScheme,
    typography = ChronoTypography,
    shapes = ChronoShapes,
    content = content,
  )
}
