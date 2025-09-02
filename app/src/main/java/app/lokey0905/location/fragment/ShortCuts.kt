package app.lokey0905.location.fragment

import android.app.PendingIntent
import android.appwidget.AppWidgetManager // don't touch this line, it's needed for widget
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ShortCuts: Fragment() {
    private var mRewardedAd: RewardedAd? = null

    private var downloadData: JSONObject? = null
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
                createShortcut(
                    "manual",
                    getString(R.string.manual),
                    R.drawable.baseline_menu_book_24,
                    url = getString(R.string.github_manual),
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.coolDownCalculator)?.setOnClickListener {
                gotoBrowser(getString(R.string.url_coolDownCalculator))
            }

            view.findViewById<MaterialCardView>(R.id.coolDownCalculator)?.setOnLongClickListener {
                createShortcut(
                    "coolDownCalculator",
                    getString(R.string.shortcuts_coolDownCalculator),
                    R.drawable.baseline_calculate_24,
                    url = getString(R.string.url_coolDownCalculator)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.LocationAccuracyActivity)?.setOnClickListener {
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                val location_accuracy_switch =
                    sharedPreferences.getBoolean("location_accuracy_switch", false)
                if (location_accuracy_switch) {
                    // open location settings page
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
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
                createShortcut(
                    "pokeInfo",
                    getString(R.string.shortcuts_pokeInfo),
                    R.drawable.ic_baseline_catching_pokemon_24,
                    url = getString(R.string.url_pokeInfo)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.pokeList_web)?.setOnClickListener {
                gotoBrowser(getString(R.string.url_pokeListWeb))
            }

            view.findViewById<MaterialCardView>(R.id.pokeList_web)?.setOnLongClickListener {
                createShortcut(
                    "pokeList_web",
                    getString(R.string.shortcuts_pokeListWeb),
                    R.drawable.baseline_radar_24,
                    url = getString(R.string.url_pokeListWeb)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.PGTools_raids)?.setOnClickListener {
                gotoBrowser(getString(R.string.url_PGToolsRaids))
            }

            view.findViewById<MaterialCardView>(R.id.PGTools_raids)?.setOnLongClickListener {
                createShortcut(
                    "PGTools_raids",
                    getString(R.string.shortcuts_PGToolsRaids),
                    R.drawable.baseline_radar_24,
                    url = getString(R.string.url_PGToolsRaids)
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

            view.findViewById<MaterialCardView>(R.id.action_nearbySharing)?.setOnLongClickListener {
                createShortcut(
                    "action_nearbySharing",
                    getString(R.string.nearbySharing),
                    R.drawable.baseline_share_24,
                    intent = Intent(Intent.ACTION_MAIN).setComponent(
                        if (appInstalledOrNot("com.samsung.android.app.sharelive")) {
                            ComponentName(
                                "com.samsung.android.app.sharelive",
                                "com.samsung.android.app.sharelive.presentation.main.MainActivity"
                            )
                        } else {
                            ComponentName(
                                "com.google.android.gms",
                                "com.google.android.gms.nearby.sharing.settings.SettingsActivity"
                            )
                        }
                    )
                )

                true
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
                        "${resources.getString(R.string.unsupportedDevices)}(${resources.getString(R.string.isNotMIUI)})",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Action", null).show()
                }
            }

            view.findViewById<MaterialCardView>(R.id.MiuiXSpace)?.setOnLongClickListener {
                if (appInstalledOrNot("com.miui.securitycore")) {
                    createShortcut(
                        "MiuiXSpace",
                        getString(R.string.shortcuts_MiuiXSpace),
                        R.drawable.baseline_call_split_24,
                        intent = Intent(Intent.ACTION_MAIN).setComponent(
                            ComponentName(
                                "com.miui.securitycore",
                                "com.miui.xspace.ui.activity.XSpaceSettingActivity"
                            )
                        )
                    )
                } else {
                    Snackbar.make(
                        view,
                        "${resources.getString(R.string.unsupportedDevices)}(${resources.getString(R.string.isNotMIUI)})",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Action", null).show()
                }

                true
            }

            view.findViewById<MaterialCardView>(R.id.location_settings)?.setOnClickListener {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }

            view.findViewById<MaterialCardView>(R.id.location_settings)?.setOnLongClickListener {
                createShortcut(
                    "location_settings",
                    getString(R.string.shortcuts_locationSettings),
                    R.drawable.baseline_edit_location_24,
                    intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
                true
            }

            view.findViewById<MaterialCardView>(R.id.developerMode)?.setOnClickListener {
                //check if developer mode is enabled
                if (Settings.Global.getInt(
                        context?.contentResolver,
                        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                        0
                    ) != 0
                ) {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    startActivity(intent)
                } else {
                    Snackbar.make(
                        view,
                        resources.getString(R.string.developerModeIsOff),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            view.findViewById<MaterialCardView>(R.id.developerMode)?.setOnLongClickListener {
                if (Settings.Global.getInt(
                        context?.contentResolver,
                        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                        0
                    ) != 0
                ) {
                    createShortcut(
                        "developerMode",
                        getString(R.string.shortcuts_developerMode),
                        R.drawable.baseline_developer_mode_24,
                        intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    )
                } else {
                    Snackbar.make(
                        view,
                        resources.getString(R.string.developerModeIsOff),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                true
            }

            view.findViewById<MaterialCardView>(R.id.accountSettings)?.setOnClickListener {
                val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
                startActivity(intent)
            }

            view.findViewById<MaterialCardView>(R.id.accountSettings)?.setOnLongClickListener {
                createShortcut(
                    "accountSettings",
                    getString(R.string.shortcuts_accountSettings),
                    R.drawable.outline_switch_account_24,
                    intent = Intent(Settings.ACTION_SYNC_SETTINGS)
                )
                true
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
        }

        setButton()
        setDownloadButton()
        // Inflate the layout for this fragment
        return view
    }

    // 新增資料類別
    data class DownloadItem(
        val key: String,
        val title: String,
        val url: String,
        val isIntent: Boolean
    )

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

        extractFilsUrlFromJson(getString(R.string.url_files)) { items ->
            gridLayout3?.removeAllViews()
            items.forEach { item ->
                val card = MaterialCardView(requireContext()).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = (160 * resources.displayMetrics.density).toInt()
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(
                            (16 * resources.displayMetrics.density).toInt(),
                            (16 * resources.displayMetrics.density).toInt(),
                            (16 * resources.displayMetrics.density).toInt(),
                            (16 * resources.displayMetrics.density).toInt()
                        )
                    }
                    isClickable = true
                    isFocusable = true
                    isCheckable = true
                    setPadding(
                        (32 * resources.displayMetrics.density).toInt(),
                        (32 * resources.displayMetrics.density).toInt(),
                        (32 * resources.displayMetrics.density).toInt(),
                        (32 * resources.displayMetrics.density).toInt()
                    )
                    setOnClickListener {
                        if (item.isIntent) {
                            try {
                                val intent = Intent.parseUri(item.url, Intent.URI_INTENT_SCHEME)
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "無法啟動應用程式", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            }
                        } else {
                            downloadAPPWithCheck(item.url)
                        }
                    }
                }

                val textView = android.widget.TextView(requireContext()).apply {
                    text = item.title
                    gravity = android.view.Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // 使用 sp 單位，支援螢幕縮放
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                card.addView(textView)
                gridLayout3?.addView(card)
            }
        }
    }


    private fun extractFilsUrlFromJson(url: String, onItemsReady: (List<DownloadItem>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = mutableListOf<DownloadItem>()
            try {
                val urlObject = URL(url)
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
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
                connection.disconnect()
                val jsonObject = JSONObject(response.toString())
                Log.d("JSON", jsonObject.toString())
                jsonObject.keys().forEach { key ->
                    val obj = jsonObject.getJSONObject(key)
                    items.add(
                        DownloadItem(
                            key,
                            obj.optString("title", key),
                            obj.optString("url", ""),
                            obj.optBoolean("isIntent", false)
                        )
                    )
                }
                // 切換到主線程調用回調函數
                launch(Dispatchers.Main) {
                    onItemsReady(items)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 即使出錯也要調用回調，防止UI阻塞
                launch(Dispatchers.Main) {
                    onItemsReady(emptyList())
                }
            }
        }
    }

    private fun createWidget() {
        val appWidgetManager = requireContext().getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
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

    private fun createShortcut(
        id: String,
        label: String,
        icon: Int,
        intent: Intent? = null,
        url: String? = null
    ) {
        val shortcutManager = context?.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null) {
            if (shortcutManager.isRequestPinShortcutSupported) {
                // Determine which intent to use
                val shortcutIntent = when {
                    intent != null -> intent
                    url != null -> Intent(Intent.ACTION_VIEW, url.toUri())
                    else -> return // No intent provided
                }

                // Create the shortcut with the appropriate intent
                val shortcut = ShortcutInfo.Builder(context, id)
                    .setShortLabel(label)
                    .setIcon(Icon.createWithResource(context, icon))
                    .setIntent(shortcutIntent)
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
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            else
                CustomTabsIntent.Builder().build()
                    .launchUrl(it, url.toUri())
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
        } else if (url.contains("apkmirror")) {
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
                        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    else
                        gotoBrowser(url)
                }
                setNegativeButton(R.string.downloadProblem) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            }
            .show()
    }
}
