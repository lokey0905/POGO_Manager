package app.lokey0905.location.shortcut

import app.lokey0905.location.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinnedShortcutIconsTest {
    private val expectedIcons = mapOf(
        "manual" to R.drawable.baseline_menu_book_24,
        "coolDownCalculator" to R.drawable.baseline_calculate_24,
        "pokeInfo" to R.drawable.ic_baseline_catching_pokemon_24,
        "pokeList_web" to R.drawable.baseline_radar_24,
        "PGTools_raids" to R.drawable.baseline_radar_24,
        "action_nearbySharing" to R.drawable.baseline_share_24,
        "MiuiXSpace" to R.drawable.baseline_call_split_24,
        "location_settings" to R.drawable.baseline_edit_location_24,
        "developerMode" to R.drawable.baseline_developer_mode_24,
        "accountSettings" to R.drawable.outline_switch_account_24
    )

    @Test
    fun allExistingShortcutIdsKeepTheirOriginalIcons() {
        expectedIcons.forEach { (id, drawable) ->
            assertEquals(id, drawable, pinnedShortcutIconResource(id))
        }
    }

    @Test
    fun unknownAndWidgetIdsAreNotMigrated() {
        listOf("", "unknown", "LocationAccuracyActivity", "download_gpx1", "pgtoolsFile").forEach {
            assertNull(it, pinnedShortcutIconResource(it))
        }
    }

    @Test
    fun shortcutIdsRemainCaseSensitive() {
        listOf("Manual", "pgtools_raids", "miuixspace").forEach {
            assertNull(it, pinnedShortcutIconResource(it))
        }
    }

    @Test
    fun registryCoversEveryShortcutCreatedByThePage() {
        val path = "src/main/java/app/lokey0905/location/fragment/ShortCuts.kt"
        val source = listOf(File(path), File("app", path)).first { it.isFile }.readText()
        val ids = Regex("""createShortcut\(\s*"([^"]+)"""")
            .findAll(source).map { it.groupValues[1] }.toSet()
        assertEquals(expectedIcons.keys, ids)
    }
}
