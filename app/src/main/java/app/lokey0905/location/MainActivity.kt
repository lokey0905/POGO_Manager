package app.lokey0905.location

import android.Manifest
import android.app.ActivityManager
import android.app.Application
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Build.DEVICE
import android.os.Build.MANUFACTURER
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.system.Os
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import app.lokey0905.location.databinding.ActivityMainBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.DecimalFormat
import java.util.*
import kotlin.math.roundToInt

class DynamicColors: Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val locationPermissionCode = 2
    private val MY_PERMISSIONS_REQUEST_LOCATION = 1

    private var mRewardedAd: RewardedAd? = null

    private var bServiceBound = false
    //private var IIsolatedService = null
    private var serviceBinder: IIsolatedService? = null

    private var url_armautocatchDownload: String = ""
    private var url_arm64autocatchDownload: String = ""
    private var appVersion_autovatch: String = "未安裝"
    private var pogoVersion: String = "未安裝"
    private var pogoVersionCodes: Array<String> = arrayOf()

    private fun appInstalledOrNot(PackageName: String): Boolean {
        val pm = packageManager
        try {
            pm.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }

    private fun appInstalled(PackageName: String): String {
        val pm = packageManager
        try {
            pm.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES)
            return pm.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES).versionName
        } catch (e: PackageManager.NameNotFoundException) {}
        return "未安裝"
    }

    private fun appUnInstall(PackageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$PackageName")
        startActivity(intent)
    }

    private fun devicesCheck() {
        val actManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMemory= (memInfo.totalMem.toDouble()/(1024*1024*1024)).roundToInt()

        if(totalMemory < 5 && !(findViewById<MaterialSwitch>(R.id.switch1).isChecked()))
            findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.GONE
        else
            findViewById<LinearLayout>(R.id.linearLayout_pokAres).visibility = View.VISIBLE

        findViewById<TextView>(R.id.android_imformation).text =
            "系統代號:\n$DEVICE($MANUFACTURER)"
        findViewById<TextView>(R.id.android_version).text =
            "Android版本:\n${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT})"
        findViewById<TextView>(R.id.android_abi).text =
            "系統架構:\n" + Build.SUPPORTED_ABIS[0] +
                    if (Build.SUPPORTED_ABIS[0] == "arm64-v8a") {"(64"} else {"(32"} + "位元)"
        findViewById<TextView>(R.id.android_ramSize).text =
            "記憶體大小:\n$totalMemory GB("+if(totalMemory >= 5){"有"}else{"不"}+"支援雙開)"
        findViewById<TextView>(R.id.android_supper).text =
            "暴力功與自動抓狀態:\n" + if(Build.SUPPORTED_ABIS[0] == "arm64-v8a"){"有"}else{"不"}+"支援"

        findViewById<TextView>(R.id.polygon_install_version).text =
            String.format(resources.getString(R.string.format_installVersion,
                appInstalled(resources.getString(R.string.packageName_polygon))))
        findViewById<TextView>(R.id.pgtools_install_version).text =
            String.format(resources.getString(R.string.format_installVersion,
            appInstalled(resources.getString(R.string.packageName_auto))))
        findViewById<TextView>(R.id.pok_install_version).text =
            String.format(resources.getString(R.string.format_installVersion,
            appInstalled(resources.getString(R.string.packageName_pok))))
        findViewById<TextView>(R.id.pokAres_install_version).text =
            String.format(resources.getString(R.string.format_installVersion,
                appInstalled(resources.getString(R.string.packageName_pokAres))+
                    if(MANUFACTURER!="samsung"){"(不支援)"} else {""}))
    }

    private fun magiskCheck(){ //magisk check
        if (this.bServiceBound) {
            var bIsMagisk = false
            try {
                Log.d(TAG, "UID:" + Os.getuid())
                bIsMagisk = serviceBinder!!.isMagiskPresent
                if (bIsMagisk) {
                    //Toast.makeText(applicationContext, "Magisk Found", Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.check_location).setBackgroundColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_error))
                    findViewById<Button>(R.id.check_location).setTextColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_on_error))
                    findViewById<TextView>(R.id.check_magisk).setTextColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_error))
                    findViewById<TextView>(R.id.check_magisk).text = "已發現(未隔離)"
                }
                else {
                    //Toast.makeText(applicationContext, "Magisk Not Found", Toast.LENGTH_LONG).show()
                    if(appInstalledOrNot(R.string.packageName_magisk.toString())){
                        findViewById<TextView>(R.id.check_magisk).text = "已發現(找到安裝)"
                        findViewById<Button>(R.id.check_location).setBackgroundColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_error))
                        findViewById<Button>(R.id.check_location).setTextColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_on_error))
                    }
                    else{
                        findViewById<TextView>(R.id.check_magisk).text = "未發現"
                        findViewById<TextView>(R.id.check_magisk).setTextColor(ContextCompat.getColor(this,R.color.green))
                        //findViewById<Button>(R.id.check_location).setBackgroundColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_primary))
                    }

                }
                //getApplicationContext().unbindService(mIsolatedServiceConnection);
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        else {
            //Toast.makeText(applicationContext, "Isolated Service not bound", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAutoVersion(){
        var url = resources.getString(R.string.url_autoJson)
        if(findViewById<MaterialSwitch>(R.id.switch1).isChecked())
            url = resources.getString(R.string.url_autoTestJson)
        val gfgThread = Thread {
            try {
                val json = Jsoup.connect(url).timeout(10000).ignoreContentType(true).execute().body();
                val jsonObject = JSONObject(json)
                url_armautocatchDownload = jsonObject.get("pogoARM").toString()
                url_arm64autocatchDownload = jsonObject.get("pogoARM64").toString()
                appVersion_autovatch = jsonObject.get("appName").toString()
                pogoVersion = jsonObject.get("pogoVersion").toString()
                pogoVersionCodes = arrayOf(jsonObject.get("pogoVersionCodes").toString())
                runOnUiThread {
                    findViewById<TextView>(R.id.pgtools_new_version).text = String.format(resources.getString(R.string.format_newerVersion),null)
                    findViewById<TextView>(R.id.pok_new_version).text = String.format(resources.getString(R.string.format_newerVersion),pogoVersion)
                    findViewById<TextView>(R.id.pokAres_new_version).text = String.format(resources.getString(R.string.format_newerVersion),pogoVersion)
                    findViewById<TextView>(R.id.pgtools_new_version).text = String.format(resources.getString(R.string.format_newerVersion),appVersion_autovatch)
                    if(pogoVersion!="未安裝" && appInstalled(resources.getString(R.string.packageName_pok))!="未安裝"){
                        if(pogoVersion.replace(".","").toInt() <
                            appInstalled(resources.getString(R.string.packageName_pok)).replace(".","").toInt()){
                            findViewById<TextView>(R.id.pok_install_version).setTextColor(ContextCompat.getColor(
                                this,com.google.android.material.R.color.design_default_color_error))
                            findViewById<TextView>(R.id.pok_install_version).text =
                                findViewById<TextView>(R.id.pok_install_version).text.toString()+"(過高)"
                        }
                        else if(pogoVersion.replace(".","").toInt() >
                            appInstalled(resources.getString(R.string.packageName_pok)).replace(".","").toInt()) {
                            findViewById<Button>(R.id.download_pok).text = resources.getString(R.string.update)
                        }
                    }
                    if(appVersion_autovatch!="未安裝" && appInstalled(resources.getString(R.string.packageName_auto))!="未安裝"){
                        if(appVersion_autovatch.replace(".","").toInt() >
                            appInstalled(resources.getString(R.string.packageName_auto)).replace(".","").toInt()){
                            findViewById<Button>(R.id.download_pgtools).text = resources.getString(R.string.update)
                        }
                    }
                }
                Log.i("auto_catch",json)
            } catch (e:Exception) {
                Log.i("auto_catch", e.toString())
            }
        }
        gfgThread.start()
        devicesCheck()
    }

    private fun gotoBrowser(url: String){
        CustomTabsIntent.Builder().build()
            .launchUrl(this, Uri.parse(url))/*
        val queryUrl: Uri = Uri.parse(url)
        val intent = Intent( Intent.ACTION_VIEW , queryUrl)
        startActivity(intent)*/
    }

    private fun downloadAPPSetup(url: String){
        Toast.makeText(applicationContext, "請手動點擊下載Download APK", Toast.LENGTH_LONG).show()
        gotoBrowser(url)
        Toast.makeText(applicationContext, "下載完成後在點安裝APK", Toast.LENGTH_LONG).show()
    }

    private fun downloadAPP(url: String){
        if (mRewardedAd != null) {
            Toast.makeText(applicationContext, "感謝您的耐心等候：）", Toast.LENGTH_LONG).show()
            mRewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null
                        loadAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(TAG, "Ad failed to show.")
                        Toast.makeText(applicationContext, "播放失敗 請稍後在試", Toast.LENGTH_LONG).show()
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }
            mRewardedAd?.show(this){
                downloadAPPSetup(url)
                mRewardedAd = null
                loadAd()
            }
        }
        else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            Toast.makeText(applicationContext, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAd(){
        if (mRewardedAd == null) {
            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                this,
                resources.getString(R.string.adID_Rewarded),
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        mRewardedAd = null
                        Toast.makeText(applicationContext, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show()
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mRewardedAd = rewardedAd
                    }
                }
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val MY_PERMISSIONS_REQUEST_LOCATION = 1
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener {
            gotoBrowser(resources.getString(R.string.facebook))/*
            Snackbar.make(view, "若有問題 請直接私訊lokey\n或蝦皮搜尋【lokey刷機工廠】", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()*/
        }

        binding.shoppe.setOnClickListener {
            gotoBrowser(resources.getString(R.string.shopee))
        }

        //******check button*********//
        findViewById<Button>(R.id.LocationAccuracyActivity).setOnClickListener {
            val activityIntent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activityIntent.component =
                    ComponentName("com.google.android.gms", "com.google.android.gms.location.settings.LocationAccuracyV31Activity")
            }
            else
            activityIntent.component =
                ComponentName("com.google.android.gms", "com.google.android.gms.location.settings.LocationAccuracyActivity")
            startActivity(activityIntent)
        }
        //******unlockPremium*********//
        findViewById<Button>(R.id.pgtools_unlockPremium).setOnClickListener {
            Toast.makeText(this, "本服務為合作夥伴服務項目，高級版相關問題請洽詢合作夥伴處理", Toast.LENGTH_LONG).show()
            gotoBrowser(resources.getString(R.string.shopee_auto))
        }
        //******download*********//
        findViewById<Button>(R.id.download_gps).setOnClickListener { view->
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                downloadAPP(resources.getString(R.string.url_gps64))
            else
                downloadAPP(resources.getString(R.string.url_gps32))
        }

        findViewById<Button>(R.id.download_polygon).setOnClickListener { view->
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                downloadAPP(resources.getString(R.string.url_polygon))
            else {
                Snackbar.make(view, "你的設備不支援此軟體("+(Build.SUPPORTED_ABIS[0])+")", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }

        findViewById<Button>(R.id.download_pgtools).setOnClickListener { view->
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                if(findViewById<MaterialSwitch>(R.id.switch1).isChecked())
                    downloadAPP("https://assets.pgtools.net/test-pgtools-$appVersion_autovatch.apk")
                else
                    downloadAPP("https://assets.pgtools.net/pgtools-$appVersion_autovatch.apk")
            else
                Snackbar.make(view, "你的設備不支援此軟體("+(Build.SUPPORTED_ABIS[0])+")", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        findViewById<Button>(R.id.download_pok).setOnClickListener {
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a")
                downloadAPP(url_arm64autocatchDownload)
            else
                downloadAPP(url_armautocatchDownload)
        }

        findViewById<Button>(R.id.download_pokAres).setOnClickListener { view->
            if(findViewById<MaterialSwitch>(R.id.switch1).isChecked())
                downloadAPP(resources.getString(R.string.url_pokAres))
            else if(MANUFACTURER=="samsung")
                downloadAPP(resources.getString(R.string.url_pokAres_store))
            else
                Snackbar.make(view, "你的設備不支援此軟體 若有需要可啟用測試版本在下載", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        findViewById<MaterialSwitch>(R.id.switch1).setOnCheckedChangeListener{ _, isChecked->
            if (isChecked) {
                Toast.makeText(applicationContext, "請注意 測試版本可能不穩!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "若無法使用可嘗試切換", Toast.LENGTH_SHORT).show()
            }
            getAutoVersion()
            devicesCheck()
        }


        //******remove*********//
        findViewById<Button>(R.id.remove_polygon).setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_polygon))
        }

        findViewById<Button>(R.id.remove_pgtools).setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_auto))
        }

        findViewById<Button>(R.id.remove_pok).setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_pok))
        }

        findViewById<Button>(R.id.remove_pokAres).setOnClickListener {
            appUnInstall(resources.getString(R.string.packageName_pokAres))
        }

        //*********驗證定位**********//
        findViewById<Button>(R.id.check_location).setOnClickListener {
            magiskCheck()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "請允許定位權限後在重試", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
            }
            else{
                getLocation()
            }
            getLocation()

        }

        //*************ad**********//
        MobileAds.initialize(this)
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = resources.getString(R.string.adID_Banner)
        val mAdView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        RewardedAd.load(this, resources.getString(R.string.adID_Rewarded), adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                mRewardedAd = null
                //Toast.makeText(applicationContext, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show();
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                mRewardedAd = rewardedAd
            }
        })

        mRewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                //mRewardedAd = null
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                //mRewardedAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        getAutoVersion()
        devicesCheck()

        val intent = Intent(this, IsolatedService::class.java)
        /*Binding to an isolated service */
        applicationContext.bindService(intent,mIsolatedServiceConnection,BIND_AUTO_CREATE)

    }

    private val mIsolatedServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            serviceBinder = IIsolatedService.Stub.asInterface(iBinder)
            bServiceBound = true
            Log.d(TAG, "Service bound")
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bServiceBound = false
            Log.d(TAG, "Service Unbound")
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            //findViewById<Button>(R.id.check_location).text = "驗證定位"
            if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && (findViewById<MaterialSwitch>(R.id.switch1).isChecked()))
                if(location.isMock) findViewById<Button>(R.id.check_location).text =
                    resources.getString(R.string.check_button)+" 無法偵測目前位置12(${Build.VERSION.SDK_INT})"
                else findViewById<Button>(R.id.check_location).text = resources.getString(R.string.check_button)
            else
                if (location.isFromMockProvider) findViewById<Button>(R.id.check_location).text =
                    resources.getString(R.string.check_button)+" 無法偵測目前位置12"
                else findViewById<Button>(R.id.check_location).text = resources.getString(R.string.check_button)

            findViewById<TextView>(R.id.location_system).text = "0.0,0.0 (${location.getProvider()})"

            findViewById<TextView>(R.id.location_system).text =
                "${DecimalFormat("#.######").format(location.latitude)},${DecimalFormat("#.######").format(location.longitude)} (${location.getProvider()})"

            val gc:Geocoder =Geocoder(this@MainActivity, Locale.getDefault())
            val locationList=gc.getFromLocation(location.latitude,location.longitude,1);
            val address:Address= locationList!!.get(0)
            var i=0
            var addressLine = ""
            while (address.getAddressLine(i)!=null){
                addressLine += address.getAddressLine(i)
                i++
            }
            findViewById<TextView>(R.id.location_system).text =
                findViewById<TextView>(R.id.location_system).text.toString() +"\n${addressLine}"
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            Toast.makeText(applicationContext, "請開啟gps或是網路", Toast.LENGTH_LONG).show();
        }
    }

    private fun getLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val bestProvider = locationManager.getBestProvider(Criteria(), true).toString()

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        //findViewById<TextView>(R.id.location_system).text = "0.0,0.0"
        /*if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0f,locationListener);
        }else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0f,locationListener);
        }else{
            locationManager.requestLocationUpdates(bestProvider.toString(), 500, 0f,locationListener)
        }*/
        locationManager.requestLocationUpdates(bestProvider, 500, 0f,locationListener)
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1f,locationListener)
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 1f,locationListener)
        //locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 500, 1f,locationListener)
    }

    private fun showAboutDialog() {
        val dialog = MaterialAlertDialogBuilder(this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .create()
        val dialogView: View = View.inflate(this, R.layout.dialog_about, null)
        dialog.setView(dialogView)
        dialogView.findViewById<TextView>(R.id.design_about_title).text = resources.getString(R.string.app_name)
        dialogView.findViewById<TextView>(R.id.design_about_version).text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        dialogView.findViewById<TextView>(R.id.design_about_info).text = "2023 by lokey0905"
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_download -> {
                downloadAPPSetup(resources.getString(R.string.url_app))
                true
            }
            R.id.action_contact -> {
                gotoBrowser(resources.getString(R.string.facebook))
                true
            }
            R.id.action_PokeList -> {
                downloadAPP(resources.getString(R.string.url_pokelist))
                true
            }
            R.id.action_wecatch -> {
                downloadAPP(resources.getString(R.string.url_wecatch))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "請允許定位權限後在重試", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
