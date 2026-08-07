package app.lokey0905.location.api

import app.lokey0905.location.version.CompatibilityStatus
import app.lokey0905.location.version.getCompatibilityStatus
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException

class PgSharpRootCodecTest {
    @Test
    fun derivesExpectedAesKeyAndIvFromRawKeyBytes() {
        val key = PgSharpRootCodec.deriveAesKey()
        val iv = PgSharpRootCodec.deriveIv(key)

        assertEquals("949aa98699e09bf16b0bfd24f07745dc", key.toHex())
        assertEquals("48ea4959029ddca468e1d6035a6b0f2c", iv.toHex())
    }

    @Test
    fun decryptsKnownCiphertext() {
        val decrypted = PgSharpRootCodec.decryptChange(KNOWN_CIPHERTEXT)

        assertEquals(FIXTURE_JSON, decrypted)
    }

    @Test
    fun extractsVersionFromFlexibleAppNames() {
        assertEquals("1.0.15", PgSharpRootCodec.extractAppVersion("PGS Root 1.0.15"))
        assertEquals("1.0.16", PgSharpRootCodec.extractAppVersion("PGS Root v1.0.16"))
    }

    @Test
    fun parsesLatestAppAndOnlyEnabledDVersions() {
        val info = PgSharpRootCodec.parseDecryptedJson(FIXTURE_JSON)

        assertEquals("1.0.16", info.appVersion)
        assertEquals("https://example.com/pgsroot.apk", info.downloadUrl)
        assertEquals("- Test", info.changelog)
        assertEquals(listOf("0.423.1"), info.supportedPogoVersions)
        assertFalse("0.424.0" in info.supportedPogoVersions)
    }

    @Test
    fun ignoresOuterVersionAndUrlFields() {
        val response =
            "{\"ver\":\"100.0.0\",\"url\":\"https://wrong.example/app.apk\"," +
                    "\"change\":\"$KNOWN_CIPHERTEXT\"}"

        val info = PgSharpRootCodec.parseApiResponse(response)

        assertEquals("1.0.16", info.appVersion)
        assertEquals("https://example.com/pgsroot.apk", info.downloadUrl)
    }

    @Test
    fun parsesLiveTopLevelShapeAndIgnoresMVerAndD2() {
        val info = PgSharpRootCodec.parseDecryptedJson(
            "{\"m\":{\"n\":\"PGS Root 1.0.15\",\"ver\":\"9.9.9\"," +
                    "\"u\":\"https://example.com/root.apk\"},\"d\":{" +
                    "\"0.423.1\":{\"v\":true}},\"d2\":{" +
                    "\"0.999.0\":{\"v\":true}}}"
        )

        assertEquals("1.0.15", info.appVersion)
        assertEquals(listOf("0.423.1"), info.supportedPogoVersions)
    }

    @Test
    fun malformedBase64FailsWithoutReturningPartialData() {
        assertThrows(IllegalArgumentException::class.java) {
            PgSharpRootCodec.decryptChange("not-valid-base64!")
        }
    }

    @Test
    fun invalidCiphertextFailsDecryption() {
        assertThrows(GeneralSecurityException::class.java) {
            PgSharpRootCodec.decryptChange("AAAAAAAAAAAAAAAAAAAAAA==")
        }
    }

    @Test
    fun malformedJsonFailsParsing() {
        assertThrows(JSONException::class.java) {
            PgSharpRootCodec.parseDecryptedJson("{not-json")
        }
    }

    @Test
    fun supportedVersionsUseExistingCompatibilityLogic() {
        val supportedVersions = PgSharpRootCodec
            .parseDecryptedJson(FIXTURE_JSON)
            .supportedPogoVersions

        assertEquals(
            CompatibilityStatus.Supported,
            getCompatibilityStatus("0.423.1", supportedVersions)
        )
        assertEquals(
            CompatibilityStatus.UnsupportedWithLatestSupportedVersion("0.423.1"),
            getCompatibilityStatus("0.424.0", supportedVersions)
        )
        assertTrue(supportedVersions.isNotEmpty())
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private companion object {
        const val FIXTURE_JSON =
            "{\"config\":{\"m\":{\"n\":\"PGS Root v1.0.16\",\"u\":" +
                    "\"https://example.com/pgsroot.apk\",\"change\":\"- Test\"},\"d\":{" +
                    "\"0.423.1\":{\"v\":true},\"0.424.0\":{\"v\":false}}}}"

        const val KNOWN_CIPHERTEXT =
            "HK7thxKNdyBlmDIgo0NSJNWLWuYmVr6U2Acx1ZSeGKqepuiXY3OE3GW6wpZIg9d7" +
                    "irtzMETKM0yp51Ihw10sdtuLhoVdqdIhG4WQfEkOBHAV0kohfiB/s0ljfzm2i4nl/" +
                    "QwYMHEbUqBycyOb9RcCFVJRMsH4iUYn9Nb3lovjGWy2twIlIvbfbXg4mXhlzCC4Y" +
                    "UACeZYLiGdXGXb2kuyn+Q=="
    }
}
