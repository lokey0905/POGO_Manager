package app.lokey0905.location.ui.layout

/**
 * Divides the rail's content height (after its padding) into equal minimum-height slots.
 * A minimum, rather than a fixed height, preserves large labels and lets Material's
 * existing scroller handle short windows. Callers retain the original natural minimum.
 */
fun navigationRailItemMinimumHeight(
    availableHeightPx: Int,
    visibleItemCount: Int,
    naturalMinimumHeightPx: Int
): Int {
    require(visibleItemCount > 0)
    require(naturalMinimumHeightPx >= 0)
    return maxOf(naturalMinimumHeightPx, availableHeightPx.coerceAtLeast(0) / visibleItemCount)
}
