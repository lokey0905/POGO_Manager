package app.lokey0905.location.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import app.lokey0905.location.R
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

        fun getDevice() {
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
                view.findViewById<TextView>(R.id.android_abi)?.text =
                    "${Build.SUPPORTED_ABIS[0]} (64位元)"
                view.findViewById<TextView>(R.id.android_ramSize)?.text =
                    "$totalMemory GB(${boolToSupport(totalMemory >= 5)}雙開)"
                view.findViewById<TextView>(R.id.android_pgtoolsSupper)?.text =
                    "✅完整支援暴力功自動抓"
            } else if (Build.SUPPORTED_ABIS[0] == "armeabi-v7a") {
                view.findViewById<TextView>(R.id.android_abi)?.text =
                    "${Build.SUPPORTED_ABIS[0]} (32位元)"
                view.findViewById<TextView>(R.id.android_ramSize)?.text =
                    "$totalMemory GB"
                view.findViewById<TextView>(R.id.android_pgtoolsSupper)?.text =
                    "❌不支援暴力功自動抓"
            } else {
                view.findViewById<TextView>(R.id.android_abi)?.text =
                    Build.SUPPORTED_ABIS[0]
                view.findViewById<TextView>(R.id.android_ramSize)?.text =
                    "$totalMemory GB"
                view.findViewById<TextView>(R.id.android_pgtoolsSupper)?.text =
                    "❌不支援暴力功自動抓"
            }
        }

        fun magiskCheck() {
            if (bIsMagisk) {
                //Toast.makeText(applicationContext, "Magisk Found", Toast.LENGTH_LONG).show()
                view.findViewById<Button>(R.id.check_location)
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this.requireContext(),
                            com.google.android.material.R.color.design_default_color_error
                        )
                    )
                view.findViewById<TextView>(R.id.check_magisk)
                    .setTextColor(
                        ContextCompat.getColor(
                            this.requireContext(),
                            com.google.android.material.R.color.design_default_color_error
                        )
                    )
                view.findViewById<TextView>(R.id.check_magisk)?.text = "❌已發現(未隔離)"
            } else {
                view.findViewById<TextView>(R.id.check_magisk).text = "✅未發現刷機"
                view.findViewById<TextView>(R.id.check_magisk)
                    .setTextColor(ContextCompat.getColor(this.requireContext(), R.color.green))
            }
        }

        val locationListener: LocationListener = object : LocationListener {
            @SuppressLint("SetTextI18n")
            override fun onLocationChanged(location: Location) {
                val locationManager =
                    requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager
                val wifiFix = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (!wifiFix) {
                    view.findViewById<Button>(R.id.check_location)
                        .setBackgroundColor(
                            ContextCompat.getColor(
                                requireActivity(),
                                com.google.android.material.R.color.design_default_color_error
                            )
                        )
                    view.findViewById<Button>(R.id.check_location).text =
                        "${resources.getString(R.string.check_button)} 無法偵測目前位置11"
                } else {
                    view.findViewById<Button>(R.id.check_location).text =
                        resources.getString(R.string.check_button)
                }

                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && newerCheckMockLocationApi) {
                    if (location.isMock) {
                        view.findViewById<Button>(R.id.check_location)
                            .setBackgroundColor(
                                ContextCompat.getColor(
                                    requireActivity(),
                                    com.google.android.material.R.color.design_default_color_error
                                )
                            )
                        view.findViewById<Button>(R.id.check_location).text =
                            "${resources.getString(R.string.check_button)} 無法偵測目前位置12(${Build.VERSION.SDK_INT})"
                    } else view.findViewById<Button>(R.id.check_location).text =
                        resources.getString(R.string.check_button)
                } else {
                    if (location.isFromMockProvider) {
                        view.findViewById<Button>(R.id.check_location)
                            .setBackgroundColor(
                                ContextCompat.getColor(
                                    requireActivity(),
                                    com.google.android.material.R.color.design_default_color_error
                                )
                            )
                        view.findViewById<Button>(R.id.check_location).text =
                            "${resources.getString(R.string.check_button)} 無法偵測目前位置12"
                    } else view.findViewById<Button>(R.id.check_location).text =
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

        view.findViewById<Button>(R.id.check_location).setOnClickListener {
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
        return view
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
}