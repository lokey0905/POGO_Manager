package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.api.ApkPure
import app.lokey0905.location.api.DataHubApi
import app.lokey0905.location.api.PgSharpRootApi
import app.lokey0905.location.api.Pokemod
import app.lokey0905.location.api.Pokemon
import app.lokey0905.location.api.PolygonXCheckResult
import app.lokey0905.location.version.CompatibilityStatus
import app.lokey0905.location.version.compareVersions
import app.lokey0905.location.version.filterVersionsAtOrAboveMinimum
import app.lokey0905.location.version.getCompatibilityStatus
import app.lokey0905.location.version.VersionCheckProgress
import app.lokey0905.location.version.VersionCheckSource
import app.lokey0905.location.version.VersionRefreshPolicy
import app.lokey0905.location.version.VersionCheckSummary
import app.lokey0905.location.version.RemoteVersionPresentation
import app.lokey0905.location.version.getVersionCheckSummary
import app.lokey0905.location.version.getRemoteVersionPresentation
import app.lokey0905.location.version.prioritizeVersions
import app.lokey0905.location.version.getProbedCompatibilityStatus
import app.lokey0905.location.version.selectTargetVersion
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.appbar.SubtitleCollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import androidx.core.net.toUri


data class PogoVersionInfo(
    val pogoVersion: String,
    val pogoVersionNumber: Long,
    val pogoARM64URLPGTools: String,
    val pogoARM64URLAPKMirror: String
)

data class AppsInfo(
    val appName: String = "",
    val packageName: String,
    val downloadButtonId: Int,
    val removeButtonId: Int,
    val moreButtonId: Int,
    var newVersionTextId: Int,
    val installVersionTextId: Int,
    var updateNewVersionFromJson: Boolean = false,
    var updateDownloadLinkFromJson: Boolean = false,
    var updateOfficialLinkFromJson: Boolean = false,
    var updateNewVersionText: Boolean = true,
    var checkNeedUpdateAppsAmount: Boolean = true,
    var newVersion: String = "未安裝",
    var downloadLink: String = "",
    var officialLink: String = "",
    var haveNewVersionTag: Boolean = false,
)

data class AppsInfoUpdate(
    val appName: String,
    val newVersion: String?,
    val downloadLink: String?,
    val officialLink: String?
)

private data class ToolCompatibilitySource(
    val appName: String,
    val displayNameResId: Int,
    val supportedVersions: List<String>?
)

class AppsPoke : Fragment() {
    private companion object {
        const val DOWNLOAD_BUTTON_ACTION_OPEN_APP = "open_app"
        const val STATE_SELECTED_POGO_VERSION = "selected_pogo_version"
        const val STATE_ALL_SUPPORTED_VERSIONS_EXPANDED =
            "all_supported_versions_expanded"
    }

    private var nowPogoVersionsList = ArrayList<PogoVersionInfo>()
    private var pgSharpRootVersionsList = ArrayList<String>()
    private var pgtoolsVersionsList = ArrayList<PogoVersionInfo>()
    private var pgToolsTestVersionsList = ArrayList<PogoVersionInfo>()
    private var pokemodVersionList = ArrayList<String>()
    private var pokemonMinVersion = ""
    private var choosePogoVersion: String = "未安裝"
    private var pgToolsProductionAppVersion: String = "未安裝"
    private var pgToolsProductionUrl = ""
    private var pgToolsProductionVersionCheckFailed = false
    private var pgToolsTestAppVersion: String = "未安裝"
    private var pgToolsTestUrl = ""
    private var pgToolsTestVersionCheckFailed = false
    private var pgSharpRootVersionCheckFailed = false
    private var dataHubVersionCheckFailed = false

    private var pgToolsTestVersion = false
    private var pokAresNoSupportDevices = false
    private var pok_download_on_apkmirror = false

    private var pgSharpRootSupportDataLoaded = false
    private var pgToolsSupportDataLoaded = false
    private var pgToolsTestSupportDataLoaded = false
    private var pokemodSupportDataLoaded = false
    private var allSupportedVersionsExpanded = false
    private var preservePogoVersionSelection = false

    private var totalMemory = 0

    private var appsInfo = listOf<AppsInfo>()
    private var url_pokAres = ""
    private var versionCheckJob: Job? = null
    private var versionCheckSnackbar: Snackbar? = null
    private val versionRefreshPolicy = VersionRefreshPolicy()
    private var refreshLocalAppState: (() -> Unit)? = null
    private val loadedAppVersions = mutableSetOf<String>()
    private val unavailableAppVersions = mutableSetOf<String>()
    private val pokemodVersionResults = mutableMapOf<String, Boolean>()
    private var polygonVersionResult: PolygonXCheckResult? = null
    private var polygonCheckedInstalledVersion: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_poke, container, false)

        savedInstanceState?.getString(STATE_SELECTED_POGO_VERSION)
            ?.takeIf { it.isNotBlank() }
            ?.let {
                choosePogoVersion = it
                preservePogoVersionSelection = true
            }
        allSupportedVersionsExpanded = savedInstanceState?.getBoolean(
            STATE_ALL_SUPPORTED_VERSIONS_EXPANDED,
            false
        ) ?: false

        appsInfo = listOf(
            AppsInfo(
                "joystick",
                getString(R.string.packageName_gps64),
                R.id.download_gps,
                R.id.remove_gps,
                R.id.gps_more,
                R.id.gps_new_version,
                R.id.gps_install_version,
                updateNewVersionFromJson = true,
                updateDownloadLinkFromJson = true,
                updateOfficialLinkFromJson = true,
                updateNewVersionText = true,
            ),
            AppsInfo(
                "pgSharpRoot",
                getString(R.string.packageName_PGSharpRoot),
                R.id.download_pgsharp_root,
                R.id.remove_pgsharp_root,
                R.id.pgsharp_root_more,
                R.id.pgsharp_root_new_version,
                R.id.pgsharp_root_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = false,
                updateOfficialLinkFromJson = false,
                updateNewVersionText = false,
                checkNeedUpdateAppsAmount = false,
            ),
            AppsInfo(
                "polygon",
                getString(R.string.packageName_polygonX),
                R.id.download_polygon,
                R.id.remove_polygon,
                R.id.polygon_more,
                R.id.polygon_new_version,
                R.id.polygon_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = false,
                updateOfficialLinkFromJson = true,
            ),
            AppsInfo(
                "PGTools",
                getString(R.string.packageName_PGTools),
                R.id.download_pgtools,
                R.id.remove_pgtools,
                R.id.pgtools_more,
                R.id.pgtools_new_version,
                R.id.pgtools_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = false,
                updateOfficialLinkFromJson = false,
                updateNewVersionText = false,
                checkNeedUpdateAppsAmount = false,
            ),
            AppsInfo(
                "pok",
                getString(R.string.packageName_pok),
                R.id.download_pok,
                R.id.remove_pok,
                R.id.pok_more,
                R.id.pok_new_version,
                R.id.pok_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = false,
                updateOfficialLinkFromJson = false,
                updateNewVersionText = false,
                checkNeedUpdateAppsAmount = false,
            ),
            AppsInfo(
                "pokAres",
                getString(R.string.packageName_pokAres),
                R.id.download_pokAres,
                R.id.remove_pokAres,
                R.id.pokAres_more,
                R.id.pokAres_new_version,
                R.id.pokAres_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = true,
                updateOfficialLinkFromJson = false,
                updateNewVersionText = false,
                checkNeedUpdateAppsAmount = false,
            ),
            AppsInfo(
                "pokeList",
                getString(R.string.packageName_PokeList),
                R.id.download_pokelist,
                R.id.remove_pokelist,
                R.id.pokelist_more,
                R.id.pokelist_new_version,
                R.id.pokelist_install_version,
                updateNewVersionFromJson = true,
                updateDownloadLinkFromJson = true,
                updateOfficialLinkFromJson = true,
            ),
            AppsInfo(
                "wecatch",
                getString(R.string.packageName_WeCatch),
                R.id.download_wecatch,
                R.id.remove_wecatch,
                R.id.wecatch_more,
                R.id.wecatch_new_version,
                R.id.wecatch_install_version,
                updateNewVersionFromJson = true,
                updateDownloadLinkFromJson = true,
                updateOfficialLinkFromJson = true,
            ),
            AppsInfo(
                "wrapper",
                getString(R.string.packageName_wrapper),
                R.id.download_wrapper,
                R.id.remove_wrapper,
                R.id.wrapper_more,
                R.id.wrapper_new_version,
                R.id.wrapper_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = false,
                updateOfficialLinkFromJson = true,
                updateNewVersionText = false,
                checkNeedUpdateAppsAmount = false,
            ),
            AppsInfo(
                "defit",
                getString(R.string.packageName_defit),
                R.id.download_defit,
                R.id.remove_defit,
                R.id.defit_more,
                R.id.defit_new_version,
                R.id.defit_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = false,
                updateOfficialLinkFromJson = false,
            ),
            AppsInfo(
                "pokemod",
                getString(R.string.packageName_pokemod),
                R.id.download_pokemod,
                R.id.remove_pokemod,
                R.id.pokemod_more,
                R.id.pokemod_new_version,
                R.id.pokemod_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = true,
                updateOfficialLinkFromJson = true,
            ),
            AppsInfo(
                "samsungStore",
                getString(R.string.packageName_galaxyStore),
                R.id.download_galaxyStore,
                R.id.remove_galaxyStore,
                R.id.galaxyStore_more,
                R.id.galaxyStore_new_version,
                R.id.galaxyStore_install_version,
                updateNewVersionFromJson = true,
                updateDownloadLinkFromJson = true,
                updateOfficialLinkFromJson = true,
            ),
            AppsInfo(
                "APKMirrorInstaller",
                getString(R.string.packageName_APKMirrorInstaller),
                R.id.download_APKMirrorInstaller,
                R.id.remove_APKMirrorInstaller,
                R.id.APKMirrorInstaller_more,
                R.id.APKMirrorInstaller_new_version,
                R.id.APKMirrorInstaller_install_version,
                updateNewVersionFromJson = false,
                updateDownloadLinkFromJson = false,
                updateOfficialLinkFromJson = true,
            )
        )

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        pokAresNoSupportDevices =
            sharedPreferences.getBoolean("allow_download_on_non_samsung", false)
        pok_download_on_apkmirror =
            sharedPreferences.getBoolean("pok_download_on_apkmirror", false)
        pgToolsTestVersion =
            sharedPreferences.getBoolean("pgtools_testversion", false)

        val actManager =
            requireActivity().getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        totalMemory = (memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)).roundToInt()

        view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility =
            viewShowOrHide(totalMemory > 4 || pokAresNoSupportDevices)

        setupListeners(view)
        setupCompatibilityCard(view)

        MobileAds.initialize(requireActivity())
        val mAdView = view.findViewById<AdView>(R.id.ad_banner)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_POGO_VERSION, choosePogoVersion)
        outState.putBoolean(
            STATE_ALL_SUPPORTED_VERSIONS_EXPANDED,
            allSupportedVersionsExpanded
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pgToolsTestVersion = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean("pgtools_testversion", false)

        fun setFragmentResultListener() {
            parentFragmentManager.setFragmentResultListener("pokAresNoSupportDevices", viewLifecycleOwner) { _, bundle ->
                pokAresNoSupportDevices = bundle.getBoolean("bundleKey")

                val pokeAresLayout = view.findViewById<LinearLayout>(R.id.linearLayout_pokAres)

                if (totalMemory > 4 || pokAresNoSupportDevices) {
                    pokeAresLayout.visibility = viewShowOrHide(true)
                } else {
                    pokeAresLayout.visibility = viewShowOrHide(false)
                }
            }

            parentFragmentManager.setFragmentResultListener("pok_download_on_apkmirror", viewLifecycleOwner) { _, bundle ->
                pok_download_on_apkmirror = bundle.getBoolean("bundleKey")
            }

            parentFragmentManager.setFragmentResultListener("pgtools_testversion", viewLifecycleOwner) { _, bundle ->
                val selectedTestVersion = bundle.getBoolean("bundleKey")
                if (pgToolsTestVersion == selectedTestVersion) {
                    return@setFragmentResultListener
                }

                pgToolsTestVersion = selectedTestVersion
                if (isHidden || !isResumed) return@setFragmentResultListener
                Toast.makeText(
                    context,
                    if (pgToolsTestVersion) "已切換至 PGTools 測試版" else "已切換至 PGTools 正式版",
                    Toast.LENGTH_SHORT
                ).show()
                refreshLocalAppState?.invoke()
            }
        }

        setFragmentResultListener()
    }

    override fun onResume() {
        super.onResume()
        pgToolsTestVersion = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean("pgtools_testversion", false)
        if (!isHidden) refreshVersionInfoIfNeeded(requireView())
    }

    private fun refreshVersionInfoIfNeeded(view: View) {
        if (versionRefreshPolicy.shouldRefresh(SystemClock.elapsedRealtime())) {
            setupAppVersionInfo(view)
        } else {
            Log.i("AppsPoke", "Using current version data; refreshing installed apps only")
            refreshLocalAppState?.invoke()
        }
    }

    override fun onDestroyView() {
        versionCheckJob?.cancel()
        versionCheckJob = null
        versionCheckSnackbar?.dismiss()
        versionCheckSnackbar = null
        refreshLocalAppState = null
        versionRefreshPolicy.reset()
        loadedAppVersions.clear()
        unavailableAppVersions.clear()
        nowPogoVersionsList.clear()
        pgSharpRootVersionsList.clear()
        pgtoolsVersionsList.clear()
        pgToolsTestVersionsList.clear()
        pokemodVersionList.clear()
        pokemodVersionResults.clear()
        polygonVersionResult = null
        polygonCheckedInstalledVersion = null
        pgSharpRootSupportDataLoaded = false
        pgToolsSupportDataLoaded = false
        pgToolsTestSupportDataLoaded = false
        pokemodSupportDataLoaded = false
        pgToolsProductionAppVersion = getString(R.string.notInstalled)
        pgToolsProductionUrl = ""
        pgToolsTestAppVersion = getString(R.string.notInstalled)
        pgToolsTestUrl = ""
        // Preserve the user's target across recreation, but never retain the old View.
        preservePogoVersionSelection = shouldPreservePogoVersionSelection() ||
                choosePogoVersion != getString(R.string.notInstalled)
        super.onDestroyView()
    }

    private fun setupListeners(view: View) {
        fun downloadAppCheckARM64(url: String) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            val allowDownloadOnNonArm64 =
                sharedPreferences.getBoolean("allow_download_on_non_arm64", false)

            if (Build.SUPPORTED_ABIS[0] == "arm64-v8a" || allowDownloadOnNonArm64)
                downloadAPPWithCheck(url)
            else
                Snackbar.make(
                    view,
                    "${getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})",
                    Snackbar.LENGTH_LONG
                ).setAction("Action", null).show()
        }

        fun downloadPoke() {
            val versionInfo = nowPogoVersionsList.firstOrNull {
                it.pogoVersion == choosePogoVersion
            } ?: pgtoolsVersionsList.firstOrNull {
                it.pogoVersion == choosePogoVersion
            } ?: pgToolsTestVersionsList.firstOrNull {
                it.pogoVersion == choosePogoVersion
            }
            val url = if (pok_download_on_apkmirror) {
                versionInfo?.pogoARM64URLAPKMirror.orEmpty()
            } else {
                versionInfo?.pogoARM64URLPGTools.orEmpty()
            }

            if (url.isBlank()) {
                Toast.makeText(
                    context,
                    getString(R.string.versionCheckFailed),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            downloadAppCheckARM64(url)
        }

        fun downloadPokAres() {
            if (Build.MANUFACTURER == "samsung" || pokAresNoSupportDevices) {
                if (appInstalledVersion(getString(R.string.packageName_galaxyStore)) == "未安裝") {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.dialogInstallSamsungStoreTitle))
                        .setMessage(getString(R.string.dialogInstallSamsungStoreMessage))
                        .apply {
                            setNeutralButton(R.string.cancel) { _, _ ->
                                Toast.makeText(
                                    context,
                                    getString(R.string.cancelOperation),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                            setPositiveButton(R.string.ok) { _, _ ->
                                downloadAPPWithCheck(
                                    appsInfo.find { it.appName == "samsungStore" }?.downloadLink
                                        ?: ""
                                )
                            }
                        }
                        .show()
                } else {
                    downloadAPPWithCheck(
                        String.format(getString(R.string.url_pokAres),
                            choosePogoVersion.replace(".", "-"),
                            choosePogoVersion.replace(".", "-"))
                    )

                }
            } else
                showAlertDialog(
                    getString(R.string.unsupportedDevices),
                    getString(R.string.unsupportedDevicesPokeAres)
                )
        }

        //set listeners
        for (mapping in appsInfo) {
            val appName = mapping.appName
            val packageName = mapping.packageName
            val removeButton = view.findViewById<Button>(mapping.removeButtonId)
            val moreButton = view.findViewById<ImageButton>(mapping.moreButtonId)
            val downloadButtonId = mapping.downloadButtonId
            val downloadButton = view.findViewById<Button>(downloadButtonId)

            //set remove button listener
            removeButton?.setOnClickListener {
                appUnInstall(packageName)
            }

            //set more button listener
            moreButton?.setOnClickListener {
                popupMenu(view, mapping.moreButtonId, packageName)
            }

            //set download button listener
            downloadButton?.setOnClickListener {
                if (isOpenAppAction(downloadButton)) {
                    openApp(packageName)
                    return@setOnClickListener
                }

                when (appName) {
                    "joystick" -> {
                        downloadAPPWithCheck(mapping.downloadLink)
                    }

                    "pok" -> {
                        downloadPoke()
                    }

                    "pokAres" -> {
                        downloadPokAres()
                    }

                    "PGTools" -> {
                        val downloadUrl = if (pgToolsTestVersion) {
                            pgToolsTestUrl
                        } else {
                            pgToolsProductionUrl
                        }
                        downloadAppCheckARM64(downloadUrl)
                    }

                    "pgSharpRoot" -> {
                        downloadAppCheckARM64(mapping.downloadLink)
                    }

                    else -> {
                        downloadAPPWithCheck(mapping.downloadLink)
                    }
                }
            }
        }

        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            .setOnRefreshListener {
                Toast.makeText(context, getString(R.string.refreshing), Toast.LENGTH_SHORT).show()
                setupAppVersionInfo(view)
            }
    }

    private fun setupCompatibilityCard(view: View) {
        view.findViewById<MaterialButton>(R.id.toggle_all_supported_versions)
            .setOnClickListener {
                allSupportedVersionsExpanded = !allSupportedVersionsExpanded
                applyAllSupportedVersionsVisibility(view)
            }

        applyAllSupportedVersionsVisibility(view)
        updateCompatibilitySummary(view)
    }

    private fun applyAllSupportedVersionsVisibility(view: View) {
        view.findViewById<LinearLayout>(R.id.all_supported_versions).visibility =
            viewShowOrHide(allSupportedVersionsExpanded)

        view.findViewById<MaterialButton>(R.id.toggle_all_supported_versions).apply {
            setText(
                if (allSupportedVersionsExpanded) {
                    R.string.compatibility_collapse_all_versions
                } else {
                    R.string.compatibility_show_all_versions
                }
            )
            setIconResource(
                if (allSupportedVersionsExpanded) {
                    R.drawable.baseline_expand_less_24
                } else {
                    R.drawable.baseline_expand_more_24
                }
            )
        }
    }

    private fun updateCompatibilitySummary(view: View) {
        val sources = getToolCompatibilitySources()
        val rows = view.findViewById<LinearLayout>(R.id.compatibility_status_rows)
        val rowsNeedRebuild = rows.childCount != sources.size ||
                sources.indices.any { index -> rows.getChildAt(index).tag != sources[index].appName }

        if (rowsNeedRebuild) {
            rows.removeAllViews()
            sources.forEach { source ->
                val row = layoutInflater.inflate(
                    R.layout.item_pogo_compatibility_status,
                    rows,
                    false
                )
                row.tag = source.appName
                rows.addView(row)
            }
        }

        sources.forEachIndexed { index, source ->
            val row = rows.getChildAt(index)
            val icon = row.findViewById<TextView>(R.id.compatibility_status_icon)
            val toolName = row.findViewById<TextView>(R.id.compatibility_tool_name)
            val statusText = row.findViewById<TextView>(R.id.compatibility_status_text)
            val status = if (source.appName == "pokemod") {
                getProbedCompatibilityStatus(
                    choosePogoVersion, pokemodVersionResults,
                    source.supportedVersions.orEmpty(), pokemodSupportDataLoaded
                )
            } else getCompatibilityStatus(choosePogoVersion, source.supportedVersions)

            toolName.setText(source.displayNameResId)
            val (iconResId, statusLabel, colorAttribute) = when (status) {
                CompatibilityStatus.Supported -> Triple(
                    R.string.compatibility_icon_supported,
                    getString(R.string.compatibility_supported),
                    androidx.appcompat.R.attr.colorPrimary
                )

                CompatibilityStatus.Unsupported -> Triple(
                    R.string.compatibility_icon_unsupported,
                    getString(R.string.compatibility_unsupported),
                    androidx.appcompat.R.attr.colorError
                )

                is CompatibilityStatus.UnsupportedWithLatestSupportedVersion -> Triple(
                    R.string.compatibility_icon_unsupported,
                    getString(
                        R.string.compatibility_latest_supported,
                        status.version
                    ),
                    androidx.appcompat.R.attr.colorError
                )

                CompatibilityStatus.Unknown -> Triple(
                    R.string.compatibility_icon_unknown,
                    getString(R.string.compatibility_unknown),
                    com.google.android.material.R.attr.colorTertiary
                )
            }
            val statusColor = MaterialColors.getColor(row, colorAttribute)

            icon.setText(iconResId)
            icon.setTextColor(statusColor)
            statusText.text = statusLabel
            statusText.setTextColor(statusColor)
        }
    }

    private fun getToolCompatibilitySources(): List<ToolCompatibilitySource> {
        return listOf(
            ToolCompatibilitySource(
                appName = "PGTools",
                displayNameResId = R.string.compatibility_tool_pgtools,
                supportedVersions = if (pgToolsSupportDataLoaded) {
                    filterVersionsAtOrAboveMinimum(
                        pgtoolsVersionsList.map(PogoVersionInfo::pogoVersion),
                        pokemonMinVersion
                    )
                } else {
                    null
                }
            ),
            ToolCompatibilitySource(
                appName = "PGToolsTest",
                displayNameResId = R.string.compatibility_tool_pgtools_test,
                supportedVersions = if (pgToolsTestSupportDataLoaded) {
                    filterVersionsAtOrAboveMinimum(
                        pgToolsTestVersionsList.map(PogoVersionInfo::pogoVersion),
                        pokemonMinVersion
                    )
                } else {
                    null
                }
            ),
            ToolCompatibilitySource(
                appName = "pokemod",
                displayNameResId = R.string.compatibility_tool_pokemod,
                supportedVersions = if (pokemodSupportDataLoaded) {
                    filterVersionsAtOrAboveMinimum(
                        pokemodVersionList,
                        pokemonMinVersion
                    )
                } else {
                    null
                }
            ),
            ToolCompatibilitySource(
                appName = "pgSharpRoot",
                displayNameResId = R.string.compatibility_tool_pgsharp_root,
                supportedVersions = if (pgSharpRootSupportDataLoaded) {
                    filterVersionsAtOrAboveMinimum(
                        pgSharpRootVersionsList,
                        pokemonMinVersion
                    )
                } else {
                    null
                }
            )
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setupAppVersionInfo(view: View) {
        versionCheckJob?.cancel()
        versionCheckSnackbar?.dismiss()
        versionCheckSnackbar = null

        val checkJob = SupervisorJob(viewLifecycleOwner.lifecycleScope.coroutineContext[Job])
        versionCheckJob = checkJob
        val checkScope =
            CoroutineScope(viewLifecycleOwner.lifecycleScope.coroutineContext + checkJob)
        versionRefreshPolicy.onStarted()
        val progress = VersionCheckProgress()
        val startedAt = SystemClock.elapsedRealtime()
        val failedSources = mutableSetOf<VersionCheckSource>()
        Log.i("AppsPoke", "Version checks started")

        pgToolsProductionVersionCheckFailed = false
        pgToolsTestVersionCheckFailed = false
        pgSharpRootVersionCheckFailed = false
        dataHubVersionCheckFailed = false
        preservePogoVersionSelection = shouldPreservePogoVersionSelection() ||
                (choosePogoVersion.isNotBlank() && choosePogoVersion != getString(R.string.notInstalled))
        updateCompatibilitySummary(view)

        val formatInstallVersion: String = getString(R.string.version_label_installed)
        val formatInstallVersionOther: String =
            getString(R.string.version_label_installed_other)
        val formatNewerVersion: String = getString(R.string.version_label_latest)
        val formatNewerVersionOther: String =
            getString(R.string.version_label_latest_other)

        val pokePackageName = getString(R.string.packageName_pok)
        val pokeAresPackageName = getString(R.string.packageName_pokAres)
        val pgSharpRootPackageName = getString(R.string.packageName_PGSharpRoot)
        val pgToolsPackageName = getString(R.string.packageName_PGTools)
        val dataHubPackageName = getString(R.string.packageName_wrapper)

        val pokeSupportVersion = view.findViewById<TextView>(R.id.pok_new_version)
        val pokeAresSupportVersion = view.findViewById<TextView>(R.id.pokAres_new_version)
        val pgToolSupportVersion = view.findViewById<TextView>(R.id.pgtools_new_version)

        val pokeDownloadButton = view.findViewById<Button>(R.id.download_pok)
        val pokeAresDownloadButton = view.findViewById<Button>(R.id.download_pokAres)
        val pgSharpRootDownloadButton = view.findViewById<Button>(R.id.download_pgsharp_root)
        val pgToolsDownloadButton = view.findViewById<Button>(R.id.download_pgtools)
        val dataHubDownloadButton = view.findViewById<Button>(R.id.download_wrapper)
        val toolbarLayout =
            view.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout)
        val swipeRefreshLayout =
            view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.isRefreshing = true

        val textInputLayout = view.findViewById<TextInputLayout>(R.id.textField)
        val autoCompleteTextView = textInputLayout.editText as? AutoCompleteTextView

        var hasVersionCheckError = false
        var dataHubNeedsUpdate = false
        var pokemonListResolved = false
        var noEligiblePokemonVersions = false
        var displayedUpdateCount = 0

        fun retryVersionCheck() {
            swipeRefreshLayout.isRefreshing = true
            setupAppVersionInfo(view)
        }

        fun showRetrySnackbar() {
            if (versionCheckSnackbar != null) {
                return
            }

            versionCheckSnackbar = Snackbar.make(
                view,
                getString(R.string.versionCheckFailed),
                Snackbar.LENGTH_INDEFINITE
            ).setAction(getString(R.string.retry)) {
                retryVersionCheck()
            }
            versionCheckSnackbar?.show()
        }

        fun updateToolbarSubtitle(needUpdateAppsAmount: Int = displayedUpdateCount) {
            displayedUpdateCount = needUpdateAppsAmount
            val summary = getVersionCheckSummary(
                progress.pending.size,
                failedSources.size,
                needUpdateAppsAmount + if (dataHubNeedsUpdate) 1 else 0
            )
            toolbarLayout.subtitle = when (summary) {
                is VersionCheckSummary.Loading -> getString(
                    if (summary.failedCount > 0) R.string.version_summary_loading_with_failures
                    else R.string.version_summary_loading,
                    summary.pendingCount
                )
                is VersionCheckSummary.PartialFailure -> getString(R.string.version_summary_partial_failure)
                is VersionCheckSummary.UpdatesAvailable -> getString(R.string.format_installApps, summary.count)
                VersionCheckSummary.Complete -> getString(R.string.version_summary_complete)
            }
        }

        fun markVersionCheckFailed(source: VersionCheckSource) {
            failedSources.add(source)
            hasVersionCheckError = true
            updateToolbarSubtitle()
            showRetrySnackbar()
        }

        fun remoteVersionValue(version: String?, source: VersionCheckSource): String {
            return when (val presentation = getRemoteVersionPresentation(
                version,
                getString(R.string.notInstalled),
                loading = source in progress.pending,
                failed = source in failedSources
            )) {
                RemoteVersionPresentation.Loading -> getString(R.string.version_value_loading)
                RemoteVersionPresentation.Unknown -> getString(R.string.version_value_unknown)
                RemoteVersionPresentation.Failed -> getString(R.string.version_value_failed)
                is RemoteVersionPresentation.Available -> presentation.version
                is RemoteVersionPresentation.Stale -> getString(R.string.version_value_stale, presentation.version)
            }
        }

        fun setVersionListAdapter(versions: List<String>) {
            val adapter: ArrayAdapter<String> = ArrayAdapter(
                view.context,
                android.R.layout.simple_list_item_1,
                versions
            )
            autoCompleteTextView?.setAdapter(adapter)
        }

        fun setSupportVersionStatus(textViewId: Int, status: String) {
            updateSupportedVersionsTextView(textViewId, listOf(status))
        }

        fun updateMinimumFilteredSupportedVersions(
            textViewId: Int,
            versions: List<String>,
            dataLoaded: Boolean
        ) {
            if (!dataLoaded) {
                setSupportVersionStatus(
                    textViewId,
                    getString(R.string.compatibility_unknown)
                )
                return
            }

            val filteredVersions = filterVersionsAtOrAboveMinimum(
                versions,
                pokemonMinVersion
            )
            if (filteredVersions.isEmpty()) {
                setSupportVersionStatus(
                    textViewId,
                    getString(R.string.versionCheckNoSupportedVersion)
                )
            } else {
                updateSupportedVersionsTextView(textViewId, filteredVersions)
            }
        }

        fun refreshMinimumFilteredToolVersions() {
            updateMinimumFilteredSupportedVersions(
                R.id.supportVersion_PGSharpRoot,
                pgSharpRootVersionsList,
                pgSharpRootSupportDataLoaded
            )
            updateMinimumFilteredSupportedVersions(
                R.id.supportVersion_PGTools,
                pgtoolsVersionsList.map(PogoVersionInfo::pogoVersion),
                pgToolsSupportDataLoaded
            )
            updateMinimumFilteredSupportedVersions(
                R.id.supportVersion_PGToolsTest,
                pgToolsTestVersionsList.map(PogoVersionInfo::pogoVersion),
                pgToolsTestSupportDataLoaded
            )
            updateMinimumFilteredSupportedVersions(
                R.id.supportVersion_pokemod,
                pokemodVersionList,
                pokemodSupportDataLoaded
            )
            updateCompatibilitySummary(view)
        }

        fun setNewVersionError(textView: TextView?) {
            val format = if (textView?.id == R.id.pok_new_version || textView?.id == R.id.pokAres_new_version) {
                R.string.version_label_target
            } else R.string.version_label_latest
            textView?.text = getString(
                format,
                getString(R.string.version_value_failed)
            )
        }

        fun updateSelectedPgToolsVersionText() {
            val selectedVersion = if (pgToolsTestVersion) {
                pgToolsTestAppVersion
            } else {
                pgToolsProductionAppVersion
            }
            pgToolSupportVersion.text = String.format(
                formatNewerVersionOther,
                remoteVersionValue(
                    selectedVersion,
                    if (pgToolsTestVersion) VersionCheckSource.PGTOOLS_TEST else VersionCheckSource.PGTOOLS
                ),
                if (pgToolsTestVersion) " (${getString(R.string.testVersion)})" else ""
            )
        }

        updateSelectedPgToolsVersionText()

        fun renderRemoteVersionLabels() {
            updateSelectedPgToolsVersionText()
            for (apps in appsInfo) {
                if (apps.appName in setOf("pok", "pokAres", "PGTools")) continue
                val source = when (apps.appName) {
                    "pgSharpRoot" -> VersionCheckSource.PGSHARP
                    "wrapper" -> VersionCheckSource.DATA_HUB
                    "polygon" -> VersionCheckSource.POLYGON
                    "pokemod" -> VersionCheckSource.POKEMOD_APP
                    "defit" -> VersionCheckSource.DEFIT
                    "APKMirrorInstaller" -> VersionCheckSource.APKMIRROR
                    else -> VersionCheckSource.APP_INFO
                }
                val hasResult = when (apps.appName) {
                    "pgSharpRoot" -> pgSharpRootSupportDataLoaded
                    "wrapper" -> apps.downloadLink.isNotBlank()
                    else -> apps.appName in loadedAppVersions
                }
                view.findViewById<TextView>(apps.newVersionTextId)?.text = getString(
                    R.string.version_label_latest,
                    remoteVersionValue(apps.newVersion.takeIf { hasResult }, source)
                )
            }
        }

        fun setDownloadActionForVersionError(button: Button, packageName: String) {
            if (appInstalledOrNot(packageName)) {
                setOpenAppAction(button)
            } else {
                setVersionCheckFailedAction(button)
            }
        }

        fun setPokemonDownloadActionsForVersionError() {
            setDownloadActionForVersionError(pokeDownloadButton, pokePackageName)
            setDownloadActionForVersionError(pokeAresDownloadButton, pokeAresPackageName)
        }

        fun completeCheck(source: VersionCheckSource) {
            if (!checkJob.isActive || versionCheckJob !== checkJob) return
            if (progress.complete(source)) {
                versionRefreshPolicy.onFinished(SystemClock.elapsedRealtime(), !hasVersionCheckError)
                Log.i("AppsPoke", "AllCheckDone!!! elapsedMs=${SystemClock.elapsedRealtime() - startedAt} failed=$failedSources")
                swipeRefreshLayout.isRefreshing = false
            }
            // Rendering only: completion must not trigger a new request or action recomputation.
            renderRemoteVersionLabels()
            updateToolbarSubtitle()
        }

        fun launchCheck(source: VersionCheckSource, block: suspend () -> Unit) {
            checkScope.launch {
                val sourceStartedAt = SystemClock.elapsedRealtime()
                Log.i("AppsPoke", "CheckStart source=$source")
                try {
                    block()
                } catch (e: CancellationException) {
                    Log.i("AppsPoke", "CheckCancelled source=$source")
                    throw e
                } catch (e: Exception) {
                    Log.e("AppsPoke", "CheckFailed source=$source", e)
                    markVersionCheckFailed(source)
                } finally {
                    if (checkJob.isActive && versionCheckJob === checkJob) {
                        Log.i("AppsPoke", "CheckDone source=$source elapsedMs=${SystemClock.elapsedRealtime() - sourceStartedAt} failed=${source in failedSources}")
                        completeCheck(source)
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun checkAppVersion(fetchRemoteVersions: Boolean = false) {
            var needUpdateAppsAmount = 0
            val download = getString(R.string.download)
            val update = getString(R.string.update)

            val pgSharpRootInstalledVersion = appInstalledVersion(pgSharpRootPackageName)
            val pgToolsInstalledVersion = appInstalledVersion(pgToolsPackageName)
            val pokInstalledVersion = appInstalledVersion(pokePackageName)
            val pokeAresInstalledVersion = appInstalledVersion(pokeAresPackageName)

            //check app if installed then show remove button and more button
            for (mapping in appsInfo) {
                val removeButton = view.findViewById<Button>(mapping.removeButtonId)
                val moreButton = view.findViewById<ImageButton>(mapping.moreButtonId)

                val visibility = viewShowOrHide(appInstalledOrNot(mapping.packageName))

                removeButton?.visibility = visibility
                moreButton?.visibility = visibility
            }

            if (fetchRemoteVersions) {
                launchCheck(VersionCheckSource.POKEMOD_APP) {
                    val apps = appsInfo.first { it.appName == "pokemod" }
                    val version = Pokemod().getPokemodVersion(getString(R.string.url_PokemodDownload))
                    currentCoroutineContext().ensureActive()
                    if (version.isBlank() || version == "ERROR") {
                        unavailableAppVersions.add(apps.appName)
                        markVersionCheckFailed(VersionCheckSource.POKEMOD_APP)
                    } else {
                        apps.newVersion = version
                        loadedAppVersions.add(apps.appName)
                        unavailableAppVersions.remove(apps.appName)
                    }
                    checkAppVersion()
                }

                fun checkApkPureVersion(source: VersionCheckSource, appName: String, urlRes: Int) {
                    launchCheck(source) {
                        val version = withContext(Dispatchers.IO) {
                            if (appName == "defit") ApkPure().getDeFitLatestVersion()
                            else ApkPure().getAPKMirrorInstallerLatestVersion()
                        }
                        val apps = appsInfo.first { it.appName == appName }
                        if (version.isBlank() || version == "ERROR") {
                            unavailableAppVersions.add(appName)
                            markVersionCheckFailed(source)
                        } else {
                            val urlVersion = version.replace(".", "-").replace(" (", "-").replace(")", "")
                            apps.newVersion = version
                            apps.downloadLink = getString(urlRes, urlVersion, urlVersion)
                            loadedAppVersions.add(appName)
                            unavailableAppVersions.remove(appName)
                        }
                        // Apply metadata and actions on Main as soon as this source finishes.
                        checkAppVersion()
                    }
                }
                checkApkPureVersion(VersionCheckSource.DEFIT, "defit", R.string.url_defit)
                checkApkPureVersion(VersionCheckSource.APKMIRROR, "APKMirrorInstaller", R.string.url_apkMirrorInstaller)

                launchCheck(VersionCheckSource.APP_INFO) {
                    extractAppVersionsFromJson(
                        getString(R.string.url_appInfo),
                        onAppVersionsExtracted = { appUpdates, pokAresUrl ->
                            for (update in appUpdates) {
                                val apps = appsInfo.find { it.appName == update.appName } ?: continue
                                update.newVersion?.let {
                                    apps.newVersion = it
                                    loadedAppVersions.add(apps.appName)
                                    unavailableAppVersions.remove(apps.appName)
                                }
                                update.downloadLink?.let { apps.downloadLink = it }
                                update.officialLink?.let { apps.officialLink = it }
                            }
                            url_pokAres = pokAresUrl
                            checkAppVersion()
                        },
                        onError = {
                            appsInfo.filter { it.updateNewVersionFromJson }.forEach {
                                unavailableAppVersions.add(it.appName)
                            }
                            markVersionCheckFailed(VersionCheckSource.APP_INFO)
                            checkAppVersion()
                        }
                    )
                }
            }

            for (apps in appsInfo) {
                val appName = apps.appName
                val packageName = apps.packageName
                val installVersion = appInstalledVersion(packageName)
                val installVersionTextView = view.findViewById<TextView>(apps.installVersionTextId)

                if (appName == "pok" ||
                    appName == "pokAres" && (Build.MANUFACTURER == "samsung" || pokAresNoSupportDevices)
                ) {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersionOther,
                            appInstalledVersion(packageName),
                            ""
                        )
                } else if (appName == "pokAres") {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersionOther,
                            appInstalledVersion(pokeAresPackageName),
                            "(${getString(R.string.unsupportedDevices)})"
                        )
                } else {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersion,
                            installVersion
                        )
                }
            }

            Log.i("AppsPoke", "choosePogoVersion: $choosePogoVersion")

            //check poke version
            if (choosePogoVersion != "未安裝" && pokInstalledVersion != "未安裝") {
                val versionComparison = compareVersions(choosePogoVersion, pokInstalledVersion)
                Log.i("Poke", "versionComparison: $versionComparison")

                if (versionComparison < 0) {
                    view.findViewById<TextView>(R.id.pok_install_version).text =
                        String.format(
                            formatInstallVersionOther,
                            appInstalledVersion(pokePackageName) + getString(R.string.versionTooHigh),
                            ""
                        )
                    pokeDownloadButton.isEnabled = false
                } else if (versionComparison > 0) {
                    setDownloadAction(pokeDownloadButton, update)
                    pokeDownloadButton.isEnabled = true
                    needUpdateAppsAmount++
                } else {
                    setOpenAppAction(pokeDownloadButton)
                    pokeDownloadButton.isEnabled = true
                }
            } else {
                setDownloadAction(pokeDownloadButton, download)
                pokeDownloadButton.isEnabled = true
            }

            //check pokeAres version
            if (choosePogoVersion != "未安裝" && pokeAresInstalledVersion != "未安裝") {
                val versionComparison = compareVersions(choosePogoVersion, pokeAresInstalledVersion)
                Log.i("PokeAres", "versionComparison: $versionComparison")

                if (versionComparison < 0) {
                    view.findViewById<TextView>(R.id.pokAres_install_version).text =
                        String.format(
                            formatInstallVersionOther,
                            appInstalledVersion(pokeAresPackageName) + getString(R.string.versionTooHigh),
                            "(${getString(R.string.unsupportedDevices)})"
                        )
                    pokeAresDownloadButton.isEnabled = false
                } else if (versionComparison > 0) {
                    setDownloadAction(pokeAresDownloadButton, update)
                    pokeAresDownloadButton.isEnabled = true
                    needUpdateAppsAmount++
                } else {
                    setOpenAppAction(pokeAresDownloadButton)
                    pokeAresDownloadButton.isEnabled = true
                }
            } else {
                setDownloadAction(pokeAresDownloadButton, download)
                pokeAresDownloadButton.isEnabled = true
            }

            val pgSharpRootInfo = appsInfo.find { it.appName == "pgSharpRoot" }
            val pgSharpRootVersion = pgSharpRootInfo?.newVersion
                ?: getString(R.string.notInstalled)
            if (pgSharpRootVersionCheckFailed) {
                setDownloadActionForVersionError(
                    pgSharpRootDownloadButton,
                    pgSharpRootPackageName
                )
            } else if (pgSharpRootVersion != getString(R.string.notInstalled) &&
                pgSharpRootInstalledVersion != getString(R.string.notInstalled)
            ) {
                if (compareVersions(pgSharpRootVersion, pgSharpRootInstalledVersion) > 0) {
                    setDownloadAction(pgSharpRootDownloadButton, update)
                    needUpdateAppsAmount++
                } else {
                    setOpenAppAction(pgSharpRootDownloadButton)
                }
            } else if (pgSharpRootInstalledVersion != getString(R.string.notInstalled)) {
                setOpenAppAction(pgSharpRootDownloadButton)
            } else {
                setDownloadAction(pgSharpRootDownloadButton, download)
            }

            // The setting selects which PGTools channel controls this single card.
            val selectedPgToolsVersion = if (pgToolsTestVersion) {
                pgToolsTestAppVersion
            } else {
                pgToolsProductionAppVersion
            }
            val selectedPgToolsCheckFailed = if (pgToolsTestVersion) {
                pgToolsTestVersionCheckFailed
            } else {
                pgToolsProductionVersionCheckFailed
            }

            if (selectedPgToolsCheckFailed) {
                setDownloadActionForVersionError(pgToolsDownloadButton, pgToolsPackageName)
            } else if (selectedPgToolsVersion != "未安裝" &&
                pgToolsInstalledVersion != "未安裝"
            ) {
                val versionComparison = compareVersions(
                    selectedPgToolsVersion,
                    pgToolsInstalledVersion
                )

                if (versionComparison > 0) {
                    setDownloadAction(pgToolsDownloadButton, update)
                    needUpdateAppsAmount++
                } else {
                    setOpenAppAction(pgToolsDownloadButton)
                }
            } else {
                setDownloadAction(pgToolsDownloadButton, download)
            }

            val dataHubInfo = appsInfo.find { it.appName == "wrapper" }
            val dataHubInstalledVersion = appInstalledVersion(dataHubPackageName)
            val dataHubInstalled = dataHubInstalledVersion != getString(R.string.notInstalled)
            dataHubNeedsUpdate = false
            when {
                dataHubVersionCheckFailed -> {
                    setDownloadActionForVersionError(dataHubDownloadButton, dataHubPackageName)
                }
                dataHubInfo == null || dataHubInfo.downloadLink.isBlank() -> {
                    if (dataHubInstalled) {
                        setOpenAppAction(dataHubDownloadButton)
                    } else {
                        setDownloadAction(dataHubDownloadButton, download)
                        dataHubDownloadButton.isEnabled = false
                    }
                }
                !dataHubInstalled -> setDownloadAction(dataHubDownloadButton, download)
                compareVersions(dataHubInfo.newVersion, dataHubInstalledVersion) > 0 -> {
                    dataHubNeedsUpdate = true
                    setDownloadAction(dataHubDownloadButton, update)
                }
                else -> setOpenAppAction(dataHubDownloadButton)
            }

            // Recompute the total once from the current snapshot, regardless of callback order.
            for (apps in appsInfo.filter { it.checkNeedUpdateAppsAmount }) {
                val button = view.findViewById<Button>(apps.downloadButtonId)
                val text = view.findViewById<TextView>(apps.newVersionTextId)
                val installedVersion = appInstalledVersion(apps.packageName)
                val installed = installedVersion != getString(R.string.notInstalled)
                if (apps.appName in loadedAppVersions) {
                    text.text = String.format(formatNewerVersion, apps.newVersion)
                    val needsUpdate = if (apps.appName == "polygon") {
                        polygonVersionResult?.status == PolygonXCheckResult.Status.UPDATE_REQUIRED
                    } else compareVersions(apps.newVersion, installedVersion) > 0
                    when {
                        !installed -> setDownloadAction(button, download)
                        needsUpdate -> {
                            setDownloadAction(button, update)
                            needUpdateAppsAmount++
                        }
                        else -> setOpenAppAction(button)
                    }
                    if (!isOpenAppAction(button) && apps.downloadLink.isBlank()) {
                        // Version and download metadata can arrive independently (e.g. Pokemod).
                        if (installed) setOpenAppAction(button) else button.isEnabled = false
                    }
                } else if (apps.appName in unavailableAppVersions) {
                    setNewVersionError(text)
                    setDownloadActionForVersionError(button, apps.packageName)
                }
            }
            if (noEligiblePokemonVersions) setPokemonDownloadActionsForVersionError()
            renderRemoteVersionLabels()
            updateToolbarSubtitle(needUpdateAppsAmount)
        }

        fun getDataHubVersion() {
            launchCheck(VersionCheckSource.DATA_HUB) {
                val versionInfo = DataHubApi().getVersionInfo(
                    getString(R.string.url_DataHubJson),
                    getString(R.string.url_DataHubDownload)
                )
                currentCoroutineContext().ensureActive()
                val apps = appsInfo.first { it.appName == "wrapper" }
                val text = view.findViewById<TextView>(R.id.wrapper_new_version)
                if (versionInfo == null) {
                    dataHubVersionCheckFailed = apps.downloadLink.isBlank()
                    if (dataHubVersionCheckFailed) setNewVersionError(text)
                    markVersionCheckFailed(VersionCheckSource.DATA_HUB)
                } else {
                    dataHubVersionCheckFailed = false
                    apps.newVersion = versionInfo.appVersion
                    apps.downloadLink = versionInfo.downloadUrl
                    text.text = String.format(formatNewerVersion, versionInfo.appVersion)
                }
                checkAppVersion()
            }
        }

        var polygonRequestInFlight = false
        fun getPolygonXVersion() {
            if (polygonRequestInFlight) return
            polygonRequestInFlight = true
            launchCheck(VersionCheckSource.POLYGON) {
                try {
                    val installedCode = appInstalledVersionCode(getString(R.string.packageName_polygonX)).toInt()
                    val result = app.lokey0905.location.api.polygonX().checkPolygonXUpdate(installedCode)
                    currentCoroutineContext().ensureActive()
                    val apps = appsInfo.first { it.appName == "polygon" }
                    polygonCheckedInstalledVersion = installedCode
                    if (result.status == PolygonXCheckResult.Status.FAILURE || result.latestVersionCode.isNullOrBlank()) {
                        unavailableAppVersions.add(apps.appName)
                        markVersionCheckFailed(VersionCheckSource.POLYGON)
                    } else {
                        polygonVersionResult = result
                        apps.newVersion = result.latestVersionCode
                        apps.downloadLink =
                            "https://polygonx.dl.assets.evermorelabs.io/apk/com.evermorelabs.polygonx-${result.latestVersionCode}.apk"
                        loadedAppVersions.add(apps.appName)
                        unavailableAppVersions.remove(apps.appName)
                    }
                    checkAppVersion()
                } finally {
                    polygonRequestInFlight = false
                }
            }
        }

        fun getPGSharpRootVersion() {
            launchCheck(VersionCheckSource.PGSHARP) {
                val versionInfo = PgSharpRootApi().getVersionInfo(getString(R.string.url_PGSharpRootAPI))
                currentCoroutineContext().ensureActive()
                val apps = appsInfo.first { it.appName == "pgSharpRoot" }
                if (versionInfo == null) {
                    pgSharpRootVersionCheckFailed = !pgSharpRootSupportDataLoaded
                    if (pgSharpRootVersionCheckFailed) {
                        setNewVersionError(view.findViewById(R.id.pgsharp_root_new_version))
                    }
                    markVersionCheckFailed(VersionCheckSource.PGSHARP)
                } else {
                    apps.newVersion = versionInfo.appVersion
                    apps.downloadLink = versionInfo.downloadUrl
                    pgSharpRootVersionsList.clear()
                    pgSharpRootVersionsList.addAll(versionInfo.supportedPogoVersions.sortedWith { a, b ->
                        compareVersions(b, a)
                    })
                    pgSharpRootSupportDataLoaded = true
                    pgSharpRootVersionCheckFailed = false
                    view.findViewById<TextView>(R.id.pgsharp_root_new_version).text =
                        String.format(formatNewerVersion, versionInfo.appVersion)
                }
                updateMinimumFilteredSupportedVersions(
                    R.id.supportVersion_PGSharpRoot, pgSharpRootVersionsList, pgSharpRootSupportDataLoaded
                )
                checkAppVersion()
                updateCompatibilitySummary(view)
            }
        }

        fun getPokemodSupportedVersion() {
            launchCheck(VersionCheckSource.POKEMOD_SUPPORT) {
                val api = Pokemod()
                val orderedVersions = nowPogoVersionsList.map { it.pogoVersion }.distinct()
                val remaining = prioritizeVersions(orderedVersions, choosePogoVersion).toMutableList()
                val completed = mutableSetOf<String>()
                pokemodSupportDataLoaded = false
                // Old successful results remain visible during a refresh; only successful probes replace them.
                pokemodVersionResults.keys.retainAll(orderedVersions.toSet())
                while (remaining.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    // If the user switches targets mid-batch, probe that target next.
                    val version = if (remaining.remove(choosePogoVersion)) choosePogoVersion else remaining.removeAt(0)
                    var unsupported = api.checkPokemod(getString(R.string.url_PokemodAPI, version))
                    currentCoroutineContext().ensureActive()
                    if (unsupported == null) {
                        Log.w("AppsPoke", "Primary Pokemod API failed; trying backup for $version")
                        unsupported = api.checkPokemod(getString(R.string.url_PokemodAPIBackup, version))
                        currentCoroutineContext().ensureActive()
                    }
                    if (unsupported == null) {
                        markVersionCheckFailed(VersionCheckSource.POKEMOD_SUPPORT)
                    } else {
                        pokemodVersionResults[version] = !unsupported
                        completed.add(version)
                    }
                    // Keep the already-sorted game-list order; do not sort on every UI update.
                    pokemodVersionList.clear()
                    pokemodVersionList.addAll(orderedVersions.filter { pokemodVersionResults[it] == true })
                    pokemodSupportDataLoaded = completed.size == orderedVersions.size
                    updateMinimumFilteredSupportedVersions(
                        R.id.supportVersion_pokemod,
                        pokemodVersionList,
                        pokemodVersionList.isNotEmpty() || pokemodSupportDataLoaded
                    )
                    updateCompatibilitySummary(view)
                    Log.i("AppsPoke", "PokemodProbe version=$version supported=${unsupported?.not()}")
                }
                if (orderedVersions.isEmpty()) {
                    markVersionCheckFailed(VersionCheckSource.POKEMOD_SUPPORT)
                    updateCompatibilitySummary(view)
                }
            }
        }

        fun selectAvailableTarget() {
            val available = nowPogoVersionsList.map { it.pogoVersion }.ifEmpty {
                filterVersionsAtOrAboveMinimum(
                    pgtoolsVersionsList.ifEmpty { pgToolsTestVersionsList }.map { it.pogoVersion },
                    pokemonMinVersion
                )
            }.distinct()
            setVersionListAdapter(available)
            val target = selectTargetVersion(
                available, pgtoolsVersionsList.map { it.pogoVersion },
                pokemonMinVersion, choosePogoVersion, shouldPreservePogoVersionSelection(),
                availableVersionsConfirmed = pokemonListResolved || nowPogoVersionsList.isNotEmpty()
            )
            if (target != null) {
                noEligiblePokemonVersions = false
                autoCompleteTextView?.setText(target, false)
                updatePogoVersionSelection(target, view)
            } else if (pokemonListResolved) {
                // An authoritative empty eligible list is not a network failure/stale-cache case.
                noEligiblePokemonVersions = true
                choosePogoVersion = getString(R.string.notInstalled)
                preservePogoVersionSelection = false
                autoCompleteTextView?.setText("", false)
                pokemodVersionResults.clear()
                pokemodVersionList.clear()
                pokemodSupportDataLoaded = false
                val noVersions = getString(R.string.versionCheckNoSupportedVersion)
                pokeSupportVersion.text = getString(R.string.version_label_target, noVersions)
                pokeAresSupportVersion.text = getString(R.string.version_label_target, noVersions)
                setSupportVersionStatus(R.id.supportVersion_pokemod, getString(R.string.compatibility_unknown))
                setPokemonDownloadActionsForVersionError()
            }
        }

        fun getPGToolsVersion(isTestVersion: Boolean = false) {
            val source = if (isTestVersion) VersionCheckSource.PGTOOLS_TEST else VersionCheckSource.PGTOOLS
            launchCheck(source) {
                extractPgToolsV2FromJson(
                    getString(if (isTestVersion) R.string.url_PGToolsTestJson else R.string.url_PGToolsJson),
                    isTestVersion,
                    onAppVersionsExtracted = { _, appVersion, appUrl, versions ->
                        if (isTestVersion) {
                            pgToolsTestVersionCheckFailed = false
                            pgToolsTestAppVersion = appVersion
                            pgToolsTestUrl = appUrl
                            pgToolsTestVersionsList.clear()
                            pgToolsTestVersionsList.addAll(versions)
                            pgToolsTestSupportDataLoaded = true
                        } else {
                            pgToolsProductionVersionCheckFailed = false
                            pgToolsProductionAppVersion = appVersion
                            pgToolsProductionUrl = appUrl
                            pgtoolsVersionsList.clear()
                            pgtoolsVersionsList.addAll(versions)
                            pgToolsSupportDataLoaded = true
                        }
                        selectAvailableTarget()
                        refreshMinimumFilteredToolVersions()
                        updateSelectedPgToolsVersionText()
                        checkAppVersion()
                    },
                    onError = {
                        if (isTestVersion) pgToolsTestVersionCheckFailed = !pgToolsTestSupportDataLoaded
                        else pgToolsProductionVersionCheckFailed = !pgToolsSupportDataLoaded
                        refreshMinimumFilteredToolVersions()
                        updateSelectedPgToolsVersionText()
                        checkAppVersion()
                        markVersionCheckFailed(source)
                    }
                )
            }
        }

        fun getPokemonAllowLoginList() {
            launchCheck(VersionCheckSource.POKEMON_LIST) {
                try {
                    val allVersions = withContext(Dispatchers.IO) { ApkPure().getPokemonGoVersions() }
                    if (allVersions.isEmpty()) {
                        markVersionCheckFailed(VersionCheckSource.POKEMON_LIST)
                        if (nowPogoVersionsList.isEmpty()) {
                            setSupportVersionStatus(R.id.supportVersion_pokemon, getString(R.string.compatibility_unknown))
                            setNewVersionError(pokeSupportVersion)
                            setNewVersionError(pokeAresSupportVersion)
                            setPokemonDownloadActionsForVersionError()
                        }
                        // Do not erase a valid selection or a previously successful game list.
                        selectAvailableTarget()
                    } else {
                        val pokeUrlFormat = getString(R.string.url_poke)
                        val versions = withContext(Dispatchers.Default) {
                            allVersions.filter { compareVersions(it.versionName, pokemonMinVersion) >= 0 }
                                .map { version ->
                                    PogoVersionInfo(
                                        version.versionName,
                                        version.versionCode.toLongOrNull() ?: 0L,
                                        "https://assets-v2.pgtools.net/games/${version.versionName}-arm64.apkm",
                                        String.format(
                                            pokeUrlFormat,
                                            version.versionName.replace(".", "-"),
                                            version.versionName.replace(".", "-")
                                        )
                                    )
                                }.sortedByDescending { it.pogoVersionNumber }
                        }
                        nowPogoVersionsList.clear()
                        nowPogoVersionsList.addAll(versions)
                        // Publish the list and its confirmation atomically on Main.
                        pokemonListResolved = true
                        view.findViewById<TextView>(R.id.supportVersion_pokemon).text =
                            versions.joinToString(", ") { it.pogoVersion }
                                .ifBlank { getString(R.string.versionCheckNoSupportedVersion) }
                        selectAvailableTarget()
                        checkAppVersion()
                    }
                    updateCompatibilitySummary(view)
                } finally {
                    if (checkJob.isActive && versionCheckJob === checkJob) {
                        if (nowPogoVersionsList.isNotEmpty()) getPokemodSupportedVersion()
                        else {
                            Log.i("AppsPoke", "CheckSkipped source=POKEMOD_SUPPORT reason=no_game_versions")
                            completeCheck(VersionCheckSource.POKEMOD_SUPPORT)
                        }
                    }
                }
            }
        }

        fun getPokemonAllowLoginMin() {
            launchCheck(VersionCheckSource.POKEMON_MINIMUM) {
                try {
                    val minVersion = Pokemon().checkPokemon(getString(R.string.url_PokemonCheckVersionAPI))
                    currentCoroutineContext().ensureActive()
                    if (minVersion != "ERROR" && minVersion.isNotBlank()) {
                        pokemonMinVersion = minVersion
                        view.findViewById<TextView>(R.id.supportVersion_pokemon_min).text =
                            "${getString(R.string.appsPokePage_supportVersion_pokemon_min)} $minVersion"
                    } else {
                        if (pokemonMinVersion.isBlank()) {
                            view.findViewById<TextView>(R.id.supportVersion_pokemon_min).text =
                                "${getString(R.string.appsPokePage_supportVersion_pokemon_min)} ${getString(R.string.versionCheckFailedShort)}"
                        }
                        markVersionCheckFailed(VersionCheckSource.POKEMON_MINIMUM)
                    }
                    refreshMinimumFilteredToolVersions()
                } finally {
                    if (checkJob.isActive && versionCheckJob === checkJob) getPokemonAllowLoginList()
                }
            }
        }

        fun setOnCheckedChangeListener() {
            autoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                val version = parent.getItemAtPosition(position).toString()
                preservePogoVersionSelection = true
                updatePogoVersionSelection(version, view)
                checkAppVersion()
            }
        }

        refreshLocalAppState = {
            updateSelectedPgToolsVersionText()
            checkAppVersion()
            // Installing/removing Polygon changes the version code sent to its API.
            if (progress.isComplete && polygonCheckedInstalledVersion !=
                appInstalledVersionCode(getString(R.string.packageName_polygonX)).toInt()
            ) getPolygonXVersion()
        }
        refreshMinimumFilteredToolVersions()
        checkAppVersion(fetchRemoteVersions = true)
        getDataHubVersion()
        getPGSharpRootVersion()
        getPolygonXVersion()
        getPGToolsVersion()
        getPGToolsVersion(isTestVersion = true)
        getPokemonAllowLoginMin()

        setOnCheckedChangeListener()
    }

    private fun shouldPreservePogoVersionSelection(): Boolean {
        return preservePogoVersionSelection &&
                choosePogoVersion.isNotBlank() &&
                 choosePogoVersion != getString(R.string.notInstalled)
    }

    private fun updateSupportedVersionsTextView(textViewId: Int, versions: List<String>) {
        val textView = view?.findViewById<TextView>(textViewId)
        val versionsText = versions.joinToString(", ")
        textView?.text = versionsText
    }

    private fun updatePogoVersionSelection(version: String, view: View) {
        val formatNewerVersionOther: String =
            getString(R.string.version_label_target_other)
        val pokeSupportVersion = view.findViewById<TextView>(R.id.pok_new_version)
        val pokeAresSupportVersion = view.findViewById<TextView>(R.id.pokAres_new_version)

        val versionInfo = nowPogoVersionsList.firstOrNull { it.pogoVersion == version }
            ?: pgtoolsVersionsList.firstOrNull { it.pogoVersion == version }
            ?: pgToolsTestVersionsList.firstOrNull { it.pogoVersion == version }
        val arm64Url = versionInfo?.pogoARM64URLPGTools.orEmpty()

        if (versionInfo != null) {
            choosePogoVersion = versionInfo.pogoVersion
        }

        if (arm64Url.isNotEmpty()) {
            pokeSupportVersion.text = String.format(
                formatNewerVersionOther,
                choosePogoVersion,
                ""
            )
            pokeAresSupportVersion.text = String.format(
                formatNewerVersionOther,
                choosePogoVersion,
                ""
            )
        }

        updateCompatibilitySummary(view)
    }

    private suspend fun extractAppVersionsFromJson(
        url: String,
        onAppVersionsExtracted: (List<AppsInfoUpdate>, String) -> Unit,
        onError: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val urlObject = URL(url)
                val activeConnection = urlObject.openConnection() as HttpURLConnection
                connection = activeConnection
                activeConnection.requestMethod = "GET"
                activeConnection.connectTimeout = 15000
                activeConnection.readTimeout = 20000

                val responseCode = activeConnection.responseCode
                if (responseCode !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
                    throw IOException("App version request failed with HTTP $responseCode")
                }

                val inputStream = activeConnection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()

                var line: String? = bufferedReader.readLine()
                while (line != null) {
                    response.append(line)
                    line = bufferedReader.readLine()
                }

                bufferedReader.close()

                val jsonObject = JSONObject(response.toString())
                val pogo = jsonObject.getJSONObject("pogo")
                val appUpdates = ArrayList<AppsInfoUpdate>()

                for (apps in appsInfo) {
                    if (!apps.updateNewVersionFromJson &&
                        !apps.updateDownloadLinkFromJson &&
                        !apps.updateOfficialLinkFromJson
                    ) {
                        continue
                    }

                    val appInfo = pogo.optJSONObject(apps.appName)
                    if (appInfo == null) {
                        Log.w(
                            "extractAppVersionsFromJson",
                            "Missing optional app entry: ${apps.appName}"
                        )
                        continue
                    }
                    appUpdates.add(
                        AppsInfoUpdate(
                            appName = apps.appName,
                            newVersion = if (apps.updateNewVersionFromJson) {
                                appInfo.optString("version").takeIf { it.isNotBlank() }
                            } else null,
                            downloadLink = if (apps.updateDownloadLinkFromJson) {
                                appInfo.optString("url").takeIf { it.isNotBlank() }
                            } else null,
                            officialLink = if (apps.updateOfficialLinkFromJson) {
                                appInfo.optString("officialLink").takeIf { it.isNotBlank() }
                            } else null
                        )
                    )
                }

                val extractedPokAresUrl = pogo.optJSONObject("pokAres")
                    ?.optString("url")
                    .orEmpty()
                currentCoroutineContext().ensureActive()

                withContext(Dispatchers.Main) {
                    onAppVersionsExtracted(appUpdates, extractedPokAresUrl)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("extractAppVersionsFromJson", "Failed to fetch app versions", e)
                withContext(Dispatchers.Main) {
                    onError()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private suspend fun extractPgToolsV2FromJson(
        url: String,
        isTestVersion: Boolean,
        onAppVersionsExtracted: (String, String, String, List<PogoVersionInfo>) -> Unit,
        onError: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val activeConnection = URL(url).openConnection() as HttpURLConnection
                connection = activeConnection
                activeConnection.requestMethod = "GET"
                activeConnection.connectTimeout = 15000
                activeConnection.readTimeout = 20000

                val responseCode = activeConnection.responseCode
                if (responseCode !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
                    throw IOException("PGTools V2 request failed with HTTP $responseCode")
                }

                val response = activeConnection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val assetEndpoint = jsonObject.getString("assetEndpoint").trimEnd('/')
                val supportedVersions = jsonObject.getJSONObject("supportedVersions")

                var latestAppVersionCode = -1
                var latestAppVersionName = ""
                var latestAppVersionHash = ""
                val extractedVersions = ArrayList<PogoVersionInfo>()
                val versionKeys = supportedVersions.keys()

                while (versionKeys.hasNext()) {
                    currentCoroutineContext().ensureActive()
                    val versionKey = versionKeys.next()
                    val versionData = supportedVersions.getJSONObject(versionKey)
                    val gameData = versionData.optJSONObject("game")
                    val pogoVersion = gameData
                        ?.optString("gameVersionName", versionKey)
                        ?.ifBlank { versionKey }
                        ?: versionKey
                    val pogoVersionNumber = gameData
                        ?.optJSONArray("gameVersionCodes")
                        ?.optLong(0)
                        ?: 0L

                    val appData = versionData.optJSONObject("app")
                    val appVersionCode = appData?.optInt("appVersionCode", -1) ?: -1
                    if (appVersionCode > latestAppVersionCode) {
                        latestAppVersionCode = appVersionCode
                        latestAppVersionName = appData?.optString("appVersionName").orEmpty()
                        latestAppVersionHash = appData?.optString("appVersionHash").orEmpty()
                    }

                    val versionInfo = PogoVersionInfo(
                        pogoVersion = pogoVersion,
                        pogoVersionNumber = pogoVersionNumber,
                        pogoARM64URLPGTools = "$assetEndpoint/games/$pogoVersion-arm64.apkm",
                        pogoARM64URLAPKMirror = String.format(
                            getString(R.string.url_poke),
                            pogoVersion.replace(".", "-"),
                            pogoVersion.replace(".", "-")
                        )
                    )
                    if (extractedVersions.none { it.pogoVersion == pogoVersion }) {
                        extractedVersions.add(versionInfo)
                    }
                }

                if (extractedVersions.isEmpty() ||
                    latestAppVersionName.isBlank() ||
                    latestAppVersionHash.isBlank()
                ) {
                    throw IOException("PGTools V2 response has no usable versions")
                }

                val sortedVersions = extractedVersions.sortedByDescending {
                    it.pogoVersionNumber
                }
                val appFilePrefix = if (isTestVersion) "test-pgtools" else "pgtools"
                val appUrl = "$assetEndpoint/$appFilePrefix-" +
                    "$latestAppVersionName-$latestAppVersionHash.apk"
                currentCoroutineContext().ensureActive()

                withContext(Dispatchers.Main) {
                    onAppVersionsExtracted(
                        sortedVersions.first().pogoVersion,
                        latestAppVersionName,
                        appUrl,
                        sortedVersions
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val channel = if (isTestVersion) "test" else "production"
                Log.e(
                    "extractPgToolsV2FromJson",
                    "Failed to fetch PGTools $channel versions",
                    e
                )
                withContext(Dispatchers.Main) {
                    onError()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(R.string.ok) { _, _ -> }
            .show()
    }

    private fun viewShowOrHide(show: Boolean): Int {
        return if (show) View.VISIBLE else View.GONE // if true then show else hide
    }

    private fun setDownloadAction(button: Button, text: String) {
        button.text = text
        button.tag = null
        button.isEnabled = true
    }

    private fun setOpenAppAction(button: Button) {
        button.text = getString(R.string.popup_menu_startApp)
        button.tag = DOWNLOAD_BUTTON_ACTION_OPEN_APP
        button.isEnabled = true
    }

    private fun setVersionCheckFailedAction(button: Button) {
        button.text = getString(R.string.versionCheckFailedShort)
        button.tag = null
        button.isEnabled = false
    }

    private fun isOpenAppAction(button: Button): Boolean {
        return button.tag == DOWNLOAD_BUTTON_ACTION_OPEN_APP
    }

    private fun openApp(packageName: String) {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)

        if (intent == null) {
            Toast.makeText(
                context,
                getString(R.string.somethingWrong),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val resolveInfo = requireContext().packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        if (resolveInfo != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                context,
                getString(R.string.somethingWrong),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun popupMenu(view: View, id: Int, packageName: String) {
        if (appInstalledVersion(packageName) == "未安裝") {
            appUnInstall(packageName)
            return
        }
        PopupMenu(requireContext(), view.findViewById<ImageButton>(id)).apply {
            menuInflater.inflate(R.menu.popup_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.open -> {
                        openApp(packageName)
                        true
                    }

                    R.id.officialLink -> {
                        val officialLink =
                            appsInfo.find { it.packageName == packageName }?.officialLink
                        if (officialLink == "" || officialLink == null) {
                            showAlertDialog(
                                getString(R.string.dialogAdNotReadyTitle),
                                getString(R.string.dialogAdNotReadyMessage)
                            )
                        } else {
                            startActivity(Intent(Intent.ACTION_VIEW, officialLink.toUri()))
                        }
                        true
                    }

                    R.id.setting -> {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                        true
                    }

                    else -> false
                }
            }
            show()
        }
    }

    private fun downloadAPPWithCheck(url: String) {
        if (url == "") {
            showAlertDialog(
                getString(R.string.dialogAdNotReadyTitle),
                getString(R.string.dialogAdNotReadyMessage)
            )
            return
        }

        val factory = LayoutInflater.from(requireContext())
        val imageView: View = factory.inflate(R.layout.dialog_imageview, null)
        var setview = false

        if (url.contains("mediafire")) {
            imageView.findViewById<ImageView>(R.id.dialog_imageview)
                .setImageResource(R.drawable.download_mediafire)
            setview = true
        } else if (url.contains("apkmirror")) {
            imageView.findViewById<ImageView>(R.id.dialog_imageview)
                .setImageResource(R.drawable.download_apk_e)
            setview = true
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(if (setview) imageView else null)
            .setTitle(getString(R.string.dialogDownloadTitle))
            .setMessage(getString(R.string.dialogDownloadMessage))
            .apply {
                setNeutralButton(R.string.cancel) { _, _ ->
                    Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT)
                        .show()
                }
                setPositiveButton(R.string.ok) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            }
            .show()
    }

    private fun boolToInstalled(boolean: Boolean): String {
        return if (boolean)
            getString(R.string.installed)
        else
            getString(R.string.notInstalled)
    }

    private fun appInstalledOrNot(packageName: String): Boolean {
        val pm = activity?.packageManager
        try {
            pm?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return false
    }

    private fun appInstalledVersion(packageName: String): String {
        if (appInstalledOrNot(packageName)) {
            val pm = activity?.packageManager
            try {
                pm?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                return pm?.getPackageInfo(
                    packageName,
                    PackageManager.GET_ACTIVITIES
                )?.versionName.toString()
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return getString(R.string.notInstalled)
    }

    private fun appInstalledVersionCode(packageName: String): Long {
        if (appInstalledOrNot(packageName)) {
            val pm = activity?.packageManager
            try {
                pm?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                return pm?.getPackageInfo(
                    packageName,
                    PackageManager.GET_ACTIVITIES
                )?.longVersionCode ?: 0L
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return 0L
    }

    private fun appUnInstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = "package:$packageName".toUri()
        startActivity(intent)
    }
}
