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
