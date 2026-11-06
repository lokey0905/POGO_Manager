package app.lokey0905.location.navigation

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class NavigationResourcesTest {
    @Test fun homeIsFirstAndAllDestinationIdsArePreserved() {
        val items = resource("menu/nav.xml").getElementsByTagName("item")
        val ids = (0 until items.length).map { index ->
            (items.item(index) as Element).getAttributeNS(ANDROID_NAMESPACE, "id")
        }

        assertEquals(
            listOf("@+id/nav_home", "@+id/nav_poke", "@+id/nav_pikmin", "@+id/nav_mhn", "@+id/nav_shortcuts"),
            ids
        )
    }

    @Test fun phoneAndTabletNavigationUseTheSameMenuOrder() {
        listOf("layout/activity_main.xml", "layout-sw600dp-w840dp-land/activity_main.xml")
            .forEach { layout ->
                val elements = resource(layout).getElementsByTagName("*")
                val navigation = (0 until elements.length)
                    .map { elements.item(it) as Element }
                    .single { it.getAttributeNS(ANDROID_NAMESPACE, "id") == "@+id/navigation" }

                assertEquals(layout, "@menu/nav", navigation.getAttributeNS(APP_NAMESPACE, "menu"))
            }
    }

    private fun resource(path: String): Document {
        // Gradle runs from the app module; IDE runners may use the repository root.
        val file = listOf(File("src/main/res", path), File("app/src/main/res", path))
            .firstOrNull { it.isFile }
            ?: error("Cannot find Android resource: $path")
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        return factory.newDocumentBuilder().parse(file)
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"
    }
}
