package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.catalog.CatalogApp
import app.lokey0905.location.catalog.CatalogCardsController
import app.lokey0905.location.api.MhnToolsChannel
import app.lokey0905.location.api.MhnToolsVersionInfo
import app.lokey0905.location.api.parseMhnToolsVersionInfo
import app.lokey0905.location.api.mergeMhnSupportedVersions
import app.lokey0905.location.version.CompatibilityStatus
import app.lokey0905.location.version.getCompatibilityStatus
import app.lokey0905.location.version.VersionCheckSummary
import app.lokey0905.location.version.getVersionCheckSummary
import app.lokey0905.location.version.selectMhnTargetVersionDuringRefresh
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

class AppsMHN : Fragment() {
    private companion object {
        const val DOWNLOAD_BUTTON_ACTION_OPEN_APP = "open_app"
        const val STATE_SELECTED_MHN_VERSION = "selected_mhn_version"
        const val STATE_MHN_TEST_VERSION = "mhn_test_version"
        const val STATE_SUPPORTED_VERSIONS_EXPANDED = "mhn_supported_versions_expanded"
    }

    private val gameVersionsMap = mutableMapOf<String, String>()
    private var mhnToolsVersion: String = "未安裝"
    private var mhnToolsUrl: String = ""
    private var mhnUrl: String = ""
    private var mhnVersion: String = "未安裝"

    private var mhn_download_on_apkmirror = false
    private var mhnTestVersion = false
    private var allSupportedVersionsExpanded = false
    private val mhnToolsResults = mutableMapOf<MhnToolsChannel, MhnToolsVersionInfo>()
    private val pendingMhnTools = mutableSetOf<MhnToolsChannel>()
    private val failedMhnTools = mutableSetOf<MhnToolsChannel>()
    private var versionCheckJob: Job? = null

    private var url_joystick = ""

    private var appListCheckFailed = false
    private var appListCheckDone = false
    private var needsRemoteRefresh = true
    private var refreshLocalAppState: (() -> Unit)? = null
    private var installerCard: CatalogCardsController? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_mhn, container, false)
        (requireActivity() as app.lokey0905.location.MainActivity).bindSettingsToolbar(view)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val preferredTestVersion = sharedPreferences.getBoolean("mhntools_testversion", false)
        mhnTestVersion = savedInstanceState?.getBoolean(STATE_MHN_TEST_VERSION, preferredTestVersion)
            ?: preferredTestVersion
        savedInstanceState?.getString(STATE_SELECTED_MHN_VERSION)
            ?.takeIf { it.isNotBlank() }
            ?.let { mhnVersion = it }
        allSupportedVersionsExpanded = savedInstanceState?.getBoolean(
            STATE_SUPPORTED_VERSIONS_EXPANDED, allSupportedVersionsExpanded
        ) ?: allSupportedVersionsExpanded
        mhn_download_on_apkmirror = sharedPreferences.getBoolean("mhn_download_on_apkmirror", false)

        setupCompatibilityCard(view)
        setupListeners(view)

        MobileAds.initialize(requireActivity())
        val mAdView = view.findViewById<AdView>(R.id.ad_banner)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        installerCard = CatalogCardsController(
            this, view.findViewById(R.id.mhn_installer_cards),
            listOf(CatalogApp.APKMIRROR_INSTALLER)
        )
        parentFragmentManager.setFragmentResultListener(
            "mhn_download_on_apkmirror", viewLifecycleOwner
        ) { _, bundle ->
            val useApkMirror = bundle.getBoolean("bundleKey")
            if (mhn_download_on_apkmirror != useApkMirror) {
                mhn_download_on_apkmirror = useApkMirror
                needsRemoteRefresh = true
                refreshVersionInfoIfNeeded()
            }
        }

        parentFragmentManager.setFragmentResultListener(
            "mhntools_testversion", viewLifecycleOwner
        ) { _, bundle ->
            val useTestVersion = bundle.getBoolean("bundleKey")
            if (mhnTestVersion != useTestVersion) {
                mhnTestVersion = useTestVersion
                if (isResumed && !isHidden) refreshLocalAppState?.invoke()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_MHN_VERSION, mhnVersion)
        outState.putBoolean(STATE_MHN_TEST_VERSION, mhnTestVersion)
        outState.putBoolean(STATE_SUPPORTED_VERSIONS_EXPANDED, allSupportedVersionsExpanded)
    }

    override fun onResume() {
        super.onResume()
        mhnTestVersion = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean("mhntools_testversion", false)
        refreshVersionInfoIfNeeded()
        installerCard?.refresh()
    }

    override fun onStop() {
        // A hidden tab stays STARTED; only leaving the Activity requests a new remote check.
        needsRemoteRefresh = true
        super.onStop()
    }

    override fun onDestroyView() {
        versionCheckJob?.cancel()
        versionCheckJob = null
        refreshLocalAppState = null
        installerCard = null
        needsRemoteRefresh = true
        super.onDestroyView()
    }

    private fun refreshVersionInfoIfNeeded() {
        if (isHidden || !isResumed) return
        val currentView = view ?: return
        if (needsRemoteRefresh || refreshLocalAppState == null) {
            needsRemoteRefresh = false
            setupAppVersionInfo(currentView)
        } else {
            refreshLocalAppState?.invoke()
        }
    }

    private fun setupListeners(view: View) {
        val mhnDownloadButton = view.findViewById<Button>(R.id.download_mhn)
        val mhnRemoveButton = view.findViewById<Button>(R.id.remove_mhn)
        val mhnToolsDownloadButton = view.findViewById<Button>(R.id.download_mhnTools)
        val mhnToolsRemoveButton = view.findViewById<Button>(R.id.remove_mhnTools)
        val gpsDownloadButton = view.findViewById<Button>(R.id.download_gps)
        val gpsRemoveButton = view.findViewById<Button>(R.id.remove_gps)
        val hylianerDownloadButton = view.findViewById<Button>(R.id.download_hylianer)
        val hylianerRemoveButton = view.findViewById<Button>(R.id.remove_hylianer)

        mhnDownloadButton.setOnClickListener {
            if (isOpenAppAction(mhnDownloadButton)) {
                openApp(resources.getString(R.string.packageName_MHNow))
                return@setOnClickListener
            }

            downloadAPPWithCheck(mhnUrl)
        }

        mhnToolsDownloadButton.setOnClickListener {
            if (isOpenAppAction(mhnToolsDownloadButton)) {
                openApp(resources.getString(R.string.packageName_mhnTools))
                return@setOnClickListener
            }

            downloadAPPWithCheck(mhnToolsUrl)
        }

        gpsDownloadButton.setOnClickListener {
            if (isOpenAppAction(gpsDownloadButton)) {
                openApp(resources.getString(R.string.packageName_gps64))
                return@setOnClickListener
            }

            downloadAPPWithCheck(url_joystick)
        }

        hylianerDownloadButton.setOnClickListener {
            if (isOpenAppAction(hylianerDownloadButton)) {
                openApp(resources.getString(R.string.packageName_hylianer))
                return@setOnClickListener
            }

            downloadAPPWithCheck("https://hylianer.net/")
        }

        mhnRemoveButton.setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_MHNow))
        }

        mhnToolsRemoveButton.setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_mhnTools))
        }

        gpsRemoveButton.setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_gps64))
        }

        hylianerRemoveButton.setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_hylianer))
        }

        view.findViewById<ImageButton>(R.id.mhn_more).setOnClickListener {
            popupMenu(view, R.id.mhn_more, resources.getString(R.string.packageName_MHNow))
        }

        view.findViewById<ImageButton>(R.id.gps_more).setOnClickListener {
            popupMenu(view, R.id.gps_more, resources.getString(R.string.packageName_gps64))
        }

        view.findViewById<ImageButton>(R.id.mhnTools_more).setOnClickListener {
            popupMenu(view, R.id.mhnTools_more, resources.getString(R.string.packageName_mhnTools))
        }

        view.findViewById<ImageButton>(R.id.hylianer_more).setOnClickListener {
            popupMenu(view, R.id.hylianer_more, resources.getString(R.string.packageName_hylianer))
        }

        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            .setOnRefreshListener {
                Toast.makeText(context, getString(R.string.refreshing), Toast.LENGTH_SHORT).show()
                setupAppVersionInfo(view)
                installerCard?.refresh(force = true)
            }
    }

    private fun setupCompatibilityCard(view: View) {
        view.findViewById<MaterialButton>(R.id.mhn_toggle_all_supported_versions)
            .setOnClickListener {
                allSupportedVersionsExpanded = !allSupportedVersionsExpanded
                applySupportedVersionsVisibility(view)
            }
        applySupportedVersionsVisibility(view)
        updateCompatibilitySummary(view)
    }

    private fun applySupportedVersionsVisibility(view: View) {
        view.findViewById<LinearLayout>(R.id.mhn_all_supported_versions).visibility =
            if (allSupportedVersionsExpanded) View.VISIBLE else View.GONE
        view.findViewById<MaterialButton>(R.id.mhn_toggle_all_supported_versions).apply {
            setText(if (allSupportedVersionsExpanded) R.string.compatibility_collapse_all_versions
                else R.string.compatibility_show_all_versions)
            setIconResource(if (allSupportedVersionsExpanded) R.drawable.baseline_expand_less_24
                else R.drawable.baseline_expand_more_24)
        }
    }

    private fun updateCompatibilitySummary(view: View) {
        val rows = view.findViewById<LinearLayout>(R.id.mhn_compatibility_status_rows)
        MhnToolsChannel.entries.forEachIndexed { index, channel ->
            val row = rows.getChildAt(index)
            val icon = row.findViewById<TextView>(R.id.compatibility_status_icon)
            val statusText = row.findViewById<TextView>(R.id.compatibility_status_text)
            row.findViewById<TextView>(R.id.compatibility_tool_name).setText(
                if (channel == MhnToolsChannel.TEST) R.string.compatibility_tool_mhntools_test
                else R.string.compatibility_tool_mhntools
            )
            val versions = mhnToolsResults[channel]?.supportedVersions
            val (iconRes, label, colorAttr) = when (
                val status = getCompatibilityStatus(mhnVersion, versions)
            ) {
                CompatibilityStatus.Supported -> Triple(
                    R.string.compatibility_icon_supported, getString(R.string.compatibility_supported),
                    androidx.appcompat.R.attr.colorPrimary
                )
                CompatibilityStatus.Unsupported -> Triple(
                    R.string.compatibility_icon_unsupported, getString(R.string.compatibility_unsupported),
                    androidx.appcompat.R.attr.colorError
                )
                is CompatibilityStatus.UnsupportedWithLatestSupportedVersion -> Triple(
                    R.string.compatibility_icon_unsupported,
                    getString(R.string.compatibility_latest_supported, status.version),
                    androidx.appcompat.R.attr.colorError
                )
                CompatibilityStatus.Unknown -> Triple(
                    R.string.compatibility_icon_unknown, getString(R.string.compatibility_unknown),
                    com.google.android.material.R.attr.colorTertiary
                )
            }
            val color = MaterialColors.getColor(row, colorAttr)
            icon.setText(iconRes)
            icon.setTextColor(color)
            statusText.text = label
            statusText.setTextColor(color)
            val versionsText = versions?.let {
                if (it.isEmpty()) getString(R.string.mhn_supported_versions_empty)
                else it.joinToString("、")
            } ?: getString(R.string.compatibility_unknown)
            view.findViewById<TextView>(
                if (channel == MhnToolsChannel.TEST) R.id.supportVersion_MHNTools_test
                else R.id.supportVersion_MHNTools
            ).text = versionsText
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupAppVersionInfo(view: View) {
        versionCheckJob?.cancel()
        val checkJob = SupervisorJob(viewLifecycleOwner.lifecycleScope.coroutineContext[Job])
        versionCheckJob = checkJob
        val checkScope = CoroutineScope(viewLifecycleOwner.lifecycleScope.coroutineContext + checkJob)
        fun isCurrentCheck() = checkJob.isActive && versionCheckJob === checkJob
        mhnToolsResults.clear()
        pendingMhnTools.clear()
        pendingMhnTools.addAll(MhnToolsChannel.entries)
        failedMhnTools.clear()
        appListCheckDone = false
        appListCheckFailed = false
        gameVersionsMap.clear()
        mhnUrl = ""
        val formatInstallVersion = resources.getString(R.string.version_label_installed)

        val mhnSupportVersion = view.findViewById<TextView>(R.id.mhn_new_version)
        val mhnToolsSupportVersion = view.findViewById<TextView>(R.id.mhnTools_new_version)
        val hylianerSupportVersion = view.findViewById<TextView>(R.id.hylianer_new_version)

        val versionSelector = view.findViewById<MaterialAutoCompleteTextView>(R.id.mhn_spinner)
        // A previous round's popup must not select a version missing from the new URL map.
        versionSelector.dismissDropDown()
        versionSelector.setAdapter(ArrayAdapter<String>(view.context, android.R.layout.simple_list_item_1, emptyList()))
        versionSelector.isEnabled = false
        versionSelector.setText(
            mhnVersion.takeIf { it.isNotBlank() && it != getString(R.string.notInstalled) }.orEmpty(),
            false
        )
        var displayedUpdateCount = 0

        fun remoteVersionValue(version: String): String =
            version.takeIf { it.isNotBlank() && it != getString(R.string.notInstalled) }
                ?: getString(R.string.version_value_unknown)

        fun updateToolbarSubtitle() {
            val pendingCount = (if (appListCheckDone) 0 else 1) + pendingMhnTools.size
            val failedCount = failedMhnTools.size + if (appListCheckFailed) 1 else 0
            view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(
                R.id.swipeRefreshLayout
            ).isRefreshing = pendingCount > 0
            val subtitle = when (val summary = getVersionCheckSummary(pendingCount, failedCount, displayedUpdateCount)) {
                is VersionCheckSummary.Loading -> getString(
                    if (summary.failedCount > 0) R.string.version_summary_loading_with_failures
                    else R.string.version_summary_loading, summary.pendingCount
                )
                is VersionCheckSummary.UpdatesAvailable -> getString(R.string.format_installApps, summary.count)
                is VersionCheckSummary.PartialFailure -> getString(R.string.version_summary_partial_failure)
                VersionCheckSummary.Complete -> getString(R.string.version_summary_complete)
            }
            view.findViewById<com.google.android.material.appbar.SubtitleCollapsingToolbarLayout>(
                R.id.toolbar_layout
            ).subtitle = subtitle
        }

        // These are display values only; comparison and download sentinels stay unchanged.
        mhnSupportVersion.text = getString(R.string.version_label_target, remoteVersionValue(mhnVersion))
        mhnToolsSupportVersion.text = getString(R.string.version_label_latest, remoteVersionValue(mhnToolsVersion))
        hylianerSupportVersion.text = getString(R.string.version_label_latest, getString(R.string.version_value_unknown))

        fun checkAppVersion(showDialogs: Boolean = true) {
            val selectedChannel = if (mhnTestVersion) MhnToolsChannel.TEST else MhnToolsChannel.RELEASE
            val selectedInfo = mhnToolsResults[selectedChannel]
            mhnToolsVersion = selectedInfo?.appVersion ?: getString(R.string.notInstalled)
            mhnToolsUrl = selectedInfo?.downloadUrl.orEmpty()
            val remoteToolVersion = when {
                selectedChannel in pendingMhnTools -> getString(R.string.version_value_loading)
                selectedChannel in failedMhnTools -> getString(R.string.version_value_failed)
                else -> remoteVersionValue(mhnToolsVersion)
            }
            mhnToolsSupportVersion.text = getString(
                R.string.version_label_latest_other, remoteToolVersion,
                if (mhnTestVersion) " (${getString(R.string.testVersion)})" else ""
            )
            mhnSupportVersion.text = getString(R.string.version_label_target, remoteVersionValue(mhnVersion))
            var needUpdateAppsAmount = 0

            val mhnPackageName = resources.getString(R.string.packageName_MHNow)
            val mhnToolsPackageName = resources.getString(R.string.packageName_mhnTools)
            val gps64PackageName = resources.getString(R.string.packageName_gps64)
            val hylianerPackageName = resources.getString(R.string.packageName_hylianer)

            val mhnInstallVersion = view.findViewById<TextView>(R.id.mhn_install_version)
            val mhnToolsInstallVersion = view.findViewById<TextView>(R.id.mhnTools_install_version)
            val gpsInstallVersion = view.findViewById<TextView>(R.id.gps_install_version)
            val hylianerInstallVersion = view.findViewById<TextView>(R.id.hylianer_install_version)

            val mhnRemoveButton = view.findViewById<Button>(R.id.remove_mhn)
            val mhnToolsRemoveButton = view.findViewById<Button>(R.id.remove_mhnTools)
            val gpsRemoveButton = view.findViewById<Button>(R.id.remove_gps)
            val hylianerRemoveButton = view.findViewById<Button>(R.id.remove_hylianer)

            val mhnDownloadButton = view.findViewById<Button>(R.id.download_mhn)
            val mhnToolsDownloadButton = view.findViewById<Button>(R.id.download_mhnTools)
            val gpsDownloadButton = view.findViewById<Button>(R.id.download_gps)
            val hylianerDownloadButton = view.findViewById<Button>(R.id.download_hylianer)

            val download = resources.getString(R.string.download)
            val update = resources.getString(R.string.update)

            mhnRemoveButton.visibility =
                if (appInstalledVersion(mhnPackageName) == "未安裝") View.GONE else View.VISIBLE
            mhnToolsRemoveButton.visibility =
                if (appInstalledVersion(mhnToolsPackageName) == "未安裝") View.GONE else View.VISIBLE
            gpsRemoveButton.visibility =
                if (appInstalledVersion(gps64PackageName) == "未安裝") View.GONE else View.VISIBLE
            hylianerRemoveButton.visibility =
                if (appInstalledVersion(hylianerPackageName) == "未安裝") View.GONE else View.VISIBLE

            // Keep overflow actions consistent with the same local installation check.
            view.findViewById<ImageButton>(R.id.mhn_more).visibility = mhnRemoveButton.visibility
            view.findViewById<ImageButton>(R.id.mhnTools_more).visibility = mhnToolsRemoveButton.visibility
            view.findViewById<ImageButton>(R.id.gps_more).visibility = gpsRemoveButton.visibility
            view.findViewById<ImageButton>(R.id.hylianer_more).visibility = hylianerRemoveButton.visibility

            mhnInstallVersion.text =
                String.format(formatInstallVersion, appInstalledVersion(mhnPackageName))
            mhnToolsInstallVersion.text =
                String.format(formatInstallVersion, appInstalledVersion(mhnToolsPackageName))
            gpsInstallVersion.text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(gps64PackageName)
                )
            hylianerInstallVersion.text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(hylianerPackageName)
                )

            fun setMhnDownloadButton(isUpdate: Boolean = false, isInstalled: Boolean = false) {
                when {
                    isUpdate -> setDownloadAction(mhnDownloadButton, update)
                    isInstalled -> setOpenAppAction(mhnDownloadButton)
                    else -> setDownloadAction(mhnDownloadButton, download)
                }

                if (!mhnDownloadButton.isEnabled)
                    mhnDownloadButton.isEnabled = true
            }

            if (appInstalledOrNot(gps64PackageName)) {
                setOpenAppAction(gpsDownloadButton)
            } else {
                setDownloadAction(gpsDownloadButton, download)
            }

            if (appInstalledOrNot(hylianerPackageName)) {
                setOpenAppAction(hylianerDownloadButton)
            } else {
                setDownloadAction(hylianerDownloadButton, download)
            }

            val mhnInstalledVersion = appInstalledVersion(mhnPackageName)
            val mhnToolsInstalledVersion = appInstalledVersion(mhnToolsPackageName)

            if (mhnVersion != "未安裝" && mhnInstalledVersion != "未安裝") {
                val mhnVersionInt: List<String> = mhnVersion.split(".")
                val mhnInstalledVersionInt: List<String> = mhnInstalledVersion.split(".")
                var needUpdate = false
                var needDowngrade = false

                for (i in mhnVersionInt.indices) {
                    val currentVersion = mhnVersionInt[i].toInt()
                    val installedVersion = mhnInstalledVersionInt[i].toInt()

                    if (currentVersion > installedVersion) {
                        needUpdate = true
                        break
                    } else if (currentVersion < installedVersion) {
                        needDowngrade = true
                        break
                    }
                }

                when {
                    needDowngrade -> {
                        mhnInstallVersion.text =
                            "${mhnInstallVersion.text} ${resources.getString(R.string.versionTooHigh)}"
                        if (mhnDownloadButton.isEnabled)
                            mhnDownloadButton.isEnabled = false

                        if (showDialogs && isResumed && !isHidden) MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.dialogVersionTooHighTitle))
                            .setMessage(resources.getString(R.string.dialogVersionTooHighMessage))
                            .setNeutralButton(R.string.ok) { _, _ -> }
                            .setNegativeButton("如何降版") { _, _ ->
                                showAlertDialog(
                                    resources.getString(R.string.dialogVersionTooHighTitle),
                                    "1. 請先移除較新版本的魔物獵人 \n2. 重新下載支援版本寶可夢 \n3. 重啟手機嘗試啟動"
                                )
                            }
                            .setPositiveButton("使用測試版") { _, _ ->
                                PreferenceManager.getDefaultSharedPreferences(requireContext())
                                    .edit().putBoolean("mhntools_testversion", true).apply()
                                mhnTestVersion = true
                                refreshLocalAppState?.invoke()
                            }
                            .show()
                    }

                    needUpdate -> {
                        setMhnDownloadButton(isUpdate = true)
                        needUpdateAppsAmount++
                        if (showDialogs && isResumed && !isHidden) {
                            showAlertDialog(
                                resources.getString(R.string.dialogUpdateAvailableTitle),
                                resources.getString(R.string.dialogUpdateAvailablePokMessage)
                            )
                        }
                    }

                    else -> {
                        setMhnDownloadButton(isInstalled = true)
                    }
                }
            } else {
                setMhnDownloadButton()
            }

            if (mhnToolsVersion != "未安裝" && mhnToolsInstalledVersion != "未安裝") {
                val pgToolsVersionInt: List<String> = mhnToolsVersion.split(".")
                val pgToolsInstalledVersionInt: List<String> = mhnToolsInstalledVersion.split(".")
                var needUpdate = false

                for (i in pgToolsVersionInt.indices) {
                    val currentVersion = pgToolsVersionInt[i].toInt()
                    val installedVersion = pgToolsInstalledVersionInt[i].toInt()

                    if (currentVersion > installedVersion) {
                        needUpdate = true
                        break
                    }
                }

                if (needUpdate) {
                    setDownloadAction(mhnToolsDownloadButton, update)
                    needUpdateAppsAmount++
                } else {
                    setOpenAppAction(mhnToolsDownloadButton)
                }
            } else if (mhnToolsInstalledVersion != getString(R.string.notInstalled)) {
                // Remote metadata can be pending or unavailable without blocking an installed app.
                setOpenAppAction(mhnToolsDownloadButton)
            } else {
                setDownloadAction(mhnToolsDownloadButton, download)
            }

            mhnToolsDownloadButton.isEnabled =
                isOpenAppAction(mhnToolsDownloadButton) || mhnToolsUrl.isNotBlank()
            if (!isOpenAppAction(mhnDownloadButton) && mhnUrl.isBlank()) {
                mhnDownloadButton.isEnabled = false
            }
            displayedUpdateCount = needUpdateAppsAmount
            updateCompatibilitySummary(view)
            updateToolbarSubtitle()
        }

        fun updateGameVersionChoices() {
            // Build the union once per source result, never while rendering individual rows.
            val versionsList = mergeMhnSupportedVersions(
                mhnToolsResults[MhnToolsChannel.RELEASE]?.supportedVersions,
                mhnToolsResults[MhnToolsChannel.TEST]?.supportedVersions
            )
            gameVersionsMap.clear()
            versionsList.forEach { version -> gameVersionsMap[version] = buildMHNDownloadUrl(version) }
            mhnVersion = selectMhnTargetVersionDuringRefresh(
                versionsList, mhnVersion, pendingMhnTools.isEmpty()
            )
            mhnUrl = gameVersionsMap[mhnVersion].orEmpty()
            versionSelector.setAdapter(ArrayAdapter(view.context, android.R.layout.simple_list_item_1, versionsList))
            versionSelector.isEnabled = versionsList.isNotEmpty()
            versionSelector.setText(
                mhnVersion.takeIf { it != getString(R.string.notInstalled) }.orEmpty(), false
            )
        }

        fun setupVersionSelectionListener() {
            versionSelector.setOnItemClickListener { parent, _, position, _ ->
                val version = parent.getItemAtPosition(position).toString()
                val arm64Url = gameVersionsMap[version]

                if (arm64Url != null) {
                    mhnUrl = arm64Url
                    mhnVersion = version
                    mhnSupportVersion.text = getString(R.string.version_label_target, version)
                }

                updateCompatibilitySummary(view)
                val visible = isResumed && !isHidden
                checkAppVersion(showDialogs = visible)
            }
        }

        refreshLocalAppState = {
            checkAppVersion(showDialogs = false)
        }
        checkAppVersion()
        setupVersionSelectionListener()

        MhnToolsChannel.entries.forEach { channel ->
            val url = getString(
                if (channel == MhnToolsChannel.TEST) R.string.url_mhnJsonTest else R.string.url_mhnJson
            )
            checkScope.launch {
                Log.d("mhnTools", "Checking $channel versions")
                val result = try {
                    Result.success(parseMhnToolsVersionInfo(readVersionJson(url), channel))
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w("mhnTools", "Version check failed for $channel", error)
                    Result.failure(error)
                }
                if (!isCurrentCheck()) return@launch
                pendingMhnTools.remove(channel)
                result.onSuccess { mhnToolsResults[channel] = it }
                    .onFailure { failedMhnTools.add(channel) }
                updateGameVersionChoices()
                checkAppVersion(showDialogs = false)
            }
        }
        checkScope.launch {
            val result = try {
                val json = JSONObject(readVersionJson(getString(R.string.url_appInfo)))
                Result.success(json.getJSONObject("mhn").getJSONObject("joystick").getString("url"))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("mhnTools", "App list check failed", error)
                Result.failure(error)
            }
            if (!isCurrentCheck()) return@launch
            appListCheckDone = true
            appListCheckFailed = result.isFailure
            result.onSuccess { url_joystick = it }
            checkAppVersion(showDialogs = false)
        }
    }

    private suspend fun readVersionJson(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.inputStream.bufferedReader().use { it.readText() }.also {
                coroutineContext.ensureActive()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildMHNDownloadUrl(gameVersionName: String): String {
        Log.i("mhnTools", "mhn_download_on_apkmirror: $mhn_download_on_apkmirror")
        if(!mhn_download_on_apkmirror) return "https://assets.mhntools.net/games/$gameVersionName-arm64.apkm"

        val normalizedVersion = gameVersionName.trim().replace('.', '-')
        return "https://www.apkmirror.com/apk/scopely-explore-inc/monster-hunter-now/" +
            "monster-hunter-now-$normalizedVersion-release/" +
            "monster-hunter-now-$normalizedVersion-android-apk-download/"
    }

    private fun showAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(R.string.ok) { _, _ -> }
            .show()
    }

    private fun setDownloadAction(button: Button, text: String) {
        button.text = text
        button.tag = null
    }

    private fun setOpenAppAction(button: Button) {
        button.text = getString(R.string.popup_menu_startApp)
        button.tag = DOWNLOAD_BUTTON_ACTION_OPEN_APP
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
                resources.getString(R.string.dialogAdNotReadyTitle),
                resources.getString(R.string.dialogAdNotReadyMessage)
            )
            return
        }

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val allowDownloadOnNonArm64 =
            sharedPreferences.getBoolean("allow_download_on_non_arm64", false)

        if (Build.SUPPORTED_ABIS[0] != "arm64-v8a" && !allowDownloadOnNonArm64) {
            Snackbar.make(
                requireView(),
                "${resources.getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})",
                Snackbar.LENGTH_LONG
            ).setAction("Action", null).show()
            return
        }

        val factory = LayoutInflater.from(requireContext())
        val imageView: View = factory.inflate(R.layout.dialog_imageview, null)
        var setview = false

        if (url.contains("mediafire")) {
            imageView.findViewById<ImageView>(R.id.dialog_imageview)
                .setImageResource(R.drawable.download_mediafire)
            setview = true
        } else if (url.contains("apkmirror") || url.contains("bit.ly")) {
            imageView.findViewById<ImageView>(R.id.dialog_imageview)
                .setImageResource(R.drawable.download_apk_e)
            setview = true
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(if (setview) imageView else null)
            .setTitle(resources.getString(R.string.dialogDownloadTitle))
            .setMessage(resources.getString(R.string.dialogDownloadMessage))
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
            resources.getString(R.string.installed)
        else
            resources.getString(R.string.notInstalled)
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
        return resources.getString(R.string.notInstalled)
    }

    private fun appUnInstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = "package:$packageName".toUri()
        startActivity(intent)
    }
}
