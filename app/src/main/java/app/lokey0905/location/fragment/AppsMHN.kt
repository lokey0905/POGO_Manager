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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.version.VersionCheckSummary
import app.lokey0905.location.version.getVersionCheckSummary
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

class AppsMHN : Fragment() {
    private companion object {
        const val DOWNLOAD_BUTTON_ACTION_OPEN_APP = "open_app"
        const val STATE_SELECTED_MHN_VERSION = "selected_mhn_version"
        const val STATE_MHN_TEST_VERSION = "mhn_test_version"
    }

    private val gameVersionsMap = mutableMapOf<String, String>()
    private var mhnToolsVersion: String = "未安裝"
    private var mhnToolsUrl: String = ""
    private var mhnToolsHash = ""
    private var mhnUrl: String = ""
    private var mhnVersion: String = "未安裝"

    private var mhn_download_on_apkmirror = false
    private var mhnTestVersion = false

    private var url_joystick = ""

    private var mhnToolsCheckDone = false
    private var appListCheckDone = false
    private var needsRemoteRefresh = true
    private var refreshLocalAppState: (() -> Unit)? = null
    private var updatingVersionControls = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_mhn, container, false)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val preferredTestVersion = sharedPreferences.getBoolean("mhntools_testversion", false)
        mhnTestVersion = savedInstanceState?.getBoolean(STATE_MHN_TEST_VERSION, preferredTestVersion)
            ?: preferredTestVersion
        savedInstanceState?.getString(STATE_SELECTED_MHN_VERSION)
            ?.takeIf { it.isNotBlank() }
            ?.let { mhnVersion = it }
        mhn_download_on_apkmirror = sharedPreferences.getBoolean("mhn_download_on_apkmirror", false)

        view.findViewById<MaterialSwitch>(R.id.mhnTestVersion_switch).isChecked = mhnTestVersion
        setupListeners(view)

        MobileAds.initialize(requireActivity())
        val mAdView = view.findViewById<AdView>(R.id.ad_banner)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                updatingVersionControls = true
                view.findViewById<MaterialSwitch>(R.id.mhnTestVersion_switch).isChecked = useTestVersion
                updatingVersionControls = false
                needsRemoteRefresh = true
                refreshVersionInfoIfNeeded()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_MHN_VERSION, mhnVersion)
        outState.putBoolean(STATE_MHN_TEST_VERSION, mhnTestVersion)
    }

    override fun onResume() {
        super.onResume()
        refreshVersionInfoIfNeeded()
    }

    override fun onStop() {
        // A hidden tab stays STARTED; only leaving the Activity requests a new remote check.
        needsRemoteRefresh = true
        super.onStop()
    }

    override fun onDestroyView() {
        refreshLocalAppState = null
        needsRemoteRefresh = true
        updatingVersionControls = false
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
        view.findViewById<MaterialSwitch>(R.id.mhnTestVersion_switch)

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
            }
    }

    @SuppressLint("SetTextI18n")
    private fun setupAppVersionInfo(view: View) {
        val formatInstallVersion = resources.getString(R.string.version_label_installed)
        val formatNewerVersionOther = resources.getString(R.string.version_label_latest_other)
        val formatTargetVersionOther = resources.getString(R.string.version_label_target_other)
        val formatNewerVersion = resources.getString(R.string.version_label_latest)

        val mhnSupportVersion = view.findViewById<TextView>(R.id.mhn_new_version)
        val mhnToolsSupportVersion = view.findViewById<TextView>(R.id.mhnTools_new_version)
        val hylianerSupportVersion = view.findViewById<TextView>(R.id.hylianer_new_version)

        val spinner = view.findViewById<Spinner>(R.id.mhn_spinner)
        var displayedUpdateCount = 0

        fun remoteVersionValue(version: String): String =
            version.takeIf { it.isNotBlank() && it != getString(R.string.notInstalled) }
                ?: getString(R.string.version_value_unknown)

        fun updateToolbarSubtitle() {
            val pendingCount = (if (appListCheckDone) 0 else 1) + (if (mhnToolsCheckDone) 0 else 1)
            val subtitle = when (val summary = getVersionCheckSummary(pendingCount, 0, displayedUpdateCount)) {
                is VersionCheckSummary.Loading -> getString(R.string.version_summary_loading, summary.pendingCount)
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

        fun checkAppVersion(fetchRemoteVersions: Boolean = true, showDialogs: Boolean = true) {
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

            if (fetchRemoteVersions) {
                appListCheckDone = false
                val url = resources.getString(R.string.url_appInfo)
                extractAppVersionsFromJson(url) {
                    appListCheckDone = true
                    updateToolbarSubtitle()
                    if (mhnToolsCheckDone)
                        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout).isRefreshing =
                            false
                }
            }

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
            val mhnTestVersionSwitch = view.findViewById<MaterialSwitch>(R.id.mhnTestVersion_switch)

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
                                mhnTestVersionSwitch.isChecked = true
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
            } else {
                setDownloadAction(mhnToolsDownloadButton, download)
            }

            displayedUpdateCount = needUpdateAppsAmount
            updateToolbarSubtitle()
        }

        fun getMHNToolsVersion() {
            mhnToolsCheckDone = false
            updateToolbarSubtitle()
            var url = resources.getString(R.string.url_mhnJson)
            if (mhnTestVersion)
                url = resources.getString(R.string.url_mhnJsonTest)
            val versionType =
                if (mhnTestVersion) " (${resources.getString(R.string.testVersion)})" else ""

            extractMHNToolsFromJson(url) { latestMhnVersion, mhnToolsVersion, gameVersionsMap ->
                val versionsList = ArrayList<String>()
                var mhnVersionList =
                    resources.getString(R.string.appsMHNPage_supportVersion_MHNTools)

                for ((version, _) in gameVersionsMap) {
                    versionsList.add(version)
                    Log.i(
                        "mhnTools",
                        "MHN支援版本: $version\n"
                    )
                    mhnVersionList += " $version,"
                }

                mhnVersionList = if (mhnTestVersion) {
                    mhnVersionList.substring(
                        0,
                        mhnVersionList.length - 1
                    ) + " (${getText(R.string.testVersion)})"

                } else {
                    mhnVersionList.substring(0, mhnVersionList.length - 1)
                }

                view.findViewById<TextView>(R.id.supportVersion_MHNTools).text = mhnVersionList

                //versionsList.reverse() //do not need reverse again

                val adapter: ArrayAdapter<String> = ArrayAdapter(
                    view.context,
                    android.R.layout.simple_spinner_item,
                    versionsList
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                val selectedIndex = versionsList.indexOf(mhnVersion).coerceAtLeast(0)
                mhnVersion = versionsList.getOrNull(selectedIndex) ?: latestMhnVersion
                mhnUrl = gameVersionsMap[mhnVersion].orEmpty()
                spinner.adapter = adapter
                spinner.setSelection(selectedIndex)

                mhnSupportVersion.text = String.format(
                    formatTargetVersionOther,
                    remoteVersionValue(mhnVersion),
                    versionType
                )
                mhnToolsSupportVersion.text = String.format(
                    formatNewerVersionOther,
                    remoteVersionValue(mhnToolsVersion),
                    versionType
                )
                hylianerSupportVersion.text = String.format(
                    formatNewerVersion,
                    getString(R.string.version_value_unknown),
                )
                checkAppVersion(fetchRemoteVersions = false, showDialogs = false)

                mhnToolsCheckDone = true
                updateToolbarSubtitle()
                if (appListCheckDone)
                    view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout).isRefreshing =
                        false
            }
        }

        fun setOnCheckedChangeListener() {
            val mhnTestVersionSwitch = view.findViewById<MaterialSwitch>(R.id.mhnTestVersion_switch)

            mhnTestVersionSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (updatingVersionControls) return@setOnCheckedChangeListener
                if (isHidden || !isResumed) {
                    mhnTestVersion = isChecked
                    needsRemoteRefresh = true
                    return@setOnCheckedChangeListener
                }
                if (isChecked) {
                    mhnTestVersion = true
                    Snackbar.make(
                        view,
                        "已切換至測試版",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show()
                } else {
                    mhnTestVersion = false
                    Snackbar.make(
                        view,
                        "已切換至正式版",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show()
                }
                getMHNToolsVersion()
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val version = parent.getItemAtPosition(position).toString()
                    val arm64Url = gameVersionsMap[version]

                    if (arm64Url != null) {
                        mhnUrl = arm64Url
                        mhnVersion = version
                        mhnSupportVersion.text = String.format(
                            formatTargetVersionOther,
                            version,
                            if (mhnTestVersion)
                                "(${getText(R.string.testVersion)})"
                            else
                                ""
                        )
                    }

                    val visible = isResumed && !isHidden
                    checkAppVersion(fetchRemoteVersions = visible, showDialogs = visible)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        refreshLocalAppState = {
            checkAppVersion(fetchRemoteVersions = false, showDialogs = false)
        }
        checkAppVersion()
        getMHNToolsVersion()
        setOnCheckedChangeListener()
    }

    private fun extractAppVersionsFromJson(url: String, onAppVersionsExtracted: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            try {
                val urlObject = URL(url)
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()

                var line: String? = bufferedReader.readLine()
                while (line != null) {
                    response.append(line)
                    line = bufferedReader.readLine()
                }

                bufferedReader.close()
                coroutineContext.ensureActive()

                val jsonObject = JSONObject(response.toString())

                val mhn = jsonObject.getJSONObject("mhn")
                val joystick = mhn.getJSONObject("joystick")


                url_joystick = joystick.getString("url")

                launch(Dispatchers.Main) {
                    onAppVersionsExtracted()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractMHNToolsFromJson(
        url: String,
        onPogoVersionExtracted: (String, String, MutableMap<String, String>) -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            try {
                val urlObject = URL(url)
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()

                var line: String? = bufferedReader.readLine()
                while (line != null) {
                    response.append(line)
                    line = bufferedReader.readLine()
                }

                bufferedReader.close()
                coroutineContext.ensureActive()

                val jsonObject = JSONObject(response.toString())
                val supportedVersions = jsonObject.optJSONObject("supportedVersions")

                if (supportedVersions == null) {
                    Log.e("mhnTools", "supportedVersions not found in response")
                    launch(Dispatchers.Main) {
                        onPogoVersionExtracted(mhnVersion, mhnToolsVersion, gameVersionsMap)
                    }
                    return@launch
                }

                val versionKeys = mutableListOf<String>()
                val iterator = supportedVersions.keys()
                while (iterator.hasNext()) {
                    versionKeys.add(iterator.next())
                }

                if (versionKeys.isEmpty()) {
                    Log.e("mhnTools", "No version entries inside supportedVersions")
                    launch(Dispatchers.Main) {
                        onPogoVersionExtracted(mhnVersion, mhnToolsVersion, gameVersionsMap)
                    }
                    return@launch
                }

                versionKeys.sortWith { first, second -> compareVersions(second, first) }

                gameVersionsMap.clear()
                var latestAppVersionCode = -1
                var latestAppVersionName = ""
                var latestAppVersionHash = ""

                versionKeys.forEach { versionKey ->
                    val versionObject = supportedVersions.optJSONObject(versionKey) ?: return@forEach
                    val gameObject = versionObject.optJSONObject("game")
                    val gameVersionName = gameObject?.optString("gameVersionName", versionKey) ?: versionKey
                    val downloadUrl = buildMHNDownloadUrl(gameVersionName)
                    gameVersionsMap[gameVersionName] = downloadUrl

                    val appObject = versionObject.optJSONObject("app")
                    val appVersionCode = appObject?.optInt("appVersionCode", -1) ?: -1
                    val appVersionName = appObject?.optString("appVersionName").orEmpty()
                    val appVersionHash = appObject?.optString("appVersionHash").orEmpty()

                    if (appVersionCode > latestAppVersionCode) {
                        latestAppVersionCode = appVersionCode
                        latestAppVersionName = appVersionName
                        latestAppVersionHash = appVersionHash
                    }
                }

                val latestGameKey = versionKeys.first()
                val latestGameObject = supportedVersions.optJSONObject(latestGameKey)
                val latestGameVersionName = latestGameObject?.optJSONObject("game")
                    ?.optString("gameVersionName", latestGameKey) ?: latestGameKey

                mhnToolsVersion = if (latestAppVersionName.isNotBlank()) latestAppVersionName else "未安裝"
                mhnToolsHash = latestAppVersionHash
                mhnToolsUrl = if (mhnToolsVersion != "未安裝" && latestAppVersionHash.isNotBlank()) {
                    if (mhnTestVersion) {
                        "https://assets.mhntools.net/test-mhntools-$mhnToolsVersion-$mhnToolsHash.apk?"
                    } else {
                        "https://assets.mhntools.net/mhntools-$mhnToolsVersion-$mhnToolsHash.apk?"
                    }
                } else {
                    ""
                }

                versionKeys.forEach { versionKey ->
                    val urlForLog = gameVersionsMap[versionKey] ?: gameVersionsMap[latestGameVersionName]
                    Log.i("mhnTools", "mhnVersion: $versionKey\nmhnUrl: ${urlForLog ?: ""}\n")
                }

                launch(Dispatchers.Main) {
                    onPogoVersionExtracted(latestGameVersionName, mhnToolsVersion, gameVersionsMap)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun compareVersions(v1: String, v2: String): Int {
        val numericV1 = v1.split(" ")[0].split("-")[0]
        val numericV2 = v2.split(" ")[0].split("-")[0]

        val parts1 = numericV1.split(".")
        val parts2 = numericV2.split(".")
        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val p1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val p2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0
            if (p1 != p2) return p1 - p2
        }
        return 0
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
