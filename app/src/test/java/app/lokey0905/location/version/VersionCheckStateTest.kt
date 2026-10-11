package app.lokey0905.location.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCheckStateTest {
    @Test
    fun defaultProgressIncludesEveryRemoteSource() {
        val progress = VersionCheckProgress()

        assertEquals(
            setOf(
                VersionCheckSource.APP_INFO,
                VersionCheckSource.POKEMON_MINIMUM,
                VersionCheckSource.POKEMON_LIST,
                VersionCheckSource.POKEMOD_SUPPORT,
                VersionCheckSource.PGTOOLS,
                VersionCheckSource.PGTOOLS_TEST,
                VersionCheckSource.PGSHARP,
                VersionCheckSource.DATA_HUB,
                VersionCheckSource.POKEMOD_APP,
                VersionCheckSource.DEFIT,
                VersionCheckSource.APKMIRROR,
                VersionCheckSource.POLYGON
            ),
            progress.pending
        )
        assertFalse(progress.isComplete)
    }

    @Test
    fun oldCompletionSourcesDoNotHideRemainingAppChecks() {
        val progress = VersionCheckProgress()
        val remainingAppChecks = setOf(
            VersionCheckSource.POKEMOD_APP,
            VersionCheckSource.DEFIT,
            VersionCheckSource.APKMIRROR,
            VersionCheckSource.POLYGON
        )

        (VersionCheckSource.entries.toSet() - remainingAppChecks).forEach { source ->
            assertFalse(progress.complete(source))
        }

        assertFalse(progress.isComplete)
        assertEquals(remainingAppChecks, progress.pending)
    }

    @Test
    fun slowDependencyRemainsPendingUntilItFinishes() {
        val progress = VersionCheckProgress()

        VersionCheckSource.entries.filter { it != VersionCheckSource.POKEMOD_SUPPORT }
            .forEach { source -> assertFalse(progress.complete(source)) }

        assertEquals(setOf(VersionCheckSource.POKEMOD_SUPPORT), progress.pending)
        assertTrue(progress.complete(VersionCheckSource.POKEMOD_SUPPORT))
        assertTrue(progress.isComplete)
        assertTrue(progress.pending.isEmpty())
    }

    @Test
    fun duplicateCompletionsCannotFinishEarlyOrNotifyTwice() {
        val progress = VersionCheckProgress(
            setOf(VersionCheckSource.PGTOOLS, VersionCheckSource.POLYGON)
        )

        assertFalse(progress.complete(VersionCheckSource.PGTOOLS))
        assertFalse(progress.complete(VersionCheckSource.PGTOOLS))
        assertFalse(progress.isComplete)
        assertTrue(progress.complete(VersionCheckSource.POLYGON))
        assertFalse(progress.complete(VersionCheckSource.POLYGON))
        assertFalse(progress.complete(VersionCheckSource.PGTOOLS))
    }

    @Test
    fun unexpectedSourceDoesNotChangeProgress() {
        val progress = VersionCheckProgress(setOf(VersionCheckSource.POLYGON))

        assertFalse(progress.complete(VersionCheckSource.PGTOOLS))
        assertEquals(setOf(VersionCheckSource.POLYGON), progress.pending)
        assertTrue(progress.complete(VersionCheckSource.POLYGON))
        assertFalse(progress.complete(VersionCheckSource.PGTOOLS))
    }

    @Test
    fun emptyRoundIsCompleteWithoutAnExtraCompletionEvent() {
        val progress = VersionCheckProgress(emptySet())

        assertTrue(progress.isComplete)
        assertFalse(progress.complete(VersionCheckSource.POLYGON))
    }

    @Test
    fun progressCopiesExpectedSourcesAndExposesSnapshots() {
        val expected = mutableSetOf(VersionCheckSource.POLYGON)
        val progress = VersionCheckProgress(expected)
        val snapshot = progress.pending
        expected.clear()

        assertEquals(setOf(VersionCheckSource.POLYGON), progress.pending)
        assertTrue(progress.complete(VersionCheckSource.POLYGON))
        assertEquals(setOf(VersionCheckSource.POLYGON), snapshot)
    }

    @Test
    fun firstRefreshIsAllowedWithoutClaimingItHasStarted() {
        val policy = VersionRefreshPolicy()

        assertTrue(policy.shouldRefresh(0L))
        assertTrue(policy.shouldRefresh(100L))
        assertFalse(policy.isRunning)
    }

    @Test
    fun successfulRefreshUsesFiveMinuteDefaultFreshness() {
        val policy = VersionRefreshPolicy()
        policy.onStarted()
        policy.onFinished(1_000L, success = true)

        assertFalse(policy.isRunning)
        assertFalse(policy.shouldRefresh(1_000L))
        assertFalse(policy.shouldRefresh(300_999L))
        assertTrue(policy.shouldRefresh(301_000L))
    }

    @Test
    fun failedRefreshUsesThirtySecondDefaultBackoff() {
        val policy = VersionRefreshPolicy()
        policy.onStarted()
        policy.onFinished(1_000L, success = false)

        assertFalse(policy.isRunning)
        assertFalse(policy.shouldRefresh(30_999L))
        assertTrue(policy.shouldRefresh(31_000L))
    }

    @Test
    fun latestCompletionSelectsSuccessOrFailureWindow() {
        val policy = VersionRefreshPolicy(freshnessMillis = 100L, failureBackoffMillis = 10L)
        policy.onStarted()
        policy.onFinished(100L, success = true)
        assertFalse(policy.shouldRefresh(110L))

        policy.onStarted()
        policy.onFinished(120L, success = false)
        assertFalse(policy.shouldRefresh(129L))
        assertTrue(policy.shouldRefresh(130L))

        policy.onStarted()
        policy.onFinished(140L, success = true)
        assertFalse(policy.shouldRefresh(150L))
        assertTrue(policy.shouldRefresh(240L))
    }

    @Test
    fun runningRoundBlocksAutomaticRefreshEvenWhenFreshnessExpires() {
        val policy = VersionRefreshPolicy()
        policy.onStarted()

        assertTrue(policy.isRunning)
        assertFalse(policy.shouldRefresh(1_000_000L))
        assertFalse(policy.shouldRefresh(-1L))
    }

    @Test
    fun forceBypassesBothRunningRoundAndFreshness() {
        val policy = VersionRefreshPolicy()
        policy.onStarted()
        assertTrue(policy.shouldRefresh(100L, force = true))

        policy.onFinished(100L, success = true)
        assertTrue(policy.shouldRefresh(100L, force = true))
        assertFalse(policy.shouldRefresh(100L))
    }

    @Test
    fun backwardsClockAllowsRefreshInsteadOfKeepingStaleData() {
        val policy = VersionRefreshPolicy()
        policy.onStarted()
        policy.onFinished(1_000L, success = true)

        assertTrue(policy.shouldRefresh(999L))

        policy.onStarted()
        policy.onFinished(1_000L, success = false)
        assertTrue(policy.shouldRefresh(999L))
    }

    @Test
    fun elapsedTimeOverflowDoesNotTreatAncientDataAsFresh() {
        val policy = VersionRefreshPolicy()
        policy.onStarted()
        policy.onFinished(Long.MIN_VALUE, success = true)

        assertTrue(policy.shouldRefresh(Long.MAX_VALUE))
    }

    @Test
    fun resetClearsBothRunningStateAndPreviousCompletion() {
        val policy = VersionRefreshPolicy()
        policy.onStarted()
        policy.onFinished(1_000L, success = true)
        policy.onStarted()
        policy.reset()

        assertFalse(policy.isRunning)
        assertTrue(policy.shouldRefresh(1_000L))
    }

    @Test
    fun zeroWaitWindowsAllowImmediateRefreshAfterCompletion() {
        val policy = VersionRefreshPolicy(freshnessMillis = 0L, failureBackoffMillis = 0L)
        policy.onFinished(1_000L, success = true)
        assertTrue(policy.shouldRefresh(1_000L))
        policy.onFinished(1_000L, success = false)
        assertTrue(policy.shouldRefresh(1_000L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeFreshnessIsRejected() {
        VersionRefreshPolicy(freshnessMillis = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeFailureBackoffIsRejected() {
        VersionRefreshPolicy(failureBackoffMillis = -1L)
    }

    @Test
    fun selectedVersionIsCheckedFirstAndOtherVersionsKeepSourceOrder() {
        assertEquals(
            listOf("0.431.0", "0.433.0", "0.432.0", "0.430.0"),
            prioritizeVersions(
                listOf("0.433.0", "0.432.0", "0.431.0", "0.430.0"),
                "0.431.0"
            )
        )
    }

    @Test
    fun duplicatesAreCheckedOnlyOnceWithoutChangingInput() {
        val versions = mutableListOf("0.433.0", "0.431.0", "0.433.0", "0.431.0")
        val original = versions.toList()

        assertEquals(listOf("0.431.0", "0.433.0"), prioritizeVersions(versions, "0.431.0"))
        assertEquals(original, versions)
    }

    @Test
    fun missingSelectionIsNotInsertedIntoTheSourceList() {
        assertEquals(
            listOf("0.433.0", "0.431.0"),
            prioritizeVersions(listOf("0.433.0", "0.431.0", "0.433.0"), "0.432.0")
        )
    }

    @Test
    fun exactVersionNamesAndSuffixesArePreserved() {
        assertEquals(
            listOf("0.431.0", "0.431.0-beta", "0.431.0-arm64", "0.431.0 (測試版)"),
            prioritizeVersions(
                listOf("0.431.0-beta", "0.431.0-arm64", "0.431.0", "0.431.0 (測試版)"),
                "0.431.0"
            )
        )
    }

    @Test
    fun emptyVersionListStaysEmpty() {
        assertTrue(prioritizeVersions(emptyList(), "0.431.0").isEmpty())
    }

    @Test
    fun alreadyFirstSelectionKeepsDistinctSourceOrder() {
        assertEquals(
            listOf("0.433.0", "0.431.0"),
            prioritizeVersions(listOf("0.433.0", "0.431.0", "0.433.0"), "0.433.0")
        )
    }

    @Test
    fun selectedProbeSuccessIsShownBeforeOtherChecksFinish() {
        assertEquals(
            CompatibilityStatus.Supported,
            getProbedCompatibilityStatus(
                "0.431.0",
                mapOf("0.431.0" to true, "0.433.0" to false),
                listOf("0.431.0"),
                allVersionsChecked = false
            )
        )
    }

    @Test
    fun selectedProbeSuccessDoesNotRequireAnAggregatedSupportList() {
        assertEquals(
            CompatibilityStatus.Supported,
            getProbedCompatibilityStatus(
                "0.431.0",
                mapOf("0.431.0" to true),
                emptyList(),
                allVersionsChecked = false
            )
        )
    }

    @Test
    fun earlyUnsupportedResultDoesNotClaimAnIncompleteHighestVersion() {
        assertEquals(
            CompatibilityStatus.Unsupported,
            getProbedCompatibilityStatus(
                "0.433.0",
                mapOf("0.433.0" to false, "0.431.0" to true),
                listOf("0.431.0"),
                allVersionsChecked = false
            )
        )
    }

    @Test
    fun completedProbesShowNumericallyHighestSupportedVersion() {
        assertEquals(
            CompatibilityStatus.UnsupportedWithLatestSupportedVersion("0.10"),
            getProbedCompatibilityStatus(
                "0.11",
                mapOf("0.11" to false, "0.9" to true, "0.10" to true),
                listOf("0.9", "0.10"),
                allVersionsChecked = true
            )
        )
    }

    @Test
    fun missingSelectedProbeRemainsUnknownWhenOtherVersionsAreSupported() {
        listOf(false, true).forEach { allVersionsChecked ->
            assertEquals(
                CompatibilityStatus.Unknown,
                getProbedCompatibilityStatus(
                    "0.433.0",
                    mapOf("0.431.0" to true),
                    listOf("0.431.0"),
                    allVersionsChecked
                )
            )
        }
    }

    @Test
    fun missingExactProbeDoesNotBorrowANumericallyEquivalentEndpointResult() {
        assertEquals(
            CompatibilityStatus.Unknown,
            getProbedCompatibilityStatus(
                "0.431.0-arm64",
                mapOf("0.431.0" to true),
                listOf("0.431.0"),
                allVersionsChecked = true
            )
        )
    }

    @Test
    fun knownUnsupportedProbeWithNoSupportedVersionsStaysUnsupported() {
        listOf(false, true).forEach { allVersionsChecked ->
            assertEquals(
                CompatibilityStatus.Unsupported,
                getProbedCompatibilityStatus(
                    "0.433.0",
                    mapOf("0.433.0" to false),
                    emptyList(),
                    allVersionsChecked
                )
            )
        }
    }

    @Test
    fun currentExplicitSelectionIsPreservedWhileAvailable() {
        assertEquals(
            "0.431.0",
            selectTargetVersion(
                listOf("0.433.0", "0.432.0", "0.431.0"),
                listOf("0.432.0"),
                "0.433.0",
                currentVersion = "0.431.0",
                preserveSelection = true
            )
        )
    }

    @Test
    fun unpreservedProvisionalSelectionYieldsToFirstAvailableSupportedVersion() {
        assertEquals(
            "0.432.0",
            selectTargetVersion(
                listOf("0.433.0", "0.432.0", "0.431.0"),
                listOf("0.434.0", "0.432.0", "0.431.0"),
                "0.433.0",
                currentVersion = "0.433.0",
                preserveSelection = false
            )
        )
    }

    @Test
    fun productionFirstAndListFirstCallbacksChooseTheSameDefault() {
        val available = listOf("0.433.0", "0.432.0", "0.431.0")
        val supported = listOf("0.432.0", "0.431.0")
        val productionFirst = selectTargetVersion(
            available,
            supported,
            "0.431.0",
            currentVersion = null,
            preserveSelection = false
        )
        val provisionalListFirst = selectTargetVersion(
            available,
            emptyList(),
            "0.431.0",
            currentVersion = null,
            preserveSelection = false
        )
        val listFirst = selectTargetVersion(
            available,
            supported,
            "0.431.0",
            currentVersion = provisionalListFirst,
            preserveSelection = false
        )

        assertEquals("0.431.0", provisionalListFirst)
        assertEquals("0.432.0", productionFirst)
        assertEquals(productionFirst, listFirst)
    }

    @Test
    fun unavailableCurrentSelectionFallsBackToSupportedSourceOrder() {
        assertEquals(
            "0.431.0",
            selectTargetVersion(
                listOf("0.433.0", "0.432.0", "0.431.0"),
                listOf("0.431.0", "0.432.0"),
                "0.433.0",
                currentVersion = "0.434.0",
                preserveSelection = true
            )
        )
    }

    @Test
    fun availableMinimumIsUsedWhenNoSupportedVersionIsAvailable() {
        assertEquals(
            "0.431.0",
            selectTargetVersion(
                listOf("0.433.0", "0.431.0"),
                listOf("0.430.0"),
                "0.431.0",
                currentVersion = null,
                preserveSelection = false
            )
        )
    }

    @Test
    fun staleOrInvalidMinimumCannotSelectOutsideAvailableVersions() {
        listOf("0.430.0", "ERROR", "").forEach { minimumVersion ->
            assertEquals(
                "0.433.0",
                selectTargetVersion(
                    listOf("0.433.0", "0.431.0"),
                    listOf("0.434.0"),
                    minimumVersion,
                    currentVersion = "0.435.0",
                    preserveSelection = true
                )
            )
        }
    }

    @Test
    fun noAvailableVersionsCannotProduceAnySelection() {
        assertNull(
            selectTargetVersion(
                emptyList(),
                listOf("0.431.0"),
                "0.431.0",
                currentVersion = "0.431.0",
                preserveSelection = true
            )
        )
    }

    @Test
    fun provisionalSupportedListCannotReplaceRestoredSelection() {
        val provisional = selectTargetVersion(
            listOf("0.431.0"),
            listOf("0.431.0"),
            "0.431.0",
            currentVersion = "0.433.0",
            preserveSelection = true,
            availableVersionsConfirmed = false
        )
        val confirmed = selectTargetVersion(
            listOf("0.433.0", "0.431.0"),
            listOf("0.431.0"),
            "0.431.0",
            currentVersion = provisional,
            preserveSelection = true,
            availableVersionsConfirmed = true
        )

        assertEquals("0.433.0", provisional)
        assertEquals("0.433.0", confirmed)
    }

    @Test
    fun unavailableRestoredSelectionIsReplacedOnlyAfterConfirmation() {
        val provisional = selectTargetVersion(
            emptyList(),
            emptyList(),
            "",
            currentVersion = "0.433.0",
            preserveSelection = true,
            availableVersionsConfirmed = false
        )

        assertEquals("0.433.0", provisional)
        assertEquals(
            "0.431.0",
            selectTargetVersion(
                listOf("0.431.0"),
                listOf("0.431.0"),
                "0.431.0",
                currentVersion = provisional,
                preserveSelection = true,
                availableVersionsConfirmed = true
            )
        )
    }

    @Test
    fun restoredSelectionIsIndependentOfProvisionalCallbackOrder() {
        val restored = "0.433.0"
        val available = listOf("0.433.0", "0.431.0")
        val supported = listOf("0.431.0")
        val productionFirst = selectTargetVersion(
            supported,
            supported,
            "0.431.0",
            currentVersion = restored,
            preserveSelection = true,
            availableVersionsConfirmed = false
        )
        val productionThenList = selectTargetVersion(
            available,
            supported,
            "0.431.0",
            currentVersion = productionFirst,
            preserveSelection = true,
            availableVersionsConfirmed = true
        )
        val listFirst = selectTargetVersion(
            available,
            emptyList(),
            "0.431.0",
            currentVersion = restored,
            preserveSelection = true,
            availableVersionsConfirmed = true
        )
        val listThenProduction = selectTargetVersion(
            available,
            supported,
            "0.431.0",
            currentVersion = listFirst,
            preserveSelection = true,
            availableVersionsConfirmed = true
        )

        assertEquals(restored, productionThenList)
        assertEquals(productionThenList, listThenProduction)
    }

    @Test
    fun provisionalListDoesNotPreserveBlankOrUnrequestedSelection() {
        listOf(null, "", " ").forEach { currentVersion ->
            assertEquals(
                "0.431.0",
                selectTargetVersion(
                    listOf("0.431.0"),
                    listOf("0.431.0"),
                    "0.431.0",
                    currentVersion,
                    preserveSelection = true,
                    availableVersionsConfirmed = false
                )
            )
        }
        assertEquals(
            "0.431.0",
            selectTargetVersion(
                listOf("0.431.0"),
                listOf("0.431.0"),
                "0.431.0",
                currentVersion = "0.433.0",
                preserveSelection = false,
                availableVersionsConfirmed = false
            )
        )
    }
}
