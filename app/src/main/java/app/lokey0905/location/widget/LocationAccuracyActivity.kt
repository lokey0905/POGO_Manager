package app.lokey0905.location.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import app.lokey0905.location.R

class LocationAccuracyActivity : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.locationaccuracyactivity)
        val buttonIntent = Intent(context, LocationAccuracyActivity::class.java).apply {
            action = ACTION_BUTTON_CLICK
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                buttonIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(context, 0, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        views.setOnClickPendingIntent(R.id.appwidget_button, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_BUTTON_CLICK) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            val location_accuracy_switch =
                sharedPreferences.getBoolean("location_accuracy_switch", false)
            var activityIntent = Intent()
            if (location_accuracy_switch) {
                // open location settings page
                activityIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activityIntent.component =
                        ComponentName(
                            context.getString(R.string.packageName_gms),
                            context.getString(R.string.packageName_gmsLocationAccuracyA12)
                        )
                } else
                    activityIntent.component =
                        ComponentName(
                            context.getString(R.string.packageName_gms),
                            context.getString(R.string.packageName_gmsLocationAccuracy)
                        )
            }

            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Add this line
            context.startActivity(activityIntent)
        }
    }


    companion object {
        private const val ACTION_BUTTON_CLICK = "com.example.ACTION_BUTTON_CLICK"
    }
}