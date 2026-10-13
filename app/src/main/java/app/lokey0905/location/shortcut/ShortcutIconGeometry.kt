package app.lokey0905.location.shortcut

import kotlin.math.min
import kotlin.math.roundToInt

internal data class ShortcutIconGeometry(
    val size: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/** Pure sizing policy shared by bitmap rendering and JVM tests. All dimensions are pixels. */
internal fun shortcutIconGeometry(
    maxWidth: Int,
    maxHeight: Int,
    intrinsicWidth: Int,
    intrinsicHeight: Int
): ShortcutIconGeometry {
    // Bound allocation even if a launcher reports an unexpectedly large icon size.
    fun safeMaximum(value: Int) = if (value > 0) value.coerceAtMost(256) else 96

    val size = min(safeMaximum(maxWidth), safeMaximum(maxHeight))
    // Keep the glyph clear of launcher masks without changing its aspect ratio.
    val inset = size / 6
    val available = size - inset * 2
    val hasIntrinsicSize = intrinsicWidth > 0 && intrinsicHeight > 0
    val sourceWidth = if (hasIntrinsicSize) intrinsicWidth.toDouble() else 1.0
    val sourceHeight = if (hasIntrinsicSize) intrinsicHeight.toDouble() else 1.0
    val scale = min(available / sourceWidth, available / sourceHeight)
    val width = (sourceWidth * scale).roundToInt().coerceIn(1, available)
    val height = (sourceHeight * scale).roundToInt().coerceIn(1, available)
    val left = (size - width) / 2
    val top = (size - height) / 2

    return ShortcutIconGeometry(size, left, top, left + width, top + height)
}
