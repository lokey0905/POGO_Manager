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
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import app.lokey0905.location.databinding.ActivityMainBinding
import app.lokey0905.location.fragment.AppsMHN
import app.lokey0905.location.fragment.AppsPoke
import app.lokey0905.location.fragment.Home
import app.lokey0905.location.fragment.Preferences
import app.lokey0905.location.fragment.ShortCuts
import app.lokey0905.location.navigation.MainTab
import app.lokey0905.location.navigation.TabNavigationState
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
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

    private companion object {
        const val STATE_TAB_HISTORY = "main_tab_history"
    }

    private val tabFragments = mutableMapOf<MainTab, Fragment>()
    private lateinit var tabNavigation: TabNavigationState
    private val tabBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val previous = tabNavigation.previous ?: return
            if (showTab(previous)) {
                tabNavigation.goBack()
                syncNavigationSelection()
            }
        }
    }

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

    private fun MainTab.menuId(): Int = when (this) {
        MainTab.POKEMON -> R.id.nav_poke
        MainTab.MONSTER_HUNTER -> R.id.nav_mhn
        MainTab.HOME -> R.id.nav_home
        MainTab.SHORTCUTS -> R.id.nav_shortcuts
        MainTab.SETTINGS -> R.id.nav_setting
    }

    private fun tabOf(fragment: Fragment): MainTab? = when (fragment) {
        is AppsPoke -> MainTab.POKEMON
        is AppsMHN -> MainTab.MONSTER_HUNTER
        is Home -> MainTab.HOME
        is ShortCuts -> MainTab.SHORTCUTS
        is Preferences -> MainTab.SETTINGS
        else -> null
    }

    private fun createTab(tab: MainTab): Fragment = when (tab) {
        MainTab.POKEMON -> AppsPoke()
        MainTab.MONSTER_HUNTER -> AppsMHN()
        MainTab.HOME -> Home()
        MainTab.SHORTCUTS -> ShortCuts()
        MainTab.SETTINGS -> Preferences()
    }

    private fun tabTag(tab: MainTab): String = "main_tab_${tab.name}"

    private fun restoreNavigation(savedInstanceState: Bundle?) {
        val restored = supportFragmentManager.fragments.filter {
            it.id == R.id.fragment_container_view && tabOf(it) != null
        }
        MainTab.entries.forEach { tab ->
            // Adopt old, untagged instances too; never create another copy on rotation.
            val fragment = supportFragmentManager.findFragmentByTag(tabTag(tab))
                ?: restored.firstOrNull { tabOf(it) == tab && !it.isHidden }
                ?: restored.firstOrNull { tabOf(it) == tab }
            if (fragment != null) tabFragments[tab] = fragment
        }
        val visibleTab = restored.firstOrNull { !it.isHidden }?.let(::tabOf)
        tabNavigation = TabNavigationState.restore(
            savedInstanceState?.getStringArrayList(STATE_TAB_HISTORY),
            visibleTab ?: MainTab.HOME
        )
        showTab(tabNavigation.current, animate = false)
        syncNavigationSelection()
        onBackPressedDispatcher.addCallback(this, tabBackCallback)

        binding.navigation.setOnItemSelectedListener { item ->
            val tab = MainTab.entries.firstOrNull { it.menuId() == item.itemId }
                ?: return@setOnItemSelectedListener false
            if (tab == tabNavigation.current) return@setOnItemSelectedListener true
            if (!showTab(tab)) return@setOnItemSelectedListener false
            tabNavigation.select(tab)
            syncNavigationSelection()
            true
        }
        binding.navigation.setOnItemReselectedListener {
            // Reselecting the current tab is deliberately a no-op, not a refresh.
        }
    }

    private fun syncNavigationSelection() {
        // Checking the menu item does not call the selection listener or append history.
        binding.navigation.menu.findItem(tabNavigation.current.menuId()).isChecked = true
        tabBackCallback.isEnabled = tabNavigation.canGoBack
    }

    private fun showTab(tab: MainTab, animate: Boolean = true): Boolean {
        val manager = supportFragmentManager
        if (manager.isStateSaved || manager.isDestroyed) return false
        val fragment = tabFragments.getOrPut(tab) { createTab(tab) }
        manager.beginTransaction().apply {
            setReorderingAllowed(true)
            if (animate) setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
            if (!fragment.isAdded) {
                add(R.id.fragment_container_view, fragment, tabTag(tab))
            }
            manager.fragments.forEach { other ->
                val otherTab = tabOf(other)
                if (other.id == R.id.fragment_container_view && otherTab != null && other != fragment) {
                    if (tabFragments[otherTab] !== other) {
                        // Clean up duplicates restored from the old untagged navigation.
                        remove(other)
                    } else {
                        hide(other)
                        setMaxLifecycle(other, Lifecycle.State.STARTED)
                    }
                }
            }
            show(fragment)
            setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
            setPrimaryNavigationFragment(fragment)
        }.commitNow()
        Log.d("MainNavigation", "Selected $tab; retainedTabs=${tabFragments.size}")
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(STATE_TAB_HISTORY, tabNavigation.save())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        syncNavigationSelection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setSupportActionBar(binding.toolbar)

        restoreNavigation(savedInstanceState)

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
