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
import androidx.fragment.app.FragmentActivity
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
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class Apps : Fragment() {
    private var mRewardedAd: RewardedAd? = null

    private var pgtoolsUrlARM: String = ""
    private var pgtoolsUrlARM64: String = ""
    private var pgtoolsVersion: String = "未安裝"
    private var pogoVersion: String = "未安裝"

    private var testPgtools = false
    var pokAresNoSupportDevices = false
    var pokAresDownloadAPK = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps, container, false)

        fun checkButton(){
            view.findViewById<Button>(R.id.download_gpx).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_gpx))
            }
            //******download*********//
            view.findViewById<Button>(R.id.download_gps).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_gps64))
            }

            view.findViewById<Button>(R.id.download_wrapper).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_wrapper))
            }

            view.findViewById<Button>(R.id.download_polygon).setOnClickListener { view->
                if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                    downloadAPPWithAd(resources.getString(R.string.url_polygon))
                else {
                    Snackbar.make(view, "${resources.getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
            }

            view.findViewById<Button>(R.id.download_pgtools).setOnClickListener { view->
                if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                    if(testPgtools)
                        downloadAPPWithAd("https://assets.pgtools.net/test-pgtools-${pgtoolsVersion}.apk")
                    else
                        downloadAPPWithAd("https://assets.pgtools.net/pgtools-${pgtoolsVersion}.apk")
                else
                    Snackbar.make(view, "${resources.getString(R.string.unsupportedDevices)}(${Build.SUPPORTED_ABIS[0]})", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
            }

            view.findViewById<Button>(R.id.download_pok).setOnClickListener {
                if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                    downloadAPPWithAd(pgtoolsUrlARM64)
                else
                    downloadAPPWithAd(pgtoolsUrlARM)
            }

            view.findViewById<Button>(R.id.download_pokAres).setOnClickListener { view->
                if(pokAresDownloadAPK)
                    downloadAPPWithAd(resources.getString(R.string.url_pokAres))
                else if(MANUFACTURER=="samsung")
                    downloadAPPWithAd(resources.getString(R.string.url_pokAres_store))
                else if(pokAresNoSupportDevices)
                    Toast.makeText(context, "請在設定中啟用\"一律下載三星版APK\"以繼續", Toast.LENGTH_LONG).show()
                else
                    Snackbar.make(view, resources.getString(R.string.unsupportedDevices), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
            }

            view.findViewById<Button>(R.id.download_pokelist).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_pokelist))
            }

            view.findViewById<Button>(R.id.download_wecatch).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_wecatch))
            }

            //******remove*********//
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

        fun setupAd(){
            MobileAds.initialize(requireActivity())
            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(requireActivity(), resources.getString(R.string.adID_Rewarded), adRequest, object : RewardedAdLoadCallback() {
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

            mRewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
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

        val actManager = activity?.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMemory= (memInfo.totalMem.toDouble()/(1024*1024*1024)).roundToInt()

        if(totalMemory >= 5 && MANUFACTURER=="samsung")
            view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.VISIBLE
        else
            view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.GONE

        setupAd()
        checkButton()

        // Inflate the layout for this fragment
        return view
    }

    @SuppressLint("CutPasteId", "SetTextI18n")
    override fun onResume(){
        super.onResume()
        val view: View = requireView()

        fun checkAppVersion(){
            view.findViewById<TextView>(R.id.polygon_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_polygon))))
            view.findViewById<TextView>(R.id.pgtools_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_pgtools))))
            view.findViewById<TextView>(R.id.pok_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_pok))))
            view.findViewById<TextView>(R.id.pokAres_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_pokAres))+
                            if(MANUFACTURER =="samsung"||pokAresNoSupportDevices) {""} else {"(不支援)"}))
            view.findViewById<TextView>(R.id.gps_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    boolToInstalled(appInstalledOrNot(resources.getString(R.string.packageName_gps)))))
            view.findViewById<TextView>(R.id.pokelist_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_PokeList))))
            view.findViewById<TextView>(R.id.wecatch_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_WeCatch))))
            view.findViewById<TextView>(R.id.wrapper_install_version).text =
                String.format(resources.getString(R.string.format_installVersion,
                    appInstalledVersion(resources.getString(R.string.packageName_wrapper))))
        }

        fun extractPogoVersionFromJson(url: String, onPogoVersionExtracted: (String,String) -> Unit) {
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
                    Log.i("auto_catch","pogoVersion:$pogoVersion\npgtoolsVersion:$pgtoolsVersion\npgtoolsUrlARM:$pgtoolsUrlARM\npgtoolsUrlARM64:$pgtoolsUrlARM64")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                launch(Dispatchers.Main) {
                    onPogoVersionExtracted(pogoVersion,pgtoolsVersion)
                }
            }
        }

        fun getAutoVersion(){
            var url = resources.getString(R.string.url_pgtoolsJson)
            if(testPgtools)
                url = resources.getString(R.string.url_pgtoolsJsonTest)

            //Snackbar.make(view, "正在取得資料", Snackbar.LENGTH_INDEFINITE).show();
            extractPogoVersionFromJson(url) { pogoVersion, pgtoolsVersion ->
                val versionType = if (testPgtools) " ${resources.getString(R.string.versionTest)}" else ""
                view.findViewById<TextView>(R.id.pok_new_version).text =
                    String.format(resources.getString(R.string.format_newerVersion),pogoVersion,versionType)
                view.findViewById<TextView>(R.id.pokAres_new_version).text =
                    String.format(resources.getString(R.string.format_newerVersion),pogoVersion,"")
                view.findViewById<TextView>(R.id.pgtools_new_version).text =
                    String.format(resources.getString(R.string.format_newerVersion),pgtoolsVersion,versionType)

                val pokInstalledVersion = appInstalledVersion(resources.getString(R.string.packageName_pok))
                val pgtoolsInstalledVersion = appInstalledVersion(resources.getString(R.string.packageName_pgtools))
                if (pogoVersion != "未安裝" && pokInstalledVersion != "未安裝") {
                    val pogoVersionFloat = pogoVersion.substringAfter("0.").toFloat()
                    val pokInstalledVersionFloat = pokInstalledVersion.substringAfter("0.").toFloat()

                    when {
                        pogoVersionFloat < pokInstalledVersionFloat -> {
                            view.findViewById<TextView>(R.id.pok_install_version).text =
                                "${view.findViewById<TextView>(R.id.pok_install_version).text} ${resources.getString(R.string.versionTooHigh)}"
                            view.findViewById<Button>(R.id.download_pok).isEnabled = false

                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(resources.getString(R.string.dialogVersionTooHighTitle))
                                .setMessage(resources.getString(R.string.dialogVersionTooHighMessage))
                                .setNeutralButton(R.string.ok) { _, _ -> }
                                .setNegativeButton("如何降版") { _, _ ->
                                    showAlertDialog(resources.getString(R.string.dialogVersionTooHighTitle),
                                        "1. 請先移除較新版本寶可夢 \n2. 重新下載支援版本寶可夢 \n3. 重啟手機嘗試啟動")}
                                .setPositiveButton("使用測試版") { _, _ ->
                                    showAlertDialog(resources.getString(R.string.dialogVersionTooHighTitle),
                                        "1. 至設定打開測試版自動抓開關 \n2. 直接下載支援版本寶可夢 \n3. 重啟手機嘗試啟動")}
                                .show()
                        }
                        pogoVersionFloat > pokInstalledVersionFloat -> {
                            view.findViewById<Button>(R.id.download_pok).text = resources.getString(R.string.update)
                            view.findViewById<Button>(R.id.download_pok).isEnabled = true

                            showAlertDialog(
                                resources.getString(R.string.dialogUpdateAvailableTitle),
                                resources.getString(R.string.dialogUpdateAvailablePokMessage)
                            )
                        }
                        else -> {
                            view.findViewById<Button>(R.id.download_pok).text = resources.getString(R.string.download)
                            view.findViewById<Button>(R.id.download_pok).isEnabled = true
                        }
                    }
                }

                if (pgtoolsVersion != "未安裝" && pgtoolsInstalledVersion != "未安裝") {
                    val pgtoolsVersionInt = pgtoolsVersion.replace(".", "").toInt()
                    val pgtoolsInstalledVersionInt = pgtoolsInstalledVersion.replace(".", "").toInt()

                    if (pgtoolsVersionInt > pgtoolsInstalledVersionInt) {
                        view.findViewById<Button>(R.id.download_pgtools).text = resources.getString(R.string.update)
                    } else {
                        view.findViewById<Button>(R.id.download_pgtools).text = resources.getString(R.string.download)
                    }
                }

                //Snackbar.make(requireActivity().findViewById(android.R.id.content), "完成", 500).show();
            }
        }

        fun setFragmentResultListener(){
            setFragmentResultListener("testPgtools") { _, bundle ->
                testPgtools = bundle.getBoolean("bundleKey")
                checkAppVersion()
                getAutoVersion()
            }

            setFragmentResultListener("pokAresNoSupportDevices") { _, bundle ->
                pokAresNoSupportDevices = bundle.getBoolean("bundleKey")
                checkAppVersion()
                if((pokAresNoSupportDevices))
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.VISIBLE
                else if(MANUFACTURER!="samsung")
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.GONE
            }

            setFragmentResultListener("pokAresDownloadAPK") { _, bundle ->
                pokAresDownloadAPK = bundle.getBoolean("bundleKey")
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
    private fun gotoBrowser(url: String){
        context?.let {
            CustomTabsIntent.Builder().build()
                .launchUrl(it, Uri.parse(url))
        }
    }

    private fun downloadAPPSetup(url: String){
        if (mRewardedAd != null) {
            Toast.makeText(context, "感謝您的耐心等候：）", Toast.LENGTH_LONG).show()
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
        }
        else {
            Log.d(ContentValues.TAG, "The rewarded ad wasn't ready yet.")
            showAlertDialog(
                resources.getString(R.string.dialogAdNotReadyTitle),
                resources.getString(R.string.dialogAdNotReadyMessage)
            )
            //Toast.makeText(context, "網路錯誤 請5秒後在試", Toast.LENGTH_LONG).show()
        }
    }

    private fun downloadAPPWithAd(url: String){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.dialogDownloadTitle))
            .setMessage(resources.getString(R.string.dialogDownloadMessage))
            .apply {
                setNeutralButton(R.string.ok) { _, _ ->
                    downloadAPPSetup(url)
                }
                setNegativeButton(R.string.cancel) { _, _ ->
                    Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT).show()
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

    private fun appInstalledOrNot(PackageName: String): Boolean {
        val pm = activity?.packageManager
        try {
            pm?.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return false
    }

    private fun appInstalledVersion(PackageName: String): String {
        val pm = activity?.packageManager
        try {
            pm?.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)
            return pm?.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)?.versionName.toString()
        } catch (_: PackageManager.NameNotFoundException) {}
        return "未安裝"
    }

    private fun appUnInstall(PackageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$PackageName")
        startActivity(intent)
    }

    private fun loadAd(){
        if (mRewardedAd == null) {
            val adRequest = AdRequest.Builder().build()

            context?.let {
                RewardedAd.load(
                    it,
                    resources.getString(R.string.adID_Rewarded),
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.d(ContentValues.TAG, adError.message)
                            mRewardedAd = null
                            Toast.makeText(context, resources.getString(R.string.dialogAdNotReadyMessage), Toast.LENGTH_LONG).show()
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