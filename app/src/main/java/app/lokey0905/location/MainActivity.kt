package app.lokey0905.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import app.lokey0905.location.databinding.ActivityMainBinding
import app.lokey0905.location.fragment.Apps_mhn
import app.lokey0905.location.fragment.Apps_poke
import app.lokey0905.location.fragment.Home
import app.lokey0905.location.fragment.Preferences
import app.lokey0905.location.fragment.ShortCuts
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
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
    private var appsPoke: Apps_poke = Apps_poke()
    private var appsMhn: Apps_mhn = Apps_mhn()
    private var shortcuts: ShortCuts = ShortCuts()
    //private var setting: Setting = Setting()
    private var preferenceFragmentCompat: Preferences = Preferences()


    var bServiceBound = false
    //private var IIsolatedService = null
    var serviceBinder: IIsolatedService? = null

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkForUpdate(githubUrl: String, onUpdateAvailable: (Boolean, Float, String) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            var updateAvailable = false
            var latestVersion = 0.0f
            var latestVersionInformation = ""

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
                val currentRelease = BuildConfig.VERSION_NAME.toFloat()
                updateAvailable = currentRelease < latestVersion
                latestVersionInformation = jsonObject.getString("body")

            } catch (e: Exception) {
                e.printStackTrace()
            }

            launch(Dispatchers.Main) {
                onUpdateAvailable(updateAvailable,latestVersion,latestVersionInformation)
            }
        }
    }
    private fun checkUpdate(){
        val githubUrl = getString(R.string.githubApi)
        checkForUpdate(githubUrl) { updateAvailable,latestVersion,latestVersionInformation ->
            if (updateAvailable) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(getString(R.string.dialogUpdateAvailableTitle)+latestVersion)
                    .setMessage("${getString(R.string.dialogUpdateAvailableManagerMessage)}\n\n${getString(R.string.updateContent)}\n$latestVersionInformation")
                    .apply {
                        setPositiveButton(R.string.ok) { _, _ ->
                            downloadAPPSetup("https://github.com/lokey0905/POGO_Manager/releases/download/$latestVersion/app-debug.apk")
                        }
                        setNegativeButton(R.string.cancel) { _, _ ->
                            Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .show()
            } else {
                Toast.makeText(this, getString(R.string.dialogIsLatestVersion)+BuildConfig.VERSION_NAME.toFloat(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun download(isBackUp: Boolean = false) {
        if (isBackUp) {
            downloadAPPSetup(resources.getString(R.string.url_app))
        } else {
            val githubUrl = getString(R.string.githubApi)
            checkForUpdate(githubUrl) { _, latestVersion, _ ->
                downloadAPPSetup("https://github.com/lokey0905/POGO_Manager/releases/download/$latestVersion/app-debug.apk")
            }
        }
    }

    private fun gotoBrowser(url: String){
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun shareText(text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra(Intent.EXTRA_TITLE, title)
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun downloadAPPSetup(url: String){
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle(getString(R.string.dialogDownloadTitle))
            .setMessage(getString(R.string.dialogDownloadMessage))
            .apply {
                setPositiveButton(R.string.ok) { _, _ ->
                    gotoBrowser(url)
                }
                setNegativeButton(R.string.cancel) { _, _ ->
                    Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            if (!fragment.isAdded) {
                add(R.id.fragment_container_view, fragment)
            }
            supportFragmentManager.fragments.forEach {
                if (it != fragment && it.isAdded) {
                    hide(it)
                }
            }
            show(fragment)
        }.commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val MY_PERMISSIONS_REQUEST_LOCATION = 1
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val fragmentContainerView =
            findViewById<FragmentContainerView>(R.id.fragment_container_view)
        fragmentContainerView.removeAllViewsInLayout()

        findViewById<BottomNavigationView>(R.id.navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(home)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_poke -> {
                    replaceFragment(appsPoke)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_mhn -> {
                    replaceFragment(appsMhn)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_shortcuts -> {
                    replaceFragment(shortcuts)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_setting -> {
                    replaceFragment(preferenceFragmentCompat)
                    return@setOnItemSelectedListener true
                }
            }
            false
        }

        if (savedInstanceState == null)
            findViewById<BottomNavigationView>(R.id.navigation).selectedItemId = R.id.nav_home

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.dialogCheckLocationAccuracyTitle))
                .setMessage(getString(R.string.dialogCheckLocationAccuracyMessage))
                .apply {
                    setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                        var activityIntent = Intent()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            activityIntent.component =
                                ComponentName(
                                    "com.google.android.gms",
                                    "com.google.android.gms.location.settings.LocationAccuracyV31Activity"
                                )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            activityIntent.component =
                                ComponentName(
                                    "com.google.android.gms",
                                    "com.google.android.gms.location.settings.LocationAccuracyActivity"
                                )
                        } else {
                            activityIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        }
                        startActivity(activityIntent)
                    }
                    setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                        Toast.makeText(
                            context,
                            getString(R.string.cancelOperation),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .show()
        }

        //*************ad**********//
        MobileAds.initialize(this)
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = resources.getString(R.string.adB)
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
            val bIsMagisk: Boolean
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
        dialogView.findViewById<TextView>(R.id.design_about_info).text = resources.getString(R.string.dialogAboutInfo)
        dialogView.findViewById<TextView>(R.id.design_about_maker).text = resources.getString(R.string.dialogAboutMaker)
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
            R.id.action_download -> {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(getString(R.string.download))
                    .setMessage("是否要重新下載管理器?")
                    .apply {
                        setPositiveButton("重新下載") { _, _ ->
                            download()
                        }
                        setNegativeButton(getString(R.string.checkUpdate)) { _, _ ->
                            checkUpdate()
                        }
                        setNeutralButton(R.string.cancel) { _, _ ->
                            Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
                true
            }

            R.id.action_share -> {
                shareText(
                    "我發現了Pogo外掛管理器 趕快來下載使用!\n${getString(R.string.url_app)}",
                    resources.getString(R.string.shareManager)
                )
                true
            }

            R.id.action_download2 -> {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(getString(R.string.download))
                    .setMessage("是否要重新下載管理器?(備用連結)")
                    .apply {
                        setPositiveButton("重新下載") { _, _ ->
                            download(true)
                        }
                        setNegativeButton(getString(R.string.checkUpdate)) { _, _ ->
                            checkUpdate()
                        }
                        setNeutralButton(R.string.cancel) { _, _ ->
                            Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
                true
            }

            R.id.action_contact -> {
                gotoBrowser(getString(R.string.facebook))
                true
            }

            R.id.action_about -> {
                showAboutDialog()
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
