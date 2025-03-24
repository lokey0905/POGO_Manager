package app.lokey0905.location.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Address
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import app.lokey0905.location.BuildConfig
import app.lokey0905.location.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.roundToInt

class Home : Fragment() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val MY_PERMISSIONS_REQUEST_LOCATION = 1

    var newerCheckMockLocationApi = false
    private var bIsMagisk = false

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val checkLocationButton = view.findViewById<Button>(R.id.check_location)
        val checkInformationLayout = view.findViewById<LinearLayout>(R.id.check_information)
        val checkRootText = view.findViewById<TextView>(R.id.check_root)

        val toolbar =
            view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        if (toolbar != null) {
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
            setHasOptionsMenu(true)
        }

        fun getDevice() {
            val androidAbi = view.findViewById<TextView>(R.id.android_abi)
            val androidRamSize = view.findViewById<TextView>(R.id.android_ramSize)
            val androidPGToolsSupper = view.findViewById<TextView>(R.id.android_pgtoolsSupper)

            val actManager =
                requireActivity().getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalMemory = (memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)).roundToInt()

            view.findViewById<TextView>(R.id.android_devices)?.text =
                getDeviceName()
            view.findViewById<TextView>(R.id.android_version)?.text =
                "${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT}) ${isTestKeyRom()}"

            if (Build.SUPPORTED_ABIS[0] == "arm64-v8a") {
                androidAbi?.text = "${Build.SUPPORTED_ABIS[0]} (64位元)"
                androidRamSize?.text = "$totalMemory GB(${boolToSupport(totalMemory >= 5)}雙開)"
                androidPGToolsSupper?.text = resources.getString(R.string.androidARM64SupperTrue)

            } else {
                if (Build.SUPPORTED_ABIS[0] == "armeabi-v7a") {
                    androidAbi?.text = "${Build.SUPPORTED_ABIS[0]} (32位元)"
                } else {
                    androidAbi?.text = Build.SUPPORTED_ABIS[0]
                }
                androidRamSize?.text = "$totalMemory GB"
                androidPGToolsSupper?.text = resources.getString(R.string.androidARM64SupperFalse)
            }
        }

        fun magiskCheck() {
            if (bIsMagisk) {
                checkLocationButton.setBackgroundColor(
                    ContextCompat.getColor(
                        this.requireContext(),
                        com.google.android.material.R.color.design_default_color_error
                    )
                )
                checkRootText.setTextColor(
                    ContextCompat.getColor(
                        this.requireContext(),
                        com.google.android.material.R.color.design_default_color_error
                    )
                )
                checkRootText?.text = resources.getString(R.string.checkInfo_rootDetected)
            } else {
                checkRootText.setTextColor(
                    ContextCompat.getColor(
                        this.requireContext(),
                        R.color.green
                    )
                )
                checkRootText.text = resources.getString(R.string.checkInfo_rootNotDetected)
            }
        }

        val locationListener: LocationListener = object : LocationListener {
            fun setButtonErrorColor() {
                checkLocationButton
                    .setBackgroundColor(
                        MaterialColors.getColor(view, androidx.appcompat.R.attr.colorError)
                    )
            }

            fun setButtonNormal() {
                checkLocationButton
                    .setBackgroundColor(
                        MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary)
                    )
            }

            @SuppressLint("SetTextI18n")
            override fun onLocationChanged(location: Location) {
                val locationManager =
                    requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager
                val wifiFix = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                var errorFlag = false

                if (!wifiFix) {
                    errorFlag = true
                    setButtonErrorColor()
                    checkLocationButton.text =
                        "${resources.getString(R.string.check_button)} " +
                                "${resources.getString(R.string.locationError)}11" // 11 = 未開啟網路輔助
                }

                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && newerCheckMockLocationApi) { // Android 12
                    if (location.isMock) {
                        errorFlag = true
                        setButtonErrorColor()
                        checkLocationButton.text =
                            "${resources.getString(R.string.check_button)} " +
                                    "${resources.getString(R.string.locationError)}12(${Build.VERSION.SDK_INT})" // 12 = 模擬定位
                    }
                } else {
                    if (location.isFromMockProvider) {
                        errorFlag = true
                        setButtonErrorColor()
                        checkLocationButton.text =
                            "${resources.getString(R.string.check_button)} " +
                                    "${resources.getString(R.string.locationError)}12" // 12 = 模擬定位
                    }
                }

                if (!errorFlag) {
                    setButtonNormal()
                    checkLocationButton.text =
                        resources.getString(R.string.check_button)
                }

                view.findViewById<TextView>(R.id.location_system).text =
                    "${DecimalFormat("#.######").format(location.latitude)},${
                        DecimalFormat("#.######").format(
                            location.longitude
                        )
                    } " + "(${location.provider}) (${if (wifiFix) "Network fix" else "No fix"})"

                val gc = Geocoder(requireActivity(), Locale.getDefault())
                val locationList = gc.getFromLocation(location.latitude, location.longitude, 1)
                val address: Address = locationList!![0]
                var i = 0
                var addressLine = ""
                while (address.getAddressLine(i) != null) {
                    addressLine += address.getAddressLine(i)
                    i++
                }
                view.findViewById<TextView>(R.id.location_system).text =
                    view.findViewById<TextView>(R.id.location_system).text.toString() + "\n${addressLine}"
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) { // 定位狀態改變
            }

            override fun onProviderEnabled(provider: String) { // 定位開啟
                Toast.makeText(context, "已偵測到定位開啟", Toast.LENGTH_SHORT).show()
            }

            override fun onProviderDisabled(provider: String) { // 定位關閉
                Toast.makeText(context, "請開啟gps或是網路", Toast.LENGTH_SHORT).show()
            }
        }

        fun setFragmentResultListener() {
            setFragmentResultListener("newerCheckMockLocationApi") { _, bundle ->
                newerCheckMockLocationApi = bundle.getBoolean("bundleKey")
            }
            setFragmentResultListener("bIsMagisk") { _, bundle ->
                bIsMagisk = bundle.getBoolean("bundleKey")
            }
        }

        getDevice()
        setFragmentResultListener()

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            checkInformationLayout.visibility = View.VISIBLE
        else
            checkInformationLayout.visibility = View.GONE

        view.findViewById<MaterialCardView>(R.id.check).setOnClickListener {
            if (checkInformationLayout.visibility == View.GONE)
                checkInformationLayout.visibility = View.VISIBLE
        }

        checkLocationButton.setOnClickListener {
            if (checkInformationLayout.visibility == View.GONE)
                checkInformationLayout.visibility = View.VISIBLE

            magiskCheck()

            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
                return@setOnClickListener
            } else if (!isLocationEnabled()) {
                Snackbar.make(
                    view,
                    "請開啟GPS或是網路",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val locationRequest = LocationRequest.create().apply {
                interval = 500
                fastestInterval = 100
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    locationResult.locations.forEach { location ->
                        locationListener.onLocationChanged(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            /*val locationManager =
                requireActivity().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    500,
                    1f,
                    locationListener
                )
            } else {
                val bestProvider = locationManager.getBestProvider(Criteria(), true).toString()
                locationManager.requestLocationUpdates(bestProvider, 500, 1f, locationListener)
            }*/

            //val bestProvider = locationManager.getBestProvider(Criteria(), true).toString()
            //locationManager.requestLocationUpdates(bestProvider, 500, 1f, locationListener)
        }

        view.findViewById<Button>(R.id.check_safetynet).setOnClickListener {
            if (appInstalledOrNot(resources.getString(R.string.packageName_integritycheck))) {
                val launchIntent =
                    requireActivity().packageManager.getLaunchIntentForPackage(resources.getString(R.string.packageName_integritycheck))
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            } else if (appInstalledOrNot(resources.getString(R.string.packageName_playintegritychecker))) {
                val launchIntent =
                    requireActivity().packageManager.getLaunchIntentForPackage(resources.getString(R.string.packageName_playintegritychecker))
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            } else {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(resources.getString(R.string.dialogDownloadTitle))
                    .setMessage(resources.getString(R.string.dialogDownloadDetectorMessage))
                    .apply {
                        setPositiveButton(getString(R.string.downloadOnGooglePlay)) { _, _ ->
                            gotoBrowser(resources.getString(R.string.url_integritycheck))
                        }
                        setNeutralButton(R.string.cancel) { _, _ ->
                            Toast.makeText(
                                context,
                                getString(R.string.cancelOperation),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
            }
        }

        view.findViewById<Button>(R.id.check_appList).setOnClickListener {
            if (appInstalledOrNot(resources.getString(R.string.packageName_appListDetector))) {
                val launchIntent =
                    requireActivity().packageManager.getLaunchIntentForPackage(resources.getString(R.string.packageName_appListDetector))
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            } else {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(resources.getString(R.string.dialogDownloadTitle))
                    .setMessage(resources.getString(R.string.dialogDownloadDetectorMessage))
                    .apply {
                        setPositiveButton(getString(R.string.downloadAPK)) { _, _ ->
                            gotoBrowser(resources.getString(R.string.url_appListDetector))
                        }
                        setNeutralButton(R.string.cancel) { _, _ ->
                            Toast.makeText(
                                context,
                                getString(R.string.cancelOperation),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
            }
        }

        MobileAds.initialize(requireActivity())
        val mAdView = view.findViewById<AdView>(R.id.ad_banner)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_download -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.download))
                    .setMessage("是否要重新下載管理器?")
                    .apply {
                        setPositiveButton("重新下載") { _, _ ->
                            gotoBrowser(resources.getString(R.string.url_app))
                        }
                        setNeutralButton(R.string.cancel) { _, _ ->
                            Toast.makeText(
                                context,
                                getString(R.string.cancelOperation),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
                true
            }

            R.id.action_share -> {
                shareText(
                    "${getString(R.string.share_information)}\n${getString(R.string.url_app)}",
                    resources.getString(R.string.shareManager)
                )
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

    private fun isTestKeyRom(): String {
        try {
            for (signature in requireActivity().packageManager.getPackageInfo("android", 64).signatures!!) {
                val hashCode: Int = signature.hashCode()
                if (hashCode == -1263674583 || hashCode == -672009692) {
                    return getString(R.string.rom_testKey)
                }
            }
            return getString(R.string.rom_releaseKey)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return getString(R.string.rom_unknown)
        }
    }


    private fun shareText(text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.putExtra(Intent.EXTRA_TITLE, title)
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    @SuppressLint("SetTextI18n")
    fun showAboutDialog() {
        val dialog = MaterialAlertDialogBuilder(
            requireActivity(),
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
        )
            .create()
        val dialogView: View = View.inflate(requireActivity(), R.layout.dialog_about, null)
        dialog.setView(dialogView)
        dialogView.findViewById<TextView>(R.id.design_about_title).text =
            resources.getString(R.string.app_name)
        dialogView.findViewById<TextView>(R.id.design_about_version).text =
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        dialogView.findViewById<TextView>(R.id.design_about_info).text =
            resources.getString(R.string.dialogAboutInfo)
        dialogView.findViewById<TextView>(R.id.design_about_maker).text =
            resources.getString(R.string.dialogAboutMaker)
        dialog.show()
    }

    private fun boolToSupport(boolean: Boolean): String {
        return if (boolean)
            "支援"
        else
            "不支援"
    }

    private fun getDeviceName(): String {
        var manufacturer =
            Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
        if (Build.BRAND != Build.MANUFACTURER) {
            manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
        }
        manufacturer += " " + Build.MODEL + " "
        return manufacturer
    }

    private fun gotoBrowser(url: String) {
        context?.let {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun appInstalledOrNot(packageName: String): Boolean {
        val pm = activity?.packageManager
        try {
            pm?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return false
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}