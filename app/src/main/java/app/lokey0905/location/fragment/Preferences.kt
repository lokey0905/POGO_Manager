package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import app.lokey0905.location.BuildConfig
import app.lokey0905.location.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar


class Preferences : PreferenceFragmentCompat() {
    private var allow_download_on_non_arm64: Boolean = false
    private var location_accuracy_switch: Boolean = false
    private var location_check_A12: Boolean = false
    private var location_accuracy_check: Boolean = false
    private var allow_download_on_non_samsung: Boolean = false
    private var always_download_apk_from_apk: Boolean = false
    private var customTabsOff: Boolean = false

    private fun gotoBrowser(url: String) {
        context?.let {
            if (customTabsOff)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else
                CustomTabsIntent.Builder().build()
                    .launchUrl(it, Uri.parse(url))
        }
    }

    @SuppressLint("SetTextI18n")
    fun showAboutDialog() {
        val dialog = MaterialAlertDialogBuilder(
            requireContext(),
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
        )
            .create()
        val dialogView: View = View.inflate(requireContext(), R.layout.dialog_about, null)
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("app")?.summary =
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        findPreference<Preference>("location_check_A12")?.isEnabled =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        findPreference<Preference>("always_download_apk_from_apk")?.isEnabled =
            Build.MANUFACTURER == "samsung"

        findPreference<Preference>("allow_download_on_non_arm64")?.isEnabled =
            !Build.SUPPORTED_ABIS.contains("arm64-v8a")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        allow_download_on_non_arm64 =
            sharedPreferences.getBoolean("allow_download_on_non_arm64", false)
        location_accuracy_switch =
            sharedPreferences.getBoolean("location_accuracy_switch", false)
        location_check_A12 =
            sharedPreferences.getBoolean("location_check_A12", false)
        location_accuracy_check =
            sharedPreferences.getBoolean("location_accuracy_check", false)
        allow_download_on_non_samsung =
            sharedPreferences.getBoolean("allow_download_on_non_samsung", false)
        always_download_apk_from_apk =
            sharedPreferences.getBoolean("always_download_apk_from_apk", false)
        customTabsOff =
            sharedPreferences.getBoolean("customTabsOff", false)

        setFragmentResult(
            "newerCheckMockLocationApi",
            bundleOf("bundleKey" to location_check_A12)
        )

        setFragmentResult(
            "pokAresNoSupportDevices",
            bundleOf("bundleKey" to allow_download_on_non_samsung)
        )

        setFragmentResult(
            "pokAresDownloadAPK",
            bundleOf("bundleKey" to always_download_apk_from_apk)
        )
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        when (preference.key) {
            "customTabsOff" -> {
                customTabsOff = sharedPreferences.getBoolean("customTabsOff", false)
                return true
            }

            "allow_download_on_non_arm64" -> {
                allow_download_on_non_arm64 =
                    sharedPreferences.getBoolean("allow_download_on_non_arm64", false)
                return true
            }

            "location_accuracy" -> {
                if (location_accuracy_switch) {
                    // open location settings page
                    val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)

                } else {
                    val activityIntent = Intent()

                    activityIntent.component =
                        ComponentName(
                            getString(R.string.packageName_gms),
                            getString(R.string.packageName_gmsLocationAccuracy)
                        )

                    startActivity(activityIntent)
                }
                return true
            }

            "location_accuracy_switch" -> {
                location_accuracy_switch =
                    sharedPreferences.getBoolean("location_accuracy_switch", false)
                setFragmentResult(
                    "location_accuracy_switch",
                    bundleOf("bundleKey" to location_accuracy_switch)
                )
                return true
            }

            "location_check_A12" -> {
                location_check_A12 = sharedPreferences.getBoolean("location_check_A12", false)
                setFragmentResult(
                    "newerCheckMockLocationApi",
                    bundleOf("bundleKey" to location_check_A12)
                )
                return true
            }

            "location_accuracy_check" -> {
                location_accuracy_check =
                    sharedPreferences.getBoolean("location_accuracy_check", false)
                setFragmentResult(
                    "location_accuracy_check",
                    bundleOf("bundleKey" to location_accuracy_check)
                )
                Snackbar.make(
                    requireView(),
                    "請重新開啟APP以套用設定",
                    Snackbar.LENGTH_SHORT
                ).show()
                return true
            }

            "disable_auto_update_pok" -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("取消自動更新")
                    .setMessage("點擊右上角三個點取消勾選自動更新")
                    .apply {
                        setNeutralButton(resources.getString(R.string.ok)) { _, _ ->
                            gotoBrowser(
                                resources.getString(R.string.url_googlePlay) +
                                        resources.getString(R.string.packageName_pok)
                            )
                            Toast.makeText(
                                context,
                                "點擊右上角三個點取消勾選自動更新",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        setPositiveButton(resources.getString(R.string.cancel)) { _, _ ->
                            Toast.makeText(
                                context,
                                getString(R.string.cancelOperation),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
                return true
            }

            "disable_auto_update_mhn" -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("取消自動更新")
                    .setMessage("點擊右上角三個點取消勾選自動更新")
                    .apply {
                        setNeutralButton(resources.getString(R.string.ok)) { _, _ ->
                            gotoBrowser(
                                resources.getString(R.string.url_googlePlay) +
                                        resources.getString(R.string.packageName_MHNow)
                            )
                            Toast.makeText(
                                context,
                                "點擊右上角三個點取消勾選自動更新",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        setPositiveButton(resources.getString(R.string.cancel)) { _, _ ->
                            Toast.makeText(
                                context,
                                getString(R.string.cancelOperation),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
                return true
            }

            "allow_download_on_non_samsung" -> {
                allow_download_on_non_samsung =
                    sharedPreferences.getBoolean("allow_download_on_non_samsung", false)
                setFragmentResult(
                    "pokAresNoSupportDevices",
                    bundleOf("bundleKey" to allow_download_on_non_samsung)
                )
                return true
            }

            "always_download_apk_from_apk" -> {
                always_download_apk_from_apk =
                    sharedPreferences.getBoolean("always_download_apk_from_apk", false)
                setFragmentResult(
                    "pokAresDownloadAPK",
                    bundleOf("bundleKey" to always_download_apk_from_apk)
                )
                return true
            }

            "shopee" -> {
                gotoBrowser(resources.getString(R.string.shopee))
                return true
            }

            "facebook" -> {
                gotoBrowser(resources.getString(R.string.facebook))
                return true
            }

            "discord" -> {
                gotoBrowser(resources.getString(R.string.discord))
                return true
            }

            "github" -> {
                gotoBrowser(resources.getString(R.string.github))
                return true
            }

            "app" -> {
                showAboutDialog()
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}