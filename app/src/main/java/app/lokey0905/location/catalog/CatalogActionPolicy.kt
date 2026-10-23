package app.lokey0905.location.catalog

import app.lokey0905.location.version.compareVersions

enum class CatalogPrimaryAction { DOWNLOAD, UPDATE, OPEN, OPEN_SETTINGS }

data class CatalogActionPolicy(
    val primaryAction: CatalogPrimaryAction,
    val showMore: Boolean,
    val showRemove: Boolean
)

private val comparableCatalogVersion = Regex("^\\s*[vV]?\\d+(?:\\.\\d+)*")

/** Installed state is explicit: a package may exist without reporting a versionName. */
fun getCatalogActionPolicy(
    installed: Boolean,
    installedVersion: String?,
    latestVersion: String?,
    systemManaged: Boolean = false
): CatalogActionPolicy {
    val action = when {
        systemManaged -> CatalogPrimaryAction.OPEN_SETTINGS
        !installed -> CatalogPrimaryAction.DOWNLOAD
        installedVersion != null && latestVersion != null &&
            comparableCatalogVersion.containsMatchIn(installedVersion) &&
            comparableCatalogVersion.containsMatchIn(latestVersion) &&
            compareVersions(latestVersion, installedVersion) > 0 -> CatalogPrimaryAction.UPDATE
        else -> CatalogPrimaryAction.OPEN
    }
    val packageActionsAllowed = installed && !systemManaged
    return CatalogActionPolicy(action, packageActionsAllowed, packageActionsAllowed)
}

/** Monotonic time keeps wall-clock changes from extending the cache unexpectedly. */
fun shouldRefreshCatalogSource(checkedAtMillis: Long?, nowMillis: Long, force: Boolean = false): Boolean =
    force || checkedAtMillis == null || nowMillis < checkedAtMillis ||
        nowMillis - checkedAtMillis >= 5 * 60 * 1000L
