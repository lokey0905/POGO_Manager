package app.lokey0905.location.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
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
import app.lokey0905.location.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.util.*
import kotlin.math.roundToInt

class Home : Fragment() {
    private val locationPermissionCode = 2
    private val MY_PERMISSIONS_REQUEST_LOCATION = 1

    var newerCheckMockLocationApi = false
    var bIsMagisk = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)

        val checkLocationButton = view.findViewById<Button>(R.id.check_location)
        val checkInformationLayout = view.findViewById<LinearLayout>(R.id.check_information)
        val checkRootText = view.findViewById<TextView>(R.id.check_root)

        fun getDevice() {
            val androidAbi = view.findViewById<TextView>(R.id.android_abi)
            val androidRamSize = view.findViewById<TextView>(R.id.android_ramSize)
            val androidPgtoolsSupper = view.findViewById<TextView>(R.id.android_pgtoolsSupper)

            val actManager =
                requireActivity().getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalMemory = (memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)).roundToInt()

            view.findViewById<TextView>(R.id.android_devices)?.text =
                getDeviceName()
            view.findViewById<TextView>(R.id.android_version)?.text =
                "${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT})"

            if (Build.SUPPORTED_ABIS[0] == "arm64-v8a") {
                androidAbi?.text = "${Build.SUPPORTED_ABIS[0]} (64位元)"
                androidRamSize?.text = "$totalMemory GB(${boolToSupport(totalMemory >= 5)}雙開)"
                androidPgtoolsSupper?.text = "✅完整支援暴力功自動抓\n✅完整支援魔物獵人"

            } else if (Build.SUPPORTED_ABIS[0] == "armeabi-v7a") {
                androidAbi?.text = "${Build.SUPPORTED_ABIS[0]} (32位元)"
                androidRamSize?.text = "$totalMemory GB"
                androidPgtoolsSupper?.text = "❌不支援暴力功自動抓\n❌不支援魔物獵人"

            } else {
                androidAbi?.text = Build.SUPPORTED_ABIS[0]
                androidRamSize?.text = "$totalMemory GB"
                androidPgtoolsSupper?.text = "❌不支援暴力功自動抓\n❌不支援魔物獵人"
            }
        }

        fun magiskCheck() {
            if (bIsMagisk) {
                //Toast.makeText(applicationContext, "Magisk Found", Toast.LENGTH_LONG).show()
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
                checkRootText?.text = resources.getString(R.string.rootDetected)
            } else {
                checkRootText.setTextColor(
                    ContextCompat.getColor(
                        this.requireContext(),
                        R.color.green
                    )
                )
                checkRootText.text = resources.getString(R.string.rootNotDetected)
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
                        "${resources.getString(R.string.check_button)} 無法偵測目前位置11"
                }

                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && newerCheckMockLocationApi) {
                    if (location.isMock) {
                        errorFlag = true
                        setButtonErrorColor()
                        checkLocationButton.text =
                            "${resources.getString(R.string.check_button)} 無法偵測目前位置12(${Build.VERSION.SDK_INT})"
                    }
                } else {
                    if (location.isFromMockProvider) {
                        errorFlag = true
                        setButtonErrorColor()
                        checkLocationButton.text =
                            "${resources.getString(R.string.check_button)} 無法偵測目前位置12"
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
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            }

            override fun onProviderEnabled(provider: String) {
                Toast.makeText(context, "已偵測到定位開啟", Toast.LENGTH_SHORT).show()
            }

            override fun onProviderDisabled(provider: String) {
                Toast.makeText(context, "請開啟gps或是網路", Toast.LENGTH_SHORT).show()
            }
        }

        fun getLocation() {
            val locationManager =
                requireActivity().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
            val bestProvider = locationManager.getBestProvider(Criteria(), true).toString()

            if ((ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionCode
                )
            }
            //findViewById<TextView>(R.id.location_system).text = "0.0,0.0"
            /*if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0f,locationListener);
            }else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0f,locationListener);
            }else{
                locationManager.requestLocationUpdates(bestProvider.toString(), 500, 0f,locationListener)
            }*/
            locationManager.requestLocationUpdates(bestProvider, 500, 0f, locationListener)
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1f,locationListener)
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 1f,locationListener)
            //locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 500, 1f,locationListener)
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
            if (context?.let { it1 ->
                    ContextCompat.checkSelfPermission(
                        it1,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "請允許定位權限後在重試", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
            } else {
                getLocation()
            }
            getLocation()
        }

        view.findViewById<Button>(R.id.check_safetynet).setOnClickListener {
            if (appInstalledOrNot(resources.getString(R.string.packageName_checkDevicesAPI))) {
                val launchIntent =
                    requireActivity().packageManager.getLaunchIntentForPackage(resources.getString(R.string.packageName_checkDevicesAPI))
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            } else {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(resources.getString(R.string.dialogDownloadTitle))
                    .setMessage(resources.getString(R.string.dialogDownloadDetectorMessage))
                    .apply {
                        setPositiveButton(getString(R.string.downloadOnGooglePlay)) { _, _ ->
                            gotoBrowser(resources.getString(R.string.url_checkDevicesAPI_official))
                        }
                        setNegativeButton(getString(R.string.downloadAPK)) { _, _ ->
                            gotoBrowser(resources.getString(R.string.url_checkDevicesAPI_unofficial))
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

        return view
    }

    override fun onStart() {
        super.onStart()

        val gridLayout = view?.findViewById<GridLayout>(R.id.gridLayout)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridLayout?.columnCount = 2
        } else {
            gridLayout?.columnCount = 1
        }
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
}