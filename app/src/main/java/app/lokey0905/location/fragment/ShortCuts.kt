package app.lokey0905.location.fragment

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import app.lokey0905.location.R
import app.lokey0905.location.widget.LocationAccuracyActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar


class ShortCuts: Fragment() {
    private var mRewardedAd: RewardedAd? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_shortcuts, container, false)

        fun setButton() {
            view.findViewById<MaterialCardView>(R.id.manual)?.setOnClickListener {
                gotoBrowser(getString(R.string.github_manual))
            }

            view.findViewById<MaterialCardView>(R.id.manual)?.setOnLongClickListener {
                createShortcutWithURL(
                    "manual",
                    getString(R.string.manual),
                    R.drawable.baseline_menu_book_24,
                    getString(R.string.github_manual),
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.coolDownCalculator)?.setOnClickListener {
                gotoBrowser(getString(R.string.url_coolDownCalculator))
            }

            view.findViewById<MaterialCardView>(R.id.coolDownCalculator)?.setOnLongClickListener {
                createShortcutWithURL(
                    "coolDownCalculator",
                    getString(R.string.shortcuts_coolDownCalculator),
                    R.drawable.baseline_calculate_24,
                    getString(R.string.url_coolDownCalculator)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.LocationAccuracyActivity)?.setOnClickListener {
                val activityIntent = Intent()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activityIntent.component =
                        ComponentName(
                            "com.google.android.gms",
                            "com.google.android.gms.location.settings.LocationAccuracyV31Activity"
                        )
                } else
                    activityIntent.component =
                        ComponentName(
                            "com.google.android.gms",
                            "com.google.android.gms.location.settings.LocationAccuracyActivity"
                        )
                startActivity(activityIntent)
            }

            view.findViewById<MaterialCardView>(R.id.LocationAccuracyActivity)
                ?.setOnLongClickListener {
                    createWidget()
                    true
                }

            view.findViewById<MaterialCardView>(R.id.pokeInfo)?.setOnClickListener {
                Toast.makeText(
                    context,
                    getString(R.string.shortcuts_pokeInfoMessage),
                    Toast.LENGTH_LONG
                ).show()
                gotoBrowser(getString(R.string.url_pokeInfo))
            }

            view.findViewById<MaterialCardView>(R.id.pokeInfo)?.setOnLongClickListener {
                Toast.makeText(
                    context,
                    getString(R.string.shortcuts_pokeInfoMessage),
                    Toast.LENGTH_LONG
                ).show()
                createShortcutWithURL(
                    "pokeInfo",
                    getString(R.string.shortcuts_pokeInfo),
                    R.drawable.ic_baseline_catching_pokemon_24,
                    getString(R.string.url_pokeInfo)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.pokeList_web)?.setOnClickListener {
                gotoBrowser(getString(R.string.url_pokeListWeb))
            }

            view.findViewById<MaterialCardView>(R.id.pokeList_web)?.setOnLongClickListener {
                createShortcutWithURL(
                    "pokeList_web",
                    getString(R.string.shortcuts_pokeListWeb),
                    R.drawable.baseline_radar_24,
                    getString(R.string.url_pokeListWeb)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.PGTools_raids)?.setOnClickListener {
                gotoBrowser(getString(R.string.url_PGToolsRaids))
            }

            view.findViewById<MaterialCardView>(R.id.PGTools_raids)?.setOnLongClickListener {
                createShortcutWithURL(
                    "PGTools_raids",
                    getString(R.string.shortcuts_PGToolsRaids),
                    R.drawable.baseline_radar_24,
                    getString(R.string.url_PGToolsRaids)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.action_nearbySharing)?.setOnClickListener {
                val activityIntent = Intent()
                if (appInstalledOrNot("com.samsung.android.app.sharelive")) {
                    activityIntent.component = ComponentName(
                        "com.samsung.android.app.sharelive",
                        "com.samsung.android.app.sharelive.presentation.main.MainActivity"
                    )
                } else {
                    activityIntent.component = ComponentName(
                        "com.google.android.gms",
                        "com.google.android.gms.nearby.sharing.settings.SettingsActivity"
                    )
                }

                startActivity(activityIntent)
            }

            view.findViewById<MaterialCardView>(R.id.MiuiXSpace)?.setOnClickListener {
                if (appInstalledOrNot("com.miui.securitycore")) {
                    val activityIntent = Intent()
                    activityIntent.component =
                        ComponentName(
                            "com.miui.securitycore",
                            "com.miui.xspace.ui.activity.XSpaceSettingActivity"
                        )
                    startActivity(activityIntent)
                } else {
                    Snackbar.make(
                        view,
                        "${resources.getString(R.string.unsupportedDevices)}(非MIUI系統)",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Action", null).show()
                }

            }
        }

        fun setDownloadButton() {
            view.findViewById<MaterialCardView>(R.id.download_gpx1)?.setOnClickListener {
                downloadAPPWithCheck(resources.getString(R.string.url_gpx1))
            }

            view.findViewById<MaterialCardView>(R.id.download_gpx2)?.setOnClickListener {
                downloadAPPWithCheck(resources.getString(R.string.url_gpx2))
            }

            view.findViewById<MaterialCardView>(R.id.download_gpx3)?.setOnClickListener {
                downloadAPPWithCheck(resources.getString(R.string.url_gpx3))
            }

            view.findViewById<MaterialCardView>(R.id.download_gpx4)?.setOnClickListener {
                downloadAPPWithCheck(resources.getString(R.string.url_gpx4))
            }

            view.findViewById<MaterialCardView>(R.id.pgtoolsFile)?.setOnClickListener {
                downloadAPPWithCheck(getString(R.string.url_PGToolsFile))
            }

            view.findViewById<MaterialCardView>(R.id.PackageDisablerPro)?.setOnClickListener {
                downloadAPPWithCheck(resources.getString(R.string.url_PackageDisablerPro))
            }

            view.findViewById<MaterialCardView>(R.id.AFWall)?.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.dialogDownloadTitle))
                    .setMessage(resources.getString(R.string.dialogDownloadOfficialMessage))
                    .setNeutralButton(R.string.cancel) { _, _ -> }
                    .setNegativeButton(getString(R.string.downloadAPK)) { _, _ ->
                        downloadAPPWithCheck(resources.getString(R.string.url_AFWall_unofficial))
                    }
                    .setPositiveButton(getString(R.string.downloadOnGooglePlay)) { _, _ ->
                        downloadAPPWithCheck(resources.getString(R.string.url_AFWall_official))
                    }
                    .show()
            }

            view.findViewById<MaterialCardView>(R.id.gameGuardian)?.setOnClickListener {
                downloadAPPWithCheck(resources.getString(R.string.url_gameGuardian))
            }

            view.findViewById<MaterialCardView>(R.id.downloadSplitScreen)?.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.dialogDownloadTitle))
                    .setMessage(resources.getString(R.string.dialogDownloadOfficialMessage))
                    .setNeutralButton(R.string.cancel) { _, _ -> }
                    .setNegativeButton(getString(R.string.downloadAPK)) { _, _ ->
                        downloadAPPWithCheck(resources.getString(R.string.url_split_unofficial))
                    }
                    .setPositiveButton(getString(R.string.downloadOnGooglePlay)) { _, _ ->
                        downloadAPPWithCheck(resources.getString(R.string.url_split_official))
                    }
                    .show()
            }

            view.findViewById<MaterialCardView>(R.id.downloadIsland)?.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.dialogDownloadTitle))
                    .setMessage(resources.getString(R.string.dialogDownloadOfficialMessage))
                    .setNeutralButton(R.string.cancel) { _, _ -> }
                    .setNegativeButton(getString(R.string.downloadAPK)) { _, _ ->
                        downloadAPPWithCheck(resources.getString(R.string.url_island_unofficial))
                    }
                    .setPositiveButton(getString(R.string.downloadOnGooglePlay)) { _, _ ->
                        downloadAPPWithCheck(resources.getString(R.string.url_island_official))
                    }
                    .show()
            }

            view.findViewById<MaterialCardView>(R.id.downloadPokeMod)?.setOnClickListener {
                downloadAPPWithCheck(getString(R.string.url_pokeMod))
            }
        }

        setButton()
        setDownloadButton()
        // Inflate the layout for this fragment
        return view
    }

    override fun onStart() {
        super.onStart()

        val gridLayout1 = view?.findViewById<GridLayout>(R.id.gridLayout1)
        val gridLayout2 = view?.findViewById<GridLayout>(R.id.gridLayout2)
        val gridLayout3 = view?.findViewById<GridLayout>(R.id.gridLayout3)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridLayout1?.columnCount = 4
            gridLayout2?.columnCount = 4
            gridLayout3?.columnCount = 4
        } else {
            gridLayout1?.columnCount = 2
            gridLayout2?.columnCount = 2
            gridLayout3?.columnCount = 2
        }
    }

    private fun createWidget() {
        val appWidgetManager = requireContext().getSystemService(AppWidgetManager::class.java)

        val myProvider = ComponentName(requireContext(), LocationAccuracyActivity::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val pinnedWidgetCallbackIntent = Intent()

            val successCallback = PendingIntent.getBroadcast(
                context,
                0,
                pinnedWidgetCallbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)

            if (Build.MANUFACTURER == "Xiaomi") {
                Toast.makeText(context, "捷徑已建立", Toast.LENGTH_SHORT).show()
            }
        } else Toast.makeText(
            context,
            "不支援釘選快捷方式!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun createShortcutWithURL(id: String, label: String, icon: Int, url: String) {
        val shortcutManager = context?.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null) {
            if (shortcutManager.isRequestPinShortcutSupported) {
                val shortcut = ShortcutInfo.Builder(context, id)
                    .setShortLabel(label)
                    .setIcon(Icon.createWithResource(context, icon))
                    .setIntent(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )
                    )
                    .build()
                shortcutManager.requestPinShortcut(shortcut, null)

                if (Build.MANUFACTURER == "Xiaomi") {
                    Toast.makeText(context, "捷徑已建立", Toast.LENGTH_SHORT).show()
                }
            } else Toast.makeText(
                context,
                "不支援釘選快捷方式!",
                Toast.LENGTH_SHORT
            ).show()
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

    private fun showAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(R.string.ok) { _, _ -> }
            .show()
    }

    private fun gotoBrowser(url: String) {
        context?.let {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val customTabsOff = sharedPreferences.getBoolean("customTabsOff", false)

            if (customTabsOff)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else
                CustomTabsIntent.Builder().build()
                    .launchUrl(it, Uri.parse(url))
        }
    }

    private fun downloadAPPWithCheck(url: String) {
        if (url == "") {
            showAlertDialog(
                resources.getString(R.string.dialogAdNotReadyTitle),
                resources.getString(R.string.dialogAdNotReadyMessage)
            )
            return
        }

        val factory = LayoutInflater.from(requireContext())
        val imageView: View = factory.inflate(R.layout.dialog_imageview, null)
        var setview = false

        if (url.contains("mediafire")) {
            imageView.findViewById<ImageView>(R.id.dialog_imageview)
                .setImageResource(R.drawable.download_mediafire)
            setview = true
        } else if (url.contains("apkmirror") || url.contains("bit.ly")) {
            imageView.findViewById<ImageView>(R.id.dialog_imageview)
                .setImageResource(R.drawable.download_apk_e)
            setview = true
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(if (setview) imageView else null)
            .setTitle(resources.getString(R.string.dialogDownloadTitle))
            .setMessage(resources.getString(R.string.dialogDownloadMessage))
            .apply {
                setNeutralButton(R.string.cancel) { _, _ ->
                    Toast.makeText(context, getString(R.string.cancelOperation), Toast.LENGTH_SHORT)
                        .show()
                }
                setPositiveButton(R.string.ok) { _, _ ->
                    if (url.endsWith(".apk"))
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    else
                        gotoBrowser(url)
                }
                setNegativeButton(R.string.downloadProblem) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
            .show()
    }
}