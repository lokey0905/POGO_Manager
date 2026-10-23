package app.lokey0905.location.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponsiveGridPolicyTest {
    private fun homeColumns(width: Float, fontScale: Float = 1f) =
        responsiveGridColumnCount(width, 280f, 8f, fontScale, 2)

    @Test
    fun singleColumnPortraitRestoresOriginalCardWidth() {
        val columns = homeColumns(381f)
        assertEquals(0, homeCardMarginPx(columns, isLandscape = false, spacedMarginPx = 21))
    }

    @Test
    fun widePortraitRetainsSpacingBetweenCardsAndTheSameColumnThreshold() {
        assertEquals(1, homeColumns(591f))
        assertEquals(2, homeColumns(592f))
        assertEquals(21, homeCardMarginPx(homeColumns(592f), isLandscape = false, spacedMarginPx = 21))
    }

    @Test
    fun landscapeRetainsOriginalMarginsEvenWhenLargeFontsRequireOneColumn() {
        assertEquals(21, homeCardMarginPx(1, isLandscape = true, spacedMarginPx = 21))
        assertEquals(21, homeCardMarginPx(2, isLandscape = true, spacedMarginPx = 21))
    }

    @Test
    fun marginsFollowResizesAndOrientationChangesWithoutRememberingOldColumns() {
        assertEquals(21, homeCardMarginPx(2, isLandscape = false, spacedMarginPx = 21))
        assertEquals(0, homeCardMarginPx(1, isLandscape = false, spacedMarginPx = 21))
        assertEquals(21, homeCardMarginPx(1, isLandscape = true, spacedMarginPx = 21))
    }

    @Test
    fun narrowContentUsesOneColumn() {
        assertEquals(1, homeColumns(280f))
        assertEquals(1, homeColumns(381f))
    }

    @Test
    fun exactTwoColumnBoundaryIncludesBothCardMargins() {
        assertEquals(1, homeColumns(591f))
        assertEquals(2, homeColumns(592f))
        assertEquals(2, homeColumns(593f))
    }

    @Test
    fun largeHomeWindowsNeverExceedTwoColumns() {
        assertEquals(2, homeColumns(4_000f))
    }

    @Test
    fun largeFontsIncreaseTheRequiredCardWidth() {
        assertEquals(2, homeColumns(900f))
        assertEquals(1, homeColumns(900f, fontScale = 2f))
        assertEquals(1, homeColumns(1_151f, fontScale = 2f))
        assertEquals(2, homeColumns(1_152f, fontScale = 2f))
    }

    @Test
    fun smallerFontsDoNotIncreaseColumnDensity() {
        assertEquals(1, homeColumns(500f, fontScale = 0.75f))
        assertEquals(2, homeColumns(592f, fontScale = 0.75f))
    }

    @Test
    fun unavailableOrInvalidMeasuredWidthsUseOneColumn() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { width ->
            assertEquals(1, homeColumns(width))
        }
    }

    @Test
    fun invalidFontScaleFallsBackToNormalTextSize() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { scale ->
            assertEquals(2, homeColumns(592f, fontScale = scale))
        }
    }

    @Test
    fun exceptionallyLargeFontScaleStillReturnsOneColumn() {
        assertEquals(1, homeColumns(592f, fontScale = Float.MAX_VALUE))
    }

    @Test
    fun policySupportsDifferentCardSizesAndColumnLimits() {
        assertEquals(1, responsiveGridColumnCount(359f, 148f, 16f, 1f, 4))
        assertEquals(2, responsiveGridColumnCount(360f, 148f, 16f, 1f, 4))
        assertEquals(3, responsiveGridColumnCount(540f, 148f, 16f, 1f, 4))
        assertEquals(4, responsiveGridColumnCount(720f, 148f, 16f, 1f, 4))
        assertEquals(4, responsiveGridColumnCount(2_000f, 148f, 16f, 1f, 4))
        assertEquals(2, responsiveGridColumnCount(720f, 148f, 16f, 2f, 4))
    }

    @Test
    fun zeroMarginsAndSingleColumnMaximumAreSupported() {
        assertEquals(2, responsiveGridColumnCount(560f, 280f, 0f, 1f, 2))
        assertEquals(1, responsiveGridColumnCount(1_200f, 280f, 0f, 1f, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonpositiveCardWidthIsRejected() {
        responsiveGridColumnCount(600f, 0f, 8f, 1f, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCardWidthIsRejected() {
        responsiveGridColumnCount(600f, Float.NaN, 8f, 1f, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeMarginsAreRejected() {
        responsiveGridColumnCount(600f, 280f, -1f, 1f, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMarginsAreRejected() {
        responsiveGridColumnCount(600f, 280f, Float.POSITIVE_INFINITY, 1f, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonpositiveMaximumIsRejected() {
        responsiveGridColumnCount(600f, 280f, 8f, 1f, 0)
    }
}
