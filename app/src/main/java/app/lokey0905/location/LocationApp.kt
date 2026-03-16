package app.lokey0905.location

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.android.material.color.DynamicColors
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class LocationApp : Application() {

    private val tag = "LocationApp"

    override fun onCreate() {
        super.onCreate()

        // Apply Dynamic Colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        // Isolated service process also creates Application; skip SDK init there.
        if (Application.getProcessName() != packageName) {
            Log.i(tag, "Skip OneSignal init in process: ${Application.getProcessName()}")
            return
        }

        runCatching {
            // Verbose logging helps triage OEM-specific startup crashes.
            OneSignal.Debug.logLevel = LogLevel.VERBOSE
            OneSignal.initWithContext(this, "03f9b446-b22e-447c-8285-73d949ae118c")
        }.onFailure { e ->
            Log.e(tag, "OneSignal init failed", e)
        }
    }
}
