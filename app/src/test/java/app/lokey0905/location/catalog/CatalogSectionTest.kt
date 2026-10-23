package app.lokey0905.location.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogSectionTest {
    @Test fun gameAndJoystickRemainInListWithoutSectionHeadings() {
        assertEquals(listOf(CatalogApp.PIKMIN, CatalogApp.JOYSTICK), CatalogApp.entries.take(2))
        assertNull(CatalogApp.PIKMIN.section)
        assertNull(CatalogApp.JOYSTICK.section)
    }

    @Test fun stepsToolsShareOneSection() {
        assertEquals(listOf(CatalogApp.DEFIT, CatalogApp.FITNESS),
            CatalogApp.entries.filter { it.section == CatalogSection.STEPS })
    }

    @Test fun healthAppsShareOneSection() {
        assertEquals(listOf(CatalogApp.GOOGLE_FIT, CatalogApp.HEALTH_CONNECT),
            CatalogApp.entries.filter { it.section == CatalogSection.HEALTH })
    }

    @Test fun sectionOrderPreservesExistingAppOrderWithoutRepeatedHeadings() {
        val sections = CatalogApp.entries.map { it.section }
        val headings = sections.filterIndexed { index, section ->
            section != null && (index == 0 || sections[index - 1] != section)
        }
        assertEquals(CatalogSection.entries.toList(), headings)
        assertEquals(7, CatalogApp.entries.size)
    }

    @Test fun supplementaryInstallerHasOnlyItsOwnSection() {
        assertEquals(CatalogSection.INSTALLATION, CatalogApp.APKMIRROR_INSTALLER.section)
    }
}
