package app.lokey0905.location.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompatibilityTest {
    @Test
    fun minimumVersionFilterExcludesOlderVersionsAndPreservesOrder() {
        assertEquals(
            listOf("0.423.1", "0.421.1"),
            filterVersionsAtOrAboveMinimum(
                listOf("0.423.1", "0.417.6", "0.421.1"),
                "0.421.1"
            )
        )
    }

    @Test
    fun minimumVersionFilterUsesNumericComparison() {
        assertEquals(
            listOf("0.10", "0.11"),
            filterVersionsAtOrAboveMinimum(
                listOf("0.9", "0.10", "0.11"),
                "0.10"
            )
        )
    }

    @Test
    fun unavailableMinimumVersionLeavesSourceListUnchanged() {
        val versions = listOf("0.423.1", "invalid", "0.417.6")

        assertEquals(versions, filterVersionsAtOrAboveMinimum(versions, ""))
        assertEquals(versions, filterVersionsAtOrAboveMinimum(versions, "ERROR"))
        assertEquals(versions, filterVersionsAtOrAboveMinimum(versions, null))
    }

    @Test
    fun numericComparisonDoesNotUseLexicographicOrdering() {
        assertTrue(compareVersions("0.9", "0.10") < 0)
        assertTrue(compareVersions("0.423.10", "0.423.2") > 0)
        assertTrue(compareVersions("ERROR", "1.0") < 0)
        assertEquals(0, compareVersions("0.423.1-arm64", "0.423.1 (測試版)"))
    }

    @Test
    fun selectedVersionSupportedByEveryToolReturnsSupported() {
        val selectedVersion = "0.423.1"
        val toolVersions = listOf(
            listOf("0.423.1", "0.423.0"),
            listOf("0.423.1-beta", "0.421.1"),
            listOf("0.423.1 (測試版)")
        )

        toolVersions.forEach { supportedVersions ->
            assertEquals(
                CompatibilityStatus.Supported,
                getCompatibilityStatus(selectedVersion, supportedVersions)
            )
        }
    }

    @Test
    fun unsupportedVersionIncludesNumericallyHighestSupportedVersion() {
        assertEquals(
            CompatibilityStatus.UnsupportedWithLatestSupportedVersion("0.10"),
            getCompatibilityStatus("0.11", listOf("0.9", "0.10"))
        )
    }

    @Test
    fun loadedEmptySupportListReturnsUnsupported() {
        assertEquals(
            CompatibilityStatus.Unsupported,
            getCompatibilityStatus("0.423.1", emptyList())
        )
    }

    @Test
    fun supportDataNotLoadedReturnsUnknown() {
        assertEquals(
            CompatibilityStatus.Unknown,
            getCompatibilityStatus("0.423.1", null)
        )
    }

    @Test
    fun unparseableVersionWithoutExactMatchReturnsUnknown() {
        assertEquals(
            CompatibilityStatus.Unknown,
            getCompatibilityStatus("測試版", listOf("0.423.1"))
        )
    }
}
