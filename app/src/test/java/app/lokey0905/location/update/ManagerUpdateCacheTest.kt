package app.lokey0905.location.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManagerUpdateCacheTest {
    private val releaseJson = """{"tag_name":"2.31","body":"Changes"}"""

    @Test
    fun firstLaunchFetches() {
        assertEquals(ManagerUpdateCacheDecision.Fetch, managerUpdateCacheDecision(0, 0, null, 100))
    }

    @Test
    fun freshSuccessReusesReleaseWithoutNetwork() {
        assertEquals(
            ManagerUpdateCacheDecision.Reuse(ManagerUpdateResult.Release("2.31", "2.31", "Changes")),
            managerUpdateCacheDecision(100, 200, releaseJson, 199)
        )
    }

    @Test
    fun expiredSuccessFetchesInsteadOfClaimingCachedDataIsCurrent() {
        assertEquals(ManagerUpdateCacheDecision.Fetch, managerUpdateCacheDecision(100, 200, releaseJson, 200))
    }

    @Test
    fun restoredFailureDeadlineStillSuppressesRequests() {
        // The same persisted fields apply to a newly created activity or process.
        repeat(3) {
            assertEquals(ManagerUpdateCacheDecision.Wait, managerUpdateCacheDecision(100, 500, null, 499))
        }
        assertEquals(ManagerUpdateCacheDecision.Fetch, managerUpdateCacheDecision(100, 500, null, 500))
    }

    @Test
    fun corruptCachedReleaseDoesNotBecomeLatestVersion() {
        listOf("not json", "{}", """{"tag_name":"garbage"}""").forEach {
            assertEquals(ManagerUpdateCacheDecision.Fetch, managerUpdateCacheDecision(100, 200, it, 150))
        }
    }

    @Test
    fun clockMovedBackwardsDoesNotLeaveAnIndefiniteCooldown() {
        assertEquals(ManagerUpdateCacheDecision.Fetch, managerUpdateCacheDecision(100, 200, null, 99))
    }

    @Test
    fun rateLimitCompletingInBackgroundCanStillBeShownAfterRecreationWithoutNetwork() {
        val failure = ManagerUpdateResult.RateLimited(403, 500)
        val stored = managerUpdateFailureJson(failure)
        assertEquals(
            ManagerUpdateCacheDecision.Reuse(failure),
            managerUpdateCacheDecision(100, 500, null, 499, stored)
        )
        assertEquals(ManagerUpdateCacheDecision.Fetch, managerUpdateCacheDecision(100, 500, null, 500, stored))
    }

    @Test
    fun structuredFailuresRoundTripWithoutBecomingSuccessfulReleases() {
        val failures = listOf(
            ManagerUpdateResult.RateLimited(429, 500),
            ManagerUpdateResult.HttpError(404),
            ManagerUpdateResult.HttpError(503, 500),
            ManagerUpdateResult.InvalidResponse
        ) + UpdateNetworkFailure.entries.map { ManagerUpdateResult.NetworkError(it) }
        failures.forEach { failure ->
            val stored = managerUpdateFailureJson(failure)!!
            assertEquals(failure, parseManagerUpdateFailure(stored))
            assertNull(parseManagerRelease(stored))
        }
        assertNull(managerUpdateFailureJson(ManagerUpdateResult.Release("2.31", "2.31", "Notes")))
    }

    @Test
    fun malformedFailureCacheCanBeRetried() {
        listOf("bad", "{}", """{"kind":"network","reason":"unknown"}""").forEach { stored ->
            assertNull(parseManagerUpdateFailure(stored))
            assertEquals(ManagerUpdateCacheDecision.Fetch, managerUpdateCacheDecision(100, 500, null, 150, stored))
        }
    }
}
