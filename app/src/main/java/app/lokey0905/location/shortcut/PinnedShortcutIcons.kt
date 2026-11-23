package app.lokey0905.location.shortcut

import androidx.annotation.DrawableRes
import app.lokey0905.location.R

/** One icon registry for newly pinned shortcuts and in-place icon repairs. */
@DrawableRes
internal fun pinnedShortcutIconResource(id: String): Int? = when (id) {
    "manual" -> R.drawable.baseline_menu_book_24
    "coolDownCalculator" -> R.drawable.baseline_calculate_24
    "pokeInfo" -> R.drawable.ic_baseline_catching_pokemon_24
    "pokeList_web", "PGTools_raids" -> R.drawable.baseline_radar_24
    "action_nearbySharing" -> R.drawable.baseline_share_24
    "MiuiXSpace" -> R.drawable.baseline_call_split_24
    "location_settings" -> R.drawable.baseline_edit_location_24
    "developerMode" -> R.drawable.baseline_developer_mode_24
    "accountSettings" -> R.drawable.outline_switch_account_24
    else -> null
}
