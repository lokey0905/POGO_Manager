package app.lokey0905.location.version

/** Retains MHN's existing source order and empty-list fallback without adding another selection state. */
fun selectMhnTargetVersion(
    availableVersions: List<String>,
    currentVersion: String,
    emptyListFallback: String
): String {
    val selectedIndex = availableVersions.indexOf(currentVersion).coerceAtLeast(0)
    return availableVersions.getOrNull(selectedIndex) ?: emptyListFallback
}

/** Waits for both channels before replacing a selection absent from a partial response. */
fun selectMhnTargetVersionDuringRefresh(
    availableVersions: List<String>,
    currentVersion: String,
    checksComplete: Boolean
): String {
    if (!checksComplete && currentVersion !in availableVersions) {
        return currentVersion
    }
    return selectMhnTargetVersion(
        availableVersions,
        currentVersion,
        availableVersions.firstOrNull() ?: currentVersion
    )
}
