package app.lokey0905.location.version

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionCheckPresentationTest {
    @Test fun pendingChecksNeverClaimEverythingIsCurrent() {
        assertEquals(VersionCheckSummary.Loading(12, 0), getVersionCheckSummary(12, 0, 0))
    }

    @Test fun partialResultsKeepLoadingUntilTheLastSourceFinishes() {
        assertEquals(VersionCheckSummary.Loading(1, 0), getVersionCheckSummary(1, 0, 3))
        assertEquals(VersionCheckSummary.UpdatesAvailable(3), getVersionCheckSummary(0, 0, 3))
    }

    @Test fun failuresRemainVisibleDuringAndAfterLoading() {
        assertEquals(VersionCheckSummary.Loading(2, 1), getVersionCheckSummary(2, 1, 0))
        assertEquals(VersionCheckSummary.PartialFailure(1), getVersionCheckSummary(0, 1, 0))
    }

    @Test fun failureTakesPriorityOverAnUpdateCount() {
        assertEquals(VersionCheckSummary.PartialFailure(2), getVersionCheckSummary(0, 2, 5))
    }

    @Test fun successfulCompletionWithoutUpdatesIsComplete() {
        assertEquals(VersionCheckSummary.Complete, getVersionCheckSummary(0, 0, 0))
    }

    @Test fun summaryChangesOnlyAfterTheFinalProgressCompletion() {
        val progress = VersionCheckProgress(setOf(VersionCheckSource.PGTOOLS, VersionCheckSource.POKEMOD_SUPPORT))
        progress.complete(VersionCheckSource.PGTOOLS)
        assertEquals(VersionCheckSummary.Loading(1, 0), getVersionCheckSummary(progress.pending.size, 0, 0))
        progress.complete(VersionCheckSource.POKEMOD_SUPPORT)
        assertEquals(VersionCheckSummary.Complete, getVersionCheckSummary(progress.pending.size, 0, 0))
    }

    @Test fun remoteNotInstalledSentinelIsLoadingNotInstallationStatus() {
        assertEquals(RemoteVersionPresentation.Loading, presentation("未安裝", loading = true))
    }

    @Test fun unavailableRemoteVersionIsUnknownWhenNoCheckIsPending() {
        listOf(null, "", "未安裝", "ERROR").forEach {
            assertEquals(RemoteVersionPresentation.Unknown, presentation(it))
        }
    }

    @Test fun failedCheckWithoutAValueIsNotMerelyUnknown() {
        assertEquals(RemoteVersionPresentation.Failed, presentation("未安裝", failed = true))
    }

    @Test fun failedRefreshKeepsTheVersionButMarksItStale() {
        assertEquals(RemoteVersionPresentation.Stale("1.10"), presentation("1.10", failed = true))
    }

    @Test fun receivedValuesRemainVisibleWhileOtherChecksArePending() {
        assertEquals(RemoteVersionPresentation.Available("1.10-beta"), presentation("1.10-beta", loading = true))
    }

    private fun presentation(version: String?, loading: Boolean = false, failed: Boolean = false) =
        getRemoteVersionPresentation(version, "未安裝", loading, failed)
}
