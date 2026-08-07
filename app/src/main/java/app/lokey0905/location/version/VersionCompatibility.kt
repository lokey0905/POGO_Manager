package app.lokey0905.location.version

sealed interface CompatibilityStatus {
    data object Supported : CompatibilityStatus

    data object Unsupported : CompatibilityStatus

    data class UnsupportedWithLatestSupportedVersion(
        val version: String
    ) : CompatibilityStatus

    data object Unknown : CompatibilityStatus
}

private val numericVersionPrefix = Regex("""^\s*[vV]?(\d+(?:\.\d+)*)""")

/**
 * Compares the leading dot-separated numeric part of two version strings.
 *
 * Suffixes such as "-arm64", "-beta", or " (測試版)" do not change the
 * numeric version. A value without a leading numeric version compares as zero,
 * matching the fallback used by the previous in-screen comparator.
 */
fun compareVersions(first: String, second: String): Int {
    val firstParts = parseNumericVersion(first) ?: listOf("0")
    val secondParts = parseNumericVersion(second) ?: listOf("0")
    val partCount = maxOf(firstParts.size, secondParts.size)

    for (index in 0 until partCount) {
        val firstPart = firstParts.getOrElse(index) { "0" }
        val secondPart = secondParts.getOrElse(index) { "0" }
        val lengthComparison = firstPart.length.compareTo(secondPart.length)
        if (lengthComparison != 0) {
            return lengthComparison
        }

        val valueComparison = firstPart.compareTo(secondPart)
        if (valueComparison != 0) {
            return valueComparison
        }
    }

    return 0
}

fun filterVersionsAtOrAboveMinimum(
    versions: List<String>,
    minimumVersion: String?
): List<String> {
    val minimum = minimumVersion?.trim().takeUnless { it.isNullOrEmpty() }
        ?: return versions
    if (parseNumericVersion(minimum) == null) {
        return versions
    }

    return versions.filter { version ->
        parseNumericVersion(version) != null && compareVersions(version, minimum) >= 0
    }
}

fun getCompatibilityStatus(
    selectedVersion: String?,
    supportedVersions: List<String>?
): CompatibilityStatus {
    val selected = selectedVersion?.trim().takeUnless { it.isNullOrEmpty() }
        ?: return CompatibilityStatus.Unknown
    val versions = supportedVersions
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.distinct()
        ?: return CompatibilityStatus.Unknown

    if (versions.any { it.equals(selected, ignoreCase = true) }) {
        return CompatibilityStatus.Supported
    }

    if (parseNumericVersion(selected) == null) {
        return CompatibilityStatus.Unknown
    }

    val comparableVersions = versions.filter { parseNumericVersion(it) != null }
    if (comparableVersions.any { compareVersions(it, selected) == 0 }) {
        return CompatibilityStatus.Supported
    }

    val latestSupportedVersion = comparableVersions.maxWithOrNull(::compareVersions)
    return when {
        latestSupportedVersion != null -> {
            CompatibilityStatus.UnsupportedWithLatestSupportedVersion(
                latestSupportedVersion
            )
        }

        versions.isEmpty() -> CompatibilityStatus.Unsupported
        else -> CompatibilityStatus.Unknown
    }
}

private fun parseNumericVersion(version: String): List<String>? {
    val match = numericVersionPrefix.find(version) ?: return null
    return match.groupValues[1]
        .split('.')
        .map { part -> part.trimStart('0').ifEmpty { "0" } }
}
