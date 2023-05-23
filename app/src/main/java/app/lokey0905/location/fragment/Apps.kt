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
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import org.jsoup.Jsoup
import kotlin.math.roundToInt

class Apps : Fragment() {
    private var mRewardedAd: RewardedAd? = null

    var url_armautocatchDownload: String = ""
    var url_arm64autocatchDownload: String = ""
    var appVersion_autovatch: String = "未安裝"
    private var pogoVersion: String = "未安裝"
    private var pogoVersionCodes: Array<String> = arrayOf()

    var testPgtools = false
    var pokAresNoSupportDevices = false
    var pokAresDownloadAPK = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun checkAppVersion(view: FragmentActivity){
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
    }

    @SuppressLint("SetTextI18n")
    fun getAutoVersion(view: FragmentActivity){
        var url = resources.getString(R.string.url_autoJson)
        if(testPgtools)
            url = resources.getString(R.string.url_autoTestJson)
        val gfgThread = Thread {
            try {
                val json = Jsoup.connect(url).timeout(10000).ignoreContentType(true).execute().body()
                val jsonObject = JSONObject(json)
                url_armautocatchDownload = jsonObject.get("pogoARM").toString()
                url_arm64autocatchDownload = jsonObject.get("pogoARM64").toString()
                appVersion_autovatch = jsonObject.get("appName").toString()
                pogoVersion = jsonObject.get("pogoVersion").toString()
                pogoVersionCodes = arrayOf(jsonObject.get("pogoVersionCodes").toString())
                activity?.runOnUiThread {
                    view.findViewById<TextView>(R.id.pok_new_version).text =
                        String.format(resources.getString(R.string.format_newerVersion),pogoVersion) + if(testPgtools){"(測試版)"} else {""}
                    view.findViewById<TextView>(R.id.pokAres_new_version).text =
                        String.format(resources.getString(R.string.format_newerVersion),pogoVersion)
                    view.findViewById<TextView>(R.id.pgtools_new_version).text =
                        String.format(resources.getString(R.string.format_newerVersion),appVersion_autovatch) + if(testPgtools){"(測試版)"} else {""}

                    if(pogoVersion!="未安裝" && appInstalledVersion(resources.getString(R.string.packageName_pok))!="未安裝"){
                        if(pogoVersion.replace(".","").toInt() <
                            appInstalledVersion(resources.getString(R.string.packageName_pok)).replace(".","").toInt()){
                            /*view.findViewById<TextView>(R.id.pok_install_version).setTextColor(
                                ContextCompat.getColor(
                                    this,com.google.android.material.R.color.design_default_color_error))*/
                            view.findViewById<TextView>(R.id.pok_install_version).text =
                                "${ view.findViewById<TextView>(R.id.pok_install_version).text} (版本過高)"
                            view.findViewById<Button>(R.id.download_pok).isEnabled = false
                        } else if(pogoVersion.replace(".","").toInt() >
                            appInstalledVersion(resources.getString(R.string.packageName_pok)).replace(".","").toInt()) {
                            view.findViewById<Button>(R.id.download_pok).text = resources.getString(R.string.update)
                            view.findViewById<Button>(R.id.download_pok).isEnabled = true
                        } else{
                            view.findViewById<Button>(R.id.download_pok).text = resources.getString(R.string.download)
                            view.findViewById<Button>(R.id.download_pok).isEnabled = true
                        }
                    }
                    if(appVersion_autovatch!="未安裝" && appInstalledVersion(resources.getString(R.string.packageName_pgtools))!="未安裝"){
                        if(appVersion_autovatch.replace(".","").toInt() >
                            appInstalledVersion(resources.getString(R.string.packageName_pgtools)).replace(".","").toInt()){
                            view.findViewById<Button>(R.id.download_pgtools).text = resources.getString(R.string.update)
                        } else{
                            view.findViewById<Button>(R.id.download_pgtools).text = resources.getString(R.string.download)
                        }
                    }
                }
                Log.i("auto_catch",json)
            } catch (e:Exception) {
                Log.i("auto_catch", e.toString())
            }
        }
        gfgThread.start()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps, container, false)

        fun gotoBrowser(url: String){
            context?.let {
                CustomTabsIntent.Builder().build()
                    .launchUrl(it, Uri.parse(url))
            }
        }

        fun downloadAPPSetup(url: String){
            //Toast.makeText(context, "請手動點擊下載Download APK", Toast.LENGTH_LONG).show()
            gotoBrowser(url)
            //Toast.makeText(context, "下載完成後在點安裝APK", Toast.LENGTH_LONG).show()
        }

        fun downloadAPPWithAd(url: String){
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
                    downloadAPPSetup(url)
                    mRewardedAd = null
                    loadAd()
                }
            }
            else {
                Log.d(ContentValues.TAG, "The rewarded ad wasn't ready yet.")
                Toast.makeText(context, "網路錯誤 請5秒後在試", Toast.LENGTH_LONG).show()
            }
        }

        fun checkButton(){
            view.findViewById<Button>(R.id.download_gpx).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_gpx))
            }
            //******unlockPremium*********//
            view.findViewById<Button>(R.id.pgtools_unlockPremium).setOnClickListener {
                Toast.makeText(context, "本服務為合作夥伴服務項目，高級版相關問題請洽詢合作夥伴處理", Toast.LENGTH_LONG).show()
                gotoBrowser(resources.getString(R.string.shopee_auto))
            }
            //******download*********//
            view.findViewById<Button>(R.id.download_gps).setOnClickListener {
                if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                    downloadAPPWithAd(resources.getString(R.string.url_gps64))
                else
                    downloadAPPWithAd(resources.getString(R.string.url_gps32))
            }

            view.findViewById<Button>(R.id.download_polygon).setOnClickListener { view->
                if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                    downloadAPPWithAd(resources.getString(R.string.url_polygon))
                else {
                    Snackbar.make(view, "你的設備不支援此軟體("+(Build.SUPPORTED_ABIS[0])+")", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
            }

            view.findViewById<Button>(R.id.download_pgtools).setOnClickListener { view->
                if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                    if(testPgtools)
                        downloadAPPWithAd("https://assets.pgtools.net/test-pgtools-${appVersion_autovatch}.apk")
                    else
                        downloadAPPWithAd("https://assets.pgtools.net/pgtools-${appVersion_autovatch}.apk")
                else
                    Snackbar.make(view, "你的設備不支援此軟體("+(Build.SUPPORTED_ABIS[0])+")", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
            }

            view.findViewById<Button>(R.id.download_pok).setOnClickListener {
                if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                    downloadAPPWithAd(url_arm64autocatchDownload)
                else
                    downloadAPPWithAd(url_armautocatchDownload)
            }

            view.findViewById<Button>(R.id.download_pokAres).setOnClickListener { view->
                if(pokAresDownloadAPK)
                    downloadAPPWithAd(resources.getString(R.string.url_pokAres))
                else if(MANUFACTURER=="samsung")
                    downloadAPPWithAd(resources.getString(R.string.url_pokAres_store))
                else if(pokAresNoSupportDevices)
                    Toast.makeText(context, "請在設定中啟用\"一律下載三星版APK\"以繼續", Toast.LENGTH_LONG).show()
                else
                    Snackbar.make(view, "你的設備不支援此軟體", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
            }

            view.findViewById<Button>(R.id.download_pokelist).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_pokelist))
            }

            view.findViewById<Button>(R.id.download_wecatch).setOnClickListener {
                downloadAPPWithAd(resources.getString(R.string.url_wecatch))
            }

            //******remove*********//
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

        fun setFragmentResultListener(){
            setFragmentResultListener("testPgtools") { _, bundle ->
                testPgtools = bundle.getBoolean("bundleKey")
                checkAppVersion(requireActivity())
                getAutoVersion(requireActivity())
            }

            setFragmentResultListener("pokAresNoSupportDevices") { _, bundle ->
                pokAresNoSupportDevices = bundle.getBoolean("bundleKey")
                checkAppVersion(requireActivity())
                if((pokAresNoSupportDevices))
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.VISIBLE
                else if(MANUFACTURER!="samsung")
                    view.findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.GONE
            }

            setFragmentResultListener("pokAresDownloadAPK") { _, bundle ->
                pokAresDownloadAPK = bundle.getBoolean("bundleKey")
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


        setFragmentResultListener()
        setupAd()
        //checkAppVersion(requireActivity())
        //getAutoVersion(requireActivity())
        checkButton()

        // Inflate the layout for this fragment
        return view
    }

    override fun onStart() {
        super.onStart()
        checkAppVersion(requireActivity())
        getAutoVersion(requireActivity())
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
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }

    private fun appInstalledVersion(PackageName: String): String {
        val pm = activity?.packageManager
        try {
            pm?.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)
            return pm?.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)?.versionName.toString()
        } catch (e: PackageManager.NameNotFoundException) {}
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
                            Toast.makeText(context, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show()
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