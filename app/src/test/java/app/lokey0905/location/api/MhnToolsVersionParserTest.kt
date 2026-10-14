package app.lokey0905.location.api

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MhnToolsVersionParserTest {
    private val response = """
        {"supportedVersions": {
          "129.0": {"game": {"gameVersionName": "129.0"},
                    "app": {"appVersionCode": 12, "appVersionName": "1.2", "appVersionHash": "old"}},
          "129.1": {"game": {"gameVersionName": "129.1"},
                    "app": {"appVersionCode": 13, "appVersionName": "1.3", "appVersionHash": "new"}}
        }}
    """.trimIndent()

    @Test
    fun parsesReleaseChannelWithExistingDownloadUrlFormat() {
        val result = parseMhnToolsVersionInfo(response, MhnToolsChannel.RELEASE)
        assertEquals(listOf("129.1", "129.0"), result.supportedVersions)
        assertEquals("1.3", result.appVersion)
        assertEquals("https://assets.mhntools.net/mhntools-1.3-new.apk?", result.downloadUrl)
    }

    @Test
    fun parsesTestChannelIndependentlyWithTestDownloadPrefix() {
        val result = parseMhnToolsVersionInfo(response, MhnToolsChannel.TEST)
        assertEquals(listOf("129.1", "129.0"), result.supportedVersions)
        assertEquals("1.3", result.appVersion)
        assertEquals("https://assets.mhntools.net/test-mhntools-1.3-new.apk?", result.downloadUrl)
    }

    @Test
    fun sortsVersionKeysNumericallyAndPreservesGameDisplayNames() {
        val json = """{"supportedVersions": {
            "0.9": {"game": {"gameVersionName": "0.9-arm64"}},
            "0.10": {"game": {"gameVersionName": "0.10-beta"}},
            "0.10.1": {"game": {"gameVersionName": "0.10.1"}}
        }}"""
        assertEquals(
            listOf("0.10.1", "0.10-beta", "0.9-arm64"),
            parseMhnToolsVersionInfo(json, MhnToolsChannel.RELEASE).supportedVersions
        )
    }

    @Test
    fun usesVersionKeyWhenGameOrGameNameIsMissingOrBlank() {
        val json = """{"supportedVersions": {
            "129.0": {}, "129.1": {"game": {}},
            "129.2": {"game": {"gameVersionName": ""}}
        }}"""
        assertEquals(
            listOf("129.2", "129.1", "129.0"),
            parseMhnToolsVersionInfo(json, MhnToolsChannel.RELEASE).supportedVersions
        )
    }

    @Test
    fun picksMaximumAppVersionCodeRatherThanNewestGameOrLexicalAppName() {
        val json = """{"supportedVersions": {
            "130.0": {"app": {"appVersionCode": 9, "appVersionName": "9.9", "appVersionHash": "a"}},
            "129.0": {"app": {"appVersionCode": 10, "appVersionName": "1.0", "appVersionHash": "b"}},
            "128.0": {"app": {"appVersionCode": 8, "appVersionName": "99.0", "appVersionHash": "c"}}
        }}"""
        val result = parseMhnToolsVersionInfo(json, MhnToolsChannel.RELEASE)
        assertEquals("1.0", result.appVersion)
        assertEquals("https://assets.mhntools.net/mhntools-1.0-b.apk?", result.downloadUrl)
    }

    @Test
    fun equalAppVersionCodesRetainFirstInDescendingGameOrder() {
        val json = """{"supportedVersions": {
            "129.0": {"app": {"appVersionCode": 10, "appVersionName": "older", "appVersionHash": "a"}},
            "130.0": {"app": {"appVersionCode": 10, "appVersionName": "newer", "appVersionHash": "b"}}
        }}"""
        assertEquals("newer", parseMhnToolsVersionInfo(json, MhnToolsChannel.RELEASE).appVersion)
    }

    @Test
    fun emptySupportObjectIsAConfirmedEmptyListWithNoMetadata() {
        assertEquals(
            MhnToolsVersionInfo(emptyList(), null, null),
            parseMhnToolsVersionInfo("""{"supportedVersions": {}}""", MhnToolsChannel.RELEASE)
        )
    }

    @Test
    fun missingSupportObjectFailsInsteadOfReturningConfirmedEmptyList() {
        assertThrows(JSONException::class.java) {
            parseMhnToolsVersionInfo("{}", MhnToolsChannel.RELEASE)
        }
    }

    @Test
    fun malformedJsonOrSupportObjectFails() {
        listOf("not json", """{"supportedVersions": []}""", """{"supportedVersions": null}""")
            .forEach { json ->
                assertThrows(JSONException::class.java) {
                    parseMhnToolsVersionInfo(json, MhnToolsChannel.RELEASE)
                }
            }
    }

    @Test
    fun missingOrBlankHashDoesNotProduceDownloadUrl() {
        listOf("", " ").forEach { hash ->
            val json = """{"supportedVersions": {"129.0": {
                "app": {"appVersionCode": 10, "appVersionName": "1.0", "appVersionHash": "$hash"}
            }}}"""
            val result = parseMhnToolsVersionInfo(json, MhnToolsChannel.RELEASE)
            assertEquals("1.0", result.appVersion)
            assertNull(result.downloadUrl)
        }
        val missingHash = """{"supportedVersions": {"129.0": {
            "app": {"appVersionCode": 10, "appVersionName": "1.0"}
        }}}"""
        assertNull(parseMhnToolsVersionInfo(missingHash, MhnToolsChannel.TEST).downloadUrl)
    }

    @Test
    fun missingAppMetadataKeepsSupportWithoutInventingVersionOrUrl() {
        val json = """{"supportedVersions": {"129.0": {"game": {"gameVersionName": "129.0"}}}}"""
        assertEquals(
            MhnToolsVersionInfo(listOf("129.0"), null, null),
            parseMhnToolsVersionInfo(json, MhnToolsChannel.RELEASE)
        )
    }

    @Test
    fun mergesBothChannelsWithNumericOrderAndNoDuplicatesWithoutMutatingInputs() {
        val release = mutableListOf("0.9", "0.10")
        val test = mutableListOf("0.10", "0.11")
        assertEquals(listOf("0.11", "0.10", "0.9"), mergeMhnSupportedVersions(release, test))
        assertEquals(listOf("0.9", "0.10"), release)
        assertEquals(listOf("0.10", "0.11"), test)
    }

    @Test
    fun keepsSuccessfulChannelWhenOtherChannelFails() {
        assertEquals(listOf("0.10", "0.9"), mergeMhnSupportedVersions(listOf("0.9", "0.10"), null))
        assertEquals(listOf("0.10", "0.9"), mergeMhnSupportedVersions(null, listOf("0.9", "0.10")))
        assertEquals(emptyList<String>(), mergeMhnSupportedVersions(null, null))
        assertEquals(emptyList<String>(), mergeMhnSupportedVersions(emptyList(), emptyList()))
    }
}
