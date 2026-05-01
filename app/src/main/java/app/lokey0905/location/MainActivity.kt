package app.lokey0905.location

import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.system.Os
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import app.lokey0905.location.databinding.ActivityMainBinding
import app.lokey0905.location.fragment.AppsMHN
import app.lokey0905.location.fragment.AppsPoke
import app.lokey0905.location.fragment.Home
import app.lokey0905.location.fragment.Preferences
import app.lokey0905.location.fragment.ShortCuts
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.onesignal.OneSignal
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.edit
import androidx.core.net.toUri
import android.content.SharedPreferences
import android.provider.Settings



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var home: Home = Home()
    private var appsPoke: AppsPoke = AppsPoke()
    private var appsMhn: AppsMHN = AppsMHN()
    private var shortcuts: ShortCuts = ShortCuts()
    private var preferenceFragmentCompat: Preferences = Preferences()
    private val navigationOrder = listOf(
        R.id.nav_mhn,
        R.id.nav_home,
        R.id.nav_shortcuts,
        R.id.nav_setting
    )
    private var currentNavIndex = 0

    var bServiceBound = false

    //private var IIsolatedService = null
    var serviceBinder: IIsolatedService? = null
    private val ACTION_LOCATION_SCANNING_SETTINGS =
        "android.settings.LOCATION_SCANNING_SETTINGS"

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkForUpdate(
        githubUrl: String,
        onUpdateAvailable: (Boolean, String, String, Boolean) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            var updateAvailable = false
            var latestVersion = ""
            var latestVersionInformation = ""
            var requestSuccess = true

            try {
                val url = URL(githubUrl)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream)).use { bufferedReader ->
                    var line: String? = bufferedReader.readLine()
                    while (line != null) {
                        response.append(line)
                        line = bufferedReader.readLine()
                    }
                }

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
                requestSuccess = false
                Log.w(TAG, "checkForUpdate failed", e)
            }

            launch(Dispatchers.Main) {
                onUpdateAvailable(
                    updateAvailable,
                    latestVersion,
                    latestVersionInformation,
                    requestSuccess
                )
            }
        }
    }

    private fun gotoBrowser(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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

    private fun replaceFragment(fragment: Fragment, targetIndex: Int) {
        supportFragmentManager.executePendingTransactions()

        val (enterAnim, exitAnim) = if (targetIndex > currentNavIndex) {
            R.anim.fragment_slide_in_right to R.anim.fragment_slide_out_left
        } else {
            R.anim.fragment_slide_in_left to R.anim.fragment_slide_out_right
        }

        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(enterAnim, exitAnim)
            if (!fragment.isAdded) {
                add(R.id.fragment_container_view, fragment)
            }
            supportFragmentManager.fragments.forEach {
                if (it != fragment && it.isAdded) {
                    hide(it)
                }
            }
            show(fragment)
        }.commitNow()
        currentNavIndex = targetIndex
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
            val targetIndex = when (item.itemId) {
                R.id.nav_poke -> -1
                else -> navigationOrder.indexOf(item.itemId)
            }
            when (item.itemId) {
                R.id.nav_poke -> {
                    replaceFragment(appsPoke, -1)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_mhn -> {
                    replaceFragment(appsMhn, targetIndex)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_home -> {
                    replaceFragment(home, targetIndex)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_shortcuts -> {
                    replaceFragment(shortcuts, targetIndex)
                    return@setOnItemSelectedListener true
                }

                R.id.nav_setting -> {
                    replaceFragment(preferenceFragmentCompat, targetIndex)
                    return@setOnItemSelectedListener true
                }
            }
            false
        }

        if (savedInstanceState == null)
            findViewById<BottomNavigationView>(R.id.navigation).selectedItemId = R.id.nav_home

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                runCatching {
                    OneSignal.Notifications.requestPermission(true)
                }.onFailure { e ->
                    Log.w(TAG, "OneSignal permission request failed", e)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        checkLocationAccuracy(locationManager, sharedPreferences)
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun compareVersions(current: String, latest: String): Int {
        val currentParts = current.split(".")
        val latestParts = latest.split(".")
        val maxSize = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxSize) {
            val currentValue = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val latestValue = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (currentValue != latestValue) {
                return currentValue.compareTo(latestValue)
            }
        }
        return 0
    }

    private fun checkUpdate() {
        if (!isNetworkAvailable()) {
            Toast.makeText(
                this,
                getString(R.string.networkError),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val githubUrl = getString(R.string.githubApi)
        checkForUpdate(githubUrl) { updateAvailable, latestVersion, latestVersionInformation, requestSuccess ->
            if (!requestSuccess) {
                Toast.makeText(
                    this,
                    getString(R.string.networkError),
                    Toast.LENGTH_SHORT
                ).show()
                return@checkForUpdate
            }

            val versionCompare = compareVersions(BuildConfig.VERSION_NAME, latestVersion)
            if (versionCompare > 0) {
                Toast.makeText(
                    this,
                    getString(R.string.testVersion),
                    Toast.LENGTH_SHORT
                ).show()
                return@checkForUpdate
            }

            if (updateAvailable) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(
                        getString(
                            R.string.dialogUpdateDetectedVersion,
                            BuildConfig.VERSION_NAME,
                            latestVersion
                        )
                    )
                    .setMessage("${getString(R.string.dialogUpdateAvailableManagerMessage)}\n\n${
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

    private fun checkLocationAccuracy(
        locationManager: LocationManager,
        sharedPreferences: SharedPreferences
    ) {
        val checkLocationAccuracy = sharedPreferences.getBoolean("location_accuracy_check", false)
        if (checkLocationAccuracy) {
            return
        }

        val wifiScanEnabled = isWifiScanAlwaysEnabled()
        val bleScanEnabled = isBleScanAlwaysEnabled()
        Log.d(TAG, "Wi-Fi scan always enabled: $wifiScanEnabled, BLE scan always enabled: $bleScanEnabled")

        if (isGooglePlayServicesAvailable()) {
            val settingsClient = LocationServices.getSettingsClient(this)
            settingsClient.isGoogleLocationAccuracyEnabled
                .addOnSuccessListener { enabled ->
                    val shouldShow = enabled || wifiScanEnabled || bleScanEnabled
                    if (shouldShow) {
                        showLocationAccuracyDialog(
                            sharedPreferences,
                            enabled,
                            wifiScanEnabled,
                            bleScanEnabled
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Google location accuracy check failed", e)
                    val shouldShow =
                        wifiScanEnabled || bleScanEnabled ||
                            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    if (shouldShow) {
                        showLocationAccuracyDialog(
                            sharedPreferences,
                            null,
                            wifiScanEnabled,
                            bleScanEnabled
                        )
                    }
                }
            return
        } else {
            Log.w(TAG, "Google Play Services not available")
            val shouldShow =
                wifiScanEnabled || bleScanEnabled ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (shouldShow) {
                showLocationAccuracyDialog(
                    sharedPreferences,
                    null,
                    wifiScanEnabled,
                    bleScanEnabled
                )
            }
        }
    }

    private fun showLocationAccuracyDialog(
        sharedPreferences: SharedPreferences,
        googleAccuracyEnabled: Boolean?,
        wifiScanEnabled: Boolean,
        bleScanEnabled: Boolean
    ) {
        val accuracyStatus = when (googleAccuracyEnabled) {
            true -> getString(R.string.status_enabled)
            false -> getString(R.string.status_disabled)
            null -> getString(R.string.status_unknown)
        }
        val wifiStatus = if (wifiScanEnabled) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_disabled)
        }
        val bleStatus = if (bleScanEnabled) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_disabled)
        }

        val statusMessage = getString(
            R.string.dialogCheckLocationAccuracyStatusMessage,
            accuracyStatus,
            wifiStatus,
            bleStatus,
            getString(R.string.dialogCheckLocationAccuracyMessage)
        )

        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle(getString(R.string.dialogCheckLocationAccuracyTitle))
            .setMessage(statusMessage)
            .apply {
                setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    openLocationAccuracyRelatedSettings(
                        googleAccuracyEnabled,
                        wifiScanEnabled,
                        bleScanEnabled
                    )
                }
                setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                    Toast.makeText(
                        context,
                        getString(R.string.cancelOperation),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                setNegativeButton(resources.getString(R.string.doNotShowAgain)) { _, _ ->
                    sharedPreferences.edit { putBoolean("location_accuracy_check", true) }
                    Snackbar.make(
                        findViewById(R.id.fragment_container_view),
                        getString(R.string.doNotShowAgain),
                        Snackbar.LENGTH_SHORT
                    ).setAction(getString(R.string.cancel)) {
                        sharedPreferences.edit { putBoolean("location_accuracy_check", false) }
                    }.show()
                }
            }
            .show()
    }

    private fun isWifiScanAlwaysEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                contentResolver,
                "wifi_scan_always_enabled",
                0
            ) == 1
        } catch (e: SecurityException) {
            Log.w(TAG, "Wi-Fi scan setting not accessible", e)
            false
        }
    }

    private fun openLocationAccuracyRelatedSettings(
        googleAccuracyEnabled: Boolean?,
        wifiScanEnabled: Boolean,
        bleScanEnabled: Boolean
    ) {
        when {
            googleAccuracyEnabled == true -> {
                val gmsAccuracyIntent = Intent().apply {
                    component = ComponentName(
                        getString(R.string.packageName_gms),
                        getString(R.string.packageName_gmsLocationAccuracy)
                    )
                }

                startActivitySafely(
                    gmsAccuracyIntent,
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            }

            wifiScanEnabled || bleScanEnabled -> {
                startActivitySafely(
                    Intent(ACTION_LOCATION_SCANNING_SETTINGS),
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            }

            else -> {
                startActivitySafely(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            }
        }
    }

    private fun isBleScanAlwaysEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                contentResolver,
                "ble_scan_always_enabled",
                0
            ) == 1
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE scan setting not accessible", e)
            false
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun startActivitySafely(vararg intents: Intent) {
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start intent: ${intent.action}, component=${intent.component}", e)
            }
        }

        Toast.makeText(
            this,
            getString(R.string.status_startFailed),
            Toast.LENGTH_SHORT
        ).show()
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
