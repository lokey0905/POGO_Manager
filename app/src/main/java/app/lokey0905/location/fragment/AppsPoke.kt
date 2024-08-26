package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.app.ActivityManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.api.polygon
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt


data class PogoVersionInfo(
    val pogoVersion: String,
    val pogoARM64: String,
    val pogoARM: String,
)

data class ButtonsIdData(
    val packageName: String,
    val removeButtonId: Int,
    val moreButtonId: Int
)

class AppsPoke : Fragment() {
    val pogoVersionsList = ArrayList<PogoVersionInfo>()
    private var polygonVersionsList = ArrayList<String>()
    private var aerilateVersionsList = ArrayList<String>()
    private var pogoVersion: String = "未安裝"
    private var pgToolsARMUrl: String = ""
    private var pgToolsARM64Url: String = ""
    private var pgToolsVersion: String = "未安裝"
    private var pgToolsUrl = ""

    private var url_jokstick = ""
    private var url_wrapper = ""
    private var url_aerilate = ""
    private var url_polygon = ""
    private var url_PokeList = ""
    private var url_WeCatch = ""
    private var url_samsungStore = ""

    private var version_wrapper = "未安裝"
    private var version_aerilate = "未安裝"
    private var version_polygon = "未安裝"
    private var version_PokeList = "未安裝"
    private var version_WeCatch = "未安裝"

    private var pgToolsTestVersion = false
    private var pokAresNoSupportDevices = false

    private var pgToolsCheckDone = false
    private var appListCheckDone = false

    private var totalMemory = 0
    private var errorCounter = 0

    private var polygonTestKey = ""
    private var polygonTestToken = ""

    private val buttonsIdData = listOf(
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_gps32),
            removeButtonId = R.id.remove_gps,
            moreButtonId = R.id.gps_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_polygon),
            removeButtonId = R.id.remove_polygon,
            moreButtonId = R.id.polygon_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_PGTools),
            removeButtonId = R.id.remove_pgtools,
            moreButtonId = R.id.pgtools_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_pok),
            removeButtonId = R.id.remove_pok,
            moreButtonId = R.id.pok_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_pokAres),
            removeButtonId = R.id.remove_pokAres,
            moreButtonId = R.id.pokAres_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_PokeList),
            removeButtonId = R.id.remove_pokelist,
            moreButtonId = R.id.pokelist_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_WeCatch),
            removeButtonId = R.id.remove_wecatch,
            moreButtonId = R.id.wecatch_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_wrapper),
            removeButtonId = R.id.remove_wrapper,
            moreButtonId = R.id.wrapper_more
        ),
        ButtonsIdData(
            packageName = resources.getString(R.string.packageName_Aerilate),
            removeButtonId = R.id.remove_Aerilate,
            moreButtonId = R.id.Aerilate_more
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_poke, container, false)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        pokAresNoSupportDevices =
            sharedPreferences.getBoolean("allow_download_on_non_samsung", false)

        val actManager =
            requireActivity().getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        totalMemory = (memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)).roundToInt()

        view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility =
            viewShowOrHide(totalMemory > 4 || pokAresNoSupportDevices)

        setupListeners(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        val view: View = requireView()

        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout).isRefreshing =
            false

        fun setFragmentResultListener() {
            setFragmentResultListener("pokAresNoSupportDevices") { _, bundle ->
                pokAresNoSupportDevices = bundle.getBoolean("bundleKey")

                if (totalMemory > 4 || pokAresNoSupportDevices) {
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility =
                        viewShowOrHide(true)
                } else {
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility =
                        viewShowOrHide(false)
                }
            }
        }

        setupAppVersionInfo(view)
        setFragmentResultListener()
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
                    "${resources.getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})",
                    Snackbar.LENGTH_LONG
                ).setAction("Action", null).show()
        }

        view.findViewById<Button>(R.id.download_gps).setOnClickListener {
            downloadAPPWithCheck(url_jokstick)
        }

        view.findViewById<Button>(R.id.download_wrapper).setOnClickListener {
            downloadAPPWithCheck(url_wrapper)
        }

        view.findViewById<Button>(R.id.download_Aerilate).setOnClickListener {
            downloadAppCheckARM64(url_aerilate)
        }

        view.findViewById<Button>(R.id.download_polygon).setOnClickListener {
            downloadAppCheckARM64(url_polygon)
        }

        view.findViewById<Button>(R.id.download_pgtools).setOnClickListener {
            downloadAppCheckARM64(pgToolsUrl)
        }

        view.findViewById<Button>(R.id.download_pok).setOnClickListener {
            val url = if (Build.SUPPORTED_ABIS[0] == "arm64-v8a") pgToolsARM64Url else pgToolsARMUrl
            downloadAPPWithCheck(url)
        }

        view.findViewById<Button>(R.id.download_pokAres).setOnClickListener {
            if (Build.SUPPORTED_ABIS[0] != "arm64-v8a") {
                Snackbar.make(
                    view,
                    "${resources.getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})",
                    Snackbar.LENGTH_LONG
                ).setAction("Action", null).show()
                return@setOnClickListener
            }
            if (Build.MANUFACTURER == "samsung" || pokAresNoSupportDevices) {
                if (appInstalledVersion(resources.getString(R.string.packageName_galaxyStore)) == "未安裝") {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(resources.getString(R.string.dialogInstallSamsungStoreTitle))
                        .setMessage(resources.getString(R.string.dialogInstallSamsungStoreMessage))
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
                                downloadAPPWithCheck(url_samsungStore)
                            }
                        }
                        .show()
                } else {
                    downloadAPPWithCheck(
                        String.format(
                            resources.getString(R.string.url_pokAres),
                            pogoVersion.replace(".", "-")
                        )
                    )

                }
            } else
                showAlertDialog(
                    resources.getString(R.string.unsupportedDevices),
                    resources.getString(R.string.unsupportedDevicesPokeAres)
                )
        }

        view.findViewById<Button>(R.id.download_pokelist).setOnClickListener {
            downloadAPPWithCheck(url_PokeList)
        }

        view.findViewById<Button>(R.id.download_wecatch).setOnClickListener {
            downloadAPPWithCheck(url_WeCatch)
        }

        for (mapping in buttonsIdData) {
            val removeButton = view.findViewById<Button>(mapping.removeButtonId)
            val moreButton = view.findViewById<ImageButton>(mapping.moreButtonId)

            removeButton?.setOnClickListener {
                appUnInstall(mapping.packageName)
            }

            moreButton?.setOnClickListener {
                popupMenu(view, mapping.moreButtonId, mapping.packageName)
            }
        }


        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            .setOnRefreshListener {
                Toast.makeText(context, getString(R.string.refreshing), Toast.LENGTH_SHORT).show()
                pgToolsCheckDone = false
                appListCheckDone = false
                setupAppVersionInfo(view)
            }
    }

    private fun setupAppVersionInfo(view: View) {
        val formatInstallVersion: String = resources.getString(R.string.format_installVersion)
        val formatInstallVersionOther: String =
            resources.getString(R.string.format_installVersion_other)
        val formatNewerVersion: String = resources.getString(R.string.format_newerVersion)
        val formatNewerVersionOther: String =
            resources.getString(R.string.format_newerVersion_other)

        val pokePackageName = resources.getString(R.string.packageName_pok)
        val pokeAresPackageName = resources.getString(R.string.packageName_pokAres)
        val pgToolsPackageName = resources.getString(R.string.packageName_PGTools)

        val pokeSupportVersion = view.findViewById<TextView>(R.id.pok_new_version)
        val pokeAresSupportVersion = view.findViewById<TextView>(R.id.pokAres_new_version)
        val pgToolSupportVersion = view.findViewById<TextView>(R.id.pgtools_new_version)

        val pokeDownloadButton = view.findViewById<Button>(R.id.download_pok)
        val pokeAresDownloadButton = view.findViewById<Button>(R.id.download_pokAres)
        val pgToolsDownloadButton = view.findViewById<Button>(R.id.download_pgtools)
        val polygonDownloadButton = view.findViewById<Button>(R.id.download_polygon)
        val wrapperDownloadButton = view.findViewById<Button>(R.id.download_wrapper)
        val pokeListDownloadButton = view.findViewById<Button>(R.id.download_pokelist)
        val weCatchDownloadButton = view.findViewById<Button>(R.id.download_wecatch)
        val aerilateDownloadButton = view.findViewById<Button>(R.id.download_Aerilate)

        val pokeTestVersionSwitch = view.findViewById<MaterialSwitch>(R.id.pokeTestVersion_switch)
        val spinner = view.findViewById<Spinner>(R.id.poke_spinner)

        fun getPolygonVersion() {
            if (polygonTestKey == "") {
                Log.i("Polygon", "polygonTestKey is empty")
                return
            }

            val polygon = polygon()
            val versionNumber = 26

            polygon.Polygon(requireContext())

            if (polygonTestToken == "") {
                Log.i("Polygon", "Get polygonTestToken")

                polygon.checkPogoVersion(polygonTestKey, versionNumber) { token ->
                    if (token == "ERROR") {
                        if (errorCounter > 3) {
                            Log.i("Polygon", "Try Login too many times")
                            return@checkPogoVersion
                        }

                        Log.i("Polygon", "Try Login again $errorCounter")
                        errorCounter++
                        getPolygonVersion()
                    } else {
                        polygonTestToken = token
                        getPolygonVersion()
                    }
                }
                return

            } else {
                polygonVersionsList.clear()

                Log.i(
                    "Polygon",
                    "polygonTestKey: $polygonTestKey\n" +
                            "polygonTestToken: $polygonTestToken"
                )

                for (pogo in pogoVersionsList) {
                    polygon.sendSecondJsonRequest(
                        polygonTestKey,
                        polygonTestToken,
                        pogo.pogoVersion,
                        versionNumber
                    ) { it ->
                        if (polygonVersionsList.contains(it))
                            return@sendSecondJsonRequest

                        if (it == "ERROR") {
                            polygonTestToken = ""

                            if (errorCounter > 3) {
                                Log.i("Polygon", "Try Login too many times")
                                return@sendSecondJsonRequest
                            }
                            Log.i("Polygon", "取得支援版本失敗 $errorCounter")
                            errorCounter++
                            getPolygonVersion()
                            return@sendSecondJsonRequest
                        }

                        polygonVersionsList.add(it)
                        polygonVersionsList.sort()

                        var pogoVersionList =
                            resources.getString(R.string.appsPokePage_supportVersion_polygon)
                        var matchingVersionInfo: PogoVersionInfo? = null

                        for (polygonSupportedVersion in polygonVersionsList) {
                            pogoVersionList += " ${polygonSupportedVersion},"

                            matchingVersionInfo = pogoVersionsList.find { it.pogoVersion == polygonSupportedVersion }
                        }

                        matchingVersionInfo?.let { versionInfo ->
                            val selectionIndex = pogoVersionsList.size - 1 - pogoVersionsList.indexOf(versionInfo)
                            spinner.post {
                                spinner.setSelection(selectionIndex)
                                Log.i("Polygon", "spinner.setSelection: $selectionIndex")
                            }
                        }

                        Log.i("Polygon", "polygonVersionsList: $polygonVersionsList")

                        pogoVersionList = pogoVersionList.substring(0, pogoVersionList.length - 1)

                        view.findViewById<TextView>(R.id.supportVersion_polygon)?.text =
                            pogoVersionList

                        errorCounter = 0
                    }
                }
            }
        }

        fun getAerilateVersion() {
            for (versionInfo in pogoVersionsList) {
                val url = String.format(resources.getString(R.string.url_AerilateAPI), versionInfo.pogoVersion)
                checkAerilate(url) { unsupportedVersion ->
                    if (!unsupportedVersion) {
                        Log.i("Aerilate", "Aerilate支援版本: ${versionInfo.pogoVersion}")
                        if (aerilateVersionsList.contains(versionInfo.pogoVersion))
                            return@checkAerilate

                        aerilateVersionsList.add(versionInfo.pogoVersion)
                        aerilateVersionsList.sort()

                        var pogoVersionList =
                            resources.getString(R.string.appsPokePage_supportVersion_Aerilate)
                        for (aerilateSupportedVersion in aerilateVersionsList) {
                            pogoVersionList += " ${aerilateSupportedVersion},"
                        }

                        pogoVersionList = pogoVersionList.substring(0, pogoVersionList.length - 1)

                        view.findViewById<TextView>(R.id.supportVersion_Aerilate)?.text =
                            pogoVersionList
                    } else {
                        Log.i("Aerilate", "Aerilate不支援版本: ${versionInfo.pogoVersion}")
                    }
                }
            }
        }

        fun appAllCheckDone() {
            if (pgToolsCheckDone && appListCheckDone) {
                pgToolsCheckDone = false
                appListCheckDone = false
                getAerilateVersion()
                getPolygonVersion()
                view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout).isRefreshing =
                    false
            }
        }

        @SuppressLint("SetTextI18n")
        fun checkAppVersion() {
            appListCheckDone = false
            var needUpdateAppsAmount = 0
            val download = resources.getString(R.string.download)
            val update = resources.getString(R.string.update)

            val pgToolsInstalledVersion = appInstalledVersion(pgToolsPackageName)
            val pokInstalledVersion = appInstalledVersion(pokePackageName)
            val pokeAresInstalledVersion = appInstalledVersion(pokeAresPackageName)

            for (mapping in buttonsIdData) {
                val removeButton = view.findViewById<Button>(mapping.removeButtonId)
                val moreButton = view.findViewById<ImageButton>(mapping.moreButtonId)

                val visibility = viewShowOrHide(appInstalledOrNot(mapping.packageName))

                removeButton?.visibility = visibility
                moreButton?.visibility = visibility
            }

            val url = resources.getString(R.string.url_appInfo)
            extractAppVersionsFromJson(url) {
                view.findViewById<TextView>(R.id.wrapper_new_version).text =
                    String.format(
                        formatNewerVersion,
                        version_wrapper,
                    )
                view.findViewById<TextView>(R.id.Aerilate_new_version).text =
                    String.format(
                        formatNewerVersion,
                        version_aerilate,
                    )
                view.findViewById<TextView>(R.id.polygon_new_version).text =
                    String.format(
                        formatNewerVersion,
                        version_polygon,
                    )
                view.findViewById<TextView>(R.id.pokelist_new_version).text =
                    String.format(
                        formatNewerVersion,
                        version_PokeList,
                    )
                view.findViewById<TextView>(R.id.wecatch_new_version).text =
                    String.format(
                        formatNewerVersion,
                        version_WeCatch,
                    )

                if (appInstalledVersion(getString(R.string.packageName_wrapper)) != "未安裝" &&
                    appInstalledVersion(getString(R.string.packageName_wrapper)) != version_wrapper
                ) {
                    wrapperDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    wrapperDownloadButton.text = download
                }

                if (appInstalledVersion(getString(R.string.packageName_polygon)) != "未安裝" &&
                    appInstalledVersion(getString(R.string.packageName_polygon)) != version_polygon
                ) {
                    polygonDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    polygonDownloadButton.text = download
                }

                if (appInstalledVersion(getString(R.string.packageName_PokeList)) != "未安裝" &&
                    appInstalledVersion(getString(R.string.packageName_PokeList)) != version_PokeList
                ) {
                    pokeListDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    pokeListDownloadButton.text = download
                }

                if (appInstalledVersion(getString(R.string.packageName_WeCatch)) != "未安裝" &&
                    appInstalledVersion(getString(R.string.packageName_WeCatch)) != version_WeCatch
                ) {
                    weCatchDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    weCatchDownloadButton.text = download
                }

                if (appInstalledVersion(getString(R.string.packageName_Aerilate)) != "未安裝" &&
                    appInstalledVersion(getString(R.string.packageName_Aerilate)) != version_aerilate
                ) {
                    aerilateDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    aerilateDownloadButton.text = download
                }

                if (needUpdateAppsAmount > 0) {
                    view.findViewById<com.google.android.material.appbar.SubtitleCollapsingToolbarLayout>(
                        R.id.toolbar_layout
                    ).subtitle = String.format(
                        resources.getString(R.string.format_installApps),
                        needUpdateAppsAmount
                    )
                } else {
                    view.findViewById<com.google.android.material.appbar.SubtitleCollapsingToolbarLayout>(
                        R.id.toolbar_layout
                    ).subtitle = getString(R.string.appsAllUpdated)
                }

                appListCheckDone = true
                appAllCheckDone()
            }

            view.findViewById<TextView>(R.id.polygon_install_version).text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_polygon))
                )
            view.findViewById<TextView>(R.id.pgtools_install_version).text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(pgToolsPackageName)
                )
            view.findViewById<TextView>(R.id.pok_install_version).text =
                String.format(
                    formatInstallVersionOther,
                    appInstalledVersion(pokePackageName),
                    appInstalledAbi(pokePackageName)
                )
            view.findViewById<TextView>(R.id.pokAres_install_version).text =
                String.format(
                    formatInstallVersionOther,
                    appInstalledVersion(resources.getString(R.string.packageName_pokAres)),
                    if (Build.MANUFACTURER == "samsung" || pokAresNoSupportDevices)
                        appInstalledAbi(pokeAresPackageName)
                    else
                        "(${resources.getString(R.string.unsupportedDevices)})"
                )
            view.findViewById<TextView>(R.id.gps_install_version).text =
                String.format(
                    formatInstallVersion,
                    boolToInstalled(appInstalledOrNot(resources.getString(R.string.packageName_gps32)))
                )
            view.findViewById<TextView>(R.id.pokelist_install_version).text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_PokeList))
                )
            view.findViewById<TextView>(R.id.wecatch_install_version).text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_WeCatch))
                )
            view.findViewById<TextView>(R.id.wrapper_install_version).text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_wrapper))
                )
            view.findViewById<TextView>(R.id.Aerilate_install_version).text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_Aerilate))
                )

            fun setDownloadButton(isUpdate: Boolean = false) {
                pokeDownloadButton.text = if (isUpdate) update else download
                if (!pokeDownloadButton.isEnabled)
                    pokeDownloadButton.isEnabled = true
            }

            if (pogoVersion != "未安裝" && pokInstalledVersion != "未安裝") {
                val pogoVersionInt: List<String> = pogoVersion.split(".")
                val pokInstalledVersionInt: List<String> = pokInstalledVersion.split(".")
                var needUpdate = false
                var needDowngrade = false

                for (i in pogoVersionInt.indices) {
                    val currentVersion = pogoVersionInt[i].toInt()
                    val installedVersion = pokInstalledVersionInt[i].toInt()

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
                        view.findViewById<TextView>(R.id.pok_install_version).text =
                            String.format(
                                formatInstallVersionOther,
                                appInstalledVersion(pokePackageName) +
                                        resources.getString(R.string.versionTooHigh),
                                appInstalledAbi(pokePackageName)
                            )

                        if (pokeDownloadButton.isEnabled)
                            pokeDownloadButton.isEnabled = false

                        /*MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.dialogVersionTooHighTitle))
                            .setMessage(resources.getString(R.string.dialogVersionTooHighMessage))
                            .setNeutralButton(R.string.ok) { _, _ -> }
                            .setNegativeButton("如何降版") { _, _ ->
                                showAlertDialog(
                                    resources.getString(R.string.dialogVersionTooHighTitle),
                                    "1. 請先移除較新版本寶可夢 \n2. 重新下載支援版本寶可夢 \n3. 重啟手機嘗試啟動"
                                )
                            }
                            .setPositiveButton("使用測試版") { _, _ ->
                                pokeTestVersionSwitch.isChecked = true
                            }
                            .show()*/
                    }

                    needUpdate -> {
                        setDownloadButton(true)
                        needUpdateAppsAmount++
                    }

                    else -> {
                        setDownloadButton()
                    }
                }
            } else {
                setDownloadButton()
            }

            if (pogoVersion != "未安裝" && pokeAresInstalledVersion != "未安裝") {
                val pogoVersionInt: List<String> = pogoVersion.split(".")
                val pokInstalledVersionInt: List<String> = pokeAresInstalledVersion.split(".")

                for (i in pogoVersionInt.indices) {
                    val currentVersion = pogoVersionInt[i].toInt()
                    val installedVersion = pokInstalledVersionInt[i].toInt()

                    if (currentVersion > installedVersion) {
                        pokeAresDownloadButton.text = update
                        needUpdateAppsAmount++
                        break
                    } else {
                        pokeAresDownloadButton.text = download
                    }
                }
            } else {
                pokeAresDownloadButton.text = download
            }

            if (pgToolsVersion != "未安裝" && pgToolsInstalledVersion != "未安裝") { //check pgtools version
                val pgToolsVersionInt: List<String> = pgToolsVersion.split(".")
                val pgToolsInstalledVersionInt: List<String> = pgToolsInstalledVersion.split(".")
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
                    pgToolsDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    pgToolsDownloadButton.text = download
                }
            } else {
                pgToolsDownloadButton.text = download
            }

            if (needUpdateAppsAmount > 0) {
                view.findViewById<com.google.android.material.appbar.SubtitleCollapsingToolbarLayout>(
                    R.id.toolbar_layout
                ).subtitle = String.format(
                    resources.getString(R.string.format_installApps),
                    needUpdateAppsAmount
                )
            } else {
                view.findViewById<com.google.android.material.appbar.SubtitleCollapsingToolbarLayout>(
                    R.id.toolbar_layout
                ).subtitle = getString(R.string.appsAllUpdated)
            }
        }

        fun getPGToolsVersion() {
            pgToolsCheckDone = false
            var url = resources.getString(R.string.url_PGToolsJson)
            if (pgToolsTestVersion)
                url = resources.getString(R.string.url_PGToolsJsonTest)
            val versionType =
                if (pgToolsTestVersion) " (${resources.getString(R.string.testVersion)})" else ""

            extractPgToolsFromJson(url) { pogoVersion, pgtoolsVersion, pogoVersionsList ->
                val versionsList = ArrayList<String>()
                var pogoVersionList =
                    resources.getString(R.string.appsPokePage_supportVersion_PGTools)

                for (versionInfo in pogoVersionsList) {
                    versionsList.add(versionInfo.pogoVersion)
                    Log.i(
                        "PgTools",
                        "PgTools支援版本: ${versionInfo.pogoVersion}\n" +
                                "pogoARM64: ${versionInfo.pogoARM64}\n" +
                                "pogoARM: ${versionInfo.pogoARM}"
                    )
                    pogoVersionList += " ${versionInfo.pogoVersion},"
                }

                pogoVersionList = if (pgToolsTestVersion) {
                    pogoVersionList.substring(
                        0,
                        pogoVersionList.length - 1
                    ) + " (${getText(R.string.testVersion)})"

                } else {
                    pogoVersionList.substring(0, pogoVersionList.length - 1)
                }

                view.findViewById<TextView>(R.id.supportVersion_PGTools).text = pogoVersionList

                versionsList.reverse()

                val adapter: ArrayAdapter<String> = ArrayAdapter(
                    view.context,
                    android.R.layout.simple_spinner_item,
                    versionsList
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                //spinner.setSelection(0)

                pokeSupportVersion.text =
                    String.format(
                        formatNewerVersionOther,
                        pogoVersion,
                        if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                            resources.getString(R.string.apps_ARMV8a) + versionType
                        else
                            resources.getString(R.string.apps_ARMV7a) + versionType
                    )
                pokeAresSupportVersion.text =
                    String.format(
                        formatNewerVersionOther,
                        pogoVersion,
                        if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                            resources.getString(R.string.apps_ARMV8a)
                        else
                            resources.getString(R.string.apps_ARMV7a)
                    )
                pgToolSupportVersion.text =
                    String.format(
                        formatNewerVersionOther,
                        pgtoolsVersion,
                        versionType
                    )

                pgToolsCheckDone = true
                appAllCheckDone()
            }
        }

        fun setOnCheckedChangeListener() {
            pokeTestVersionSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    pgToolsTestVersion = true
                    Snackbar.make(
                        view,
                        "已切換至測試版",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show()
                } else {
                    pgToolsTestVersion = false
                    Snackbar.make(
                        view,
                        "已切換至正式版",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show()
                }
                getPGToolsVersion()
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val version = parent.getItemAtPosition(position).toString()
                    var armUrl = ""
                    var arm64Url = ""

                    for (versionInfo in pogoVersionsList) {
                        if (versionInfo.pogoVersion == version) {
                            pogoVersion = versionInfo.pogoVersion
                            armUrl = versionInfo.pogoARM
                            arm64Url = versionInfo.pogoARM64
                            break
                        }
                    }

                    if (arm64Url != "" && armUrl != "") {
                        pgToolsARMUrl = armUrl
                        pgToolsARM64Url = arm64Url

                        val versionType =
                            if (pgToolsTestVersion) " (${resources.getString(R.string.testVersion)})" else ""
                        pokeSupportVersion.text =
                            String.format(
                                formatNewerVersionOther,
                                pogoVersion,
                                if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                                    resources.getString(R.string.apps_ARMV8a) + versionType
                                else
                                    resources.getString(R.string.apps_ARMV7a) + versionType
                            )
                        pokeAresSupportVersion.text =
                            String.format(
                                formatNewerVersionOther,
                                pogoVersion,
                                if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                                    resources.getString(R.string.apps_ARMV8a)
                                else
                                    resources.getString(R.string.apps_ARMV7a)
                            )
                    }
                    checkAppVersion()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Another interface callback
                }
            }
        }

        //checkAppVersion()
        getPGToolsVersion()
        setOnCheckedChangeListener()
    }

    private fun extractAppVersionsFromJson(url: String, onAppVersionsExtracted: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
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

                val jsonObject = JSONObject(response.toString())

                val pogo = jsonObject.getJSONObject("pogo")
                val jokstick = pogo.getJSONObject("jokstick")
                val warpper = pogo.getJSONObject("warpper")
                val aerilate = pogo.getJSONObject("aerilate")
                val polygon = pogo.getJSONObject("polygon")
                val pokeList = pogo.getJSONObject("pokeList")
                val wecatch = pogo.getJSONObject("wecatch")
                val samsungStore = pogo.getJSONObject("samsungStore")

                url_jokstick = jokstick.getString("url")
                url_wrapper = warpper.getString("url")
                url_aerilate = aerilate.getString("url")
                url_polygon = polygon.getString("url")
                url_PokeList = pokeList.getString("url")
                url_WeCatch = wecatch.getString("url")
                url_samsungStore = samsungStore.getString("url")

                version_wrapper = warpper.getString("version")
                version_aerilate = aerilate.getString("version")
                version_polygon = polygon.getString("version")
                polygonTestKey = polygon.getString("testKey")
                version_PokeList = pokeList.getString("version")
                version_WeCatch = wecatch.getString("version")

                Log.i(
                    "PgTools",
                    "warpper:$version_wrapper\n" +
                            "aerilate:$version_aerilate\n" +
                            "polygon:$version_polygon $polygonTestKey\n" +
                            "pokeList:$version_PokeList\n" +
                            "wecatch:$version_WeCatch"
                )

                launch(Dispatchers.Main) {
                    onAppVersionsExtracted()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractPgToolsFromJson(
        url: String,
        onAppVersionsExtracted: (String, String, ArrayList<PogoVersionInfo>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
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

                val jsonObject = JSONObject(response.toString())

                pogoVersion = jsonObject.getString("pogoVersion")
                pgToolsARMUrl = jsonObject.getString("pogoARM")
                pgToolsARM64Url = jsonObject.getString("pogoARM64")
                pgToolsVersion = jsonObject.getString("appName")
                pgToolsUrl = if (pgToolsTestVersion)
                    "https://assets.pgtools.net/test-pgtools-${pgToolsVersion}.apk"
                else
                    "https://assets.pgtools.net/pgtools-${pgToolsVersion}.apk"

                Log.i(
                    "PgTools",
                    "pogoVersion:$pogoVersion\n" +
                            "pgToolsVersion:$pgToolsVersion\n" +
                            "pgToolsARMUrl:$pgToolsARMUrl\n" +
                            "pgToolsARM64Url:$pgToolsARM64Url"
                )

                pogoVersionsList.clear()

                val supportPogoVersions = jsonObject.getJSONObject("supportPogoVersions")
                val pogoVersions = supportPogoVersions.keys()

                while (pogoVersions.hasNext()) {
                    val pogoVersion = pogoVersions.next() as String
                    val pogoData = supportPogoVersions.getJSONObject(pogoVersion)
                    val arm64Url = pogoData.getString("pogoARM64")
                    val armUrl = pogoData.getString("pogoARM")

                    val versionInfo = PogoVersionInfo(pogoVersion, arm64Url, armUrl)
                    pogoVersionsList.add(versionInfo)
                }

                Log.i(
                    "PgTools",
                    "PgTools支援版本: $pogoVersion\n" +
                            "pogoARM64: $pgToolsARM64Url\n" +
                            "pogoARM: $pgToolsARMUrl"
                )

                launch(Dispatchers.Main) {
                    onAppVersionsExtracted(pogoVersion, pgToolsVersion, pogoVersionsList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAerilate(url: String, onCheckCompleted: (Boolean) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlObject = URL(url)
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }

                inputStream.close()

                val containsNullP = response.contains("null\"P")

                launch(Dispatchers.Main) {
                    onCheckCompleted(containsNullP)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                launch(Dispatchers.Main) {
                    onCheckCompleted(false)
                }
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
                        val intent =
                            requireContext().packageManager.getLaunchIntentForPackage(packageName)

                        if (intent != null) {

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

    private fun gotoBrowser(url: String) {
        context?.let {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val customTabsOff = sharedPreferences.getBoolean("customTabsOff", false)

            if (customTabsOff)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else
                CustomTabsIntent.Builder().build()
                    .launchUrl(it, Uri.parse(url))
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
                    if (url.endsWith(".apk"))
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    else
                        gotoBrowser(url)
                }
                setNegativeButton(R.string.downloadProblem) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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

    private fun appInstalledAbi(packageName: String): String {
        if (appInstalledOrNot(packageName)) {
            val pm = activity?.packageManager
            try {
                pm?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                return pm?.getPackageInfo(
                    packageName,
                    PackageManager.GET_ACTIVITIES
                )?.applicationInfo?.nativeLibraryDir?.contains("arm64").let {
                    if (it == true)
                        resources.getString(R.string.apps_ARMV8a)
                    else
                        resources.getString(R.string.apps_ARMV7a)
                }
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return ""
    }

    private fun appUnInstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
}