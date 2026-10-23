package app.lokey0905.location.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRailLayoutPolicyTest {
    @Test
    fun tabletHeightIsDividedAmongFiveDestinations() {
        assertEquals(144, navigationRailItemMinimumHeight(720, 5, 80))
    }

    @Test
    fun usesContentHeightAfterInsetsWithoutSubtractingThemAgain() {
        assertEquals(140, navigationRailItemMinimumHeight(748 - 24 - 24, 5, 80))
    }

    @Test
    fun roundingDoesNotPushTheLastDestinationOutsideTheViewport() {
        for (height in 700..704) {
            val itemHeight = navigationRailItemMinimumHeight(height, 5, 80)
            assertEquals(140, itemHeight)
            assertTrue(height - itemHeight * 5 in 0..4)
        }
    }

    @Test
    fun shortWindowsKeepNaturalMinimumAndAllowScrolling() {
        assertEquals(80, navigationRailItemMinimumHeight(300, 5, 80))
        assertEquals(120, navigationRailItemMinimumHeight(300, 5, 120))
    }

    @Test
    fun resizingCanBothGrowAndShrinkItems() {
        assertEquals(160, navigationRailItemMinimumHeight(800, 5, 80))
        assertEquals(100, navigationRailItemMinimumHeight(500, 5, 80))
        assertEquals(80, navigationRailItemMinimumHeight(300, 5, 80))
        assertEquals(160, navigationRailItemMinimumHeight(800, 5, 80))
    }

    @Test
    fun visibleDestinationCountIsNotHardcoded() {
        assertEquals(200, navigationRailItemMinimumHeight(800, 4, 80))
        assertEquals(800, navigationRailItemMinimumHeight(800, 1, 80))
    }

    @Test
    fun unavailableHeightDoesNotProduceNegativeMinimum() {
        assertEquals(80, navigationRailItemMinimumHeight(0, 5, 80))
        assertEquals(80, navigationRailItemMinimumHeight(-1, 5, 80))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyMenuIsRejectedBeforeDivision() {
        navigationRailItemMinimumHeight(800, 0, 80)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeNaturalMinimumIsRejected() {
        navigationRailItemMinimumHeight(800, 5, -1)
    }
}
