package app.lokey0905.location.ui.layout

/**
 * Chooses columns from the measured content width, already excluding parent padding
 * and navigation. Card margins are per side. Larger fonts reserve more card width;
 * smaller fonts never pack additional cards into the same space.
 */
fun responsiveGridColumnCount(
    availableWidthDp: Float,
    minimumCardWidthDp: Float,
    cardHorizontalMarginDp: Float,
    fontScale: Float,
    maxColumns: Int
): Int {
    require(minimumCardWidthDp.isFinite() && minimumCardWidthDp > 0f)
    require(cardHorizontalMarginDp.isFinite() && cardHorizontalMarginDp >= 0f)
    require(maxColumns >= 1)
    if (!availableWidthDp.isFinite() || availableWidthDp <= 0f) return 1

    val textScale = if (fontScale.isFinite() && fontScale > 0f) {
        fontScale.coerceAtLeast(1f)
    } else {
        1f
    }
    val cellWidthDp = minimumCardWidthDp * textScale + 2f * cardHorizontalMarginDp
    return (availableWidthDp / cellWidthDp).toInt().coerceIn(1, maxColumns)
}
