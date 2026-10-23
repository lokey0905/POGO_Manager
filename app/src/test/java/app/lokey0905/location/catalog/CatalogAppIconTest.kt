package app.lokey0905.location.catalog

import app.lokey0905.location.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogAppIconTest {
    @Test fun pikminUsesBundledAppArtworkInsteadOfNavigationSymbol() {
        assertEquals(R.drawable.catalog_pikmin, catalogAppIcon(CatalogApp.PIKMIN))
    }

    @Test fun joystickUsesTheSameBundledResourceAsOtherGamePages() {
        assertEquals(R.drawable.gps_icon, catalogAppIcon(CatalogApp.JOYSTICK))
    }

    @Test fun fitnessUsesBundledLauncherArtworkInsteadOfGenericFitnessSymbol() {
        assertEquals(R.drawable.catalog_fitness, catalogAppIcon(CatalogApp.FITNESS))
    }

    @Test fun remainingAppsKeepTheirBundledArtworkWithoutPackageQueries() {
        assertEquals(R.drawable.defit_icon, catalogAppIcon(CatalogApp.DEFIT))
        assertEquals(R.drawable.catalog_google_fit, catalogAppIcon(CatalogApp.GOOGLE_FIT))
        assertEquals(R.drawable.catalog_health_connect, catalogAppIcon(CatalogApp.HEALTH_CONNECT))
        assertEquals(R.drawable.apkmirrorinstaller_icon, catalogAppIcon(CatalogApp.APKMIRROR_INSTALLER))
    }
}
