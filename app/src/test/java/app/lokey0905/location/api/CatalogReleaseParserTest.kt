package app.lokey0905.location.api

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogReleaseParserTest {
    private fun asset(name: String = "app-debug.apk", url: String =
        "https://github.com/lokey0905/Fitness-Hooks/releases/download/v2.2.1/$name"
    ): String = """{"name":"$name","browser_download_url":"$url"}"""

    private fun release(
        tag: String = "v2.2.1",
        draft: String = "false",
        prerelease: String = "false",
        assets: String = asset()
    ): String = """{"tag_name":"$tag","draft":$draft,"prerelease":$prerelease,"assets":[$assets]}"""

    @Test
    fun parsesPublishedStableFitnessVersionAndExactAsset() {
        assertEquals(
            CatalogRelease("2.2.1", "https://github.com/lokey0905/Fitness-Hooks/releases/download/v2.2.1/app-debug.apk"),
            parseFitnessCatalogRelease(release())
        )
    }

    @Test
    fun acceptsNumericTagWithoutPrefix() {
        assertEquals("2.10.0", parseFitnessCatalogRelease(release(
            tag = "2.10.0",
            assets = asset(url = "https://github.com/lokey0905/Fitness-Hooks/releases/download/2.10.0/app-debug.apk")
        )).versionName)
    }

    @Test
    fun doesNotAcceptDraftPrereleaseOrStringBooleans() {
        listOf(release(draft = "true"), release(prerelease = "true"), release(draft = "\"false\""))
            .forEach { json -> assertThrows(IllegalArgumentException::class.java) { parseFitnessCatalogRelease(json) } }
    }

    @Test
    fun doesNotTreatMissingReleaseFlagsAsStable() {
        assertThrows(JSONException::class.java) {
            parseFitnessCatalogRelease("""{"tag_name":"v2.2.1","assets":[]} """)
        }
    }

    @Test
    fun rejectsNonNumericAndPrereleaseTags() {
        listOf("", "latest", "v2.2.1-beta", "2.2.1/other").forEach { tag ->
            assertThrows(IllegalArgumentException::class.java) { parseFitnessCatalogRelease(release(tag = tag)) }
        }
    }

    @Test
    fun rejectsUntrustedOrMismatchedAssetUrls() {
        val path = "/lokey0905/Fitness-Hooks/releases/download/v2.2.1/app-debug.apk"
        listOf(
            "http://github.com$path",
            "https://example.com$path",
            "https://github.com.example.com$path",
            "https://user@github.com$path",
            "https://github.com:8443$path",
            "https://github.com$path?redirect=https://example.com",
            "https://github.com$path#fragment",
            "https://github.com/another/Fitness-Hooks/releases/download/v2.2.1/app-debug.apk",
            "https://github.com/lokey0905/Fitness-Hooks/releases/download/v2.2.0/app-debug.apk",
            "https://github.com/lokey0905/Fitness-Hooks/releases/download/v2.2.1/%2e%2e/app-debug.apk"
        ).forEach { url ->
            assertThrows(IllegalArgumentException::class.java) {
                parseFitnessCatalogRelease(release(assets = asset(url = url)))
            }
        }
    }

    @Test
    fun ignoresNonApkAssetsAndPrefersPublishedAppDebugName() {
        val assets = listOf(asset("checksums.txt"), asset("other.apk"), asset()).joinToString(",")
        assertEquals(
            "https://github.com/lokey0905/Fitness-Hooks/releases/download/v2.2.1/app-debug.apk",
            parseFitnessCatalogRelease(release(assets = assets)).downloadUrl
        )
    }

    @Test
    fun acceptsSingleRenamedApkButRejectsAmbiguousApks() {
        assertEquals(
            "https://github.com/lokey0905/Fitness-Hooks/releases/download/v2.2.1/Fitness-Hooks.apk",
            parseFitnessCatalogRelease(release(assets = asset("Fitness-Hooks.apk"))).downloadUrl
        )
        assertThrows(IllegalArgumentException::class.java) {
            parseFitnessCatalogRelease(release(assets = "${asset("arm.apk")},${asset("x86.apk")}"))
        }
    }

    @Test
    fun missingApkOrMalformedResponseFailsInsteadOfInventingVersion() {
        assertThrows(IllegalArgumentException::class.java) { parseFitnessCatalogRelease(release(assets = "")) }
        assertThrows(JSONException::class.java) { parseFitnessCatalogRelease("not json") }
    }

    @Test
    fun readsJoystickFromExistingPokemonSource() {
        val json = """{"pogo":{"joystick":{"version":"5.3.3","url":"https://www.mediafire.com/file/id/joystick.apk/file"}},
            "mhn":{"joystick":{"version":"1.0","url":"https://example.com/old.apk"}}}"""
        assertEquals(CatalogRelease("5.3.3", "https://www.mediafire.com/file/id/joystick.apk/file"),
            parseJoystickCatalogRelease(json))
    }

    @Test
    fun joystickMissingMetadataIsFailure() {
        assertThrows(JSONException::class.java) { parseJoystickCatalogRelease("{}") }
        assertThrows(IllegalArgumentException::class.java) {
            parseJoystickCatalogRelease("""{"pogo":{"joystick":{"version":"","url":"https://example.com/a.apk"}}}""")
        }
    }

    @Test
    fun joystickRejectsNonHttpsAndCredentialUrls() {
        listOf("file:///tmp/app.apk", "javascript:alert(1)", "http://example.com/a.apk", "https://user@example.com/a.apk")
            .forEach { url ->
                assertThrows(IllegalArgumentException::class.java) {
                    parseJoystickCatalogRelease("""{"pogo":{"joystick":{"version":"5.3.3","url":"$url"}}}""")
                }
            }
    }

    @Test
    fun preservesExistingApkMirrorBuildSuffixFormatting() {
        assertEquals("0-8-5", catalogApkMirrorVersionToken("0.8.5"))
        assertEquals("1-10-3-35-e9bfdb6", catalogApkMirrorVersionToken("1.10.3 (35-e9bfdb6)"))
    }

    @Test
    fun failedVersionResultCannotBecomeDownloadUrl() {
        listOf("", " ", "ERROR", "not available").forEach { version ->
            assertThrows(IllegalArgumentException::class.java) { catalogApkMirrorVersionToken(version) }
        }
    }
}
