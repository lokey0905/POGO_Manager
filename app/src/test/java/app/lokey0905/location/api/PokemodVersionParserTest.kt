package app.lokey0905.location.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PokemodVersionParserTest {
    @Test
    fun parsesCurrentApkFileName() {
        assertEquals(
            "13.0.0",
            parsePokemodVersion(
                "https://chronos.pokemod.dev/pokemod_apps/Public/Pokemod-v13.0.0.apk"
            )
        )
    }

    @Test
    fun parsesCurrentFileNameWithQuery() {
        assertEquals(
            "13.0.0",
            parsePokemodVersion("https://example.com/Pokemod-v13.0.0.apk?download=1")
        )
    }

    @Test
    fun keepsPreviousPublicFormatSupport() {
        assertEquals(
            "11.1.2",
            parsePokemodVersion("https://example.com/PokemodPublic-v11.1.2r1112")
        )
    }

    @Test
    fun keepsLegacyUnderscoreFormatSupport() {
        assertEquals(
            "1.2.3",
            parsePokemodVersion("https://example.com/Pokemod_Public_v1_2_3r")
        )
    }

    @Test
    fun doesNotExtractUnrelatedVersion() {
        assertNull(parsePokemodVersion("https://example.com/another-app-v13.0.0.apk"))
    }
}
