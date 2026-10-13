package app.lokey0905.location.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutIconGeometryTest {
    @Test
    fun squareGlyphHasSafeInsets() {
        assertEquals(
            ShortcutIconGeometry(96, 16, 16, 80, 80),
            shortcutIconGeometry(96, 96, 24, 24)
        )
    }

    @Test
    fun wideGlyphPreservesAspectRatioAndIsCentered() {
        assertEquals(
            ShortcutIconGeometry(96, 16, 32, 80, 64),
            shortcutIconGeometry(96, 96, 48, 24)
        )
    }

    @Test
    fun tallGlyphPreservesAspectRatioAndIsCentered() {
        assertEquals(
            ShortcutIconGeometry(96, 32, 16, 64, 80),
            shortcutIconGeometry(96, 96, 24, 48)
        )
    }

    @Test
    fun rectangularLauncherLimitsUseTheSmallerMaximum() {
        assertEquals(96, shortcutIconGeometry(192, 96, 24, 24).size)
        assertEquals(96, shortcutIconGeometry(96, 192, 24, 24).size)
    }

    @Test
    fun unreportedLauncherLimitsUseABoundedFallback() {
        assertEquals(96, shortcutIconGeometry(0, -1, 24, 24).size)
        assertEquals(48, shortcutIconGeometry(0, 48, 24, 24).size)
    }

    @Test
    fun excessiveLauncherSizesDoNotAllocateAnUnboundedBitmap() {
        assertEquals(256, shortcutIconGeometry(Int.MAX_VALUE, Int.MAX_VALUE, 24, 24).size)
    }

    @Test
    fun missingIntrinsicSizeFallsBackToASquare() {
        val expected = ShortcutIconGeometry(96, 16, 16, 80, 80)
        assertEquals(expected, shortcutIconGeometry(96, 96, -1, -1))
        assertEquals(expected, shortcutIconGeometry(96, 96, 24, 0))
    }

    @Test
    fun smallestAndExtremeAspectRatiosAlwaysHavePositiveInBoundsGeometry() {
        for (size in listOf(1, 2, 5, 95, 96, 256, Int.MAX_VALUE)) {
            for ((width, height) in listOf(24 to 24, 1 to Int.MAX_VALUE, Int.MAX_VALUE to 1)) {
                val bounds = shortcutIconGeometry(size, size, width, height)
                assertTrue(bounds.size in 1..256)
                assertTrue(bounds.left >= 0)
                assertTrue(bounds.top >= 0)
                assertTrue(bounds.right > bounds.left)
                assertTrue(bounds.bottom > bounds.top)
                assertTrue(bounds.right <= bounds.size)
                assertTrue(bounds.bottom <= bounds.size)
                assertTrue(kotlin.math.abs(bounds.left - (bounds.size - bounds.right)) <= 1)
                assertTrue(kotlin.math.abs(bounds.top - (bounds.size - bounds.bottom)) <= 1)
            }
        }
    }
}
