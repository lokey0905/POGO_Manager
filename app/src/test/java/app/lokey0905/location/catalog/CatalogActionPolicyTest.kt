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

    @Test fun systemSettingsDoNotReplaceStandaloneApkDownload() {
        val policy = getCatalogActionPolicy(false, null, null, hasSystemSettings = true)
        assertEquals(CatalogPrimaryAction.DOWNLOAD, policy.primaryAction)
        assertFalse(policy.showMore)
        assertFalse(policy.showRemove)
        assertTrue(policy.showSystemSettings)
    }

    @Test fun installedStandaloneApkKeepsItsOwnActionsAlongsideSystemSettings() {
        listOf("1.0.0", null).forEach { version ->
            val policy = getCatalogActionPolicy(true, version, null, hasSystemSettings = true)
            assertEquals(CatalogPrimaryAction.OPEN, policy.primaryAction)
            assertTrue(policy.showMore)
            assertTrue(policy.showRemove)
            assertTrue(policy.showSystemSettings)
        }
    }

    @Test fun uninstallingStandaloneApkRestoresDownloadButKeepsSystemSettings() {
        val before = getCatalogActionPolicy(true, "1.0.0", null, hasSystemSettings = true)
        val after = getCatalogActionPolicy(false, null, null, hasSystemSettings = true)
        assertEquals(CatalogPrimaryAction.OPEN, before.primaryAction)
        assertEquals(CatalogPrimaryAction.DOWNLOAD, after.primaryAction)
        assertTrue(after.showSystemSettings)
        assertFalse(after.showRemove)
    }

    @Test fun systemSettingsAreOnlyOfferedForHealthConnectOnAndroid14AndAbove() {
        listOf(28, 32, 33).forEach { api ->
            assertFalse(hasSystemHealthConnectSettings(CatalogApp.HEALTH_CONNECT, api))
        }
        listOf(34, 35, 36, 37).forEach { api ->
            assertTrue(hasSystemHealthConnectSettings(CatalogApp.HEALTH_CONNECT, api))
        }
        CatalogApp.entries.filter { it != CatalogApp.HEALTH_CONNECT }.forEach { app ->
            assertFalse(hasSystemHealthConnectSettings(app, 34))
        }
    }

    @Test fun android13HealthConnectKeepsApkOnlyActions() {
        listOf(true, false).forEach { installed ->
            val policy = getCatalogActionPolicy(
                installed, null, null,
                hasSystemSettings = hasSystemHealthConnectSettings(CatalogApp.HEALTH_CONNECT, 33)
            )
            assertEquals(if (installed) CatalogPrimaryAction.OPEN else CatalogPrimaryAction.DOWNLOAD, policy.primaryAction)
            assertFalse(policy.showSystemSettings)
            assertEquals(installed, policy.showMore)
            assertEquals(installed, policy.showRemove)
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
