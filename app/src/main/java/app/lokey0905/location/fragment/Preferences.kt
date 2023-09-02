package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.lokey0905.location.BuildConfig
import app.lokey0905.location.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class Preferences : PreferenceFragmentCompat() {
    private var test_pgtools: Boolean = false
    private var location_check_A12: Boolean = false
    private var allow_download_on_non_samsung: Boolean = false
    private var always_download_apk_from_apk: Boolean = false

    private fun gotoBrowser(url: String) {
        context?.let {
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
        findPreference<Preference>("allow_download_on_non_samsung")?.isEnabled =
            Build.MANUFACTURER != "samsung"
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "test_pgtools" -> {
                test_pgtools = !test_pgtools
                setFragmentResult("testPgtools", bundleOf("bundleKey" to test_pgtools))
                return true
            }

            "location_accuracy" -> {
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
                return true
            }

            "location_check_A12" -> {
                location_check_A12 = !location_check_A12
                setFragmentResult(
                    "newerCheckMockLocationApi",
                    bundleOf("bundleKey" to location_check_A12)
                )
                return true
            }

            "disable_auto_update" -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(
                        preference.sharedPreferences?.getBoolean("location_check_A12", false)
                            .toString()
                    )
                    .setMessage("點擊右上角三個點取消勾選自動更新")
                    .apply {
                        setNeutralButton(resources.getString(R.string.ok)) { _, _ ->
                            gotoBrowser(resources.getString(R.string.url_pok_store))
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
                allow_download_on_non_samsung = !allow_download_on_non_samsung
                setFragmentResult(
                    "pokAresNoSupportDevices",
                    bundleOf("bundleKey" to allow_download_on_non_samsung)
                )
                return true
            }

            "always_download_apk_from_apk" -> {
                always_download_apk_from_apk = !always_download_apk_from_apk
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