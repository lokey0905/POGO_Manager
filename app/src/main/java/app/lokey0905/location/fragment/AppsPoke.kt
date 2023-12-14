package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.MANUFACTURER
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import app.lokey0905.location.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class AppsPoke : Fragment() {
    private var mRewardedAd: RewardedAd? = null

    private var pgtoolsUrlARM: String = ""
    private var pgtoolsUrlARM64: String = ""
    private var pgtoolsVersion: String = "未安裝"
    private var pogoVersion: String = "未安裝"

    private var testPgtools = false
    private var pokAresNoSupportDevices = false
    private var pokAresDownloadAPK = false
    private var customTabsOff = false

    private var errorTimeAD = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_poke, container, false)

        fun checkButton() {
            //******download*********//
            view.findViewById<Button>(R.id.download_gps).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_gps32))
            }

            view.findViewById<Button>(R.id.download_wrapper).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_wrapper))
            }

            view.findViewById<Button>(R.id.download_polygon).setOnClickListener { view ->
                if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                    downloadAPPWithAd(resources.getString(R.string.url_polygon))
                else {
                    Snackbar.make(
                        view,
                        "${resources.getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Action", null).show()
                }
            }

            view.findViewById<Button>(R.id.download_pgtools).setOnClickListener { view ->
                if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                    if (testPgtools)
                        downloadAPPWithAd("https://assets.pgtools.net/test-pgtools-${pgtoolsVersion}.apk")
                    else
                        downloadAPPWithAd("https://assets.pgtools.net/pgtools-${pgtoolsVersion}.apk")
                else
                    Snackbar.make(
                        view,
                        "${resources.getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Action", null).show()
            }

            view.findViewById<Button>(R.id.download_pok).setOnClickListener {
                if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                    downloadAPPWithAd(pgtoolsUrlARM64)
                else
                    downloadAPPWithAd(pgtoolsUrlARM)
            }

            view.findViewById<Button>(R.id.download_pokAres).setOnClickListener { view ->
                if (pokAresDownloadAPK || pokAresNoSupportDevices)
                    downloadAPPWithAd(resources.getString(R.string.url_pokAres))
                else if (MANUFACTURER == "samsung")
                    downloadAPPWithAd(resources.getString(R.string.url_pokAres_store))
                else
                    Snackbar.make(
                        view,
                        resources.getString(R.string.unsupportedDevices),
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Action", null).show()
            }

            view.findViewById<Button>(R.id.download_pokelist).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_pokelist))
            }

            view.findViewById<Button>(R.id.download_wecatch).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_wecatch))
            }

            //******remove*********//
            view.findViewById<Button>(R.id.remove_gps).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_gps32))
            }

            view.findViewById<Button>(R.id.remove_wrapper).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_wrapper))
            }

            view.findViewById<Button>(R.id.remove_polygon).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_polygon))
            }

            view.findViewById<Button>(R.id.remove_pgtools).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_pgtools))
            }

            view.findViewById<Button>(R.id.remove_pok).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_pok))
            }

            view.findViewById<Button>(R.id.remove_pokAres).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_pokAres))
            }

            view.findViewById<Button>(R.id.remove_pokelist).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_PokeList))
            }

            view.findViewById<Button>(R.id.remove_wecatch).setOnClickListener {
                appUnInstall(resources.getString(R.string.packageName_WeCatch))
            }
        }

        fun setupAd() {
            MobileAds.initialize(requireActivity())
            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                requireActivity(),
                resources.getString(R.string.adR),
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(ContentValues.TAG, adError.toString())
                        mRewardedAd = null
                        //Toast.makeText(applicationContext, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show();
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(ContentValues.TAG, "Ad was loaded.")
                        mRewardedAd = rewardedAd
                    }
                })

            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(ContentValues.TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(ContentValues.TAG, "Ad dismissed fullscreen content.")
                    //mRewardedAd = null
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    Log.e(ContentValues.TAG, "Ad failed to show fullscreen content.")
                    //mRewardedAd = null
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(ContentValues.TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(ContentValues.TAG, "Ad showed fullscreen content.")
                }
            }
        }

        val actManager =
            activity?.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMemory= (memInfo.totalMem.toDouble()/(1024*1024*1024)).roundToInt()

        if (totalMemory >= 5 && MANUFACTURER == "samsung")
            view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.VISIBLE
        else
            view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.GONE

        setupAd()
        checkButton()

        // Inflate the layout for this fragment
        return view
    }

    @SuppressLint("CutPasteId", "SetTextI18n")
    override fun onResume() {
        super.onResume()
        val view: View = requireView()

        val formatNewerVersion:String = resources.getString(R.string.format_newerVersion)

        val pokePackageName = resources.getString(R.string.packageName_pok)
        val pgToolsPackageName = resources.getString(R.string.packageName_pgtools)
        val pokeDownloadButton = view.findViewById<Button>(R.id.download_pok)
        val pgToolsDownloadButton = view.findViewById<Button>(R.id.download_pgtools)

        fun checkAppVersion() {
            view.findViewById<TextView>(R.id.remove_gps).visibility =
                if (appInstalledVersion(resources.getString(R.string.packageName_gps32)) == "未安裝") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.remove_polygon).visibility =
                if (appInstalledVersion(resources.getString(R.string.packageName_polygon)) == "未安裝") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.remove_pgtools).visibility =
                if (appInstalledVersion(pgToolsPackageName) == "未安裝") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.remove_pok).visibility =
                if (appInstalledVersion(pokePackageName) == "未安裝") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.remove_pokAres).visibility =
                if (appInstalledVersion(resources.getString(R.string.packageName_pokAres)) == "未安裝") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.remove_pokelist).visibility =
                if (appInstalledVersion(resources.getString(R.string.packageName_PokeList)) == "未安裝") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.remove_wecatch).visibility =
                if (appInstalledVersion(resources.getString(R.string.packageName_WeCatch)) == "未安裝") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.remove_wrapper).visibility =
                if (appInstalledVersion(resources.getString(R.string.packageName_wrapper)) == "未安裝") View.GONE else View.VISIBLE

            view.findViewById<TextView>(R.id.wrapper_new_version).text =
                String.format(
                    formatNewerVersion,
                    resources.getString(R.string.version_wrapper),
                    ""
                )
            view.findViewById<TextView>(R.id.polygon_new_version).text =
                String.format(
                    formatNewerVersion,
                    resources.getString(R.string.version_polygon),
                    ""
                )
            view.findViewById<TextView>(R.id.pokelist_new_version).text =
                String.format(
                    formatNewerVersion,
                    resources.getString(R.string.version_pokelist),
                    ""
                )
            view.findViewById<TextView>(R.id.wecatch_new_version).text =
                String.format(
                    formatNewerVersion,
                    resources.getString(R.string.version_wecatch),
                    ""
                )

            val formatInstallVersion: String = resources.getString(R.string.format_installVersion)

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
                    formatInstallVersion,
                    appInstalledVersion(pokePackageName)
                )
            view.findViewById<TextView>(R.id.pokAres_install_version).text =
                String.format(
                    formatInstallVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_pokAres)) +
                            if (MANUFACTURER == "samsung" || pokAresNoSupportDevices) ""
                            else "(不支援)"
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
        }

        val pokInstalledVersion = appInstalledVersion(pokePackageName)
        val pgToolsInstalledVersion = appInstalledVersion(pgToolsPackageName)
        val download = resources.getString(R.string.download)
        val update = resources.getString(R.string.update)

        fun extractPogoVersionFromJson(
            url: String,
            onPogoVersionExtracted: (String, String) -> Unit
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
                    pogoVersion = jsonObject.getString("pogoVersion")
                    pgtoolsUrlARM = jsonObject.getString("pogoARM")
                    pgtoolsUrlARM64 = jsonObject.getString("pogoARM64")
                    pgtoolsVersion = jsonObject.getString("appName")
                    Log.i(
                        "auto_catch",
                        "pogoVersion:$pogoVersion\npgtoolsVersion:$pgtoolsVersion\npgtoolsUrlARM:$pgtoolsUrlARM\npgtoolsUrlARM64:$pgtoolsUrlARM64"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                launch(Dispatchers.Main) {
                    onPogoVersionExtracted(pogoVersion, pgtoolsVersion)
                }
            }
        }

        fun getAutoVersion() {
            var url = resources.getString(R.string.url_pgtoolsJson)
            if (testPgtools)
                url = resources.getString(R.string.url_pgtoolsJsonTest)

            //Snackbar.make(view, "正在取得資料", Snackbar.LENGTH_INDEFINITE).show();
            extractPogoVersionFromJson(url) { pogoVersion, pgtoolsVersion ->
                val versionType =
                    if (testPgtools) " ${resources.getString(R.string.versionTest)}" else ""
                view.findViewById<TextView>(R.id.pok_new_version).text =
                    String.format(
                        formatNewerVersion,
                        pogoVersion,
                        versionType
                    )
                view.findViewById<TextView>(R.id.pokAres_new_version).text =
                    String.format(
                        formatNewerVersion,
                        pogoVersion,
                        ""
                    )
                view.findViewById<TextView>(R.id.pgtools_new_version).text =
                    String.format(
                        formatNewerVersion,
                        pgtoolsVersion,
                        versionType
                    )

                fun setDownloadButton(isUpdate:Boolean = false) {
                    pokeDownloadButton.text = if(isUpdate) update else download
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
                                "${view.findViewById<TextView>(R.id.pok_install_version).text} ${
                                    resources.getString(
                                        R.string.versionTooHigh
                                    )
                                }"
                            if (pokeDownloadButton.isEnabled)
                                pokeDownloadButton.isEnabled = false

                            MaterialAlertDialogBuilder(requireContext())
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
                                    showAlertDialog(
                                        resources.getString(R.string.dialogVersionTooHighTitle),
                                        "1. 至設定打開測試版自動抓開關 \n2. 直接下載支援版本寶可夢 \n3. 重啟手機嘗試啟動"
                                    )
                                }
                                .show()
                        }

                        needUpdate -> {
                            setDownloadButton(true)

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

                if (pgtoolsVersion != "未安裝" && pgToolsInstalledVersion != "未安裝") {
                    val pgToolsVersionInt: List<String> = pgtoolsVersion.split(".")
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
                    } else {
                        pgToolsDownloadButton.text = download
                    }
                } else {
                    pgToolsDownloadButton.text = download
                }
            }
        }

        fun setFragmentResultListener() {
            setFragmentResultListener("testPgtools") { _, bundle ->
                testPgtools = bundle.getBoolean("bundleKey")
                checkAppVersion()
                getAutoVersion()
            }

            setFragmentResultListener("pokAresNoSupportDevices") { _, bundle ->
                pokAresNoSupportDevices = bundle.getBoolean("bundleKey")
                if ((pokAresNoSupportDevices))
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility =
                        View.VISIBLE
                else if (MANUFACTURER != "samsung")
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility =
                        View.GONE
                checkAppVersion()
            }

            setFragmentResultListener("pokAresDownloadAPK") { _, bundle ->
                pokAresDownloadAPK = bundle.getBoolean("bundleKey")
            }

            setFragmentResultListener("customTabsOff") { _, bundle ->
                customTabsOff = bundle.getBoolean("bundleKey")
            }
        }

        checkAppVersion()
        getAutoVersion()
        setFragmentResultListener()
        //Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show()
    }


    private fun showAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(R.string.ok) { _, _ -> }
            .show()
    }

    private fun gotoBrowser(url: String) {
        context?.let {
            if (customTabsOff)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else
                CustomTabsIntent.Builder().build()
                    .launchUrl(it, Uri.parse(url))
        }
    }

    private fun downloadAPPSetup(url: String) {
        if (mRewardedAd != null) {
            errorTimeAD = 0
            Toast.makeText(context, getString(R.string.thanksForWaiting), Toast.LENGTH_LONG).show()
            mRewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(ContentValues.TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null
                        loadAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(ContentValues.TAG, "Ad failed to show.")
                        Toast.makeText(context, "播放失敗 請稍後在試", Toast.LENGTH_LONG).show()
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(ContentValues.TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }
            mRewardedAd?.show(requireActivity()) {
                loadAd()
                mRewardedAd = null

                gotoBrowser(url)
            }
        } else {
            Log.d(ContentValues.TAG, "The rewarded ad wasn't ready yet.")
            showAlertDialog(
                resources.getString(R.string.dialogAdNotReadyTitle),
                resources.getString(R.string.dialogAdNotReadyMessage)
            )
            errorTimeAD++
            if (errorTimeAD > 3) {
                errorTimeAD = 0
                gotoBrowser(url)
            }
            //Toast.makeText(context, "網路錯誤 請5秒後在試", Toast.LENGTH_LONG).show()
        }
    }

    private fun downloadAPPWithAd(url: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.dialogDownloadTitle))
            .setMessage(resources.getString(R.string.dialogDownloadMessage))
            .apply {
                setNeutralButton(R.string.ok) { _, _ ->
                    downloadAPPSetup(url)
                }
                setNegativeButton(R.string.cancel) { _, _ ->
                    Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT)
                        .show()
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

    private fun loadAd() {
        if (mRewardedAd == null) {
            val adRequest = AdRequest.Builder().build()

            context?.let {
                RewardedAd.load(
                    it,
                    resources.getString(R.string.adR),
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.d(ContentValues.TAG, adError.message)
                            mRewardedAd = null
                            Toast.makeText(
                                context,
                                resources.getString(R.string.dialogAdNotReadyMessage),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            Log.d(ContentValues.TAG, "Ad was loaded.")
                            mRewardedAd = rewardedAd
                        }
                    }
                )
            }
        }
    }
}