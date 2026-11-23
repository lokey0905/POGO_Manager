package app.lokey0905.location.ui.layout

import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class GameHeaderResourcesTest {
    @Test
    fun allGameHeadersStartWithTheSameVersionCheckStatus() {
        listOf("poke", "mhn", "pikmin").forEach { game ->
            val header = resource("layout/fragment_apps_$game.xml")
                .getElementsByTagName("com.google.android.material.appbar.SubtitleCollapsingToolbarLayout")
                .item(0) as Element

            assertEquals(game, "@string/version_summary_loading_brief", header.getAttributeNS(APP_NAMESPACE, "subtitle"))
            assertEquals(game, "?attr/collapsingToolbarLayoutLargeStyle", header.getAttribute("style"))
        }
    }

    @Test
    fun sharedVersionCheckWordingRetainsProgressWithoutClaimingEveryAppIsCurrent() {
        val strings = strings("values/version_labels.xml")

        assertEquals("版本檢查中", strings["version_summary_loading_brief"])
        assertEquals("版本檢查完成", strings["version_summary_complete"])
        assertEquals("部分版本檢查失敗，請下拉重試", strings["version_summary_partial_failure"])
        listOf("version_summary_loading", "version_summary_loading_with_failures").forEach { name ->
            val message = strings.getValue(name)
            assertTrue(message.startsWith(strings.getValue("version_summary_loading_brief")))
            assertTrue(message.contains("%1\$d"))
        }
    }

    @Test
    fun managerDownloadDestinationIsTheGithubReleaseWebPage() {
        val url = URI(strings("values/url.xml").getValue("github_releases"))

        assertEquals("https", url.scheme)
        assertEquals("github.com", url.host)
        assertEquals("/lokey0905/POGO_Manager/releases/latest", url.path)
    }

    private fun strings(path: String): Map<String, String> {
        val elements = resource(path).getElementsByTagName("string")
        return (0 until elements.length).associate { index ->
            val element = elements.item(index) as Element
            element.getAttribute("name") to element.textContent.trim()
        }
    }

    private fun resource(path: String): Document {
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
        const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"
    }
}
