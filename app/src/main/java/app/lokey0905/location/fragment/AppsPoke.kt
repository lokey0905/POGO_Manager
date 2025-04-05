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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.api.polygon
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.appbar.SubtitleCollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
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

data class AppsInfo(
    val appName: String = "",
    val packageName: String,
    val downloadButtonId: Int,
    val removeButtonId: Int,
    val moreButtonId: Int,
    var newVersionTextId: Int,
    val installVersionTextId: Int,
    var newVersion: String = "未安裝",
    var downloadLink: String = "",
    var officialLink: String = "",
)

class AppsPoke : Fragment() {
    val pogoVersionsList = ArrayList<PogoVersionInfo>()
    private var polygonVersionsList = ArrayList<String>()
    private var aerilateVersionsList = ArrayList<String>()
    private var pokemodVersionsList = ArrayList<String>()
    private var pokemonMinVersion = ""
    private var pogoVersion: String = "未安裝"
    private var pgToolsARMUrl: String = ""
    private var pgToolsARM64Url: String = ""
    private var pgToolsVersion: String = "未安裝"
    private var pgToolsUrl = ""

    private var pgToolsTestVersion = false
    private var pokAresNoSupportDevices = false

    private var pgToolsCheckDone = false
    private var appListCheckDone = false

    private var totalMemory = 0
    private var errorCounter = 0

    private var polygonTestKey = ""
    private var polygonTestToken = ""
    private var appsInfo = listOf<AppsInfo>()
    private var url_pokAres = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_poke, container, false)

        appsInfo = listOf(
            AppsInfo(
                "joystick",
                resources.getString(R.string.packageName_gps64),
                R.id.download_gps,
                R.id.remove_gps,
                R.id.gps_more,
                0,
                R.id.gps_install_version
            ),
            AppsInfo(
                "polygon",
                resources.getString(R.string.packageName_polygon),
                R.id.download_polygon,
                R.id.remove_polygon,
                R.id.polygon_more,
                R.id.polygon_new_version,
                R.id.polygon_install_version
            ),
            AppsInfo(
                "PGTools",
                resources.getString(R.string.packageName_PGTools),
                R.id.download_pgtools,
                R.id.remove_pgtools,
                R.id.pgtools_more,
                R.id.pgtools_new_version,
                R.id.pgtools_install_version
            ),
            AppsInfo(
                "pok",
                resources.getString(R.string.packageName_pok),
                R.id.download_pok,
                R.id.remove_pok,
                R.id.pok_more,
                R.id.pok_new_version,
                R.id.pok_install_version
            ),
            AppsInfo(
                "pokAres",
                resources.getString(R.string.packageName_pokAres),
                R.id.download_pokAres,
                R.id.remove_pokAres,
                R.id.pokAres_more,
                R.id.pokAres_new_version,
                R.id.pokAres_install_version
            ),
            AppsInfo(
                "pokeList",
                resources.getString(R.string.packageName_PokeList),
                R.id.download_pokelist,
                R.id.remove_pokelist,
                R.id.pokelist_more,
                R.id.pokelist_new_version,
                R.id.pokelist_install_version
            ),
            AppsInfo(
                "wecatch",
                resources.getString(R.string.packageName_WeCatch),
                R.id.download_wecatch,
                R.id.remove_wecatch,
                R.id.wecatch_more,
                R.id.wecatch_new_version,
                R.id.wecatch_install_version
            ),
            AppsInfo(
                "wrapper",
                resources.getString(R.string.packageName_wrapper),
                R.id.download_wrapper,
                R.id.remove_wrapper,
                R.id.wrapper_more,
                R.id.wrapper_new_version,
                R.id.wrapper_install_version
            ),
            AppsInfo(
                "aerilate",
                resources.getString(R.string.packageName_Aerilate),
                R.id.download_Aerilate,
                R.id.remove_Aerilate,
                R.id.Aerilate_more,
                R.id.Aerilate_new_version,
                R.id.Aerilate_install_version
            ),
            AppsInfo(
                "samsungStore",
                resources.getString(R.string.packageName_galaxyStore),
                R.id.download_galaxyStore,
                R.id.remove_galaxyStore,
                R.id.galaxyStore_more,
                R.id.galaxyStore_new_version,
                R.id.galaxyStore_install_version
            ),
            AppsInfo(
                "APKMirrorInstaller",
                resources.getString(R.string.packageName_APKMirrorInstaller),
                R.id.download_APKMirrorInstaller,
                R.id.remove_APKMirrorInstaller,
                R.id.APKMirrorInstaller_more,
                R.id.APKMirrorInstaller_new_version,
                R.id.APKMirrorInstaller_install_version
            )
        )

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

        MobileAds.initialize(requireActivity())
        val mAdView = view.findViewById<AdView>(R.id.ad_banner)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        url_pokAres = resources.getString(R.string.url_pokAres)

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

                val pokeAresLayout = view.findViewById<LinearLayout>(R.id.linearLayout_pokAres)

                if (totalMemory > 4 || pokAresNoSupportDevices) {
                    pokeAresLayout.visibility = viewShowOrHide(true)
                } else {
                    pokeAresLayout.visibility = viewShowOrHide(false)
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

        fun downloadPokAres() {
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
                                downloadAPPWithCheck(
                                    appsInfo.find { it.appName == "samsungStore" }?.downloadLink
                                        ?: ""
                                )
                            }
                        }
                        .show()
                } else {
                    downloadAPPWithCheck(
                        String.format(url_pokAres, pogoVersion.replace(".", "-"))
                    )

                }
            } else
                showAlertDialog(
                    resources.getString(R.string.unsupportedDevices),
                    resources.getString(R.string.unsupportedDevicesPokeAres)
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
                when (appName) {
                    "pok" -> {
                        val url =
                            if (Build.SUPPORTED_ABIS[0] == "arm64-v8a") pgToolsARM64Url else pgToolsARMUrl
                        downloadAPPWithCheck(url)
                    }

                    "pokAres" -> {
                        downloadPokAres()
                    }

                    "PGTools" -> {
                        downloadAppCheckARM64(pgToolsUrl)
                    }

                    "polygon" -> {
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
                pgToolsCheckDone = false
                appListCheckDone = false
                setupAppVersionInfo(view)
            }
    }

    @SuppressLint("SetTextI18n")
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

        val pokeTestVersionSwitch = view.findViewById<MaterialSwitch>(R.id.pokeTestVersion_switch)
        val textInputLayout = view.findViewById<TextInputLayout>(R.id.textField)
        val autoCompleteTextView = textInputLayout.editText as? AutoCompleteTextView

        fun getPokemonLatestVersion() {
            checkPokemon { latestVersion ->
                Log.i("Pokemon", "Pokemon最新版本: $latestVersion")
                view.findViewById<TextView>(R.id.supportVersion_pokemon)?.text =
                    "${getString(R.string.appsPokePage_supportVersion_pokemon)} $latestVersion"

                if (latestVersion != "ERROR" && latestVersion != "") {
                    pokemonMinVersion = latestVersion
                }
            }
        }

        fun getPolygonSupportedVersion() {
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
                        if (errorCounter > 1) {
                            Log.i("Polygon", "Try Login too many times")
                            return@checkPogoVersion
                        }

                        Log.i("Polygon", "Try Login again $errorCounter")
                        errorCounter++
                        getPolygonSupportedVersion()
                    } else {
                        polygonTestToken = token
                        getPolygonSupportedVersion()
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

                            if (errorCounter > 1) {
                                Log.i("Polygon", "Try Login too many times")
                                return@sendSecondJsonRequest
                            }
                            Log.i("Polygon", "取得支援版本失敗 $errorCounter")
                            errorCounter++
                            getPolygonSupportedVersion()
                            return@sendSecondJsonRequest
                        }

                        polygonVersionsList.add(it)
                        polygonVersionsList.sort()

                        var pogoVersionList =
                            resources.getString(R.string.appsPokePage_supportVersion_polygon)
                        //var matchingVersionInfo: PogoVersionInfo? = null

                        for (polygonSupportedVersion in polygonVersionsList) {
                            pogoVersionList += " ${polygonSupportedVersion},"

                            /*matchingVersionInfo =
                                pogoVersionsList.find { it.pogoVersion == polygonSupportedVersion }
                             */
                        }

                        /*matchingVersionInfo?.let { versionInfo ->
                            val selectionIndex =
                                pogoVersionsList.size - 1 - pogoVersionsList.indexOf(versionInfo)
                            spinner.post {
                                spinner.setSelection(selectionIndex)
                                Log.i("Polygon", "spinner.setSelection: $selectionIndex")
                            }
                        }*/

                        Log.i("Polygon", "polygonVersionsList: $polygonVersionsList")

                        pogoVersionList = pogoVersionList.substring(0, pogoVersionList.length - 1)

                        view.findViewById<TextView>(R.id.supportVersion_polygon)?.text =
                            pogoVersionList

                        errorCounter = 0
                    }
                }
            }
        }

        fun getAerilateSupportedVersion() {
            for (versionInfo in pogoVersionsList) {
                val url = String.format(
                    resources.getString(R.string.url_AerilateAPI),
                    versionInfo.pogoVersion
                )

                checkAerilate(url) { unsupportedVersion ->
                    if (!unsupportedVersion) {
                        Log.i("Aerilate", "Aerilate支援版本: ${versionInfo.pogoVersion}")
                        if (aerilateVersionsList.contains(versionInfo.pogoVersion))
                            return@checkAerilate

                        aerilateVersionsList.add(versionInfo.pogoVersion)
                        aerilateVersionsList.sort()

                        var pogoVersionList =
                            resources.getString(R.string.appsPokePage_supportVersion_Aerilate)
                        var matchingVersionInfo: PogoVersionInfo? = null
                        for (aerilateSupportedVersion in aerilateVersionsList) {
                            pogoVersionList += " ${aerilateSupportedVersion},"

                            matchingVersionInfo =
                                pogoVersionsList.find { it.pogoVersion == aerilateSupportedVersion }
                        }

                        val adapter: ArrayAdapter<String> = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            pogoVersionsList.map { it.pogoVersion }
                        )
                        autoCompleteTextView?.setAdapter(adapter)

                        matchingVersionInfo?.let { versionInfo ->
                            autoCompleteTextView?.post {
                                autoCompleteTextView.setText(versionInfo.pogoVersion, false)
                            }
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

        fun getPokemodSupportedVersion() {
            for (versionInfo in pogoVersionsList) {
                val url = String.format(
                    resources.getString(R.string.url_PokemodAPI),
                    versionInfo.pogoVersion
                )

                checkPokemod(url) { unsupportedVersion ->
                    if (!unsupportedVersion) {
                        Log.i("Pokemod", "Pokemod支援版本: ${versionInfo.pogoVersion}")
                        if (pokemodVersionsList.contains(versionInfo.pogoVersion))
                            return@checkPokemod

                        pokemodVersionsList.add(versionInfo.pogoVersion)
                        pokemodVersionsList.sort()

                        var pogoVersionList =
                            resources.getString(R.string.appsPokePage_supportVersion_Pokemod)
                        //var matchingVersionInfo: PogoVersionInfo? = null
                        for (aerilateSupportedVersion in pokemodVersionsList) {
                            pogoVersionList += " ${aerilateSupportedVersion},"

                            //matchingVersionInfo = pogoVersionsList.find { it.pogoVersion == aerilateSupportedVersion }
                        }

                        /*matchingVersionInfo?.let { versionInfo ->
                            val selectionIndex = pogoVersionsList.size - 1 - pogoVersionsList.indexOf(versionInfo)
                            spinner.post {
                                spinner.setSelection(selectionIndex)
                                Log.i("Pokemod", "spinner.setSelection: $selectionIndex")
                            }
                        }*/

                        pogoVersionList = pogoVersionList.substring(0, pogoVersionList.length - 1)

                        view.findViewById<TextView>(R.id.supportVersion_Pokemod)?.text =
                            pogoVersionList
                    } else {
                        Log.i("Pokemod", "Pokemod不支援版本: ${versionInfo.pogoVersion}")
                    }
                }
            }
        }

        fun appAllCheckDone() {
            //check if the latest version is in the list
            if (pogoVersionsList.find { it.pogoVersion == pokemonMinVersion } == null && pokemonMinVersion != "") {
                pogoVersionsList.add(
                     PogoVersionInfo(
                        pokemonMinVersion,
                         String.format("https://assets.pgtools.net/games/%1\$s-arm64.apkm", pokemonMinVersion),
                         String.format("https://assets.pgtools.net/games/%1\$s-arm.apkm", pokemonMinVersion)
                    )
                )

                Log.i( "Pokemon", "Pokemon新增版本: $pokemonMinVersion\n")

                // update spinner
                val adapter: ArrayAdapter<String> = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    pogoVersionsList.map { it.pogoVersion }
                )
                autoCompleteTextView?.setAdapter(adapter)
            }

            if (pgToolsCheckDone && appListCheckDone) {
                pgToolsCheckDone = false
                appListCheckDone = false
                getAerilateSupportedVersion()
                getPolygonSupportedVersion()
                getPokemodSupportedVersion()
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

            val toolbar_layout =
                view.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout)

            //check app if installed then show remove button and more button
            for (mapping in appsInfo) {
                val removeButton = view.findViewById<Button>(mapping.removeButtonId)
                val moreButton = view.findViewById<ImageButton>(mapping.moreButtonId)

                val visibility = viewShowOrHide(appInstalledOrNot(mapping.packageName))

                removeButton?.visibility = visibility
                moreButton?.visibility = visibility
            }

            val url = resources.getString(R.string.url_appInfo)
            extractAppVersionsFromJson(url) {
                fun updateNewVersionText() {
                    val needUpdateList =
                        listOf(
                            "wrapper",
                            "aerilate",
                            "polygon",
                            "pokeList",
                            "wecatch",
                            "samsungStore",
                            "APKMirrorInstaller"
                        )

                    for (apps in appsInfo) {
                        val appName = apps.appName
                        if (!needUpdateList.contains(appName)) {
                            continue
                        }

                        val newVersion = apps.newVersion
                        val newVersionText = view.findViewById<TextView>(apps.newVersionTextId)
                        newVersionText.text = String.format(
                            formatNewerVersion,
                            newVersion
                        )
                    }
                }

                fun checkNeedUpdateAppsAmount() {
                    val needUpdateList =
                        listOf(
                            "wrapper", "aerilate", "polygon", "pokeList", "wecatch",
                        )

                    for (apps in appsInfo) {
                        val appName = apps.appName
                        if (!needUpdateList.contains(appName)) {
                            continue
                        }

                        val packageName = apps.packageName
                        val newVersion = apps.newVersion
                        val installVersion = appInstalledVersion(packageName)

                        if (installVersion != "未安裝" && compareVersions(
                                newVersion,
                                installVersion
                            ) > 0
                        ) {
                            needUpdateAppsAmount++
                        }
                    }
                }

                updateNewVersionText()
                checkNeedUpdateAppsAmount()

                toolbar_layout.subtitle = if (needUpdateAppsAmount > 0){
                    String.format(
                        resources.getString(R.string.format_installApps),
                        needUpdateAppsAmount
                    )
                } else {
                    getString(R.string.appsAllUpdated)
                }

                appListCheckDone = true
                appAllCheckDone()
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
                            appInstalledAbi(packageName)
                        )
                } else if (appName == "pokAres") {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersionOther,
                            appInstalledVersion(pokeAresPackageName),
                            "(${resources.getString(R.string.unsupportedDevices)})"
                        )
                } else if (appName == "joystick") {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersionOther,
                            boolToInstalled(appInstalledOrNot(packageName)),
                            appInstalledAbi(packageName)
                        )
                } else {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersion,
                            installVersion
                        )
                }
            }

            fun setDownloadButton(isUpdate: Boolean = false) {
                pokeDownloadButton.text = if (isUpdate) update else download

                pokeDownloadButton.isEnabled = true
            }

            //check poke version
            if (pogoVersion != "未安裝" && pokInstalledVersion != "未安裝") {
                val versionComparison = compareVersions(pogoVersion, pokInstalledVersion)
                when {
                    // need downgrade
                    versionComparison < 0 -> {
                        view.findViewById<TextView>(R.id.pok_install_version).text =
                            String.format(
                                formatInstallVersionOther,
                                appInstalledVersion(pokePackageName) + resources.getString(R.string.versionTooHigh),
                                appInstalledAbi(pokePackageName)
                            )

                        pokeDownloadButton.isEnabled = false
                    }
                    // need update
                    versionComparison > 0 -> {
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

            //check pokeAres version
            if (pogoVersion != "未安裝" && pokeAresInstalledVersion != "未安裝") {
                val versionComparison = compareVersions(pogoVersion, pokeAresInstalledVersion)

                pokeAresDownloadButton.text = if (versionComparison > 0) {
                    needUpdateAppsAmount++
                    update
                } else {
                    download
                }
            } else {
                pokeAresDownloadButton.text = download
            }

            //check pgtools version
            if (pgToolsVersion != "未安裝" && pgToolsInstalledVersion != "未安裝") {
                val versionComparison = compareVersions(pogoVersion, pokeAresInstalledVersion)

                pgToolsDownloadButton.text = if (versionComparison > 0) {
                    needUpdateAppsAmount++
                    update
                } else {
                    download
                }
            } else {
                pgToolsDownloadButton.text = download
            }

            toolbar_layout.subtitle = if (needUpdateAppsAmount > 0){
                String.format(
                    resources.getString(R.string.format_installApps),
                    needUpdateAppsAmount
                )
            } else {
                getString(R.string.appsAllUpdated)
            }
        }

        fun getPGToolsVersion() {
            pgToolsCheckDone = false
            var url = resources.getString(R.string.url_PGToolsJson)
            if (pgToolsTestVersion)
                url = resources.getString(R.string.url_PGToolsJsonTest)

            var versionType = ""
            if (pgToolsTestVersion)
                versionType = " (${resources.getString(R.string.testVersion)})"

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
                    android.R.layout.simple_list_item_1,
                    versionsList
                )
                autoCompleteTextView?.setAdapter(adapter)

                autoCompleteTextView?.setText(pogoVersion, false)

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

            autoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
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
                if (arm64Url.isNotEmpty() && armUrl.isNotEmpty()) {
                    pgToolsARMUrl = armUrl
                    pgToolsARM64Url = arm64Url
                    val versionType = if (pgToolsTestVersion) " (${resources.getString(R.string.testVersion)})" else ""
                    pokeSupportVersion.text = String.format(
                        formatNewerVersionOther,
                        pogoVersion,
                        if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                            resources.getString(R.string.apps_ARMV8a) + versionType
                        else
                            resources.getString(R.string.apps_ARMV7a) + versionType
                    )
                    pokeAresSupportVersion.text = String.format(
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
        }

        checkAppVersion()
        getPokemonLatestVersion()
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

                val needUpdateList =
                    listOf(
                        "joystick",
                        "wrapper",
                        "aerilate",
                        "polygon",
                        "pokeList",
                        "wecatch",
                        "samsungStore",
                        "APKMirrorInstaller"
                    )

                for (apps in appsInfo) {
                    val appName = apps.appName
                    if (!needUpdateList.contains(appName)) {
                        continue
                    }

                    try {
                        val appInfo = pogo.getJSONObject(appName)
                        apps.newVersion = appInfo.getString("version")
                        apps.downloadLink = appInfo.getString("url")
                        apps.officialLink = appInfo.getString("officialLink")

                        if (appName == "polygon") {
                            polygonTestKey = appInfo.getString("testKey")
                        }

                        Log.i(
                            "getAppsInfo",
                            "appName:$appName version:${apps.newVersion} downloadLink:${apps.downloadLink} officialLink:${apps.officialLink}"
                        )
                    } catch (e: Exception) {
                        Log.e("extractAppVersionsFromJson", "Error processing $appName", e)
                    }
                }

                try {
                    url_pokAres = pogo.getJSONObject("pokAres").getString("url")
                } catch (e: Exception) {
                    Log.e("extractAppVersionsFromJson", "Error processing pokAres", e)
                }

                launch(Dispatchers.Main) {
                    onAppVersionsExtracted()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPokemon(onCheckCompleted: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlObject = URL(getString(R.string.url_PokemonCheckVersionAPI))
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }

                inputStream.close()

                // Extract the version number from the response
                val version = response.trim().substringAfter("")

                launch(Dispatchers.Main) {
                    onCheckCompleted(version)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                launch(Dispatchers.Main) {
                    onCheckCompleted("ERROR")
                }
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

                val supportPogoVersions = jsonObject.getJSONObject("supportPogoVersions")
                val pogoVersions = supportPogoVersions.keys()

                while (pogoVersions.hasNext()) {
                    val version = pogoVersions.next() as String
                    val pogoData = supportPogoVersions.getJSONObject(version)
                    val arm64Url = pogoData.getString("pogoARM64")
                    val armUrl = pogoData.getString("pogoARM")

                    val existingIndex = pogoVersionsList.indexOfFirst { it.pogoVersion == version }
                    if (existingIndex != -1) {
                        pogoVersionsList[existingIndex] = PogoVersionInfo(version, arm64Url, armUrl)
                    } else {
                        pogoVersionsList.add(PogoVersionInfo(version, arm64Url, armUrl))
                    }
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
                val containsNull = response.contains("null\"")

                launch(Dispatchers.Main) {
                    onCheckCompleted(containsNullP || containsNull)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                launch(Dispatchers.Main) {
                    onCheckCompleted(false)
                }
            }
        }
    }

    private fun checkPokemod(url: String, onCheckCompleted: (Boolean) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlObject = URL(url)
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }

                inputStream.close()

                val containsFlag = response.contains("#GAME_VERSION@")

                launch(Dispatchers.Main) {
                    onCheckCompleted(containsFlag)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                launch(Dispatchers.Main) {
                    onCheckCompleted(false)
                }
            }
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".")
        val parts2 = v2.split(".")
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

                    R.id.officialLink -> {
                        val officialLink =
                            appsInfo.find { it.packageName == packageName }?.officialLink
                        if (officialLink == "") {
                            showAlertDialog(
                                resources.getString(R.string.dialogAdNotReadyTitle),
                                resources.getString(R.string.dialogAdNotReadyMessage)
                            )
                        } else {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(officialLink)))
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
        } else if (url.contains("apkmirror")) {
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