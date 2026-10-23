package app.lokey0905.location.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogActionPolicyTest {
    @Test fun uninstalledAppOffersDownloadWithoutPackageActions() {
        val policy = getCatalogActionPolicy(false, null, "2.2.0")
        assertEquals(CatalogPrimaryAction.DOWNLOAD, policy.primaryAction)
        assertFalse(policy.showMore)
        assertFalse(policy.showRemove)
    }

    @Test fun uninstalledAppCanStillUseFallbackWithoutRemoteVersion() {
        assertEquals(CatalogPrimaryAction.DOWNLOAD, getCatalogActionPolicy(false, null, null).primaryAction)
    }

    @Test fun installedAppWithoutRemoteVersionStaysOpenable() {
        val policy = getCatalogActionPolicy(true, "2.2.0", null)
        assertEquals(CatalogPrimaryAction.OPEN, policy.primaryAction)
        assertTrue(policy.showMore)
        assertTrue(policy.showRemove)
    }

    @Test fun installedAppWithoutVersionNameIsNotMisreportedAsUninstalled() {
        assertEquals(CatalogPrimaryAction.OPEN, getCatalogActionPolicy(true, null, "2.2.0").primaryAction)
    }

    @Test fun numericComparisonUsesNumericSegmentsAndNotStringOrder() {
        assertEquals(CatalogPrimaryAction.UPDATE, getCatalogActionPolicy(true, "0.9", "0.10").primaryAction)
        assertEquals(CatalogPrimaryAction.OPEN, getCatalogActionPolicy(true, "0.10", "0.9").primaryAction)
    }

    @Test fun equalSuffixVersionsAndNewerInstalledVersionStayOpenable() {
        assertEquals(CatalogPrimaryAction.OPEN, getCatalogActionPolicy(true, "2.2.0-arm64", "2.2").primaryAction)
        assertEquals(CatalogPrimaryAction.OPEN, getCatalogActionPolicy(true, "2.3.0", "2.2.0").primaryAction)
    }

    @Test fun unknownVersionsDoNotInventAnUpdate() {
        assertEquals(CatalogPrimaryAction.OPEN, getCatalogActionPolicy(true, "unknown", "2.2.0").primaryAction)
        assertEquals(CatalogPrimaryAction.OPEN, getCatalogActionPolicy(true, "2.2.0", "ERROR").primaryAction)
    }

    @Test fun systemHealthConnectOpensSettingsEvenWithoutSeparateInstalledPackage() {
        listOf(true, false).forEach { installed ->
            val policy = getCatalogActionPolicy(installed, null, null, systemManaged = true)
            assertEquals(CatalogPrimaryAction.OPEN_SETTINGS, policy.primaryAction)
            assertFalse(policy.showMore)
            assertFalse(policy.showRemove)
        }
    }

    @Test fun cacheRefreshesOnlyWhenMissingExpiredOrForced() {
        assertTrue(shouldRefreshCatalogSource(null, 1_000))
        assertFalse(shouldRefreshCatalogSource(1_000, 300_999))
        assertTrue(shouldRefreshCatalogSource(1_000, 301_000))
        assertTrue(shouldRefreshCatalogSource(1_000, 1_001, force = true))
        assertTrue(shouldRefreshCatalogSource(1_000, 999))
    }
}
