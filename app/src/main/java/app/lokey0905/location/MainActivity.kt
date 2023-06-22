package app.lokey0905.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.provider.Settings
import android.system.Os
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import app.lokey0905.location.databinding.ActivityMainBinding
import app.lokey0905.location.fragment.Apps
import app.lokey0905.location.fragment.Home
import app.lokey0905.location.fragment.Setting
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder


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
    private val MY_PERMISSIONS_REQUEST_LOCATION = 1

    private var home: Home = Home()
    private var apps: Apps = Apps()
    private var setting: Setting = Setting()

    var bServiceBound = false
    //private var IIsolatedService = null
    var serviceBinder: IIsolatedService? = null

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

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            if (fragment.isAdded) {
                show(fragment)
            } else {
                add(R.id.fragment_container_view, fragment)
            }
            supportFragmentManager.fragments.forEach {
                if (it != fragment && it.isAdded) {
                    hide(it)
                }
            }
        }.commit()
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
/*
        binding.fab.setOnClickListener {
            gotoBrowser(resources.getString(R.string.facebook))/*
            Snackbar.make(view, "若有問題 請直接私訊lokey\n或蝦皮搜尋【lokey刷機工廠】", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()*/
        }

        binding.shoppe.setOnClickListener {
            gotoBrowser(resources.getString(R.string.shopee))
        }*/

        //*************ad**********//
        MobileAds.initialize(this)
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = resources.getString(R.string.adID_Banner)
        val mAdView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, IsolatedService::class.java)
        /*Binding to an isolated service */
        applicationContext.bindService(intent,mIsolatedServiceConnection,BIND_AUTO_CREATE)

        findViewById<BottomNavigationView>(R.id.navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(home)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_apps -> {
                    replaceFragment(apps)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_setting -> {
                    replaceFragment(setting)
                    return@setOnItemSelectedListener true
                }
            }
            false }

        findViewById<BottomNavigationView>(R.id.navigation).selectedItemId= R.id.nav_home

    }

    fun magiskCheck() {
        if (this.bServiceBound) {
            var bIsMagisk =false
            try {
                Log.d(TAG, "UID:" + Os.getuid())
                bIsMagisk = serviceBinder!!.isMagiskPresent
                Log.d(TAG, "bIsMagisk: $bIsMagisk")
                supportFragmentManager.setFragmentResult("bIsMagisk", bundleOf("bundleKey" to bIsMagisk))
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        else {
            Log.d(TAG, "Isolated Service not bound")
        }
    }

    private val mIsolatedServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            serviceBinder = IIsolatedService.Stub.asInterface(iBinder)
            bServiceBound = true
            Log.d(TAG, "Service bound")
            magiskCheck()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bServiceBound = false
            Log.d(TAG, "Service Unbound")
        }
    }

    @SuppressLint("SetTextI18n")
    fun showAboutDialog() {
        val dialog = MaterialAlertDialogBuilder(this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .create()
        val dialogView: View = View.inflate(this, R.layout.dialog_about, null)
        dialog.setView(dialogView)
        dialogView.findViewById<TextView>(R.id.design_about_title).text = resources.getString(R.string.app_name)
        dialogView.findViewById<TextView>(R.id.design_about_version).text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        dialogView.findViewById<TextView>(R.id.design_about_info).text = "相關檔案皆為網路搜尋取得\n檔案不歸我擁有\n2023 by lokey0905"
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
            R.id.action_nearbySharing -> {
                val activityIntent = Intent()
                activityIntent.component =
                    ComponentName("com.google.android.gms",
                        "com.google.android.gms.nearby.sharing.SettingsCollapsingToolbarActivity")
                startActivity(activityIntent)
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
