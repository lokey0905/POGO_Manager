package app.lokey0905.location.catalog

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.api.CatalogRelease
import app.lokey0905.location.api.CatalogVersionRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

/** Page-owned cards: create in onViewCreated and release the controller in onDestroyView. */
class CatalogCardsController(
    private val fragment: Fragment,
    private val container: ViewGroup,
    apps: List<CatalogApp>,
    showSectionHeaders: Boolean = false,
    private val onProgress: (loading: Boolean, failedCount: Int) -> Unit = { _, _ -> }
) {
    private data class SourceState(
        var release: CatalogRelease? = null,
        var loading: Boolean = false,
        var failed: Boolean = false,
        var checkedAtMillis: Long? = null
    )

    private data class InstalledState(val installed: Boolean, val version: String?)

    private val context = fragment.requireContext()
    private val owner = fragment.viewLifecycleOwner
    private val repository = CatalogVersionRepository(context)
    private val entries = apps.distinct()
    private val sources = entries.filter(::checksRemoteVersion).associateWith { SourceState() }
    private val installed = mutableMapOf<CatalogApp, InstalledState>()
    private val cards = linkedMapOf<CatalogApp, View>()
    private var refreshJob: Job? = null
    private var generation = 0L

    init {
        entries.forEachIndexed { index, app ->
            val section = app.section
            if (showSectionHeaders && section != null && (index == 0 || entries[index - 1].section != section)) {
                val heading = LayoutInflater.from(context)
                    .inflate(R.layout.item_catalog_section, container, false) as TextView
                heading.setText(sectionTitle(section))
                ViewCompat.setAccessibilityHeading(heading, true)
                container.addView(heading)
            }
            val card = LayoutInflater.from(context).inflate(R.layout.item_catalog_app, container, false)
            // These presentation-only cards reuse child IDs and are rebuilt by this controller.
            // Do not let one card's saved child state overwrite another in the parent hierarchy.
            card.isSaveFromParentEnabled = false
            card.tag = app
            cards[app] = card
            container.addView(card)
            card.findViewById<TextView>(R.id.catalog_category).setText(category(app))
            card.findViewById<TextView>(R.id.catalog_name).setText(title(app))
            // Catalog artwork must not change with package installation or refresh results.
            card.findViewById<ImageView>(R.id.catalog_icon).setImageResource(catalogAppIcon(app))
            card.findViewById<ImageButton>(R.id.catalog_more).apply {
                contentDescription = context.getString(R.string.catalog_more_description, context.getString(title(app)))
                setOnClickListener { showMore(app, this) }
            }
            card.findViewById<MaterialButton>(R.id.catalog_remove).setOnClickListener {
                refreshInstalled()
                if (policy(app).showRemove) launchExternal(Intent(Intent.ACTION_DELETE, "package:${packageName(app)}".toUri()))
            }
            card.findViewById<MaterialButton>(R.id.catalog_action).setOnClickListener { performPrimaryAction(app) }
        }
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                generation++
                refreshJob?.cancel()
                refreshJob = null
            }
        })
        refreshInstalled()
    }

    fun refresh(force: Boolean = false) {
        if (!viewIsAlive()) return
        refreshInstalled()
        if (refreshJob?.isActive == true && !force) return
        val now = SystemClock.elapsedRealtime()
        val pending = sources.filterValues { shouldRefreshCatalogSource(it.checkedAtMillis, now, force) }.keys
        if (pending.isEmpty()) {
            publishProgress()
            return
        }
        val requestGeneration = ++generation
        refreshJob?.cancel()
        pending.forEach { app ->
            sources.getValue(app).apply { loading = true; failed = false }
            render(app)
        }
        publishProgress()
        refreshJob = owner.lifecycleScope.launch {
            supervisorScope {
                pending.map { app ->
                    launch {
                        val source = sources.getValue(app)
                        try {
                            val release = withTimeout(30_000) { repository.load(app) }
                            if (requestGeneration == generation && viewIsAlive()) source.release = release
                        } catch (_: TimeoutCancellationException) {
                            if (requestGeneration == generation && viewIsAlive()) source.failed = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            if (requestGeneration == generation && viewIsAlive()) source.failed = true
                        } finally {
                            if (requestGeneration == generation && viewIsAlive()) {
                                source.loading = false
                                source.checkedAtMillis = SystemClock.elapsedRealtime()
                                render(app)
                                publishProgress()
                            }
                        }
                    }
                }.joinAll()
            }
        }
    }

    fun refreshInstalled() {
        if (!viewIsAlive()) return
        entries.forEach { app ->
            installed[app] = if (isSystemHealthConnect(app)) {
                InstalledState(true, null)
            } else {
                try {
                    val info = context.packageManager.getPackageInfo(packageName(app), 0)
                    InstalledState(true, info.versionName)
                } catch (_: PackageManager.NameNotFoundException) {
                    InstalledState(false, null)
                } catch (_: SecurityException) {
                    InstalledState(false, null)
                }
            }
            render(app)
        }
    }

    private fun render(app: CatalogApp) {
        val card = cards.getValue(app)
        val local = installed[app] ?: InstalledState(false, null)
        val source = sources[app]
        val versionText = when {
            isSystemHealthConnect(app) -> context.getString(R.string.catalog_system_component)
            !local.installed -> context.getString(R.string.catalog_not_installed)
            else -> local.version ?: context.getString(R.string.version_value_unknown)
        }
        card.findViewById<TextView>(R.id.catalog_installed).text =
            context.getString(R.string.version_label_installed, versionText)
        card.findViewById<TextView>(R.id.catalog_latest).apply {
            visibility = if (source == null) View.GONE else View.VISIBLE
            if (source != null) {
                val version = when {
                    source.loading -> context.getString(R.string.catalog_source_loading)
                    source.failed && source.release != null -> context.getString(R.string.catalog_source_stale, source.release!!.versionName)
                    source.failed -> context.getString(R.string.catalog_source_failed)
                    else -> source.release?.versionName ?: context.getString(R.string.version_value_unknown)
                }
                text = context.getString(R.string.version_label_latest, version)
            }
        }
        val actions = policy(app)
        card.findViewById<ImageButton>(R.id.catalog_more).visibility = if (actions.showMore) View.VISIBLE else View.GONE
        card.findViewById<MaterialButton>(R.id.catalog_remove).visibility = if (actions.showRemove) View.VISIBLE else View.GONE
        card.findViewById<MaterialButton>(R.id.catalog_action).apply {
            isEnabled = !isSystemHealthConnect(app) || canOpenSystemHealthConnect()
            setText(if (!isEnabled) R.string.catalog_unavailable else when (actions.primaryAction) {
                CatalogPrimaryAction.DOWNLOAD -> R.string.download
                CatalogPrimaryAction.UPDATE -> R.string.update
                CatalogPrimaryAction.OPEN -> if (app == CatalogApp.HEALTH_CONNECT) R.string.catalog_open_settings else R.string.popup_menu_startApp
                CatalogPrimaryAction.OPEN_SETTINGS -> R.string.catalog_open_settings
            })
        }
    }

    private fun policy(app: CatalogApp): CatalogActionPolicy {
        val local = installed[app] ?: InstalledState(false, null)
        return getCatalogActionPolicy(local.installed, local.version, sources[app]?.release?.versionName, isSystemHealthConnect(app))
    }

    private fun performPrimaryAction(app: CatalogApp) {
        refreshInstalled()
        when (policy(app).primaryAction) {
            CatalogPrimaryAction.DOWNLOAD, CatalogPrimaryAction.UPDATE -> confirmDownload(app)
            CatalogPrimaryAction.OPEN_SETTINGS -> if (canOpenSystemHealthConnect()) {
                launchExternal(Intent("android.health.connect.action.HEALTH_HOME_SETTINGS"))
            }
            CatalogPrimaryAction.OPEN -> openInstalledApp(app)
        }
    }

    private fun openInstalledApp(app: CatalogApp) {
        val intent = if (app == CatalogApp.HEALTH_CONNECT) {
            Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").setPackage(packageName(app))
        } else context.packageManager.getLaunchIntentForPackage(packageName(app))
        if (intent != null) launchExternal(intent) else showLaunchFailed()
    }

    private fun showMore(app: CatalogApp, anchor: View) {
        refreshInstalled()
        if (!policy(app).showMore) return
        PopupMenu(context, anchor).apply {
            menu.add(0, 3, 0, R.string.popup_menu_startApp)
            menu.add(0, 1, 1, R.string.catalog_app_details)
            menu.add(0, 2, 2, R.string.catalog_download_page)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> launchExternal(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${packageName(app)}".toUri()))
                    2 -> confirmDownload(app)
                    3 -> openInstalledApp(app)
                }
                true
            }
        }.show()
    }

    private fun confirmDownload(app: CatalogApp) {
        if (app == CatalogApp.JOYSTICK && Build.SUPPORTED_ABIS.firstOrNull() != "arm64-v8a" &&
            !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("allow_download_on_non_arm64", false)
        ) {
            Snackbar.make(container, context.getString(R.string.unsupportedDevices), Snackbar.LENGTH_LONG).show()
            return
        }
        val url = sources[app]?.release?.downloadUrl ?: context.getString(fallbackUrl(app))
        val opensPlayStore = url.toUri().host == "play.google.com"
        val downloadGuide = when {
            url.contains("apkmirror", ignoreCase = true) -> R.drawable.download_apk_e
            url.contains("mediafire", ignoreCase = true) -> R.drawable.download_mediafire
            else -> null
        }?.let { drawable ->
            LayoutInflater.from(context).inflate(R.layout.dialog_imageview, null).apply {
                findViewById<ImageView>(R.id.dialog_imageview).setImageResource(drawable)
            }
        }
        MaterialAlertDialogBuilder(context)
            .setView(downloadGuide)
            .setTitle(if (opensPlayStore) R.string.catalog_store_title else R.string.dialogDownloadTitle)
            .setMessage(if (opensPlayStore) R.string.catalog_store_message else R.string.dialogDownloadMessage)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> launchExternal(Intent(Intent.ACTION_VIEW, url.toUri())) }
            .show()
    }

    private fun launchExternal(intent: Intent) {
        if (!viewIsAlive()) return
        try {
            fragment.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showLaunchFailed()
        } catch (_: SecurityException) {
            showLaunchFailed()
        }
    }

    private fun showLaunchFailed() = Toast.makeText(context, R.string.catalog_launch_failed, Toast.LENGTH_SHORT).show()

    private fun publishProgress() {
        if (viewIsAlive()) onProgress(sources.values.any { it.loading }, sources.values.count { it.failed })
    }

    private fun viewIsAlive(): Boolean = owner.lifecycle.currentState != Lifecycle.State.DESTROYED

    private fun isSystemHealthConnect(app: CatalogApp): Boolean =
        app == CatalogApp.HEALTH_CONNECT && Build.VERSION.SDK_INT >= 34

    private fun canOpenSystemHealthConnect(): Boolean = runCatching {
        Intent("android.health.connect.action.HEALTH_HOME_SETTINGS").resolveActivity(context.packageManager) != null
    }.getOrDefault(false)

    private fun checksRemoteVersion(app: CatalogApp): Boolean =
        app != CatalogApp.GOOGLE_FIT && app != CatalogApp.HEALTH_CONNECT

    private fun packageName(app: CatalogApp): String = context.getString(when (app) {
        CatalogApp.PIKMIN -> R.string.catalog_package_pikmin
        CatalogApp.JOYSTICK -> R.string.packageName_gps64
        CatalogApp.DEFIT -> R.string.packageName_defit
        CatalogApp.FITNESS -> R.string.catalog_package_fitness
        CatalogApp.GOOGLE_FIT -> R.string.catalog_package_google_fit
        CatalogApp.HEALTH_CONNECT -> R.string.catalog_package_health_connect
        CatalogApp.APKMIRROR_INSTALLER -> R.string.packageName_APKMirrorInstaller
    })

    private fun title(app: CatalogApp): Int = when (app) {
        CatalogApp.PIKMIN -> R.string.catalog_title_pikmin
        CatalogApp.JOYSTICK -> R.string.catalog_title_joystick
        CatalogApp.DEFIT -> R.string.catalog_title_defit
        CatalogApp.FITNESS -> R.string.catalog_title_fitness
        CatalogApp.GOOGLE_FIT -> R.string.catalog_title_google_fit
        CatalogApp.HEALTH_CONNECT -> R.string.catalog_title_health_connect
        CatalogApp.APKMIRROR_INSTALLER -> R.string.catalog_title_installer
    }

    private fun sectionTitle(section: CatalogSection): Int = when (section) {
        CatalogSection.STEPS -> R.string.catalog_category_fitness
        CatalogSection.HEALTH -> R.string.catalog_section_health
        CatalogSection.INSTALLATION -> R.string.catalog_category_installer
    }

    private fun category(app: CatalogApp): Int = when (app) {
        CatalogApp.PIKMIN -> R.string.catalog_category_game
        CatalogApp.JOYSTICK -> R.string.catalog_category_location
        CatalogApp.DEFIT, CatalogApp.FITNESS -> R.string.catalog_category_fitness
        CatalogApp.GOOGLE_FIT -> R.string.catalog_category_google_fit
        CatalogApp.HEALTH_CONNECT -> R.string.catalog_category_integration
        CatalogApp.APKMIRROR_INSTALLER -> R.string.catalog_category_installer
    }

    private fun fallbackUrl(app: CatalogApp): Int = when (app) {
        CatalogApp.PIKMIN -> R.string.catalog_url_pikmin_download
        CatalogApp.JOYSTICK -> R.string.catalog_url_joystick_fallback
        CatalogApp.DEFIT -> R.string.catalog_url_defit_fallback
        CatalogApp.FITNESS -> R.string.catalog_url_fitness_releases
        CatalogApp.GOOGLE_FIT -> R.string.catalog_url_google_fit
        CatalogApp.HEALTH_CONNECT -> R.string.catalog_url_health_connect
        CatalogApp.APKMIRROR_INSTALLER -> R.string.catalog_url_installer_fallback
    }
}
