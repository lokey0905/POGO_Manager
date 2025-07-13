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
import app.lokey0905.location.api.Aerilate
import app.lokey0905.location.api.Pokemod
import app.lokey0905.location.api.Pokemon
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.appbar.SubtitleCollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    var newVersion: String = "未安裝",
    var downloadLink: String = "",
    var officialLink: String = "",
)

class AppsPoke : Fragment() {
    private val nowPogoVersionsList = ArrayList<PogoVersionInfo>()
    private var pgtoolsVersionsList = ArrayList<PogoVersionInfo>()
    private var aerilateVersionList = ArrayList<String>()
    private var pokemodVersionList = ArrayList<String>()
    private var pokemonMinVersion = ""
    private var choosePogoVersion: String = "未安裝"
    private var pgToolsVersion: String = "未安裝"
    private var pgToolsUrl = ""

    private var pgToolsTestVersion = false
    private var pokAresNoSupportDevices = false
    private var pok_download_on_apkmirror = false

    private var pgToolsCheckDone = false
    private var appListCheckDone = false

    private var totalMemory = 0

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
                getString(R.string.packageName_gps64),
                R.id.download_gps,
                R.id.remove_gps,
                R.id.gps_more,
                0,
                R.id.gps_install_version
            ),
            AppsInfo(
                "polygon",
                getString(R.string.packageName_polygonX),
                R.id.download_polygon,
                R.id.remove_polygon,
                R.id.polygon_more,
                R.id.polygon_new_version,
                R.id.polygon_install_version
            ),
            AppsInfo(
                "PGTools",
                getString(R.string.packageName_PGTools),
                R.id.download_pgtools,
                R.id.remove_pgtools,
                R.id.pgtools_more,
                R.id.pgtools_new_version,
                R.id.pgtools_install_version
            ),
            AppsInfo(
                "pok",
                getString(R.string.packageName_pok),
                R.id.download_pok,
                R.id.remove_pok,
                R.id.pok_more,
                R.id.pok_new_version,
                R.id.pok_install_version
            ),
            AppsInfo(
                "pokAres",
                getString(R.string.packageName_pokAres),
                R.id.download_pokAres,
                R.id.remove_pokAres,
                R.id.pokAres_more,
                R.id.pokAres_new_version,
                R.id.pokAres_install_version
            ),
            AppsInfo(
                "pokeList",
                getString(R.string.packageName_PokeList),
                R.id.download_pokelist,
                R.id.remove_pokelist,
                R.id.pokelist_more,
                R.id.pokelist_new_version,
                R.id.pokelist_install_version
            ),
            AppsInfo(
                "wecatch",
                getString(R.string.packageName_WeCatch),
                R.id.download_wecatch,
                R.id.remove_wecatch,
                R.id.wecatch_more,
                R.id.wecatch_new_version,
                R.id.wecatch_install_version
            ),
            AppsInfo(
                "wrapper",
                getString(R.string.packageName_wrapper),
                R.id.download_wrapper,
                R.id.remove_wrapper,
                R.id.wrapper_more,
                R.id.wrapper_new_version,
                R.id.wrapper_install_version
            ),
            AppsInfo(
                "aerilate",
                getString(R.string.packageName_Aerilate),
                R.id.download_Aerilate,
                R.id.remove_Aerilate,
                R.id.Aerilate_more,
                R.id.Aerilate_new_version,
                R.id.Aerilate_install_version
            ),
            AppsInfo(
                "samsungStore",
                getString(R.string.packageName_galaxyStore),
                R.id.download_galaxyStore,
                R.id.remove_galaxyStore,
                R.id.galaxyStore_more,
                R.id.galaxyStore_new_version,
                R.id.galaxyStore_install_version
            ),
            AppsInfo(
                "APKMirrorInstaller",
                getString(R.string.packageName_APKMirrorInstaller),
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

        MobileAds.initialize(requireActivity())
        val mAdView = view.findViewById<AdView>(R.id.ad_banner)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

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

            setFragmentResultListener("pok_download_on_apkmirror") { _, bundle ->
                pok_download_on_apkmirror = bundle.getBoolean("bundleKey")
            }

            setFragmentResultListener("pgtools_testversion") { _, bundle ->
                pgToolsTestVersion = bundle.getBoolean("bundleKey")

                if (pgToolsTestVersion) {
                    pgToolsUrl = getString(R.string.url_PGToolsJsonTest)
                    Toast.makeText(context, "已切換至測試版", Toast.LENGTH_SHORT).show()
                } else {
                    pgToolsUrl = getString(R.string.url_PGToolsJson)
                    Toast.makeText(context, "已切換至正式版", Toast.LENGTH_SHORT).show()
                }

                setupAppVersionInfo(view)
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
                    "${getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})",
                    Snackbar.LENGTH_LONG
                ).setAction("Action", null).show()
        }

        fun downloadPoke() {
            var url = ""
            if (pok_download_on_apkmirror) {
                url = String.format(
                    getString(R.string.url_poke),
                    choosePogoVersion.replace(".", "-"),
                    choosePogoVersion.replace(".", "-"))

            } else {
                for (versionInfo in nowPogoVersionsList) {
                    if (versionInfo.pogoVersion == choosePogoVersion) {
                        url = versionInfo.pogoARM64URLPGTools
                        break
                    }
                }
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
                        downloadAppCheckARM64(pgToolsUrl)
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
        val formatInstallVersion: String = getString(R.string.format_installVersion)
        val formatInstallVersionOther: String =
            getString(R.string.format_installVersion_other)
        val formatNewerVersion: String = getString(R.string.format_newerVersion)
        val formatNewerVersionOther: String =
            getString(R.string.format_newerVersion_other)

        val pokePackageName = getString(R.string.packageName_pok)
        val pokeAresPackageName = getString(R.string.packageName_pokAres)
        val pgToolsPackageName = getString(R.string.packageName_PGTools)

        val pokeSupportVersion = view.findViewById<TextView>(R.id.pok_new_version)
        val pokeAresSupportVersion = view.findViewById<TextView>(R.id.pokAres_new_version)
        val pgToolSupportVersion = view.findViewById<TextView>(R.id.pgtools_new_version)

        val pokeDownloadButton = view.findViewById<Button>(R.id.download_pok)
        val pokeAresDownloadButton = view.findViewById<Button>(R.id.download_pokAres)
        val pgToolsDownloadButton = view.findViewById<Button>(R.id.download_pgtools)

        val textInputLayout = view.findViewById<TextInputLayout>(R.id.textField)
        val autoCompleteTextView = textInputLayout.editText as? AutoCompleteTextView

        fun getPokemonMinLoginVersion() {
            val pokemonApi = Pokemon()
            lifecycleScope.launch {
                val minLoginVersion =
                    pokemonApi.checkPokemon(getString(R.string.url_PokemonCheckVersionAPI))
                Log.i("Pokemon", "Pokemon最低可登入版本: $minLoginVersion")
                view.findViewById<TextView>(R.id.supportVersion_pokemon)?.text =
                    "${getString(R.string.appsPokePage_supportVersion_pokemon)} $minLoginVersion"

                if (minLoginVersion != "ERROR" && minLoginVersion != "") {
                    pokemonMinVersion = minLoginVersion
                }
            }
        }

        fun getPolygonXSupportedVersion() {
            val polygonXApi = app.lokey0905.location.api.polygonX()
            val polygonXVersionCode =
                appInstalledVersionCode(getString(R.string.packageName_polygonX)).toInt()
            lifecycleScope.launch {
                val result = polygonXApi.checkPolygonXUpdate(polygonXVersionCode)
                val apps = appsInfo.find { it.appName == "polygon" }
                if (apps == null) {
                    Log.e("PolygonX", "PolygonX app info not found")
                    return@launch
                }

                val newVersionText = view.findViewById<TextView>(apps.newVersionTextId)
                val downloadButton = view.findViewById<Button>(apps.downloadButtonId)

                if (result.latestVersionCode != null) {
                    val newVersion = result.latestVersionCode.toString()
                    apps.newVersion = newVersion
                    apps.downloadLink =
                        "https://polygonx.dl.assets.evermorelabs.io/apk/com.evermorelabs.polygonx-${result.latestVersionCode}.apk"
                    newVersionText.text = String.format(formatNewerVersion, newVersion)
                } else {
                    newVersionText.text =
                        String.format(formatNewerVersion, getString(R.string.error))
                }

                if(appInstalledOrNot(getString(R.string.packageName_polygonX))) {
                    when (result.status) {
                        app.lokey0905.location.api.PolygonXCheckResult.Status.SUCCESS -> {
                            downloadButton.text = getString(R.string.downloadAgain)
                        }
                        app.lokey0905.location.api.PolygonXCheckResult.Status.UPDATE_REQUIRED -> {
                            downloadButton.text = getString(R.string.update)
                        }
                        app.lokey0905.location.api.PolygonXCheckResult.Status.FAILURE -> {
                            downloadButton.text = getString(R.string.download)
                        }
                    }
                } else {
                    downloadButton.text = getString(R.string.download)
                }
            }
        }

        fun getAerilateSupportedVersion() {
            val aerilateApi = Aerilate()
            val installedVersion = appInstalledVersion(getString(R.string.packageName_Aerilate))
            val installedVersionCode = appInstalledVersionCode(getString(R.string.packageName_Aerilate))

            lifecycleScope.launch {
                val supportedVersions = nowPogoVersionsList
                    .filter { versionInfo ->
                        aerilateApi.checkAerilate(
                            installedVersion,
                            installedVersionCode,
                            versionInfo.pogoVersion,
                            versionInfo.pogoVersionNumber
                        )
                    }
                    .map { it.pogoVersion }

                if (supportedVersions.isNotEmpty()) {
                    aerilateVersionList.clear()
                    aerilateVersionList.addAll(supportedVersions)
                    aerilateVersionList.sort()

                    updateSupportedVersionsTextView(
                        R.id.supportVersion_Aerilate,
                        R.string.appsPokePage_supportVersion_Aerilate,
                        aerilateVersionList
                    )

                    // Update AutoCompleteTextView based on the latest supported version
                    val latestSupported = aerilateVersionList.last()
                    val matchingVersionInfo =
                        nowPogoVersionsList.find { it.pogoVersion == latestSupported }
                    matchingVersionInfo?.let {
                        autoCompleteTextView?.setText(it.pogoVersion, false)
                        updatePogoVersionSelection(it.pogoVersion, requireView())
                    }
                }
            }
        }

        fun getPokemodSupportedVersion() {
            val pokemodApi = Pokemod()
            lifecycleScope.launch {
                val supportedVersions = nowPogoVersionsList
                    .filter { versionInfo ->
                        val url = getString(R.string.url_PokemodAPI, versionInfo.pogoVersion)
                        !pokemodApi.checkPokemod(url) // checkPokemod returns true if unsupported
                    }
                    .map { it.pogoVersion }

                if (supportedVersions.isNotEmpty()) {
                    pokemodVersionList.clear()
                    pokemodVersionList.addAll(supportedVersions)
                    pokemodVersionList.sort()

                    updateSupportedVersionsTextView(
                        R.id.supportVersion_Pokemod,
                        R.string.appsPokePage_supportVersion_Pokemod,
                        pokemodVersionList
                    )
                }
            }
        }

        fun appAllCheckDone() {
            //check if the latest version is in the list
            if (nowPogoVersionsList.find { it.pogoVersion == pokemonMinVersion } == null && pokemonMinVersion != "") {
                nowPogoVersionsList.add(
                     PogoVersionInfo(
                         pokemonMinVersion,
                         0,
                         String.format("https://assets.pgtools.net/games/%1\$s-arm64.apkm", pokemonMinVersion),
                         String.format(
                             getString(R.string.url_poke),
                             pokemonMinVersion.replace(".", "-"),
                             pokemonMinVersion.replace(".", "-"))
                    )
                )

                Log.i( "Pokemon", "Pokemon新增版本: $pokemonMinVersion\n")

                // update spinner
                val adapter: ArrayAdapter<String> = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    nowPogoVersionsList.map { it.pogoVersion }
                )
                autoCompleteTextView?.setAdapter(adapter)
            }

            if (pgToolsCheckDone && appListCheckDone) {
                pgToolsCheckDone = false
                appListCheckDone = false
                getAerilateSupportedVersion()
                getPolygonXSupportedVersion()
                getPokemodSupportedVersion()
                view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout).isRefreshing =
                    false
            }
        }

        @SuppressLint("SetTextI18n")
        fun checkAppVersion() {
            appListCheckDone = false
            var needUpdateAppsAmount = 0
            val download = getString(R.string.download)
            val downloadAgain = getString(R.string.downloadAgain)
            val update = getString(R.string.update)

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

            val url = getString(R.string.url_appInfo)
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

                        val packageName = apps.packageName
                        val newVersion = apps.newVersion
                        val installVersion = appInstalledVersion(packageName)
                        val downloadButton = view.findViewById<Button>(apps.downloadButtonId)

                        if (installVersion != "未安裝") {
                            if (compareVersions(newVersion, installVersion) > 0 &&
                                appName != "joystick"
                            ) {
                                downloadButton.text = update
                                needUpdateAppsAmount++
                            } else {
                                downloadButton.text = downloadAgain
                            }
                        } else {
                            downloadButton.text = download
                        }
                    }
                }

                updateNewVersionText()
                checkNeedUpdateAppsAmount()

                toolbar_layout.subtitle = if (needUpdateAppsAmount > 0){
                    String.format(
                        getString(R.string.format_installApps),
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
                            ""
                        )
                } else if (appName == "pokAres") {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersionOther,
                            appInstalledVersion(pokeAresPackageName),
                            "(${getString(R.string.unsupportedDevices)})"
                        )
                } else if (appName == "joystick") {
                    installVersionTextView.text =
                        String.format(
                            formatInstallVersionOther,
                            boolToInstalled(appInstalledOrNot(packageName)),
                            ""
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
                    pokeDownloadButton.text = update
                    pokeDownloadButton.isEnabled = true
                    needUpdateAppsAmount++
                } else {
                    pokeDownloadButton.text = downloadAgain
                    pokeDownloadButton.isEnabled = true
                }
            } else {
                pokeDownloadButton.text = download
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
                    pokeAresDownloadButton.text = update
                    pokeAresDownloadButton.isEnabled = true
                    needUpdateAppsAmount++
                } else {
                    pokeAresDownloadButton.text = downloadAgain
                    pokeAresDownloadButton.isEnabled = true
                }
            } else {
                pokeAresDownloadButton.text = download
                pokeAresDownloadButton.isEnabled = true
            }

            //check pgtools version
            if (pgToolsVersion != "未安裝" && pgToolsInstalledVersion != "未安裝") {
                val versionComparison = compareVersions(pgToolsVersion, pgToolsInstalledVersion)

                if (versionComparison > 0) {
                    pgToolsDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    pgToolsDownloadButton.text = downloadAgain
                }
            } else {
                pgToolsDownloadButton.text = download
            }

            toolbar_layout.subtitle = if (needUpdateAppsAmount > 0){
                String.format(
                    getString(R.string.format_installApps),
                    needUpdateAppsAmount
                )
            } else {
                getString(R.string.appsAllUpdated)
            }
        }

        fun getPGToolsVersion() {
            pgToolsCheckDone = false
            val url =
                if (pgToolsTestVersion) getString(R.string.url_PGToolsJsonTest) else getString(R.string.url_PGToolsJson)
            val versionType =
                if (pgToolsTestVersion) " (${getString(R.string.testVersion)})" else ""

            extractPgToolsFromJson(url) { pogoVersion, pgtoolsVersion ->
                nowPogoVersionsList.clear()
                nowPogoVersionsList.addAll(pgtoolsVersionsList)

                val pgtoolsPogoVersionList = pgtoolsVersionsList.map { it.pogoVersion }
                var pgtoolsPogoVersionString =
                    getString(R.string.appsPokePage_supportVersion_PGTools)

                for (versionInfo in pgtoolsVersionsList) {
                    Log.i(
                        "PgTools",
                        "PgTools支援版本: ${versionInfo.pogoVersion}\n" +
                                "pogoVersionNumber: ${versionInfo.pogoVersionNumber}\n" +
                                "pogoARM64: ${versionInfo.pogoARM64URLPGTools}"
                    )
                    pgtoolsPogoVersionString += " ${versionInfo.pogoVersion},"
                }

                pgtoolsPogoVersionString = if (pgToolsTestVersion) {
                    pgtoolsPogoVersionString.substring(
                        0,
                        pgtoolsPogoVersionString.length - 1
                    ) + " (${getText(R.string.testVersion)})"

                } else {
                    pgtoolsPogoVersionString.substring(0, pgtoolsPogoVersionString.length - 1)
                }

                view.findViewById<TextView>(R.id.supportVersion_PGTools).text =
                    pgtoolsPogoVersionString

                val adapter: ArrayAdapter<String> = ArrayAdapter(
                    view.context,
                    android.R.layout.simple_list_item_1,
                    pgtoolsPogoVersionList.reversed()
                )
                autoCompleteTextView?.setAdapter(adapter)

                autoCompleteTextView?.setText(pogoVersion, false)
                updatePogoVersionSelection(pogoVersion, view)
                checkAppVersion()

                pokeSupportVersion.text =
                    String.format(
                        formatNewerVersionOther,
                        pogoVersion,
                        versionType
                    )
                pokeAresSupportVersion.text =
                    String.format(
                        formatNewerVersionOther,
                        pogoVersion,
                        ""
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
            autoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                val version = parent.getItemAtPosition(position).toString()
                updatePogoVersionSelection(version, view)
                checkAppVersion()
            }
        }

        checkAppVersion()
        getPokemonMinLoginVersion()
        getPGToolsVersion()
        setOnCheckedChangeListener()
    }

    private fun updateSupportedVersionsTextView(textViewId: Int, stringId: Int, versions: List<String>) {
        val textView = view?.findViewById<TextView>(textViewId)
        val prefix = getString(stringId)
        val versionsText = versions.joinToString(", ")
        textView?.text = "$prefix $versionsText"
    }

    private fun updatePogoVersionSelection(version: String, view: View) {
        val formatNewerVersionOther: String =
            getString(R.string.format_newerVersion_other)
        val pokeSupportVersion = view.findViewById<TextView>(R.id.pok_new_version)
        val pokeAresSupportVersion = view.findViewById<TextView>(R.id.pokAres_new_version)

        var arm64Url = ""
        for (versionInfo in nowPogoVersionsList) {
            if (versionInfo.pogoVersion == version) {
                choosePogoVersion = versionInfo.pogoVersion
                arm64Url = versionInfo.pogoARM64URLPGTools
                break
            }
        }

        if (arm64Url.isNotEmpty()) {
            val versionType = if (pgToolsTestVersion) " (${getString(R.string.testVersion)})" else ""

            pokeSupportVersion.text = String.format(
                formatNewerVersionOther,
                choosePogoVersion,
                versionType
            )
            pokeAresSupportVersion.text = String.format(
                formatNewerVersionOther,
                choosePogoVersion,
                ""
            )
        }
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
                        if (appName == "polygon") {
                            apps.officialLink = appInfo.getString("officialLink")
                        } else{
                            apps.newVersion = appInfo.getString("version")
                            apps.downloadLink = appInfo.getString("url")
                            apps.officialLink = appInfo.getString("officialLink")
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

    private fun extractPgToolsFromJson(
        url: String,
        onAppVersionsExtracted: (String, String) -> Unit
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

                val choosePogoVersion = jsonObject.getString("pogoVersion")
                val pgToolsVersion = jsonObject.getString("appName")
                pgToolsUrl = if (pgToolsTestVersion)
                    "https://assets.pgtools.net/test-pgtools-${pgToolsVersion}.apk"
                else
                    "https://assets.pgtools.net/pgtools-${pgToolsVersion}.apk"

                val supportPogoVersions = jsonObject.getJSONObject("supportPogoVersions")
                val pogoVersions = supportPogoVersions.keys()

                pgtoolsVersionsList.clear()
                while (pogoVersions.hasNext()) {
                    val version = pogoVersions.next() as String
                    val pogoData = supportPogoVersions.getJSONObject(version)
                    val versionNumber: Long = pogoData.getJSONArray("pogoVersionCodes").getLong(1)
                    val arm64Url = pogoData.getString("pogoARM64")

                    Log.i(
                        "PgTools",
                        "支援版本: $version\n" +
                                "pogoVersionNumber: $versionNumber\n" +
                                "pogoARM64: $arm64Url"
                    )

                    val newVersionInfo = PogoVersionInfo(
                        version,
                        versionNumber,
                        arm64Url,
                        String.format(
                            getString(R.string.url_poke),
                            version.replace(".", "-"),
                            version.replace(".", "-")
                        )
                    )

                    if (pgtoolsVersionsList.none { it.pogoVersion == newVersionInfo.pogoVersion }) {
                        pgtoolsVersionsList.add(newVersionInfo)
                    }
                }

                launch(Dispatchers.Main) {
                    // Pass the clean, local list back.
                    onAppVersionsExtracted(choosePogoVersion, pgToolsVersion)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val cleanV1 = v1.split(" ")[0]
        val cleanV2 = v2.split(" ")[0]
        val parts1 = cleanV1.split(".")
        val parts2 = cleanV2.split(".")
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
