package app.lokey0905.location.version

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionCheckFailureActionTest {
    private val latestDownloadUrl = "https://discovery.pokemod.dev/dl/pkmd/public"

    @Test
    fun uninstalledAppCanDownloadWithoutVersionResult() {
        assertEquals(
            VersionCheckFailureAction.DOWNLOAD,
            getVersionCheckFailureAction(false, latestDownloadUrl)
        )
    }

    @Test
    fun installedAppKeepsLatestDownloadAvailableWhenVersionCheckFails() {
        assertEquals(
            VersionCheckFailureAction.DOWNLOAD,
            getVersionCheckFailureAction(true, latestDownloadUrl)
        )
    }

    @Test
    fun unavailableVersionSpecificDownloadStillAllowsOpeningInstalledApp() {
        assertEquals(VersionCheckFailureAction.OPEN, getVersionCheckFailureAction(true))
    }

    @Test
    fun unavailableVersionSpecificDownloadRemainsDisabledForUninstalledApp() {
        assertEquals(VersionCheckFailureAction.UNAVAILABLE, getVersionCheckFailureAction(false))
    }

    @Test
    fun blankUrlDoesNotEnableDownload() {
        listOf("", " ", "\n").forEach { url ->
            assertEquals(VersionCheckFailureAction.OPEN, getVersionCheckFailureAction(true, url))
            assertEquals(VersionCheckFailureAction.UNAVAILABLE, getVersionCheckFailureAction(false, url))
        }
    }
}
