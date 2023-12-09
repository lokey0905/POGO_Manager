package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import app.lokey0905.location.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class Apps_mhn : Fragment() {
    private var mRewardedAd: RewardedAd? = null

    private var mhnToolsUrl: String = ""
    private var mhnToolsVersion: String = "未安裝"
    private var mhnUrl: String = "未安裝"
    private var mhnVersion: String = "未安裝"

    private var customTabsOff = false

    private var errorTimeAD = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps_mhn, container, false)

        val mhnDownloadButton = view.findViewById<Button>(R.id.download_mhn)
        val mhnRemoveButton = view.findViewById<Button>(R.id.remove_mhn)
        val mhnToolsDownloadButton = view.findViewById<Button>(R.id.download_mhnTools)
        val mhnToolsRemoveButton = view.findViewById<Button>(R.id.remove_mhnTools)
        val gpsDownloadButton = view.findViewById<Button>(R.id.download_gps)
        val gpsRemoveButton = view.findViewById<Button>(R.id.remove_gps)

        fun checkButton() {
            mhnDownloadButton.setOnClickListener {
                downloadAPPWithAd(mhnUrl)
            }

            mhnToolsDownloadButton.setOnClickListener {
                downloadAPPWithAd(mhnToolsUrl)
            }

            gpsDownloadButton.setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_gps64))
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

        val mhnDownloadButton = view.findViewById<Button>(R.id.download_mhn)
        val mhnRemoveButton = view.findViewById<Button>(R.id.remove_mhn)
        val mhnSupportVersion = view.findViewById<TextView>(R.id.mhn_new_version)
        val mhnInstallVersion = view.findViewById<TextView>(R.id.mhn_install_version)
        val mhnToolsDownloadButton = view.findViewById<Button>(R.id.download_mhnTools)
        val mhnToolsRemoveButton = view.findViewById<Button>(R.id.remove_mhnTools)
        val mhnToolsSupportVersion = view.findViewById<TextView>(R.id.mhnTools_new_version)
        val mhnToolsInstallVersion = view.findViewById<TextView>(R.id.mhnTools_install_version)
        val gpsRemoveButton = view.findViewById<Button>(R.id.remove_gps)
        val gpsInstallVersion = view.findViewById<TextView>(R.id.gps_install_version)
        val mhnPackageName = resources.getString(R.string.packageName_MHNow)
        val mhnToolsPackageName = resources.getString(R.string.packageName_mhnTools)
        val gps64PackageName = resources.getString(R.string.packageName_gps64)

        val formatInstallVersion: String = resources.getString(R.string.format_installVersion)

        fun checkAppVersion() {
            mhnRemoveButton.visibility =
                if (appInstalledVersion(mhnPackageName) == "未安裝") View.GONE else View.VISIBLE
            mhnToolsRemoveButton.visibility =
                if (appInstalledVersion(mhnToolsPackageName) == "未安裝") View.GONE else View.VISIBLE
            gpsRemoveButton.visibility =
                if (appInstalledVersion(gps64PackageName) == "未安裝") View.GONE else View.VISIBLE

            mhnInstallVersion.text =
                String.format(formatInstallVersion, appInstalledVersion(mhnPackageName))
            mhnToolsInstallVersion.text =
                String.format(formatInstallVersion, appInstalledVersion(mhnToolsPackageName))
            gpsInstallVersion.text =
                String.format(
                    formatInstallVersion,
                    boolToInstalled(appInstalledOrNot(gps64PackageName))
                )
        }

        fun extractPogoVersionFromJson(
            url: String,
            onPogoVersionExtracted: (String, String) -> Unit,
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

                    val appVersionHash: String
                    val jsonObject = JSONObject(response.toString())
                    mhnVersion = jsonObject.getString("gameVersion")
                    appVersionHash = jsonObject.getString("appVersionHash")
                    mhnToolsUrl =
                        "https://assets.mhntools.net/test-mhntools-$mhnToolsVersion-$appVersionHash.apk?"
                    mhnUrl = jsonObject.getString("gameARM64")
                    mhnToolsVersion = jsonObject.getString("appVersionName")
                    Log.i(
                        "mhnTools",
                        "mhnVersion:$mhnVersion\nmhnToolsUrl:$mhnToolsUrl\nmhnUrl:$mhnUrl\nmhnToolsVersion:$mhnToolsVersion"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                launch(Dispatchers.Main) {
                    onPogoVersionExtracted(mhnVersion, mhnToolsVersion)
                }
            }
        }

        fun getToolsVersion() {
            var url = resources.getString(R.string.url_mhnJson)

            //Snackbar.make(view, "正在取得資料", Snackbar.LENGTH_INDEFINITE).show();
            extractPogoVersionFromJson(url) { mhnVersion, mhnToolsVersion ->
                val versionType = ""
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
                        } else if (currentVersion < installedVersion) {
                            needDowngrade = true
                        }
                    }

                    when {
                        needDowngrade -> {
                            mhnInstallVersion.text =
                                "${mhnInstallVersion.text} ${resources.getString(R.string.versionTooHigh)}"
                            mhnDownloadButton.isEnabled = false

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
                            mhnDownloadButton.text = resources.getString(R.string.update)
                            mhnDownloadButton.isEnabled = true

                            showAlertDialog(
                                resources.getString(R.string.dialogUpdateAvailableTitle),
                                resources.getString(R.string.dialogUpdateAvailablePokMessage)
                            )
                        }

                        else -> {
                            mhnDownloadButton.text = resources.getString(R.string.download)
                            mhnDownloadButton.isEnabled = true
                        }
                    }
                } else {
                    mhnDownloadButton.text = resources.getString(R.string.download)
                    mhnDownloadButton.isEnabled = true
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
                        }
                    }

                    if (needUpdate) {
                        mhnToolsDownloadButton.text = resources.getString(R.string.update)
                    } else {
                        mhnToolsDownloadButton.text = resources.getString(R.string.download)
                    }
                } else {
                    mhnToolsDownloadButton.text = resources.getString(R.string.download)
                }
            }
        }

        fun setFragmentResultListener() {
            setFragmentResultListener("customTabsOff") { _, bundle ->
                customTabsOff = bundle.getBoolean("bundleKey")
            }
        }

        checkAppVersion()
        getToolsVersion()
        setFragmentResultListener()
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

    private fun appInstalledVersion(PackageName: String): String {
        val pm = activity?.packageManager
        try {
            pm?.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)
            return pm?.getPackageInfo(
                PackageName,
                PackageManager.GET_ACTIVITIES
            )?.versionName.toString()
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return "未安裝"
    }

    private fun appUnInstall(PackageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$PackageName")
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