package app.lokey0905.location.version

enum class VersionCheckSource {
    APP_INFO,
    POKEMON_MINIMUM,
    POKEMON_LIST,
    POKEMOD_SUPPORT,
    PGTOOLS,
    PGTOOLS_TEST,
    PGSHARP,
    DATA_HUB,
    POKEMOD_APP,
    DEFIT,
    APKMIRROR,
    POLYGON
}

/** Tracks terminal results, including failures, for one refresh round. */
class VersionCheckProgress(
    expected: Set<VersionCheckSource> = VersionCheckSource.entries.toSet()
) {
    private val remaining = expected.toMutableSet()

    val isComplete: Boolean
        get() = remaining.isEmpty()

    val pending: Set<VersionCheckSource>
        get() = remaining.toSet()

    /** Returns true only for the first transition to all sources being terminal. */
    fun complete(source: VersionCheckSource): Boolean {
        return remaining.remove(source) && remaining.isEmpty()
    }
}

/**
 * A refresh gate independent of Android's lifecycle and clock APIs.
 * Callers should supply a monotonic clock and replace the active round when forcing refresh.
 */
class VersionRefreshPolicy(
    private val freshnessMillis: Long = 300_000L,
    private val failureBackoffMillis: Long = 30_000L
) {
    init {
        require(freshnessMillis >= 0) { "freshnessMillis must not be negative" }
        require(failureBackoffMillis >= 0) { "failureBackoffMillis must not be negative" }
    }

    var isRunning: Boolean = false
        private set

    private var lastFinishedAt: Long? = null
    private var lastSucceeded = false

    fun shouldRefresh(nowMillis: Long, force: Boolean = false): Boolean {
        if (force) return true
        if (isRunning) return false

        val finishedAt = lastFinishedAt ?: return true
        if (nowMillis < finishedAt) return true

        val elapsed = nowMillis - finishedAt
        val waitMillis = if (lastSucceeded) freshnessMillis else failureBackoffMillis
        // A negative elapsed value here means subtraction overflow, not fresh data.
        return elapsed < 0 || elapsed >= waitMillis
    }

    fun onStarted() {
        isRunning = true
    }

    fun onFinished(nowMillis: Long, success: Boolean) {
        isRunning = false
        lastFinishedAt = nowMillis
        lastSucceeded = success
    }

    fun reset() {
        isRunning = false
        lastFinishedAt = null
        lastSucceeded = false
    }
}

/** Keeps exact source names, checking an existing selected version before the rest. */
fun prioritizeVersions(versions: List<String>, selectedVersion: String): List<String> {
    val uniqueVersions = versions.distinct()
    if (uniqueVersions.indexOf(selectedVersion) <= 0) return uniqueVersions

    return buildList(uniqueVersions.size) {
        add(selectedVersion)
        uniqueVersions.forEach { version ->
            if (version != selectedVersion) add(version)
        }
    }
}

/** A result for another version cannot establish support for the selected endpoint. */
fun getProbedCompatibilityStatus(
    selectedVersion: String,
    results: Map<String, Boolean>,
    supportedVersions: List<String>,
    allVersionsChecked: Boolean
): CompatibilityStatus {
    return when (results[selectedVersion]) {
        true -> CompatibilityStatus.Supported
        false -> if (allVersionsChecked) {
            getCompatibilityStatus(selectedVersion, supportedVersions)
        } else {
            CompatibilityStatus.Unsupported
        }
        null -> CompatibilityStatus.Unknown
    }
}

/**
 * Preserves an explicit selection until the available-version list is confirmed.
 * Once confirmed, chooses only an available version in the existing source-list ordering.
 */
fun selectTargetVersion(
    availableVersions: List<String>,
    supportedVersions: List<String>,
    minimumVersion: String,
    currentVersion: String?,
    preserveSelection: Boolean,
    availableVersionsConfirmed: Boolean = true
): String? {
    if (preserveSelection && !availableVersionsConfirmed && !currentVersion.isNullOrBlank()) {
        return currentVersion
    }
    val available = availableVersions.toSet()
    if (preserveSelection && currentVersion != null && currentVersion in available) {
        return currentVersion
    }

    return supportedVersions.firstOrNull { it in available }
        ?: minimumVersion.takeIf { it in available }
        ?: availableVersions.firstOrNull()
}
