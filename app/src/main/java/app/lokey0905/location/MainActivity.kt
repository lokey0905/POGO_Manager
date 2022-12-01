package app.lokey0905.location

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.system.Os
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import app.lokey0905.location.databinding.ActivityMainBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.DecimalFormat
import java.util.*

class DynamicColors: Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val locationPermissionCode = 2
    private val MY_PERMISSIONS_REQUEST_LOCATION = 1

    private var mRewardedAd: RewardedAd? = null

    private var bServiceBound = false
    //private var IIsolatedService = null
    private var serviceBinder: IIsolatedService? = null

    private var url_temp: String = ""
    private var url_armautocatchDownload: String = ""
    private var url_arm64autocatchDownload: String = ""
    private val url_autocatch: String = "https://assets.pgtools.net/auto-versions.json"
    private val url_testautocatch: String = "https://assets.pgtools.net/test-auto-versions.json"
    private var appVersion_autovatch: String = "(檢查中)"
    private var pogoVersion: String = "(檢查中)"
    private var pogoVersionCodes: Array<String> = arrayOf()
    private val apkChecklist =  arrayOf("com.topjohnwu.magisk",
                                        "com.evermorelabs.polygonsharp",
                                        "net.pgtools.auto",
                                        "com.nianticlabs.pokemongo"
    )

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
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return "未安裝"
    }

    private fun appUnInstall(PackageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$PackageName")
        startActivity(intent)
    }

    private fun devicesCheck() {
        findViewById<TextView>(R.id.android_imformation).text = "系統代號:" + "\n" + Build.DEVICE
        findViewById<TextView>(R.id.android_version).text = "Android版本:" + "\n" + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")"
        findViewById<TextView>(R.id.android_abi).text = "系統架構:" + "\n" + Build.SUPPORTED_ABIS[0] +
                    if (Build.SUPPORTED_ABIS[0] == "arm64-v8a") {"(64"} else {"(32"} + "位元)"
        findViewById<TextView>(R.id.android_supper).text ="是否支援暴力功:"+ "\n" + if(Build.SUPPORTED_ABIS[0] == "arm64-v8a"){"有"}else{"不"}+"支援"

        findViewById<TextView>(R.id.polygon_install_verison).text =
            "已安裝版本:" + appInstalled(apkChecklist[1])
        findViewById<TextView>(R.id.autocatch_install_verison).text =
            "已安裝版本:" + appInstalled(apkChecklist[2])
        findViewById<TextView>(R.id.pok_install_verison).text =
            "已安裝版本:" + appInstalled(apkChecklist[3])
    }
    private fun magiskCheck(){
        //magisk check
        if (bServiceBound) {
            var bIsMagisk = false
            try {
                Log.d(TAG, "UID:" + Os.getuid())
                bIsMagisk = serviceBinder!!.isMagiskPresent
                if (bIsMagisk) {
                    //Toast.makeText(applicationContext, "Magisk Found", Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.check_location).setBackgroundColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_error))
                    findViewById<Button>(R.id.check_location).setTextColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_on_error))
                    findViewById<TextView>(R.id.check_magisk).setTextColor(ContextCompat.getColor(this,com.google.android.material.R.color.design_default_color_error))
                    findViewById<Button>(R.id.check_location).text = "驗證定位 無法偵測目前位置11"
                    findViewById<TextView>(R.id.check_magisk).text = "已發現(未隔離)"+ if(appInstalledOrNot(apkChecklist[0])){ "(找到安裝)" } else { "" }
                }
                else {
                    //Toast.makeText(applicationContext, "Magisk Not Found", Toast.LENGTH_LONG).show()
                    if(appInstalledOrNot(apkChecklist[0])){
                        findViewById<TextView>(R.id.check_magisk).text = "已發現(找到安裝)"
                        findViewById<Button>(R.id.check_location).text = "驗證定位 無法偵測目前位置11"
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
        } else {
            //Toast.makeText(applicationContext, "Isolated Service not bound", Toast.LENGTH_SHORT).show()
        }

    }

    private fun getAutocatchVersion(url: String = url_autocatch){
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
                    findViewById<TextView>(R.id.autocatch_new_verison).text = "最新版本　:"
                    findViewById<TextView>(R.id.pok_new_verison).text = "支援版本　:"+pogoVersion
                    findViewById<TextView>(R.id.autocatch_new_verison).text = "最新版本　:"+appVersion_autovatch
                    if(pogoVersion.replace(".","") < appInstalled(apkChecklist[3]).replace(".","")
                        && appInstalled(apkChecklist[3])!="未安裝"){
                        findViewById<TextView>(R.id.pok_install_verison).setTextColor(ContextCompat.getColor(
                            this,com.google.android.material.R.color.design_default_color_error))
                        findViewById<TextView>(R.id.pok_install_verison).text =
                            findViewById<TextView>(R.id.pok_install_verison).text.toString()+"(過高)"
                    }
                }

                Log.i("auto_catch",json)
            } catch (e:Exception) {
                Log.i("auto_catch", e.toString())
            }
        }
        gfgThread.start()
    }

    private fun downloadAPPSetup(url: String){
        Toast.makeText(applicationContext, "請手動點擊下載Download APK", Toast.LENGTH_LONG).show();
        CustomTabsIntent.Builder().build()
            .launchUrl(this, Uri.parse(url))/*
        val queryUrl: Uri = Uri.parse(url)
        val intent = Intent( Intent.ACTION_VIEW , queryUrl)
        startActivity(intent)*/
        Toast.makeText(applicationContext, "下載完成後在點安裝APK", Toast.LENGTH_LONG).show();
    }

    private fun downloadAPP(url: String){
        if (mRewardedAd != null) {
            Toast.makeText(applicationContext, "感謝您的耐心等候：）", Toast.LENGTH_LONG).show();
            mRewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null
                        loadad()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(TAG, "Ad failed to show.")
                        Toast.makeText(applicationContext, "播放失敗 請稍後在試", Toast.LENGTH_LONG).show();
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

            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            Toast.makeText(applicationContext, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show();
        }
        //downloadAPPSetup(url)
    }

    private fun loadad(){
        if (mRewardedAd == null) {
            var adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                this,
                "ca-app-pub-9117573027413270/3285311458",
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError?.message)
                        mRewardedAd = null
                        Toast.makeText(applicationContext, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show();
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

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "若有問題 請直接私訊lokey\n或蝦皮搜尋【lokey刷機工廠】", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()

        }


        //******check button*********//
        //******download*********//
        findViewById<Button>(R.id.download_gps).setOnClickListener { view->
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a"){
                downloadAPP("https://www.mediafire.com/file/maf260fw7u805tm/gpsjoystick_lokey_new.apk/file")
            }
            else {
                downloadAPP("https://www.mediafire.com/file/07pe1z0shwr06hf/gpsjoystick_lokey_old.apk/file")
            }
        }

        findViewById<Button>(R.id.download_pok).setOnClickListener { view->
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a"){
                downloadAPP(url_arm64autocatchDownload)
                }
            else {
                downloadAPP(url_armautocatchDownload)
            }
        }

        findViewById<Button>(R.id.download_polygon).setOnClickListener { view->
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a"){
                downloadAPP("https://www.mediafire.com/file/l01stxwk10o2e8w/com.evermorelabs.polygonsharp-0.9.1pxl-b.5.apk/file")
            }
            else {
                Snackbar.make(view, "你的設備不支援此軟體("+(Build.SUPPORTED_ABIS[0])+")", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()}
        }

        findViewById<Button>(R.id.download_autocatch).setOnClickListener { view->
            if(Build.SUPPORTED_ABIS[0]=="arm64-v8a"){
                downloadAPP("https://assets.pgtools.net/test-auto-catch-$appVersion_autovatch.apk")
                }
            else {
                downloadAPP("https://assets.pgtools.net/test-auto-catch-$appVersion_autovatch.apk")
            }
        }

        findViewById<MaterialSwitch>(R.id.switch1).setOnCheckedChangeListener{ _, isChecked->
            if (isChecked) {
                // 開啟時
                getAutocatchVersion(url_testautocatch)
                Toast.makeText(applicationContext, "請注意 測試版本可能會有不穩狀況!", Toast.LENGTH_LONG).show();
            } else {
                // 關閉時
                getAutocatchVersion()
            }
            devicesCheck()
        }


        //******remove*********//
        findViewById<Button>(R.id.remove_polygon).setOnClickListener {
            appUnInstall(apkChecklist[1])
        }

        findViewById<Button>(R.id.remove_autocatch).setOnClickListener {
            appUnInstall(apkChecklist[2])
        }

        findViewById<Button>(R.id.remove_pok).setOnClickListener {
            appUnInstall(apkChecklist[3])
        }
        findViewById<Button>(R.id.check_location).setOnClickListener {
            magiskCheck()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "請允許定位權限後在重試", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
            }
            else{
                getLocation(LocationManager.FUSED_PROVIDER)
            }
            getLocation(LocationManager.FUSED_PROVIDER)

        }

        //*************ad**********//
        MobileAds.initialize(this)
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-9117573027413270/9041694963"
        val mAdView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        RewardedAd.load(this,"ca-app-pub-9117573027413270/3285311458", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError?.toString())
                mRewardedAd = null
                Toast.makeText(applicationContext, "網路錯誤 請稍後在試", Toast.LENGTH_LONG).show();
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
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this.application)
        }*/
    }

    override fun onStart() {
        super.onStart()
        devicesCheck()
        getAutocatchVersion()
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
            if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && (findViewById<MaterialSwitch>(R.id.switch1).isChecked))
                if(location.isMock) findViewById<Button>(R.id.check_location).text = "驗證定位 無法偵測目前位置12(${Build.VERSION.SDK_INT})"
                else findViewById<Button>(R.id.check_location).text = "驗證定位"
            else
                if (location.isFromMockProvider) findViewById<Button>(R.id.check_location).text = "驗證定位 無法偵測目前位置12"
                else findViewById<Button>(R.id.check_location).text = "驗證定位"

            findViewById<TextView>(R.id.location_system).text = "0.0,0.0 (${location.getProvider()})"

            if (location != null) {
                findViewById<TextView>(R.id.location_system).text =
                    "${DecimalFormat("#.######").format(location.latitude)},${DecimalFormat("#.######").format(location.longitude)} (${location.getProvider()})"
            }else {
                findViewById<TextView>(R.id.location_system).text =
                    "0.0,0.0 (${location.getProvider()})"
            }

            val gc:Geocoder =Geocoder(this@MainActivity, Locale.getDefault())
            var locationList=gc.getFromLocation(location.latitude,location.longitude,1);
            val address:Address= locationList!!.get(0)
            val countryName=address.countryName
            val locale=address.locale
            var i=0
            var AddressLine : String = ""
            while (address.getAddressLine(i)!=null){
                AddressLine += address.getAddressLine(i)
                i++
            }
            findViewById<TextView>(R.id.location_system).text =
                findViewById<TextView>(R.id.location_system).text.toString() +"\n"+AddressLine
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            Toast.makeText(applicationContext, "請開啟gps或是網路", Toast.LENGTH_LONG).show();
        }
    }

    private fun getLocation(Mode: String) {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var bestProvider = locationManager.getBestProvider(Criteria(), true)

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        findViewById<TextView>(R.id.location_system).text = "0.0,0.0"
        locationManager.requestLocationUpdates(bestProvider.toString(), 500, 0f,locationListener)
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1f,locationListener)
        //locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 100, 1f,locationListener)
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
        val id = item.itemId

        // 依照id判斷點了哪個項目並做相應事件
        if (id == R.id.action_about) {
            // 按下「設定」要做的事
            Toast.makeText(this, "本APP由lokey0905製作", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onOptionsItemSelected(item)
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
