package app.lokey0905.location

import android.Manifest
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
import android.system.Os
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.preference.PreferenceManager
import app.lokey0905.location.databinding.ActivityMainBinding
import app.lokey0905.location.fragment.AppsMHN
import app.lokey0905.location.fragment.AppsPoke
import app.lokey0905.location.fragment.Home
import app.lokey0905.location.fragment.Preferences
import app.lokey0905.location.fragment.ShortCuts
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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

    private var home: Home = Home()
    private var appsPoke: AppsPoke = AppsPoke()
    private var appsMhn: AppsMHN = AppsMHN()
    private var shortcuts: ShortCuts = ShortCuts()
    private var preferenceFragmentCompat: Preferences = Preferences()


    var bServiceBound = false

    //private var IIsolatedService = null
    var serviceBinder: IIsolatedService? = null

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkForUpdate(
        githubUrl: String,
        onUpdateAvailable: (Boolean, String, String) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            var updateAvailable = false
            var latestVersion = ""
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

                latestVersion = jsonObject.getString("name")
                val latestVersionInt: List<String> = jsonObject.getString("name").split(".")
                val currentRelease = BuildConfig.VERSION_NAME.split(".")

                for (i in latestVersionInt.indices) {
                    val currentVersion = latestVersionInt[i].toInt()
                    val installedVersion = currentRelease[i].toInt()

                    if (currentVersion > installedVersion) {
                        updateAvailable = true
                    }
                }

                latestVersionInformation = jsonObject.getString("body")

            } catch (e: Exception) {
                e.printStackTrace()
            }

            launch(Dispatchers.Main) {
                onUpdateAvailable(updateAvailable, latestVersion, latestVersionInformation)
            }
        }
    }

    private fun checkUpdate() {
        val githubUrl = getString(R.string.githubApi)
        checkForUpdate(githubUrl) { updateAvailable, latestVersion, latestVersionInformation ->
            if (updateAvailable) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(getString(R.string.dialogUpdateAvailableTitle) + latestVersion)
                    .setMessage(
                        "${getString(R.string.dialogUpdateAvailableManagerMessage)}\n\n${
                            getString(
                                R.string.updateContent
                            )
                        }\n$latestVersionInformation"
                    )
                    .apply {
                        setPositiveButton(R.string.ok) { _, _ ->
                            downloadAPPSetup("https://github.com/lokey0905/POGO_Manager/releases/download/$latestVersion/app-debug.apk")
                        }
                        setNegativeButton(R.string.cancel) { _, _ ->
                            Toast.makeText(
                                context,
                                getString(R.string.cancelOperation),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                    .show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.dialogIsLatestVersion) + BuildConfig.VERSION_NAME,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun gotoBrowser(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun downloadAPPSetup(url: String) {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle(getString(R.string.dialogDownloadTitle))
            .setMessage(getString(R.string.dialogDownloadMessage))
            .apply {
                setPositiveButton(R.string.ok) { _, _ ->
                    gotoBrowser(url)
                }
                setNegativeButton(R.string.cancel) { _, _ ->
                    Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT)
                        .show()
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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setSupportActionBar(binding.toolbar)

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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val checkLocationAccuracy = sharedPreferences.getBoolean("location_accuracy_check", false)

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !checkLocationAccuracy) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.dialogCheckLocationAccuracyTitle))
                .setMessage(getString(R.string.dialogCheckLocationAccuracyMessage))
                .apply {
                    setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                        val activityIntent = Intent()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            activityIntent.component =
                                ComponentName(
                                    getString(R.string.packageName_gms),
                                    getString(R.string.packageName_gmsLocationAccuracyA12)
                                )
                        } else
                            activityIntent.component =
                                ComponentName(
                                    getString(R.string.packageName_gms),
                                    getString(R.string.packageName_gmsLocationAccuracy)
                                )
                        startActivity(activityIntent)
                    }
                    setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                        Toast.makeText(
                            context,
                            getString(R.string.cancelOperation),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    setNegativeButton(resources.getString(R.string.doNotShowAgain)) { _, _ ->
                        sharedPreferences.edit().putBoolean("location_accuracy_check", true).apply()
                        Snackbar.make(
                            findViewById(R.id.fragment_container_view),
                            getString(R.string.doNotShowAgain),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        checkUpdate()

        val intent = Intent(this, IsolatedService::class.java)
        /*Binding to an isolated service */
        applicationContext.bindService(intent, mIsolatedServiceConnection, BIND_AUTO_CREATE)
    }

    fun magiskCheck() {
        if (this.bServiceBound) {
            val bIsMagisk: Boolean
            try {
                Log.d(TAG, "UID:" + Os.getuid())
                bIsMagisk = serviceBinder!!.isMagiskPresent
                Log.d(TAG, "bIsMagisk: $bIsMagisk")
                supportFragmentManager.setFragmentResult(
                    "bIsMagisk",
                    bundleOf("bundleKey" to bIsMagisk)
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        } else {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val MY_PERMISSIONS_REQUEST_LOCATION = 99
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.requestPermission),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}