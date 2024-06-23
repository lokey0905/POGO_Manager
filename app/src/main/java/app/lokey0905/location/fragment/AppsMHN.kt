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
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.databinding.ActivityMainBinding
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AppsMHN : Fragment() {
    private lateinit var binding: ActivityMainBinding

    private var mRewardedAd: RewardedAd? = null

    private val gameVersionsMap = mutableMapOf<String, String>()
    private var mhnToolsVersion: String = "未安裝"
    private var mhnToolsUrl: String = ""
    private var mhnToolsHash = ""
    private var mhnUrl: String = ""
    private var mhnVersion: String = "未安裝"

    private var mhnTestVersion = false

    private var url_jokstick = ""

    private var mhnToolsCheckDone = false
    private var appListCheckDone = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_mhn, container, false)

        setupListeners(view)

        // Inflate the layout for this fragment
        return view
    }

    @SuppressLint("CutPasteId", "SetTextI18n")
    override fun onResume() {
        super.onResume()
        val view: View = requireView()

        setupAppVersionInfo(view)
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
        val mhnTestVersionSwitch = view.findViewById<MaterialSwitch>(R.id.mhnTestVersion_switch)

        mhnDownloadButton.setOnClickListener {
            downloadAPPWithCheck(mhnUrl)
        }

        mhnToolsDownloadButton.setOnClickListener {
            downloadAPPWithCheck(mhnToolsUrl)
        }

        gpsDownloadButton.setOnClickListener {
            downloadAPPWithCheck(url_jokstick)
        }

        hylianerDownloadButton.setOnClickListener {
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
        val formatInstallVersion = resources.getString(R.string.format_installVersion)
        val formatNewerVersion = resources.getString(R.string.format_newerVersion)

        val mhnSupportVersion = view.findViewById<TextView>(R.id.mhn_new_version)
        val mhnToolsSupportVersion = view.findViewById<TextView>(R.id.mhnTools_new_version)
        val hylianerSupportVersion = view.findViewById<TextView>(R.id.hylianer_new_version)

        val spinner = view.findViewById<Spinner>(R.id.mhn_spinner)

        fun checkAppVersion() {
            appListCheckDone = false
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

            val url = resources.getString(R.string.url_appInfo)
            extractAppVersionsFromJson(url) {
                appListCheckDone = true
                if (mhnToolsCheckDone)
                    view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout).isRefreshing =
                        false
            }

            mhnInstallVersion.text =
                String.format(formatInstallVersion, appInstalledVersion(mhnPackageName))
            mhnToolsInstallVersion.text =
                String.format(formatInstallVersion, appInstalledVersion(mhnToolsPackageName))
            gpsInstallVersion.text =
                String.format(
                    formatInstallVersion,
                    boolToInstalled(appInstalledOrNot(gps64PackageName))
                )
            hylianerInstallVersion.text =
                String.format(
                    formatInstallVersion,
                    boolToInstalled(appInstalledOrNot(hylianerPackageName))
                )

            fun setDownloadButton(isUpdate: Boolean = false) {
                mhnDownloadButton.text = if (isUpdate) update else download
                if (!mhnDownloadButton.isEnabled)
                    mhnDownloadButton.isEnabled = true
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

                        MaterialAlertDialogBuilder(requireContext())
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
                        setDownloadButton(true)
                        needUpdateAppsAmount++
                        showAlertDialog(
                            resources.getString(R.string.dialogUpdateAvailableTitle),
                            resources.getString(R.string.dialogUpdateAvailablePokMessage)
                        )
                    }

                    else -> {
                        setDownloadButton()
                    }
                }
            } else {
                setDownloadButton()
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
                    mhnToolsDownloadButton.text = update
                    needUpdateAppsAmount++
                } else {
                    mhnToolsDownloadButton.text = download
                }
            } else {
                mhnToolsDownloadButton.text = download
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
                ).subtitle = getString(R.string.allUpdated)
            }
        }

        fun getMHNToolsVersion() {
            mhnToolsCheckDone = false
            var url = resources.getString(R.string.url_mhnJson)
            if (mhnTestVersion)
                url = resources.getString(R.string.url_mhnJsonTest)
            val versionType =
                if (mhnTestVersion) " (${resources.getString(R.string.testVersion)})" else ""

            extractMHNToolsFromJson(url) { mhnVersion, mhnToolsVersion, gameVersionsMap ->
                val versionsList = ArrayList<String>()
                var mhnVersionList = resources.getString(R.string.appsMHNPage_supportVersion_MHNTools)

                for ((version, _) in gameVersionsMap) {
                    versionsList.add(version)
                    Log.i(
                        "mhnTools",
                        "MHN支援版本: $version\n"
                    )
                    mhnVersionList += " $version,"
                }

                mhnVersionList = if(mhnTestVersion) {
                    mhnVersionList.substring(0, mhnVersionList.length - 1) + " (${getText(R.string.testVersion)})"

                } else {
                    mhnVersionList.substring(0, mhnVersionList.length - 1)
                }

                view.findViewById<TextView>(R.id.supportVersion_MHNTools).text = mhnVersionList

                versionsList.reverse()

                val adapter: ArrayAdapter<String> = ArrayAdapter(
                    view.context,
                    android.R.layout.simple_spinner_item,
                    versionsList
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                spinner.setSelection(0)

                mhnSupportVersion.text = String.format(
                    formatNewerVersion,
                    mhnVersion,
                    versionType
                )
                mhnToolsSupportVersion.text = String.format(
                    formatNewerVersion,
                    mhnToolsVersion,
                    versionType
                )
                hylianerSupportVersion.text = String.format(
                    formatNewerVersion,
                    "未知",
                    ""
                )

                mhnToolsCheckDone = true
                if (appListCheckDone)
                    view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout).isRefreshing =
                        false
            }
        }

        fun setOnCheckedChangeListener() {
            val mhnTestVersionSwitch = view.findViewById<MaterialSwitch>(R.id.mhnTestVersion_switch)

            mhnTestVersionSwitch.setOnCheckedChangeListener { _, isChecked ->
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
                            formatNewerVersion,
                            version,
                            if (mhnTestVersion)
                                "(${getText(R.string.testVersion)})"
                            else
                                ""
                        )
                    }

                    checkAppVersion()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        checkAppVersion()
        getMHNToolsVersion()
        setOnCheckedChangeListener()
    }

    private fun extractAppVersionsFromJson(url: String, onAppVersionsExtracted: () -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {

            try {
                val url = URL(url)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
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

                val mhn = jsonObject.getJSONObject("mhn")
                val jokstick = mhn.getJSONObject("jokstick")


                url_jokstick = jokstick.getString("url")

                launch(Dispatchers.Main) {
                    onAppVersionsExtracted()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractMHNToolsFromJson(
        url: String,
        onPogoVersionExtracted: (String, String, MutableMap<String, String>) -> Unit,
    ) {
        GlobalScope.launch(Dispatchers.IO) {

            try {
                val url = URL(url)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
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

                mhnVersion = jsonObject.getString("gameVersion")
                mhnUrl = jsonObject.getString("gameARM64")
                mhnToolsVersion = jsonObject.getString("appVersionName")
                mhnToolsHash = jsonObject.getString("appVersionHash")
                mhnToolsUrl = if (mhnTestVersion)
                    "https://assets.mhntools.net/test-mhntools-$mhnToolsVersion-$mhnToolsHash.apk?"
                else
                    "https://assets.mhntools.net/mhntools-$mhnToolsVersion-$mhnToolsHash.apk?"

                gameVersionsMap.clear()

                val supportGameVersions = jsonObject.getJSONObject("supportGameVersions")
                val gameVersions = supportGameVersions.keys()

                while (gameVersions.hasNext()) {
                    val gameVersion = gameVersions.next() as String
                    val gameData = supportGameVersions.optJSONObject(gameVersion)
                    val version = gameData?.optString("gameVersion", "")
                    val arm64Url = gameData?.optString("gameARM64", "")

                    if (version != null && arm64Url != null) {
                        gameVersionsMap[version] = arm64Url

                        Log.i(
                            "mhnTools",
                            "mhnVersion: $version\nmhnUrl: $arm64Url\n"
                        )
                    } else {
                        Log.e(
                            "mhnTools",
                            "Invalid data for game version: $gameVersion"
                        )
                    }
                }

                launch(Dispatchers.Main) {
                    onPogoVersionExtracted(mhnVersion, mhnToolsVersion, gameVersionsMap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                                    getString(androidx.compose.ui.R.string.default_error_message),
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
            "已安裝"
        else
            "未安裝"
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
        val pm = activity?.packageManager
        try {
            pm?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return pm?.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES
            )?.versionName.toString()
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return "未安裝"
    }

    private fun appUnInstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
}