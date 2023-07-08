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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


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

    var latestVersion = BuildConfig.VERSION_NAME.toFloat()

    var bServiceBound = false
    //private var IIsolatedService = null
    var serviceBinder: IIsolatedService? = null

    private fun checkForUpdate(currentRelease: Float, githubUrl: String, onUpdateAvailable: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            var updateAvailable = false

            try {
                val url = URL(githubUrl)
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
                latestVersion = jsonObject.getString("name").toFloat()
                updateAvailable = currentRelease < latestVersion
                //println("$currentRelease ++ $latestVersion")

            } catch (e: Exception) {
                e.printStackTrace()
            }

            launch(Dispatchers.Main) {
                onUpdateAvailable(updateAvailable)
            }
        }
    }
    private fun checkUpdate(){
        val currentVersion = BuildConfig.VERSION_NAME.toFloat()
        val githubUrl = resources.getString(R.string.githubApi)
        checkForUpdate(currentVersion, githubUrl) { updateAvailable ->
            if (updateAvailable) {
                val builder = MaterialAlertDialogBuilder(this@MainActivity)
                builder.setTitle(resources.getString(R.string.dialogUpdateTitle))
                builder.setMessage(resources.getString(R.string.dialogUpdateManagerMessage))
                builder.apply {
                    setPositiveButton(R.string.ok) { _, _ ->
                        downloadAPPSetup("https://github.com/lokey0905/POGO_Manager/releases/download/$latestVersion/app-debug.apk")
                    }
                    setNegativeButton(R.string.cancel) { _, _ ->
                        Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                builder.create().show()
            } else {
                Toast.makeText(this, "已經是最新版本", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun gotoBrowser(url: String){
        CustomTabsIntent.Builder().build()
            .launchUrl(this, Uri.parse(url))/*
        val queryUrl: Uri = Uri.parse(url)
        val intent = Intent( Intent.ACTION_VIEW , queryUrl)
        startActivity(intent)*/
    }

    private fun shareText(text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra(Intent.EXTRA_TITLE, title)
        startActivity(Intent.createChooser(intent, "分享"))
    }

    private fun downloadAPPSetup(url: String){
        val builder = MaterialAlertDialogBuilder(this@MainActivity)
            builder.setTitle(resources.getString(R.string.dialogDownloadTitle))
            builder.setMessage(resources.getString(R.string.dialogDownloadMessage))
            builder.apply {
                setPositiveButton(R.string.ok) { _, _ ->
                    gotoBrowser(url)
                }
                setNegativeButton(R.string.cancel) { _, _ ->
                    Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT)
                        .show()
                }
            }
            builder.create().show()
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
        binding.shoppe.setOnClickListener {
            gotoBrowser(resources.getString(R.string.shopee))
        }*/

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
        checkUpdate()

        val intent = Intent(this, IsolatedService::class.java)
        /*Binding to an isolated service */
        applicationContext.bindService(intent,mIsolatedServiceConnection,BIND_AUTO_CREATE)

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
                        "com.google.android.gms.nearby.sharing.QuickSettingsActivity")
                startActivity(activityIntent)
                true
            }
            R.id.action_share -> {
                shareText(resources.getString(R.string.url_app), resources.getString(R.string.shareManager))
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
