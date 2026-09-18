package com.nj031.onetask.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// One Task's centralized theme-token architecture (Profile & Settings >
// General > Appearance). Every screen reads its colors from
// MaterialTheme.colorScheme (wired up in Theme.kt), never from these values
// directly - so swapping which OneTaskColorPalette below feeds that
// ColorScheme is enough to recolor the whole app without touching any
// screen's own layout/structure.
// ============================================================================

/** Every semantic color token a single color theme (in one display mode) defines. */
data class OneTaskColorPalette(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val border: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val disabled: Color
)

// ----------------------------------------------------------------------------
// Blue - Light. This is One Task's original, already-shipped light theme: the
// 7 values below are the exact pre-existing constants (same names, same hex)
// this app has always used, kept unchanged. The handful of tokens Blue Light
// never previously defined (primaryDark/accent/mutedText/success/error/
// warning/disabled) are new and reserved for future use - Theme.kt's Blue
// Light color scheme does not read them, so their exact values have no
// visible effect today.
// ----------------------------------------------------------------------------
val OneTaskBackground = Color(0xFFF4F7FC)
val OneTaskPrimary = Color(0xFF2F6FD6)
val OneTaskLightBlue = Color(0xFFDBEBFA)
val OneTaskDarkText = Color(0xFF17365D)
val OneTaskSecondaryText = Color(0xFF6B7C93)
val OneTaskSurface = Color(0xFFFFFFFF)
val OneTaskBorder = Color(0xFFD5E1EE)

val BlueLightPalette = OneTaskColorPalette(
    primary = OneTaskPrimary,
    primaryDark = Color(0xFF1B4C99),
    primaryLight = OneTaskLightBlue,
    accent = Color(0xFF5B93E0),
    background = OneTaskBackground,
    surface = OneTaskSurface,
    elevatedSurface = OneTaskSurface,
    primaryText = OneTaskDarkText,
    secondaryText = OneTaskSecondaryText,
    mutedText = Color(0xFF9AA9B8),
    border = OneTaskBorder,
    success = Color(0xFF16A34A),
    error = Color(0xFFB3261E),
    warning = Color(0xFFE9A23B),
    disabled = Color(0xFFB8C5D3)
)

// ----------------------------------------------------------------------------
// Blue - Dark
// ----------------------------------------------------------------------------
val BlueDarkPalette = OneTaskColorPalette(
    primary = Color(0xFF2684FF),
    primaryDark = Color(0xFF1565C0),
    primaryLight = Color(0xFF102F52),
    accent = Color(0xFF4DA3FF),
    background = Color(0xFF0B1118),
    surface = Color(0xFF151D26),
    elevatedSurface = Color(0xFF1D2733),
    primaryText = Color(0xFFF3F7FB),
    secondaryText = Color(0xFFA8B5C5),
    mutedText = Color(0xFF707E8F),
    border = Color(0xFF293541),
    success = Color(0xFF22C55E),
    error = Color(0xFFF05252),
    warning = Color(0xFFF59E0B),
    disabled = Color(0xFF4B5663)
)

// ----------------------------------------------------------------------------
// Green
// ----------------------------------------------------------------------------
val GreenLightPalette = OneTaskColorPalette(
    primary = Color(0xFF22B455),
    primaryDark = Color(0xFF128A3D),
    primaryLight = Color(0xFFE8F8EE),
    accent = Color(0xFF39C568),
    background = Color(0xFFF6FBF8),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFFFFFFF),
    primaryText = Color(0xFF16352A),
    secondaryText = Color(0xFF6B7D75),
    mutedText = Color(0xFF9AA9A2),
    border = Color(0xFFDDEAE2),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC4C4C),
    warning = Color(0xFFE9A23B),
    disabled = Color(0xFFB8C5BE)
)

val GreenDarkPalette = OneTaskColorPalette(
    primary = Color(0xFF32C866),
    primaryDark = Color(0xFF159447),
    primaryLight = Color(0xFF123D27),
    accent = Color(0xFF4ADE80),
    background = Color(0xFF0B1210),
    surface = Color(0xFF141D19),
    elevatedSurface = Color(0xFF1C2822),
    primaryText = Color(0xFFF2F7F4),
    secondaryText = Color(0xFFA8B8AF),
    mutedText = Color(0xFF718078),
    border = Color(0xFF29362F),
    success = Color(0xFF22C55E),
    error = Color(0xFFF05252),
    warning = Color(0xFFF59E0B),
    disabled = Color(0xFF4B5751)
)

// ----------------------------------------------------------------------------
// Teal
// ----------------------------------------------------------------------------
val TealLightPalette = OneTaskColorPalette(
    primary = Color(0xFF12AFC0),
    primaryDark = Color(0xFF087F91),
    primaryLight = Color(0xFFDDF7FA),
    accent = Color(0xFF24C1D0),
    background = Color(0xFFF5FCFC),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFFFFFFF),
    primaryText = Color(0xFF102A43),
    secondaryText = Color(0xFF65758B),
    mutedText = Color(0xFF9AA9B8),
    border = Color(0xFFD9E9EC),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC4C4C),
    warning = Color(0xFFE6A23C),
    disabled = Color(0xFFB7C5C9)
)

val TealDarkPalette = OneTaskColorPalette(
    primary = Color(0xFF12B8C8),
    primaryDark = Color(0xFF078A99),
    primaryLight = Color(0xFF103A40),
    accent = Color(0xFF2DD4E3),
    background = Color(0xFF0B1215),
    surface = Color(0xFF141E22),
    elevatedSurface = Color(0xFF1B282D),
    primaryText = Color(0xFFF3F7F8),
    secondaryText = Color(0xFFA7B8BD),
    mutedText = Color(0xFF71858B),
    border = Color(0xFF29383D),
    success = Color(0xFF22C55E),
    error = Color(0xFFF05252),
    warning = Color(0xFFF59E0B),
    disabled = Color(0xFF4B5A5F)
)

// ----------------------------------------------------------------------------
// Amber
// ----------------------------------------------------------------------------
val AmberLightPalette = OneTaskColorPalette(
    primary = Color(0xFFF59E0B),
    primaryDark = Color(0xFFD97706),
    primaryLight = Color(0xFFFFF3D6),
    accent = Color(0xFFFBBF24),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFFFFFFF),
    primaryText = Color(0xFF172B3A),
    secondaryText = Color(0xFF718096),
    mutedText = Color(0xFFA0AEC0),
    border = Color(0xFFF1E4D0),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC4C4C),
    warning = Color(0xFFD97706),
    disabled = Color(0xFFC5B9A8)
)

val AmberDarkPalette = OneTaskColorPalette(
    primary = Color(0xFFFF9F0A),
    primaryDark = Color(0xFFD97706),
    primaryLight = Color(0xFF4A2D08),
    accent = Color(0xFFFFB52E),
    background = Color(0xFF0D1117),
    surface = Color(0xFF171C24),
    elevatedSurface = Color(0xFF1E2530),
    primaryText = Color(0xFFF5F7FA),
    secondaryText = Color(0xFFAAB4C3),
    mutedText = Color(0xFF737E8E),
    border = Color(0xFF2B323D),
    success = Color(0xFF22C55E),
    error = Color(0xFFF05252),
    warning = Color(0xFFF59E0B),
    disabled = Color(0xFF4B5360)
)

// ----------------------------------------------------------------------------
// Pink (replaces Purple - Purple is not one of this app's theme options)
// ----------------------------------------------------------------------------
val PinkLightPalette = OneTaskColorPalette(
    primary = Color(0xFFE91E63),
    primaryDark = Color(0xFFC2185B),
    primaryLight = Color(0xFFFCE4EC),
    accent = Color(0xFFEC407A),
    background = Color(0xFFFFF7FA),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFFFFFFF),
    primaryText = Color(0xFF172B3A),
    secondaryText = Color(0xFF718096),
    mutedText = Color(0xFFA0AEC0),
    border = Color(0xFFF3DCE5),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC4C4C),
    warning = Color(0xFFE6A23C),
    disabled = Color(0xFFC9B5BE)
)

val PinkDarkPalette = OneTaskColorPalette(
    primary = Color(0xFFFF2D73),
    primaryDark = Color(0xFFD81B60),
    primaryLight = Color(0xFF4A182C),
    accent = Color(0xFFFF4F8B),
    background = Color(0xFF0D1117),
    surface = Color(0xFF171C24),
    elevatedSurface = Color(0xFF1E2430),
    primaryText = Color(0xFFF5F7FA),
    secondaryText = Color(0xFFA8B1C0),
    mutedText = Color(0xFF727C8C),
    border = Color(0xFF2A303B),
    success = Color(0xFF22C55E),
    error = Color(0xFFF05252),
    warning = Color(0xFFF59E0B),
    disabled = Color(0xFF4B5360)
)
