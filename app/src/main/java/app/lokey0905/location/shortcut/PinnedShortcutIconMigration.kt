package app.lokey0905.location.shortcut

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val MIGRATION_VERSION = 1
private val migrationMutex = Mutex()

/** Repair D3 bitmap icons once, without replacing labels, intents, IDs or launcher placement. */
internal suspend fun migratePinnedShortcutIcons(context: Context) = withContext(Dispatchers.IO) {
    migrationMutex.withLock {
        try {
            val preferences = context.getSharedPreferences("pinned_shortcut_icons", Context.MODE_PRIVATE)
            if (preferences.getInt("resource_icon_version", 0) >= MIGRATION_VERSION) return@withLock
            // Leave MIUI's existing workaround and cached icons unchanged.
            if (hasMiuiSecurityCore(context)) return@withLock

            val manager = context.getSystemService(ShortcutManager::class.java) ?: return@withLock
            val updates = manager.pinnedShortcuts.mapNotNull { shortcut ->
                if (shortcut.isImmutable) return@mapNotNull null
                val iconResource = pinnedShortcutIconResource(shortcut.id) ?: return@mapNotNull null
                // updateShortcuts accepts partial records: omitted fields keep their existing values.
                ShortcutInfo.Builder(context, shortcut.id)
                    .setIcon(Icon.createWithResource(context, iconResource))
                    .build()
            }
            if (updates.isNotEmpty() && !manager.updateShortcuts(updates)) {
                Log.w("PinnedShortcutIcons", "Icon repair rate-limited; retry on next app start")
                return@withLock
            }
            preferences.edit().putInt("resource_icon_version", MIGRATION_VERSION).apply()
            Log.i("PinnedShortcutIcons", "Resource icon repair completed: ${updates.size} shortcuts")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // A locked user or an OEM error must not prevent the app from opening or mark success.
            Log.w("PinnedShortcutIcons", "Icon repair deferred", error)
        }
    }
}
