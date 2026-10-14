package app.lokey0905.location.version

import org.junit.Assert.assertEquals
import org.junit.Test

/** MHN uses the same pure compatibility policy as Pokemon, with its existing selection. */
class MhnCompatibilityTest {
    @Test
    fun selectedSupportedMhnVersionUsesTheSharedStatus() {
        val versions = listOf("129.1", "129.0")
        val selected = selectMhnTargetVersion(versions, "129.0", "129.1")
        assertEquals("129.0", selected)
        assertEquals(CompatibilityStatus.Supported, getCompatibilityStatus(selected, versions))
    }

    @Test
    fun changingTargetImmediatelyRecomputesSupportWithoutMutatingVersions() {
        val versions = listOf("129.1", "129.0")
        assertEquals(CompatibilityStatus.Supported, getCompatibilityStatus("129.0", versions))
        assertEquals(
            CompatibilityStatus.UnsupportedWithLatestSupportedVersion("129.1"),
            getCompatibilityStatus("130.0", versions)
        )
        assertEquals(listOf("129.1", "129.0"), versions)
    }

    @Test
    fun highestSupportedMhnVersionUsesNumericOrder() {
        assertEquals(
            CompatibilityStatus.UnsupportedWithLatestSupportedVersion("129.10"),
            getCompatibilityStatus("130.0", listOf("129.9", "129.10", "128.0"))
        )
    }

    @Test
    fun unavailableAndConfirmedEmptyDataAreDifferent() {
        assertEquals(CompatibilityStatus.Unknown, getCompatibilityStatus("129.1", null))
        assertEquals(CompatibilityStatus.Unsupported, getCompatibilityStatus("129.1", emptyList()))
    }

    @Test
    fun refreshCanReturnToUnknownAndThenSupported() {
        val target = "129.1"
        assertEquals(CompatibilityStatus.Supported, getCompatibilityStatus(target, listOf(target)))
        assertEquals(CompatibilityStatus.Unknown, getCompatibilityStatus(target, null))
        assertEquals(CompatibilityStatus.Supported, getCompatibilityStatus(target, listOf(target)))
    }

    @Test
    fun longVersionListsPreserveInputOrderAndSuffixHandling() {
        val versions = (300 downTo 1).map { "129.$it-arm64" }
        assertEquals(CompatibilityStatus.Supported, getCompatibilityStatus("129.10", versions))
        assertEquals("129.300-arm64", versions.first())
        assertEquals("129.1-arm64", versions.last())
    }
}
