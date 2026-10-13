package app.lokey0905.location.version

/** Include follow-up work without resetting terminal results from the original refresh. */
fun getPendingVersionCheckCount(
    pending: Set<VersionCheckSource>,
    additionallyActive: Set<VersionCheckSource>
): Int = (pending + additionallyActive).size

/** Presentation only: never controls refresh scheduling or download actions. */
sealed interface VersionCheckSummary {
    data class Loading(val pendingCount: Int, val failedCount: Int) : VersionCheckSummary
    data class PartialFailure(val failedCount: Int) : VersionCheckSummary
    data class UpdatesAvailable(val count: Int) : VersionCheckSummary
    data object Complete : VersionCheckSummary
}

fun getVersionCheckSummary(
    pendingCount: Int,
    failedCount: Int,
    updateCount: Int
): VersionCheckSummary = when {
    pendingCount > 0 -> VersionCheckSummary.Loading(pendingCount, failedCount.coerceAtLeast(0))
    failedCount > 0 -> VersionCheckSummary.PartialFailure(failedCount)
    updateCount > 0 -> VersionCheckSummary.UpdatesAvailable(updateCount)
    else -> VersionCheckSummary.Complete
}

sealed interface RemoteVersionPresentation {
    data object Loading : RemoteVersionPresentation
    data object Unknown : RemoteVersionPresentation
    data object Failed : RemoteVersionPresentation
    data class Available(val version: String) : RemoteVersionPresentation
    data class Stale(val version: String) : RemoteVersionPresentation
}

/** Maps business sentinels to display states without changing the underlying values. */
fun getRemoteVersionPresentation(
    version: String?,
    unavailableSentinel: String,
    loading: Boolean,
    failed: Boolean
): RemoteVersionPresentation {
    val available = version?.takeIf {
        it.isNotBlank() && it != unavailableSentinel && it != "ERROR"
    }
    return when {
        failed && available != null -> RemoteVersionPresentation.Stale(available)
        failed -> RemoteVersionPresentation.Failed
        available != null -> RemoteVersionPresentation.Available(available)
        loading -> RemoteVersionPresentation.Loading
        else -> RemoteVersionPresentation.Unknown
    }
}
