package app.lokey0905.location.version

enum class VersionCheckFailureAction {
    DOWNLOAD,
    OPEN,
    UNAVAILABLE
}

/** Only pass a download URL that works without parsed version metadata. */
fun getVersionCheckFailureAction(
    installed: Boolean,
    versionIndependentDownloadUrl: String? = null
): VersionCheckFailureAction = when {
    !versionIndependentDownloadUrl.isNullOrBlank() -> VersionCheckFailureAction.DOWNLOAD
    installed -> VersionCheckFailureAction.OPEN
    else -> VersionCheckFailureAction.UNAVAILABLE
}
