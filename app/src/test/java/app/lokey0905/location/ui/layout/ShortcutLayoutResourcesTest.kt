package app.lokey0905.location.ui.layout

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class ShortcutLayoutResourcesTest {
    @Test
    fun allStaticShortcutCardsUseExplicitMarginsForGridLayout() {
        val cards = resource("layout/fragment_shortcuts.xml")
            .getElementsByTagName("com.google.android.material.card.MaterialCardView")
        assertEquals(13, cards.length)

        for (index in 0 until cards.length) {
            val card = cards.item(index) as Element
            val id = card.getAttributeNS(ANDROID_NAMESPACE, "id")

            // Framework GridLayout rereads individual margins after MarginLayoutParams.
            // Axis shortcuts cannot be relied on to override its margin defaults.
            listOf("layout_margin", "layout_marginHorizontal", "layout_marginVertical")
                .forEach { attribute ->
                    assertFalse("$id must not use $attribute", card.hasAttributeNS(ANDROID_NAMESPACE, attribute))
                }
            listOf("layout_marginLeft", "layout_marginRight").forEach { attribute ->
                assertEquals(
                    "$id $attribute",
                    "@dimen/shortcut_card_horizontal_margin",
                    card.getAttributeNS(ANDROID_NAMESPACE, attribute)
                )
            }
            listOf("layout_marginTop", "layout_marginBottom").forEach { attribute ->
                assertEquals(
                    "$id $attribute",
                    "@dimen/shortcut_card_margin",
                    card.getAttributeNS(ANDROID_NAMESPACE, attribute)
                )
            }
        }
    }

    @Test
    fun horizontalMarginsAreReducedWithoutChangingVerticalMargins() {
        val dimensions = resource("values/shortcut_layout.xml").getElementsByTagName("dimen")
        val values = (0 until dimensions.length).associate { index ->
            val dimension = dimensions.item(index) as Element
            dimension.getAttribute("name") to dimension.textContent.trim()
        }

        assertEquals("8dp", values["shortcut_card_horizontal_margin"])
        assertEquals("16dp", values["shortcut_card_margin"])
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
    }
}
