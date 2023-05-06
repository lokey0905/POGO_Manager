package app.lokey0905.location.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import app.lokey0905.location.BuildConfig
import app.lokey0905.location.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class Setting : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_setting, container, false)
        var touch = 0

        @SuppressLint("SetTextI18n")
        fun about(){
            view.findViewById<LinearLayout>(R.id.LinearLayout_shopee).setOnClickListener {
                gotoBrowser(resources.getString(R.string.shopee))
            }
            view.findViewById<LinearLayout>(R.id.LinearLayout_facebook).setOnClickListener {
                gotoBrowser(resources.getString(R.string.facebook))
            }
            view.findViewById<LinearLayout>(R.id.LinearLayout_discord).setOnClickListener {
                gotoBrowser(resources.getString(R.string.discord))
            }
            view.findViewById<LinearLayout>(R.id.LinearLayout_youtube).setOnClickListener {
                gotoBrowser(resources.getString(R.string.youtube))
            }
            view.findViewById<LinearLayout>(R.id.LinearLayout_github).setOnClickListener {
                gotoBrowser(resources.getString(R.string.github))
            }
            view.findViewById<TextView>(R.id.appVersion).text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            view.findViewById<LinearLayout>(R.id.LinearLayout_appVersion).setOnClickListener {
                if (touch == 5){
                    Toast.makeText(context, "已啟用開發人員模式", Toast.LENGTH_SHORT).show()
                    setFragmentResult("developerMode", bundleOf("bundleKey" to true))
                }
                if (touch > 5){
                    showAboutDialog()
                }
                else touch++
            }
        }

        @SuppressLint("CutPasteId")
        fun switch(){
            view.findViewById<LinearLayout>(R.id.LinearLayout_switch1).setOnClickListener {
                view.findViewById<MaterialSwitch>(R.id.switch1).isChecked = !view.findViewById<MaterialSwitch>(R.id.switch1).isChecked
            }
            view.findViewById<LinearLayout>(R.id.LinearLayout_switch2).setOnClickListener {
                view.findViewById<MaterialSwitch>(R.id.switch2).isChecked = !view.findViewById<MaterialSwitch>(R.id.switch2).isChecked
            }
            view.findViewById<LinearLayout>(R.id.LinearLayout_switch3).setOnClickListener {
                view.findViewById<MaterialSwitch>(R.id.switch3).isChecked = !view.findViewById<MaterialSwitch>(R.id.switch3).isChecked
            }
            view.findViewById<LinearLayout>(R.id.LinearLayout_switch4).setOnClickListener {
                view.findViewById<MaterialSwitch>(R.id.switch4).isChecked = !view.findViewById<MaterialSwitch>(R.id.switch4).isChecked
            }

            view.findViewById<MaterialSwitch>(R.id.switch1).setOnCheckedChangeListener{ _, isChecked->
                if (isChecked) {
                    Toast.makeText(context, "自動抓與寶可夢已切換至測試通道", Toast.LENGTH_LONG).show()
                    setFragmentResult("testPgtools", bundleOf("bundleKey" to true))
                } else {
                    Toast.makeText(context, "已切換回正式通道", Toast.LENGTH_SHORT).show()
                    setFragmentResult("testPgtools", bundleOf("bundleKey" to false))
                }
            }

            view.findViewById<MaterialSwitch>(R.id.switch2).setOnCheckedChangeListener{ _, isChecked->
                if (isChecked) {
                    Toast.makeText(context, "已啟用新版檢測API", Toast.LENGTH_LONG).show()
                    setFragmentResult("newerCheckMockLocationApi", bundleOf("bundleKey" to true))
                } else {
                    Toast.makeText(context, "已切換回舊版檢測API", Toast.LENGTH_SHORT).show()
                    setFragmentResult("newerCheckMockLocationApi", bundleOf("bundleKey" to false))
                }
            }

            view.findViewById<MaterialSwitch>(R.id.switch3).setOnCheckedChangeListener{ _, isChecked->
                if (isChecked) {
                    Toast.makeText(context, "已啟用三星版寶可夢", Toast.LENGTH_LONG).show()
                    setFragmentResult("pokAresNoSupportDevices", bundleOf("bundleKey" to true))
                } else {
                    Toast.makeText(context, "已停用三星版寶可夢", Toast.LENGTH_SHORT).show()
                    setFragmentResult("pokAresNoSupportDevices", bundleOf("bundleKey" to false))
                }
            }

            view.findViewById<MaterialSwitch>(R.id.switch4).setOnCheckedChangeListener{ _, isChecked->
                if (isChecked) {
                    Toast.makeText(context, "已切換直接下載APK", Toast.LENGTH_LONG).show()
                    setFragmentResult("pokAresDownloadAPK", bundleOf("bundleKey" to true))
                } else {
                    Toast.makeText(context, "已切換透過三星商店下載", Toast.LENGTH_SHORT).show()
                    setFragmentResult("pokAresDownloadAPK", bundleOf("bundleKey" to false))
                }
            }
        }

        @SuppressLint("CutPasteId")
        fun check(){
            view.findViewById<MaterialSwitch>(R.id.switch3).isEnabled = Build.MANUFACTURER != "samsung"
            view.findViewById<LinearLayout>(R.id.LinearLayout_switch3).isEnabled = Build.MANUFACTURER != "samsung"

            view.findViewById<MaterialSwitch>(R.id.switch2).isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            view.findViewById<LinearLayout>(R.id.LinearLayout_switch2).isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

        view.findViewById<LinearLayout>(R.id.LinearLayout_disabledPgAutoUpdate).setOnClickListener {
            Toast.makeText(context, "點擊右上角三個點取消勾選自動更新", Toast.LENGTH_LONG).show()
            gotoBrowser(resources.getString(R.string.url_pok_store))
            Toast.makeText(context, "點擊右上角三個點取消勾選自動更新", Toast.LENGTH_LONG).show()
        }

        view.findViewById<LinearLayout>(R.id.LocationAccuracyActivity).setOnClickListener {
            val activityIntent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activityIntent.component =
                    ComponentName("com.google.android.gms", "com.google.android.gms.location.settings.LocationAccuracyV31Activity")
                startActivity(activityIntent)
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                activityIntent.component =
                    ComponentName("com.google.android.gms", "com.google.android.gms.location.settings.LocationAccuracyActivity")
                startActivity(activityIntent)
            }
            else {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }


        check()
        switch()
        about()
        // Inflate the layout for this fragment
        return view
    }

    private fun gotoBrowser(url: String){
        context?.let {
            CustomTabsIntent.Builder().build()
                .launchUrl(it, Uri.parse(url))
        }
    }

    @SuppressLint("SetTextI18n")
    fun showAboutDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(),
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .create()
        val dialogView: View = View.inflate(requireContext(), R.layout.dialog_about, null)
        dialog.setView(dialogView)
        dialogView.findViewById<TextView>(R.id.design_about_title).text = resources.getString(R.string.app_name)
        dialogView.findViewById<TextView>(R.id.design_about_version).text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        dialogView.findViewById<TextView>(R.id.design_about_info).text = "相關檔案皆為網路搜尋取得\n檔案不歸我擁有\n2023 by lokey0905"
        dialog.show()
    }
}