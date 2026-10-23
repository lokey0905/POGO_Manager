package app.lokey0905.location.catalog

import androidx.annotation.DrawableRes
import app.lokey0905.location.R

/** Bundled artwork, deliberately independent of installed packages and remote version state. */
@DrawableRes
fun catalogAppIcon(app: CatalogApp): Int = when (app) {
    CatalogApp.PIKMIN -> R.drawable.catalog_pikmin
    CatalogApp.JOYSTICK -> R.drawable.gps_icon
    CatalogApp.DEFIT -> R.drawable.defit_icon
    CatalogApp.FITNESS -> R.drawable.catalog_fitness
    CatalogApp.GOOGLE_FIT -> R.drawable.catalog_google_fit
    CatalogApp.HEALTH_CONNECT -> R.drawable.catalog_health_connect
    CatalogApp.APKMIRROR_INSTALLER -> R.drawable.apkmirrorinstaller_icon
}
