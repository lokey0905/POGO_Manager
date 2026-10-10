package app.lokey0905.location.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PokemodVersionFormatTest {
    @Test
    fun normalizesUnderscoresInCurrentDownloadFileName() {
        assertEquals(
            "13.8.0",
            parsePokemodVersion("https://chronos.pokemod.dev/pokemod_apps/Public/Pokemod-v13_8_0.apk")
        )
    }

    @Test
    fun normalizesUnderscoresInPublicFileNameWithRevisionAndQuery() {
        assertEquals(
            "13.8.0",
            parsePokemodVersion("https://example.com/PokemodPublic-v13_8_0r1380.apk?download=1#apk")
        )
    }

    @Test
    fun acceptsUnderscoreFileNameWithoutExtension() {
        assertEquals("13.8.0", parsePokemodVersion("https://example.com/Pokemod-v13_8_0"))
    }

    @Test
    fun acceptsCaseInsensitiveFileNames() {
        assertEquals("13.8.0", parsePokemodVersion("https://example.com/POKEMOD-v13_8_0.APK"))
    }

    @Test
    fun preservesExistingDotAndLegacyFormats() {
        val versions = mapOf(
            "Pokemod-v13.8.0.apk" to "13.8.0",
            "Pokemod-v13.8.0.apk?download=1" to "13.8.0",
            "PokemodPublic-v11.1.2r1112" to "11.1.2",
            "Pokemod_Public_v1_2_3r" to "1.2.3"
        )
        versions.forEach { (fileName, expected) ->
            assertEquals(fileName, expected, parsePokemodVersion("https://example.com/$fileName"))
        }
    }

    @Test
    fun rejectsMalformedAndUnrelatedFileNames() {
        listOf(
            "another-app-v13_8_0.apk",
            "Pokemod-v13_8.apk",
            "Pokemod-v13_x_0.apk",
            "Pokemod-v13_8_0.apk.bak"
        ).forEach { fileName ->
            assertNull(fileName, parsePokemodVersion("https://example.com/$fileName"))
        }
    }
}
